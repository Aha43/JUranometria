package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import juranometria.chart.StarIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The star-identity loader against the real bundled pack and against
 * dishonest synthetic packs: identities load exactly as generated,
 * and every malformed, tampered, foreign, or drifting input fails
 * with a diagnostic naming the problem - never a silently partial or
 * wrong identity layer.
 */
class StarIdentitiesTest {

    @Test
    void theRealBundledPackLoadsItsIdentitiesExactly() {
        StarIdentities identities = StarIdentities.load();
        // 4,805 rows minus the 274 rows whose every field is unknown
        // (source entries carrying only identifiers the decision does
        // not pack) - counted rows, honestly not identities.
        assertEquals(4531, identities.size());
        assertEquals(new StarIdentity("Betelgeuse", "α", "58", "Ori"),
                identities.identityOf("TYC 129-1873-1"));
        assertEquals(new StarIdentity("Acrux", "α1", null, "Cru"),
                identities.identityOf("TYC 8979-3464-1"),
                "component digits stay verbatim");
        assertEquals(new StarIdentity(null, null, "35", "Cru"),
                identities.identityOf("TYC 8658-751-1"),
                "a Flamsteed-only identity keeps its unknowns unknown");
        assertNull(identities.identityOf("TYC 1-1-1"),
                "a star the pack does not know has no identity");
    }

    @Test
    void theIdentityRecordRefusesDishonestShapes() {
        assertThrows(IllegalArgumentException.class,
                () -> new StarIdentity("", null, null, null),
                "blank is not unknown");
        assertThrows(IllegalArgumentException.class,
                () -> new StarIdentity(null, null, null, null),
                "an identity with no fields is no identity");
        assertThrows(IllegalArgumentException.class,
                () -> new StarIdentity(null, "α", null, null),
                "a designation never floats without its constellation");
    }

    @Test
    void aTamperedCsvFailsItsChecksumBeforeAnyRowIsTrusted() {
        Map<String, byte[]> pack = validPack();
        pack.put("star-identities.csv",
                (csv("TYC 1-2-1,Example,α,,Ori") + "tampered\n")
                        .getBytes(StandardCharsets.UTF_8));
        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> load(pack));
        assertTrue(failure.getMessage().contains("checksum"),
                failure.getMessage());
    }

    @Test
    void malformedAndDishonestRowsFailLoudly() {
        assertLoadFails(csv("TYC 1-2-1,Example,α,,Ori,extra"), "malformed");
        assertLoadFails(csv("TYC 1-2-1,Example,α,,"), "dishonest");
        assertLoadFails(csv(",Example,,,Ori"), "without a TYC id");
        assertLoadFails(csv("TYC 1-2-1,A,,,Ori\nTYC 1-2-1,B,,,Ori"),
                "duplicate");
        assertLoadFails("wrong,header\n", "header");
    }

    @Test
    void rowCountDriftAndForeignManifestsAreRejected() {
        Map<String, byte[]> drifted = validPack();
        drifted.put("manifest.properties", manifest(
                sha256(drifted.get("star-identities.csv")), "2",
                "star-identities", "1"));
        assertTrue(assertThrows(IllegalStateException.class,
                        () -> load(drifted)).getMessage().contains("rows"),
                "declared row count must match the data");

        Map<String, byte[]> foreign = validPack();
        foreign.put("manifest.properties", manifest(
                sha256(foreign.get("star-identities.csv")), "1",
                "bright-sky", "1"));
        assertThrows(IllegalStateException.class, () -> load(foreign));

        Map<String, byte[]> unsupported = validPack();
        unsupported.put("manifest.properties", manifest(
                sha256(unsupported.get("star-identities.csv")), "1",
                "star-identities", "7"));
        assertTrue(assertThrows(IllegalStateException.class,
                        () -> load(unsupported)).getMessage()
                        .contains("format.version"),
                "unsupported versions are named");
    }

    @Test
    void aRowWhoseEveryFieldIsUnknownIsARowWithoutAnIdentity() {
        Map<String, byte[]> pack = pack(
                csv("TYC 1-2-1,Example,α,,Ori\nTYC 1-2-2,,,,"), "2");
        StarIdentities identities = load(pack);
        assertEquals(1, identities.size());
        assertNull(identities.identityOf("TYC 1-2-2"));
    }

    private static void assertLoadFails(String csvText, String diagnostic) {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> load(pack(csvText,
                        Long.toString(csvText.lines().count() - 1))));
        assertTrue(failure.getMessage().contains(diagnostic),
                "expected '" + diagnostic + "' in: " + failure.getMessage());
    }

    private static StarIdentities load(Map<String, byte[]> pack) {
        Function<String, InputStream> resources = name -> {
            byte[] bytes = pack.get(name);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        };
        return StarIdentities.load(resources);
    }

    private static Map<String, byte[]> validPack() {
        return pack(csv("TYC 1-2-1,Example,α,,Ori"), "1");
    }

    private static Map<String, byte[]> pack(String csvText, String rows) {
        byte[] csv = csvText.getBytes(StandardCharsets.UTF_8);
        Map<String, byte[]> pack = new HashMap<>();
        pack.put("star-identities.csv", csv);
        pack.put("manifest.properties", manifest(sha256(csv), rows,
                "star-identities", "1"));
        return pack;
    }

    private static String csv(String rows) {
        return "tyc,name,bayer,flamsteed,constellation\n" + rows + "\n";
    }

    private static byte[] manifest(String checksum, String rows,
                                   String packName, String version) {
        return ("format.version=" + version + "\n"
                + "pack.name=" + packName + "\n"
                + "join.contract=test\n"
                + "rows=" + rows + "\n"
                + "source=test\n"
                + "source.commit=0\n"
                + "license=BSD-3-Clause\n"
                + "checksum.star-identities.csv=" + checksum + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest(bytes)) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
