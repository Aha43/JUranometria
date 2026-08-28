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

        controller.zoomOut();                 // already at 8 degrees
        controller.increaseMagnitudeLimit();  // already at V 8.0
        controller.reset();                   // already the default

        assertEquals(1, seen.size(), "only the registration callback fires");
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
