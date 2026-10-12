package juranometria.chart;

/**
 * What the reader is currently asking about (Sprint 19, issue #169,
 * docs/decisions/point-and-identify.md).
 *
 * <p>A selection carries <strong>identity and position only</strong>.
 * Names, magnitudes, types and sizes are read from the catalogue when
 * they are shown; copying them here would make a second, staler copy
 * of the catalogue - the mistake this project has already had to
 * correct in the label pass and in the symbol geometry.
 *
 * <p>Nothing here knows about Swing, the inspector, or the chart
 * view. A selection is a question the reader has asked; it is not a
 * command, and answering it never moves the chart.
 */
public sealed interface Selection {

    /** Where on the sky this selection points. */
    SkyPosition position();

    /** Nothing is selected. */
    record None() implements Selection {
        @Override
        public SkyPosition position() {
            return null;
        }
    }

    /**
     * A catalogued object the reader pointed at, held by the identity
     * the catalogue itself uses - a TYC identifier for a star, an
     * OpenNGC id for a deep-sky object - so the details can always be
     * looked up again and can never go stale here.
     */
    record Object(Kind kind, String catalogueId, SkyPosition position)
            implements Selection {

        public enum Kind { STAR, DEEP_SKY }

        public Object {
            if (kind == null || catalogueId == null || catalogueId.isBlank()
                    || position == null) {
                throw new IllegalArgumentException(
                        "a selected object needs a kind, an identity, and"
                                + " a position");
            }
        }
    }

    /**
     * A point of sky with nothing catalogued within reach. This is an
     * answer, not a failure: on a quiet page at the reviewed
     * tolerance, 98.9% of clicks land here.
     */
    record EmptySky(SkyPosition position) implements Selection {
        public EmptySky {
            if (position == null) {
                throw new IllegalArgumentException("a position is required");
            }
        }
    }

    /**
     * A drawn Solar System body, as computed for an instant and an
     * observer (Sprint 48, issue #513): the identity the page draws it
     * under - {@code sun}, {@code moon}, {@code venus}, Jupiter's - with
     * the instant and the observer's place its drawn position belongs
     * to, because a moving body's place is nothing without them. Still
     * identity and position only: its facts are the service's, read
     * when shown. The chart core owns no clock; the instant is the one
     * Place and Time gave the module that drew it.
     */
    record Occurrence(String identity, java.time.Instant instant,
                      double latitudeDegrees, double eastLongitudeDegrees,
                      SkyPosition position) implements Selection {
        public Occurrence {
            if (identity == null || identity.isBlank() || instant == null
                    || position == null) {
                throw new IllegalArgumentException(
                        "an occurrence names a body, its instant and its"
                                + " position");
            }
            if (Double.isNaN(latitudeDegrees) || latitudeDegrees < -90.0
                    || latitudeDegrees > 90.0 || !Double.isFinite(eastLongitudeDegrees)) {
                throw new IllegalArgumentException(
                        "an occurrence's observer is a place on the Earth");
            }
        }

        /** Whether this is the same body at the same instant and place. */
        public boolean sameAs(Occurrence other) {
            return other != null && identity.equals(other.identity)
                    && instant.equals(other.instant)
                    && latitudeDegrees == other.latitudeDegrees
                    && eastLongitudeDegrees == other.eastLongitudeDegrees;
        }
    }

    /** The selection every session starts with. */
    Selection NOTHING = new None();
}
