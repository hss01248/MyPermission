package com.hss01248.location;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.location.Location;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;

/**
 * @Despciption todo
 * @Author hss
 * @Date 19/08/2022 14:36
 * @Version 1.0
 */
public class WrappedLocationCallback implements MyLocationCallback{

    public WrappedLocationCallback(MyLocationCallback callback) {
        this.callback = callback;
    }

    MyLocationCallback callback;
    @Override
    public void onFailed(int type, String msg) {
        MyLocationCallback.super.onFailed(type, msg);
    }

    @Override
    public void onSuccess(Location location, String msg) {
        dismissDialog();
        callback.onSuccess(location, msg);
    }

    @Override
    public void onEachLocationChanged(Location location, String provider) {
       callback.onEachLocationChanged(location, provider);
    }

    @Override
    public void onEachLocationChanged(Location location, String provider, long costOfJustThisUpdate, long costFromUtilStart) {
        MyLocationCallback.super.onEachLocationChanged(location, provider, costOfJustThisUpdate, costFromUtilStart);
        //上报统计数据
        try {
            if(LocationUtil.getLocationMetric() != null){
                LocationUtil.getLocationMetric().reportEachLocationChanged(location,provider,location.getProvider(),costOfJustThisUpdate,costFromUtilStart);
            }
        }catch (Throwable throwable){
           LogUtils.w(throwable);
        }

    }

    @Override
    public boolean configAcceptOnlyCoarseLocationPermission() {
        return callback.configAcceptOnlyCoarseLocationPermission() ;
    }

    @Override
    public boolean configShowLoadingDialog() {
        return callback.configShowLoadingDialog();
    }

    @Override
    public void onEachLocationStart(String provider) {
       callback.onEachLocationStart(provider);
    }

    @Override
    public void onGmsSwitchDialogShow() {
       callback.onGmsSwitchDialogShow();
    }

    @Override
    public void onGmsDialogOkClicked() {
       callback.onGmsDialogOkClicked();
    }

    @Override
    public void onGmsDialogCancelClicked() {
       callback.onGmsDialogCancelClicked();
    }

    @Override
    public long configTimeoutWhenOnlyGpsProvider() {
        return callback.configTimeoutWhenOnlyGpsProvider();
    }

    @Override
    public boolean configForceUseOnlyGpsProvider() {
        return callback.configForceUseOnlyGpsProvider();
    }

    ProgressDialog dialog;
    @Override
    public void onBeforeReallyRequest() {
       callback.onBeforeReallyRequest();
       if(configShowLoadingDialog()){
           if(callback instanceof MyLocationFastCallback){
               return;
           }
           ThreadUtils.getMainHandler().post(new Runnable() {
               @Override
               public void run() {
                   dialog = new ProgressDialog(ActivityUtils.getTopActivity());
                   dialog.show();
               }
           });
       }
    }

    void dismissDialog(){
        if(dialog == null){
            return;
        }
        ThreadUtils.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if(dialog != null){
                    dialog.dismiss();
                }
            }
        });
    }

    @Override
    public long useCacheInTimeOfMills() {
        return callback.useCacheInTimeOfMills();
    }

    @Override
    public boolean configUseSystemLastKnownLocation() {
        return callback.configUseSystemLastKnownLocation();
    }

    @Override
    public boolean configUseSpCache() {
        return callback.configUseSpCache();
    }

    @Override
    public boolean configNeedParseAdress() {
        return callback.configNeedParseAdress();
    }

    @Override
    public boolean configNoNetworkProvider() {
        return callback.configNoNetworkProvider();
    }

    @Override
    public boolean configJustAskPermissionAndSwitch() {
        return callback.configJustAskPermissionAndSwitch();
    }

    @Override
    public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
        if(configJustAskPermissionAndSwitch()){
            callback.onFailed(type, msg, isFailBeforeReallyRequest);
            return;
        }
        LocationInfo fullLocationInfo = LocationSync.getFullLocationInfo();
        if(fullLocationInfo == null){
            dismissDialog();
            callback.onFailed(type, msg,isFailBeforeReallyRequest);
            return;
        }
        if(System.currentTimeMillis() - fullLocationInfo.timeStamp > useCacheInTimeOfMills()){
            LogUtils.w("失败,取缓存,有缓存,但超过了失效:"+ useCacheInTimeOfMills(),fullLocationInfo);
            dismissDialog();
            callback.onFailed(type, msg,isFailBeforeReallyRequest);
            return;
        }
        dismissDialog();
        callback.onSuccess(LocationSync.toAndroidLocation(fullLocationInfo), "from new cache and "+msg);
    }
}
