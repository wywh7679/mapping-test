package com.benco.mapping.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(indices = {@Index("aid"), @Index("lid"), @Index("dateTime")},tableName = "applications")
public class Applications {
    @PrimaryKey(autoGenerate = true)
    public int aid;

    public int lid;

    public Date dateTime;
    public String notes;
    public String config;

    public Applications(int lid, Date dateTime, String notes, String config) {
        this.lid = lid;
        this.dateTime = dateTime;
        this.notes = notes;
        this.config = config;
    }
}