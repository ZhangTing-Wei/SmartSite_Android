package com.example.smartsite;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LineChart lineChartCO, lineChartO3, lineChartPM25, lineChartPM10;
    private SimpleDateFormat sdf;
    private ArrayList<ArrayList<Entry>> allData;  // 多條折線數據 // 存儲所有數據
    private long startDateMillis, endDateMillis;  // 用戶選擇的時間範圍
    private TextView tvSelectedDate;
    private ImageView ivPrevDay, ivNextDay;
    private Calendar calendar;
    String[] label = {"CO", "O3", "PM2.5", "PM10"};

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // 綁定 UI 元件
        lineChartCO = view.findViewById(R.id.lineChartCO);
        lineChartO3 = view.findViewById(R.id.lineChartO3);
        lineChartPM25 = view.findViewById(R.id.lineChartPM2_5);
        lineChartPM10 = view.findViewById(R.id.lineChartPM10);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        ivPrevDay = view.findViewById(R.id.ivPrevDay);
        ivNextDay = view.findViewById(R.id.ivNextDay);

        calendar = Calendar.getInstance();

        // 初始化當天日期範圍
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        startDateMillis = calendar.getTimeInMillis(); // 當天 00:00:00
        endDateMillis = startDateMillis + 86400000L - 1; // 當天 23:59:59

        // 初始化當前日期
        updateDateDisplay();

        // 初始化數據
        String selectedDateStr = sdf.format(new Date(startDateMillis));  // 例：2025-03-30
        // 載入該日 Firebase 數據
        loadFirebaseData(selectedDateStr);

        // 點擊左右箭頭調整日期
        ivPrevDay.setOnClickListener(v -> changeDate(-1)); // 前一天
        ivNextDay.setOnClickListener(v -> changeDate(1));  // 後一天

        // 點擊日期顯示選擇器
        tvSelectedDate.setOnClickListener(v -> showDatePicker());

        return view;
    }

    // 🔹 根據選擇的日期範圍更新折線圖
    // 🔥完整版 updateChart
    private void updateChart() {
        // 🛡️ 新增這個防呆
        if (!isAdded() || getContext() == null) {
            Log.w("Chart", "Fragment not attached，略過更新圖表");
            return;
        }

        if (allData == null || allData.isEmpty()) {
            Log.e("Chart", "數據為空，無法更新圖表");
            return;
        }

        Log.d("Chart", "更新圖表，數據量：" + allData.get(0).size() +
                ", 時間範圍：" + sdf.format(new Date(startDateMillis)) + " - " +
                sdf.format(new Date(endDateMillis)));

        LineChart[] charts = {lineChartCO, lineChartO3, lineChartPM25, lineChartPM10};

        for (int i = 0; i < allData.size() && i < charts.length; i++) {
            ArrayList<Entry> filteredData = new ArrayList<>();

            // 🔹 只取當天的數據
            for (Entry entry : allData.get(i)) {
                if (entry.getX() >= startDateMillis && entry.getX() < endDateMillis) {
                    filteredData.add(entry);
                }
            }

            if (!filteredData.isEmpty()) {
                float maxY = Float.NEGATIVE_INFINITY;
                float minY = Float.POSITIVE_INFINITY;
                ArrayList<Integer> pointColors = new ArrayList<>();
                for (Entry entry : filteredData) {
                    pointColors.add(getColorForValue(i, entry.getY()));
                    if (entry.getY() > maxY) maxY = entry.getY();
                    if (entry.getY() < minY) minY = entry.getY();
                }

                // 🔹 設定 Y 軸範圍
                float range = maxY - minY;
                if (range < 1f) {
                    range = 1f; // 保底範圍至少 1
                }
                float adjustedMaxY = maxY + range * 0.1f;
                float adjustedMinY = minY - range * 0.1f;


                charts[i].getAxisLeft().setAxisMaximum(adjustedMaxY);
                charts[i].getAxisLeft().setAxisMinimum(adjustedMinY);
                charts[i].getAxisRight().setEnabled(false);

                // 🔹 清空描述跟圖例
                Description description = new Description();
                description.setText("");
                charts[i].setDescription(description);
                charts[i].getLegend().setEnabled(false);

                // 🔹 建立資料線
                LineDataSet dataSet = new LineDataSet(filteredData, label[i]);
                dataSet.setDrawCircles(true);
                dataSet.setCircleColors(pointColors);
                dataSet.setCircleRadius(1f);
                dataSet.setDrawCircleHole(false);
                dataSet.setColor(getResources().getColor(R.color.green));
                dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
                dataSet.setValueTextSize(10f);
                dataSet.setLineWidth(2f);

                LineData lineData = new LineData(dataSet);
                charts[i].setData(lineData);

                // 🔥 這邊是重點：設定X軸範圍 🔥
                XAxis xAxis = charts[i].getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setLabelRotationAngle(30f);
                xAxis.setTextSize(10f);
                xAxis.setLabelCount(4, true);
                xAxis.setAvoidFirstLastClipping(true);
                xAxis.setGranularityEnabled(true);
                xAxis.setGranularity(3600000f); // 1小時
                xAxis.setValueFormatter(new DateAxisFormatter());

                // 最後刷新圖表
                charts[i].notifyDataSetChanged();
                charts[i].invalidate();
            } else {
                charts[i].clear();
            }
        }
    }

    // 🔹 根據污染標準決定顏色
    private int getColorForValue(int index, float value) {
        switch (index) {
            case 0: // CO (ppm)
                if (value < 8) return getResources().getColor(R.color.green);
                if (value < 18) return getResources().getColor(R.color.orange);
                return getResources().getColor(R.color.red);
            case 1: // O3 (ppb)
                if (value < 53) return getResources().getColor(R.color.green);
                if (value < 107) return getResources().getColor(R.color.orange);
                return getResources().getColor(R.color.red);
            case 2: // PM2.5 (µg/m³)
                if (value < 67) return getResources().getColor(R.color.green);
                if (value < 133) return getResources().getColor(R.color.orange);
                return getResources().getColor(R.color.red);
            case 3: // PM10 (µg/m³)
                if (value < 167) return getResources().getColor(R.color.green);
                if (value < 333) return getResources().getColor(R.color.orange);
                return getResources().getColor(R.color.red);
            default:
                return getResources().getColor(R.color.black);
        }
    }

    // 更新日期顯示
    private void updateDateDisplay() {
        tvSelectedDate.setText(sdf.format(calendar.getTime()));
    }

    // 切換日期
    private void changeDate(int days) {
        try {
            // 1️⃣ 取得當前顯示的日期（yyyy-MM-dd）
            String currentDateStr = tvSelectedDate.getText().toString();

            // 2️⃣ 解析為 Date 物件
            Date currentDate = sdf.parse(currentDateStr);
            if (currentDate == null) return;

            // 3️⃣ 轉換為毫秒級時間戳
            long currentMillis = currentDate.getTime();

            // 4️⃣ 加減天數
            long newMillis = currentMillis + (days * 86400000L);

            // 5️⃣ 更新日期範圍
            startDateMillis = newMillis;
            endDateMillis = newMillis + 86400000L;  // ✅ 修正：補足一天的時間

            // 6️⃣ 更新 UI
            tvSelectedDate.setText(sdf.format(new Date(startDateMillis)));

            String selectedDateStr = sdf.format(new Date(newMillis));
            loadFirebaseData(selectedDateStr);

        } catch (Exception e) {
            Log.e("DateError", "日期解析錯誤: " + e.getMessage());
        }
    }

    // 顯示日期選擇器
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇日期")
                .setSelection(calendar.getTimeInMillis())
                .build();

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");

        datePicker.addOnPositiveButtonClickListener(selection -> {
            calendar.setTimeInMillis(selection);
            startDateMillis = selection;
            endDateMillis = selection + 86400000L;  // ✅ 修正：補足一天的時間
            updateDateDisplay();
            // 更新圖表

            String selectedDateStr = sdf.format(new Date(selection));  // 例：2025-03-30

            // 載入該日 Firebase 數據
            loadFirebaseData(selectedDateStr);
        });
    }

    private void loadFirebaseData(String selectedDateStr) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance()
                .getReference("air_quality")
                .child(selectedDateStr); // 例如 "2025-03-30"

        databaseRef.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allData = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    allData.add(new ArrayList<>());
                }

                for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                    try {
                        long timestamp = Long.parseLong(timeSnapshot.getKey());

                        if (timestamp < 10000000000L) {
                            timestamp *= 1000;
                        }

                        Float co = timeSnapshot.child("co").getValue(Float.class);
                        Float o3 = timeSnapshot.child("o3").getValue(Float.class);
                        Float pm25 = timeSnapshot.child("pm2_5").getValue(Float.class);
                        Float pm10 = timeSnapshot.child("pm10").getValue(Float.class);

                        if (co != null) allData.get(0).add(new Entry(timestamp, co));
                        if (o3 != null) allData.get(1).add(new Entry(timestamp, o3));
                        if (pm25 != null) allData.get(2).add(new Entry(timestamp, pm25));
                        if (pm10 != null) allData.get(3).add(new Entry(timestamp, pm10));

                    } catch (Exception e) {
                        Log.e("Firebase", "資料格式錯誤: " + e.getMessage());
                    }
                }

                updateChart();  // 更新折線圖
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "讀取資料失敗: " + error.getMessage());
            }
        });
    }

}