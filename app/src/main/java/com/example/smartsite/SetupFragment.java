package com.example.smartsite;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SetupFragment: 用於設置和管理 Wi-Fi 與藍牙設備的 Fragment
 * 功能包括掃描 Wi-Fi 和藍牙設備、選擇 Wi-Fi 並輸入密碼、通過藍牙傳送 Wi-Fi 憑證
 */
public class SetupFragment extends Fragment {

    // UI 元件
    private Switch switchWifi, switchBluetooth; // Wi-Fi 和藍牙的開關
    private RecyclerView recyclerWifi, recyclerBluetooth; // 顯示 Wi-Fi 和藍牙設備的列表
    private WifiAdapter wifiAdapter; // Wi-Fi 列表適配器
    private BluetoothListAdapter bluetoothListAdapter; // 藍牙設備列表適配器

    // 系統服務
    private WifiManager wifiManager; // Wi-Fi 管理器
    private BluetoothAdapter bluetoothAdapterObj; // 藍牙適配器

    // 數據存儲
    private final List<String> wifiList = new ArrayList<>(); // Wi-Fi SSID 列表
    private final List<String> bluetoothList = new ArrayList<>(); // 藍牙設備名稱列表
    private final List<BluetoothDevice> bluetoothDevices = new ArrayList<>(); // 藍牙設備對象列表
    private String selectedWifiSSID; // 選擇的 Wi-Fi SSID
    private String selectedWifiPassword; // 選擇的 Wi-Fi 密碼

    // 常量
    private static final int REQUEST_CODE_PERMISSIONS = 1; // 權限請求代碼
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 藍牙 RFCOMM UUID

    // 狀態標誌
    private boolean isWifiReceiverRegistered = false; // Wi-Fi 廣播接收器是否已註冊
    private boolean isBluetoothReceiverRegistered = false; // 藍牙廣播接收器是否已註冊

    public SetupFragment() {
        // 空的構造函數，Fragment 要求
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 載入佈局檔案 fragment_setup.xml
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化 UI 元件
        switchWifi = view.findViewById(R.id.switch_wifi);
        switchBluetooth = view.findViewById(R.id.switch_bluetooth);
        recyclerWifi = view.findViewById(R.id.recycler_wifi);
        recyclerBluetooth = view.findViewById(R.id.recycler_bluetooth);
        TextView tvWifiStatus = view.findViewById(R.id.tv_wifi_status);
        TextView tvBluetoothStatus = view.findViewById(R.id.tv_bluetooth_status);

        // 設置 RecyclerView 的佈局管理器
        recyclerWifi.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBluetooth.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化適配器並設置點擊事件
        wifiAdapter = new WifiAdapter(wifiList, this::showWifiPasswordDialog);
        bluetoothListAdapter = new BluetoothListAdapter(bluetoothList, deviceName -> {
            if (selectedWifiSSID == null || selectedWifiPassword == null) {
                Toast.makeText(requireContext(), "請先選擇 Wi-Fi 並輸入密碼", Toast.LENGTH_SHORT).show();
                return;
            }
            connectToBluetoothDevice(deviceName, selectedWifiSSID, selectedWifiPassword);
        });

        // 將適配器綁定到 RecyclerView
        recyclerWifi.setAdapter(wifiAdapter);
        recyclerBluetooth.setAdapter(bluetoothListAdapter);

        // 初始化系統服務
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapterObj = BluetoothAdapter.getDefaultAdapter();

        // Wi-Fi 開關監聽器
        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                recyclerWifi.setVisibility(View.VISIBLE);
                tvWifiStatus.setText("已開啟");
                tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                checkAndRequestPermissions();
                scanWifi();
            } else {
                recyclerWifi.setVisibility(View.GONE);
                tvWifiStatus.setText("已關閉");
                tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            }
        });

        // 藍牙開關監聽器
        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                recyclerBluetooth.setVisibility(View.VISIBLE);
                tvBluetoothStatus.setText("已開啟");
                tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                checkAndRequestPermissions();
                scanBluetooth();
            } else {
                recyclerBluetooth.setVisibility(View.GONE);
                tvBluetoothStatus.setText("已關閉");
                tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            }
        });
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

    // 藍牙設備發現接收器
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "缺少藍牙連接權限", Toast.LENGTH_SHORT).show();
                return;
            }
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getName() != null && !bluetoothList.contains(device.getName())) {
                bluetoothList.add(device.getName());
                bluetoothDevices.add(device);
                bluetoothListAdapter.notifyDataSetChanged();
            }
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

    // 掃描藍牙設備
    private void scanBluetooth() {
        if (bluetoothAdapterObj == null || !bluetoothAdapterObj.isEnabled()) {
            Toast.makeText(requireContext(), "請開啟藍牙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        bluetoothDevices.clear();
        bluetoothList.clear();
        bluetoothListAdapter.notifyDataSetChanged();

        if (!isBluetoothReceiverRegistered) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                requireContext().registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                isBluetoothReceiverRegistered = true;
            } else {
                Toast.makeText(requireContext(), "缺少藍牙掃描權限", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapterObj.startDiscovery();
        }
    }

    // 檢查 Wi-Fi 權限
    private boolean hasWifiPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 檢查藍牙權限（Android 12+ 需要額外權限）
    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
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
        if (isBluetoothReceiverRegistered) {
            requireContext().unregisterReceiver(bluetoothReceiver);
            isBluetoothReceiverRegistered = false;
        }
    }

    // 單獨請求藍牙權限
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_CODE_PERMISSIONS);
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
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 連接到藍牙設備並發送 Wi-Fi 憑證
    private void connectToBluetoothDevice(String deviceName, String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
        }

        BluetoothDevice device = bluetoothDevices.stream()
                .filter(d -> deviceName.equals(d.getName()))
                .findFirst()
                .orElse(null);

        if (device == null) {
            Toast.makeText(requireContext(), "未找到匹配設備", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "正在連接設備：" + device.getName(), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "藍牙連接成功", Toast.LENGTH_SHORT).show()
                );
                sendWifiCredentials(bluetoothSocket, ssid, password);
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "藍牙連接失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }

    // 發送 Wi-Fi 憑證到藍牙設備
    private void sendWifiCredentials(BluetoothSocket socket, String ssid, String password) {
        new Thread(() -> {
            try {
                String credentials = ssid + "," + password + "\n"; // 格式化為 SSID,密碼\n
                socket.getOutputStream().write(credentials.getBytes());
                socket.getOutputStream().flush();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Wi-Fi 資訊已發送", Toast.LENGTH_SHORT).show()
                );
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "發送 Wi-Fi 資訊失敗", Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }
}