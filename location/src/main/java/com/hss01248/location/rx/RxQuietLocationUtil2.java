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
import androidx.core.util.Pair;

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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
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
 * <p>
 * 饱和式定位工具类: 同时发起gps,network,passive定位
 * <p>
 * gms管理的蛋疼之处:
 * 当手机有gms时,需要定位开关+gms详细位置开关 同时开启,才能定位,如果gms详细位置开关关闭,则定位必失败,onlocationChanged无回调
 * 但同样一台手机上谷歌,高德能定位成功,是什么原理?
 * <p>
 * <p>
 * <p>
 * https://juejin.cn/post/7016937919533285407
 */
public class RxQuietLocationUtil2 {

    public int getTimeOut() {
        return timeOut;
    }

    public RxQuietLocationUtil2 setTimeOut(int timeOut) {
        this.timeOut = timeOut;
        return this;
    }

    int timeOut = 10000;//10s

    HandlerThread handlerThread;
    boolean useCacheIfFail = true;

    public Observable<Pair<String,Location>> getLocation(Context context) {
       return getLocation(context, timeOut,true);
    }


    public Observable<Pair<String,Location>> getLocation(Context context, int timeoutMills,boolean useCacheIfFail) {
        timeOut = timeoutMills;
        this.useCacheIfFail = useCacheIfFail;
        context = context.getApplicationContext();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
       /* MyLocationCallback listener = listener0;
        if (noPermission(context)) {
            listener.onFailed(1, "no permission");
            return;
        }

        if (locationManager == null) {
            listener.onFailed(6, "locationManager is null");
            return;
        }
        boolean locationEnabled = isLocationEnabled(locationManager);

        if (!locationEnabled) {
            listener.onFailed(2, "location switch off");
            return;
        }*/

        List<String> providers = locationManager.getProviders(true);
        LogUtils.i("getAllProviders-enabled:", providers);

        handlerThread = new HandlerThread("silentlocation");
        handlerThread.start();

        long start = System.currentTimeMillis();
        int size = providers.size();


       return  byFlatMap(context,  locationManager, providers, start, size);

    }

    private Observable<Pair<String,Location>> byFlatMap(Context context,   LocationManager locationManager, List<String> providers, long start, int size) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        List<Pair<String,Location>> pairList = new ArrayList<>();

