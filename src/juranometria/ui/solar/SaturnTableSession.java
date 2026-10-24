package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.context.ChartContext;
import juranometria.context.ChartContextHolder;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.TimeRange;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Query;

/**
 * Saturn's applied query and its result, shared by every host that
 * shows the Saturn table (Sprint 52, issue #576, under the contract
 * ruled on #574): the Controller's Saturn group and the Saturn dialog
 * kept for dogfooding, as Jupiter's are since #474.
 *
 * <p>The query is the Solar System tables' own - the view, and for a
 * range its start, end and step as a host drafted them - and so are
 * the steps and the readings of a typed instant. What differs is the
 * answer: for each instant, Saturn, its rings and its seven ruled moons
 * in the ruled order, from {@link SaturnSystemService} and nowhere
 * else. Applying reads Place and Time's observer afresh, computes once,
 * and tells every subscriber; nothing computes merely because a host
 * was built, opened, expanded or restored.
 *
 * <p>Refusals are data a host phrases in its language: an instant it
 * cannot read, a range that runs backwards, more than
 * {@link #MAX_INSTANTS} instants (seven moon rows each, within the
 * 1 000-row cap the Sun's tables keep), and Saturn's years. Outside the
 * moons' years (2000–2100) a single instant still shows Saturn and says
 * the configuration is refused, naming its years; a range that leaves
 * them is refused whole, because a range is read as configurations.
 */
public final class SaturnTableSession {

    /** At most this many instants in a range: seven moon rows each, within 1 000 rows. */
    public static final int MAX_INSTANTS = TimeRange.MAX_ROWS / 7;

    /** How many moon rows an instant answers. */
    public static final int MOONS = SaturnSystemService.Moon.values().length;

    /** What a query answered. */
    public enum Outcome {
        ROWS, MOONS_OUTSIDE, OBSERVER_ABSENT, REFUSED_ROWS, REFUSED_INTERVAL, REFUSED_MOONS_INTERVAL
    }

    /**
     * One answered instant: the sample, Saturn, and the configuration
     * of the seven moons - null where the moons are outside their years.
     */
    public record Entry(TimeRange.Sample sample, SaturnObservation saturn,
                        Configuration configuration) {
    }

    /** What the last application answered. */
    public record Result(Query query, Outcome outcome, List<Entry> entries,
                         Observer observer, String detail, long instantsAsked,
                         long revision) {

        /** A result before its stamp: revision 0, which a plain supplier never leaves. */
        public Result(Query query, Outcome outcome, List<Entry> entries,
                      Observer observer, String detail, long instantsAsked) {
            this(query, outcome, entries, observer, detail, instantsAsked, 0);
        }

        /** This result stamped with the revision of the context it was computed at (#612). */
        Result at(long revision) {
            return new Result(query, outcome, entries, observer, detail, instantsAsked,
                    revision);
        }

        static Result none() {
            return new Result(null, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0);
        }

        /** Whether anything has been applied yet. */
        public boolean applied() {
            return query != null;
        }
    }

    /** The chart's context, read once per application: the observer and the revision it is at. */
    private Supplier<ChartContext> context;
    /** The holder a row may set the chart time through (#612, P2); null over a plain supplier. */
    private ChartContextHolder installer;
    private final Supplier<SaturnSystemService> service;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private Result result = Result.none();
    private int computations;

    /**
     * Over a service read when first needed: the 79 MB Saturn pack is
     * loaded on the first table, never at startup.
     */
    public SaturnTableSession(Supplier<Observer> observer,
                              Supplier<SaturnSystemService> service) {
        if (observer == null || service == null) {
            throw new IllegalArgumentException(
                    "a session reads an observer and a service");
        }
        // A plain supplier of the observer (tests and photographers)
        // stands at revision 0 for ever: nothing it answers is ever stale.
        this.context = () -> {
            Observer now = observer.get();
            return now == null ? null : new ChartContext(now, 0);
        };
        this.service = service;
    }

    /** The observer the chart holds now, or null while it holds none. */
    public Observer observerNow() {
        ChartContext at = context.get();
        return at == null ? null : at.observer();
    }

    /** How a host is told Place and Time changed; nothing until the atlas says. */
    private java.util.function.Function<Runnable, Runnable> observerChanges =
            listener -> () -> { };

    /**
     * Where Place and Time's changes are announced (#259, ruling 5 on
     * #556): a subscription that returns its cancel. A host follows it
     * to say again whether its table agrees with the chart - never to
     * recompute, which only the reader asks for.
     */
    public void followObserver(java.util.function.Function<Runnable, Runnable> subscribe) {
        if (subscribe == null) {
            throw new IllegalArgumentException("a subscription to follow");
        }
        this.observerChanges = subscribe;
    }

