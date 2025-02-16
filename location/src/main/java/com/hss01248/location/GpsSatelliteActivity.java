package com.hss01248.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssAntennaInfo;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hss01248.location.databinding.GpsStatusInfoBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import io.reactivex.functions.Consumer;

/**
 * @Despciption todo
 * @Author hss
 * @Date 4/30/24 2:58 PM
 * @Version 1.0
 */
public class GpsSatelliteActivity extends AppCompatActivity {



   public static  void start(){
        ActivityUtils.getTopActivity().startActivity(new Intent(ActivityUtils.getTopActivity(),GpsSatelliteActivity.class));
    }
    GpsStatusInfoBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         binding = GpsStatusInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                LocationUtil.getLocation(GpsSatelliteActivity.this,
                        false, 15000, false, true,
                        new MyLocationCallback() {
                            @Override
                            public boolean configJustAskPermissionAndSwitch() {
                                return true;
                            }

                            @Override
                            public void onSuccess(Location location, String msg) {

                                initGps();
                            }

                            @Override
                            public void onFailed(int type, String msg, boolean isFailBeforeReallyRequest) {
                                ToastUtils.showShort(msg);
                            }
                        });

            }
        },1000);

    }

    GnssStatus.Callback gnssCallback;

    LocationManager locationManager;
    Thread thread;

    private void initGps() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            long start = System.currentTimeMillis();
            showProviderInfo();

            Thread thread2 = new Thread(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    Looper.prepare();
                    requestLocation(start);
                    Looper.loop();
                }
            });
            thread2.start();
            //requestLocation2(start);



            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gnssCallback = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                        super.onSatelliteStatusChanged(status);
                        //LogUtils.i("onSatelliteStatusChanged", status.getSatelliteCount());
                        if(isDestroyed()){
                            Looper.myLooper().quitSafely();
                            return;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showSatelliteInfo(status);
                            }
                        });
                    }

                    @Override
                    public void onFirstFix(int ttffMillis) {
                        super.onFirstFix(ttffMillis);
                        LogUtils.i("GnssStatus-onFirstFix", ttffMillis);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.tvSatellitesStatus.setText("GnssStatus-onFirstFix "+ttffMillis);
                            }
                        });
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.tvSatellitesStatus.setText("stopped connect satellites");
                            }
                        });
                    }

                    @Override
                    public void onStarted() {
                        super.onStarted();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.tvSatellitesStatus.setText("start connecting satellites...");
                            }
                        });
                    }
                };
                 thread = new Thread(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        Looper.prepare();
                        locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.myLooper()));
                        Looper.loop();
                    }
                });
                thread.start();

            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.registerGnssMeasurementsCallback(new GnssMeasurementsEvent.Callback() {
                    @Override
                    public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                        super.onGnssMeasurementsReceived(eventArgs);
                        LogUtils.i("onGnssMeasurementsReceived", eventArgs);
                    }

                    @Override
                    public void onStatusChanged(int status) {
                        super.onStatusChanged(status);
                        LogUtils.i("onGnssMeasurementsReceived", status);
                    }
                }, new Handler(Looper.getMainLooper()));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerAntennaInfoListener(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                }, new GnssAntennaInfo.Listener() {
                    @Override
                    public void onGnssAntennaInfoReceived(@NonNull List<GnssAntennaInfo> gnssAntennaInfos) {
                        LogUtils.i("onGnssAntennaInfoReceived", gnssAntennaInfos);
                    }
                });
            }
        } else {
            ToastUtils.showShort("no permission");
        }
    }

    private void showProviderInfo() {
        if(isDestroyed()){
            return;
        }
        List<String> allProviders = locationManager.getAllProviders();
        StringBuilder sb = new StringBuilder("AllProviders:\n");
        if(allProviders ==null ){
            sb.append("locationManager.getAllProviders() is null !!!! \n then check manually by code:\n\n");
            allProviders = new ArrayList<>();
            allProviders.add(LocationManager.GPS_PROVIDER);//这里抛空指针
            allProviders.add(LocationManager.NETWORK_PROVIDER);
            allProviders.add(LocationManager.PASSIVE_PROVIDER);
            allProviders.add("fused");
            /*sb.append("LocationManager.GPS_PROVIDER: ").append(LocationManager.GPS_PROVIDER).append("\n");
            sb.append("LocationManager.NETWORK_PROVIDER: ").append(LocationManager.NETWORK_PROVIDER).append("\n");
            sb.append("LocationManager.PASSIVE_PROVIDER: ").append(LocationManager.PASSIVE_PROVIDER).append("\n");*/
            for (String allProvider : allProviders) {
                sb.append(allProvider).append(" -avaiable: ").append(locationManager.isProviderEnabled(allProvider)).append("\n");
            }

        }else if(allProviders.isEmpty()){
            sb.append("locationManager.getAllProviders() is empty");
        }else{
            for (String allProvider : allProviders) {
                sb.append(allProvider).append(" -avaiable: ").append(locationManager.isProviderEnabled(allProvider)).append("\n");
            }
        }

        LocationStateUtil.getLocationState(new Consumer<LocationStateInfo>() {
            @Override
            public void accept(LocationStateInfo locationStateInfo) throws Exception {
                sb.append("\n").append(locationStateInfo);
                binding.tvLocationProviders.setText(sb.toString());
            }
        });

        binding.tvLocationProviders.setText(sb.toString());

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                showProviderInfo();
            }
        },1000);

    }

    private void requestLocation2(long start) {
        LocationUtil.getLocationFast(10000, new MyLocationFastCallback() {
            @Override
            public void onSuccessFast(Location location, String msg) {
                if(isDestroyed()){
                    return;
                }
                showLocationInfo(location);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requestLocation2(start);
                    }
                },1000);
            }

            @Override
            public void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.tvLocationInfo.setText("type: "+type+"\nmsg:"+msg+"\nisFailBeforeReallyRequest:"+isFailBeforeReallyRequest);
                    }
                });
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestLocation(long start) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {

                        LogUtils.i(location, "cost(s):" + (System.currentTimeMillis() - start) / 1000,
                                "old:" + (System.currentTimeMillis() - location.getTime()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            if (isDestroyed()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    Looper.myLooper().quitSafely();
                                }
                                return;
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showLocationInfo(location);
                            }
                        });

                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        LocationListener.super.onProviderDisabled(provider);
                        LogUtils.w("onProviderDisabled", provider);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        LocationListener.super.onStatusChanged(provider, status, extras);
                        LogUtils.w("onStatusChanged", provider, status, extras);

                    }
                });
    }

    int updateCount = 0;
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showSatelliteInfo(GnssStatus status) {
        updateCount++;
        StringBuilder builder = new StringBuilder();
        builder.append("Satellite count: ");
        builder.append(status.getSatelliteCount());
        builder.append(",    update count: ");
        builder.append(updateCount);
        //先排序:
        List<Map<String,Object>> list = new ArrayList<>();

        for (int i = 0; i < status.getSatelliteCount(); i++) {
            builder.append("\n\n");
            builder.append("Svid: ");
            builder.append(status.getSvid(i));
            builder.append(" ,");
            builder.append("AzimuthDegrees: ");
            builder.append(status.getAzimuthDegrees(i));
            builder.append(" ,");
            builder.append("ElevationDegrees: ");
            builder.append(status.getElevationDegrees(i));
            builder.append(" ,");
            builder.append("ConstellationType: ");
            builder.append(status.getConstellationType(i));
            builder.append(" ,");
            builder.append("Cn0DbHz: ");
            builder.append(status.getCn0DbHz(i));
            builder.append(" ,");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.append("BasebandCn0DbHz: ");
                builder.append(status.getBasebandCn0DbHz(i));
                builder.append(" ,");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.append("CarrierFrequencyHz: ");
                builder.append(status.getCarrierFrequencyHz(i));
                builder.append(" ,");
            }
        }
        String text = builder.toString();
        binding.tvSatellitesInfo.setText(text);

        //showInTable(status);
    }

