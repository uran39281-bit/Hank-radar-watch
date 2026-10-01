package com.jb.radar;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Original supplied radar artwork, decoded once and composited without recoloring. */
final class RadarIcons {
    static final String[] FILES = {
        "player_missile.png", "hostile_jet.png", "friendly_jet.png",
        "hostile_missile.png", "hostile_no_ID.png", "friendlty_no_ID.png",
        "unknown_track.png", "Unknown_untracked_target.png"
    };
    private final Map<String, Bitmap> bitmaps = new HashMap<String, Bitmap>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();

    RadarIcons(AssetManager assets) {
        // SCREEN preserves supplied colors while the artwork's black backing leaves the scope visible.
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        paint.setFilterBitmap(true);
        for (String filename : FILES) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream input = assets.open("radar-icons/" + filename)) {
                    BitmapFactory.decodeStream(input, null, bounds);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 256)
                    options.inSampleSize *= 2;
                try (InputStream input = assets.open("radar-icons/" + filename)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                    if (bitmap != null) bitmaps.put(filename, bitmap);
                }
            } catch (IOException ignored) {
                // loaded() exposes missing artwork to verification and the caller's fallback.
            }
        }
    }

    boolean loaded(String filename) { return bitmaps.containsKey(filename); }

    /** Heading is clockwise from north; identification diamonds deliberately remain upright. */
    void draw(Canvas canvas, String filename, float cx, float cy, float boundingSize,
              double headingRadians, int alpha) {
        Bitmap bitmap = bitmaps.get(filename);
        if (bitmap == null || boundingSize <= 0 || alpha <= 0) return;
        float scale = boundingSize / Math.max(bitmap.getWidth(), bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        destination.set(-width / 2, -height / 2, width / 2, height / 2);
        paint.setAlpha(Math.min(255, alpha));
        canvas.save();
        canvas.translate(cx, cy);
        if (filename.equals("player_missile.png") || filename.equals("hostile_missile.png")
                || filename.equals("hostile_jet.png") || filename.equals("friendly_jet.png"))
            canvas.rotate((float)Math.toDegrees(headingRadians));
        canvas.drawBitmap(bitmap, null, destination, paint);
        canvas.restore();
    }
}
