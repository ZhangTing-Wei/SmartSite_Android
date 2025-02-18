package com.example.smartsite;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateAxisFormatter extends ValueFormatter {
//    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        long timestamp = (long) value;  // ✅ 確保精度
        return dateFormat.format(new Date(timestamp));
    }

}
