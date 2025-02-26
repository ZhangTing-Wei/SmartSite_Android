package com.example.smartsite;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetupBluetoothFragment extends Fragment {

    // UI 元件
    private Switch switchWifi, switchBluetooth; // Wi-Fi 和藍牙的開關
    private RecyclerView recyclerWifi, recyclerBluetooth; // 顯示 Wi-Fi 和藍牙設備的列表
    private WifiAdapter wifiAdapter; // Wi-Fi 列表適配器
    private BluetoothListAdapter bluetoothListAdapter; // 藍牙設備列表適配器

    // 系統服務
    private WifiManager wifiManager; // Wi-Fi 管理器
    private BluetoothAdapter bluetoothAdapterObj; // 藍牙適配器

    private Button btnSetupWiFi;

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


    public SetupBluetoothFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapterObj = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_bluetooth, container, false);
    }
}