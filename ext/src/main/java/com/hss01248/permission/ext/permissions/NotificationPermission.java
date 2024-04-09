package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.permission.ext.ExtHelper;
import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption todo
 * @Author hss
 * @Date 18/01/2022 10:04
 * @Version 1.0
 */
public class NotificationPermission implements IExtPermission {
    @Override
    public String name() {
        return "Notification";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        return NotificationManagerCompat.from(Utils.getApp()).areNotificationsEnabled();
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
       /* Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setPackage(AppUtils.getAppPackageName());*/
        return getNotifyPermissionIntent(activity);
    }



    /**
     * 获取通知栏权限设置界面意图
     */
    static Intent getNotifyPermissionIntent(Context context) {
        Intent intent = null;
        if (ExtHelper.isAndroid8()) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            //intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
        }
        if (intent == null || !ExtHelper.areActivityIntent(context, intent)) {
            intent = ExtHelper.getApplicationDetailsIntent(context);
        }
        return intent;
    }
}
