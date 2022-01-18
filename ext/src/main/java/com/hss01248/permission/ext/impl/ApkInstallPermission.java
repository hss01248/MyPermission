package com.hss01248.permission.ext.impl;

import com.hss01248.permission.ext.IExtPermission;

/**
 * @Despciption todo
 * @Author hss
 * @Date 17/01/2022 19:01
 * @Version 1.0
 */
public class ApkInstallPermission implements IExtPermission {
    @Override
    public boolean checkPermission() {
        return false;
    }
}
