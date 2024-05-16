package com.hss01248.location.sim;

import android.text.TextUtils;


public class BaseStationInfo {


    public static int toInt(String number, int defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(number.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }


}
