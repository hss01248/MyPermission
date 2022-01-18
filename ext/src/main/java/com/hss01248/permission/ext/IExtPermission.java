package com.hss01248.permission.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * // 申请安装包权限
 *         //.permission(Permission.REQUEST_INSTALL_PACKAGES)
 *         // 申请悬浮窗权限
 *         //.permission(Permission.SYSTEM_ALERT_WINDOW)
 *         // 申请通知栏权限
 *         //.permission(Permission.NOTIFICATION_SERVICE)
 *         // 申请系统设置权限
 *         //.permission(Permission.WRITE_SETTINGS)
 *
 *         //查看应用使用情况权限
 *
 *         //所有文件管理权限
 *
 */
public interface IExtPermission {

    String name();

    boolean checkPermission(Activity activity);

    Intent intentToRequestPermission(Activity activity);
}
