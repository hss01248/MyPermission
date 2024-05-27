package com.hss01248.location.sim;


import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.wifi.WifiAccessPoint;
import com.hss01248.location.wifi.WifiCommonCallback;
import com.hss01248.location.wifi.WifiInfoForList;
import com.hss01248.location.wifi.WifiListUtil;
import com.hss01248.location.wifi.WifiToLocationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/27/24 4:52 PM
 * @Version 1.0
 */
public class WifiAndBaseStationUtil {

    public static void requestLocationSilent(MyLocationCallback callback){
        CellTowerUtil.loadInfo(new WifiCommonCallback<GeoParam>() {
            @Override
            public void onSuccess(GeoParam param0) {
                requestWifi(param0,false,callback);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                requestWifi(null,false,callback);
            }
        });
    }

    public static void requestLocation(MyLocationCallback callback) {

        CellTowerUtil.getCellTowerInfo(new WifiCommonCallback<GeoParam>() {
            @Override
            public void onSuccess(GeoParam param0) {
                requestWifi(param0,true,callback);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                requestWifi(null,true,callback);
            }
        });
    }

    private static void requestWifi(GeoParam param0,boolean requestPermission, MyLocationCallback callback) {


        WifiListUtil.getList(Utils.getApp(), false, requestPermission,new WifiCommonCallback<List<WifiInfoForList>>() {
            @Override
            public void onSuccess(List<WifiInfoForList> wifiInfoForLists0) {
                List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();
                for (WifiInfoForList info : wifiInfoForLists0) {
                    WifiAccessPoint point = new WifiAccessPoint();
                    point.setMacAddress(info.wifi_mac);
                    point.setSignalStrength(info.signal_strength);
                    point.setSignalToNoiseRatio(info.signalToNoiseRatio);
                    wifiAccessPoints.add(point);
                }
                GeoParam param = param0;
                if(param ==null){
                    param = new GeoParam();
                }
                param.wifiAccessPoints = wifiAccessPoints;
                reqeustApi(param,callback);
            }



            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                if(param0 == null){
                    callback.onFailed(6,"wifi and cell tower both not avaiable");
                }else {
                    reqeustApi(param0,callback);
                }
            }
        });
    }

    private static void reqeustApi(GeoParam param, MyLocationCallback callback) {
        WifiToLocationUtil.requestApi(param, callback);
    }
}
