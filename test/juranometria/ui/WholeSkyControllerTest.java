package juranometria.ui;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.HammerProjection;
import juranometria.project.PlanePoint;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole sky as a mode (#687, ruled R-8 to R-10 and R-13 on #686):
 * entered and left by one switch, its ordinary page kept and restored
 * exactly, Open here into the field the reader left, zoom and pan
 * refused, Home the ordinary Home, the central meridian following the
 * observer - and never persisted.
 */
class WholeSkyControllerTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final SkyPosition SIRIUS = new SkyPosition(101.287, -16.716);
    private static final SkyPosition CENTRE = HammerProjection.centreFor(65.0);

    @Test
    void enteringKeepsTheOrdinaryViewAndLeavingRestoresItExactly() {
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 24.0);
        ChartViewState before = controller.state();
        assertFalse(controller.wholeSky());
        controller.enterWholeSky(CENTRE);
        ChartViewState whole = controller.state();
        assertTrue(whole.wholeSky());
        assertEquals(ChartProjection.HAMMER, whole.projection());
        assertEquals(CENTRE, whole.centre());
        assertEquals(5.0, whole.limitingMagnitude(),
                "the brighter of the reader's V 8.0 and the whole sky's V 5.0");
        assertEquals(ChartProjection.WHOLE_SKY_DEGREES, whole.fieldWidthDegrees());
        controller.leaveWholeSky();
        assertEquals(before, controller.state(), "the ordinary page, exactly as left");
        controller.leaveWholeSky();
        assertEquals(before, controller.state(), "and leaving twice changes nothing");
    }

    @Test
    void openHereLeavesForTheOrdinaryChartAtTheFieldLeft() {
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 60.0);
        controller.enterWholeSky(CENTRE);
        controller.recenter(SIRIUS);
        ChartViewState opened = controller.state();
        assertFalse(opened.wholeSky());
        assertEquals(60.0, opened.fieldWidthDegrees(), "the field the reader left");
        assertEquals(SIRIUS, opened.centre());
        assertEquals(ChartProjection.STEREOGRAPHIC, opened.projection());
        assertTrue(opened.targetLabel() == null, "anonymous, titled by its coordinates");
        // And with a named target, the same leaving.
        controller.enterWholeSky(CENTRE);
        controller.recenter(SIRIUS, "Sirius", "TYC 5949-2777-1");
        assertEquals("Sirius", controller.state().targetLabel());
        assertEquals(60.0, controller.state().fieldWidthDegrees());
        // A recentre with its own field (a bookmark, Centre on chart's
        // rule) takes that field.
        controller.enterWholeSky(CENTRE);
        controller.recenter(SIRIUS, 8.0);
        assertEquals(8.0, controller.state().fieldWidthDegrees());
        assertFalse(controller.state().wholeSky());
    }

    @Test
    void zoomAndPanAreRefusedOnTheWholeSky() {
        ChartViewController controller = new ChartViewController();
        controller.enterWholeSky(CENTRE);
        ChartViewState whole = controller.state();
        assertFalse(controller.canZoomIn());
        assertFalse(controller.canZoomOut());
        assertTrue(controller.zoomInTarget().isEmpty());
        controller.zoomIn();
        controller.zoomOut();
        assertEquals(whole, controller.state(), "the ladder does not walk here");
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new PlanePoint(0.3, 0.1), true, 2000, 1000));
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new PlanePoint(0.0, 0.0), false, 2000, 1000));
        assertFalse(controller.pan(SIRIUS, new PlanePoint(0.5, 0.2)), "no drag pans");
        assertEquals(whole, controller.state());
        // The magnitude limit is still the reader's.
        controller.increaseMagnitudeLimit();
        assertEquals(6.0, controller.state().limitingMagnitude());
        assertTrue(controller.state().wholeSky());
    }

    @Test
    void theCentralMeridianFollowsTheObserverAndTheReturnIsKept() {
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 24.0);
        ChartViewState before = controller.state();
        controller.enterWholeSky(CENTRE);
        SkyPosition later = HammerProjection.centreFor(100.0);
        controller.followObserver(later);
        assertTrue(controller.state().wholeSky());
        assertEquals(later, controller.state().centre());
        // Entering again while shown moves the meridian too.
        controller.enterWholeSky(HammerProjection.centreFor(120.0));
        assertEquals(HammerProjection.centreFor(120.0), controller.state().centre());
        controller.leaveWholeSky();
        assertEquals(before, controller.state(), "the first ordinary page, not a later one");
        // Following an ordinary page does nothing.
        controller.followObserver(later);
        assertEquals(before, controller.state());
    }

    @Test
    void homeIsTheOrdinaryHome() {
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 24.0);
        controller.enterWholeSky(CENTRE);
        controller.reset();
        assertEquals(ChartViewState.DEFAULT, controller.state());
        assertFalse(controller.wholeSky());
    }

    @Test
    void theSessionDerivesTheCentreFromTheZenithsEclipticLongitude() {
        Observer oslo = new Observer(59.91, 10.75, Instant.parse("2026-12-15T21:00:00Z"));
        SkyPosition centre = WholeSkySession.centreFor(oslo);
        Ecliptic.Coordinates c = Ecliptic.toEcliptic(centre);
        assertEquals(0.0, c.latitudeDegrees(), 1e-9, "a point of the ecliptic");
        SkyPosition zenith = new LocalSky(oslo).zenith();
        assertEquals(Ecliptic.toEcliptic(zenith).longitudeDegrees(), c.longitudeDegrees(), 1e-9,
                "at the zenith's ecliptic longitude: the zenith is on the central"
                        + " meridian, not at the centre");
        assertTrue(Math.abs(Ecliptic.toEcliptic(zenith).latitudeDegrees()) > 30.0,
                "from Oslo the zenith stands well off the middle line");

        ChartViewController controller = new ChartViewController();
        Observer[] now = {oslo};
        WholeSkySession session = new WholeSkySession(controller, () -> now[0]);
        assertFalse(session.on());
        session.enter();
        assertTrue(session.on());
        assertEquals(centre, controller.state().centre());
        // Chart time moves: so does the central meridian.
        now[0] = oslo.at(Instant.parse("2026-12-16T03:00:00Z"));
        session.observerChanged();
        assertEquals(WholeSkySession.centreFor(now[0]), controller.state().centre());
        session.toggle();
        assertFalse(session.on());
        assertEquals(ChartViewState.DEFAULT, controller.state());
        // Without an observer the whole sky cannot be entered.
        now[0] = null;
        session.enter();
        assertFalse(session.on());
    }

    @Test
    void aWholeSkyStateIsNeverRecentredAndNothingElseFollows() {
        ChartViewState whole = ChartViewState.wholeSky(CENTRE, 8.0);
        assertThrows(IllegalStateException.class, () -> whole.recenteredAt(SIRIUS));
        assertThrows(IllegalStateException.class,
                () -> new ChartViewState(ORION, 24.0, 6.0).wholeSkyAbout(CENTRE));
        assertEquals(8.0, whole.withFieldWidth(8.0).fieldWidthDegrees(), "left by a field");
        assertFalse(whole.withFieldWidth(8.0).wholeSky());
        assertEquals(ChartViewState.DEFAULT, whole.reset());
        assertTrue(whole.overview(), "not the atlas's own projection");
    }
}
