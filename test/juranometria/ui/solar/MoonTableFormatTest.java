package juranometria.ui.solar;

import org.junit.jupiter.api.Test;

import juranometria.solar.SolarSystemService.CompassPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Moon table's own rounding (issue #406, M5 and M6; issue #408), in
 * {@code Locale.ROOT}: the kilometre with spaced thousands, the tenth
 * of a percent, the tenth of a degree with the side's letter, the whole
 * degree of position angle.
 */
class MoonTableFormatTest {

    @Test
    void kilometresAreWholeWithSpacedThousands() {
        assertEquals("384 400 km", MoonTableFormat.kilometres(384400.0));
        assertEquals("356 345 km", MoonTableFormat.kilometres(356344.6));
        assertEquals("1 000 000 km", MoonTableFormat.kilometres(999999.5));
        assertEquals("999 km", MoonTableFormat.kilometres(999.4));
        assertEquals("0 km", MoonTableFormat.kilometres(0.2));
    }

    @Test
    void percentIsToATenthOfTheWholeDisc() {
        assertEquals("67.9 %", MoonTableFormat.percent(0.67855));
        assertEquals("0.0 %", MoonTableFormat.percent(0.00004));
        assertEquals("100.0 %", MoonTableFormat.percent(1.0));
        assertEquals("50.0 %", MoonTableFormat.percent(0.5));
    }

    @Test
    void elongationCarriesTheSidesLetterAsTheLanguageSpellsIt() {
        assertEquals("84.8° E", MoonTableFormat.elongation(84.826, "E"));
        assertEquals("175.0° W", MoonTableFormat.elongation(174.97, "W"));
        assertEquals("3.8° Ø", MoonTableFormat.elongation(3.8, "Ø"));
    }

    @Test
    void positionAngleIsWholeDegreesWithThreeSixtyAsZero() {
        assertEquals("285°", MoonTableFormat.positionAngle(285.04));
        assertEquals("0°", MoonTableFormat.positionAngle(359.7));
        assertEquals("0°", MoonTableFormat.positionAngle(0.2));
        assertEquals("67°", MoonTableFormat.positionAngle(66.6));
    }

    @Test
    void theCompassLettersAreNotation() {
        assertEquals("WNW", MoonTableFormat.letters(CompassPoint.WNW));
        assertEquals("N", MoonTableFormat.letters(CompassPoint.N));
    }
}
