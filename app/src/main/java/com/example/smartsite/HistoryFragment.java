package com.example.smartsite;

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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LineChart lineChart;
    private EditText etDateRange;
    private SimpleDateFormat sdf;
    private ArrayList<Entry> allData;  // 存儲所有數據
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
        initData();

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

    // 🔹 初始化數據
    private void initData() {
        allData = new ArrayList<>();
        allData.add(new Entry(1738387200000f, 1f));  // 2025-02-01
        allData.add(new Entry(1738473600000f, 2f));  // 2025-02-02
        allData.add(new Entry(1738560000000f, 3f));  // 2025-02-03
        allData.add(new Entry(1738646400000f, 4f));  // 2025-02-04
        allData.add(new Entry(1738732800000f, 5f));  // 2025-02-05
        allData.add(new Entry(1738819200000f, 6f));  // 2025-02-06
        allData.add(new Entry(1738905600000f, 7f));  // 2025-02-07
        allData.add(new Entry(1738992000000f, 8f));  // 2025-02-08

        // 默認顯示全部數據
        startDateMillis = (long) allData.get(0).getX();
        endDateMillis = (long) allData.get(allData.size() - 1).getX();
        updateChart();
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
        ArrayList<Entry> filteredData = new ArrayList<>();
        for (Entry entry : allData) {
            if (entry.getX() >= startDateMillis && entry.getX() <= endDateMillis) {
                filteredData.add(entry);
            }
        }

        // 創建數據集
        LineDataSet dataSet = new LineDataSet(filteredData, "歷史數據");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));  // 線的顏色
        dataSet.setValueTextColor(getResources().getColor(android.R.color.holo_red_light));  // 數值顏色
        dataSet.setLineWidth(2f);  //折線寬度
        dataSet.setValueTextSize(10f);  //數值字體大小
        dataSet.setCircleRadius(5f);  //點大小

        // 設定 X 軸格式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(90);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new DateAxisFormatter());  // 使用自訂的 DateAxisFormatter

        // 設定新數據
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();  // 刷新圖表
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
}
