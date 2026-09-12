package juranometria.tool.globe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.ui.ChartViewController;

/**
 * What a drag does near the limb (Sprint 32, issue #330).
 *
 * <p>The owner dragged the first production globe and reported that it
 * feels natural, which settles the ordinary case and leaves the one
 * the arithmetic is worst at. A globe's radial scale is {@code cos} of
 * the angle from the centre: it collapses to nothing at the limb, so
 * the same pixel of movement asks for more and more sky the further
 * out the grab is. Whether that stays usable is a measurement, not an
 * opinion.
 *
 * <p>Measured through the production path and nothing beside it. A
 * drag is {@code PanInteraction.mouseDragged}: the sky taken at the
 * press, the pointer's plane point on the current page, and
 * {@code ChartViewController.pan} between them. This asks exactly
 * that, so what is reported is what a reader's hand does.
 *
 * <p>Four questions, and the issue stops for a decision rather than
 * inventing an answer to any of them:
 *
 * <ul>
 *   <li><strong>Does the grabbed sky stay under the pointer?</strong>
 *       The promise every other rung is held to.</li>
 *   <li><strong>How fast does the page move?</strong> Degrees of
 *       centre travel per pixel of drag, by radius.</li>
 *   <li><strong>Does the solve stay well posed?</strong> The solver
 *       says of itself whether it was constrained or ambiguous.</li>
 *   <li><strong>Does an accepted step ever jump?</strong> The centre
 *       equation has two roots and the tie-break takes the nearer; a
 *       near-limb drag is where it could take the far one.</li>
 * </ul>
 */
public final class GlobeDragStudyMain {

    private GlobeDragStudyMain() {
    }

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    /** The disc, as the production mapping lays it down. */
    private static final double DISC_PX = 0.90 * HIGH_PX / 2.0;

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /** Where a grab is taken, as a fraction of the disc's radius. */
    private static final double[] RADII =
            {0.0, 0.25, 0.50, 0.75, 0.90, 0.95, 0.98, 0.995};

    /** How far the hand moves, in pixels, between reports. */
    private static final double[] STEPS = {1.0, 4.0, 16.0, 64.0};

    public static void main(String[] args) throws IOException {
        System.out.println("# What a drag does near the limb");
        System.out.println();
        System.out.println("Measured through the production path: the"
                + " sky taken at the press, the pointer's plane");
        System.out.println("point on the current page, and"
                + " ChartViewController.pan between them - which is");
        System.out.println("what PanInteraction.mouseDragged does when"
                + " a reader moves their hand.");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "A %dx%d window, so the disc is %.0f px across and its"
                        + " radius is %.0f px.%n",
                WIDE_PX, HIGH_PX, 2 * DISC_PX, DISC_PX);

