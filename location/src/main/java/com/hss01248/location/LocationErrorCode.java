package com.hss01248.location;

import com.blankj.utilcode.util.StringUtils;

public class LocationErrorCode {
    public static final int NO_PERMISSION = 1;
    public static final int LOCATION_SWITCH_OFF = 2;
    public static final int TIMEOUT = 88;
    public static final int LOCATION_MANAGER_TIMEOUT_AND_API_FAILED = 77;

    public static final int LOCATION_MANAGER_NULL = 6;
    public static final int FAKE_LOCATION = 7;

    public static String getErrorMsg(int errorCode) {
        switch (errorCode) {
            case NO_PERMISSION:
                return StringUtils.getString(R.string.location_error_no_permission);
            case LOCATION_SWITCH_OFF:
                return StringUtils.getString(R.string.location_error_switch_off);
            case TIMEOUT:
                return StringUtils.getString(R.string.location_error_timeout);
            case LOCATION_MANAGER_TIMEOUT_AND_API_FAILED:
                return StringUtils.getString(R.string.location_error_timeout_and_api_failed);
            case LOCATION_MANAGER_NULL:
                return StringUtils.getString(R.string.location_error_location_manager_null);
            case FAKE_LOCATION:
                return StringUtils.getString(R.string.location_error_fake);
            default:
                return StringUtils.getString(R.string.location_error_unknown);
        }
    }

}
