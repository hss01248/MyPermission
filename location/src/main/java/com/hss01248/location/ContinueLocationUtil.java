package com.hss01248.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

/**
 * 连续定位工具类
 */
public class ContinueLocationUtil {

    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback gmsCallback;
    private List<LocationListener> listeners = new ArrayList<>();
    private MyLocationCallback externalCallback;
    private long startTime;

    public void startLocation( long interval, MyLocationCallback callback) {
        this.externalCallback = callback;
        this.startTime = System.currentTimeMillis();
        Context context = Utils.getApp();

        if (QuietLocationUtil.isGmsAvaiable(context)) {
            startGmsLocation(context, interval);
        } else {
            startSystemLocation(context, interval);
        }
    }

    @SuppressLint("MissingPermission")
    private void startGmsLocation(Context context, long interval) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(interval / 2)
                .build();

        gmsCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    reportLocation(location, "gms");
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, gmsCallback, Looper.getMainLooper());
        LogUtils.i("ContinueLocationUtil", "Started GMS continuous location updates");
    }

    @SuppressLint("MissingPermission")
    private void startSystemLocation(Context context, long interval) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            externalCallback.onFailed(LocationErrorCode.LOCATION_MANAGER_NULL, "LocationManager is null");
            return;
        }

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                reportLocation(location, location.getProvider());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                LogUtils.w("onProviderDisabled", provider);
                externalCallback.onFailed(199, provider+":provider disabled", false);

            }
        };

        listeners.add(listener);

        if (locationManager.isProviderEnabled("fused")) {
            locationManager.requestLocationUpdates("fused", interval, 0, listener,
                    Looper.getMainLooper());
        }else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, listener,
                    Looper.getMainLooper());
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0, listener,
                    Looper.getMainLooper());
        }
        LogUtils.i("ContinueLocationUtil", "Started System continuous location updates");
    }

    private void reportLocation(Location location, String provider) {
        if (location == null)
            return;
        long now = System.currentTimeMillis();
        long costOfUpdate = 0; // Continuous updates don't easily track single-shot cost
        long costFromStart = now - startTime;

        LogUtils.d("ContinueLocationUtil", "onLocationChanged", provider, location);
        LocationSync.putToCache(location, provider, false, costOfUpdate, costFromStart);
        externalCallback.onEachLocationChanged(location, provider, costOfUpdate, costFromStart);
       // externalCallback.onSuccess(location, "continuous update from " + provider);
    }

    public void stopLocation() {
        if (fusedLocationProviderClient != null && gmsCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(gmsCallback);
            gmsCallback = null;
        }
        if (locationManager != null) {
            for (LocationListener listener : listeners) {
                locationManager.removeUpdates(listener);
            }
            listeners.clear();
        }
        LogUtils.i("ContinueLocationUtil", "Stopped continuous location updates");
    }
}
