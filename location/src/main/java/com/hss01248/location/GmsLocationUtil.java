package com.hss01248.location;

import android.content.Context;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

/**
 * @Despciption GMS location utility
 * @Author hss
 * @Date 23/08/2022 20:00
 * @Version 1.1
 */
public class GmsLocationUtil {

    public static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public static void hasGmsGranted(Context context, IGmsSettingsStateCallback callback) {
        try {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            LocationServices.getSettingsClient(context)
                    .checkLocationSettings(builder.build())
                    .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                            try {
                                LocationSettingsResponse response = task.getResult(ApiException.class);
                                // All location settings are satisfied. The client can initialize location
                                // requests here.
                                callback.open();
                            } catch (ApiException exception) {
                                switch (exception.getStatusCode()) {
                                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                        // Location settings are not satisfied. But could be fixed by showing the user a
                                        // dialog.
                                        callback.needRequest();
                                        break;
                                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                        // Location settings are not satisfied. However, we have no way to fix the
                                        // settings.
                                        callback.error();
                                        break;
                                    default:
                                        callback.error();
                                        break;
                                }
                            }
                        }
                    });
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
            callback.error();
        }
    }

    public interface IGmsSettingsStateCallback {
        void open();

        void close(String msg);

        default void timeout() {
            close("time out");
        }

        default void needRequest() {
            close("need request");
        }

        default void error() {
            close("unusable");
        }
    }
}
