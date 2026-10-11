package juranometria.solar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * The bundled planets ephemeris pack (Sprint 47, issue #501; layout C
 * as ruled on #500): one logical pack - one manifest, one notice, one
 * provenance - with one JUranometria-modified DE440 kernel file per
 * planet. It holds Venus first; later releases add a planet's file and
 * its manifest keys, and never rewrite a released planet's file, whose
 * name and digest stand for good.
 *
 * <p>Every file is held to the manifest's SHA-256 before use, as the
 * Solar System and Jovian packs are; a missing or corrupt file refuses
 * the whole pack, and the Sun, the Moon and Jupiter, which read their
 * own packs, are unaffected. The pack carries no time scales and no
 * Sun or Earth: a planet's place is measured from the Solar System
 * pack's, so one ephemeris and one leap-second record govern the sky.
 */
public final class PlanetsPack {

    static final int SUPPORTED_FORMAT_VERSION = 1;

    /** Where the pack lives on the classpath; its last segment is the pack's name. */
    static String root() {
        return "/resources/planets/";
    }

    /**
     * One planet's file and the facts the manifest states for it.
     *
     * @param name the planet's key, lower case ({@code venus})
     * @param kernel its kernel, every segment of its file
     * @param bodies the NAIF ids the file carries, barycentre first
     * @param firstDay the first civil day it answers for, inclusive
     * @param lastDay the last, inclusive
     * @param radiusKm its mean radius, as the PCK states it
     */
    public record Planet(String name, SpkKernel kernel, List<Integer> bodies,
                         LocalDate firstDay, LocalDate lastDay, double radiusKm) {
    }

    private final Map<String, String> manifest;
    private final Map<String, Planet> planets;
    private final double sunRadiusKm;

    private PlanetsPack(Map<String, String> manifest, Map<String, Planet> planets,
                        double sunRadiusKm) {
        this.manifest = manifest;
        this.planets = planets;
        this.sunRadiusKm = sunRadiusKm;
    }

    /** Loads the pack the application ships. */
    public static PlanetsPack load() {
        return load(name -> PlanetsPack.class.getResourceAsStream(
                root() + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static PlanetsPack load(Function<String, InputStream> resources) {
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
            throw new PackIntegrityException("planets pack format " + format
                    + " is not the supported " + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!root().equals("/resources/" + name + "/")) {
            throw new PackIntegrityException("expected the planets pack, found " + name);
        }
        Map<String, Planet> planets = new LinkedHashMap<>();
        try {
            for (String planet : required(manifest, "pack.planets").split(",")) {
                String key = planet.strip();
                SpkKernel kernel = SpkKernel.read(verified(resources, manifest,
                        required(manifest, String.format(Locale.ROOT, "ephemeris.%s.kernel", key))));
                List<Integer> bodies = new ArrayList<>();
                for (String id : required(manifest,
                        String.format(Locale.ROOT, "ephemeris.%s.bodies", key)).split(",")) {
                    bodies.add(Integer.parseInt(id.strip()));
                }
                int centre = bodies.get(bodies.size() - 1);
                planets.put(key, new Planet(key, kernel,
                        Collections.unmodifiableList(bodies),
                        LocalDate.parse(required(manifest,
                                String.format(Locale.ROOT, "ephemeris.%s.first.day", key))),
                        LocalDate.parse(required(manifest,
                                String.format(Locale.ROOT, "ephemeris.%s.last.day", key))),
                        values(manifest, String.format(Locale.ROOT, "pck.BODY%d_RADII", centre), 3)[0]));
            }
        } catch (IOException e) {
            throw new PackIntegrityException("the planets pack cannot be read: "
                    + e.getMessage(), e);
        }
        return new PlanetsPack(Collections.unmodifiableMap(manifest),
                Collections.unmodifiableMap(planets),
                values(manifest, "pck.BODY10_RADII", 3)[0]);
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** The planets the pack holds, in the order they were added. */
    public List<String> planets() {
        return List.copyOf(planets.keySet());
    }

    /** One planet, or a refusal naming the planets the pack holds. */
    public Planet planet(String name) {
        Planet planet = planets.get(name);
        if (planet == null) {
            throw new PackIntegrityException("the planets pack holds "
                    + planets.keySet() + ", not " + name);
        }
        return planet;
    }

    /** The Sun's radius, as the PCK states it: the in-front and behind states. */
    public double sunRadiusKm() {
        return sunRadiusKm;
    }

    private static double[] values(Map<String, String> manifest, String key,
                                   int count) {
        String[] fields = required(manifest, key).split(",");
        if (fields.length != count) {
            throw new PackIntegrityException("planets manifest " + key
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
        String expected = required(manifest, String.format(Locale.ROOT, "checksum.%s", name));
        String actual = Sha256.hex(bytes);
        if (!expected.equals(actual)) {
            throw new PackIntegrityException(String.format(
                    "%s fails its checksum: expected %s, found %s - the pack is"
                            + " corrupt or stale; regenerate with"
                            + " make import-planets", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources,
                                       String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("planets pack resource " + name
                    + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("planets manifest lacks " + key);
        }
        return value;
    }
}
