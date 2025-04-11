package com.example.smartsite;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class setup_alarm extends AppCompatActivity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_setup_alarm);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }

    // SharedPreferences 名稱 & 存放鬧鐘清單的 key
    private static final String TAG = "MainActivity";

    private ArrayList<AlarmData> alarmList;
    private AlarmAdapter alarmAdapter;

    // 用於 Spinner Repeat 的原始資料 (例如 "One-time only"、"Every day"、"Monday to Friday"、"Custom")
    private List<String> repeatOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 進入 MainActivity");
        setContentView(R.layout.activity_setup_alarm);

        // 讀取先前儲存的鬧鐘清單
        alarmList = loadAlarmList();
        if (alarmList == null) {
            alarmList = new ArrayList<>();
        }
        Log.d(TAG, "目前鬧鐘數量：" + alarmList.size());
        // ★ 印出每筆鬧鐘資料
        for (AlarmData alarm : alarmList) {
            Log.d(TAG, "MainActivity onCreate - alarmList item: " +
                    "time=" + alarm.getTimeText() +
                    ", enabled=" + alarm.isEnabled() +
                    ", repeat=" + alarm.getRepeatType() +
                    ", remind=" + alarm.getRemindTime() +
                    ", customRemindMinutes=" + alarm.getCustomRemindMinutes());

            registerAlarm(alarm);
        }

        // 初始化 ListView 與 Adapter
        ListView listView = findViewById(R.id.alarmListView);
        alarmAdapter = new AlarmAdapter(this, R.layout.item_alarm, alarmList);
        listView.setAdapter(alarmAdapter);

        // Adapter 的事件回呼
        alarmAdapter.setOnAlarmItemActionListener(new AlarmAdapter.OnAlarmItemActionListener() {
            @Override
            public void onAlarmSwitchChanged(int position, boolean isEnabled) {
                AlarmData data = alarmList.get(position);
                data.setEnabled(isEnabled);
                Log.d(TAG, "[onAlarmSwitchChanged] alarmId=" + data.getAlarmId()
                        + ", time=" + data.getTimeText() + ", isEnabled=" + isEnabled);

                Toast.makeText(setup_alarm.this,
                        "Alarm " + data.getTimeText() + (isEnabled ? " Enabled" : " Disabled"),
                        Toast.LENGTH_SHORT).show();
                saveAlarmList();
            }

            @Override
            public void onDeleteClicked(int position) {
                AlarmData data = alarmList.get(position);
                int alarmId = data.getAlarmId();
                Log.d(TAG, "[onDeleteClicked] alarmId=" + alarmId + ", time=" + data.getTimeText());

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    // 取消主鬧鐘
                    Intent mainIntent = new Intent(setup_alarm.this, AlarmReceiver.class);
                    PendingIntent mainPending = PendingIntent.getBroadcast(
                            setup_alarm.this,
                            alarmId,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    alarmManager.cancel(mainPending);

                    // 取消提醒鬧鐘（如果有）
                    Intent remindIntent = new Intent(setup_alarm.this, ReminderReceiver.class);
                    PendingIntent remindPending = PendingIntent.getBroadcast(
                            setup_alarm.this,
                            alarmId + 100000,
                            remindIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    alarmManager.cancel(remindPending);

                    Log.d(TAG, "Canceled alarmId=" + alarmId + " Main alarm and reminder alarm");
                }

                alarmList.remove(position);
                alarmAdapter.notifyDataSetChanged();
                Toast.makeText(setup_alarm.this,
                        "Deleted alarm:" + data.getTimeText(),
                        Toast.LENGTH_SHORT).show();
                saveAlarmList();
            }
        });

        // FAB 點擊 -> 顯示新增鬧鐘對話框
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showAddAlarmDialog());

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent mainIntent = new Intent(this, ScheduleReceiver.class);
        PendingIntent mainPending = PendingIntent.getBroadcast(
                this,
                (new Random()).nextInt(100000),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * 顯示新增鬧鐘對話框
     */
    private void showAddAlarmDialog() {
        Log.d(TAG, "showAddAlarmDialog: 顯示新增鬧鐘對話框");
        Dialog dialog = new Dialog(setup_alarm.this);
        dialog.setContentView(R.layout.dialog_add_alarm);
        dialog.setTitle("Add alarm");

        // TimePicker
        TimePicker timePicker = dialog.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        // Spinner: 重複模式
        Spinner spinnerRepeat = dialog.findViewById(R.id.spinnerRepeat);

        // 自訂星期區塊 (layoutCustomDays)
        LinearLayout layoutCustomDays = dialog.findViewById(R.id.layoutCustomDays);
        CheckBox checkboxMon = dialog.findViewById(R.id.checkboxMon);
        CheckBox checkboxTue = dialog.findViewById(R.id.checkboxTue);
        CheckBox checkboxWed = dialog.findViewById(R.id.checkboxWed);
        CheckBox checkboxThu = dialog.findViewById(R.id.checkboxThu);
        CheckBox checkboxFri = dialog.findViewById(R.id.checkboxFri);
        CheckBox checkboxSat = dialog.findViewById(R.id.checkboxSat);
        CheckBox checkboxSun = dialog.findViewById(R.id.checkboxSun);

        // 1) 讀取 "repeat_options" 陣列，放進 List 以便後續動態修改
        repeatOptions = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.repeat_options)));
        // 2) 讀取 "remind_options" 陣列 - 移除提醒功能
