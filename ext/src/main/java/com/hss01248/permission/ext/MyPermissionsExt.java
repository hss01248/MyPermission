package com.hss01248.permission.ext;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.permission.ext.permissions.NotificationPermission;

import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 17/01/2022 18:59
 * @Version 1.0
 */
public class MyPermissionsExt {

    public static void askPermission(Activity activity,IExtPermission permissionImpl,IExtPermissionCallback callback){
        boolean havePermission = permissionImpl.checkPermission(activity);
        if(havePermission){
            callback.onGranted(permissionImpl.name());
        }else {
            if(permissionImpl instanceof NotificationPermission){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    if(PermissionUtils.isGranted(Manifest.permission.POST_NOTIFICATIONS)){
                        callback.onGranted(permissionImpl.name());
                        return;
                    }

                    PermissionUtils.permission(Manifest.permission.POST_NOTIFICATIONS)
                            .callback(new PermissionUtils.SimpleCallback() {
                                @Override
                                public void onGranted() {
                                    //回调有bug
                                    //LogUtils.d("onGranted-->");
                                    callback.onGranted(permissionImpl.name());
                                }

                                @Override
                                public void onDenied() {
                                    //LogUtils.d("onDenied-->");
                                    callback.onDenied(permissionImpl.name());
                                }
                            }).request();
                    return;
                }
            }
            //todo 弹窗引导
            //Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
            Intent intent = permissionImpl.intentToRequestPermission(activity);
            //intent.setData(packageURI);
            //intent.setPackage(AppUtils.getAppPackageName());
            StartActivityUtil.goOutAppForResult(activity, intent, new ActivityResultListener() {
                @Override
                public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                    boolean havePermission2 = permissionImpl.checkPermission(activity);
                    if(havePermission2){
                        callback.onGranted(permissionImpl.name());
                    }else {
                        callback.onDenied(permissionImpl.name());
                    }
                }

                @Override
                public void onActivityNotFound(Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
