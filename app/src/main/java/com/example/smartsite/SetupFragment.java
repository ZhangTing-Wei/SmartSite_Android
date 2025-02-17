package com.example.smartsite;

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SetupFragment extends Fragment {

    private Switch switchWifi, switchBluetooth;
    private RecyclerView recyclerWifi, recyclerBluetooth;
    private WifiAdapter wifiAdapter;
    private BluetoothListAdapter bluetoothListAdapter;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapterObj;
    private final List<String> wifiList = new ArrayList<>();
    private final List<String> bluetoothList = new ArrayList<>();

    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private boolean isWifiReceiverRegistered = false;
    private boolean isBluetoothReceiverRegistered = false;

    public SetupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchWifi = view.findViewById(R.id.switch_wifi);
        switchBluetooth = view.findViewById(R.id.switch_bluetooth);
        recyclerWifi = view.findViewById(R.id.recycler_wifi);
        recyclerBluetooth = view.findViewById(R.id.recycler_bluetooth);

        // Wi-Fi 和藍牙狀態顯示
        TextView tvWifiStatus = view.findViewById(R.id.tv_wifi_status);
        TextView tvBluetoothStatus = view.findViewById(R.id.tv_bluetooth_status);

        recyclerWifi.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerBluetooth.setLayoutManager(new LinearLayoutManager(getContext()));

        wifiAdapter = new WifiAdapter(wifiList);
        bluetoothListAdapter = new BluetoothListAdapter(bluetoothList);

        recyclerWifi.setAdapter(wifiAdapter);
        recyclerBluetooth.setAdapter(bluetoothListAdapter);

        wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapterObj = BluetoothAdapter.getDefaultAdapter();

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

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // **先檢查權限**
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "缺少藍牙連接權限", Toast.LENGTH_SHORT).show();
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getName() != null) {
                bluetoothList.add(device.getName());
                bluetoothListAdapter.notifyDataSetChanged();
            }
        }
    };


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
        wifiManager.startScan();
    }

    private void scanBluetooth() {
        if (bluetoothAdapterObj == null || !bluetoothAdapterObj.isEnabled()) {
            Toast.makeText(requireContext(), "請開啟藍牙", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先檢查權限
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        bluetoothList.clear();

        // **檢查權限後註冊 Receiver**
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

        // **檢查權限後開始掃描**
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapterObj.startDiscovery();
        } else {
            Toast.makeText(requireContext(), "缺少藍牙掃描權限", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean hasWifiPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (!hasWifiPermissions()) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!hasBluetoothPermissions() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
        }
    }

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

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_CODE_PERMISSIONS);
        }
    }

}
