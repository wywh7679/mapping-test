package com.benco.mapping.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Locations locations);

    @Update
    void update(Locations locations);

    @Query("SELECT * from locations ORDER By name Asc")
    LiveData<List<Locations>> getLocations();

    @Query("DELETE from locations")
    void deleteAll();
}
