package juranometria.ui.solar;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.TimeRange;

/**
 * One body's applied query and its result, shared by every host that
 * shows the body's table (Sprint 42, issue #458, ruled on #457).
 *
 * <p>A host keeps its own draft - which view, and the range's start,
 * end and step as typed - and <strong>applies</strong> it here.
 * Applying reads Place and Time's observer, computes, and tells every
 * subscriber; the dialog and the Controller then show the same rows
 * and the same outcome, whichever host asked. {@link #update()}
 * re-reads the observer and recomputes the applied query: the
 * dialog's <em>Update from Place and Time</em>, and its coming to the
 * front (ruled on #400, kept on #457). Nothing here calculates unless
 * asked, and nothing is persisted (ruled on #400).
 *
 * <p>The refusals are the dialog's, moved here so both hosts refuse
 * the same thing: a range that cannot be read, runs backwards, asks
 * for more rows than the service answers, or leaves the years it
 * covers. They are data; a host phrases them in its language.
 */
public final class SolarTableSession {

    /** Which view a query asks for. */
    public enum Mode {
        INSTANT, RANGE
    }

    /** The steps offered, in order, by key suffix and duration. */
    public static final List<String> STEP_KEYS = List.of("step.hour",
            "step.sixHours", "step.day", "step.week", "step.month");
    public static final List<Duration> STEPS = List.of(Duration.ofHours(1),
            Duration.ofHours(6), Duration.ofDays(1), Duration.ofDays(7),
            Duration.ofDays(30));

    /** A query as a host drafted it: the view, and for a range the start, end and step. */
    public record Query(Mode mode, String start, String end, int step) {

        public Query {
            if (mode == null) {
                throw new IllegalArgumentException("a query asks for a view");
            }
            start = start == null ? "" : start.strip();
            end = end == null ? "" : end.strip();
            if (step < 0 || step >= STEPS.size()) {
                throw new IllegalArgumentException("no such step: " + step);
            }
        }

        /** The single-instant query, which needs no range. */
        public static Query instant() {
            return new Query(Mode.INSTANT, "", "", 2);
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
                         Observer observer, String detail, long rowsAsked) {

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

    private final Supplier<Observer> observer;
    private final SolarSystemService service;
    private final SolarTable table;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private Result result = Result.none();
    private int computations;

    public SolarTableSession(Supplier<Observer> observer, SolarSystemService service,
                             SolarTable table) {
        if (observer == null || service == null || table == null) {
            throw new IllegalArgumentException(
                    "a session reads an observer and a service, for a body");
        }
        this.observer = observer;
        this.service = service;
        this.table = table;
    }

    /** The body this session is over. */
    public SolarTable table() {
        return table;
    }

    /** The observer Place and Time holds now, or null while it holds none. */
    public Observer observerNow() {
        return observer.get();
    }

    /** The service's time scales, for the note a host writes under its table. */
    public juranometria.solar.time.TimeScales timeScales() {
        return service.timeScales();
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
        Observer now = observer.get();
        Result answered = now == null
                ? new Result(query, Outcome.OBSERVER_ABSENT, List.of(), null, "", 0)
                : query.mode() == Mode.INSTANT ? instant(query, now) : range(query, now);
        result = answered;
        for (Consumer<Result> listener : List.copyOf(listeners)) {
            listener.accept(answered);
        }
    }

    /**
     * Re-reads the observer and recomputes the applied query - the
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
        SolarSystemService.Observation o = service.observe(table.body(), now);
        return new Result(query, Outcome.ROWS, List.of(new SolarTableModel.Row(
                new TimeRange.Sample(now.instant(), false), o)), now, "", 1);
    }

    private Result range(Query query, Observer now) {
        Instant from = parse(query.start());
        Instant to = parse(query.end());
        if (from == null || to == null) {
            return new Result(query, Outcome.REFUSED_INSTANT, List.of(), now,
                    from == null ? query.start() : query.end(), 0);
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
                service.observe(table.body(), now, new TimeRange(from, to, by));
        List<SolarTableModel.Row> shown = answered.stream()
                .map(r -> new SolarTableModel.Row(r.sample(), r.observation())).toList();
        boolean appended = answered.get(answered.size() - 1).sample().appendedEnd();
        return new Result(query, appended ? Outcome.APPENDED : Outcome.ROWS, shown, now,
                "", shown.size());
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
