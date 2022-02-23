package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;

import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption https://blog.csdn.net/scau_zhangpeng/article/details/88259464
 *
 * https://www.jianshu.com/p/1d16c3a04593
 *
 * 和微信一样,弹窗引导即可
 * 路径一：设置 - 应用列表 - XX应用 - 权限 - 创建桌面快捷方式
 * 路径二：设置 - 权限列表 - 创建桌面快捷方式 - xx应用
 * 路径三：自带手机管家 - 权限列表 - 创建桌面快捷方式 - xx应用
 * @Author hss
 * @Date 23/02/2022 15:54
 * @Version 1.0
 */
@Deprecated
public class DesktopShorcutPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean checkPermission(Activity activity) {
        //return new ShortcutInfoCompat.Builder().;
        return false;
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        return null;
    }
}
