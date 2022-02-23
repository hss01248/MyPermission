package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.blankj.utilcode.util.AppUtils;
import com.hss01248.openuri.OpenUri;
import com.hss01248.permission.ext.IExtPermission;

import java.io.File;

/**
 * @Despciption todo
 * @Author hss
 * @Date 18/01/2022 09:52
 * @Version 1.0
 */
public class ApkPermissionImpl implements IExtPermission {


    @Override
    public String name() {
        return "apk";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        return Build.VERSION.SDK_INT >= 26 && activity.getPackageManager().canRequestPackageInstalls();
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
            //设置这个才能
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageURI);
            return intent;
        }
        return null;
    }
}
