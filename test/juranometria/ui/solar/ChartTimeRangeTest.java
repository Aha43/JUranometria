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
 * A range starts at the chart time, always (Sprint 54, issue #612,
 * ruling P2 as the owner amended it on the second candidate): the chart
 * time is the one time base of everything drawn or tabled, so a range
 * is a span and a step from it, stores no instant, and is read from
 * the chart time whenever it is computed - which is why two ranges of
 * two bodies can never disagree about where they start. To range from
 * another instant the reader sets the chart time: through Place and
 * Time, or through <em>Set chart time to this instant</em> on a
 * selected row, which installs through the holder Place and Time uses
 * and stales this table as any change does. Headless, of the
 * content-test kind; installations happen on the event thread.
 */
class ChartTimeRangeTest {

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
    void twoViewsAndARangeIsASpanAndAStepFromTheChartTime() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        assertEquals("At chart time", controls.instantView.getText());
        assertEquals("Over a time range", controls.rangeView.getText());
        assertEquals(List.of("1 day", "7 days", "30 days", "90 days", "365 days"),
                items(controls));
        assertEquals(1, controls.span.getSelectedIndex(), "a week by default");
        assertEquals(0, controls.session().computations(), "building computes nothing");

        controls.rangeView.setSelected(true);
        controls.apply();
        assertEquals(8, controls.model.getRowCount(), "a week of daily rows");
        assertEquals(AUGUST, rowInstant(controls, 0), "the first row is the chart time");
        assertEquals(AUGUST.plus(Duration.ofDays(7)), rowInstant(controls, 7));
        assertEquals("8 rows.", controls.status());
        assertEquals(SolarTableSession.Mode.RANGE, controls.session().result().query().mode());

        Instant later = Instant.parse("2026-09-01T18:00:00Z");
        edt(() -> chart.install(OSLO.at(later)));
        assertTrue(controls.stale(), "the chart moved on: stale until computed");
        assertEquals(AUGUST, rowInstant(controls, 0), "and nothing recomputed");
        controls.apply();
        assertFalse(controls.stale());
        assertEquals(later, rowInstant(controls, 0),
                "computed again, the range starts at the chart time now");
        assertEquals(2, controls.session().computations());
    }

    private static List<String> items(SolarTableControls controls) {
        List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < controls.span.getItemCount(); i++) {
            items.add(controls.span.getItemAt(i));
        }
        return items;
    }

    @Test
    void aSpanReachesWholeStepsOnlyAndTooManyRowsAreRefused() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.rangeView.setSelected(true);
        controls.span.setSelectedIndex(1);      // 7 days
        controls.step.setSelectedIndex(4);      // 30 days
        controls.apply();
        assertEquals(1, controls.model.getRowCount(),
                "no whole 30-day step fits in 7 days: the chart time alone");
        controls.span.setSelectedIndex(4);      // 365 days
        controls.apply();
        assertEquals(13, controls.model.getRowCount(), "twelve 30-day steps fit in a year");
        assertEquals(AUGUST.plus(Duration.ofDays(360)), rowInstant(controls, 12));
        for (SolarTableModel.Row row : controls.model.rows()) {
            assertFalse(row.sample().appendedEnd(), "nothing is appended");
        }
        controls.step.setSelectedIndex(0);      // 1 hour
        controls.apply();
        assertEquals(SolarTableSession.Outcome.REFUSED_ROWS, controls.session().result().outcome());
        assertEquals("That range asks for 8761 rows and at most 1000 are shown; shorten it"
                + " or lengthen the step.", controls.status());
    }

    @Test
    void aSelectedRowSetsTheChartTimeThroughTheHolderAndStalesTheTable() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        SolarTableControls controls = venus(chart);
        assertNotNull(controls.setChartTime, "over the chart's holder the row action is offered");
        controls.inController();
        controls.rangeView.setSelected(true);
        controls.span.setSelectedIndex(0);
        controls.step.setSelectedIndex(1);      // a day in 6-hour steps
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

        // Compute then moves the range along to the row's instant.
        controls.apply();
        assertEquals(chosen, rowInstant(controls, 0));
        assertFalse(controls.stale());
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
        assertNotNull(dialog.span);
    }

    @Test
    void jupitersRangeIsTheSameSpanAndStepFromTheChartTime() throws Exception {
        ChartContextHolder chart = new ChartContextHolder(OSLO);
        JovianTableSession session = new JovianTableSession(chart::observer,
                () -> new JovianSystemService(juranometria.solar.SolarSystemPack.load(),
                        juranometria.solar.JovianPack.load()));
        session.followContext(chart);
        JovianTableControls controls = new JovianTableControls(session, EN, false);
        controls.inController();
        assertEquals(1, controls.span.getSelectedIndex(), "a week by default");
        controls.rangeView.setSelected(true);
        controls.span.setSelectedIndex(0);
        controls.step.setSelectedIndex(1);
        controls.apply();
        List<JovianTableSession.Entry> entries = session.result().entries();
        assertEquals(5, entries.size());
        assertEquals(AUGUST, entries.get(0).sample().instant(), "the first instant is the chart time");
        assertEquals(AUGUST.plus(Duration.ofDays(1)), entries.get(4).sample().instant());
        edt(() -> controls.table.setRowSelectionInterval(3, 3));
        assertEquals(AUGUST, controls.selectedInstant(), "a moon line answers its group's instant");
        edt(() -> chart.install(OSLO.at(Instant.parse("2026-08-20T00:00:00Z"))));
        assertTrue(controls.stale());
        controls.apply();
        assertEquals(Instant.parse("2026-08-20T00:00:00Z"),
                session.result().entries().get(0).sample().instant(),
                "computed again, from the chart time now");
    }
}
