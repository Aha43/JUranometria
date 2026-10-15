package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The field as the atlas prints it (#543): the ladder unchanged, closer fields to their own precision. */
class FieldWidthTextTest {

    @Test
    void theLadderKeepsTheFormsItHasAlwaysPrinted() {
        for (double field : ChartViewState.fieldWidthSteps()) {
            assertEquals(String.format(java.util.Locale.ROOT, "%.1f", field),
                    FieldWidthText.title(field));
            assertEquals(String.format(java.util.Locale.ROOT, "%.0f", field),
                    FieldWidthText.whole(field));
        }
    }

    @Test
    void aCloserFieldPrintsToItsOwnPrecisionEverywhere() {
        // The ladder's formats would print 0.25 as "0.3" and "0".
        assertEquals("0.5", FieldWidthText.title(0.5));
        assertEquals("0.25", FieldWidthText.title(0.25));
        assertEquals("0.5", FieldWidthText.whole(0.5));
        assertEquals("0.25", FieldWidthText.whole(0.25));
    }
}
