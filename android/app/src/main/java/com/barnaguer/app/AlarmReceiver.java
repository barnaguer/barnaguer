package com.barnaguer.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "barnaguer_turn_alarms";

    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL, "Alarmas de turnos BARNAGUER", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Avisos un minuto antes de cada turno");
                channel.enableVibration(true);
                channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
                manager.createNotificationChannel(channel);
            }
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title == null ? "BARNAGUER" : title)
            .setContentText(message == null ? "Próximo turno" : message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message == null ? "Próximo turno" : message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(content);
        manager.notify((intent.getStringExtra("alarm_id") == null ? "turno" : intent.getStringExtra("alarm_id")).hashCode(), notification.build());
    }
}
