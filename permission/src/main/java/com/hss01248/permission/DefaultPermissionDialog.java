package com.hss01248.permission;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;

import java.util.Arrays;
import java.util.List;

public class DefaultPermissionDialog implements IPermissionDialog{
    @Override
    public void show(List<String> permissions,IPermissionDialogBtnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                .setTitle("权限提醒")
                .setMessage("需要xx权限来继续:\n"+ Arrays.toString(permissions.toArray()))
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onPositive();

                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onNegtivite();

                    }
                }).show();
    }
}
