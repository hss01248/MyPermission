package com.hss01248.location;

import android.location.Location;

public interface MyLocationCallback {
    /**
     * type随便写的,主要看msg
     * @param type
     * @param msg
     */
    default void onFailed(int type,String msg){
        onFailed(type, msg,false);
    }

    void onSuccess(Location location,String msg);



   default void onEachLocationChanged(Location location,String provider){}

    /**
     * 定位时,每个provider的onEachLocationChanged的回调,因为多个provider,所以这个方法会回调多次
     * @param provider
     */
    default void onEachLocationChanged(Location location,String provider,long costOfJustThisUpdate,long costFromUtilStart){
       onEachLocationChanged(location, provider);
    }

    /**
     * 定位时,每个provider开始前的回调
     * @param provider
     */
    default void onEachLocationStart(String provider){}

    /**
     * 用于埋点
     */
    default void onGmsSwitchDialogShow(){}
    /**
     * 用于埋点
     */
    default void onGmsDialogOkClicked(){}
    /**
     * 用于埋点
     */
    default void onGmsDialogCancelClicked(){}

    /**
     * 开关,权限都ok后,真正请求定位前的回调
     */
    default void onBeforeReallyRequest(){}



    /**
     * 缓存有效时间:
     * 默认一年,约等于永久 , 每次都可以单独配置
     * 逻辑:  实时定位失败/超时后,从缓存读取,如果System.currentTimeMills-缓存定位里的time < useCacheInTimeOfMills,
     * 则使用该缓存,否则不使用该缓存
     *
     * @return
     */
    default long useCacheInTimeOfMills(){
       return 365*24*60*60*1000L;
    }


    /**
     * 老api,不再起作用,仅用于api兼容
     * @return
     */
    @Deprecated
    default boolean configUseSystemLastKnownLocation(){
        return true;
    }
    /**
     * 老api,不再起作用,仅用于api兼容
     * @return
     */
    @Deprecated
    default boolean configUseSpCache(){
        return true;
    }

    /**
     * 暂未实现逆geo解析
     * @return
     */
    default boolean configNeedParseAdress(){
        return false;
    }
    @Deprecated
    default boolean configNoNetworkProvider(){
        return false;
    }

    /**
     * 是否支持仅模糊定位. 经纬度会偏差几千米
     * @return
     */
    default boolean configAcceptOnlyCoarseLocationPermission(){
        return false;
    }

    /**
     * 从开关和权限完成后,真正开始定位前,弹loading dialog,定位回调成功或失败时关闭.
     * @return
     */
    default boolean configShowLoadingDialog(){
        return false;
    }


    /**
     * 只判断/请求定位权限和定位开关,不实际发起定位请求
     * @return
     */
    default boolean configJustAskPermissionAndSwitch(){
        return false;
    }

     void onFailed(int type,String msg,boolean isFailBeforeReallyRequest);




}
