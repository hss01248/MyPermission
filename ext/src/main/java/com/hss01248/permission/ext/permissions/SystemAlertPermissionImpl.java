package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.blankj.utilcode.util.AppUtils;
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
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + AppUtils.getAppPackageName()));
        return intent;
    }
}
