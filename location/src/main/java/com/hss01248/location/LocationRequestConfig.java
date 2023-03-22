package com.hss01248.location;

import android.content.Context;

import com.hss01248.permission.IPermissionDialog;

/**
 * @Despciption todo
 * @Author hss
 * @Date 02/04/2022 17:31
 * @Version 1.0
 */
public class LocationRequestConfig {

   public Context context;
   boolean silent;
   int timeoutMills;
   boolean showDialogBeforeRequestPermission;
    boolean showDialogAfterPermissionDenied;
    boolean requestGmsDialogIfGmsAvaiable = true;
    boolean asQuickAsPossible;
    boolean useLastKnownLocationOrCache;

    String gpsText;
    String afterPermissionText;
    String goSettingText;
    IPermissionDialog permissionDialog;

    boolean showLoadingDialog;
    boolean justAskPermissionAndSwitch;
    boolean acceptOnlyCoarseLocationPermission = true;

    /**
     * 缓存有效时间:
     * 默认一年,约等于永久 , 每次都可以单独配置
     * 逻辑:  实时定位失败/超时后,从缓存读取,如果System.currentTimeMills-缓存定位里的time < useCacheInTimeOfMills,
     * 则使用该缓存,否则不使用该缓存
     */
    long useCacheInTimeOfMills = 365*24*60*60*1000L;


    public LocationRequestConfig useCacheInTimeOfMills(long useCacheInTimeOfMills) {
        this.useCacheInTimeOfMills = useCacheInTimeOfMills;
        return this;
    }
    public LocationRequestConfig showLoadingDialog(boolean showLoadingDialog) {
        this.showLoadingDialog = showLoadingDialog;
        return this;
    }
    public LocationRequestConfig justAskPermissionAndSwitch(boolean justAskPermissionAndSwitch) {
        this.justAskPermissionAndSwitch = justAskPermissionAndSwitch;
        return this;
    }
    /**
     * 是否支持仅模糊定位. 经纬度会偏差几千米
     * @return
     */
    public LocationRequestConfig acceptOnlyCoarseLocationPermission(boolean acceptOnlyCoarseLocationPermission) {
        this.acceptOnlyCoarseLocationPermission = acceptOnlyCoarseLocationPermission;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public LocationRequestConfig setGpsText(String gpsText) {
        this.gpsText = gpsText;
        return this;
    }
    public LocationRequestConfig setAfterPermissionText(String afterPermissionText) {
        this.afterPermissionText = afterPermissionText;
        return this;
    }
    public LocationRequestConfig setGoSettingText(String goSettingText) {
        this.goSettingText = goSettingText;
        return this;
    }
    public LocationRequestConfig setPermissionDialog(IPermissionDialog permissionDialog) {
        this.permissionDialog = permissionDialog;
        return this;
    }

    public LocationRequestConfig setContext(Context context) {
        this.context = context;
        return this;
    }

    public boolean isSilent() {
        return silent;
    }

    public LocationRequestConfig setSilent(boolean silent) {
        this.silent = silent;
        return this;
    }

    public int getTimeoutMills() {
        return timeoutMills;
    }

    public LocationRequestConfig setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
        return this;
    }

    public boolean isShowDialogBeforeRequestPermission() {
        return showDialogBeforeRequestPermission;
    }

    public LocationRequestConfig setShowDialogBeforeRequestPermission(boolean showDialogBeforeRequestPermission) {
        this.showDialogBeforeRequestPermission = showDialogBeforeRequestPermission;
        return this;
    }

    public boolean isShowDialogAfterPermissionDenied() {
        return showDialogAfterPermissionDenied;
    }

    public LocationRequestConfig setShowDialogAfterPermissionDenied(boolean showDialogAfterPermissionDenied) {
        this.showDialogAfterPermissionDenied = showDialogAfterPermissionDenied;
        return this;
    }

    public boolean isRequestGmsDialogIfGmsAvaiable() {
        return requestGmsDialogIfGmsAvaiable;
    }

    public LocationRequestConfig setRequestGmsDialogIfGmsAvaiable(boolean requestGmsDialogIfGmsAvaiable) {
        this.requestGmsDialogIfGmsAvaiable = requestGmsDialogIfGmsAvaiable;
        return this;
    }

    public boolean isAsQuickAsPossible() {
        return asQuickAsPossible;
    }

    public LocationRequestConfig setAsQuickAsPossible(boolean asQuickAsPossible) {
        this.asQuickAsPossible = asQuickAsPossible;
        return this;
    }

    public boolean isUseLastKnownLocationOrCache() {
        return useLastKnownLocationOrCache;
    }

    public LocationRequestConfig setUseLastKnownLocationOrCache(boolean useLastKnownLocationOrCache) {
        this.useLastKnownLocationOrCache = useLastKnownLocationOrCache;
        return this;
    }

    public MyLocationCallback getCallback() {
        return callback;
    }

    public LocationRequestConfig setCallback(MyLocationCallback callback) {
        this.callback = callback;
        return this;
    }

    MyLocationCallback callback;
}
