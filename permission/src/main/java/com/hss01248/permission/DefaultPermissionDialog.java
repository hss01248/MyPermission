package com.hss01248.permission;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public class DefaultPermissionDialog implements IPermissionDialog{
    @Override
    public void show(List<String> permissions,IPermissionDialogBtnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                .setTitle(R.string.mypermission_title)
                .setMessage(StringUtils.getString(R.string.mypermission_msg)+ ":\n"+ Arrays.toString(permissions.toArray())
                        .toLowerCase()
                        .replaceAll("android\\.permission\\.","")
                        .replaceAll("\\[","")
                        .replaceAll("\\]","")
                        .replaceAll(",","\n"))
                .setPositiveButton(R.string.mypermission_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onPositive();

                    }
                }).setNegativeButton(R.string.mypermission_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onNegtivite();

                    }
                }).create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();
    }
}
