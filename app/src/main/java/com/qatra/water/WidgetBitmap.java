package com.qatra.water;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Creates a fresh drop image for each widget update. */
public final class WidgetBitmap {
    private WidgetBitmap() {
    }

    public static Bitmap render(Context context, int widthDp, int heightDp, float rawProgress) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.max(48, Math.round(widthDp * density));
        int height = Math.max(64, Math.round(heightDp * density));
        float progress = Math.max(0f, Math.min(1f, rawProgress));

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint shell = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint water = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path drop = new Path();
        Path wave = new Path();

        float padding = 4f * density;
        float size = Math.min(width - padding * 2f, height - padding * 2f);
        RectF bounds = new RectF((width - size) / 2f, (height - size) / 2f,
                (width + size) / 2f, (height + size) / 2f);
        DropProgressView.buildDropPath(drop, bounds);

        shell.setColor(Color.WHITE);
        canvas.drawPath(drop, shell);

        int save = canvas.save();
        canvas.clipPath(drop);
        Paint pale = new Paint(Paint.ANTI_ALIAS_FLAG);
        pale.setColor(0xFFEAF7FF);
        canvas.drawRect(bounds, pale);

        float innerTop = bounds.top + bounds.height() * 0.12f;
        float innerBottom = bounds.bottom - bounds.height() * 0.07f;
        float waterY = innerBottom - (innerBottom - innerTop) * progress;
        float amplitude = 2.2f * density;
        float wavelength = bounds.width() * 0.48f;
        wave.moveTo(bounds.left - wavelength, waterY);
        for (float x = bounds.left - wavelength; x <= bounds.right + wavelength; x += density * 2f) {
            float y = (float) (waterY + Math.sin(x / wavelength * Math.PI * 2f) * amplitude);
            wave.lineTo(x, y);
        }
        wave.lineTo(bounds.right + wavelength, bounds.bottom + density * 4f);
        wave.lineTo(bounds.left - wavelength, bounds.bottom + density * 4f);
        wave.close();
        water.setShader(new LinearGradient(0, waterY, 0, bounds.bottom,
                0xFF64CEFF, 0xFF078DE8, Shader.TileMode.CLAMP));
        canvas.drawPath(wave, water);
        canvas.restoreToCount(save);

        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(1.8f * density);
        outline.setColor(0xFFBFE8FF);
        canvas.drawPath(drop, outline);
        return bitmap;
    }
}
