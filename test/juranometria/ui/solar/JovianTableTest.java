package juranometria.ui.solar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.VisibilityState;
import juranometria.tool.JovianComparison;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Jupiter table (issue #474, the table ruled on #472): one shared
 * applied query and result for the Controller's group and the dialog;
 * no computation from building, opening or expanding; every value shown
 * the service's, rounded; the named configurations - the December
 * triple transit and every state Horizons names in December 2026 -
 * reading in both languages; and the refusals, with the years named.
 */
class JovianTableTest {

    private static JovianSystemService service;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final Instant TRIPLE = Instant.parse("2026-12-11T22:45:00Z");

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
    }

    static Observer oslo(Instant when) {
        return JovianComparison.observer("oslo", when);
    }

    static void onEdt(Runnable body) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] instanceof Error e) {
            throw e;
        }
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    @Test
    void buildingOpeningAndExpandingComputeNothingAndLoadNoPack() throws Exception {
        AtomicInteger loaded = new AtomicInteger();
        Supplier<JovianSystemService> counted = () -> {
            loaded.incrementAndGet();
            return service;
        };
        Observer[] observer = {oslo(TRIPLE)};
        JovianTableSession session = new JovianTableSession(() -> observer[0], counted);
        onEdt(() -> {
            JovianTableControls controller = new JovianTableControls(session, EN, false);
            controller.inController();
            JovianTableControls dialog = new JovianTableControls(session, EN, true);
            dialog.inDialog();
            // a second expansion of the group
            controller.inController();
            assertEquals(0, session.computations(), "building and expanding compute nothing");
            assertEquals(0, loaded.get(), "and the pack is not even read");
            assertTrue(!session.result().applied());
            assertEquals(" ", controller.cardTitle.getText(), "no card before a computation");
            controller.apply();
            assertEquals(1, session.computations(), "applying computes once");
        });
    }

    @Test
    void bothHostsShowOneAppliedResultAndNeitherOverwritesTheOthersDraft() throws Exception {
        JovianTableSession session = new JovianTableSession(() -> oslo(TRIPLE), () -> service);
        onEdt(() -> {
            JovianTableControls controller = new JovianTableControls(session, EN, false);
            controller.inController();
            JovianTableDialog.Content dialog = JovianTableDialog.content(session, NB);
            JovianTableControls d = dialog.controls();
            // an unfinished draft in the dialog
            d.rangeView.setSelected(true);
            d.start.setText("2026-12-11 22:3");
            controller.apply();
            assertSame(controller.shown(), d.shown(), "one applied result");
            assertEquals("2026-12-11 22:3", d.start.getText(), "the dialog's draft is its own");
            assertTrue(d.rangeView.isSelected());
            assertEquals(controller.cardValue("ra"), d.cardValue("ra").replace(',', '.'),
                    "the same answer in both hosts, each in its language");
            assertEquals(4, controller.model.getRowCount());
            assertEquals(4, d.model.getRowCount());
            controller.release();
            dialog.controls().release();
            assertEquals(0, session.subscribers(), "both let go");
        });
    }

    @Test
    void everyValueShownIsTheServicesAnswerRounded() throws Exception {
        JovianTableSession session = new JovianTableSession(() -> oslo(TRIPLE), () -> service);
        onEdt(() -> {
            JovianTableControls c = new JovianTableControls(session, EN, false);
            c.apply();
            Configuration shown = session.result().entries().get(0).configuration();
            Configuration fresh = service.observeMoons(oslo(TRIPLE));
            // the rows are the session's result objects, not recomputed
            for (int row = 0; row < 4; row++) {
                assertSame(shown.moons().get(row), c.model.lines().get(row).moon());
            }
            // and that result is the service's answer for the observer
            for (int row = 0; row < 4; row++) {
                MoonPlace a = shown.moons().get(row);
                MoonPlace b = fresh.moons().get(row);
                assertEquals(b.xArcseconds(), a.xArcseconds());
                assertEquals(b.separationJupiterRadii(), a.separationJupiterRadii());
                assertEquals(b.state(), a.state());
                for (int col = 0; col < JovianMoonsModel.COLUMNS.size(); col++) {
                    assertEquals(JovianMoonsModel.cell(b, col, c.words()),
                            c.model.getValueAt(row, col), "row " + row + " column " + col);
                }
            }
            assertEquals(String.format(Locale.ROOT, "%.3f AU", fresh.jupiter().distanceAu()),
                    c.cardValue("distance"));
            assertEquals(String.format(Locale.ROOT, "%.1f″ / %.1f″",
                    fresh.jupiter().equatorialDiameterArcseconds(),
                    fresh.jupiter().polarDiameterArcseconds()), c.cardValue("diameter"));
            assertEquals(String.format(Locale.ROOT, "%.1f°", fresh.jupiter().poleAngleDegrees()),
                    c.cardValue("pole"));
        });
    }

    @Test
    void theTripleTransitReadsNaturallyInBothLanguages() throws Exception {
        JovianTableSession session = new JovianTableSession(() -> oslo(TRIPLE), () -> service);
        onEdt(() -> {
            JovianTableControls en = new JovianTableControls(session, EN, false);
            JovianTableControls nb = new JovianTableControls(session, NB, false);
            en.apply();
            assertEquals(List.of("Io", "Europa", "Ganymede", "Callisto"), column(en, 0));
            assertEquals(List.of("in front of Jupiter", "in front of Jupiter",
                    "clear of Jupiter", "in front of Jupiter"), column(en, 6));
            assertEquals(List.of("east", "east", "west", "west"), column(en, 1));
            assertTrue(((String) en.model.getValueAt(2, 2)).endsWith(" w"));
            assertEquals("Jupiter · 2026-12-11 22:45:00 UTC", en.cardTitle.getText());
            assertEquals(List.of("Io", "Europa", "Ganymedes", "Callisto"), column(nb, 0));
            assertEquals(List.of("foran Jupiter", "foran Jupiter", "klar av Jupiter",
                    "foran Jupiter"), column(nb, 6));
            assertEquals(List.of("øst", "øst", "vest", "vest"), column(nb, 1));
            assertTrue(((String) nb.model.getValueAt(0, 2)).endsWith(" ø"));
            assertTrue(((String) nb.model.getValueAt(0, 4)).contains(","),
                    "the Norwegian decimal comma: " + nb.model.getValueAt(0, 4));
            assertEquals(en.status().isBlank(), false);
        });
    }

    private static List<String> column(JovianTableControls c, int column) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < c.model.getRowCount(); row++) {
            out.add((String) c.model.getValueAt(row, column));
        }
        return out;
    }

    /** Horizons' codes as the state the table says (ruled precedence). */
    private static final Map<String, VisibilityState> STATE_OF_CODE = Map.of(
            "t", VisibilityState.IN_FRONT_OF_JUPITER,
            "O", VisibilityState.BEHIND_JUPITER,
            "U", VisibilityState.BEHIND_JUPITER_IN_ITS_SHADOW,
            "P", VisibilityState.BEHIND_JUPITER_IN_ITS_SHADOW,
            "u", VisibilityState.IN_JUPITERS_SHADOW,
            "p", VisibilityState.PARTLY_IN_JUPITERS_SHADOW,
            "*", VisibilityState.CLEAR_OF_JUPITER);

    @Test
    void everyDecember2026HourAtOsloReadsHorizonsStateInWordsButTheAllowlistedGraze()
            throws Exception {
        Observer[] at = {null};
        JovianTableSession session = new JovianTableSession(() -> at[0], () -> service);
        int[] checked = new int[1];
        List<String> seen = new ArrayList<>();
        onEdt(() -> {
            JovianTableControls en = new JovianTableControls(session, EN, false);
            JovianTableControls nb = new JovianTableControls(session, NB, false);
            try {
                for (Moon moon : Moon.values()) {
                    String body = moon.name().toLowerCase(Locale.ROOT);
                    for (String[] f : JovianComparison.rows("december-2026-hourly-" + body + "-oslo")) {
                        Instant when = JovianComparison.stamp(f[0]);
                        String code = f[13].replace("/", "").strip();
                        code = code.isEmpty() ? "*" : code;
                        if (moon == Moon.EUROPA && when.equals(Instant.parse("2026-12-06T17:00:00Z"))) {
                            continue; // the one allowlisted graze (ruled on #472, #473)
                        }
                        at[0] = oslo(when);
                        en.apply();
                        VisibilityState expected = STATE_OF_CODE.get(code);
                        assertNotNull(expected, "a code Horizons uses: " + code);
                        int row = moon.ordinal();
                        assertEquals(JovianMoonsModel.stateWords(expected, en.words()),
                                en.model.getValueAt(row, 6), body + " at " + when + " (" + code + ")");
                        assertEquals(JovianMoonsModel.stateWords(expected, nb.words()),
                                nb.model.getValueAt(row, 6), body + " at " + when + " in Norwegian");
                        if (!seen.contains(code)) {
                            seen.add(code);
                        }
                        checked[0]++;
                    }
                }
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        assertTrue(checked[0] > 2900, "four moons, 744 hours: " + checked[0]);
        assertTrue(seen.containsAll(List.of("t", "O", "U", "P", "u", "p", "*")),
                "every state Horizons names appears in the month: " + seen);
    }

    @Test
    void theRefusalsNameWhatTheyRefuse() throws Exception {
        Observer[] at = {null};
        JovianTableSession session = new JovianTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            JovianTableControls c = new JovianTableControls(session, EN, false);
            c.apply();
            assertEquals(JovianTableSession.Outcome.OBSERVER_ABSENT, session.result().outcome());
            assertEquals(0, session.computations());

            at[0] = oslo(Instant.parse("1950-06-01T00:00:00Z"));
            c.apply();
            assertEquals(JovianTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome());
            assertTrue(c.cardTitle.getText().startsWith("Jupiter · 1950-06-01"),
                    "Jupiter still answers: " + c.cardTitle.getText());
            assertEquals(0, c.model.getRowCount(), "and no moon rows");
            assertTrue(c.status().contains("2000-01-01") && c.status().contains("2100-12-31")
                    && c.status().contains("only Jupiter"), c.status());

            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            c.apply();
            assertEquals(JovianTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertTrue(c.status().startsWith("Jupiter is computed for civil dates from 1900-01-01"
                    + " to 2100-12-31"), c.status());

            at[0] = oslo(TRIPLE);
            c.rangeView.setSelected(true);
            c.start.setText("2026-12-11 00:00");
            c.end.setText("2027-02-01 00:00");
            c.step.setSelectedIndex(0);
            c.apply();
            assertEquals(JovianTableSession.Outcome.REFUSED_ROWS, session.result().outcome());
            assertTrue(c.status().contains("at most 250 are shown, four moon rows each"),
                    c.status());
            c.start.setText("1999-12-30 00:00");
            c.end.setText("2000-01-02 00:00");
            c.step.setSelectedIndex(2);
            c.apply();
            assertEquals(JovianTableSession.Outcome.REFUSED_MOONS_INTERVAL,
                    session.result().outcome());
            assertTrue(c.status().contains("1999-12-30"), c.status());
            c.start.setText("2026-12-12 00:00");
            c.end.setText("2026-12-11 00:00");
            c.apply();
            assertEquals(JovianTableSession.Outcome.REFUSED_BACKWARDS, session.result().outcome());
            c.start.setText("not a time");
            c.apply();
            assertEquals(JovianTableSession.Outcome.REFUSED_INSTANT, session.result().outcome());
            assertTrue(c.status().startsWith("not a time is not an instant"), c.status());
        });
    }

    @Test
    void aRangeIsGroupedByInstantAndCappedAt250Instants() throws Exception {
        JovianTableSession session = new JovianTableSession(() -> oslo(TRIPLE), () -> service);
        onEdt(() -> {
            JovianTableControls c = new JovianTableControls(session, EN, false);
            c.rangeView.setSelected(true);
            c.start.setText("2026-12-11 22:30");
            c.end.setText("2026-12-11 23:00");
            c.step.setSelectedIndex(0); // 1 hour: 22:30, then the end appended
            c.apply();
            assertEquals(JovianTableSession.Outcome.APPENDED, session.result().outcome());
            assertEquals(14, c.model.getRowCount(),
                    "two groups: a heading, two lines of Jupiter, four moons each");
            assertEquals("2026-12-11 22:30 UTC", c.model.getValueAt(0, 0));
            assertEquals("2026-12-11 23:00 UTC †", c.model.getValueAt(7, 0));
            assertEquals("clear of Jupiter", c.model.getValueAt(13, 6), "Callisto has left at 23:00");
            assertEquals(" ", c.cardTitle.getText(), "a range shows no Jupiter card");
            // exactly 250 instants is answered
            c.start.setText("2026-12-01 00:00");
            c.end.setText("2026-12-11 09:00");
            c.apply();
            assertEquals(250, session.result().entries().size());
            assertEquals(1000, c.model.moonRows());
        });
    }

    /**
     * The owner's checkpoint finding on #474: a range answered only how
     * the moons stand around Jupiter, not where Jupiter is. Every group
     * is now one Jupiter result and its four moons - the instant's
     * heading, Jupiter's eight summary values as the instant's card
     * spells them, then Io, Europa, Ganymede and Callisto - and Jupiter's
     * coordinates move across the range, in both languages.
     */
    @Test
    void everyRangeGroupIsOneJupiterResultAndItsFourMoonsInBothLanguages() throws Exception {
        JovianTableSession session = new JovianTableSession(() -> oslo(TRIPLE), () -> service);
        onEdt(() -> {
            JovianTableControls en = new JovianTableControls(session, EN, false);
            JovianTableControls nb = new JovianTableControls(session, NB, false);
            en.rangeView.setSelected(true);
            // Seven weeks at a week's step: Jupiter is near its stationary
            // point in December 2026 (measured: about 0.05 s of right
            // ascension an hour on the 11th), so hourly positions repeat
            // at the table's rounding; weekly ones cannot.
            en.start.setText("2026-10-30 22:45");
            en.end.setText("2026-12-11 22:45");
            en.step.setSelectedIndex(3);
            en.apply();
            List<JovianTableSession.Entry> entries = session.result().entries();
            assertEquals(7, entries.size(), "seven weekly instants");
            for (JovianTableControls c : List.of(en, nb)) {
                SolarTableWords w = c.words();
                assertEquals(7, c.model.groups());
                assertEquals(7 * 7, c.model.getRowCount(), "seven lines a group");
                List<String> rightAscensions = new ArrayList<>();
                List<String> declinations = new ArrayList<>();
                for (int g = 0; g < entries.size(); g++) {
                    JovianTableSession.Entry entry = entries.get(g);
                    int at = g * 7;
                    List<JovianMoonsModel.Line> lines = c.model.lines();
                    assertEquals(JovianMoonsModel.Kind.HEADING, lines.get(at).kind());
                    assertEquals(JovianMoonsModel.Kind.JUPITER_POSITION, lines.get(at + 1).kind());
                    assertEquals(JovianMoonsModel.Kind.JUPITER_FIGURE, lines.get(at + 2).kind());
                    for (int m = 0; m < 4; m++) {
                        assertEquals(JovianMoonsModel.Kind.MOON, lines.get(at + 3 + m).kind());
                        assertSame(entry.configuration().moons().get(m), lines.get(at + 3 + m).moon(),
                                "the group's moons are its instant's");
                    }
                    for (int k = 0; k < 7; k++) {
                        assertSame(entry.jupiter(), lines.get(at + k).jupiter(),
                                "every line of a group is one Jupiter result");
                    }
                    // Jupiter's eight values, exactly as the instant's card spells them.
                    List<String> v = JovianMoonsModel.jupiterValues(entry.jupiter(), w);
                    String position = (String) c.model.getValueAt(at + 1, 0);
                    String figure = (String) c.model.getValueAt(at + 2, 0);
                    assertEquals(w.say("range.jupiter.position", v.get(0), v.get(1), v.get(2), v.get(3)),
                            position);
                    assertEquals(w.say("range.jupiter.figure", v.get(4), v.get(5), v.get(6), v.get(7)),
                            figure);
                    assertTrue(position.contains(v.get(0)) && position.contains(v.get(1)),
                            "right ascension and declination present: " + position);
                    rightAscensions.add(v.get(0));
                    declinations.add(v.get(1));
                }
                assertEquals(7, new java.util.HashSet<>(rightAscensions).size(),
                        "Jupiter's right ascension moves across the range: " + rightAscensions);
                assertTrue(new java.util.HashSet<>(declinations).size() > 1,
                        "and its declination: " + declinations);
            }
            // the words a reader of each language reads
            assertTrue(((String) en.model.getValueAt(1, 0)).startsWith("Jupiter: right ascension "));
            assertTrue(((String) nb.model.getValueAt(1, 0)).startsWith("Jupiter: rektascensjon "));
            assertTrue(((String) nb.model.getValueAt(2, 0)).contains("nordpol"));
            assertEquals("Jupiter and its four moons, by instant", en.moonsHeading.getText());
            assertEquals("Jupiter og de fire månene, etter tidspunkt", nb.moonsHeading.getText());
            assertTrue(en.table.getAccessibleContext().getAccessibleName()
                    .startsWith("Jupiter and the four Galilean moons, grouped by instant"));
            // and the card's own values for one of those instants are the same strings
            en.instantView.setSelected(true);
            en.apply();
            List<String> card = new ArrayList<>();
            for (String line : JovianTableControls.CARD_LINES) {
                card.add(en.cardValue(line));
            }
            assertEquals(JovianMoonsModel.jupiterValues(
                    session.result().entries().get(0).jupiter(), en.words()), card);
        });
    }

    @Test
    void anEstimatedInstantIsMarkedAndItsStatesAreNotOfferedAsPredictions() throws Exception {
        JovianTableSession session = new JovianTableSession(
                () -> oslo(Instant.parse("2050-06-01T00:00:00Z")), () -> service);
        onEdt(() -> {
            JovianTableControls c = new JovianTableControls(session, EN, false);
            c.apply();
            assertTrue(c.cardTitle.getText().endsWith(" est."), c.cardTitle.getText());
            assertTrue(c.status().contains("not predicted times of events"), c.status());
        });
    }

    @Test
    void theIntervalsTheTableNamesAreThePacks() {
        var pack = service.pack();
        assertEquals(pack.jupiterFirstDay(), JovianSystemService.JUPITER_FIRST_DAY);
        assertEquals(pack.jupiterLastDay(), JovianSystemService.JUPITER_LAST_DAY);
        assertEquals(pack.moonsFirstDay(), JovianSystemService.MOONS_FIRST_DAY);
        assertEquals(pack.moonsLastDay(), JovianSystemService.MOONS_LAST_DAY);
    }
}
