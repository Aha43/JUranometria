package juranometria.tool.allsky;

/**
 * The three whole-sky forms the #686 study draws, on a unit sphere.
 *
 * <p><strong>Study only.</strong> This lives in the tool package for
 * the same reason {@code StereographicCandidate} did for #283: the
 * study changes no production behaviour, and nothing adopts a form
 * until the owner rules. If a form is admitted, #687 moves a reviewed
 * implementation into {@code juranometria.project}; the rest is
 * deleted with the study.
 *
 * <p>Coordinates: {@code lambda} is longitude from the central
 * meridian in radians, in {@code [-pi, pi]}, positive towards
 * increasing right ascension or ecliptic longitude (celestial east);
 * {@code phi} is latitude in radians. The plane's {@code x} is
 * positive east and {@code y} positive north - the same sense as
 * {@code PlanePoint}'s {@code xiEast}/{@code etaNorth}; which way east
 * faces on paper is the page's business, not the form's.
 *
 * <p>Formulas, as transcribed from Wolfram MathWorld (pages read
 * 2026-11-08), each of which attributes them to J. P. Snyder,
 * <em>Map Projections - A Working Manual</em>, USGS Professional
 * Paper 1395 (1987). The USGS copy refused an automated fetch, so the
 * Snyder page numbers are MathWorld's, and every formula below is held
 * by an executable check rather than by the citation alone
 * ({@code AllSkyGeometryTest}): forward and inverse round-trip, the
 * Mollweide auxiliary equation's residual, equal area by the
 * Jacobian, and the outline.
 * <ul>
 * <li>Hammer (Hammer-Aitoff), MathWorld "Hammer-Aitoff Equal-Area
 * Projection" eqs (1)-(5); derivation from the equatorial Lambert
 * azimuthal equal-area, Snyder 1987 pp. 182, 185.</li>
 * <li>Mollweide, MathWorld "Mollweide Projection" eqs (1)-(3), (7)-(9);
 * Snyder 1987 pp. 249-252. The auxiliary angle is solved here by a
 * bracketed Newton iteration of our own (eq (4)'s step, safeguarded by
 * bisection); MathWorld's suggested first guess, eq (6), was not
 * adopted because it disagrees with the equation it is meant to start
 * (it gives 1.0472 where the root is 1.2672 at 45 degrees).</li>
 * <li>Equidistant cylindrical with standard parallel 0
 * (equirectangular, plate carree), MathWorld "Cylindrical Equidistant
 * Projection" eqs (1)-(4); Snyder 1987 pp. 90-91. Scaled here by
 * {@code sqrt 2 * 2 / pi} on both axes so that its outline has the
 * same height as the two ellipses' ({@code y = +-sqrt 2} at the
 * poles), which makes a drawn comparison at one page size fair; the
 * scale is a page choice and does not change the form.</li>
 * </ul>
 */
public enum AllSkyForm {

    HAMMER("Hammer"),

    MOLLWEIDE("Mollweide"),

    EQUIRECTANGULAR("equirectangular");

    private static final double SQRT2 = Math.sqrt(2.0);

    /** Equirectangular scale so its poles sit at y = +-sqrt 2. */
    private static final double EQUIRECT_SCALE = SQRT2 * 2.0 / Math.PI;

    private final String displayName;

