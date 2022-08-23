package com.hss01248.location;

import android.content.Context;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.concurrent.TimeUnit;

/**
 * @Despciption todo
 * @Author hss
 * @Date 23/08/2022 20:00
 * @Version 1.0
 */
public class GmsLocationUtil {

   public static boolean isGmsAvaiable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        /*;
        return client.;*/
    }

    public static void hasGmsGranted(Context context, IGmsSettingsStateCallback callback){
       try {
           GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                   .addApi(LocationServices.API).build();
           googleApiClient.connect();

           LocationRequest locationRequest = LocationRequest.create();
           locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
           locationRequest.setInterval(10000);
           locationRequest.setFastestInterval(10000 / 2);

           LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
           boolean[] haveCallbacked = new boolean[]{false};
           //超时处理
           ThreadUtils.executeBySingleWithDelay(new ThreadUtils.SimpleTask<Object>() {
               @Override
               public Object doInBackground() throws Throwable {
                   return null;
               }

               @Override
               public void onSuccess(Object result) {
                   if(haveCallbacked[0]){
                       return;
                   }
                   haveCallbacked[0] = true;
                   LogUtils.w("gms 判断状态超时,辣鸡gms: LocationServices.SettingsApi.checkLocationSettings");
                   callback.timeout();
                   //getLocation(context, silent, timeout, showBeforeRequest, showAfterRequest, false,asQuickAsPossible,useLastKnownLocation ,true,callback);
               }
           },1500, TimeUnit.MILLISECONDS);

           PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
           // 有的手机TMD这里不回调,也不抛异常, 辣鸡GMS
           result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
               @Override
               public void onResult(@NonNull LocationSettingsResult result) {
                   if(haveCallbacked[0]){
                       return;
                   }
                   haveCallbacked[0] = true;
                   if (result == null) {
                       callback.error();
                       return;
                   }

                   final Status status = result.getStatus();
                   switch (status.getStatusCode()) {
                       case LocationSettingsStatusCodes.SUCCESS:
                           //已经打开/允许
                           callback.open();
                           break;
                       case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                           //需要发起请求
                           callback.needRequest();
                           break;
                       default:
                           //出错,开不了
                           callback.error();
                           break;
                   }
               }
           });
       }catch (Throwable throwable){
           LogUtils.w(throwable);
           callback.error();
       }

    }

    public interface IGmsSettingsStateCallback{
       void open();
       void close(String msg);

       default void timeout(){
           close("time out");
       }
       default void needRequest(){
           close("need request");
       }
       default void error(){
           close("unusable");
       }
    }
}
