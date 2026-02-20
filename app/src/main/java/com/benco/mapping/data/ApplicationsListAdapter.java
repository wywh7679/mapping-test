package com.benco.mapping.data;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.GLMapActivity;
import com.benco.mapping.MainActivity;
import com.benco.mapping.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ApplicationsListAdapter extends RecyclerView.Adapter<ApplicationsListAdapter.ApplicationsViewHolder> {
    ArrayList<Applications> applicationsArrayList;
    private final OnApplicationDeleteListener onApplicationDeleteListener;

    public interface OnApplicationDeleteListener {
        void onDeleteRequested(Applications application);
    }

    public ApplicationsListAdapter(ArrayList<Applications> applications, OnApplicationDeleteListener onApplicationDeleteListener) {
        this.applicationsArrayList = applications;
        this.onApplicationDeleteListener = onApplicationDeleteListener;
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
        holder.listApplicationEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
            intent.putExtra("aid", applicationsArrayList.get(holder.getAdapterPosition()).aid + "");
            holder.itemView.getContext().startActivity(intent);
        });
        holder.listApplicationEditButton3d.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), GLMapActivity.class);
            intent.putExtra("aid", applicationsArrayList.get(holder.getAdapterPosition()).aid + "");
            holder.itemView.getContext().startActivity(intent);
        });
        holder.listApplicationDeleteButton.setOnClickListener(v -> {
            if (onApplicationDeleteListener != null) {
                onApplicationDeleteListener.onDeleteRequested(applicationsArrayList.get(holder.getAdapterPosition()));
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
        Button listApplicationDeleteButton;

        public ApplicationsViewHolder(@NonNull View itemView) {
            super(itemView);
            findViews(itemView);
        }

        private void findViews(View itemView) {
            listApplicationName = itemView.findViewById(R.id.listApplicationName);
            listApplicationEditButton = itemView.findViewById(R.id.listApplicationEditButton);
            listApplicationEditButton3d = itemView.findViewById(R.id.listApplicationEditButton3d);
            listApplicationDeleteButton = itemView.findViewById(R.id.listApplicationDeleteButton);
        }
    }
}
