package com.hss01248.location.rx;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.GoOutOfAppForResultFragment;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.activityresult.TheActivityListener;
import com.hss01248.location.GoOutOfAppForResultFragment2;
import com.hss01248.location.LocationRequestConfig;
import com.hss01248.location.LocationSync;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.QuietLocationUtil;
import com.hss01248.location.R;
import com.hss01248.permission.MyPermissions;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;

/**
 * @Despciption todo
 * @Author hss
 * @Date 08/12/2021 14:53
 * @Version 1.0
 */
public class RxLocationUtil {



    LocationRequestConfig config;
    /**
     * 默认版 拒绝权限后有一次挽回行为
     *
     * @param context
     * @param callback
     */
    public static Observable<Pair<String,Location>> getLocation(Context context, MyLocationCallback callback) {
        return getLocation(context, false, 10000, false,
                true, callback);
    }

    public static Location getLocation() {
        if (LocationSync.getLocation() != null) {
            return LocationSync.getLocation();
        }
        if (LocationSync.getLatitude() != 0 && LocationSync.getLongitude() != 0) {
            Location location = new Location(LocationManager.PASSIVE_PROVIDER);
            location.setLongitude(LocationSync.getLongitude());
            location.setLatitude(LocationSync.getLatitude());
            return location;
        }
        return null;
    }

