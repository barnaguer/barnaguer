package com.barnaguer.app;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class OfflineBookingSyncWorker extends Worker {
    private static final String TAG = "BarnaguerOfflineSync";
    private static final String SUPABASE_URL = "https://kxrsnlttqwneqgvwwclq.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_jNRVDDZX73NuPZt190O9vw_q8e0Lt37";

    public OfflineBookingSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        OfflineDatabase database = Room.databaseBuilder(
            getApplicationContext(), OfflineDatabase.class, "barnaguer-offline.db"
        ).build();
        try {
            for (PendingBooking booking : database.offlineDao().getPendingBookings()) {
                if (isStopped()) return Result.retry();
                HttpURLConnection connection = null;
                try {
                    URL endpoint = new URL(SUPABASE_URL + "/rest/v1/bookings");
                    connection = (HttpURLConnection) endpoint.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("apikey", SUPABASE_KEY);
                    connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Prefer", "return=minimal");

                    JSONObject payload = new JSONObject();
                    payload.put("customer_name", booking.customerName);
                    payload.put("customer_phone", booking.customerPhone);
                    payload.put("booking_date", booking.bookingDate);
                    payload.put("booking_time", booking.bookingTime);
                    byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(bytes);
                    }

                    int status = connection.getResponseCode();
                    String response = readResponse(status >= 400
                        ? connection.getErrorStream() : connection.getInputStream());

                    if (status >= 200 && status < 300) {
                        database.offlineDao().updateBookingStatus(booking.id, "CONFIRMADA");
                    } else if (status == 409 || response.contains("23505") ||
                            response.contains("P0001") ||
                            response.toLowerCase().contains("horario bloqueado")) {
                        database.offlineDao().updateBookingStatus(booking.id, "NO_DISPONIBLE");
                    } else if (status == 401 || status == 403 || status >= 500) {
                        Log.w(TAG, "La sincronización debe reintentarse. HTTP " + status);
                        return Result.retry();
                    } else {
                        Log.e(TAG, "Respuesta no reconocida al sincronizar: HTTP " + status);
                        return Result.retry();
                    }
                } catch (Exception error) {
                    Log.w(TAG, "No se pudo sincronizar una solicitud pendiente", error);
                    return Result.retry();
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            return Result.success();
        } finally {
            database.close();
        }
    }

    private static String readResponse(InputStream stream) {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
