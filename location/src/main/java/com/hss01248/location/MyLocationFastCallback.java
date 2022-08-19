package com.hss01248.location;

import android.location.Location;

import com.blankj.utilcode.util.LogUtils;

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

    @Override
    public void onBeforeReallyRequest() {
        MyLocationCallback.super.onBeforeReallyRequest();
        start = System.currentTimeMillis();
    }

    @Override
    public void onSuccess(Location location, String msg) {
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onSuccess",msg);
            return;
        }
        hasCallbacked = true;
        onReport(location,msg,true);
        onSuccessFast(location,msg);
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

    @Override
    public void onEachLocationChanged(Location location, String provider) {
        MyLocationCallback.super.onEachLocationChanged(location, provider);
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onEachLocationChanged",provider);
            return;
        }
        hasCallbacked = true;
        onReport(location,provider,true);
        onSuccessFast(location,"from real_time sys api");

    }
    public abstract void onSuccessFast(Location location,String msg);

    public abstract void onFinalFail(int type,String msg, boolean isFailBeforeReallyRequest);

    /**
     * 2min内缓存有效
     * @return
     */
    @Override
    public long useCacheInTimeOfMills() {
        return 2*60*1000;
    }

    @Override
    public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onFailed",msg);
            return;
        }
        hasCallbacked = true;
        onReport(null,msg,false);
        onFinalFail(type, msg,isFailBeforeReallyRequest);
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
