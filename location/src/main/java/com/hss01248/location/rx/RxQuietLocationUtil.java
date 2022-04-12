package com.hss01248.location.rx;

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
import android.os.HandlerThread;
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
import com.hss01248.location.LocationSync;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.QuietLocationUtil;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


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
public class RxQuietLocationUtil {

    public int getTimeOut() {
        return timeOut;
    }

    public RxQuietLocationUtil setTimeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    int timeOut = 10000;//10s
    //static ExecutorService executors;
    volatile Handler handler;
    Runnable timeoutRun;
    Runnable gmsRunnable;
    boolean hasEnd;

    HandlerThread handlerThread;

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
            public void onFailed(int type, String msg,boolean isFailBeforeReallyRequest) {
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

            @Override
            public void onEachLocationChanged(Location location, String provider) {
                listener0.onEachLocationChanged(location, provider);
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

        List<String> providers = locationManager.getProviders(true);
        LogUtils.i("getAllProviders-enabled:", providers);

       handlerThread =  new HandlerThread("silentlocation");
       handlerThread.start();

        AtomicInteger count = new AtomicInteger(providers.size());
        long start = System.currentTimeMillis();


       /* Observable<String> stringObservable = null;
        for (String provider : providers) {
            if(stringObservable == null){
                stringObservable = Observable.just(provider);
            }else {
                stringObservable = Observable.merge(stringObservable,Observable.just(provider));
            }
        }*/
        //stringObservable.do


        Observable<Location> providersObservable = Observable.fromIterable(providers)
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<String, ObservableSource<Location>>() {
                    @Override
                    public ObservableSource<Location> apply(@io.reactivex.annotations.NonNull String provider) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<Location>() {
                            @Override
                            @SuppressLint("MissingPermission")
                            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Location> emitter) throws Exception {

                                //locationManager.getCurrentLocation(provider,);
                                locationManager.requestSingleUpdate(provider, new LocationListener() {
                                    @Override
                                    public void onLocationChanged(@NonNull Location location) {
                                        LogUtils.d("onLocationChanged", location, provider, "耗时(ms):", (System.currentTimeMillis() - start));
                                        emitter.onNext(location);
                                        int i = count.decrementAndGet();
                                        LogUtils.i("count.decrementAndGet()",i);
                                        if(i <=0){
                                            emitter.onComplete();
                                        }
                                        locationManager.removeUpdates(this);
                                    }

                                    @Override
                                    public void onStatusChanged(String provider, int status, Bundle extras) {
                                        LogUtils.d("onStatusChanged", provider, status, extras);
                                    }

                                    @Override
                                    public void onProviderDisabled(@NonNull String provider) {
                                        LogUtils.w("onProviderDisabled", provider);
                                        locationManager.removeUpdates(this);
                                        //用着用着突然关掉了
                                        int i = count.decrementAndGet();
                                        if(i <=0){
                                            emitter.onComplete();
                                        }
                                    }

                                    @Override
                                    public void onProviderEnabled(@NonNull String provider) {
                                        LogUtils.d("onProviderEnabled", provider);
                                    }
                                    //todo looper
                                }, handlerThread.getLooper());
                            }
                        });
                    }
                });

        Observable<Location>  gmsObservable = null;
        if(isGmsAvaiable(context)){
            Context finalContext1 = context;
            count.incrementAndGet();
             gmsObservable = Observable.create(new ObservableOnSubscribe<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Location> emitter) throws Exception {

                    LocationServices.getFusedLocationProviderClient(finalContext1)
                            .requestLocationUpdates(new LocationRequest()
                            .setExpirationDuration(timeOut)
                            .setNumUpdates(1)
                            .setMaxWaitTime(timeOut), new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult result) {
                            LogUtils.i("gms result", result);
                            if (result != null && result.getLocations() != null && !result.getLocations().isEmpty()) {
                                Location location = result.getLocations().get(0);
                                emitter.onNext(location);
                            }
                            int i = count.decrementAndGet();
                            if(i <=0){
                                emitter.onComplete();
                            }
                        }

                    }, handlerThread.getLooper());
                }
            });
        }
        Observable<Location> finalObservable = providersObservable;
        if(gmsObservable != null){
            //zip、concat 、merge区别: https://blog.csdn.net/mwthe/article/details/82780193   concat有顺序,而merge不限制先后顺序
            //flatMap,zip,Merge区别 https://blog.csdn.net/wds1181977/article/details/90041686
            finalObservable = Observable.merge(providersObservable,gmsObservable);
        }

        finalObservable.timeout(15, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Location>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Location location) {
                        LogUtils.i("onNext",location);
                        listener.onEachLocationChanged(location,"");

                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        LogUtils.w("onError",e);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            handlerThread.quitSafely();
                        }
                    }

                    @Override
                    public void onComplete() {
                        LogUtils.i("onComplete");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            handlerThread.quitSafely();
                        }
                    }
                });

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



    private Location getResultSafe(Task<Location> task) {
        try {
            return task.getResult();
        } catch (Throwable throwable) {
            LogUtils.w("dd", throwable);
        }
        return null;
    }





    static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*;
        return client.;*/
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


