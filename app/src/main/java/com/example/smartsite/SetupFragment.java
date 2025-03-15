package com.example.smartsite;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class SetupFragment extends Fragment implements SetupBluetoothFragment.BluetoothStateListener, SetupWiFiFragment.WifiStateListener {

    private static final String TAG = "SetupFragment";
    private Button btnSetupBluetooth, btnSetupWiFi, btnSetupAlarm;
    private TextView tvBluetoothStatus, tvWifiStatus;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private boolean isBluetoothEnabled = false;
    private boolean isWifiEnabled = false;

    public SetupFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated called");
        super.onViewCreated(view, savedInstanceState);

        btnSetupBluetooth = view.findViewById(R.id.btn_SetupBluetooth);
        btnSetupWiFi = view.findViewById(R.id.btn_SetupWiFi);
        btnSetupAlarm = view.findViewById(R.id.btn_SetupAlarm);
        tvBluetoothStatus = view.findViewById(R.id.tvBluetoothStatus);
        tvWifiStatus = view.findViewById(R.id.tvWifiStatus);

        updateBluetoothStatusUI();
        updateWifiStatusUI();

        btnSetupBluetooth.setOnClickListener(v -> {
            Log.d(TAG, "Bluetooth setup button clicked");
            Toast.makeText(requireContext(), "開啟藍牙設置", Toast.LENGTH_SHORT).show();
            SetupBluetoothFragment fragment = new SetupBluetoothFragment();
            fragment.setBluetoothConnectionListener(socket -> {
                this.bluetoothSocket = socket;
                this.bluetoothDevice = fragment.getConnectedDevice();
                Log.d(TAG, "Bluetooth connected: " + (bluetoothSocket != null && bluetoothSocket.isConnected()));
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "藍牙已連接，可以設置Wi-Fi", Toast.LENGTH_SHORT).show();
                });
            });
            fragment.setBluetoothStateListener(this);
            fragment.setInitialBluetoothState(isBluetoothEnabled); // 傳遞當前藍牙狀態
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        btnSetupWiFi.setOnClickListener(v -> {
            Log.d(TAG, "Wi-Fi setup button clicked, Bluetooth connected: " + (bluetoothSocket != null && bluetoothSocket.isConnected()));
            Toast.makeText(requireContext(), "嘗試開啟Wi-Fi設置", Toast.LENGTH_SHORT).show();
            if (bluetoothSocket == null || !bluetoothSocket.isConnected() || bluetoothDevice == null) {
                Log.w(TAG, "Bluetooth not connected, socket null, or device null");
                if (bluetoothDevice != null) {
                    new Thread(() -> {
                        try {
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                bluetoothSocket.connect();
                                Log.d(TAG, "Bluetooth reconnected successfully");
                                proceedToWiFiSetup();
                            } else {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(), "缺少藍牙連接權限", Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to reconnect Bluetooth: " + e.getMessage());
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "藍牙重連失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        } catch (SecurityException e) {
                            Log.e(TAG, "Security exception occurred: " + e.getMessage());
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "藍牙權限被拒絕: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }).start();
                } else {
                    Toast.makeText(requireContext(), "請先連接藍牙設備", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            proceedToWiFiSetup();
        });

        // 新增 btnSetupAlarm 的點擊事件處理
        btnSetupAlarm.setOnClickListener(v -> {
            Log.d(TAG, "Alarm setup button clicked");
            Toast.makeText(requireContext(), "開啟鬧鐘設置", Toast.LENGTH_SHORT).show();
            // 啟動 setup_alarm 活動
            Intent intent = new Intent(requireContext(), setup_alarm.class);
            startActivity(intent);
        });

        if (savedInstanceState != null) {
            isBluetoothEnabled = savedInstanceState.getBoolean("bluetooth_state", false);
            isWifiEnabled = savedInstanceState.getBoolean("wifi_state", false);
            String address = savedInstanceState.getString("device_address");
            if (address != null) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothDevice = adapter.getRemoteDevice(address);
            }
            updateBluetoothStatusUI();
            updateWifiStatusUI();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called, Bluetooth state: " + isBluetoothEnabled + ", Wi-Fi state: " + isWifiEnabled);
        updateBluetoothStatusUI();
        updateWifiStatusUI();
    }

    private void updateBluetoothStatusUI() {
        if (isBluetoothEnabled) {
            tvBluetoothStatus.setText("已開啟");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            tvBluetoothStatus.setText("已關閉");
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }
    }

    private void updateWifiStatusUI() {
        if (isWifiEnabled) {
            tvWifiStatus.setText("已開啟");
            tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            tvWifiStatus.setText("已關閉");
            tvWifiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }
    }

    private void proceedToWiFiSetup() {
        SetupWiFiFragment fragment = new SetupWiFiFragment();
        fragment.setBluetoothSocket(bluetoothSocket);
        fragment.setBluetoothDevice(bluetoothDevice);
        fragment.setWifiStateListener(this);
        fragment.setInitialWifiState(isWifiEnabled);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBluetoothStateChanged(boolean isEnabled) {
        Log.d(TAG, "Bluetooth State " + isEnabled);
        isBluetoothEnabled = isEnabled;
        updateBluetoothStatusUI();
    }

    @Override
    public void onWifiStateChanged(boolean isEnabled) {
        Log.d(TAG, "WiFi State " + isEnabled);
        isWifiEnabled = isEnabled;
        updateWifiStatusUI();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.d(TAG, "Bluetooth socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Failed to close Bluetooth socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("bluetooth_state", isBluetoothEnabled);
        outState.putBoolean("wifi_state", isWifiEnabled);
        if (bluetoothDevice != null) {
            outState.putString("device_address", bluetoothDevice.getAddress());
        }
    }
}