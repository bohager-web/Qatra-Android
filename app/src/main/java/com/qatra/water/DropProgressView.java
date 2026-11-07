package com.qatra.water;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

/** Animated water-filled drop used on the main screen. */
public final class DropProgressView extends View {
    private final Paint shellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dropPath = new Path();
    private final Path wavePath = new Path();
    private final RectF bounds = new RectF();

    private float progress = 0f;
    private float wavePhase = 0f;
    private ValueAnimator waveAnimator;
    private ValueAnimator progressAnimator;

    public DropProgressView(Context context) {
        this(context, null);
    }

    public DropProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        shellPaint.setColor(Color.WHITE);
        shellPaint.setShadowLayer(dp(18), 0, dp(8), 0x260078B8);

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(3));
        outlinePaint.setColor(0xFFBFE8FF);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD));
        textPaint.setColor(0xFF0875C9);
    }

    public void setProgress(float newProgress, boolean animate) {
        float target = clamp(newProgress);
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (!animate) {
            progress = target;
            invalidate();
            return;
        }
        progressAnimator = ValueAnimator.ofFloat(progress, target);
        progressAnimator.setDuration(650L);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startWaveAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (waveAnimator != null) {
            waveAnimator.cancel();
        }
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = dp(20);
        float availableWidth = getWidth() - padding * 2;
        float availableHeight = getHeight() - padding * 2;
        float size = Math.min(availableWidth, availableHeight);
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        bounds.set(left, top, left + size, top + size);
        buildDropPath(dropPath, bounds);

        canvas.drawPath(dropPath, shellPaint);

        int save = canvas.save();
        canvas.clipPath(dropPath);
        Paint softFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        softFill.setColor(0xFFEAF7FF);
        canvas.drawRect(bounds, softFill);

        float innerTop = bounds.top + bounds.height() * 0.12f;
        float innerBottom = bounds.bottom - bounds.height() * 0.08f;
        float waterY = innerBottom - (innerBottom - innerTop) * progress;
        float amplitude = Math.max(dp(3), bounds.height() * 0.018f);
        float wavelength = Math.max(dp(40), bounds.width() * 0.42f);

        wavePath.reset();
        wavePath.moveTo(bounds.left - wavelength, waterY);
        for (float x = bounds.left - wavelength; x <= bounds.right + wavelength; x += dp(3)) {
            float y = (float) (waterY + Math.sin((x / wavelength * Math.PI * 2f) + wavePhase) * amplitude);
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(bounds.right + wavelength, bounds.bottom + dp(10));
        wavePath.lineTo(bounds.left - wavelength, bounds.bottom + dp(10));
        wavePath.close();

        waterPaint.setShader(new LinearGradient(
                0, waterY, 0, bounds.bottom,
                0xFF5FCBFF, 0xFF078DE8,
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(wavePath, waterPaint);

        Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlight.setColor(0x55FFFFFF);
        canvas.drawOval(
                bounds.left + bounds.width() * 0.28f,
                bounds.top + bounds.height() * 0.27f,
                bounds.left + bounds.width() * 0.40f,
                bounds.top + bounds.height() * 0.48f,
                highlight
        );
        canvas.restoreToCount(save);

        canvas.drawPath(dropPath, outlinePaint);

        int percentage = Math.round(progress * 100f);
        textPaint.setTextSize(bounds.width() * 0.14f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = bounds.centerY() - (fm.ascent + fm.descent) / 2f + bounds.height() * 0.12f;
        textPaint.setShadowLayer(dp(2), 0, dp(1), 0x55FFFFFF);
        canvas.drawText(String.format(Locale.US, "%d%%", percentage), bounds.centerX(), textY, textPaint);
        textPaint.clearShadowLayer();
    }

    private void startWaveAnimation() {
        if (waveAnimator != null && waveAnimator.isRunning()) {
            return;
        }
        waveAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        waveAnimator.setDuration(2400L);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setInterpolator(null);
        waveAnimator.addUpdateListener(animation -> {
            wavePhase = (float) animation.getAnimatedValue();
            invalidate();
        });
        waveAnimator.start();
    }

    static void buildDropPath(Path path, RectF box) {
        float cx = box.centerX();
        float top = box.top + box.height() * 0.04f;
        float bottom = box.bottom - box.height() * 0.04f;
        float left = box.left + box.width() * 0.16f;
        float right = box.right - box.width() * 0.16f;
        float shoulderY = box.top + box.height() * 0.48f;

        path.reset();
        path.moveTo(cx, top);
        path.cubicTo(
                cx + box.width() * 0.08f,
                box.top + box.height() * 0.18f,
                right,
                box.top + box.height() * 0.42f,
                right,
                shoulderY
        );
        path.cubicTo(
                right,
                box.top + box.height() * 0.75f,
                cx + box.width() * 0.18f,
                bottom,
                cx,
                bottom
        );
        path.cubicTo(
                cx - box.width() * 0.18f,
                bottom,
                left,
                box.top + box.height() * 0.75f,
                left,
                shoulderY
        );
        path.cubicTo(
                left,
                box.top + box.height() * 0.42f,
                cx - box.width() * 0.08f,
                box.top + box.height() * 0.18f,
                cx,
                top
        );
        path.close();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
