package com.qatra.water;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class ReminderScheduler {
    private static final int REQUEST_CODE = 7201;

    private ReminderScheduler() {
    }

    public static void apply(Context context) {
        QatraStore store = new QatraStore(context);
        if (store.isReminderEnabled()) {
            schedule(context, store.getReminderIntervalHours());
        } else {
            cancel(context);
        }
    }

    public static void schedule(Context context, int intervalHours) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        long interval = Math.max(1, Math.min(6, intervalHours)) * AlarmManager.INTERVAL_HOUR;
        long firstTrigger = System.currentTimeMillis() + interval;
        manager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                firstTrigger,
                interval,
                pendingIntent(context)
        );
    }

    public static void cancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) {
            manager.cancel(pendingIntent(context));
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
