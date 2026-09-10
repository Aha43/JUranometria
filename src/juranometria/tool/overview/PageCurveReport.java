package juranometria.tool.overview;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * What a page actually asks of the curve vocabulary (issue #296).
 *
 * <p>{@link CurveFormReport} asks what shape a great circle takes.
 * This asks the two questions production's
 * {@code Optional<GreatCirclePage.Arc>} silently answers "once" and
 * "it has two ends" to: <strong>how many separate runs</strong> the
 * curve makes across one page, and whether any of them
 * <strong>closes</strong> with no ends for a label to hang on.
 */
final class PageCurveReport {

    private PageCurveReport() {
    }

    /** The circles the atlas already draws, plus a horizon. */
    record Circle(String name, SkyPosition pole) {
    }

    /**
     * The circles the atlas already draws, and the one it draws for
     * an observer.
     *
     * <p>A great circle is named by its pole. The meridian's pole
     * lies on the celestial equator, ninety degrees of right
     * ascension from the observer's own; the horizon's pole is the
     * zenith. Both are placed here at an ordinary evening hour for
     * Bergen, so that the pages carry a meridian, a mathematical
     * horizon and the ecliptic together, as the issue asks.
     */
    static final List<Circle> CIRCLES = List.of(
            new Circle("the celestial equator", new SkyPosition(0.0, 90.0)),
            new Circle("the ecliptic", new SkyPosition(270.0, 66.5607)),
            new Circle("a meridian", new SkyPosition(150.0, 0.0)),
            new Circle("a horizon", new SkyPosition(60.0, 25.0)));

    record Field(String name, SkyPosition centre) {
    }

    static final List<Field> FIELDS = List.of(
            new Field("Orion", new SkyPosition(83.0, 0.0)),
            new Field("the north pole", new SkyPosition(0.0, 90.0)),
            new Field("the vernal equinox", new SkyPosition(0.0, 0.0)));

    /** How many rows the last table held, and the worst miss in it. */
    static int rows;
    static double worstMiss;

    /**
     * How often the form the projection stated was also the form an
     * independent fit to projected points arrived at, and how often
     * it was not.
     */
    static int agreed;
    static int disagreed;

    /** The residuals themselves, for the record beside the report. */
    static final StringBuilder observed = new StringBuilder();

    static String of(double[] widths, double wide, double high) {
        rows = 0;
        worstMiss = 0.0;
        agreed = 0;
        disagreed = 0;
        Rectangle2D page = new Rectangle2D.Double(0, 0, wide, high);
        observed.setLength(0);
        observed.append("| projection | centre | field | circle |"
                + " worst miss |\n|---|---|---:|---|---:|\n");
        StringBuilder out = new StringBuilder();
        out.append("| projection | centre | field | circle | form |"
                + " passes through every point | runs | ends |\n");
        out.append("|---|---|---:|---|---|---|---:|---|\n");
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            for (Field field : FIELDS) {
                StudyProjection projection = Candidates.named(name,
                        field.centre());
                for (double width : widths) {
                    if (width / 2.0 >= projection.limitDegrees()) {
                        continue;
                    }
                    StudyMapping mapping = new StudyMapping(projection,
                            width, wide, high);
                    for (Circle circle : CIRCLES) {
                        Optional<PageCurves.Built> built =
                                PageCurves.greatCircle(mapping,
                                        circle.pole(), 720);
                        if (built.isEmpty()) {
                            continue;
                        }
                        List<PlaneCurve.Run> runs = built.get().curve()
                                .clipTo(mapping.region());
                        if (runs.isEmpty()) {
                            continue;  // off the page is silence
                        }
                        boolean closed = runs.stream()
                                .anyMatch(PlaneCurve.Run::closed);
                        rows++;
                        // The same curve, arrived at the other way.
                        String fitted = PageCurves
                                .fitted(mapping, circle.pole(), 720)
                                .map(PlaneCurve::form).orElse("none");
                        if (fitted.equals(built.get().curve().form())) {
                            agreed++;
                        } else {
                            disagreed++;
                        }
                        if (!Double.isNaN(built.get().residual())) {
                            worstMiss = Math.max(worstMiss,
                                    built.get().residual());
                        }
                        // The residual is rounding: 2.7e-13 on one
                        // machine and 2.8e-13 on another, because a
                        // JDK and a chip associate a sum differently.
                        // What the row is claiming is that the drawn
                        // curve passes through the projected points,
                        // and that is which side of the tolerance it
                        // falls on (#315). The magnitudes are in the
                        // record beside the report.
                        double residual = built.get().residual();
                        out.append(String.format(Locale.ROOT,
                                "| %s | %s | %.0f° | %s | %s | %s | %d |"
                                        + " %s |%n",
                                name, field.name(), width, circle.name(),
                                built.get().curve().form(),
                                Double.isNaN(residual) ? "—"
                                        : residual <= CurveFormReport
                                                .FITS_WITHIN
                                                ? "yes" : "**no**",
                                runs.size(),
                                closed ? "**none - it closes**"
                                        : "two per run"));
                        observed.append(String.format(Locale.ROOT,
                                "| %s | %s | %.0f° | %s | %s |%n",
                                name, field.name(), width,
                                circle.name(),
                                Double.isNaN(residual) ? "—"
                                        : String.format(Locale.ROOT,
                                                "%.1e", residual)));
                    }
                }
            }
        }
        return out.toString();
    }
}
