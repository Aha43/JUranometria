package juranometria.geo;

import juranometria.chart.SkyPosition;

/**
 * One great-circle segment of constellation geography - a piece of a
 * traditional line figure or of a reconstructed IAU boundary polyline -
 * tagged with the constellation it belongs to.
 */
public record GeoSegment(String constellationId, SkyPosition from,
                         SkyPosition to) {

    public GeoSegment {
        if (constellationId == null || constellationId.isBlank()) {
            throw new IllegalArgumentException("constellation id must not be blank");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("segment endpoints must not be null");
        }
    }

    /**
     * The smallest angular distance in degrees from a sky position to
     * this segment's great-circle arc - the exact geometry the bounded
     * query needs, correct across the RA wrap and at the poles because
     * it works in vector space, never in RA/Dec differences.
     */
    public double angularDistanceDegrees(SkyPosition position) {
        double[] a = unit(from);
        double[] b = unit(to);
        double[] p = unit(position);
        double[] n = cross(a, b);
        double norm = length(n);
        if (norm < 1e-12) {
            // Degenerate: endpoints (anti)parallel; fall back to endpoints.
            return Math.min(separation(p, a), separation(p, b));
        }
        // The closest point of the full great circle to p.
        double[] unitN = {n[0] / norm, n[1] / norm, n[2] / norm};
        double sine = dot(p, unitN);
        double circleDistance = Math.toDegrees(Math.abs(
                Math.asin(Math.clamp(sine, -1.0, 1.0))));
        // That closest point counts only if it lies within the arc:
        // project p onto the circle's plane and test betweenness via the
        // arc's interior direction at each endpoint.
        double[] foot = {p[0] - sine * unitN[0], p[1] - sine * unitN[1],
                p[2] - sine * unitN[2]};
        double footLength = length(foot);
        if (footLength > 1e-12) {
            foot = new double[] {foot[0] / footLength, foot[1] / footLength,
                    foot[2] / footLength};
            if (dot(cross(a, foot), n) >= 0.0 && dot(cross(foot, b), n) >= 0.0) {
                return circleDistance;
            }
        }
        return Math.min(separation(p, a), separation(p, b));
    }

    private static double separation(double[] u, double[] v) {
        return Math.toDegrees(Math.acos(Math.clamp(dot(u, v), -1.0, 1.0)));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double[] cross(double[] u, double[] v) {
        return new double[] {u[1] * v[2] - u[2] * v[1],
                u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
    }

    private static double dot(double[] u, double[] v) {
        return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
    }

    private static double length(double[] u) {
        return Math.sqrt(dot(u, u));
    }
}