    /** Tells a host whenever Place and Time changes; returns its cancel. */
    public Runnable onObserverChange(Runnable listener) {
        return observerChanges.apply(listener);
    }

    /**
     * Follows the chart's one context (#612): the observer and the
     * revision it stands at are read together from the holder, and every
     * host is told on the holder's own subscription - to say the table is
     * stale (ruling P1), never to recompute.
     */
    public void followContext(ChartContextHolder holder) {
        if (holder == null) {
            throw new IllegalArgumentException("a holder to follow");
        }
        this.context = holder::current;
        this.installer = holder;
        this.observerChanges = listener -> holder.onChange(change -> listener.run())::cancel;
    }

    /** The revision the chart's context stands at now, or -1 while it holds none. */
    public long revision() {
        ChartContext at = context.get();
        return at == null ? -1 : at.revision();
    }

    /**
     * Whether the result was computed at an earlier revision of the
     * chart's context than the chart holds now (ruling P1 on #612): the
     * stamp decides, not a comparison of observers.
     */
    public boolean stale() {
        return result.applied() && result.observer() != null
                && result.revision() != revision();
    }

    /** Whether a host may set the chart time from a row: only over the chart's holder (#612, P2). */
    public boolean canSetChartTime() {
        return installer != null;
    }

    /**
     * Sets the chart time to this instant at the chart's place - the
     * row action <em>Set chart time to this instant</em> (#612, ruling
     * P2) - through the holder Place and Time installs through, so
     * every enabled body is redrawn for it. On the event thread, as
     * every installation is; this table is then stale until computed.
     */
    public void setChartTime(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("an instant to set the chart time to");
        }
        if (installer == null) {
            throw new IllegalStateException("this session follows no chart holder");
        }
        installer.install(installer.observer().at(instant));
    }

    /** The first civil day Saturn answers for. */
    public LocalDate saturnFirstDay() {
        return SaturnSystemService.SATURN_FIRST_DAY;
    }

    /** The last civil day Saturn answers for. */
    public LocalDate saturnLastDay() {
        return SaturnSystemService.SATURN_LAST_DAY;
    }

    /** The first civil day the moons answer for. */
    public LocalDate moonsFirstDay() {
        return SaturnSystemService.MOONS_FIRST_DAY;
    }

    /** The last civil day the moons answer for. */
    public LocalDate moonsLastDay() {
        return SaturnSystemService.MOONS_LAST_DAY;
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
        ChartContext at = context.get();
        Observer now = at == null ? null : at.observer();
        Result answered = now == null
                ? new Result(query, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0)
                : (query.mode() == Mode.INSTANT ? instant(query, now) : range(query, now))
                        .at(at.revision());
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
        if (!inside(now.instant(), saturnFirstDay(), saturnLastDay())) {
            return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                    SolarTableSession.shown(now.instant()), 0);
        }
        computations++;
        TimeRange.Sample sample = new TimeRange.Sample(now.instant(), false);
        if (!inside(now.instant(), moonsFirstDay(), moonsLastDay())) {
            SaturnObservation saturn = service.get().observeSaturn(now);
            return new Result(query, Outcome.MOONS_OUTSIDE,
                    List.of(new Entry(sample, saturn, null)), now,
                    SolarTableSession.shown(now.instant()), 1);
        }
        Configuration c = service.get().observeMoons(now);
        return new Result(query, Outcome.ROWS, List.of(new Entry(sample, c.saturn(), c)),
                now, "", 1);
    }

    private Result range(Query query, Observer now) {
        Duration by = SolarTableSession.STEPS.get(query.step());
        Instant[] edges = SolarTableSession.edges(query, now.instant());
        Instant from = edges[0];
        Instant to = edges[1];
        long instants = TimeRange.rowsOf(from, to, by);
        if (instants > MAX_INSTANTS) {
            return new Result(query, Outcome.REFUSED_ROWS, List.of(), now, "", instants);
        }
        for (Instant edge : List.of(from, to)) {
            if (!inside(edge, saturnFirstDay(), saturnLastDay())) {
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
        SaturnSystemService saturn = service.get();
        for (TimeRange.Sample sample : new TimeRange(from, to, by).samples()) {
            Configuration c = saturn.observeMoons(now.at(sample.instant()));
            entries.add(new Entry(sample, c.saturn(), c));
        }
        return new Result(query, Outcome.ROWS, List.copyOf(entries), now, "", entries.size());
    }

}
