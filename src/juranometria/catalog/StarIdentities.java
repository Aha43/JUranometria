package juranometria.catalog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import juranometria.chart.StarIdentity;

/**
 * The bundled star-identity pack behind an immutable TYC-keyed lookup
 * (issue #114). Loading validates the manifest contract (format
 * version and pack identity), verifies the identity CSV against its
 * manifest checksum before trusting a row, and fails with a clear
 * diagnostic on any malformed row, orphan designation, duplicate
 * identifier, or row-count drift - never a silently partial or wrong
 * identity layer. A star the pack does not know simply has no
 * identity.
 */
public final class StarIdentities {

    static final String DEFAULT_ROOT = "/resources/catalog/star-identities/";
    static final String PACK_NAME = "star-identities";
    static final String CSV_NAME = "star-identities.csv";
    static final String SUPPORTED_FORMAT_VERSION = "1";

    /** Manifest keys every valid pack must carry. */
    static final String[] REQUIRED_MANIFEST_KEYS = {
            "format.version", "pack.name", "join.contract", "rows",
            "source", "source.commit", "license",
            "checksum." + CSV_NAME,
    };

    private final Map<String, StarIdentity> byTyc;

    private StarIdentities(Map<String, StarIdentity> byTyc) {
        this.byTyc = Map.copyOf(byTyc);
    }

    /** Loads the bundled star-identity pack from the classpath. */
    public static StarIdentities load() {
        return load(name -> StarIdentities.class
                .getResourceAsStream(DEFAULT_ROOT + name));
    }

    /** Loads a pack through any resource source; for tests and futures. */
    static StarIdentities load(Function<String, InputStream> resources) {
        Properties manifest = new Properties();
        InputStream manifestStream = resources.apply("manifest.properties");
        if (manifestStream == null) {
            throw new PackIntegrityException(
                    "star-identity pack manifest is missing");
        }
        try (manifestStream) {
            manifest.load(new InputStreamReader(manifestStream,
                    StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PackIntegrityException(
                    "failed to read the star-identity manifest", e);
        }
        validateManifest(manifest);

        byte[] bytes = verifiedCsv(resources,
                manifest.getProperty("checksum." + CSV_NAME));
        Parsed parsed = parse(bytes);
        int declaredRows = Integer.parseInt(manifest.getProperty("rows"));
        if (parsed.rows() != declaredRows) {
            throw new PackIntegrityException(String.format(Locale.ROOT,
                    "star-identity pack declares %d rows but carries %d",
                    declaredRows, parsed.rows()));
        }
        return new StarIdentities(parsed.byTyc());
    }

    /**
     * Validates a star-identity pack manifest; the generator shares
     * this check so an unsupported format or foreign pack fails
     * loudly on both the generating and the loading side.
     */
    public static void validateManifest(Properties manifest) {
        for (String key : REQUIRED_MANIFEST_KEYS) {
            if (manifest.getProperty(key) == null) {
                throw new PackIntegrityException(
                        "star-identity manifest is missing " + key);
            }
        }
        if (!SUPPORTED_FORMAT_VERSION.equals(
                manifest.getProperty("format.version"))) {
            throw new PackIntegrityException("unsupported star-identity pack"
                    + " format.version "
                    + manifest.getProperty("format.version")
                    + "; this build supports " + SUPPORTED_FORMAT_VERSION);
        }
        if (!PACK_NAME.equals(manifest.getProperty("pack.name"))) {
            throw new PackIntegrityException("manifest belongs to pack "
                    + manifest.getProperty("pack.name") + ", not " + PACK_NAME);
        }
    }

    /** The identity for a TYC identifier, or null: honestly unknown. */
    public StarIdentity identityOf(String tyc) {
        return byTyc.get(tyc);
    }

    /** The number of identities carried. */
    public int size() {
        return byTyc.size();
    }

    private static byte[] verifiedCsv(Function<String, InputStream> resources,
                                      String expected) {
        InputStream stream = resources.apply(CSV_NAME);
        if (stream == null) {
            throw new PackIntegrityException("star-identity data listed in"
                    + " the manifest is missing: " + CSV_NAME);
        }
        byte[] bytes;
        try (stream) {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new PackIntegrityException(
                    "failed to read " + CSV_NAME, e);
        }
        String actual = sha256Hex(bytes);
        if (!actual.equals(expected)) {
            throw new PackIntegrityException(CSV_NAME
                    + " does not match its manifest checksum"
                    + "\n  expected " + expected + "\n  actual   " + actual);
        }
        return bytes;
    }

    private record Parsed(Map<String, StarIdentity> byTyc, int rows) {
    }

    private static Parsed parse(byte[] bytes) {
        Map<String, StarIdentity> byTyc = new HashMap<>();
        java.util.Set<String> tycs = new java.util.HashSet<>();
        int rows = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (!"tyc,name,bayer,flamsteed,constellation".equals(header)) {
                throw new PackIntegrityException(
                        "unexpected star-identity header: " + header);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    throw new PackIntegrityException(
                            "malformed star-identity line: " + line);
                }
                if (fields[0].isBlank()) {
                    throw new PackIntegrityException(
                            "star-identity line without a TYC id: " + line);
                }
                if (!tycs.add(fields[0])) {
                    throw new PackIntegrityException(
                            "duplicate star-identity id: " + fields[0]);
                }
                rows++;
                // A row whose every identity field is unknown (the
                // pack's counted source-anomaly category) is a row
                // without an identity, honestly - not an error.
                if (fields[1].isEmpty() && fields[2].isEmpty()
                        && fields[3].isEmpty() && fields[4].isEmpty()) {
                    continue;
                }
                try {
                    byTyc.put(fields[0], new StarIdentity(
                            nullIfEmpty(fields[1]), nullIfEmpty(fields[2]),
                            nullIfEmpty(fields[3]), nullIfEmpty(fields[4])));
                } catch (IllegalArgumentException e) {
                    throw new PackIntegrityException(
                            "dishonest star-identity line: " + line, e);
                }
            }
        } catch (IOException e) {
            throw new PackIntegrityException("failed to read " + CSV_NAME, e);
        }
        return new Parsed(byTyc, rows);
    }

    private static String nullIfEmpty(String field) {
        return field.isEmpty() ? null : field;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
