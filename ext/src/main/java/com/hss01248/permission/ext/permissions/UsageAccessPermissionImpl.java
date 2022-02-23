package com.hss01248.permission.ext.permissions;

import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.blankj.utilcode.util.AppUtils;
import com.hss01248.permission.ext.IExtPermission;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * @Despciption todo https://codeantenna.com/a/mPHeAh6dMM
 * @Author hss
 * @Date 23/02/2022 16:16
 * @Version 1.0
 */
public class UsageAccessPermissionImpl implements IExtPermission {
    @Override
    public String name() {
        return "Usage Access";
    }

    @Override
    public boolean checkPermission(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            //"usagestats"  Context.USAGE_STATS_SERVICE
            UsageStatsManager usm=(UsageStatsManager)activity.getSystemService(Context.USAGE_STATS_SERVICE);
            if(usm == null){
                return false;
            }
            Calendar calendar= Calendar.getInstance();
            long toTime=calendar.getTimeInMillis();
            calendar.add(Calendar.YEAR,-1);
            long fromTime=calendar.getTimeInMillis();
            final List queryUsageStats=usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY,fromTime,toTime);
            boolean granted=queryUsageStats!=null&&queryUsageStats!= Collections.EMPTY_LIST;
            return granted;
        }
        return false;
    }

    @Override
    public Intent intentToRequestPermission(Activity activity) {
        Intent intent =  new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.setData(Uri.parse("package:" + AppUtils.getAppPackageName()));
        return intent;
    }
}
