package com.example.smartsite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {

    private final List<String> wifiList;
    private final OnWifiClickListener listener;

    public interface OnWifiClickListener {
        void onWifiClick(String ssid);
    }

    public WifiAdapter(List<String> wifiList, OnWifiClickListener listener) {
        this.wifiList = wifiList;
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
        String ssid = wifiList.get(position);
        holder.wifiName.setText(ssid);

        // 設置點擊事件，觸發連線
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWifiClick(ssid);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView wifiName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiName = itemView.findViewById(android.R.id.text1);
        }
    }
}
