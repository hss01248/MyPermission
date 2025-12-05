package com.hss01248.location.sim;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.hss01248.location.LocationInfo;
import com.hss01248.location.LocationSync;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.QuietLocationUtil;
import com.hss01248.location.wifi.WifiAccessPoint;
import com.hss01248.location.wifi.WifiCommonCallback;
import com.hss01248.location.wifi.WifiInfoForList;
import com.hss01248.location.wifi.WifiListUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
 * @Date 5/27/24 4:52 PM
 * @Version 1.0
 */
public class WifiAndBaseStationUtil {

    //一天内,使用缓存,不调用api
    public static long cacheTime = 24*60*60*1000L;
    //public static long cacheTime = 1000L;

     public static String xxx = "xxx";
    public static String refererUrl = "https://xxxx.com";

     public static boolean forceAlsoUseHttpApi = "PAX".equalsIgnoreCase(Build.MANUFACTURER);

    public static boolean useHttpApi(){
        if(forceAlsoUseHttpApi){
            return true;
        }
        try{
            LocationManager locationManager = (LocationManager) Utils.getApp().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            boolean network = locationManager ==null || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            boolean gmsAvaliabled = QuietLocationUtil.isGmsAvaiable(Utils.getApp());
            if(!network && !gmsAvaliabled){
                //network provider不可用,gms不可用. 即使gps模块可用,那也可能硬件有问题,比如百富的pos终端
                return true;
            }
            return false;
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            return false;
        }

    }
    public static  Location readCacheLocation( boolean ignoreTime){
        String string = SPStaticUtils.getString("google-geo-cache");
        if(!TextUtils.isEmpty(string)){
            try{
                LocationInfo info = GsonUtils.fromJson(string,LocationInfo.class);
                if(info !=null){
                    if(ignoreTime || (info.timeStamp> 0 && System.currentTimeMillis() - info.timeStamp < cacheTime)) {
                        Location androidLocation = LocationSync.toAndroidLocation(info);
                        //重写时间
                        androidLocation.setTime(System.currentTimeMillis());
                        androidLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        androidLocation.setProvider(androidLocation.getProvider()+"-fromCache");
                        return androidLocation;
                    }
                }
            }catch (Throwable throwable){
                LogUtils.w(string,throwable);
            }
        }
        return null;
    }
    
    public static void writeLocation(Location location){
        LocationInfo locationInfo = LocationSync.toLocationInfo(location);
        SPStaticUtils.put("google-geo-cache",GsonUtils.toJson(locationInfo));
    }

    public static void requestLocationSilent(MyLocationCallback callback){
        Location androidLocation = readCacheLocation(false);
        if(androidLocation !=null){
            callback.onSuccess(androidLocation,"from cache");
            return;
        }
        
        

        long start = System.currentTimeMillis();
        CellTowerUtil.loadInfo(new WifiCommonCallback<GeoParam>() {
            @Override
            public void onSuccess(GeoParam param0) {
                requestWifi(param0,false,callback,start);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                requestWifi(null,false,callback,start);
            }
        });
    }

    public static void requestLocation(MyLocationCallback callback) {
        long start = System.currentTimeMillis();
        CellTowerUtil.getCellTowerInfo(new WifiCommonCallback<GeoParam>() {
            @Override
            public void onSuccess(GeoParam param0) {
                requestWifi(param0,true,callback,start);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                requestWifi(null,true,callback,start);
            }
        });
    }

    private static void requestWifi(GeoParam param0,boolean requestPermission, MyLocationCallback callback,long start) {


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
                requestApi(param,callback,start);
            }



            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                LogUtils.w(code,msg,throwable);
                if(param0 == null){
                    callback.onFailed(6,"wifi and cell tower both not avaiable");
                }else {
                    requestApi(param0,callback,start);
                }
            }
        });
    }



    public static void requestApi(GeoParam param,MyLocationCallback callback,long start){
        long start2 = System.currentTimeMillis();
        String url="https://www.googleapis.com/geolocation/v1/geolocate?key="+ xxx;
        param.considerIp = false;
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        RequestBody body = RequestBody.create(JSON,new GsonBuilder().create().toJson(param));
        Request request = new Request.Builder()
                .url(url)
                //.header("X-Android-Package", AppUtils.getAppPackageName())
                //.header("X-Android-Cert", getSha1Signature(Utils.getApp())+"")
                //Requests from this Android client application com.xxx.debug are blocked.
                //.header("User-Agent", System.getProperty("http.agent")+"")
                //.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                .addHeader("Referer", refererUrl)
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
                        boolean noWifiList = param.wifiAccessPoints==null ||param.wifiAccessPoints.isEmpty();
                        //private Integer status;
                        //    private String result;
                        String json = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            Location location = new Location("googleMapGeoApi-"+(noWifiList?"onlyCellTower":"withWifiList"));
                            location.setAccuracy((float) jsonObject.optDouble("accuracy"));
                            JSONObject location1 = jsonObject.getJSONObject("location");
                            location.setLatitude(location1.optDouble("lat"));
                            location.setLongitude(location1.optDouble("lng"));
                            location.setTime(start);
                            long cost = System.currentTimeMillis() - start;
                            long offset = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                            long targetElapsedRealtimeMs = start - offset;
                            long targetElapsedRealtimeNanos = targetElapsedRealtimeMs * 1000000L;
                            location.setElapsedRealtimeNanos(targetElapsedRealtimeNanos);

                            Bundle bundle = new Bundle();
                            bundle.putLong("timeCost",System.currentTimeMillis()-start2);
                            bundle.putLong("costFromBegin",cost);
                            bundle.putLong("millsOldWhenSaved",cost);
                            bundle.putBoolean("hasFineLocationPermission", true);
                            bundle.putString("calledMethod","google geo api");
                            location.setExtras(bundle);

                            callback.onSuccess(location,"from google geo api");
                            writeLocation(location);
                        } catch (Exception e) {
                            callback.onFailed(4,"http request 200, but response.body not json: \n"+json+"\n\n"+e.getMessage());
                        }
                    }
                });
    }

    public static String getSha1Signature(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );

            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();

                // 将字节转换为十六进制字符串 (例如: "A1:B2:C3...")
                StringBuilder hexString = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i != 0) hexString.append(":");
                    String appendString = Integer.toHexString(0xFF & digest[i]).toUpperCase();
                    if (appendString.length() == 1) hexString.append("0");
                    hexString.append(appendString);
                }
                return hexString.toString();
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
