package juranometria.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.ui.SceneAssembler;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

/**
 * What is actually on a page (Sprint 24, issue #214).
 *
 * <p>The gate's evidence. The chart has always known more than it
 * draws - a page carries objects whose family is switched off, whose
 * magnitude is past the limit, whose symbol the atlas does not have,
 * and until now the only way to ask was to point at ink that was not
 * there. Before deciding what a table should say, this measures what
 * a table would have to hold.
 *
 * <p>Everything comes from production: the same projection and
 * viewport mapping the renderer places marks with, the same
 * {@code permitted} rule the family switches obey, the same detail
 * policy, and the scene the application's own assembler builds.
 */
public final class OnThisPageStudyMain {

    private OnThisPageStudyMain() {
    }

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** The pages the gate asks for. */
    private record Page(String name, double ra, double dec, String why) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("m31", 10.684708, 41.268750, "the released default"),
            new Page("orion", 83.8, 0.0, "bright, familiar, equatorial"),
            new Page("virgo", 187.706, 12.391, "the densest galaxies"),
            new Page("lmc", 80.894, -69.756, "the Large Magellanic Cloud"),
            new Page("ra-zero", 0.0, 20.0, "the seam"),
            new Page("polar", 0.0, 85.0, "near the pole"));

    private static final double[] FIELDS = {1.0, 8.0, 18.0, 36.0};

    /**
     * Why an object is or is not visible, decided by production
     * rather than guessed. The order is the order a reader meets
     * them: what they can see, then the three reasons they cannot.
     */
    private enum Visibility {
        DRAWN("drawn"),
        FAMILY_HIDDEN("hidden by a chart option"),
        BELOW_LIMIT("fainter than the magnitude limit"),
        NO_SYMBOL("no chart symbol for its type"),
        TOO_SMALL("below the detail policy at this field");

        private final String prose;

        Visibility(String prose) {
            this.prose = prose;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("# What is on this page");
        System.out.println();
        System.out.println("Measured by `make on-this-page-study`."
                + " Every position, projection and visibility answer"
                + " comes from production: the projection and viewport"
                + " mapping the renderer places marks with, the"
                + " `permitted` rule the family switches obey, the"
                + " detail policy, and the scene the application's own"
                + " assembler builds.");
        System.out.println();

        definition();
        sizes();
        extentRule();
        visibilityBreakdown();
        theStarQuestion();
        ordering();
        keyboard();
        cost();
    }

    // ------------------------------------------------------------------

    private static void definition() {
        System.out.println("## What \"on this page\" means");
        System.out.println();
        System.out.println("**An object is on the page when its"
                + " recorded position projects onto the paper.**"
                + " Nothing else: not whether its symbol is drawn, its"
                + " family switched on, its magnitude inside the"
                + " limit, or the atlas has a symbol for its type at"
                + " all.");
        System.out.println();
        System.out.println("The paper is the viewport the renderer"
                + " clips to - `1, 1, width-2, height-2` - which is"
                + " the same rectangle `drawnMarks` tests against, so"
                + " a table and the drawing cannot come to disagree"
                + " about where the page ends. Letterbox chrome is"
                + " not paper: it is not in the viewport at all.");
        System.out.println();
        System.out.println("An object behind the projection's horizon"
                + " has no place on the page and is not on it. A"
                + " gnomonic page shows less than a hemisphere, and"
                + " the projection says so by refusing the point"
                + " rather than by returning a distant one.");
        System.out.println();
    }

    /** Production's own answer to where a position lands. */
    private static PixelPoint onPage(GnomonicProjection projection,
                                     ViewportMapping mapping,
                                     SkyPosition position) {
        return projection.project(position)
                .map(mapping::toPixel)
                .filter(OnThisPageStudyMain::insidePaper)
                .orElse(null);
    }

    /** The paper, as the renderer clips it. */
    private static final java.awt.geom.Rectangle2D PAPER =
            new java.awt.geom.Rectangle2D.Double(1, 1, WIDTH - 2, HEIGHT - 2);

    /**
     * Whether an object's <strong>recorded ellipse</strong> reaches
     * the paper.
     *
     * <p>A centre-only rule omits an object a reader can plainly
     * see: M 31 is 178 arcminutes long, so a page can be filled by
     * its disc while its centre sits outside the paper.
     *
     * <p>The geometry is the <em>source's</em>, never the display
     * values the loader substitutes so the renderer always has
     * dimensions to draw with. Where the source is silent the
     * fallback is explicit rather than invented, and each one is
     * stated because each one is a different kind of ignorance:
     *
     * <ul>
     *   <li><strong>no recorded size</strong> - the atlas knows of
     *       no extent at all, so the object is a <strong>point</strong>;</li>
     *   <li><strong>a major axis but no minor, or no position
     *       angle</strong> - the catalogue permits a family of
     *       ellipses, and every one of them fits inside the circle
     *       of the recorded half-major, so that circle is used and
     *       the answer is honestly conservative;</li>
     *   <li><strong>the full ellipse</strong> - tested as the
     *       ellipse it is, oriented as recorded, through the same
     *       {@code Shape.intersects} the renderer's own
     *       {@code drawnMarks} uses to decide what is on the
     *       paper.</li>
     * </ul>
     *
     * <p>The envelope was the whole rule until review pointed out
     * that it is not what the decision says: a long thin galaxy
     * lying away from the page can have its circle reach the paper
     * while the ellipse never does.
     */
    private static boolean reachesPaper(GnomonicProjection projection,
                                        ViewportMapping mapping,
                                        ChartScene scene,
                                        DeepSkyObject dso) {
        PixelPoint centre = projection.project(dso.position())
                .map(mapping::toPixel).orElse(null);
        if (centre == null) {
            // Behind the projection's horizon: no place on this page.
            return false;
        }
        if (insidePaper(centre)) {
            return true;
        }
        DeepSkyObject.Recorded recorded = dso.recorded();
        if (!recorded.hasSize()) {
            return false;
        }
        double semiMajorDeg = recorded.majorAxisArcmin() / 120.0;
        if (recorded.minorAxisArcmin() == null
                || recorded.positionAngleDegrees() == null) {
            // Orientation or width unrecorded: the catalogue permits
            // a family of ellipses and every one of them lies inside
            // the circle of the semi-major, so the circle is asked -
            // on the sphere, at every bearing, not as a radius in
            // pixels.
            return sphericalReaches(projection, mapping, scene,
                    dso.position(), semiMajorDeg, semiMajorDeg, 0.0);
        }
        return sphericalReaches(projection, mapping, scene,
                dso.position(), semiMajorDeg,
                recorded.minorAxisArcmin() / 120.0,
                recorded.positionAngleDegrees());
    }

    /**
     * Whether an object's <strong>angular</strong> ellipse reaches
     * the paper: built on the sphere and projected, never scaled in
     * the plane (gate review).
     *
     * <p>The earlier rule turned arcminutes into pixels once, at the
     * page centre's scale, and tested a plane ellipse. A gnomonic
     * page does not have one scale: it stretches away from the
     * centre, and the Large Magellanic Cloud is nearly eleven
     * degrees across. A flat ellipse sized at the centre is the
     * wrong shape by the time it reaches an edge, which is exactly
     * where this question is asked.
     *
     * <p>So the boundary is walked on the sphere - at the recorded
     * semi-axes and position angle, east of north as the catalogue
     * records it - and each point is projected through the atlas's
     * own projection and viewport mapping. If any of it lands on the
     * paper, the object reaches the page.
     */
    /**
     * The rule, for a test to reach: does this angular ellipse reach
     * the page's paper?
     */
    static boolean reachesPaper(ChartScene scene, SkyPosition centre,
                                double semiMajorDeg, double semiMinorDeg,
                                double positionAngleDeg) {
        return sphericalReaches(
                new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), scene, centre,
                semiMajorDeg, semiMinorDeg, positionAngleDeg);
    }

    private static boolean sphericalReaches(GnomonicProjection projection,
                                            ViewportMapping mapping,
                                            ChartScene scene,
                                            SkyPosition centre,
                                            double semiMajorDeg,
                                            double semiMinorDeg,
                                            double positionAngleDeg) {
        // Nowhere near this page, and no walk needed to say so:
        // nothing further from the page centre than the paper's own
        // reach plus the object's own size can touch the paper.
        // This is what keeps the refusal below rare - an object out
        // by the projection's horizon is answered here rather than
        // walked.
        if (centre.separationDegrees(scene.viewport().centre())
                > pageReachDegrees(scene) + semiMajorDeg) {
            return false;
        }
        java.awt.geom.Path2D.Double outline = outlineOf(projection, mapping,
                centre, semiMajorDeg, semiMinorDeg, positionAngleDeg);
        if (outline.getCurrentPoint() == null) {
            return false;          // nothing of it is on this sky
        }
        outline.closePath();
        // Closed, this answers both things a bag of points cannot.
        // A crossing anywhere along an edge counts, not only at a
        // sample. And a rectangle lying inside the path intersects
        // it - which is the object holding the whole page: nothing
        // of its outline is on the paper and its centre is off it,
        // yet every pixel in front of the reader is inside it.
        return outline.intersects(PAPER);
    }

    /**
     * How far this page's own corners reach from its centre, in
     * degrees - asked of the page, at whatever size it was built.
     */
    static double pageReachDegrees(ChartScene scene) {
        SkyPosition centre = scene.viewport().centre();
        double width = scene.viewport().widthPx();
        double height = scene.viewport().heightPx();
        double furthest = 0;
        for (double[] corner : new double[][] {
                {1, 1}, {width - 1, 1},
                {1, height - 1}, {width - 1, height - 1}}) {
            SkyPosition sky = juranometria.render.ChartHitTest.skyAt(
                    scene, corner[0], corner[1]);
            if (sky != null) {
                furthest = Math.max(furthest,
                        centre.separationDegrees(sky));
            }
        }
        return furthest;
    }

    /**
     * The projected boundary, as a path that stays within
     * {@link #FLATNESS_PX} of the true curve.
     *
     * <p>A fixed number of samples has no bound on how far its
     * chords stray from the curve they stand for (gate review). The
     * step is uniform in the parameter, and neither the ellipse nor
     * the projection is: a thin ellipse turns hardest at the ends of
     * its major axis, and a gnomonic projection stretches without
     * limit towards its horizon. Wherever those meet, evenly spaced
     * samples are furthest apart exactly where the curve bends most.
     *
     * <p>So each arc is halved until its midpoint lies within
     * {@code FLATNESS_PX} of the chord that replaces it - the usual
     * sagitta test, which spends subdivisions only where the curve
     * actually needs them. The criterion is a criterion and not a
     * proof, so the achieved distance is <em>measured</em> against a
     * dense sample of the true curve in
     * {@code OnThisPageSphericalTest}.
     */
    static java.awt.geom.Path2D.Double outlineOf(
            GnomonicProjection projection, ViewportMapping mapping,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg) {
        return outlineOf(projection, mapping, centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, MAX_DEPTH);
    }

    static java.awt.geom.Path2D.Double outlineOf(
            GnomonicProjection projection, ViewportMapping mapping,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg, int maxDepth) {
        java.awt.geom.Path2D.Double outline =
                new java.awt.geom.Path2D.Double();
        java.awt.geom.Point2D.Double previous = null;
        double previousT = 0;
        for (int i = 0; i <= INITIAL_ARCS; i++) {
            double t = 2 * Math.PI * i / INITIAL_ARCS;
            java.awt.geom.Point2D.Double here = boundaryPixel(projection,
                    mapping, centre, semiMajorDeg, semiMinorDeg,
                    positionAngleDeg, t);
            if (here == null) {
                // Part of this object lies past the projection's
                // horizon, and this rule does not decide that case.
                //
                // It drew a chord across the gap once, which stood
                // for sky the projection had refused. Breaking the
                // path instead cost the containment a closed curve
                // gives for free, and probing a handful of points of
                // the paper cannot decide an arbitrary clipped
                // region either (gate review). Nothing bundled with
                // the atlas can reach here - the largest object it
                // holds is a few degrees across - so the honest
                // answer is to refuse, loudly, rather than to
                // approximate a case that does not arise.
                throw new IllegalStateException(String.format(
                        Locale.ROOT,
                        "this rule does not decide an object that runs"
                                + " off the projection: %.3f x %.3f deg"
                                + " at %.3f, %.3f",
                        semiMajorDeg, semiMinorDeg, centre.raDegrees(),
                        centre.decDegrees()));
            }
            if (previous == null) {
                outline.moveTo(here.x, here.y);
            } else {
                subdivide(projection, mapping, centre, semiMajorDeg,
                        semiMinorDeg, positionAngleDeg, previousT, previous,
                        t, here, 0, maxDepth, outline);
            }
            previous = here;
            previousT = t;
        }
        return outline;
    }

    private static void subdivide(GnomonicProjection projection,
                                  ViewportMapping mapping,
                                  SkyPosition centre, double semiMajorDeg,
                                  double semiMinorDeg, double positionAngleDeg,
                                  double t0, java.awt.geom.Point2D.Double p0,
                                  double t1, java.awt.geom.Point2D.Double p1,
                                  int depth, int maxDepth,
                                  java.awt.geom.Path2D.Double outline) {
        double tm = 0.5 * (t0 + t1);
        java.awt.geom.Point2D.Double pm = boundaryPixel(projection, mapping,
                centre, semiMajorDeg, semiMinorDeg, positionAngleDeg, tm);
        if (pm == null || java.awt.geom.Line2D.ptSegDist(p0.x, p0.y,
                p1.x, p1.y, pm.x, pm.y) <= FLATNESS_PX) {
            outline.lineTo(p1.x, p1.y);
            return;
        }
        if (depth >= maxDepth) {
            // The guarantee has run out. Counting the lapse and
            // drawing the chord anyway was still an unbounded chord
            // wearing a tally (gate review): the bound either holds
            // or the geometry does not answer. It does not answer.
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "the boundary could not be followed to within"
                            + " %.2f px at depth %d: %.3f x %.3f deg"
                            + " at %.3f, %.3f", FLATNESS_PX, maxDepth,
                    semiMajorDeg, semiMinorDeg, centre.raDegrees(),
                    centre.decDegrees()));
        }
        subdivide(projection, mapping, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, t0, p0, tm, pm, depth + 1, maxDepth,
                outline);
        subdivide(projection, mapping, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, tm, pm, t1, p1, depth + 1, maxDepth,
                outline);
    }

    /**
     * The boundary point at parameter {@code t}, projected onto the
     * page. Null where the projection has nothing to say.
     */
    static java.awt.geom.Point2D.Double boundaryPixel(
            GnomonicProjection projection, ViewportMapping mapping,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg, double t) {
        double along = semiMajorDeg * Math.cos(t);
        double across = semiMinorDeg * Math.sin(t);
        double distance = Math.hypot(along, across);
        double bearing = positionAngleDeg
                + Math.toDegrees(Math.atan2(across, along));
        PixelPoint pixel = projection
                .project(offsetOf(centre, distance, bearing))
                .map(mapping::toPixel).orElse(null);
        return pixel == null ? null
                : new java.awt.geom.Point2D.Double(pixel.x(), pixel.y());
    }

    /** The same boundary, for a page rather than for a projection. */
    static java.awt.geom.Path2D.Double outlineOn(ChartScene scene,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg) {
        return outlineOn(scene, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, MAX_DEPTH);
    }

    static java.awt.geom.Path2D.Double outlineOn(ChartScene scene,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg, int maxDepth) {
        return outlineOf(new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, maxDepth);
    }

    /** The same boundary point, for a page rather than a projection. */
    static java.awt.geom.Point2D.Double boundaryPixelOn(ChartScene scene,
            SkyPosition centre, double semiMajorDeg, double semiMinorDeg,
            double positionAngleDeg, double t) {
        return boundaryPixel(new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, t);
    }

    /**
     * Membership in an angular ellipse, by true separation and
     * bearing east of north - the convention the catalogue records
     * its position angles in.
     */
    static boolean insideAngularEllipse(SkyPosition centre,
                                        SkyPosition point,
                                        double semiMajorDeg,
                                        double semiMinorDeg,
                                        double positionAngleDeg) {
        double r = centre.separationDegrees(point);
        if (r > semiMajorDeg) {
            return false;
        }
        double offset = Math.toRadians(
                bearingDegrees(centre, point) - positionAngleDeg);
        double along = r * Math.cos(offset);
        double across = r * Math.sin(offset);
        return (along * along) / (semiMajorDeg * semiMajorDeg)
                + (across * across) / (semiMinorDeg * semiMinorDeg) <= 1.0;
    }

    /** Bearing east of north, on the sphere. */
    static double bearingDegrees(SkyPosition from, SkyPosition to) {
        double dec1 = Math.toRadians(from.decDegrees());
        double dec2 = Math.toRadians(to.decDegrees());
        double dRa = Math.toRadians(to.raDegrees() - from.raDegrees());
        double y = Math.sin(dRa) * Math.cos(dec2);
        double x = Math.cos(dec1) * Math.sin(dec2)
                - Math.sin(dec1) * Math.cos(dec2) * Math.cos(dRa);
        double degrees = Math.toDegrees(Math.atan2(y, x)) % 360.0;
        return degrees < 0 ? degrees + 360.0 : degrees;
    }

    /**
     * How far the drawn path may stray from the true curve, and how
     * hard it may try. A twentieth of a pixel is far below anything
     * a reader could see, and well below the width of the thinnest
     * mark the atlas paints.
     */
    static final double FLATNESS_PX = 0.05;
    private static final int INITIAL_ARCS = 48;
    private static final int MAX_DEPTH = 18;

    /**
     * The point a given angular distance from a centre, at a given
     * bearing east of north. Spherical, so it stays true for an
     * object degrees across and near a pole.
     */
    static SkyPosition offsetOf(SkyPosition centre, double distanceDeg,
                                double bearingDeg) {
        double dec = Math.toRadians(centre.decDegrees());
        double ra = Math.toRadians(centre.raDegrees());
        double d = Math.toRadians(distanceDeg);
        double theta = Math.toRadians(bearingDeg);
        double sinDec = Math.sin(dec) * Math.cos(d)
                + Math.cos(dec) * Math.sin(d) * Math.cos(theta);
        double newDec = Math.asin(Math.max(-1.0, Math.min(1.0, sinDec)));
        double newRa = ra + Math.atan2(
                Math.sin(theta) * Math.sin(d) * Math.cos(dec),
                Math.cos(d) - Math.sin(dec) * Math.sin(newDec));
        // Normalised carefully: a tiny negative plus 360 lands on
        // exactly 360.0, which is not a right ascension.
        double degrees = Math.toDegrees(newRa) % 360.0;
        if (degrees < 0) {
            degrees += 360.0;
        }
        if (degrees >= 360.0) {
            degrees = 0.0;
        }
        return new SkyPosition(degrees, Math.toDegrees(newDec));
    }

    /** The recorded ellipse in page pixels, oriented as the chart draws. */
    static java.awt.Shape recordedEllipse(PixelPoint centre, double majorPx,
                                          double minorPx,
                                          double positionAngleDegrees) {
        java.awt.geom.Ellipse2D local = new java.awt.geom.Ellipse2D.Double(
                -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
        java.awt.geom.AffineTransform place =
                java.awt.geom.AffineTransform.getTranslateInstance(
                        centre.x(), centre.y());
        // Position angle is east of north; east is left on the chart,
        // which is a clockwise-negative rotation in pixel space - the
        // renderer's own convention, not a second one.
        place.rotate(-Math.toRadians(positionAngleDegrees));
        return place.createTransformedShape(local);
    }

    static double arcminToPx(double arcmin, ViewportMapping mapping) {
        return arcmin / 60.0 * mapping.pixelsPerPlaneUnit()
                * Math.PI / 180.0;
    }

    /** The conservative envelope, used only where orientation is unknown. */
    private static boolean circleReaches(PixelPoint centre, double radiusPx) {
        double dx = Math.max(0.0, Math.max(PAPER.getMinX() - centre.x(),
                centre.x() - PAPER.getMaxX()));
        double dy = Math.max(0.0, Math.max(PAPER.getMinY() - centre.y(),
                centre.y() - PAPER.getMaxY()));
        return Math.hypot(dx, dy) <= radiusPx;
    }

    private static boolean insidePaper(PixelPoint pixel) {
        return pixel.x() >= 1 && pixel.y() >= 1
                && pixel.x() <= WIDTH - 1 && pixel.y() <= HEIGHT - 1;
    }

    private record Inventory(List<DeepSkyObject> deepSky, List<Star> stars) {
    }

    private static Inventory inventoryOf(ChartScene scene) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        List<DeepSkyObject> deepSky = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (reachesPaper(projection, mapping, scene, dso)) {
                deepSky.add(dso);
            }
        }
        List<Star> stars = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (onPage(projection, mapping, star.position()) != null) {
                stars.add(star);
            }
        }
        return new Inventory(deepSky, stars);
    }

    private static ChartScene pageOf(Page page, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(page.ra(), page.dec()), field, 8.0),
                WIDTH, HEIGHT);
    }

    // ------------------------------------------------------------------

    private static void sizes() {
        System.out.println("## How much is on a page");
        System.out.println();
        System.out.println("| page | why | field | deep-sky | stars |"
                + " total |");
        System.out.println("|---|---|---:|---:|---:|---:|");
        int worstStars = 0;
        int worstDeepSky = 0;
        for (Page page : PAGES) {
            for (double field : FIELDS) {
                Inventory inventory = inventoryOf(pageOf(page, field));
                worstStars = Math.max(worstStars, inventory.stars().size());
                worstDeepSky = Math.max(worstDeepSky,
                        inventory.deepSky().size());
                System.out.printf(Locale.ROOT,
                        "| %s | %s | %.0f° | %d | %d | %d |%n",
                        page.name(), page.why(), field,
                        inventory.deepSky().size(),
                        inventory.stars().size(),
                        inventory.deepSky().size()
                                + inventory.stars().size());
            }
        }
        System.out.println();
        System.out.printf(Locale.ROOT,
                "The worst page here carries **%d stars** and **%d"
                        + " deep-sky objects**. That is the number a"
                        + " table has to survive, and it decides"
                        + " whether one undifferentiated list is"
                        + " honest.%n%n",
                worstStars, worstDeepSky);
    }

    // ------------------------------------------------------------------

    /**
     * What the centre-only rule would have missed. The gate first
     * proposed "the recorded position projects onto the paper", and
     * review pointed out that a large symbol can cross the page edge
     * with its centre outside it - so the table would report nothing
     * while the page showed a galaxy.
     */
    private static void extentRule() {
        System.out.println("## Centres are not enough");
        System.out.println();
        System.out.println("An object is on the page when its"
                + " **recorded extent** reaches the paper, not merely"
                + " its centre. M 31 is 178 arcminutes long: a page"
                + " can be filled by its disc while its centre sits"
                + " outside the paper, and a centre-only rule would"
                + " report an empty page in front of a visible"
                + " galaxy.");
        System.out.println();
        System.out.println("| page | field | centres only | with"
                + " recorded extent | missed |");
        System.out.println("|---|---:|---:|---:|---:|");
        int missedTotal = 0;
        for (Page page : PAGES) {
            for (double field : FIELDS) {
                ChartScene scene = pageOf(page, field);
                GnomonicProjection projection =
                        new GnomonicProjection(scene.viewport().centre());
                ViewportMapping mapping =
                        new ViewportMapping(scene.viewport());
                int centres = 0;
                int extents = 0;
                for (DeepSkyObject dso : scene.deepSkyObjects()) {
                    if (onPage(projection, mapping, dso.position()) != null) {
                        centres++;
                    }
                    if (reachesPaper(projection, mapping, scene, dso)) {
                        extents++;
                    }
                }
                if (extents == centres) {
                    continue;
                }
                missedTotal += extents - centres;
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | **%d** |%n",
                        page.name(), field, centres, extents,
                        extents - centres);
            }
        }
        System.out.println();
        System.out.printf(Locale.ROOT,
                "**%d objects** across these pages would have been"
                        + " left out of a table that asked only about"
                        + " centres - among them M 32 and M 110 on a"
                        + " 1° view of M 31, which is the closest look"
                        + " the atlas offers at the page it opens"
                        + " on.%n%n", missedTotal);
        System.out.println("The extent used is what the **source"
                + " recorded**, never the display size the loader"
                + " substitutes for the renderer where the catalogue"
                + " is silent. **An object of unknown size is a"
                + " point**: the atlas knows of no extent for it to"
                + " reach the paper with, and inventing one would put"
                + " a size nobody measured in charge of what a table"
                + " says is on the page.");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Measured over the bundled pack, **%.1f%% of rows"
                        + " record no size at all** - about one in"
                        + " ten, so the rule decides real rows rather"
                        + " than a corner case.%n%n",
                100.0 * unsized() / packSize());
        System.out.println("### The ellipse, on the sphere");
        System.out.println();
        System.out.println("Two earlier drafts were bounds rather"
                + " than the thing: a square of the half-major, then"
                + " a circle of it. A circle contains the ellipse"
                + " whichever way it lies, so it never misses - and"
                + " it says yes for a thin object lying along the"
                + " page edge whose known shape never comes near"
                + " it.");
        System.out.println();
        System.out.println("A third turned arcminutes into pixels"
                + " once, at the page centre's scale, and tested a"
                + " flat ellipse. **A gnomonic page has no single"
                + " scale**: it stretches away from the centre, and"
                + " the Large Magellanic Cloud is nearly eleven"
                + " degrees across. An ellipse sized at the middle is"
                + " the wrong shape by the time it reaches an edge -"
                + " which is exactly where this question is asked.");
        System.out.println();
        System.out.println("So the boundary is walked **on the"
                + " sphere**, at the recorded semi-axes and position"
                + " angle east of north, and each point is projected"
                + " through the atlas's own projection and viewport"
                + " mapping. Where the source is silent the fallback"
                + " is explicit rather than invented:");
        System.out.println();
        System.out.println("| what the source recorded | what is"
                + " tested | rows in the pack |");
        System.out.println("|---|---|---:|");
        int[] tally = geometryTally();
        System.out.printf(Locale.ROOT,
                "| nothing | a **point** - the atlas knows of no"
                        + " extent | %,d |%n", tally[0]);
        System.out.printf(Locale.ROOT,
                "| a major axis only, or no position angle | the"
                        + " **circle** of the half-major, because"
                        + " every ellipse the catalogue permits fits"
                        + " inside it | %,d |%n", tally[1]);
        System.out.printf(Locale.ROOT,
                "| major, minor and orientation | the **ellipse**"
                        + " itself | %,d |%n", tally[2]);
        System.out.println();
        System.out.printf(Locale.ROOT,
                "The envelope therefore decides **%,d rows** and the"
                        + " exact ellipse decides **%,d**, which is"
                        + " the honest split: the conservative answer"
                        + " is given exactly where the catalogue"
                        + " leaves no better one.%n%n",
                tally[1], tally[2]);
        System.out.printf(Locale.ROOT,
                "Every boundary above was drawn to within a twentieth"
                        + " of a pixel of the true curve, and the"
                        + " geometry **refuses** rather than"
                        + " approximates: an object that runs off the"
                        + " projection, or a boundary that cannot be"
                        + " followed to that distance, stops the study"
                        + " where it stands.%n%n");
        System.out.printf(Locale.ROOT,
                "Neither can happen with the bundled pack, and the"
                        + " reason is structural rather than a survey"
                        + " of pages. At the widest field the atlas"
                        + " offers, the tallest page the assembler"
                        + " will build reaches **%.1f°** from its"
                        + " centre - it letterboxes a taller window"
                        + " rather than promising sky the projection"
                        + " cannot draw honestly; the assembler"
                        + " queries"
                        + " that reach plus the pack's declared"
                        + " **%.2f°** object margin; and nothing it"
                        + " returns extends more than that same"
                        + " **%.2f°** from its own centre. So the"
                        + " furthest any boundary can lie from a page"
                        + " centre is %.1f + %.2f + %.2f ="
                        + " **%.2f°**, short of the %.0f° horizon."
                        + " The largest object the pack actually"
                        + " records is %.2f°, and it holds %,d in"
                        + " all. Every number here is asked of"
                        + " production rather than restated: the"
                        + " margin from the pack's manifest, the reach"
                        + " from a page the assembler built.%n%n",
                widestPageReachDegrees(), declaredObjectMarginDegrees(),
                declaredObjectMarginDegrees(), widestPageReachDegrees(),
                declaredObjectMarginDegrees(), declaredObjectMarginDegrees(),
                widestPageReachDegrees() + 2 * declaredObjectMarginDegrees(),
                90.0, largestRecordedSemiMajorDegrees(), packSize());
        System.out.println("`OnThisPageSphericalTest` checks the"
                + " rule from the opposite direction: it samples the"
                + " **paper**, turns each pixel back into a sky"
                + " position through the atlas's own inverse - what"
                + " grab-to-pan uses - and asks whether that"
                + " position lies inside the object's angular"
                + " ellipse. Forward and inverse are independent"
                + " enough to disagree if the geometry is wrong, and"
                + " the Magellanic Cloud placed off-centre on a"
                + " 36-degree page is one of the cases they are held"
                + " to.");
        System.out.println();
        System.out.println();
    }

    /** How the pack's rows are decided: point, envelope, ellipse. */
    private static int[] geometryTally() {
        int[] tally = new int[3];
        for (DeepSkyObject dso : DeepSkyVocabularyStudyMain.wholePack()) {
            DeepSkyObject.Recorded recorded = dso.recorded();
            if (!recorded.hasSize()) {
                tally[0]++;
            } else if (recorded.minorAxisArcmin() == null
                    || recorded.positionAngleDegrees() == null) {
                tally[1]++;
            } else {
                tally[2]++;
            }
        }
        return tally;
    }

    private static int unsized() {
        int count = 0;
        for (DeepSkyObject dso : DeepSkyVocabularyStudyMain.wholePack()) {
            if (!dso.recorded().hasSize()) {
                count++;
            }
        }
        return count;
    }

    private static int packSize() {
        return DeepSkyVocabularyStudyMain.wholePack().size();
    }

    /**
     * How far a page can reach from its centre, whatever window a
     * reader opens.
     *
     * <p>Not a number written down here: the assembler already caps
     * it, letterboxing a tall window rather than promising sky the
     * projection cannot draw honestly, and restating that cap in
     * this study would be a second copy of it to drift (gate
     * review). So the widest field the atlas offers is assembled
     * into the tallest page the assembler will allow, and the page
     * that comes back is asked how far its corners reach.
     */
    static double widestPageReachDegrees() {
        SceneAssembler assembler = Atlas.assembler();
        double widestField = ChartViewState.fieldWidthSteps().get(0);
        SkyPosition centre = Atlas.DEFAULT_CENTRE;
        int tallest = assembler.maxPageHeightPx(centre, widestField, WIDTH);
        return pageReachDegrees(assembler.assemble(
                new ChartViewState(centre, widestField, 8.0),
                WIDTH, tallest));
    }

    /** The largest object extent the pack's own manifest declares. */
    static double declaredObjectMarginDegrees() {
        return juranometria.catalog.TiledCatalogue.load().manifest()
                .maxObjectSemiExtentDegrees();
    }

    /**
     * The half-length of the longest object the pack records, in
     * degrees. The refusal above is only defensible because this is
     * small: an object has to be tens of degrees across before any
     * page can put part of it past the projection's horizon.
     */
    static double largestRecordedSemiMajorDegrees() {
        double largest = 0;
        for (DeepSkyObject dso : DeepSkyVocabularyStudyMain.wholePack()) {
            if (dso.recorded().hasSize()) {
                largest = Math.max(largest,
                        dso.recorded().majorAxisArcmin() / 120.0);
            }
        }
        return largest;
    }

    private static void visibilityBreakdown() {
        ChartRenderer renderer =
                new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
        ChartOptions defaults = ChartOptions.DEFAULTS;
        ChartOptions galaxiesOff = defaults.withFamily(
                juranometria.render.SymbolFamily.GALAXIES, false);

        System.out.println("## Present, and why it cannot be seen");
        System.out.println();
        System.out.println("Every state is production's own answer."
                + " `permitted` is the rule the family switches obey,"
                + " the detail policy is the one the renderer asks,"
                + " and the magnitude limit is the scene's. Nothing"
                + " here infers a state from a missing pixel.");
        System.out.println();
        System.out.println("| page | field | drawn | hidden by an"
                + " option | fainter than the limit | no symbol |"
                + " too small at this field |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|");
        for (Page page : PAGES) {
            for (double field : new double[] {8.0, 36.0}) {
                ChartScene scene = pageOf(page, field);
                Map<Visibility, Integer> tally =
                        classify(scene, defaults, renderer);
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %d | %d | %d |%n",
                        page.name(), field,
                        tally.getOrDefault(Visibility.DRAWN, 0),
                        tally.getOrDefault(Visibility.FAMILY_HIDDEN, 0),
                        tally.getOrDefault(Visibility.BELOW_LIMIT, 0),
                        tally.getOrDefault(Visibility.NO_SYMBOL, 0),
                        tally.getOrDefault(Visibility.TOO_SMALL, 0));
            }
        }
        System.out.println();

        ChartScene m31 = pageOf(PAGES.get(0), 8.0);
        Map<Visibility, Integer> withGalaxies =
                classify(m31, defaults, renderer);
        Map<Visibility, Integer> without =
                classify(m31, galaxiesOff, renderer);
        System.out.printf(Locale.ROOT,
                "Switching **Galaxies** off on the released page moves"
                        + " **%d** objects from *drawn* to *hidden by a"
                        + " chart option* - %d to %d - without changing"
                        + " what the page contains. That is the"
                        + " distinction the table exists to make:"
                        + " presence is a fact about the sky, and"
                        + " visibility is a fact about the reader's"
                        + " choices.%n%n",
                withGalaxies.getOrDefault(Visibility.DRAWN, 0)
                        - without.getOrDefault(Visibility.DRAWN, 0),
                withGalaxies.getOrDefault(Visibility.DRAWN, 0),
                without.getOrDefault(Visibility.DRAWN, 0));
    }

    private static Map<Visibility, Integer> classify(
            ChartScene scene, ChartOptions options,
            ChartRenderer renderer) {
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        Inventory inventory = inventoryOf(scene);
        Map<Visibility, Integer> tally = new LinkedHashMap<>();
        for (DeepSkyObject dso : inventory.deepSky()) {
            tally.merge(stateOf(scene, dso, options, policy), 1, Integer::sum);
        }
        for (Star star : inventory.stars()) {
            tally.merge(star.magnitude() > scene.limitingMagnitude()
                            ? Visibility.BELOW_LIMIT : Visibility.DRAWN,
                    1, Integer::sum);
        }
        return tally;
    }

    private static Visibility stateOf(ChartScene scene, DeepSkyObject dso,
                                      ChartOptions options,
                                      RegionalDetailPolicy policy) {
        if (ChartRenderer.symbolForType(dso.type())
                == ChartRenderer.Symbol.NONE) {
            return Visibility.NO_SYMBOL;
        }
        if (!ChartRenderer.permitted(scene, dso, options)) {
            return Visibility.FAMILY_HIDDEN;
        }
        if (!policy.drawn(dso)) {
            return Visibility.TOO_SMALL;
        }
        return Visibility.DRAWN;
    }

    // ------------------------------------------------------------------

    /**
     * The star question. A page can carry more than a thousand
     * stars, and almost all of them are a catalogue number and a
     * brightness - a row saying "TYC 2801-2090-1, 4.51" answers
     * nothing a reader asked. This measures how many carry a name,
     * a Bayer letter or a Flamsteed number: something a reader could
     * have come looking for.
     */
    private static void theStarQuestion() {
        System.out.println("## The stars, and whether they belong in it");
        System.out.println();
        System.out.println("A page carries far more stars than"
                + " deep-sky objects, and nearly all of them are a"
                + " catalogue number and a brightness. A row reading"
                + " *TYC 2801-2090-1, 4.5* answers no question a"
                + " reader had. So the question is not whether stars"
                + " are on the page - they are, and the table must"
                + " not pretend otherwise - but which of them a"
                + " reader could have come looking for.");
        System.out.println();
        System.out.println("| page | field | stars | with a name,"
                + " Bayer letter or Flamsteed number | anonymous |");
        System.out.println("|---|---:|---:|---:|---:|");
        for (Page page : PAGES) {
            for (double field : new double[] {8.0, 36.0}) {
                ChartScene scene = pageOf(page, field);
                List<Star> stars = inventoryOf(scene).stars();
                int named = 0;
                for (Star star : stars) {
                    if (isNamed(star)) {
                        named++;
                    }
                }
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %d |%n",
                        page.name(), field, stars.size(), named,
                        stars.size() - named);
            }
        }
        System.out.println();

        // And what the magnitude limit does, which the bundled pack
        // hides at the default: it holds bright sky only, so nothing
        // is past V 8 until a reader asks for less.
        ChartScene bright = Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(83.8, 0.0), 18.0, 4.0), WIDTH, HEIGHT);
        List<Star> all = inventoryOf(bright).stars();
        int past = 0;
        for (Star star : all) {
            if (star.magnitude() > bright.limitingMagnitude()) {
                past++;
            }
        }
        System.out.printf(Locale.ROOT,
                "**The magnitude limit only bites when a reader asks"
                        + " it to.** The bundled pack is bright sky,"
                        + " so at the released limit of V 8 nothing on"
                        + " any page above is past it. Set Orion's"
                        + " 18° page to V 4 and **%d of its %d stars**"
                        + " become present-but-unplotted - which is"
                        + " precisely the state a reader cannot"
                        + " discover by looking at the paper.%n%n",
                past, all.size());
    }

    /** Whether a star carries something a reader could search for. */
    private static boolean isNamed(Star star) {
        juranometria.chart.StarIdentity identity = star.identity();
        return identity != null && (identity.name() != null
                || identity.bayer() != null
                || identity.flamsteed() != null);
    }

    /**
     * One row of the table: what a reader reads, built from the
     * catalogue and from production's own visibility answer.
     */
    record Row(String identity, String kind, String magnitude,
               String from, String visibility, boolean counted) {
    }

    /**
     * The page's rows, in the decided total order.
     *
     * <p>The order is total <em>across kinds</em>, which the first
     * draft left undefined (gate review): a table holding galaxies,
     * named stars and counted lines has to say which comes first, or
     * two runs can disagree.
     *
     * <ol>
     *   <li><strong>deep-sky objects</strong>, then <strong>named
     *       stars</strong>, then the <strong>counted lines</strong>.
     *       A reader asking what is here is hunting objects; the
     *       named stars are the landmarks they steer by; and a line
     *       that counts what is not listed is a statement about the
     *       page rather than a thing on it, so it never sorts into
     *       the middle of its kind.</li>
     *   <li>within a kind: a Messier number first, then recorded
     *       brightness, then distance from the centre, then
     *       catalogue identity - which makes it total, so the same
     *       page always lists identically however the catalogue
     *       arrives.</li>
     * </ol>
     */
    static List<Row> rowsFor(ChartScene scene, ChartOptions options) {
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        SkyPosition centre = scene.viewport().centre();
        Inventory inventory = inventoryOf(scene);

        List<DeepSkyObject> deepSky = new ArrayList<>(inventory.deepSky());
        deepSky.sort(defaultOrder(centre));
        List<Row> rows = new ArrayList<>();
        for (DeepSkyObject dso : deepSky) {
            rows.add(new Row(nameOf(dso), kindOf(dso),
                    magnitudeOf(dso.magnitude(), dso.recorded().band()),
                    String.format(Locale.ROOT, "%.2f°",
                            centre.separationDegrees(dso.position())),
                    wordFor(stateOf(scene, dso, options, policy)), false));
        }

        List<Star> named = new ArrayList<>();
        int anonymous = 0;
        for (Star star : inventory.stars()) {
            if (isNamed(star)) {
                named.add(star);
            } else {
                anonymous++;
            }
        }
        named.sort(Comparator
                .comparingDouble(Star::magnitude)
                .thenComparingDouble(star ->
                        centre.separationDegrees(star.position()))
                .thenComparing(Star::id));
        for (Star star : named) {
            rows.add(new Row(bestName(star), "star",
                    magnitudeOf(star.magnitude(), null),
                    String.format(Locale.ROOT, "%.2f°",
                            centre.separationDegrees(star.position())),
                    star.magnitude() > scene.limitingMagnitude()
                            ? "too faint" : "drawn", false));
        }
        if (anonymous > 0) {
            rows.add(new Row(String.format(Locale.ROOT,
                    "and %,d further stars", anonymous), "star", "", "",
                    "none named", true));
        }
        return rows;
    }

    private static String nameOf(DeepSkyObject dso) {
        Integer messier = messierOf(dso);
        return messier == null ? dso.id() : "M " + messier;
    }

    private static String kindOf(DeepSkyObject dso) {
        juranometria.render.SymbolFamily family =
                juranometria.render.SymbolFamily.of(dso);
        return family == null
                ? dso.type().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                : family.label().toLowerCase(Locale.ROOT);
    }

    private static String magnitudeOf(double magnitude,
                                      DeepSkyObject.Recorded.Band band) {
        if (Double.isNaN(magnitude)) {
            return "not recorded";
        }
        String suffix = band == null
                || band == DeepSkyObject.Recorded.Band.VISUAL ? " V"
                : band == DeepSkyObject.Recorded.Band.BLUE ? " B" : "";
        return String.format(Locale.ROOT, "%.1f%s", magnitude, suffix);
    }

    private static String bestName(Star star) {
        juranometria.chart.StarIdentity identity = star.identity();
        if (identity.name() != null) {
            return identity.name();
        }
        if (identity.bayer() != null) {
            return identity.bayer()
                    + (identity.constellation() == null ? ""
                            : " " + identity.constellation());
        }
        return identity.flamsteed()
                + (identity.constellation() == null ? ""
                        : " " + identity.constellation());
    }

    /** The short word the table shows for a state. */
    static String wordFor(Visibility state) {
        return switch (state) {
            case DRAWN -> "drawn";
            case FAMILY_HIDDEN -> "hidden";
            case BELOW_LIMIT -> "too faint";
            case NO_SYMBOL -> "no symbol";
            case TOO_SMALL -> "too small here";
        };
    }

    /** The released page, for the mock-ups to draw the real thing. */
    static ChartScene scenePage(String name, double field) {
        for (Page page : PAGES) {
            if (page.name().equals(name)) {
                return pageOf(page, field);
            }
        }
        throw new IllegalArgumentException("no such study page: " + name);
    }

    private static void ordering() {
        System.out.println("## The order a reader meets them");
        System.out.println();
        System.out.println("Proposed default: **a Messier number"
                + " first, then recorded brightness, then angular"
                + " distance from the centre, then catalogue"
                + " identity.** Identity last makes it total, so the"
                + " same page always lists in the same order however"
                + " the catalogue arrives.");
        System.out.println();
        ChartScene scene = pageOf(PAGES.get(0), 8.0);
        List<DeepSkyObject> deepSky =
                new ArrayList<>(inventoryOf(scene).deepSky());
        deepSky.sort(defaultOrder(scene.viewport().centre()));
        System.out.println("The released page, in that order:");
        System.out.println();
        System.out.println("| # | object | Messier | magnitude |"
                + " from centre |");
        System.out.println("|---:|---|---|---:|---:|");
        int row = 0;
        for (DeepSkyObject dso : deepSky) {
            if (++row > 8) {
                break;
            }
            System.out.printf(Locale.ROOT, "| %d | %s | %s | %s | %.2f° |%n",
                    row, dso.id(),
                    messierOf(dso) == null ? "—" : "M " + messierOf(dso),
                    Double.isNaN(dso.magnitude()) ? "not recorded"
                            : String.format(Locale.ROOT, "%.1f",
                                    dso.magnitude()),
                    scene.viewport().centre()
                            .separationDegrees(dso.position()));
        }
        System.out.printf(Locale.ROOT, "%n**%d** deep-sky rows in"
                + " all on that page.%n%n", deepSky.size());
    }

    /** A Messier number from the object's aliases, or null. */
    private static Integer messierOf(DeepSkyObject dso) {
        for (String alias : dso.aliases()) {
            String trimmed = alias.trim();
            if (trimmed.startsWith("M ") || trimmed.startsWith("M")) {
                String digits = trimmed.replaceAll("[^0-9]", "");
                if (!digits.isEmpty() && trimmed.replaceAll("[0-9 ]", "")
                        .equalsIgnoreCase("M")) {
                    return Integer.valueOf(digits);
                }
            }
        }
        return null;
    }

    /**
     * How a blue magnitude and a visual one sort together
     * (gate review).
     *
     * <p>They are different measurements and the atlas does not
     * convert between them - 68.1% of the bundled pack records no V
     * magnitude at all, so a table refusing to place B rows would
     * refuse most of the sky. The rule is therefore: <strong>sort by
     * the recorded number, never converted, with the band always
     * shown; where two numbers are equal, the visual one first;
     * where nothing is recorded, last.</strong>
     *
     * <p>The consequence is stated rather than hidden: a B 9.0 sorts
     * beside a V 9.0 though it is not the same measurement, and the
     * reader can see which is which because the band is in the cell.
     * Ordering approximately by brightness while labelling each
     * value honestly is worth more than an exact order over a
     * quantity the catalogue does not hold.
     */
    private static Comparator<DeepSkyObject> byRecordedBrightness() {
        return Comparator
                .comparingDouble((DeepSkyObject dso) ->
                        Double.isNaN(dso.magnitude()) ? Double.MAX_VALUE
                                : dso.magnitude())
                .thenComparingInt(dso -> dso.recorded().band()
                        == DeepSkyObject.Recorded.Band.VISUAL ? 0 : 1);
    }

    private static Comparator<DeepSkyObject> defaultOrder(SkyPosition centre) {
        return Comparator
                .comparingInt((DeepSkyObject dso) ->
                        messierOf(dso) == null ? 1 : 0)
                .thenComparingInt(dso -> messierOf(dso) == null
                        ? Integer.MAX_VALUE : messierOf(dso))
                .thenComparing(byRecordedBrightness())
                .thenComparingDouble(dso ->
                        centre.separationDegrees(dso.position()))
                .thenComparing(DeepSkyObject::id);
    }

    // ------------------------------------------------------------------

    /**
     * The keyboard decision. The evidence for it is a test rather
     * than a report section: a study firing Swing actions on an
     * off-screen table proves a binding exists, not that a key
     * reaches it (gate review), and the difference is exactly what
     * #209 turned out to be.
     */
    private static void keyboard() {
        System.out.println("## Working it without a pointer");
        System.out.println();
        System.out.println("**The module adds no key bindings of its"
                + " own.** Walking rows and extending a selection are"
                + " gestures the platform already provides, and a"
                + " module that taught the table new keys would be a"
                + " module assistive technology has to be taught"
                + " too.");
        System.out.println();
        System.out.println("**And what a key does is not the same"
                + " everywhere.** `HOME` returns to the first row on"
                + " the Linux runner and moves the column under the"
                + " macOS bindings, leaving the selection where it"
                + " was. This gate first recorded the second as a"
                + " universal gap; the display job found otherwise on"
                + " its first run of these tests.");
        System.out.println();
        System.out.println("That is the argument for the rule rather"
                + " than against it. A module that reasoned about"
                + " particular keys would have reasoned from one"
                + " desktop. **#216 offers getting back to the top as"
                + " an explicit control** - not because no platform"
                + " binds a key for it, but because they do not agree"
                + " on which.");
        System.out.println();
        System.out.println("That is asserted in"
                + " `OnThisPageKeyboardTest`, in a real window, with"
                + " the window and the table made to hold the focus,"
                + " using real key events - so a key is proved to"
                + " *arrive*, not merely to have somewhere to arrive."
                + " It runs in the display job on every pull"
                + " request, which is how the platform difference"
                + " above came to light. Firing the bound actions"
                + " off-screen, as this study first did, would have"
                + " proved the bindings exist while saying nothing"
                + " about whether a reader's keys reach them - and"
                + " would have proved it on one desktop.");
        System.out.println();
        System.out.println("**Enter** takes the lead row into the"
                + " Selected facts and **Centre here** is explicit -"
                + " selecting a row never moves the chart, the"
                + " promise point-and-identify has kept since Sprint"
                + " 19.");
        System.out.println();
    }

    private static void cost() {
        System.out.println("## What it costs to know");
        System.out.println();
        System.out.println("The scene the chart already assembled"
                + " holds **every** star and deep-sky object in the"
                + " queried region, unfiltered - the renderer applies"
                + " the magnitude limit as it draws. So an inventory"
                + " needs no catalogue query of its own: it is a"
                + " projection sweep over a list the page is already"
                + " holding.");
        System.out.println();
        long objects = 0;
        for (Page page : PAGES) {
            ChartScene scene = pageOf(page, 8.0);
            objects += scene.stars().size() + scene.deepSkyObjects().size();
        }
        System.out.printf(Locale.ROOT,
                "Across the six pages at 8°, that is **%d objects**"
                        + " to project - arithmetic, no catalogue, no"
                        + " allocation beyond the answer.%n%n", objects);
        System.out.println("Timings are deliberately absent from this"
                + " report. They differ between machines and between"
                + " two runs on one machine, and a study whose output"
                + " moves cannot be reproduced - the same rule the"
                + " Milky Way gate settled. #215 sets the interaction"
                + " budget and enforces it on the supported CI path.");
        System.out.println();
    }
}
