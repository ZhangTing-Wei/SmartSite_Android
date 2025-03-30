package com.example.smartsite;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        final AtomicInteger aqi = new AtomicInteger(0);
//        int aqi = 0;

//        Button btn_1 = view.findViewById(R.id.button);
//        Button btn_2 = view.findViewById(R.id.button2);
//        Button btn_3 = view.findViewById(R.id.button3);
//        Button btn_4 = view.findViewById(R.id.button4);
//        Button btn_5 = view.findViewById(R.id.button5);
//        Button btn_6 = view.findViewById(R.id.button6);

        TextView aqiGrade = view.findViewById(R.id.AQIGrade);

        // 初始化 4 個儀表
        SpeedView gaugeCO = view.findViewById(R.id.gaugeCO);
        SpeedView gaugeO3 = view.findViewById(R.id.gaugeO3);
        SpeedView gaugePM25 = view.findViewById(R.id.gaugePM25);
        SpeedView gaugePM10 = view.findViewById(R.id.gaugePM10);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("air_quality").child(today);

        // 取得最新一筆資料（最大 timestamp）
        databaseRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot dataPoint : snapshot.getChildren()) {
                    Double co = dataPoint.child("co").getValue(Double.class);
                    Double o3 = dataPoint.child("o3").getValue(Double.class);
                    Integer pm10 = dataPoint.child("pm10").getValue(Integer.class);
                    Integer pm25 = dataPoint.child("pm2_5").getValue(Integer.class);
                    Integer aqiValue = dataPoint.child("aqi").getValue(Integer.class);

                    // 更新到 SpeedView 儀表板
                    if (co != null) gaugeCO.speedTo(co.floatValue());
                    if (o3 != null) gaugeO3.speedTo(o3.floatValue() * 1000);
                    if (pm25 != null) gaugePM25.speedTo(pm25);
                    if (pm10 != null) gaugePM10.speedTo(pm10);
                    if (aqiValue != null) {
                        aqi.set(aqiValue);
                        setupGradeText(aqiGrade, aqiValue);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "讀取失敗", error.toException());
            }
        });


        // 設定 CO 儀表範圍與顏色
        setupGauge(gaugeCO, "CO 濃度 (ppm)", 0.0f, 25f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{.33f, .66f, 1f});
//        gaugeCO.speedTo(35);  // 設定初始值

        // 設定 O3 儀表範圍與顏色
        setupGauge(gaugeO3, "O₃ 濃度 (ppb)", 0f, 160f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{.33f, .66f, 1f});
//        gaugeO3.speedTo(256);

        // 設定 PM2.5 儀表範圍與顏色
        setupGauge(gaugePM25, "PM2.5 (µg/m³)", 0f, 200f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{.33f, .66f, 1f});
//        gaugePM25.speedTo(80);

        // 設定 PM10 儀表範圍與顏色
        setupGauge(gaugePM10, "PM10 (µg/m³)", 0f, 500f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{.33f, .66f, 1f});
//        gaugePM10.speedTo(80);

        //        setupGradeText(aqiGrade, aqi);

        // 設定監聽器，當速度變化時更新文字顏色(整數變化下)
