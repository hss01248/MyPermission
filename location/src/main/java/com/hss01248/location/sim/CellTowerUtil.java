package com.hss01248.location.sim;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ReflectUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.location.MyLocationCallback;
import com.hss01248.location.wifi.WifiCommonCallback;
import com.hss01248.location.wifi.WifiToLocationUtil;
import com.hss01248.permission.MyPermissions;

import java.util.ArrayList;
import java.util.List;

/**
 * @Despciption todo
 * @Author hss
 * @Date 5/16/24 9:25 AM
 * @Version 1.0
 */
public class CellTowerUtil {

    public static void getCellTowerInfo(WifiCommonCallback<GeoParam> callback) {
        MyPermissions.requestByMostEffort(false, true,
                new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        loadInfo(callback);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        callback.onFail("permission", "no permission", null);
                    }
                }, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
        //Manifest.permission.READ_PHONE_STATE
    }

    public static void getLocation(MyLocationCallback callback) {
        getCellTowerInfo(new WifiCommonCallback<GeoParam>() {
            @Override
            public void onSuccess(GeoParam param) {
                WifiToLocationUtil.requestApi(param, callback);
            }

            @Override
            public void onFail(String code, String msg, Throwable throwable) {
                callback.onFailed(-1, msg);
            }
        });
    }

    public static void loadInfo(WifiCommonCallback<GeoParam> callback) {
        TelephonyManager tm = (TelephonyManager) Utils.getApp().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            LogUtils.e("TelephonyManager is null");
            callback.onFail("-1", "telephonyManager is empty", null);
            return;
        }
        GeoParam param = new GeoParam();
        String operator = tm.getNetworkOperator();
        LogUtils.d("getNetworkOperator", operator);
        try {
            if (!TextUtils.isEmpty(operator) && operator.length() >= 3) {
                param.homeMobileCountryCode = BaseStationInfo.toInt(operator.substring(0, 3), 0);
                //// 前三位是MCC，接下来的是MNC
                param.homeMobileNetworkCode = BaseStationInfo.toInt(operator.substring(3), 0);
            }
        } catch (Throwable e) {
            LogUtils.w("basestation", e);
        }
        if (PermissionUtils.isGranted(Manifest.permission.READ_PHONE_STATE)) {
           // param.radioType = radioType(tm.getNetworkType());
        }
        List<CellInfo> infos = tm.getAllCellInfo();
        if (infos == null || infos.isEmpty()) {
            LogUtils.w("基站信息为空");
            callback.onFail("-1", "cellInfo list is empty", null);
            return;
        }
        LogUtils.w("基站个数: "+infos.size());
        for (CellInfo info : infos) {
            //LogUtils.d(info);
        }

        convertCellInfoToCellTower(infos, param);
        for (CellTower cellTower : param.cellTowers) {
            LogUtils.i(cellTower);
        }
        callback.onSuccess(param);
    }


    /**
     * 移动无线装置类型。支持的值包括 gsm、cdma、wcdma、lte 和 nr。
     *
     * @param networkType
     * @return
     */
    private static String radioType(int networkType) {
        String radioType = "unknown";
        // 默认值为"unknown", 用于处理任何未知或未列出的网络类型
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                radioType = "gprs";
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                radioType = "edge";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                radioType = "umts";
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                radioType = "cdma";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                radioType = "evdo_0";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                radioType = "evdo_a";
                break;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                radioType = "1xrtt";
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                radioType = "hsdpa";
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                radioType = "hsupa";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                radioType = "hspa";
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                radioType = "iden";
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                radioType = "evdo_b";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                radioType = "lte";
                break;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                radioType = "ehrpd";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                radioType = "hspap";
                break;
            case TelephonyManager.NETWORK_TYPE_GSM:
                radioType = "gsm";
                break;
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                radioType = "td_scdma";
                break;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                radioType = "iwlan";
                break;
            //TelephonyManager.NETWORK_TYPE_NR:
            case 20:
                radioType = "nr"; // 5G New Radio
                break;
        }
        return radioType;
    }


    public static List<CellTower> convertCellInfoToCellTower(List<CellInfo> infos, GeoParam param) {
        List<CellTower> cellTowers = new ArrayList<>();
        //param.cellTowers = cellTowers;
        List<CellTower> lte = new ArrayList<>();
        List<CellTower> gsm = new ArrayList<>();
        List<CellTower> wcdma = new ArrayList<>();
        List<CellTower> cdma = new ArrayList<>();
        List<CellTower> nr = new ArrayList<>();
        for (CellInfo info : infos) {
            /*if(!info.isRegistered()){
                LogUtils.w("没有注册: ",info);
                continue;
            }*/
            CellTower cellTower = new CellTower();

            if (info instanceof CellInfoLte) {
                CellIdentityLte identity = ((CellInfoLte) info).getCellIdentity();
                cellTower.cellId = identity.getCi();
                cellTower.locationAreaCode = identity.getTac();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cellTower.mobileCountryCode = identity.getMccString() != null ? Integer.parseInt(identity.getMccString()) : -1;
                    cellTower.mobileNetworkCode = identity.getMncString() != null ? Integer.parseInt(identity.getMncString()) : -1;
                }
                cellTower.signalStrength = ((CellInfoLte) info).getCellSignalStrength().getDbm();
                lte.add(cellTower);

            } else if (info instanceof CellInfoGsm) {
                CellIdentityGsm identity = ((CellInfoGsm) info).getCellIdentity();
                cellTower.cellId = identity.getCid();
                cellTower.locationAreaCode = identity.getLac();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cellTower.mobileCountryCode = identity.getMccString() != null ? Integer.parseInt(identity.getMccString()) : -1;
                    cellTower.mobileNetworkCode = identity.getMncString() != null ? Integer.parseInt(identity.getMncString()) : -1;
                }
                cellTower.signalStrength = ((CellInfoGsm) info).getCellSignalStrength().getDbm();
                gsm.add(cellTower);

            } else if (info instanceof CellInfoWcdma) {
                CellIdentityWcdma identity = ((CellInfoWcdma) info).getCellIdentity();
                cellTower.cellId = identity.getCid();
                cellTower.locationAreaCode = identity.getLac();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cellTower.mobileCountryCode = identity.getMccString() != null ? Integer.parseInt(identity.getMccString()) : -1;
                    cellTower.mobileNetworkCode = identity.getMncString() != null ? Integer.parseInt(identity.getMncString()) : -1;
                }

                cellTower.signalStrength = ((CellInfoWcdma) info).getCellSignalStrength().getDbm();
                wcdma.add(cellTower);

            } else if (info instanceof CellInfoCdma) {
                CellIdentityCdma identity = ((CellInfoCdma) info).getCellIdentity();
                cellTower.cellId = identity.getBasestationId();
                // CDMA不使用LAC和MCC/MNC
                cellTower.signalStrength = ((CellInfoCdma) info).getCellSignalStrength().getDbm();

                cellTower.locationAreaCode = identity.getSystemId();
                cdma.add(cellTower);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (info instanceof CellInfoNr) {
                    CellIdentityNr identity = (CellIdentityNr) ((CellInfoNr) info).getCellIdentity();
                    cellTower.newRadioCellId = Long.valueOf(identity.getPci()).intValue();  // NCI is long, convert if necessary
                   //cellTower.cellId = Long.valueOf(identity.getPci()).intValue();  // NCI is long, convert if necessary
                    cellTower.mobileCountryCode = identity.getMccString() != null ? Integer.parseInt(identity.getMccString()) : -1;
                    cellTower.mobileNetworkCode = identity.getMncString() != null ? Integer.parseInt(identity.getMncString()) : -1;
                    cellTower.signalStrength = ((CellInfoNr) info).getCellSignalStrength().getDbm();
                    //cellTower.locationAreaCode = identity.getTac();
                    nr.add(cellTower);
                }
            } else if (info != null) {
                try {
                    //5G
                    if ("android.telephony.CellInfoNr".equals(info.getClass().getName())) {
                        CellIdentity cellIdentity = ReflectUtils.reflect(info).method("getCellIdentity").get();
                        CellSignalStrength signalStrength = ReflectUtils.reflect(info).method("getCellSignalStrength").get();
                        if (signalStrength != null) {
                            cellTower.signalStrength = signalStrength.getDbm();
                            if (cellIdentity != null) {
                                //https://developer.android.google.cn/reference/android/telephony/CellIdentityNr
                                String mncStr = ReflectUtils.reflect(cellIdentity).method("getMncString").get();
                                String mccStr = ReflectUtils.reflect(cellIdentity).method("getMccString").get();
                                cellTower.mobileNetworkCode = BaseStationInfo.toInt(mncStr, -1);
                                cellTower.mobileCountryCode = BaseStationInfo.toInt(mccStr, -1);
                                cellTower.newRadioCellId = ReflectUtils.reflect(cellIdentity).method("getPci").get();
                                //cellTower.cellId = ReflectUtils.reflect(cellIdentity).method("getPci").get();
                               // cellTower.locationAreaCode = ReflectUtils.reflect(cellIdentity).method("getTac").get();
                            }
                        }
                        nr.add(cellTower);
                    }
                } catch (Throwable throwable) {
                    LogUtils.w("dd", throwable);
                }
            }

            //5G和4G不能混用
            //5G基本不可用
            /*if("nr".equals(param.radioType)){
                if(cellTower.newRadioCellId !=null){
                    cellTowers.add(cellTower);
                }
            }else{
                if(cellTower.newRadioCellId ==null){
                    cellTowers.add(cellTower);
                }
            }*/

            //todo 重要: 不能混用,一次定位只能用一批同类型的基站. 5G可能会和其他4G的共存,需要区分
            cellTowers = lte;
            param.radioType = "lte";
            if(gsm.size() > cellTowers.size()){
                param.radioType = "gsm";
                cellTowers = gsm;
            }
            if(cdma.size() > cellTowers.size()){
                param.radioType = "cdma";
                cellTowers = cdma;
            }
            if(wcdma.size() > cellTowers.size()){
                param.radioType = "wcdma";
                cellTowers = wcdma;
            }
            //5G似乎无法定位,报找不到这个基站 404
            if(nr.size() > cellTowers.size()){
                param.radioType = "nr";
                cellTowers = nr;
            }
            param.cellTowers = cellTowers;
            /*if(!cellTowers.isEmpty()){
                param.homeMobileNetworkCode = cellTowers.get(0).mobileNetworkCode;
                param.homeMobileCountryCode = cellTowers.get(0).mobileCountryCode;
            }*/

        }
        return cellTowers;
    }


}
