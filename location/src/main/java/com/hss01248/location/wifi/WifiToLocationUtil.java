package com.hss01248.location.wifi;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.QuietLocationUtil;
import com.hss01248.location.sim.GeoParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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


    public static String apiKey = "uuuu";
    public static boolean useHttpApi(){
        LocationManager locationManager = (LocationManager) Utils.getApp().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean network = locationManager ==null || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean gmsAvaliabled = QuietLocationUtil.isGmsAvaiable(Utils.getApp());
        if(!network && !gmsAvaliabled){
            //network不可用,gms不可用. 即使gps模块可用,那也可能硬件有问题,比如百富的pos终端
            return true;
        }
        return false;
    }

    public static void reqeustLocation(MyLocationCallback callback){
        WifiListUtil.getList(Utils.getApp(), false, new WifiCommonCallback<List<WifiInfoForList>>() {
            @Override
            public void onSuccess(List<WifiInfoForList> wifiInfoForLists) {
                LogUtils.d(wifiInfoForLists);
                reqeustGoogleApi(apiKey,wifiInfoForLists,callback);
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
     * @param apiKey
     * @param wifiInfoForLists
     * @param callback
     */
    private static void reqeustGoogleApi(String apiKey,List<WifiInfoForList> wifiInfoForLists,MyLocationCallback callback) {
        if(wifiInfoForLists.size() > 6){
            wifiInfoForLists = wifiInfoForLists.subList(0,7);
        }
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

        requestApi(param,callback);

    }


    public static void requestApi(GeoParam param,MyLocationCallback callback){
        String url="https://www.googleapis.com/geolocation/v1/geolocate?key="+apiKey;

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        RequestBody body = RequestBody.create(JSON,new GsonBuilder().create().toJson(param));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onFailed(4,"http request error: "+ e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if(!response.isSuccessful()){
                            callback.onFailed(4,"http request error: "+ response.code());
                            return;
                        }
                        if(response.body() ==null){
                            callback.onFailed(4,"http request 200, but response.body() ==null");
                            return;
                        }
                        //private Integer status;
                        //    private String result;
                        String json = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            Location location = new Location("network");
                            location.setAccuracy((float) jsonObject.optDouble("accuracy"));
                            JSONObject location1 = jsonObject.getJSONObject("location");
                            location.setLatitude(location1.optDouble("lat"));
                            location.setLongitude(location1.optDouble("lng"));
                            location.setTime(System.currentTimeMillis());
                            callback.onSuccess(location,"from google geo api");
                        } catch (Exception e) {
                            callback.onFailed(4,"http request 200, but response.body not json: \n"+json+"\n\n"+e.getMessage());
                        }
                    }
                });
    }


}
