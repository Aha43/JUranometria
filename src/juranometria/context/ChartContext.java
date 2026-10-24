package juranometria.context;

import java.time.Instant;

import juranometria.sky.Observer;

/**
 * The chart's one time and place (Sprint 54, issue #612, the rulings on
 * {@code docs/decisions/chart-context.md}): an immutable snapshot of the
 * observer - latitude, east longitude and the frozen UTC instant - with
 * the revision it was installed as. Every unqualified time-dependent
 * mark, table, spoken description, report and export reads one of these
 * and nothing else; a consumer that keeps a result keeps the revision it
 * was computed at, so staleness is a comparison of revisions, never of
 * observers.
 *
 * <p>Nothing here reads a clock. The application boundary obtains "now"
 * once and installs it through {@link ChartContextHolder}.
 */
public record ChartContext(Observer observer, long revision) {

    public ChartContext {
        if (observer == null) {
            throw new IllegalArgumentException("a chart context is somewhere, at some instant");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("a revision counts installations from zero");
        }
    }

    /** The chart time: the frozen UTC instant. */
    public Instant instant() {
        return observer.instant();
    }

    /** Whether another context has the same observer and instant, whatever its revision. */
    public boolean sameAs(ChartContext other) {
        return other != null && observer.equals(other.observer);
    }
}
