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
 * The bundled Saturn ephemeris pack (issue #575, Jupiter's layout as
 * ruled on #574): two JUranometria-modified excerpts of SAT441 - Saturn
 * and its barycentre over 1900–2100, Mimas, Enceladus, Tethys, Dione,
 * Rhea, Titan and Iapetus over 2000–2100 - the planetary constants the
 * contract names, copied verbatim from NAIF's {@code pck00011.tpc}, and
 * the ruled ring model's four radii, quoted from the PDS Ring-Moon
 * Systems Node's table. Every file is held to the manifest's SHA-256
 * before use; a missing or corrupt file refuses the whole pack, and the
 * Sun, the Moon, Jupiter, Mercury and Venus, which read their own packs,
 * are unaffected.
 *
 * <p>The pack carries no time scales: the Saturn service reads them
 * from the Solar System pack, so one leap-second record governs the
 * whole sky.
 */
public final class SaturnPack {

    static final String RESOURCE_ROOT = "/resources/saturn-system/";
    static final int SUPPORTED_FORMAT_VERSION = 1;
    static final String PACK_NAME = "saturn-system";

    /**
     * The planetary constants and ring radii, as the manifest carries them
     * (kilometres and degrees; the pole terms per Julian century from J2000).
     *
     * @param saturnEquatorialRadiusKm the first of Saturn's radii (1 bar)
     * @param saturnPolarRadiusKm the third
     * @param poleRa0Degrees Saturn's north pole, right ascension at J2000
     * @param poleRa1DegreesPerCentury its rate
     * @param poleDec0Degrees declination at J2000
     * @param poleDec1DegreesPerCentury its rate
     * @param sunRadiusKm the Sun's radius
     * @param moonRadiiKm each moon's first (largest) radius, by NAIF id
     * @param ringInnerRadiusKm the C Ring's inner boundary
     * @param ringBOuterRadiusKm the B Ring's outer boundary
     * @param ringCassiniOuterRadiusKm the Cassini Division's outer boundary
     * @param ringOuterRadiusKm the A Ring's outer boundary
     */
    public record Constants(double saturnEquatorialRadiusKm,
                            double saturnPolarRadiusKm,
                            double poleRa0Degrees, double poleRa1DegreesPerCentury,
                            double poleDec0Degrees, double poleDec1DegreesPerCentury,
                            double sunRadiusKm,
                            Map<Integer, Double> moonRadiiKm,
                            double ringInnerRadiusKm,
                            double ringBOuterRadiusKm,
                            double ringCassiniOuterRadiusKm,
                            double ringOuterRadiusKm) {
    }

    private final Map<String, String> manifest;
    private final SpkKernel saturnKernel;
    private final SpkKernel moonsKernel;
    private final Constants constants;

    private SaturnPack(Map<String, String> manifest, SpkKernel saturnKernel,
                       SpkKernel moonsKernel, Constants constants) {
        this.manifest = manifest;
        this.saturnKernel = saturnKernel;
        this.moonsKernel = moonsKernel;
        this.constants = constants;
    }

    /** Loads the pack the application ships. */
    public static SaturnPack load() {
        return load(name -> SaturnPack.class.getResourceAsStream(RESOURCE_ROOT + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static SaturnPack load(Function<String, InputStream> resources) {
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
            throw new PackIntegrityException("saturn-system pack format " + format
                    + " is not the supported " + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!PACK_NAME.equals(name)) {
            throw new PackIntegrityException("expected pack " + PACK_NAME + ", found " + name);
        }
        String saturnName = required(manifest, "ephemeris.saturn.kernel");
        String moonsName = required(manifest, "ephemeris.moons.kernel");
        try {
            SpkKernel saturn = SpkKernel.read(verified(resources, manifest, saturnName));
            SpkKernel moons = SpkKernel.read(verified(resources, manifest, moonsName));
            return new SaturnPack(Collections.unmodifiableMap(manifest), saturn, moons,
                    constants(manifest));
        } catch (IOException e) {
            throw new PackIntegrityException("the saturn-system pack cannot be read: "
                    + e.getMessage(), e);
        }
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** Saturn (699) and its barycentre (6), 1900–2100. */
    public SpkKernel saturnKernel() {
        return saturnKernel;
    }

    /** The seven moons about the barycentre, 2000–2100. */
    public SpkKernel moonsKernel() {
        return moonsKernel;
    }

    /** The PCK constants and ring radii, as the manifest carries them. */
    public Constants constants() {
        return constants;
    }

    /** The first civil day Saturn answers for, inclusive. */
    public LocalDate saturnFirstDay() {
        return LocalDate.parse(required(manifest, "ephemeris.saturn.first.day"));
    }

    /** The last civil day Saturn answers for, inclusive. */
    public LocalDate saturnLastDay() {
        return LocalDate.parse(required(manifest, "ephemeris.saturn.last.day"));
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
        double[] saturn = values(manifest, "pck.BODY699_RADII", 3);
        double[] poleRa = values(manifest, "pck.BODY699_POLE_RA", 3);
        double[] poleDec = values(manifest, "pck.BODY699_POLE_DEC", 3);
        double[] sun = values(manifest, "pck.BODY10_RADII", 3);
        Map<Integer, Double> moons = new TreeMap<>();
        for (String id : required(manifest, "ephemeris.moons.bodies").split(",")) {
            int body = Integer.parseInt(id.strip());
            moons.put(body, values(manifest, "pck.BODY" + body + "_RADII", 3)[0]);
        }
        return new Constants(saturn[0], saturn[2], poleRa[0], poleRa[1], poleDec[0], poleDec[1],
                sun[0], Collections.unmodifiableMap(moons),
                values(manifest, "rings.c-ring.inner.km", 1)[0],
                values(manifest, "rings.b-ring.outer.km", 1)[0],
                values(manifest, "rings.cassini-division.outer.km", 1)[0],
                values(manifest, "rings.a-ring.outer.km", 1)[0]);
    }

    private static double[] values(Map<String, String> manifest, String key, int count) {
        String[] fields = required(manifest, key).split(",");
        if (fields.length != count) {
            throw new PackIntegrityException("saturn-system manifest " + key + " carries "
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
        String expected = required(manifest, "checksum." + name);
        String actual = Sha256.hex(bytes);
        if (!expected.equals(actual)) {
            throw new PackIntegrityException(String.format(
                    "%s fails its checksum: expected %s, found %s - the pack is corrupt or stale;"
                            + " regenerate with make import-saturn-system", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources, String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("saturn-system pack resource " + name + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("saturn-system manifest lacks " + key);
        }
        return value;
    }
}
