package com.hss01248.location;

import android.app.ProgressDialog;
import android.location.Location;
import android.location.LocationManager;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;

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
        onReport(location, "onEachLocationChanged", true);
        handleNewLocation(location, "from " + provider);
    }

    private synchronized void handleNewLocation(Location location, String msg) {
        if (hasCallbacked) {
            return;
        }

        if (LocationSync.isFakeLocation(location) && !LocationSync.acceptFakeLocation) {
            return;
        }
        if(location !=null){
            if(LocationManager.FUSED_PROVIDER.equals(location.getProvider())
            || LocationManager.GPS_PROVIDER.equals(location.getProvider())){
                LogUtils.w("已经是FUSED_PROVIDER或GPS_PROVIDER,立刻返回",location);
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
            onSuccessAccurate(location, msg);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
            onFinalFail(1, "error occur in success:" + throwable.getMessage(), false);
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
                LocationUtil.getLocationMetric().reportFastCallback(success, location, success ? "" : msg, success ? msg : "", System.currentTimeMillis() - start);
            }
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    void dismissDialog() {
        if (dialog == null) {
            return;
        }
        ThreadUtils.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        },1000);
    }

    @Override
    public long useCacheInTimeOfMills() {
        return 90 * 1000; // 90s内缓存有效，同FastCallback
    }

    public abstract void onSuccessAccurate(Location location, String msg);

    public abstract void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest);
}
