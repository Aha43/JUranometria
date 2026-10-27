package juranometria.solar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

import juranometria.catalog.PackIntegrityException;
import juranometria.catalog.Sha256;
import juranometria.solar.spk.SpkKernel;

/**
 * The bundled Uranus System pack (issue #634, as ruled on #633): two
 * JUranometria-modified excerpts of NAIF's {@code ura184_part-3.bsp} - the
 * Uranus barycentre from DE442 (1900-2100), which stands for the planet
 * (U-1a, U-2), and Miranda, Ariel, Umbriel, Titania and Oberon about it from
 * URA182.21 (2000-2100, U-6a) - and Uranus's radii and pole and the moons'
 * radii copied verbatim from NAIF's {@code pck00011.tpc}. Every file is held
 * to the manifest's SHA-256 before use; a missing or corrupt file refuses the
 * whole pack, and every other body, reading its own pack, is unaffected.
 */
public final class UranusSystemPack {

    static final int SUPPORTED_FORMAT_VERSION = 1;

    /** Where the pack lives on the classpath; its last segment is the pack's name. */
    static String root() {
        return "/resources/uranus-system/";
    }

    private final Map<String, String> manifest;
    private final SpkKernel uranusKernel;
    private final SpkKernel moonsKernel;
    private final List<Integer> moons;
    private final Map<Integer, Double> moonRadiiKm;

    private UranusSystemPack(Map<String, String> manifest, SpkKernel uranusKernel,
                             SpkKernel moonsKernel, List<Integer> moons,
                             Map<Integer, Double> moonRadiiKm) {
        this.manifest = manifest;
        this.uranusKernel = uranusKernel;
        this.moonsKernel = moonsKernel;
        this.moons = moons;
        this.moonRadiiKm = moonRadiiKm;
    }

    /** Loads the pack the application ships. */
    public static UranusSystemPack load() {
        return load(name -> UranusSystemPack.class.getResourceAsStream(root() + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static UranusSystemPack load(Function<String, InputStream> resources) {
        Properties properties = new Properties();
        try (InputStream in = require(resources, "manifest.properties")) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> manifest = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            manifest.put(key, properties.getProperty(key));
        }
        int format = Integer.parseInt(required(manifest, "format.version"));
        if (format != SUPPORTED_FORMAT_VERSION) {
            throw new PackIntegrityException("uranus-system pack format " + format
                    + " is not the supported " + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!root().equals("/resources/" + name + "/")) {
            throw new PackIntegrityException("expected the uranus-system pack, found " + name);
        }
        try {
            SpkKernel uranus = SpkKernel.read(verified(resources, manifest,
                    required(manifest, "ephemeris.uranus.kernel")));
            SpkKernel moonsKernel = SpkKernel.read(verified(resources, manifest,
                    required(manifest, "ephemeris.moons.kernel")));
            List<Integer> moons = new java.util.ArrayList<>();
            Map<Integer, Double> radii = new TreeMap<>();
            for (String id : required(manifest, "ephemeris.moons.bodies").split(",")) {
                int body = Integer.parseInt(id.strip());
                moons.add(body);
                radii.put(body, values(manifest, "pck.BODY" + body + "_RADII", 3)[0]);
            }
            return new UranusSystemPack(Collections.unmodifiableMap(manifest), uranus,
                    moonsKernel, List.copyOf(moons), Collections.unmodifiableMap(radii));
        } catch (IOException e) {
            throw new PackIntegrityException("the uranus-system pack cannot be read: "
                    + e.getMessage(), e);
        }
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** The Uranus barycentre (7) about the solar-system barycentre (0), DE442's. */
    public SpkKernel uranusKernel() {
        return uranusKernel;
    }

    /** The five moons about the Uranus barycentre (7). */
    public SpkKernel moonsKernel() {
        return moonsKernel;
    }

    /** The ruled moons, outward from Uranus. */
    public List<Integer> moons() {
        return moons;
    }

    /** Each moon's first (largest) PCK radius, by NAIF id. */
    public Map<Integer, Double> moonRadiiKm() {
        return moonRadiiKm;
    }

    /** A PCK value the manifest carries, verbatim, by its keyword: three values for a radius. */
    public double[] constant(String keyword) {
        String key = String.format(Locale.ROOT, "pck.%s", keyword);
        String value = required(manifest, key);
        return values(manifest, key, value.split(",").length);
    }

    public LocalDate firstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.uranus.first.day"));
    }

    public LocalDate lastDay() {
        return LocalDate.parse(required(manifest, "ephemeris.uranus.last.day"));
    }

    public LocalDate moonsFirstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.moons.first.day"));
    }

    public LocalDate moonsLastDay() {
        return LocalDate.parse(required(manifest, "ephemeris.moons.last.day"));
    }

    private static double[] values(Map<String, String> manifest, String key, int count) {
        String[] fields = required(manifest, key).split(",");
        if (fields.length != count) {
            throw new PackIntegrityException("uranus-system manifest " + key + " carries "
                    + fields.length + " values, not " + count);
        }
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = Double.parseDouble(fields[i].strip());
        }
        return out;
    }

    private static byte[] verified(Function<String, InputStream> resources,
                                   Map<String, String> manifest, String name) throws IOException {
        byte[] bytes;
        try (InputStream in = require(resources, name)) {
            bytes = in.readAllBytes();
        }
        String expected = required(manifest, String.format(Locale.ROOT, "checksum.%s", name));
        String actual = Sha256.hex(bytes);
        if (!expected.equals(actual)) {
            throw new PackIntegrityException(String.format(
                    "%s fails its checksum: expected %s, found %s - the pack is corrupt or stale;"
                            + " regenerate with make import-uranus-system", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources, String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("uranus-system pack resource " + name + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("uranus-system manifest lacks " + key);
        }
        return value;
    }
}
