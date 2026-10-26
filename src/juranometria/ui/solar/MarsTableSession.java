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
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.Configuration;
import juranometria.solar.MarsService.MarsObservation;
import juranometria.solar.TimeRange;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Query;

/**
 * Mars's applied query and its result, shared by every host that shows
 * the Mars table (issue #603, under the contract ruled on #601): the
 * Controller's Mars group and the Mars dialog, as Saturn's are.
 *
 * <p>The query is the Solar System tables' own - the view, and for a
 * range its span and step - and so are the steps. What differs is the
 * answer: for each instant, Mars and its two ruled moons in the ruled
 * order, from {@link MarsService} and nowhere else. Applying reads the
 * chart's context afresh, computes once and tells every subscriber;
 * nothing computes merely because a host was built, opened, expanded or
 * restored.
 *
 * <p>Refusals are data a host phrases in its language: more than
 * {@link #MAX_INSTANTS} instants (two moon rows each, within the 1 000-row
 * cap the Sun's tables keep), and Mars's years. Phobos and Deimos answer
 * from 2000 up to, not including, 2050-01-01 00:00 TDB (#601, M-7a with
 * the amendment), as the service decides for each instant. Where they do
 * not answer, Mars still does: a single instant shows Mars and says the
 * moons are refused, naming their years; a range shows Mars at every
 * instant and the moons wherever they answer, the instants without them
 * counted and said, so Mars's own summary is never left out.
 */
public final class MarsTableSession {

    /** At most this many instants in a range: two moon rows each, within 1 000 rows. */
    public static final int MAX_INSTANTS = TimeRange.MAX_ROWS / 2;

    /** How many moon rows an instant answers. */
    public static final int MOONS = MarsService.Moon.values().length;

    /** What a query answered. */
    public enum Outcome {
        /** Every instant answered, the moons with it. */
        ROWS,
        /** A range answered: Mars at every instant, the moons not at some (counted in {@code detail}). */
        ROWS_MOONS_IN_PART,
        /** One instant answered: Mars, the moons refused. */
        MOONS_OUTSIDE,
        OBSERVER_ABSENT, REFUSED_ROWS, REFUSED_INTERVAL
    }

    /**
     * One answered instant: the sample, Mars, and the configuration of
     * its two moons - null where the moons do not answer.
     */
    public record Entry(TimeRange.Sample sample, MarsObservation mars,
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
    private final Supplier<MarsService> service;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private Result result = Result.none();
    private int computations;

    /**
     * Over a service read when first needed: the 44 MB Mars System pack
     * is loaded on the first table, never at startup.
     */
    public MarsTableSession(Supplier<Observer> observer,
                              Supplier<MarsService> service) {
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

    /** The first civil day Mars answers for. */
    public LocalDate marsFirstDay() {
        return MarsService.MARS_FIRST_DAY;
    }

    /** The last civil day Mars answers for. */
    public LocalDate marsLastDay() {
        return MarsService.MARS_LAST_DAY;
    }

    /** The first civil day the moons answer for. */
    public LocalDate moonsFirstDay() {
        return MarsService.MOONS_FIRST_DAY;
    }

    /** The last year the reader is told the moons answer for: 2049, the end exclusive. */
    public int moonsLastYear() {
        return MarsService.MOONS_LAST_YEAR;
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
        if (!inside(now.instant(), marsFirstDay(), marsLastDay())) {
            return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                    SolarTableSession.shown(now.instant()), 0);
        }
        computations++;
        MarsService mars = service.get();
        TimeRange.Sample sample = new TimeRange.Sample(now.instant(), false);
        if (!mars.moonsAnswerAt(now.instant())) {
            return new Result(query, Outcome.MOONS_OUTSIDE,
                    List.of(new Entry(sample, mars.observe(now), null)), now,
                    SolarTableSession.shown(now.instant()), 1);
        }
        Configuration c = mars.observeMoons(now);
        return new Result(query, Outcome.ROWS, List.of(new Entry(sample, c.mars(), c)),
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
            if (!inside(edge, marsFirstDay(), marsLastDay())) {
                return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                        SolarTableSession.shown(edge), 0);
            }
        }
        computations++;
        List<Entry> entries = new ArrayList<>();
        MarsService mars = service.get();
        int alone = 0;
        for (TimeRange.Sample sample : new TimeRange(from, to, by).samples()) {
            Observer at = now.at(sample.instant());
            if (mars.moonsAnswerAt(sample.instant())) {
                Configuration c = mars.observeMoons(at);
                entries.add(new Entry(sample, c.mars(), c));
            } else {
                entries.add(new Entry(sample, mars.observe(at), null));
                alone++;
            }
        }
        return new Result(query, alone == 0 ? Outcome.ROWS : Outcome.ROWS_MOONS_IN_PART,
                List.copyOf(entries), now, alone == 0 ? "" : Integer.toString(alone),
                entries.size());
    }

}
