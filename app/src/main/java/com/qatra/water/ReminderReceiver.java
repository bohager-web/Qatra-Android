package com.qatra.water;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalTime;
import java.util.Locale;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "qatra_water_reminders";
    private static final int NOTIFICATION_ID = 7401;

    @Override
    public void onReceive(Context context, Intent intent) {
        QatraStore store = new QatraStore(context);
        if (!store.isReminderEnabled()) {
            return;
        }
        int hour = LocalTime.now().getHour();
        if (!isInsideWindow(hour, store.getReminderStartHour(), store.getReminderEndHour())) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "تذكيرات شرب الماء",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("تذكيرات قطرة اليومية");
        manager.createNotificationChannel(channel);

        int total = store.getCurrentTotalMl();
        int goal = store.getGoalMl();
        int remaining = Math.max(0, goal - total);
        String text = remaining == 0
                ? "أحسنت! حققت هدف الماء اليومي."
                : String.format(Locale.US, "باقي %d مل للوصول إلى هدف اليوم 💧", remaining);

        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("حان وقت شرب الماء")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    private static boolean isInsideWindow(int hour, int start, int end) {
        if (start == end) {
            return true;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }
}
