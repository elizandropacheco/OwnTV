package com.conreo.couchytv;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Local visual identity selection. The system app icon remains the EP developer signature. */
public final class OwnBrand {
    private static boolean started;

    public static String mode(Context c) {
        return c.getSharedPreferences("own_brand", 0).getString("mode", "ep");
    }

    public static int theme(Context c) {
        String m = mode(c);
        return m.equals("owntv")
                ? R.style.Theme_LiteTV_OwnTV
                : m.equals("custom") ? R.style.Theme_LiteTV_Custom : R.style.Theme_LiteTV;
    }

    public static Drawable logo(Context c) {
        if (mode(c).equals("custom"))
            try {
                Uri u = Uri.parse(c.getSharedPreferences("own_brand", 0).getString("custom", ""));
                BitmapFactory.Options b = new BitmapFactory.Options();
                b.inJustDecodeBounds = true;
                try (InputStream in = c.getContentResolver().openInputStream(u)) {
                    BitmapFactory.decodeStream(in, null, b);
                }
                int sample = 1;
                while (Math.max(b.outWidth, b.outHeight) / sample > 1024) sample *= 2;
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = sample;
                try (InputStream in = c.getContentResolver().openInputStream(u)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(in, null, o);
                    if (bitmap != null) return new BitmapDrawable(c.getResources(), bitmap);
                }
            } catch (Exception ignored) {
            }
        return c.getDrawable(
                mode(c).equals("owntv") ? R.drawable.own_monogram : R.drawable.own_ep_monogram);
    }

    public static void showIntro(Activity a) {
        if (started) return;
        started = true;
        preview(a);
    }

    /** Render only published bitmap assets; private logo geometry is never embedded in code. */
    private static Bitmap introBitmap(Context context) {
        Drawable drawable = logo(context);
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        int width = Math.max(1, drawable.getIntrinsicWidth()),
                height = Math.max(1, drawable.getIntrinsicHeight());
        float scale = Math.min(1f, 1024f / Math.max(width, height));
        Bitmap bitmap =
                Bitmap.createBitmap(
                        Math.max(1, Math.round(width * scale)),
                        Math.max(1, Math.round(height * scale)),
                        Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    public static void preview(Activity a) {
        Bitmap image = introBitmap(a);
        View cover =
                new View(a) {
                    long first;
                    boolean removing;
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

                    @Override
                    protected void onDraw(Canvas canvas) {
                        long now = android.os.SystemClock.uptimeMillis();
                        if (first == 0) first = now;
                        float elapsed = now - first;
                        float enter = Math.min(1, elapsed / 500f),
                                fade = elapsed <= 650 ? 1 : Math.max(0, 1 - (elapsed - 650) / 180f);
                        canvas.drawARGB(Math.round(255 * fade), 3, 11, 18);
                        float
                                available =
                                        Math.min(
                                                getWidth() * .7f,
                                                240 * getResources().getDisplayMetrics().density),
                                scale =
                                        Math.min(
                                                        available / image.getWidth(),
                                                        available / image.getHeight())
                                                * (.92f + .08f * enter);
                        float w = image.getWidth() * scale, h = image.getHeight() * scale;
                        paint.setAlpha(Math.round(255 * fade * (.35f + .65f * enter)));
                        canvas.drawBitmap(
                                image,
                                null,
                                new RectF(
                                        (getWidth() - w) / 2,
                                        (getHeight() - h) / 2,
                                        (getWidth() + w) / 2,
                                        (getHeight() + h) / 2),
                                paint);
                        if (elapsed < 830) postInvalidateOnAnimation();
                        else if (!removing) {
                            removing = true;
                            post(
                                    () -> {
                                        if (getParent() instanceof ViewGroup)
                                            ((ViewGroup) getParent()).removeView(this);
                                    });
                        }
                    }
                };
        a.addContentView(cover, new ViewGroup.LayoutParams(-1, -1));
    }
}
