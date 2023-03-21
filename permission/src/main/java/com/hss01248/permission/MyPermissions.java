package com.hss01248.permission;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyPermissions {

    public static void setCanAcceptOnlyCoarseLocationPermission(boolean canAcceptOnlyCoarseLocationPermission) {
        MyPermissions.canAcceptOnlyCoarseLocationPermission = canAcceptOnlyCoarseLocationPermission;
    }

    public static boolean canAcceptOnlyCoarseLocationPermission = false;

    public static boolean isStateInManifest(String permission) {
        try {
            if (TextUtils.isEmpty(permission)) {
                return false;
            }
            PackageInfo packageInfo = Utils.getApp().getPackageManager().getPackageInfo(AppUtils.getAppPackageName(), PackageManager.GET_PERMISSIONS);
            //Utils.getApp().getPackageManager()
            LogUtils.i("permissioninfo", packageInfo.permissions, packageInfo.requestedPermissions);
            if (packageInfo.requestedPermissions != null) {
                for (String requestedPermission : packageInfo.requestedPermissions) {
                    if (permission.equals(requestedPermission)) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.w(e);
        }
        return false;
    }

    public static void setDefaultPermissionDialog(IPermissionDialog defaultPermissionDialog) {
        MyPermissions.defaultPermissionDialog = defaultPermissionDialog;
    }

    public static IPermissionDialog defaultPermissionDialog = new DefaultPermissionDialog();
    public static IAlertDialog defaultAlertDialog = new DefaultAlertDialogImpl();

    public static void request(PermissionUtils.FullCallback callback,
                               String... permission) {
        requestByMostEffort(false, false, callback, permission);

    }

    public static void requestByMostEffort(boolean showBeforeRequest, boolean showAfterRequest,
                                           PermissionUtils.FullCallback callback,
                                           String... permission) {
        new MyPermissions().requestByMostEffort(null, showBeforeRequest, showAfterRequest, callback, permission);
    }

    IPermissionDialog dialog = defaultPermissionDialog;
    boolean showBeforeRequest;
    boolean showAfterRequest;
    PermissionUtils.FullCallback callback;
    String[] permissions;
    List<String> permissionsList;
    boolean isGoSettingFirstTime;

    public void requestByMostEffort(IPermissionDialog alertDialog,
                                    boolean showBeforeRequest, boolean showAfterRequest,
                                    PermissionUtils.FullCallback callback,
                                    String... permission) {
        if (alertDialog != null) {
            dialog = alertDialog;
        }
        this.showAfterRequest = showAfterRequest;
        this.showBeforeRequest = showBeforeRequest;
        this.callback = callback;
        this.permissions = permission;


        LogUtils.i("首次请求权限", permission);
        permissionsList = new ArrayList<>();
        for (String s : permission) {
            permissionsList.add(s);
        }
        if (PermissionUtils.isGranted(permission)) {
            callback.onGranted(permissionsList);
            return;
        }
        //经过了一次请求后,这两个判断才准确
        List<String> deniedForeverList = getDeniedForeverList(permission);
        List<String> deniedTemporary = getDeniedTemporary(permission);
        // LogUtils.i("首次请求权限,原始权限状态(永久拒绝,暂时拒绝)", deniedForeverList, deniedTemporary);


        if (showBeforeRequest) {
            dialog.show(false, permissionsList, new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    requestPermissionFirstTime();
                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(deniedForeverList, deniedTemporary);
                }
            });
        } else {
            requestPermissionFirstTime();
        }


    }

    private static List<String> getDeniedForeverList(String[] permission) {
        List<String> deniedForever = new ArrayList<>();
        for (String per : permission) {
            if (!PermissionUtils.isGranted(per) && !ActivityCompat.shouldShowRequestPermissionRationale(ActivityUtils.getTopActivity(), per)) {
                //todo 没有请求过,也会走这里,还是蛋疼
                deniedForever.add(per);
            }
        }
        return deniedForever;
    }

    private static List<String> getDeniedTemporary(String[] permission) {
        List<String> deniedForever = new ArrayList<>();
        for (String per : permission) {
            if (!PermissionUtils.isGranted(per) && ActivityCompat.shouldShowRequestPermissionRationale(ActivityUtils.getTopActivity(), per)) {
                deniedForever.add(per);
            }
        }
        return deniedForever;
    }

    private boolean isOnlyLocation() {
        return permissionsList.size() == 2
                && permissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                && permissionsList.contains(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissionFirstTime() {

        List<String> deniedForeverList = getDeniedForeverList(permissions);
        List<String> deniedTemporary = getDeniedTemporary(permissions);
        LogUtils.i("首次请求权限,原始权限状态(永久拒绝,暂时拒绝)", deniedForeverList, deniedTemporary);

        long sysPermissionDialogShowTime = System.currentTimeMillis();
        PermissionUtils.permission(permissions)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        LogUtils.d("onGranted", granted);
                        //单个请求允许也会返回到这里,蛋疼
                        if (PermissionUtils.isGranted(permissions)) {
                            LogUtils.d("onGranted", "所有权限均被允许,流程结束");
                            callback.onGranted(granted);
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        LogUtils.d("onDenied0", deniedForever, denied);
                        boolean hasShowSysDialog = dealDeniedList(deniedForever, denied, sysPermissionDialogShowTime);
                        //如果是部分允许,部分拒绝,那么最终回调这里
                        //小于1s,说明没有显示系统权限弹窗,直接调用了denied,且全部是deniedForever,或者没有在manifest里声明权限
                        LogUtils.d("onDenied", deniedForever, denied);

                        if (hasShowSysDialog) {
                            //如何区分一个本身永久拒绝,一个本次允许的情况?

                            Collection intersection = CollectionUtils.intersection(deniedForeverList, deniedForever);
                            //精确和模糊定位同时请求,且拒绝了精确,选择了模糊定位权限的情况
                            if (canAcceptOnlyCoarseLocationPermission
                                    && Build.VERSION.SDK_INT > Build.VERSION_CODES.R
                                    && isOnlyLocation()
                                    && PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                                if (!showAfterRequest) {
                                    callback.onDenied(deniedForever, denied);
                                    return;
                                }
                            }

                            if (intersection.isEmpty()) {
                                LogUtils.i("deniedForeverList和deniedForever的交集为空,则说明权限状态都是本次处理的,这时就开始下一步");
                                checkIfRetryAfterFirstTimeRequest(deniedForever, denied);
                            } else {
                                LogUtils.w("deniedForeverList和deniedForever的交集不为空,则说明有权限本身就永久拒绝,这时就跳去设置页面");
                                LogUtils.w("但不适用于精确和模糊定位同时请求,且拒绝了精确,选择了模糊定位权限的情况,此时如果选择了not show after dialog,也会走到这里,需要做兼容处理");
                                goSettingFirstTimeWrapper(false, deniedForever);
                            }

                        } else {
                            if (canAcceptOnlyCoarseLocationPermission
                                    && Build.VERSION.SDK_INT > Build.VERSION_CODES.R
                                    && isOnlyLocation()
                                    && PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    && !PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                if (!showAfterRequest && !showBeforeRequest) {
                                    callback.onDenied(deniedForever, denied);
                                    return;
                                }
                            }

                            isGoSettingFirstTime = true;
                            LogUtils.w("第一次_全部都是永久拒绝_直接gosetting_小于1s,说明没有显示系统权限弹窗,直接调用了denied,且全部是deniedForever,或者没有在manifest里声明权限");
                            //LogUtils.w("但不适用于精确和模糊定位同时请求,且拒绝了精确,选择了模糊定位权限的情况,此时如果选择了not show after dialog,也会走到这里,需要做兼容处理");
                            goSettingFirstTimeWrapper(true, deniedForever);
                        }
                    }
                }).request();


    }

    private void goSettingFirstTimeWrapper(boolean canShowAfterIfSettingFailed,
                                           List<String> deniedForeverList) {
        if (!showBeforeRequest || !canShowAfterIfSettingFailed) {
            LogUtils.i("第一次_需要gosettings_没有弹出前置弹窗,那么就试着弹出拒绝后弹窗, 用来引导:");
            dialog.show(true, permissionsList, new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    goSettingsFirstTime(canShowAfterIfSettingFailed);
                }

                @Override
                public void onNegtivite() {
                    callback.onDenied(deniedForeverList, new ArrayList<>());
                }
            });
        } else {
            LogUtils.w("第一次_需要gosettings_已经显示过前置弹窗,且是第一次请求,那么直接去settings");
            goSettingsFirstTime(canShowAfterIfSettingFailed);
        }
    }

    /**
     * 对定义进行转换: denied仅包含暂时拒绝的,deniedForever才是永久拒绝的
     *
     * @param deniedForever
     * @param denied
     * @param sysPermissionDialogShowTime
     */
    private static boolean dealDeniedList(List<String> deniedForever, List<String> denied, long sysPermissionDialogShowTime) {
        long duration = System.currentTimeMillis() - sysPermissionDialogShowTime;
        boolean hasShowSysDialog = duration > 500;
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

    private void goSettingsFirstTime(boolean canShowAfterIfSettingFailed) {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                LogUtils.d("第一次go setting回来,检验权限情况: ");
                if (PermissionUtils.isGranted(permissions)) {
                    LogUtils.i("全部都允许了");
                    callback.onGranted(permissionsList);
                } else {
                    List<String> deniedForeverList = getDeniedForeverList(permissions);
                    List<String> deniedTemporary = getDeniedTemporary(permissions);
                    LogUtils.i("还有这些没有被允许:", deniedForeverList, deniedTemporary);
                    //相当于显示了系统权限弹窗
                    if (canShowAfterIfSettingFailed) {
                        checkIfRetryAfterFirstTimeRequest(deniedForeverList, deniedTemporary);
                    } else {
                        LogUtils.w("第一次有系统弹窗,有go setting,则不再处理后置dialog");
                        callback.onDenied(deniedForeverList, deniedTemporary);
                    }

                    //
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                List<String> deniedForeverList = getDeniedForeverList(permissions);
                List<String> deniedTemporary = getDeniedTemporary(permissions);
                callback.onDenied(deniedForeverList, deniedTemporary);
            }
        });
    }

    private void goSettingsSecondTime() {
        Intent intent = IntentUtils.getLaunchAppDetailsSettingsIntent(Utils.getApp().getPackageName(), false);
        StartActivityUtil.goOutAppForResult(ActivityUtils.getTopActivity(), intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                LogUtils.d("第2次go setting回来,检验权限情况: ");
                if (PermissionUtils.isGranted(permissions)) {
                    LogUtils.i("全部都允许了");
                    callback.onGranted(permissionsList);
                } else {
                    List<String> deniedForeverList = getDeniedForeverList(permissions);
                    List<String> deniedTemporary = getDeniedTemporary(permissions);
                    LogUtils.i("还有这些没有被允许:", deniedForeverList, deniedTemporary);
                    //相当于显示了系统权限弹窗
                    callback.onDenied(deniedForeverList, deniedTemporary);
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {
                e.printStackTrace();
                List<String> deniedForeverList = getDeniedForeverList(permissions);
                List<String> deniedTemporary = getDeniedTemporary(permissions);
                callback.onDenied(deniedForeverList, deniedTemporary);
            }
        });
    }

    private void checkIfRetryAfterFirstTimeRequest(@NonNull List<String> deniedForeverList, @NonNull List<String> deniedTemporary) {
        //准备重试
       /* List<String> deniedForeverList = getDeniedForeverList(permissions);
        List<String> deniedTemporary = getDeniedTemporary(permissions);*/
        if (isGoSettingFirstTime) {
            //如果第一次已经是直接跳设置页,那么回来后没有全部给权限,就不再挽回了,即使配置了showAfterRequest也不挽回
            showAfterRequest = false;
        }
        if (!showAfterRequest) {
            LogUtils.w("没有配置dialogAfterDenied,直接返回拒绝,流程结束", deniedForeverList, deniedTemporary);
            callback.onDenied(deniedForeverList, deniedTemporary);
        } else {

            dialog.show(deniedTemporary.isEmpty(), permissionsList, new IPermissionDialogBtnClickListener() {
                @Override
                public void onPositive() {
                    if (deniedTemporary.isEmpty()) {
                        LogUtils.i("重试开始_都是永久拒绝,那么再次gosetting");
                        goSettingsSecondTime();
                    } else {
                        LogUtils.i("有暂时拒绝的,那么先申请暂时拒绝的(requestPermission),再申请永久拒绝的(go setting)");
                        requstPermissionSencondTime(deniedTemporary, deniedForeverList);
                    }
                }

                @Override
                public void onNegtivite() {
                    LogUtils.w("dialogAfterDenied,点击了取消,流程结束", deniedForeverList, deniedTemporary);
                    callback.onDenied(deniedForeverList, deniedTemporary);
                }
            });
        }
    }

    private void requstPermissionSencondTime(List<String> denied, List<String> deniedForever) {

        String[] theDenied = new String[denied.size()];
        for (int i = 0; i < denied.size(); i++) {
            theDenied[i] = denied.get(i);
        }
        LogUtils.i("第二次请求权限", deniedForever, denied);
        long start = System.currentTimeMillis();
        PermissionUtils.permission(theDenied)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        if (PermissionUtils.isGranted(theDenied)) {
                            LogUtils.d("2 申请成功一部分后,再去申请那些需要开设置的", granted);
                            if (deniedForever.isEmpty()) {
                                if (PermissionUtils.isGranted(permissions)) {
                                    callback.onGranted(permissionsList);
                                } else {
                                    LogUtils.e("2 校对错误", "deniedForever 为空,但不是所有权限都被允许");
                                }
                                return;
                            }
                            dialog.show(true, permissionsList, new IPermissionDialogBtnClickListener() {
                                @Override
                                public void onPositive() {
                                    goSettingsSecondTime();
                                }

                                @Override
                                public void onNegtivite() {
                                    callback.onDenied(deniedForever, new ArrayList<>());
                                }
                            });
                        } else {
                            LogUtils.w("2 第二次_非全部允许");
                        }

                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever2, @NonNull List<String> denied) {
                       /* deniedForever2.addAll(deniedForever);
                        dealDeniedList(deniedForever2,denied,start);*/
                        List<String> deniedForeverList = getDeniedForeverList(permissions);
                        List<String> deniedTemporary = getDeniedTemporary(permissions);
                        LogUtils.w("2 再次申请权限被拒", deniedForever2, denied);
                        callback.onDenied(deniedForeverList, deniedTemporary);
                    }
                }).request();
    }
}
