package com.example.smartsite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SwitchActivity extends AppCompatActivity {

    private Switch switchToggle;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);

        switchToggle = findViewById(R.id.switchToggle);
        tvStatus = findViewById(R.id.tvStatus);

        // 根據初始狀態設定顯示「已開啟 / 已關閉」與文字顏色
        if (switchToggle.isChecked()) {
            tvStatus.setText("已開啟");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("已關閉");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // 監聽 Switch 切換事件
        switchToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 開啟時：顯示「已開啟」+ 綠色
                    tvStatus.setText("已開啟");
                    tvStatus.setTextColor(ContextCompat.getColor(SwitchActivity.this, android.R.color.holo_green_dark));

                    // 可以在這裡執行切換到 MainActivity 的邏輯
                    Intent intent = new Intent(SwitchActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // 關閉時：顯示「已關閉」+ 灰色
                    tvStatus.setText("已關閉");
                    tvStatus.setTextColor(ContextCompat.getColor(SwitchActivity.this, android.R.color.darker_gray));
                }
            }
        });
    }
}
