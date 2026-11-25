package com.barnaguer.app;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PendingBooking.class, CachedSlot.class}, version = 1, exportSchema = false)
public abstract class OfflineDatabase extends RoomDatabase {
    public abstract OfflineDao offlineDao();
}
