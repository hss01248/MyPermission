package com.hss01248.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;

import java.util.HashMap;
import java.util.Map;

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
