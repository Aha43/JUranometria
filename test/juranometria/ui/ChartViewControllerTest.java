package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartViewControllerTest {

    @Test
    void startsAtTheDefaultAndHandsItToNewListeners() {
        ChartViewController controller = new ChartViewController();
        List<ChartViewState> seen = new ArrayList<>();
        controller.onChange(seen::add);
        assertEquals(List.of(ChartViewState.DEFAULT), seen);
    }

    @Test
    void transitionsNotifyListenersWithTheNewState() {
        ChartViewController controller = new ChartViewController();
        List<Double> fieldWidths = new ArrayList<>();
        controller.onChange(state -> fieldWidths.add(state.fieldWidthDegrees()));

        controller.zoomIn();
        controller.zoomIn();
        controller.zoomOut();
        controller.reset();

        assertEquals(List.of(8.0, 6.0, 4.0, 6.0, 8.0), fieldWidths);
        assertEquals(ChartViewState.DEFAULT, controller.state());
    }

    @Test
    void boundedNoOpTransitionsNotifyNobody() {
        ChartViewController controller = new ChartViewController();
        List<ChartViewState> seen = new ArrayList<>();
        controller.onChange(seen::add);

        while (controller.state().canZoomOut()) {
            controller.zoomOut();             // walk to the 36-degree bound
        }
        int afterWalk = seen.size();
        controller.zoomOut();                 // already at 36 degrees
        controller.increaseMagnitudeLimit();  // already at V 8.0
        assertEquals(afterWalk, seen.size(),
                "bounded no-op transitions notify nobody");
        controller.reset();
        controller.reset();                   // already the default
        assertEquals(afterWalk + 1, seen.size(),
                "a repeated reset is a no-op after the first");
    }

    @Test
    void recentringNotifiesAndResetRestoresM31() {
        ChartViewController controller = new ChartViewController();
        List<ChartViewState> seen = new ArrayList<>();
        controller.onChange(seen::add);

        juranometria.chart.SkyPosition offset = new juranometria.chart.SkyPosition(12.0, 43.0);
        controller.recenter(offset);
        assertEquals(offset, controller.state().centre());
        controller.recenter(offset, 4.0);
        assertEquals(4.0, controller.state().fieldWidthDegrees());
        controller.reset();
        assertEquals(ChartViewState.DEFAULT, controller.state());
        assertEquals(4, seen.size(), "registration, two recenters, and reset notify");
    }

    @Test
    void oneAcceptedPanIsOneAtomicNotification() {
        ChartViewController controller = new ChartViewController();
        List<ChartViewState> seen = new ArrayList<>();
        controller.onChange(seen::add);

        SkyPosition before = controller.state().centre();
        assertTrue(controller.pan(before, new PlanePoint(0.02, 0.01)),
                "grabbing the centre and moving it is an accepted pan");
        assertEquals(2, seen.size(), "registration plus exactly one change");
        assertEquals(seen.get(1), controller.state());
        assertTrue(controller.state().centre().separationDegrees(before) > 0.5);
        assertEquals(ChartViewState.DEFAULT.fieldWidthDegrees(),
                controller.state().fieldWidthDegrees(),
                "field width survives the pan");
        assertEquals(ChartViewState.DEFAULT.limitingMagnitude(),
                controller.state().limitingMagnitude(),
                "magnitude survives the pan");
    }

    @Test
    void refusedPansChangeNothingAndNotifyNobody() {
        // Past-pole hold: the grabbed near-polar point cannot be pulled
        // past the pole; the solver classifies, the controller no-ops.
        ChartViewController controller = new ChartViewController();
        controller.recenter(new SkyPosition(37.946619, 89.264135));
        List<ChartViewState> seen = new ArrayList<>();
        controller.onChange(seen::add);
        assertFalse(controller.pan(controller.state().centre(),
                        new PlanePoint(0.0, -0.4)),
                "pulling the polar grab past the pole holds");
        assertEquals(1, seen.size(), "only the registration callback fired");

        // Coverage refusal: a validity predicate refusing everything.
        ChartViewController fenced = new ChartViewController(state -> false);
        List<ChartViewState> fencedSeen = new ArrayList<>();
        fenced.onChange(fencedSeen::add);
        assertFalse(fenced.pan(fenced.state().centre(),
                new PlanePoint(0.02, 0.0)));
        assertEquals(1, fencedSeen.size());
        assertEquals(ChartViewState.DEFAULT, fenced.state());
    }

    @Test
    void theFirstPanAfterASearchClearsTheTargetAtomically() {
        ChartViewController controller = new ChartViewController();
        SkyPosition m42 = new SkyPosition(83.818667, -5.389667);
        controller.recenter(m42, "M 42 · Great Orion Nebula region",
                "NGC 1976");
        assertTrue(controller.pan(m42, new PlanePoint(0.01, -0.015)));
        assertEquals(null, controller.state().targetLabel(),
                "a real pan departs the named target");
        assertEquals(null, controller.state().targetIdentity(),
                "label and identity clear together, atomically");

        // Already-anonymous views stay anonymous through further pans.
        assertTrue(controller.pan(controller.state().centre(),
                new PlanePoint(-0.02, 0.0)));
        assertEquals(null, controller.state().targetLabel());
        assertEquals(null, controller.state().targetIdentity());
    }

    @Test
    void zoomAndMagnitudeOperateAroundThePannedCentre() {
        ChartViewController controller = new ChartViewController();
        assertTrue(controller.pan(controller.state().centre(),
                new PlanePoint(0.03, -0.02)));
        SkyPosition panned = controller.state().centre();
        controller.zoomOut();
        controller.decreaseMagnitudeLimit();
        assertEquals(panned, controller.state().centre(),
                "zoom and magnitude keep the panned centre");
        assertEquals(12.0, controller.state().fieldWidthDegrees());
        assertEquals(7.0, controller.state().limitingMagnitude());

        controller.reset();
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "reset after pan restores the exact released default");
    }

    @Test
    void magnitudeTransitionsFlowThrough() {
        ChartViewController controller = new ChartViewController();
        controller.decreaseMagnitudeLimit();
        assertEquals(7.0, controller.state().limitingMagnitude());
        controller.increaseMagnitudeLimit();
        assertEquals(8.0, controller.state().limitingMagnitude());
    }
}
