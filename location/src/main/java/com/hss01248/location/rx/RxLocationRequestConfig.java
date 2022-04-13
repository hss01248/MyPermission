package com.hss01248.location.rx;

import android.content.Context;

import com.hss01248.location.MyLocationCallback;

/**
 * @Despciption todo
 * @Author hss
 * @Date 02/04/2022 17:31
 * @Version 1.0
 */
public class RxLocationRequestConfig {

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

    public RxLocationRequestConfig setContext(Context context) {
        this.context = context;
        return this;
    }

    public boolean isSilent() {
        return silent;
    }

    public RxLocationRequestConfig setSilent(boolean silent) {
        this.silent = silent;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public RxLocationRequestConfig setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public boolean isShowDialogBeforeRequestPermission() {
        return showDialogBeforeRequestPermission;
    }

    public RxLocationRequestConfig setShowDialogBeforeRequestPermission(boolean showDialogBeforeRequestPermission) {
        this.showDialogBeforeRequestPermission = showDialogBeforeRequestPermission;
        return this;
    }

    public boolean isShowDialogAfterPermissionDenied() {
        return showDialogAfterPermissionDenied;
    }

    public RxLocationRequestConfig setShowDialogAfterPermissionDenied(boolean showDialogAfterPermissionDenied) {
        this.showDialogAfterPermissionDenied = showDialogAfterPermissionDenied;
        return this;
    }

    public boolean isRequestGmsDialogIfGmsAvaiable() {
        return requestGmsDialogIfGmsAvaiable;
    }

    public RxLocationRequestConfig setRequestGmsDialogIfGmsAvaiable(boolean requestGmsDialogIfGmsAvaiable) {
        this.requestGmsDialogIfGmsAvaiable = requestGmsDialogIfGmsAvaiable;
        return this;
    }

    public boolean isAsQuickAsPossible() {
        return asQuickAsPossible;
    }

    public RxLocationRequestConfig setAsQuickAsPossible(boolean asQuickAsPossible) {
        this.asQuickAsPossible = asQuickAsPossible;
        return this;
    }

    public boolean isUseLastKnownLocationOrCache() {
        return useLastKnownLocationOrCache;
    }

    public RxLocationRequestConfig setUseLastKnownLocationOrCache(boolean useLastKnownLocationOrCache) {
        this.useLastKnownLocationOrCache = useLastKnownLocationOrCache;
        return this;
    }

    public MyLocationCallback getCallback() {
        return callback;
    }

    public RxLocationRequestConfig setCallback(MyLocationCallback callback) {
        this.callback = callback;
        return this;
    }

    MyLocationCallback callback;
}
