package com.hss01248.location;

import com.blankj.utilcode.util.SPUtils;

/**
 * @Despciption todo
 * @Author hss
 * @Date 22/08/2022 16:53
 * @Version 1.0
 */
public class DefaultLocationCache implements ILocationCache{
    @Override
    public String getLocationJasonArrStr() {
        return SPUtils.getInstance().getString("cachedLocations","");
    }

    @Override
    public void saveLocations(String jsonArr) {
        SPUtils.getInstance().put("cachedLocations",jsonArr);
    }
}