/*    private void showInTable(GnssStatus status) {
        //普通列
        Column<String> column1 = new Column<>("Svid", "Svid");
        Column<Integer> column2 = new Column<>("AzimuthDegrees", "AzimuthDegrees");
        Column<Long> column3 = new Column<>("ElevationDegrees", "ElevationDegrees");
        Column<String> column4 = new Column<>("ConstellationType", "ConstellationType");
        Column<String> column5 = new Column<>("ConstellationType", "ConstellationType");
        Column<String> column6 = new Column<>("BasebandCn0DbHz", "BasebandCn0DbHz");
        Column<String> column7 = new Column<>("CarrierFrequencyHz", "CarrierFrequencyHz");

        List<Map<String,Object>> list = new ArrayList<>();
        //如果是多层，可以通过.来实现多级查询
       // Column<String> column5 = new Column<>("班级", "class.className");
        //组合列
        //Column totalColumn1 = new Column("组合列名",column1,column2);
        //表格数据 datas是需要填充的数据
        final TableData<Map<String,Object>> tableData = new TableData<>("卫星数据",list,
                column1, column2,column3,column4,column5,column6,column7);
        //设置数据
        //table.setZoom(true,3);是否缩放
        binding.table.setTableData(tableData);
    }*/

    int locationUpdateCount = 0;
    private void showLocationInfo(Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                locationUpdateCount++;
                String text = "点击可跳到地图查看---> locationUpdateCount: "+locationUpdateCount+"\n"+locationToString(location);
                binding.tvLocationInfo.setText(text);
                binding.tvLocationInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MapUtil.showFormatedLocationInfoInDialog(location);
                    }
                });
            }
        });

    }

    public static String locationToString(Location location) {
        if (location == null) {
            return "Location is null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(location.toString()).append("\n");
        sb.append("Provider: ").append(location.getProvider()).append("\n");
        sb.append("Latitude: ").append(location.getLatitude()).append("\n");
        sb.append("Longitude: ").append(location.getLongitude()).append("\n");
        sb.append("Altitude: ").append(location.getAltitude()).append("\n");
        sb.append("Speed: ").append(location.getSpeed()).append("\n");
        sb.append("Bearing: ").append(location.getBearing()).append("\n");
        sb.append("Time: ").append(location.getTime()).append("\n");
        sb.append("Accuracy: ").append(location.getAccuracy()).append("\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sb.append("ElapsedRealtimeNanos: ").append(location.getElapsedRealtimeNanos()).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sb.append("ElapsedRealtimeUncertaintyNanos: ").append(location.getElapsedRealtimeUncertaintyNanos()).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append("VerticalAccuracyMeters: ").append(location.getVerticalAccuracyMeters()).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append("SpeedAccuracyMetersPerSecond: ").append(location.getSpeedAccuracyMetersPerSecond()).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append("BearingAccuracyDegrees: ").append(location.getBearingAccuracyDegrees()).append("\n");
        }

        if(location.getExtras() !=null){
            sb.append("extras-------->: size:").append(location.getExtras().size()).append("\n");
            Set<String> stringSet = location.getExtras().keySet();
            for (String s : stringSet) {
                sb.append(s).append(": ").append(location.getExtras().get(s)).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager == null) {
            return;
        }
        if(thread !=null){
            thread = null;
        }
        if (gnssCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssStatusCallback(gnssCallback);
            }
        }

    }
}
