package juranometria.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * The pinned raw inputs shared by the import tools: their SHA-256
 * checksums (audited 2026-08-29 from the sources in
 * docs/decisions/catalogue-sources.md) and the helpers to verify and read
 * them. A checksum mismatch or missing file fails before any
 * transformation.
 */
final class PinnedInputs {

    static final Map<String, String> SHA256 = Map.ofEntries(
            Map.entry("tyc2.dat.00.gz", "1b224570fc6eb151984ce106fdf797728649d1ea77aaf3effd2d3444cfac6df6"),
            Map.entry("tyc2.dat.01.gz", "36221f4a5cdd9c5009d64299d2e6409f8d4cdf924321768780165bd3e2a2a99a"),
            Map.entry("tyc2.dat.02.gz", "e2ea4eeb7d204dde70ac54d172b1c429540c0d23d62ec0d7500561adc44cc57b"),
            Map.entry("tyc2.dat.03.gz", "29e0cb57cba7e1651455efcb528276b0c28e1fe83f47f8ebe963e48d8b180afb"),
            Map.entry("tyc2.dat.04.gz", "d04868329b470aa320f0fe5e9c525be619a7a228b364433e5f114f2178946d9b"),
            Map.entry("tyc2.dat.05.gz", "51f08772197dcf0cd26ff098c866036340b797f71ee04be22712517c10c73c4e"),
            Map.entry("tyc2.dat.06.gz", "f55503a66abce0e11d1a4ccbcde1f4c97a6e3e769de85e005f57ba963086ab03"),
            Map.entry("tyc2.dat.07.gz", "bc15b5b1f6308477360fbb93f6a98d9029601cab68973202be0f8764c127f524"),
            Map.entry("tyc2.dat.08.gz", "315ac34c678bc2e0f568b43b6b2d7c3d7c9cf9c089b51115d18b872864e50426"),
            Map.entry("tyc2.dat.09.gz", "d66c671fa29aad10bc5fd697f68c918bb774c1ffc3c7db9c20234895268390d5"),
            Map.entry("tyc2.dat.10.gz", "d86529df819ebdf3f4b8892510973ee10e1db06930814e014dd467193e740d52"),
            Map.entry("tyc2.dat.11.gz", "fc0c3203b3d2787da54c43d9963e363c8a3926576c383b101a15350a7ca23e9c"),
            Map.entry("tyc2.dat.12.gz", "68fd9d7e7353d52dea0de043cc62b45c04d234104eb05c063dc23ea7b6759576"),
            Map.entry("tyc2.dat.13.gz", "a08ecc092e6d134742c0f594c56c5c2e02b8fd5e76cffb1779877f2fcefac3fc"),
            Map.entry("tyc2.dat.14.gz", "2846e8cc489795a4d8160d2d5b64ff0e9a00598adba35a52a4f8ccf4c5e38b3e"),
            Map.entry("tyc2.dat.15.gz", "74784a3656382090a70d778820335f4a781509dbe3fcb9e92d18cf96b2c46c72"),
            Map.entry("tyc2.dat.16.gz", "320560e3d551cc40fa1f54ef7133709bf4bd45efa6688f808be7c09be5544a4d"),
            Map.entry("tyc2.dat.17.gz", "a8154e940aa0a0e8f31d91b4bd9fd56e6849da42cddb13c7dda772439b06991a"),
            Map.entry("tyc2.dat.18.gz", "5e951a1a51df7956f205b83cbfa5501d357c843640a253c1e6d3917bebe7d928"),
            Map.entry("tyc2.dat.19.gz", "f59605a38116f517a31a7dbdee3469c077658f2f40b8afe5da2aeb832eaee3dd"),
            Map.entry("suppl_1.dat.gz", "d256a9fc47259d506e4849b054e9392a62b2ed128e48ac6a25a3a60fcc317f0e"),
            Map.entry("openngc-NGC.csv", "840fe0c9ee1332e551b2e722a0e92726cd7b157914a3d2177602832aadd3aa9e"),
            Map.entry("openngc-addendum.csv", "1d8f0914e643ada325a5a94d88d8fefad6a4937a2f77cc34f21483af22b11983"),
            Map.entry("openngc-CC-BY-SA-4.0.txt", "cde7883b9050a1104f4ac19a1572aafd6e5d7323b68351aaf51fbf4beba54966"));

    private PinnedInputs() {
    }

    /** Verifies every pinned input in deterministic order. */
    static void verifyAll(Path rawDir) throws IOException {
        for (Map.Entry<String, String> pinned : new TreeMap<>(SHA256).entrySet()) {
            verifyChecksum(rawDir.resolve(pinned.getKey()), pinned.getValue());
        }
    }

    static void verifyChecksum(Path file, String expectedSha256) throws IOException {
        if (!Files.exists(file)) {
            throw new IllegalStateException("missing pinned input: " + file
                    + " (run scripts/download-catalogue-sources.sh)");
        }
        String actual = sha256Hex(Files.readAllBytes(file));
        if (!actual.equals(expectedSha256)) {
            throw new IllegalStateException("checksum mismatch for " + file
                    + "\n  expected " + expectedSha256 + "\n  actual   " + actual);
        }
    }

    static String sha256Hex(byte[] bytes) {
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

    static void readGzLines(Path file, Consumer<String> consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
    }
}
