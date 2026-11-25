package com.barnaguer.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_slots", primaryKeys = {"bookingDate", "bookingTime"})
public class CachedSlot {
    @NonNull public String bookingDate;
    @NonNull public String bookingTime;

    public CachedSlot(@NonNull String bookingDate, @NonNull String bookingTime) {
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
    }
}
