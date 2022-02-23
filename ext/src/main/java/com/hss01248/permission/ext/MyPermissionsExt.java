package com.hss01248.permission.ext;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.AppUtils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

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
