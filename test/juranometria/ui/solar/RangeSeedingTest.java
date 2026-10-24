package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.context.ChartContextHolder;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The range as labelled analysis, and the row action (Sprint 54, issue
 * #612, ruling P2 as the owner amended it on the first candidate): two
 * views only - the chart's one instant, or a time range as typed. The
 * range is seeded from the chart time once, at build - From with the
 * chart time, To with the default span after it - and is the reader's
 * from then on; <em>Start at chart time</em> puts that seed back from
 * the chart time now, touching the fields and computing nothing; a
 * selected row may set the chart time through the holder Place and
 * Time installs through, which stales this table as any change does.
 * Headless, of the content-test kind; installations happen on the
 * event thread.
 */
class RangeSeedingTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final Instant AUGUST = Instant.parse("2026-08-15T06:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, AUGUST);
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

    private static Instant rowInstant(SolarTableControls controls, int row) {
        return controls.model.rows().get(row).sample().instant();
    }

    @Test
    void twoViewsOnlyAndTheRangeIsSeededFromTheChartTimeOnce() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        assertEquals("At chart time", controls.instantView.getText());
        assertEquals("Over a time range", controls.rangeView.getText());
        assertEquals("2026-08-15 06:00:00", controls.start.getText(),
                "From seeded from the chart time, never the clock");
        assertEquals("2026-08-22 06:00:00", controls.end.getText(),
                "To seeded with the default span after it");
        assertEquals(0, controls.session().computations(), "seeding computes nothing");

        edt(() -> chart.install(OSLO.at(Instant.parse("2026-09-01T18:00:00Z"))));
        assertEquals("2026-08-15 06:00:00", controls.start.getText(),
                "the chart moved on; the typed range is the reader's and did not");
        controls.rangeView.setSelected(true);
        controls.apply();
        assertEquals(AUGUST, rowInstant(controls, 0), "labelled analysis of the typed range");
        assertFalse(controls.stale(), "computed for the chart's context now, whatever its instants");
    }

    @Test
    void startAtChartTimePutsTheSeedBackFromTheChartTimeNowAndComputesNothing() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        assertEquals("Start at chart time", controls.startAtChartTime.getText());
        controls.start.setText("2027-01-01 00:00:00");
        controls.end.setText("2027-01-03 00:00:00");
        Instant later = Instant.parse("2026-09-01T18:00:00Z");
        edt(() -> chart.install(OSLO.at(later)));
        int computed = controls.session().computations();

        edt(controls.startAtChartTime::doClick);

        assertEquals("2026-09-01 18:00:00", controls.start.getText(), "From is the chart time now");
        assertEquals("2026-09-08 18:00:00", controls.end.getText(), "To the default span after it");
        assertEquals(computed, controls.session().computations(), "the fields only: nothing computed");
        assertTrue(controls.instantView.isSelected(), "and the view is not changed either");

        controls.rangeView.setSelected(true);
        controls.apply();
        assertEquals(later, rowInstant(controls, 0), "computed when asked, from the chart time");
        assertEquals(8, controls.model.getRowCount());
    }

    @Test
    void aSelectedRowSetsTheChartTimeThroughTheHolderAndStalesTheTable() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        assertNotNull(controls.setChartTime, "over the chart's holder the row action is offered");
        controls.inController();
        controls.rangeView.setSelected(true);
        controls.step.setSelectedIndex(1);      // 6 hours
        controls.end.setText("2026-08-16 06:00:00");
        controls.apply();
        assertEquals(5, controls.model.getRowCount());
        edt(() -> controls.table.setRowSelectionInterval(2, 2));
        Instant chosen = rowInstant(controls, 2);
        assertEquals(AUGUST.plus(Duration.ofHours(12)), chosen);
        assertEquals(chosen, controls.selectedInstant());
        assertTrue(controls.setChartTime.isEnabled());
        int computed = controls.session().computations();

        edt(controls.setChartTime::doClick);

        assertEquals(chosen, chart.observer().instant(), "the chart time is the row's instant");
        assertEquals(59.91, chart.observer().latitudeDegrees(), "at the chart's place");
        assertEquals(1, chart.current().revision(), "one installation, through the holder");
        assertTrue(controls.stale(), "this table was computed at the earlier context");
        assertEquals(computed, controls.session().computations(), "and nothing recomputed");
        assertEquals("2026-08-15 06:00:00", controls.start.getText(),
                "the typed range is untouched by the row action");

        // Start at chart time then moves the range along to the row's instant.
        edt(controls.startAtChartTime::doClick);
        assertEquals("2026-08-15 18:00:00", controls.start.getText());
    }

    @Test
    void anInstantViewOffersItsOneRowAndAPlainSessionOffersNothing() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.apply();
        assertEquals(AUGUST, controls.selectedInstant(), "the instant view's one row, unselected");
        assertTrue(controls.setChartTime.isEnabled());

        SolarTableSession plain = new SolarTableSession(() -> OSLO, service, SolarTable.venus());
        SolarTableControls over = new SolarTableControls(plain, EN, null, false);
        assertNull(over.setChartTime, "a plain supplier cannot install: no row action");
        assertNotNull(over.startAtChartTime, "but Start at chart time needs no holder");
        assertFalse(plain.canSetChartTime());
        SolarTableDialog.Content dialog = SolarTableDialog.content(controls.session(), EN);
        assertNotNull(dialog.setChartTime, "the dialog offers it over the same session");
        assertNotNull(dialog.startAtChartTime);
    }

    @Test
    void jupitersRangeSeedsADayAndStartsAtChartTimeTheSameWay() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        JovianTableSession session = new JovianTableSession(chart::observer,
                () -> new JovianSystemService(juranometria.solar.SolarSystemPack.load(),
                        juranometria.solar.JovianPack.load()));
        session.followContext(chart);
        JovianTableControls controls = new JovianTableControls(session, EN, false);
        controls.inController();
        assertEquals("2026-08-15 06:00:00", controls.start.getText());
        assertEquals("2026-08-16 06:00:00", controls.end.getText(), "Jupiter's default span is a day");
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-20T00:00:00Z"))));
        edt(controls.startAtChartTime::doClick);
        assertEquals("2026-08-20 00:00:00", controls.start.getText());
        assertEquals("2026-08-21 00:00:00", controls.end.getText());
        assertEquals(0, session.computations());
        controls.rangeView.setSelected(true);
        controls.step.setSelectedIndex(1);
        controls.apply();
        List<JovianTableSession.Entry> entries = session.result().entries();
        assertEquals(5, entries.size());
        assertEquals(Instant.parse("2026-08-20T00:00:00Z"), entries.get(0).sample().instant());
        edt(() -> controls.table.setRowSelectionInterval(3, 3));
        assertEquals(Instant.parse("2026-08-20T00:00:00Z"), controls.selectedInstant(),
                "a moon line answers its group's instant");
    }
}
