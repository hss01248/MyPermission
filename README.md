# 权限工具

> 基于utilcode的permissionUtils进行增强



![image-20211208160156189](https://cdn.jsdelivr.net/gh/hss01248/picbed@master/pic/1638950533530-1638950521845-image-20211208160156189.jpg)





## 权限请求:MyPermission

```java
//api: 可自由配置前置dialog,后置dialog
MyPermission.requestByMostEffort(String permission, IPermissionDialog dialogBeforeRequest,
                                           IPermissionDialog dialogAfterDenied,PermissionUtils.FullCallback callback)

//示例
MyPermission.requestByMostEffort(Manifest.permission.READ_EXTERNAL_STORAGE, null, new DefaultPermissionDialog(), new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                ToastUtils.showShort("onGranted:"+Arrays.toString(granted.toArray()));
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                ToastUtils.showShort("onDenied:"+Arrays.toString(deniedForever.toArray()) +"\n"+Arrays.toString(denied.toArray()));
            }
        });
```



# 定位功能

基础知识:

 [Android GPS初涉](https://www.runoob.com/w3cnote/android-tutorial-gps.html?from_wecom=1)

## 一些概念和范畴

### 定位数据-Location类

- float **getAccuracy**()：获得定位信息的精度
- double **getAltitude**()：获得定位信息的高度
- float **getBearing**()：获得定位信息的方向
- double **getLatitude**()：获得定位信息的纬度
- double **getLongitude**()：获得定位信息的精度
- String **getProvider**()：获得提供该定位信息的LocationProvider----> 真正的provider
- float **getSpeed**()：获得定位信息的速度

### 定位提供者-provider

区分:

* 发起者: network,gps,fused, passive,以及通过gms发起定位.
* 真正的provider: 相当于硬件上是哪些定位器, 只有network,gps,fused

### 定位权限

* 精确定位权限
* 模糊定位权限: android12的手机,app的targetsdk>=31时,权限界面会提供两者的区分,否则不区分.

### 定位精准度到底由谁决定?

并不是由provider决定,而是由是否为精确定位权限决定.

有精确定位权限时,哪种provider都很准

只有模糊定位权限时,所有provider都偏差几千米

### 坐标系

见下方定位坐标系模块



## 获取定位: LocationUtil

> 饱和式救援

### 三/四种常用模式:

* 快速模式
* 静默模式
* 普通超时模式
* 连续定位模式(todo)

```java
//超时模式: 简易api 默认10s超时. 拒绝后有一次挽回弹窗
getLocation(Context context, MyLocationCallback callback)

//全配置api
getLocation(Context context, int timeout, IPermissionDialog dialogBeforeRequest,
                                   IPermissionDialog dialogAfterDenied, MyLocationCallback callback) 
    
    //静默模式: 无开关和权限弹窗,无loading 弹窗. 
 public static  void getLocationSilent(long timeoutMills,MyLocationCallback callback)
//快速模式: 适用于业务流程中串行的定位. 据线上数据统计,性能非常不错.
 public static  void getLocationFast(long timeoutMills,MyLocationFastCallback callback)


//示例
 LocationUtil.getLocation(view.getContext(),new MyLocationCallback() {
            @Override
            public void onFailed(int type, String msg) {
                ToastUtils.showLong(type+","+msg);
                LogUtils.w(msg,type);
            }

            @Override
            public void onSuccess(Location location, String msg) {
                ToastUtils.showLong("success,"+msg+", location:"+location);
                LogUtils.i(msg,location);

            }
        });
```

## 回调

* 普通回调  MyLocationCallback
* 快速定位的回调  MyLocationFastCallback

## 一些配置

###  超时

getLocation(Context context, int timeout

### callback里的配置:

> 带config开头的

```java
 
    /**缓存有效时间:
     * 默认一年,约等于永久:
     * 逻辑:  实时定位失败/超时后,从缓存读取,如果System.currentTimeMills-缓存定位里的time < useCacheInTimeOfMills,
     * 则使用该缓存,否则不使用该缓存
     *
     * @return
     */
    default long useCacheInTimeOfMills(){
       return 365*24*60*60*1000L;
    }

    /**
     * 是否支持仅模糊定位. 经纬度会偏差几千米
     * @return
     */
    default boolean configAcceptOnlyCoarseLocationPermission(){
        return false;
    }

    /**
     * 从开关和权限完成后,真正开始定位前,弹loading dialog,定位回调成功或失败时关闭.
     * @return
     */
    default boolean configShowLoadingDialog(){
        return false;
    }


    /**
     * 只判断/请求定位权限和定位开关,不实际发起定位请求
     * @return
     */
    default boolean configJustAskPermissionAndSwitch(){
        return false;
    }
```



## 耗时:

![image-20211214192158731](https://cdn.jsdelivr.net/gh/hss01248/picbed@master/pic/1639480923963-image-20211214192158731.jpg)



![image-20211214192410958](https://cdn.jsdelivr.net/gh/hss01248/picbed@master/pic/1639481051011-image-20211214192410958.jpg)





### 耗时的性能监控

```java
LocationUtil.setLocationMetric(ILocationMetric locationMetric);


public interface ILocationMetric {

    /**
     * 上报快速定位模式回调的耗时信息
     * @param success
     * @param location
     * @param failReason
     * @param successMsg
     * @param cost
     */
    void reportFastCallback(boolean success, Location location,String failReason,String successMsg,long cost);

    /**
     * 上报每个单独的provider的实际定位耗时
     * @param location
     * @param calledProvider
     * @param readProvider
     * @param costOfJustThisUpdate
     * @param costFromUtilStart
     */
    void reportEachLocationChanged( Location location,String calledProvider,String readProvider,long costOfJustThisUpdate, long costFromUtilStart);
}
```

#### 线上统计数据:

#### fast模式:-90分位数据

成功失败率:   97.6%定位成功

fast模式下,缓存有效期为2min.

失败里,都是超时且没有有效缓存. 

![image-20221016110616829](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016110616829.png.jpg)

成功定位的回调里: 具体是哪种provider**发起**的定位最先回调(最快):

可以看到,最快里没有gps. 说明gps永远比其他的慢.

![image-20221016110506400](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016110506400.png.jpg)

上面的是发起的,那么成功里,真实的provider(location.getProvider())的分布是:

![image-20221016111006236](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016111006236.png.jpg)

#### eachLocationChanged-90分位数据

calledProvider: 

每个provider从start到locationChanged回调的耗时,用于衡量单个定位器的硬件性能

![image-20221016112110300](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016112110300.png.jpg)

calledProvider: 

每个provider从整个定位工具类代码发起定位到locationChanged回调的耗时,用于衡量代码组织的性能:

可以看到gms与上面的偏差比较大, 原因是: gms上报时补报了一次(gms返回的定位超过了配置的定位有效期)时的数据.

![image-20221016112558284](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016112558284.png.jpg)

real provider:

![image-20221016112305420](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016112305420.png.jpg)

# gradle

```groovy
com.github.hss01248.MyPermission:location:1.0.0
com.github.hss01248.MyPermission:permission:1.0.0
```



# 定位坐标系和准确性

>  WGS-84：是国际标准，GPS坐标（Google Earth使用、或者GPS模块）；
>
> GCJ-02：中国坐标偏移标准，Google Map(国内. 国外谷歌地图为 WGS-84坐标系?)、高德、腾讯使用；
>
> BD-09：百度坐标偏移标准，Baidu Map使用；

https://www.hss01248.tech/mapsdemo2022.html?lat=-7.19981408&lng=113.47079679&from=gps

from的取值: 

* gps : Android/ios系统返回的定位信息
* baidu: 百度地图拾取到的定位信息
* gaode  或者tenxun: 高德,腾讯地图拾取到的经纬度

转换的目标坐标系为GCJ-02

内部用到的工具: https://github.com/hujiulong/gcoord

参考: [地图，GPS位置地图坐标系](https://blog.csdn.net/ShareUs/article/details/86695708)



# Android12 定位-模糊定位和精确定位的问题

> android12手机,且app的target sdk>=31时,会有精确位置和大致位置的权限的区分问题. 如下图

![image-20221009180154103](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac2/1665309714286-image-20221009180154103.jpg)

一般来说都是同时申请模糊定位和精确定位权限.

以前的老版本,本工具库已经写死,必须申请精确定位权限,否则回调onError.

目前可支持配置: 是否支持仅请求大致位置:

```java
  LocationUtil.getLocationFast( 15000,
                new MyLocationFastCallback() {

                    @Override
                    public boolean configAcceptOnlyCoarseLocationPermission() {
                        //这里配置是否允许只有模糊位置权限的定位. 默认是false-不允许. 即必须要有精确定位权限.
                        return true;
                    }

                    @Override
                    public boolean configShowLoadingDialog() {
                        return true;
                    }

                    @Override
                    public void onSuccessFast(Location location, String msg) {
                        //ToastUtils.showLong("success," + msg + ", location:" + location);
                        LogUtils.i(msg, location);
                        showFormatedLocationInfoInDialog(location);
                    }

                    @Override
                    public void onFinalFail(int type, String msg, boolean isFailBeforeReallyRequest) {
                        ToastUtils.showLong(type + "," + msg);
                        LogUtils.w(msg, type);
                    }
                });
```



* 只有糊定位权限时,实测:    accuracy=2000米, 中心点**随机偏差**几千米.

* 而有精确定位权限时,不管是network还是gps,fused融合卫星定位,中心点经纬度**都非常准**,偏差仅几十米.

单独申请ACCESS_FINE_LOCATION时,精确定位 android12也会跳出模糊定位的选项,就是上面的截图. 但国产手机有的没有这个界面,面向国外的手机才有. 充分说明了国内的隐私保护情况之恶劣.

![image-20221016102010667](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/image3/image-20221016102010667.png.jpg)

