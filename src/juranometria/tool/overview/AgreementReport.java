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

    static String of(SkyPosition centre) {
        StringBuilder out = new StringBuilder();
        out.append("| candidate | checked against | worst difference |"
                + " round trip | reaches |\n|---|---|---:|---:|---:|\n");
        int all = sample().size();
        out.append(String.format(Locale.ROOT,
                "| gnomonic | production's `GnomonicProjection` | %.3e |"
                        + " %.1e° | %d of %d |%n",
                gnomonicAgreement(centre),
                roundTrip(Candidates.gnomonic(centre)),
                reaches(Candidates.gnomonic(centre)), all));
        out.append(String.format(Locale.ROOT,
                "| stereographic | Sprint 29's `StereographicCandidate`"
                        + " | %.3e | %.1e° | %d of %d |%n",
                stereographicAgreement(centre),
                roundTrip(Candidates.stereographic(centre)),
                reaches(Candidates.stereographic(centre)), all));
        out.append(String.format(Locale.ROOT,
                "| orthographic | its own definition, `sin(theta)` | %.3e |"
                        + " %.1e° | %d of %d |%n",
                orthographicAgreement(centre),
                roundTrip(Candidates.orthographic(centre)),
                reaches(Candidates.orthographic(centre)), all));
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
