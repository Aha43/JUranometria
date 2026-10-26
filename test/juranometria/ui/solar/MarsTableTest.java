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
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.Configuration;
import juranometria.solar.MarsService.Moon;
import juranometria.solar.MarsService.MoonPlace;
import juranometria.solar.MarsService.VisibilityState;
import juranometria.tool.MarsComparison;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mars table (issue #603, the table ruled on #601): one shared applied
 * query and result for the Controller's group and the dialog; no
 * computation and no pack read from building, opening or expanding; every
 * value shown the service's, rounded; the moons' states in words where
 * Horizons names them; the moons' exclusive end; a range grouped as one
 * Mars result - three summary lines, never left out, even where the moons
 * are not computed - and its moons; and the refusals, with the years named.
 */
class MarsTableTest {

    private static MarsService service;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** The study's "today" row (docs/studies/mars-system/horizons/named-mars-oslo.txt). */
    private static final Instant CARD = Instant.parse("2026-10-25T12:00:00Z");

    @BeforeAll
    static void load() {
        service = MarsService.load();
    }

    static Observer oslo(Instant when) {
        return MarsComparison.observer("oslo", when);
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
        Supplier<MarsService> counted = () -> {
            loaded.incrementAndGet();
            return service;
        };
        MarsTableSession session = new MarsTableSession(() -> oslo(CARD), counted);
        onEdt(() -> {
            MarsTableControls controller = new MarsTableControls(session, EN, false);
            controller.inController();
            MarsTableControls dialog = new MarsTableControls(session, EN, true);
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
        MarsTableSession session = new MarsTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            javax.swing.JComponent group = new MarsTableControls(session, EN, false).inController();
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
        MarsTableSession session = new MarsTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            MarsTableControls controller = new MarsTableControls(session, EN, false);
            controller.inController();
            MarsTableDialog.Content dialog = MarsTableDialog.content(session, NB);
            MarsTableControls d = dialog.controls();
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
        MarsTableSession session = new MarsTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            MarsTableControls c = new MarsTableControls(session, EN, false);
            c.apply();
            Configuration shown = session.result().entries().get(0).configuration();
            Configuration fresh = service.observeMoons(oslo(CARD));
            for (int row = 0; row < 2; row++) {
                assertSame(shown.moons().get(row), c.model.lines().get(row).moon());
                MoonPlace b = fresh.moons().get(row);
                for (int col = 0; col < MarsMoonsModel.COLUMNS.size(); col++) {
                    assertEquals(MarsMoonsModel.cell(b, col, c.words()), c.model.getValueAt(row, col),
                            "row " + row + " column " + col);
                }
            }
            var m = fresh.mars();
            assertEquals(String.format(Locale.ROOT, "%.3f AU", m.distanceAu()), c.cardValue("distance"));
            assertEquals(String.format(Locale.ROOT, "%.1f″ / %.1f″", m.equatorialDiameterArcseconds(),
                    m.polarDiameterArcseconds()), c.cardValue("diameter"));
            assertEquals(String.format(Locale.ROOT, "%.1f°", m.poleAngleDegrees()), c.cardValue("pole"));
            List<String> card = new ArrayList<>();
            for (String line : MarsTableControls.CARD_LINES) {
                card.add(c.cardValue(line));
            }
            assertEquals(MarsMoonsModel.marsValues(m, c.words()), card);
        });
    }

    @Test
    void theCardsInstantReadsNaturallyInBothLanguages() throws Exception {
        MarsTableSession session = new MarsTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            MarsTableControls en = new MarsTableControls(session, EN, false);
            MarsTableControls nb = new MarsTableControls(session, NB, false);
            en.apply();
            assertEquals(List.of("Phobos", "Deimos"), column(en, 0));
            assertEquals(List.of("clear of Mars", "clear of Mars"), column(en, 6),
                    "as Horizons codes the study's today (*)");
            assertEquals(List.of("klar av Mars", "klar av Mars"), column(nb, 6));
            assertEquals("Mars · 2026-10-25 12:00:00 UTC", en.cardTitle.getText());
            assertEquals("gibbous", en.cardValue("phase"));
            assertEquals("mer enn halvt belyst", nb.cardValue("phase"));
            assertTrue(en.cardValue("elongation").endsWith(" W"), "the morning sky: " + en.cardValue("elongation"));
            assertTrue(nb.cardValue("elongation").endsWith(" V"), nb.cardValue("elongation"));
            assertTrue(en.cardValue("tilt").endsWith("(north pole towards you)"), en.cardValue("tilt"));
            assertTrue(nb.cardValue("tilt").endsWith("(nordpolen mot deg)"), nb.cardValue("tilt"));
            assertEquals("Avstand fra Mars", nb.model.getColumnName(4));
            assertEquals("Marsradier", nb.model.getColumnName(5));
            assertEquals("Phobos and Deimos", en.moonsHeading.getText());
            assertEquals("Phobos og Deimos", nb.moonsHeading.getText());
            assertTrue(((String) nb.model.getValueAt(0, 4)).contains(","), "the Norwegian decimal comma");
            assertEquals("Tilt towards you: " + en.cardValue("tilt"),
                    en.cardValues.get(MarsTableControls.CARD_LINES.indexOf("tilt"))
                            .getAccessibleContext().getAccessibleName(),
                    "a screen reader hears the heading with the value");
        });
    }

    /** Horizons' codes, as the state the table says (Jupiter's precedence). */
    private static final Map<String, VisibilityState> STATE_OF_CODE = Map.of(
            "t", VisibilityState.IN_FRONT_OF_MARS,
            "O", VisibilityState.BEHIND_MARS,
            "U", VisibilityState.BEHIND_MARS_IN_ITS_SHADOW,
            "u", VisibilityState.IN_MARS_SHADOW);

    /**
     * Phobos as Horizons codes it from Oslo (matrix-7d-phobos-oslo.txt) -
     * behind Mars, behind it in its shadow, in its shadow, and in front of
     * it - is said in those words, in both languages.
     */
    @Test
    void phobosNamedConfigurationsReadAsHorizonsCodesThem() throws Exception {
        Observer[] at = {null};
        MarsTableSession session = new MarsTableSession(() -> at[0], () -> service);
        Map<String, String> rows = Map.of("2005-02-20T00:00:00Z", "O", "2005-02-27T00:00:00Z", "U",
                "2005-03-06T00:00:00Z", "u", "2005-08-07T00:00:00Z", "t");
        onEdt(() -> {
            MarsTableControls en = new MarsTableControls(session, EN, false);
            MarsTableControls nb = new MarsTableControls(session, NB, false);
            for (Map.Entry<String, String> row : rows.entrySet()) {
                at[0] = oslo(Instant.parse(row.getKey()));
                en.apply();
                MoonPlace p = session.result().entries().get(0).configuration().moon(Moon.PHOBOS);
                assertEquals(STATE_OF_CODE.get(row.getValue()), p.state(), row.getKey());
                assertEquals(MarsMoonsModel.stateWords(p.state(), en.words()), en.model.getValueAt(0, 6));
                assertEquals(MarsMoonsModel.stateWords(p.state(), nb.words()), nb.model.getValueAt(0, 6));
            }
            at[0] = oslo(Instant.parse("2005-02-27T00:00:00Z"));
            en.apply();
            assertEquals("behind Mars, in its shadow", en.model.getValueAt(0, 6));
            assertEquals("bak Mars, i skyggen", nb.model.getValueAt(0, 6));
        });
    }

    @Test
    void theMoonsLastSecondsAreRefusedByTheTdbBoundaryAndMarsIsStillShown() throws Exception {
        Observer[] at = {oslo(Instant.parse("2049-12-31T23:58:00Z"))};
        MarsTableSession session = new MarsTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            MarsTableControls en = new MarsTableControls(session, EN, false);
            MarsTableControls nb = new MarsTableControls(session, NB, false);
            en.apply();
            assertEquals(MarsTableSession.Outcome.ROWS, session.result().outcome());
            assertEquals(2, en.model.getRowCount());
            at[0] = oslo(Instant.parse("2049-12-31T23:59:30Z"));
            en.apply();
            assertEquals(MarsTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome(),
                    "the last civil seconds of 2049 lie past 2050-01-01 00:00 TDB");
            assertTrue(en.cardTitle.getText().startsWith("Mars · 2049-12-31"), "Mars still answers");
            assertEquals(0, en.model.getRowCount(), "and no moon rows");
            assertTrue(en.status().contains("2000-01-01") && en.status().contains("end of 2049")
                    && en.status().contains("only Mars"), en.status());
            assertTrue(nb.status().contains("utgangen av 2049") && nb.status().contains("bare Mars"),
                    nb.status());
            at[0] = oslo(Instant.parse("1999-12-31T23:59:59Z"));
            en.apply();
            assertEquals(MarsTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome());
        });
    }

    /**
     * Mars's own summary is never left out of a range (#603, required):
     * across the moons' end every group is one Mars result - its heading
     * and three lines - with its moons where they answer and one line
     * saying so where they do not, and the status counts them.
     */
    @Test
    void aRangeAcrossTheMoonsEndKeepsMarsAndSaysWhereTheMoonsAreNotComputed() throws Exception {
        MarsTableSession session = new MarsTableSession(
                () -> oslo(Instant.parse("2049-12-10T00:00:00Z")), () -> service);
        onEdt(() -> {
            MarsTableControls en = new MarsTableControls(session, EN, false);
            MarsTableControls nb = new MarsTableControls(session, NB, false);
            en.rangeView.setSelected(true);
            en.span.setSelectedIndex(2);
            en.step.setSelectedIndex(3); // 30 days, weekly, across 2050-01-01 00:00 TDB
            en.apply();
            MarsTableSession.Result r = session.result();
            assertEquals(MarsTableSession.Outcome.ROWS_MOONS_IN_PART, r.outcome());
            assertEquals(5, r.entries().size());
            int without = 0;
            for (MarsTableSession.Entry e : r.entries()) {
                without += e.configuration() == null ? 1 : 0;
                assertEquals(!service.moonsAnswerAt(e.sample().instant()), e.configuration() == null,
                        "the moons are left out exactly where the boundary refuses them: "
                                + e.sample().instant());
            }
            assertTrue(without > 0 && without < r.entries().size(), "the range crosses the end: " + without);
            assertEquals(Integer.toString(without), r.detail());
            int with = r.entries().size() - without;
            for (MarsTableControls c : List.of(en, nb)) {
                List<MarsMoonsModel.Line> lines = c.model.lines();
                assertEquals(5, c.model.groups());
                assertEquals(with * 6 + without * 5, lines.size(),
                        "with moons: heading, three lines, two moons; without: heading, three lines, one refusal");
                int i = 0;
                for (MarsTableSession.Entry e : r.entries()) {
                    assertEquals(MarsMoonsModel.Kind.HEADING, lines.get(i).kind());
                    assertEquals(MarsMoonsModel.Kind.MARS_POSITION, lines.get(i + 1).kind());
                    assertEquals(MarsMoonsModel.Kind.MARS_FIGURE, lines.get(i + 2).kind());
                    assertEquals(MarsMoonsModel.Kind.MARS_LIGHT, lines.get(i + 3).kind());
                    assertSame(e.mars(), lines.get(i + 1).mars(), "one Mars result a group");
                    if (e.configuration() == null) {
                        assertEquals(MarsMoonsModel.Kind.MOONS_REFUSED, lines.get(i + 4).kind());
                        i += 5;
                    } else {
                        assertSame(e.configuration().moons().get(0), lines.get(i + 4).moon());
                        assertSame(e.configuration().moons().get(1), lines.get(i + 5).moon());
                        i += 6;
                    }
                }
            }
            assertTrue(en.status().startsWith("5 instants, " + (2 * with) + " moon rows. At " + without
                    + " of these instants Phobos and Deimos are not shown"), en.status());
            assertTrue(nb.status().contains("Ved " + without + " av disse tidspunktene"), nb.status());
            assertTrue(((String) en.model.getValueAt(3, 0)).startsWith("Mars: illuminated "));
            assertTrue(((String) nb.model.getValueAt(3, 0)).startsWith("Mars: belyst "));
        });
    }

    @Test
    void theRefusalsNameWhatTheyRefuse() throws Exception {
        Observer[] at = {null};
        MarsTableSession session = new MarsTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            MarsTableControls c = new MarsTableControls(session, EN, false);
            c.apply();
            assertEquals(MarsTableSession.Outcome.OBSERVER_ABSENT, session.result().outcome());
            assertEquals(0, session.computations());

            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            c.apply();
            assertEquals(MarsTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertTrue(c.status().startsWith("Mars is computed for civil dates from 1900-01-01"
                    + " to 2100-12-31"), c.status());

            at[0] = oslo(CARD);
            c.rangeView.setSelected(true);
            c.span.setSelectedIndex(2); // 30 days of hours: 721 instants, over the cap
            c.step.setSelectedIndex(0);
            c.apply();
            assertEquals(MarsTableSession.Outcome.REFUSED_ROWS, session.result().outcome());
            assertTrue(c.status().contains("at most 500 are shown, two moon rows each"), c.status());
        });
    }

    @Test
    void aDateLineObserverIsAnsweredForItsOwnPlace() throws Exception {
        Observer chatham = MarsComparison.observer("chatham", CARD);
        MarsTableSession session = new MarsTableSession(() -> chatham, () -> service);
        onEdt(() -> {
            MarsTableControls c = new MarsTableControls(session, EN, false);
            c.apply();
            assertEquals(MarsMoonsModel.marsValues(service.observe(chatham), c.words()).get(2),
                    c.cardValue("altitude"), "Chatham's own altitude");
            assertTrue(c.observerNote.getText().contains("-176.5") || c.observerNote.getText().contains("−176.5"),
                    "west of the date line, as Place and Time folds it: " + c.observerNote.getText());
        });
    }

    @Test
    void anEstimatedInstantIsMarkedAndItsStatesAreNotOfferedAsPredictions() throws Exception {
        MarsTableSession session = new MarsTableSession(
                () -> oslo(Instant.parse("2040-06-01T00:00:00Z")), () -> service);
        onEdt(() -> {
            MarsTableControls c = new MarsTableControls(session, EN, false);
            c.apply();
            assertTrue(c.cardTitle.getText().endsWith(" est."), c.cardTitle.getText());
            assertTrue(c.status().contains("not predicted times of events"), c.status());
        });
    }

    @Test
    void theIntervalsTheTableNamesAreTheServices() {
        assertEquals(MarsService.MARS_FIRST_DAY, service.firstDay());
        assertEquals(MarsService.MARS_LAST_DAY, service.lastDay());
        assertEquals(500, MarsTableSession.MAX_INSTANTS);
        assertEquals(2, MarsTableSession.MOONS);
    }

    private static List<String> column(MarsTableControls c, int column) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < c.model.getRowCount(); row++) {
            out.add((String) c.model.getValueAt(row, column));
        }
        return out;
    }
}
