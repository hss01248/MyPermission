package com.hss01248.location.wifi;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.QuietLocationUtil;
import com.hss01248.location.sim.GeoParam;
import com.hss01248.location.sim.WifiAndBaseStationUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/15/24 3:41 PM
 * @Version 1.0
 */
public class WifiToLocationUtil {




    public static void reqeustLocation(MyLocationCallback callback){
        WifiListUtil.getList(Utils.getApp(), false, true,new WifiCommonCallback<List<WifiInfoForList>>() {
            @Override
            public void onSuccess(List<WifiInfoForList> wifiInfoForLists) {
                LogUtils.d(wifiInfoForLists);
                reqeustGoogleApi(wifiInfoForLists,callback);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                callback.onFailed(-1,msg);
            }
        });
    }

    /**
     * https://developers.google.com/maps/documentation/geolocation/overview?hl=zh-cn
     * https://developers.google.com/maps/documentation/geolocation/requests-geolocation?hl=zh-cn#wifi_access_point_object
     * @param wifiInfoForLists
     * @param callback
     */
    private static void reqeustGoogleApi(List<WifiInfoForList> wifiInfoForLists,MyLocationCallback callback) {
       /* if(wifiInfoForLists.size() > 6){
            wifiInfoForLists = wifiInfoForLists.subList(0,7);
        }*/
        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();
        for (WifiInfoForList info : wifiInfoForLists) {
            WifiAccessPoint point = new WifiAccessPoint();
            point.setMacAddress(info.wifi_mac);
            point.setSignalStrength(info.signal_strength);
            point.setSignalToNoiseRatio(info.signalToNoiseRatio);
            wifiAccessPoints.add(point);
        }


        GeoParam param = new GeoParam();
        param.considerIp = false;
        param.wifiAccessPoints = wifiAccessPoints;

        WifiAndBaseStationUtil.requestApi(param,callback);

    }





}