//        List<String> remindOptions = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.remind_options)));

        // 建立 Adapter for repeat
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, repeatOptions
        );
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);

        // 當重複模式選擇「Custom」時，顯示自訂星期區塊
        spinnerRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = repeatOptions.get(position);
                Log.d(TAG, "[spinnerRepeat] selected=" + selected);
                if ("Custom".equals(selected)) {
                    layoutCustomDays.setVisibility(View.VISIBLE);
                } else {
                    layoutCustomDays.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 按鈕
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "showAddAlarmDialog: 使用者點擊取消");
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            int hour, minute;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = timePicker.getHour();
                minute = timePicker.getMinute();
            } else {
                hour = timePicker.getCurrentHour();
                minute = timePicker.getCurrentMinute();
            }

            // 讀取 Spinner 的值 for repeat
            int repeatPos = spinnerRepeat.getSelectedItemPosition();
            String repeat = repeatOptions.get(repeatPos);

            // 移除提醒相關設定，預設提醒為空字串或固定值
            String remind = "";

            String timeText = formatTime24(hour, minute);

            // 產生唯一的 alarmId
            int alarmId = (int) System.currentTimeMillis();

            Log.d(TAG, "showAddAlarmDialog: 使用者設定鬧鐘時間=" + timeText
                    + ", repeat=" + repeat + ", remind=" + remind + ", alarmId=" + alarmId);

            // 建立新的 AlarmData
            AlarmData newAlarm = new AlarmData(timeText, true, repeat, remind);
            newAlarm.setAlarmId(alarmId);

            // 🔥【加這裡】設定triggerTimestamp
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            newAlarm.setTriggerTimestamp(calendar.getTimeInMillis()); // ← 這行很重要

            // 處理「Custom」重複模式，收集勾選星期
            if ("Custom".equals(repeat)) {
                ArrayList<Integer> customDaysList = new ArrayList<>();
                if (checkboxMon.isChecked()) customDaysList.add(Calendar.MONDAY);
                if (checkboxTue.isChecked()) customDaysList.add(Calendar.TUESDAY);
                if (checkboxWed.isChecked()) customDaysList.add(Calendar.WEDNESDAY);
                if (checkboxThu.isChecked()) customDaysList.add(Calendar.THURSDAY);
                if (checkboxFri.isChecked()) customDaysList.add(Calendar.FRIDAY);
                if (checkboxSat.isChecked()) customDaysList.add(Calendar.SATURDAY);
                if (checkboxSun.isChecked()) customDaysList.add(Calendar.SUNDAY);

                int[] customDays = new int[customDaysList.size()];
                for (int i = 0; i < customDaysList.size(); i++) {
                    customDays[i] = customDaysList.get(i);
                }
                newAlarm.setCustomDays(customDays);

                // 組合星期字串，動態修改「Custom」的標籤
                String dayLabel = buildDayLabel(customDaysList); // 例如 "Monday, Wednesday, Friday"
                int customIndex = repeatOptions.indexOf("Custom");
                if (customIndex != -1) {
                    String newLabel = "Custom(" + dayLabel + ")";
                    repeatOptions.set(customIndex, newLabel);
                    Log.d(TAG, "showAddAlarmDialog: 變更 Spinner 顯示為=" + newLabel);
                    repeatAdapter.notifyDataSetChanged();
                    spinnerRepeat.setSelection(customIndex);
                }
            }

            // 新增到清單並更新 UI
            alarmList.add(newAlarm);
            alarmAdapter.notifyDataSetChanged();
            Toast.makeText(setup_alarm.this, "Alarm added：" + timeText, Toast.LENGTH_SHORT).show();

            // 儲存鬧鐘清單
            saveAlarmList();

            registerAlarm(newAlarm);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void registerAlarm(AlarmData alarmData) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Gson gson = new Gson();
        String json = gson.toJson(alarmData);
        Intent mainIntent = new Intent(this, ScheduleReceiver.class);
        mainIntent.putExtra("alarm_data", json);
        PendingIntent mainPending = PendingIntent.getBroadcast(
                this,
                (new Random()).nextInt(100000),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 1);
        long milliseconds = calendar.getTimeInMillis();

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, milliseconds, mainPending);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, milliseconds, mainPending);
                }
                Log.d(TAG, "[MainActivity] 觸發 ScheduleReceiver 重新排程");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "[MainActivity] 無精準鬧鐘權限", e);
        }
    }

    /**
     * 將自訂星期 (Calendar.MONDAY, ...) 轉成字串 (例如 "Monday, Wednesday, Friday")
     */
    private String buildDayLabel(ArrayList<Integer> customDaysList) {
        ArrayList<String> labels = new ArrayList<>();
        for (int day : customDaysList) {
            switch (day) {
                case Calendar.MONDAY:
                    labels.add("Monday");
                    break;
                case Calendar.TUESDAY:
                    labels.add("Tuesday");
                    break;
                case Calendar.WEDNESDAY:
                    labels.add("Wednesday");
                    break;
                case Calendar.THURSDAY:
                    labels.add("Thursday");
                    break;
                case Calendar.FRIDAY:
                    labels.add("Friday");
                    break;
                case Calendar.SATURDAY:
                    labels.add("Saturday");
                    break;
                case Calendar.SUNDAY:
                    labels.add("Sunday");
                    break;
            }
        }
        if (labels.isEmpty()) {
            return "";
        }
        return android.text.TextUtils.join(", ", labels);
    }

    /**
     * 將時間格式化為 24 小時制字串 (例如 "14:05")
     */
    private String formatTime24(int hourOfDay, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
    }

    /**
     * 將 alarmList 轉成 JSON 存入 SharedPreferences
     */
    private void saveAlarmList() {
        SharedPreferences prefs = getSharedPreferences(Constant.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(alarmList);
        editor.putString(Constant.KEY_ALARM_LIST, json);
        editor.apply();
        Log.d(TAG, "saveAlarmList: 鬧鐘清單已儲存，大小=" + alarmList.size());
    }

    /**
     * 從 SharedPreferences 讀取 JSON，轉回 ArrayList<AlarmData>
     */
    private ArrayList<AlarmData> loadAlarmList() {
        SharedPreferences prefs = getSharedPreferences(Constant.PREFS_NAME, MODE_PRIVATE);
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