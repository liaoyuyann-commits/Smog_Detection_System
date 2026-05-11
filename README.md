# Smog_Detection_System（雾霾探测系统）

一款基于 Android 原生开发的智能空气质量监测与气象预报应用，集成百度地图 LBS 定位与和风天气 QWeather V7 高精度接口，为用户提供实时雾霾探测、多污染物数据展示、可视化图表及个性化健康防护建议。

> **技术声明**：本项目 README 文档及部分核心业务逻辑（V7 数据解析、专属 Host 拼接、建议算法）由 AI 辅助编写优化。

---

## 目录

1. [核心功能](#核心功能)
2. [开发环境](#开发环境)
3. [项目部署与运行](#项目部署与运行)
4. [百度地图 SDK 配置](#百度地图-sdk-配置)
5. [和风天气 API 配置](#和风天气-api-配置)
6. [核心技术实现](#核心技术实现)
7. [改动说明](#改动说明)
8. [结果演示](#结果演示)
9. [参考资料](#参考资料)
10. [问题反馈](#问题反馈)

---

## 核心功能

- **高精度定位**：基于百度地图 SDK，获取经纬度与省市区三级行政区划
- **气象实况监测**：实时展示气温（含每日最高/最低）、天气状况、湿度、更新时间
- **空气质量矩阵**：支持 AQI 指数、PM2.5、PM10、SO₂、NO₂、O₃、CO 六大污染物展示
- **多因子智能防护建议**：AQI 等级 × 气温区间交叉判断，动态生成防护文案
- **数据可视化**：MPAndroidChart 绘制未来 7 日温湿度变化曲线

---

## 开发环境

| 配置项 | 版本 |
|--------|------|
| IDE | Android Studio Arctic Fox (2020.3.1) 及以上 |
| JDK | 1.8 (Java 8) |
| Gradle | 3.6.2 |
| MinSDK | API 21 (Android 5.0) |
| 关键依赖 | OkHttp、Alibaba FastJSON、MPAndroidChart |

---

## 项目部署与运行

```bash
git clone https://github.com/你的用户名/Smog_Detection_System.git
```

1. Android Studio → **Open** 导入项目
2. 按下方说明分别配置百度地图密钥和和风天气密钥
3. 连接 Android 真机，开启**开发者模式**和 **USB 调试**，USB 用途选择 **MTP（传输文件）**
4. 编译安装后，首次运行务必允许 App 获取 **GPS 定位权限**

---

## 百度地图 SDK 配置

### 第一步：获取 SHA1 签名

> ⚠️ **注意：SHA1 有调试版（Debug）和发布版（Release）两种，务必区分！**
>
> - **调试版 SHA1**：仅在开发调试时有效，APK 安装到其他设备后定位功能将失效。
> - **发布版 SHA1**：打包正式 APK 时必须使用此版本，否则其他设备无法定位。
>
> 建议：申请 AK 时填写**发布版 SHA1**，或在百度控制台同时创建两个 AK（分别对应调试版和发布版）。

在 Android Studio Terminal 中执行以下命令获取签名信息：

```bash
./gradlew signingReport
```

输出结果中找到 `Variant: release` 下的 `SHA1` 字段即为发布版。

### 第二步：确认包名

查看 `app/build.gradle` 中的 `applicationId`，例如：

```gradle
applicationId "com.baidu.location.demo"
```

### 第三步：申请 API Key

登录[百度地图开放平台](https://lbsyun.baidu.com/)，创建应用时填入包名与 SHA1，获取 AK。

### 第四步：写入项目

在 `app/src/main/AndroidManifest.xml` 中替换密钥：

```xml
<meta-data
    android:name="com.baidu.lbsapi.API_KEY"
    android:value="你的百度地图API_KEY" />
```

---

## 和风天气 API 配置

### MY_KEY

登录[和风天气控制台](https://dev.qweather.com/)，创建项目后获取 Key，填入 `MainActivity.java`：

```java
private static final String MY_KEY = "你的和风天气Key";
```

### MY_HOST（重要）

`MY_HOST` 是你在和风天气后台分配到的**专属 API 接入域名**，每个账号/项目不同。请勿填写通用示例域名，应登录控制台查看你自己的专属地址。

登录和风天气控制台 → **项目管理** → 找到你的项目 → 查看「API Host」或「专属接入点」字段，格式通常如下：

```
xxxxxxxx.re.qweatherapi.com
```

将其填入 `MainActivity.java`：

```java
private static final String MY_HOST = "你的专属Host域名";
```

> **为什么不能随便填？**  
> 和风天气 V7 接口采用专属域名鉴权，直接使用 `devapi.qweather.com` 或 `api.qweather.com` 等通用地址在新版账号体系下可能导致请求失败。你的 Host 与你的 Key 是绑定的，必须配套使用。

---

## 核心技术实现

### 1. 百度地图 SDK 定位配置

采用高精度模式定位，返回百度坐标系（bd09ll）经纬度，用于对接和风天气接口：

```java
LocationClientOption option = new LocationClientOption();
// 高精度定位模式
option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
// 使用百度坐标系
option.setCoorType("bd09ll");
// 返回详细地址（省市区）
option.setIsNeedAddress(true);
mLocationClient.setLocOption(option);
```

### 2. 和风天气 V7 接口封装

通过经纬度动态拼接请求路径，精准获取对应位置的气象与空气质量数据：

```java
private String getUrl(double lon, double lat, String type) {
    // type 传 "weather" 或 "airquality/v1/current"
    String baseUrl = "https://" + MY_HOST + "/v7/";
    return String.format(Locale.US, "%s%s/now?location=%.2f,%.2f&key=%s",
                         baseUrl, type, lon, lat, MY_KEY);
}
```

> 空气质量接口路径结构与天气接口略有不同，请参考[和风天气 V7 文档](https://dev.qweather.com/docs/)。

### 3. OkHttp 异步请求 + FastJSON 解析

在子线程发起网络请求，解析 V7 嵌套 JSON 后切回主线程更新 UI：

```java
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder().url(apiUrl).build();
client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        String res = response.body().string();
        JSONObject data = JSON.parseObject(res);
        runOnUiThread(() -> {
            mtemp.setText(data.getJSONObject("now").getString("temp") + "°C");
            // 其他 UI 更新...
        });
    }
    @Override
    public void onFailure(Call call, IOException e) {
        e.printStackTrace();
    }
});
```

### 4. 多因子智能防护建议算法

结合 AQI 污染等级与实时气温进行交叉判断，动态生成个性化防护文案：

```java
private void updateAirAndAdvice(String aqiData) {
    if (aqiData == null || aqiData.isEmpty()) aqiData = "0";
    float aqiVal = Float.parseFloat(aqiData);
    rpbAqi.setMaxProgress(300);
    rpbAqi.setProgress(aqiVal);
    rpbAqi.setFirstText(aqiData);

    String tempStr = mtemp.getText().toString().replace("°C", "").trim();
    float currentTemp = 22; // 默认舒适温度
    try {
        currentTemp = Float.parseFloat(tempStr);
    } catch (Exception e) { e.printStackTrace(); }

    String advice;
    int aqiInt = (int) aqiVal;

    if (aqiInt <= 50) {
        advice = "【空气优】";
        if (currentTemp < 10)      advice += "空气清新，但天气寒冷，建议穿羽绒服，谨防感冒。";
        else if (currentTemp > 28) advice += "天气晴朗，气温较高，外出请着短袖并注意防晒补水。";
        else                        advice += "气候宜人，空气纯净，是户外运动的绝佳时机！";

    } else if (aqiInt <= 100) {
        advice = "【空气良】";
        if (currentTemp < 10)      advice += "空气尚可，体感偏冷，外出请加强保暖，避开晨间雾气。";
        else if (currentTemp > 28) advice += "气温较高，建议穿着轻便透气。敏感人群请适度活动。";
        else                        advice += "整体舒适，有轻微浮尘，不影响正常出行。";

    } else if (aqiInt <= 200) {
        advice = "【轻中度污染】";
        if (currentTemp < 10)      advice += "雾霾伴低温，请佩戴N95口罩并穿厚外套，减少冷空气刺激。";
        else if (currentTemp > 28) advice += "高温有霾，空气闷热，建议减少剧烈运动，尽量留在室内。";
        else                        advice += "空气较差，建议佩戴防护口罩，回家后及时清洗口鼻。";

    } else {
        advice = "【重度雾霾】";
        if (currentTemp < 0) advice += "极寒重污染！非必要不出门。若必须外出：防霾口罩 + 厚羽绒服。";
        else                  advice += "空气质量极差，请关闭门窗，开启净化器，避免一切户外锻炼！";
    }

    if (mAdviceTextView != null) {
        mAdviceTextView.setText(advice);
    }
}
```

---

## 改动说明

本项目基于开源项目 [Weather_App](https://github.com/zstar1003/Weather_App) 进行二次开发，主要改动如下：

**文件改动：**
- `MainActivity.java`：针对和风天气 V7 接口完全重写，适配专属 Host 接入与新版数据结构
- `activity_main.xml`：优化 UI 层级，增加每日最高/最低气温看板与智能建议展示区域

**新增功能：**
- 每日极值气温展示（最高/最低气温）
- AQI × 气温双因子智能防护建议算法

---

## 结果演示

![演示图1](2ee62ebf151bc992ff3c637b30d9ca13.jpg)
![演示图2](25641f5ed749f3aa08e836eaaed88f2d.jpg)

---

## 参考资料

1. 基础架构参考：[Weather_App (zstar1003)](https://github.com/zstar1003/Weather_App)
2. 百度地图定位 SDK 集成：[CSDN - 百度地图 Android 定位 SDK 使用详解](https://blog.csdn.net/qq1198768105/article/details/125130786)
3. V7 接口适配参考：[CSDN - 和风天气 V7 接口封装](https://blog.csdn.net/zggq323/article/details/147223798)
4. 百度地图 Android 定位 SDK 官方文档
5. 和风天气 QWeather V7 开发文档

---

## 问题反馈

如在配置 SDK、申请 Key、确定专属 Host 或运行代码时遇到问题，欢迎联系作者：

- **QQ**：`2644081990`
- **添加备注**：**"雾霾APP咨询"** 或说明来意

---

*最后更新：2026.05.11*
