package com.qatra.water;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Keeps the widget and reminder schedule healthy after date, clock and boot changes. */
public final class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        QatraStore store = new QatraStore(context);
        store.getCurrentTotalMl();
        QatraWidgetProvider.updateAll(context);
        context.sendBroadcast(new Intent(QatraWidgetProvider.ACTION_DATA_CHANGED)
                .setPackage(context.getPackageName()));
        ReminderScheduler.apply(context);
    }
}
