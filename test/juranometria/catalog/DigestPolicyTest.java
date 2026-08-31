package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One digest, one policy (audit review, P2).
 *
 * <p>Three packs each carried a private copy of the same hashing
 * helper, and one copy drifted: it reported an absent SHA-256 as a
 * pack integrity failure, which would have advised a reader with a
 * perfectly good download to fetch the catalogue again. The copies
 * are gone; these tests keep them gone.
 */
class DigestPolicyTest {

    @Test
    void theSharedDigestIsTheOrdinaryOne() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad",
                Sha256.hex("abc".getBytes(StandardCharsets.UTF_8)),
                "the atlas hashes bytes the way everything else does,"
                        + " so a published checksum can be checked with"
                        + " any tool");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb924"
                        + "27ae41e4649b934ca495991b7852b855",
                Sha256.hex(new byte[0]));
    }

    @Test
    void onlyOnePlaceInTheApplicationHashesAnything() throws IOException {
        // The structural guard: a fourth private copy is exactly how
        // this defect arrived, so a new one fails here rather than in
        // a reader's error message.
        List<String> owners = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(Path.of("src/juranometria"))) {
            for (Path source : tree.filter(p -> p.toString().endsWith(".java"))
                    .toList()) {
                // juranometria.tool is the developer's import and
                // study pipeline, not the running atlas: it verifies
                // pinned upstream downloads, which is its own concern.
                if (source.toString().contains("/tool/")
                        || source.endsWith("Sha256.java")) {
                    continue;
                }
                if (Files.readString(source).contains("MessageDigest")) {
                    owners.add(source.toString());
                }
            }
        }
        assertEquals(List.of(), owners,
                "every pack must verify itself through Sha256, so the"
                        + " policy for an unavailable algorithm cannot"
                        + " differ between packs");
    }

    @Test
    void anUnavailableAlgorithmIsARuntimeFaultNeverADamagedDownload()
            throws IOException {
        // The consequence that matters, stated where it is decided.
        String source = Files.readString(
                Path.of("src/juranometria/catalog/Sha256.java"));
        int branch = source.indexOf("NoSuchAlgorithmException e");
        assertTrue(branch > 0, "the branch must still exist");
        String handling = source.substring(branch);
        assertTrue(handling.contains("new IllegalStateException"),
                "an absent SHA-256 is a broken Java runtime");
        assertFalse(handling.contains("new PackIntegrityException"),
                "and never a pack integrity failure, which would tell"
                        + " the reader to re-download data that is"
                        + " perfectly good (the launch surface's own"
                        + " side of this is asserted in"
                        + " StartupFailureTest)");
    }
}
