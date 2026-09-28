package juranometria.solar;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

import juranometria.catalog.PackIntegrityException;
import juranometria.catalog.Sha256;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.time.LeapSeconds;

/**
 * The bundled Solar System ephemeris pack (Sprint 35, issue #399): a
 * JUranometria-modified excerpt of JPL DE440 and the IERS leap-second
 * file, loaded from the generated pack resources under
 * {@code /resources/solar-system/} with every file verified against
 * the manifest's SHA-256 before use, as the catalogue and geography
 * packs are.
 *
 * <p>Loading is a pure data operation: no Swing, no preferences, no
 * network, no clock. What the pack knows about itself - its version,
 * the bodies it carries, its coverage, where its inputs came from - is
 * in the manifest and exposed as it is written, so a caller that
 * states it quotes the pack rather than repeating it.
 */
public final class SolarSystemPack {

    static final String RESOURCE_ROOT = "/resources/solar-system/";
    static final int SUPPORTED_FORMAT_VERSION = 1;
    static final String PACK_NAME = "solar-system";

    private final Map<String, String> manifest;
    private final SpkKernel kernel;
    private final LeapSeconds leapSeconds;

    private SolarSystemPack(Map<String, String> manifest, SpkKernel kernel,
                            LeapSeconds leapSeconds) {
        this.manifest = manifest;
        this.kernel = kernel;
        this.leapSeconds = leapSeconds;
    }

    /** Loads the pack the application ships. */
    public static SolarSystemPack load() {
        return load(name -> SolarSystemPack.class.getResourceAsStream(
                RESOURCE_ROOT + name));
    }

    /** Loads a pack through an opener, as a test or a builder does. */
    public static SolarSystemPack load(Function<String, InputStream> resources) {
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
            throw new PackIntegrityException("solar-system pack format "
                    + format + " is not the supported "
                    + SUPPORTED_FORMAT_VERSION);
        }
        String name = required(manifest, "pack.name");
        if (!PACK_NAME.equals(name)) {
            throw new PackIntegrityException("expected pack " + PACK_NAME
                    + ", found " + name);
        }
        String kernelName = required(manifest, "ephemeris.kernel");
        String leapName = required(manifest, "leapseconds.file");
        try {
            SpkKernel kernel = SpkKernel.read(
                    verified(resources, manifest, kernelName));
            LeapSeconds leap = LeapSeconds.parse(
                    new java.io.ByteArrayInputStream(
                            verified(resources, manifest, leapName)));
            return new SolarSystemPack(Collections.unmodifiableMap(manifest),
                    kernel, leap);
        } catch (IOException e) {
            throw new PackIntegrityException("the solar-system pack cannot be"
                    + " read: " + e.getMessage(), e);
        }
    }

    /** The manifest, as written by the builder. */
    public Map<String, String> manifest() {
        return manifest;
    }

    /** The ephemeris excerpt. */
    public SpkKernel kernel() {
        return kernel;
    }

    /** The leap-second record, with its own validity. */
    public LeapSeconds leapSeconds() {
        return leapSeconds;
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
                            + " make import-solar-system", name, expected, actual));
        }
        return bytes;
    }

    private static InputStream require(Function<String, InputStream> resources,
                                       String name) {
        InputStream in = resources.apply(name);
        if (in == null) {
            throw new PackIntegrityException("solar-system pack resource "
                    + name + " is missing");
        }
        return in;
    }

    private static String required(Map<String, String> manifest, String key) {
        String value = manifest.get(key);
        if (value == null || value.isBlank()) {
            throw new PackIntegrityException("solar-system manifest lacks " + key);
        }
        return value;
    }
}
