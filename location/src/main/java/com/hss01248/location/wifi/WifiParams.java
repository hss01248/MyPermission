package com.hss01248.location.wifi;

import androidx.annotation.Keep;

import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/15/24 3:42 PM
 * @Version 1.0
 */
@Keep
public class WifiParams {
    /**
     * 当WIFI无法定位的时候，是否采用IP定位，这里最好不要开启，设置为false
     */
    private boolean considerIp;
    private List<WifiAccessPoint> wifiAccessPoints;

    public boolean isConsiderIp() {
        return considerIp;
    }

    public void setConsiderIp(boolean considerIp) {
        this.considerIp = considerIp;
    }

    public List<WifiAccessPoint> getWifiAccessPoints() {
        return wifiAccessPoints;
    }

    public void setWifiAccessPoints(List<WifiAccessPoint> wifiAccessPoints) {
        this.wifiAccessPoints = wifiAccessPoints;
    }
}
