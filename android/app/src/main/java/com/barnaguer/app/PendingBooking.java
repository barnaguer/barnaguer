package com.barnaguer.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_bookings", indices = {
    @Index(value = {"bookingDate", "bookingTime"}, unique = true)
})
public class PendingBooking {
    @PrimaryKey @NonNull public String id;
    @NonNull public String customerName;
    @NonNull public String customerPhone;
    @NonNull public String bookingDate;
    @NonNull public String bookingTime;
    @NonNull public String status;
    public long createdAt;

    public PendingBooking(@NonNull String id, @NonNull String customerName,
            @NonNull String customerPhone, @NonNull String bookingDate,
            @NonNull String bookingTime, @NonNull String status, long createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.status = status;
        this.createdAt = createdAt;
    }
}
