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
 * The bundled Mars System pack (issue #602, as ruled on #601, M-7a with
 * the exclusive boundary): one JUranometria-modified excerpt of NAIF's
 * MAR099 - Phobos and Deimos about the Mars barycentre, from 2000 up to,
 * not including, 2050-01-01 00:00 TDB - and the moons' radii copied
 * verbatim from NAIF's {@code pck00011.tpc}. Mars itself is the planets
 * pack's. Every file is held to the manifest's SHA-256 before use; a
 * missing or corrupt file refuses the whole pack, and every other body,
 * reading its own pack, is unaffected.
 */
public final class MarsSystemPack {

    static final int SUPPORTED_FORMAT_VERSION = 1;

    /** Where the pack lives on the classpath; its last segment is the pack's name. */
    static String root() {
        return "/resources/mars-system/";
    }

    private final Map<String, String> manifest;
    private final SpkKernel moonsKernel;
    private final List<Integer> moons;
    private final Map<Integer, Double> moonRadiiKm;

    private MarsSystemPack(Map<String, String> manifest, SpkKernel moonsKernel,
                           List<Integer> moons, Map<Integer, Double> moonRadiiKm) {
        this.manifest = manifest;
        this.moonsKernel = moonsKernel;
        this.moons = moons;
        this.moonRadiiKm = moonRadiiKm;
    }

    /** Loads the pack the application ships. */
    public static MarsSystemPack load() {
        return load(name -> MarsSystemPack.class.getResourceAsStream(root() + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static MarsSystemPack load(Function<String, InputStream> resources) {
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
            throw new PackIntegrityException("mars-system pack format " + format
                    + " is not the supported " + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!root().equals("/resources/" + name + "/")) {
            throw new PackIntegrityException("expected the mars-system pack, found " + name);
        }
        try {
            SpkKernel kernel = SpkKernel.read(verified(resources, manifest,
                    required(manifest, "ephemeris.moons.kernel")));
            List<Integer> moons = new java.util.ArrayList<>();
            Map<Integer, Double> radii = new TreeMap<>();
            for (String id : required(manifest, "ephemeris.moons.bodies").split(",")) {
                int body = Integer.parseInt(id.strip());
                moons.add(body);
                radii.put(body, values(manifest, "pck.BODY" + body + "_RADII", 3)[0]);
            }
            return new MarsSystemPack(Collections.unmodifiableMap(manifest), kernel,
                    List.copyOf(moons), Collections.unmodifiableMap(radii));
        } catch (IOException e) {
            throw new PackIntegrityException("the mars-system pack cannot be read: "
                    + e.getMessage(), e);
        }
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** Phobos (401) and Deimos (402) about the Mars barycentre (4). */
    public SpkKernel moonsKernel() {
        return moonsKernel;
    }

    /** The ruled moons, outward from Mars. */
    public List<Integer> moons() {
        return moons;
    }

    /** Each moon's first (largest) PCK radius, by NAIF id. */
    public Map<Integer, Double> moonRadiiKm() {
        return moonRadiiKm;
    }

    /** The first civil day the moons answer for, inclusive. */
    public LocalDate moonsFirstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.moons.first.day"));
    }

    /** The moons' last year as the reader is told it (2049): the boundary itself is exclusive. */
    public int moonsLastYear() {
        return Integer.parseInt(required(manifest, "ephemeris.moons.last.year").strip());
    }

    /**
     * The moons' exclusive end as a TDB Julian date: an instant whose TDB
     * reading is at or after it is refused (#601, M-7a amendment).
     */
    public double moonsEndJdTdb() {
        return values(manifest, "ephemeris.moons.coverage.end.jd.exclusive", 1)[0];
    }

    private static double[] values(Map<String, String> manifest, String key, int count) {
        String[] fields = required(manifest, key).split(",");
        if (fields.length != count) {
            throw new PackIntegrityException("mars-system manifest " + key + " carries "
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
                            + " regenerate with make import-mars-system", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources, String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("mars-system pack resource " + name + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("mars-system manifest lacks " + key);
        }
        return value;
    }
}
