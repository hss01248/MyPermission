# app定位功能

https://www.runoob.com/w3cnote/android-tutorial-gps.html  定位的相关api和能拿到的所有信息

![企业微信截图_9f90be4e-c16f-4414-81b0-7fe4285bbc8a](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac2/1660708781401-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_9f90be4e-c16f-4414-81b0-7fe4285bbc8a.jpg)

![企业微信截图_223bc4db-b740-44b8-bf9a-52d02fc7c17a](https://cdn.jsdelivr.net/gh/shuiniuhss/myimages@main/imagemac2/1660708794107-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_223bc4db-b740-44b8-bf9a-52d02fc7c17a.jpg)

# provider种类

看似最多能有五种定位渠道:

[passive, gps, network,gms,fused]

实际上只有三种: gps,network.fused

### 定位策略: 饱和式定位

同时发起**所有的可用的定位渠道**

每个定位渠道都会发起requestSingleUpdate + getlastKnownLocation.

然后把定位结果保存到一个数组中, 按定位获取到的时间来倒序,最新的定位在最前面.

然后可以发现一些有趣的现象:

* 都tm是requestSingleUpdate, 直接由locationManager发起的,看它的时间都是实时,而由gms的api发起的,往往会过期一段时间,比如几分钟之前的.
* fused的数据有时和network或gps返回的一模一样(经纬度和时间完全一致.)



### 两种定位模式:

* 快速模式: 同时发起n种,最快回调的那种作为成功回调.--> 适用于业务: 使用:  **MyLocationFastCallback**

* 超时模式: 同时发起n种,超时时间内均拿到定位为结束,或者超时当做结束,结束时,取本次拿到的定位中时间最靠前(最新)的一个.

以上模式的最终失败回调中, 均会再从本地取一次定位缓存,拿到最新的一个缓存并返回成功.

可以配置:

* 实时定位失败时,不取缓存,直接返回失败
* 实时定位失败时,只接受多少时间内的缓存.

通过callback里的方法来配置: 

```java
 /**
     * 默认一年,约等于永久:
     * @return
     */
    default long useCacheInTimeOfMills(){
       return 365*24*60*60*1000L;
    }
```





## 另外一种定位模式: 持续定位

//todo

## 定位的存储维护

```java
LocationSync.java
```

每次通过系统api获取到定位,均调用下面的方法添加到有序列表(list),并序列化到sharedPrefence.

```java
putToCache(Location location, String startProviderName,
                              boolean isFromLastKnowLocation,
                              long timeCost,
                              LocationProvider provider)
```

list中最多保留8个定位,并按定位时间(location.getTime())倒排序.

数据示例:

```json
[
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "fused",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "fused",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 19,
    "timeStamp": 1660718552054,
    "timeStampStr": "2022-08-17 14:42:32"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "passive-lastKnowLocation",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "network",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 2,
    "timeStamp": 1660718552000,
    "timeStampStr": "2022-08-17 14:42:32"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "fused",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "fused",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 38,
    "timeStamp": 1660718550746,
    "timeStampStr": "2022-08-17 14:42:30"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "passive",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "network",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 30,
    "timeStamp": 1660718550712,
    "timeStampStr": "2022-08-17 14:42:30"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "network-lastKnowLocation",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "network",
    "secondsBeforeSaved": 95,
    "speed": 0,
    "timeCost": 7,
    "timeStamp": 1660718455477,
    "timeStampStr": "2022-08-17 14:40:55"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "fused-lastKnowLocation",
    "lattidude": 22.532097,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "fused",
    "secondsBeforeSaved": 95,
    "speed": 0,
    "timeCost": 3,
    "timeStamp": 1660718455476,
    "timeStampStr": "2022-08-17 14:40:55"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "fused",
    "lattidude": 22.53207,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "fused",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 104,
    "timeStamp": 1660718453780,
    "timeStampStr": "2022-08-17 14:40:53"
  },
  {
    "accuracy": 40,
    "altitude": 0,
    "bearing": 0,
    "calledMethod": "network",
    "lattidude": 22.53207,
    "locale": "CN",
    "longtitude": 113.939018,
    "providerInfo": null,
    "realProvider": "network",
    "secondsBeforeSaved": 0,
    "speed": 0,
    "timeCost": 171,
    "timeStamp": 1660718453733,
    "timeStampStr": "2022-08-17 14:40:53"
  }
]
```

