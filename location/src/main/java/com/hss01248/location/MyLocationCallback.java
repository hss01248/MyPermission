package com.hss01248.location;

import android.location.Location;

public interface MyLocationCallback {
    default void onFailed(int type,String msg){
        onFailed(type, msg,false);
    }

    void onSuccess(Location location,String msg);



   default void onEachLocationChanged(Location location,String provider){}

    default void onEachLocationChanged(Location location,String provider,long costOfJustThisUpdate,long costFromUtilStart){
       onEachLocationChanged(location, provider);
    }

    default void onEachLocationStart(String provider){}

    default void onGmsSwitchDialogShow(){}

    default void onGmsDialogOkClicked(){}

    default void onGmsDialogCancelClicked(){}

    default void onBeforeReallyRequest(){}



    /**
     * 默认一年,约等于永久:
     * @return
     */
    default long useCacheInTimeOfMills(){
       return 365*24*60*60*1000L;
    }


    @Deprecated
    default boolean configUseSystemLastKnownLocation(){
        return true;
    }
    @Deprecated
    default boolean configUseSpCache(){
        return true;
    }

    default boolean configNeedParseAdress(){
        return false;
    }

    default boolean configNoNetworkProvider(){
        return false;
    }

    default boolean configAcceptOnlyCoarseLocationPermission(){
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
