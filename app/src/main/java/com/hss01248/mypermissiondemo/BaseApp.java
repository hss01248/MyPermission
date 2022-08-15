package com.hss01248.mypermissiondemo;

import androidx.multidex.MultiDexApplication;

import com.hss01248.location.LocationSync;

/**
 * @Despciption todo
 * @Author hss
 * @Date 13/12/2021 10:13
 * @Version 1.0
 */
public class BaseApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        LocationSync.initAsync();
    }
}
