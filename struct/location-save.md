# LocationSync —— Location 对象存取逻辑

## 1. 核心数据结构

### 1.1 内存缓存

```java
// 有序的定位信息列表，按 timeStamp 降序排列（最新的在前），最多 10 条
private static final List<LocationInfo> cachedLocations = new CopyOnWriteArrayList<>();
private static final int maxCacheCount = 10;
```

### 1.2 LocationInfo

自定义 POJO，对 `android.location.Location` 的二次封装，包含：

| 字段 | 说明 |
|---|---|
| `lattidude` / `longtitude` | 经纬度 |
| `timeStamp` | `Location.getTime()` |
| `elapsedRealtimeNanos` | 系统启动时间戳 |
| `accuracy` | 精度（米），越小越准 |
| `altitude` / `speed` / `bearing` | 海拔、速度、方向 |
| `realProvider` | 实际 provider（gps / network） |
| `calledMethod` | 调用来源标记，如 `gps`、`gms`、`gms-lastKnowLocation` |
| `isFromMockProvider` | 是否模拟定位 |
| `timeCost` / `costFromBegin` | 定位耗时统计 |
| `hasFineLocationPermission` | 是否拥有精确定位权限 |
| `millsOldWhenSaved` | 存入时，该定位已过期了多少毫秒 |

**去重逻辑**：`equals` 基于 `longtitude + lattidude + timeStamp`，三者相同视为同一条数据。

### 1.3 持久化接口

```
ILocationCache
├── getLocationJasonArrStr()   // 读取 JSON 字符串
└── saveLocations(String json) // 保存 JSON 字符串
```

默认实现 `DefaultLocationCache`：通过 `SPUtils`（SharedPreferences）读写，key 为 `"cachedLocations"`。
可通过 `initAsync(ILocationCache)` 替换为自定义实现。

---

## 2. 存入流程（写入）

### 2.1 入口 `putToCache()`

调用方：`QuietLocationUtil`、`ContinueLocationUtil` 在获取到定位回调或 lastKnownLocation 时调用。

```
putToCache(location, providerName, isFromLastKnowLocation, timeCost, costFromBegin)
```

### 2.2 完整流程

```
putToCache()
  │
  ├── 1. null 检查
  │
  ├── 2. toLocationInfo(location)
  │       将 android.location.Location → LocationInfo
  │       包括从 location.getExtras() Bundle 中还原额外字段
  │
  ├── 3. 模拟定位检查
  │       if (isFromMockProvider && !acceptFakeLocation) → 丢弃
  │
  ├── 4. 补充元信息
  │       ├── timeCost / costFromBegin
  │       ├── calledMethod（拼接 provider + "-lastKnowLocation"）
  │       ├── millsOldWhenSaved（非 lastKnownLocation 时计算）
  │       ├── hasFineLocationPermission
  │       └── isFromMockProvider
  │
  ├── 5. saveExtraToLocation(location, info)
  │       将元信息写回 location 的 Bundle.extras
  │       使 Location 对象本身也携带这些扩展字段
  │
  ├── 6. sortBeforeAdd(info, cachedLocations)  ← synchronized
  │       ├── 去重：cachedLocations.contains(info) → 跳过
  │       ├── 过旧检查：缓存已满且新数据时间 < 最旧那条 → 跳过
  │       ├── 排序：按 timeStamp 降序
  │       └── 裁剪：超过 maxCacheCount(10) 则截取前10条
  │
  └── 7. saveAsync()  ← synchronized
          cachedLocations → GsonUtils.toJson → locationCache.saveLocations(json)
          即写入 SharedPreferences
```

### 2.3 数据双写

| 存储层 | 方式 | 时机 |
|---|---|---|
| 内存 `cachedLocations` | `sortBeforeAdd()` 中直接操作 | 每次 putToCache |
| SP 持久化 | `saveAsync()` → `locationCache.saveLocations()` | 每次成功写入内存后 |

---

## 3. 读取流程

### 3.1 初始化加载 `initAsync()`

应用启动时调用，从持久化恢复缓存：

```
initAsync(locationCache)
  │
  ├── 保存自定义 ILocationCache 实现
  │
  └── 异步线程:
        ├── locationCache.getLocationJasonArrStr()  ← 从 SP 读 JSON
        ├── GsonUtils.fromJson → List<LocationInfo>
        ├── cachedLocations.clear() + addAll()
        └── sort()  ← 按 timeStamp 降序排列
```

