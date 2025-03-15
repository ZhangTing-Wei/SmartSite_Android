package com.example.smartsite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

public class ScheduleReceiver extends BroadcastReceiver {

    private static final String TAG = "ScheduleReceiver";

    private Context context;
    private AlarmManager alarmManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        String data = intent.getStringExtra("alarm_data");

        if (data != null && data.length() > 0) {
            Gson gson = new Gson();
            AlarmData alarmData = gson.fromJson(data, AlarmData.class);
            scheduleAlarm(alarmData);
        } else {
            ArrayList<AlarmData> datas = loadAlarmList();
            for (AlarmData alarmData : datas) {
                if (alarmData.isEnabled()) {
                    scheduleAlarm(alarmData);
                }
            }
        }

    }

    private void scheduleAlarm(AlarmData alarmData) {
        Log.d(TAG, "scheduleAlarm: [Main Alarm] alarmId=" + alarmData.getAlarmId()
                + ", timeText=" + alarmData.getTimeText()
                + ", repeat=" + alarmData.getRepeatType()
                + ", remind=" + alarmData.getRemindTime()
                + ", customRemindMinutes=" + alarmData.getCustomDays());

        String[] parts = alarmData.getTimeText().split(":");
        if (parts.length != 2) {
            Log.e(TAG, "時間格式錯誤: " + alarmData.getTimeText());
            return;
        }
        int hour, minute;
        try {
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "解析時間失敗: " + alarmData.getTimeText(), e);
            return;
        }

        Gson gson = new Gson();
        String data = gson.toJson(alarmData);

        long milliseconds = calculateNextTriggerTime(alarmData.getRepeatType(), alarmData.getCustomDays(), hour, minute);

        if (milliseconds <=0) {
            return;
        }

        Intent mainIntent = new Intent(context, AlarmReceiver.class);
        mainIntent.putExtra("alarm_data", data);

        PendingIntent mainPending = PendingIntent.getBroadcast(
                context,
                alarmData.getAlarmId(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, milliseconds, mainPending);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, milliseconds, mainPending);
                }
                Log.d(TAG, "[BootReceiver] 已重新排程主鬧鐘: alarmId=" + alarmData.getAlarmId()
                        + ", time=" + alarmData.getTimeText() + ", triggerAtMillis=" + milliseconds);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "[BootReceiver] 無精準鬧鐘權限", e);
        }

    }

    /**
     * 根據 alarm 的重複模式計算下一次觸發時間（毫秒）
     */
    private long calculateNextTriggerTime(String repeatType, int[] customDays,  int hour, int minute) {
        Calendar baseTime = Calendar.getInstance();
        baseTime.set(Calendar.HOUR_OF_DAY, hour);
        baseTime.set(Calendar.MINUTE, minute);
        baseTime.set(Calendar.SECOND, 0);
        baseTime.set(Calendar.MILLISECOND, 0);

        if (Constant.REPEAT_ONCE.equals(repeatType)) {
            if (baseTime.getTimeInMillis() > System.currentTimeMillis()) {
                return baseTime.getTimeInMillis();
            }
        } else if (Constant.REPEAT_EVERYDAY.equals(repeatType)) {
            if (baseTime.getTimeInMillis() > System.currentTimeMillis()) {
                return baseTime.getTimeInMillis();
            }
            baseTime.add(Calendar.DAY_OF_MONTH, 1);
            return baseTime.getTimeInMillis();
        } else if (Constant.REPEAT_WEEKDAY.equals(repeatType)) {
            // 週一到週五：計算下一個平日
            Calendar nextAlarm = getNextWeekdayAlarm(baseTime);
            return nextAlarm.getTimeInMillis();
        } else if (Constant.REPEAT_CUSTOM.equals(repeatType)) {
            // 自訂：依據使用者設定的星期來計算下一次觸發時間
            if (customDays != null && customDays.length > 0) {
                Calendar nextAlarm = getNextCustomAlarmTime(baseTime, customDays);
                return nextAlarm.getTimeInMillis();
            }
        }

        return -1;
    }

    private Calendar getNextWeekdayAlarm(Calendar baseTime) {
        Calendar nextAlarm = (Calendar) baseTime.clone();
        if (nextAlarm.getTimeInMillis() > System.currentTimeMillis()) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
        }
        while (isWeekend(nextAlarm)) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
        }
        return nextAlarm;
    }

    private boolean isWeekend(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return (day == Calendar.SATURDAY || day == Calendar.SUNDAY);
    }

    private Calendar getNextCustomAlarmTime(Calendar baseTime, int[] customDays) {
        Calendar nextAlarm = (Calendar) baseTime.clone();
        if (nextAlarm.getTimeInMillis() > System.currentTimeMillis()) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
        }
        while (!isCustomDay(nextAlarm, customDays)) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
        }
        return nextAlarm;
    }

    private boolean isCustomDay(Calendar calendar, int[] customDays) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        for (int customDay : customDays) {
            if (day == customDay) {
                return true;
            }
        }
        return false;
    }

    /**
     * 從 SharedPreferences 讀取 JSON，轉回 ArrayList<AlarmData>
     */
    private ArrayList<AlarmData> loadAlarmList() {
        SharedPreferences prefs = context.getSharedPreferences(Constant.PREFS_NAME, context.MODE_PRIVATE);
        String json = prefs.getString(Constant.KEY_ALARM_LIST, null);
        if (json == null) {
            Log.d(TAG, "loadAlarmList: 無資料，回傳null");
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<AlarmData>>() {}.getType();
        ArrayList<AlarmData> list = gson.fromJson(json, type);
        Log.d(TAG, "loadAlarmList: 讀取到鬧鐘清單大小=" + (list != null ? list.size() : 0));
        return list;
    }
}
