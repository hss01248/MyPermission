package com.hss01248.location;

import android.location.Location;

public interface MyLocationCallback {
    void onFailed(int type,String msg);

    void onSuccess(Location location,String msg);

    default void onGmsSwitchDialogShow(){}

    default void onGmsDialogOkClicked(){}

    default void onGmsDialogCancelClicked(){}


}
