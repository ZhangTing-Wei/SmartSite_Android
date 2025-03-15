package com.example.smartsite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "裝置已重啟，開始重新排程所有鬧鐘");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent mainIntent = new Intent(context, ScheduleReceiver.class);
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
                    Log.d(TAG, "[BootReceiver] 觸發 ScheduleReceiver 重新排程");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "[BootReceiver] 無精準鬧鐘權限", e);
            }
        }
    }
}
