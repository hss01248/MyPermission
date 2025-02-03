package com.hss01248.location.sim;

import androidx.annotation.Keep;

/**
 * @Despciption https://developers.google.com/maps/documentation/geolocation/requests-geolocation?hl=zh-cn#cell_tower_object
 * @Author hss
 * @Date 5/16/24 9:10 AM
 * @Version 1.0
 */
@Keep
public class CellTower {
    public Integer cellId;
    /**NR (5G) 小区的唯一标识符。
     * */
    public Integer newRadioCellId;
    public Integer locationAreaCode;
    public Integer mobileCountryCode;
    public Integer mobileNetworkCode;


    public double signalStrength;
    public Integer age;
    public Integer timingAdvance;

    @Override
    public String toString() {
        return "CellTower{" +
                "cellId=" + cellId +
                "\n newRadioCellId=" + newRadioCellId +
                "\n locationAreaCode=" + locationAreaCode +
                "\n mobileCountryCode=" + mobileCountryCode +
                "\n mobileNetworkCode=" + mobileNetworkCode +
                "\n signalStrength=" + signalStrength +
                "\n age=" + age +
                "\n timingAdvance=" + timingAdvance +
                '}';
    }
}
