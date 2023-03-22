package com.hss01248.permission;

import android.app.Dialog;
import android.content.DialogInterface;

import androidx.annotation.Nullable;

public interface IAlertDialog {

     Dialog showAlert(@Nullable  String title,@Nullable  String msg,
                      @Nullable  String positiveText,@Nullable  String negativeText,
                      boolean cancelable, boolean outsideCancelable,
                      @Nullable  DialogInterface.OnClickListener positiveOnClick,
                      @Nullable  DialogInterface.OnClickListener negativeOnClick
                      );
}
