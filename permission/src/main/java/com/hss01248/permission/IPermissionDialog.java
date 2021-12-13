package com.hss01248.permission;

import java.util.List;

public interface IPermissionDialog {

    void show(boolean isGuideToSetting,List<String> permissions, IPermissionDialogBtnClickListener listener);
}
