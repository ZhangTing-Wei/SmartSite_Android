package com.example.smartsite;

/**
 * AlarmData is a model class for storing alarm settings,
 * including time, repeat type, remind time, custom days, etc.
 */
public class AlarmData {

    // ----------------------
    // Constants for remindTime
    // ----------------------
//    /** 無提醒 */
//    public static final String REMIND_NONE = "None";
//    /** 5 分鐘後再提醒 */
//    public static final String REMIND_5MIN = "Every 5 minutes";
//    /** 10 分鐘後再提醒 */
//    public static final String REMIND_10MIN = "Every 10 minutes";
//    /** 15 分鐘後再提醒 */
//    public static final String REMIND_15MIN = "Every 15 minutes";
//    /** 自訂 (可輸入分鐘數) */
//    public static final String REMIND_CUSTOM = "Custom";

    // ----------------------
    // Fields
    // ----------------------

    /** 鬧鐘時間 (例如 "07:59") */
    private String timeText;

    /** 是否啟用這個鬧鐘 (Switch) */
    private boolean isEnabled;

    /** 重複模式 ("One-time only", "Every day", "Monday to Friday", "Custom") */
    private String repeatType;

    /** 提醒模式 ("None", "5 minutes after", "10 minutes after", "15 minutes after", "Custom") */
    private String remindTime;

    /** 鬧鐘 ID，用於在 AlarmManager 裡區分不同鬧鐘 */
    private int alarmId;

    /** 自訂模式下勾選的星期 (Calendar.MONDAY, Calendar.TUESDAY, ...) */
    private int[] customDays;

    /** 自訂提醒時間 (分鐘)，當 remindTime 為 "Custom" 時使用 */
    private int customRemindMinutes;

    /** 鬧鐘觸發的時間（毫秒） */
    private long triggerTimestamp;

    // ----------------------
    // Constructor
    // ----------------------
    public AlarmData(String timeText, boolean isEnabled, String repeatType, String remindTime) {
        this.timeText = timeText;
        this.isEnabled = isEnabled;
        this.repeatType = repeatType;
        this.remindTime = remindTime;
        this.customRemindMinutes = 0; // 預設為 0，表示尚未設定自訂提醒
    }

    // ----------------------
    // Getter / Setter
    // ----------------------
    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public String getTimeText() {
        return timeText;
    }

    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }

    public int[] getCustomDays() {
        return customDays;
    }

    public void setCustomDays(int[] customDays) {
        this.customDays = customDays;
    }

    public int getCustomRemindMinutes() {
        return customRemindMinutes;
    }

    public void setCustomRemindMinutes(int customRemindMinutes) {
        this.customRemindMinutes = customRemindMinutes;
    }

    public long getTriggerTimestamp() {
        return triggerTimestamp;
    }

    public void setTriggerTimestamp(long triggerTimestamp) {
        this.triggerTimestamp = triggerTimestamp;
    }
}
