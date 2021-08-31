package com.hss01248.permission;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.util.ArrayList;
import java.util.List;

public class MyPermission {

    public static void requestByMostEffort(String permission, IPermissionDialog dialogAfterDenied,PermissionUtils.FullCallback callback){
        if(PermissionUtils.isGranted(permission)){
            List<String> permissions = new ArrayList<>();
            permissions.add(permission);
            callback.onGranted(permissions);
            return;
        }

        requestPermission(permission,dialogAfterDenied,true,callback);


    }

    private static void requestPermission(String permission,IPermissionDialog dialogAfterDenied,boolean needRetry,PermissionUtils.FullCallback callback){
        PermissionUtils.permission(permission)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        callback.onGranted(granted);
                    }
                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        if(needRetry){
                            if(deniedForever.contains(permission)){
                                showPermissionDialog(permission,dialogAfterDenied,true,callback);
                            }else if(denied.contains(permission)){
                                showPermissionDialog(permission,dialogAfterDenied,false,callback);
                            }
                        }else {
                            callback.onDenied(deniedForever, denied);
                        }
                    }
                }).request();
    }

    private static void showPermissionDialog(String permission,IPermissionDialog dialogAfterDenied,
                                             boolean deniedForever,PermissionUtils.FullCallback callback) {
        if(dialogAfterDenied == null){
            List<String> permissions = new ArrayList<>();
            permissions.add(permission);
            if(deniedForever){
                callback.onDenied(permissions,new ArrayList<>());
            }else {
                callback.onDenied(new ArrayList<>(),permissions);
            }
            return;
        }

        dialogAfterDenied.show(new IPermissionDialogBtnClickListener() {
            @Override
            public void onPositive() {
                if(deniedForever){
                    Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
                    StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
                        @Override
                        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                            if(PermissionUtils.isGranted(permission)){
                                List<String> permissions = new ArrayList<>();
                                permissions.add(permission);
                               callback.onGranted(permissions);
                                return;
                            }
                        }
                        @Override
                        public void onActivityNotFound(Throwable e) {

                        }
                    });
                }else {
                    requestPermission(permission,dialogAfterDenied, deniedForever, callback);
                }
            }

            @Override
            public void onNegtivite() {
                List<String> permissions = new ArrayList<>();
                permissions.add(permission);
                if(deniedForever){
                    callback.onDenied(permissions,new ArrayList<>());
                }else {
                    callback.onDenied(new ArrayList<>(),permissions);
                }
            }
        });
    }


}
