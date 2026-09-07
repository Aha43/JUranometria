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

    private static String show(CurveForm.Fit fit) {
        if (fit.degenerate()) {
            return "degenerate";
        }
        return String.format(Locale.ROOT, "%.1e", fit.worstResidual());
    }
}
