package juranometria.ui.placeandtime;

import java.time.Instant;
import java.util.prefs.Preferences;

import juranometria.sky.Observer;

/**
 * The reader's place, remembered - and only the place (Sprint 25,
 * issue #228).
 *
 * <p>Decided by the gate: latitude and east-positive longitude
 * change rarely and are tedious to type, so they belong with the
 * reader's other stored choices. The <strong>instant is never
 * stored</strong> - it is a frozen snapshot of a moment the reader
 * chose, and a remembered one would come back stale and
 * wrong-looking with no way to tell why. There is no key for it
 * here, so a stale saved clock cannot masquerade as <em>Now</em>.
 *
 * <p>The visibility switches are not stored either: the gate
 * approves persisting the place and nothing else, so every session
 * begins with the ordinary chart and the reference lines are a
 * choice a reader makes each time.
 *
 * <p>Same discipline as {@code ChartOptionsStore}: an interface over
 * an explicit node, so tests use dedicated nodes and never the
 * developer's real preferences.
 */
public interface PlaceStore {

    /**
     * The remembered place as an observer at the given instant -
     * which the caller supplies, because this store has no clock and
     * no stored moment to offer.
     *
     * <p>With nothing remembered: the equator at Greenwich. Neutral,
     * honest about being nobody's home, and drawn for at most one
     * session before the reader's first entry replaces it.
     */
    Observer load(Instant instant);

    /** Remembers this place. The observer's instant is not read. */
    void save(double latitudeDegrees, double eastLongitudeDegrees);

    /** Whether a reader has ever entered a place. */
    boolean remembered();

    /**
     * Pushes what has been saved to the backing store, so a fresh
     * session can read it.
     */
    void flush();

    /** The store the running application uses. */
    static PlaceStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** An implementation over an explicit node; tests use a test node. */
    static PlaceStore forNode(Preferences node) {
        return new PlaceStore() {

            @Override
            public Observer load(Instant instant) {
                return new Observer(
                        node.getDouble("place.latitude", 0.0),
                        node.getDouble("place.eastLongitude", 0.0),
                        instant);
            }

            @Override
            public void save(double latitudeDegrees,
                             double eastLongitudeDegrees) {
                node.putDouble("place.latitude", latitudeDegrees);
                node.putDouble("place.eastLongitude", eastLongitudeDegrees);
            }

            @Override
            public boolean remembered() {
                return node.get("place.latitude", null) != null;
            }

            @Override
            public void flush() {
                try {
                    node.flush();
                } catch (java.util.prefs.BackingStoreException e) {
                    throw new IllegalStateException(
                            "the place could not be remembered", e);
                }
            }
        };
    }
}
