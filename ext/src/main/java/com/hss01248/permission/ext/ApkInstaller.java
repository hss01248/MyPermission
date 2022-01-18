package com.hss01248.permission.ext;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;
import com.hss01248.openuri.OpenUri;

import java.io.File;

/**
 * @Despciption todo
 * @Author hss
 * @Date 18/01/2022 09:40
 * @Version 1.0
 */
public class ApkInstaller {


    private static void installApk(final FragmentActivity activity, final File file) {
        LogUtils.w(file.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= 26) {
            boolean havePermission = activity.getPackageManager().canRequestPackageInstalls();
            if (havePermission) {
                installApk(activity, file);
            } else {
                ToastUtils.showLong("请打开本app的安装apk的权限");
                //  引导用户手动开启安装权限
                ////设置这个才能直接跳到应用安装设置页面
                Uri packageURI = Uri.parse("package:" + AppUtils.getAppPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageURI);
                StartActivityUtil.goOutAppForResult(activity, intent, new ActivityResultListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                        installApk(activity, file);
                    }

                    @Override
                    public void onActivityNotFound(Throwable e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            installApk(activity, file);
        }
    }

    private static void doInstallApk(FragmentActivity activity, File file) {

        try {
            Uri uri = OpenUri.fromFile(activity, file);
            LogUtils.w(uri.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        }catch (Throwable throwable){
            throwable.printStackTrace();
            ToastUtils.showLong(throwable.getMessage());
        }

    }
}
