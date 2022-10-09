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



## 获取定位: LocationUtil

> 饱和式救援

```java
//简易api 默认10s超时. 拒绝后有一次挽回弹窗
getLocation(Context context, MyLocationCallback callback)

//全配置api
getLocation(Context context, int timeout, IPermissionDialog dialogBeforeRequest,
                                   IPermissionDialog dialogAfterDenied, MyLocationCallback callback) 


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

耗时:

![image-20211214192158731](https://cdn.jsdelivr.net/gh/hss01248/picbed@master/pic/1639480923963-image-20211214192158731.jpg)



![image-20211214192410958](https://cdn.jsdelivr.net/gh/hss01248/picbed@master/pic/1639481051011-image-20211214192410958.jpg)

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

![image-20221009180154103](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac2/1665309714286-image-20221009180154103.jpg)

一般来说都是同时申请模糊定位和精确定位权限.本工具库已经写死,必须申请精确定位权限,否则回调onError.

>  经改代码测试: 

如果单独申请模糊定位权限,实测:    accuracy=2000米, 中心点随机偏差几千米

单独申请ACCESS_FINE_LOCATION时,精确定位 android12也会跳出模糊定位的选项,就是上面的截图. 但国产手机有的没有这个界面,面向国外的手机才有. 充分说明了国内的隐私保护情况之恶劣.

