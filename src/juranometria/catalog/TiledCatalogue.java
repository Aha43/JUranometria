package juranometria.catalog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

/**
 * The bundled tiled catalogue behind the {@link Catalogue} interface. The
 * manifest is parsed and validated at load; a bounded query selects only
 * the tiles intersecting the region, reads each selected tile once
 * (verifying its SHA-256 against the manifest before first use), caches
 * it, and filters rows by the true region. Results are immutable and
 * deterministic, and home-tile uniqueness makes duplicates impossible.
 *
 * A missing tile file, a checksum mismatch, or a malformed row is a clear
 * failure naming the resource — never a silently sparse sky.
 *
 * Loader mapping of the pack's explicitly-unknown values into the chart
 * model: an absent magnitude stays unknown (NaN, with B used when V is
 * absent); absent dimensions become a nominal 1.0 arcminute display
 * minimum; an absent minor axis mirrors the major; an absent position
 * angle becomes 0. These are display decisions — the pack itself
 * preserves the facts.
 */
public final class TiledCatalogue implements Catalogue {

    private static final String DEFAULT_ROOT = "/resources/catalog/bright-sky/";
    private static final double NOMINAL_EXTENT_ARCMIN = 1.0;

    private final Function<String, InputStream> resources;
    private final PackManifest manifest;
    private final StarIdentities identities;
    private final Map<String, Tile> loadedTiles = new HashMap<>();

    private record Tile(List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
    }

    private TiledCatalogue(Function<String, InputStream> resources,
                           PackManifest manifest, StarIdentities identities) {
        this.resources = resources;
        this.manifest = manifest;
        this.identities = identities;
    }

    /** Loads the bundled bright-sky pack and star identities from the
     *  classpath; identities attach to their stars at tile load. */
    public static TiledCatalogue load() {
        return load(name -> TiledCatalogue.class.getResourceAsStream(DEFAULT_ROOT + name),
                StarIdentities.load());
    }

    /** Loads a pack through any resource source; for tests and futures. */
    static TiledCatalogue load(Function<String, InputStream> resources) {
        return load(resources, null);
    }

