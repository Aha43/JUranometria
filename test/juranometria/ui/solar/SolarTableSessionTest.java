package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.TimeRange;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Outcome;
import juranometria.ui.solar.SolarTableSession.Query;
import juranometria.ui.solar.SolarTableSession.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One body's applied query and result (Sprint 42, issue #458, ruled on
 * #457): nothing is computed until a query is applied; applying reads
 * the observer and tells every subscriber; the refusals are the
 * dialog's, as data; Update recomputes the applied query over a fresh
 * observer; Sun and Moon sessions are independent.
 */
class SolarTableSessionTest {

    /** The observer the dialog tests state: Oslo at midsummer 2026, as SunTableDialogTest's OSLO. */
    private static Observer oslo() {
        return new Observer(59.91, 10.75, java.time.Instant.parse("2026-06-21T10:00:00Z"));
    }

    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    @Test
    void nothingIsComputedUntilAQueryIsApplied() {
        Observer[] observer = {oslo()};
        SolarTableSession session = new SolarTableSession(() -> observer[0], service,
                SolarTable.sun());
        assertEquals(0, session.computations(), "building asks the service nothing");
        assertFalse(session.result().applied());
        assertTrue(session.result().rows().isEmpty());
        List<Result> heard = new ArrayList<>();
        session.onChange(heard::add);
        assertEquals(1, heard.size(), "told the current result at once");
        assertEquals(0, session.computations(), "and subscribing computes nothing");

        session.apply(Query.instant());
        assertEquals(1, session.computations());
        assertEquals(Outcome.ROWS, session.result().outcome());
        assertEquals(1, session.result().rows().size());
        assertEquals(observer[0], session.result().observer(), "the observer read");
        assertEquals(2, heard.size(), "every subscriber hears the result");
    }

    @Test
    void updateRecomputesTheAppliedQueryOverAFreshObserver() {
        Observer[] observer = {oslo()};
        SolarTableSession session = new SolarTableSession(() -> observer[0], service,
                SolarTable.sun());
        session.update();
        assertEquals(Mode.INSTANT, session.result().query().mode(),
                "nothing applied yet: Update is the single instant");
        Instant first = session.result().rows().get(0).sample().instant();
        observer[0] = new Observer(observer[0].latitudeDegrees(),
                observer[0].eastLongitudeDegrees(), observer[0].instant().plus(Duration.ofDays(1)));
        assertEquals(first, session.result().rows().get(0).sample().instant(),
                "a moved observer changes nothing until asked");
        session.update();
        assertEquals(first.plus(Duration.ofDays(1)),
                session.result().rows().get(0).sample().instant(),
                "asked, the applied query is recomputed over the fresh observer");
    }

    @Test
    void aRangeAnswersItsRowsAndEveryRefusalIsData() {
        Observer observer = oslo();
        SolarTableSession session = new SolarTableSession(() -> observer, service,
                SolarTable.moon());
        session.apply(Query.range(1, 2));
        assertEquals(Outcome.ROWS, session.result().outcome());
        assertEquals(8, session.result().rows().size(),
                "a week of days from the chart time, both ends");
        assertEquals(observer.instant(), session.result().rows().get(0).sample().instant(),
                "the first row is the chart time (#612, P2)");
        assertEquals(observer.instant().plus(Duration.ofDays(7)),
                session.result().rows().get(7).sample().instant());
        for (SolarTableModel.Row row : session.result().rows()) {
            assertFalse(row.sample().appendedEnd(), "whole steps: nothing is appended");
        }

        session.apply(Query.range(1, 4));
        assertEquals(1, session.result().rows().size(),
                "no whole 30-day step fits in a week: the chart time alone");

        session.apply(Query.range(4, 0));
        assertEquals(Outcome.REFUSED_ROWS, session.result().outcome());
        assertEquals(8761, session.result().rowsAsked(), "a year of hours");
        assertTrue(session.result().rowsAsked() > TimeRange.MAX_ROWS);

        SolarTableSession late = new SolarTableSession(
                () -> observer.at(Instant.parse("2100-12-28T00:00:00Z")), service,
                SolarTable.moon());
        late.apply(Query.range(1, 2));
        assertEquals(Outcome.REFUSED_INTERVAL, late.result().outcome());
        assertEquals("2101-01-04 00:00:00", late.result().detail(),
                "the edge that left the interval");
    }

    @Test
    void anAbsentObserverAnswersNothingAndTwoBodiesAreIndependent() {
        Observer[] observer = {null};
        SolarTableSession sun = new SolarTableSession(() -> observer[0], service,
                SolarTable.sun());
        sun.apply(Query.instant());
        assertEquals(Outcome.OBSERVER_ABSENT, sun.result().outcome());
        assertNull(sun.result().observer());
        assertEquals(0, sun.computations(), "nothing to compute for");

        observer[0] = oslo();
        SolarTableSession moon = new SolarTableSession(() -> observer[0], service,
                SolarTable.moon());
        moon.apply(Query.instant());
        assertEquals(1, moon.computations());
        assertEquals(0, sun.computations(), "the Moon's computation is not the Sun's");
        assertSame(SolarTable.moon(), moon.table());
    }

    @Test
    void releasingASubscriptionLetsGoAndTwiceIsOnce() {
        SolarTableSession session = new SolarTableSession(SolarTableSessionTest::oslo,
                service, SolarTable.sun());
        SolarTableSession.Subscription following = session.onChange(r -> { });
        assertEquals(1, session.subscribers());
        following.cancel();
        following.cancel();
        assertEquals(0, session.subscribers());
    }
}
