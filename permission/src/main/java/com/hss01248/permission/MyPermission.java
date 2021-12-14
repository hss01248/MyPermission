package com.hss01248.permission;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.blankj.utilcode.util.UtilsTransActivity;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Deprecated
 class MyPermission {

    public static void requestByMostEffort(String permission, IPermissionDialog dialogBeforeRequest,
                                           IPermissionDialog dialogAfterDenied,PermissionUtils.FullCallback callback){
        List<String> permissions = new ArrayList<>();
        permissions.add(permission);
        if(PermissionUtils.isGranted(permission)){
            callback.onGranted(permissions);
            return;
        }
        if(dialogBeforeRequest != null ){
            dialogBeforeRequest.show(false,permissions,new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    requestPermission(permission,true,dialogAfterDenied,true,callback);
                }

                @Override
                public void onNegtivite() {
                    List<String> permissions = new ArrayList<>();
                    permissions.add(permission);
                    callback.onDenied(new ArrayList<>(),permissions);
                }
            });
        }else {
            requestPermission(permission,false,dialogAfterDenied,true,callback);
        }




    }

    private static void requestPermission(String permission,
                                          boolean hasShowBeforeDialog,
                                          IPermissionDialog dialogAfterDenied,
                                          boolean needRetry,
                                          PermissionUtils.FullCallback callback){
        //如何知道此处有系统弹窗?
        long sysPermissionDialogShowTime = System.currentTimeMillis();
        PermissionUtils.permission(permission)
             /*   .rationale(new PermissionUtils.OnRationaleListener() {
                    @Override
                    public void rationale(@NonNull UtilsTransActivity activity, @NonNull ShouldRequest shouldRequest) {
                        new AlertDialog.Builder(ActivityUtils.getTopActivity())
                                .setTitle("rationale")
                                .setMessage("rationale 需要xx权限来继续:\n"+ permission)
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        shouldRequest.again(true);

                                    }
                                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                callbackIfDenied(permission,false,callback);

                            }
                        }).show();
                    }
                })*/
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        callback.onGranted(granted);
                    }
                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        if(needRetry){
                            long duration = System.currentTimeMillis() - sysPermissionDialogShowTime;
                            boolean hasShowSysDialog = duration > 1000;
                            //小于1s,说明没有显示系统权限弹窗,直接调用了denied
                            if(deniedForever.contains(permission)){
                                showPermissionDialog(permission,hasShowBeforeDialog,hasShowSysDialog,dialogAfterDenied,true,callback);
                            }else if(denied.contains(permission)){
                                showPermissionDialog(permission,hasShowBeforeDialog,hasShowSysDialog,dialogAfterDenied,false,callback);
                            }
                        }else {
                            callback.onDenied(deniedForever, denied);
                        }
                    }
                }).request();
    }

    private static void showPermissionDialog(String permission,
                                             boolean hasShowBeforeDialog,
                                             boolean hasShowSysDialog,
                                             IPermissionDialog dialogAfterDenied,
                                             boolean deniedForever,
                                             PermissionUtils.FullCallback callback) {
        if(dialogAfterDenied == null){
            if(deniedForever){
                requestGoSettings(permission, callback);
            }else {
                callbackIfDenied(permission, deniedForever, callback);
            }
            return;
        }

        //此处判断有问题,无法指定beforedialog_->系统权限弹窗->after弹窗  直接是否有系统权限弹窗.
        if(hasShowBeforeDialog && !hasShowSysDialog && deniedForever){
            requestGoSettings(permission, callback);
            return;
        }
        List<String> permissions = new ArrayList<>();
        permissions.add(permission);
        dialogAfterDenied.show(false,permissions,new IPermissionDialogBtnClickListener() {
            @Override
            public void onPositive() {
                if(deniedForever){
                    requestGoSettings(permission, callback);
                }else {
                    requestPermission(permission,true,dialogAfterDenied, deniedForever, callback);
                }
            }

            @Override
            public void onNegtivite() {
                callbackIfDenied(permission, deniedForever, callback);
            }
        });
    }

    private static void callbackIfDenied(String permission, boolean deniedForever, PermissionUtils.FullCallback callback) {
        List<String> permissions = new ArrayList<>();
        permissions.add(permission);
        if (deniedForever) {
            callback.onDenied(permissions, new ArrayList<>());
        } else {
            callback.onDenied(new ArrayList<>(), permissions);
        }
    }

    private static void requestGoSettings(String permission, PermissionUtils.FullCallback callback) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                List<String> permissions = new ArrayList<>();
                permissions.add(permission);
                if (PermissionUtils.isGranted(permission)) {
                    callback.onGranted(permissions);
                } else {
                    callback.onDenied(permissions, new ArrayList<>());
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                List<String> permissions = new ArrayList<>();
                permissions.add(permission);
                callback.onDenied(permissions, new ArrayList<>());
            }
        });
    }


}
