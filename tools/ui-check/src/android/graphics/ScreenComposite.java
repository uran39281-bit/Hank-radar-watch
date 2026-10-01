package android.graphics;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/** Desktop equivalent of PorterDuff SCREEN, including source alpha and paint opacity. */
final class ScreenComposite implements Composite {
    final float opacity;
    ScreenComposite(float opacity) { this.opacity = opacity; }
    public CompositeContext createContext(final ColorModel sourceModel,
            final ColorModel destinationModel, RenderingHints hints) {
        return new CompositeContext() {
            public void dispose() {}
            public void compose(Raster source, Raster destination, WritableRaster result) {
                int width = Math.min(source.getWidth(), destination.getWidth());
                int height = Math.min(source.getHeight(), destination.getHeight());
                Object sourcePixel = null, destinationPixel = null, outputPixel = null;
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    sourcePixel = source.getDataElements(source.getMinX() + x, source.getMinY() + y, sourcePixel);
                    destinationPixel = destination.getDataElements(destination.getMinX() + x, destination.getMinY() + y, destinationPixel);
                    int src = sourceModel.getRGB(sourcePixel), dst = destinationModel.getRGB(destinationPixel);
                    double a = ((src >>> 24) / 255.0) * opacity, b = (dst >>> 24) / 255.0;
                    double alpha = a + b - a * b;
                    int output = (int)Math.round(alpha * 255) << 24;
                    for (int shift = 0; shift <= 16; shift += 8) {
                        double s = ((src >> shift) & 255) / 255.0 * a;
                        double d = ((dst >> shift) & 255) / 255.0 * b;
                        int channel = alpha == 0 ? 0 : (int)Math.round((s + d - s * d) / alpha * 255);
                        output |= Math.min(255, Math.max(0, channel)) << shift;
                    }
                    outputPixel = destinationModel.getDataElements(output, outputPixel);
                    result.setDataElements(result.getMinX() + x, result.getMinY() + y, outputPixel);
                }
            }
        };
    }
}
