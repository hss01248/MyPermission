package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import com.hss01248.permission.ext.IExtPermission;
import com.hss01248.permission.ext.MyBaseAccessibilityService;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/15/24 9:19 AM
 * @Version 1.0
 */
public class AccessibilityPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return "Accessibility";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        return MyBaseAccessibilityService.isAccessibilityServiceEnabled();
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }
}
