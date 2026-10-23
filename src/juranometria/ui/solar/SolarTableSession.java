package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.context.ChartContext;
import juranometria.context.ChartContextHolder;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.TimeRange;

/**
 * One body's applied query and its result, shared by every host that
 * shows the body's table (Sprint 42, issue #458, ruled on #457).
 *
 * <p>A host keeps its own draft - which view, and the range's start,
 * end and step as typed - and <strong>applies</strong> it here.
 * Applying reads the chart's context once, computes, and tells every
 * subscriber; the dialog and the Controller then show the same rows
 * and the same outcome, whichever host asked. A result is stamped
 * with the revision of the context it was computed at (#612); when
 * the chart moves on, the result stays as it was and {@link #stale()}
 * says so - nothing recomputes until a host applies again (ruling P1).
 * Nothing here calculates unless asked, and nothing is persisted
 * (ruled on #400).
 *
 * <p>The refusals are the dialog's, moved here so both hosts refuse
 * the same thing: a range that cannot be read, runs backwards, asks
 * for more rows than the service answers, or leaves the years it
 * covers. They are data; a host phrases them in its language.
 */
public final class SolarTableSession {

    /**
     * Which view a query asks for: the instant, a span from the chart
     * time (#612, ruling P2), or an absolute range as typed.
     */
    public enum Mode {
        INSTANT, RELATIVE, RANGE
    }

    /** The steps offered, in order, by key suffix and duration. */
    public static final List<String> STEP_KEYS = List.of("step.hour",
            "step.sixHours", "step.day", "step.week", "step.month");
    public static final List<Duration> STEPS = List.of(Duration.ofHours(1),
            Duration.ofHours(6), Duration.ofDays(1), Duration.ofDays(7),
            Duration.ofDays(30));

    /** The spans a relative range offers, in order, by key suffix and duration (#612, P2). */
    public static final List<String> SPAN_KEYS = List.of("span.day", "span.week",
            "span.month", "span.quarter", "span.year");
    public static final List<Duration> SPANS = List.of(Duration.ofDays(1),
            Duration.ofDays(7), Duration.ofDays(30), Duration.ofDays(90),
            Duration.ofDays(365));

    /**
     * A query as a host drafted it: the view; for an absolute range the
     * start, end and step as typed; for a relative range (#612, ruling
     * P2) the span, the direction and the step - its instants are read
     * from the chart time whenever it is applied, never stored.
     */
    public record Query(Mode mode, String start, String end, int step, int span,
                        boolean back) {

        public Query {
            if (mode == null) {
                throw new IllegalArgumentException("a query asks for a view");
            }
            start = start == null ? "" : start.strip();
            end = end == null ? "" : end.strip();
            if (step < 0 || step >= STEPS.size()) {
                throw new IllegalArgumentException("no such step: " + step);
            }
            if (span < 0 || span >= SPANS.size()) {
                throw new IllegalArgumentException("no such span: " + span);
            }
            if (mode == Mode.RELATIVE) {
                // A relative range stores no instant: its edges are read
                // from the chart time whenever it is applied.
                start = "";
                end = "";
            }
        }

        /** An instant or absolute-range query, as before #612: a week's span, forward. */
        public Query(Mode mode, String start, String end, int step) {
            this(mode, start, end, step, 1, false);
        }

        /** The single-instant query, which needs no range. */
        public static Query instant() {
            return new Query(Mode.INSTANT, "", "", 2);
        }

        /** A relative range: this span from the chart time, forward or back, at this step. */
        public static Query relative(int span, boolean back, int step) {
            return new Query(Mode.RELATIVE, "", "", step, span, back);
        }
    }

    /** What a query answered: rows, or why none. */
    public enum Outcome {
        ROWS, APPENDED, OBSERVER_ABSENT, REFUSED_INSTANT, REFUSED_BACKWARDS,
        REFUSED_ROWS, REFUSED_INTERVAL
    }

    /**
     * What the last application answered: the query, the outcome, the
     * rows, the observer read, and what a refusal is about - the text
     * that could not be read, or the instant that left the interval -
     * and how many rows a refused range asked for.
     */
    public record Result(Query query, Outcome outcome, List<SolarTableModel.Row> rows,
                         Observer observer, String detail, long rowsAsked, long revision) {

        /** A result before its stamp: revision 0, which a plain supplier never leaves. */
        public Result(Query query, Outcome outcome, List<SolarTableModel.Row> rows,
                      Observer observer, String detail, long rowsAsked) {
            this(query, outcome, rows, observer, detail, rowsAsked, 0);
        }

        /** This result stamped with the revision of the context it was computed at (#612). */
        Result at(long revision) {
            return new Result(query, outcome, rows, observer, detail, rowsAsked, revision);
        }

        /** Before any query: nothing applied, nothing to show. */
        static Result none() {
            return new Result(null, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0);
        }

        /** Whether anything has been applied yet. */
        public boolean applied() {
            return query != null;
        }
    }

    /** Lets a subscriber go; cancelling twice is once. */
    public interface Subscription {
        void cancel();
    }

