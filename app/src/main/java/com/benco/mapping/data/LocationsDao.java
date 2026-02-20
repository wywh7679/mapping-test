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

    @Query("DELETE from locations WHERE lid=:lid")
    void deleteLocationById(int lid);

    @Query("SELECT * from locations WHERE lid=:lid LIMIT 1")
    Locations getLocationByIdSync(int lid);
}
