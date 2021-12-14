package com.hss01248.location;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.permission.DefaultPermissionDialog;
import com.hss01248.permission.IPermissionDialog;
import com.hss01248.permission.MyPermissions;


import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 08/12/2021 14:53
 * @Version 1.0
 */
public class LocationUtil {


    /**
     * 默认版 拒绝权限后有一次挽回行为
     * @param context
     * @param callback
     */
    public static void getLocation(Context context, MyLocationCallback callback){
        getLocation(context,10000,null,
                new DefaultPermissionDialog(),callback);
    }

    /**
     * 完全配置版
     * @param context
     * @param timeout
     * @param dialogBeforeRequest
     * @param dialogAfterDenied
     * @param callback
     */
    public static void getLocation(Context context, int timeout, IPermissionDialog dialogBeforeRequest,
                                   IPermissionDialog dialogAfterDenied, MyLocationCallback callback) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (SilentLocationUtil.isLocationEnabled(locationManager)) {
            checkPermission(context,timeout,dialogBeforeRequest,dialogAfterDenied, callback);
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                .setTitle(com.hss01248.permission.R.string.mypermission_location_title)
                .setMessage(com.hss01248.permission.R.string.mypermission_location_switch)
                .setPositiveButton(com.hss01248.permission.R.string.mypermission_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), locationIntent, new ActivityResultListener() {
                            @Override
                            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                                if (!SilentLocationUtil.isLocationEnabled(locationManager)) {
                                    callback.onFailed(2, "location switch off");
                                    return;
                                }
                                checkPermission(context,timeout,dialogBeforeRequest,dialogAfterDenied, callback);
                            }

                            @Override
                            public void onActivityNotFound(Throwable e) {

                            }
                        });

                    }
                }).setNegativeButton(com.hss01248.permission.R.string.mypermission_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onFailed(2, "location switch off");

                    }
                }).create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();


    }

    private static void checkPermission(Context context,int timeout, IPermissionDialog dialogBeforeRequest,
                                        IPermissionDialog dialogAfterDenied,  MyLocationCallback callback) {
        if (PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            doRequestLocation(context,timeout, callback);
        } else {
            MyPermissions.requestByMostEffort(
                    true,
                    false,
                    new PermissionUtils.FullCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> granted) {
                            doRequestLocation(context,timeout, callback);
                        }

                        @Override
                        public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                            callback.onFailed(1, "no permission");
                        }
                    },Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private static void doRequestLocation(Context context,int timeout, MyLocationCallback callback) {
        new SilentLocationUtil().getLocation(context,timeout, callback);
    }
}