    /** Loads a pack with an identity layer (null = no identities). */
    static TiledCatalogue load(Function<String, InputStream> resources,
                               StarIdentities identities) {
        InputStream stream = resources.apply("manifest.properties");
        if (stream == null) {
            throw new PackIntegrityException("catalogue pack manifest is missing");
        }
        PackManifest manifest = PackManifest.parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8), "bright-sky");
        return new TiledCatalogue(resources, manifest, identities);
    }

    public PackManifest manifest() {
        return manifest;
    }

    @Override
    public List<Star> starsIn(SkyRegion region) {
        List<Star> stars = new ArrayList<>();
        for (String tileId : SkyTiling.tilesIntersecting(region)) {
            for (Star star : tile(tileId).stars()) {
                if (region.contains(star.position())) {
                    stars.add(star);
                }
            }
        }
        return List.copyOf(stars);
    }

    @Override
    public List<DeepSkyObject> deepSkyObjectsIn(SkyRegion region) {
        List<DeepSkyObject> objects = new ArrayList<>();
        for (String tileId : SkyTiling.tilesIntersecting(region)) {
            for (DeepSkyObject dso : tile(tileId).deepSkyObjects()) {
                if (region.contains(dso.position())) {
                    objects.add(dso);
                }
            }
        }
        return List.copyOf(objects);
    }

    /** The tile ids read so far; lets tests assert query locality. */
    java.util.Set<String> loadedTileIds() {
        return java.util.Set.copyOf(loadedTiles.keySet());
    }

    private Tile tile(String tileId) {
        Tile cached = loadedTiles.get(tileId);
        if (cached != null) {
            return cached;
        }
        List<Star> stars = new ArrayList<>();
        List<DeepSkyObject> dsos = new ArrayList<>();
        byte[] starBytes = verifiedTileFile(tileId, "stars.csv");
        if (starBytes != null) {
            for (String[] fields : records(starBytes, 4, tileId + "/stars.csv")) {
                stars.add(new Star(fields[0],
                        new SkyPosition(Double.parseDouble(fields[1]),
                                Double.parseDouble(fields[2])),
                        Double.parseDouble(fields[3]),
                        identities == null ? null
                                : identities.identityOf(fields[0])));
            }
        }
        byte[] dsoBytes = verifiedTileFile(tileId, "dsos.csv");
        if (dsoBytes != null) {
            for (String[] fields : records(dsoBytes, 11, tileId + "/dsos.csv")) {
                dsos.add(deepSkyObject(fields));
            }
        }
        Tile tile = new Tile(List.copyOf(stars), List.copyOf(dsos));
        loadedTiles.put(tileId, tile);
        return tile;
    }

    private static DeepSkyObject deepSkyObject(String[] fields) {
        double major = optional(fields[5]);
        double minor = optional(fields[6]);
        double pa = optional(fields[7]);
        double vmag = optional(fields[8]);
        double bmag = optional(fields[9]);
        // What the source actually said, before any substitution
        // (issue #169): the display values below keep the renderer
        // working, and these keep the application honest.
        Double recordedMajor = Double.isNaN(major) || major <= 0
                ? null : major;
        Double recordedMinor = Double.isNaN(minor) || minor <= 0
                || minor > major ? null : minor;
        Double recordedPa = Double.isNaN(pa) ? null : pa;
        DeepSkyObject.Recorded.Band band =
                !Double.isNaN(vmag) ? DeepSkyObject.Recorded.Band.VISUAL
                        : !Double.isNaN(bmag)
                                ? DeepSkyObject.Recorded.Band.BLUE
                                : DeepSkyObject.Recorded.Band.NONE;

        if (Double.isNaN(major) || major <= 0) {
            major = NOMINAL_EXTENT_ARCMIN;
        }
        if (Double.isNaN(minor) || minor <= 0 || minor > major) {
            minor = major;
        }
        if (Double.isNaN(pa)) {
            pa = 0.0;
        }
        double magnitude = Double.isNaN(vmag) ? bmag : vmag;
        return new DeepSkyObject(
                fields[0],
                fields[1].isEmpty() ? List.of() : List.of(fields[1].split("\\|")),
                DsoType.fromOpenNgcToken(fields[2]),
                new SkyPosition(Double.parseDouble(fields[3]), Double.parseDouble(fields[4])),
                major, minor, pa, magnitude,
                Integer.parseInt(fields[10]),
                new DeepSkyObject.Recorded(recordedMajor, recordedMinor,
                        recordedPa, band));
    }

    private static double optional(String field) {
        return field.isEmpty() ? Double.NaN : Double.parseDouble(field);
    }

    /** Null when the manifest lists no such file (a genuinely empty layer). */
    private byte[] verifiedTileFile(String tileId, String fileName) {
        String key = "checksum.tiles/" + tileId + "/" + fileName;
        String expected = manifest.entries().get(key);
        if (expected == null) {
            return null;
        }
        String resource = "tiles/" + tileId + "/" + fileName;
        InputStream stream = resources.apply(resource);
        if (stream == null) {
            throw new PackIntegrityException(
                    "catalogue tile listed in the manifest is missing: " + resource);
        }
        byte[] bytes;
        try (stream) {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new PackIntegrityException("failed to read catalogue tile " + resource, e);
        }
        String actual = Sha256.hex(bytes);
        if (!actual.equals(expected)) {
            throw new PackIntegrityException("catalogue tile " + resource
                    + " does not match its manifest checksum"
                    + "\n  expected " + expected + "\n  actual   " + actual);
        }
        return bytes;
    }

    private static List<String[]> records(byte[] bytes, int fieldCount, String name) {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != fieldCount) {
                    throw new PackIntegrityException(
                            "malformed catalogue line in " + name + ": " + line);
                }
                records.add(fields);
            }
        } catch (IOException e) {
            throw new PackIntegrityException("failed to read catalogue tile " + name, e);
        }
        return records;
    }

}
