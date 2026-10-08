package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.TimeRange;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Query;

/**
 * Jupiter's applied query and its result, shared by every host that
 * shows the Jupiter table (Sprint 44, issue #474, under the contract
 * ruled on #472): the Controller's Jupiter group and the Jupiter dialog
 * kept for dogfooding, as the Sun's and the Moon's are shared since
 * #458.
 *
 * <p>The query is the Solar System tables' own - the view, and for a
 * range its start, end and step as a host drafted them - and so are
 * the steps and the readings of a typed instant. What differs is the
 * answer: for each instant, Jupiter and the four Galilean moons in the
 * ruled order, from {@link JovianSystemService} and nowhere else.
 * Applying reads Place and Time's observer afresh, computes once, and
 * tells every subscriber; nothing computes merely because a host was
 * built, opened or expanded.
 *
 * <p>Refusals are data a host phrases in its language: an instant it
 * cannot read, a range that runs backwards, more than
 * {@link #MAX_INSTANTS} instants (four moon rows each, so the
 * 1 000-row cap the Sun's tables keep), and Jupiter's years. Outside
 * the moons' years (2000–2100) a single instant still shows Jupiter and
 * says the configuration is refused, naming its years; a range that
 * leaves them is refused whole, because a range is read as
 * configurations.
 */
public final class JovianTableSession {

    /** At most this many instants in a range: four moon rows each, 1 000 rows. */
    public static final int MAX_INSTANTS = TimeRange.MAX_ROWS / 4;

    /** What a query answered. */
    public enum Outcome {
        ROWS, APPENDED, MOONS_OUTSIDE, OBSERVER_ABSENT, REFUSED_INSTANT,
        REFUSED_BACKWARDS, REFUSED_ROWS, REFUSED_INTERVAL, REFUSED_MOONS_INTERVAL
    }

    /**
     * One answered instant: the sample, Jupiter, and the configuration
     * of the four moons - null where the moons are outside their years.
     */
    public record Entry(TimeRange.Sample sample, JupiterObservation jupiter,
                        Configuration configuration) {
    }

    /** What the last application answered. */
    public record Result(Query query, Outcome outcome, List<Entry> entries,
                         Observer observer, String detail, long instantsAsked) {

        static Result none() {
            return new Result(null, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0);
        }

        /** Whether anything has been applied yet. */
        public boolean applied() {
            return query != null;
        }
    }

    private final Supplier<Observer> observer;
    private final Supplier<JovianSystemService> service;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private Result result = Result.none();
    private int computations;

    /**
     * Over a service read when first needed: the 53 MB Jovian pack is
     * loaded on the first table, never at startup.
     */
    public JovianTableSession(Supplier<Observer> observer,
                              Supplier<JovianSystemService> service) {
        if (observer == null || service == null) {
            throw new IllegalArgumentException(
                    "a session reads an observer and a service");
        }
        this.observer = observer;
        this.service = service;
    }

    /** The observer Place and Time holds now, or null while it holds none. */
    public Observer observerNow() {
        return observer.get();
    }

    /** The first civil day Jupiter answers for. */
    public LocalDate jupiterFirstDay() {
        return JovianSystemService.JUPITER_FIRST_DAY;
    }

    /** The last civil day Jupiter answers for. */
    public LocalDate jupiterLastDay() {
        return JovianSystemService.JUPITER_LAST_DAY;
    }

    /** The first civil day the moons answer for. */
    public LocalDate moonsFirstDay() {
        return JovianSystemService.MOONS_FIRST_DAY;
    }

    /** The last civil day the moons answer for. */
    public LocalDate moonsLastDay() {
        return JovianSystemService.MOONS_LAST_DAY;
    }

    /** The service's time scales, for the note a host writes under its table. */
    public juranometria.solar.time.TimeScales timeScales() {
        return service.get().timeScales();
    }

    /** The last result, which is the empty one before any query. */
    public Result result() {
        return result;
    }

