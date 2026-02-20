package com.benco.mapping.data;
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
import com.benco.mapping.GLMapActivity;
import com.benco.mapping.HomeActivity;
import com.benco.mapping.MainActivity;
import com.benco.mapping.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ApplicationsListAdapter extends RecyclerView.Adapter<ApplicationsListAdapter.ApplicationsViewHolder> {
    ArrayList<Applications> applicationsArrayList;

    public ApplicationsListAdapter(ArrayList<Applications> applications) {
        this.applicationsArrayList = applications;
    }

    @NonNull
    @Override
    public ApplicationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.recyclerview_applications_item_edit_list, parent, false);
        return new ApplicationsListAdapter.ApplicationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationsViewHolder holder, int position) {
        Date dateTime = applicationsArrayList.get(position).dateTime;
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/YY hh:mm:ss");
        String formattedDate = formatter.format(dateTime);
        holder.listApplicationName.setText(formattedDate);
        holder.listApplicationEditButton.setText("View");
        holder.listApplicationEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Applications
                Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                intent.putExtra("aid", applicationsArrayList.get(holder.getAdapterPosition()).aid+""); // Pass the ID or any other data
                holder.itemView.getContext().startActivity(intent);
            }
        });
        holder.listApplicationEditButton3d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Applications
                Intent intent = new Intent(holder.itemView.getContext(), GLMapActivity.class);
                intent.putExtra("aid", applicationsArrayList.get(holder.getAdapterPosition()).aid+""); // Pass the ID or any other data
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return applicationsArrayList.size();
    }

    static class ApplicationsViewHolder extends RecyclerView.ViewHolder {
        TextView listApplicationName;
        Button listApplicationEditButton;
        Button listApplicationEditButton3d;

        public ApplicationsViewHolder(@NonNull View itemView) {
            super(itemView);
            findViews(itemView);
        }

        private void findViews(View itemView) {
            listApplicationName = itemView.findViewById(R.id.listApplicationName);
            listApplicationEditButton = itemView.findViewById(R.id.listApplicationEditButton);
            listApplicationEditButton3d = itemView.findViewById(R.id.listApplicationEditButton3d);

        }
    }
}
