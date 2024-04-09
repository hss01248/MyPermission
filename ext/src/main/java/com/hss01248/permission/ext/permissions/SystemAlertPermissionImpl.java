package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.blankj.utilcode.util.AppUtils;
import com.hss01248.permission.ext.ExtHelper;
import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption todo
 * @Author hss
 * @Date 23/02/2022 15:53
 * @Version 1.0
 */
public class SystemAlertPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(activity.getApplicationContext());
        }
        return true;
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        //Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        //intent.setData(Uri.parse("package:" + AppUtils.getAppPackageName()));
        return getWindowPermissionIntent(activity);
    }

    /**
     * 获取悬浮窗权限设置界面意图
     */
    static Intent getWindowPermissionIntent(Context context) {
        Intent intent = null;
        if (ExtHelper.isAndroid6()) {
            intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            // 在 Android 11 加包名跳转也是没有效果的，官方文档链接：
            // https://developer.android.google.cn/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
            intent.setData(ExtHelper.getPackageNameUri(context));
        }

        if (intent == null || !ExtHelper.areActivityIntent(context, intent)) {
            intent = ExtHelper.getApplicationDetailsIntent(context);
        }
        return intent;
    }
}
