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
import juranometria.solar.NeptuneService;
import juranometria.solar.NeptuneService.Configuration;
import juranometria.solar.NeptuneService.Moon;
import juranometria.solar.NeptuneService.MoonPlace;
import juranometria.solar.NeptuneService.VisibilityState;
import juranometria.tool.NeptuneComparison;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Neptune table (issue #656, the table ruled on #654): one shared
 * applied query and result for the Controller's group and the dialog; no
 * computation and no pack read from building, opening or expanding; every
 * value shown the service's, rounded; the card N-9 rules - Saturn's
 * without the rings, the tilt and pole as Mars's - with the fraction and
 * phase angle and no phase words (N-5); the moons' states in words where
 * Horizons names them; Neptune shown outside the moons' years, as Mars's
 * table does; a range grouped as one Neptune result - three summary lines,
 * never left out - and its moons; and the refusals, with the years named.
 */
class NeptuneTableTest {

    private static NeptuneService service;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** The study's "today" row (docs/studies/neptune-system/horizons/named-*-oslo.txt). */
    private static final Instant CARD = Instant.parse("2026-10-29T12:00:00Z");

    @BeforeAll
    static void load() {
        service = NeptuneService.load();
    }

    static Observer oslo(Instant when) {
        return NeptuneComparison.observer("oslo", when);
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
        Supplier<NeptuneService> counted = () -> {
            loaded.incrementAndGet();
            return service;
        };
        NeptuneTableSession session = new NeptuneTableSession(() -> oslo(CARD), counted);
        onEdt(() -> {
            NeptuneTableControls controller = new NeptuneTableControls(session, EN, false);
            controller.inController();
            NeptuneTableControls dialog = new NeptuneTableControls(session, EN, true);
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
    void neitherHostHasAChartSwitchOrCentreOnChart() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            for (javax.swing.JComponent host : List.of(
                    new NeptuneTableControls(session, EN, false).inController(),
                    NeptuneTableDialog.content(session, EN))) {
                List<java.awt.Component> all = new ArrayList<>();
                collect(host, all);
                for (java.awt.Component c : all) {
                    assertTrue(!(c instanceof javax.swing.JCheckBox), "no Show on chart box: " + c.getName());
                    if (c instanceof javax.swing.AbstractButton b && b.getName() != null) {
                        assertTrue(!b.getName().toLowerCase(Locale.ROOT).contains("centre"),
                                "no Centre on chart: " + b.getName());
                    }
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
        NeptuneTableSession session = new NeptuneTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            NeptuneTableControls controller = new NeptuneTableControls(session, EN, false);
            controller.inController();
            NeptuneTableDialog.Content dialog = NeptuneTableDialog.content(session, NB);
            NeptuneTableControls d = dialog.controls();
            d.rangeView.setSelected(true);
            d.span.setSelectedIndex(3);
            controller.apply();
            assertSame(controller.shown(), d.shown(), "one applied result");
            assertEquals(3, d.span.getSelectedIndex(), "the dialog's draft is its own");
            assertTrue(d.rangeView.isSelected());
            assertEquals(controller.cardValue("ra"), d.cardValue("ra").replace(',', '.'),
                    "the same answer in both hosts, each in its language");
            assertEquals(2, controller.model.getRowCount());
            assertEquals(2, d.model.getRowCount());
            controller.release();
            dialog.controls().release();
            assertEquals(0, session.subscribers(), "both let go");
        });
    }

    @Test
    void everyValueShownIsTheServicesAnswerRounded() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            NeptuneTableControls c = new NeptuneTableControls(session, EN, false);
            c.apply();
            Configuration shown = session.result().entries().get(0).configuration();
            Configuration fresh = service.observeMoons(oslo(CARD));
            for (int row = 0; row < 2; row++) {
                assertSame(shown.moons().get(row), c.model.lines().get(row).moon());
                MoonPlace b = fresh.moons().get(row);
                for (int col = 0; col < NeptuneMoonsModel.COLUMNS.size(); col++) {
                    assertEquals(NeptuneMoonsModel.cell(b, col, c.words()), c.model.getValueAt(row, col),
                            "row " + row + " column " + col);
                }
            }
            var u = fresh.neptune();
            assertEquals(String.format(Locale.ROOT, "%.3f AU", u.distanceAu()), c.cardValue("distance"));
            assertEquals(String.format(Locale.ROOT, "%.1f″ / %.1f″", u.equatorialDiameterArcseconds(),
                    u.polarDiameterArcseconds()), c.cardValue("diameter"));
            assertEquals(String.format(Locale.ROOT, "%.2f°", u.phaseAngleDegrees()), c.cardValue("phaseangle"));
            assertEquals(String.format(Locale.ROOT, "%.1f°", u.poleAngleDegrees()), c.cardValue("pole"));
            List<String> card = new ArrayList<>();
            for (String line : NeptuneTableControls.CARD_LINES) {
                card.add(c.cardValue(line));
            }
            assertEquals(NeptuneMoonsModel.neptuneValues(u, c.words()), card);
        });
    }

    /** N-9 and N-5: Saturn's card without the rings, Mars's pole and tilt, no phase words. */
    @Test
    void theCardIsTheRuledOne() {
        assertEquals(List.of("ra", "dec", "altitude", "azimuth", "distance", "diameter",
                "illuminated", "phaseangle", "pole", "tilt"), NeptuneTableControls.CARD_LINES);
        assertEquals(List.of("moon", "side", "eastwest", "northsouth", "separation", "radii", "state"),
                NeptuneMoonsModel.COLUMNS, "Jupiter's columns");
    }

    @Test
    void theCardsInstantReadsNaturallyInBothLanguages() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            NeptuneTableControls en = new NeptuneTableControls(session, EN, false);
            NeptuneTableControls nb = new NeptuneTableControls(session, NB, false);
            en.apply();
            assertEquals(List.of("Triton", "Nereid"), column(en, 0));
            assertEquals(column(en, 0), column(nb, 0), "the moon names identical in both (N-9)");
            assertEquals(List.of("clear of Neptune", "clear of Neptune"), column(en, 6),
                    "as Horizons codes the study's today (*) for both");
            assertEquals("klar av Neptun", nb.model.getValueAt(1, 6));
            assertEquals("Neptune · 2026-10-29 12:00:00 UTC", en.cardTitle.getText());
            String tilt = service.observe(oslo(CARD)).northPoleTowardObserver()
                    ? "(north pole towards you)" : "(south pole towards you)";
            assertTrue(en.cardValue("tilt").endsWith(tilt), en.cardValue("tilt"));
            assertTrue(nb.cardValue("tilt").endsWith(tilt.contains("north")
                    ? "(nordpolen mot deg)" : "(sørpolen mot deg)"), nb.cardValue("tilt"));
            assertTrue(nb.cardValue("phaseangle").contains(","), "the Norwegian decimal comma");
            assertEquals("Avstand fra Neptun", nb.model.getColumnName(4));
            assertEquals("Neptunradier", nb.model.getColumnName(5));
            assertEquals("Neptune radii", en.model.getColumnName(5));
            assertEquals("Triton and Nereid", en.moonsHeading.getText());
            assertEquals("Triton og Nereid", nb.moonsHeading.getText());
            assertEquals("Phase angle: " + en.cardValue("phaseangle"),
                    en.cardValues.get(NeptuneTableControls.CARD_LINES.indexOf("phaseangle"))
                            .getAccessibleContext().getAccessibleName(),
                    "a screen reader hears the heading with the value");
        });
    }

    /** Horizons' codes, as the state the table says (Jupiter's precedence). */
    private static final Map<String, VisibilityState> STATE_OF_CODE = Map.of(
            "t", VisibilityState.IN_FRONT_OF_NEPTUNE,
            "U", VisibilityState.BEHIND_NEPTUNE_IN_ITS_SHADOW);

    /**
     * Rows Horizons codes from Oslo (matrix-7d-triton-oslo.txt) - Triton in
     * front of Neptune and behind it in its shadow - are said in those words,
     * in both languages. Triton's transits and eclipses come only in seasons
     * around Neptune's equinoxes: inside the moons' years the first fall in
     * 2043-2049, after the exact interval, and Nereid has none at all. The
     * two codes these rows carry are the only ones the moons' years offer.
     */
    @Test
    void namedConfigurationsReadAsHorizonsCodesThem() throws Exception {
        Observer[] at = {null};
        NeptuneTableSession session = new NeptuneTableSession(() -> at[0], () -> service);
        record Coded(String instant, Moon moon, String code) {
        }
        List<Coded> rows = List.of(new Coded("2043-09-07T00:00:00Z", Moon.TRITON, "t"),
                new Coded("2044-09-26T00:00:00Z", Moon.TRITON, "U"),
                new Coded("2045-05-22T00:00:00Z", Moon.TRITON, "t"),
                new Coded("2045-08-21T00:00:00Z", Moon.TRITON, "U"));
        onEdt(() -> {
            NeptuneTableControls en = new NeptuneTableControls(session, EN, false);
            NeptuneTableControls nb = new NeptuneTableControls(session, NB, false);
            for (Coded row : rows) {
                at[0] = oslo(Instant.parse(row.instant()));
                en.apply();
                MoonPlace p = session.result().entries().get(0).configuration().moon(row.moon());
                assertEquals(STATE_OF_CODE.get(row.code()), p.state(), row.instant());
                int r = row.moon().ordinal();
                assertEquals(NeptuneMoonsModel.stateWords(p.state(), en.words()), en.model.getValueAt(r, 6));
                assertEquals(NeptuneMoonsModel.stateWords(p.state(), nb.words()), nb.model.getValueAt(r, 6));
            }
            at[0] = oslo(Instant.parse("2044-09-26T00:00:00Z"));
            en.apply();
            assertEquals("behind Neptune, in its shadow", en.model.getValueAt(0, 6));
            assertEquals("bak Neptun, i skyggen", nb.model.getValueAt(0, 6));
        });
    }

    /**
     * The table says the flattened figure's state (N-7), Horizons' sphere
     * computed beside it for the comparison only. For Neptune the two never
     * part on a committed row (the accuracy report's "figure differs" is 0
     * for both moons), so this holds the table to the flattened reading at
     * Triton's 2044 eclipse rather than at a row where they differ.
     */
    @Test
    void theStateIsTheFlattenedFigures() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(
                () -> oslo(Instant.parse("2044-09-26T00:00:00Z")), () -> service);
        onEdt(() -> {
            NeptuneTableControls en = new NeptuneTableControls(session, EN, false);
            en.apply();
            MoonPlace triton = session.result().entries().get(0).configuration().moon(Moon.TRITON);
            assertEquals(VisibilityState.of(triton.discRelation(), triton.shadowRelation()), triton.state());
            assertEquals(NeptuneMoonsModel.stateWords(triton.state(), en.words()), en.model.getValueAt(0, 6));
        });
    }

    @Test
    void beforeTheMoonsYearsNeptuneIsShownAloneAndTheRefusalNamesThem() throws Exception {
        Observer[] at = {oslo(Instant.parse("2000-01-01T00:00:00Z"))};
        NeptuneTableSession session = new NeptuneTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            NeptuneTableControls en = new NeptuneTableControls(session, EN, false);
            NeptuneTableControls nb = new NeptuneTableControls(session, NB, false);
            en.apply();
            assertEquals(NeptuneTableSession.Outcome.ROWS, session.result().outcome(),
                    "the moons' first day answers");
            assertEquals(2, en.model.getRowCount());
            at[0] = oslo(Instant.parse("1999-12-31T23:59:59Z"));
            en.apply();
            assertEquals(NeptuneTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome());
            assertTrue(en.cardTitle.getText().startsWith("Neptune · 1999-12-31"), "Neptune still answers");
            assertEquals(0, en.model.getRowCount(), "and no moon rows");
            assertTrue(en.status().contains("2000-01-01") && en.status().contains("2100-12-31")
                    && en.status().contains("only Neptune"), en.status());
            assertTrue(nb.status().contains("2100-12-31") && nb.status().contains("bare Neptun"),
                    nb.status());
            at[0] = oslo(Instant.parse("2100-12-31T23:59:00Z"));
            en.apply();
            assertEquals(NeptuneTableSession.Outcome.ROWS, session.result().outcome(),
                    "the moons' last day answers to its end");
        });
    }

    /**
     * Neptune's own summary is never left out of a range (#656, required):
     * across the moons' start every group is one Neptune result - its
     * heading and three lines - with its moons where they answer and one
     * line saying so where they do not, and the status counts them.
     */
    @Test
    void aRangeAcrossTheMoonsStartKeepsNeptuneAndSaysWhereTheMoonsAreNotComputed() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(
                () -> oslo(Instant.parse("1999-12-10T00:00:00Z")), () -> service);
        onEdt(() -> {
            NeptuneTableControls en = new NeptuneTableControls(session, EN, false);
            NeptuneTableControls nb = new NeptuneTableControls(session, NB, false);
            en.rangeView.setSelected(true);
            en.span.setSelectedIndex(2);
            en.step.setSelectedIndex(3); // 30 days, weekly, across 2000-01-01
            en.apply();
            NeptuneTableSession.Result r = session.result();
            assertEquals(NeptuneTableSession.Outcome.ROWS_MOONS_IN_PART, r.outcome());
            assertEquals(5, r.entries().size());
            int without = 0;
            for (NeptuneTableSession.Entry e : r.entries()) {
                without += e.configuration() == null ? 1 : 0;
                assertEquals(!service.moonsAnswerAt(e.sample().instant()), e.configuration() == null,
                        "the moons are left out exactly where the service refuses them: "
                                + e.sample().instant());
            }
            assertTrue(without > 0 && without < r.entries().size(), "the range crosses the start: " + without);
            assertEquals(Integer.toString(without), r.detail());
            int with = r.entries().size() - without;
            for (NeptuneTableControls c : List.of(en, nb)) {
                List<NeptuneMoonsModel.Line> lines = c.model.lines();
                assertEquals(5, c.model.groups());
                assertEquals(with * 6 + without * 5, lines.size(),
                        "with moons: heading, three lines, two moons; without: heading, three lines, one refusal");
                int i = 0;
                for (NeptuneTableSession.Entry e : r.entries()) {
                    assertEquals(NeptuneMoonsModel.Kind.HEADING, lines.get(i).kind());
                    assertEquals(NeptuneMoonsModel.Kind.NEPTUNE_POSITION, lines.get(i + 1).kind());
                    assertEquals(NeptuneMoonsModel.Kind.NEPTUNE_FIGURE, lines.get(i + 2).kind());
                    assertEquals(NeptuneMoonsModel.Kind.NEPTUNE_LIGHT, lines.get(i + 3).kind());
                    assertSame(e.neptune(), lines.get(i + 1).neptune(), "one Neptune result a group");
                    if (e.configuration() == null) {
                        assertEquals(NeptuneMoonsModel.Kind.MOONS_REFUSED, lines.get(i + 4).kind());
                        i += 5;
                    } else {
                        for (int m = 0; m < 2; m++) {
                            assertSame(e.configuration().moons().get(m), lines.get(i + 4 + m).moon());
                        }
                        i += 6;
                    }
                }
            }
            assertTrue(en.status().startsWith("5 instants, " + (2 * with) + " moon rows. At " + without
                    + " of these instants the moons are not shown"), en.status());
            assertTrue(nb.status().contains("Ved " + without + " av disse tidspunktene"), nb.status());
            assertTrue(((String) en.model.getValueAt(3, 0)).startsWith("Neptune: illuminated "));
            assertTrue(((String) nb.model.getValueAt(3, 0)).startsWith("Neptun: belyst "));
        });
    }

    @Test
    void theRefusalsNameWhatTheyRefuse() throws Exception {
        Observer[] at = {null};
        NeptuneTableSession session = new NeptuneTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            NeptuneTableControls c = new NeptuneTableControls(session, EN, false);
            c.apply();
            assertEquals(NeptuneTableSession.Outcome.OBSERVER_ABSENT, session.result().outcome());
            assertEquals(0, session.computations());

            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            c.apply();
            assertEquals(NeptuneTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertTrue(c.status().startsWith("Neptune is computed for civil dates from 1900-01-01"
                    + " to 2100-12-31"), c.status());

            at[0] = oslo(CARD);
            c.rangeView.setSelected(true);
            c.span.setSelectedIndex(2); // 30 days of hours: 721 instants, over the cap of 500
            c.step.setSelectedIndex(0);
            c.apply();
            assertEquals(NeptuneTableSession.Outcome.REFUSED_ROWS, session.result().outcome());
            assertTrue(c.status().contains("at most 500 are shown, two moon rows each"), c.status());
        });
    }

    @Test
    void aDateLineObserverIsAnsweredForItsOwnPlace() throws Exception {
        Observer chatham = NeptuneComparison.observer("chatham", CARD);
        NeptuneTableSession session = new NeptuneTableSession(() -> chatham, () -> service);
        onEdt(() -> {
            NeptuneTableControls c = new NeptuneTableControls(session, EN, false);
            c.apply();
            assertEquals(NeptuneMoonsModel.neptuneValues(service.observe(chatham), c.words()).get(2),
                    c.cardValue("altitude"), "Chatham's own altitude");
            assertTrue(c.observerNote.getText().contains("-176.5") || c.observerNote.getText().contains("−176.5"),
                    "west of the date line, as Place and Time folds it: " + c.observerNote.getText());
        });
    }

    @Test
    void anEstimatedInstantIsMarkedAndItsStatesAreNotOfferedAsPredictions() throws Exception {
        NeptuneTableSession session = new NeptuneTableSession(
                () -> oslo(Instant.parse("2040-06-01T00:00:00Z")), () -> service);
        onEdt(() -> {
            NeptuneTableControls c = new NeptuneTableControls(session, EN, false);
            c.apply();
            assertTrue(c.cardTitle.getText().endsWith(" est."), c.cardTitle.getText());
            assertTrue(c.status().contains("not predicted times of events"), c.status());
        });
    }

    @Test
    void theIntervalsTheTableNamesAreThePacks() {
        assertEquals(NeptuneService.NEPTUNE_FIRST_DAY, service.firstDay());
        assertEquals(NeptuneService.NEPTUNE_LAST_DAY, service.lastDay());
        assertEquals(NeptuneService.MOONS_FIRST_DAY, service.moonsFirstDay());
        assertEquals(NeptuneService.MOONS_LAST_DAY, service.moonsLastDay());
        assertEquals(500, NeptuneTableSession.MAX_INSTANTS);
        assertEquals(2, NeptuneTableSession.MOONS);
    }

    private static List<String> column(NeptuneTableControls c, int column) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < c.model.getRowCount(); row++) {
            out.add((String) c.model.getValueAt(row, column));
        }
        return out;
    }
}
