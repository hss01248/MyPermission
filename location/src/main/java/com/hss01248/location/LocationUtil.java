package com.hss01248.location;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.StringUtils;
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
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.GoOutOfAppForResultFragment;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.activityresult.TheActivityListener;
import com.hss01248.permission.DefaultPermissionDialog;
import com.hss01248.permission.IPermissionDialog;
import com.hss01248.permission.MyPermissions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Despciption todo
 * @Author hss
 * @Date 08/12/2021 14:53
 * @Version 1.0
 */
public class LocationUtil {


    public static ILocationMetric getLocationMetric() {
        return locationMetric;
    }

    public static void setLocationMetric(ILocationMetric locationMetric) {
        LocationUtil.locationMetric = locationMetric;
    }

    private static ILocationMetric locationMetric;


    public static void getLocationSilent(long timeoutMills, MyLocationCallback callback) {
        new QuietLocationUtil().getLocation(Utils.getApp(), (int) timeoutMills, callback);
    }

    public static void getLocationFast(long timeoutMills, MyLocationFastCallback callback) {
        LocationUtil.getLocation(Utils.getApp(), false, (int) timeoutMills, false, true, callback);
    }

    /**
     * 默认版 拒绝权限后有一次挽回行为
     *
     * @param context
     * @param callback
     */
    public static void getLocation(Context context, MyLocationCallback callback) {
        getLocation(context, false, 10000, false,
                true, callback);
    }

    public static Location getLocation() {
        if (LocationSync.getLocation() != null) {
            return LocationSync.getLocation();
        }
        return null;
    }


    public static void getLocationSilent(Context context, int timeoutMills, MyLocationCallback callback) {
        getLocation(context, true, timeoutMills, false, false, callback);
    }

    public static void getLocation(IPermissionDialog alertDialog, Context context, boolean silent, int timeoutMills,
                                   boolean showBeforeRequest, boolean showAfterRequest,
                                   MyLocationCallback callback) {
        if (callback instanceof WrappedLocationCallback) {

        } else {
            callback = new WrappedLocationCallback(callback);
        }

        getLocation(alertDialog,context, silent, timeoutMills, showBeforeRequest, showAfterRequest,
                true, false, false, false, callback);
    }

    public static void getLocation(Context context, boolean silent, int timeoutMills,
                                   boolean showBeforeRequest, boolean showAfterRequest,
                                   MyLocationCallback callback) {
            getLocation(null,context,silent,timeoutMills,showBeforeRequest,showAfterRequest,callback);
    }

    /**
     * 完全配置版
     *
     * @param context
     * @param timeout
     * @param callback
     */
    public static void getLocation(IPermissionDialog permissionDialog,Context context, boolean silent, int timeout, boolean showBeforeRequest,
                                    boolean showAfterRequest, boolean requestGmsDialog, boolean asQuickAsPossible,
                                    boolean useLastKnownLocation, boolean withoutGms, MyLocationCallback callback) {

        if(permissionDialog ==null){
            permissionDialog = MyPermissions.defaultPermissionDialog;
        }

        if (silent) {
            doRequestLocation(context, timeout, withoutGms, callback);
            return;
        }
        //有的手机getSystemService抛异常

        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //如果谷歌服务可用,则直接申请谷歌:
        if (QuietLocationUtil.isGmsAvaiable(context) && requestGmsDialog) {
            checkSwitchByGms(permissionDialog,context, silent, timeout, showBeforeRequest,
                    showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, true);
            return;
        }
        //开关打开,则去申请权限
        if (QuietLocationUtil.isLocationEnabled(locationManager)) {
            checkPermission(permissionDialog,context, timeout, showBeforeRequest, showAfterRequest, withoutGms, callback);
            return;
        }

        askGpsSwitchDialog(permissionDialog,locationManager, context, timeout, showBeforeRequest, showAfterRequest, withoutGms, callback);

    }

