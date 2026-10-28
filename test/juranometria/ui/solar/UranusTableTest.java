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
import juranometria.solar.UranusService;
import juranometria.solar.UranusService.Configuration;
import juranometria.solar.UranusService.Moon;
import juranometria.solar.UranusService.MoonPlace;
import juranometria.solar.UranusService.VisibilityState;
import juranometria.tool.UranusComparison;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Uranus table (issue #635, the table ruled on #633): one shared
 * applied query and result for the Controller's group and the dialog; no
 * computation and no pack read from building, opening or expanding; every
 * value shown the service's, rounded; the card U-9 rules - Saturn's
 * without the rings, the tilt and pole as Mars's - with the fraction and
 * phase angle and no phase words (U-5); the moons' states in words where
 * Horizons names them; Uranus shown outside the moons' years, as Mars's
 * table does; a range grouped as one Uranus result - three summary lines,
 * never left out - and its moons; and the refusals, with the years named.
 */
class UranusTableTest {

    private static UranusService service;
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    /** The study's "today" row (docs/studies/uranus-system/horizons/named-*-oslo.txt). */
    private static final Instant CARD = Instant.parse("2026-10-27T12:00:00Z");

    @BeforeAll
    static void load() {
        service = UranusService.load();
    }

    static Observer oslo(Instant when) {
        return UranusComparison.observer("oslo", when);
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
        Supplier<UranusService> counted = () -> {
            loaded.incrementAndGet();
            return service;
        };
        UranusTableSession session = new UranusTableSession(() -> oslo(CARD), counted);
        onEdt(() -> {
            UranusTableControls controller = new UranusTableControls(session, EN, false);
            controller.inController();
            UranusTableControls dialog = new UranusTableControls(session, EN, true);
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
        UranusTableSession session = new UranusTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            for (javax.swing.JComponent host : List.of(
                    new UranusTableControls(session, EN, false).inController(),
                    UranusTableDialog.content(session, EN))) {
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
        UranusTableSession session = new UranusTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            UranusTableControls controller = new UranusTableControls(session, EN, false);
            controller.inController();
            UranusTableDialog.Content dialog = UranusTableDialog.content(session, NB);
            UranusTableControls d = dialog.controls();
            d.rangeView.setSelected(true);
            d.span.setSelectedIndex(3);
            controller.apply();
            assertSame(controller.shown(), d.shown(), "one applied result");
            assertEquals(3, d.span.getSelectedIndex(), "the dialog's draft is its own");
            assertTrue(d.rangeView.isSelected());
            assertEquals(controller.cardValue("ra"), d.cardValue("ra").replace(',', '.'),
                    "the same answer in both hosts, each in its language");
            assertEquals(5, controller.model.getRowCount());
            assertEquals(5, d.model.getRowCount());
            controller.release();
            dialog.controls().release();
            assertEquals(0, session.subscribers(), "both let go");
        });
    }

    @Test
    void everyValueShownIsTheServicesAnswerRounded() throws Exception {
        UranusTableSession session = new UranusTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            UranusTableControls c = new UranusTableControls(session, EN, false);
            c.apply();
            Configuration shown = session.result().entries().get(0).configuration();
            Configuration fresh = service.observeMoons(oslo(CARD));
            for (int row = 0; row < 5; row++) {
                assertSame(shown.moons().get(row), c.model.lines().get(row).moon());
                MoonPlace b = fresh.moons().get(row);
                for (int col = 0; col < UranusMoonsModel.COLUMNS.size(); col++) {
                    assertEquals(UranusMoonsModel.cell(b, col, c.words()), c.model.getValueAt(row, col),
                            "row " + row + " column " + col);
                }
            }
            var u = fresh.uranus();
            assertEquals(String.format(Locale.ROOT, "%.3f AU", u.distanceAu()), c.cardValue("distance"));
            assertEquals(String.format(Locale.ROOT, "%.1f″ / %.1f″", u.equatorialDiameterArcseconds(),
                    u.polarDiameterArcseconds()), c.cardValue("diameter"));
            assertEquals(String.format(Locale.ROOT, "%.2f°", u.phaseAngleDegrees()), c.cardValue("phaseangle"));
            assertEquals(String.format(Locale.ROOT, "%.1f°", u.poleAngleDegrees()), c.cardValue("pole"));
            List<String> card = new ArrayList<>();
            for (String line : UranusTableControls.CARD_LINES) {
                card.add(c.cardValue(line));
            }
            assertEquals(UranusMoonsModel.uranusValues(u, c.words()), card);
        });
    }

    /** U-9 and U-5: Saturn's card without the rings, Mars's pole and tilt, no phase words. */
    @Test
    void theCardIsTheRuledOne() {
        assertEquals(List.of("ra", "dec", "altitude", "azimuth", "distance", "diameter",
                "illuminated", "phaseangle", "pole", "tilt"), UranusTableControls.CARD_LINES);
        assertEquals(List.of("moon", "side", "eastwest", "northsouth", "separation", "radii", "state"),
                UranusMoonsModel.COLUMNS, "Jupiter's columns");
    }

    @Test
    void theCardsInstantReadsNaturallyInBothLanguages() throws Exception {
        UranusTableSession session = new UranusTableSession(() -> oslo(CARD), () -> service);
        onEdt(() -> {
            UranusTableControls en = new UranusTableControls(session, EN, false);
            UranusTableControls nb = new UranusTableControls(session, NB, false);
            en.apply();
            assertEquals(List.of("Miranda", "Ariel", "Umbriel", "Titania", "Oberon"), column(en, 0));
            assertEquals(column(en, 0), column(nb, 0), "the moon names identical in both (U-9)");
            assertEquals(List.of("clear of Uranus", "clear of Uranus", "clear of Uranus",
                    "clear of Uranus", "clear of Uranus"), column(en, 6),
                    "as Horizons codes the study's today (*) for all five");
            assertEquals("klar av Uranus", nb.model.getValueAt(4, 6));
            assertEquals("Uranus · 2026-10-27 12:00:00 UTC", en.cardTitle.getText());
            String tilt = service.observe(oslo(CARD)).northPoleTowardObserver()
                    ? "(north pole towards you)" : "(south pole towards you)";
            assertTrue(en.cardValue("tilt").endsWith(tilt), en.cardValue("tilt"));
            assertTrue(nb.cardValue("tilt").endsWith(tilt.contains("north")
                    ? "(nordpolen mot deg)" : "(sørpolen mot deg)"), nb.cardValue("tilt"));
            assertTrue(nb.cardValue("phaseangle").contains(","), "the Norwegian decimal comma");
            assertEquals("Avstand fra Uranus", nb.model.getColumnName(4));
            assertEquals("Uranusradier", nb.model.getColumnName(5));
            assertEquals("Uranus radii", en.model.getColumnName(5));
            assertEquals("Miranda, Ariel, Umbriel, Titania and Oberon", en.moonsHeading.getText());
            assertEquals("Miranda, Ariel, Umbriel, Titania og Oberon", nb.moonsHeading.getText());
            assertEquals("Phase angle: " + en.cardValue("phaseangle"),
                    en.cardValues.get(UranusTableControls.CARD_LINES.indexOf("phaseangle"))
                            .getAccessibleContext().getAccessibleName(),
                    "a screen reader hears the heading with the value");
        });
    }

    /** Horizons' codes, as the state the table says (Jupiter's precedence). */
    private static final Map<String, VisibilityState> STATE_OF_CODE = Map.of(
            "t", VisibilityState.IN_FRONT_OF_URANUS,
            "O", VisibilityState.BEHIND_URANUS,
            "U", VisibilityState.BEHIND_URANUS_IN_ITS_SHADOW,
            "u", VisibilityState.IN_URANUS_SHADOW);

    /**
     * Rows Horizons codes from Oslo inside the exact interval
     * (matrix-7d-umbriel-oslo.txt, matrix-7d-miranda-oslo.txt,
     * matrix-7d-ariel-oslo.txt) - behind
     * Uranus, behind it in its shadow, in front of it, and in its shadow -
     * are said in those words, in both languages.
     */
    @Test
    void namedConfigurationsReadAsHorizonsCodesThem() throws Exception {
        Observer[] at = {null};
        UranusTableSession session = new UranusTableSession(() -> at[0], () -> service);
        record Coded(String instant, Moon moon, String code) {
        }
        List<Coded> rows = List.of(new Coded("2009-12-05T00:00:00Z", Moon.UMBRIEL, "O"),
                new Coded("2005-03-19T00:00:00Z", Moon.MIRANDA, "U"),
                new Coded("2005-05-28T00:00:00Z", Moon.MIRANDA, "t"),
                new Coded("2006-02-04T00:00:00Z", Moon.ARIEL, "u"));
        onEdt(() -> {
            UranusTableControls en = new UranusTableControls(session, EN, false);
            UranusTableControls nb = new UranusTableControls(session, NB, false);
            for (Coded row : rows) {
                at[0] = oslo(Instant.parse(row.instant()));
                en.apply();
                MoonPlace p = session.result().entries().get(0).configuration().moon(row.moon());
                assertEquals(STATE_OF_CODE.get(row.code()), p.state(), row.instant());
                int r = row.moon().ordinal();
                assertEquals(UranusMoonsModel.stateWords(p.state(), en.words()), en.model.getValueAt(r, 6));
                assertEquals(UranusMoonsModel.stateWords(p.state(), nb.words()), nb.model.getValueAt(r, 6));
            }
            at[0] = oslo(Instant.parse("2005-03-19T00:00:00Z"));
            en.apply();
            assertEquals("behind Uranus, in its shadow", en.model.getValueAt(0, 6));
            assertEquals("bak Uranus, i skyggen", nb.model.getValueAt(0, 6));
        });
    }

    /**
     * The table says the flattened figure's state (U-7), not Horizons'
     * sphere: on 2004-05-29 Horizons codes Miranda O, behind Uranus's
     * equatorial sphere, while 1.0035 radii out it clears the flattened
     * limb - one of the rows the accuracy report counts as "figure differs".
     */
    @Test
    void theStateIsTheFlattenedFiguresWhereHorizonsSphereDiffers() throws Exception {
        UranusTableSession session = new UranusTableSession(
                () -> oslo(Instant.parse("2004-05-29T00:00:00Z")), () -> service);
        onEdt(() -> {
            UranusTableControls en = new UranusTableControls(session, EN, false);
            en.apply();
            MoonPlace miranda = session.result().entries().get(0).configuration().moon(Moon.MIRANDA);
            assertEquals(UranusService.DiscRelation.BEHIND, miranda.sphericalDiscRelation(),
                    "Horizons' sphere: behind (O)");
            assertEquals(VisibilityState.CLEAR_OF_URANUS, miranda.state());
            assertEquals("clear of Uranus", en.model.getValueAt(0, 6));
        });
    }

    @Test
    void beforeTheMoonsYearsUranusIsShownAloneAndTheRefusalNamesThem() throws Exception {
        Observer[] at = {oslo(Instant.parse("2000-01-01T00:00:00Z"))};
        UranusTableSession session = new UranusTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            UranusTableControls en = new UranusTableControls(session, EN, false);
            UranusTableControls nb = new UranusTableControls(session, NB, false);
            en.apply();
            assertEquals(UranusTableSession.Outcome.ROWS, session.result().outcome(),
                    "the moons' first day answers");
            assertEquals(5, en.model.getRowCount());
            at[0] = oslo(Instant.parse("1999-12-31T23:59:59Z"));
            en.apply();
            assertEquals(UranusTableSession.Outcome.MOONS_OUTSIDE, session.result().outcome());
            assertTrue(en.cardTitle.getText().startsWith("Uranus · 1999-12-31"), "Uranus still answers");
            assertEquals(0, en.model.getRowCount(), "and no moon rows");
            assertTrue(en.status().contains("2000-01-01") && en.status().contains("2100-12-31")
                    && en.status().contains("only Uranus"), en.status());
            assertTrue(nb.status().contains("2100-12-31") && nb.status().contains("bare Uranus"),
                    nb.status());
            at[0] = oslo(Instant.parse("2100-12-31T23:59:00Z"));
            en.apply();
            assertEquals(UranusTableSession.Outcome.ROWS, session.result().outcome(),
                    "the moons' last day answers to its end");
        });
    }

    /**
     * Uranus's own summary is never left out of a range (#635, required):
     * across the moons' start every group is one Uranus result - its
     * heading and three lines - with its moons where they answer and one
     * line saying so where they do not, and the status counts them.
     */
    @Test
    void aRangeAcrossTheMoonsStartKeepsUranusAndSaysWhereTheMoonsAreNotComputed() throws Exception {
        UranusTableSession session = new UranusTableSession(
                () -> oslo(Instant.parse("1999-12-10T00:00:00Z")), () -> service);
        onEdt(() -> {
            UranusTableControls en = new UranusTableControls(session, EN, false);
            UranusTableControls nb = new UranusTableControls(session, NB, false);
            en.rangeView.setSelected(true);
            en.span.setSelectedIndex(2);
            en.step.setSelectedIndex(3); // 30 days, weekly, across 2000-01-01
            en.apply();
            UranusTableSession.Result r = session.result();
            assertEquals(UranusTableSession.Outcome.ROWS_MOONS_IN_PART, r.outcome());
            assertEquals(5, r.entries().size());
            int without = 0;
            for (UranusTableSession.Entry e : r.entries()) {
                without += e.configuration() == null ? 1 : 0;
                assertEquals(!service.moonsAnswerAt(e.sample().instant()), e.configuration() == null,
                        "the moons are left out exactly where the service refuses them: "
                                + e.sample().instant());
            }
            assertTrue(without > 0 && without < r.entries().size(), "the range crosses the start: " + without);
            assertEquals(Integer.toString(without), r.detail());
            int with = r.entries().size() - without;
            for (UranusTableControls c : List.of(en, nb)) {
                List<UranusMoonsModel.Line> lines = c.model.lines();
                assertEquals(5, c.model.groups());
                assertEquals(with * 9 + without * 5, lines.size(),
                        "with moons: heading, three lines, five moons; without: heading, three lines, one refusal");
                int i = 0;
                for (UranusTableSession.Entry e : r.entries()) {
                    assertEquals(UranusMoonsModel.Kind.HEADING, lines.get(i).kind());
                    assertEquals(UranusMoonsModel.Kind.URANUS_POSITION, lines.get(i + 1).kind());
                    assertEquals(UranusMoonsModel.Kind.URANUS_FIGURE, lines.get(i + 2).kind());
                    assertEquals(UranusMoonsModel.Kind.URANUS_LIGHT, lines.get(i + 3).kind());
                    assertSame(e.uranus(), lines.get(i + 1).uranus(), "one Uranus result a group");
                    if (e.configuration() == null) {
                        assertEquals(UranusMoonsModel.Kind.MOONS_REFUSED, lines.get(i + 4).kind());
                        i += 5;
                    } else {
                        for (int m = 0; m < 5; m++) {
                            assertSame(e.configuration().moons().get(m), lines.get(i + 4 + m).moon());
                        }
                        i += 9;
                    }
                }
            }
            assertTrue(en.status().startsWith("5 instants, " + (5 * with) + " moon rows. At " + without
                    + " of these instants the moons are not shown"), en.status());
            assertTrue(nb.status().contains("Ved " + without + " av disse tidspunktene"), nb.status());
            assertTrue(((String) en.model.getValueAt(3, 0)).startsWith("Uranus: illuminated "));
            assertTrue(((String) nb.model.getValueAt(3, 0)).startsWith("Uranus: belyst "));
        });
    }

    @Test
    void theRefusalsNameWhatTheyRefuse() throws Exception {
        Observer[] at = {null};
        UranusTableSession session = new UranusTableSession(() -> at[0], () -> service);
        onEdt(() -> {
            UranusTableControls c = new UranusTableControls(session, EN, false);
            c.apply();
            assertEquals(UranusTableSession.Outcome.OBSERVER_ABSENT, session.result().outcome());
            assertEquals(0, session.computations());

            at[0] = oslo(Instant.parse("2101-01-01T00:00:00Z"));
            c.apply();
            assertEquals(UranusTableSession.Outcome.REFUSED_INTERVAL, session.result().outcome());
            assertTrue(c.status().startsWith("Uranus is computed for civil dates from 1900-01-01"
                    + " to 2100-12-31"), c.status());

            at[0] = oslo(CARD);
            c.rangeView.setSelected(true);
            c.span.setSelectedIndex(2); // 30 days of hours: 721 instants, over the cap
            c.step.setSelectedIndex(0);
            c.apply();
            assertEquals(UranusTableSession.Outcome.REFUSED_ROWS, session.result().outcome());
            assertTrue(c.status().contains("at most 200 are shown, five moon rows each"), c.status());
        });
    }

    @Test
    void aDateLineObserverIsAnsweredForItsOwnPlace() throws Exception {
        Observer chatham = UranusComparison.observer("chatham", CARD);
        UranusTableSession session = new UranusTableSession(() -> chatham, () -> service);
        onEdt(() -> {
            UranusTableControls c = new UranusTableControls(session, EN, false);
            c.apply();
            assertEquals(UranusMoonsModel.uranusValues(service.observe(chatham), c.words()).get(2),
                    c.cardValue("altitude"), "Chatham's own altitude");
            assertTrue(c.observerNote.getText().contains("-176.5") || c.observerNote.getText().contains("−176.5"),
                    "west of the date line, as Place and Time folds it: " + c.observerNote.getText());
        });
    }

    @Test
    void anEstimatedInstantIsMarkedAndItsStatesAreNotOfferedAsPredictions() throws Exception {
        UranusTableSession session = new UranusTableSession(
                () -> oslo(Instant.parse("2040-06-01T00:00:00Z")), () -> service);
        onEdt(() -> {
            UranusTableControls c = new UranusTableControls(session, EN, false);
            c.apply();
            assertTrue(c.cardTitle.getText().endsWith(" est."), c.cardTitle.getText());
            assertTrue(c.status().contains("not predicted times of events"), c.status());
        });
    }

    @Test
    void theIntervalsTheTableNamesAreThePacks() {
        assertEquals(UranusService.URANUS_FIRST_DAY, service.firstDay());
        assertEquals(UranusService.URANUS_LAST_DAY, service.lastDay());
        assertEquals(UranusService.MOONS_FIRST_DAY, service.moonsFirstDay());
        assertEquals(UranusService.MOONS_LAST_DAY, service.moonsLastDay());
        assertEquals(200, UranusTableSession.MAX_INSTANTS);
        assertEquals(5, UranusTableSession.MOONS);
    }

    private static List<String> column(UranusTableControls c, int column) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < c.model.getRowCount(); row++) {
            out.add((String) c.model.getValueAt(row, column));
        }
        return out;
    }
}
