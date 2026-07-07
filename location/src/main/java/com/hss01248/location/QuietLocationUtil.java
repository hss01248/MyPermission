package com.hss01248.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationListener;
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

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
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
import com.hss01248.location.sim.WifiAndBaseStationUtil;
import com.hss01248.permission.DefaultPermissionDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * location switch off 27k
 * no permission 2.8k
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

    int timeOut = 10000;// 10s
    // static ExecutorService executors;
    volatile Handler handler;
    Runnable timeoutRun;
    boolean hasEnd;
    FusedLocationProviderClient gmsFusedClient;
    LocationCallback gmsLocationCallback;
    GoogleApiClient gmsApiClient;

    public void getLocation(Context context, MyLocationCallback listener) {
        getLocation(context, timeOut, listener);
    }

    @Deprecated
    public void getLocation(Context context, int timeoutMills, MyLocationCallback listener0) {
        getLocation(context, timeoutMills, false, listener0);
    }

    public void getLocation(Context context, int timeoutMills, boolean withoutGms, MyLocationCallback listener0) {
        timeOut = timeoutMills;
        /*
         * if (executors == null) {
         * executors = Executors.newCachedThreadPool();
         * }
         */
        if(context ==null){
            context = Utils.getApp();
        }
        context = context.getApplicationContext();

        MyLocationCallback listener = listener0;
        if (!(listener0 instanceof WrappedLocationCallback)) {
            // 包裹,处理缓存的情况
            listener = new WrappedLocationCallback(listener0);
        }
        if (noPermission(context)) {
            listener.onFailed(LocationErrorCode.NO_PERMISSION,
                    LocationErrorCode.getErrorMsg(LocationErrorCode.NO_PERMISSION));
            return;
        }
        LocationManager locationManager = (LocationManager) context.getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            listener.onFailed(LocationErrorCode.LOCATION_MANAGER_NULL,
                    LocationErrorCode.getErrorMsg(LocationErrorCode.LOCATION_MANAGER_NULL));
            return;
        }
        boolean locationEnabled = isLocationEnabled(locationManager);

        if (!locationEnabled) {
            listener.onFailed(LocationErrorCode.LOCATION_SWITCH_OFF,
                    LocationErrorCode.getErrorMsg(LocationErrorCode.LOCATION_SWITCH_OFF));
            return;
        }

        LogUtils.i("getAllProviders-enabled:", locationManager.getProviders(true));
        // [passive, network, fused, gps]

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            LogUtils.w("该设备没有network定位模块,重新设置定位超时时间", listener.configTimeoutWhenOnlyGpsProvider());
            timeOut = (int) listener.configTimeoutWhenOnlyGpsProvider();
        } else {
            if (listener.configForceUseOnlyGpsProvider()) {
                LogUtils.w("配置了强制使用gps,不使用network", listener.configTimeoutWhenOnlyGpsProvider());
                timeOut = (int) listener.configTimeoutWhenOnlyGpsProvider();
            }
        }

        Context finalContext = context;
        MyLocationCallback finalListener1 = listener;
        long startFromBeginning = System.currentTimeMillis();
        new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                } catch (Throwable throwable) {
                    LogUtils.w("Looper",throwable);
                }

                handler = new Handler(Looper.myLooper());
                List<Location> map = Collections.synchronizedList(new ArrayList<>());
                Set<String> countSet = Collections.synchronizedSet(new HashSet<>());

                timeoutRun = new Runnable() {
                    @Override
                    public void run() {
                        callback(map,
                                DefaultPermissionDialog.getString(R.string.location_timeout_msg).replace("15",
                                        timeOut / 1000 + ""),
                                true, finalListener1);
                    }
                };
                handler.postDelayed(timeoutRun, timeOut);

                /*
                 * if (onlyCoarsePermission(finalContext)) {
                 * requestNetWorkOr(finalContext,locationManager, finalListener1);
                 * return;
                 * }
                 */
                try {
                    boolean canUseNetwork = !finalListener1.configNoNetworkProvider()
                            && !finalListener1.configForceUseOnlyGpsProvider();

                    // canUseNetwork = false;

                    if (canUseNetwork) {
                        requestByType(LocationManager.NETWORK_PROVIDER, locationManager, map, countSet, finalListener1,
                                startFromBeginning);
                    }
                    requestByGoogleMapGeoApi("googleMapGeoApi", map, countSet, finalListener1, startFromBeginning);

                    requestByType(LocationManager.GPS_PROVIDER, locationManager, map, countSet, finalListener1,
                            startFromBeginning);
                    if (canUseNetwork) {
                        requestByType(LocationManager.PASSIVE_PROVIDER, locationManager, map, countSet, finalListener1,
                                startFromBeginning);
                        requestByType("fused", locationManager, map, countSet, finalListener1, startFromBeginning);

                        if (!withoutGms && isGmsAvaiable(finalContext)) {
                            countSet.add("gms");
                            GmsLocationUtil.hasGmsGranted(finalContext,
                                    new GmsLocationUtil.IGmsSettingsStateCallback() {
                                        @Override
                                        public void open() {
                                            requestGmsLocation(finalContext, locationManager, map, countSet,
                                                    finalListener1, startFromBeginning);
                                        }

                                        @Override
                                        public void close(String msg) {
                                            LogUtils.w("gms state wrong:" + msg);
                                            finishGmsOnHandler(new Runnable() {
                                                @Override
                                                public void run() {
                                                    cancelGmsLocationRequest();
                                                    onEnd("gms", null, map, countSet, finalListener1);
                                                }
                                            });
                                        }
                                    });

                            // return;
                        }
                    }
                    //

                } catch (Throwable throwable) {
                    LogUtils.w("定位异常1", throwable);
                }

                try {
                    Looper.loop();
                } catch (Throwable throwable) {
                    LogUtils.w("定位异常2", throwable);
                }

            }
        })).start();

        /*
         * if (onlyCoarsePermission(finalContext)) {
         * requestNetWorkOr(finalContext,locationManager, finalListener1);
         * return;
         * }
         * if (isGmsAvaiable(finalContext)) {
         * requestGmsLocation(finalContext,locationManager, finalListener1);
         * return;
         * }
         * requestGPS(finalContext,locationManager, finalListener1);
         */
    }

    /**
     * 要先申请权限再判断开关,如果没有权限,那么即使开关是打开的,这里的locationManager.getProviders(true)也返回0
     * 下面的2和3不受定位权限的限制
     * 
     * @param locationManager
     * @return
     */
    public static boolean isLocationEnabled(LocationManager locationManager) {
        try {
            if (locationManager == null) {
                LogUtils.w("locationManager == null");
                return false;
            }
            // todo tm的不准,靠!!! Compat个寂寞
            /*
             * boolean locationEnabled =
             * LocationManagerCompat.isLocationEnabled(locationManager);
             * if (locationEnabled) {
             * return locationEnabled;
             * }
             */
            /*
             * boolean locationEnabled3 = isLocationEnabled3();
             * if (locationEnabled3) {
             * return true;
             * }
             */
            // 要先申请权限再判断开关,如果没有权限,那么即使开关是打开的,这里的getProviders也返回0
            List<String> allProviders = locationManager.getProviders(true);
            // 如果只有几个passive,那么判定开关关闭
            if (allProviders != null && allProviders.size() == 1) {
                if ("passive".equals(allProviders.get(0))) {
                    return isLocationEnabled3() || isLocationEnabled2(locationManager);
                }
            }
            LogUtils.d("providers:", allProviders);
            if (allProviders != null && !allProviders.isEmpty()) {
                for (String provider : allProviders) {
                    boolean enabled = locationManager.isProviderEnabled(provider);
                    if (enabled) {
                        return enabled;
                    }
                }
            }
            return isLocationEnabled3() || isLocationEnabled2(locationManager);
        } catch (Throwable throwable) {
            LogUtils.w("isLocationEnabled", throwable);
            try {
                return isLocationEnabled3() || isLocationEnabled2(locationManager);
            } catch (Throwable throwable1) {
                LogUtils.w(throwable1);
                return false;
            }
        }
    }

    public static boolean isLocationEnabled2(LocationManager locationManager) {
        return LocationManagerCompat.isLocationEnabled(locationManager);
    }

    public static boolean isLocationEnabled3() {
        try {
            int locationMode = 0;
            String locationProviders;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    locationMode = Settings.Secure.getInt(Utils.getApp().getContentResolver(),
                            Settings.Secure.LOCATION_MODE);
                } catch (Settings.SettingNotFoundException e) {
                    LogUtils.w(e);
                    return false;
                }
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } else {
                locationProviders = Settings.Secure.getString(Utils.getApp().getContentResolver(),
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                return !TextUtils.isEmpty(locationProviders);
            }
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
            return false;
        }

    }

    private void sortLocationMap(List<Location> map) {
        Collections.sort(map, new Comparator<Location>() {
            @Override
            public int compare(Location o1, Location o2) {
                return Long.compare(o2.getTime(), o1.getTime());
            }
        });
    }

    private void addValidLocationsToMap(List<Location> map, List<Location> locations, long maxTime) {
        synchronized (map) {
            boolean added = false;
            for (Location location : locations) {
                if (System.currentTimeMillis() - location.getTime() < maxTime) {
                    map.add(location);
                    added = true;
                }
            }
            if (added) {
                sortLocationMap(map);
            }
        }
    }

    private void finishGmsOnHandler(Runnable runnable) {
        if (handler != null) {
            handler.post(runnable);
        } else {
            runnable.run();
        }
    }

    private Location getMostAcurLocation(List<Location> map) {
        synchronized (map) {
            if (map.isEmpty()) {
                return null;
            }
        /*
         * if (map.containsKey(LocationManager.GPS_PROVIDER) &&
         * map.get(LocationManager.GPS_PROVIDER) != null) {
         * return map.get(LocationManager.GPS_PROVIDER);
         * }
         * if (map.containsKey("fused") && map.get("fused") != null) {
         * return map.get("fused");
         * }
         * if (map.containsKey(LocationManager.PASSIVE_PROVIDER) &&
         * map.get(LocationManager.PASSIVE_PROVIDER) != null) {
         * return map.get(LocationManager.PASSIVE_PROVIDER);
         * }
         * if (map.containsKey(LocationManager.NETWORK_PROVIDER) &&
         * map.get(LocationManager.NETWORK_PROVIDER) != null) {
         * return map.get(LocationManager.NETWORK_PROVIDER);
         * }
         */

            return map.get(0);
        }
    }

    Runnable gmsRunnable;
    //https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
    @SuppressLint("MissingPermission")
    private void onGmsConnected(Context context, Set<String> countSet, LocationManager locationManager,
                                List<Location> map, MyLocationCallback listener, long startFromBeginning) {
        try {
            listener.onEachLocationStart("gms");
            long start0 = System.currentTimeMillis();
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
            gmsFusedClient = fusedLocationProviderClient;
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
                                //没有finelocation权限时,locationManager.getProvider(gps)会抛异常
                                // locationManager.getProvider(lastLocation1.getProvider())
                                LocationSync.putToCache(lastLocation1,"gms",true,System.currentTimeMillis()- start0,System.currentTimeMillis() - startFromBeginning);
                                /*map.put(lastLocation1.getProvider(), lastLocation1);
                                if(LocationSync.getLongitude() ==0){
                                    LocationSync.save(lastLocation1.getLatitude(), lastLocation1.getLongitude());
                                    LocationSync.saveLocation(lastLocation1);
                                }*/
                            }else {
                                LogUtils.w("gms", "get last location:" + lastLocation1);
                            }
                        }
                    });

                    LogUtils.i("start request gms");
                    long start = System.currentTimeMillis();
                    gmsLocationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult result) {
                            try {
                                if (result != null && result.getLocations() != null
                                        && !result.getLocations().isEmpty()) {
                                    List<Location> locations = result.getLocations();
                                    LogUtils.w("gmslocations", locations);
                                    LogUtils.i("onLocationChanged", locations.get(0), locations.get(0).getTime(),
                                            "gms", "耗时(ms):", (System.currentTimeMillis() - start), "距最初耗时(ms)",
                                            System.currentTimeMillis() - startFromBeginning);

                                    for (Location location1 : locations) {
                                        LocationSync.putToCache(location1, "gms", false,
                                                System.currentTimeMillis() - start,
                                                System.currentTimeMillis() - startFromBeginning);
                                    }
                                    long maxTime = 30000;// listener.useCacheInTimeOfMills()
                                    for (Location location : locations) {
                                        // gms有时会返回比较老的数据,对定位实时性要求高的业务造成干扰,所以需要判断
                                        if (System.currentTimeMillis() - location.getTime() < maxTime) {
                                            listener.onEachLocationChanged(location, "gms",
                                                    System.currentTimeMillis() - start,
                                                    System.currentTimeMillis() - startFromBeginning);
                                        } else {
                                            LogUtils.e("gmsLocation",
                                                    "gms返回的定位超过了配置的定位有效期,坑爹的gms:"
                                                            + (System.currentTimeMillis() - location.getTime()) / 1000
                                                            + "s之前的数据");
                                        }
                                    }
                                    addValidLocationsToMap(map, locations, maxTime);
                                }
                            } finally {
                                cancelGmsLocationRequest();
                                onEnd("gms", null, map, countSet, listener);
                            }
                        }

                    };
                    fusedLocationProviderClient.requestLocationUpdates(new LocationRequest()
                            .setExpirationDuration(timeOut)
                            .setNumUpdates(1)
                            .setMaxWaitTime(timeOut), gmsLocationCallback, Looper.myLooper());
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
                                                           finishGmsOnHandler(new Runnable() {
                                                               @Override
                                                               public void run() {
                                                                   cancelGmsLocationRequest();
                                                                   onEnd("gms", null, map, countSet, listener);
                                                               }
                                                           });
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
                                                       finishGmsOnHandler(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               cancelGmsLocationRequest();
                                                               onEnd("gms", null, map, countSet, listener);
                                                           }
                                                       });
                                                   }
                                               }
                                           }
                    );
        } catch (Throwable throwable) {
            finishGmsOnHandler(new Runnable() {
                @Override
                public void run() {
                    cancelGmsLocationRequest();
                    onEnd("gms", null, map, countSet, listener);
                }
            });
            LogUtils.w("gms", throwable);
        }
    }

    private void disconnectGmsApiClient() {
        if (gmsApiClient != null) {
            try {
                if (gmsApiClient.isConnected()) {
                    gmsApiClient.disconnect();
                }
            } catch (Throwable throwable) {
                LogUtils.w("gms disconnect", throwable);
            }
            gmsApiClient = null;
        }
    }

    private void cancelGmsLocationRequest() {
        if (gmsFusedClient != null && gmsLocationCallback != null) {
            gmsFusedClient.removeLocationUpdates(gmsLocationCallback);
        }
        gmsFusedClient = null;
        gmsLocationCallback = null;
        disconnectGmsApiClient();
    }

    private Location getResultSafe(Task<Location> task) {
        try {
            return task.getResult();
        } catch (Throwable throwable) {
            LogUtils.w("dd", throwable);
        }
        return null;
    }

