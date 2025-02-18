package com.example.smartsite;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.core.util.Pair;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;

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

    private LineChart lineChart;
    private EditText etDateRange;
    private SimpleDateFormat sdf;
    private ArrayList<ArrayList<Entry>> allData;  // 多條折線數據 // 存儲所有數據
    private long startDateMillis, endDateMillis;  // 用戶選擇的時間範圍
    private TextView tvSelectedDate;
    private ImageView ivPrevDay, ivNextDay;
    private Calendar calendar;


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
        lineChart = view.findViewById(R.id.lineChart);
        etDateRange = view.findViewById(R.id.etDateRange);

        // 初始化數據
//        initData();
        loadCSVData();  // 讀取 CSV 數據

        // 點擊日期選擇框，顯示日期範圍選擇器
        etDateRange.setOnClickListener(v -> showDateRangePicker());

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

    // 🔹 顯示日期範圍選擇器
    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("選擇日期範圍");

        // 限制選擇的日期不能超過今天
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointBackward.now());
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = builder.build();
        dateRangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");

        // 處理選擇結果
        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            startDateMillis = selection.first;
            endDateMillis = selection.second + 86400000L;  // ✅ 修正：補足一天的時間

            // 顯示選擇的日期範圍
            etDateRange.setText(sdf.format(new Date(startDateMillis)) + " - " + sdf.format(new Date(endDateMillis - 86400000L)));

            // 更新圖表
            updateChart();
        });
    }

    // 🔹 根據選擇的日期範圍更新折線圖
    private void updateChart() {
        if (allData == null || allData.isEmpty()) {
            Log.e("Chart", "數據為空，無法更新圖表");
            return;
        }

        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        int[] colors = {
                android.R.color.holo_blue_light,
                android.R.color.holo_red_light,
                android.R.color.holo_green_light,
                android.R.color.holo_green_light
//                android.R.color.holo_orange_light
        };

        for (int i = 0; i < allData.size(); i++) {
            ArrayList<Entry> filteredData = new ArrayList<>();
            for (Entry entry : allData.get(0)) {
                if (entry.getX() >= startDateMillis && entry.getX() <= endDateMillis) {
                    filteredData.add(entry);
                }
            }

            if (!filteredData.isEmpty()) {
                LineDataSet dataSet = new LineDataSet(filteredData, "數據 " + (i + 1));
                dataSet.setColor(getResources().getColor(colors[i % colors.length]));
                dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
                dataSet.setLineWidth(2f);
                dataSet.setValueTextSize(10f);
                dataSet.setCircleRadius(2f);
                dataSet.setCircleColors(getResources().getColor(colors[i % colors.length]));
                dataSet.setCircleHoleColor(getResources().getColor(colors[i % colors.length]));
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSets.add(dataSet);
            }
        }

        if (dataSets.isEmpty()) {
            Log.e("Chart", "沒有符合篩選條件的數據");
            return;
        }

        // 設定 X 軸格式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(45);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new DateAxisFormatter());

        // 設定 Y 軸限制線 (黃線在 Y=95)
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines(); // 移除舊的標記線
        yAxis.setAxisMinimum(85f);
        yAxis.setAxisMaximum(105f);
        LimitLine limitLine = new LimitLine(95f);
        limitLine.setLineColor(Color.YELLOW);
        limitLine.setLineWidth(2f);
        limitLine.setTextColor(Color.BLACK);
        limitLine.setTextSize(12f);
        yAxis.addLimitLine(limitLine);

        // 設定多條數據
        LineData lineData = new LineData();
        for (LineDataSet dataSet : dataSets) {
            lineData.addDataSet(dataSet);
        }

        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    // 更新日期顯示
    private void updateDateDisplay() {
        tvSelectedDate.setText(sdf.format(calendar.getTime()));
    }

    // 切換日期
    private void changeDate(int days) {
        calendar.add(Calendar.DAY_OF_MONTH, days);
        updateDateDisplay();
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
            updateDateDisplay();
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