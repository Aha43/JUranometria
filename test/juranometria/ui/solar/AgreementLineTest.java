package juranometria.ui.solar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The agreement line at the top of every Solar System panel (Sprint 51,
 * issue #259, ruling 5 on #556 as the owner amended it): agreement
 * covers both the observer and the instant; the line says <em>Same place
 * and instant as the chart</em> only when both agree, names both sides
 * when either differs, states a range without calling it wrong, and is
 * said again when Place and Time changes - and saying it never
 * recomputes the table. Headless, of the content-test kind.
 */
class AgreementLineTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    /** Place and Time as the panels see it: an observer, and its announcements. */
    private static final class PlaceAndTime {
        final AtomicReference<Observer> now = new AtomicReference<>(
                new Observer(59.91, 10.75, Instant.parse("2026-08-15T06:00:00Z")));
        final List<Runnable> listeners = new ArrayList<>();

        void set(Observer observer) {
            now.set(observer);
            for (Runnable listener : List.copyOf(listeners)) {
                listener.run();
            }
        }

        Runnable follow(Runnable listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }

    private static SolarTableControls venus(PlaceAndTime chart) {
        SolarTableSession session = new SolarTableSession(chart.now::get, service,
                SolarTable.venus());
        session.followObserver(chart::follow);
        return new SolarTableControls(session, EN, null, false);
    }

    @Test
    void theLineSaysSameOnlyWhenPlaceAndInstantBothAgree() {
        PlaceAndTime chart = new PlaceAndTime();
        SolarTableControls controls = venus(chart);
        controls.inController();
        assertEquals("Chart: 59.91° N · 10.75° E, 2026-08-15 06:00:00 UTC",
                controls.agreementText(), "nothing computed yet: the chart's moment alone");
        assertFalse(controls.update.isVisible(), "and nothing to update from");

        controls.apply();
        assertEquals("Same place and instant as the chart: 59.91° N · 10.75° E,"
                + " 2026-08-15 06:00:00 UTC", controls.agreementText());
        assertFalse(controls.update.isVisible(), "agreeing, the line offers no update");
    }

    @Test
    void anotherInstantNamesBothSidesAndRecomputesNothing() {
        PlaceAndTime chart = new PlaceAndTime();
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.apply();
        int computed = controls.session().computations();
        var result = controls.session().result();

        chart.set(chart.now.get().at(Instant.parse("2026-08-15T21:30:00Z")));

        assertEquals("Chart: 59.91° N · 10.75° E, 2026-08-15 21:30:00 UTC\n"
                + "This table: 59.91° N · 10.75° E, 2026-08-15 06:00:00 UTC",
                controls.agreementText(), "said again on Place and Time's change");
        assertTrue(controls.update.isVisible(),
                "with Update from Place and Time beside it");
        assertEquals(computed, controls.session().computations(),
                "saying it recomputed nothing");
        assertEquals(result, controls.session().result(), "and changed no result");

        // The button applies this host's draft, as Compute does; the
        // Controller journey presses it for real.
        controls.apply();
        assertEquals(computed + 1, controls.session().computations(),
                "the reader asking recomputes, once");
        assertTrue(controls.agreementText().startsWith("Same place and instant"));
    }

    @Test
    void anotherPlaceAtTheChartsInstantDoesNotReadAsAgreeing() {
        PlaceAndTime chart = new PlaceAndTime();
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.apply();
        chart.set(chart.now.get().from(-33.87, 151.21));
        assertEquals("Chart: 33.87° S · 151.21° E, 2026-08-15 06:00:00 UTC\n"
                + "This table: 59.91° N · 10.75° E, 2026-08-15 06:00:00 UTC",
                controls.agreementText(),
                "the amendment: the same instant at another place is not the chart's");
        assertTrue(controls.update.isVisible());

        chart.set(chart.now.get().from(59.91001, 10.75));
        assertEquals("Chart: 59.910010° N · 10.750000° E, 2026-08-15 06:00:00 UTC\n"
                + "This table: 59.910000° N · 10.750000° E, 2026-08-15 06:00:00 UTC",
                controls.agreementText(),
                "places that differ are never written alike: the decimals widen until"
                        + " they tell them apart");
    }

    @Test
    void aRangeStatesItselfAndTheChartWithoutCallingItWrong() {
        PlaceAndTime chart = new PlaceAndTime();
        SolarTableControls controls = venus(chart);
        controls.inController();
        controls.rangeView.setSelected(true);
        controls.start.setText("2026-08-01 00:00:00");
        controls.end.setText("2026-08-03 00:00:00");
        controls.apply();
        assertEquals("This range: 59.91° N · 10.75° E, from 2026-08-01 00:00:00 to"
                + " 2026-08-03 00:00:00 UTC\n"
                + "Chart: 59.91° N · 10.75° E, 2026-08-15 06:00:00 UTC",
                controls.agreementText());
        assertFalse(controls.update.isVisible(),
                "the same place: a range is not the chart's moment, and is not wrong");
        chart.set(chart.now.get().from(-33.87, 151.21));
        assertTrue(controls.update.isVisible(),
                "computed for another place, it offers the update");
    }

    @Test
    void jupitersPanelSaysTheSame() {
        PlaceAndTime chart = new PlaceAndTime();
        JovianTableSession session = new JovianTableSession(chart.now::get,
                () -> new JovianSystemService(juranometria.solar.SolarSystemPack.load(),
                        juranometria.solar.JovianPack.load()));
        session.followObserver(chart::follow);
        JovianTableControls controls = new JovianTableControls(session, EN, false);
        controls.inController();
        controls.apply();
        assertEquals("Same place and instant as the chart: 59.91° N · 10.75° E,"
                + " 2026-08-15 06:00:00 UTC", controls.agreementText());
        chart.set(chart.now.get().at(Instant.parse("2026-08-16T06:00:00Z")));
        assertTrue(controls.agreementText().startsWith("Chart: "),
                controls.agreementText());
        assertTrue(controls.update.isVisible());
    }

    @Test
    void releasingThePanelStopsFollowingPlaceAndTime() {
        PlaceAndTime chart = new PlaceAndTime();
        SolarTableControls controls = venus(chart);
        assertEquals(1, chart.listeners.size());
        controls.release();
        assertEquals(0, chart.listeners.size(), "released, the panel listens to nothing");
    }
}
