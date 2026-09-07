package juranometria.tool.overview;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

/**
 * Draws a real scene under any candidate projection (issue #296).
 *
 * <p>This is a study painter, and it exists because
 * <strong>production's renderer cannot draw these pages at all</strong>.
 * {@code ChartRenderer} constructs a {@code GnomonicProjection} and a
 * {@code ViewportMapping} inside itself, twenty-five times across
 * seven files, and neither can be asked for anything else. That is
 * not a criticism of the renderer - one projection was the right
 * answer for four sprints - it is the measurement that says what
 * issue #297 has to open.
 *
 * <p>What is production's here, unchanged: the scene, the catalogue,
 * the constellation geography and its detail policy, the star size
 * policy, the palettes, the 0.5-degree geography sampling step, and
 * the rule that a constellation is named at the centroid of its
 * visible ink. What is the study's own: the projection, the mapping,
 * the curve vocabulary, and the arrangement of these into a page.
 *
 * <p>Deep-sky objects are selected by production's own families
 * and detail policy, but drawn as a plain ring rather than as the
 * renderer's family symbols: which objects a page carries is a
 * question about the projection, and what a galaxy looks like is
 * not.
 *
 * <p>What is deliberately absent: <strong>star labels</strong>. The
 * label collision policy is production's and this gate changes
 * nothing about it - a name is placed beside a mark on the page, and
 * a page is a page whatever put the marks there. The one thing that
 * does change is how many marks compete, and that is counted rather
 * than drawn.
 */
final class StudyPage {

    /** Production's own sampling step for a figure or boundary. */
    static final double GEOGRAPHY_STEP_DEGREES = 0.5;