//        gaugeCO.setOnSpeedChangeListener((gauge, isSpeedUp, isByTremble) -> {
//            int speed = gauge.getCurrentIntSpeed(); // 取得當前的速度
//
//            // 根據速度設置 AQI 文字顏色
//            if (speed < 8) {
//                gauge.setSpeedTextColor(Color.GREEN); // 低於 8 綠色
//            } else if (speed < 18) {
//                gauge.setSpeedTextColor(Color.rgb(255, 153, 51)); // 8 到 18 橙色
//            } else {
//                gauge.setSpeedTextColor(Color.RED); // 大於 18 紅色
//            }
//
//            return null;
//        });
//
//        gaugeO3.setOnSpeedChangeListener((gauge, isSpeedUp, isByTremble) -> {
//            int speed = gauge.getCurrentIntSpeed(); // 取得當前的速度
//
//            // 根據速度設置 AQI 文字顏色
//            if (speed < 53) {
//                gauge.setSpeedTextColor(Color.GREEN); // 低於 8 綠色
//            } else if (speed < 107) {
//                gauge.setSpeedTextColor(Color.rgb(255, 153, 51)); // 8 到 18 橙色
//            } else {
//                gauge.setSpeedTextColor(Color.RED); // 大於 18 紅色
//            }
//
//            return null;
//        });
//
//        gaugePM25.setOnSpeedChangeListener((gauge, isSpeedUp, isByTremble) -> {
//            int speed = gauge.getCurrentIntSpeed(); // 取得當前的速度
//
//            // 根據速度設置 AQI 文字顏色
//            if (speed < 67) {
//                gauge.setSpeedTextColor(Color.GREEN); // 低於 8 綠色
//            } else if (speed < 133) {
//                gauge.setSpeedTextColor(Color.rgb(255, 153, 51)); // 8 到 18 橙色
//            } else {
//                gauge.setSpeedTextColor(Color.RED); // 大於 18 紅色
//            }
//
//            return null;
//        });
//
//        gaugePM10.setOnSpeedChangeListener((gauge, isSpeedUp, isByTremble) -> {
//            int speed = gauge.getCurrentIntSpeed(); // 取得當前的速度
//
//            // 根據速度設置 AQI 文字顏色
//            if (speed < 167) {
//                gauge.setSpeedTextColor(Color.GREEN); // 低於 8 綠色
//            } else if (speed < 333) {
//                gauge.setSpeedTextColor(Color.rgb(255, 153, 51)); // 8 到 18 橙色
//            } else {
//                gauge.setSpeedTextColor(Color.RED); // 大於 18 紅色
//            }
//
//            return null;
//        });

        gaugeCO.setOnSectionChangeListener((previousSection, newSection) -> {
            if (newSection == null)
                return null;

            // 根據 section 來變換速度文字顏色
            gaugeCO.setSpeedTextColor(newSection.getColor());

            return null;
        });

        gaugeO3.setOnSectionChangeListener((prev, newSec) -> {
            if (newSec == null) return null;
            gaugeO3.setSpeedTextColor(newSec.getColor());
            return null;
        });

        gaugePM25.setOnSectionChangeListener((prev, newSec) -> {
            if (newSec == null) return null;
            gaugePM25.setSpeedTextColor(newSec.getColor());
            return null;
        });

        gaugePM10.setOnSectionChangeListener((prev, newSec) -> {
            if (newSec == null) return null;
            gaugePM10.setSpeedTextColor(newSec.getColor());
            return null;
        });


        // Set up button click listeners to change AQI grade color
//        btn_1.setOnClickListener(v -> {
//            aqi.set(30); // Test with AQI value 30
//            setupGradeText(aqiGrade, aqi.get());
//        });
//
//        btn_2.setOnClickListener(v -> {
//            aqi.set(80); // Test with AQI value 80
//            setupGradeText(aqiGrade, aqi.get());
//        });
//
//        btn_3.setOnClickListener(v -> {
//            aqi.set(120); // Test with AQI value 120
//            setupGradeText(aqiGrade, aqi.get());
//        });
//
//        btn_4.setOnClickListener(v -> {
//            aqi.set(180); // Test with AQI value 180
//            setupGradeText(aqiGrade, aqi.get());
//        });
//
//        btn_5.setOnClickListener(v -> {
//            aqi.set(250); // Test with AQI value 250
//            setupGradeText(aqiGrade, aqi.get());
//        });
//
//        btn_6.setOnClickListener(v -> {
//            aqi.set(350); // Test with AQI value 350
//            setupGradeText(aqiGrade, aqi.get());
//        });


        return view;
    }

    private void setupGradeText(TextView aqiGrade, int aqi) {
        if (aqi >= 0 && aqi <= 50) {
            aqiGrade.setText("良好");
            aqiGrade.setTextColor(Color.GREEN);
        } else if (aqi >= 51 && aqi <= 100) {
            aqiGrade.setText("普通");
            aqiGrade.setTextColor(Color.rgb(255, 215, 0));
        } else if ((aqi >= 101 && aqi <= 150)) {
            aqiGrade.setText("敏感人群不健康");
            aqiGrade.setTextColor(Color.rgb(255, 165, 0));
        } else if (aqi >= 151 && aqi <= 200) {
            aqiGrade.setText("所有族群不健康");
            aqiGrade.setTextColor(Color.rgb(223, 23, 13));
        } else if (aqi >= 201 && aqi <= 300) {
            aqiGrade.setText("非常不健康");
            aqiGrade.setTextColor(Color.rgb(138,43,226));
        } else {
            aqiGrade.setText("危險");
            aqiGrade.setTextColor(Color.rgb(128,0,0));
        }
    }

    private void setupGauge(SpeedView gauge, String title, float min, float max, int[] colors, float[] ranges) {
        gauge.setMaxSpeed(max);
        gauge.clearSections();

        // 設定等級區間顏色
        for (int i = 0; i < colors.length; i++) {
            gauge.addSections(new Section(i == 0 ? min : ranges[i - 1], ranges[i], colors[i], 30f));
        }

//        gauge.setSpeedTextColor(Color.GREEN);

        gauge.setUnit("");  // 設定標題
//        gauge.setUnitTextSize(50f); // 設定標題字體大小
        gauge.setTickNumber(7); //設定刻度數量
        gauge.setTextSize(30f); // 設定刻度數值字體大小
    }

}
