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

    /** The selection every session starts with. */
    Selection NOTHING = new None();
}
