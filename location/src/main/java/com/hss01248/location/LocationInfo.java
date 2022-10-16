package com.hss01248.location;

import androidx.annotation.Keep;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Despciption todo  https://www.runoob.com/w3cnote/android-tutorial-gps.html
 * @Author hss
 * @Date 15/08/2022 10:14
 * @Version 1.0
 */
@Keep
public class LocationInfo {

    public double longtitude;
    public double lattidude;
    public long timeStamp;
    public String timeStampStr;
    public String locale;

    public long millsOldWhenSaved = -1;

    /**
     * gms,gps,network
     */
    public String realProvider;
    public String calledMethod;
    public ProviderInfo providerInfo;

    public long timeCost;

    public  long costFromBegin;

    /**
     * 海拔高度
     */
    public double altitude;
    /**
     * 精确度
     */
    public float accuracy;

    public float speed;

    /**
     * 方向
     */
    public float bearing;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationInfo)) return false;

        LocationInfo info = (LocationInfo) o;

        if (Double.compare(info.longtitude, longtitude) != 0) return false;
        if (Double.compare(info.lattidude, lattidude) != 0) return false;
        return timeStamp == info.timeStamp;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(longtitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lattidude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        return result;
    }

    //  ", timeStamp=" + timeStamp +", "+new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(timeStamp))+

    @Override
    public String toString() {
        return "LocationInfo{" +
                "longtitude=" + longtitude +
                ", lattidude=" + lattidude +
                ", timeStamp=" + timeStamp +
                ", timeStampStr='" + timeStampStr + '\'' +
                ", locale='" + locale + '\'' +
                ", millsOldWhenSaved=" + millsOldWhenSaved +
                ", realProvider='" + realProvider + '\'' +
                ", calledMethod='" + calledMethod + '\'' +
                ", providerInfo=" + providerInfo +
                ", timeCost=" + timeCost +
                ", costFromBegin=" + costFromBegin +
                ", altitude=" + altitude +
                ", accuracy=" + accuracy +
                ", speed=" + speed +
                ", bearing=" + bearing +
                '}';
    }
}
