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
    @Override
    public void onSuccess(Location location, String msg) {
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onSuccess",msg);
            return;
        }
        hasCallbacked = true;
        onSuccessFast(location,msg);
    }

    @Override
    public void onEachLocationChanged(Location location, String provider) {
        MyLocationCallback.super.onEachLocationChanged(location, provider);
        if(hasCallbacked){
            LogUtils.w("location","已经回调过-onEachLocationChanged",provider);
            return;
        }
        hasCallbacked = true;
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
