package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.ChartViewState;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void magnitudeTransitionsFlowThrough() {
        ChartViewController controller = new ChartViewController();
        controller.decreaseMagnitudeLimit();
        assertEquals(7.0, controller.state().limitingMagnitude());
        controller.increaseMagnitudeLimit();
        assertEquals(8.0, controller.state().limitingMagnitude());
    }
}
