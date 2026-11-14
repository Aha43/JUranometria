package juranometria.meteor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

import juranometria.catalog.PackIntegrityException;
import juranometria.catalog.Sha256;
import juranometria.chart.SkyPosition;

/**
 * The bundled meteor-shower pack (#716): one named edition of the IMO
 * Meteor Shower Calendar's working list and radiant table for the
 * showers the atlas carries, with their IAU identities, read and
 * checked against its manifest.
 *
 * <p>Loading is a pure data operation: no Swing, no preferences, no
 * network, no clock. The edition and its date are the manifest's, so a
 * caller that names the edition quotes the pack (R-4).
 */
public final class MeteorShowerPack {

    static final String RESOURCE_ROOT = "/resources/meteor-showers/";
    static final String SUPPORTED_FORMAT_VERSION = "1";
    static final String PACK_NAME = "meteor-showers";
    public static final String SHOWERS = "showers.tsv";
    public static final String RADIANTS = "radiants.tsv";
    // One literal each: a header is one name for the language ledger, not fifteen.
    public static final String SHOWERS_HEADER = "code\tiau_number\tiau_name\tparent_body\tactivity_start\tactivity_end\tstart_solar_longitude\tend_solar_longitude\tmaximum_date\tmaximum_solar_longitude\tra\tdec\tv_infinity\tpopulation_index\tzhr";
    public static final String RADIANTS_HEADER = "code\tmonth\tday\tsolar_longitude\tra\tdec";
    public static final List<String> REQUIRED_MANIFEST_KEYS = List.of("format.version",
            "pack.name", "edition", "edition.year", "edition.correctAsOf", "credit",
            "identity.source", "identity.lastUpdate", "showers", "radiant.rows",
            "checksum." + SHOWERS, "checksum." + RADIANTS);

    private final Map<String, String> manifest;
    private final List<MeteorShower> showers;

    private MeteorShowerPack(Map<String, String> manifest, List<MeteorShower> showers) {
        this.manifest = manifest;
        this.showers = showers;
    }

    /** Loads the pack the application ships. */
    public static MeteorShowerPack load() {
        return load(name -> MeteorShowerPack.class.getResourceAsStream(RESOURCE_ROOT + name));
    }

    /** Loads a pack through an opener, as a test or the importer does. */
    public static MeteorShowerPack load(Function<String, InputStream> resources) {
        Properties properties = new Properties();
        try (InputStream in = require(resources, "manifest.properties")) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        validateManifest(properties);
        Map<String, String> manifest = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            manifest.put(key, properties.getProperty(key));
        }
        Map<String, List<MeteorShower.Row>> rows = new LinkedHashMap<>();
        List<String> radiantLines = lines(resources, manifest, RADIANTS, RADIANTS_HEADER);
        for (String line : radiantLines) {
            String[] f = line.split("\t", -1);
            if (f.length != 6) {
                throw new PackIntegrityException("a radiant row has six fields: " + line);
            }
            rows.computeIfAbsent(f[0], k -> new ArrayList<>()).add(new MeteorShower.Row(
                    Integer.parseInt(f[1]), Integer.parseInt(f[2]), Double.parseDouble(f[3]),
                    new SkyPosition(Double.parseDouble(f[4]), Double.parseDouble(f[5]))));
        }
        List<MeteorShower> showers = new ArrayList<>();
        for (String line : lines(resources, manifest, SHOWERS, SHOWERS_HEADER)) {
            String[] f = line.split("\t", -1);
            if (f.length != 15) {
                throw new PackIntegrityException("a shower row has fifteen fields: " + line);
            }
            try {
                showers.add(new MeteorShower(f[0], Integer.parseInt(f[1]), f[2], f[3], f[4],
                        f[5], Double.parseDouble(f[6]), Double.parseDouble(f[7]), f[8], f[9],
                        new SkyPosition(Double.parseDouble(f[10]), Double.parseDouble(f[11])),
                        Double.parseDouble(f[12]), f[13], f[14],
                        rows.getOrDefault(f[0], List.of())));
            } catch (IllegalArgumentException e) {
                throw new PackIntegrityException("a shower row cannot be read: " + line, e);
            }
        }
        requireCount(manifest, "showers", showers.size());
        requireCount(manifest, "radiant.rows", radiantLines.size());
        for (String code : rows.keySet()) {
            if (showers.stream().noneMatch(s -> s.code().equals(code))) {
                throw new PackIntegrityException("radiant rows for a shower the pack"
                        + " does not list: " + code);
            }
        }
        return new MeteorShowerPack(Collections.unmodifiableMap(manifest),
                List.copyOf(showers));
    }

    /** The one contract the importer and the loader hold a manifest to. */
    public static void validateManifest(Properties manifest) {
        for (String key : REQUIRED_MANIFEST_KEYS) {
            String value = manifest.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new PackIntegrityException("meteor-shower manifest lacks " + key);
            }
        }
        if (!SUPPORTED_FORMAT_VERSION.equals(manifest.getProperty("format.version"))) {
            throw new PackIntegrityException("meteor-shower pack format "
                    + manifest.getProperty("format.version") + " is not the supported "
                    + SUPPORTED_FORMAT_VERSION);
        }
        if (!PACK_NAME.equals(manifest.getProperty("pack.name"))) {
            throw new PackIntegrityException("expected pack " + PACK_NAME + ", found "
                    + manifest.getProperty("pack.name"));
        }
    }

    /** The manifest, as the importer wrote it. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** The showers, in the order of the year by their nominal maximum. */
    public List<MeteorShower> showers() {
        return showers;
    }

    /** The edition the numbers are from, for example "IMO Meteor Shower Calendar 2027". */
    public String edition() {
        return manifest.get("edition");
    }

    /** When the edition says its working list was correct, as the edition words it. */
    public String correctAsOf() {
        return manifest.get("edition.correctAsOf");
    }

    /** The credit line the source asks for. */
    public String credit() {
        return manifest.get("credit");
    }

    private static void requireCount(Map<String, String> manifest, String key, int found) {
        if (Integer.parseInt(manifest.get(key)) != found) {
            throw new PackIntegrityException("the meteor-shower manifest states " + key + "="
                    + manifest.get(key) + " and the pack holds " + found);
        }
    }

    private static List<String> lines(Function<String, InputStream> resources,
                                      Map<String, String> manifest, String name,
                                      String header) {
        byte[] bytes;
        try (InputStream in = require(resources, name)) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new PackIntegrityException("the meteor-shower pack cannot be read: "
                    + e.getMessage(), e);
        }
        String expected = manifest.get("checksum." + name);
        String actual = Sha256.hex(bytes);
        if (!expected.equals(actual)) {
            throw new PackIntegrityException(String.format("%s fails its checksum: expected"
                    + " %s, found %s - the pack is corrupt or stale; regenerate with"
                    + " make import-meteor-showers", name, expected, actual));
        }
        List<String> lines = new ArrayList<>(
                new String(bytes, StandardCharsets.UTF_8).lines().toList());
        if (lines.isEmpty() || !header.equals(lines.get(0))) {
            throw new PackIntegrityException(name + " does not start with its header");
        }
        lines.remove(0);
        return lines;
    }

    private static InputStream require(Function<String, InputStream> resources, String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("meteor-shower pack resource " + name
                    + " is missing");
        }
        return in;
    }
}
