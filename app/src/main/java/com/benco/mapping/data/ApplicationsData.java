package com.benco.mapping.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;
@Entity(indices = {@Index("adid"), @Index("aid"), @Index("lid"), @Index("dateTime")},tableName = "applications_data")
public class ApplicationsData {
    @PrimaryKey(autoGenerate = true)
    public int adid;
    public int aid;

    public int lid;

    public Date dateTime;
    public Double lat;
    public Double lng;
    public Double speed;
    public float bearing;
    public float timestamp;
    public float azimuth;
    public String isSpraying;
    public String aLine;
    public String bLine;

}
