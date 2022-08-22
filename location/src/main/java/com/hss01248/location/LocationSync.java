package com.hss01248.location;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationProvider;
import android.text.TextUtils;

import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private static  final List<LocationInfo> cachedLocations = new CopyOnWriteArrayList<>();
    //PriorityBlockingQueue

    public static void putToCache(Location location, String startProviderName,
                                  boolean isFromLastKnowLocation,
                                  long timeCost,
                                  LocationProvider provider){
        if(location == null){
            return;
        }
        LocationInfo info = toLocationInfo(location);

        info.timeCost = timeCost;
        if(!isFromLastKnowLocation){
            info.secondsBeforeSaved = (System.currentTimeMillis() - info.timeStamp)/1000;
        }

        //location.getSpeedAccuracyMetersPerSecond()
        if(isFromLastKnowLocation){
            info.calledMethod = startProviderName+"-lastKnowLocation";
        }else {
            info.calledMethod = startProviderName;
        }
        if(provider != null){
           // info.providerInfo = new ProviderInfo();
           // info.providerInfo.initByProvider(provider);
        }
        boolean shouldSave = sortBeforeAdd(info, cachedLocations);
        if(!shouldSave){
            return;
        }
        try {
            if(LogUtils.getConfig().isLogSwitch()){
                String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(cachedLocations);
                LogUtils.json(json);
            }

            saveAsync();
        }catch (Throwable throwable){
            LogUtils.w(throwable);
        }

    }

    /**
     * 模拟PriorityBlockingQueue
     * @param info
     * @param cachedLocations
     */
    private static  boolean sortBeforeAdd(LocationInfo info, List<LocationInfo> cachedLocations) {
        synchronized (LocationSync.class){
            try {
                if(cachedLocations.contains(info)){
                    LogUtils.w("经纬度和时间相同,同一条数据,不添加到list:",info);
                    return false;
                }
                if(cachedLocations.size() == 8){
                    if(cachedLocations.get(cachedLocations.size()-1).timeStamp > info.timeStamp){
                        LogUtils.w("数据太老,不添加到list:",info);
                        return false;
                    }
                }
                List<LocationInfo> locationInfos2 = new ArrayList<>(cachedLocations);
                locationInfos2.add(info);
                Collections.sort(locationInfos2, new Comparator<LocationInfo>() {
                    @Override
                    public int compare(LocationInfo o1, LocationInfo o2) {
                        return (int) (o2.timeStamp - o1.timeStamp);
                    }
                });
                if(locationInfos2.size() > 8){
                    List<LocationInfo> list = locationInfos2.subList(0, 8);
                    cachedLocations.clear();
                    cachedLocations.addAll(list);
                    //ConcurrentModificationException
                }else {
                    cachedLocations.clear();
                    cachedLocations.addAll(locationInfos2);
                }
                LocationInfo fullLocationInfo = getFullLocationInfo();
                if(fullLocationInfo != null){
                    save(fullLocationInfo.lattidude,fullLocationInfo.longtitude);
                }
            }catch (Throwable throwable){
                LogUtils.e(throwable);
            }
            return true;
        }
    }

    private static void sort() {
        try {
            Collections.sort(cachedLocations, new Comparator<LocationInfo>() {
                @Override
                public int compare(LocationInfo o1, LocationInfo o2) {
                    return (int) (o2.timeStamp - o1.timeStamp);
                }
            });
        }catch (Throwable throwable){
            LogUtils.w(throwable);
        }

    }

    private static synchronized void saveAsync() {

        try {
            //这里内部会遍历
            String json = GsonUtils.toJson(cachedLocations);
            //LogUtils.json(json);
            locationCache.saveLocations(json);
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }

    }
    static boolean hasAsync = false;
    static ILocationCache locationCache = new DefaultLocationCache();
    @Deprecated
    public static void initAsync(){

    }

    public static void initAsync(ILocationCache locationCache2){
        if(hasAsync){
            return;
        }
        if(locationCache2 != null){
            locationCache = locationCache2;
        }

        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                String str = locationCache.getLocationJasonArrStr();
                if(TextUtils.isEmpty(str)){
                    return null;
                }
                List<LocationInfo> list = GsonUtils.fromJson(str,new TypeToken<List<LocationInfo>>(){}.getType());
                if(list != null && !list.isEmpty()){
                    cachedLocations.clear();
                    cachedLocations.addAll(list);
                    sort();
                }
                return null;
            }

            @Override
            public void onSuccess(Object result) {
                hasAsync = true;
            }
        });


    }

    public static LocationInfo getFullLocationInfo(){
        if(cachedLocations.isEmpty()){
            //发起一次定位:
            //requestOnce();
            return null;
        }
        try {
            return cachedLocations.get(0);
        }catch (Throwable throwable){
            LogUtils.e(throwable);
            synchronized (LocationSync.class){
                try {
                    return cachedLocations.get(0);
                }catch (Throwable throwable2){
                    LogUtils.e(throwable);
                }
            }
        }

        return null;
    }

    private static void requestOnce() {
        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Object>() {
            @Override
            public Object doInBackground() throws Throwable {
                new QuietLocationUtil().getLocation(Utils.getApp(), new MyLocationCallback() {
                    @Override
                    public void onSuccess(Location location, String msg) {

                    }

                    @Override
                    public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {

                    }
                });
                return null;
            }

            @Override
            public void onSuccess(Object result) {

            }
        });
    }

    public static Location getLocation3(){
        LocationInfo info = getFullLocationInfo();
        if(info == null){
            return null;
        }
        return toAndroidLocation(info);
    }

    public static Location toAndroidLocation(LocationInfo info){
        Location location = new Location(info.realProvider);
        location.setAltitude(info.altitude);
        location.setTime(info.timeStamp);
        location.setLongitude(info.longtitude);
        location.setAccuracy(info.accuracy);
        location.setLatitude(info.lattidude);
        location.setBearing(info.bearing);
        location.setSpeed(info.speed);
        return location;
    }

    public static LocationInfo toLocationInfo(Location location){
        if(location == null){
            return null;
        }
        LocationInfo info = new LocationInfo();
        info.lattidude = location.getLatitude();
        info.longtitude = location.getLongitude();
        info.timeStamp = location.getTime();
        if(LogUtils.getConfig().isLogSwitch()){
            info.timeStampStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(info.timeStamp));
        }
        info.locale = Locale.getDefault().getCountry();
        info.altitude = location.getAltitude();
        info.accuracy = location.getAccuracy();
        info.bearing = location.getBearing();
        info.speed = location.getSpeed();
        info.realProvider = location.getProvider();
        return info;
    }


    /**
     * 保存定位得到的经纬度
     *
     * @param mLatitude 纬度
     * @param mLongitude 经度
     */
    @Deprecated
    public static void save(double mLatitude, double mLongitude) {
        //LogUtils.i(TAG, "设置:" + mLatitude + ", " + mLongitude);
        //put(PARAMS_LAT, String.valueOf(mLatitude));
        //put(PARAMS_LONG, String.valueOf(mLongitude));
    }


    /**获取经度*/
    public static double getLongitude(){
        LocationInfo location2 = getFullLocationInfo();
        if(location2 == null){
            return 0;
        }
        return location2.longtitude;
    }

    /**建议直接使用getFullLocationInfo*/
    @Deprecated
    public static double getLatitude(){
        LocationInfo location2 = getFullLocationInfo();
        if(location2 == null){
            return 0;
        }
       return location2.lattidude;

    }

    private static void put(String key, String valueOf) {
        Utils.getApp().getSharedPreferences("locationutil", Context.MODE_PRIVATE).edit().putString(key,valueOf).apply();
    }

    private static String getString(String key) {
        return Utils.getApp().getSharedPreferences("locationutil",Context.MODE_PRIVATE).getString(key,"");
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
    @Deprecated
    public static void saveLocation(Location mLocation){

    }


    /**
     * 从内存中获取 Location 信息
     * 建议直接使用getFullLocationInfo
     * @return Location
     */
    @Deprecated
    public static Location getLocation(){
        return getLocation3();
    }
}
