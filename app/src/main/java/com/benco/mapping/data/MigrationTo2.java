package com.benco.mapping.data;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationTo2 extends Migration {
    public MigrationTo2() {
        super(2, 3);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // Add the new column
        // database.execSQL("ALTER TABLE applications_data ADD COLUMN isSpraying INTEGER DEFAULT 0");
        database.execSQL("ALTER TABLE applications ADD COLUMN config TEXT DEFAULT ''");
        database.execSQL("CREATE TABLE locations_new(lid INTEGER PRIMARY KEY, name TEXT, config TEXT)");
        database.execSQL("INSERT INTO locations_new(lid, name) SELECT lid, name FROM locations");
        database.execSQL("DROP TABLE locations");
        database.execSQL("ALTER TABLE locations_new RENAME TO locations");
    }
}