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
 */
public class SilentLocationUtil {

    public int getTimeOut() {
        return timeOut;
    }

    public SilentLocationUtil setTimeOut(int timeOut) {
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


    public void getLocation(Context context, int timeoutMills, MyLocationCallback listener) {
        timeOut = timeoutMills;
        /*if (executors == null) {
            executors = Executors.newCachedThreadPool();
        }*/
        context = context.getApplicationContext();

        MyLocationCallback finalListener = listener;

        listener = new MyLocationCallback() {
            @Override
            public void onFailed(int type, String msg) {
                finalListener.onFailed(type, msg);
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
        boolean locationEnabled = isLocationEnabled2(locationManager);

        if (!locationEnabled) {
            listener.onFailed(2, "location switch off");
            return;
        }
        //LogUtils.w("getAllProviders:" + ObjParser.parseObj(locationManager.getAllProviders()));

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
                        callback(map, timeOut + "s timeout", finalListener3);
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
                    requestByType("fused", locationManager, map, countSet, finalListener1);
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

    private boolean isLocationEnabled2(LocationManager locationManager) {
        try {
            boolean locationEnabled = LocationManagerCompat.isLocationEnabled(locationManager);
            if (locationEnabled) {
                return locationEnabled;
            }
            boolean locationEnabled3 = isLocationEnabled3();
            if (locationEnabled3) {
                return true;
            }

            List<String> allProviders = locationManager.getAllProviders();
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
            LogUtils.w("gps", throwable);
            return false;
        }

    }

    boolean isLocationEnabled3() {
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
                                LogUtils.w("gms", "get last location:" + lastLocation1);
                                map.put(lastLocation1.getProvider(), lastLocation1);
                            }
                        }
                    });

                    fusedLocationProviderClient.requestLocationUpdates(new LocationRequest()
                            .setExpirationDuration(timeOut)
                            .setNumUpdates(1)
                            .setMaxWaitTime(timeOut), new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult result) {
                            LogUtils.d("gmsresult", result);
                            if (result != null && result.getLocations() != null && !result.getLocations().isEmpty()) {
                                Location location = result.getLocations().get(0);
                                countSet.remove("gms");
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
                                                           LogUtils.w("gms location not available");
                                                           countSet.remove("gms");
                                                           return;
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

    private boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*;
        return client.;*/
    }

    @SuppressLint("MissingPermission")
    private void requestByType(String provider, LocationManager locationManager, Map<String, Location> map, Set<String> countSet, MyLocationCallback listener) {
        if (locationManager.isProviderEnabled(provider)) {
            // locationManager.requestSingleUpdate(buildCriteria(context,locationManager),);
            try {
                countSet.add(provider);
                LogUtils.d(countSet);
                @SuppressLint("MissingPermission") Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    //LogUtils.d(lastKnownLocation);
                    map.put(lastKnownLocation.getProvider(), lastKnownLocation);
                }
                locationManager.requestSingleUpdate(provider, new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        LogUtils.d("onLocationChanged", location);
                        countSet.remove(provider);
                        onEnd(location, map, countSet, listener);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        LogUtils.d("onStatusChanged", provider, status, extras);
                    }
                }, Looper.myLooper());
            } catch (Throwable throwable) {
                countSet.remove(provider);
                throwable.printStackTrace();
            }
        }
    }


    private void onEnd(Location location, Map<String, Location> map, Set<String> count, MyLocationCallback listener) {
        //LogUtils.d(location);
        if (location != null) {
            map.put(location.getProvider(), location);
        }
        LogUtils.d("silent", "count:" + count.size());
        LogUtils.d(count);

        if (count.size() == 0) {
            callback(map, "complete normal", listener);
        }

    }

    public static Location getFromCache(Map ext, String msg) {
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

    private void callback(Map<String, Location> map, String msg, MyLocationCallback listener) {
        LogUtils.d(map);
        if (hasEnd) {
            LogUtils.w("callback when has end");
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
            Location cache = LocationSync.getLocation();
            if (cache == null) {
                double lon = LocationSync.getLongitude();
                double lat = LocationSync.getLatitude();
                if (lon == 0 && lat == 0) {
                    listener.onFailed(5, "no cache and " + msg);
                } else {
                    cache = new Location(LocationManager.PASSIVE_PROVIDER);
                    cache.setLongitude(LocationSync.getLongitude());
                    cache.setLatitude(LocationSync.getLatitude());
                    listener.onSuccess(location, "from disk cache");
                }
            } else {
                listener.onSuccess(location, "from memory cache");
            }
        }
        try {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                if (handler != null) {
                    handler.removeCallbacks(timeoutRun);
                    if (gmsRunnable != null) {
                        handler.removeCallbacks(gmsRunnable);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Looper.myLooper().quitSafely();
                } else {
                    Looper.myLooper().quit();
                }
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


