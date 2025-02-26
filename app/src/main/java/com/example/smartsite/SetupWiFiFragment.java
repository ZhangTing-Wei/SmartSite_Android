package com.example.smartsite;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetupWiFiFragment extends Fragment {

    // UI 元件
    private Switch switchWifi; // Wi-Fi 和藍牙的開關
    private RecyclerView recyclerWifi; // 顯示 Wi-Fi 和藍牙設備的列表
    private WifiAdapter wifiAdapter; // Wi-Fi 列表適配器

    // 系統服務
    private WifiManager wifiManager; // Wi-Fi 管理器

    // 數據存儲
    private final List<String> wifiList = new ArrayList<>(); // Wi-Fi SSID 列表
    private String selectedWifiSSID; // 選擇的 Wi-Fi SSID
    private String selectedWifiPassword; // 選擇的 Wi-Fi 密碼

    // 常量
    private static final int REQUEST_CODE_PERMISSIONS = 1; // 權限請求代碼

    // 狀態標誌
    private boolean isWifiReceiverRegistered = false; // Wi-Fi 廣播接收器是否已註冊

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化系統服務（這裡不需要 UI 元件）
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setup_wi_fi, container, false);

        // 初始化 UI 元件
        switchWifi = view.findViewById(R.id.switch_wifi);
        recyclerWifi = view.findViewById(R.id.recycler_wifi);
        TextView tvWifiStatus = view.findViewById(R.id.tv_wifi_status);

        // 設置 RecyclerView 的佈局管理器
        recyclerWifi.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化適配器並設置點擊事件
        wifiAdapter = new WifiAdapter(wifiList, this::showWifiPasswordDialog);

        // 將適配器綁定到 RecyclerView
        recyclerWifi.setAdapter(wifiAdapter);

        // Wi-Fi 開關監聽器
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                recyclerWifi.setVisibility(View.VISIBLE);
                tvWifiStatus.setText("  已開啟");
                tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                checkAndRequestPermissions();
                scanWifi();
            } else {
                recyclerWifi.setVisibility(View.GONE);
                tvWifiStatus.setText("  已關閉");
                tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            }
        });

        return view;
    }

    // Wi-Fi 掃描結果接收器
    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();
            wifiList.clear();
            for (ScanResult result : results) {
                if (!result.SSID.isEmpty()) {
                    wifiList.add(result.SSID);
                }
            }
            wifiAdapter.notifyDataSetChanged();
        }
    };

    // 掃描 Wi-Fi 網絡
    private void scanWifi() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Toast.makeText(requireContext(), "請開啟 Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasWifiPermissions()) {
            checkAndRequestPermissions();
            return;
        }
        if (!isWifiReceiverRegistered) {
            requireContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isWifiReceiverRegistered = true;
        }
        if (!wifiManager.startScan()) {
            new android.os.Handler().postDelayed(this::scanWifi, 3000); // 掃描失敗時延遲 3 秒重試
        }
    }

    // 檢查 Wi-Fi 權限
    private boolean hasWifiPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 檢查並請求必要的權限
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (!hasWifiPermissions()) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                    permissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    // Fragment 銷毀時取消廣播接收器註冊
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isWifiReceiverRegistered) {
            requireContext().unregisterReceiver(wifiReceiver);
            isWifiReceiverRegistered = false;
        }
    }

    // 顯示 Wi-Fi 密碼輸入對話框
    private void showWifiPasswordDialog(String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("連接到 " + ssid);
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("確定", (dialog, which) -> {
            selectedWifiSSID = ssid;
            selectedWifiPassword = input.getText().toString();
            Toast.makeText(requireContext(), "已選擇 Wi-Fi: " + selectedWifiSSID, Toast.LENGTH_SHORT).show();
            // 在這裡關閉 Wi-Fi Switch
            switchWifi.setChecked(false); // 自動關閉 Wi-Fi Switch
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}