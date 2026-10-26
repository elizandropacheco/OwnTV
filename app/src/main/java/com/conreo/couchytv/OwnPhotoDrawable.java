package com.conreo.couchytv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** A real cover background: no intrinsic-size positioning or uncovered bands. */
public final class OwnPhotoDrawable extends Drawable {
    private final Bitmap bitmap;
    private final float opacity, bias;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public OwnPhotoDrawable(Bitmap bitmap, float opacity, String alignment) {
        this.bitmap = bitmap;
        this.opacity = opacity;
        bias = alignment.equals("left") ? 0 : alignment.equals("center") ? .5f : 1f;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float scale =
                Math.max(
                        b.width() / (float) bitmap.getWidth(),
                        b.height() / (float) bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        float x = b.left + (b.width() - width) * bias, y = b.top + (b.height() - height) * .5f;
        paint.setShader(null);
        paint.setAlpha((int) (255 * opacity));
        canvas.save();
        canvas.clipRect(b);
        canvas.drawBitmap(bitmap, null, new RectF(x, y, x + width, y + height), paint);
        canvas.restore();
        paint.setAlpha(255);
    }

    @Override
    public void setAlpha(int a) {}

    @Override
    public void setColorFilter(ColorFilter f) {}

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
