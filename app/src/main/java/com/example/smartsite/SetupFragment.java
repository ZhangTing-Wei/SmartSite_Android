package com.example.smartsite;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;

public class SetupFragment extends Fragment {

    private static final String TAG = "SetupFragment"; // 日誌標籤
    private Button btnSetupBluetooth, btnSetupWiFi;
    private BluetoothSocket bluetoothSocket; // 儲存藍牙連接
    private BluetoothDevice bluetoothDevice; // 新增變數

    public SetupFragment() {
        // 空的構造函數
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

        // 藍牙設置按鈕
        btnSetupBluetooth.setOnClickListener(v -> {
            Log.d(TAG, "Bluetooth setup button clicked");
            Toast.makeText(requireContext(), "開啟藍牙設置", Toast.LENGTH_SHORT).show();
            SetupBluetoothFragment fragment = new SetupBluetoothFragment();
            fragment.setBluetoothConnectionListener(socket -> {
                this.bluetoothSocket = socket; // 儲存藍牙連接
                this.bluetoothDevice = fragment.getConnectedDevice(); // 保存設備
                Log.d(TAG, "Bluetooth connected: " + (bluetoothSocket != null && bluetoothSocket.isConnected()));
                requireActivity().runOnUiThread(() -> {
//                    btnSetupWiFi.setEnabled(true); // 藍牙連接成功後啟用Wi-Fi設置
                    Log.d(TAG, "Wi-Fi button enabled: " + btnSetupWiFi.isEnabled() + ", Clickable: " + btnSetupWiFi.isClickable());
                    Toast.makeText(requireContext(), "藍牙已連接，可以設置Wi-Fi", Toast.LENGTH_SHORT).show();
                });
            });
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Wi-Fi設置按鈕
        btnSetupWiFi.setOnClickListener(v -> {
            Log.d(TAG, "Wi-Fi setup button clicked, Bluetooth connected: " + (bluetoothSocket != null && bluetoothSocket.isConnected()));
            Toast.makeText(requireContext(), "嘗試開啟Wi-Fi設置", Toast.LENGTH_SHORT).show();
            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                Log.w(TAG, "Bluetooth not connected or socket null");
                Toast.makeText(requireContext(), "請先連接藍牙設備", Toast.LENGTH_SHORT).show();
                return;
            }
            SetupWiFiFragment fragment = new SetupWiFiFragment();
            fragment.setBluetoothSocket(bluetoothSocket); // 傳遞藍牙連接
            fragment.setBluetoothDevice(bluetoothDevice); // 傳遞設備
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 初始時禁用Wi-Fi設置按鈕，直到藍牙連接成功
        Log.d(TAG, "Initializing Wi-Fi button as disabled: " + btnSetupWiFi.isEnabled() + ", Clickable: " + btnSetupWiFi.isClickable());
//        btnSetupWiFi.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close(); // 清理藍牙連接
                Log.d(TAG, "Bluetooth socket closed");
            } catch (IOException e) {
                Log.e(TAG, "Failed to close Bluetooth socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}