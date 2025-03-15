package com.example.smartsite;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "reminder_channel_id";
    private static final String CHANNEL_NAME = "Reminder Channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        String alarmTime = intent.getStringExtra("alarm_time");
        String alarmRemind = intent.getStringExtra("alarm_remind");
        String alarmRepeat = intent.getStringExtra("alarm_repeat"); // ★ 新增，取得重複模式
        int customRemindMinutes = intent.getIntExtra("remind_minutes", 0);

        Log.d(TAG, "接收到提前提醒廣播, alarmId=" + alarmId
                + ", remind=" + alarmRemind
                + ", repeat=" + alarmRepeat
                + ", customRemindMinutes=" + customRemindMinutes);

        // 建立通知頻道
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 建立並發送「提前提醒」通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm) // 請換成自己的圖示
                .setContentTitle("The alarm time is approaching.")
                .setContentText("The alarm will go off in... "
                        + alarmTime + " Ringing, the current time is... "
                        + alarmRemind + " Reminder")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // 使用 alarmId + 100000 作為通知 ID，避免與主鬧鐘的通知 ID 相衝
        notificationManager.notify(alarmId + 100000, builder.build());

        // ★ 以下邏輯：若非一次性，則排下一次提醒
        if (alarmRepeat != null && !alarmRepeat.equals(Constant.REPEAT_ONCE)) {
            // 1) 解析 alarmTime，取得 hour & minute
            if (alarmTime != null) {
                String[] parts = alarmTime.split(":");
                if (parts.length == 2) {
                    try {
                        int hour = Integer.parseInt(parts[0]);
                        int minute = Integer.parseInt(parts[1]);
                        Calendar baseTime = Calendar.getInstance();
                        baseTime.set(Calendar.HOUR_OF_DAY, hour);
                        baseTime.set(Calendar.MINUTE, minute);
                        baseTime.set(Calendar.SECOND, 0);
                        baseTime.set(Calendar.MILLISECOND, 0);

                        // 如果當前時間已經超過 baseTime，往後推一天
                        if (baseTime.getTimeInMillis() <= System.currentTimeMillis()) {
                            baseTime.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        // 2) 根據重複模式計算「下一次」提醒基準
                        Calendar nextAlarm = null;
                        if (Constant.REPEAT_EVERYDAY.equals(alarmRepeat)) {
                            baseTime.add(Calendar.DAY_OF_MONTH, 1);
                            nextAlarm = baseTime;
                        } else if (Constant.REPEAT_WEEKDAY.equals(alarmRepeat)) {
                            nextAlarm = getNextWeekdayAlarm(baseTime);
                        } else if (Constant.REPEAT_CUSTOM.equals(alarmRepeat)) {
                            // 嘗試從 Intent 中讀取自訂星期
                            int[] customDays = null;
                            if (intent.hasExtra("custom_days")) {
                                customDays = intent.getIntArrayExtra("custom_days");
                            }
                            if (customDays == null || customDays.length == 0) {
                                // 若未取得自訂星期，就預設週一、週三、週五
                                customDays = new int[]{Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY};
                            }
                            nextAlarm = getNextCustomAlarmTime(baseTime, customDays);
                        }

                        if (nextAlarm != null) {
                            Log.d(TAG, "ReminderReceiver: 下一次提醒基準時間 = " + nextAlarm.getTime());

                            // 3) 計算下一次提醒的毫秒數
                            long nextRemindTime = nextAlarm.getTimeInMillis() + (customRemindMinutes * 60_000L);
                            Log.d(TAG, "ReminderReceiver: 下一次提醒毫秒值 = " + nextRemindTime);

                            // 4) 排程下一次提醒鬧鐘
                            Intent newRemindIntent = new Intent(context, ReminderReceiver.class);
                            newRemindIntent.putExtra("alarm_time", alarmTime);
                            newRemindIntent.putExtra("alarm_id", alarmId);
                            newRemindIntent.putExtra("alarm_remind", alarmRemind);
                            newRemindIntent.putExtra("alarm_repeat", alarmRepeat);
                            newRemindIntent.putExtra("remind_minutes", customRemindMinutes);

                            if (Constant.REPEAT_CUSTOM.equals(alarmRepeat) && intent.hasExtra("custom_days")) {
                                newRemindIntent.putExtra("custom_days", intent.getIntArrayExtra("custom_days"));
                            }

                            PendingIntent nextRemindPending = PendingIntent.getBroadcast(
                                    context,
                                    alarmId + 100000, // 同一個 requestCode 會覆蓋之前的提醒鬧鐘
                                    newRemindIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );

                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            if (alarmManager != null) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        if (alarmManager.canScheduleExactAlarms()) {
                                            alarmManager.setExactAndAllowWhileIdle(
                                                    AlarmManager.RTC_WAKEUP, nextRemindTime, nextRemindPending
                                            );
                                        } else {
                                            Log.e(TAG, "ReminderReceiver: 無法設定精準鬧鐘，請確認已授予 SCHEDULE_EXACT_ALARM 權限");
                                        }
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        alarmManager.setExactAndAllowWhileIdle(
                                                AlarmManager.RTC_WAKEUP, nextRemindTime, nextRemindPending
                                        );
                                    } else {
                                        alarmManager.setExact(
                                                AlarmManager.RTC_WAKEUP, nextRemindTime, nextRemindPending
                                        );
                                    }
                                    Log.d(TAG, "ReminderReceiver: 已排程下一次提醒，時間=" + nextAlarm.getTime()
                                            + ", alarmId=" + (alarmId + 100000));
                                } catch (SecurityException e) {
                                    Log.e(TAG, "ReminderReceiver: 設定提醒鬧鐘失敗：" + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "ReminderReceiver: 解析 alarm_time 失敗", e);
                    }
                }
            }
        }
    }

    // 以下三個方法，與 AlarmReceiver 相同，用於計算下一次時間

    private Calendar getNextWeekdayAlarm(Calendar baseTime) {
        Calendar nextAlarm = (Calendar) baseTime.clone();
        if (nextAlarm.getTimeInMillis() <= System.currentTimeMillis()) {
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
        if (nextAlarm.getTimeInMillis() <= System.currentTimeMillis()) {
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
}
