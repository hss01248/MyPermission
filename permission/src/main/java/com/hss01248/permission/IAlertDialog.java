package com.hss01248.permission;

import android.app.Dialog;
import android.content.DialogInterface;

public interface IAlertDialog {

     Dialog showAlert(String title, String msg, String positiveText, String negativeText,
                      DialogInterface.OnClickListener positiveOnClick,
                      DialogInterface.OnClickListener negativeOnClick,
                      boolean cancelable, boolean outsideCancelable);
}
