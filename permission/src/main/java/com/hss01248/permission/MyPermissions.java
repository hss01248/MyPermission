package com.hss01248.permission;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.util.ArrayList;
import java.util.List;

public class MyPermissions {



    public static void requestByMostEffort( IPermissionDialog dialogBeforeRequest,
                                           IPermissionDialog dialogAfterDenied,
                                            PermissionUtils.FullCallback callback,
                                            String... permission){
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
                    requestPermission(true,dialogAfterDenied,true,callback,permissionsList,permission);
                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(new ArrayList<>(),permissionsList);
                }
            });
        }else {
            requestPermission(false,dialogAfterDenied,true,callback,permissionsList,permission);
        }




    }

    private static void requestPermission(
                                          boolean hasShowBeforeDialog,
                                          IPermissionDialog dialogAfterDenied,
                                          boolean isFirstTimeRequest,
                                          PermissionUtils.FullCallback callback,
                                          List<String> permissionsList,
                                          String... permission){
        //如何知道此处有系统弹窗?
        long sysPermissionDialogShowTime = System.currentTimeMillis();
        PermissionUtils.permission(permission)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        LogUtils.d("onGranted",granted);
                        callback.onGranted(granted);
                    }
                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        LogUtils.d("onDenied",deniedForever,denied);
                        if(!isFirstTimeRequest){
                            callback.onDenied(deniedForever, denied);
                        }else {
                            long duration = System.currentTimeMillis() - sysPermissionDialogShowTime;
                            boolean hasShowSysDialog = duration > 1000;
                            //小于1s,说明没有显示系统权限弹窗,直接调用了denied
                            checkIfShowAfterDeniedDialog(hasShowBeforeDialog,hasShowSysDialog, true,
                                    dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);

                        }
                    }
                }).request();
    }

    private static void checkIfShowAfterDeniedDialog(
                                             boolean hasShowBeforeDialog,
                                             boolean hasShowSysDialog,
                                             boolean isFirstTimeResult,
                                             IPermissionDialog dialogAfterDenied,
                                             List<String> deniedForever,
                                             List<String> denied,
                                             PermissionUtils.FullCallback callback,
                                             List<String> permissionsList,
                                             String... permission) {
        //当没有显示系统权限弹窗,     则永久拒绝肯定不为空,普通拒绝肯定为空
        if( !hasShowSysDialog ){
            //&& !deniedForever.isEmpty()
            LogUtils.i("check !deniedForever.isEmpty()  && denied.isEmpty()");
            //如果之前显示了before的dialog,那么就跳去系统设置页
            //或者没有设置dialog after,则
            if(hasShowBeforeDialog){
                requestGoSettings(hasShowBeforeDialog,hasShowSysDialog,isFirstTimeResult,
                        dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
            }else {
                if(dialogAfterDenied == null){
                    requestGoSettings(hasShowBeforeDialog,hasShowSysDialog,isFirstTimeResult,
                            dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
                }else {
                    dialogAfterDenied.show(permissionsList,new IPermissionDialogBtnClickListener() {
                        @Override
                        public void onPositive() {
                            requestGoSettings(hasShowBeforeDialog,hasShowSysDialog,isFirstTimeResult,
                                    dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
                        }

                        @Override
                        public void onNegtivite() {
                            callback.onDenied(deniedForever, denied);
                        }
                    });
                }


            }
        }
        if(denied.isEmpty() && !deniedForever.isEmpty()){
            //弹出了系统权限弹窗,但被用户点击了永久拒绝,那么要挽回一下:
            if(dialogAfterDenied == null){
                //没有配置挽回弹窗,那就不挽回
                callback.onDenied(deniedForever,denied);
            }else {
                //如果是重试,则不再弹窗
                if(!isFirstTimeResult){
                    callback.onDenied(deniedForever,denied);
                }else {
                    showAfterDeniedDialog(hasShowBeforeDialog,hasShowSysDialog,isFirstTimeResult,
                            dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
                }
            }

        }else if(!denied.isEmpty() && deniedForever.isEmpty()){
            //全部都是普通拒绝,没有永久拒绝,则再次申请权限即可
            if(dialogAfterDenied == null){
                //没有配置挽回弹窗,那就不挽回
                callback.onDenied(deniedForever,denied);
            }else {
                //如果是重试,则不再弹窗
                if(!isFirstTimeResult){
                    callback.onDenied(deniedForever,denied);
                }else {
                    showAfterDeniedDialog(hasShowBeforeDialog,hasShowSysDialog,isFirstTimeResult,
                            dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
                }
            }
        }else if(!denied.isEmpty() && !deniedForever.isEmpty()){
            //两者都有,那么
        }




    }

    private static void showAfterDeniedDialog(boolean hasShowBeforeDialog,
                                              boolean hasShowSysDialog,
                                              boolean isCurrentRetryResult,
                                              IPermissionDialog dialogAfterDenied,
                                              List<String> deniedForever,
                                              List<String> denied,
                                              PermissionUtils.FullCallback callback,
                                              List<String> permissionsList,
                                              String[] permission) {

        dialogAfterDenied.show(permissionsList,new IPermissionDialogBtnClickListener() {
            @Override
            public void onPositive() {

            }

            @Override
            public void onNegtivite() {

            }
        });

    }

    private static void requestGoSettings(boolean hasShowBeforeDialog,
                                          boolean hasShowSysDialog,
                                          boolean isFirstTimeUIRequest,
                                          IPermissionDialog dialogAfterDenied,
                                          List<String> deniedForever,
                                          List<String> denied,
                                          PermissionUtils.FullCallback callback,
                                          List<String> permissionsList,
                                          String... permission) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                if (PermissionUtils.isGranted(permission)) {
                    callback.onGranted(permissionsList);
                } else {
                    if(isCurrentRetryResult){
                        callback.onDenied(permissionsList, new ArrayList<>());
                    }else {
                        if(hasShowBeforeDialog);
                    }

                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                callback.onDenied(permissionsList, new ArrayList<>());
            }
        });
    }


}
