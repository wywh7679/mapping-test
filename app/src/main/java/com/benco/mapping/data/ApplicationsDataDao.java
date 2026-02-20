package com.benco.mapping.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ApplicationsDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ApplicationsData applicationsData);

    @Update
    void update(ApplicationsData applicationsData);

    @Query("SELECT * from applications_data WHERE aid=:aid AND lat!=0 AND lng!=0 ORDER By dateTime DESC LIMIT 2")
    LiveData<List<ApplicationsData>> getApplicationsData(int aid);

    @Query("UPDATE applications_data SET aid =:aid WHERE aid=:aid")
    int reloadAllApplicationsData(int aid);

    @Query("SELECT * from applications_data WHERE aid=:aid AND lat!=0 AND lng!=0 ORDER By dateTime ASC")
    LiveData<List<ApplicationsData>> getAllApplicationsData(int aid);

    @Query("SELECT * from applications_data WHERE aid=:aid AND lat!=0 AND lng!=0 ORDER By dateTime ASC")
    List<ApplicationsData> getAllApplicationsDataList(int aid);

    @Query("DELETE from applications_data")
    void deleteAll();
}
