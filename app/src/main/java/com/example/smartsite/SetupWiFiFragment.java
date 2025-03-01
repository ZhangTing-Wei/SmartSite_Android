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

    private static final String TAG = "SetupWiFiFragment"; // 日誌標籤
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
    private BluetoothDevice bluetoothDevice; // 新增變數
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean isDevicePending = false;

    // 接收藍牙連接
    public void setBluetoothSocket(BluetoothSocket socket) {
        this.bluetoothSocket = socket;
        Log.d(TAG, "Setting Bluetooth socket: " + (socket != null ? "valid" : "null") + ", connected: " + (socket != null && socket.isConnected()));
        if (isAdded()) {
            Toast.makeText(requireContext(), "藍牙連接設置: " + (socket != null ? "有效" : "無效"), Toast.LENGTH_SHORT).show();
        } else {
            isSocketPending = true;
            Log.w(TAG, "Fragment not attached yet, socket pending");
        }
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        Log.d(TAG, "Setting Bluetooth device");
        if (isAdded()) { // 檢查 Fragment 是否已附加到 Activity
            this.bluetoothDevice = device;
            String deviceName = "null";
            if (device != null) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
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
            isDevicePending = true; // 標記設備待處理
            Log.w(TAG, "Fragment not attached yet, device pending");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

        if (isDevicePending && bluetoothDevice != null) {
            String deviceName = "null";
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
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

        if (isSocketPending && bluetoothSocket != null) {
            Toast.makeText(requireContext(), "藍牙連接設置: " + (bluetoothSocket != null ? "有效" : "無效"), Toast.LENGTH_SHORT).show();
            isSocketPending = false;
        }

        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Wi-Fi switch changed: " + isChecked);
            Toast.makeText(requireContext(), "Wi-Fi switch: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
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

        return view;
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
//            Toast.makeText(requireContext(), "發現 " + wifiList.size() + " 個 Wi-Fi 网络", Toast.LENGTH_SHORT).show();
        }
    };

    private void scanWifi() {
        Log.d(TAG, "Starting Wi-Fi scan");
//        Toast.makeText(requireContext(), "開始掃描 Wi-Fi 網絡", Toast.LENGTH_SHORT).show();
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            Log.w(TAG, "Wi-Fi manager is null or not enabled");
            Toast.makeText(requireContext(), "請開啟 Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasWifiPermissions()) {
            Log.w(TAG, "Wi-Fi permissions not granted");
            checkAndRequestPermissions();
            return;
        }
        if (!isWifiReceiverRegistered) {
            requireContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isWifiReceiverRegistered = true;
            Log.d(TAG, "Wi-Fi receiver registered");
        }
        if (!wifiManager.startScan()) {
            Log.w(TAG, "Wi-Fi scan failed, retrying in 3 seconds");
            new android.os.Handler().postDelayed(this::scanWifi, 3000);
        }
    }

    private boolean hasWifiPermissions() {
        boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Wi-Fi permissions - Location: " + hasPermission);
        return hasPermission;
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking Wi-Fi permissions");
        if (!hasWifiPermissions()) {
            Log.w(TAG, "Requesting Wi-Fi permissions");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (isWifiReceiverRegistered) {
            requireContext().unregisterReceiver(wifiReceiver);
            isWifiReceiverRegistered = false;
            Log.d(TAG, "Wi-Fi receiver unregistered");
        }
    }

    private void showWifiPasswordDialog(String ssid) {
        Log.d(TAG, "Showing Wi-Fi password dialog for SSID: " + ssid);
        Toast.makeText(requireContext(), "選擇 Wi-Fi: " + ssid, Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("連接到 " + ssid);
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("確定", (dialog, which) -> {
            selectedWifiSSID = ssid;
            selectedWifiPassword = input.getText().toString();
            Log.d(TAG, "Selected Wi-Fi: " + selectedWifiSSID + ", Password: " + selectedWifiPassword);
            Toast.makeText(requireContext(), "已選擇 Wi-Fi: " + selectedWifiSSID, Toast.LENGTH_SHORT).show();
            sendWifiCredentials(selectedWifiSSID, selectedWifiPassword);
//            switchWifi.setChecked(false); // 傳送後關閉Wi-Fi開關
            recyclerWifi.setVisibility(View.GONE);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendWifiCredentials(String ssid, String password) {
        Log.d(TAG, "Sending Wi-Fi credentials: SSID=" + ssid + ", Password=" + password);
        Log.d(TAG, "BluetoothSocket state: " + (bluetoothSocket != null ? "valid" : "null") + ", connected: " + (bluetoothSocket != null && bluetoothSocket.isConnected()));
        Toast.makeText(requireContext(), "發送 Wi-Fi 憑證: " + ssid, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    Log.w(TAG, "Bluetooth socket is null or not connected, attempting to reconnect");
                    if (bluetoothDevice == null) {
                        Log.w(TAG, "Bluetooth device is null, cannot reconnect");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "藍牙設備不可用", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // 檢查權限
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG, "BLUETOOTH_CONNECT permission not granted");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "缺少藍牙連接權限", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // 重新連接並處理 SecurityException
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        bluetoothSocket.connect();
                        Log.d(TAG, "Reconnected to Bluetooth device");
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException during reconnect: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "藍牙連接權限被拒絕: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                        return;
                    } catch (IOException e) {
                        Log.e(TAG, "IOException during reconnect: " + e.getMessage());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "藍牙重連失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }
                }

                // 發送 Wi-Fi 憑證
                String credentials = ssid + "," + password + "\n";
                bluetoothSocket.getOutputStream().write(credentials.getBytes());
                bluetoothSocket.getOutputStream().flush();
                Log.d(TAG, "Wi-Fi credentials sent successfully");

                byte[] buffer = new byte[1024];
                int bytesRead = bluetoothSocket.getInputStream().read(buffer);
                String response = new String(buffer, 0, bytesRead).trim();
                Log.d(TAG, "Received response from ESP32: " + response);

                if (response.equals("OK")) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Wi-Fi資訊已發送並確認", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "ESP32未正確回應: " + response, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to send Wi-Fi credentials: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "發送失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}