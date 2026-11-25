package com.barnaguer.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AlarmScheduler {
    private static final String PREFS = "barnaguer_alarms";
    private static final String KEY = "schedule";

    private AlarmScheduler() {}

    public static synchronized void replace(Context context, String json) throws Exception {
        JSONArray next = new JSONArray(json);
        JSONArray previous = new JSONArray(context.getSharedPreferences(PREFS, 0).getString(KEY, "[]"));
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) throw new IllegalStateException("AlarmManager unavailable");

        for (int i = 0; i < previous.length(); i++) {
            JSONObject item = previous.optJSONObject(i);
            if (item != null) cancelOne(context, manager, item.optString("id"));
        }

        JSONArray accepted = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < next.length(); i++) {
            JSONObject item = next.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "");
            long triggerAt = item.optLong("triggerAt", 0L);
            if (id.isEmpty() || triggerAt <= now) continue;

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("alarm_id", id);
            intent.putExtra("title", item.optString("title", "BARNAGUER"));
            intent.putExtra("message", item.optString("message", "Próximo turno"));
            PendingIntent pending = PendingIntent.getBroadcast(context, requestCode(id), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= 23) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
            else manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending);
            accepted.put(item);
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, accepted.toString()).apply();
    }

    private static void cancelOne(Context context, AlarmManager manager, String id) {
        if (id == null || id.isEmpty()) return;
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, requestCode(id), intent,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) { manager.cancel(pending); pending.cancel(); }
    }

    private static int requestCode(String id) { return id.hashCode() & 0x7fffffff; }

    public static void restore(Context context) {
        try {
            String json = context.getSharedPreferences(PREFS, 0).getString(KEY, "[]");
            replace(context, json);
        } catch (Exception ignored) {}
    }
}
