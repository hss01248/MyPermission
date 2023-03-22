package com.hss01248.permission;

import android.content.Context;

/**
 * @Despciption todo
 * @Author hss
 * @Date 22/03/2023 10:25
 * @Version 1.0
 */
public class PermissionConfig  {

    public Context context;

    String afterPermissionText;
    String goSettingText;
    IPermissionDialog permissionDialog;

    boolean showDialogBeforeRequestPermission;
    boolean showDialogAfterPermissionDenied;



}
