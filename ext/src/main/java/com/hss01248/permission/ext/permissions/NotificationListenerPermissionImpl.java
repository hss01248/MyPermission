package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import com.hss01248.permission.ext.IExtPermission;

import java.util.Set;

/**
 * @Despciption https://blog.csdn.net/aiynmimi/article/details/77540153
 * @Author hss
 * @Date 23/02/2022 16:08
 * @Version 1.0
 */
public class NotificationListenerPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return "NotificationListener";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        return isNotificationListenerEnabled(activity.getApplicationContext());
    }

    protected boolean isNotificationListenerEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }


    @Override
    public Intent intentToRequestPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        return intent;
    }
}
