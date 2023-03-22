package com.hss01248.permission;

import android.content.DialogInterface;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ResourceUtils;
import com.blankj.utilcode.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultPermissionDialog implements IPermissionDialog {
    @Override
    public void show(boolean isGuideToSetting,
                     @Nullable String title,//""--> 不要title,  null -> 使用默认title
                    @Nullable String afterPermissionMsg,
                     @Nullable String guideToSettingMsg,
                     List<String> permissions,
                     IPermissionDialogBtnClickListener listener) {
        List<String> permissionsNotGranted = new ArrayList<>();
        for (String permission : permissions) {
            if (!PermissionUtils.isGranted(permission)) {
                permissionsNotGranted.add(permission);
            }
        }
        if (permissionsNotGranted.isEmpty()) {
            listener.onPositive();
            return;
        }
        String msg = null;
        if(isGuideToSetting){
            if(!TextUtils.isEmpty(guideToSettingMsg)){
                msg = guideToSettingMsg;
            }
        }else {
            if(!TextUtils.isEmpty(afterPermissionMsg)){
                msg = afterPermissionMsg;
            }
        }
        if(TextUtils.isEmpty(msg)){
            msg = getDefalutMsg();
            if (TextUtils.isEmpty(msg)) {
                msg = StringUtils.getString(R.string.mypermission_msg) + ":\n"
                        + Arrays.toString(permissionsNotGranted.toArray())
                        .toLowerCase()
                        .replaceAll("android\\.permission\\.", "")
                        .replaceAll("\\[", "")
                        .replaceAll("\\]", "")
                        .replaceAll(",", "\n") +
                        (isGuideToSetting ? "\n" + StringUtils.getString(R.string.mypermission_go_settings) : "");

            }
        }


        if(title == null){
            title = getDefalutTitle();
            if(TextUtils.isEmpty(title)){
                title = StringUtils.getString(R.string.mypermission_title);
            }
        }

        MyPermissions.defaultAlertDialog
                .showAlert(title,
                        msg,
                        StringUtils.getString(isGuideToSetting ? R.string.mypermission_go_settings_btn: R.string.mypermission_ok),
                        StringUtils.getString(R.string.mypermission_cancel)
                        , false,
                        false,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                listener.onPositive();

                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                listener.onNegtivite();

                            }
                        }

                );

    }
}
