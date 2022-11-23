package com.hss01248.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
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

import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

/**
 * @Despciption todo
 * @Author hss
 * @Date 23/11/2022 10:20
 * @Version 1.0
 */
public class LocationStateUtil {

    /**
     * 判断是否支持高精度定位服务
     * @return
     */
    public static void getLocationState(Consumer<LocationStateInfo> infoConsumer){
        //从系统服务中获取定位管理器
        LocationManager lm = (LocationManager) Utils.getApp().getSystemService(Context.LOCATION_SERVICE);
        LocationStateInfo info = new LocationStateInfo();
        info.preciseLocationSwitchEnabled =  lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        info.coarseLocationPermissionGranted = PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION);
        info.fineLocationPermissionGranted = PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION);

        info.locationSwitchOpen = QuietLocationUtil.isLocationEnabled(lm);

        info.gmsAvaliabled = QuietLocationUtil.isGmsAvaiable(Utils.getApp());
        if(!info.gmsAvaliabled){
            try {
                infoConsumer.accept(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        checkGmsSwitchState(Utils.getApp(),
                new Runnable() {
                    @Override
                    public void run() {
                        info.gmsLocationSwitchOpened = true;
                        try {
                            infoConsumer.accept(info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        info.gmsLocationSwitchOpened = false;
                        try {
                            infoConsumer.accept(info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        info.gmsLocationSwitchOpened = false;
                        try {
                            infoConsumer.accept(info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

    }

    public static String buildViewGpsInMapUrl(double lattidude,double longtitude){
        String url = "https://www.hss01248.tech/baidumap.html?lat="+ lattidude+"&lng="+ longtitude+"&from=gps";
        return url;
    }
    public static void viewLocationOnMap(double lattidude,double longtitude){
        String url = buildViewGpsInMapUrl(lattidude, longtitude);
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            ActivityUtils.getTopActivity().startActivity(intent);
        }catch (Throwable throwable){
            ToastUtils.showLong(throwable.getMessage());
        }
    }

    public static boolean isPreciseLocationSwitchEnabled(LocationManager manager){
       return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled("fused");
    }

    private static void checkGmsSwitchState(Context context, Runnable open,Runnable toOpen,Runnable error) {
        boolean[] haveCallbacked = new boolean[]{false};
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


            //超时处理
            ThreadUtils.executeBySingleWithDelay(new ThreadUtils.SimpleTask<Object>() {
                @Override
                public Object doInBackground() throws Throwable {
                    return null;
                }

                @Override
                public void onSuccess(Object result) {
                    if(haveCallbacked[0]){
                        return;
                    }
                    haveCallbacked[0] = true;
                    LogUtils.w("gms 判断状态超时,辣鸡gms: LocationServices.SettingsApi.checkLocationSettings");
                    error.run();
                    //getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false,asQuickAsPossible,useLastKnownLocation ,true,callback);
                }
            },2000, TimeUnit.MILLISECONDS);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            // 有的手机TMD这里不回调,也不抛异常, 辣鸡GMS
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    if(haveCallbacked[0]){
                        return;
                    }
                    haveCallbacked[0] = true;
                    if (result == null) {
                        error.run();
                        return;
                    }
                    LogUtils.i(result.getStatus(), result.getLocationSettingsStates());
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            //Location settings satisfied
                            Log.i("gms", "Location settings satisfied");
                           open.run();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            //Location settings are not satisfied. Show the user a dialog to upgrade location settings
                            toOpen.run();
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Location settings are inadequate, and cannot be fixed here. Dialog not created.
                            //todo
                            Log.w("gms", "Error enabling location. Please try again");
                           error.run();
                            break;
                        default:
                            //todo
                            Log.w("gms", "Error enabling location. Please try again2-->"+status.getStatusCode());
                            error.run();
                            break;
                    }
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if(haveCallbacked[0]){
                return;
            }
            haveCallbacked[0] = true;
            error.run();
        }

    }
}
