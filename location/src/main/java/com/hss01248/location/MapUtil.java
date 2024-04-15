package com.hss01248.location;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.net.URLEncoder;

/**
 * @Despciption 提供百度地图,高德地图,谷歌地图的根据经纬度在地图上标记点的功能.
 * 腾讯地图跳转需要提供apikey,所以这里不提供方法. 辣鸡腾讯.
 * @Author hss
 * @Date 21/09/2023 10:08
 * @Version 1.0
 */
public class MapUtil {

     static final String PN_GAODE_MAP = "com.autonavi.minimap"; // 高德地图包名
     static final String PN_BAIDU_MAP = "com.baidu.BaiduMap"; // 百度地图包名
     static final String DOWNLOAD_GAODE_MAP = "http://www.autonavi.com/"; // 高德地图下载地址
     static final String DOWNLOAD_BAIDU_MAP = "http://map.baidu.com/zt/client/index/"; // 百度地图下载地址




    public static void showFormatedLocationInfoInDialog(Location location){
        ThreadUtils.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                LocationInfo info = LocationSync.toLocationInfo(location);
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(info);

                AlertDialog dialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                        .setTitle("定位结果")
                        .setMessage(json)
                        .setPositiveButton("跳到地图", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showMapChooseDialog(location.getLatitude(),location.getLongitude());
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dialog.show();
            }
        });


    }

    public static void showMapChooseDialog(double lat,double lon){
        CharSequence[] maps = {"百度地图(一键回来)","高德地图(切换任务栏回来)"
                ,"谷歌地图web(国内有偏移)","谷歌地图app"};
        AlertDialog dialog = new AlertDialog.Builder(ActivityUtils.getTopActivity())
                .setTitle("选择在一个地图上显示经纬度")
                .setSingleChoiceItems(maps, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean can = false;
                        if(which ==0){
                            can =  openBaiduMap(lat, lon);
                        }else if(which ==1){
                            can = openAmap(lat, lon);
                        }else if(which ==2){
                            can = openGoogleMap(lat, lon);
                        }else if(which ==3){
                            can = openGoogleMapApp(lat, lon);
                        }
                        if(can){
                            dialog.dismiss();
                        }

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .create();
        dialog .setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     *
     * 高德地图跳转文档: https://lbs.amap.com/api/amap-mobile/guide/android/marker
     * act=android.intent.action.VIEW
     * cat=android.intent.category.DEFAULT
     * dat=androidamap://viewMap?sourceApplication=appname&poiname=abc&lat=36.2&lon=116.1&dev=0
     * pkg=com.autonavi.minimap
     * @param lat
     * @param lon
     * @return
     */
    public static boolean openAmap(double lat,double lon){
        try {
            String address = "当前定位:"+lat+","+lon;
            StringBuilder builder = new StringBuilder("androidamap://viewMap?sourceApplication=");
            builder.append(AppUtils.getAppName())
                    .append("&lat=")
                    .append(lat)
                    .append("&lon=")
                    .append(lon)
                    .append("&poiname=")
                    .append(address)
                    .append("&dev=1");
            //dev
            //起终点是否偏移(0:lat 和 lon 是已经加密后的,不需要国测加密; 1:需要国测加密)
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(PN_GAODE_MAP);
            intent.setData(Uri.parse(builder.toString()));
            ActivityUtils.getTopActivity().startActivity(intent);
            return true;
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            ToastUtils.showLong(throwable.getMessage());
            return false;
        }
    }

    /**
     *
     * https://lbsyun.baidu.com/faq/api?title=webapi/uri/andriod
     * baidumap://map/marker
     * @param lat
     * @param lon
     * @return
     */
    public static boolean openBaiduMap(double lat,double lon){
        try {
            String address = "当前定位:"+lat+","+lon;
            StringBuilder builder = new StringBuilder("baidumap://map/marker?");
            builder
                    .append("location=")//lat,lng (先纬度，后经度)
                    .append(lat)
                    .append(",")
                    .append(lon)
                    .append("&src=")
                    .append(AppUtils.getAppPackageName())
                    .append("&title=")
                    .append(address)
                    .append("&coord_type=wgs84");
            //dev
            //起终点是否偏移(0:lat 和 lon 是已经加密后的,不需要国测加密; 1:需要国测加密)
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(PN_BAIDU_MAP);
            intent.setData(Uri.parse(builder.toString()));
            ActivityUtils.getTopActivity().startActivity(intent);
            return true;
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            ToastUtils.showLong(throwable.getMessage());
            return false;
        }
    }



    /**
     * https://developers.google.com/maps/documentation/android-api/intents
     * https://developers.google.com/maps/documentation/urls/get-started?hl=zh-cn
     * intent无效,只能用于web. 而且这个url能直接调起谷歌地图
     * 但是谷歌地图在国内会有偏移
     * https://www.google.com/maps/search/?api=1&query=47.5951518%2C-122.3316393
     * @param lat
     * @param lon
     * @return
     */
    public  static  boolean openGoogleMap(double lat,double lon){
        try {
        Uri uri = Uri.parse("https://www.google.com/maps/search/?gl=CN&api=1&query="+ URLEncoder.encode(lat+","+lon));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        ActivityUtils.getTopActivity().startActivity(intent);
        return true;
    }catch (Throwable throwable){
        LogUtils.w(throwable);
        ToastUtils.showLong(throwable.getMessage());
        return false;
    }
    }

    public static boolean openGoogleMapApp(double lat,double lon){
        try {
            Uri gmmIntentUri = Uri.parse("geo:"+lat+","+lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            ActivityUtils.getTopActivity().startActivity(mapIntent);
            return true;
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            ToastUtils.showLong(throwable.getMessage());
            return false;
        }

    }

}
