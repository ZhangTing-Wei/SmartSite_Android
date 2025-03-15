package com.example.smartsite;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class AlarmAdapter extends ArrayAdapter<AlarmData> {

    private int resourceId;
    private OnAlarmItemActionListener listener;

    public AlarmAdapter(Context context, int resource, List<AlarmData> objects) {
        super(context, resource, objects);
        this.resourceId = resource;
    }

    // 設置監聽介面，讓外部可以處理 Switch 切換和刪除事件
    public void setOnAlarmItemActionListener(OnAlarmItemActionListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            holder = new ViewHolder();
            holder.tvAlarmTime = convertView.findViewById(R.id.tvAlarmTime);
            holder.tvRepeatRemind = convertView.findViewById(R.id.tvRepeatRemind);
            holder.switchEnable = convertView.findViewById(R.id.switchEnable);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AlarmData alarmData = getItem(position);
        if (alarmData != null) {
            // 1) 取得 repeatType 與 remindTime
            String repeatType = alarmData.getRepeatType();   // 可能是「僅此一次」「每天」「自訂」...
            String remindTime = alarmData.getRemindTime();   // 可能是「無」「5 分鐘前」「自訂」...

            // 2) 如果 repeatType == 「自訂」，顯示自訂星期
            if ("Custom".equals(repeatType)) {
                int[] customDays = alarmData.getCustomDays(); // 使用者勾選的自訂星期
                String dayLabel = buildDayLabel(customDays);  // 例如「週四, 週五」
                // 讓 repeatType 顯示成「自訂(週四, 週五)」
                repeatType = "自訂(" + dayLabel + ")";
            }

//            // 3) 如果 remindTime == 「自訂」，顯示自訂分鐘
//            if ("Custom".equals(remindTime)) {
//                int customRemind = alarmData.getCustomRemindMinutes(); // 使用者輸入的分鐘數
//                // 讓 remindTime 顯示成「自訂(30 分鐘前)」
//                remindTime = "自訂(" + customRemind + " 分鐘後)";
//            }

            // 4) 組合顯示文字
            String repeatRemindText = repeatType + ", " + remindTime;
            holder.tvRepeatRemind.setText(repeatRemindText);

            // 5) 顯示鬧鐘時間、Switch 狀態
            holder.tvAlarmTime.setText(alarmData.getTimeText());
            holder.switchEnable.setChecked(alarmData.isEnabled());
        }

        // Switch 事件
        holder.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (alarmData != null) {
                alarmData.setEnabled(isChecked);
                // 通知外部 (MainActivity) 去處理鬧鐘啟用/關閉的後續邏輯 (例如 AlarmManager)
                if (listener != null) {
                    listener.onAlarmSwitchChanged(position, isChecked);
                }
            }
        });

        // 刪除按鈕事件
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(position);
            }
        });

        return convertView;
    }

    /**
     * 將自訂星期 (Calendar.MONDAY, ...) 轉成字串 (例如「週一, 週三, 週五」)
     */
    private String buildDayLabel(int[] days) {
        if (days == null || days.length == 0) {
            return "未選擇";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            int day = days[i];
            String dayString;
            switch (day) {
                case Calendar.MONDAY:
                    dayString = "週一";
                    break;
                case Calendar.TUESDAY:
                    dayString = "週二";
                    break;
                case Calendar.WEDNESDAY:
                    dayString = "週三";
                    break;
                case Calendar.THURSDAY:
                    dayString = "週四";
                    break;
                case Calendar.FRIDAY:
                    dayString = "週五";
                    break;
                case Calendar.SATURDAY:
                    dayString = "週六";
                    break;
                case Calendar.SUNDAY:
                    dayString = "週日";
                    break;
                default:
                    dayString = "???";
                    break;
            }
            sb.append(dayString);
            if (i < days.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    static class ViewHolder {
        TextView tvAlarmTime;
        TextView tvRepeatRemind;
        Switch switchEnable;
        ImageButton btnDelete;
    }

    // 自訂介面，讓外部知道使用者切換或刪除
    public interface OnAlarmItemActionListener {
        void onAlarmSwitchChanged(int position, boolean isEnabled);
        void onDeleteClicked(int position);
    }
}
