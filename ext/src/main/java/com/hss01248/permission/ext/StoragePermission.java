package com.hss01248.permission.ext;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.hss01248.permission.MyPermissions;
import com.hss01248.permission.ext.permissions.StorageManagerPermissionImpl;

import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 09/04/2024 16:35
 * @Version 1.0
 */
public class StoragePermission {
    public static void askWritePermission(IExtPermissionCallback callback){
        Activity activity = ActivityUtils.getTopActivity();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                && Environment.isExternalStorageLegacy())) {
            MyPermissions.requestByMostEffort(false, true,
                    new PermissionUtils.FullCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> granted) {
                            callback.onGranted("storage");
                        }

                        @Override
                        public void onDenied(@NonNull List<String> deniedForever,
                                             @NonNull List<String> denied) {
                            callback.onDenied("storage");
                        }
                    }, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }else{
            MyPermissionsExt.askPermission(activity, new StorageManagerPermissionImpl(),
                    new IExtPermissionCallback() {
                        @Override
                        public void onGranted(String name) {
                            MyPermissions.requestByMostEffort(false, true,
                                    new PermissionUtils.FullCallback() {
                                        @Override
                                        public void onGranted(@NonNull List<String> granted) {
                                            callback.onGranted("storage");
                                        }

                                        @Override
                                        public void onDenied(@NonNull List<String> deniedForever,
                                                             @NonNull List<String> denied) {
                                            callback.onDenied("storage");
                                        }
                                    },
                                    Manifest.permission.READ_EXTERNAL_STORAGE);

                        }

                        @Override
                        public void onDenied(String name) {
                            callback.onDenied("storage");
                        }
                    });
        }


    }
}
