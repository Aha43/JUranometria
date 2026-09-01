package juranometria.tool;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * Which brightness level covers a place in the sky (Sprint 22, issue
 * #189).
 *
 * <p>This exists because the obvious approach does not work. Projecting
 * each ring point and closing whatever survives produces
 * <strong>wrong fills</strong>: a gnomonic page can only show the
 * hemisphere in front of it, so a ring that reaches past that horizon
 * arrives in pieces, each piece gets closed on its own, and the
 * even-odd rule then counts crossings that the sky does not have. The
 * study measured the consequence at the south galactic pole - the
 * emptiest sky there is - where the naive layer filled
 * <strong>100%</strong> of a page that should carry nothing at all.
 *
 * <p>So containment is asked of the sky instead of the page: every
 * pixel is turned back into a sky position through the atlas's own
 * inverse ({@code ChartHitTest.skyAt}, which is what grab-to-pan
 * uses), and that position is tested against the outlines. No
 * projection topology, no clipping rules, no seam - the answer is
 * the same whatever the page is showing.
 *
 * <p>Edges are bucketed by declination so a test touches only the
 * edges that could cross its own latitude.
 */
final class MilkyWaySky {

    /** One degree of declination per bucket. */
    private static final double BUCKET_DEGREES = 1.0;
    private static final int BUCKETS =
            (int) Math.ceil(180.0 / BUCKET_DEGREES) + 1;

    /** An edge of an outline, with the two ends it joins. */
    private record Edge(double ra1, double dec1, double ra2, double dec2) {
    }

    /** One level's edges, bucketed by the declinations they span. */
    private static final class LevelIndex {
        private final List<List<Edge>> buckets = new ArrayList<>(BUCKETS);

        LevelIndex() {
            for (int i = 0; i < BUCKETS; i++) {
                buckets.add(new ArrayList<>());
            }
        }

        void add(Edge edge) {
            int from = bucketOf(Math.min(edge.dec1(), edge.dec2()));
            int to = bucketOf(Math.max(edge.dec1(), edge.dec2()));
            for (int i = from; i <= to; i++) {
                buckets.get(i).add(edge);
            }
        }

        List<Edge> at(double dec) {
            return buckets.get(bucketOf(dec));
        }

        private static int bucketOf(double dec) {
            int index = (int) Math.floor((dec + 90.0) / BUCKET_DEGREES);
            return Math.max(0, Math.min(BUCKETS - 1, index));
        }
    }

    private final List<LevelIndex> levels = new ArrayList<>();

    MilkyWaySky(List<MilkyWayGeometry.Level> source) {
        for (MilkyWayGeometry.Level level : source) {
            LevelIndex index = new LevelIndex();
            for (MilkyWayGeometry.Ring ring : level.rings()) {
                List<SkyPosition> points = ring.points();
                for (int i = 0, j = points.size() - 1; i < points.size();
                        j = i++) {
                    index.add(new Edge(points.get(j).raDegrees(),
                            points.get(j).decDegrees(),
                            points.get(i).raDegrees(),
                            points.get(i).decDegrees()));
                }
            }
            levels.add(index);
        }
    }

    /** Whether one source level covers a position, by its number. */
    boolean coveredBy(SkyPosition position, int sourceLevel) {
        return covers(levels.get(sourceLevel - 1), position);
    }

    /**
     * The <strong>highest source level containing this position</strong>,
     * or 0 for sky no outline reaches. Not a count of levels: a point
     * inside level 5 answers 5 whether or not the fainter levels
     * happen to enclose it too.
     */
    int levelAt(SkyPosition position) {
        int covered = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (covers(levels.get(i), position)) {
                covered = i + 1;
            }
        }
        return covered;
    }

    /**
     * Even-odd containment for one level: a ray cast in right
     * ascension, counting the edges it crosses.
     *
     * <p>Each edge's right ascensions are brought next to the tested
     * position before the crossing is computed, so an edge that steps
     * over RA 0 is measured as the short step it is rather than as a
     * leap across the sky. Consecutive outline points are a fraction
     * of a degree apart, so this is exact for this source.
     */
    private static boolean covers(LevelIndex level, SkyPosition position) {
        double ra = position.raDegrees();
        double dec = position.decDegrees();
        boolean inside = false;
        for (Edge edge : level.at(dec)) {
            if ((edge.dec1() > dec) == (edge.dec2() > dec)) {
                continue;
            }
            double x1 = near(edge.ra1(), ra);
            double x2 = near(edge.ra2(), ra);
            double crossing = x1 + (dec - edge.dec1())
                    * (x2 - x1) / (edge.dec2() - edge.dec1());
            if (ra < crossing) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** A right ascension expressed within half a sky of a reference. */
    private static double near(double ra, double reference) {
        double shifted = ra;
        while (shifted - reference > 180.0) {
            shifted -= 360.0;
        }
        while (reference - shifted > 180.0) {
            shifted += 360.0;
        }
        return shifted;
    }
}
