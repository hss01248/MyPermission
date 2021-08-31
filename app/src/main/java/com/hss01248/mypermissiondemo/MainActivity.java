package com.hss01248.mypermissiondemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.permission.DefaultPermissionDialog;
import com.hss01248.permission.MyPermission;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.init(getApplication());
    }

    public void normal(View view) {
        MyPermission.requestByMostEffort(Manifest.permission.ACCESS_FINE_LOCATION, null, null, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        });
    }

    public void beforeRequest(View view) {
        MyPermission.requestByMostEffort(Manifest.permission.READ_CONTACTS, new DefaultPermissionDialog(), null, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        });
    }

    public void afterDenied(View view) {
        MyPermission.requestByMostEffort(Manifest.permission.READ_EXTERNAL_STORAGE, null, new DefaultPermissionDialog(), new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        });
    }

    public void both(View view) {
        MyPermission.requestByMostEffort(Manifest.permission.READ_EXTERNAL_STORAGE, new DefaultPermissionDialog(), new DefaultPermissionDialog(), new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        });
    }
}