    private static final BasicStroke BOUNDARY = new BasicStroke(0.6f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {1.0f, 3.0f}, 0.0f);
    private static final BasicStroke FIGURE = new BasicStroke(1.0f);
    private static final BasicStroke GRID = new BasicStroke(0.5f);
    private static final BasicStroke REFERENCE = new BasicStroke(1.0f);
    private static final BasicStroke DASH_DOT = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {12.0f, 4.0f, 2.0f, 4.0f}, 0.0f);
    private static final Font NAME_FONT =
            new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font TITLE_FONT =
            new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final StudyProjection projection;
    private final StudyMapping mapping;
    private final ChartScene scene;
    private final ChartPalette palette;
    private final Rectangle2D page;

    /** What the page turned out to need, for the report. */
    private final List<String> notes = new ArrayList<>();
    private int starsDrawn;
    private int starsHeld;

    StudyPage(StudyProjection projection, ChartScene scene,
              ChartPalette palette) {
        this.projection = projection;
        this.scene = scene;
        this.palette = palette;
        this.page = new Rectangle2D.Double(0, 0,
                scene.viewport().widthPx(), scene.viewport().heightPx());
        this.mapping = new StudyMapping(projection,
                scene.viewport().fieldWidthDegrees(),
                scene.viewport().widthPx(), scene.viewport().heightPx());
    }

    List<String> notes() {
        return notes;
    }

    int starsDrawn() {
        return starsDrawn;
    }

    int starsHeld() {
        return starsHeld;
    }

    StudyMapping mapping() {
        return mapping;
    }

    /**
     * The chart's stack, in production's order: ground, grid,
     * geography, the modules' reference ink, then the sky's own
     * marks, then furniture. Reference ink sits above the grid and
     * below the catalogue because it is somebody's line, not
     * somebody's star.
     */
    void paint(Graphics2D g, List<PageCurveReport.Circle> reference) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(palette.ground());
        g.fill(page);

        Graphics2D chart = (Graphics2D) g.create();
        try {
            chart.clip(page);
            graticule(chart);
            geography(chart);
            reference(chart, reference);
            sky(chart);
        } finally {
            chart.dispose();
        }
        furniture(g);
    }

    /**
     * The grid, sampled - as production samples it.
     *
     * <p>A meridian is a great circle and a parallel is not, so no
     * one vocabulary of exact forms covers a graticule under every
     * projection. Production already answers this by sampling both
     * at {@code field/180} and recording its worst chord error, and
     * this gate has no reason to change that answer: it re-measures
     * it at overview fields instead.
     */
    private void graticule(Graphics2D g) {
        g.setColor(palette.gridInk());
        g.setStroke(GRID);
        double field = scene.viewport().fieldWidthDegrees();
        double step = field >= 90.0 ? 30.0 : field >= 45.0 ? 15.0 : 10.0;
        double sample = field / 180.0;

        for (double ra = 0.0; ra < 360.0; ra += step) {
            List<Point2D> run = new ArrayList<>();
            for (double dec = -90.0; dec <= 90.0; dec += sample) {
                add(run, new SkyPosition(ra, dec), g);
            }
            stroke(g, run);
        }
        for (double dec = -60.0; dec <= 60.0; dec += step) {
            List<Point2D> run = new ArrayList<>();
            for (double ra = 0.0; ra <= 360.0; ra += sample) {
                add(run, new SkyPosition(ra % 360.0, dec), g);
            }
            stroke(g, run);
        }
    }

    private void add(List<Point2D> run, SkyPosition position, Graphics2D g) {
        Optional<Point2D> at = mapping.pageOf(position);
        if (at.isEmpty()) {
            stroke(g, run);
            run.clear();
        } else {
            run.add(at.get());
        }
    }

    /**
     * Draws a sampled run, breaking it wherever two neighbours land
     * absurdly far apart.
     *
     * <p>Near a projection's edge two positions a sample apart can
     * project to opposite sides of the paper, and joining them draws
     * a line across the page that is not in the sky. Production's
     * gnomonic pages meet this at the 90-degree horizon; every
     * candidate meets it somewhere, so the study breaks the run
     * rather than trusting the projection to have no edge.
     */
    private void stroke(Graphics2D g, List<Point2D> run) {
        double leap = Math.max(page.getWidth(), page.getHeight()) / 4.0;
        for (int at = 1; at < run.size(); at++) {
            Point2D from = run.get(at - 1);
            Point2D to = run.get(at);
            if (from.distance(to) > leap) {
                continue;
            }
            Line2D.Double piece = new Line2D.Double(from, to);
            if (piece.intersects(page)) {
                g.draw(piece);
            }
        }
    }

    private void geography(Graphics2D g) {
        g.setColor(palette.boundaryInk());
        g.setStroke(BOUNDARY);
        for (GeoSegment segment : scene.geography().boundarySegments()) {
            segment(g, segment, null);
        }
        g.setColor(palette.figureInk());
        g.setStroke(FIGURE);
        Map<String, double[]> visibleInk = new LinkedHashMap<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            segment(g, segment, visibleInk);
        }
        names(g, visibleInk);
    }

    /** One figure or boundary, slerped and sampled as production does. */
    private void segment(Graphics2D g, GeoSegment segment,
                         Map<String, double[]> visibleInk) {
        int steps = Math.max(1, (int) Math.ceil(
                StudyScenes.separation(segment.from(), segment.to())
                        / GEOGRAPHY_STEP_DEGREES));
        Point2D previous = null;
        for (int i = 0; i <= steps; i++) {
            Optional<Point2D> at = mapping.pageOf(
                    slerp(segment.from(), segment.to(), (double) i / steps));
            if (at.isEmpty()) {
                previous = null;
                continue;
            }
            Point2D here = at.get();
            if (previous != null
                    && previous.distance(here) < page.getWidth() / 4.0) {
                Line2D.Double piece = new Line2D.Double(previous, here);
                if (piece.intersects(page)) {
                    g.draw(piece);
                    if (visibleInk != null) {
                        double[] sum = visibleInk.computeIfAbsent(
                                segment.constellationId(),
                                key -> new double[3]);
                        sum[0] += (previous.getX() + here.getX()) / 2.0;
                        sum[1] += (previous.getY() + here.getY()) / 2.0;
                        sum[2] += 1.0;
                    }
                }
            }
            previous = here;
        }
    }

    /** Production's rule: at the centroid of the constellation's ink. */
    private void names(Graphics2D g, Map<String, double[]> visibleInk) {
        g.setFont(NAME_FONT);
        g.setColor(palette.constellationNameInk());
        for (Map.Entry<String, String> name
                : scene.geography().latinNames().entrySet()) {
            double[] sum = visibleInk.get(name.getKey());
            if (sum == null) {
                continue;
            }
            String text = name.getValue().toUpperCase(Locale.ROOT);
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (float) (sum[0] / sum[2] - width / 2.0),
                    (float) (sum[1] / sum[2]));
        }
    }

    /** The modules' great circles, in the vocabulary being proposed. */
    private void reference(Graphics2D g,
                           List<PageCurveReport.Circle> circles) {
        g.setColor(palette.figureInk());
        for (PageCurveReport.Circle circle : circles) {
            Optional<PageCurves.Built> built =
                    PageCurves.greatCircle(mapping, circle.pole(), 720);
            if (built.isEmpty()) {
                continue;
            }
            List<PageCurve.Run> runs = built.get().curve().clipTo(page);
            if (runs.isEmpty()) {
                continue;  // off the page is silence
            }
            g.setStroke(circle.name().equals("the ecliptic")
                    ? DASH_DOT : REFERENCE);
            for (PageCurve.Run run : runs) {
                g.draw(run.shape());
            }
            notes.add(String.format(Locale.ROOT,
                    "%s: %s, %d run%s, %s", circle.name(),
                    built.get().curve().form(), runs.size(),
                    runs.size() == 1 ? "" : "s",
                    runs.stream().anyMatch(PageCurve.Run::closed)
                            ? "closed - no end to hang a name on"
                            : "named at the upper end"));
            label(g, runs, circle.name());
        }
    }

    /** Where reference names have already been written on this page. */
    private final List<Rectangle2D> placed = new ArrayList<>();

    /** Production's label rule, applied to whichever run offers an end. */
    private void label(Graphics2D g, List<PageCurve.Run> runs, String name) {
        Point2D best = null;
        for (PageCurve.Run run : runs) {
            if (run.closed()) {
                continue;
            }
            for (Point2D end : new Point2D[] {run.from(), run.to()}) {
                if (best == null || end.getY() < best.getY()
                        || (end.getY() == best.getY()
                                && end.getX() > best.getX())) {
                    best = end;
                }
            }
        }
        if (best == null) {
            return;
        }
        g.setFont(NAME_FONT);
        g.setColor(palette.gridLabelInk());
        double width = g.getFontMetrics().stringWidth(name);
        double height = g.getFontMetrics().getHeight();
        double x = Math.min(Math.max(best.getX() + 4.0, 4.0),
                page.getWidth() - width - 4.0);
        double y = Math.min(Math.max(best.getY() + 12.0, 14.0),
                page.getHeight() - 4.0);
        // Four reference lines can leave the page at nearly the same
        // corner, and two names written there are one unreadable
        // name. Production has never had to solve this - at 42
        // degrees no page carries more than two - so the study
        // stacks them and records that an overview will have to.
        Rectangle2D box = new Rectangle2D.Double(x, y - height, width,
                height);
        while (placed.stream().anyMatch(box::intersects)
                && box.getMaxY() + height < page.getHeight()) {
            box = new Rectangle2D.Double(box.getX(), box.getY() + height,
                    width, height);
        }
        placed.add(box);
        g.drawString(name, (float) box.getX(), (float) box.getMaxY());
    }

    /** The catalogue's own marks, at production's sizes. */
    private void sky(Graphics2D g) {
        g.setColor(palette.deepSkyOutline());
        g.setStroke(FIGURE);
        // Production's own selection: which families the reader asked
        // for, and which objects are big enough at this scale to be
        // worth a symbol. Drawing every catalogue row instead put
        // several hundred identical rings on a 90-degree page and
        // made every projection look equally crowded, which is a
        // page nobody's atlas would ever show.
        RegionalDetailPolicy detail = new RegionalDetailPolicy(scene,
                mapping.pageUnitsPerPlaneUnit());
        for (DeepSkyObject object : scene.deepSkyObjects()) {
            if (!ChartRenderer.permitted(scene, object, ChartOptions.DEFAULTS)
                    || !detail.drawn(object)) {
                continue;
            }
            mapping.pageOf(object.position()).ifPresent(at -> {
                if (page.contains(at)) {
                    g.draw(new Ellipse2D.Double(at.getX() - 3.0,
                            at.getY() - 3.0, 6.0, 6.0));
                }
            });
        }
        g.setColor(palette.starInk());
        for (Star star : scene.stars()) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            Optional<Point2D> at = mapping.pageOf(star.position());
            if (at.isEmpty()) {
                starsHeld++;
                continue;
            }
            double radius = StarSizePolicy.DEFAULT.radiusFor(star.magnitude());
            Ellipse2D.Double dot = new Ellipse2D.Double(
                    at.get().getX() - radius, at.get().getY() - radius,
                    2.0 * radius, 2.0 * radius);
            if (dot.intersects(page)) {
                g.fill(dot);
                starsDrawn++;
            } else {
                starsHeld++;
            }
        }
    }

    /** The frame, and one line saying what this page is. */
    private void furniture(Graphics2D g) {
        g.setColor(palette.frameInk());
        g.setStroke(FIGURE);
        g.draw(new Rectangle2D.Double(0.5, 0.5, page.getWidth() - 1.0,
                page.getHeight() - 1.0));
        g.setFont(TITLE_FONT);
        g.setColor(palette.textInk());
        g.drawString(String.format(Locale.ROOT,
                        "%s   %s   %.0f° field   %d stars",
                        scene.title(), projection.name(),
                        scene.viewport().fieldWidthDegrees(), starsDrawn),
                6.0f, (float) page.getHeight() - 6.0f);
    }

    private static SkyPosition slerp(SkyPosition from, SkyPosition to,
                                     double t) {
        double[] a = unit(from);
        double[] b = unit(to);
        double omega = Math.acos(Math.clamp(
                a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1.0, 1.0));
        double sa;
        double sb;
        if (omega < 1e-9) {
            sa = 1.0 - t;
            sb = t;
        } else {
            sa = Math.sin((1.0 - t) * omega) / Math.sin(omega);
            sb = Math.sin(t * omega) / Math.sin(omega);
        }
        double x = sa * a[0] + sb * b[0];
        double y = sa * a[1] + sb * b[1];
        double z = sa * a[2] + sb * b[2];
        return new SkyPosition(
                (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0,
                Math.toDegrees(Math.asin(Math.clamp(
                        z / Math.sqrt(x * x + y * y + z * z), -1.0, 1.0))));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }
}
