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



# gradle

