package com.example.smartsite;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.text.InputType;
import android.util.Log;
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

public class SetupWiFiFragment extends Fragment {

    private static final String TAG = "SetupWiFiFragment";
    private Switch switchWifi;
    private RecyclerView recyclerWifi;
    private WifiAdapter wifiAdapter;
    private WifiManager wifiManager;
    private final List<String> wifiList = new ArrayList<>();
    private String selectedWifiSSID;
    private String selectedWifiPassword;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private boolean isWifiReceiverRegistered = false;
    private BluetoothSocket bluetoothSocket;
    private boolean isSocketPending = false;
    private BluetoothDevice bluetoothDevice;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean isDevicePending = false;
    private final Object socketLock = new Object();
    private boolean initialWifiState = false;

    // 新增方法設置初始狀態
    public void setInitialWifiState(boolean isEnabled) {
        this.initialWifiState = isEnabled;
    }

    // 新增狀態監聽器介面
    public interface WifiStateListener {
        void onWifiStateChanged(boolean isEnabled);
    }

    private WifiStateListener stateListener;

    public void setWifiStateListener(WifiStateListener listener) {
        this.stateListener = listener;
    }

    public void setBluetoothSocket(BluetoothSocket socket) {
        this.bluetoothSocket = socket;
        Log.d(TAG, "Setting Bluetooth socket: " + (socket != null ? "valid" : "null") + ", connected: " + (socket != null && socket.isConnected()));
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "藍牙連接設置: " + (socket != null ? "有效" : "無效"), Toast.LENGTH_SHORT).show();
        } else {
            isSocketPending = true;
            Log.w(TAG, "Fragment not attached yet, socket pending");
        }
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        Log.d(TAG, "Setting Bluetooth device");
        if (isAdded() && getContext() != null) {
            this.bluetoothDevice = device;
            String deviceName = "null";
            if (device != null) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        deviceName = device.getName() != null ? device.getName() : "unknown";
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException while getting device name: " + e.getMessage());
                        deviceName = "unknown (permission denied)";
                    }
                } else {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted");
                    deviceName = "unknown (permission not granted)";
                }
            }
            Log.d(TAG, "Bluetooth device set: " + deviceName);
        } else {
            this.bluetoothDevice = device;
            isDevicePending = true;
            Log.w(TAG, "Fragment not attached yet, device pending");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        } else {
            Log.w(TAG, "Context not available in onCreate, wifiManager initialization delayed");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_setup_wi_fi, container, false);

        switchWifi = view.findViewById(R.id.switch_wifi);
        recyclerWifi = view.findViewById(R.id.recycler_wifi);
        TextView tvWifiStatus = view.findViewById(R.id.tv_wifi_status);

        recyclerWifi.setLayoutManager(new LinearLayoutManager(getContext()));
        wifiAdapter = new WifiAdapter(wifiList, this::showWifiPasswordDialog);
        recyclerWifi.setAdapter(wifiAdapter);

        if (isDevicePending && bluetoothDevice != null && getContext() != null) {
            String deviceName = "null";
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    deviceName = bluetoothDevice.getName() != null ? bluetoothDevice.getName() : "unknown";
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException while getting device name: " + e.getMessage());
                    deviceName = "unknown (permission denied)";
                }
            } else {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted");
                deviceName = "unknown (permission not granted)";
            }
            Log.d(TAG, "Bluetooth device set (delayed): " + deviceName);
            isDevicePending = false;
        }

        if (isSocketPending && bluetoothSocket != null && getContext() != null) {
            Toast.makeText(getContext(), "藍牙連接設置: " + (bluetoothSocket != null ? "有效" : "無效"), Toast.LENGTH_SHORT).show();
            isSocketPending = false;
        }

        // 設置初始狀態
        switchWifi.setChecked(initialWifiState);
        if (initialWifiState) {
            recyclerWifi.setVisibility(View.VISIBLE);
            tvWifiStatus.setText("已開啟");
            tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            checkAndRequestPermissions();
            wifiScanRunnable.run();
        } else {
            recyclerWifi.setVisibility(View.GONE);
            tvWifiStatus.setText("已關閉");
            tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }

        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Wi-Fi switch changed: " + isChecked);
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached, skipping switch action");
                return;
            }
            Toast.makeText(getContext(), "Wi-Fi switch: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
            if (isChecked) {
                recyclerWifi.setVisibility(View.VISIBLE);
                tvWifiStatus.setText("已開啟");
                tvWifiStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_green_dark));
                checkAndRequestPermissions();
                wifiScanRunnable.run();
            } else {
                recyclerWifi.setVisibility(View.GONE);
                wifiScanHandler.removeCallbacks(wifiScanRunnable);
                tvWifiStatus.setText("已關閉");
                tvWifiStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
            }
            // 通知狀態變化
            if (stateListener != null) {
                stateListener.onWifiStateChanged(isChecked);
            }
        });

        return view;
    }

    private final android.os.Handler wifiScanHandler = new android.os.Handler();
    private final Runnable wifiScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && switchWifi != null && switchWifi.isChecked()) {
                scanWifi();
                wifiScanHandler.postDelayed(this, 30000); // 每10秒掃一次
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (switchWifi != null && switchWifi.isChecked()) {
            wifiScanHandler.removeCallbacks(wifiScanRunnable); // 清除舊的，避免重複
            wifiScanRunnable.run();  // 啟動自動掃描
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        wifiScanHandler.removeCallbacks(wifiScanRunnable); // 停止自動掃描
    }

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Wi-Fi scan results received");
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Wi-Fi location permission not granted");
                return;
            }
            List<ScanResult> results = wifiManager.getScanResults();
            wifiList.clear();
            for (ScanResult result : results) {
                if (!result.SSID.isEmpty()) {
                    wifiList.add(result.SSID);
                    Log.d(TAG, "Found Wi-Fi SSID: " + result.SSID);
                }
            }
            wifiAdapter.notifyDataSetChanged();
        }
    };

    private void scanWifi() {
        Log.d(TAG, "Starting Wi-Fi scan");
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping Wi-Fi scan");
            return;
        }
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Log.w(TAG, "Wi-Fi manager is null or not enabled");
            Toast.makeText(getContext(), "請開啟 Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasWifiPermissions()) {
            Log.w(TAG, "Wi-Fi permissions not granted");
            checkAndRequestPermissions();
            return;
        }
        if (!isWifiReceiverRegistered) {
            getContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isWifiReceiverRegistered = true;
            Log.d(TAG, "Wi-Fi receiver registered");
        }
        if (!wifiManager.startScan()) {
            Log.w(TAG, "Wi-Fi scan failed, retrying in 3 seconds");
            new android.os.Handler().postDelayed(this::scanWifi, 3000);
        }
    }

    private boolean hasWifiPermissions() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context not available, skipping permission check");
            return false;
        }
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Wi-Fi permissions - Location: " + hasPermission);
        return hasPermission;
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking Wi-Fi permissions");
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping permission request");
            return;
        }
        if (!hasWifiPermissions()) {
            Log.w(TAG, "Requesting Wi-Fi permissions");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (isWifiReceiverRegistered && getContext() != null) {
            try {
                getContext().unregisterReceiver(wifiReceiver);
                isWifiReceiverRegistered = false;
                Log.d(TAG, "Wi-Fi receiver unregistered");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered: " + e.getMessage());
            }
        }
    }

    private void showWifiPasswordDialog(String ssid) {
        Log.d(TAG, "Showing Wi-Fi password dialog for SSID: " + ssid);
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping dialog");
            return;
        }
        Toast.makeText(getContext(), "選擇 Wi-Fi: " + ssid, Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("連接到 " + ssid);
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("確定", (dialog, which) -> {
            selectedWifiSSID = ssid;
            selectedWifiPassword = input.getText().toString();
            Log.d(TAG, "Selected Wi-Fi: " + selectedWifiSSID + ", Password: " + selectedWifiPassword);
            Toast.makeText(getContext(), "已選擇 Wi-Fi: " + selectedWifiSSID, Toast.LENGTH_SHORT).show();
            sendWifiCredentials(selectedWifiSSID, selectedWifiPassword);
            recyclerWifi.setVisibility(View.GONE);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendWifiCredentials(String ssid, String password) {
        Log.d(TAG, "Sending Wi-Fi credentials: SSID=" + ssid + ", Password=" + password);
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping send credentials");
            return;
        }

        showToastOnUiThread("發送 Wi-Fi 憑證: " + ssid);

        new Thread(() -> {
            try {
                synchronized (socketLock) {
                    if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                        Log.w(TAG, "Bluetooth socket is null or not connected, attempting to reconnect");

                        if (bluetoothDevice == null) {
                            showToastOnUiThread("藍牙設備不可用");
                            return;
                        }

                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            showToastOnUiThread("缺少藍牙連接權限");
                            return;
                        }

                        try {
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                            bluetoothSocket.connect();
                            Log.d(TAG, "Reconnected to Bluetooth device");
                        } catch (SecurityException e) {
                            Log.e(TAG, "SecurityException during reconnect: " + e.getMessage());
                            showToastOnUiThread("藍牙連接權限被拒絕: " + e.getMessage());
                            return;
                        } catch (IOException e) {
                            Log.e(TAG, "IOException during reconnect: " + e.getMessage());
                            showToastOnUiThread("藍牙重連失敗: " + e.getMessage());
                            return;
                        }
                    }

                    // 發送Wi-Fi帳密
                    String credentials = ssid + "," + password + "\n";
                    bluetoothSocket.getOutputStream().write(credentials.getBytes());
                    bluetoothSocket.getOutputStream().flush();
                    Log.d(TAG, "Wi-Fi credentials sent successfully");

                    // 讀取ESP32回應
                    byte[] buffer = new byte[1024];
                    int bytesRead = bluetoothSocket.getInputStream().read(buffer);
                    String response = new String(buffer, 0, bytesRead).trim();
                    Log.d(TAG, "Received response from ESP32: " + response);

                    if ("OK".equals(response)) {
                        showToastOnUiThread("Wi-Fi資訊已發送並確認");
                    } else {
                        showToastOnUiThread("ESP32未正確回應: " + response);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to send Wi-Fi credentials: " + e.getMessage(), e);
                showToastOnUiThread("發送失敗: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 在主線程顯示Toast，保護Fragment狀態，避免背景執行緒崩潰
     */
    private void showToastOnUiThread(String message) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show()
            );
        } else {
            Log.w(TAG, "Cannot show toast, fragment not attached: " + message);
        }
    }

}