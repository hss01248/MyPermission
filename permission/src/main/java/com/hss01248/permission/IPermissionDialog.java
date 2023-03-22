package com.hss01248.permission;

import androidx.annotation.Nullable;

import java.util.List;

public interface IPermissionDialog {

    void show(boolean isGuideToSetting,
              @Nullable String title,//""--> 不要title,  null -> 使用默认title
              @Nullable String afterPermissionMsg,
              @Nullable String guideToSettingMsg,
              List<String> permissions,
              IPermissionDialogBtnClickListener listener);

    default String getDefalutMsg(){
        return null;
    }
    default String getDefalutTitle(){
        return null;
    }
}
