package com.conreo.couchytv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** One fixed, dithered surface shared by every native page. */
public final class OwnBackdrop extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(
                new LinearGradient(
                        0,
                        0,
                        w,
                        h,
                        new int[] {0xFF030B12, 0xFF102A38, 0xFF06151E},
                        null,
                        Shader.TileMode.CLAMP));
        c.drawRect(b, paint);
        paint.setShader(
                new RadialGradient(
                        w * .8f,
                        h * .18f,
                        w * .65f,
                        new int[] {0x240084FE, 0x000084FE},
                        null,
                        Shader.TileMode.CLAMP));
        c.drawRect(b, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.3f);
        paint.setShader(
                new LinearGradient(
                        w * .5f,
                        0,
                        w,
                        h,
                        new int[] {0x110084FE, 0x550084FE, 0x332CEB43},
                        null,
                        Shader.TileMode.CLAMP));
        for (int i = 0; i < 5; i++) {
            Path p = new Path();
            p.moveTo(w * (.61f + i * .055f), -10);
            p.lineTo(w * (.88f + i * .055f), h * .5f);
            p.lineTo(w * (.7f + i * .055f), h * .83f);
            p.lineTo(w * (.9f + i * .055f), h * 1.1f);
            c.drawPath(p, paint);
        }
        paint.setShader(null);
    }

    @Override
    public void setAlpha(int a) {}

    @Override
    public void setColorFilter(ColorFilter f) {}

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
