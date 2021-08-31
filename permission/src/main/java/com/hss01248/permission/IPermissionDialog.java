package com.hss01248.permission;

import java.util.List;

public interface IPermissionDialog {

    void show(List<String> permissions, IPermissionDialogBtnClickListener listener);
}
