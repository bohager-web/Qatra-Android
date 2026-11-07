package com.qatra.water;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Locale;

public final class QatraWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_ADD_250 = "com.qatra.water.action.ADD_250";
    public static final String ACTION_ADD_500 = "com.qatra.water.action.ADD_500";
    public static final String ACTION_UNDO = "com.qatra.water.action.UNDO";
    public static final String ACTION_DATA_CHANGED = "com.qatra.water.action.DATA_CHANGED";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateOne(context, manager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        updateOne(context, manager, appWidgetId);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, QatraWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) {
            updateOne(context, manager, id);
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager, int appWidgetId) {
        QatraStore store = new QatraStore(context);
        int total = store.getCurrentTotalMl();
        int goal = store.getGoalMl();
        int percent = goal <= 0 ? 0 : Math.min(100, Math.round(total * 100f / goal));
        float progress = goal <= 0 ? 0f : Math.min(1f, total / (float) goal);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_qatra);
        views.setTextViewText(R.id.widget_amount,
                String.format(Locale.US, "%d / %d مل", total, goal));
        views.setTextViewText(R.id.widget_percent,
                String.format(Locale.US, "%d%% من هدف اليوم", percent));
        views.setImageViewBitmap(R.id.widget_drop, WidgetBitmap.render(context, 68, 88, progress));

        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                context, appWidgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openPending);
        views.setOnClickPendingIntent(R.id.widget_drop, openPending);
        views.setOnClickPendingIntent(R.id.widget_title, openPending);
        views.setOnClickPendingIntent(R.id.widget_amount, openPending);

        views.setOnClickPendingIntent(R.id.widget_add_250,
                actionPendingIntent(context, ACTION_ADD_250, appWidgetId * 10 + 1));
        views.setOnClickPendingIntent(R.id.widget_add_500,
                actionPendingIntent(context, ACTION_ADD_500, appWidgetId * 10 + 2));
        views.setOnClickPendingIntent(R.id.widget_undo,
                actionPendingIntent(context, ACTION_UNDO, appWidgetId * 10 + 3));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent actionPendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, WidgetActionReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
