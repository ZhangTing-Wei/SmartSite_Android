package com.example.smartsite;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;

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

        Button btn_1 = view.findViewById(R.id.button);
        Button btn_2 = view.findViewById(R.id.button2);
        Button btn_3 = view.findViewById(R.id.button3);
        Button btn_4 = view.findViewById(R.id.button4);
        Button btn_5 = view.findViewById(R.id.button5);
        Button btn_6 = view.findViewById(R.id.button6);

        TextView aqiGrade = view.findViewById(R.id.AQIGrade);

        // 初始化 4 個儀表
        SpeedView gaugeCO = view.findViewById(R.id.gaugeCO);
        SpeedView gaugeO3 = view.findViewById(R.id.gaugeO3);
        SpeedView gaugePM25 = view.findViewById(R.id.gaugePM25);
        SpeedView gaugePM10 = view.findViewById(R.id.gaugePM10);

        // 設定 CO 儀表範圍與顏色
        setupGauge(gaugeCO, "CO 濃度 (ppm)", 0f, 50.4f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{0.187f, 0.304f, 1.0f});
        gaugeCO.speedTo(35);  // 設定初始值

        // 設定 O3 儀表範圍與顏色
        setupGauge(gaugeO3, "O₃ 濃度 (ppb)", 0f, 604f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{0.116f, 0.172f, 1.0f});
        gaugeO3.speedTo(256f);

        // 設定 PM2.5 儀表範圍與顏色
        setupGauge(gaugePM25, "PM2.5 (µg/m³)", 0f, 500.4f, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{0.061f, 0.251f, 1.0f});
        gaugePM25.speedTo(80);

        // 設定 PM10 儀表範圍與顏色
        setupGauge(gaugePM10, "PM10 (µg/m³)", 0f, 604, new int[]{Color.GREEN, Color.rgb(255, 165, 0), Color.RED}, new float[]{0.124f, 0.584f, 1.0f});
        gaugePM10.speedTo(80);

//        setupGradeText(aqiGrade, aqi);

        // Set up button click listeners to change AQI grade color
        btn_1.setOnClickListener(v -> {
            aqi.set(30); // Test with AQI value 30
            setupGradeText(aqiGrade, aqi.get());
        });

        btn_2.setOnClickListener(v -> {
            aqi.set(80); // Test with AQI value 80
            setupGradeText(aqiGrade, aqi.get());
        });

        btn_3.setOnClickListener(v -> {
            aqi.set(120); // Test with AQI value 120
            setupGradeText(aqiGrade, aqi.get());
        });

        btn_4.setOnClickListener(v -> {
            aqi.set(180); // Test with AQI value 180
            setupGradeText(aqiGrade, aqi.get());
        });

        btn_5.setOnClickListener(v -> {
            aqi.set(250); // Test with AQI value 250
            setupGradeText(aqiGrade, aqi.get());
        });

        btn_6.setOnClickListener(v -> {
            aqi.set(350); // Test with AQI value 350
            setupGradeText(aqiGrade, aqi.get());
        });


        return view;
    }

    private void setupGradeText(TextView aqiGrade, int aqi) {
//        aqiGrade.setText("test");
//        if (aqi >= 0 && aqi <= 50)
//            aqiGrade.setTextColor(Color.GREEN);
//        else if (aqi >= 51 && aqi <= 100)
//            aqiGrade.setTextColor(Color.YELLOW);
//        else if ((aqi >= 101 && aqi <= 150))
//            aqiGrade.setTextColor(Color.rgb(255, 165, 0));
//        else if (aqi >= 151 && aqi <= 200)
//            aqiGrade.setTextColor(Color.MAGENTA);
//        else if (aqi >= 201 && aqi <= 300)
//            aqiGrade.setTextColor(Color.rgb(138,43,226));
//        else
//            aqiGrade.setTextColor(Color.rgb(128,0,0));

        if (aqi >= 0 && aqi <= 50) {
            aqiGrade.setText("良好");
            aqiGrade.setTextColor(Color.GREEN);
        } else if (aqi >= 51 && aqi <= 100) {
            aqiGrade.setText("普通");
//            aqiGrade.setTextColor(Color.YELLOW);
            aqiGrade.setTextColor(Color.rgb(240, 230, 140));
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

        gauge.setUnit("");  // 設定標題
//        gauge.setUnitTextSize(50f); // 設定標題字體大小
        gauge.setTickNumber(7); //設定刻度數量
        gauge.setTextSize(30f); // 設定刻度數值字體大小
    }

}
