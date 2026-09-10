package juranometria.tool.overview;

import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;

/** The curve-form section of the gate's report (issue #296). */
final class CurveFormReport {

    private CurveFormReport() {
    }

    /** Great circles the atlas actually draws, plus the awkward one. */
    private static final Object[][] CIRCLES = {
            {"the celestial equator", new SkyPosition(0.0, 90.0)},
            {"the ecliptic", new SkyPosition(270.0, 66.56)},
            {"a circle through the page centre", new SkyPosition(173.0, 0.0)},
    };

    static String of(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| projection | great circle | points | as a line |"
                + " as a circle | as a conic |\n");
        out.append("|---|---|---:|---:|---:|---:|\n");

        for (StudyProjection projection : List.of(
                Candidates.gnomonic(centre),
                Candidates.stereographic(centre),
                Candidates.orthographic(centre))) {
            for (Object[] circle : CIRCLES) {
                var points = CurveForm.project(projection,
                        (SkyPosition) circle[1], 400);
                out.append(String.format(Locale.ROOT,
                        "| %s | %s | %d | %s | %s | %s |%n",
                        projection.name(), circle[0], points.size(),
                        show(CurveForm.asLine(points)),
                        show(CurveForm.asCircle(points)),
                        show(CurveForm.asConic(points))));
            }
        }
        return out.toString();
    }

    /**
     * Whether the curve takes this form, rather than by how much.
     *
     * <p>The residuals separate by fourteen orders of magnitude: a
     * curve that is a circle fits it to 7e-15 and a curve that is
     * not a line misses it by 1e+01. Which side of that gap a fit
     * falls on is the study's answer and is the same on any machine;
     * the last two digits of a 1e-30 are a JDK and a chip
     * associating a sum differently, and are recorded beside the
     * report (#315).
     */
    static final double FITS_WITHIN = 1e-6;

    private static String show(CurveForm.Fit fit) {
        if (fit.degenerate()) {
            return "degenerate";
        }
        return fit.worstResidual() <= FITS_WITHIN ? "yes" : "no";
    }

    /** The residuals themselves, for the record beside the report. */
    static String observed(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| projection | great circle | points | as a line |"
                + " as a circle | as a conic |\n");
        out.append("|---|---|---:|---:|---:|---:|\n");
        for (StudyProjection projection : List.of(
                Candidates.gnomonic(centre),
                Candidates.stereographic(centre),
                Candidates.orthographic(centre))) {
            for (Object[] circle : CIRCLES) {
                var points = CurveForm.project(projection,
                        (SkyPosition) circle[1], 400);
                out.append(String.format(Locale.ROOT,
                        "| %s | %s | %d | %s | %s | %s |%n",
                        projection.name(), circle[0], points.size(),
                        magnitude(CurveForm.asLine(points)),
                        magnitude(CurveForm.asCircle(points)),
                        magnitude(CurveForm.asConic(points))));
            }
        }
        return out.toString();
    }

    private static String magnitude(CurveForm.Fit fit) {
        return fit.degenerate() ? "degenerate"
                : String.format(Locale.ROOT, "%.1e",
                        fit.worstResidual());
    }
}
