package juranometria.app;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The view report's selection line names an occurrence whole (#513). */
class ViewReportOccurrenceTest {

    @Test
    void anOccurrenceIsReportedWithItsInstantObserverAndPlace() {
        Selection.Occurrence moon = new Selection.Occurrence("moon",
                Instant.parse("2026-03-26T10:32:00Z"), 59.913, 10.752,
                new SkyPosition(40.5, -15.25));
        assertEquals("occurrence moon at 2026-03-26T10:32:00Z for latitude 59.913;"
                + " longitude 10.752; RA 40.5; Dec -15.25", ViewReport.selection(moon));
        assertEquals("none", ViewReport.selection(Selection.NOTHING));
    }
}
