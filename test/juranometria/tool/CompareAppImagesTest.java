package juranometria.tool;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The app-image reproducibility reporter (issue #158): the
 * differing branch must run to completion and report, because that
 * is the outcome the release contract asks to be recorded rather
 * than failed on - the earlier inline version aborted while
 * counting, since diff exits 1 under `set -e`. A real diff error
 * still fails.
 */
class CompareAppImagesTest {

    private record Run(int status, String output) {
    }

    private static Run compare(Path a, Path b) throws Exception {
        var process = new ProcessBuilder("scripts/compare-app-images.sh",
                a.toString(), b.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        return new Run(process.waitFor(), output);
    }

    @Test
    void identicalDifferingAndBrokenComparisonsEachBehave() throws Exception {
        Assumptions.assumeTrue(
                Files.isExecutable(Path.of("scripts/compare-app-images.sh")),
                "POSIX script not executable in this checkout");
        Path root = Files.createTempDirectory("app-image-compare");
        try {
            Path a = Files.createDirectories(root.resolve("a/runtime"));
            Path b = Files.createDirectories(root.resolve("b/runtime"));
            Files.writeString(a.resolve("release"), "JAVA_VERSION=\"21\"\n");
            Files.writeString(b.resolve("release"), "JAVA_VERSION=\"21\"\n");

            Run identical = compare(root.resolve("a"), root.resolve("b"));
            assertEquals(0, identical.status());
            assertTrue(identical.output().contains("byte-identical"),
                    identical.output());

            // The branch the inline version could never reach: a real
            // difference is counted, reported, and NOT a failure.
            Files.writeString(b.resolve("release"), "JAVA_VERSION=\"21.0.1\"\n");
            Files.writeString(b.resolve("stray.txt"), "only in b\n");
            Run differing = compare(root.resolve("a"), root.resolve("b"));
            assertEquals(0, differing.status(),
                    "reporting differences must run to completion: "
                            + differing.output());
            assertTrue(differing.output().contains("2 differing entries"),
                    differing.output());
            assertTrue(differing.output().contains("release"),
                    "representative entries are shown: "
                            + differing.output());

            // A genuine error is still a failure.
            Run broken = compare(root.resolve("a"),
                    root.resolve("does-not-exist"));
            assertEquals(1, broken.status());
            assertTrue(broken.output().contains("not a directory"),
                    broken.output());
        } finally {
            try (var walk = Files.walk(root)) {
                for (Path path : walk.sorted(
                        java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
