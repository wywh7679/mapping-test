package com.benco.mapping.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.R;

import java.util.ArrayList;

public class ApplicationsDataListAdapter extends RecyclerView.Adapter<ApplicationsDataListAdapter.ApplicationsDataViewHolder> {
    ArrayList<ApplicationsData> applicationsDataArrayList;

    public ApplicationsDataListAdapter(ArrayList<ApplicationsData> applications) {
        this.applicationsDataArrayList = applications;
    }

    @NonNull
    @Override
    public ApplicationsDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.recyclerview_lat_lng_update_list, parent, false);
        return new ApplicationsDataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationsDataViewHolder holder, int position) {
        if (applicationsDataArrayList.get(position).lat == null) applicationsDataArrayList.get(position).lat = 0d;
        if (applicationsDataArrayList.get(position).lng == null) applicationsDataArrayList.get(position).lng = 0d;
        if (applicationsDataArrayList.get(position).speed == null) applicationsDataArrayList.get(position).speed = 0d;
        if (applicationsDataArrayList.get(position).bearing == 0) applicationsDataArrayList.get(position).bearing = 0f;
        String aid = applicationsDataArrayList.get(position).aid+"";
        holder.text_adid.setText(aid);
        holder.text_lat.setText(String.format("%.2f", applicationsDataArrayList.get(position).lat));
        holder.text_lng.setText(String.format("%.2f", applicationsDataArrayList.get(position).lng));
        holder.text_speed.setText(String.format("%.2f", applicationsDataArrayList.get(position).speed));
        holder.text_bearing.setText(String.format("%.2f", applicationsDataArrayList.get(position).bearing));
    }

    @Override
    public int getItemCount() {
        return applicationsDataArrayList.size();
    }

    static class ApplicationsDataViewHolder extends RecyclerView.ViewHolder {
        TextView text_adid;
        TextView text_lat;
        TextView text_lng;
        TextView text_bearing;
        TextView text_speed;

        public ApplicationsDataViewHolder(@NonNull View itemView) {
            super(itemView);
            findViews(itemView);
        }

        private void findViews(View itemView) {
            text_adid = itemView.findViewById(R.id.text_adid);
            text_lat = itemView.findViewById(R.id.text_lat);
            text_lng = itemView.findViewById(R.id.text_lng);
            text_bearing = itemView.findViewById(R.id.text_bearing);
            text_speed = itemView.findViewById(R.id.text_speed);
        }
    }
}