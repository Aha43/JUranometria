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

    static String of(double[] widths, double wide, double high) {
        rows = 0;
        worstMiss = 0.0;
        Rectangle2D page = new Rectangle2D.Double(0, 0, wide, high);
        StringBuilder out = new StringBuilder();
        out.append("| projection | centre | field | circle | form |"
                + " worst miss | runs | ends |\n");
        out.append("|---|---|---:|---|---|---:|---:|---|\n");
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
                        List<PageCurve.Run> runs =
                                built.get().curve().clipTo(page);
                        if (runs.isEmpty()) {
                            continue;  // off the page is silence
                        }
                        boolean closed = runs.stream()
                                .anyMatch(PageCurve.Run::closed);
                        rows++;
                        if (!Double.isNaN(built.get().residual())) {
                            worstMiss = Math.max(worstMiss,
                                    built.get().residual());
                        }
                        out.append(String.format(Locale.ROOT,
                                "| %s | %s | %.0f° | %s | %s | %s | %d |"
                                        + " %s |%n",
                                name, field.name(), width, circle.name(),
                                built.get().curve().form(),
                                Double.isNaN(built.get().residual())
                                        ? "—"
                                        : String.format(Locale.ROOT, "%.1e",
                                                built.get().residual()),
                                runs.size(),
                                closed ? "**none - it closes**"
                                        : "two per run"));
                    }
                }
            }
        }
        return out.toString();
    }
}
