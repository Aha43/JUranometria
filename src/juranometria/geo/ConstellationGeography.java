package juranometria.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

/**
 * The bundled constellation geography: identities, traditional line
 * figures, and reconstructed IAU boundary polylines, loaded from the
 * generated pack resources (docs/decisions/constellation-geography.md)
 * with every file verified against its manifest SHA-256 before use.
 *
 * Loading and querying are pure data operations - no Swing, no
 * painting. Queries return every segment whose great-circle arc
 * intersects the requested region, including segments with neither
 * endpoint inside it, and behave correctly across the RA wrap and at
 * both poles because the geometry is evaluated in vector space.
 */
public final class ConstellationGeography {

    static final String RESOURCE_ROOT = "/resources/geo/constellations/";
    static final int SUPPORTED_FORMAT_VERSION = 1;
    static final String PACK_NAME = "constellation-geography";

    private final List<Constellation> constellations;
    private final List<GeoSegment> figureSegments;
    private final List<GeoSegment> boundarySegments;

    private ConstellationGeography(List<Constellation> constellations,
                                   List<GeoSegment> figureSegments,
                                   List<GeoSegment> boundarySegments) {
        this.constellations = List.copyOf(constellations);
        this.figureSegments = List.copyOf(figureSegments);
        this.boundarySegments = List.copyOf(boundarySegments);
    }

    /** Loads the bundled pack from the classpath. */
    public static ConstellationGeography load() {
        return load(resource -> ConstellationGeography.class
                .getResourceAsStream(RESOURCE_ROOT + resource));
    }

    /** Loads through an injectable resource opener, for tests. */
    public static ConstellationGeography load(
            Function<String, InputStream> resources) {
        Properties manifest = new Properties();
        try (InputStream stream = require(resources, "manifest.properties")) {
            manifest.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "cannot read the constellation-geography manifest", e);
        }
        int version = Integer.parseInt(
                required(manifest, "format.version"));
        if (version != SUPPORTED_FORMAT_VERSION) {
            throw new IllegalStateException(
                    "unsupported constellation-geography format version "
                            + version + "; this build supports "
                            + SUPPORTED_FORMAT_VERSION);
        }
        String packName = required(manifest, "pack.name");
        if (!PACK_NAME.equals(packName)) {
            throw new IllegalStateException(
                    "unexpected geography pack name: " + packName);
        }

        Map<String, Constellation> byId = new HashMap<>();
        List<Constellation> constellations = new ArrayList<>();
        for (String[] row : rows(verified(resources, manifest,
                "constellations.csv"), 4)) {
            Constellation constellation = new Constellation(
                    row[0], row[1], row[2], Integer.parseInt(row[3]));
            if (byId.put(constellation.id(), constellation) != null) {
                throw new IllegalStateException(
                        "duplicate constellation id: " + constellation.id());
            }
            constellations.add(constellation);
        }
        List<GeoSegment> figures = segments(
                verified(resources, manifest, "figures.csv"), byId);
        List<GeoSegment> boundaries = segments(
                verified(resources, manifest, "boundaries.csv"), byId);

        check(constellations.size(), manifest, "rows.constellations");
        check(figures.size(), manifest, "rows.figure.segments");
        check(boundaries.size(), manifest, "rows.boundary.segments");
        return new ConstellationGeography(constellations, figures, boundaries);
    }

    public List<Constellation> constellations() {
        return constellations;
    }

    /** Every figure segment whose arc intersects the region. */
    public List<GeoSegment> figureSegmentsIn(SkyRegion region) {
        return intersecting(figureSegments, region);
    }

    /** Every boundary polyline piece whose arc intersects the region. */
    public List<GeoSegment> boundarySegmentsIn(SkyRegion region) {
        return intersecting(boundarySegments, region);
    }

    private static List<GeoSegment> intersecting(List<GeoSegment> segments,
                                                 SkyRegion region) {
        List<GeoSegment> found = new ArrayList<>();
        for (GeoSegment segment : segments) {
            if (segment.angularDistanceDegrees(region.centre())
                    <= region.radiusDegrees()) {
                found.add(segment);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------

    private static List<GeoSegment> segments(String content,
                                             Map<String, Constellation> byId) {
        List<GeoSegment> segments = new ArrayList<>();
        for (String[] row : parse(content, 5)) {
            if (!byId.containsKey(row[0])) {
                throw new IllegalStateException(
                        "segment references unknown constellation: " + row[0]);
            }
            segments.add(new GeoSegment(row[0],
                    new SkyPosition(Double.parseDouble(row[1]),
                            Double.parseDouble(row[2])),
                    new SkyPosition(Double.parseDouble(row[3]),
                            Double.parseDouble(row[4]))));
        }
        return segments;
    }

    private static List<String[]> rows(String content, int columns) {
        return parse(content, columns);
    }

    private static List<String[]> parse(String content, int columns) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalStateException("empty geography resource");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] row = line.split(",", -1);
                if (row.length != columns) {
                    throw new IllegalStateException(
                            "malformed geography row: " + line);
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }

    /** Reads a resource and verifies it against the manifest checksum. */
    private static String verified(Function<String, InputStream> resources,
                                   Properties manifest, String name) {
        byte[] bytes;
        try (InputStream stream = require(resources, name)) {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read geography " + name, e);
        }
        String expected = required(manifest, "checksum." + name);
        String actual = sha256Hex(bytes);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "geography resource %s fails its checksum: expected %s,"
                            + " found %s - the pack is corrupt or stale;"
                            + " regenerate with make import-constellations",
                    name, expected, actual));
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static InputStream require(Function<String, InputStream> resources,
                                       String name) {
        InputStream stream = resources.apply(name);
        if (stream == null) {
            throw new IllegalStateException(
                    "missing constellation-geography resource: " + name);
        }
        return stream;
    }

    private static String required(Properties manifest, String key) {
        String value = manifest.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "geography manifest is missing " + key);
        }
        return value;
    }

    private static void check(int actual, Properties manifest, String key) {
        int declared = Integer.parseInt(required(manifest, key));
        if (declared != actual) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "geography manifest declares %s=%d but %d were loaded",
                    key, declared, actual));
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(bytes)) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