    private static final DateTimeFormatter SHOWN = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TYPED_SHORT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT).withZone(ZoneOffset.UTC);

    /** The chart's context, read once per application: the observer and the revision it is at. */
    private Supplier<ChartContext> context;
    /** The holder a row may set the chart time through (#612, P2); null over a plain supplier. */
    private ChartContextHolder installer;
    private final Supplier<SolarSystemService> service;
    private final SolarTable table;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private Result result = Result.none();
    private int computations;

    public SolarTableSession(Supplier<Observer> observer, SolarSystemService service,
                             SolarTable table) {
        this(observer, () -> service, table);
        if (service == null) {
            throw new IllegalArgumentException("a session reads a service");
        }
    }

    /**
     * Over a service read when first needed - the application's 8.9 MB
     * pack is loaded on the first table, never at startup (#400).
     */
    public SolarTableSession(Supplier<Observer> observer,
                             Supplier<SolarSystemService> service, SolarTable table) {
        if (observer == null || service == null || table == null) {
            throw new IllegalArgumentException(
                    "a session reads an observer and a service, for a body");
        }
        // A plain supplier of the observer (tests and photographers)
        // stands at revision 0 for ever: nothing it answers is ever stale.
        this.context = () -> {
            Observer now = observer.get();
            return now == null ? null : new ChartContext(now, 0);
        };
        this.service = service;
        this.table = table;
    }

    /** The body this session is over. */
    public SolarTable table() {
        return table;
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
    public Subscription onChange(Consumer<Result> listener) {
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

    /**
     * Recomputes the applied query at the chart's context now - the
     * single instant when nothing has been applied yet, which is what
     * a dialog opens on.
     */
    public void update() {
        apply(result.applied() ? result.query() : Query.instant());
    }

    private Result instant(Query query, Observer now) {
        LocalDate day = now.instant().atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(SolarSystemService.FIRST_DAY)
                || day.isAfter(SolarSystemService.LAST_DAY)) {
            return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                    shown(now.instant()), 0);
        }
        computations++;
        SolarSystemService.Observation o = service.get().observe(table.body(), now);
        return new Result(query, Outcome.ROWS, List.of(new SolarTableModel.Row(
                new TimeRange.Sample(now.instant(), false), o)), now, "", 1);
    }

    private Result range(Query query, Observer now) {
        Instant from;
        Instant to;
        if (query.mode() == Mode.RELATIVE) {
            // From the chart time read now (#612, P2), never a stored instant.
            Instant[] edges = relativeEdges(query, now.instant());
            from = edges[0];
            to = edges[1];
        } else {
            from = parse(query.start());
            to = parse(query.end());
            if (from == null || to == null) {
                return new Result(query, Outcome.REFUSED_INSTANT, List.of(), now,
                        from == null ? query.start() : query.end(), 0);
            }
        }
        Duration by = STEPS.get(query.step());
        if (to.isBefore(from)) {
            return new Result(query, Outcome.REFUSED_BACKWARDS, List.of(), now, "", 0);
        }
        long rows = TimeRange.rowsOf(from, to, by);
        if (rows > TimeRange.MAX_ROWS) {
            return new Result(query, Outcome.REFUSED_ROWS, List.of(), now, "", rows);
        }
        for (Instant edge : List.of(from, to)) {
            LocalDate day = edge.atOffset(ZoneOffset.UTC).toLocalDate();
            if (day.isBefore(SolarSystemService.FIRST_DAY)
                    || day.isAfter(SolarSystemService.LAST_DAY)) {
                return new Result(query, Outcome.REFUSED_INTERVAL, List.of(), now,
                        shown(edge), 0);
            }
        }
        computations++;
        List<SolarSystemService.Row> answered =
                service.get().observe(table.body(), now, new TimeRange(from, to, by));
        List<SolarTableModel.Row> shown = answered.stream()
                .map(r -> new SolarTableModel.Row(r.sample(), r.observation())).toList();
        if (query.mode() == Mode.RELATIVE && query.back()) {
            shown = backwards(shown);
        }
        boolean appended = answered.get(answered.size() - 1).sample().appendedEnd();
        return new Result(query, appended ? Outcome.APPENDED : Outcome.ROWS, shown, now,
                "", shown.size());
    }

    /**
     * A relative range's edges from the chart time (#612, ruling P2): as
     * many whole steps as the span holds, the chart time one edge - so
     * the chart time is always a row, and the first row once the rows
     * of a backward range are read backwards.
     */
    public static Instant[] relativeEdges(Query query, Instant chartTime) {
        Duration by = STEPS.get(query.step());
        long steps = SPANS.get(query.span()).toNanos() / by.toNanos();
        Duration reach = by.multipliedBy(steps);
        return query.back()
                ? new Instant[] {chartTime.minus(reach), chartTime}
                : new Instant[] {chartTime, chartTime.plus(reach)};
    }

    /** Rows read backwards: a backward range's first row is the chart time. */
    public static <T> List<T> backwards(List<T> rows) {
        List<T> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    /** A typed instant, in either form the dialog accepts, or null. */
    public static Instant parse(String text) {
        String typed = text == null ? "" : text.strip();
        for (DateTimeFormatter format : List.of(SHOWN, TYPED_SHORT)) {
            try {
                return Instant.from(format.parse(typed));
            } catch (DateTimeParseException e) {
                // the other format may fit
            }
        }
        return null;
    }

    /** An instant as the fields and the table show it. */
    public static String shown(Instant instant) {
        return SHOWN.format(instant);
    }
}
