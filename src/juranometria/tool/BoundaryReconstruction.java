package juranometria.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;
import juranometria.tool.ConstellationStudyMain.Segment;

/**
 * Reconstructs the true IAU constellation boundaries from corner
 * vertices, per docs/decisions/constellation-geography.md: boundary
 * edges are arcs of constant right ascension or constant declination
 * in the B1875 frame, so each J2000 corner pair is precessed to B1875,
 * classified, sampled along its constant coordinate in steps of at
 * most one degree, and precessed back. Corner chains the source has
 * already chorded across a constant-declination arc (the Ursa
 * Minor/Cepheus border at B1875 +88 degrees) are detected and restored
 * to the true arc. Anything that fits neither shape is a hard error -
 * boundaries are never silently bent.
 */
final class BoundaryReconstruction {

    /** A corner pair must align with a constant coordinate this well. */
    static final double ALIGNMENT_TOLERANCE_DEGREES = 0.02;

    /** Sampling step along the constant coordinate, degrees. */
    static final double SAMPLE_STEP_DEGREES = 1.0;

    /** The decision's boundary fidelity requirement, degrees (1'). */
    static final double RECONSTRUCTION_TOLERANCE_DEGREES = 1.0 / 60.0;

    record Report(List<Segment> reconstructed, int constantRaEdges,
                  int constantDecEdges, List<String> exceptionalChains,
                  double worstChordDeviationDegrees, int chordsOverThreeArcmin,
                  double worstReconstructionDeviationDegrees) {
    }

    private BoundaryReconstruction() {
    }

    /** Reconstructs one closed ring of J2000 corner vertices. */
    static Report reconstructRings(List<List<SkyPosition>> ringsById,
                                   List<String> ids) {
        List<Segment> reconstructed = new ArrayList<>();
        List<String> chains = new ArrayList<>();
        int raEdges = 0;
        int decEdges = 0;
        double worstChord = 0.0;
        int chordsOver = 0;
        double worstReconstruction = 0.0;

        for (int r = 0; r < ringsById.size(); r++) {
            String id = ids.get(r);
            List<SkyPosition> corners = ringsById.get(r);
            List<SkyPosition> b1875 = new ArrayList<>(corners.size());
            for (SkyPosition corner : corners) {
                b1875.add(PrecessionB1875.toB1875(corner));
            }

            int i = 0;
            while (i < corners.size() - 1) {
                SkyPosition a = b1875.get(i);
                int j = i + 1;
                String kind = classify(a, b1875.get(j));
                if (kind == null) {
                    // A chain of unclassifiable pairs: the source chorded
                    // a constant-declination arc. Extend to the chain's
                    // end and require the endpoints to share declination.
                    while (j < corners.size() - 1
                            && classify(b1875.get(j), b1875.get(j + 1)) == null) {
                        j++;
                    }
                    SkyPosition end = b1875.get(j);
                    if (Math.abs(a.decDegrees() - end.decDegrees())
                            > ALIGNMENT_TOLERANCE_DEGREES) {
                        throw new IllegalStateException(String.format(Locale.ROOT,
                                "%s: corner chain %s -> %s fits neither a"
                                        + " constant-RA nor a constant-Dec"
                                        + " B1875 edge; refusing to bend the"
                                        + " boundary", id, a, end));
                    }
                    chains.add(String.format(Locale.ROOT,
                            "%s: %d source chords across the constant-Dec"
                                    + " %.4f deg B1875 arc RA %.4f -> %.4f"
                                    + " restored to the true arc",
                            id, j - i, a.decDegrees(), a.raDegrees(),
                            end.raDegrees()));
                    kind = "dec";
                } else {
                    // Measure the straight-chord error the decision rejects.
                    double deviation = chordDeviation(a, b1875.get(j), kind,
                            corners.get(i), corners.get(j));
                    worstChord = Math.max(worstChord, deviation);
                    if (deviation > 3.0 / 60.0) {
                        chordsOver++;
                    }
                }
                SkyPosition end = b1875.get(j);
                List<SkyPosition> samples = sampleEdge(a, end, kind);
                for (int s = 1; s < samples.size(); s++) {
                    SkyPosition from = PrecessionB1875.toJ2000(samples.get(s - 1));
                    SkyPosition to = PrecessionB1875.toJ2000(samples.get(s));
                    reconstructed.add(new Segment(id, from, to));
                    worstReconstruction = Math.max(worstReconstruction,
                            pieceDeviation(samples.get(s - 1), samples.get(s),
                                    kind, from, to));
                }
                if ("ra".equals(kind)) {
                    raEdges++;
                } else {
                    decEdges++;
                }
                i = j;
            }
        }
        return new Report(reconstructed, raEdges, decEdges, chains,
                worstChord, chordsOver, worstReconstruction);
    }

