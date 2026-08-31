package juranometria.catalog;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The versioned manifest of a bundled catalogue pack, stored as a plain
 * {@link Properties} file so coverage, magnitude depth, tiling scheme, and
 * source/licence provenance are machine-readable without README prose.
 * Parsing validates the contract from docs/decisions/all-sky-tiling.md and
 * fails with a clear diagnostic on anything missing or incompatible.
 */
public record PackManifest(int formatVersion, String packName, String coverage,
                           double starLimitVmag, String tilingScheme,
                           double maxObjectSemiExtentDegrees,
                           Map<String, String> entries) {

    public static final int SUPPORTED_FORMAT_VERSION = 1;
    public static final String TILING_SCHEME = "radec-grid-30";

    private static final String[] REQUIRED_KEYS = {
            "format.version", "pack.name", "coverage.type", "stars.limit.vmag",
            "tiling.scheme", "objects.max.semi.extent.degrees",
            "sources.tycho2.catalogue", "sources.openngc.release",
            "license.stars", "license.dsos",
    };

    public PackManifest {
        entries = Map.copyOf(new TreeMap<>(entries));
    }

    /** Parses and validates a manifest; the reader is not closed. */
    public static PackManifest parse(Reader reader, String name) {
        Properties properties = new Properties();
        try {
            properties.load(reader);
        } catch (IOException e) {
            throw new PackIntegrityException("failed to read pack manifest " + name, e);
        }
        Map<String, String> entries = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            entries.put(key, properties.getProperty(key));
        }
        for (String required : REQUIRED_KEYS) {
            if (!entries.containsKey(required) || entries.get(required).isBlank()) {
                throw new IllegalArgumentException(
                        "pack manifest " + name + " lacks required key " + required);
            }
        }
        int formatVersion = parseInt(entries.get("format.version"), "format.version", name);
        if (formatVersion != SUPPORTED_FORMAT_VERSION) {
            throw new IllegalArgumentException("pack manifest " + name
                    + " has unsupported format.version " + formatVersion
                    + " (supported: " + SUPPORTED_FORMAT_VERSION + ")");
        }
        String tilingScheme = entries.get("tiling.scheme");
        if (!TILING_SCHEME.equals(tilingScheme)) {
            throw new IllegalArgumentException("pack manifest " + name
                    + " uses unsupported tiling.scheme " + tilingScheme);
        }
        double starLimit = parseDouble(entries.get("stars.limit.vmag"),
                "stars.limit.vmag", name);
        if (!(starLimit > 0.0) || !Double.isFinite(starLimit)) {
            throw new IllegalArgumentException("pack manifest " + name
                    + " has invalid stars.limit.vmag " + entries.get("stars.limit.vmag"));
        }
        double maxSemiExtent = parseDouble(entries.get("objects.max.semi.extent.degrees"),
                "objects.max.semi.extent.degrees", name);
        if (!(maxSemiExtent > 0.0) || !Double.isFinite(maxSemiExtent)) {
            throw new IllegalArgumentException("pack manifest " + name
                    + " has invalid objects.max.semi.extent.degrees "
                    + entries.get("objects.max.semi.extent.degrees"));
        }
        return new PackManifest(formatVersion, entries.get("pack.name"),
                entries.get("coverage.type"), starLimit, tilingScheme,
                maxSemiExtent, entries);
    }

    private static int parseInt(String value, String key, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "pack manifest " + name + " has non-integer " + key + ": " + value);
        }
    }

    private static double parseDouble(String value, String key, String name) {
        try {
            return Double.parseDouble(value.trim().toLowerCase(Locale.ROOT));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "pack manifest " + name + " has non-numeric " + key + ": " + value);
        }
    }
}
