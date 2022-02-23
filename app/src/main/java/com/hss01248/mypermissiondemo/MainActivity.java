package com.hss01248.mypermissiondemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.location.LocationUtil;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.permission.MyPermissions;
import com.hss01248.permission.ext.IExtPermission;
import com.hss01248.permission.ext.IExtPermissionCallback;
import com.hss01248.permission.ext.MyPermissionsExt;
import com.hss01248.permission.ext.permissions.ApkPermissionImpl;
import com.hss01248.permission.ext.permissions.NotificationListenerPermissionImpl;
import com.hss01248.permission.ext.permissions.NotificationPermission;
import com.hss01248.permission.ext.permissions.StorageManagerPermissionImpl;
import com.hss01248.permission.ext.permissions.UsageAccessPermissionImpl;

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
        MyPermissions.requestByMostEffort( false, false, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        },Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE);
    }

    public void beforeRequest(View view) {
        MyPermissions.requestByMostEffort(true, false, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        },Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE);
    }

    public void afterDenied(View view) {
        MyPermissions.requestByMostEffort( false, true, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        },Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.CALL_PHONE);
    }

    public void both(View view) {
        MyPermissions.requestByMostEffort( true, true, new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        },Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE);
    }

    public void getLocation(View view) {
        LocationUtil.getLocation(view.getContext(),false,5000,false,false,new MyLocationCallback() {
            @Override
            public void onFailed(int type, String msg) {
                ToastUtils.showLong(type+","+msg);
                LogUtils.w(msg,type);
            }

            @Override
            public void onSuccess(Location location, String msg) {
                ToastUtils.showLong("success,"+msg+", location:"+location);
                LogUtils.i(msg,location);

            }
        });
    }

    public void multiPermission(View view) {
       // MyPermissions.requestByMostEffort();
    }
    private void ask(IExtPermission permission){
        MyPermissionsExt.askPermission(this, permission, new IExtPermissionCallback() {
            @Override
            public void onGranted(String name) {
                ToastUtils.showShort("onGranted "+ name);
            }

            @Override
            public void onDenied(String name) {
                ToastUtils.showShort("onDenied "+ name);
            }
        });
    }

    public void isInManifest(View view) {
        boolean stateInManifest = MyPermissions.isStateInManifest(Manifest.permission.READ_SMS);
        ToastUtils.showLong("sms是否声明在manifest里:"+stateInManifest);

        /*boolean stateInManifest2 = MyPermissions.isStateInManifest(Manifest.permission.READ_EXTERNAL_STORAGE);
        ToastUtils.showLong("READ_EXTERNAL_STORAGE是否声明在manifest里:"+stateInManifest2);*/
    }

    public void askExtPermissions(View view) {

        ask(new ApkPermissionImpl() );
    }



    public void askNotification(View view) {
        ask(new NotificationPermission() );
    }

    public void askManagerAllStorage(View view) {
        ask(new StorageManagerPermissionImpl());
    }

    public void askNotificationListener(View view) {
        ask(new NotificationListenerPermissionImpl());
    }

    public void useageStatus(View view) {
        ask(new UsageAccessPermissionImpl());
    }
}