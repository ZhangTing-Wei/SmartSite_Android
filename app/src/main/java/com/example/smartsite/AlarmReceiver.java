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

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel_id";
    private static final String CHANNEL_NAME = "Alarm Channel";

    private Context context = null;
    private AlarmManager alarmManager = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        String data = intent.getStringExtra("alarm_data");
        if (data == null || data.length() == 0) {
            return;
        }

        Gson gson = new Gson();
        AlarmData alarmData = gson.fromJson(data, AlarmData.class);

        registerAlarm(alarmData);

        Log.d(TAG, "【AlarmReceiver】接收到鬧鐘廣播, alarmId: " + alarmData.getAlarmId());

        Log.d(TAG, "【AlarmReceiver】鬧鐘時間: " + alarmData.getTimeText());
        Log.d(TAG, "【AlarmReceiver】重複模式: " + alarmData.getRepeatType());
        Log.d(TAG, "【AlarmReceiver】自訂提醒時間: " + alarmData.getCustomRemindMinutes() + " 分鐘");

        // 建立通知頻道
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "【AlarmReceiver】通知頻道已建立");
        }

        // 設定通知內容
        String notificationText = "接收到廣播! 鬧鐘時間: " + alarmData.getTimeText() + "重複模式: " + alarmData.getRepeatType();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Alarm reminder")
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(alarmData.getAlarmId(), builder.build());
        Log.d(TAG, "【AlarmReceiver】通知已發送，alarmId=" + alarmData.getAlarmId());
    }

    private void registerAlarm(AlarmData alarmData) {
        Gson gson = new Gson();
        String json = gson.toJson(alarmData);
        Intent mainIntent = new Intent(context, ScheduleReceiver.class);
        mainIntent.putExtra("alarm_data", json);
        PendingIntent mainPending = PendingIntent.getBroadcast(
                context,
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
                Log.d(TAG, "[AlarmReceiver] 觸發 ScheduleReceiver 重新排程");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "[AlarmReceiver] 無精準鬧鐘權限", e);
        }
    }
}
