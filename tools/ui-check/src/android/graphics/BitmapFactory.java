package android.graphics;

public class BitmapFactory {
    public static class Options {
        public int inSampleSize = 1, outWidth, outHeight;
        public boolean inJustDecodeBounds;
    }
    public static Bitmap decodeStream(java.io.InputStream stream, Object padding, Options options) {
        try {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(stream);
            if (original == null) return null;
            if (options != null) {
                options.outWidth = original.getWidth(); options.outHeight = original.getHeight();
                if (options.inJustDecodeBounds) return null;
            }
            Bitmap bitmap = new Bitmap();
            int sample = options == null ? 1 : Math.max(1, options.inSampleSize);
            if (sample == 1) bitmap.image = original;
            else {
                int width = Math.max(1, (original.getWidth() + sample - 1) / sample);
                int height = Math.max(1, (original.getHeight() + sample - 1) / sample);
                bitmap.image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D graphics = bitmap.image.createGraphics();
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(original, 0, 0, width, height, null);
                graphics.dispose();
            }
            return bitmap;
        } catch (java.io.IOException ignored) { return null; }
    }
}
