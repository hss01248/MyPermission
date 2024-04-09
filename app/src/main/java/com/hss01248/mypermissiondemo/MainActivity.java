package com.hss01248.mypermissiondemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.hss01248.basewebview.BaseWebviewActivity;
import com.hss01248.bus.AndroidBus;
import com.hss01248.bus.ContextBusObserver;
import com.hss01248.location.LocationInfo;
import com.hss01248.location.LocationStateInfo;
import com.hss01248.location.LocationStateUtil;
import com.hss01248.location.LocationSync;
import com.hss01248.location.LocationUtil;
import com.hss01248.location.MapUtil;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.MyLocationFastCallback;
import com.hss01248.location.QuietLocationUtil;
import com.hss01248.permission.IPermissionDialog;
import com.hss01248.permission.IPermissionDialogBtnClickListener;
import com.hss01248.permission.MyPermissions;
import com.hss01248.permission.ext.IExtPermission;
import com.hss01248.permission.ext.IExtPermissionCallback;
import com.hss01248.permission.ext.MyPermissionsExt;
import com.hss01248.permission.ext.permissions.ApkPermissionImpl;
import com.hss01248.permission.ext.permissions.ManageMediaPermission;
import com.hss01248.permission.ext.permissions.NotificationListenerPermissionImpl;
import com.hss01248.permission.ext.permissions.NotificationPermission;
import com.hss01248.permission.ext.permissions.StorageManagerPermissionImpl;
import com.hss01248.permission.ext.permissions.UsageAccessPermissionImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.init(getApplication());
    }

    public void normal(View view) {
        MyPermissions.requestByMostEffort(false, false, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:" + Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:" + Arrays.toString(deniedForever.toArray()) + "\n" + Arrays.toString(denied.toArray()));
            }
        }, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE);
    }

    public void beforeRequest(View view) {
        MyPermissions.requestByMostEffort(true, false, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:" + Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:" + Arrays.toString(deniedForever.toArray()) + "\n" + Arrays.toString(denied.toArray()));
            }
        }, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE);
    }

    public void afterDenied(View view) {
        ///MyPermissions.setDefaultAlertDialog();
        MyPermissions.create()
                .setAfterPermissionMsg("after msg")
                .setGuideToSettingMsg("guide to settings")
                .setAfterDialogTitle("permission request")
                .setPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE})
                .setShowAfterRequest(true)
                .setShowBeforeRequest(false)
