package com.benco.mapping.data;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.ApplicationsActivity;
import com.benco.mapping.HomeActivity;
import com.benco.mapping.LocationsActivity;
import com.benco.mapping.R;
import com.benco.mapping.SettingsActivity;

import java.util.ArrayList;

public class LocationsListAdapter  extends RecyclerView.Adapter<LocationsListAdapter.LocationsViewHolder> {
    ArrayList<Locations> locationsArrayList;

    public LocationsListAdapter(ArrayList<Locations> locations) {
        this.locationsArrayList = locations;
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
        holder.listLocationsViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Applications
                Intent intent = new Intent(holder.itemView.getContext(), ApplicationsActivity.class);
                intent.putExtra("lid", locationsArrayList.get(holder.getAdapterPosition()).lid); // Pass the ID or any other data
                holder.itemView.getContext().startActivity(intent);

            }
        });
        holder.listLocationsEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Applications
                Intent intent = new Intent(holder.itemView.getContext(), ApplicationsActivity.class);
                intent.putExtra("lid", locationsArrayList.get(holder.getAdapterPosition()).lid); // Pass the ID or any other data
                holder.itemView.getContext().startActivity(intent);

            }
        });
        /*holder.editLocationsALineLat.setText(String.format("%s", locationsArrayList.get(position).aLineLat));
        holder.editLocationsALineLng.setText(String.format("%s", locationsArrayList.get(position).aLineLng));
        holder.editLocationsBLineLat.setText(String.format("%s", locationsArrayList.get(position).bLineLat));
        holder.editLocationsBLineLng.setText(String.format("%s", locationsArrayList.get(position).bLineLng));*/
    }

    @Override
    public int getItemCount() {
        return locationsArrayList.size();
    }

    static class LocationsViewHolder extends RecyclerView.ViewHolder {
        TextView editLocationsName;
        Button listLocationsViewButton;
        Button listLocationsEditButton;
        TextView editLocationsALineLat;
        TextView editLocationsALineLng;
        TextView editLocationsBLineLat;
        TextView editLocationsBLineLng;

        public LocationsViewHolder(@NonNull View itemView) {
            super(itemView);
            findViews(itemView);
        }

        private void findViews(View itemView) {
            editLocationsName = itemView.findViewById(R.id.listLocationsName);
            listLocationsViewButton = itemView.findViewById(R.id.listLocationsViewButton);
            listLocationsEditButton = itemView.findViewById(R.id.listLocationsEditButton);
           /* editLocationsALineLat = itemView.findViewById(R.id.editLocationsALineLat);
            editLocationsALineLng = itemView.findViewById(R.id.editLocationsALineLng);
            editLocationsBLineLat = itemView.findViewById(R.id.editLocationsBLineLat);
            editLocationsBLineLng = itemView.findViewById(R.id.editLocationsBLineLng);*/
        }
    }
}