package juranometria.tool.overview;

import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;

/**
 * Where a curve stops being one thing and starts being another
 * (issue #296).
 *
 * <p>A great circle can pass arbitrarily close to a degenerate case:
 * a stereographic circle of unbounded radius is a line, and an
 * orthographic ellipse of no width is a line too. Nothing in the sky
 * forbids a pole from sitting a hundred-millionth of a degree away
 * from square to a page centre, and the atlas has to draw it.
 *
 * <p>Two reviews were needed to get this right, and the second was
 * the one that mattered. The first version substituted the simpler
 * form when a pole component fell below 1e-12, which was an
 * unjustified epsilon. Measuring it showed something worse: the
 * epsilon was in the wrong place. Between 1e-3 and 1e-12 the exact
 * circle's radius is so large that working out where it crosses a
 * page loses every digit that matters, so the band the epsilon was
 * protecting was the band it left unprotected. Replacing the epsilon
 * with an exact equality was no better - {@code cos(270 degrees)} is
 * -1.8e-16 in a double, so the exactly degenerate case almost never
 * arrives exactly.
 *
 * <p>The answer was to stop asking the question there. A projection
 * now states a conic, which stays finite through every degeneracy,
 * and the <em>page</em> decides which drawable form is right, by how
 * far the simpler curve would lie from the true one over that paper.
 * That leaves one number to choose, {@link StudyMapping} allowed page
 * units, and this measures it rather than asserting it.
 */
final class SubstitutionReport {

    private SubstitutionReport() {
    }

    /** The centre these poles are constructed around. */
    private static final SkyPosition CENTRE = new SkyPosition(0.0, 0.0);

    /**
     * The fields to measure over.
     *
     * <p>Both ends matter and they fail in opposite directions. A
     * narrow page has an enormous scale, so a small error in the
     * plane is multiplied hugely; a wide page reaches far from the
     * centre, where the simpler and true curves diverge most.
     */
    private static final double[] FIELDS = {1.0, 12.0, 42.0, 90.0, 180.0};

    /** How near square to the page centre the poles are put. */
    private static final double[] OFFSETS = {1.0e-1, 1.0e-2, 1.0e-3,
            1.0e-4, 1.0e-5, 1.0e-6, 1.0e-7, 1.0e-8, 1.0e-10, 1.0e-12,
            1.0e-15};

    /**
     * A pole whose component along the centre is {@code sin(offset)}.
     *
     * <p>Constructed rather than searched for: with the centre on the
     * equator at right ascension zero, a pole at right ascension
     * {@code 90 - d} has that component exactly.
     */
    private static SkyPosition nearlySquare(double offsetRadians) {
        return new SkyPosition(90.0 - Math.toDegrees(offsetRadians), 0.0);
    }

    /** A pole whose component along the centre approaches one. */
    private static SkyPosition nearlyCentred(double offsetRadians) {
        return new SkyPosition(Math.toDegrees(offsetRadians), 0.0);
    }

    /** The worst a drawn curve misses its own projected points by. */
    private static double worstOver(List<SkyPosition> poles,
                                    StudyProjection sky, double field,
                                    double allowed) {
        double worst = 0.0;
        StudyMapping mapping = new StudyMapping(sky, field, 900, 700,
                allowed);
        for (SkyPosition pole : poles) {
            var built = PageCurves.greatCircle(mapping, pole, 720);
            if (built.isPresent()
                    && !Double.isNaN(built.get().residual())) {
                worst = Math.max(worst, built.get().residual());
            }
        }
        return worst;
    }

    /** Curves that sit near a degeneracy, at one tolerance. */
    static double worstDegenerate(double allowed) {
        double worst = 0.0;
        for (double field : FIELDS) {
            for (double offset : OFFSETS) {
                worst = Math.max(worst, worstOver(
                        List.of(nearlySquare(offset)),
                        Candidates.stereographic(CENTRE), field, allowed));
                worst = Math.max(worst, worstOver(
                        List.of(nearlySquare(offset), nearlyCentred(offset)),
                        Candidates.orthographic(CENTRE), field, allowed));
            }
        }
        return worst;
    }

    /** Every curve the study actually draws, at one tolerance. */
    static double worstOrdinary(double allowed) {
        double worst = 0.0;
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            for (PageCurveReport.Field field : PageCurveReport.FIELDS) {
                StudyProjection projection =
                        Candidates.named(name, field.centre());
                for (double width : new double[] {42, 60, 90, 120, 180}) {
                    if (width / 2.0 >= projection.limitDegrees()) {
                        continue;
                    }
                    StudyMapping mapping = new StudyMapping(projection,
                            width, 900, 700, allowed);
                    for (PageCurveReport.Circle circle
                            : PageCurveReport.CIRCLES) {
                        var built = PageCurves.greatCircle(mapping,
                                circle.pole(), 720);
                        if (built.isPresent()
                                && !Double.isNaN(built.get().residual())) {
                            worst = Math.max(worst,
                                    built.get().residual());
                        }
                    }
                }
            }
        }
        return worst;
    }

    /** The tolerances the sweep considers, widest first. */
    static final double[] CANDIDATES = {1.0e-1, 1.0e-2, 1.0e-3, 1.0e-4,
            1.0e-5, 1.0e-6};

    /** The tolerance the sweep chooses, and the study uses. */
    static final double CHOSEN = 1.0e-3;

    static String of() {
        StringBuilder out = new StringBuilder();
        out.append("| allowed page units | worst miss, curves near a"
                + " degeneracy | worst miss, every curve the study"
                + " draws |\n|---:|---:|---:|\n");
        for (double allowed : CANDIDATES) {
            out.append(String.format(Locale.ROOT,
                    "| %.0e%s | %.2e | %.2e |%n", allowed,
                    allowed == CHOSEN ? " **(chosen)**" : "",
                    worstDegenerate(allowed), worstOrdinary(allowed)));
        }
        return out.toString();
    }
}
