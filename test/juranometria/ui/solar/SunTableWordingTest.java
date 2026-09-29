package juranometria.ui.solar;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sun table's explanation of ecliptic longitude distinguishes the
 * fixed J2000 equinox from the of-date seasonal events (issue #410).
 *
 * <p>The column is measured from the fixed J2000 equinox, while the
 * equinoxes and solstices of a later year are of-date events and
 * therefore lie near, not exactly at, the cardinal longitudes - the
 * 2026 events at 359.64°, 89.63°, 179.63° and 269.63°. The first
 * explanation shipped said "0° at the March equinox, 90° at the June
 * solstice", which the Sun article caught; both languages must now
 * say what the number means, and neither may say the old thing.
 */
class SunTableWordingTest {

    private static final String KEY = "suntable.column.longitude.explain";

    @Test
    void englishSaysFixedEquinoxAndNearNotAt() {
        String text = InterfaceText.forLanguage("en").say(KEY);
        for (String required : List.of("fixed J2000 equinox", "near, not exactly at",
                "0°, 90°, 180° and 270°", "the equinox itself moves")) {
            assertTrue(text.contains(required), "says: " + required + " - " + text);
        }
        for (String wrong : List.of("0° at the March equinox",
                "90° at the June solstice")) {
            assertFalse(text.contains(wrong), "no longer says: " + wrong);
        }
    }

    @Test
    void norwegianSaysTheSame() {
        String text = InterfaceText.forLanguage("nb-NO").say(KEY);
        for (String required : List.of("faste J2000-vårjevndøgnspunktet",
                "nær, ikke nøyaktig på", "0°, 90°, 180° og 270°",
                "jevndøgnspunktet selv flytter seg")) {
            assertTrue(text.contains(required), "sier: " + required + " - " + text);
        }
        for (String wrong : List.of("0° ved jevndøgnet i mars",
                "90° ved solverv i juni")) {
            assertFalse(text.contains(wrong), "sier ikke lenger: " + wrong);
        }
    }

    @Test
    void theNumbersAndTheirParsingAreUntouched() {
        // The repair is words only: the column's value is the same
        // fixed-J2000 longitude the contract froze, formatted the same.
        assertTrue(SunTableFormat.degrees(359.64).equals("359.64°"));
        assertTrue(SunTableFormat.degrees(89.63).equals("89.63°"));
    }
}
