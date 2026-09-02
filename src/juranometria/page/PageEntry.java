package juranometria.page;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;

/**
 * One thing on this page.
 *
 * <p>An entry holds the catalogue object itself rather than a set of
 * strings taken from it, so <em>what the source recorded</em> - and
 * every silence in it - survives the journey to a reader. 19.4% of
 * the bundled pack records no position angle, 68.1% no V magnitude,
 * 9.7% no size at all, and a model that flattened those into "0" or
 * "—" would be inventing facts on the catalogue's behalf.
 */
public sealed interface PageEntry {

    /** The stable catalogue identity this entry is known by. */
    String identity();

    /** Where the source records it. */
    SkyPosition position();

    /** Why it can or cannot be seen here. */
    PageVisibility visibility();

    /** How far from the page's centre, in degrees. */
    double separationDegrees();

    /** Whether the catalogue gives it a name of its own. */
    boolean named();

    /** A deep-sky object, with everything the source recorded. */
    record DeepSky(DeepSkyObject object, PageVisibility visibility,
                   double separationDegrees) implements PageEntry {

        public DeepSky {
            if (object == null || visibility == null) {
                throw new IllegalArgumentException(
                        "an entry always states its object and its"
                                + " visibility");
            }
        }

        @Override
        public String identity() {
            return object.id();
        }

        @Override
        public SkyPosition position() {
            return object.position();
        }

        /**
         * Every deep-sky object the atlas holds carries a catalogue
         * designation; none is anonymous the way a star can be.
         */
        @Override
        public boolean named() {
            return true;
        }
    }

    /**
     * A star.
     *
     * <p>Most are anonymous: a page holds hundreds of stars with
     * nothing but a catalogue number, and listing them would bury
     * the objects a reader is hunting. They are still entries -
     * counting them is a reader's question too - and
     * {@link #named()} is how a module tells the two apart.
     */
    record StarEntry(Star star, PageVisibility visibility,
                     double separationDegrees) implements PageEntry {

        public StarEntry {
            if (star == null || visibility == null) {
                throw new IllegalArgumentException(
                        "an entry always states its star and its"
                                + " visibility");
            }
        }

        @Override
        public String identity() {
            return star.id();
        }

        @Override
        public SkyPosition position() {
            return star.position();
        }

        @Override
        public boolean named() {
            juranometria.chart.StarIdentity identity = star.identity();
            return identity != null && (identity.name() != null
                    || identity.bayer() != null
                    || identity.flamsteed() != null);
        }
    }
}
