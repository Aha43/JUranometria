package juranometria.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The view report names a constrained or locked pointer drag and says
 * nothing of a free one (#691, C-5 on #690), so no report a reader has
 * already taken changes.
 */
class ViewReportPointerDragTest {

    @Test
    void hiddenFigureEndpointsAreALineOnlyWhenThereAreAny() {
        assertFalse(ViewReport.format(ViewReportTest.rich()).contains("figure-endpoints"));
        String report = ViewReport.format(
                ViewReportTest.rich().withFigureEndpointsBelowLimit(76));
        assertTrue(report.contains("\nfigure-endpoints-below-limit: 76\n"), "#571, D-4");
    }

    @Test
    void aFreeDragAddsNoLine() {
        String before = ViewReport.format(ViewReportTest.rich());
        String free = ViewReport.format(ViewReportTest.rich().withPointerDrag("free"));
        assertEquals(before, free);
        assertFalse(free.contains("pointer-drag"));
    }

    @Test
    void aConstrainedOrLockedDragIsOneLineAfterTheFurniture() {
        for (String mode : new String[] {"horizontal", "vertical", "locked"}) {
            String report = ViewReport.format(ViewReportTest.rich().withPointerDrag(mode));
            assertTrue(report.contains("\npointer-drag: " + mode + "\n"), mode);
            assertTrue(report.indexOf("furniture:") < report.indexOf("pointer-drag:"));
            assertTrue(report.indexOf("pointer-drag:") < report.indexOf("\nselection:"));
        }
    }
}
