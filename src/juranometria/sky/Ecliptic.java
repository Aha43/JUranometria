package juranometria.sky;

import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * The ecliptic the atlas draws (Sprint 28, issue #272).
 *
 * <p><strong>The mean ecliptic of J2000</strong>, and no other. The
 * gate compared it with the ecliptic of date and chose it by
 * measurement: the two circles are about a pixel and a half apart at
 * the narrowest field, while their equinoxes are 157 px apart, so a
 * fixed atlas anchors both the circle and its landmarks to J2000
 * (docs/decisions/ecliptic.md).
 *
 * <p><strong>There is no epoch parameter, and that is deliberate.</strong>
 * Nothing here takes a date, an instant or a fidelity mode. An
 * implementation that can be asked for the ecliptic of date is an
 * implementation that will eventually be asked for it, and the one
 * thing the gate settled is that a fixed chart must not be. The
 * rejected shortcut is not reachable from this type.
 *
 * <p>Everything returned is in <strong>J2000</strong>, the only frame
 * the chart knows. Nothing here draws, projects, clips, reads a
 * clock, knows an observer or knows what a page is: it is the
 * ecliptic as geometry, and the chart decides what that looks like.
 *
 * <p>The circle itself is an ordinary {@link GreatCircle}. The gate
 * found no new geometry type was needed, and none is added here: a
 * pole is a direction, and the projection clips a great circle
 * analytically, so nothing is sampled and no tolerance is invented.
 */
public final class Ecliptic {

    private Ecliptic() {
    }

    /**
     * The J2000 mean obliquity, from the atlas's own model.
     *
     * <p>Taken from {@link SkyFrame} at t = 0 rather than written
     * again as a literal, so there is one obliquity in the atlas and
     * not two that can drift apart.
     */
    public static final double OBLIQUITY_DEGREES =
            SkyFrame.meanObliquityDegrees(0.0);

    /**
     * The ecliptic north pole: exactly the obliquity from the
     * celestial pole, on the RA 18h meridian.
     */
    public static final SkyPosition POLE =
            new SkyPosition(270.0, 90.0 - OBLIQUITY_DEGREES);

    /** The ecliptic, as the great circle about {@link #POLE}. */
    public static GreatCircle circle() {
        return new GreatCircle(POLE);
    }

    /**
     * A named place on the ecliptic that does not move.
     *
     * <p>The identity is stable and machine-facing, so a reader can
     * point at it and a test can name it; the name is what a reader
     * is told, and is <em>not</em> derived from the identity.
     *
     * <p>Named by month rather than by season: a southern reader's
     * summer is a northern reader's winter, and this geometry has no
     * observer to have a season at all.
     */
    public record Landmark(String identity, String name,
                           double longitudeDegrees, SkyPosition at) {

        public Landmark {
            if (identity == null || identity.isBlank()) {
                throw new IllegalArgumentException(
                        "a landmark carries a stable identity");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "a landmark carries a reader-facing name: "
                                + identity);
            }
            if (at == null) {
                throw new IllegalArgumentException(
                        "a landmark is somewhere: " + identity);
            }
        }
    }

    /**
     * The four cardinal landmarks, in order of ecliptic longitude.
     *
     * <p>Their positions come from {@link #toEquatorial} rather than
     * from four written-down coordinates, so a landmark cannot
     * silently disagree with the circle it is supposed to lie on: one
     * transformation, one place to be wrong, one place to be tested.
     */
    public static List<Landmark> landmarks() {
        return LANDMARKS;
    }

    private static final List<Landmark> LANDMARKS = List.of(
            landmark("march-equinox", "March equinox", 0.0),
            landmark("june-solstice", "June solstice", 90.0),
            landmark("september-equinox", "September equinox", 180.0),
            landmark("december-solstice", "December solstice", 270.0));

    private static Landmark landmark(String identity, String name,
                                     double longitudeDegrees) {
        return new Landmark(identity, name, longitudeDegrees,
                toEquatorial(longitudeDegrees, 0.0));
    }

    /** The landmark with this identity, or empty if there is none. */
    public static java.util.Optional<Landmark> landmark(String identity) {
        return LANDMARKS.stream()
                .filter(each -> each.identity().equals(identity))
                .findFirst();
    }

    /**
     * An ecliptic direction, expressed in the chart's J2000 frame.
     *
     * <p>The rotation is about the equinox axis by the obliquity, and
     * it is the whole of the transformation: there is no precession
     * here and nothing to precess, because both frames are J2000.
     *
     * @param longitudeDegrees ecliptic longitude, measured eastward
     *     from the March equinox; any real value, wrapped
     * @param latitudeDegrees ecliptic latitude, in [-90, 90]
     */
    public static SkyPosition toEquatorial(double longitudeDegrees,
                                           double latitudeDegrees) {
        if (!(latitudeDegrees >= -90.0 && latitudeDegrees <= 90.0)) {
            throw new IllegalArgumentException(
                    "ecliptic latitude must be in [-90, 90] degrees: "
                            + latitudeDegrees);
        }
        if (!Double.isFinite(longitudeDegrees)) {
            throw new IllegalArgumentException(
                    "ecliptic longitude must be a number: "
                            + longitudeDegrees);
        }
        double lambda = Math.toRadians(longitudeDegrees);
        double beta = Math.toRadians(latitudeDegrees);
        double eps = Math.toRadians(OBLIQUITY_DEGREES);

        double x = Math.cos(beta) * Math.cos(lambda);
        double y = Math.cos(beta) * Math.sin(lambda) * Math.cos(eps)
                - Math.sin(beta) * Math.sin(eps);
        double z = Math.cos(beta) * Math.sin(lambda) * Math.sin(eps)
                + Math.sin(beta) * Math.cos(eps);

        // Normalised, because right ascension is an angle on a circle
        // and SkyPosition holds it in [0, 360).
        return new SkyPosition(
                SkyFrame.normalise(Math.toDegrees(Math.atan2(y, x))),
                Math.toDegrees(Math.asin(clamp(z))));
    }

    /** An ecliptic longitude and latitude, both in degrees. */
    public record Coordinates(double longitudeDegrees,
                              double latitudeDegrees) {
    }

    /**
     * The ecliptic longitude and latitude of a J2000 direction: the
     * inverse of {@link #toEquatorial}.
     *
     * <p>Here so the transformation can be checked against itself as
     * well as against the authority. A round trip that returns the
     * angle it was given constrains the rotation from the other side,
     * and says which of longitude and latitude went wrong when one
     * does - which a separation from an oracle vector cannot.
     */
    public static Coordinates toEcliptic(SkyPosition position) {
        if (position == null) {
            throw new IllegalArgumentException(
                    "a direction is somewhere");
        }
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        double eps = Math.toRadians(OBLIQUITY_DEGREES);

        double x = Math.cos(dec) * Math.cos(ra);
        double y = Math.cos(dec) * Math.sin(ra);
        double z = Math.sin(dec);

        double lambdaY = y * Math.cos(eps) + z * Math.sin(eps);
        double betaZ = z * Math.cos(eps) - y * Math.sin(eps);

        return new Coordinates(
                SkyFrame.normalise(Math.toDegrees(Math.atan2(lambdaY, x))),
                Math.toDegrees(Math.asin(clamp(betaZ))));
    }

    /**
     * How far a direction lies from the ecliptic plane, in degrees:
     * positive north of it, negative south.
     *
     * <p>The same number {@link #toEcliptic} reports as latitude,
     * reached without the longitude, so a test can ask whether
     * something is on the circle without depending on where along it.
     */
    public static double latitudeDegrees(SkyPosition position) {
        return 90.0 - POLE.separationDegrees(position);
    }

    /** Guards asin against a magnitude a hair over one. */
    private static double clamp(double sine) {
        return Math.max(-1.0, Math.min(1.0, sine));
    }
}
