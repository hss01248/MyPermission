package com.hss01248.permission;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.util.ArrayList;
import java.util.List;

public class MyPermissions {

    public static void requestByMostEffort( IPermissionDialog dialogBeforeRequest,
                                           IPermissionDialog dialogAfterDenied,PermissionUtils.FullCallback callback,String... permission){
        List<String> permissionsList = new ArrayList<>();
        for (String s : permission) {
            permissionsList.add(s);
        }
        if(PermissionUtils.isGranted(permission)){
            callback.onGranted(permissionsList);
            return;
        }
        if(dialogBeforeRequest != null ){
            dialogBeforeRequest.show(permissionsList,new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    requestPermission(true,dialogAfterDenied,true,callback,permission);
                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(new ArrayList<>(),permissionsList);
                }
            });
        }else {
            requestPermission(false,dialogAfterDenied,true,callback,permission);
        }




    }

    private static void requestPermission(
                                          boolean hasShowBeforeDialog,
                                          IPermissionDialog dialogAfterDenied,
                                          boolean needRetry,
                                          PermissionUtils.FullCallback callback,String... permission){
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
                                showPermissionDialog(hasShowBeforeDialog,hasShowSysDialog,dialogAfterDenied,true,callback,permission);
                            }else if(denied.contains(permission)){
                                showPermissionDialog(hasShowBeforeDialog,hasShowSysDialog,dialogAfterDenied,false,callback,permission);
                            }
                        }else {
                            callback.onDenied(deniedForever, denied);
                        }
                    }
                }).request();
    }

    private static void showPermissionDialog(
                                             boolean hasShowBeforeDialog,
                                             boolean hasShowSysDialog,
                                             IPermissionDialog dialogAfterDenied,
                                             boolean deniedForever,
                                             PermissionUtils.FullCallback callback,String... permission) {
        if(dialogAfterDenied == null){
            if(deniedForever){
                requestGoSettings( callback,permission);
            }else {
                callbackIfDenied( deniedForever, callback,permission);
            }
            return;
        }

        //此处判断有问题,无法指定beforedialog_->系统权限弹窗->after弹窗  直接是否有系统权限弹窗.
        if(hasShowBeforeDialog && !hasShowSysDialog && deniedForever){
            requestGoSettings(callback,permission);
            return;
        }
        List<String> permissions = new ArrayList<>();
        for (String s : permission) {
            permissions.add(s);
        }
        dialogAfterDenied.show(permissions,new IPermissionDialogBtnClickListener() {
            @Override
            public void onPositive() {
                if(deniedForever){
                    requestGoSettings( callback,permission);
                }else {
                    requestPermission(true,dialogAfterDenied, deniedForever, callback,permission);
                }
            }

            @Override
            public void onNegtivite() {
                callbackIfDenied( deniedForever, callback,permission);
            }
        });
    }

    private static void callbackIfDenied(boolean deniedForever, PermissionUtils.FullCallback callback,String... permission) {
        List<String> permissions = new ArrayList<>();
        for (String s : permission) {
            permissions.add(s);
        }
        if (deniedForever) {
            callback.onDenied(permissions, new ArrayList<>());
        } else {
            callback.onDenied(new ArrayList<>(), permissions);
        }
    }

    private static void requestGoSettings(PermissionUtils.FullCallback callback,String... permission) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                List<String> permissions = new ArrayList<>();
                for (String s : permission) {
                    permissions.add(s);
                }
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
                for (String s : permission) {
                    permissions.add(s);
                }
                callback.onDenied(permissions, new ArrayList<>());
            }
        });
    }


}
