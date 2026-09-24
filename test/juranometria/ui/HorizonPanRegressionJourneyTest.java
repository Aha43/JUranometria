package juranometria.ui;

import java.awt.image.BufferedImage;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The horizon-pan regression, reproduced deterministically
 * (fix/horizon-pan-regression).
 *
 * <p>The owner's report, made mechanical: for a fixed observer,
 * dragging may move the horizon across the page but must not change
 * its relationship to the stars - and the owner could drag it
 * through Polaris while the event thread threw the #301 limb guard.
 *
 * <p>These tests assert the CORRECT behaviour and are expected to
 * fail on this tree: they are the reproduction the repair must turn
 * green, and the investigation identified the failing layer as
 * {@code juranometria.project} - two distinct defects:
 *
 * <ul>
 *   <li><b>The fold ghost</b> (pre-existing since #298, byte-identical
 *       on 7fd6394): the closed-form conic route draws the whole
 *       ellipse of a great circle, which on an orthographic page is
 *       the image of BOTH hemispheres - so the far, invisible half
 *       of the horizon is inked over the near-side sky, and slides
 *       across the stars as the reader drags. Point-projection
 *       refuses far-side points; the conic route never asks.</li>
 *   <li><b>The degeneracy band</b>: with the circle's pole within
 *       about 0.15 degrees of ninety from the page centre, the conic
 *       is neither substituted (the DEGENERATE_CONIC gate does not
 *       reach it) nor extracted within the limb guard's 1e-7 graze -
 *       ViewportMapping.onPage tolerates 1e-3 page units near the
 *       degeneracy by design, and PlaneCurve's guard refuses at
 *       4.1e-5. A horizon dragged across the page centre crosses the
 *       band every time.</li>
 * </ul>
 */
class HorizonPanRegressionJourneyTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    /** The observer whose horizon is dragged; fixed for the session. */
    private static final Observer OSLO = new Observer(59.9, 10.7,
            Instant.parse("2026-03-20T21:33:00Z"));

    /** Polaris, the star the owner dragged the horizon through. */
    private static final SkyPosition POLARIS =
            new SkyPosition(37.954, 89.264);

    /**
     * A page centre solved so that one far-hemisphere point of this
     * observer's horizon folds onto exactly Polaris' pixel, while
     * Polaris itself is on the visible side (60 degrees from centre).
     * Solved from s = polaris - 2 (polaris . c) c with s on the
     * horizon; residual 1e-5.
     */
    private static final SkyPosition FOLD_CENTRE =
            new SkyPosition(152.686364, 30.171839);

    private ChartComponent chart;
    private MeridianModule module;

    private ChartComponent globe(SkyPosition centre) {
        chart = new ChartComponent(Atlas.assembler(), ENGLISH);
        chart.setSize(920, 920);
        chart.setViewState(new ChartViewState(centre, 180.0, 4.0));
        ChartModuleHost host = new ChartModuleHost(chart,
                new juranometria.chart.SelectionModel(), request -> { });
        module = host.attach(new MeridianModule(OSLO));
        module.showing(false, true, false);
        return chart;
    }

    private BufferedImage painted() {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    @Test
    void theHorizonIsNeverDrawnThroughPolaris() {
        // Polaris stands 59.7 degrees above this observer's horizon,
        // always: at the fold centre, any horizon ink at Polaris'
        // own pixel is ink from the far, invisible half of the
        // horizon - the ghost the reader watched slide through it.
        globe(FOLD_CENTRE);
        BufferedImage with = painted();
        module.showing(false, false, false);
        BufferedImage without = painted();

        var mapping = new ViewportMapping(chart.currentScene().viewport(),
                Projections.forViewport(chart.currentScene().viewport()));
        PixelPoint at = Projections
                .forViewport(chart.currentScene().viewport())
                .project(POLARIS).map(mapping::toPixel).orElseThrow();
        int px = (int) Math.round(at.x());
        int py = (int) Math.round(at.y());

        int nearPolaris = 0;
        for (int y = py - 6; y <= py + 6; y++) {
            for (int x = px - 6; x <= px + 6; x++) {
                if (with.getRGB(x, y) != without.getRGB(x, y)) {
                    nearPolaris++;
                }
            }
        }
        assertEquals(0, nearPolaris,
                "the horizon is 59.7 degrees from Polaris for this"
                        + " observer, so no horizon ink may fall"
                        + " within 6 px of Polaris' pixel - the "
                        + nearPolaris + " px found are the far"
                        + " hemisphere's fold, drawn over the near"
                        + " sky");
    }

    @Test
    void aDragAcrossThePageCentreNeverThrowsOnThePaintPath() {
        // The gesture the owner made, reduced to what the mouse
        // reduces to: one grab, then the controller's atomic pan to
        // a target that creeps across the plane, carrying the
        // horizon's pole-to-centre angle through ninety degrees -
        // the conic degeneracy. Every frame is painted, as the event
        // thread paints it.
        LocalSky sky = new LocalSky(OSLO);
        SkyPosition pole = sky.zenith();
        // Start with the pole 89.8 degrees from centre, drag so the
        // angle sweeps past 90.2: rotate the pole by theta in a
        // fixed plane.
        SkyPosition start = away(pole, 89.80);
        globe(start);

        ChartViewController controller =
                new ChartViewController(Atlas.assembler()::fits);
        controller.onChange(chart::setViewState);
        controller.recenter(start, 180.0);

        PixelPoint press = new PixelPoint(460.0, 300.0);
        PlanePoint plane0 = PanSolver.planeFromPixel(
                chart.currentScene().viewport(), press);
        SkyPosition grabbed = PanSolver.skyAt(
                chart.currentScene().viewport(), plane0).orElseThrow();

        double closest = 180.0;
        for (int step = 1; step <= 800; step++) {
            // Both drag directions, so the sweep cannot miss the
            // band whichever way this page's frame is turned.
            double d = step * 2.0e-5;
            for (PlanePoint target : new PlanePoint[] {
                    new PlanePoint(plane0.xiEast() + d,
                            plane0.etaNorth()),
                    new PlanePoint(plane0.xiEast(),
                            plane0.etaNorth() + d)}) {
                controller.pan(grabbed, target);
                double theta = pole.separationDegrees(
                        chart.currentScene().viewport().centre());
                closest = Math.min(closest, Math.abs(theta - 90.0));
                try {
                    painted();
                } catch (RuntimeException thrown) {
                    throw new AssertionError(
                            "the drag reached pole-to-centre "
                                    + String.format(java.util.Locale.ROOT,
                                            "%.4f", theta)
                                    + " degrees and painting threw: "
                                    + thrown.getMessage(), thrown);
                }
            }
        }
        // The journey's own premise: the sweep really crossed the
        // degeneracy, or this test would prove nothing.
        assertTrue(closest < 0.02,
                "the drag crossed the degeneracy band (closest "
                        + closest + " degrees from ninety)");
    }

    /** A position a stated angle from the pole, in a fixed plane. */
    private static SkyPosition away(SkyPosition pole, double degrees) {
        double ra = Math.toRadians(pole.raDegrees());
        double dec = Math.toRadians(pole.decDegrees());
        double[] p = {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
        double[] axis = {0, 0, 1};
        double[] u = {axis[1] * p[2] - axis[2] * p[1],
                axis[2] * p[0] - axis[0] * p[2],
                axis[0] * p[1] - axis[1] * p[0]};
        double n = Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        double t = Math.toRadians(degrees);
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            c[i] = Math.cos(t) * p[i] + Math.sin(t) * u[i] / n;
        }
        double outRa = Math.toDegrees(Math.atan2(c[1], c[0]));
        if (outRa < 0) {
            outRa += 360.0;
        }
        return new SkyPosition(outRa, Math.toDegrees(Math.asin(
                Math.max(-1.0, Math.min(1.0, c[2])))));
    }
}
