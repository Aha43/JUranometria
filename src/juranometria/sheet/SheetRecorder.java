package juranometria.sheet;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link Graphics2D} that records what the cartography draws
 * instead of rasterising it (Sprint 29, issues #283 and #285).
 *
 * <p>Written for the gate that asked whether the atlas's renderer
 * could serve paper; production since #285, which made SVG a real
 * writer against it. It is the one place a chart becomes something
 * other than pixels, and it is deliberately not in a celestial
 * package: a module contributes geometry and never learns paper.
 *
 * <p>The gate's architectural question is whether the atlas's one
 * Java2D renderer can serve paper honestly, or whether a second,
 * target-neutral description of a chart sheet is needed - with the
 * standing rule that the cartography must not be forked into a
 * second renderer.
 *
 * <p>A scan of the compiled painters answers the first half: the
 * renderer, the graticule, the reference ink and the working-mark
 * ink between them use <strong>seventeen</strong> {@code Graphics2D}
 * methods, and every one of them is vector - shapes, strokes,
 * colours, text, clips and transforms. Nothing draws an image,
 * composites, or asks for a gradient.
 *
 * <p>So this records those seventeen and refuses the rest. Every
 * method the atlas does not use throws, which is the point: if the
 * cartography ever reaches for something a page cannot carry, an
 * export fails loudly here rather than quietly falling back to a
 * raster.
 *
 * <p>Shapes are recorded in <em>device</em> space, with the current
 * transform already applied, so a writer needs no transform model of
 * its own.
 */
public final class SheetRecorder extends Graphics2D {

    /**
     * One thing the cartography did, in the order it did it.
     *
     * <p>Shapes and text used to be kept in two lists, and every
     * writer emitted all of one and then all of the other. That is
     * not what the renderer did: it draws a constellation name,
     * fills the title panel over it, and writes the title on top.
     * Replaying shapes-then-text put the constellation name back
     * above the panel, and "CANIS MAJOR" read straight through the
     * title box of every exported Orion sheet - found on paper, by
     * the physical inspection #287 owed (PR #292).
     *
     * <p>So there is one sequence now, and a writer that walks it in
     * order cannot make that mistake again.
     */
    public sealed interface Operation permits Drawn, Text {

        /** The clip in force when this happened, or null. */
        Shape clip();
    }

    /** One stroked or filled shape, as it lands on the sheet. */
    public record Drawn(Shape shape, boolean filled, Color colour,
                        BasicStrokeSpec stroke, Shape clip)
            implements Operation {
    }

    /** One run of text, at its device position. */
    public record Text(String text, double x, double y, Font font,
                       Color colour, Shape clip) implements Operation {
    }

    /**
     * A stroke, whole.
     *
     * <p>Width and dash alone are not the stroke. A cap decides
     * whether a one-point line ends square or flush, a join decides
     * what a corner looks like, and a sheet that keeps neither draws
     * a subtly different chart from the one on screen - and a raster
     * replay of it draws a third (issue #286). So all of it is
     * carried, and each writer says as much of it as its format can.
     */
    public record BasicStrokeSpec(float width, int cap, int join,
                                  float miterLimit, float[] dash,
                                  float dashPhase) {
    }

    /** Everything the render did, in the order it did it. */
    private final List<Operation> operations;
    private final BufferedImage metricsSource;

    private AffineTransform transform = new AffineTransform();
    private Color colour = Color.BLACK;
    private Stroke stroke = new java.awt.BasicStroke(1.0f);
    private Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private Shape clip;

    public SheetRecorder(int widthPx, int heightPx) {
        this(new ArrayList<>(),
                new BufferedImage(Math.max(1, widthPx),
                        Math.max(1, heightPx), BufferedImage.TYPE_INT_RGB));
    }

    private SheetRecorder(List<Operation> operations,
                               BufferedImage metricsSource) {
        this.operations = operations;
        this.metricsSource = metricsSource;
    }

    /**
     * Everything the render did, in the order it did it. This is
     * what a writer replays; the two views below are for asking
     * questions about one kind of operation, never for emitting.
     */
    public List<Operation> operations() {
        return operations;
    }

    /** Everything stroked or filled, in the order it was drawn. */
    public List<Drawn> drawn() {
        return operations.stream()
                .filter(each -> each instanceof Drawn)
                .map(each -> (Drawn) each).toList();
    }

    /** Every run of text, in the order it was drawn. */
    public List<Text> text() {
        return operations.stream()
                .filter(each -> each instanceof Text)
                .map(each -> (Text) each).toList();
    }

    // ---- the seventeen the cartography uses --------------------------

    @Override
    public Graphics create() {
        SheetRecorder copy =
                new SheetRecorder(operations, metricsSource);
        copy.transform = new AffineTransform(transform);
        copy.colour = colour;
        copy.stroke = stroke;
        copy.font = font;
        copy.clip = clip;
        return copy;
    }

    @Override
    public void dispose() {
        // A recorder owns no device resource; the lists outlive it.
    }

    @Override
    public void draw(Shape shape) {
        operations.add(new Drawn(transform.createTransformedShape(shape), false,
                colour, strokeSpec(), clip));
    }

    @Override
    public void fill(Shape shape) {
        operations.add(new Drawn(transform.createTransformedShape(shape), true,
                colour, strokeSpec(), clip));
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        draw(new Rectangle(x, y, width, height));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        fill(new Rectangle(x, y, width, height));
    }

    @Override
    public void drawString(String string, int x, int y) {
        drawString(string, (float) x, (float) y);
    }

    @Override
    public void drawString(String string, float x, float y) {
        java.awt.geom.Point2D at = transform.transform(
                new java.awt.geom.Point2D.Float(x, y), null);
        operations.add(new Text(string, at.getX(), at.getY(), font,
                colour, clip));
    }

    @Override
    public FontMetrics getFontMetrics(Font which) {
        // Real metrics, from a real device: the sheet's line breaking
        // and collision policy must be the page's, not an estimate.
        return metricsSource.createGraphics().getFontMetrics(which);
    }

    @Override
    public void setColor(Color next) {
        this.colour = next;
    }

    @Override
    public void setFont(Font next) {
        this.font = next;
    }

    @Override
    public void setStroke(Stroke next) {
        this.stroke = next;
    }

    @Override
    public void setClip(Shape next) {
        this.clip = next == null ? null
                : transform.createTransformedShape(next);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void clip(Shape next) {
        Shape device = transform.createTransformedShape(next);
        if (clip == null) {
            clip = device;
        } else {
            Area area = new Area(clip);
            area.intersect(new Area(device));
            clip = area;
        }
    }

    @Override
    public void translate(int x, int y) {
        transform.translate(x, y);
    }

    @Override
    public void translate(double x, double y) {
        transform.translate(x, y);
    }

    @Override
    public void rotate(double radians) {
        transform.rotate(radians);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object value) {
        // A sheet is resolution-independent; antialiasing and stroke
        // control are the raster's business and are recorded nowhere.
    }

    // ---- what a sheet genuinely needs to answer ----------------------

    @Override
    public Color getColor() {
        return colour;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public Shape getClip() {
        return clip;
    }

    @Override
    public Rectangle getClipBounds() {
        return clip == null ? null : clip.getBounds();
    }

    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    @Override
    public void setTransform(AffineTransform next) {
        this.transform = new AffineTransform(next);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return metricsSource.createGraphics().getFontRenderContext();
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key key) {
        return null;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return new RenderingHints(Map.of());
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
    }

    private BasicStrokeSpec strokeSpec() {
        if (stroke instanceof java.awt.BasicStroke basic) {
            return new BasicStrokeSpec(basic.getLineWidth(),
                    basic.getEndCap(), basic.getLineJoin(),
                    basic.getMiterLimit(), basic.getDashArray(),
                    basic.getDashPhase());
        }
        throw new UnsupportedOperationException(
                "a chart sheet carries basic strokes: " + stroke);
    }

    // ---- everything the cartography does not use ---------------------
    //
    // Each of these throws rather than silently doing nothing. An
    // export that quietly dropped ink, or fell back to a raster,
    // would be the failure this whole gate exists to prevent.

    private static UnsupportedOperationException refuse(String what) {
        return new UnsupportedOperationException(
                "a chart sheet does not carry " + what
                        + "; the cartography has reached beyond what a"
                        + " vector page can hold, and an export must"
                        + " fail rather than rasterise");
    }

    @Override public void setPaintMode() { throw refuse("paint modes"); }
    @Override public void setXORMode(Color c) { throw refuse("XOR"); }
    @Override public void clipRect(int x, int y, int w, int h) {
        clip(new Rectangle(x, y, w, h));
    }
    @Override public void copyArea(int x, int y, int w, int h, int dx,
                                   int dy) { throw refuse("copyArea"); }
    @Override public void drawLine(int x1, int y1, int x2, int y2) {
        draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
    }
    @Override public void clearRect(int x, int y, int w, int h) {
        throw refuse("clearRect");
    }
    @Override public void drawRoundRect(int x, int y, int w, int h, int aw,
                                        int ah) {
        draw(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, aw, ah));
    }
    @Override public void fillRoundRect(int x, int y, int w, int h, int aw,
                                        int ah) {
        fill(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, aw, ah));
    }
    @Override public void drawOval(int x, int y, int w, int h) {
        draw(new java.awt.geom.Ellipse2D.Float(x, y, w, h));
    }
    @Override public void fillOval(int x, int y, int w, int h) {
        fill(new java.awt.geom.Ellipse2D.Float(x, y, w, h));
    }
    @Override public void drawArc(int x, int y, int w, int h, int s,
                                  int e) { throw refuse("arcs"); }
    @Override public void fillArc(int x, int y, int w, int h, int s,
                                  int e) { throw refuse("arcs"); }
    @Override public void drawPolyline(int[] xs, int[] ys, int n) {
        throw refuse("polylines");
    }
    @Override public void drawPolygon(int[] xs, int[] ys, int n) {
        throw refuse("polygons");
    }
    @Override public void fillPolygon(int[] xs, int[] ys, int n) {
        throw refuse("polygons");
    }
    @Override public void drawString(AttributedCharacterIterator i, int x,
                                     int y) { throw refuse("attributed text"); }
    @Override public void drawString(AttributedCharacterIterator i, float x,
                                     float y) { throw refuse("attributed text"); }
    @Override public boolean drawImage(Image i, int x, int y,
                                       ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, int x, int y, int w, int h,
                                       ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, int x, int y, Color b,
                                       ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, int x, int y, int w, int h,
                                       Color b, ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, int dx1, int dy1, int dx2,
                                       int dy2, int sx1, int sy1, int sx2,
                                       int sy2, ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, int dx1, int dy1, int dx2,
                                       int dy2, int sx1, int sy1, int sx2,
                                       int sy2, Color b, ImageObserver o) {
        throw refuse("images");
    }
    @Override public boolean drawImage(Image i, AffineTransform t,
                                       ImageObserver o) {
        throw refuse("images");
    }
    @Override public void drawImage(BufferedImage i, BufferedImageOp op,
                                    int x, int y) { throw refuse("images"); }
    @Override public void drawRenderedImage(RenderedImage i,
                                            AffineTransform t) {
        throw refuse("images");
    }
    @Override public void drawRenderableImage(RenderableImage i,
                                              AffineTransform t) {
        throw refuse("images");
    }
    @Override public void drawGlyphVector(GlyphVector v, float x, float y) {
        throw refuse("glyph vectors");
    }
    @Override public boolean hit(Rectangle r, Shape s, boolean onStroke) {
        throw refuse("hit testing");
    }
    @Override public GraphicsConfiguration getDeviceConfiguration() {
        throw refuse("a device configuration");
    }
    @Override public void setComposite(Composite c) {
        throw refuse("compositing");
    }
    @Override public void setPaint(Paint p) {
        if (p instanceof Color asColour) {
            this.colour = asColour;
            return;
        }
        throw refuse("paints other than colours");
    }
    @Override public Paint getPaint() { return colour; }
    @Override public Composite getComposite() {
        throw refuse("compositing");
    }
    @Override public void setBackground(Color c) {
        throw refuse("a background");
    }
    @Override public Color getBackground() {
        throw refuse("a background");
    }
    @Override public void rotate(double radians, double x, double y) {
        transform.rotate(radians, x, y);
    }
    @Override public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }
    @Override public void shear(double sx, double sy) {
        throw refuse("shear");
    }
    @Override public void transform(AffineTransform t) {
        transform.concatenate(t);
    }
    @Override public FontMetrics getFontMetrics() {
        return getFontMetrics(font);
    }
}
