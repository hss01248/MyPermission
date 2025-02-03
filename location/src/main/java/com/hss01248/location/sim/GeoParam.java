package com.hss01248.location.sim;

import androidx.annotation.Keep;

import com.hss01248.location.wifi.WifiAccessPoint;

import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/16/24 9:14 AM
 * @Version 1.0
 */
@Keep
public class GeoParam {
    public Integer homeMobileCountryCode;
    public Integer homeMobileNetworkCode;

    /**
     * gsm、cdma、wcdma、lte 和 nr
     */
    public String radioType;
    public String carrier;
    public Boolean considerIp  = false;
    public List<CellTower> cellTowers;
    public List<WifiAccessPoint> wifiAccessPoints;

    @Override
    public String toString() {
        return "GeoParam{" +
                "homeMobileCountryCode=" + homeMobileCountryCode +
                "\n homeMobileNetworkCode=" + homeMobileNetworkCode +
                "\n radioType='" + radioType + '\'' +
                "\n carrier='" + carrier + '\'' +
                "\n considerIp=" + considerIp +
                "\n cellTowers=" + cellTowers +
                "\n wifiAccessPoints=" + wifiAccessPoints +
                '}';
    }
}
