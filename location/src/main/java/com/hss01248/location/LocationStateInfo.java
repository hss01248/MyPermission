package com.hss01248.location;

import androidx.annotation.Keep;

/**
 * @Despciption todo
 * @Author hss
 * @Date 23/11/2022 10:21
 * @Version 1.0
 */
@Keep
public class LocationStateInfo {

    boolean preciseLocationSwitchEnabled;
    boolean locationSwitchOpen;

    boolean coarseLocationPermissionGranted;
    boolean fineLocationPermissionGranted;

    boolean gmsAvaliabled;
    boolean gmsLocationSwitchOpened;
}
