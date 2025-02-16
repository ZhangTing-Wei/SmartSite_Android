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
import com.github.mikephil.charting.components.XAxis;
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
//        updateChart();
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
//            updateChart();
        });
    }

    // 🔹 根據選擇的日期範圍更新折線圖
    private void updateChart(ArrayList<Entry> data1, ArrayList<Entry> data2, ArrayList<Entry> data3, ArrayList<Entry> data4) {
        if (data1.isEmpty() || data2.isEmpty() || data3.isEmpty() || data4.isEmpty()) {
            lineChart.setNoDataText("沒有數據可顯示");
            lineChart.invalidate();
            return;
        }

        LineDataSet dataSet1 = new LineDataSet(data1, "數據1");
        dataSet1.setColor(Color.BLUE);
        dataSet1.setLineWidth(2f);
        dataSet1.setCircleRadius(4f);

//        LineDataSet dataSet2 = new LineDataSet(data2, "數據2");
//        dataSet2.setColor(Color.GREEN);
//        dataSet2.setLineWidth(2f);
//        dataSet2.setCircleRadius(4f);
//
//        LineDataSet dataSet3 = new LineDataSet(data3, "數據3");
//        dataSet3.setColor(Color.YELLOW);
//        dataSet3.setLineWidth(2f);
//        dataSet3.setCircleRadius(4f);
//
//        LineDataSet dataSet4 = new LineDataSet(data4, "數據4");
//        dataSet4.setColor(Color.MAGENTA);
//        dataSet4.setLineWidth(2f);
//        dataSet4.setCircleRadius(4f);

//        LineData lineData = new LineData(dataSet1, dataSet2, dataSet3, dataSet4);
        LineData lineData = new LineData(dataSet1);
        lineChart.setData(lineData);

        // 🔹 修正 X 軸為日期格式
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new DateAxisFormatter());
        xAxis.setGranularity(1f); // 避免標籤太擁擠
        xAxis.setLabelRotationAngle(45);

        lineChart.setVisibleXRangeMaximum(10);
        lineChart.moveViewToX(startDateMillis);
        lineChart.invalidate();
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
        ArrayList<Entry> data1 = new ArrayList<>();
        ArrayList<Entry> data2 = new ArrayList<>();
        ArrayList<Entry> data3 = new ArrayList<>();
        ArrayList<Entry> data4 = new ArrayList<>();

        try {
            InputStream inputStream = getActivity().getAssets().open("test_data.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {  // 跳過標題行
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 5) {  // 確保有足夠的數據列
                    long timestamp = (long) Double.parseDouble(parts[0]);
                    data1.add(new Entry(timestamp, Float.parseFloat(parts[1])));
//                    data2.add(new Entry(timestamp, Float.parseFloat(parts[2])));
//                    data3.add(new Entry(timestamp, Float.parseFloat(parts[3])));
//                    data4.add(new Entry(timestamp, Float.parseFloat(parts[4])));
                    Log.d("CSV", "timestamp: " + timestamp + ", value1: " + parts[1] + ", value2: " + parts[2]);

                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CSV", "讀取 CSV 失敗: " + e.getMessage());
        }

        if (!data1.isEmpty()) {
            startDateMillis = (long) data1.get(0).getX();
            endDateMillis = (long) data1.get(data1.size() - 1).getX();
            updateChart(data1, data2, data3, data4);
        }

        Log.d("CSV", "讀取到的數據行數：" + data1.size());
        Log.d("Chart", "data1 size: " + data1.size() + ", data2 size: " + data2.size());
    }
}
