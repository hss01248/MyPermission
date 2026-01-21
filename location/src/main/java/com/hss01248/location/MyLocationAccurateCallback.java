package com.hss01248.location;

import android.app.ProgressDialog;
import android.location.Location;
import android.location.LocationManager;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;

/**
 * 定位模式：设定一个最短定位时间,比如至少要定位3秒(可配置),
 * 过了这个时间,看目前拿到的最准确的定位是什么,有拿到,就返回最准确的那个定位对象,
 * 没有,则继续定位直到成功返回或超时
 */
public abstract class MyLocationAccurateCallback implements MyLocationCallback {

    private volatile boolean hasCallbacked = false;
    private long start;
    private long minTimeMills = 3000; // 默认3秒
    private Location bestLocation;
    private String bestMsg;
    private ProgressDialog dialog;
    private volatile boolean isMinTimeReached = false;

    public MyLocationAccurateCallback() {
    }

    public MyLocationAccurateCallback(long minTimeMills) {
        this.minTimeMills = minTimeMills;
    }

    @Override
    public void onBeforeReallyRequest() {
        MyLocationCallback.super.onBeforeReallyRequest();
        start = System.currentTimeMillis();
        if (configShowLoadingDialog()) {
            ThreadUtils.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    dialog = new ProgressDialog(ActivityUtils.getTopActivity());
                    dialog.show();
                }
            });
        }

        // 启动定时器
        ThreadUtils.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onMinTimeReached();
            }
        }, minTimeMills);
    }

    private synchronized void onMinTimeReached() {
        if (hasCallbacked) {
            return;
        }
        isMinTimeReached = true;
        LogUtils.i("location", "Min time reached: " + minTimeMills + "ms");
        if (bestLocation != null) {
            doCallbackSuccess(bestLocation, bestMsg + " (best in " + minTimeMills + "ms)");
        } else {
            LogUtils.i("location", "Min time reached but no location yet, continue waiting...");
        }
    }

    @Override
    public void onSuccess(Location location, String msg) {
        handleNewLocation(location, msg);
    }

    @Override
    public void onEachLocationChanged(Location location, String provider) {
        MyLocationCallback.super.onEachLocationChanged(location, provider);
        handleNewLocation(location, "from " + provider);
    }
    public static long lastShowToastTime;
    private synchronized void handleNewLocation(Location location, String msg) {
        if (hasCallbacked) {
            return;
        }

        // fake location的处理
        if(LocationSync.isFakeLocation(location) ){
            LogUtils.w("from real_time sys api, but fake location,will return fail in release app",location);
            if(LocationSync.acceptFakeLocation){
                if(AppUtils.isAppDebug()){
                    if(System.currentTimeMillis() - lastShowToastTime > 30000){
                        lastShowToastTime = System.currentTimeMillis();
                        ToastUtils.showLong("from real_time sys api, but fake location,will return fail in release app");
                    }
                }
            }else {
                onFailed(LocationErrorCode.FAKE_LOCATION, LocationErrorCode.getErrorMsg(LocationErrorCode.FAKE_LOCATION),false);
                return;
            }
        }

        if(location !=null){
            if("fused".equals(location.getProvider())
            || "gps".equals(location.getProvider())){
                LogUtils.i("已经是FUSED_PROVIDER或GPS_PROVIDER,立刻返回",location);
                doCallbackSuccess(location, msg);
                return;
            }
        }

        // 更新最准确的定位
        if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
            bestLocation = location;
            bestMsg = msg;
            LogUtils.i("location", "Update best location accuracy: " + location.getAccuracy());
        }

        // 如果已经过了最小时间，立即返回
        if (isMinTimeReached) {
            doCallbackSuccess(bestLocation, bestMsg);
        }
    }

    private void doCallbackSuccess(Location location, String msg) {
        if (hasCallbacked) {
            return;
        }
        hasCallbacked = true;
        dismissDialog();
        try {
            onReport(location, msg, true);
            onSuccessAccurate(location, msg);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
            hasCallbacked = false;
            onFailed(LocationErrorCode.ERROR_IN_SUCCESS_CALLBACK, "error occur in success:" + throwable.getMessage(), false);
        }
    }

    @Override
    public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
        synchronized (this) {
            if (hasCallbacked) {
                return;
            }
            // 如果还没到最小时间，且有最佳位置（虽然当前这个回调失败了，但之前可能有过onEachLocationChanged），
            // 理论上由handleNewLocation处理。如果这里是最终失败（比如超时）
            hasCallbacked = true;
        }
        
        onReport(null, msg, false);
        dismissDialog();
        try {
            onFinalFail(type, msg, isFailBeforeReallyRequest);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    private void onReport(Location location, String msg, boolean success) {
        try {
            if (LocationUtil.getLocationMetric() != null) {
                LocationUtil.getLocationMetric().reportFastAccuracyCallback(success, location, success ? "" : msg, success ? msg : "", System.currentTimeMillis() - start);
            }
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    void dismissDialog() {
        ThreadUtils.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        },300);
    }

    @Override
    public long useCacheInTimeOfMills() {
        return 90 * 1000; // 90s内缓存有效，同FastCallback
    }

    public abstract void onSuccessAccurate(Location location, String msg);

    public abstract void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest);
}
