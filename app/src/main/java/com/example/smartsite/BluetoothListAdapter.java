package com.example.smartsite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.ViewHolder> {

    private final List<String> bluetoothDevices;
    private final OnBluetoothClickListener listener;

    public interface OnBluetoothClickListener {
        void onBluetoothClick(String deviceName);
    }

    public BluetoothListAdapter(List<String> bluetoothDevices, OnBluetoothClickListener listener) {
        this.bluetoothDevices = bluetoothDevices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String deviceName = bluetoothDevices.get(position);
        holder.deviceName.setText(deviceName);

        // 設置點擊事件，觸發連線
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBluetoothClick(deviceName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(android.R.id.text1);
        }
    }
}
