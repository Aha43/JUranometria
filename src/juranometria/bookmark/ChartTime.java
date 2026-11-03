package juranometria.bookmark;

import java.time.Instant;
import java.util.Optional;

/**
 * A bookmark's chart time: one UTC instant and why it is kept. The chart
 * has one time; a bookmark never owns a clock of its own, and opening one
 * with a time sets the chart to exactly this instant.
 */
public record ChartTime(Instant instant, Purpose purpose) {

    /** Why the instant is kept. */
    public enum Purpose {
        /** The instant belongs to the astronomical configuration. */
        EVENT("event"),
        /** The instant is the reader's confirmed time of an observation. */
        OBSERVATION("observation");

        private final String token;

        Purpose(String token) {
            this.token = token;
        }

        /** The document's word for the purpose. */
        public String token() {
            return token;
        }

        static Optional<Purpose> of(String token) {
            for (Purpose p : values()) {
                if (p.token.equals(token)) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }
    }

    public ChartTime {
        if (instant == null || purpose == null) {
            throw new IllegalArgumentException("a chart time is an instant and a purpose");
        }
    }
}
