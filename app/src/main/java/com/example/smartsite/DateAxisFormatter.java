package com.example.smartsite;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateAxisFormatter extends ValueFormatter {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        Date date = new Date((long) value);
        return dateFormat.format(date);
    }

}
