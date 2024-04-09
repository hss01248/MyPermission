package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;

import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption todo
 * @Author hss
 * @Date 09/04/2024 15:49
 * @Version 1.0
 */
public class ManageMediaPermission implements IExtPermission {
    @Override
    public String name() {
        return "manager media";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return MediaStore.canManageMedia(activity);
        }else{
            return  true;
        }
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