用 `hasAsync` 标志位保证只加载一次。

### 3.2 获取最优定位 `getFullLocationInfo()`

核心读取方法，返回单条最优 `LocationInfo`：

```
getFullLocationInfo()
  │
  ├── 遍历 cachedLocations
  │     筛选条件: (now - timeStamp) <= maxFreshTimeMsForBestLocation (默认45s)
  │     在新鲜数据中找 accuracy 最小（最精确）的
  │
  ├── 如果找到 → 返回该条（45s 内最精确）
  │
  └── 如果未找到（全部超过45s）→ 返回 cachedLocations.get(0)（最新的）
```

### 3.3 对外获取接口

| 方法 | 返回类型 | 逻辑 |
|---|---|---|
| `getFullLocationInfo()` | `LocationInfo` | **推荐使用**。返回最优定位信息 |
| `getLocation3()` | `Location` | 调用 `getFullLocationInfo()` → `toAndroidLocation()` 转回 Android Location |
| `getLocation()` | `Location` | **@Deprecated**，内部调用 `getLocation3()` |
| `getLatitude()` | `double` | **@Deprecated**，调 `getFullLocationInfo()` 取纬度 |
| `getLongitude()` | `double` | 调 `getFullLocationInfo()` 取经度 |
| `getAddress()` | `Address` | 从内存 `ADDRESS_MAP` 读取，与 Location 缓存体系无关 |

---

## 4. 转换方法

### Location → LocationInfo：`toLocationInfo()`

```
提取: lat, lng, time, altitude, accuracy, bearing, speed, provider, elapsedRealtimeNanos, isFromMockProvider
解析 Bundle extras: calledMethod, millsOldWhenSaved, timeCost, costFromBegin, hasFineLocationPermission, isFromMockProvider
```

### LocationInfo → Location：`toAndroidLocation()`

```
新建 Location(realProvider)
设置: altitude, time, longitude, accuracy, latitude, bearing, speed, elapsedRealtimeNanos
调 saveExtraToLocation() 将扩展字段写入 Bundle extras
```

---

## 5. 流程总览图

```
┌────────────────────────────────────────────────────────────────────┐
│                         写入流程                                   │
│                                                                    │
│  QuietLocationUtil / ContinueLocationUtil                          │
│        │                                                           │
│        ▼                                                           │
│  putToCache(Location, provider, isLastKnown, timeCost, cost)       │
│        │                                                           │
│        ├── toLocationInfo(Location) ──→ LocationInfo               │
│        ├── 模拟定位过滤                                             │
│        ├── 补充元信息(timeCost, calledMethod, accuracy等)            │
│        ├── saveExtraToLocation(Location ← info)                    │
│        ├── sortBeforeAdd() → 内存缓存(去重+排序+裁剪, max=10)        │
│        └── saveAsync() → SP 持久化 (JSON)                           │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│                         读取流程                                   │
│                                                                    │
│  initAsync() ─── SP → JSON → List<LocationInfo> → cachedLocations  │
│                                                                    │
│  getFullLocationInfo()                                             │
│        │                                                           │
│        ├── 45s 内最精确(accuracy最小)的 → 返回                       │
│        └── 全部超时 → 返回最新一条                                    │
│                                                                    │
│  getLocation3() → toAndroidLocation(LocationInfo) → Location       │
└────────────────────────────────────────────────────────────────────┘
```

---

## 6. 其他存取（Address）

`Address` 对象使用独立的 `HashMap` 内存缓存，不涉及持久化：

```java
saveAddress(Address) → ADDRESS_MAP.put("addressxx", address)
getAddress()         → ADDRESS_MAP.get("addressxx")
```

---

## 7. 已废弃方法

| 方法 | 说明 |
|---|---|
| `save(double, double)` | 原经纬度直接存 SP，已空实现 |
| `saveLocation(Location)` | 原 Location 存内存 Map，已空实现 |
| `getLocation()` | 替换为 `getLocation3()` 或 `getFullLocationInfo()` |
| `getLatitude()` | 替换为 `getFullLocationInfo().lattidude` |
| `initAsync()` (无参) | 替换为 `initAsync(ILocationCache)` |
