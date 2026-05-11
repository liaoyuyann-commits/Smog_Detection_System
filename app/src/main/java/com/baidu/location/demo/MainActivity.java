package com.baidu.location.demo;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.demo.view.ChartUtil;
import com.baidu.location.demo.view.RoundProgressBar;
import com.bumptech.glide.Glide;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends CheckPermissionsActivity {

    // --- UI 控件变量 ---
    private TextView mProvince, mCity, mArea, mtemp, mweather, mtime, mweek, mTempRange;
    private TextView mpm10, mpm25, mno2, mso2, mo3, mco, mAdviceTextView;
    private ImageView mbg;
    private RoundProgressBar rpbAqi;
    private LineChart lineChart, lineChart2;
    private LocationClient mLocClientOne = null;

    // --- 配置信息 (你的专属域名和Key) ---
    private final String MY_HOST = "mb7fc3fdfp.re.qweatherapi.com";
    private final String MY_KEY = "cd1e745f9ca547ac86a07a020ffc6815";

    // --- 数据容器 ---
    private ArrayList<String> hum_list = new ArrayList<>();
    private ArrayList<String> max_temp_list = new ArrayList<>();
    private ArrayList<String> min_temp_list = new ArrayList<>();
    private ArrayList<String> date_list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化沉浸式状态栏
        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.hide();
        StatusBarUtil.transparencyBar(this);

        // 2. 初始化 View 绑定
        initView();

        // 3. 设置静态资源
        Glide.with(this).load(R.drawable.bg).into(mbg);
        mweek.setText("星期" + Weektime.StringData());

        // 4. 启动百度定位 (成功后会自动触发数据请求)
        startOneLocaton();
    }

    private void initView() {
        mProvince = findViewById(R.id.province);
        mCity = findViewById(R.id.city);
        mArea = findViewById(R.id.area);
        rpbAqi = findViewById(R.id.rpb_aqi);
        mpm10 = findViewById(R.id.tv_pm10);
        mpm25 = findViewById(R.id.tv_pm25);
        mno2 = findViewById(R.id.tv_no2);
        mso2 = findViewById(R.id.tv_so2);
        mo3 = findViewById(R.id.tv_o3);
        mco = findViewById(R.id.tv_co);
        mtemp = findViewById(R.id.tv_temperature);
        mweather = findViewById(R.id.tv_info);
        mtime = findViewById(R.id.tv_old_time);
        mweek = findViewById(R.id.tv_week);
        mTempRange = findViewById(R.id.tv_temp_range); // 对应新布局中的温差文本
        mAdviceTextView = findViewById(R.id.tv_advice); // 对应新布局中的建议文本
        mbg = findViewById(R.id.bg);
        lineChart = findViewById(R.id.lineChart);
        lineChart2 = findViewById(R.id.lineChart2);
    }

    // --- 数据请求核心逻辑 ---

    private void refreshAllData(double lat, double lon) {
        getAirData(lat, lon);       // 获取空气质量
        getWeatherNow(lat, lon);    // 获取实时天气
        getWeatherForecast(lat, lon); // 获取7天预报
    }

    // 1. 获取空气质量 (V1 接口)
    private void getAirData(double lat, double lon) {
        String url = String.format(Locale.US, "https://%s/airquality/v1/current/%.2f/%.2f?key=%s",
                MY_HOST, lat, lon, MY_KEY);

        doGetRequest(url, responseStr -> {
            JSONObject json = JSON.parseObject(responseStr);
            JSONArray indexes = json.getJSONArray("indexes");
            final String aqi = (indexes != null && indexes.size() > 0) ?
                    indexes.getJSONObject(0).getString("aqiDisplay") : "0";

            JSONArray pollutants = json.getJSONArray("pollutants");
            String p10="0", p25="0", n2="0", s2="0", o3v="0", coV="0";
            if (pollutants != null) {
                for (int i = 0; i < pollutants.size(); i++) {
                    JSONObject p = pollutants.getJSONObject(i);
                    String code = p.getString("code");
                    String val = p.getJSONObject("concentration").getString("value");
                    switch (code) {
                        case "pm10": p10 = val; break;
                        case "pm2p5": p25 = val; break;
                        case "no2": n2 = val; break;
                        case "so2": s2 = val; break;
                        case "o3": o3v = val; break;
                        case "co": coV = val; break;
                    }
                }
            }
            final String f10=p10, f25=p25, fn2=n2, fs2=s2, fo3=o3v, fco=coV;
            runOnUiThread(() -> {
                mpm10.setText(f10); mpm25.setText(f25); mno2.setText(fn2);
                mso2.setText(fs2); mo3.setText(fo3); mco.setText(fco);
                updateAirAndAdvice(aqi);
            });
        });
    }

    // 2. 获取实时天气 (V7 接口)
    private void getWeatherNow(double lat, double lon) {
        String url = String.format(Locale.US, "https://%s/v7/weather/now?location=%.2f,%.2f&key=%s",
                MY_HOST, lon, lat, MY_KEY);

        doGetRequest(url, responseStr -> {
            JSONObject json = JSON.parseObject(responseStr);
            if ("200".equals(json.getString("code"))) {
                JSONObject now = json.getJSONObject("now");
                final String temp = now.getString("temp");
                final String text = now.getString("text");
                final String update = json.getString("updateTime").substring(11, 16);
                runOnUiThread(() -> {
                    mtemp.setText(temp);
                    mweather.setText(text);
                    mtime.setText("最近更新: " + update);
                });
            }
        });
    }

    // 3. 获取7天预报并绘图 (V7 接口)
    private void getWeatherForecast(double lat, double lon) {
        String url = String.format(Locale.US, "https://%s/v7/weather/7d?location=%.2f,%.2f&key=%s",
                MY_HOST, lon, lat, MY_KEY);

        doGetRequest(url, responseStr -> {
            JSONObject json = JSON.parseObject(responseStr);
            if ("200".equals(json.getString("code"))) {
                JSONArray daily = json.getJSONArray("daily");
                date_list.clear(); max_temp_list.clear(); min_temp_list.clear(); hum_list.clear();
                for (int i = 0; i < daily.size(); i++) {
                    JSONObject d = daily.getJSONObject(i);
                    date_list.add(d.getString("fxDate").substring(5));
                    max_temp_list.add(d.getString("tempMax"));
                    min_temp_list.add(d.getString("tempMin"));
                    hum_list.add(d.getString("humidity"));
                }
                runOnUiThread(() -> {
                    // 更新今日温差显示
                    if (max_temp_list.size() > 0) {
                        mTempRange.setText("最高 " + max_temp_list.get(0) + "° / 最低 " + min_temp_list.get(0) + "°");
                    }
                    plotCharts();
                });
            }
        });
    }

    // --- 辅助方法 ---

    private void updateAirAndAdvice(String aqiData) {
        if (aqiData == null || aqiData.isEmpty()) aqiData = "0";
        float aqiVal = Float.parseFloat(aqiData);
        rpbAqi.setMaxProgress(300);
        rpbAqi.setProgress(aqiVal);
        rpbAqi.setFirstText(aqiData);

        // 获取当前温度
        String tempStr = mtemp.getText().toString().replace("°C", "").trim();
        float currentTemp = 22; // 默认舒适温度
        try {
            currentTemp = Float.parseFloat(tempStr);
        } catch (Exception e) { e.printStackTrace(); }

        String advice = "";
        int aqiInt = (int) aqiVal;

        // --- 第一层：判断空气质量 ---
        if (aqiInt <= 50) {


            advice = "【空气优】";
            // 第二层：结合气温给穿衣/出行建议
            if (currentTemp < 10) advice += "空气清新，但天气寒冷，建议穿羽绒服或厚大衣，谨防感冒。";
            else if (currentTemp > 28) advice += "天气晴朗，气温较高，外出请着短袖并注意防晒补水。";
            else advice += "气候宜人，空气纯净，是户外运动的绝佳时机！";

        } else if (aqiInt <= 100) {
            advice = "【空气良】";
            if (currentTemp < 10) advice += "空气质量尚可，体感偏冷，外出请加强保暖，并避开晨间雾气。";
            else if (currentTemp > 28) advice += "气温较高，建议穿着轻便透气衣物。敏感人群室外活动请适度。";
            else advice += "整体舒适，虽有轻微浮尘，但不影响正常出行。";

        } else if (aqiInt <= 200) {
            advice = "【轻中度污染】";
            if (currentTemp < 10) advice += "雾霾来袭且伴随低温，请佩戴N95口罩，穿厚外套，减少冷空气对呼吸道刺激。";
            else if (currentTemp > 28) advice += "高温且有霾，空气闷热，建议减少剧烈运动，尽量留在室内开启空调。";
            else advice += "空气质量较差，建议佩戴防护口罩，回家后及时清洗口鼻。";

        } else {
            advice = "【重度雾霾】";
            // 重度污染时，首要建议是保护呼吸道，无论气温如何
            if (currentTemp < 0) advice += "极寒且重度污染！非必要不出门。若必须外出，务必全副武装：防霾口罩+厚羽绒服。";
            else advice += "空气质量极差，请关闭门窗，开启净化器，避免一切户外锻炼！";
        }

        // 更新 UI
        if (mAdviceTextView != null) {
            mAdviceTextView.setText(advice);
        }
    }

    private void plotCharts() {
        // 温度图
        List<Entry> yTemp = new ArrayList<>();
        for (int i = 0; i < date_list.size(); i++) {
            float avg = (Float.parseFloat(max_temp_list.get(i)) + Float.parseFloat(min_temp_list.get(i))) / 2;
            yTemp.add(new Entry(avg, i));
        }
        ChartUtil.showChart(this, lineChart, new ArrayList<>(date_list), yTemp, "", "平均温度", "℃");

        // 湿度图
        List<Entry> yHum = new ArrayList<>();
        for (int i = 0; i < date_list.size(); i++) {
            yHum.add(new Entry(Float.parseFloat(hum_list.get(i)), i));
        }
        ChartUtil.showChart(this, lineChart2, new ArrayList<>(date_list), yHum, "", "相对湿度", "%");
    }

    private void doGetRequest(String url, final HttpHandler handler) {
        new OkHttpClient().newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { Log.e("WeatherError", e.getMessage()); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) handler.onResult(response.body().string());
            }
        });
    }
    interface HttpHandler { void onResult(String res); }

    // --- 百度定位实现 ---

    private void startOneLocaton() {
        mLocClientOne = new LocationClient(getApplicationContext());
        mLocClientOne.registerLocationListener(oneLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setOnceLocation(true);
        option.setIsNeedAddress(true);
        mLocClientOne.setLocOption(option);
        mLocClientOne.start();
    }

    public BDAbstractLocationListener oneLocationListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null == location) return;
            runOnUiThread(() -> {
                mProvince.setText(location.getProvince());
                mCity.setText(location.getCity());
                mArea.setText(location.getDistrict());
            });
            refreshAllData(location.getLatitude(), location.getLongitude());
        }
    };
}