    private static void askGpsSwitchDialog(IPermissionDialog permissionDialog,LocationManager locationManager, Context context,
                                           int timeout, boolean showBeforeRequest, boolean showAfterRequest,
                                           boolean withoutGms, MyLocationCallback callback) {
        callback.onGmsSwitchDialogShow();
        //开关关闭,就去申请打开开关
        MyPermissions.defaultAlertDialog.showAlert(
                DefaultPermissionDialog.getString(R.string.location_tip),
                DefaultPermissionDialog.getString(R.string.location_msg_gps),
                DefaultPermissionDialog.getString(R.string.location_ok),
                DefaultPermissionDialog.getString(R.string.location_cancel),
                false, false,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), locationIntent, new ActivityResultListener() {
                            @Override
                            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                                if (!QuietLocationUtil.isLocationEnabled(locationManager)) {
                                    callback.onGmsDialogCancelClicked();
                                    callback.onFailed(2, "location switch off-2", true);
                                    return;
                                }
                                callback.onGmsDialogOkClicked();
                                checkPermission(permissionDialog,context, timeout, showBeforeRequest, showAfterRequest, withoutGms, callback);
                            }

                            @Override
                            public void onActivityNotFound(Throwable e) {

                            }
                        });

                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onGmsDialogCancelClicked();
                        callback.onFailed(2, "location switch off", true);
                    }
                }
        );
    }

    private static void checkSwitchByGms(IPermissionDialog permissionDialog,Context context,
                                         boolean silent, int timeout, boolean showBeforeRequest,
                                         boolean showAfterRequest, boolean asQuickAsPossible,
                                         boolean useLastKnownLocation,
                                         MyLocationCallback callback, boolean isFirstIn) {
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
                    if (haveCallbacked[0]) {
                        return;
                    }
                    haveCallbacked[0] = true;
                    LogUtils.w("gms 判断状态超时,辣鸡gms: LocationServices.SettingsApi.checkLocationSettings");
                    checkIfShowGpsSwitchDialog(permissionDialog,context, silent, timeout, showBeforeRequest,
                            showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, isFirstIn);
                    //getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false,asQuickAsPossible,useLastKnownLocation ,true,callback);
                }
            }, 2000, TimeUnit.MILLISECONDS);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            // 有的手机TMD这里不回调,也不抛异常, 辣鸡GMS
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    if (haveCallbacked[0]) {
                        return;
                    }
                    haveCallbacked[0] = true;
                    if (result == null) {
                        getLocation(permissionDialog,context, silent, timeout, showBeforeRequest,
                                showAfterRequest, false, asQuickAsPossible,
                                useLastKnownLocation, true, callback);
                        return;
                    }
                    LogUtils.i(result.getStatus(), result.getLocationSettingsStates());
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            //Location settings satisfied
                            Log.i("gms", "Location settings satisfied");
                            if (!isFirstIn) {
                                callback.onGmsDialogOkClicked();
                            }
                            checkPermission(permissionDialog,context, timeout, showBeforeRequest,
                                    showAfterRequest, false, callback);
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            //Location settings are not satisfied. Show the user a dialog to upgrade location settings
                            if (isFirstIn) {
                                callback.onGmsSwitchDialogShow();
                                requestGmsSwitch(permissionDialog,context, silent, timeout,
                                        showAfterRequest, showAfterRequest, asQuickAsPossible,
                                        useLastKnownLocation, callback, result);
                            } else {
                                callback.onGmsDialogCancelClicked();
                                Log.w("gms", "不同意gms弹窗,且定位开关开启,那么看是否要弹原生dialog");
                                //miui9上,即使gms点击了同意,再查看状态时还是RESOLUTION_REQUIRED,精确位置开关被miui控制
                                checkIfShowGpsSwitchDialog(permissionDialog,context, silent, timeout, showBeforeRequest,
                                        showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, isFirstIn);
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Location settings are inadequate, and cannot be fixed here. Dialog not created.
                            //todo
                            Log.w("gms", "Error enabling location. Please try again");
                            checkIfShowGpsSwitchDialog(permissionDialog,context, silent, timeout, showBeforeRequest,
                                    showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, isFirstIn);
                            break;
                        default:
                            //todo
                            Log.w("gms", "Error enabling location. Please try again2");
                            checkIfShowGpsSwitchDialog(permissionDialog,context, silent, timeout, showBeforeRequest,
                                    showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, isFirstIn);
                            break;
                    }
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if (haveCallbacked[0]) {
                return;
            }
            haveCallbacked[0] = true;
            checkIfShowGpsSwitchDialog(permissionDialog,context, silent, timeout, showBeforeRequest,
                    showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, isFirstIn);
        }

    }

    private static void checkIfShowGpsSwitchDialog(IPermissionDialog permissionDialog,Context context, boolean silent, int timeout, boolean showBeforeRequest,
                                                   boolean showAfterRequest, boolean asQuickAsPossible, boolean useLastKnownLocation,
                                                   MyLocationCallback callback, boolean isFirstIn) {
        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (LocationStateUtil.isPreciseLocationSwitchEnabled(locationManager)) {
            Log.w("gms", "不同意gms弹窗,但gps可用,那么请求原生定位");
            getLocation(permissionDialog,context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible, useLastKnownLocation, true, callback);
            return;
        }
        if (QuietLocationUtil.isLocationEnabled(locationManager) && callback.configAcceptOnlyCoarseLocationPermission()) {
            Log.w("gms", "不同意gms弹窗,+gps不可用,但定位开关开启+允许模糊定位,那么绕过gms,请求原生定位");
            getLocation(permissionDialog,context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible, useLastKnownLocation, true, callback);
            return;
        }
        Log.w("gms", "不同意gms弹窗,且定位开关关闭,那么再挽回一次,弹出原生定位开关引导弹窗");
        askGpsSwitchDialog(permissionDialog,locationManager, context, timeout, showBeforeRequest, showAfterRequest, false, callback);
    }

    private static void requestGmsSwitch(IPermissionDialog permissionDialog,Context context, boolean silent, int timeout, boolean showBeforeRequest, boolean showAfterRequest,
                                         boolean asQuickAsPossible, boolean useLastKnownLocation, MyLocationCallback callback, LocationSettingsResult result) {
        new GoOutOfAppForResultFragment2((FragmentActivity) ActivityUtils.getTopActivity(), null).goOutApp(new ActivityResultListener() {
            @Override
            public boolean onInterceptStartIntent(@NonNull Fragment fragment, @Nullable Intent intent, int requestCode) {
                ThreadUtils.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //ActivityUtils.getTopActivity()某种情况下为空
                            result.getStatus().startResolutionForResult(ActivityUtils.getTopActivity(), requestCode);
                        } catch (Exception e) {
                            LogUtils.w("location", "PendingIntent unable to execute request.", e);
                            getLocation(permissionDialog,context, silent, timeout, showBeforeRequest, showAfterRequest, false, asQuickAsPossible, useLastKnownLocation, true, callback);
                        }
                    }
                }, 300);
                return true;
            }

            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                //再次检查
                ThreadUtils.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkSwitchByGms(permissionDialog,context, silent, timeout, showBeforeRequest, showAfterRequest, asQuickAsPossible, useLastKnownLocation, callback, false);
                    }
                }, 200);

            }

            @Override
            public void onActivityNotFound(Throwable e) {
                //todo
            }
        });
    }

    private static boolean isGranted(final String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(Utils.getApp(), permission);
    }

    private static void checkPermission(IPermissionDialog permissionDialog,Context context, int timeout, boolean showBeforeRequest, boolean showAfterRequest, boolean withoutGms, MyLocationCallback callback) {
        if (PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            doRequestLocation(context, timeout, withoutGms, callback);
        } else {
            MyPermissions.setCanAcceptOnlyCoarseLocationPermission(callback.configAcceptOnlyCoarseLocationPermission());
            if (PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                    && !PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (callback.configAcceptOnlyCoarseLocationPermission()) {
                    if (!showBeforeRequest && !showAfterRequest) {
                        //能接受模糊权限,且只有模糊权限时,如果配置不要权限前置和后置弹窗,那么直接去定位,不发起权限请求.
                        //为项目特殊要求,因项目中用其他权限框架预先请求了定位权限,此处再弹会导致多次权限弹窗
                        //真实使用中,其实应该再请求一下
                        doRequestLocation(context, timeout, withoutGms, callback);
                        return;
                    }
                }
            }
            //
            //只有ACCESS_COARSE_LOCATION时,  模糊定位,accuracy=2000米, 中心点随机偏差几千米
            //只有ACCESS_FINE_LOCATION时,精确定位 android12也会跳出模糊定位的选项.

            MyPermissions.create()
                    //.setAfterPermissionMsg("after msg")
                    //.setGuideToSettingMsg("guide to settings")
                    .setDialog(permissionDialog)
                    .setPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION})
                    .setShowAfterRequest(true)
                    .setShowBeforeRequest(false)
                    .callback(new PermissionUtils.FullCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> granted) {
                            if(isGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
                                LogUtils.w("fine location granted");
                            }else {
                                LogUtils.w("fine location reject");
                            }
                            if (!callback.configAcceptOnlyCoarseLocationPermission()) {
                                if(!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
                                    callback.onFailed(1, "no permission", true);
                                    return;
                                }
                            }
                            doRequestLocation(context, timeout, withoutGms, callback);
                        }

                        @Override
                        public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                            //ToastUtils.showShort("onDenied:" + Arrays.toString(deniedForever.toArray()) + "\n" + Arrays.toString(denied.toArray()));
                            if (callback.configAcceptOnlyCoarseLocationPermission()) {
                                List<String> list = new ArrayList<>();
                                list.addAll(deniedForever);
                                list.addAll(denied);
                                if (!list.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                    //只拒绝了fine location权限,没有拒绝模糊定位权限-android12
                                    doRequestLocation(context, timeout, withoutGms, callback);
                                } else {
                                    callback.onFailed(1, "no permission", true);
                                }
                            } else {
                                callback.onFailed(1, "no permission", true);
                            }
                        }
                    });
        }
    }

    private static void doRequestLocation(Context context, int timeout, boolean withoutGms, MyLocationCallback callback) {

        if (!callback.configJustAskPermissionAndSwitch()) {
            callback.onBeforeReallyRequest();
            new QuietLocationUtil().getLocation(context, timeout, withoutGms, callback);
        } else {
            callback.onSuccess(null, "permission granted");
        }
    }
}
