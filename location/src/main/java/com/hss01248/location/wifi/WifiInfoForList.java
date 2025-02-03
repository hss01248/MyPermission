package com.hss01248.location.wifi;

import androidx.annotation.Keep;

@Keep
public class WifiInfoForList {


    public String wifi_name;

    public String wifi_mac;

    public int signal_strength;

    public int signalToNoiseRatio;

    @Override
    public String toString() {
        return "WifiInfoForList{" +
                "wifi_name='" + wifi_name + '\'' +
                ", wifi_mac='" + wifi_mac + '\'' +
                ", signal_strength=" + signal_strength +
                ", signalToNoiseRatio=" + signalToNoiseRatio +
                '}';
    }
}

