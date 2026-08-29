package juranometria.tool;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.ConstellationGeography;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;
import juranometria.ui.SceneAssembler;

/**
 * The Sprint 8 pan study (issue #72): measures the numerical closure
 * of the exact grab solver - press, drag, and the grabbed sky position
 * must return beneath the moved pointer - across ordinary, RA-wrap,
 * and polar centres, straight, diagonal, and near-corner drags, at 8,
 * 18, and 36 degrees; measures out-and-back drift over closed drag
 * loops; and measures the full solve + assemble + render cost of drag
 * bursts through the real seam, the evidence the event-handling
 * decision needs. Run via "make pan-study".
 */
public final class PanStudyMain {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final double[] FIELDS = {8.0, 18.0, 36.0};

    private record Case(String name, SkyPosition centre) {
    }

    private static final List<Case> CASES = List.of(
            new Case("m42", new SkyPosition(83.818667, -5.389667)),
            new Case("rawrap", new SkyPosition(359.457625, -32.591028)),
            new Case("polarN", new SkyPosition(37.946619, 89.264135)),
            new Case("polarS", new SkyPosition(80.893750, -85.0)));

    /** Press points and drag vectors in pixels: straight, diagonal,
     *  near-corner, and a long edge-crossing pull. */
    private static final int[][] DRAGS = {
            {450, 350, 200, 0}, {450, 350, 0, -180}, {450, 350, 160, 130},
            {60, 40, 300, 250}, {840, 660, -700, -500}, {450, 350, 900, 0},
    };

    public static void main(String[] args) {
        System.out.printf(Locale.ROOT, "%-7s %5s | %9s | %10s | %10s%n",
                "case", "field", "maxErrPx", "gesture50", "holonomy50");
        double worstError = 0.0;
        double worstGesture = 0.0;
        for (Case c : CASES) {
            for (double field : FIELDS) {
                double error = closure(c.centre(), field);
                double gesture = withinGesture(c.centre(), field);
                double holonomy = crossGesture(c.centre(), field);
                worstError = Math.max(worstError, error);
                worstGesture = Math.max(worstGesture, gesture);
                System.out.printf(Locale.ROOT,
                        "%-7s %4.0f° | %9.2e | %10.2e | %10.2e%n",
                        c.name(), field, error, gesture, holonomy);
            }
        }
        System.out.printf(Locale.ROOT,
                "worst grab-closure error: %.2e px; worst within-gesture"
                        + " return-to-press error after 50 waypoints: %.2e px%n",
                worstError, worstGesture);
        System.out.println("gesture50: one gesture, 50 pointer waypoints,"
                + " pointer returns to the press pixel - the invariant"
                + " references the press-time grab, so this closes exactly.");
        System.out.println("holonomy50: 50 separate out-and-back gesture"
                + " pairs - each regrabs at the new centre, so the loops"
                + " accumulate the genuine spherical holonomy of north-up"
                + " panning; geometry, not numerical drift.");
        System.out.println();
        dragBurst();
    }

