package juranometria.solar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

import juranometria.catalog.PackIntegrityException;
import juranometria.catalog.Sha256;
import juranometria.solar.spk.SpkKernel;

/**
 * The bundled Jovian ephemeris pack (issue #473, layout A as ruled on
 * #472): two JUranometria-modified excerpts of JUP365 - Jupiter and its
 * barycentre over 1900–2100, the four Galilean moons over 2000–2100 -
 * and the planetary constants the contract names, copied verbatim from
 * NAIF's {@code pck00011.tpc} into the manifest. Every file is held to
 * the manifest's SHA-256 before use, as the Solar System pack is; a
 * missing or corrupt file refuses the whole pack, and the Sun and the
 * Moon, which read the Solar System pack alone, are unaffected.
 *
 * <p>The pack carries no time scales: the Jovian service reads them
 * from the Solar System pack, so one leap-second record governs the
 * whole sky.
 */
public final class JovianPack {

    static final String RESOURCE_ROOT = "/resources/jovian-system/";
    static final int SUPPORTED_FORMAT_VERSION = 1;
    static final String PACK_NAME = "jovian-system";

    /**
     * The planetary constants, as the PCK states them (kilometres and
     * degrees; the pole terms per Julian century from J2000).
     *
     * @param jupiterEquatorialRadiusKm the first of Jupiter's radii
     * @param jupiterPolarRadiusKm the third
     * @param poleRa0Degrees Jupiter's north pole, right ascension at J2000
     * @param poleRa1DegreesPerCentury its rate
     * @param poleDec0Degrees declination at J2000
     * @param poleDec1DegreesPerCentury its rate
     * @param sunRadiusKm the Sun's radius
     * @param moonEquatorialRadiiKm each moon's first radius, by NAIF id
     */
    public record Constants(double jupiterEquatorialRadiusKm,
                            double jupiterPolarRadiusKm,
                            double poleRa0Degrees, double poleRa1DegreesPerCentury,
                            double poleDec0Degrees, double poleDec1DegreesPerCentury,
                            double sunRadiusKm,
                            Map<Integer, Double> moonEquatorialRadiiKm) {
    }

    private final Map<String, String> manifest;
    private final SpkKernel jupiterKernel;
    private final SpkKernel moonsKernel;
    private final Constants constants;

    private JovianPack(Map<String, String> manifest, SpkKernel jupiterKernel,
                       SpkKernel moonsKernel, Constants constants) {
        this.manifest = manifest;
        this.jupiterKernel = jupiterKernel;
        this.moonsKernel = moonsKernel;
        this.constants = constants;
    }

    /** Loads the pack the application ships. */
    public static JovianPack load() {
        return load(name -> JovianPack.class.getResourceAsStream(
                RESOURCE_ROOT + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static JovianPack load(Function<String, InputStream> resources) {
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
            throw new PackIntegrityException("jovian-system pack format "
                    + format + " is not the supported "
                    + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!PACK_NAME.equals(name)) {
            throw new PackIntegrityException("expected pack " + PACK_NAME
                    + ", found " + name);
        }
        String jupiterName = required(manifest, "ephemeris.jupiter.kernel");
        String moonsName = required(manifest, "ephemeris.moons.kernel");
        try {
            SpkKernel jupiter = SpkKernel.read(
                    verified(resources, manifest, jupiterName));
            SpkKernel moons = SpkKernel.read(
                    verified(resources, manifest, moonsName));
            return new JovianPack(Collections.unmodifiableMap(manifest),
                    jupiter, moons, constants(manifest));
        } catch (IOException e) {
            throw new PackIntegrityException("the jovian-system pack cannot be"
                    + " read: " + e.getMessage(), e);
        }
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** Jupiter (599) and its barycentre (5), 1900–2100. */
    public SpkKernel jupiterKernel() {
        return jupiterKernel;
    }

    /** Io, Europa, Ganymede and Callisto about the barycentre, 2000–2100. */
    public SpkKernel moonsKernel() {
        return moonsKernel;
    }

    /** The PCK constants, as the manifest carries them. */
    public Constants constants() {
        return constants;
    }

    /** The first civil day Jupiter answers for, inclusive. */
    public LocalDate jupiterFirstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.jupiter.first.day"));
    }

    /** The last civil day Jupiter answers for, inclusive. */
    public LocalDate jupiterLastDay() {
        return LocalDate.parse(required(manifest, "ephemeris.jupiter.last.day"));
    }

    /** The first civil day the moons answer for, inclusive. */
    public LocalDate moonsFirstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.moons.first.day"));
    }

    /** The last civil day the moons answer for, inclusive. */
    public LocalDate moonsLastDay() {
        return LocalDate.parse(required(manifest, "ephemeris.moons.last.day"));
    }

    private static Constants constants(Map<String, String> manifest) {
        double[] jupiter = values(manifest, "pck.BODY599_RADII", 3);
        double[] poleRa = values(manifest, "pck.BODY599_POLE_RA", 3);
        double[] poleDec = values(manifest, "pck.BODY599_POLE_DEC", 3);
        double[] sun = values(manifest, "pck.BODY10_RADII", 3);
        Map<Integer, Double> moons = new TreeMap<>();
        for (int id : new int[] {501, 502, 503, 504}) {
            moons.put(id, values(manifest, "pck.BODY" + id + "_RADII", 3)[0]);
        }
        return new Constants(jupiter[0], jupiter[2], poleRa[0], poleRa[1],
                poleDec[0], poleDec[1], sun[0], Collections.unmodifiableMap(moons));
    }

    private static double[] values(Map<String, String> manifest, String key,
                                   int count) {
        String[] fields = required(manifest, key).split(",");
        if (fields.length != count) {
            throw new PackIntegrityException("jovian-system manifest " + key
                    + " carries " + fields.length + " values, not " + count);
        }
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = Double.parseDouble(fields[i].strip());
        }
        return out;
    }

    private static byte[] verified(Function<String, InputStream> resources,
                                   Map<String, String> manifest, String name)
            throws IOException {
        byte[] bytes;
        try (InputStream in = require(resources, name)) {
            bytes = in.readAllBytes();
        }
        String expected = required(manifest, "checksum." + name);
        String actual = Sha256.hex(bytes);
        if (!expected.equals(actual)) {
            throw new PackIntegrityException(String.format(
                    "%s fails its checksum: expected %s, found %s - the pack is"
                            + " corrupt or stale; regenerate with"
                            + " make import-jovian-system", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources,
                                       String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("jovian-system pack resource "
                    + name + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("jovian-system manifest lacks " + key);
        }
        return value;
    }
}