        stays();
        rate();
        posed();
        crossing();
        otherRungs();

    }

    // No platform record beside this one, and that is a statement
    // rather than an omission. Every other globe study measures ink,
    // which is the desktop's answer as much as the atlas's; this
    // measures where a page goes when a hand moves, which is
    // projection arithmetic and the same on every machine (#315).

    /**
     * Whether the grabbed sky stays beneath the pointer.
     *
     * <p>Grab at a radius, move the hand by a step, and ask where the
     * grabbed sky now lands. A drag that keeps its promise puts it
     * back under the pointer; the number reported is how far it
     * misses, in pixels of the reader's own page.
     */
    private static void stays() {
        System.out.println();
        System.out.println("## Does the grabbed sky stay under the"
                + " pointer");
        System.out.println();
        System.out.println("Drift in page pixels, by where the grab"
                + " was taken and how far the hand moved. A dash");
        System.out.println("is a step the atlas refused, which is an"
                + " answer rather than a gap.");
        System.out.println();
        System.out.printf(Locale.ROOT, "  %8s %10s", "radius", "sky out");
        for (double step : STEPS) {
            System.out.printf(Locale.ROOT, " %10s",
                    String.format(Locale.ROOT, "%.0f px", step));
        }
        System.out.println();

        for (double radius : RADII) {
            System.out.printf(Locale.ROOT, "  %8.3f %9.1f°", radius,
                    Math.toDegrees(Math.asin(Math.min(1.0, radius))));
            for (double step : STEPS) {
                Dragged dragged = drag(180.0, radius, step);
                System.out.printf(Locale.ROOT, " %10s",
                        dragged.refused() ? "-"
                                : String.format(Locale.ROOT, "%.2f",
                                        dragged.driftPx()));
            }
            System.out.println();
        }
    }

    /**
     * How much sky a pixel of hand movement asks for.
     *
     * <p>The scale collapses at the limb, so this is where a drag
     * would run away if it were going to: the same pixel buys more
     * and more degrees of centre travel the further out the grab is.
     */
    private static void rate() {
        System.out.println();
        System.out.println("## How far the page moves per pixel of"
                + " hand");
        System.out.println();
        System.out.println("Degrees the centre travels for one pixel"
                + " of drag, at each radius. The page's own scale");
        System.out.println("at the centre is the first row, and every"
                + " row after it is that number times how much");
        System.out.println("the sphere has turned away.");
        System.out.println();
        System.out.printf(Locale.ROOT, "  %8s %10s %14s %12s%n",
                "radius", "sky out", "deg per px", "times centre");

        double atCentre = 0.0;
        for (double radius : RADII) {
            Dragged dragged = drag(180.0, radius, 1.0);
            if (dragged.refused()) {
                System.out.printf(Locale.ROOT,
                        "  %8.3f %9.1f° %14s %12s%n", radius,
                        Math.toDegrees(Math.asin(Math.min(1.0, radius))),
                        "refused", "-");
                continue;
            }
            if (radius == 0.0) {
                atCentre = dragged.movedDegrees();
            }
            System.out.printf(Locale.ROOT,
                    "  %8.3f %9.1f° %14.4f %11.1fx%n", radius,
                    Math.toDegrees(Math.asin(Math.min(1.0, radius))),
                    dragged.movedDegrees(),
                    atCentre == 0.0 ? 0.0
                            : dragged.movedDegrees() / atCentre);
        }
    }

    /**
     * What the solver says about its own footing.
     *
     * <p>It reports whether a solve was constrained - pushed to the
     * edge of what the page can ask for - and whether two verified
     * centres were far enough apart to be a real ambiguity rather
     * than the double root's numerical twins. Both are the solver's
     * own words, not this study's guess about them.
     */
    private static void posed() {
        System.out.println();
        System.out.println("## What the solver says about its own"
                + " footing");
        System.out.println();
        System.out.println("Over a ring of eight bearings at each"
                + " radius, and four step sizes: how many of the");
        System.out.println("thirty-two solves were refused, how many"
                + " came back constrained, and how many");
        System.out.println("ambiguous - two verified centres far"
                + " enough apart to be different answers.");
        System.out.println();
        System.out.printf(Locale.ROOT, "  %8s %10s %10s %12s %11s%n",
                "radius", "solves", "refused", "constrained", "ambiguous");

        for (double radius : RADII) {
            int solves = 0;
            int refused = 0;
            int constrained = 0;
            int ambiguous = 0;
            for (double bearing = 0.0; bearing < 360.0; bearing += 45.0) {
                for (double step : STEPS) {
                    Dragged dragged = drag(180.0, radius, step, bearing);
                    solves++;
                    if (dragged.refused()) {
                        refused++;
                    }
                    if (dragged.constrained()) {
                        constrained++;
                    }
                    if (dragged.ambiguous()) {
                        ambiguous++;
                    }
                }
            }
            System.out.printf(Locale.ROOT,
                    "  %8.3f %10d %10d %12d %11d%n", radius, solves,
                    refused, constrained, ambiguous);
        }
    }

    /**
     * A hand that leaves the disc and comes back.
     *
     * <p>#329 settled that a pointer with no sky under it is refused,
     * and tested it on a press. A drag is a moving gesture: the
     * question here is whether crossing the limb mid-drag leaves the
     * page somewhere sensible, and whether coming back inside picks
     * the gesture up again rather than jumping.
     */
    private static void crossing() {
        System.out.println();
        System.out.println("## A hand that leaves the disc and comes"
                + " back");
        System.out.println();
        System.out.println("One gesture, walked pixel by pixel from"
                + " the middle of the disc out past the limb and");
        System.out.println("back to where it started. What is"
                + " reported is where the page ended up against");
        System.out.println("where it began - a gesture that returns"
                + " to its own press should return the page.");
        System.out.println();

        ChartViewState start = new ChartViewState(SAGITTARIUS, 180.0, 5.0);
        ChartViewport page = viewportFor(start);
        ChartViewController controller = new ChartViewController();
        controller.recenter(start.centre(), start.fieldWidthDegrees());

        PixelPoint press = new PixelPoint(WIDE_PX / 2.0 + 0.4 * DISC_PX,
                HIGH_PX / 2.0);
        SkyPosition grabbed = PanSolver.skyAt(page,
                PanSolver.planeFromPixel(page, press)).orElseThrow();

        int moved = 0;
        int refused = 0;
        List<double[]> path = new ArrayList<>();
        for (double out = 0.0; out <= 260.0; out += 4.0) {
            path.add(new double[] {press.x() + out, press.y()});
        }
        for (double back = 260.0; back >= 0.0; back -= 4.0) {
            path.add(new double[] {press.x() + back, press.y()});
        }
        for (double[] at : path) {
            boolean took = controller.pan(grabbed,
                    PanSolver.planeFromPixel(page,
                            new PixelPoint(at[0], at[1])));
            if (took) {
                moved++;
            } else {
                refused++;
            }
        }

        double home = start.centre()
                .separationDegrees(controller.state().centre());
        System.out.printf(Locale.ROOT,
                "  %d steps: %d moved the page, %d refused%n",
                path.size(), moved, refused);
        System.out.printf(Locale.ROOT,
                "  back at the press pixel, the centre is %.6f degrees"
                        + " from where it started%n", home);
        System.out.println(home < 1.0e-6
                ? "  which is the same page, to a millionth of a degree."
                : "  ** the gesture did not return the page **");
    }

    /** The rungs that are not the globe's, held as they were. */
    private static void otherRungs() {
        System.out.println();
        System.out.println("## The other rungs, unchanged");
        System.out.println();
        System.out.println("The same measurement on the pages a"
                + " reader had before the globe. Their planes have");
        System.out.println("no edge, so a pointer anywhere on the"
                + " paper is sky and no step is refused.");
        System.out.println();
        System.out.printf(Locale.ROOT, "  %8s %10s %12s %12s %10s%n",
                "field", "drawn by", "worst drift", "deg per px",
                "refused");

        for (double field : new double[] {42.0, 120.0, 180.0}) {
            double worst = 0.0;
            double perPixel = 0.0;
            int refused = 0;
            for (double radius : RADII) {
                for (double step : STEPS) {
                    Dragged dragged = drag(field, radius, step);
                    if (dragged.refused()) {
                        refused++;
                        continue;
                    }
                    worst = Math.max(worst, dragged.driftPx());
                    if (radius == 0.0 && step == 1.0) {
                        perPixel = dragged.movedDegrees();
                    }
                }
            }
            System.out.printf(Locale.ROOT,
                    "  %7.0f° %10s %11.3f %12.4f %10d%n", field,
                    ChartProjection.forField(field).displayName(),
                    worst, perPixel, refused);
        }
    }

    /** What one drag did. */
    private record Dragged(boolean refused, double driftPx,
                           double movedDegrees, boolean constrained,
                           boolean ambiguous) {
    }

    private static Dragged drag(double field, double radius,
                                double stepPx) {
        return drag(field, radius, stepPx, 0.0);
    }

    /**
     * One drag, exactly as the interaction makes it.
     *
     * <p>The grab is taken at the press pixel and held; the target is
     * the pointer's plane point on the page being looked at, which is
     * what {@code mouseDragged} passes. Whether the page moved is the
     * controller's answer, and the drift is measured afterwards by
     * asking where the grabbed sky now lands.
     */
    private static Dragged drag(double field, double radius,
                                double stepPx, double bearingDegrees) {
        ChartViewState start = new ChartViewState(SAGITTARIUS, field, 5.0);
        ChartViewport page = viewportFor(start);
        // From the state, which names its own projection and is
        // refused if it disagrees with its field - not from the
        // viewport, because only DrawnPage.of may work a projection
        // out from viewport state, and a study asking again would be
        // the fourteenth copy that rule exists to catch (#335).
        Projection projection = drawnBy(start);
        ViewportMapping mapping = new ViewportMapping(page, projection);

        double reach = Double.isFinite(projection.visiblePlaneRadius())
                ? DISC_PX : WIDE_PX / 2.0 - 2.0;
        double bearing = Math.toRadians(bearingDegrees);
        PixelPoint press = new PixelPoint(
                WIDE_PX / 2.0 + radius * reach * Math.cos(bearing),
                HIGH_PX / 2.0 + radius * reach * Math.sin(bearing));
        var grabbedAt = PanSolver.skyAt(page,
                PanSolver.planeFromPixel(page, press));
        if (grabbedAt.isEmpty()) {
            return new Dragged(true, 0.0, 0.0, false, false);
        }
        SkyPosition grabbed = grabbedAt.get();

        // Towards the centre, so the step is a drag a reader could
        // make from here rather than one that leaves the page.
        double towardsX = WIDE_PX / 2.0 - press.x();
        double towardsY = HIGH_PX / 2.0 - press.y();
        double length = Math.hypot(towardsX, towardsY);
        PixelPoint to = length == 0.0
                ? new PixelPoint(press.x() + stepPx, press.y())
                : new PixelPoint(press.x() + stepPx * towardsX / length,
                        press.y() + stepPx * towardsY / length);

        PlanePoint target = PanSolver.planeFromPixel(page, to);
        var solution = PanSolver.solveCentre(page.projection(), grabbed,
                target, start.centre());

        ChartViewController controller = new ChartViewController();
        controller.recenter(start.centre(), field);
        if (!controller.pan(grabbed, target)) {
            return new Dragged(true, 0.0, 0.0, solution.constrained(),
                    solution.ambiguous());
        }

        // Where the grabbed sky landed on the page the drag made.
        ChartViewport after = viewportFor(controller.state());
        Projection afterProjection = drawnBy(controller.state());
        var landedAt = afterProjection.project(grabbed);
        double drift = landedAt.isEmpty() ? Double.NaN
                : distance(new ViewportMapping(after, afterProjection)
                        .toPixel(landedAt.get()), to);
        return new Dragged(false, drift,
                start.centre().separationDegrees(
                        controller.state().centre()),
                solution.constrained(), solution.ambiguous());
    }

    private static double distance(PixelPoint one, PixelPoint other) {
        return Math.hypot(one.x() - other.x(), one.y() - other.y());
    }

    /** The projection a state names, asked of the registry once. */
    private static Projection drawnBy(ChartViewState state) {
        return Projections.of(state.projection(), state.centre());
    }

    private static ChartViewport viewportFor(ChartViewState state) {
        return new ChartViewport(state.centre(),
                state.fieldWidthDegrees(), WIDE_PX, HIGH_PX,
                state.projection());
    }
}