/*    private void requestGmsLocation(Context context, LocationManager locationManager, List<Location> map,
            Set<String> countSet, MyLocationCallback listener, long startFromBeginning) {
        try {
            countSet.add("gms");
            onGmsConnected(context, countSet, locationManager, map, listener, startFromBeginning);
        } catch (Throwable throwable) {
            countSet.remove("gms");
            onEnd(null, map, countSet, listener);
            LogUtils.w("gms2",throwable);
        }
    }*/
private void requestGmsLocation(Context context, LocationManager locationManager, List<Location> map,
                                Set<String> countSet, MyLocationCallback listener, long startFromBeginning) {
    try {
        gmsApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult onConnectionFailed) {
                        LogUtils.w("gms", "onConnectionFailed:" + onConnectionFailed);
                        disconnectGmsApiClient();
                        finishGmsOnHandler(new Runnable() {
                            @Override
                            public void run() {
                                cancelGmsLocationRequest();
                                onEnd("gms", null, map, countSet, listener);
                            }
                        });
                    }
                })
                .build();

        gmsApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                LogUtils.w("gms", "onConnected:");
                onGmsConnected(context, countSet, locationManager, map, listener, startFromBeginning);
                disconnectGmsApiClient();
            }

            @Override
            public void onConnectionSuspended(int i) {
                LogUtils.w("gms", "onConnectionSuspended:" + i);
            }
        });
        gmsApiClient.connect();
    } catch (Throwable throwable) {
        disconnectGmsApiClient();
        finishGmsOnHandler(new Runnable() {
            @Override
            public void run() {
                cancelGmsLocationRequest();
                onEnd("gms", null, map, countSet, listener);
            }
        });
        LogUtils.w("gms", throwable);
    }

}

    public static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*
         * ;
         * return client.;
         */
    }

    private void requestByGoogleMapGeoApi(String provider, List<Location> map, Set<String> countSet,
            MyLocationCallback listener, long startFromBeginning) {

        if (!WifiAndBaseStationUtil.useHttpApi()) {
            return;
        }

        // todo 建立额外的缓存: 比如缓存时效为一天

        countSet.add(provider);
        LogUtils.d("start request " + provider);
        listener.onEachLocationStart(provider);
        long start = System.currentTimeMillis();
        WifiAndBaseStationUtil.requestLocationSilent(new MyLocationCallback() {
            @Override
            public void onSuccess(Location location, String msg) {
                LogUtils.i("onLocationChanged", location, location.getTime(), provider, "耗时(ms):",
                        (System.currentTimeMillis() - start), "距最初耗时(ms)",
                        System.currentTimeMillis() - startFromBeginning);
                if (location != null) {
                    LocationSync.putToCache(location, provider, false, System.currentTimeMillis() - start,
                            System.currentTimeMillis() - startFromBeginning);
                    listener.onEachLocationChanged(location, provider, System.currentTimeMillis() - start,
                            System.currentTimeMillis() - startFromBeginning);
                }
                onEnd(provider, location, map, countSet, listener);
            }

            @Override
            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                onEnd(provider, null, map, countSet, listener);
            }
        });

    }

    @SuppressLint("MissingPermission")
    private void requestByType(String provider, LocationManager locationManager, List<Location> map,
            Set<String> countSet, MyLocationCallback listener, long startFromBeginning) {
        // 不要相信系统的LocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))的返回值，被改过的系统中isProviderEnabled用于判断GPS还是可以的，判断其他定位方式就算了。
        // 作者：一步三回头
        // 链接：https://juejin.cn/post/7016937919533285407。
        if (locationManager.isProviderEnabled(provider)) {
            try {
                countSet.add(provider);
                LogUtils.d("start request " + provider);
                listener.onEachLocationStart(provider);
                long start = System.currentTimeMillis();
                // if(listener.configUseSystemLastKnownLocation()){
                @SuppressLint("MissingPermission")
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    // LogUtils.d(lastKnownLocation);
                    LogUtils.d("lastKnownLocation", lastKnownLocation, provider, "耗时(ms):",
                            (System.currentTimeMillis() - start), "距最初耗时(ms)",
                            System.currentTimeMillis() - startFromBeginning);
                    // map.put(lastKnownLocation.getProvider(), lastKnownLocation);
                    LocationSync.putToCache(lastKnownLocation, provider, true, System.currentTimeMillis() - start,
                            System.currentTimeMillis() - startFromBeginning);
                }
                // }
                // todo 原始数据计算
                /*
                 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                 * && LocationManager.GPS_PROVIDER.equals(provider)) {
                 * locationManager.registerGnssMeasurementsCallback(
                 * new GnssMeasurementsEvent.Callback() {
                 * 
                 * @Override
                 * public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                 * super.onGnssMeasurementsReceived(eventArgs);
                 * GnssClock clock = eventArgs.getClock();
                 * LogUtils.d(clock);
                 * Collection<GnssMeasurement> measurements = eventArgs.getMeasurements();
                 * for(GnssMeasurement measurement : measurements){
                 * LogUtils.d(measurement);
                 * }
                 * }
                 * 
                 * @Override
                 * public void onStatusChanged(int status) {
                 * super.onStatusChanged(status);
                 * }
                 * },handler);
                 * }
                 */

                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        LogUtils.i("onLocationChanged", location, location.getTime(), provider, "耗时(ms):",
                                (System.currentTimeMillis() - start), "距最初耗时(ms)",
                                System.currentTimeMillis() - startFromBeginning);
                        if (location != null) {
                            LocationSync.putToCache(location, provider, false, System.currentTimeMillis() - start,
                                    System.currentTimeMillis() - startFromBeginning);
                            listener.onEachLocationChanged(location, provider, System.currentTimeMillis() - start,
                                    System.currentTimeMillis() - startFromBeginning);
                        }
                        onEnd(provider, location, map, countSet, listener);
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        LogUtils.d("onStatusChanged", provider, status, extras);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        LogUtils.w("onProviderDisabled", provider);
                        onEnd(provider, null, map, countSet, listener);
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        LogUtils.d("onProviderEnabled", provider);
                    }
                }, Looper.myLooper());
            } catch (Throwable throwable) {
                onEnd(provider, null, map, countSet, listener);
                LogUtils.w("location", throwable, provider);
            }
        } else {
            LogUtils.w("locationManager.isProviderEnabled", provider, false, map);
        }
    }

    private void onEnd(@Nullable String provider, @Nullable Location location, List<Location> map, Set<String> count,
            MyLocationCallback listener) {
        boolean shouldCallback = false;
        synchronized (count) {
            if (provider != null) {
                count.remove(provider);
            }
            if (location != null) {
                synchronized (map) {
                    map.add(location);
                    sortLocationMap(map);
                }
            }
            LogUtils.d(count);
            shouldCallback = count.isEmpty();
        }
        if (shouldCallback) {
            callback(map, "complete normal", false, listener);
        }
    }

    private void callback(List<Location> map, String msg, boolean isTimeout, MyLocationCallback listener) {
        LogUtils.i(map, msg, "是否为超时的回调:" + isTimeout);
        if (hasEnd) {
            LogUtils.w("callback when has end,是否为超时的回调:" + isTimeout);
            Location location = getMostAcurLocation(map);
            if (location != null) {
                if (!isTimeout) {
                    LogUtils.w("超时后保存定位:", location);
                }
                // LocationSync.save(location.getLatitude(), location.getLongitude());
                // LocationSync.saveLocation(location);

                if (!isTimeout) {
                    // 假定: 超时后,只有一个没有完成的回调
                    LogUtils.w("超时后looper继续onLocationChanged回调,写缓存,然后立刻移除looper");
                    endLooper();
                } else {
                    LogUtils.w("超时后再延时45s关闭looper");
                    // 再延时30s关闭
                    new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.w("已延时45s关闭looper2");
                            cancelGmsLocationRequest();
                            endLooper();
                        }
                    }, 45000);
                }
            }

            return;
        }
        hasEnd = true;
        Location location = getMostAcurLocation(map);
        if (location != null) {
            // listener.onSuccess(location, "from real_time sys api");
            if (LocationSync.isFakeLocation(location)) {
                if (LocationSync.acceptFakeLocation) {
                    LogUtils.w("from real_time sys api, but fake location,will return fail in release app", location);
                    if (AppUtils.isAppDebug()) {
                        if (System.currentTimeMillis() - MyLocationFastCallback.lastShowToastTime > 30000) {
                            MyLocationFastCallback.lastShowToastTime = System.currentTimeMillis();
                            ToastUtils.showLong(
                                    "from real_time sys api, but fake location,will return fail in release app");
                        }
                    }
                    listener.onSuccess(location, "from real_time sys api, but fake location");
                } else {
                    listener.onFailed(LocationErrorCode.FAKE_LOCATION,
                            LocationErrorCode.getErrorMsg(LocationErrorCode.FAKE_LOCATION), false);
                }
            } else {
                listener.onSuccess(location, "from real_time sys api");
            }
        } else {
            if (isTimeout) {
                listener.onFailed(LocationErrorCode.TIMEOUT,msg);
                //LocationErrorCode.getErrorMsg(LocationErrorCode.TIMEOUT)
            } else {
                listener.onFailed(LocationErrorCode.LOCATION_MANAGER_TIMEOUT_AND_API_FAILED,
                        LocationErrorCode.getErrorMsg(LocationErrorCode.LOCATION_MANAGER_TIMEOUT_AND_API_FAILED));
            }
        }
        if (!isTimeout) {
            LogUtils.i("正常结束,去掉调那些timeoutRunnable");
            if (handler != null) {
                handler.removeCallbacks(timeoutRun);
            }
        }

        if (!isTimeout) {
            endLooper();
        } else {
            LogUtils.w("超时后再延时45s关闭looper2");
            new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    LogUtils.w("已延时45s,立刻关闭looper2");
                    cancelGmsLocationRequest();
                    endLooper();
                }
            }, 45000);
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
                // LocationManager locationManager = (LocationManager)
                // Utils.getApp().getSystemService(Context.LOCATION_SERVICE);
                // locationManager.removeUpdates();
            }
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    private static boolean onlyCoarsePermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    private static boolean noPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }
}
