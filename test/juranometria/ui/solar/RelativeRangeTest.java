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
 * The relative range and the row action (Sprint 54, issue #612, ruling
 * P2): <em>From chart time</em> reads its instants from the chart time
 * whenever it is computed - the first row is the chart time, forward or
 * back - and stores none; the absolute range is seeded from the chart
 * time once and is the reader's from then on; and a selected row may
 * set the chart time through the holder Place and Time installs
 * through, which stales this table as any change does. Headless, of the
 * content-test kind; installations happen on the event thread.
 */
class RelativeRangeTest {

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
    void fromChartTimeTheFirstRowIsTheChartTimeAndFollowsItOnCompute() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.relativeView.setSelected(true);
        controls.span.setSelectedIndex(0);      // 1 day
        controls.step.setSelectedIndex(1);      // 6 hours
        controls.apply();
        assertEquals(5, controls.model.getRowCount(), "a day in whole 6-hour steps, both ends");
        assertEquals(AUGUST, rowInstant(controls, 0), "the first row is the chart time");
        assertEquals(AUGUST.plus(Duration.ofDays(1)), rowInstant(controls, 4));
        assertEquals("5 rows.", controls.status());
        assertEquals(SolarTableSession.Mode.RELATIVE, controls.session().result().query().mode());
        assertEquals("", controls.session().result().query().start(),
                "a relative query stores no instant");

        Instant later = Instant.parse("2026-09-01T18:00:00Z");
        edt(() -> chart.install(OSLO.at(later)));
        assertTrue(controls.stale(), "the chart moved on: stale until computed");
        assertEquals(AUGUST, rowInstant(controls, 0), "and nothing recomputed");

        controls.apply();
        assertFalse(controls.stale());
        assertEquals(later, rowInstant(controls, 0),
                "computed again, the range is re-seeded from the chart time now");
        assertEquals(2, controls.session().computations());
    }

    @Test
    void backRunsFromTheChartTimeBackwards() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.relativeView.setSelected(true);
        controls.span.setSelectedIndex(0);
        controls.step.setSelectedIndex(1);
        controls.direction.setSelectedIndex(1); // back
        controls.apply();
        assertEquals(5, controls.model.getRowCount());
        assertEquals(AUGUST, rowInstant(controls, 0), "the first row is still the chart time");
        assertEquals(AUGUST.minus(Duration.ofHours(6)), rowInstant(controls, 1));
        assertEquals(AUGUST.minus(Duration.ofDays(1)), rowInstant(controls, 4));
        assertTrue(controls.session().result().query().back());
        for (SolarTableModel.Row row : controls.model.rows()) {
            assertFalse(row.sample().appendedEnd(), "no appended end: the chart time is an edge");
        }
    }

    @Test
    void aSpanReachesWholeStepsOnly() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.relativeView.setSelected(true);
        controls.span.setSelectedIndex(1);      // 7 days
        controls.step.setSelectedIndex(4);      // 30 days
        controls.apply();
        assertEquals(1, controls.model.getRowCount(),
                "no whole 30-day step fits in 7 days: the chart time alone");
        assertEquals(AUGUST, rowInstant(controls, 0));
        controls.span.setSelectedIndex(4);      // 365 days
        controls.apply();
        assertEquals(13, controls.model.getRowCount(), "twelve 30-day steps fit in a year");
        assertEquals(AUGUST.plus(Duration.ofDays(360)), rowInstant(controls, 12));
    }

    @Test
    void theAbsoluteRangeIsSeededFromTheChartTimeOnceAndIsThenTheReaders() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        assertEquals("2026-08-15 06:00:00", controls.start.getText(),
                "seeded from the chart time, never from the clock");
        assertEquals("2026-08-22 06:00:00", controls.end.getText());
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-09-01T18:00:00Z"))));
        assertEquals("2026-08-15 06:00:00", controls.start.getText(),
                "the chart moved on; the typed range did not");
        controls.rangeView.setSelected(true);
        controls.apply();
        assertEquals(AUGUST, rowInstant(controls, 0), "labelled analysis of the typed range");
        assertFalse(controls.stale(), "computed for the chart's context now, whatever its instants");
    }

    @Test
    void aSelectedRowSetsTheChartTimeThroughTheHolderAndStalesTheTable() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        assertNotNull(controls.setChartTime, "over the chart's holder the row action is offered");
        controls.inController();
        controls.relativeView.setSelected(true);
        controls.span.setSelectedIndex(0);
        controls.step.setSelectedIndex(1);
        controls.apply();
        assertTrue(controls.setChartTime.isEnabled() == (controls.selectedInstant() != null));
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
        assertEquals(5, controls.model.getRowCount(), "the rows stand, dimmed");
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
        assertFalse(plain.canSetChartTime());
        SolarTableDialog.Content dialog = SolarTableDialog.content(controls.session(), EN);
        assertNotNull(dialog.setChartTime, "the dialog offers it over the same session");
        assertNotNull(dialog.relativeView);
    }

    @Test
    void jupitersRelativeRangeIsTheSame() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        JovianTableSession session = new JovianTableSession(chart::observer,
                () -> new JovianSystemService(juranometria.solar.SolarSystemPack.load(),
                        juranometria.solar.JovianPack.load()));
        session.followContext(chart);
        JovianTableControls controls = new JovianTableControls(session, EN, false);
        controls.inController();
        controls.relativeView.setSelected(true);
        controls.span.setSelectedIndex(0);
        controls.step.setSelectedIndex(1);
        controls.direction.setSelectedIndex(1);
        controls.apply();
        List<JovianTableSession.Entry> entries = session.result().entries();
        assertEquals(5, entries.size());
        assertEquals(AUGUST, entries.get(0).sample().instant(), "the first instant is the chart time");
        assertEquals(AUGUST.minus(Duration.ofDays(1)), entries.get(4).sample().instant());
        edt(() -> controls.table.setRowSelectionInterval(0, 0));
        assertEquals(AUGUST, controls.selectedInstant(), "a heading line answers its instant");
        edt(() -> controls.table.setRowSelectionInterval(3, 3));
        assertEquals(AUGUST, controls.selectedInstant(), "and so does a moon line under it");
    }
}
