package juranometria.tool.allsky;

import juranometria.chart.SkyPosition;

/**
 * One whole-sky map: a form, an orientation and a central meridian
 * (#686, study only).
 *
 * <p>The seam is the meridian opposite the central one. A position on
 * it belongs to both edges; the map answers the east edge for it, and
 * says so through {@link #seamDistanceDegrees} so that a caller that
 * joins points can refuse to draw straight across the page.
 */
public record AllSkyMap(AllSkyForm form, AllSkyOrientation orientation,
                        double centralLongitudeDegrees) {

    public AllSkyMap {
        if (form == null || orientation == null) {
            throw new IllegalArgumentException("a map has a form and an orientation");
        }
        if (!Double.isFinite(centralLongitudeDegrees)) {
            throw new IllegalArgumentException("central longitude must be a number");
        }
        centralLongitudeDegrees = ((centralLongitudeDegrees % 360.0) + 360.0) % 360.0;
    }

    /** Longitude east of the central meridian, in (-180, 180]. */
    public double offsetDegrees(SkyPosition position) {
        double lon = orientation.toMap(position)[0];
        double d = ((lon - centralLongitudeDegrees) % 360.0 + 360.0) % 360.0;
        return d > 180.0 ? d - 360.0 : d;
    }

    /** How far a position is from the seam, in degrees of longitude. */
    public double seamDistanceDegrees(SkyPosition position) {
        return 180.0 - Math.abs(offsetDegrees(position));
    }

    /** {x, y} on the unit-sphere plane, x positive east. Every position has one. */
    public double[] project(SkyPosition position) {
        double[] m = orientation.toMap(position);
        return form.forward(Math.toRadians(offsetDegrees(position)),
                Math.toRadians(m[1]));
    }

    /** The sky under a plane point, or null outside the outline. */
    public SkyPosition unproject(double x, double y) {
        double[] lp = form.inverse(x, y);
        if (lp == null) {
            return null;
        }
        return orientation.toSky(centralLongitudeDegrees + Math.toDegrees(lp[0]),
                Math.toDegrees(lp[1]));
    }

    /**
     * Whether the short way from a to b crosses the seam: their offsets
     * lie on opposite sides of +-180 with less than 180 degrees between
     * them going round through it.
     */
    public boolean crossesSeam(SkyPosition a, SkyPosition b) {
        double oa = offsetDegrees(a);
        double ob = offsetDegrees(b);
        return Math.abs(oa - ob) > 180.0;
    }

    /** The sky position at the centre of the map. */
    public SkyPosition centre() {
        return orientation.toSky(centralLongitudeDegrees, 0.0);
    }

    public String describe() {
        return String.format(java.util.Locale.ROOT, "%s, %s, central longitude %.2f",
                form.displayName(), orientation.displayName(), centralLongitudeDegrees);
    }
}
