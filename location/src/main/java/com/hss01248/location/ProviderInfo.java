package com.hss01248.location;

import android.location.Criteria;
import android.location.LocationProvider;

import androidx.annotation.Keep;

/**
 * @Despciption todo
 * @Author hss
 * @Date 15/08/2022 11:14
 * @Version 1.0
 */
@Keep
public class ProviderInfo {

   public float accuracy;//()：返回LocationProvider精度
    public String name;//()：返回LocationProvider名称
    //  getPowerRequirement()：获取LocationProvider的电源需求
   public boolean hasMonetaryCost;//()：返回该LocationProvider是收费还是免费的
   // meetsCriteria(Criteria criteria)：判断LocationProvider是否满足Criteria条件
   public boolean requiresCell;//()：判断LocationProvider是否需要访问网络基站
   public boolean requiresNetwork;//()：判断LocationProvider是否需要访问网络数据
  public boolean  requiresSatellite;//()：判断LocationProvider是否需要访问基于卫星的定位系统
   public boolean supportsAltitude;//()：判断LocationProvider是否支持高度信息
   public boolean supportsBearing;//()：判断LocationProvider是否支持方向信息
   public boolean supportsSpeed;//：判断是LocationProvider否支持速度信息

    public boolean isProviderNull;

    public void initByProvider(LocationProvider provider){
        if(provider == null){
            isProviderNull = true;
            return;
        }
        accuracy = provider.getAccuracy();
        name = provider.getName();
        hasMonetaryCost = provider.hasMonetaryCost();
        requiresCell = provider.requiresCell();
        requiresNetwork = provider.requiresNetwork();
        requiresSatellite = provider.requiresSatellite();
        supportsAltitude = provider.supportsAltitude();
        supportsBearing = provider.supportsBearing();
        supportsSpeed = provider.supportsSpeed();
    }

    @Override
    public String toString() {
        return "ProviderInfo{" +
                "accuracy=" + accuracy +
                ", name='" + name + '\'' +
                ", hasMonetaryCost=" + hasMonetaryCost +
                ", requiresCell=" + requiresCell +
                ", requiresNetwork=" + requiresNetwork +
                ", requiresSatellite=" + requiresSatellite +
                ", supportsAltitude=" + supportsAltitude +
                ", supportsBearing=" + supportsBearing +
                ", supportsSpeed=" + supportsSpeed +
                '}';
    }
}
