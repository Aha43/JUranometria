package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChartComponentTest {

    // Regression guard: a plain JComponent subclass has no accessible
    // context by default, and setting the accessible name during
    // construction crashed the application at startup.
    @Test
    void exposesAnAccessibleContextWithAName() {
        ChartComponent component = new ChartComponent(
                new SkyPosition(10.684708, 41.268750),
                "Test chart", List.of(), List.of());
        assertNotNull(component.getAccessibleContext());
        assertEquals("Star chart", component.getAccessibleContext().getAccessibleName());
    }

    @Test
    void startsAtTheDefaultViewStateAndAdoptsNewOnes() {
        ChartComponent component = new ChartComponent(
                new SkyPosition(10.684708, 41.268750),
                "Test chart", List.of(), List.of());
        assertEquals(ChartViewState.DEFAULT, component.viewState());
        component.setViewState(ChartViewState.DEFAULT.zoomIn());
        assertEquals(6.0, component.viewState().fieldWidthDegrees());
    }
}
