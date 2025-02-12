package com.example.smartsite;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private LineChart lineChart;  // 宣告 LineChart 變數

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public HistoryFragment() {
        // Required empty public constructor
    }

    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // 初始化 LineChart
        lineChart = view.findViewById(R.id.lineChart);

        // 設定 LineChart 數據
        setupLineChart();

        return view;
    }

    // 設置 LineChart 的數據和樣式
    private void setupLineChart() {
        // 準備數據（這裡使用的是一個示範的數據集）
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(1612137600000f, 1f));  // (時間戳, y)
        entries.add(new Entry(1612224000000f, 2f));
        entries.add(new Entry(1612310400000f, 3f));
        entries.add(new Entry(1612396800000f, 4f));

        // 創建 LineDataSet 並設置樣式
        LineDataSet dataSet = new LineDataSet(entries, "Demo Data");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));  // 線的顏色
        dataSet.setValueTextColor(getResources().getColor(android.R.color.holo_red_light));  // 數字的顏色
        dataSet.setLineWidth(2f);  // 線的寬度

        // 設定 X 軸
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(90);
        xAxis.setValueFormatter(new DateAxisFormatter());

        // 設定 X 軸的 granularity（刻度間隔）
        xAxis.setGranularity(1f); // 每個刻度表示一天（可根據需求調整）

        // 創建 LineData 並設置給 LineChart
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // 刷新 LineChart 顯示
        lineChart.invalidate();
    }
}
