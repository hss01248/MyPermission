package com.hss01248.location;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationProvider;
import android.text.TextUtils;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * @Despciption todo
 * @Author hss
 * @Date 08/12/2021 11:56
 * @Version 1.0
 */
public class LocationSync {

    public static final String TAG = "LocationSync";

    private static final Map<String, Address> ADDRESS_MAP = new HashMap<>();
    private static final Map<String, Location> LOCATION_MAP = new HashMap<>();
    private static final String PARAMS_ADDRESS = "addressxx";
    private static final String PARAMS_LOCATION = "locationxx";
    private static final String PARAMS_LAT = "latitudexx";
    private static final String PARAMS_LONG = "longitudexx";

    private static  final TreeSet<LocationInfo> cachedLocations = new TreeSet<>(new Comparator<LocationInfo>() {
        @Override
        public int compare(LocationInfo o1, LocationInfo o2) {
            return (int) (o2.timeStamp - o1.timeStamp);
        }
    });

    public static void putToCache(Location location, String startProviderName,
                                  boolean isFromLastKnowLocation,
                                  long timeCost,
                                  LocationProvider provider){
        if(location == null){
            return;
        }
        LocationInfo info = new LocationInfo();
        info.lattidude = location.getLatitude();
        info.longtitude = location.getLongitude();
        info.timeStamp = location.getTime();
        info.altitude = location.getAltitude();
        info.accuracy = location.getAccuracy();
        info.bearing = location.getBearing();
        info.speed = location.getSpeed();
        info.timeCost = timeCost;
        info.realProvider = location.getProvider();
        //location.getSpeedAccuracyMetersPerSecond()
        if(isFromLastKnowLocation){
            info.calledMethod = startProviderName+"-lastKnowLocation";
        }else {
            info.calledMethod = startProviderName;
        }
        if(provider != null){
            info.providerInfo = new ProviderInfo();
            info.providerInfo.initByProvider(provider);
        }
        cachedLocations.add(info);

        if(LogUtils.getConfig().isLogSwitch()){
            String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(cachedLocations);
            LogUtils.json(json);
        }

        saveAsync(cachedLocations);
    }

    private static void saveAsync(TreeSet<LocationInfo> cachedLocations) {
        //长度控制: 最多十条
        Iterator<LocationInfo> iterator = cachedLocations.iterator();
        int count = 0;
        while (iterator.hasNext()){
            count++;
            LocationInfo next = iterator.next();
            if(count > 9){
                iterator.remove();
            }
        }
        String json = GsonUtils.toJson(cachedLocations);
        SPUtils.getInstance().put("cachedLocations",json);
    }
    public static void initAsync(){
        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                String str = SPUtils.getInstance().getString("cachedLocations","");
                if(TextUtils.isEmpty(str)){
                    return null;
                }
                List<LocationInfo> list = GsonUtils.fromJson(str,new TypeToken<List<LocationInfo>>(){}.getType());
                if(list != null && !list.isEmpty()){
                    cachedLocations.addAll(list);
                }
                return null;
            }

            @Override
            public void onSuccess(Object result) {

            }
        });


    }

    public static LocationInfo getLocation2(){
        if(cachedLocations.isEmpty()){
            return null;
        }
        try {
            for (LocationInfo cachedLocation : cachedLocations) {
                return cachedLocation;
            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }

        return cachedLocations.first();
    }


    /**
     * 保存定位得到的经纬度
     *
     * @param mLatitude 纬度
     * @param mLongitude 经度
     */
    public static void save(double mLatitude, double mLongitude) {
        LogUtils.i(TAG, "设置:" + mLatitude + ", " + mLongitude);
        put(PARAMS_LAT, String.valueOf(mLatitude));
        put(PARAMS_LONG, String.valueOf(mLongitude));
    }


    /**获取经度*/
    public static double getLongitude(){
        String mLongitude = getString(PARAMS_LONG);
        if(TextUtils.isEmpty(mLongitude)){
            return 0;
        }
        try {
            return Double.parseDouble(mLongitude);
        }catch (Throwable throwable){
            throwable.printStackTrace();
            return 0;
        }
    }

    /**获取纬度*/
    public static double getLatitude(){
        String mLatitude = getString(PARAMS_LAT);
        if(TextUtils.isEmpty(mLatitude)){
            return 0;
        }
        try {
            return Double.parseDouble(mLatitude);
        }catch (Throwable throwable){
            throwable.printStackTrace();
            return 0;
        }

    }

    private static void put(String paramsLat, String valueOf) {
        Utils.getApp().getSharedPreferences("locationutil", Context.MODE_PRIVATE).edit().putString(paramsLat,valueOf).apply();
    }

    private static String getString(String paramsLat) {
        return Utils.getApp().getSharedPreferences("locationutil",Context.MODE_PRIVATE).getString(paramsLat,"");
    }

    /**
     *将定位获取到的address放在内存中
     *
     * @param mAddress mAddress
     */
    public static void saveAddress(Address mAddress){
        ADDRESS_MAP.put(PARAMS_ADDRESS,mAddress);
    }


    /**
     * 从内存中获取adress信息
     *
     * @return Address
     */
    public static Address getAddress(){
        return ADDRESS_MAP.get(PARAMS_ADDRESS);
    }

    /**
     * 将定位获取到的 Location 放在内存中
     *
     * @param mLocation mLocation
     */
    public static void saveLocation(Location mLocation){
        LOCATION_MAP.put(PARAMS_LOCATION,mLocation);
    }


    /**
     * 从内存中获取 Location 信息
     * @return Location
     */
    public static Location getLocation(){
        return LOCATION_MAP.get(PARAMS_LOCATION);
    }
}
