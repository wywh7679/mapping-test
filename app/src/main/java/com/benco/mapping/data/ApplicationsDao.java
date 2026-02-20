package com.benco.mapping.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ApplicationsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Applications applications);

    @Update
    void update(Applications applications);

    //@Query("SELECT DISTINCT applications.lid, applications.* from applications, applications_data WHERE applications.lid=:lid AND applications_data.lid=:lid")
    @Query("SELECT applications.*, applications_data.*, COUNT(applications_data.adid) AS rowCount FROM applications_data, applications WHERE applications.aid=applications_data.aid AND applications_data.lat!=0 AND applications_data.lng!=0 AND applications.lid=:lid GROUP BY applications_data.aid")
    LiveData<List<Applications>> getApplicationByLid(int lid);

    @Query("SELECT * from applications ORDER By aid Asc")
    LiveData<List<Applications>> getApplications();

    @Query("SELECT * from applications WHERE aid=:aid")
    LiveData<List<Applications>> getApplication(int aid);

    @Query("DELETE from applications")
    void deleteAll();
}