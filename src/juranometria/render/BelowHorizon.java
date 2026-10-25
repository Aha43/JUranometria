package juranometria.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;

/**
 * Subdue below horizon: the one rule, for one page, from one chart
 * context (Sprint 55, issue #615; rulings in
 * {@code docs/decisions/below-horizon.md}).
 *
 * <p>Built once per paint or export from the zenith of the chart's
 * observer in the chart's J2000 frame - the pole of the great circle
 * the meridian module draws as the mathematical horizon - so every
 * layer agrees to the pixel about where the horizon is. A position is
 * below it when it lies more than 90° from the zenith, the same answer
 * {@code LocalSky.altitudeDegrees} gives. {@link #NONE} when Subdue
 * below horizon is off (or there is no observer): every question then
 * answers "not below" and every drawing runs as it always has.
 *
 * <p>The treatment keeps {@link #KEPT} of an ink's distance from the
 * ground (ruling P-C), applied to each ink as an opaque colour - never
 * as opacity, which would compound where two subdued inks overlap and
 * would rasterise a vector export. Marks and labels take their colour
 * through {@link #ink} or {@link #toneFor}; curves are drawn through
 * {@link #split}, which hands the drawing a colour transform per side.
 * Placement, refusal,
 * occlusion and hit-testing never ask this class anything: it is
 * consulted at colour time only.
 */
public final class BelowHorizon {

    /** The share of an ink's distance from the ground kept below the horizon (ruling P-C). */
    public static final double KEPT = 0.40;

    /** Subdue below horizon off: nothing is below. */
    public static final BelowHorizon NONE = new BelowHorizon(null, null);

    /** Chords per curved run of the horizon when its crossings with a row are found. */
    private static final int CHORDS = 512;

    private final double[] zenith;
    private final SkyPosition zenithPosition;
    private final ChartScene scene;
    /** The drawn horizon on this page, as chords x0, y0, x1, y1. */
    private List<double[]> chords;
    private DrawnPage page;
    private Shape below;
    private Shape above;

    private BelowHorizon(SkyPosition zenithPosition, ChartScene scene) {
        this.zenithPosition = zenithPosition;
        this.zenith = zenithPosition == null ? null : unit(zenithPosition);
        this.scene = scene;
    }

    /**
     * The rule for this page, or {@link #NONE} without a zenith.
     *
     * @param zenithJ2000 the observer's zenith in the chart's frame, or
     *     null when Subdue below horizon is off
     */
    public static BelowHorizon of(SkyPosition zenithJ2000, ChartScene scene) {
        if (zenithJ2000 == null || scene == null) {
            return NONE;
        }
        return new BelowHorizon(zenithJ2000, scene);
    }

    /** Whether the treatment applies on this page at all. */
    public boolean active() {
        return zenith != null;
    }

    /** Whether a sky position lies below the mathematical horizon. */
    public boolean below(SkyPosition position) {
        if (zenith == null || position == null) {
            return false;
        }
        double[] u = unit(position);
        return u[0] * zenith[0] + u[1] * zenith[1] + u[2] * zenith[2] < 0.0;
    }

    /** Whether the sky at a page pixel lies below the horizon; false where the page has no sky. */
    public boolean belowAt(double x, double y) {
        if (zenith == null) {
            return false;
        }
        if (page == null) {
            page = DrawnPage.of(scene);
        }
        return below(ChartHitTest.skyAt(page, x, y));
    }

    /** An opaque ink subdued: moved towards the ground, keeping {@link #KEPT} of its distance. */
    public static Color subdued(Color ink, Color ground) {
        return new Color(
                towards(ground.getRed(), ink.getRed()),
                towards(ground.getGreen(), ink.getGreen()),
                towards(ground.getBlue(), ink.getBlue()),
                ink.getAlpha());
    }

    private static int towards(int ground, int ink) {
        return (int) Math.round(ground + (ink - ground) * KEPT);
    }

    /** An ink as drawn for something that belongs above or below the horizon. */
    public Color ink(Color ink, Color ground, boolean belongsBelow) {
        return zenith != null && belongsBelow ? subdued(ink, ground) : ink;
    }

    /**
     * The colour transform for one thing drawn whole - a mark, a label -
     * by where it belongs (rulings P-L and the marks' rule): the
     * identity above the horizon, {@link #subdued} below it.
     */
    public UnaryOperator<Color> toneFor(boolean belongsBelow, Color ground) {
        return zenith != null && belongsBelow ? ink -> subdued(ink, ground)
                : UnaryOperator.identity();
    }

    /**
     * Draws a curve split at the horizon plane (ruling P-X): run twice,
     * once clipped to the sky above with its inks as they are, once
     * clipped to the sky below with every ink it sets subdued. The
     * drawing sets its colours through the transform it is handed, so
     * crossing inks overwrite each other as they always have and
     * nothing compounds; vector output stays vector.
     */
    public void split(Graphics2D g, Color ground, Consumer<UnaryOperator<Color>> draw) {
        if (zenith == null) {
            draw.accept(UnaryOperator.identity());
            return;
        }
        regions();
        Shape clip = g.getClip();
        try {
            g.clip(above);
            draw.accept(UnaryOperator.identity());
            g.setClip(clip);
            g.clip(below);
            draw.accept(ink -> subdued(ink, ground));
        } finally {
            g.setClip(clip);
        }
    }

