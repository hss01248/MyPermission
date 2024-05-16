package com.hss01248.location.wifi;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.location.LocationManagerCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.permission.MyPermissions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



/**
 * 在Android7.0以上还需要打开定位（也就是手机中”位置信息“）
 */
public class WifiListUtil {

    public static void getList(Application context, boolean justSync, WifiCommonCallback<List<WifiInfoForList>> callback){

        WifiManager wm = (WifiManager) Utils.getApp().getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm == null) {
            callback.onFail("-1","no wifi manager",null);
            return ;
        }




        MyPermissions.requestByMostEffort(false, true,
                new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                        boolean isLocationEnable =  LocationManagerCompat.isLocationEnabled(locationManager);
                        LogUtils.w("LocationManagerCompat.isLocationEnabled:"+isLocationEnable);
                        if(!isLocationEnable){
                            callback.onFail("-1","location switch off",null);
                            return;
                        }
                        if(justSync){
                            LogUtils.w("request wifi results last time in sync");
                            getLastTime(wm,callback);
                            return;
                        }
                        LogUtils.w("requestScan wifi");
                        requestScan(context,wm,callback);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        callback.onFail("-1","no location permission",null);
                    }
                },Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);

    }

    private static void requestScan(Application context, WifiManager wm,WifiCommonCallback<List<WifiInfoForList>> callback) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                callback.onFail("-1","scan wifi timeout",null);
            }
        };
        Handler handler =new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable,2000);


        BroadcastReceiver mReceiver =new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtils.w("requestScan wifi  onReceive:"+intent);
                handler.removeCallbacks(runnable);
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    //List results =wm.getScanResults();
                    getLastTime(wm,callback);
                }
                context.unregisterReceiver(this);
            }
        };
        IntentFilter filter =new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(mReceiver, filter);
        wm.startScan();
    }

    private static void getLastTime(WifiManager wm,WifiCommonCallback<List<WifiInfoForList>> callback) {
        try {
            List<ScanResult> scanResults = wm.getScanResults();
            if(scanResults == null || scanResults.isEmpty()){
                callback.onFail("0","scan result is emtpy",null);
                return;
            }
            List<WifiInfoForList> list = new ArrayList<>(scanResults.size());
            for (ScanResult scanResult : scanResults) {
                LogUtils.d("result",scanResult);
                WifiInfoForList info = new WifiInfoForList();
                info.signal_strength  = scanResult.level;
                info.wifi_name = scanResult.SSID;
                info.wifi_mac = scanResult.BSSID;
                //info.signalToNoiseRatio = scanResult.frequency;
                list.add(info);
            }
            Collections.sort(list, new Comparator<WifiInfoForList>() {
                @Override
                public int compare(WifiInfoForList o1, WifiInfoForList o2) {
                    return o1.signal_strength - o2.signal_strength;
                }
            });
            callback.onSuccess(list);
        }catch (Throwable throwable){
            LogUtils.w("dd",throwable);
            callback.onFail(throwable.getClass().getSimpleName(),throwable.getMessage(),throwable);
        }


    }
}

