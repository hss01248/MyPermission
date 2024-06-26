package com.hss01248.permission;

import android.app.Dialog;
import android.content.DialogInterface;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ThreadUtils;


/**
 * @Despciption todo
 * @Author hss
 * @Date 21/03/2023 20:08
 * @Version 1.0
 */
public class DefaultAlertDialogImpl implements IAlertDialog{
    @Override
    public Dialog showAlert(String title, String msg, String positiveText, String negativeText,
                            boolean cancelable,
                            boolean outsideCancelable,
                            DialogInterface.OnClickListener positiveOnClick,
                            DialogInterface.OnClickListener negativeOnClick
                           ) {
        //为防止activity调用了finish(),但还没有将isFinishing标识改成true的情况,这里延时200ms:
        ThreadUtils.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(ActivityUtils.getTopActivity());
                if(!TextUtils.isEmpty(title)){
                    builder.setTitle(title);
                }
                if(!TextUtils.isEmpty(msg)){
                    builder.setMessage(msg);
                }
                if(!TextUtils.isEmpty(positiveText)){
                    builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(positiveOnClick !=null){
                                positiveOnClick.onClick(dialog, which);
                            }
                        }
                    });
                }
                if(!TextUtils.isEmpty(negativeText)){
                    builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(negativeOnClick !=null){
                                negativeOnClick.onClick(dialog, which);
                            }
                        }
                    });
                }
                AlertDialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(outsideCancelable);
                alertDialog.setCancelable(cancelable);
                alertDialog.show();
            }
        },200);


        return null;
    }
}
