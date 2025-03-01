package com.example.smartsite;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat; // 使用 SwitchCompat
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetupBluetoothFragment extends Fragment {

    private static final String TAG = "SetupBluetoothFragment"; // 日誌標籤
    private Switch switchBluetooth;
    private RecyclerView recyclerBluetooth;
    private BluetoothListAdapter bluetoothListAdapter;
    private BluetoothAdapter bluetoothAdapterObj;
    private final List<String> bluetoothList = new ArrayList<>();
    private final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private boolean isBluetoothReceiverRegistered = false;
    private BluetoothConnectionListener connectionListener;
    private BluetoothDevice connectedDevice; // 新增變數保存已連接的設備

    // 回調介面，用於傳遞藍牙連接
    public interface BluetoothConnectionListener {
        void onConnected(BluetoothSocket socket);
    }

    public void setBluetoothConnectionListener(BluetoothConnectionListener listener) {
        this.connectionListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapterObj = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "Fragment created, BluetoothAdapter: " + (bluetoothAdapterObj != null));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_setup_bluetooth, container, false);

        switchBluetooth = view.findViewById(R.id.switch_bluetooth);
        recyclerBluetooth = view.findViewById(R.id.recycler_bluetooth);
        TextView tvBluetoothStatus = view.findViewById(R.id.tv_bluetooth_status);

        recyclerBluetooth.setLayoutManager(new LinearLayoutManager(getContext()));
        bluetoothListAdapter = new BluetoothListAdapter(bluetoothList, this::connectToBluetoothDevice);
        recyclerBluetooth.setAdapter(bluetoothListAdapter);

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Bluetooth switch changed: " + isChecked);
            Toast.makeText(requireContext(), "Bluetooth switch: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
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

        return view;
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Bluetooth device discovered");
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Bluetooth scan permission not granted");
                return;
            }
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getName() != null && !bluetoothList.contains(device.getName())) {
                String deviceName = device.getName();
                bluetoothList.add(deviceName);
                bluetoothDevices.add(device);
                bluetoothListAdapter.notifyItemInserted(bluetoothList.size() - 1); // 使用更精確的更新方法
                Log.d(TAG, "New device added: " + deviceName);
//                Toast.makeText(requireContext(), "發現設備: " + deviceName, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void scanBluetooth() {
        Log.d(TAG, "Starting Bluetooth scan");
        Toast.makeText(requireContext(), "開始掃描藍牙設備", Toast.LENGTH_SHORT).show();
        if (bluetoothAdapterObj == null || !bluetoothAdapterObj.isEnabled()) {
            Log.w(TAG, "Bluetooth adapter is null or not enabled");
            Toast.makeText(requireContext(), "請開啟藍牙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Bluetooth permissions not granted");
            requestBluetoothPermissions();
            return;
        }
        bluetoothDevices.clear();
        bluetoothList.clear();
        bluetoothListAdapter.notifyDataSetChanged();
        Log.d(TAG, "Bluetooth device list cleared");

        if (!isBluetoothReceiverRegistered) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                requireContext().registerReceiver(bluetoothReceiver, filter);
                isBluetoothReceiverRegistered = true;
                Log.d(TAG, "Bluetooth receiver registered");
            } else {
                Log.w(TAG, "Bluetooth scan permission not granted during registration");
                Toast.makeText(requireContext(), "缺少藍牙掃描權限", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapterObj.startDiscovery();
            Log.d(TAG, "Bluetooth discovery started");
        }
    }

    private void connectToBluetoothDevice(String deviceName) {
        Log.d(TAG, "Attempting to connect to device: " + deviceName);
        Toast.makeText(requireContext(), "嘗試連接設備: " + deviceName, Toast.LENGTH_SHORT).show();
        BluetoothDevice device;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            device = bluetoothDevices.stream()
                    .filter(d -> deviceName.equals(d.getName()))
                    .findFirst()
                    .orElse(null);
        } else {
            Log.w(TAG, "Bluetooth connect permission not granted");
            Toast.makeText(requireContext(), "缺少藍牙連接權限", Toast.LENGTH_SHORT).show();
            return;
        }

        if (device == null) {
            Log.w(TAG, "Device not found: " + deviceName);
            Toast.makeText(requireContext(), "未找到設備", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Bluetooth connect permission not granted in thread");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "缺少藍牙連接權限", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                connectedDevice = device; // 保存已連接的設備
                Log.d(TAG, "Bluetooth connection established to: " + deviceName);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "藍牙連接成功：" + deviceName, Toast.LENGTH_SHORT).show();
//                    switchBluetooth.setChecked(false); // 連接成功後關閉開關
                    recyclerBluetooth.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "觸發回調給 SetupFragment", Toast.LENGTH_SHORT).show();
                    if (connectionListener != null) {
                        Log.d(TAG, "Calling connectionListener with socket");
                        connectionListener.onConnected(socket);
                    } else {
                        Log.w(TAG, "connectionListener is null");
                        Toast.makeText(requireContext(), "回調監聽器為空", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Bluetooth connection failed: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "藍牙連接失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "權限不足：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private boolean hasBluetoothPermissions() {
        boolean hasScanPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        boolean hasConnectPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Bluetooth permissions - Scan: " + hasScanPermission + ", Connect: " + hasConnectPermission);
        return hasScanPermission && hasConnectPermission;
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking Bluetooth permissions");
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Requesting Bluetooth permissions");
            requestBluetoothPermissions();
        }
    }

    private void requestBluetoothPermissions() {
        Log.d(TAG, "Requesting Bluetooth permissions (API >= S)");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called");
        super.onDestroyView();
        if (isBluetoothReceiverRegistered) {
            requireContext().unregisterReceiver(bluetoothReceiver);
            isBluetoothReceiverRegistered = false;
            Log.d(TAG, "Bluetooth receiver unregistered");
        }
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
}