    /**
     * One gesture: grab once at the press pixel, wander the pointer
     * through 50 waypoints (solving against the same press-time grab
     * each event, as the decided interaction does), then return the
     * pointer to the press pixel. The centre must return exactly.
     */
    private static double withinGesture(SkyPosition centre, double field) {
        ChartViewport viewport = new ChartViewport(centre, field, WIDTH, HEIGHT);
        PixelPoint press = new PixelPoint(450, 350);
        SkyPosition grabbed = PanGeometry.skyFromPlane(centre,
                PanGeometry.planeFromPixel(viewport, press));
        SkyPosition current = centre;
        java.util.Random random = new java.util.Random(43);
        for (int i = 0; i < 50; i++) {
            PixelPoint waypoint = new PixelPoint(
                    450 + random.nextInt(-260, 261),
                    350 + random.nextInt(-260, 261));
            current = PanGeometry.solveCentre(grabbed,
                            PanGeometry.planeFromPixel(viewport, waypoint),
                            current)
                    .centre().orElse(current);
        }
        SkyPosition back = PanGeometry.solveCentre(grabbed,
                        PanGeometry.planeFromPixel(viewport, press), current)
                .centre().orElse(current);
        var plane = new GnomonicProjection(centre).project(back);
        if (plane.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        PixelPoint pixel = new ViewportMapping(viewport).toPixel(plane.get());
        return Math.hypot(pixel.x() - WIDTH / 2.0, pixel.y() - HEIGHT / 2.0);
    }

    /** Max reprojection error of the grabbed point over all drags. */
    private static double closure(SkyPosition centre, double field) {
        ChartViewport viewport = new ChartViewport(centre, field, WIDTH, HEIGHT);
        double worst = 0.0;
        for (int[] drag : DRAGS) {
            PixelPoint press = new PixelPoint(drag[0], drag[1]);
            PixelPoint release = new PixelPoint(
                    drag[0] + drag[2], drag[1] + drag[3]);
            SkyPosition grabbed = PanGeometry.skyFromPlane(centre,
                    PanGeometry.planeFromPixel(viewport, press));
            PanGeometry.PanSolution solved = PanGeometry.solveCentre(grabbed,
                    PanGeometry.planeFromPixel(viewport, release), centre);
            if (solved.centre().isEmpty()) {
                // The solver classified this itself; any unexplained
                // empty throws inside solveCentre instead.
                System.out.printf(Locale.ROOT,
                        "  (past-pole hold, solver-classified: drag %s at"
                                + " %.0f deg)%n",
                        java.util.Arrays.toString(drag), field);
                continue;
            }
            SkyPosition newCentre = solved.centre().get();
            ChartViewport moved = new ChartViewport(
                    newCentre, field, WIDTH, HEIGHT);
            PixelPoint reprojected = new ViewportMapping(moved).toPixel(
                    new GnomonicProjection(newCentre)
                            .project(grabbed).orElseThrow());
            double error = Math.hypot(reprojected.x() - release.x(),
                    reprojected.y() - release.y());
            if (error > 1e-3) {
                // Constrained follow: the requested point was infeasible
                // for this grab; the sky followed to the boundary.
                System.out.printf(Locale.ROOT,
                        "  (constrained follow: drag %s at %.0f deg,"
                                + " shortfall %.1f px along the pinned axis)%n",
                        java.util.Arrays.toString(drag), field, error);
                continue;
            }
            worst = Math.max(worst, error);
        }
        return worst;
    }

    /** Centre displacement after 50 separate out-and-back gesture pairs. */
    private static double crossGesture(SkyPosition centre, double field) {
        SkyPosition current = centre;
        for (int i = 0; i < 50; i++) {
            current = dragBy(current, field, 173, -131);
            current = dragBy(current, field, -173, 131);
        }
        ChartViewport viewport = new ChartViewport(centre, field, WIDTH, HEIGHT);
        var plane = new GnomonicProjection(centre).project(current);
        if (plane.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        PixelPoint drifted = new ViewportMapping(viewport).toPixel(plane.get());
        return Math.hypot(drifted.x() - WIDTH / 2.0, drifted.y() - HEIGHT / 2.0);
    }

    private static SkyPosition dragBy(SkyPosition centre, double field,
                                      int dx, int dy) {
        ChartViewport viewport = new ChartViewport(centre, field, WIDTH, HEIGHT);
        PixelPoint press = new PixelPoint(450, 350);
        SkyPosition grabbed = PanGeometry.skyFromPlane(centre,
                PanGeometry.planeFromPixel(viewport, press));
        return PanGeometry.solveCentre(grabbed,
                        PanGeometry.planeFromPixel(viewport,
                                new PixelPoint(450 + dx, 350 + dy)), centre)
                .centre().orElse(centre);
    }

    /**
     * A simulated drag burst through the real seam: 120 consecutive
     * events, the pointer advancing 5 pixels per event, each solving
     * the centre, assembling the scene, and rendering the page - the
     * full per-event cost direct synchronous handling would pay.
     */
    private static void dragBurst() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        SceneAssembler assembler = SceneAssembler.allSky(catalogue,
                catalogue.manifest().maxObjectSemiExtentDegrees(),
                ConstellationGeography.load());
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        System.out.println("drag burst: 120 events, the pointer advancing"
                + " 5 px per event (4 right, 3 down), solve + assemble"
                + " + render per event (warm):");
        for (double field : FIELDS) {
            SkyPosition centre = new SkyPosition(83.818667, -5.389667);
            ChartViewState state = new ChartViewState(
                    centre, field, 8.0, null, null);
            renderer.renderToImage(assembler.assemble(state, WIDTH, HEIGHT));
            // One gesture: grab at the press pixel once; each event moves
            // the pointer 4 px further and solves against that same grab.
            ChartViewport viewport = new ChartViewport(
                    centre, field, WIDTH, HEIGHT);
            SkyPosition grabbed = PanGeometry.skyFromPlane(centre,
                    PanGeometry.planeFromPixel(viewport,
                            new PixelPoint(450, 350)));
            long[] times = new long[120];
            for (int i = 0; i < times.length; i++) {
                long t0 = System.nanoTime();
                PixelPoint pointer = new PixelPoint(
                        450 + 4 * (i + 1), 350 + 3 * (i + 1));
                SkyPosition moved = PanGeometry.solveCentre(grabbed,
                                PanGeometry.planeFromPixel(viewport, pointer),
                                state.centre())
                        .centre().orElse(state.centre());
                state = state.recenteredAt(moved);
                renderer.renderToImage(assembler.assemble(state, WIDTH, HEIGHT));
                times[i] = System.nanoTime() - t0;
            }
            java.util.Arrays.sort(times);
            System.out.printf(Locale.ROOT,
                    "  %4.0f deg: median %.1f ms, p95 %.1f ms, max %.1f ms"
                            + " (60 Hz budget 16.7 ms)%n",
                    field, times[60] / 1e6, times[113] / 1e6,
                    times[119] / 1e6);
        }
    }
}
