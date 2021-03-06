package com.hss01248.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * location switch off  27k
 * no permission        2.8k
 * no cache+10s timeout 0.4k
 * 以上约占总上报量的10%
 *
 * 饱和式定位工具类: 同时发起gps,network,passive定位
 *
 * gms管理的蛋疼之处:
 * 当手机有gms时,需要定位开关+gms详细位置开关 同时开启,才能定位,如果gms详细位置开关关闭,则定位必失败,onlocationChanged无回调
 * 但同样一台手机上谷歌,高德能定位成功,是什么原理?
 *
 *
 *
 * https://juejin.cn/post/7016937919533285407
 */
public class QuietLocationUtil {

    public int getTimeOut() {
        return timeOut;
    }

    public QuietLocationUtil setTimeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    int timeOut = 10000;//10s
    //static ExecutorService executors;
    volatile Handler handler;
    Runnable timeoutRun;
    Runnable gmsRunnable;
    boolean hasEnd;

    public void getLocation(Context context, MyLocationCallback listener) {
        getLocation(context, timeOut, listener);
    }


    public void getLocation(Context context, int timeoutMills, MyLocationCallback listener0) {
        timeOut = timeoutMills;
        /*if (executors == null) {
            executors = Executors.newCachedThreadPool();
        }*/
        context = context.getApplicationContext();

        MyLocationCallback finalListener = listener0;

        MyLocationCallback listener = new MyLocationCallback() {
            @Override
            public void onFailed(int type, String msg) {
                Map<String, String> ext = new HashMap<>();
                ext.put("msg", msg);
                Location cache = getFromCache(ext, msg);
                if (cache == null) {
                    finalListener.onFailed(type, ext.get("msg"));
                } else {
                    finalListener.onSuccess(cache, ext.get("msg"));
                }
            }

            @Override
            public void onSuccess(Location location, String msg) {
                finalListener.onSuccess(location, msg);
            }
        };
        if (noPermission(context)) {
            listener.onFailed(1, "no permission");
            return;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            listener.onFailed(6, "locationManager is null");
            return;
        }
        boolean locationEnabled = isLocationEnabled(locationManager);

        if (!locationEnabled) {
            listener.onFailed(2, "location switch off");
            return;
        }

        LogUtils.i("getAllProviders-enabled:", locationManager.getProviders(true));

        Context finalContext = context;
        MyLocationCallback finalListener1 = listener;

        MyLocationCallback finalListener2 = listener;
        MyLocationCallback finalListener3 = listener;
        new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }


                handler = new Handler(Looper.myLooper());
                Map<String, Location> map = new HashMap<>();
                Set<String> countSet = new HashSet<>();

                timeoutRun = new Runnable() {
                    @Override
                    public void run() {
                        callback(map, timeOut + "s timeout", true, finalListener3);
                    }
                };
                handler.postDelayed(timeoutRun, timeOut);

