package com.hss01248.location.wifi;

import androidx.annotation.Keep;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/15/24 3:42 PM
 * @Version 1.0
 */
@Keep
public class WifiAccessPoint {
    private String macAddress;
    private Integer signalStrength;
    private Integer signalToNoiseRatio;

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Integer getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Integer getSignalToNoiseRatio() {
        return signalToNoiseRatio;
    }

    public void setSignalToNoiseRatio(Integer signalToNoiseRatio) {
        this.signalToNoiseRatio = signalToNoiseRatio;
    }
}
