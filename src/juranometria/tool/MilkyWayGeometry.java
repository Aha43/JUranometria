package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import juranometria.chart.SkyPosition;

/**
 * The candidate Milky Way outlines, read and given a topology (Sprint
 * 22, issue #189).
 *
 * <p>This is study code. It reads the candidate source from the
 * gitignored import directory, states what it finds, and hands the
 * study rings it can project through the production seams. Nothing
 * here is bundled and nothing in production reads it.
 *
 * <p><strong>Winding is not used to decide anything.</strong> The
 * source mixes winding directions - level 2 carries 103 clockwise
 * rings and 10 counter-clockwise ones, and rings sit inside rings in
 * both directions - so the GeoJSON convention that outer rings run
 * counter-clockwise and holes clockwise does not hold here. Filling
 * is decided by <em>containment depth</em> instead: a ring at even
 * depth encloses ink, a ring at odd depth cuts a hole in it. That is
 * the even-odd rule, applied explicitly rather than inferred.
 */
final class MilkyWayGeometry {

    private MilkyWayGeometry() {
    }

    /** The pinned candidate: d3-celestial's distributed outline file. */
    static final String SOURCE = "mw.json";
    static final String SHA256 =
            "aee221a7a0e879418e685de00c3e68fbdfac5667c0a8aab74929ef9cf4aab4fb";

    /** Where the study looks; gitignored, fetched by a script. */
    static final Path RAW = Path.of("imports/raw/milky-way");

    /**
     * One closed ring of the outline, in sky coordinates.
     *
     * <p>{@code depth} is how many rings of the same level contain
     * it: 0 is an outermost region, 1 a hole inside it, 2 an island
     * inside that hole, and so on. {@code seam} records that the ring
     * crosses the RA 0/360 discontinuity, which every consumer has to
     * handle before projecting it.
     */
    record Ring(List<SkyPosition> points, int depth, boolean seam,
                double spanRaDegrees, double minDec, double maxDec) {

        boolean fills() {
            return depth % 2 == 0;
        }
    }

    /** One brightness level: the source's own `ol1`..`ol5`. */
    record Level(String id, int index, List<Ring> rings) {

        long filled() {
            return rings.stream().filter(Ring::fills).count();
        }

        long holes() {
            return rings.size() - filled();
        }

        int points() {
            return rings.stream().mapToInt(r -> r.points().size()).sum();
        }
    }

    /** Reads the candidate and gives every ring its containment depth. */
    static List<Level> load() throws IOException {
        Path file = RAW.resolve(SOURCE);
        if (!Files.exists(file)) {
            throw new IOException("missing " + file + " - run"
                    + " scripts/download-milky-way-sources.sh");
        }
        String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
        if (!SHA256.equals(actual)) {
            throw new IOException(SOURCE + " is not the pinned bytes:"
                    + " expected " + SHA256 + ", found " + actual);
        }
        Map<String, Object> root = MiniJson.object(MiniJson.parse(
                Files.readString(file, StandardCharsets.UTF_8)));
        List<Level> levels = new ArrayList<>();
        int index = 0;
        for (Object feature : MiniJson.array(root.get("features"))) {
            Map<String, Object> f = MiniJson.object(feature);
            String id = String.valueOf(f.get("id"));
            Map<String, Object> geometry =
                    MiniJson.object(f.get("geometry"));
            List<List<SkyPosition>> rings = new ArrayList<>();
            for (Object polygon
                    : MiniJson.array(geometry.get("coordinates"))) {
                for (Object ring : MiniJson.array(polygon)) {
                    rings.add(readRing(MiniJson.array(ring)));
                }
            }
            levels.add(new Level(id, ++index, withDepths(rings)));
        }
        return List.copyOf(levels);
    }

    private static List<SkyPosition> readRing(List<Object> coordinates) {
        List<SkyPosition> points = new ArrayList<>(coordinates.size());
        for (Object point : coordinates) {
            List<Object> pair = MiniJson.array(point);
            double lon = ((Number) pair.get(0)).doubleValue();
            double lat = ((Number) pair.get(1)).doubleValue();
            // The source carries longitude in [-180, 180]; the atlas
            // speaks right ascension in [0, 360).
            double ra = lon < 0.0 ? lon + 360.0 : lon;
            points.add(new SkyPosition(ra % 360.0, lat));
        }
        return List.copyOf(points);
    }

    /**
     * Gives every ring the depth of rings containing it, by testing a
     * point of each ring against every other. Quadratic in the number
     * of rings, which at 113 rings a level costs nothing and keeps
     * the rule visible.
     */
    private static List<Ring> withDepths(List<List<SkyPosition>> rings) {
        List<Ring> out = new ArrayList<>(rings.size());
        for (int i = 0; i < rings.size(); i++) {
            List<SkyPosition> ring = rings.get(i);
            int depth = 0;
            for (int j = 0; j < rings.size(); j++) {
                if (i != j && contains(rings.get(j), ring.get(0))) {
                    depth++;
                }
            }
            double minRa = 360.0;
            double maxRa = 0.0;
            double minDec = 90.0;
            double maxDec = -90.0;
            for (SkyPosition p : ring) {
                minRa = Math.min(minRa, p.raDegrees());
                maxRa = Math.max(maxRa, p.raDegrees());
                minDec = Math.min(minDec, p.decDegrees());
                maxDec = Math.max(maxDec, p.decDegrees());
            }
            double span = maxRa - minRa;
            // A ring whose right ascensions reach across almost the
            // whole sky is not enormous - it steps over RA 0.
            boolean seam = span > 300.0;
            out.add(new Ring(ring, depth, seam, span, minDec, maxDec));
        }
        return List.copyOf(out);
    }

    /**
     * Whether a ring encloses a point, by ray crossing in right
     * ascension and declination.
     *
     * <p>Rings that step over RA 0 are unwrapped first: their points
     * and the tested point are shifted into one continuous run, so
     * the crossing count is taken in a frame where the ring does not
     * jump 360 degrees in one edge.
     */
    static boolean contains(List<SkyPosition> ring, SkyPosition point) {
        double[] xs = unwrapped(ring);
        double px = point.raDegrees();
        // Bring the tested point into the same unwrapped frame.
        double first = xs[0];
        if (px - first > 180.0) {
            px -= 360.0;
        } else if (first - px > 180.0) {
            px += 360.0;
        }
        double py = point.decDegrees();
        boolean in = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            double xi = xs[i];
            double yi = ring.get(i).decDegrees();
            double xj = xs[j];
            double yj = ring.get(j).decDegrees();
            if ((yi > py) != (yj > py)
                    && px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /**
     * A ring's right ascensions made continuous: every step larger
     * than half the sky is read as crossing RA 0 rather than as a
     * leap across it.
     */
    static double[] unwrapped(List<SkyPosition> ring) {
        double[] xs = new double[ring.size()];
        xs[0] = ring.get(0).raDegrees();
        for (int i = 1; i < ring.size(); i++) {
            double previous = xs[i - 1];
            double raw = ring.get(i).raDegrees();
            double step = raw - wrapTo(previous);
            if (step > 180.0) {
                step -= 360.0;
            } else if (step < -180.0) {
                step += 360.0;
            }
            xs[i] = previous + step;
        }
        return xs;
    }

    private static double wrapTo(double ra) {
        double wrapped = ra % 360.0;
        return wrapped < 0.0 ? wrapped + 360.0 : wrapped;
    }
}