    public static Observable<Pair<String,Location>> getLocation(Context context, boolean silent, int timeout, boolean showBeforeRequest, boolean showAfterRequest, MyLocationCallback callback) {
       return getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, true,false,false, callback);
    }




    /**
     * 完全配置版
     *
     * @param context
     * @param timeout
     * @param callback
     */
    private static Observable<Pair<String,Location>> getLocation(Context context, boolean silent, int timeout, boolean showBeforeRequest,
                                                                 boolean showAfterRequest, boolean requestGmsDialog, boolean asQuickAsPossible, boolean useLastKnownLocation, MyLocationCallback callback) {


        if (silent) {
           return doRequestLocation(context, timeout, callback);

        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //如果谷歌服务可用,则直接申请谷歌:
        if (RxQuietLocationUtil.isGmsAvaiable(context) && requestGmsDialog) {
           return checkSwitchByGms(context, silent, timeout, showBeforeRequest, showAfterRequest,asQuickAsPossible,useLastKnownLocation , callback, true);
        }
        //开关打开,则去申请权限
        if (QuietLocationUtil.isLocationEnabled(locationManager)) {
           return checkPermission(context, timeout, showBeforeRequest, showAfterRequest, callback);
        }
        return Observable.create(new ObservableOnSubscribe<Pair<String, Location>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String, Location>> emitter) throws Exception {
                callback.onGmsSwitchDialogShow();
                //开关关闭,就去申请打开开关
                AlertDialog alertDialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                        .setTitle(R.string.location_tip)
                        .setMessage(R.string.location_msg_gps)
                        .setPositiveButton(R.string.location_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), locationIntent, new ActivityResultListener() {
                                    @Override
                                    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                                        if (!QuietLocationUtil.isLocationEnabled(locationManager)) {
                                            callback.onGmsDialogCancelClicked();
                                            emitter.onError(new LocationFailException("location switch off-2").setFailBeforeReallyRequest(true));
                                           // callback.onFailed(2, "location switch off-2",true);
                                            return;
                                        }
                                        callback.onGmsDialogOkClicked();
                                        Observable<Pair<String, Location>> pairObservable = checkPermission(context, timeout, showBeforeRequest, showAfterRequest, callback);
                                        connectEmitterAndObservable(emitter,pairObservable);
                                    }

                                    @Override
                                    public void onActivityNotFound(Throwable e) {

                                    }
                                });

                            }
                        }).setNegativeButton(R.string.location_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                callback.onGmsDialogCancelClicked();
                                emitter.onError(new LocationFailException("location switch off").setFailBeforeReallyRequest(true));
                               // callback.onFailed(2, "location switch off",true);
                            }
                        }).create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }).subscribeOn(AndroidSchedulers.mainThread());



    }

    private static Observable<Pair<String,Location>> checkSwitchByGms(Context context, boolean silent, int timeout, boolean showBeforeRequest,
                                         boolean showAfterRequest,boolean asQuickAsPossible,boolean useLastKnownLocation, MyLocationCallback callback, boolean isFirstIn) {

        return Observable.create(new ObservableOnSubscribe<Pair<String, Location>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String, Location>> emitter) throws Exception {
                try {
                    GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                            .addApi(LocationServices.API).build();
                    googleApiClient.connect();

                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setInterval(10000);
                    locationRequest.setFastestInterval(10000 / 2);

                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);

                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                        @Override
                        public void onResult(LocationSettingsResult result) {
                            if (result == null) {
                                Observable<Pair<String, Location>> location = getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible, useLastKnownLocation, callback);
                                connectEmitterAndObservable(emitter,location);
                                return;
                            }
                            LogUtils.i(result.getStatus(), result.getLocationSettingsStates());
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    //Location settings satisfied
                                    Log.i("gms", "Location settings satisfied");
                                    if(!isFirstIn){
                                        callback.onGmsDialogOkClicked();
                                    }
                                    checkPermission(context, timeout, showBeforeRequest, showAfterRequest, callback);
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    //Location settings are not satisfied. Show the user a dialog to upgrade location settings
                                    if (isFirstIn) {
                                        callback.onGmsSwitchDialogShow();
                                        Observable<Pair<String, Location>> pairObservable = requestGmsSwitch(context, silent, timeout, showAfterRequest, showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, result);
                                        connectEmitterAndObservable(emitter,pairObservable);
                                    } else {
                                        callback.onGmsDialogCancelClicked();
                                        emitter.onError(new LocationFailException("location switch off-gms").setFailBeforeReallyRequest(true));
                                        //callback.onFailed(2, "location switch off-gms",true);
                                        //todo emitter.onError();
                                        // Log.w("gms", "不同意gms弹窗,那么绕过gms,请求原生定位");
                                        // getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false,asQuickAsPossible,useLastKnownLocation , callback);
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    //Location settings are inadequate, and cannot be fixed here. Dialog not created.
                                    //todo
                                    Log.w("gms", "Error enabling location. Please try again");
                                    Observable<Pair<String, Location>> location =  getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false,asQuickAsPossible,useLastKnownLocation , callback);
                                    connectEmitterAndObservable(emitter,location);
                                    break;
                                default:
                                    //todo
                                    Log.w("gms", "Error enabling location. Please try again2");
                                    Observable<Pair<String, Location>> location2 =  getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible,useLastKnownLocation ,callback);
                                    connectEmitterAndObservable(emitter,location2);
                                    break;
                            }
                        }
                    });
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });


    }

    private static Observable<Pair<String,Location>> requestGmsSwitch(Context context, boolean silent, int timeout, boolean showBeforeRequest, boolean showAfterRequest,
                                         boolean asQuickAsPossible,boolean useLastKnownLocation,MyLocationCallback callback, LocationSettingsResult result) {
       return Observable.create(new ObservableOnSubscribe<Pair<String, Location>>() {
           @Override
           public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String, Location>> emitter) throws Exception {
               new GoOutOfAppForResultFragment2((FragmentActivity) ActivityUtils.getTopActivity(), null).goOutApp(new ActivityResultListener() {
                   @Override
                   public boolean onInterceptStartIntent(@NonNull Fragment fragment, @Nullable Intent intent, int requestCode) {
                       ThreadUtils.getMainHandler().postDelayed(new Runnable() {
                           @Override
                           public void run() {
                               try {
                                   result.getStatus().startResolutionForResult(ActivityUtils.getTopActivity(), requestCode);
                               } catch (IntentSender.SendIntentException e) {
                                   Log.i("location", "PendingIntent unable to execute request.");
                                   e.printStackTrace();
                                   //todo
                                   Observable<Pair<String, Location>> location = getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible, useLastKnownLocation, callback);
                                    connectEmitterAndObservable(emitter,location);
                               }
                           }
                       }, 300);
                       return true;
                   }

                   @Override
                   public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                       //再次检查
                       Observable<Pair<String, Location>> pairObservable = checkSwitchByGms(context, silent, timeout, showBeforeRequest, showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, false);
                        connectEmitterAndObservable(emitter,pairObservable);
                   }

                   @Override
                   public void onActivityNotFound(Throwable e) {
                       //todo
                   }
               });
           }
       }).subscribeOn(AndroidSchedulers.mainThread());

    }

    private static Observable<Pair<String,Location>> checkPermission(Context context, int timeout, boolean showBeforeRequest, boolean showAfterRequest, MyLocationCallback callback) {
        if (PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                && PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
          return   doRequestLocation(context, timeout, callback);
        } else {
            return Observable.create(new ObservableOnSubscribe<Pair<String, Location>>() {
                @Override
                public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String, Location>> emitter) throws Exception {
                    MyPermissions.requestByMostEffort(
                            showBeforeRequest,
                            showAfterRequest,
                            new PermissionUtils.FullCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> granted) {
                                    Observable<Pair<String, Location>> pairObservable = doRequestLocation(context, timeout, callback);
                                    connectEmitterAndObservable(emitter,pairObservable);
                                }

                                @Override
                                public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                                    //callback.onFailed(1, "no permission",true);
                                    emitter.onError(new LocationFailException("no permission").setFailBeforeReallyRequest(true));
                                }
                            }, Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }).subscribeOn(AndroidSchedulers.mainThread());

        }
    }
    //todo 对接 emitter.onNext();
    private static void connectEmitterAndObservable(ObservableEmitter<Pair<String, Location>> emitter,Observable<Pair<String, Location>> pairObservable){
        pairObservable.subscribe(new Observer<Pair<String, Location>>() {
            @Override
            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
            }

            @Override
            public void onNext(@io.reactivex.annotations.NonNull Pair<String, Location> stringLocationPair) {
                emitter.onNext(stringLocationPair);
            }

            @Override
            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                emitter.onError(e);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        });
    }

    private static Observable<Pair<String,Location>> doRequestLocation(Context context, int timeout, MyLocationCallback callback) {
        callback.onBeforeReallyRequest();
       // new QuietLocationUtil().getLocation(context, timeout, callback);
        //todo
        return Observable.create(new ObservableOnSubscribe<Pair<String, Location>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String, Location>> emitter) throws Exception {
                Observable<Pair<String,Location>> pairObservable =  new RxQuietLocationUtil2().getLocation(Utils.getApp(),timeout,false);
                connectEmitterAndObservable(emitter,pairObservable);
            }
        });
    }


}
