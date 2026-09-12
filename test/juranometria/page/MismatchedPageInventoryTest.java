package juranometria.page;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.PlaneConic;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A page keeps its projection all the way down (Sprint 32, issue
 * #301; review of PR #335).
 *
 * <p>The source scan counts where a page is <em>built</em>, and that
 * cannot see an <em>indirect</em> rebuild: a method that takes a page,
 * hands a scene to a helper, and lets the helper derive a projection
 * of its own. The inventory did exactly that - it started from the
 * page it was given and then asked
 * {@code PageExtent.onPage(scene, dso)} for every extended object, so
 * on a globe the stars were inventoried orthographically while the
 * galaxies were tested by a projection nothing had drawn with. The
 * chain ran four deep, through the page's reach and the hit test to
 * the pan solver.
 *
 * <p>So this asks about behaviour rather than about source. It builds
 * a page whose viewport says one thing and whose projection does
 * another, puts an object where the two disagree, and requires the
 * answer to come from the projection that draws the page. A rebuild
 * anywhere along the chain gives the other answer.
 */
class MismatchedPageInventoryTest {

    private static final SkyPosition CENTRE =
            new SkyPosition(266.0, -28.0);

    /**
     * A hemisphere: nothing further than ninety degrees exists, and
     * the limb is at plane radius one.
     *
     * <p>The viewport will go on saying stereographic, which shows
     * everything out to a hundred and eighty and would happily place
     * the object below.
     */
    private static final class Hemisphere implements Projection {

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
            double away = CENTRE.separationDegrees(position);
            if (away > 90.0) {
                return Optional.empty();
            }
            double radius = Math.sin(Math.toRadians(away));
            return Optional.of(new PlanePoint(radius, 0.0));
        }

        @Override
        public Optional<SkyPosition> unproject(PlanePoint point) {
            // Honest, and it matters that it is: an earlier version
            // answered the centre for every point inside the limb,
            // which made this page report a reach of zero degrees for
            // the same reason the real one did - and so hid the bug
            // this test was written to find (review of #335).
            double radius = Math.hypot(point.xiEast(), point.etaNorth());
            if (radius > 1.0) {
                return Optional.empty();
            }
            return Optional.of(CENTRE);
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

    /**
     * An extended object on the far side of the globe - a hundred and
     * twenty degrees out, which the hemisphere cannot show and the
     * viewport's stereographic projection places without complaint.
     */
    private static DeepSkyObject farSide() {
        SkyPosition beyond = new SkyPosition(
                (CENTRE.raDegrees() + 120.0) % 360.0,
                CENTRE.decDegrees());
        return new DeepSkyObject("far-side", List.of(),
                DsoType.GALAXY, beyond, 600.0, 400.0, 45.0,
                8.0, 1,
                new DeepSkyObject.Recorded(600.0, 400.0, 45.0,
                        DeepSkyObject.Recorded.Band.VISUAL));
    }

    private static DrawnPage hemispherePage(List<DeepSkyObject> objects) {
        ChartViewport viewport = new ChartViewport(CENTRE, 180.0,
                900, 900, ChartProjection.STEREOGRAPHIC);
        ChartScene scene = new ChartScene(viewport, List.of(), objects,
                "Sagittarius", 8.0, null);
        return new DrawnPage(scene, new Hemisphere());
    }

    @Test
    void theFarSideIsNotOnAHemispherePage() {
        DeepSkyObject beyond = farSide();
        assertTrue(CENTRE.separationDegrees(beyond.position()) > 90.0,
                "the fixture is on the hidden half");

        DrawnPage page = hemispherePage(List.of(beyond));

        assertFalse(PageExtent.onPage(page, beyond),
                "an object on the far side of the globe is not on the"
                        + " page - asked of the projection that draws"
                        + " it, not of the kind its viewport names");
    }

    @Test
    void aWholeHemisphereReachesToItsLimb() {
        // The arithmetic the carried page exposed. Walking the
        // paper's corners back into the sky assumes a corner is sky;
        // on a globe every corner is outside the limb, all four
        // inverse projections come back empty, and the page reports
        // that it reaches nowhere - which would exclude every object
        // on it from the inventory, including the ones it draws.
        //
        // Measured against the real projection rather than the
        // stand-in above, because a stand-in is exactly what hid this.
        SkyPosition centre = CENTRE;
        ChartViewport viewport = new ChartViewport(centre, 180.0,
                900, 900, ChartProjection.STEREOGRAPHIC);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "Sagittarius", 8.0, null);
        DrawnPage globe = new DrawnPage(scene,
                new juranometria.tool.globe.GlobeProjection(centre));

        assertEquals(90.0, PageExtent.pageReachDegrees(globe), 1e-9,
                "a page showing a whole hemisphere reaches to its"
                        + " limb, which is ninety degrees of sky - not"
                        + " to nowhere");
    }

    @Test
    void theInventoryKeepsItsPageThroughTheLoop() {
        // The finding itself. The inventory took the page, then
        // handed each extended object to the scene-taking adapter,
        // and the object came back judged by a projection that had
        // drawn nothing on this page.
        DeepSkyObject beyond = farSide();
        DrawnPage page = hemispherePage(List.of(beyond));

        PageContents contents = PageInventory.of(page,
                ChartOptions.DEFAULTS);

        boolean listed = contents.entries().stream()
                .anyMatch(entry -> "far-side".equals(entry.identity()));
        assertFalse(listed,
                "what the globe does not show does not enter its"
                        + " inventory, however deep the call goes:"
                        + " " + contents.entries().size()
                        + " entries");
    }
}
