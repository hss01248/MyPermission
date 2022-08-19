package com.hss01248.location;

import android.location.Location;

public interface ILocationMetric {

    /**
     * 上报快速定位模式回调的耗时信息
     * @param success
     * @param location
     * @param failReason
     * @param successMsg
     * @param cost
     */
    void reportFastCallback(boolean success, Location location,String failReason,String successMsg,long cost);

    /**
     * 上报每个单独的provider的实际定位耗时
     * @param location
     * @param calledProvider
     * @param readProvider
     * @param costOfJustThisUpdate
     * @param costFromUtilStart
     */
    void reportEachLocationChanged( Location location,String calledProvider,String readProvider,long costOfJustThisUpdate, long costFromUtilStart);
}
