package com.qatra.water;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Private receiver used only by PendingIntents created by this app's widget.
 * Keeping it unexported prevents other apps from forging water-entry actions.
 */
public final class WidgetActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        QatraStore store = new QatraStore(context);
        if (QatraWidgetProvider.ACTION_ADD_250.equals(action)) {
            store.addWater(250);
        } else if (QatraWidgetProvider.ACTION_ADD_500.equals(action)) {
            store.addWater(500);
        } else if (QatraWidgetProvider.ACTION_UNDO.equals(action)) {
            store.undoLast();
        } else {
            return;
        }
        QatraWidgetProvider.updateAll(context);
        context.sendBroadcast(new Intent(QatraWidgetProvider.ACTION_DATA_CHANGED)
                .setPackage(context.getPackageName()));
    }
}
