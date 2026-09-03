package juranometria.sky;

import java.time.Instant;

/**
 * Where a reader is, and when they are looking (Sprint 25, issue
 * #226).
 *
 * <p>Decided by the gate in {@code docs/decisions/place-and-time.md}
 * and repeated here only where the code has to enforce it:
 *
 * <ul>
 *   <li><strong>Longitude is east-positive.</strong> The single
 *       easiest thing to get wrong, and a chart drawn for the wrong
 *       hemisphere looks entirely plausible - so the name says which
 *       way it counts, and the field cannot be read as anything
 *       else.</li>
 *   <li><strong>The instant is explicit and frozen.</strong> There
 *       is no default, no clock and no "now" hiding in a
 *       constructor: a caller that wants the present moment says so,
 *       once, and what it gets back never moves again.</li>
 * </ul>
 *
 * <p>Immutable, and free of everything: no Swing, no preferences, no
 * renderer, no operating system, no network. It is two angles and a
 * point in time.
 */
public record Observer(double latitudeDegrees, double eastLongitudeDegrees,
                       Instant instant) {

    public Observer {
        if (Double.isNaN(latitudeDegrees)
                || latitudeDegrees < -90 || latitudeDegrees > 90) {
            throw new IllegalArgumentException(
                    "a latitude runs from -90 to 90 degrees: "
                            + latitudeDegrees);
        }
        if (Double.isNaN(eastLongitudeDegrees)
                || Double.isInfinite(eastLongitudeDegrees)) {
            throw new IllegalArgumentException(
                    "a longitude is a number of degrees east: "
                            + eastLongitudeDegrees);
        }
        if (instant == null) {
            throw new IllegalArgumentException(
                    "an observer looks at a stated instant; there is no"
                            + " default, because a default would be a"
                            + " clock");
        }
    }

    /**
     * The same place at another instant - which is what every
     * control that changes the time does.
     */
    public Observer at(Instant when) {
        return new Observer(latitudeDegrees, eastLongitudeDegrees, when);
    }

    /**
     * The same instant somewhere else.
     */
    public Observer from(double latitude, double eastLongitude) {
        return new Observer(latitude, eastLongitude, instant);
    }

    /**
     * Longitude folded into (-180, 180], for display and for
     * comparison. The stored value is left as the reader typed it:
     * 190° east and 170° west are the same meridian, and neither is
     * wrong.
     */
    public double eastLongitudeFolded() {
        double folded = eastLongitudeDegrees % 360.0;
        if (folded > 180.0) {
            folded -= 360.0;
        }
        if (folded <= -180.0) {
            folded += 360.0;
        }
        return folded;
    }
}
