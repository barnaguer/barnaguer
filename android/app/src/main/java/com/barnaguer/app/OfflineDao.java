package com.barnaguer.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface OfflineDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertPending(PendingBooking booking);

    @Query("SELECT * FROM pending_bookings WHERE status = 'PENDIENTE' ORDER BY createdAt ASC")
    List<PendingBooking> getPendingBookings();

    @Query("UPDATE pending_bookings SET status = :status WHERE id = :id")
    void updateBookingStatus(String id, String status);

    @Query("SELECT * FROM pending_bookings WHERE bookingDate = :date AND bookingTime = :time AND status = 'PENDIENTE' LIMIT 1")
    PendingBooking findPendingForSlot(String date, String time);

    @Query("SELECT * FROM pending_bookings ORDER BY createdAt DESC LIMIT 50")
    List<PendingBooking> getRecentBookings();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void cacheOccupiedSlot(CachedSlot slot);

    @Query("DELETE FROM cached_slots WHERE bookingDate = :date")
    void clearCachedSlotsForDate(String date);

    @Query("SELECT bookingTime FROM cached_slots WHERE bookingDate = :date")
    List<String> getOccupiedSlots(String date);
}
