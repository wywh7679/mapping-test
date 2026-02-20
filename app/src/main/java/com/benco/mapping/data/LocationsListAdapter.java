package com.benco.mapping.data;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.ApplicationsActivity;
import com.benco.mapping.R;

import java.util.ArrayList;

public class LocationsListAdapter extends RecyclerView.Adapter<LocationsListAdapter.LocationsViewHolder> {
    ArrayList<Locations> locationsArrayList;
    private final OnLocationDeleteListener onLocationDeleteListener;

    public interface OnLocationDeleteListener {
        void onDeleteRequested(Locations location);
    }

    public LocationsListAdapter(ArrayList<Locations> locations, OnLocationDeleteListener onLocationDeleteListener) {
        this.locationsArrayList = locations;
        this.onLocationDeleteListener = onLocationDeleteListener;
    }

    @NonNull
    @Override
    public LocationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.recyclerview_locations_item_edit_list, parent, false);
        return new LocationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationsViewHolder holder, int position) {
        holder.editLocationsName.setText(locationsArrayList.get(position).name);
        holder.listLocationsViewButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ApplicationsActivity.class);
            intent.putExtra("lid", locationsArrayList.get(holder.getAdapterPosition()).lid);
            holder.itemView.getContext().startActivity(intent);
        });
        holder.listLocationsEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ApplicationsActivity.class);
            intent.putExtra("lid", locationsArrayList.get(holder.getAdapterPosition()).lid);
            holder.itemView.getContext().startActivity(intent);
        });
        holder.listLocationsDeleteButton.setOnClickListener(v -> {
            if (onLocationDeleteListener != null) {
                onLocationDeleteListener.onDeleteRequested(locationsArrayList.get(holder.getAdapterPosition()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return locationsArrayList.size();
    }

    static class LocationsViewHolder extends RecyclerView.ViewHolder {
        TextView editLocationsName;
        Button listLocationsViewButton;
        Button listLocationsEditButton;
        Button listLocationsDeleteButton;

        public LocationsViewHolder(@NonNull View itemView) {
            super(itemView);
            findViews(itemView);
        }

        private void findViews(View itemView) {
            editLocationsName = itemView.findViewById(R.id.listLocationsName);
            listLocationsViewButton = itemView.findViewById(R.id.listLocationsViewButton);
            listLocationsEditButton = itemView.findViewById(R.id.listLocationsEditButton);
            listLocationsDeleteButton = itemView.findViewById(R.id.listLocationsDeleteButton);
        }
    }
}
