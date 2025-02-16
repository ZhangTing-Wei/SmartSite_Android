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

    public WifiAdapter(List<String> wifiList) {
        this.wifiList = wifiList;
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
        holder.wifiName.setText(wifiList.get(position));
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