/*                .setDialog(new IPermissionDialog() {
                    @Override
                    public void show(boolean isGuideToSetting, @Nullable String title,
                                     @Nullable String afterPermissionMsg, @Nullable String guideToSettingMsg,
                                     List<String> permissions, IPermissionDialogBtnClickListener listener) {

                    }
                })*/
                .callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:" + Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:" + Arrays.toString(deniedForever.toArray()) + "\n" + Arrays.toString(denied.toArray()));
            }
        });
    }

    public void both(View view) {
        MyPermissions.requestByMostEffort(true, true, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:" + Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:" + Arrays.toString(deniedForever.toArray()) + "\n" + Arrays.toString(denied.toArray()));
            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE);
    }

    public void getLocation(View view) {
        LocationUtil.getLocation(view.getContext(), false, 10000, false, false, LogProxy.getProxy(new MyLocationCallback() {

            @Override
            public void onSuccess(Location location, String msg) {
               // ToastUtils.showLong("success," + msg + ", location:" + location);
                LogUtils.i(msg, location);
                showFormatedLocationInfoInDialog(location);
            }

            @Override
            public boolean configShowLoadingDialog() {
                return true;
            }

            @Override
            public boolean configUseSpCache() {
                return false;
            }

            @Override
            public boolean configUseSystemLastKnownLocation() {
                return true;
            }

            @Override
            public void onGmsSwitchDialogShow() {
                MyLocationCallback.super.onGmsSwitchDialogShow();
            }

            @Override
            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                ToastUtils.showLong(type + "," + msg);
                LogUtils.w(msg, type);
            }
        }));
    }

    public void multiPermission(View view) {
        // MyPermissions.requestByMostEffort();
    }

    private void ask(IExtPermission permission) {
        MyPermissionsExt.askPermission(this, permission, new IExtPermissionCallback() {
            @Override
            public void onGranted(String name) {
                LogUtils.i("onGranted",permission.toString());
                ToastUtils.showShort("onGranted " + name);
            }

            @Override
            public void onDenied(String name) {
                LogUtils.w("onGranted",permission.toString());
                ToastUtils.showShort("onDenied " + name);
            }
        });
    }

    public void isInManifest(View view) {
        boolean stateInManifest = MyPermissions.isStateInManifest(Manifest.permission.READ_SMS);
        ToastUtils.showLong("sms是否声明在manifest里:" + stateInManifest);

        /*boolean stateInManifest2 = MyPermissions.isStateInManifest(Manifest.permission.READ_EXTERNAL_STORAGE);
        ToastUtils.showLong("READ_EXTERNAL_STORAGE是否声明在manifest里:"+stateInManifest2);*/
    }

    public void askExtPermissions(View view) {

        ask(new ApkPermissionImpl());
    }


    public void askNotification(View view) {
        ask(new NotificationPermission());
    }

    public void askManagerAllStorage(View view) {
        ask(new StorageManagerPermissionImpl());
    }

    public void askNotificationListener(View view) {
        ask(new NotificationListenerPermissionImpl());
    }

    public void useageStatus(View view) {
        ask(new UsageAccessPermissionImpl());
    }

    public void getLocationByLocationManager(View view) {
        /*LocationConfiguration awesomeConfiguration = new LocationConfiguration.Builder()
                .keepTracking(false)
                .askForPermission(new PermissionConfiguration.Builder()
                        .permissionProvider(new YourCustomPermissionProvider())
                        .rationaleMessage("Gimme the permission!")
                        .rationaleDialogProvider(new YourCustomDialogProvider())
                        .requiredPermissions(new String[] { permission.ACCESS_FINE_LOCATION })
                        .build())
                .useGooglePlayServices(new GooglePlayServicesConfiguration.Builder()
                        .locationRequest(YOUR_CUSTOM_LOCATION_REQUEST_OBJECT)
                        .fallbackToDefault(true)
                        .askForGooglePlayServices(false)
                        .askForSettingsApi(true)
                        .failOnConnectionSuspended(true)
                        .failOnSettingsApiSuspended(false)
                        .ignoreLastKnowLocation(false)
                        .setWaitPeriod(20 * 1000)
                        .build())
                .useDefaultProviders(new DefaultProviderConfiguration.Builder()
                        .requiredTimeInterval(5 * 60 * 1000)
                        .requiredDistanceInterval(0)
                        .acceptableAccuracy(5.0f)
                        .acceptableTimePeriod(5 * 60 * 1000)
                        .gpsMessage("Turn on GPS?")
                        .gpsDialogProvider(new YourCustomDialogProvider())
                        .setWaitPeriod(ProviderType.GPS, 20 * 1000)
                        .setWaitPeriod(ProviderType.NETWORK, 20 * 1000)
                        .build())
                .build();

        LocationManager awesomeLocationManager = new LocationManager.Builder(getApplicationContext())
                .activity(activityInstance) // Only required to ask permission and/or GoogleApi - SettingsApi
                .fragment(fragmentInstance) // Only required to ask permission and/or GoogleApi - SettingsApi
                .configuration(awesomeConfiguration)
                .locationProvider(new YourCustomLocationProvider())
                .notify(new LocationListener() {  })
                .build();
        awesomeLocationManager.get();*/
    }

    public void getLocationFast(View view) {
        LocationUtil.getLocationFast( 15000,
                new MyLocationFastCallback() {
                    @Override
                    public boolean configAcceptOnlyCoarseLocationPermission() {
                        return false;
                    }

                    @Override
                    public boolean configForceUseOnlyGpsProvider() {
                        return false;
                    }

                    @Override
                    public void onSuccessFast(Location location, String msg) {
                        //ToastUtils.showLong("success," + msg + ", location:" + location);
                        LogUtils.i(msg, location);
                        showFormatedLocationInfoInDialog(location);
                    }

                    @Override
                    public void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest) {
                        ToastUtils.showLong(type + "," + msg);
                        LogUtils.w(msg, type);
                    }
                });
    }

    public void concurrentModify(View view) {
        ThreadUtils.executeByCpu(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                for (int i = 0; i < 100; i++) {
                    int finalI = i;
                    ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<Object>() {
                        @Override
                        public Object doInBackground() throws Throwable {
                            LogUtils.w("putToCache---->" + finalI);
                            Location location = new Location("gps");
                            location.setLatitude(new Random(80).nextDouble());
                            location.setLongitude(new Random(90).nextDouble());
                            location.setTime(System.currentTimeMillis());
                            LocationSync.putToCache(location, "gms", false, 0, null);
                            return null;
                        }

                        @Override
                        public void onSuccess(Object result) {

                        }
                    });
                }
                return null;
            }

            @Override
            public void onSuccess(Object result) {

            }
        });

        ThreadUtils.executeByCpu(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                for (int i = 0; i < 100; i++) {
                    int finalI = i;
                    ThreadUtils.executeByCpu(new ThreadUtils.SimpleTask<Object>() {
                        @Override
                        public Object doInBackground() throws Throwable {
                            LogUtils.w("getFullLocationInfo---->" + finalI);
                            LocationInfo fullLocationInfo = LocationSync.getFullLocationInfo();
                            LogUtils.i(fullLocationInfo);
                            return null;
                        }

                        @Override
                        public void onSuccess(Object result) {

                        }
                    });
                }
                return null;
            }

            @Override
            public void onSuccess(Object result) {

            }
        });


    }

    public void getLocationSilent(View view) {


       LocationUtil.getLocationSilent( 10000, new MyLocationCallback() {
            @Override
            public void onSuccess(Location location, String msg) {
                //ToastUtils.showLong("success," + msg + ", location:" + location);
                LogUtils.i(msg, location);
                showFormatedLocationInfoInDialog(location);
            }

            @Override
            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                ToastUtils.showLong(type + "," + msg);
                LogUtils.w(msg, type);
            }
        });
    }

    public void showCachedLocation(View view) {
        AlertDialog dialog = LocationSync.showFormatedLocationInfosInDialog();

        ThreadUtils.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocationFast(null);
            }
        },2000);
        //ui跟着数据的更新而刷新: 利用Android bus
        AndroidBus.observerByTag("location", new ContextBusObserver<List<LocationInfo>>(this) {
            @Override
            protected void doObserverReally(List<LocationInfo> obj) {
                String infos = LocationSync.getFormatedLocationInfos();
                dialog.setMessage(infos);
            }
        });
    }



    public void gpsOnly(View view) {
        android.location.LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            long start = System.currentTimeMillis();
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        //ToastUtils.showLong( "cost(s):"+(System.currentTimeMillis() - start)/1000+", location:" + location);
                        LogUtils.i( location,"cost(s):"+(System.currentTimeMillis() - start)/1000,
                                "old:"+(System.currentTimeMillis() - location.getTime()));
                        showFormatedLocationInfoInDialog(location);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        LocationListener.super.onProviderDisabled(provider);
                        LogUtils.w("onProviderDisabled",provider);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        LocationListener.super.onStatusChanged(provider, status, extras);
                        LogUtils.w("onStatusChanged",provider,status,extras);
                    }
                }, Looper.getMainLooper());
        }else {
            ToastUtils.showShort("no permission");
        }

    }

    public void fusedOnly(View view) {
        android.location.LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            long start = System.currentTimeMillis();
            locationManager.requestSingleUpdate("fused", new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    //ToastUtils.showLong( "cost(s):"+(System.currentTimeMillis() - start)/1000+", location:" + location);
                    LogUtils.i( location,"cost(ms):"+(System.currentTimeMillis() - start),
                            "old:"+(System.currentTimeMillis() - location.getTime()));
                    showFormatedLocationInfoInDialog(location);
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    LocationListener.super.onProviderDisabled(provider);
                    LogUtils.w("onProviderDisabled",provider);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    LocationListener.super.onStatusChanged(provider, status, extras);
                    LogUtils.w("onStatusChanged",provider,status,extras);
                }
            }, Looper.getMainLooper());
        }else {
            ToastUtils.showShort("no permission");
        }
    }

    public void showLocationInmap(View view) {
        LocationInfo info = LocationSync.getFullLocationInfo();
        if(info ==null){
            ToastUtils.showShort("没有缓存数据");
            return;
        }
      MapUtil.showMapChooseDialog(info.lattidude,info.longtitude);
    }



    public  void showFormatedLocationInfoInDialog(Location location){
        MapUtil.showFormatedLocationInfoInDialog(location);
    }

    public void coarseLocationOnly(View view) {
        LocationUtil.getLocationFast( 15000,
                new MyLocationFastCallback() {

                    @Override
                    public boolean configAcceptOnlyCoarseLocationPermission() {
                        return true;
                    }

                    @Override
                    public boolean configShowLoadingDialog() {
                        return true;
                    }

                    @Override
                    public void onSuccessFast(Location location, String msg) {
                        //ToastUtils.showLong("success," + msg + ", location:" + location);
                        LogUtils.i(msg, location);
                        showFormatedLocationInfoInDialog(location);
                    }

                    @Override
                    public void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest) {
                        ToastUtils.showLong(type + "," + msg);
                        LogUtils.w(msg, type);
                    }
                });
    }

    public void onlySwitch(View view) {
        LocationUtil.getLocation(this,false, 10000, false, false, new MyLocationCallback() {
            @Override
            public boolean configJustAskPermissionAndSwitch() {
                return true;
            }

            @Override
            public void onSuccess(Location location, String msg) {
                ToastUtils.showShort(msg);
                LogUtils.i(msg);

            }

            @Override
            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                ToastUtils.showShort(msg);
                LogUtils.w(msg);
            }
        });
    }

    public void quickLocationNoAfterDialog(View view) {
        //MyPermissions.setCanAcceptOnlyCoarseLocationPermission(true);
        LocationUtil.getLocation(this, false, 10000, false, false, new MyLocationCallback() {
            @Override
            public void onSuccess(Location location, String msg) {
                showFormatedLocationInfoInDialog(location);
            }

            @Override
            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                ToastUtils.showLong(type + "," + msg);
            }
        });
    }

    public void locationState(View view) {

        LocationStateUtil.getLocationState(new Consumer<LocationStateInfo>() {
            @Override
            public void accept(LocationStateInfo locationStateInfo) throws Exception {
                ThreadUtils.getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        String json = new GsonBuilder().setPrettyPrinting().create().toJson(locationStateInfo);

                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("定位状态")
                                .setMessage(json)
                                .setPositiveButton("ok",null)
                                .create();
                        dialog.show();
                    }
                });
            }
        });




    }

    public void isLocationEnabled1(View view) {
        LogUtils.w("isEnabled: "+ QuietLocationUtil.isLocationEnabled((LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE)));
    }

    public void isLocationEnabled2(View view) {
        LogUtils.w("isEnabled2: "+ QuietLocationUtil.isLocationEnabled2((LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE)));
    }

    public void isLocationEnabled3(View view) {
        LogUtils.w("isEnabled3: "+ QuietLocationUtil.isLocationEnabled3());
    }

    public void askManageMedia(View view) {
        MyPermissionsExt.askPermission(this, new ManageMediaPermission(),
                new IExtPermissionCallback() {
                    @Override
                    public void onGranted(String name) {
                        ToastUtils.showShort("已经允许:"+name);
                    }

                    @Override
                    public void onDenied(String name) {
                        ToastUtils.showShort("已经拒绝:"+name);
                    }
                });
    }
}