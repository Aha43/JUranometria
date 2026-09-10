package juranometria.tool.overview;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PlanePoint;
import juranometria.tool.StereographicCandidate;

/**
 * Whether the study's candidates are the projections they claim to be
 * (issue #296).
 *
 * <p>The three candidates are built by one shared azimuthal factory
 * that differs only in a radius function, which is what makes them
 * comparable - and is also exactly how a study can measure the wrong
 * thing convincingly. So each one is checked against something that
 * was written independently and reviewed before this issue existed:
 *
 * <ul>
 *   <li>the gnomonic candidate against production's own
 *       {@code GnomonicProjection}, which draws every released
 *       page;</li>
 *   <li>the stereographic candidate against
 *       {@code StereographicCandidate}, written for the Sprint 29
 *       gate (issue #283) from the projection's closed form rather
 *       than from a radius function.</li>
 * </ul>
 *
 * <p>The orthographic candidate has nothing in this repository to be
 * checked against, so it is held to its own definition instead: a
 * point {@code theta} from the centre lands at {@code sin(theta)},
 * and the limb is a circle of radius one.
 *
 * <p>Every candidate is also round-tripped, sky to plane and back,
 * because a projection that could not be inverted would be a
 * projection a reader could not point at.
 */
final class AgreementReport {

    private AgreementReport() {
    }

    /** Positions spread over the whole sphere, deterministically. */
    static List<SkyPosition> sample() {
        List<SkyPosition> positions = new java.util.ArrayList<>();
        for (double dec = -85.0; dec <= 85.0; dec += 5.0) {
            for (double ra = 0.0; ra < 360.0; ra += 15.0) {
                positions.add(new SkyPosition(ra, dec));
            }
        }
        return positions;
    }

    /** The worst the study's gnomonic differs from production's. */
    static double gnomonicAgreement(SkyPosition centre) {
        StudyProjection study = Candidates.gnomonic(centre);
        GnomonicProjection production = new GnomonicProjection(centre);
        return worst(study, production::project);
    }

    /** The worst the study's stereographic differs from Sprint 29's. */
    static double stereographicAgreement(SkyPosition centre) {
        StudyProjection study = Candidates.stereographic(centre);
        StereographicCandidate sprint29 = new StereographicCandidate(centre);
        return worst(study, sprint29::project);
    }

    private interface Projector {
        Optional<PlanePoint> project(SkyPosition position);
    }

    private static double worst(StudyProjection study, Projector other) {
        double found = 0.0;
        for (SkyPosition position : sample()) {
            Optional<PlanePoint> mine = study.project(position);
            Optional<PlanePoint> theirs = other.project(position);
            if (mine.isEmpty() || theirs.isEmpty()) {
                // A disagreement about the domain is worse than a
                // disagreement about a coordinate, so it is not
                // quietly skipped.
                if (mine.isPresent() != theirs.isPresent()) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }
            found = Math.max(found, Math.hypot(
                    mine.get().xiEast() - theirs.get().xiEast(),
                    mine.get().etaNorth() - theirs.get().etaNorth()));
        }
        return found;
    }

    /** The worst a projection loses by being inverted, in degrees. */
    static double roundTrip(StudyProjection projection) {
        double found = 0.0;
        for (SkyPosition position : sample()) {
            Optional<PlanePoint> plane = projection.project(position);
            if (plane.isEmpty()) {
                continue;
            }
            Optional<SkyPosition> back = projection.unproject(plane.get());
            if (back.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            found = Math.max(found,
                    StudyScenes.separation(position, back.get()));
        }
        return found;
    }

    /** How much of the sphere each candidate can put on a plane. */
    static int reaches(StudyProjection projection) {
        int reached = 0;
        for (SkyPosition position : sample()) {
            if (projection.project(position).isPresent()) {
                reached++;
            }
        }
        return reached;
    }

    /**
     * How far from exact agreement still counts as agreement.
     *
     * <p>The residues are rounding in double arithmetic - 7.8e-14
     * degrees on one machine and 7.7e-14 on another, because a JDK
     * and a chip are free to associate a sum differently. The claim
     * the study makes is that the candidate agrees with production
     * and that a round trip returns where it started; a tolerance
     * nine orders of magnitude above the residue says exactly that
     * and nothing about a machine's last decimal (#315).
     */
    static final double AGREES_WITHIN = 1e-9;

    static String of(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| candidate | checked against | agrees within "
                + AGREES_WITHIN + " | round trip returns within "
                + AGREES_WITHIN + "° | reaches |\n"
                + "|---|---|---|---|---:|\n");
        int all = sample().size();
        out.append(row("gnomonic", "production's `GnomonicProjection`",
                gnomonicAgreement(centre),
                roundTrip(Candidates.gnomonic(centre)),
                reaches(Candidates.gnomonic(centre)), all));
        out.append(row("stereographic",
                "Sprint 29's `StereographicCandidate`",
                stereographicAgreement(centre),
                roundTrip(Candidates.stereographic(centre)),
                reaches(Candidates.stereographic(centre)), all));
        out.append(row("orthographic",
                "its own definition, `sin(theta)`",
                orthographicAgreement(centre),
                roundTrip(Candidates.orthographic(centre)),
                reaches(Candidates.orthographic(centre)), all));
        return out.toString();
    }

    private static String row(String candidate, String against,
                              double difference, double trip,
                              int reaches, int all) {
        if (difference > AGREES_WITHIN || trip > AGREES_WITHIN) {
            throw new IllegalStateException(candidate + " no longer"
                    + " agrees with " + against + " to within "
                    + AGREES_WITHIN + ": " + difference + ", round"
                    + " trip " + trip);
        }
        return String.format(Locale.ROOT,
                "| %s | %s | yes | yes | %d of %d |%n",
                candidate, against, reaches, all);
    }

    /** The residues themselves, for the record beside the report. */
    static String observed(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| candidate | worst difference | round trip |\n"
                + "|---|---:|---:|\n");
        out.append(String.format(Locale.ROOT,
                "| gnomonic | %.3e | %.1e° |%n",
                gnomonicAgreement(centre),
                roundTrip(Candidates.gnomonic(centre))));
        out.append(String.format(Locale.ROOT,
                "| stereographic | %.3e | %.1e° |%n",
                stereographicAgreement(centre),
                roundTrip(Candidates.stereographic(centre))));
        out.append(String.format(Locale.ROOT,
                "| orthographic | %.3e | %.1e° |%n",
                orthographicAgreement(centre),
                roundTrip(Candidates.orthographic(centre))));
        return out.toString();
    }

    /** The orthographic radius against the sine it is defined to be. */
    static double orthographicAgreement(SkyPosition centre) {
        StudyProjection orthographic = Candidates.orthographic(centre);
        double found = 0.0;
        for (double angle = 0.0; angle <= 90.0; angle += 0.25) {
            found = Math.max(found, Math.abs(orthographic.planeRadius(angle)
                    - Math.sin(Math.toRadians(angle))));
        }
        return found;
    }
}
