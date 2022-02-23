package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import com.blankj.utilcode.util.AppUtils;
import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption todo
 * @Author hss
 * @Date 23/02/2022 15:30
 * @Version 1.0
 */
public class StorageManagerPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return "Manager All Storage";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return  Environment.isExternalStorageManager();
        }
        return false;
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        Intent intent =  new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        //intent.setPackage(AppUtils.getAppPackageName());
        return intent;
    }
}
