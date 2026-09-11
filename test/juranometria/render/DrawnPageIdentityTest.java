package juranometria.render;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.PlaneConic;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A page drawn by a projection its viewport does not name says so
 * everywhere (Sprint 32, issue #301).
 *
 * <p>The celestial-globe gate needs production pages drawn by a
 * projection the enum has no value for, so for the length of the gate
 * a page exists whose viewport <em>kind</em> is not what drew it. The
 * mismatch is the point of the door; what must not happen is a page
 * drawn as one thing and named as another, which the atlas already
 * shipped once - every exported sheet said "gnomonic" until #300.
 *
 * <p>So this builds the mismatch deliberately - a viewport that says
 * stereographic, a page drawn by something else - and requires the
 * geometry, the title block, the accessible description and the
 * exported sheet's metadata to agree with the projection that drew
 * it, because they read it from the same value.
 *
 * <p><strong>#329 owns the removal.</strong> When orthographic is a
 * production projection paired with a real field, no page can be
 * built this way and this test should be replaced by the pairing
 * rather than deleted.
 */
class DrawnPageIdentityTest {

    private static final SkyPosition CENTRE = new SkyPosition(83.0, -1.0);

    /**
     * A stand-in for the gate's orthographic projection: it is not
     * the one the viewport names, it answers to its own name, and its
     * geometry is its own.
     */
    private static class NotWhatTheViewportSays
            implements Projection {

        @Override
        public String name() {
            return "orthographic";
        }

        @Override
        public SkyPosition centre() {
            return CENTRE;
        }

        @Override
        public Optional<PlanePoint> project(SkyPosition position) {
            double separation = CENTRE.separationDegrees(position);
            if (separation > 90.0) {
                return Optional.empty();
            }
            return Optional.of(new PlanePoint(
                    Math.sin(Math.toRadians(separation)), 0.0));
        }

        @Override
        public Optional<SkyPosition> unproject(PlanePoint point) {
            return Optional.empty();
        }

        @Override
        public double planeRadius(double angleDegrees) {
            return angleDegrees > 90.0 ? Double.NaN
                    : Math.sin(Math.toRadians(angleDegrees));
        }

        @Override
        public double angleAtPlaneRadius(double planeRadius) {
            return planeRadius > 1.0 ? Double.NaN
                    : Math.toDegrees(Math.asin(planeRadius));
        }

        @Override
        public double limitDegrees() {
            return 90.0;
        }

        @Override
        public double usefulCornerDegrees() {
            return 90.0;
        }

        @Override
        public double visiblePlaneRadius() {
            return 1.0;
        }

        @Override
        public Optional<PlaneConic> greatCircle(SkyPosition pole) {
            return Optional.empty();
        }
    }

    private static ChartScene sceneSayingStereographic() {
        ChartViewport viewport = new ChartViewport(CENTRE, 180.0,
                900, 700, ChartProjection.STEREOGRAPHIC);
        return new ChartScene(viewport, List.of(), List.of(),
                "Orion", 6.0, null);
    }

    private static DrawnPage mismatchedPage() {
        return new DrawnPage(sceneSayingStereographic(),
                new NotWhatTheViewportSays());
    }

    @Test
    void theViewportAndTheProjectionReallyDoDisagree() {
        // The premise. Without this the other three could pass by
        // accident on a page where both answers were the same word.
        DrawnPage page = mismatchedPage();
        assertEquals("stereographic",
                page.scene().viewport().projection().displayName(),
                "the viewport says stereographic");
        assertEquals("orthographic", page.projectionName(),
                "and the page is drawn by something else - which is"
                        + " the situation the door exists for");
    }

    @Test
    void aPageCannotBeDrawnAboutSomewhereElse() {
        // Review of #335. Refusing nulls was not enough: a scene
        // centred on Orion carrying a projection centred on M31 was
        // accepted, and every mark would have been placed around M31
        // while the title, the description, the query and the sheet
        // all said Orion. One page, two geometric identities - the
        // thing this value exists to make impossible.
        ChartScene orion = sceneSayingStereographic();
        SkyPosition m31 = new SkyPosition(10.68, 41.27);
        Projection elsewhere = new NotWhatTheViewportSays() {
            @Override
            public SkyPosition centre() {
                return m31;
            }
        };

        IllegalArgumentException refused = assertThrows(
                IllegalArgumentException.class,
                () -> new DrawnPage(orion, elsewhere),
                "a page drawn about M31 is not a page centred on"
                        + " Orion, whatever its viewport says");
        assertTrue(refused.getMessage().contains("degrees apart"),
                "and it says how far apart they are, because the"
                        + " number is the evidence: "
                        + refused.getMessage());

        // The same place written another way is the same place: a
        // centre travels through solvers and round trips, and the
        // projections agree to 3.3e-13 degrees.
        SkyPosition sameAgain = new SkyPosition(
                CENTRE.raDegrees() + 1e-12,
                CENTRE.decDegrees() - 1e-12);
        new DrawnPage(orion, new NotWhatTheViewportSays() {
            @Override
            public SkyPosition centre() {
                return sameAgain;
            }
        });
    }

    @Test
    void theGeometryIsTheProjectionThatDrewIt() {
        DrawnPage page = mismatchedPage();
        // A point ninety degrees from the centre is on a globe's limb
        // and has no stereographic radius anything like it. Asking
        // the page rather than the viewport is what puts the marks in
        // the right places.
        assertEquals(1.0, page.projection().planeRadius(90.0), 1e-12,
                "ninety degrees lands on the limb at radius one,"
                        + " which is the orthographic answer");
        // The far side, expressed as a place rather than as an
        // impossible declination: half a turn away in right
        // ascension and a degree the other side of the equator is
        // very nearly the antipode of the centre.
        SkyPosition farSide = new SkyPosition(263.0, 1.0);
        assertTrue(CENTRE.separationDegrees(farSide) > 90.0,
                "which really is over the horizon of this globe");
        assertTrue(page.projection().project(farSide).isEmpty(),
                "and what is over the horizon of the globe does not"
                        + " project at all");
    }

    @Test
    void theTitleBlockNamesTheProjectionThatDrewIt() {
        DrawnPage page = mismatchedPage();
        BufferedImage canvas = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            ChartRenderer renderer = ChartRenderer.drawing(page,
                    StarSizePolicy.DEFAULT);
            java.awt.Rectangle box = ChartRenderer.titleBlockLayout(
                    g.getFontMetrics(), page.scene(),
                    page.projectionName());
            assertTrue(box != null && box.width > 0,
                    "the block is sized from the words it will carry");
            assertThrows(IllegalArgumentException.class,
                    () -> renderer.render(g, sceneSayingStereographic()),
                    "and a renderer told one page's projection refuses"
                            + " to draw a different page with it");
        } finally {
            g.dispose();
        }
    }

    @Test
    void theAccessibleDescriptionNamesTheProjectionThatDrewIt() {
        String said = mismatchedPage().describe();
        assertTrue(said.contains("orthographic projection"),
                "a reader who cannot see the page is told what drew"
                        + " it: " + said);
        assertTrue(!said.contains("stereographic"),
                "and not what the viewport happens to name: " + said);
    }
}
