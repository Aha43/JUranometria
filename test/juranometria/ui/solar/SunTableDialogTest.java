package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sun table's content, headless (issue #400): what it shows, what
 * it refuses and says, what it reads from Place and Time and when,
 * and what it never does - through the shared shell of #408, whose
 * words for the Sun are exactly the words of #400.
 */
class SunTableDialogTest {

    private static final Instant MIDSUMMER = Instant.parse("2026-06-21T10:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, MIDSUMMER);
    private static SolarSystemService service;

    @BeforeAll
    static void loadTheService() {
        service = SolarSystemService.load();
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
            }, service, InterfaceText.forLanguage(language), SolarTable.sun());
        }
    }

    @Test
    void aFreshTableOpensOnTheSingleInstantFromPlaceAndTime() {
        Rig rig = new Rig("en");
        assertTrue(rig.content.instantView.isSelected(), "the instant view");
        assertFalse(rig.content.rangeView.isSelected());
        assertEquals(1, rig.content.model.getRowCount());
        assertEquals("Observer at latitude 59.91, longitude 10.75 east; instant"
                + " 2026-06-21 10:00:00 UTC, from Place and Time.",
                rig.content.observerNote.getText());
        assertEquals("1 rows.", rig.content.status());
        assertEquals("2026-06-21 10:00:00", rig.content.start.getText(),
                "the range starts at Place and Time's instant");
        assertEquals("2026-06-28 10:00:00", rig.content.end.getText(),
                "and ends a week later");
        assertEquals(2, rig.content.step.getSelectedIndex(), "one day");
    }

    @Test
    void theRowIsTheServicesAnswerSpelledByTheFormat() {
        Rig rig = new Rig("en");
        SunObservation o = (SunObservation) service.observe(Body.SUN, OSLO);
        SolarTableModel m = rig.content.model;
        assertEquals(SunTableFormat.minute(MIDSUMMER), m.getValueAt(0, 0));
        assertEquals(SunTableFormat.hms(o.astrometricJ2000()), m.getValueAt(0, 1));
        assertEquals(SunTableFormat.dms(o.astrometricJ2000()), m.getValueAt(0, 2));
        assertEquals(SunTableFormat.degrees(o.eclipticLongitudeJ2000Degrees()),
                m.getValueAt(0, 3));
        assertEquals("51.01°", m.getValueAt(0, 4));
        assertEquals("150.56°", m.getValueAt(0, 5));
        assertEquals("1.016165 AU (152.016 mill. km)", m.getValueAt(0, 6));
        assertEquals("31′ 27.9″", m.getValueAt(0, 7));
        assertEquals(List.of("Instant (UTC)", "Right ascension (J2000)",
                "Declination (J2000)", "Ecliptic longitude (J2000)",
                "Altitude (no refraction)", "Azimuth (from north through east)",
                "Distance", "Apparent diameter"),
                columnNames(m));
    }

    @Test
    void changingTheObserverChangesTheRowOnTheNextRead() {
        Rig rig = new Rig("en");
        int reads = rig.reads;
        Object before = rig.content.model.getValueAt(0, 4);
        rig.observer = new Observer(-0.18, -78.5, MIDSUMMER);
        assertEquals(before, rig.content.model.getValueAt(0, 4),
                "nothing moves until the table reads again");
        rig.content.update();
        assertEquals(reads + 1, rig.reads, "one read per update");
        assertNotEquals(before, rig.content.model.getValueAt(0, 4),
                "Quito's Sun stands at a different height");
        assertTrue(rig.content.observerNote.getText().contains("-0.18"));
        SunObservation quito = (SunObservation) service.observe(Body.SUN,
                rig.observer);
        assertEquals(SunTableFormat.altitude(quito.horizontal().altitudeDegrees(),
                "(below the horizon)"), rig.content.model.getValueAt(0, 4));
    }

    @Test
    void aRangeAnswersItsRowsAndMarksAnAppendedEnd() {
        Rig rig = new Rig("en");
        rig.content.rangeView.setSelected(true);
        rig.content.update();
        assertEquals(8, rig.content.model.getRowCount(), "a week, both ends");
        assertEquals("8 rows.", rig.content.status());
        rig.content.end.setText("2026-06-24 07:30");
        rig.content.update();
        assertEquals(4, rig.content.model.getRowCount());
        assertEquals("2026-06-24 07:30 †", rig.content.model.getValueAt(3, 0));
        assertEquals("4 rows; the end is not on the grid, so it is the last"
                + " row, marked †.", rig.content.status());
    }

    @Test
    void everyRefusalSaysWhyAndLeavesTheTableEmpty() {
        Rig rig = new Rig("en");
        rig.content.rangeView.setSelected(true);
        rig.content.end.setText("2026-06-21 09:00");
        rig.content.update();
        assertEquals("The range runs backwards: the end is before the start.",
                rig.content.status());
        assertEquals(0, rig.content.model.getRowCount());
        rig.content.end.setText("2030-06-21 10:00");
        rig.content.update();
        assertEquals("That range asks for 1462 rows and at most 1000 are shown;"
                + " shorten it or lengthen the step.", rig.content.status());
        rig.content.end.setText("noon tomorrow");
        rig.content.update();
        assertEquals("noon tomorrow is not an instant this table can read; type"
                + " yyyy-mm-dd hh:mm or hh:mm:ss, in UTC.", rig.content.status());
        rig.content.start.setText("1899-12-31 12:00");
        rig.content.end.setText("1900-01-02 12:00");
        rig.content.update();
        assertEquals("The Sun is computed for civil dates from 1900-01-01 to"
                + " 2100-12-31; 1899-12-31 12:00:00 is outside that.",
                rig.content.status());
        rig.content.instantView.setSelected(true);
        rig.observer = OSLO.at(Instant.parse("2101-01-01T00:00:00Z"));
        rig.content.update();
        assertTrue(rig.content.status().contains("2101-01-01 00:00:00 is outside"));
        assertEquals(0, rig.content.model.getRowCount());
    }

    @Test
    void anEstimatedInstantIsMarked() {
        Rig rig = new Rig("en");
        rig.observer = OSLO.at(Instant.parse("1950-06-21T12:00:00Z"));
        rig.content.update();
        assertEquals("1950-06-21 12:00 est.", rig.content.model.getValueAt(0, 0));
        rig.observer = OSLO;
        rig.content.update();
        assertEquals("2026-06-21 10:00", rig.content.model.getValueAt(0, 0));
    }

    @Test
    void withoutPlaceAndTimeThereIsNothingToComputeAndItSaysSo() {
        Rig rig = new Rig("en");
        rig.observer = null;
        rig.content.update();
        assertEquals("Place and Time is not attached, so there is no observer"
                + " to compute for.", rig.content.observerNote.getText());
        assertEquals(0, rig.content.model.getRowCount());
        assertFalse(rig.content.compute.isEnabled());
        assertFalse(rig.content.start.isEnabled());
        rig.observer = OSLO;
        rig.content.update();
        assertTrue(rig.content.compute.isEnabled());
        assertEquals(1, rig.content.model.getRowCount());
    }

    @Test
    void theNorwegianTableSaysItsWordsAndKeepsTheNumbers() {
        Rig en = new Rig("en");
        Rig nb = new Rig("nb-NO");
        assertEquals(List.of("Tidspunkt (UTC)", "Rektascensjon (J2000)",
                "Deklinasjon (J2000)", "Ekliptisk lengde (J2000)",
                "Høyde (uten refraksjon)", "Asimut (fra nord via øst)",
                "Avstand", "Tilsynelatende diameter"), columnNames(nb.content.model));
        // The owner ruled a decimal comma for the Norwegian reader's
        // table (#400); the digits are the same, the separator is the
        // language's, and what is parsed back keeps the point.
        assertEquals("51,01\u00b0", nb.content.model.getValueAt(0, 4));
        assertEquals("1,016165 AU (152,016 mill. km)", nb.content.model.getValueAt(0, 6));
        assertEquals("31\u2032 27,9\u2033", nb.content.model.getValueAt(0, 7));
        assertEquals("05h 58m 40,8s", nb.content.model.getValueAt(0, 1));
        for (int c : List.of(0, 1, 2, 3, 4, 5, 7)) {
            assertEquals(String.valueOf(en.content.model.getValueAt(0, c))
                            .replace(".", ","),
                    String.valueOf(nb.content.model.getValueAt(0, c)),
                    "column " + c + ": the same digits with the language's"
                            + " separator; the abbreviation point in"
                            + " \"mill. km\" is not a decimal and stays");
        }
        assertEquals("2026-06-21 10:00:00", nb.content.start.getText(),
                "the typed range keeps the machine form");
        assertTrue(nb.content.observerNote.getText().contains("59,91"),
                "the observer note follows the language too");
        assertEquals("1 rader.", nb.content.status());
        nb.observer = new Observer(59.91, 10.75,
                Instant.parse("2026-12-21T20:50:09Z"));
        nb.content.update();
        assertTrue(String.valueOf(nb.content.model.getValueAt(0, 4))
                .endsWith("° (under horisonten)"),
                "the status below the horizon is the language's own words");
        assertTrue(nb.content.observerNote.getText().startsWith("Observatør"));
    }

    @Test
    void everyControlHasASpokenNameAndEveryLetterIsItsOwn() {
        for (String language : List.of("en", "nb-NO")) {
            Rig rig = new Rig(language);
            List<String> nameless = new ArrayList<>();
            Set<Integer> letters = new HashSet<>();
            List<String> shared = new ArrayList<>();
            for (Component c : all(rig.content)) {
                // The look and feel's own parts - a combo's arrow, a
                // scroll bar's buttons - are its to name, not this
                // surface's; AccessibleSurfaceTest draws the same line.
                if (c.getClass().getName().contains(".plaf.")) {
                    continue;
                }
                if (c instanceof AbstractButton || c instanceof JTextField
                        || c instanceof JComboBox) {
                    String name = c.getAccessibleContext().getAccessibleName();
                    if (name == null || name.isBlank()) {
                        nameless.add(c.getName() + " (" + c.getClass()
                                .getSimpleName() + ")");
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
            assertEquals(List.of(), nameless, language + ": every control"
                    + " names itself to a screen reader");
            assertEquals(List.of(), shared, language + ": no two controls"
                    + " share an access letter");
            assertTrue(letters.size() >= 8, language + ": the eight controls"
                    + " have letters: " + letters.size());
            String tableName = rig.content.table.getAccessibleContext()
                    .getAccessibleName();
            assertTrue(tableName != null && !tableName.isBlank(),
                    language + ": the table names itself");
        }
    }

    @Test
    void theTableReadsNoClockAndRemembersNothing() throws Exception {
        for (String source : List.of(
                "src/juranometria/ui/solar/SolarTableDialog.java",
                "src/juranometria/ui/solar/SolarTableModel.java",
                "src/juranometria/ui/solar/SolarTable.java",
                "src/juranometria/ui/solar/SolarTableWords.java",
                "src/juranometria/ui/solar/SunTableFormat.java",
                "src/juranometria/ui/solar/MoonTableFormat.java")) {
            String text = Files.readString(Path.of(source), StandardCharsets.UTF_8);
            for (String forbidden : List.of("Instant.now", "currentTimeMillis",
                    "nanoTime", "Clock.", "Preferences", "java.io.File",
                    "java.nio.file", "java.net", "ZoneId.systemDefault")) {
                assertFalse(text.contains(forbidden), source + " mentions "
                        + forbidden + ": the table reads Place and Time's"
                        + " instant and keeps no clock, store or file of its own");
            }
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
