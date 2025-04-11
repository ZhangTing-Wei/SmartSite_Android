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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

        // Firebase 讀取資料
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("air_quality").child(today);

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long alarmTime = alarmData.getTriggerTimestamp(); // 用 triggerTimestamp
                    long closestDiff = Long.MAX_VALUE;
                    DataSnapshot closestSnapshot = null;

                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        try {
                            long timestamp = Long.parseLong(childSnapshot.getKey());
                            long diff = Math.abs(timestamp * 1000L - alarmTime); // 注意單位！firebase timestamp是秒，alarmTime是毫秒
                            if (diff < closestDiff) {
                                closestDiff = diff;
                                closestSnapshot = childSnapshot;
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "【AlarmReceiver】timestamp格式錯誤：" + childSnapshot.getKey());
                        }
                    }

                    if (closestSnapshot != null) {
                        Integer aqiValue = closestSnapshot.child("aqi").getValue(Integer.class);
                        if (aqiValue == null) {
                            Log.w(TAG, "【AlarmReceiver】最接近時間的資料裡，aqi是null");
                            sendNotification("無法取得最新AQI數值", alarmData.getAlarmId());
                            return;
                        }

                        String aqiDescription;
                        if (aqiValue >= 0 && aqiValue <= 50) {
                            aqiDescription = "良好";
                        } else if (aqiValue <= 100) {
                            aqiDescription = "普通";
                        } else if (aqiValue <= 150) {
                            aqiDescription = "敏感人群不健康";
                        } else if (aqiValue <= 200) {
                            aqiDescription = "所有族群不健康";
                        } else if (aqiValue <= 300) {
                            aqiDescription = "非常不健康";
                        } else {
                            aqiDescription = "危險";
                        }

                        String firebaseMessage = "AQI: " + aqiValue + "（" + aqiDescription + "）";
                        sendNotification(firebaseMessage, alarmData.getAlarmId());
                    } else {
                        Log.w(TAG, "【AlarmReceiver】找不到接近鬧鐘時間的AQI資料");
                        sendNotification("找不到接近時間的AQI資料", alarmData.getAlarmId());
                    }
                } else {
                    Log.w(TAG, "【AlarmReceiver】今天日期下沒有資料");
                    sendNotification("找不到今日AQI資料", alarmData.getAlarmId());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "【AlarmReceiver】firebase資料讀取失敗", error.toException());
                sendNotification("讀取AQI資料失敗", alarmData.getAlarmId());
            }
        });
    }

    private void sendNotification(String message, int alarmId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("空氣品質提醒")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(alarmId, builder.build());
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
