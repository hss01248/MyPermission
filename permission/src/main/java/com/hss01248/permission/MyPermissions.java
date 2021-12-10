package com.hss01248.permission;

import android.content.Intent;
import android.util.Log;

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
import java.util.Collections;
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
                                          final boolean hasShowBeforeDialog,
                                          IPermissionDialog dialogAfterDenied,
                                          boolean needRetry,
                                          PermissionUtils.FullCallback callback,
                                          List<String> permissionsList,
                                          String... permission){
        //如何知道此处有系统弹窗?
        long sysPermissionDialogShowTime = System.currentTimeMillis();
        PermissionUtils.permission(permission)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {

                        //todo 单个请求允许也会返回到这里,蛋疼
                        if(PermissionUtils.isGranted(permission)){
                            LogUtils.d("onGranted",granted);
                            callback.onGranted(granted);
                        }

                    }
                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        long duration = System.currentTimeMillis() - sysPermissionDialogShowTime;
                        boolean hasShowSysDialog = duration > 1000;
                        //小于1s,说明没有显示系统权限弹窗,直接调用了denied,且全部是deniedForever,或者没有在manifest里声明权限
                        LogUtils.d("onDenied",deniedForever,denied);
                        //denied 包含deniedForever
                        if(hasShowSysDialog){
                            //
                            LogUtils.w("有弹出系统半弹窗请求权限");
                            if(!needRetry){
                                LogUtils.w("二次请求后拒绝");
                                callback.onDenied(deniedForever, denied);
                            }else {
                                checkIfRetryAfterFirstTimeRequest(hasShowBeforeDialog,hasShowSysDialog, true,
                                        dialogAfterDenied,deniedForever,denied,callback,permissionsList,permission);
                            }
                        }else {
                            LogUtils.w("小于1s,说明没有显示系统权限弹窗,直接调用了denied,且全部是deniedForever,或者没有在manifest里声明权限",
                                    "相当于没有真正发起请求,那么这里就发起一次:");
                            //如果是没有弹出前置弹窗,那么就试着弹出拒绝后弹窗:
                            if(needRetry){
                                if(!hasShowBeforeDialog){
                                    LogUtils.i("如果是没有弹出前置弹窗,那么就试着弹出拒绝后弹窗:");
                                    if(dialogAfterDenied != null){
                                        dialogAfterDenied.show(permissionsList, new IPermissionDialogBtnClickListener() {
                                            @Override
                                            public void onPositive() {
                                                goSettingsFirstTime(denied, hasShowSysDialog, permission, callback, permissionsList, needRetry, true, dialogAfterDenied);
                                            }

                                            @Override
                                            public void onNegtivite() {
                                                callback.onDenied(deniedForever, denied);
                                            }
                                        });
                                    }else {
                                        LogUtils.w("没有配置后置弹窗,且是第一次请求,那么直接去settings");
                                        goSettingsFirstTime(denied, hasShowSysDialog, permission, callback, permissionsList, needRetry, true, dialogAfterDenied);
                                    }
                                }else {
                                    LogUtils.w("已经显示过前置弹窗,且是第一次请求,那么直接去settings");
                                    goSettingsFirstTime(denied, hasShowSysDialog, permission, callback, permissionsList, needRetry, true, dialogAfterDenied);
                                }
                            }else {
                                //重试
                                LogUtils.w("应该不会走到");
                                goSettingsFirstTime(denied, hasShowSysDialog, permission, callback, permissionsList, needRetry, hasShowBeforeDialog, dialogAfterDenied);
                            }


                        }

                    }
                }).request();
    }

    private static void goSettingsFirstTime(@NonNull List<String> denied, boolean hasShowSysDialog, String[] permission,
                                            PermissionUtils.FullCallback callback, List<String> permissionsList, boolean needRetry,
                                            boolean hasShowBeforeDialog, IPermissionDialog dialogAfterDenied) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                if (PermissionUtils.isGranted(permission)) {
                    callback.onGranted(permissionsList);
                } else {
                    List<String> realDeniedForever = new ArrayList<>();
                    //找出真正没有允许的权限
                    for (String s : permission) {
                        if(!PermissionUtils.isGranted(s)){
                            realDeniedForever.add(s);
                        }
                    }
                    if(!needRetry){
                        callback.onDenied(realDeniedForever, realDeniedForever);
                    }else {
                        //相当于显示了系统权限弹窗
                        checkIfRetryAfterFirstTimeRequest(hasShowBeforeDialog,true, needRetry,
                                dialogAfterDenied,realDeniedForever,denied,callback,permissionsList,permission);
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

    private static void checkIfRetryAfterFirstTimeRequest(
                                             boolean hasShowBeforeDialog,
                                             boolean hasShowSysDialog,
                                             boolean isFirstTimeResult,
                                             IPermissionDialog dialogAfterDenied,
                                             List<String> deniedForever,
                                             List<String> denied,
                                             PermissionUtils.FullCallback callback,
                                             List<String> permissionsList,
                                             String... permission) {


        //准备重试
        if(dialogAfterDenied == null){
            callback.onDenied(deniedForever, denied);
        }else {
            dialogAfterDenied.show(permissionsList,new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    if(deniedForever.size() == denied.size()){
                        LogUtils.i("如果所有都是永久拒绝,那么直接去请求全量权限");
                        requestPermission(hasShowBeforeDialog,dialogAfterDenied,false,callback,permissionsList,permission);
                        return;
                    }
                    LogUtils.i("如果有暂时拒绝的,那么先申请暂时拒绝的,再申请永久拒绝的");
                    requstPermissionSencondTime(denied, permission, callback, permissionsList, deniedForever);


                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(deniedForever, denied);
                }
            });
        }
    }

    private static void requstPermissionSencondTime(List<String> denied, String[] permission,
                                                    PermissionUtils.FullCallback callback,
                                                    List<String> permissionsList, List<String> deniedForever) {
        String[] theDenied = new String[denied.size()];
        for (int i = 0; i < denied.size(); i++) {
            theDenied[i] = denied.get(i);
        }
        PermissionUtils.permission(theDenied)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        if(PermissionUtils.isGranted(theDenied)){
                            LogUtils.d("2 申请成功一部分后,再去申请那些需要开设置的",granted);
                            Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
                            StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
                                @Override
                                public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                                    if (PermissionUtils.isGranted(permission)) {
                                        callback.onGranted(permissionsList);
                                    } else {
                                        List<String> realDeniedForever = new ArrayList<>();
                                        //找出真正没有允许的权限
                                        for (String s : permission) {
                                            if(!PermissionUtils.isGranted(s)){
                                                realDeniedForever.add(s);
                                            }
                                        }
                                        LogUtils.w("2 找出真正没有允许的权限",realDeniedForever);
                                        callback.onDenied(realDeniedForever, realDeniedForever);

                                    }
                                }

                                @Override
                                public void onActivityNotFound(Throwable e) {
                                    e.printStackTrace();
                                    callback.onDenied(deniedForever, new ArrayList<>());
                                }
                            });
                        }

                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever2, @NonNull List<String> denied) {
                        deniedForever2.addAll(deniedForever);
                        LogUtils.w("再次申请权限被拒",deniedForever2,denied);
                        callback.onDenied(deniedForever2, denied);
                    }
                }).request();
    }
}
