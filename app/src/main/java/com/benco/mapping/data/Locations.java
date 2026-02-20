package com.benco.mapping.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index("lid"), @Index("name")}, tableName = "locations")
public class Locations {
    @PrimaryKey(autoGenerate = true)
    public int lid;

    public String name;

    public String config;

    public Locations(String name, String config) {
        this.name = name;
        this.config = config;
    }

}
