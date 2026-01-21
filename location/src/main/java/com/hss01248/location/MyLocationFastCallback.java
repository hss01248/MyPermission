package com.hss01248.location;

import android.app.ProgressDialog;
import android.location.Location;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hss01248.permission.MyPermissions;

import java.util.HashMap;
import java.util.Map;

/**
 * @Despciption todo
 * @Author hss
 * @Date 15/08/2022 16:10
 * @Version 1.0
 */
public abstract class MyLocationFastCallback implements MyLocationCallback{

    private  volatile boolean hasCallbacked = false;
    private long start;

    ProgressDialog dialog;



    @Override
    public void onBeforeReallyRequest() {
        MyLocationCallback.super.onBeforeReallyRequest();
        start = System.currentTimeMillis();
        if(configShowLoadingDialog()){
            ThreadUtils.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    dialog = new ProgressDialog(ActivityUtils.getTopActivity());
                    dialog.show();
                }
            });
        }
    }

    @Override
    public boolean configAcceptOnlyCoarseLocationPermission() {
        return false;
    }

    void dismissDialog(){
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
    public void onSuccess(Location location, String msg) {
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onSuccess",msg);
            return;
        }
        hasCallbacked = true;
        onReport(location,msg,true);
        dismissDialog();
        onSuccessFastWrapper(location,msg);
    }

    private void onReport(Location location, String msg, boolean success) {
        //上报统计数据
        try {
            if(LocationUtil.getLocationMetric() != null){
                LocationUtil.getLocationMetric().reportFastCallback(success,location,success? "":msg,success?msg:"",System.currentTimeMillis() - start);
            }
        }catch (Throwable throwable){
            LogUtils.w(throwable);
        }
    }

  public static long lastShowToastTime;
    @Override
    public void onEachLocationChanged(Location location, String provider) {
        MyLocationCallback.super.onEachLocationChanged(location, provider);
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onEachLocationChanged",provider);
            return;
        }
        hasCallbacked = true;
        onReport(location,provider,true);
        dismissDialog();
        if(LocationSync.isFakeLocation(location) ){
            if(LocationSync.acceptFakeLocation){
                LogUtils.w("from real_time sys api, but fake location,will return fail in release app",location);
                if(AppUtils.isAppDebug()){
                    if(System.currentTimeMillis() - lastShowToastTime > 30000){
                        lastShowToastTime = System.currentTimeMillis();
                        ToastUtils.showLong("from real_time sys api, but fake location,will return fail in release app");
                    }
                }
                onSuccessFastWrapper(location,"from real_time sys api, but fake location");
            }else {
                onFinalFail(LocationErrorCode.FAKE_LOCATION, LocationErrorCode.getErrorMsg(LocationErrorCode.FAKE_LOCATION),false);
            }
        }else {
            onSuccessFastWrapper(location,"from real_time sys api");
        }
    }
    protected   void onSuccessFastWrapper(Location location,String msg){
        try{
            onSuccessFast(location,msg);
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            hasCallbacked = false;
            onFailed(LocationErrorCode.ERROR_IN_SUCCESS_CALLBACK,"error occur in success:"+throwable.getMessage(),false);
        }
    }

    public abstract void onSuccessFast(Location location,String msg);

    public abstract void onFinalFail(int type,String msg, boolean isFailBeforeReallyRequest);

    /**
     * 2min内缓存有效
     * @return
     */
    @Override
    public long useCacheInTimeOfMills() {
        return 90*1000;
    }

    @Override
    public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
        if(hasCallbacked ){
            LogUtils.w("location","已经回调过-onFailed",msg);
            return;
        }
        hasCallbacked = true;
        onReport(null,msg,false);
        dismissDialog();
        try{
            onFinalFail(type, msg,isFailBeforeReallyRequest);
        }catch (Throwable throwable){
            LogUtils.w(throwable);
        }

        /*LocationInfo fullLocationInfo = LocationSync.getFullLocationInfo();
        if(fullLocationInfo == null){
            onFinalFail(type, msg,isFailBeforeReallyRequest);
            return;
        }
        if(System.currentTimeMillis() - fullLocationInfo.timeStamp > useCacheInTimeOfMills()){
            LogUtils.w("失败,取缓存,有缓存,但超过了失效:"+ useCacheInTimeOfMills(),fullLocationInfo);
            onFinalFail(type, msg,isFailBeforeReallyRequest);
            return;
        }
        onSuccessFast(LocationSync.toAndroidLocation(fullLocationInfo), "from new cache and "+msg);*/
    }
}
