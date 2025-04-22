package com.example.smartsite;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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


        // 初始化數據
        loadCSVData();  // 讀取 CSV 數據

        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        ivPrevDay = view.findViewById(R.id.ivPrevDay);
        ivNextDay = view.findViewById(R.id.ivNextDay);

        calendar = Calendar.getInstance();

        // 初始化當前日期
        updateDateDisplay();

        // 點擊左右箭頭調整日期
        ivPrevDay.setOnClickListener(v -> changeDate(-1)); // 前一天
        ivNextDay.setOnClickListener(v -> changeDate(1));  // 後一天

        // 點擊日期顯示選擇器
        tvSelectedDate.setOnClickListener(v -> showDatePicker());

        return view;
    }

    // 🔹 根據選擇的日期範圍更新折線圖
    private void updateChart() {
        if (allData == null || allData.isEmpty()) {
            Log.e("Chart", "數據為空，無法更新圖表");
            return;
        }

        LineChart[] charts = {lineChartCO, lineChartO3, lineChartPM25, lineChartPM10};
        int[] colors = {
                android.R.color.holo_blue_light,
                android.R.color.holo_purple,
                android.R.color.holo_green_light,
                android.R.color.black
        };

        for (int i = 0; i < allData.size() && i < charts.length; i++) {
            ArrayList<Entry> filteredData = new ArrayList<>();
            for (Entry entry : allData.get(i)) {
                if (entry.getX() >= startDateMillis && entry.getX() <= endDateMillis) {
                    filteredData.add(entry);
                }
            }

            if (!filteredData.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(filteredData, label[i]);
                dataSet.setColor(getResources().getColor(colors[i % colors.length]));
                dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
                dataSet.setLineWidth(2f);
                dataSet.setValueTextSize(10f);
                dataSet.setCircleRadius(2f);
                dataSet.setCircleColors(getResources().getColor(colors[i % colors.length]));
                dataSet.setCircleHoleColor(getResources().getColor(colors[i % colors.length]));
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData lineData = new LineData(dataSet);
                charts[i].setData(lineData);

                // 設定 X 軸格式
                XAxis xAxis = charts[i].getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setLabelRotationAngle(45);
                xAxis.setGranularityEnabled(true);
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(new DateAxisFormatter());

                charts[i].notifyDataSetChanged();
                charts[i].invalidate();
            } else {
                charts[i].clear();
            }
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
            updateChart();
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
            updateChart();
        });
    }

    private void loadCSVData() {
        allData = new ArrayList<>();
        try {
            InputStream inputStream = getActivity().getAssets().open("test_data.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                // 忽略 CSV 第一行標題
                if (firstLine) {
                    firstLine = false;

                    // 初始化 allData（跳過 Timestamp 和 Date 欄位）
                    for (int i = 1; i < parts.length - 1; i++) {
                        allData.add(new ArrayList<>());
                    }
                    continue;
                }

                // 確保數據欄位足夠
                if (parts.length >= 6) {  // 5 個數據欄位 + 1 個日期欄
                    try {
                        long timestamp = Long.parseLong(parts[0]);

                        // 轉換時間戳為毫秒級
                        if (timestamp < 10000000000L) {
                            timestamp *= 1000;
                        }

                        // 解析數據（跳過 Date 欄）
                        for (int i = 1; i < parts.length - 1; i++) {
                            float value = Float.parseFloat(parts[i]);  // 解析數字
                            allData.get(i - 1).add(new Entry(timestamp, value));
                        }
                    } catch (NumberFormatException e) {
                        Log.e("CSV", "數據格式錯誤: " + e.getMessage() + " -> " + line);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CSV", "讀取 CSV 失敗: " + e.getMessage());
        }

        // 確保數據不為空，然後更新圖表
        if (!allData.isEmpty() && !allData.get(0).isEmpty()) {
            startDateMillis = (long) allData.get(0).get(0).getX();
            endDateMillis = (long) allData.get(0).get(allData.get(0).size() - 1).getX();
            updateChart();
        }
    }

}