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
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.tool.SaturnComparison;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Saturn table (issue #576, the table ruled on #574): one shared
 * applied query and result for the Controller's group and the dialog; no
 * computation and no pack read from building, opening or expanding;
 * every value shown the service's, rounded; the ring openings with their
 * faces, edge-on and the unlit face in words; the card's instant and
 * every 2026 day Horizons names reading in both languages; a range
 * grouped as one Saturn result - three summary lines, never left out -
 * and its seven moons; and the refusals, with the years named.
 */
class SaturnTableTest {

    private static SaturnSystemService service;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** The card's instant ruled on #574: the 2026 daily Oslo row nearest the Earth. */
    private static final Instant CARD = Instant.parse("2026-10-05T00:00:00Z");

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    static Observer oslo(Instant when) {
        return SaturnComparison.observer("oslo", when);
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
        Supplier<SaturnSystemService> counted = () -> {
            loaded.incrementAndGet();
            return service;
        };
        Observer[] observer = {oslo(CARD)};
        SaturnTableSession session = new SaturnTableSession(() -> observer[0], counted);
        onEdt(() -> {
            SaturnTableControls controller = new SaturnTableControls(session, EN, false);
            controller.inController();
            SaturnTableControls dialog = new SaturnTableControls(session, EN, true);
            dialog.inDialog();
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
    void theControllerGroupHasNoChartSwitchAndNoCentreOnChart() throws Exception {
        SaturnTableSession session = new SaturnTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            javax.swing.JComponent group = new SaturnTableControls(session, EN, false).inController();
            List<java.awt.Component> all = new ArrayList<>();
            collect(group, all);
            for (java.awt.Component c : all) {
                assertTrue(!(c instanceof javax.swing.JCheckBox), "no Show on chart box: " + c.getName());
                if (c instanceof javax.swing.AbstractButton b && b.getName() != null) {
                    assertTrue(!b.getName().toLowerCase(Locale.ROOT).contains("centre"),
                            "no Centre on chart: " + b.getName());
                }
            }
        });
    }

    private static void collect(java.awt.Component c, List<java.awt.Component> out) {
        out.add(c);
        if (c instanceof java.awt.Container k) {
            for (java.awt.Component child : k.getComponents()) {
                collect(child, out);
            }
        }
    }

    @Test
    void bothHostsShowOneAppliedResultAndNeitherOverwritesTheOthersDraft() throws Exception {
        SaturnTableSession session = new SaturnTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            SaturnTableControls controller = new SaturnTableControls(session, EN, false);
            controller.inController();
            SaturnTableDialog.Content dialog = SaturnTableDialog.content(session, NB);
            SaturnTableControls d = dialog.controls();
            d.rangeView.setSelected(true);
            d.span.setSelectedIndex(3);
            controller.apply();
            assertSame(controller.shown(), d.shown(), "one applied result");
            assertEquals(3, d.span.getSelectedIndex(), "the dialog's draft is its own");
            assertTrue(d.rangeView.isSelected());
            assertEquals(controller.cardValue("ra"), d.cardValue("ra").replace(',', '.'),
                    "the same answer in both hosts, each in its language");
            assertEquals(7, controller.model.getRowCount());
            assertEquals(7, d.model.getRowCount());
            controller.release();
            dialog.controls().release();
            assertEquals(0, session.subscribers(), "both let go");
        });
    }

    @Test
    void everyValueShownIsTheServicesAnswerRounded() throws Exception {
        SaturnTableSession session = new SaturnTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            SaturnTableControls c = new SaturnTableControls(session, EN, false);
            c.apply();
            Configuration shown = session.result().entries().get(0).configuration();
            Configuration fresh = service.observeMoons(oslo(CARD));
            for (int row = 0; row < 7; row++) {
                assertSame(shown.moons().get(row), c.model.lines().get(row).moon());
                MoonPlace a = shown.moons().get(row);
                MoonPlace b = fresh.moons().get(row);
                assertEquals(b.xArcseconds(), a.xArcseconds());
                assertEquals(b.separationSaturnRadii(), a.separationSaturnRadii());
                assertEquals(b.state(), a.state());
                for (int col = 0; col < SaturnMoonsModel.COLUMNS.size(); col++) {
                    assertEquals(SaturnMoonsModel.cell(b, col, c.words()), c.model.getValueAt(row, col),
                            "row " + row + " column " + col);
                }
            }
            var s = fresh.saturn();
            assertEquals(String.format(Locale.ROOT, "%.3f AU", s.distanceAu()), c.cardValue("distance"));
            assertEquals(String.format(Locale.ROOT, "%.1f″ / %.1f″", s.equatorialDiameterArcseconds(),
                    s.polarDiameterArcseconds()), c.cardValue("diameter"));
            assertEquals(String.format(Locale.ROOT, "%.1f°", s.poleAngleDegrees()), c.cardValue("pole"));
            assertEquals(String.format(Locale.ROOT, "%.2f° (south face)", -s.ringOpeningDegrees()),
                    c.cardValue("ringOpening"));
            double outer = s.ringRadiiKm().get(3);
            assertEquals(String.format(Locale.ROOT, "%.1f″ × %.1f″", 2 * s.ringSemiMajorArcseconds(outer),
                    2 * s.ringSemiMinorArcseconds(outer)), c.cardValue("ringSize"));
            List<String> card = new ArrayList<>();
            for (String line : SaturnTableControls.CARD_LINES) {
                card.add(c.cardValue(line));
            }
            assertEquals(SaturnMoonsModel.saturnValues(s, c.words()), card);
        });
    }

    @Test
    void theCardsInstantReadsNaturallyInBothLanguages() throws Exception {
        SaturnTableSession session = new SaturnTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            SaturnTableControls en = new SaturnTableControls(session, EN, false);
            SaturnTableControls nb = new SaturnTableControls(session, NB, false);
            en.apply();
            assertEquals(List.of("Mimas", "Enceladus", "Tethys", "Dione", "Rhea", "Titan", "Iapetus"),
                    column(en, 0));
            assertEquals(List.of("clear of Saturn", "clear of Saturn", "behind Saturn, in its shadow",
                    "clear of Saturn", "clear of Saturn", "clear of Saturn", "clear of Saturn"), column(en, 6),
                    "as Horizons codes the card's instant (#574)");
            assertEquals(List.of("klar av Saturn", "klar av Saturn", "bak Saturn, i skyggen", "klar av Saturn",
                    "klar av Saturn", "klar av Saturn", "klar av Saturn"), column(nb, 6));
            assertEquals("Saturn · 2026-10-05 00:00:00 UTC", en.cardTitle.getText());
            assertEquals("7.43° (south face)", en.cardValue("ringOpening"));
            assertEquals("7,43° (sørsiden)", nb.cardValue("ringOpening"));
            assertTrue(!en.cardValue("ringSun").contains("unlit"), "the lit face is seen in October 2026");
            assertEquals("Separation", en.model.getColumnName(4));
            assertEquals("Avstand fra Saturn", nb.model.getColumnName(4), "Saturn's own heading (#574)");
            assertEquals("Saturnradier", nb.model.getColumnName(5));
            assertEquals("The seven moons", en.moonsHeading.getText());
            assertEquals("De sju månene", nb.moonsHeading.getText());
            assertTrue(((String) nb.model.getValueAt(0, 4)).contains(","), "the Norwegian decimal comma");
            assertEquals("Rings, opening to the observer: 7.43° (south face)",
                    en.cardValues.get(SaturnTableControls.CARD_LINES.indexOf("ringOpening"))
                            .getAccessibleContext().getAccessibleName(),
                    "a screen reader hears the heading with the value");
        });
    }

    @Test
    void theRingsReadEdgeOnAndTheUnlitFaceAroundThe2025Crossings() throws Exception {
        Observer[] at = {oslo(Instant.parse("2025-03-23T17:51:00Z"))};
        SaturnTableSession session = new SaturnTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            SaturnTableControls en = new SaturnTableControls(session, EN, false);
            SaturnTableControls nb = new SaturnTableControls(session, NB, false);
            en.apply();
            assertEquals("0.00° (edge-on)", en.cardValue("ringOpening"),
                    "the Earth's 2025 ring-plane crossing, at Horizons' minute (#575)");
            assertEquals("0,00° (på kant)", nb.cardValue("ringOpening"));
            // Between the Earth's crossing and the Sun's (2025-05-06) the
            // two are on opposite sides of the ring plane.
            at[0] = oslo(Instant.parse("2025-04-15T00:00:00Z"));
            en.apply();
            assertTrue(service.observeSaturn(at[0]).unlitFaceSeen());
            assertTrue(en.cardValue("ringSun").endsWith(" · the unlit face is seen"), en.cardValue("ringSun"));
            assertTrue(nb.cardValue("ringSun").endsWith(" · den ubelyste siden sees"), nb.cardValue("ringSun"));
        });
    }

    @Test
    void aDateLineObserverIsAnsweredForItsOwnPlace() throws Exception {
        Observer chatham = SaturnComparison.observer("chatham", CARD);
        SaturnTableSession session = new SaturnTableSession(() -> chatham, () -> service);
        onEdt(() -> {
            SaturnTableControls c = new SaturnTableControls(session, EN, false);
            c.apply();
            assertEquals(SaturnMoonsModel.saturnValues(service.observeSaturn(chatham), c.words()).get(2),
                    c.cardValue("altitude"), "Chatham's own altitude");
            assertTrue(c.observerNote.getText().contains("-176.5") || c.observerNote.getText().contains("−176.5"),
                    "west of the date line, as Place and Time folds it: " + c.observerNote.getText());
        });
    }

    private static List<String> column(SaturnTableControls c, int column) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < c.model.getRowCount(); row++) {
            out.add((String) c.model.getValueAt(row, column));
        }
        return out;
    }

    /** Horizons' codes as the state the table says (Jupiter's precedence). */
    private static final Map<String, VisibilityState> STATE_OF_CODE = Map.of(
            "t", VisibilityState.IN_FRONT_OF_SATURN,
            "O", VisibilityState.BEHIND_SATURN,
            "U", VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW,
            "P", VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW,
            "u", VisibilityState.IN_SATURNS_SHADOW,
            "p", VisibilityState.PARTLY_IN_SATURNS_SHADOW,
            "*", VisibilityState.CLEAR_OF_SATURN);

    /**
     * Every day of 2026 at Oslo, each moon's state in words is Horizons'
     * wherever the reader's flattened figure and Horizons' sphere agree,
     * and the reader's where they differ - the near-limb rows #575 names,
     * a handful in the year.
     */
    @Test
    void every2026DayAtOsloReadsHorizonsStateInWordsWhereTheFiguresAgree() throws Exception {
        Observer[] at = {null};
        SaturnTableSession session = new SaturnTableSession(() -> at[0], () -> service);
        int[] checked = new int[2];
        List<String> seen = new ArrayList<>();
        onEdt(() -> {
            SaturnTableControls en = new SaturnTableControls(session, EN, false);
            SaturnTableControls nb = new SaturnTableControls(session, NB, false);
            try {
                for (Moon moon : Moon.values()) {
                    String body = moon.name().toLowerCase(Locale.ROOT);
                    String name = "dense-2026-" + body + "-oslo";
                    int vis = SaturnComparison.column(SaturnComparison.header(name), "vis.");
                    for (String[] f : SaturnComparison.rows(name)) {
                        Instant when = SaturnComparison.stamp(f[0]);
                        String code = f[vis].replace("/", "").strip();
                        code = code.isEmpty() ? "*" : code;
                        at[0] = oslo(when);
                        en.apply();
                        MoonPlace p = session.result().entries().get(0).configuration().moon(moon);
                        VisibilityState sphere = VisibilityState.of(p.sphericalDiscRelation(),
                                p.sphericalShadowRelation());
                        VisibilityState expected = STATE_OF_CODE.get(code);
                        assertNotNull(expected, "a code Horizons uses: " + code);
                        int row = moon.ordinal();
                        if (sphere == p.state()) {
                            assertEquals(expected, p.state(), body + " at " + when + " (" + code + ")");
                        } else {
                            checked[1]++;
                        }
                        assertEquals(SaturnMoonsModel.stateWords(p.state(), en.words()),
                                en.model.getValueAt(row, 6), body + " at " + when);
                        assertEquals(SaturnMoonsModel.stateWords(p.state(), nb.words()),
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
        assertEquals(7 * 365, checked[0], "seven moons, every day of 2026");
        assertTrue(checked[1] < 20, "the flattened figure differs from the sphere at a few near-limb rows: "
                + checked[1]);
        assertTrue(seen.containsAll(List.of("t", "O", "u", "*")),
                "in front, behind, in the shadow and clear all appear in the year: " + seen);
    }

    @Test
    void theRefusalsNameWhatTheyRefuse() throws Exception {
        Observer[] at = {null};
        SaturnTableSession session = new SaturnTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            SaturnTableControls c = new SaturnTableControls(session, EN, false);
            c.apply();
            assertEquals(SaturnTableSession.Outcome.OBSERVER_ABSENT, session.result().outcome());
            assertEquals(0, session.computations());

            at[0] = oslo(Instant.parse("1950-06-01T00:00:00Z"));
            c.apply();
            assertEquals(SaturnTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome());
            assertTrue(c.cardTitle.getText().startsWith("Saturn · 1950-06-01"),
                    "Saturn still answers, rings and all: " + c.cardTitle.getText());
            assertTrue(c.cardValue("ringOpening").endsWith("face)"), c.cardValue("ringOpening"));
            assertEquals(0, c.model.getRowCount(), "and no moon rows");
            assertTrue(c.status().contains("2000-01-01") && c.status().contains("2100-12-31")
                    && c.status().contains("only Saturn"), c.status());

            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            c.apply();
            assertEquals(SaturnTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertTrue(c.status().startsWith("Saturn is computed for civil dates from 1900-01-01"
                    + " to 2100-12-31"), c.status());

            at[0] = oslo(CARD);
            c.rangeView.setSelected(true);
            c.span.setSelectedIndex(1); // a week of hours: 169 instants, over the cap
            c.step.setSelectedIndex(0);
            c.apply();
            assertEquals(SaturnTableSession.Outcome.REFUSED_ROWS, session.result().outcome());
            assertTrue(c.status().contains("at most 142 are shown, seven moon rows each"), c.status());
            at[0] = oslo(Instant.parse("1999-12-30T00:00:00Z"));
            c.span.setSelectedIndex(1);
            c.step.setSelectedIndex(2);
            c.apply();
            assertEquals(SaturnTableSession.Outcome.REFUSED_MOONS_INTERVAL, session.result().outcome());
            assertTrue(c.status().contains("1999-12-30"), c.status());
        });
    }

    @Test
    void aRangeIsGroupedByInstantAndStaysUnderTheCapOf142Instants() throws Exception {
        SaturnTableSession session = new SaturnTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            SaturnTableControls c = new SaturnTableControls(session, EN, false);
            c.rangeView.setSelected(true);
            c.span.setSelectedIndex(0);
            c.step.setSelectedIndex(0); // a day of hours from the chart time: 25 instants
            c.apply();
            assertEquals(SaturnTableSession.Outcome.ROWS, session.result().outcome());
            assertEquals(25 * 11, c.model.getRowCount(),
                    "25 groups: a heading, three lines of Saturn, seven moons each");
            assertEquals("2026-10-05 00:00 UTC", c.model.getValueAt(0, 0),
                    "the first group is the chart time");
            assertEquals("2026-10-05 01:00 UTC", c.model.getValueAt(11, 0));
            assertEquals(" ", c.cardTitle.getText(), "a range shows no Saturn card");
            c.span.setSelectedIndex(2);
            c.step.setSelectedIndex(1); // 30 days of 6-hour steps: 121 instants, under the cap
            c.apply();
            assertEquals(121, session.result().entries().size(), "under the cap, answered");
            assertEquals(121 * 7, c.model.moonRows());
        });
    }

    /**
     * Saturn's own summary is never left out of a range (#576, required):
     * every group is one Saturn result - the instant's heading, then its
     * position, figure and rings, the card's eleven values as the card
     * spells them - and its seven moons, in both languages.
     */
    @Test
    void everyRangeGroupIsOneSaturnResultAndItsSevenMoonsInBothLanguages() throws Exception {
        // Five weeks ending at the card's instant, from the chart time
        // four weeks before it (#612: a range starts at the chart time).
        SaturnTableSession session = new SaturnTableSession(
                () -> oslo(CARD.minus(java.time.Duration.ofDays(28))), () -> service);
        onEdt(() -> {
            SaturnTableControls en = new SaturnTableControls(session, EN, false);
            SaturnTableControls nb = new SaturnTableControls(session, NB, false);
            en.rangeView.setSelected(true);
            en.span.setSelectedIndex(2);
            en.step.setSelectedIndex(3);
            en.apply();
            List<SaturnTableSession.Entry> entries = session.result().entries();
            assertEquals(5, entries.size(), "five weekly instants");
            assertEquals(CARD, entries.get(4).sample().instant(), "ending at the card's instant");
            for (SaturnTableControls c : List.of(en, nb)) {
                SolarTableWords w = c.words();
                assertEquals(5, c.model.groups());
                assertEquals(5 * 11, c.model.getRowCount(), "eleven lines a group");
                List<String> rightAscensions = new ArrayList<>();
                for (int g = 0; g < entries.size(); g++) {
                    SaturnTableSession.Entry entry = entries.get(g);
                    int at = g * 11;
                    List<SaturnMoonsModel.Line> lines = c.model.lines();
                    assertEquals(SaturnMoonsModel.Kind.HEADING, lines.get(at).kind());
                    assertEquals(SaturnMoonsModel.Kind.SATURN_POSITION, lines.get(at + 1).kind());
                    assertEquals(SaturnMoonsModel.Kind.SATURN_FIGURE, lines.get(at + 2).kind());
                    assertEquals(SaturnMoonsModel.Kind.SATURN_RINGS, lines.get(at + 3).kind());
                    for (int m = 0; m < 7; m++) {
                        assertSame(entry.configuration().moons().get(m), lines.get(at + 4 + m).moon());
                    }
                    for (int k = 0; k < 11; k++) {
                        assertSame(entry.saturn(), lines.get(at + k).saturn(), "one Saturn result a group");
                    }
                    List<String> v = SaturnMoonsModel.saturnValues(entry.saturn(), w);
                    assertEquals(w.say("range.saturn.position", v.get(0), v.get(1), v.get(2), v.get(3)),
                            c.model.getValueAt(at + 1, 0));
                    assertEquals(w.say("range.saturn.figure", v.get(4), v.get(5), v.get(6), v.get(7)),
                            c.model.getValueAt(at + 2, 0));
                    assertEquals(w.say("range.saturn.rings", v.get(8), v.get(9), v.get(10)),
                            c.model.getValueAt(at + 3, 0));
                    rightAscensions.add(v.get(0));
                }
                assertEquals(5, new java.util.HashSet<>(rightAscensions).size(),
                        "Saturn's right ascension moves across the range: " + rightAscensions);
            }
            assertTrue(((String) en.model.getValueAt(3, 0)).startsWith("Rings: open "));
            assertTrue(((String) nb.model.getValueAt(3, 0)).startsWith("Ringene: åpnet "));
            assertEquals("Saturn and its seven moons, by instant", en.moonsHeading.getText());
            assertEquals("Saturn og de sju månene, etter tidspunkt", nb.moonsHeading.getText());
            assertTrue(en.table.getAccessibleContext().getAccessibleName()
                    .startsWith("Saturn and its seven moons, grouped by instant"));
        });
    }

    @Test
    void anEstimatedInstantIsMarkedAndItsStatesAreNotOfferedAsPredictions() throws Exception {
        SaturnTableSession session = new SaturnTableSession(
                () -> oslo(Instant.parse("2050-06-01T00:00:00Z")), () -> service);
        onEdt(() -> {
            SaturnTableControls c = new SaturnTableControls(session, EN, false);
            c.apply();
            assertTrue(c.cardTitle.getText().endsWith(" est."), c.cardTitle.getText());
            assertTrue(c.status().contains("not predicted times of events"), c.status());
        });
    }

    @Test
    void theIntervalsTheTableNamesAreThePacks() {
        var pack = service.pack();
        assertEquals(pack.saturnFirstDay(), SaturnSystemService.SATURN_FIRST_DAY);
        assertEquals(pack.saturnLastDay(), SaturnSystemService.SATURN_LAST_DAY);
        assertEquals(pack.moonsFirstDay(), SaturnSystemService.MOONS_FIRST_DAY);
        assertEquals(pack.moonsLastDay(), SaturnSystemService.MOONS_LAST_DAY);
        assertEquals(142, SaturnTableSession.MAX_INSTANTS);
    }
}
