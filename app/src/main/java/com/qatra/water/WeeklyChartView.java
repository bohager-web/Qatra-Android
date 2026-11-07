package com.qatra.water;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lightweight seven-day chart with no external charting dependency. */
public final class WeeklyChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<QatraStore.DayRecord> days;
    private final int goalMl;

    public WeeklyChartView(Context context, List<QatraStore.DayRecord> recentDays, int goalMl) {
        super(context);
        days = new ArrayList<>(recentDays);
        Collections.reverse(days);
        this.goalMl = Math.max(1, goalMl);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        labelPaint.setColor(0xFF68859A);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float leftPadding = 12f * density;
        float rightPadding = 12f * density;
        float topPadding = 20f * density;
        float bottomPadding = 34f * density;
        float chartWidth = getWidth() - leftPadding - rightPadding;
        float chartHeight = getHeight() - topPadding - bottomPadding;
        if (chartWidth <= 0 || chartHeight <= 0 || days.isEmpty()) {
            return;
        }

        int maxAmount = goalMl;
        for (QatraStore.DayRecord day : days) {
            maxAmount = Math.max(maxAmount, day.amountMl);
        }
        maxAmount = Math.round(maxAmount * 1.12f);

        paint.setStrokeWidth(1f * density);
        paint.setColor(0xFFE1F1FA);
        for (int i = 0; i <= 4; i++) {
            float y = topPadding + chartHeight * i / 4f;
            canvas.drawLine(leftPadding, y, getWidth() - rightPadding, y, paint);
        }

        float goalY = topPadding + chartHeight * (1f - goalMl / (float) maxAmount);
        paint.setColor(0xFF8DD6FF);
        paint.setStrokeWidth(1.5f * density);
        canvas.drawLine(leftPadding, goalY, getWidth() - rightPadding, goalY, paint);

        float slot = chartWidth / days.size();
        float barWidth = Math.min(28f * density, slot * 0.56f);
        labelPaint.setTextSize(11f * density);
        for (int i = 0; i < days.size(); i++) {
            QatraStore.DayRecord day = days.get(i);
            float centerX = leftPadding + slot * i + slot / 2f;
            float ratio = day.amountMl / (float) maxAmount;
            float barTop = topPadding + chartHeight * (1f - ratio);
            RectF bar = new RectF(centerX - barWidth / 2f, barTop,
                    centerX + barWidth / 2f, topPadding + chartHeight);
            paint.setColor(day.amountMl >= goalMl ? 0xFF0875C9 : 0xFF5FCBFF);
            canvas.drawRoundRect(bar, barWidth / 2f, barWidth / 2f, paint);

            String label = day.dayName == null || day.dayName.isEmpty()
                    ? ""
                    : day.dayName.substring(0, Math.min(3, day.dayName.length()));
            canvas.drawText(label, centerX, getHeight() - 9f * density, labelPaint);
        }
    }
}
