package juranometria.tool.overview;

import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;

/**
 * What each projection does to shape and to scale (issue #296).
 *
 * <p>Measured from each candidate's own radius function rather than
 * quoted from a textbook, by differencing it - so the table is a
 * reading of the code that would draw the page, and a candidate whose
 * arithmetic drifted from its name would show it here.
 *
 * <p>Two costs, and conflating them hides a false claim, which is the
 * distinction Sprint 29's gate had to make and this one inherits.
 * <strong>Scale</strong> makes distances read wrong.
 * <strong>Anisotropy</strong> stretches shapes - a round cluster
 * becomes an ellipse - and shape is what a reader matches against the
 * sky.
 */
final class DistortionReport {

    private DistortionReport() {
    }

    /** Radial scale, tangential scale, and their ratio, at an angle. */
    static double[] at(StudyProjection projection, double angleDegrees) {
        double step = 1.0e-6;
        double radial = (projection.planeRadius(angleDegrees + step)
                - projection.planeRadius(angleDegrees - step))
                / (2.0 * Math.toRadians(step));
        double tangential = projection.planeRadius(angleDegrees)
                / Math.sin(Math.toRadians(angleDegrees));
        return new double[] {radial, tangential,
                Math.max(radial, tangential) / Math.min(radial, tangential)};
    }

    /**
     * The corner angle of a page of the given field width, at the
     * atlas's own 900x700 aspect - the place every cost is worst.
     */
    static double cornerDegrees(double fieldDegrees, StudyProjection projection) {
        // The corner is found on the page, not by a tangent-plane
        // formula: half the field across, times the aspect, then
        // asked back through the projection's own inverse. A
        // hemisphere projection has no corner beyond its limb.
        double halfWide = projection.planeRadius(fieldDegrees / 2.0);
        double halfHigh = halfWide * 700.0 / 900.0;
        double cornerRadius = Math.hypot(halfWide, halfHigh);
        double lo = 0.0;
        double hi = projection.limitDegrees();
        if (projection.planeRadius(hi) < cornerRadius) {
            return Double.NaN;  // the corner is off the projection
        }
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2.0;
            if (projection.planeRadius(mid) < cornerRadius) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    static String of(SkyPosition centre, double[] fields) {
        StringBuilder out = new StringBuilder();
        out.append("| projection | field | corner | scale at the corner |"
                + " shape at the corner |\n");
        out.append("|---|---:|---:|---:|---:|\n");
        for (StudyProjection projection : List.of(
                Candidates.gnomonic(centre),
                Candidates.stereographic(centre),
                Candidates.orthographic(centre))) {
            for (double field : fields) {
                double corner = cornerDegrees(field, projection);
                if (Double.isNaN(corner)
                        || field / 2.0 > projection.limitDegrees()) {
                    out.append(String.format(Locale.ROOT,
                            "| %s | %.0f° | — | — | the page leaves the"
                                    + " projection |%n",
                            projection.name(), field));
                    continue;
                }
                double[] centreScale = at(projection, 1.0e-4);
                double[] cornerScale = at(projection, corner);
                out.append(String.format(Locale.ROOT,
                        "| %s | %.0f° | %.1f° | %+.1f%% | %+.1f%% |%n",
                        projection.name(), field, corner,
                        100.0 * (cornerScale[0] / centreScale[0] - 1.0),
                        100.0 * (cornerScale[2] - 1.0)));
            }
        }
        return out.toString();
    }

    /**
     * What one page unit is worth, in the sky, at the centre and at
     * the corner.
     *
     * <p>The question behind it is not cartographic. A reader points
     * at something on the page and the atlas has to say what it is,
     * and a page unit that covers four arcminutes at the corner is a
     * page where pointing at one star of a pair means pointing at
     * both. This is the same anisotropy as the shape column, read in
     * the units the reader's finger works in.
     */
    static String pointing(SkyPosition centre, double[] fields,
                           double widthPx, double heightPx) {
        StringBuilder out = new StringBuilder();
        out.append("| projection | field | at the centre | at the corner |"
                + "\n|---|---:|---:|---:|\n");
        for (StudyProjection projection : List.of(
                Candidates.gnomonic(centre),
                Candidates.stereographic(centre),
                Candidates.orthographic(centre))) {
            for (double field : fields) {
                double corner = cornerDegrees(field, projection);
                if (Double.isNaN(corner)
                        || field / 2.0 > projection.limitDegrees()) {
                    continue;
                }
                double perPlane = widthPx
                        / (2.0 * projection.planeRadius(field / 2.0));
                // Radial scale is plane units per radian, so its
                // reciprocal over the page scale is radians per page
                // unit - the coarser of the two directions at the
                // corner, which is the one that decides.
                double middle = 3600.0 * Math.toDegrees(
                        1.0 / (at(projection, 1.0e-4)[0] * perPlane));
                double[] edge = at(projection, corner);
                double worst = 3600.0 * Math.toDegrees(
                        1.0 / (Math.min(edge[0], edge[1]) * perPlane));
                out.append(String.format(Locale.ROOT,
                        "| %s | %.0f° | %.0f\" | %.0f\" |%n",
                        projection.name(), field, middle, worst));
            }
        }
        return out.toString();
    }

    /** What each projection can show at all, and what it does at its edge. */
    static String domains(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| projection | shows | radius at 80° | at 89.9° |"
                + " at its own limit | edge |\n|---|---|---:|---:|---:|---|\n");
        for (StudyProjection projection : List.of(
                Candidates.gnomonic(centre),
                Candidates.stereographic(centre),
                Candidates.orthographic(centre))) {
            double limit = projection.limitDegrees();
            double atLimit = projection.planeRadius(
                    Math.min(limit, 179.99));
            // Bounded or not is the question a page has to answer:
            // one of these can draw its whole domain on a finite
            // sheet and the other two cannot.
            boolean bounded = atLimit < 100.0;
            out.append(String.format(Locale.ROOT,
                    "| %s | out to %.0f° | %.2f | %.1f | %s | %s |%n",
                    projection.name(), limit,
                    projection.planeRadius(80.0),
                    projection.planeRadius(89.9),
                    bounded ? String.format(Locale.ROOT, "%.2f", atLimit)
                            : "unbounded",
                    bounded
                            ? "a closed limb the whole domain fits inside"
                            : "grows without bound as the edge is"
                                    + " approached"));
        }
        return out.toString();
    }
}