    /** "ra" for a constant-RA edge, "dec" for constant-Dec, else null. */
    private static String classify(SkyPosition a, SkyPosition b) {
        double dra = Math.abs(wrap(a.raDegrees() - b.raDegrees()));
        double dde = Math.abs(a.decDegrees() - b.decDegrees());
        if (dra <= ALIGNMENT_TOLERANCE_DEGREES) {
            return "ra";
        }
        if (dde <= ALIGNMENT_TOLERANCE_DEGREES) {
            return "dec";
        }
        return null;
    }

    /** Samples the constant-coordinate B1875 edge, endpoints included. */
    private static List<SkyPosition> sampleEdge(SkyPosition a, SkyPosition b,
                                                String kind) {
        double span = "ra".equals(kind)
                ? b.decDegrees() - a.decDegrees()
                : wrap(b.raDegrees() - a.raDegrees());
        int steps = Math.max(1, (int) Math.ceil(
                Math.abs(span) / SAMPLE_STEP_DEGREES));
        List<SkyPosition> samples = new ArrayList<>(steps + 1);
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            if ("ra".equals(kind)) {
                samples.add(new SkyPosition(a.raDegrees(),
                        a.decDegrees() + t * span));
            } else {
                samples.add(new SkyPosition(
                        (a.raDegrees() + t * span + 360.0) % 360.0,
                        a.decDegrees()));
            }
        }
        return samples;
    }

    /**
     * Maximum deviation of the true B1875 edge from the straight J2000
     * chord between its corners - the error of drawing corners directly.
     */
    private static double chordDeviation(SkyPosition a1875, SkyPosition b1875,
                                         String kind, SkyPosition a2000,
                                         SkyPosition b2000) {
        double worst = 0.0;
        for (int s = 1; s < 32; s++) {
            SkyPosition truePoint = PrecessionB1875.toJ2000(
                    along(a1875, b1875, kind, s / 32.0));
            worst = Math.max(worst,
                    greatCircleDistance(truePoint, a2000, b2000));
        }
        return worst;
    }

    /**
     * Maximum deviation of the true edge from one reconstructed piece -
     * the residual error the decision bounds at one arcminute.
     */
    private static double pieceDeviation(SkyPosition a1875, SkyPosition b1875,
                                         String kind, SkyPosition from2000,
                                         SkyPosition to2000) {
        double worst = 0.0;
        for (int s = 1; s < 4; s++) {
            SkyPosition truePoint = PrecessionB1875.toJ2000(
                    along(a1875, b1875, kind, s / 4.0));
            worst = Math.max(worst,
                    greatCircleDistance(truePoint, from2000, to2000));
        }
        return worst;
    }

    private static SkyPosition along(SkyPosition a, SkyPosition b,
                                     String kind, double t) {
        if ("ra".equals(kind)) {
            return new SkyPosition(a.raDegrees(),
                    a.decDegrees() + t * (b.decDegrees() - a.decDegrees()));
        }
        double span = wrap(b.raDegrees() - a.raDegrees());
        return new SkyPosition((a.raDegrees() + t * span + 360.0) % 360.0,
                a.decDegrees());
    }

    /** Angular distance of p from the great circle through a and b. */
    private static double greatCircleDistance(SkyPosition p, SkyPosition a,
                                              SkyPosition b) {
        double[] va = unit(a);
        double[] vb = unit(b);
        double[] vp = unit(p);
        double[] n = {va[1] * vb[2] - va[2] * vb[1],
                va[2] * vb[0] - va[0] * vb[2],
                va[0] * vb[1] - va[1] * vb[0]};
        double norm = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (norm == 0.0) {
            return 0.0;
        }
        double sine = (vp[0] * n[0] + vp[1] * n[1] + vp[2] * n[2]) / norm;
        return Math.abs(Math.toDegrees(Math.asin(Math.clamp(sine, -1.0, 1.0))));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double wrap(double degrees) {
        return ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }
}