                /*if (onlyCoarsePermission(finalContext)) {
                    requestNetWorkOr(finalContext,locationManager, finalListener1);
                    return;
                }*/
                try {
                    requestByType(LocationManager.NETWORK_PROVIDER, locationManager, map, countSet, finalListener1);
                    requestByType(LocationManager.GPS_PROVIDER, locationManager, map, countSet, finalListener1);
                    requestByType(LocationManager.PASSIVE_PROVIDER, locationManager, map, countSet, finalListener1);
                    //requestByType("fused", locationManager, map, countSet, finalListener1);
                    if (isGmsAvaiable(finalContext)) {
                        requestGmsLocation(finalContext, locationManager, map, countSet, finalListener1);
                        //return;
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }


                try {
                    Looper.loop();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        })).start();


      /*  if (onlyCoarsePermission(finalContext)) {
            requestNetWorkOr(finalContext,locationManager, finalListener1);
            return;
        }
        if (isGmsAvaiable(finalContext)) {
            requestGmsLocation(finalContext,locationManager, finalListener1);
            return;
        }
        requestGPS(finalContext,locationManager, finalListener1);*/
    }

    public static boolean isLocationEnabled(LocationManager locationManager) {
        try {
            if (locationManager == null) {
                LogUtils.w("locationManager == null");
                return false;
            }
            //todo tm的不准,靠!!! Compat个寂寞
          /*  boolean locationEnabled = LocationManagerCompat.isLocationEnabled(locationManager);
            if (locationEnabled) {
                return locationEnabled;
            }*/
            boolean locationEnabled3 = isLocationEnabled3();
            if (locationEnabled3) {
                return true;
            }

            List<String> allProviders = locationManager.getProviders(true);
            LogUtils.d("providers:", allProviders);
            if (allProviders != null && !allProviders.isEmpty()) {
                for (String provider : allProviders) {
                    boolean enabled = locationManager.isProviderEnabled(provider);
                    if (enabled) {
                        return enabled;
                    }
                }
            }
            return false;
        } catch (Throwable throwable) {
            LogUtils.w("isLocationEnabled", throwable);
            return false;
        }

    }

    static boolean isLocationEnabled3() {
        try {
            int locationMode = 0;
            String locationProviders;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    locationMode = Settings.Secure.getInt(Utils.getApp().getContentResolver(), Settings.Secure.LOCATION_MODE);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } else {
                locationProviders = Settings.Secure.getString(Utils.getApp().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                return !TextUtils.isEmpty(locationProviders);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }

    }


    private Location getMostAcurLocation(Map<String, Location> map) {
        if (map.isEmpty()) {
            return null;
        }
       /* Location location = null;
        for (Map.Entry<String, Location> entry : map.entrySet()) {
            if(entry.getValue() == null){
                continue;
            }
            if(location == null){
                location = entry.getValue();
            }else {
                if(location.getAccuracy() < entry.getValue().getAccuracy()){
                    location = entry.getValue();
                }
            }
        }*/
        if (map.containsKey(LocationManager.GPS_PROVIDER) && map.get(LocationManager.GPS_PROVIDER) != null) {
            return map.get(LocationManager.GPS_PROVIDER);
        }
        if (map.containsKey("fused") && map.get("fused") != null) {
            return map.get("fused");
        }
        if (map.containsKey(LocationManager.PASSIVE_PROVIDER) && map.get(LocationManager.PASSIVE_PROVIDER) != null) {
            return map.get(LocationManager.PASSIVE_PROVIDER);
        }
        if (map.containsKey(LocationManager.NETWORK_PROVIDER) && map.get(LocationManager.NETWORK_PROVIDER) != null) {
            return map.get(LocationManager.NETWORK_PROVIDER);
        }

        return null;
    }

    @SuppressLint("MissingPermission")
    private void onGmsConnected(Context context, Set<String> countSet, LocationManager locationManager, Map<String, Location> map, MyLocationCallback listener) {
        try {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
            gmsRunnable = new Runnable() {

                @Override
                public void run() {
                    fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location lastLocation1 = getResultSafe(task);
                            //Fatal Exception: com.google.android.gms.tasks.RuntimeExecutionException
                            //com.google.android.gms.common.api.ApiException: 8: The connection to Google Play services was lost
                            //com.google.android.gms.tasks.zzu.getResult (zzu.java:15)
                            //devicedata.gps.SilentLocationUtil$3$1.onComplete (SilentLocationUtil.java:265)
                            //com.google.android.gms.tasks.zzj.run (zzj.java:4)
                            //com.android.internal.os.ZygoteInit.main (ZygoteInit.java:873)
                            if (lastLocation1 != null) {
                                LogUtils.i("gms", "get last location:" + lastLocation1);
                                map.put(lastLocation1.getProvider(), lastLocation1);
                                if(LocationSync.getLongitude() ==0){
                                    LocationSync.save(lastLocation1.getLatitude(), lastLocation1.getLongitude());
                                    LocationSync.saveLocation(lastLocation1);
                                }
                            }else {
                                LogUtils.w("gms", "get last location:" + lastLocation1);
                            }
                        }
                    });
                    LogUtils.i("start request gms");
                    fusedLocationProviderClient.requestLocationUpdates(new LocationRequest()
                            .setExpirationDuration(timeOut)
                            .setNumUpdates(1)
                            .setMaxWaitTime(timeOut), new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult result) {
                            LogUtils.i("gms result", result);
                            if (result != null && result.getLocations() != null && !result.getLocations().isEmpty()) {
                                Location location = result.getLocations().get(0);
                                countSet.remove("gms");
                                LocationSync.save(location.getLatitude(), location.getLongitude());
                                LocationSync.saveLocation(location);
                                onEnd(location, map, countSet, listener);
                            } else {
                                countSet.remove("gms");
                                onEnd(null, map, countSet, listener);
                            }
                        }

                    }, Looper.myLooper());
                }
            };


            fusedLocationProviderClient.getLocationAvailability()
                    .addOnCompleteListener(new OnCompleteListener<LocationAvailability>() {
                                               @Override
                                               public void onComplete(@NonNull Task<LocationAvailability> task) {
                                                   //com.google.android.gms.common.api.ApiException: 8: The connection to Google Play services was lost
                                                   //devicedata.gps.SilentLocationUtil$4.onComplete
                                                   try {
                                                       if (task.getResult() == null) {
                                                           LogUtils.w("gms getLocationAvailability result null");
                                                           countSet.remove("gms");
                                                           return;
                                                       }
                                                       boolean locationAvailable = task.getResult().isLocationAvailable();
                                                       if (!locationAvailable) {
                                                           LogUtils.e("gms location not available--> 这个辣鸡api不准, " +
                                                                   "第一次打开定位开关,但关闭谷歌定位精准度时,这个返回false,但实际可以发起定位,且能很快定位成功." +
                                                                   "而gps和passive大概率超时,所以不要在这里拦截,不管能不能用都发起定位,反正有超时机制,不怕没有callback");
                                                          // countSet.remove("gms");
                                                           //return;
                                                       }
                                                       handler.post(gmsRunnable);
                                                   } catch (Throwable throwable) {
                                                       LogUtils.w("gms", throwable);
                                                       countSet.remove("gms");
                                                   }
                                               }
                                           }
                    );
        } catch (Throwable throwable) {
            countSet.remove("gms");
            throwable.printStackTrace();
        }


    }

    private Location getResultSafe(Task<Location> task) {
        try {
            return task.getResult();
        } catch (Throwable throwable) {
            LogUtils.w("dd", throwable);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private void onGmsConnected2(GoogleApiClient finalClient, Context context, Set<String> countSet, LocationManager locationManager, Map<String, Location> map, MyLocationCallback listener) {
        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(finalClient);
            if (lastLocation != null) {
                map.put(lastLocation.getProvider(), lastLocation);
            }
            LocationRequest locationRequest = LocationRequest.create();
            //new android.location.LocationRequest()

            LocationServices.FusedLocationApi.requestLocationUpdates(finalClient,
                    locationRequest.setExpirationDuration(timeOut)
                            .setMaxWaitTime(timeOut),
                    new com.google.android.gms.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            LogUtils.i("gms", "onLocationChanged:" + location);
                            countSet.remove("gms");
                            onEnd(location, map, countSet, listener);
                        }
                    });
        } catch (Throwable throwable) {
            countSet.remove("gms");
            throwable.printStackTrace();
        }


    }


    private void requestGmsLocation(Context context, LocationManager locationManager, Map<String, Location> map, Set<String> countSet, MyLocationCallback listener) {
        try {


            //LocationServices.getFusedLocationProviderClient(context).getLastLocation().addOnCompleteListener()

            GoogleApiClient client = null;
            client = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult onConnectionFailed) {
                            LogUtils.w("gms", "onConnectionFailed:" + onConnectionFailed);
                            //requestGPS(context,locationManager, map, listener);
                            countSet.remove("gms");
                            onEnd(null, map, countSet, listener);
                        }
                    })
                    .build();

            GoogleApiClient finalClient = client;
            client.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @SuppressLint("MissingPermission")
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    LogUtils.w("gms", "onConnected:");
                    onGmsConnected(context, countSet, locationManager, map, listener);
                    //onGmsConnected2(finalClient,context, countSet, locationManager, map, listener);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    LogUtils.w("gms", "onConnectionSuspended:" + i);
                }
            });
            client.connect();
            countSet.add("gms");
        } catch (Throwable throwable) {
            countSet.remove("gms");
            onEnd(null, map, countSet, listener);
            throwable.printStackTrace();
        }

    }

    static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*;
        return client.;*/
    }

    @SuppressLint("MissingPermission")
    private void requestByType(String provider, LocationManager locationManager, Map<String, Location> map, Set<String> countSet, MyLocationCallback listener) {
        //不要相信系统的LocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))的返回值，被改过的系统中isProviderEnabled用于判断GPS还是可以的，判断其他定位方式就算了。
        //作者：一步三回头
        //链接：https://juejin.cn/post/7016937919533285407。
        if (locationManager.isProviderEnabled(provider)) {
            try {
                countSet.add(provider);
                LogUtils.d("start request " + provider);
                long start = System.currentTimeMillis();
                @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    //LogUtils.d(lastKnownLocation);
                    LogUtils.d("lastKnownLocation", lastKnownLocation, provider, "耗时(ms):", (System.currentTimeMillis() - start));
                    map.put(lastKnownLocation.getProvider(), lastKnownLocation);
                    //如果本地缓存没有,就更新一次
                    if(LocationSync.getLongitude() ==0){
                        LocationSync.save(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        LocationSync.saveLocation(lastKnownLocation);
                    }
                }
                locationManager.requestSingleUpdate(provider, new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        LogUtils.d("onLocationChanged", location, provider, "耗时(ms):", (System.currentTimeMillis() - start));
                        if(location != null){
                            LocationSync.save(location.getLatitude(), location.getLongitude());
                            LocationSync.saveLocation(location);
                        }
                        countSet.remove(provider);
                        onEnd(location, map, countSet, listener);
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        LogUtils.d("onStatusChanged", provider, status, extras);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        LogUtils.w("onProviderDisabled", provider);
                        countSet.remove(provider);
                        onEnd(null, map, countSet, listener);
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        LogUtils.d("onProviderEnabled", provider);
                    }
                }, Looper.myLooper());
            } catch (Throwable throwable) {
                countSet.remove(provider);
                throwable.printStackTrace();
            }
        }else {
            LogUtils.w("locationManager.isProviderEnabled",provider,false,map);
        }
    }


    private void onEnd(Location location, Map<String, Location> map, Set<String> count, MyLocationCallback listener) {
        //LogUtils.d(location);
        if (location != null) {
            map.put(location.getProvider(), location);
        }
        LogUtils.d(count);

        if (count.size() == 0) {
            callback(map, "complete normal", false, listener);
        }

    }

    static Location getFromCache(Map ext, String msg) {
        Location cache = LocationSync.getLocation();
        if (cache == null) {
            double lon = LocationSync.getLongitude();
            double lat = LocationSync.getLatitude();
            if (lon == 0 && lat == 0) {
                ext.put("msg", "no cache and " + msg);
            } else {
                cache = new Location(LocationManager.PASSIVE_PROVIDER);
                cache.setLongitude(LocationSync.getLongitude());
                cache.setLatitude(LocationSync.getLatitude());
                ext.put("msg", "from disk cache and " + msg);
            }
        } else {
            ext.put("msg", "from memory cache and " + msg);
        }
        return cache;


    }

    private void callback(Map<String, Location> map, String msg, boolean isTimeout, MyLocationCallback listener) {
        LogUtils.i(map, msg, "是否超时:" + isTimeout);
        if (hasEnd) {
            LogUtils.w("callback when has end,是否超时:" + isTimeout);
            Location location = getMostAcurLocation(map);
            if (location != null) {
                if (!isTimeout) {
                    LogUtils.w("超时后保存定位:", location);
                }
                LocationSync.save(location.getLatitude(), location.getLongitude());
                LocationSync.saveLocation(location);

                if (!isTimeout) {
                    LogUtils.w("超时后looper继续回调,写缓存,然后移除looper");
                    endLooper();
                }else {
                    //再延时30s关闭
                   new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           endLooper();
                       }
                   },30000);
                }
            }

            return;
        }
        hasEnd = true;
        Location location = getMostAcurLocation(map);
        if (location != null) {
            listener.onSuccess(location, "from sys api");
            try {
                LocationSync.save(location.getLatitude(), location.getLongitude());
                LocationSync.saveLocation(location);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        } else {
            listener.onFailed(77, "no location get when api request end");
        }
        if (!isTimeout) {
            LogUtils.i("正常结束,去掉调那些timeoutRunnable");
            if (handler != null) {
                handler.removeCallbacks(timeoutRun);
                if (gmsRunnable != null) {
                    handler.removeCallbacks(gmsRunnable);
                }
            }
        }

        if (!isTimeout) {
            endLooper();
        }else {
            //再延时30s关闭
            new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    endLooper();
                }
            },30000);
        }


    }

    private void endLooper() {
        try {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                LogUtils.d("quit loop.myLooper");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Looper.myLooper().quitSafely();
                } else {
                    Looper.myLooper().quit();
                }
               // LocationManager locationManager = (LocationManager) Utils.getApp().getSystemService(Context.LOCATION_SERVICE);
                //locationManager.removeUpdates();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static boolean onlyCoarsePermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    private static boolean noPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }
}


