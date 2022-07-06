package com.hss01248.location;

import android.location.Location;

public interface MyLocationCallback {
    default void onFailed(int type,String msg){
        onFailed(type, msg,false);
    }

    void onSuccess(Location location,String msg);

   default void onEachLocationChanged(Location location,String provider){}

    default void onGmsSwitchDialogShow(){}

    default void onGmsDialogOkClicked(){}

    default void onGmsDialogCancelClicked(){}

    default void onBeforeReallyRequest(){}

    default boolean configUseSpCache(){
       return true;
    }

     void onFailed(int type,String msg,boolean isFailBeforeReallyRequest);


}
