package com.hss01248.permission.ext;

public interface IExtPermissionCallback {


    void onGranted(String name);

    void onDenied(String name);
}
