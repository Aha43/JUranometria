package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.context.ChartContextHolder;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stale line at the top of every Solar System panel (Sprint 54,
 * issue #612, ruling P1): a table is for the chart's one time and place;
 * when the chart moves on - another instant, or another place at the
 * same instant - the rows stay, dimmed, under <em>Computed for the
 * previous chart time and place — Compute again</em>, and nothing
 * recomputes until the reader computes; a never-computed table says
 * nothing. The stamp decides, not a comparison of observers. Headless,
 * of the content-test kind; the chart's context is installed on the
 * event thread, as the holder requires.
 */
class StaleLineTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final String MESSAGE =
            "Computed for the previous chart time and place — Compute again";
    private static final Observer OSLO =
            new Observer(59.91, 10.75, Instant.parse("2026-08-15T06:00:00Z"));
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    private static void edt(Runnable work) throws Exception {
        SwingUtilities.invokeAndWait(work);
    }

    private static SolarTableControls venus(ChartContextHolder chart) {
        SolarTableSession session = new SolarTableSession(chart::observer, service,
                SolarTable.venus());
        session.followContext(chart);
        return new SolarTableControls(session, EN, null, false);
    }

    private static List<JLabel> labelsUnder(Component root) {
        List<JLabel> found = new ArrayList<>();
        if (root instanceof JLabel label) {
            found.add(label);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(labelsUnder(child));
            }
        }
        return found;
    }

    private static Component named(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component hit = named(child, name);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    @Test
    void theLineSaysNothingUntilTheChartMovesOnFromAComputedTable() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        javax.swing.JComponent group = controls.inController();
        assertEquals("", controls.staleText(), "nothing computed: nothing to say");
        assertFalse(controls.stale());

        controls.apply();
        assertEquals("", controls.staleText(), "computed for the chart's context now");
        assertEquals(StaleLine.tone(false), controls.table.getForeground());
        int computed = controls.session().computations();
        var result = controls.session().result();

        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-15T21:30:00Z"))));

        assertEquals(MESSAGE, controls.staleText(), "said on the chart's change");
        assertTrue(controls.stale());
        assertEquals(computed, controls.session().computations(),
                "saying it recomputed nothing");
        assertEquals(result, controls.session().result(), "and changed no result");
        assertEquals(StaleLine.tone(true), controls.table.getForeground(),
                "the rows are dimmed, and still readable");
        assertNotEquals(StaleLine.tone(false), StaleLine.tone(true));
        Component card = named(group, "venusCard");
        assertTrue(card != null, "the instant's card stands");
        List<JLabel> values = labelsUnder(card);
        assertTrue(values.stream().anyMatch(l -> l.getText().contains("2026-08-15 06:00")),
                "showing the rows as computed: " + values);
        assertTrue(values.stream().anyMatch(l -> StaleLine.tone(true).equals(l.getForeground())),
                "the card's values dimmed too");
        assertTrue(controls.observerNote.getText().contains("2026-08-15 06:00:00"),
                "the note states the context the rows are for: "
                        + controls.observerNote.getText());

        controls.apply();
        assertEquals(computed + 1, controls.session().computations(),
                "the reader computing recomputes, once");
        assertEquals("", controls.staleText(), "for the chart's context now");
        assertFalse(controls.stale());
        assertEquals(StaleLine.tone(false), controls.table.getForeground());
        assertTrue(controls.observerNote.getText().contains("2026-08-15 21:30:00"),
                controls.observerNote.getText());
    }

    @Test
    void anotherPlaceAtTheChartsInstantIsStaleToo() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.apply();
        edt(() -> chart.install(OSLO.from(-33.87, 151.21)));
        assertEquals(MESSAGE, controls.staleText(),
                "the same instant at another place is not the chart's context");
        assertTrue(controls.stale());
    }

    @Test
    void aRangeIsStaleWhenTheChartMovesOnAndCurrentOnceComputedAgain() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.rangeView.setSelected(true);
        controls.start.setText("2026-08-01 00:00:00");
        controls.end.setText("2026-08-03 00:00:00");
        controls.apply();
        assertEquals("", controls.staleText(), "a range computed for the chart's context");
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-15T21:30:00Z"))));
        assertEquals(MESSAGE, controls.staleText());
        assertEquals("2026-08-01 00:00:00", controls.start.getText(),
                "the typed range survives the chart's change");
        controls.apply();
        assertEquals("", controls.staleText());
        assertEquals(3, controls.model.getRowCount());
    }

    @Test
    void aNeverComputedTableSaysNothingWhenTheChartMoves() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-16T06:00:00Z"))));
        assertEquals("", controls.staleText());
        assertFalse(controls.stale());
        assertEquals(0, controls.session().computations());
        assertTrue(controls.observerNote.getText().contains("2026-08-16 06:00:00"),
                "the note follows the chart while nothing is computed: "
                        + controls.observerNote.getText());
    }

    @Test
    void jupitersPanelSaysTheSameAndDimsItsCard() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        JovianTableSession session = new JovianTableSession(chart::observer,
                () -> new JovianSystemService(juranometria.solar.SolarSystemPack.load(),
                        juranometria.solar.JovianPack.load()));
        session.followContext(chart);
        JovianTableControls controls = new JovianTableControls(session, EN, false);
        controls.inController();
        controls.apply();
        assertEquals("", controls.staleText());
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-16T06:00:00Z"))));
        assertEquals(MESSAGE, controls.staleText());
        assertTrue(controls.stale());
        assertEquals(StaleLine.tone(true), controls.table.getForeground());
        assertEquals(StaleLine.tone(true), controls.cardTitle.getForeground());
        assertEquals(1, session.computations(), "nothing recomputed");
        controls.apply();
        assertEquals("", controls.staleText());
        assertEquals(StaleLine.tone(false), controls.cardTitle.getForeground());
        assertEquals(2, session.computations());
    }

    @Test
    void theLineTakesNoRoomWhileItSaysNothing() throws Exception {
        // The owner's finding on the first #612 candidate: an empty band
        // where the line stands is not furniture. Hidden until it speaks,
        // framed when it does, and gone again once computed.
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        javax.swing.JComponent line = controls.staleLine();
        assertFalse(line.isVisible(), "nothing computed: no room taken");
        controls.apply();
        assertFalse(line.isVisible(), "current: no room taken");
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-15T21:30:00Z"))));
        assertTrue(line.isVisible(), "stale: the line appears, framed");
        assertTrue(line.getPreferredSize().height
                >= 2 * line.getFontMetrics(line.getFont()).getHeight(),
                "two lines high at least, so a wrapped message never clips");
        controls.apply();
        assertFalse(line.isVisible(), "computed again: gone");
    }

    @Test
    void releasingThePanelStopsFollowingTheChart() {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        assertEquals(1, chart.subscribers());
        controls.release();
        assertEquals(0, chart.subscribers(), "released, the panel listens to nothing");
    }
}