       return  Observable.fromIterable(providers)
                .flatMap(new Function<String, ObservableSource<Pair<String,Location>>>() {
                    @Override
                    public ObservableSource<Pair<String,Location>> apply(@io.reactivex.annotations.NonNull String provider) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<Pair<String,Location>>() {
                            @Override
                            @SuppressLint("MissingPermission")
                            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String,Location>> emitter) throws Exception {

                                LogUtils.i("flatMap", "requestSingleUpdate-" + provider);
                                //locationManager.getCurrentLocation(provider,);
                                locationManager.requestSingleUpdate(provider, new LocationListener() {
                                    @Override
                                    public void onLocationChanged(@NonNull Location location) {
                                        LogUtils.d("onLocationChanged", location, provider, "耗时(ms):", (System.currentTimeMillis() - start));
                                        saveLocation2(location);
                                        emitter.onNext(new Pair<>(provider,location));
                                        locationManager.removeUpdates(this);
                                        //todo 重要: flatMap内部的Observable.create: 因为创建了多个Observable,必须每个Observable都调用onComplete,才能触发最终observer的onComplete
                                        emitter.onComplete();
                                    }

                                    @Override
                                    public void onStatusChanged(String provider, int status, Bundle extras) {
                                        LogUtils.d("onStatusChanged", provider, status, extras);
                                    }

                                    @Override
                                    public void onProviderDisabled(@NonNull String provider) {
                                        LogUtils.w("onProviderDisabled", provider);
                                        locationManager.removeUpdates(this);
                                        //用着用着突然关掉了,会触发这里
                                        emitter.onComplete();
                                    }

                                    @Override
                                    public void onProviderEnabled(@NonNull String provider) {
                                        LogUtils.d("onProviderEnabled", provider);
                                    }
                                }, handlerThread.getLooper());
                            }
                            //要这里指定subscribeOn(Schedulers.io()),才能让每个Observable在不同的线程工作
                        }).subscribeOn(Schedulers.io());
                    }
                }).mergeWith(Observable.create(new ObservableOnSubscribe<Pair<String,Location>>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Pair<String,Location>> emitter) throws Exception {
                            //if-else的代码直接在Observable内部实现
                            if (isGmsAvaiable(context)) {
                                LocationServices.getFusedLocationProviderClient(context)
                                        .requestLocationUpdates(new LocationRequest()
                                                .setExpirationDuration(timeOut)
                                                .setNumUpdates(1)
                                                .setMaxWaitTime(timeOut), new LocationCallback() {
                                            @Override
                                            public void onLocationResult(LocationResult result) {
                                                LogUtils.i("gms result", result);
                                                if (result != null && result.getLocations() != null && !result.getLocations().isEmpty()) {
                                                    Location location = result.getLocations().get(0);
                                                    saveLocation2(location);
                                                    emitter.onNext(new Pair<>("gms",location));
                                                }
                                                emitter.onComplete();
                                            }

                                        }, handlerThread.getLooper());
                            } else {
                                //如果没有onNext,那就直接发onComplete,否则最后Observer无法调用onComplete
                                emitter.onComplete();
                            }

                        }})
                .subscribeOn(Schedulers.io()))
                .timeout(timeOut, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread());

    }

    private void dealOnFail(@io.reactivex.annotations.NonNull Throwable e, List<Pair<String,Location>> pairList,MyLocationCallback listener) {
        String msg = e.getClass().getSimpleName();
        if (e instanceof TimeoutException) {
            msg =  "timeout after " + timeOut + "ms";
        }
        Pair<String, Location> mostAcurLocation2 = getMostAcurLocation2(pairList);
        if(mostAcurLocation2 != null){
            listener.onSuccess(mostAcurLocation2.second, mostAcurLocation2.first +" "+ msg);
        }else {
            //是否读缓存:
            if(useCacheIfFail){
                Map<String, String> ext = new HashMap<>();
                ext.put("msg", msg);
                Location cache = getFromCache(ext, msg);
                if (cache == null) {
                    listener.onFailed(8,msg);
                } else {
                    listener.onSuccess(cache, ext.get("msg"));
                }
            }else {
                listener.onFailed(9,msg);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //java.util.concurrent.TimeoutException
            //如果超时,关闭handlerThread,那么定位器无法回调: LocationManager: thread not runable, ignore msg, state:TERMINATED, pkg:com.hss01248.mypermissiondemo

            Observable.just(1L)
                    .subscribeOn(Schedulers.io())
                    .delay(15, TimeUnit.SECONDS)
                    .doOnNext(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            LogUtils.w("15s延时已经到了,关闭handlerThread");
                            handlerThread.quitSafely();
                        }
                    }).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@io.reactivex.annotations.NonNull Long aLong) {

                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void byMerge(Context context, int timeoutMills, MyLocationCallback listener, LocationManager locationManager, List<String> providers, long start, int size) {
        Observable<Location>[] locations = new Observable[size];
        for (int i = 0; i < size; i++) {
            String provider = providers.get(i);
            locations[i] = Observable.create(new ObservableOnSubscribe<Location>() {
                @Override
                @SuppressLint("MissingPermission")
                public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Location> emitter) throws Exception {

                    LogUtils.i("Observable.create", "requestSingleUpdate-" + provider);
                    //locationManager.getCurrentLocation(provider,);
                    locationManager.requestSingleUpdate(provider, new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            LogUtils.d("onLocationChanged", location, provider, "耗时(ms):", (System.currentTimeMillis() - start));
                            saveLocation2(location);
                            emitter.onNext(location);
                            locationManager.removeUpdates(this);
                            emitter.onComplete();
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
                            emitter.onComplete();
                        }

                        @Override
                        public void onProviderEnabled(@NonNull String provider) {
                            LogUtils.d("onProviderEnabled", provider);
                        }
                        //todo looper
                    }, handlerThread.getLooper());
                }
            }).subscribeOn(Schedulers.io());
        }
        Observable<Location> merge = Observable.mergeArray(locations);

        Observable<Location> gmsObservable = null;
        if (isGmsAvaiable(context)) {
            Context finalContext1 = context;
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
                                        saveLocation2(location);
                                        emitter.onNext(location);
                                    }
                                    emitter.onComplete();
                                }

                            }, handlerThread.getLooper());
                }
            }).subscribeOn(Schedulers.io());
        }
        if (gmsObservable != null) {
            merge = Observable.merge(gmsObservable, merge);
        }
        merge.timeout(timeoutMills, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Location>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Location location) {
                        LogUtils.i("onNext", location);
                        listener.onEachLocationChanged(location, "");
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        LogUtils.w("onError", e);
                        if (e instanceof TimeoutException) {
                            listener.onFailed(3, "timeout after " + timeOut + "ms");
                        } else {
                            listener.onFailed(1, e.getClass().getSimpleName());
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            //java.util.concurrent.TimeoutException
                            handlerThread.quitSafely();
                        }
                    }

                    @Override
                    public void onComplete() {
                        LogUtils.i("onComplete");
                        //todo listener.onSuccess();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            handlerThread.quitSafely();
                        }
                    }
                });
    }

    private void saveLocation2(Location location) {
        if (location != null) {
            LocationSync.save(location.getLatitude(), location.getLongitude());
            LocationSync.saveLocation(location);
        }
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



    private Pair<String,Location> getMostAcurLocation2(List<Pair<String,Location>> pairs){
        if(pairs == null || pairs.isEmpty()){
            return null;
        }
        for (Pair<String, Location> pair : pairs) {
            if (LocationManager.GPS_PROVIDER.equals(pair.first)) {
                return pair;
            }
            if ("gms".equals(pair.first)) {
                return pair;
            }
            if (LocationManager.PASSIVE_PROVIDER.equals(pair.first)) {
                return pair;
            }
            if (LocationManager.NETWORK_PROVIDER.equals(pair.first)) {
                return pair;
            }
        }
        return null;
    }


    static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*;
        return client.;*/
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

    private static boolean noPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }
}


