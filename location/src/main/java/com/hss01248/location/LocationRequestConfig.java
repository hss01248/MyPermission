package com.hss01248.location;

import android.content.Context;

/**
 * @Despciption todo
 * @Author hss
 * @Date 02/04/2022 17:31
 * @Version 1.0
 */
public class LocationRequestConfig {

   public Context context;
   boolean silent;
   int timeout;
   boolean showDialogBeforeRequestPermission;
    boolean showDialogAfterPermissionDenied;
    boolean requestGmsDialogIfGmsAvaiable = true;
    boolean asQuickAsPossible;
    boolean useLastKnownLocationOrCache;

    public Context getContext() {
        return context;
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

    public int getTimeout() {
        return timeout;
    }

    public LocationRequestConfig setTimeout(int timeout) {
        this.timeout = timeout;
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
