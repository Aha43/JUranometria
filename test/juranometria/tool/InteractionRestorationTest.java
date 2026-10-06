package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interaction route's tree comes out as it went in (#463).
 *
 * <p>The route's first real run (#462) passed its contract and then
 * failed its "Nothing moved" step: the platform records reproduce on
 * the runner, as the contract asks, but with the runner's bytes, which
 * it left in the tree. The ruling is restoration, not exclusion: a
 * green interaction run puts back exactly the records it judged, to
 * their pre-run bytes; an unrelated change to a record, or to anything
 * else, is left for the diff to name; and a breached run restores no
 * record, so what it wrote stays readable. These hold the policy
 * through the contract's own concluding path, the way the inspection
 * imagery's restoration is held.
 */
class InteractionRestorationTest {

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String key(Path file) {
        return file.toString().replace(java.io.File.separatorChar, '/');
    }

    private static void delete(Path root) throws Exception {
        try (var tree = Files.walk(root)) {
            for (Path file : tree.sorted(
                    java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    void aGreenInteractionRunPutsBackExactlyTheRecordsItJudged()
            throws Exception {
        Path root = Files.createTempDirectory("interaction-restore");
        try {
            Path judged = root.resolve("a/platform.md");
            Path alreadyChanged = root.resolve("b/platform.md");
            Path unjudged = root.resolve("c/platform.md");
            Path inspection = root.resolve("controls-thing.png");
            Path renderer = root.resolve("page-thing.png");
            Files.createDirectories(judged.getParent());
            Files.createDirectories(alreadyChanged.getParent());
            Files.createDirectories(unjudged.getParent());
            Files.write(judged, bytes("| operating system | recorded here |"));
            Files.write(alreadyChanged, bytes("| operating system | as committed |"));
            // A change that was already in the tree when the run began:
            // the run's pre-run bytes are these, not the committed ones.
            Files.write(alreadyChanged,
                    bytes("| operating system | changed before the run |"));
            Files.write(unjudged, bytes("| operating system | a chart producer's |"));
            Files.write(inspection, new byte[] {1, 2, 3});
            Files.write(renderer, new byte[] {4, 5, 6});
            Map<String, EvidenceContractMain.Snapshot> committed =
                    EvidenceContractMain.snapshot(root);

            boolean green = EvidenceContractMain.concludeUnderRestoration(
                    root, EvidenceContractMain.Mode.INTERACTION, committed,
                    List.of(key(judged), key(alreadyChanged)), () -> {
                        // What the run does on another machine: every
                        // record it runs is rewritten with that
                        // machine's words; inspection imagery drifts;
                        // and, here, a chart picture moves too.
                        Files.write(judged,
                                bytes("| operating system | the runner's |"));
                        Files.write(alreadyChanged,
                                bytes("| operating system | the runner's |"));
                        Files.write(unjudged,
                                bytes("| operating system | the runner's |"));
                        Files.write(inspection, new byte[] {9, 9, 9});
                        Files.write(renderer, new byte[] {7, 7, 7});
                    });

            assertTrue(green, "no breach: the run concluded green");
            assertArrayEquals(bytes("| operating system | recorded here |"),
                    Files.readAllBytes(judged),
                    "a judged record goes back to its pre-run bytes");
            assertArrayEquals(
                    bytes("| operating system | changed before the run |"),
                    Files.readAllBytes(alreadyChanged),
                    "pre-run, not older: a record already changed when the"
                            + " run began stays changed, for the diff to name");
            assertArrayEquals(bytes("| operating system | the runner's |"),
                    Files.readAllBytes(unjudged),
                    "a record the run did not judge is not restored - the"
                            + " diff names it");
            assertArrayEquals(new byte[] {7, 7, 7},
                    Files.readAllBytes(renderer),
                    "nothing but platform records is touched: a moved chart"
                            + " picture stays moved, for the diff to name");
            assertArrayEquals(new byte[] {1, 2, 3},
                    Files.readAllBytes(inspection),
                    "and the inspection imagery goes back as it always has");
        } finally {
            delete(root);
        }
    }

    @Test
    void aBreachedRunRestoresNoRecordSoTheEvidenceStaysReadable()
            throws Exception {
        Path root = Files.createTempDirectory("interaction-breach");
        try {
            Path judged = root.resolve("a/platform.md");
            Path inspection = root.resolve("controls-thing.png");
            Files.createDirectories(judged.getParent());
            Files.write(judged, bytes("| operating system | recorded here |"));
            Files.write(inspection, new byte[] {1, 2, 3});
            Map<String, EvidenceContractMain.Snapshot> committed =
                    EvidenceContractMain.snapshot(root);

            boolean green = EvidenceContractMain.concludeUnderRestoration(
                    root, EvidenceContractMain.Mode.INTERACTION, committed,
                    List.of(key(judged)), () -> {
                        Files.write(judged,
                                bytes("| operating system | the runner's |"));
                        Files.write(inspection, new byte[] {9, 9, 9});
                        throw new EvidenceContractMain.Breached();
                    });

            assertFalse(green, "a breach is the answer, not an exception");
            assertArrayEquals(bytes("| operating system | the runner's |"),
                    Files.readAllBytes(judged),
                    "a breached run leaves what it wrote: the record is the"
                            + " evidence a reader needs, not tidied away");
            assertArrayEquals(new byte[] {1, 2, 3},
                    Files.readAllBytes(inspection),
                    "while the inspection imagery still goes back - that"
                            + " guarantee is older and separate");
        } finally {
            delete(root);
        }
    }

    @Test
    void aGeneratorThatDiesStillComesOutAsItsFailure() throws Exception {
        Path root = Files.createTempDirectory("interaction-died");
        try {
            Path judged = root.resolve("a/platform.md");
            Files.createDirectories(judged.getParent());
            Files.write(judged, bytes("| operating system | recorded here |"));
            Map<String, EvidenceContractMain.Snapshot> committed =
                    EvidenceContractMain.snapshot(root);
            IllegalStateException died = assertThrows(
                    IllegalStateException.class, () ->
                    EvidenceContractMain.concludeUnderRestoration(root,
                            EvidenceContractMain.Mode.INTERACTION, committed,
                            List.of(key(judged)), () -> {
                                Files.write(judged,
                                        bytes("| operating system | half |"));
                                throw new IllegalStateException("died");
                            }));
            assertEquals("died", died.getMessage());
            assertArrayEquals(bytes("| operating system | half |"),
                    Files.readAllBytes(judged),
                    "a run that died is not green: nothing is put back");
        } finally {
            delete(root);
        }
    }

    @Test
    void thePortableAndCanonicalContractsLeaveTheirRecordsAsWritten()
            throws Exception {
        // The wide route's job has no tree-unchanged step and the
        // canonical machine's records are the committed bytes; neither
        // changes (#463 is the interaction route's repair).
        for (EvidenceContractMain.Mode mode : List.of(
                EvidenceContractMain.Mode.PORTABLE,
                EvidenceContractMain.Mode.CANONICAL)) {
            Path root = Files.createTempDirectory("portable-records");
            try {
                Path judged = root.resolve("a/platform.md");
                Files.createDirectories(judged.getParent());
                Files.write(judged, bytes("| operating system | recorded here |"));
                Map<String, EvidenceContractMain.Snapshot> committed =
                        EvidenceContractMain.snapshot(root);
                assertTrue(EvidenceContractMain.concludeUnderRestoration(root,
                        mode, committed, List.of(key(judged)), () ->
                                Files.write(judged,
                                        bytes("| operating system | written |"))));
                assertArrayEquals(bytes("| operating system | written |"),
                        Files.readAllBytes(judged),
                        mode + " leaves the record as the run wrote it");
            } finally {
                delete(root);
            }
        }
    }

    @Test
    void restorationCountsWhatItMovedAndLeavesANewcomerToItsBreach()
            throws Exception {
        Path root = Files.createTempDirectory("restore-count");
        try {
            Path same = root.resolve("a/platform.md");
            Path moved = root.resolve("b/platform.md");
            Path newcomer = root.resolve("c/platform.md");
            for (Path file : List.of(same, moved, newcomer)) {
                Files.createDirectories(file.getParent());
            }
            Files.write(same, bytes("same"));
            Files.write(moved, bytes("before"));
            Map<String, EvidenceContractMain.Snapshot> committed =
                    EvidenceContractMain.snapshot(root);
            Files.write(moved, bytes("after"));
            Files.write(newcomer, bytes("generated but not committed"));

            int restored = EvidenceContractMain.restorePlatformRecords(
                    committed, List.of(key(same), key(moved), key(newcomer)));

            assertEquals(1, restored, "only the record whose bytes moved");
            assertArrayEquals(bytes("before"), Files.readAllBytes(moved));
            assertArrayEquals(bytes("same"), Files.readAllBytes(same));
            assertTrue(Files.exists(newcomer),
                    "a record the snapshot never held is a newcomer the run"
                            + " has already named as a breach - restoration"
                            + " neither writes nor deletes it");
        } finally {
            delete(root);
        }
    }
}