    AllSkyForm(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Half-width of the outline on the plane: at lambda = +-pi, phi = 0. */
    public double halfWidth() {
        return switch (this) {
            case HAMMER, MOLLWEIDE -> 2.0 * SQRT2;
            case EQUIRECTANGULAR -> EQUIRECT_SCALE * Math.PI;
        };
    }

    /** Half-height of the outline on the plane: at the poles. */
    public double halfHeight() {
        return SQRT2;
    }

    /** Whether equal areas on the sky are equal areas on the plane. */
    public boolean equalArea() {
        return this != EQUIRECTANGULAR;
    }

    /** {x, y} for longitude lambda (from the central meridian) and latitude phi. */
    public double[] forward(double lambda, double phi) {
        return switch (this) {
            case HAMMER -> {
                // MathWorld Hammer-Aitoff eqs (1), (2).
                double cosPhi = Math.cos(phi);
                double d = Math.sqrt(1.0 + cosPhi * Math.cos(lambda / 2.0));
                yield new double[] {
                        2.0 * SQRT2 * cosPhi * Math.sin(lambda / 2.0) / d,
                        SQRT2 * Math.sin(phi) / d};
            }
            case MOLLWEIDE -> {
                // MathWorld Mollweide eqs (1), (2).
                double theta = mollweideTheta(phi);
                yield new double[] {
                        2.0 * SQRT2 * lambda * Math.cos(theta) / Math.PI,
                        SQRT2 * Math.sin(theta)};
            }
            case EQUIRECTANGULAR -> new double[] {
                    // MathWorld eqs (1), (2) with phi_1 = 0, scaled.
                    EQUIRECT_SCALE * lambda, EQUIRECT_SCALE * phi};
        };
    }

    /**
     * {lambda, phi} for a plane point, or null where the plane has no
     * sky under it (outside the outline).
     */
    public double[] inverse(double x, double y) {
        return switch (this) {
            case HAMMER -> {
                if ((x * x) / 8.0 + (y * y) / 2.0 > 1.0) {
                    yield null;
                }
                // MathWorld Hammer-Aitoff eqs (3)-(5); the arctangent
                // takes both signs (atan2), as the page specifies.
                double z = Math.sqrt(Math.max(0.0,
                        1.0 - (x / 4.0) * (x / 4.0) - (y / 2.0) * (y / 2.0)));
                double lambda = 2.0 * Math.atan2(z * x, 2.0 * (2.0 * z * z - 1.0));
                double phi = Math.asin(clamp(y * z));
                yield new double[] {lambda, phi};
            }
            case MOLLWEIDE -> {
                if ((x * x) / 8.0 + (y * y) / 2.0 > 1.0) {
                    yield null;
                }
                // MathWorld Mollweide eqs (9), (7), (8).
                double theta = Math.asin(clamp(y / SQRT2));
                double phi = Math.asin(clamp(
                        (2.0 * theta + Math.sin(2.0 * theta)) / Math.PI));
                double cosTheta = Math.cos(theta);
                double lambda = cosTheta == 0.0 ? 0.0
                        : Math.PI * x / (2.0 * SQRT2 * cosTheta);
                if (Math.abs(lambda) > Math.PI) {
                    yield null;
                }
                yield new double[] {lambda, phi};
            }
            case EQUIRECTANGULAR -> {
                double lambda = x / EQUIRECT_SCALE;
                double phi = y / EQUIRECT_SCALE;
                if (Math.abs(lambda) > Math.PI || Math.abs(phi) > Math.PI / 2.0) {
                    yield null;
                }
                yield new double[] {lambda, phi};
            }
        };
    }

    /** Whether a plane point lies inside the outline (on it counts). */
    public boolean inside(double x, double y) {
        return switch (this) {
            case HAMMER, MOLLWEIDE -> (x * x) / 8.0 + (y * y) / 2.0 <= 1.0;
            case EQUIRECTANGULAR -> Math.abs(x) <= halfWidth()
                    && Math.abs(y) <= halfHeight();
        };
    }

    /**
     * The Mollweide auxiliary angle: theta with
     * 2 theta + sin 2 theta = pi sin phi (MathWorld eq (3)).
     *
     * <p>Solved as t = 2 theta in t + sin t = pi sin phi. The left side
     * is non-decreasing in t on [-pi, pi] (its derivative 1 + cos t is
     * zero only at the ends), so the root is unique and bracketed;
     * Newton's step (MathWorld eq (4)) is taken when it stays inside
     * the bracket and bisection otherwise. Near the poles the
     * derivative vanishes and Newton alone converges only linearly,
     * which is why the bracket is here.
     */
    static double mollweideTheta(double phi) {
        double target = Math.PI * Math.sin(phi);
        if (target >= Math.PI) {
            return Math.PI / 2.0;
        }
        if (target <= -Math.PI) {
            return -Math.PI / 2.0;
        }
        double lo = -Math.PI;
        double hi = Math.PI;
        double t = phi; // any start inside the bracket
        for (int i = 0; i < 200; i++) {
            double f = t + Math.sin(t) - target;
            if (f == 0.0) {
                break;
            }
            if (f > 0.0) {
                hi = t;
            } else {
                lo = t;
            }
            double slope = 1.0 + Math.cos(t);
            double next = slope > 0.0 ? t - f / slope : Double.NaN;
            if (!(next > lo && next < hi)) {
                next = 0.5 * (lo + hi);
            }
            if (Math.abs(next - t) <= 1e-15 * Math.max(1.0, Math.abs(t))) {
                t = next;
                break;
            }
            t = next;
        }
        return t / 2.0;
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