    /** The page's sky below the horizon, as page pixels. */
    public Shape belowRegion() {
        regions();
        return below;
    }

    private void regions() {
        if (below != null) {
            return;
        }
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        double from = -2.0;
        double to = width + 2.0;
        Path2D.Double belowPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
        Path2D.Double abovePath = new Path2D.Double(Path2D.WIND_NON_ZERO);
        double[] runStart = null;
        int runFrom = -2;
        // One row of spans at a time; consecutive rows with the same
        // spans are one rectangle, so a page whose horizon is a line
        // across it costs a few hundred rectangles, not a mask.
        for (int y = -2; y <= height + 2; y++) {
            double[] spans = y < height + 2 ? spansOf(y + 0.5, from, to) : null;
            if (runStart != null && (spans == null || !Arrays.equals(spans, runStart))) {
                emit(belowPath, abovePath, runStart, runFrom, y, from, to);
                runStart = null;
            }
            if (spans != null && runStart == null) {
                runStart = spans;
                runFrom = y;
            }
        }
        below = belowPath;
        above = abovePath;
    }

    private static void emit(Path2D.Double belowPath, Path2D.Double abovePath,
                             double[] spans, int top, int bottom,
                             double from, double to) {
        double h = bottom - top;
        double x = from;
        for (int i = 0; i < spans.length; i += 2) {
            if (spans[i] > x) {
                abovePath.append(new java.awt.geom.Rectangle2D.Double(
                        x, top, spans[i] - x, h), false);
            }
            belowPath.append(new java.awt.geom.Rectangle2D.Double(
                    spans[i], top, spans[i + 1] - spans[i], h), false);
            x = spans[i + 1];
        }
        if (to > x) {
            abovePath.append(new java.awt.geom.Rectangle2D.Double(x, top, to - x, h), false);
        }
    }

    /**
     * The row's below-horizon spans, as [from, to) pairs in increasing x.
     * Where the row crosses the drawn horizon is found on the horizon's
     * own curve - the runs the reference layer draws, from
     * {@code GreatCirclePage.clip} - so the boundary is the line the
     * reader sees; which side each stretch between crossings lies on is
     * asked of the sky once per stretch.
     */
    private double[] spansOf(double y, double from, double to) {
        if (chords == null) {
            chords = horizonChords();
        }
        List<Double> crossings = new ArrayList<>();
        for (double[] c : chords) {
            if ((c[1] <= y && y < c[3]) || (c[3] <= y && y < c[1])) {
                double x = c[0] + (y - c[1]) * (c[2] - c[0]) / (c[3] - c[1]);
                if (x > from && x < to) {
                    crossings.add(x);
                }
            }
        }
        crossings.sort(null);
        List<Double> edges = new ArrayList<>();
        double start = from;
        boolean open = false;
        for (int i = 0; i <= crossings.size(); i++) {
            double end = i < crossings.size() ? crossings.get(i) : to;
            if (end <= start) {
                continue;
            }
            boolean underneath = sideOf(start, end, y);
            if (underneath && !open) {
                edges.add(start);
                open = true;
            } else if (!underneath && open) {
                edges.add(start);
                open = false;
            }
            start = end;
        }
        if (open) {
            edges.add(to);
        }
        double[] spans = new double[edges.size()];
        for (int i = 0; i < spans.length; i++) {
            spans[i] = edges.get(i);
        }
        return spans;
    }

    /** Whether a stretch of a row lies below the horizon: the first point of it that is sky decides. */
    private boolean sideOf(double from, double to, double y) {
        if (page == null) {
            page = DrawnPage.of(scene);
        }
        for (double f : new double[] {0.5, 0.25, 0.75, 0.125, 0.375, 0.625, 0.875}) {
            SkyPosition at = ChartHitTest.skyAt(page, from + (to - from) * f, y);
            if (at != null) {
                return below(at);
            }
        }
        return false;
    }

    private List<double[]> horizonChords() {
        if (page == null) {
            page = DrawnPage.of(scene);
        }
        juranometria.project.ViewportMapping mapping =
                new juranometria.project.ViewportMapping(page);
        juranometria.project.PageRegion region =
                mapping.regionFor(scene.viewport(), page.projection());
        List<double[]> out = new ArrayList<>();
        for (juranometria.project.CurveRun run : juranometria.project.GreatCirclePage.clip(
                page.projection(), mapping, region, zenithPosition)) {
            int n = run instanceof juranometria.project.CurveRun.Segment ? 1 : CHORDS;
            juranometria.project.PixelPoint was = run.at(0.0);
            for (int i = 1; i <= n; i++) {
                juranometria.project.PixelPoint next = run.at((double) i / n);
                out.add(new double[] {was.x(), was.y(), next.x(), next.y()});
                was = next;
            }
        }
        return out;
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra),
                Math.sin(dec)};
    }
}
