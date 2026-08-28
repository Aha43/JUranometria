package juranometria.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import juranometria.chart.ChartScene;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

/**
 * Draws a chart scene onto a Java2D target. The renderer is deterministic:
 * it consumes a complete scene, fetches nothing, and mutates no state
 * outside the drawing target.
 *
 * The chart owns its own palette — white paper with black and grey ink —
 * independent of the application theme.
 */
public final class ChartRenderer {

    private static final Color PAPER = Color.WHITE;
    private static final Color INK = Color.BLACK;
    private static final Color FRAME = new Color(51, 51, 51);

    private final StarSizePolicy starSizePolicy;

    public ChartRenderer(StarSizePolicy starSizePolicy) {
        if (starSizePolicy == null) {
            throw new IllegalArgumentException("star size policy must not be null");
        }
        this.starSizePolicy = starSizePolicy;
    }

    /** Renders the scene onto the graphics target. */
    public void render(Graphics2D g, ChartScene scene) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        g.setColor(PAPER);
        g.fillRect(0, 0, width, height);

        GnomonicProjection projection = new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());

        g.setClip(1, 1, width - 2, height - 2);
        g.setColor(INK);
        for (Star star : scene.stars()) {
            projection.project(star.position()).ifPresent(plane -> {
                PixelPoint pixel = mapping.toPixel(plane);
                double radius = starSizePolicy.radiusFor(star.magnitude());
                g.fill(new Ellipse2D.Double(
                        pixel.x() - radius, pixel.y() - radius,
                        2.0 * radius, 2.0 * radius));
            });
        }
        g.setClip(null);

        g.setColor(FRAME);
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new Rectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0));
    }

    /** Renders the scene into a fresh raster image, for export and tests. */
    public BufferedImage renderToImage(ChartScene scene) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            render(g, scene);
        } finally {
            g.dispose();
        }
        return image;
    }
}
