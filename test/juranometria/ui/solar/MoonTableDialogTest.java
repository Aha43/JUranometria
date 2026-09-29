package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Phase;
import juranometria.tool.MoonEventsFixture;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Moon table's content, headless (Sprint 36, issue #408): the same
 * shell as the Sun's, over the Moon's eleven columns - what it shows,
 * what it says in each language, how the named events read, what
 * changes with the observer and the instant, and what it never does.
 */
class MoonTableDialogTest {

    private static final Instant MIDSUMMER = Instant.parse("2026-06-21T10:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, MIDSUMMER);
    private static SolarSystemService service;
    private static Map<String, MoonEventsFixture.Event> events;

    @BeforeAll
    static void load() throws IOException {
        service = SolarSystemService.load();
        events = MoonEventsFixture.read();
    }

    /** A rig whose observer a test can change between reads. */
    private static final class Rig {
        Observer observer = OSLO;
        int reads;
        final SolarTableDialog.Content content;

        Rig(String language) {
            content = SolarTableDialog.content(() -> {
                reads++;
                return observer;
            }, service, InterfaceText.forLanguage(language), SolarTable.moon());
        }

        String cell(int row, int column) {
            return String.valueOf(content.model.getValueAt(row, column));
        }
    }

    @Test
    void theShellIsTheSunsAndTheColumnsAreTheMoons() {
        Rig rig = new Rig("en");
        assertTrue(rig.content.instantView.isSelected());
        assertEquals(1, rig.content.model.getRowCount());
        assertEquals("Observer at latitude 59.91, longitude 10.75 east; instant"
                + " 2026-06-21 10:00:00 UTC, from Place and Time.",
                rig.content.observerNote.getText());
        assertEquals("1 rows.", rig.content.status());
        assertEquals("2026-06-21 10:00:00", rig.content.start.getText());
        assertEquals("2026-06-28 10:00:00", rig.content.end.getText());
        assertEquals(List.of("Instant (UTC)", "Right ascension (J2000)",
                "Declination (J2000)", "Altitude (no refraction)",
                "Azimuth (from north through east)", "Distance",
                "Apparent diameter", "Illuminated", "Phase",
                "Elongation from the Sun", "Lit side"),
                columnNames(rig.content.model));
        assertEquals("moonTable", rig.content.table.getName());
        assertEquals("The Moon's computed quantities",
                rig.content.table.getAccessibleContext().getAccessibleName());
        assertTrue(rig.content.model.columnExplanation(10)
                .contains("from celestial north through east"));
        assertTrue(rig.content.model.columnExplanation(0).contains("†"),
                "the shared instant explanation names the appended mark");
    }

    @Test
    void theRowIsTheServicesAnswerSpelledByTheFormats() {
        Rig rig = new Rig("en");
        MoonObservation o = (MoonObservation) service.observe(Body.MOON, OSLO);
        assertEquals(SunTableFormat.minute(MIDSUMMER), rig.cell(0, 0));
        assertEquals(SunTableFormat.hms(o.astrometricJ2000()), rig.cell(0, 1));
        assertEquals(SunTableFormat.dms(o.astrometricJ2000()), rig.cell(0, 2));
        assertEquals(SunTableFormat.altitude(o.horizontal().altitudeDegrees(),
                "(below the horizon)"), rig.cell(0, 3));
        assertEquals(SunTableFormat.degrees(o.horizontal().azimuthDegrees()),
                rig.cell(0, 4));
        assertEquals(MoonTableFormat.kilometres(o.distanceKm()), rig.cell(0, 5));
        assertEquals(SunTableFormat.minutesSeconds(o.angularDiameterArcseconds()),
                rig.cell(0, 6));
        assertEquals(MoonTableFormat.percent(o.illuminatedFraction()), rig.cell(0, 7));
        assertEquals("waxing crescent", rig.cell(0, 8));
        assertEquals(Phase.WAXING_CRESCENT, o.phase());
        assertEquals(MoonTableFormat.elongation(o.elongationDegrees(), "E"),
                rig.cell(0, 9));
        assertEquals(MoonTableFormat.positionAngle(o.brightLimbAngleDegrees())
                + " (west-northwest)", rig.cell(0, 10));
        assertTrue(rig.cell(0, 5).endsWith(" km") && rig.cell(0, 5).contains(" "),
                "kilometres with spaced thousands: " + rig.cell(0, 5));
    }

    @Test
    void theNamedEventsReadAsTheirNames() {
        // Espenak's instants from the cited fixture; the words are the
        // table's, and the numbers make size and phase changes visible.
        Rig rig = new Rig("en");
        rig.observer = OSLO.at(events.get("new-moon-june").instant());
        rig.content.update();
        assertEquals("near new Moon", rig.cell(0, 8));
        assertEquals("not well-defined (near new Moon)", rig.cell(0, 10));
        assertEquals("0.1 %", rig.cell(0, 7));
        rig.observer = OSLO.at(events.get("first-quarter-june").instant());
        rig.content.update();
        assertEquals("near first quarter", rig.cell(0, 8));
        assertEquals("50.1 %", rig.cell(0, 7));
        assertEquals("89.9° E", rig.cell(0, 9));
        assertEquals("293° (west-northwest)", rig.cell(0, 10));
        rig.observer = OSLO.at(events.get("full-moon-june").instant());
        rig.content.update();
        assertEquals("near full Moon", rig.cell(0, 8));
        assertEquals("not well-defined (near full Moon)", rig.cell(0, 10));
        assertEquals("175.0° W", rig.cell(0, 9));
        rig.observer = OSLO.at(events.get("last-quarter-july").instant());
        rig.content.update();
        assertEquals("near last quarter", rig.cell(0, 8));
        assertEquals("67° (east-northeast)", rig.cell(0, 10));
        rig.observer = OSLO.at(events.get("perigee-nearest").instant());
        rig.content.update();
        String nearest = rig.cell(0, 5);
        String large = rig.cell(0, 6);
        rig.observer = OSLO.at(events.get("apogee-farthest").instant());
        rig.content.update();
        assertEquals("357 508 km", nearest);
        assertEquals("409 024 km", rig.cell(0, 5));
        assertEquals("33′ 24.8″", large);
        assertEquals("29′ 12.3″", rig.cell(0, 6));
    }

    @Test
    void changingTheObserverChangesExactlyTheExpectedValues() {
        Rig rig = new Rig("en");
        int reads = rig.reads;
        String altitude = rig.cell(0, 3);
        String phase = rig.cell(0, 8);
        String distance = rig.cell(0, 5);
        rig.observer = new Observer(-0.18, -78.5, MIDSUMMER);
        assertEquals(altitude, rig.cell(0, 3), "nothing moves until the table reads");
        rig.content.update();
        assertEquals(reads + 1, rig.reads, "one read per update");
        assertNotEquals(altitude, rig.cell(0, 3), "a different sky");
        assertNotEquals(distance, rig.cell(0, 5), "a different topocentric distance");
        assertEquals(phase, rig.cell(0, 8), "but the same phase word: waxing or"
                + " waning is the Moon's, not the observer's");
        assertTrue(rig.content.observerNote.getText().contains("-0.18"));
    }

    @Test
    void theRangeBehavesExactlyAsTheSuns() {
        Rig moon = new Rig("en");
        moon.content.rangeView.setSelected(true);
        moon.content.update();
        assertEquals(8, moon.content.model.getRowCount());
        assertEquals("8 rows.", moon.content.status());
        moon.content.end.setText("2026-06-24 07:30");
        moon.content.update();
        assertEquals(4, moon.content.model.getRowCount());
        assertEquals("2026-06-24 07:30 †", moon.cell(3, 0));
        assertEquals("4 rows; the end is not on the grid, so it is the last"
                + " row, marked †.", moon.content.status());
        moon.content.end.setText("2026-06-21 09:00");
        moon.content.update();
        assertEquals("The range runs backwards: the end is before the start.",
                moon.content.status());
        assertEquals(0, moon.content.model.getRowCount());
        moon.content.end.setText("2030-06-21 10:00");
        moon.content.update();
        assertEquals("That range asks for 1462 rows and at most 1000 are shown;"
                + " shorten it or lengthen the step.", moon.content.status());
        moon.content.start.setText("1899-12-31 12:00");
        moon.content.end.setText("1900-01-02 12:00");
        moon.content.update();
        assertEquals("The Moon is computed for civil dates from 1900-01-01 to"
                + " 2100-12-31; 1899-12-31 12:00:00 is outside that.",
                moon.content.status());
        moon.content.instantView.setSelected(true);
        moon.observer = OSLO.at(Instant.parse("1950-06-21T12:00:00Z"));
        moon.content.update();
        assertEquals("1950-06-21 12:00 est.", moon.cell(0, 0));
        moon.observer = null;
        moon.content.update();
        assertEquals("Place and Time is not attached, so there is no observer"
                + " to compute for.", moon.content.observerNote.getText());
        assertFalse(moon.content.compute.isEnabled());
    }

    @Test
    void theNorwegianTableSaysItsWordsAndKeepsTheNumbers() {
        Rig en = new Rig("en");
        Rig nb = new Rig("nb-NO");
        assertEquals(List.of("Tidspunkt (UTC)", "Rektascensjon (J2000)",
                "Deklinasjon (J2000)", "Høyde (uten refraksjon)",
                "Asimut (fra nord via øst)", "Avstand", "Tilsynelatende diameter",
                "Belyst", "Fase", "Elongasjon fra Solen", "Belyst side"),
                columnNames(nb.content.model));
        assertEquals("tiltagende månesigd", nb.cell(0, 8));
        assertTrue(nb.cell(0, 9).endsWith("° Ø"), nb.cell(0, 9));
        assertTrue(nb.cell(0, 10).endsWith(" (vest-nordvest)"), nb.cell(0, 10));
        assertEquals(en.cell(0, 7).replace(".", ","), nb.cell(0, 7));
        assertEquals(en.cell(0, 5), nb.cell(0, 5), "kilometres carry no decimal");
        for (int c : List.of(0, 1, 2, 4, 6)) {
            assertEquals(en.cell(0, c).replace(".", ","), nb.cell(0, c),
                    "column " + c + ": the same digits with the language's separator");
        }
        // The Moon is below Oslo's horizon that morning: the number is
        // the same, the status is the language's own words.
        assertEquals(en.cell(0, 3).replace(".", ",")
                .replace("(below the horizon)", "(under horisonten)"), nb.cell(0, 3));
        assertEquals("1 rader.", nb.content.status());
        assertEquals("Månens beregnede størrelser",
                nb.content.table.getAccessibleContext().getAccessibleName());
        nb.observer = OSLO.at(events.get("full-moon-june").instant());
        nb.content.update();
        assertEquals("nær fullmåne", nb.cell(0, 8));
        assertEquals("ikke veldefinert (nær fullmåne)", nb.cell(0, 10));
        assertTrue(nb.cell(0, 9).endsWith("° V"));
        nb.observer = OSLO.at(events.get("last-quarter-july").instant());
        nb.content.update();
        assertEquals("nær siste kvarter", nb.cell(0, 8));
        assertTrue(nb.cell(0, 10).endsWith("(øst-nordøst)"));
        assertTrue(nb.cell(0, 3).endsWith("° (under horisonten)"));
        nb.content.rangeView.setSelected(true);
        nb.content.end.setText("2026-06-21 09:00");
        nb.content.update();
        assertEquals("Tidsrommet går baklengs: slutten er før starten.",
                nb.content.status());
    }

    @Test
    void everyPhaseWordExistsInBothLanguagesAndNoneIsMinkende() {
        for (String language : List.of("en", "nb-NO")) {
            SolarTableWords words = new SolarTableWords(
                    InterfaceText.forLanguage(language), "moontable");
            Set<String> seen = new HashSet<>();
            for (Phase phase : Phase.values()) {
                String word = SolarTable.Moon.phaseWord(phase, words);
                assertTrue(seen.add(word), language + ": " + word + " twice");
                assertFalse(word.contains("minkende"), word);
            }
            for (SolarSystemService.CompassPoint point
                    : SolarSystemService.CompassPoint.values()) {
                assertTrue(seen.add(SolarTable.Moon.compassWord(point, words)),
                        language + ": a compass word twice");
            }
            assertEquals(24, seen.size());
        }
    }

    @Test
    void everyControlHasASpokenNameAndEveryLetterIsItsOwn() {
        for (String language : List.of("en", "nb-NO")) {
            Rig rig = new Rig(language);
            List<String> nameless = new ArrayList<>();
            Set<Integer> letters = new HashSet<>();
            List<String> shared = new ArrayList<>();
            for (Component c : all(rig.content)) {
                if (c.getClass().getName().contains(".plaf.")) {
                    continue;
                }
                if (c instanceof AbstractButton || c instanceof JTextField
                        || c instanceof JComboBox) {
                    String name = c.getAccessibleContext().getAccessibleName();
                    if (name == null || name.isBlank()) {
                        nameless.add(c.getName());
                    }
                }
                int letter = 0;
                if (c instanceof AbstractButton button) {
                    letter = button.getMnemonic();
                } else if (c instanceof JLabel label) {
                    letter = label.getDisplayedMnemonic();
                }
                if (letter != 0 && !letters.add(letter)) {
                    shared.add(language + ": " + (char) letter);
                }
            }
            assertEquals(List.of(), nameless, language);
            assertEquals(List.of(), shared, language);
            assertTrue(letters.size() >= 8, language + ": " + letters.size());
            assertEquals(language.equals("en") ? "Show the Moon at the instant"
                    + " set in Place and Time" : "Vis Månen ved tidspunktet satt"
                    + " i Sted og tid", rig.content.instantView
                    .getAccessibleContext().getAccessibleName(),
                    "the body's own a11y word overrides the shared one");
        }
    }

    private static List<String> columnNames(SolarTableModel model) {
        List<String> names = new ArrayList<>();
        for (int c = 0; c < model.getColumnCount(); c++) {
            names.add(model.getColumnName(c));
        }
        return names;
    }

    private static List<Component> all(Container root) {
        List<Component> found = new ArrayList<>();
        for (Component c : root.getComponents()) {
            found.add(c);
            if (c instanceof Container inner) {
                found.addAll(all(inner));
            }
        }
        return found;
    }
}
