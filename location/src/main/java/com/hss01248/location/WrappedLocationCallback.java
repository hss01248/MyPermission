package com.hss01248.location;

import android.location.Location;

import com.blankj.utilcode.util.LogUtils;

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
    public void onBeforeReallyRequest() {
       callback.onBeforeReallyRequest();
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
        LocationInfo fullLocationInfo = LocationSync.getFullLocationInfo();
        if(fullLocationInfo == null){
            callback.onFailed(type, msg,isFailBeforeReallyRequest);
            return;
        }
        if(System.currentTimeMillis() - fullLocationInfo.timeStamp > useCacheInTimeOfMills()){
            LogUtils.w("失败,取缓存,有缓存,但超过了失效:"+ useCacheInTimeOfMills(),fullLocationInfo);
            callback.onFailed(type, msg,isFailBeforeReallyRequest);
            return;
        }
        callback.onSuccess(LocationSync.toAndroidLocation(fullLocationInfo), "from new cache and "+msg);
    }
}
