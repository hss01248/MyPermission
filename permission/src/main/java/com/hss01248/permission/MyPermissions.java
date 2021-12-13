package com.hss01248.permission;

import android.Manifest;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

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

     IPermissionDialog dialog =  new DefaultPermissionDialog();
    boolean showBeforeRequest;
    boolean showAfterRequest;
    PermissionUtils.FullCallback callback;
    String[] permission;
    List<String> permissionsList ;
    public  void requestByMostEffort(IPermissionDialog alertDialog,
                                           boolean showBeforeRequest,boolean showAfterRequest,
                                           PermissionUtils.FullCallback callback,
                                           String... permission) {
        if(alertDialog != null){
            dialog = alertDialog;
        }
        this.showAfterRequest = showAfterRequest;
        this.showBeforeRequest = showBeforeRequest;
        this.callback = callback;
        this.permission = permission;


        LogUtils.i("首次请求权限",permission);
        permissionsList = new ArrayList<>();
        for (String s : permission) {
            permissionsList.add(s);
        }
        if (PermissionUtils.isGranted(permission)) {
            callback.onGranted(permissionsList);
            return;
        }
        List<String> deniedForeverList = getDeniedForeverList(permission);
        List<String> deniedTemporary = getDeniedTemporary(permission);
        LogUtils.i("首次请求权限,原始权限状态(永久拒绝,暂时拒绝)",deniedForeverList,deniedTemporary);


        if (showBeforeRequest) {
            dialog.show(getDeniedForeverList(permission).isEmpty(),permissionsList, new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    requestPermissionFirstTime(dialog,true,   callback, permissionsList, permission);
                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(deniedForeverList, deniedTemporary);
                }
            });
        } else {
            requestPermissionFirstTime(dialog,false, callback, permissionsList, permission);
        }


    }

    private static List<String> getDeniedForeverList(String[] permission) {
        List<String> deniedForever = new ArrayList<>();
        for (String per : permission) {
            if(!PermissionUtils.isGranted(per) && !ActivityCompat.shouldShowRequestPermissionRationale(ActivityUtils.getTopActivity(), per)){
                deniedForever.add(per);
            }
        }
        return deniedForever;
    }

    private static List<String> getDeniedTemporary(String[] permission) {
        List<String> deniedForever = new ArrayList<>();
        for (String per : permission) {
            if(!PermissionUtils.isGranted(per) && ActivityCompat.shouldShowRequestPermissionRationale(ActivityUtils.getTopActivity(), per)){
                deniedForever.add(per);
            }
        }
        return deniedForever;
    }

    private  void requestPermissionFirstTime(
            /*IPermissionDialog dialogBeforeRequest,
            final boolean hasShowBeforeDialog,
            PermissionUtils.FullCallback callback,
            List<String> permissionsList,
            String... permission*/) {

        List<String> deniedForeverList = getDeniedForeverList(permission);
        List<String> deniedTemporary = getDeniedTemporary(permission);
        if(deniedTemporary.isEmpty()){
            LogUtils.i("第一次_全部都是永久拒绝_直接gosetting");
            goSettingFirstTimeWrapper(dialogBeforeRequest,hasShowBeforeDialog,  callback, permissionsList, deniedForeverList, permission);
        }else {
            LogUtils.i("第一次_有暂时拒绝的,先弹系统弹窗,然后看是否goSetting");
            long sysPermissionDialogShowTime = System.currentTimeMillis();
            String[] permissions2 = new String[deniedTemporary.size()];
            for (int i = 0; i < deniedTemporary.size(); i++) {
                permissions2[i] = deniedTemporary.get(i);
            }
            PermissionUtils.permission(permissions2)
                    .callback(new PermissionUtils.FullCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> granted) {
                            //单个请求允许也会返回到这里,蛋疼
                            if (PermissionUtils.isGranted(permissions2)) {
                                LogUtils.d("onGranted", granted);
                                if(deniedForeverList.isEmpty()){
                                    LogUtils.d("第一次_所有权限均已允许,没有初始永久拒绝的权限,返回成功",permission);
                                    callback.onGranted(granted);
                                }else {
                                    LogUtils.d("第一次_请求的权限均已允许,但有初始永久拒绝的权限,继续去设置页面",deniedForeverList);
                                    goSettingFirstTimeWrapper(dialogBeforeRequest,false, dialogAfterDenied, callback, permissionsList, deniedForeverList, permission);
                                }
                            }
                        }

                        @Override
                        public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                            boolean hasShowSysDialog =  dealDeniedList(deniedForever,denied,sysPermissionDialogShowTime);
                            //如果是部分允许,部分拒绝,那么最终回调这里
                            //小于1s,说明没有显示系统权限弹窗,直接调用了denied,且全部是deniedForever,或者没有在manifest里声明权限
                            LogUtils.d("onDenied", deniedForever, denied);
                            if(deniedForeverList.isEmpty()){
                                LogUtils.d("第一次_请求的权限被拒绝了一些,此时没有初始被永久被拒绝的权限,那么开始后处理",permission);
                                checkIfRetryAfterFirstTimeRequest(false,
                                        dialogAfterDenied, deniedForever, denied, callback, permissionsList, permission);
                            }else {
                                LogUtils.d("第一次_请求的权限被拒绝了一些,但还有初始永久拒绝的权限,开始后处理",deniedForeverList);
                                checkIfRetryAfterFirstTimeRequest(false,
                                        dialogAfterDenied, deniedForever, denied, callback, permissionsList, permission);
                                //goSettingFirstTimeWrapper(dialogBeforeRequest,false, dialogAfterDenied, callback, permissionsList, deniedForeverList, permission);
                            }


                        }
                    }).request();
        }



    }

    private static void goSettingFirstTimeWrapper(IPermissionDialog dialogBeforeRequest,boolean hasShowBeforeDialog,
                                                  IPermissionDialog dialogAfterDenied, PermissionUtils.FullCallback callback,
                                                  List<String> permissionsList, List<String> deniedForeverList, String[] permission) {
        if (!hasShowBeforeDialog) {
            LogUtils.i("第一次_需要gosettings_没有弹出前置弹窗,那么就试着弹出拒绝后弹窗, 用来引导:");
            if(dialogBeforeRequest == null){
                dialogBeforeRequest = dialogAfterDenied;
            }
            if (dialogBeforeRequest != null) {
                dialogBeforeRequest.show(true,permissionsList, new IPermissionDialogBtnClickListener() {
                    @Override
                    public void onPositive() {
                        goSettingsFirstTime(deniedForeverList, permission, callback, permissionsList, true, dialogAfterDenied);
                    }

                    @Override
                    public void onNegtivite() {
                        callback.onDenied(deniedForeverList, new ArrayList<>());
                    }
                });
            } else {
                LogUtils.w("第一次_需要gosettings_没有弹出过前置弹窗,也没有配置后置弹窗,那么直接去settings,弹个吐司意思一下");
                goSettingsFirstTime(deniedForeverList, permission, callback, permissionsList, false, dialogAfterDenied);
            }
        } else {
            LogUtils.w("第一次_需要gosettings_已经显示过前置弹窗,且是第一次请求,那么直接去settings");
            goSettingsFirstTime(deniedForeverList, permission, callback, permissionsList, hasShowBeforeDialog, dialogAfterDenied);
        }
    }

    /**
     * 对定义进行转换: denied仅包含暂时拒绝的,deniedForever才是永久拒绝的
     * @param deniedForever
     * @param denied
     * @param sysPermissionDialogShowTime
     */
    private static boolean dealDeniedList(List<String> deniedForever, List<String> denied, long sysPermissionDialogShowTime) {
        long duration = System.currentTimeMillis() - sysPermissionDialogShowTime;
        boolean hasShowSysDialog = duration > 1000;
        if (!hasShowSysDialog) {
            //当一个权限没有声明在manifest里,这个权限不会被放到deniedForever里,那需要处理成deniedForever
            deniedForever.clear();
            deniedForever.addAll(denied);
            denied.clear();
        }
        for (String deniedP : deniedForever) {
            denied.remove(deniedP);
        }

        return hasShowSysDialog;
    }

    private static void goSettingsFirstTime(@NonNull List<String> denied, String[] permission,
                                            PermissionUtils.FullCallback callback, List<String> permissionsList,
                                            boolean hasShowBeforeDialog, IPermissionDialog dialogAfterDenied) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                LogUtils.d("第一次go setting回来,检验权限情况: ");
                if (PermissionUtils.isGranted(permission)) {
                    LogUtils.i("全部都允许了");
                    callback.onGranted(permissionsList);
                } else {
                    List<String> deniedForeverList = getDeniedForeverList(permission);
                    List<String> deniedTemporary = getDeniedTemporary(permission);
                    LogUtils.i("还有这些没有被允许:",deniedForeverList,deniedTemporary);
                    //相当于显示了系统权限弹窗
                    checkIfRetryAfterFirstTimeRequest(hasShowBeforeDialog,
                            dialogAfterDenied, deniedForeverList, deniedTemporary, callback, permissionsList, permission);
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                List<String> deniedForeverList = getDeniedForeverList(permission);
                List<String> deniedTemporary = getDeniedTemporary(permission);
                callback.onDenied(deniedForeverList, deniedTemporary);
            }
        });
    }

    private static void goSettingsSecondTime(@NonNull List<String> denied,  String[] permission,
                                            PermissionUtils.FullCallback callback, List<String> permissionsList) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                LogUtils.d("第2次go setting回来,检验权限情况: ");
                if (PermissionUtils.isGranted(permission)) {
                    LogUtils.i("全部都允许了");
                    callback.onGranted(permissionsList);
                } else {
                    List<String> deniedForeverList = getDeniedForeverList(permission);
                    List<String> deniedTemporary = getDeniedTemporary(permission);
                    LogUtils.i("还有这些没有被允许:",deniedForeverList,deniedTemporary);
                    //相当于显示了系统权限弹窗
                  callback.onDenied(deniedForeverList,deniedTemporary);
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                List<String> deniedForeverList = getDeniedForeverList(permission);
                List<String> deniedTemporary = getDeniedTemporary(permission);
                callback.onDenied(deniedForeverList, deniedTemporary);
            }
        });
    }

    private static void checkIfRetryAfterFirstTimeRequest(
            boolean hasShowBeforeDialog,
            IPermissionDialog dialogAfterDenied,
            List<String> deniedForever,
            List<String> denied,
            PermissionUtils.FullCallback callback,
            List<String> permissionsList,
            String... permission) {
        //准备重试
        List<String> deniedForeverList = getDeniedForeverList(permission);
        List<String> deniedTemporary = getDeniedTemporary(permission);
        if (dialogAfterDenied == null) {
            LogUtils.w("没有配置dialogAfterDenied,直接返回拒绝,流程结束",deniedForeverList,deniedTemporary);
            callback.onDenied(deniedForeverList, deniedTemporary);
        } else {
            dialogAfterDenied.show(deniedForeverList.isEmpty(),permissionsList, new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    if(deniedTemporary.isEmpty()){
                        LogUtils.i("重试开始_都是永久拒绝,那么再次gosetting");
                        goSettingsSecondTime(deniedForeverList,permission,callback,permissionsList);
                    }else {
                        LogUtils.i("有暂时拒绝的,那么先申请暂时拒绝的(requestPermission),再申请永久拒绝的(go setting)");
                        requstPermissionSencondTime(deniedTemporary, permission, callback, permissionsList, deniedForeverList,dialogAfterDenied);
                    }
                }

                @Override
                public void onNegtivite() {
                    LogUtils.w("dialogAfterDenied,点击了取消,流程结束",deniedForever,denied);
                    callback.onDenied(deniedForeverList, deniedTemporary);
                }
            });
        }
    }

    private static void requstPermissionSencondTime(List<String> denied, String[] permission,
                                                    PermissionUtils.FullCallback callback,
                                                    List<String> permissionsList, List<String> deniedForever,
                                                    IPermissionDialog dialogAfterDenied) {

        String[] theDenied = new String[denied.size()];
        for (int i = 0; i < denied.size(); i++) {
            theDenied[i] = denied.get(i);
        }
        LogUtils.i("第二次请求权限",deniedForever,denied);
        long start = System.currentTimeMillis();
        PermissionUtils.permission(theDenied)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        if (PermissionUtils.isGranted(theDenied)) {
                            LogUtils.d("2 申请成功一部分后,再去申请那些需要开设置的", granted);
                            if(deniedForever.isEmpty()){
                                if(PermissionUtils.isGranted(permission)){
                                    callback.onGranted(permissionsList);
                                }else {
                                    LogUtils.e("2 校对错误","deniedForever 为空,但不是所有权限都被允许");
                                }
                                return;
                            }

                            if(dialogAfterDenied != null){
                                dialogAfterDenied.show(true, permissionsList, new IPermissionDialogBtnClickListener() {
                                    @Override
                                    public void onPositive() {
                                        goSettingsSecondTime(deniedForever,permission,callback,permissionsList);
                                    }

                                    @Override
                                    public void onNegtivite() {
                                        callback.onDenied(deniedForever, new ArrayList<>());
                                    }
                                });
                            }else {
                                goSettingsSecondTime(deniedForever,permission,callback,permissionsList);
                            }
                        }else {
                            LogUtils.w("2 第二次_非全部允许");
                        }

                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever2, @NonNull List<String> denied) {
                       /* deniedForever2.addAll(deniedForever);
                        dealDeniedList(deniedForever2,denied,start);*/
                        List<String> deniedForeverList = getDeniedForeverList(permission);
                        List<String> deniedTemporary = getDeniedTemporary(permission);
                        LogUtils.w("2 再次申请权限被拒", deniedForever2, denied);
                        callback.onDenied(deniedForeverList, deniedTemporary);
                    }
                }).request();
    }
}