    /** How many times the service was asked, for a test that holds "nothing calculates". */
    public int computations() {
        return computations;
    }

    /** How many subscribers follow, for a test that holds release. */
    public int subscribers() {
        return listeners.size();
    }

    /** Follows every result from now on; told the current one at once. */
    public SolarTableSession.Subscription onChange(Consumer<Result> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        listeners.add(listener);
        listener.accept(result);
        return () -> listeners.remove(listener);
    }

    /** Applies a host's draft: reads the observer, computes, tells everyone. */
    public void apply(Query query) {
        if (query == null) {
            throw new IllegalArgumentException("a query is required");
        }
        Observer now = observer.get();
        Result answered = now == null
                ? new Result(query, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0)
                : query.mode() == Mode.INSTANT ? instant(query, now) : range(query, now);
        result = answered;
        for (Consumer<Result> listener : List.copyOf(listeners)) {
            listener.accept(answered);
        }
    }

    /** Re-reads the observer and recomputes the applied query, else the instant. */
    public void update() {
        apply(result.applied() ? result.query() : Query.instant());
    }

    private boolean inside(Instant instant, LocalDate first, LocalDate last) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        return !day.isBefore(first) && !day.isAfter(last);
    }

    private Result instant(Query query, Observer now) {
        if (!inside(now.instant(), jupiterFirstDay(), jupiterLastDay())) {
            return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                    SolarTableSession.shown(now.instant()), 0);
        }
        computations++;
        TimeRange.Sample sample = new TimeRange.Sample(now.instant(), false);
        if (!inside(now.instant(), moonsFirstDay(), moonsLastDay())) {
            JupiterObservation jupiter = service.get().observeJupiter(now);
            return new Result(query, Outcome.MOONS_OUTSIDE,
                    List.of(new Entry(sample, jupiter, null)), now,
                    SolarTableSession.shown(now.instant()), 1);
        }
        Configuration c = service.get().observeMoons(now);
        return new Result(query, Outcome.ROWS, List.of(new Entry(sample, c.jupiter(), c)),
                now, "", 1);
    }

    private Result range(Query query, Observer now) {
        Instant from = SolarTableSession.parse(query.start());
        Instant to = SolarTableSession.parse(query.end());
        if (from == null || to == null) {
            return new Result(query, Outcome.REFUSED_INSTANT, List.of(), now,
                    from == null ? query.start() : query.end(), 0);
        }
        Duration by = SolarTableSession.STEPS.get(query.step());
        if (to.isBefore(from)) {
            return new Result(query, Outcome.REFUSED_BACKWARDS, List.of(), now, "", 0);
        }
        long instants = TimeRange.rowsOf(from, to, by);
        if (instants > MAX_INSTANTS) {
            return new Result(query, Outcome.REFUSED_ROWS, List.of(), now, "", instants);
        }
        for (Instant edge : List.of(from, to)) {
            if (!inside(edge, jupiterFirstDay(), jupiterLastDay())) {
                return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                        SolarTableSession.shown(edge), 0);
            }
        }
        for (Instant edge : List.of(from, to)) {
            if (!inside(edge, moonsFirstDay(), moonsLastDay())) {
                return new Result(query, Outcome.REFUSED_MOONS_INTERVAL, List.of(), now,
                        SolarTableSession.shown(edge), 0);
            }
        }
        computations++;
        List<Entry> entries = new ArrayList<>();
        JovianSystemService jovian = service.get();
        for (TimeRange.Sample sample : new TimeRange(from, to, by).samples()) {
            Configuration c = jovian.observeMoons(now.at(sample.instant()));
            entries.add(new Entry(sample, c.jupiter(), c));
        }
        boolean appended = entries.get(entries.size() - 1).sample().appendedEnd();
        return new Result(query, appended ? Outcome.APPENDED : Outcome.ROWS,
                List.copyOf(entries), now, "", entries.size());
    }
}
