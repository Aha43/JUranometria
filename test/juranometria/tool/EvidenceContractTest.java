package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evidence contracts a PR check can afford (Sprint 26, issue
 * #242).
 *
 * <p>The full regeneration lives in {@code make evidence-contracts};
 * what runs here on every push is the cheap, decisive half: the
 * fast deterministic reports reproduced from stdout, the fixtures
 * held to their digests, every report present, and the inspection
 * imagery held to structure - dimensions, themes, variant
 * distinctness - because its bytes are not its contract and its
 * content still must be.
 */
class EvidenceContractTest {

    // ---- deterministic reports, the cheap four -----------------------

    @Test
    void theFastReportsReproduceByteForByteFromTheirMains()
            throws Exception {
        Map<String, String> fast = Map.of(
                "juranometria.tool.TestEvidenceStudyMain",
                "docs/studies/test-evidence/measurements.md",
                "juranometria.tool.PlaceAndTimeStudyMain",
                "docs/studies/place-and-time/measurements.md",
                "juranometria.tool.OnThisPageStudyMain",
                "docs/studies/on-this-page/measurements.md",
                "juranometria.tool.IdentifyStudyMain",
                "docs/studies/point-and-identify/measurements.md");
        PrintStream realOut = System.out;
        for (Map.Entry<String, String> report : fast.entrySet()) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured, true, "UTF-8"));
            try {
                Class.forName(report.getKey())
                        .getMethod("main", String[].class)
                        .invoke(null, (Object) new String[0]);
            } finally {
                System.setOut(realOut);
            }
            assertArrayEquals(
                    Files.readAllBytes(Path.of(report.getValue())),
                    captured.toByteArray(),
                    report.getValue() + " reproduces from its main:"
                            + " a hand edit, a stale commit or a"
                            + " nondeterminism is a finding");
        }
    }

    @Test
    void everyDeterministicReportExists() throws Exception {
        for (String report : List.of("application-mark",
                "chart-furniture", "deep-sky-occlusion",
                "deep-sky-vocabulary", "on-this-page", "place-and-time",
                "point-and-identify", "test-evidence")) {
            Path path = Path.of("docs/studies", report,
                    "measurements.md");
            assertTrue(Files.exists(path) && Files.size(path) > 1000,
                    "omitting a report fails: " + path);
        }
    }

    // ---- fixtures, by digest -----------------------------------------

    @Test
    void theFixturesCarryExactlyTheirPinnedBytes() throws Exception {
        for (Map.Entry<String, String> fixture
                : EvidenceContractMain.FIXTURES.entrySet()) {
            assertEquals(fixture.getValue(),
                    juranometria.catalog.Sha256.hex(Files.readAllBytes(
                            Path.of(fixture.getKey()))),
                    fixture.getKey() + " is committed data with"
                            + " provenance; a change is a provenance"
                            + " event, not a regeneration");
        }
    }

    @Test
    void theCapturedEvidenceCarriesExactlyItsPinnedBytes()
            throws Exception {
        for (Map.Entry<String, String> capture
                : EvidenceContractMain.CAPTURES.entrySet()) {
            assertEquals(capture.getValue(),
                    juranometria.catalog.Sha256.hex(Files.readAllBytes(
                            Path.of(capture.getKey()))),
                    capture.getKey() + " is an operating-system"
                            + " screenshot with provenance; a"
                            + " re-capture is a provenance event, not"
                            + " a regeneration");
        }
    }

    @Test
    void theCaptureContractJudgesAllItsBranches() throws Exception {
        String pinned = EvidenceContractMain.CAPTURES.keySet()
                .iterator().next();
        byte[] bytes = Files.readAllBytes(Path.of(pinned));

        assertEquals(List.of(),
                EvidenceContractMain.captureBreaches(pinned, bytes),
                "the pinned bytes pass");
        List<String> unpinned = EvidenceContractMain.captureBreaches(
                "docs/studies/mac-identity/screenshot-stray.png",
                bytes);
        assertEquals(1, unpinned.size(),
                "a capture nobody pinned is a breach, never a silent"
                        + " baseline");
        assertTrue(unpinned.get(0).contains("without a pinned digest"),
                unpinned.get(0));
        List<String> vanished = EvidenceContractMain.captureBreaches(
                pinned, null);
        assertEquals(1, vanished.size(), "a capture that vanished"
                + " mid-run is a breach");
        assertTrue(vanished.get(0).contains("vanished"),
                vanished.get(0));
        byte[] doctored = bytes.clone();
        doctored[doctored.length / 2] ^= 1;
        List<String> changed = EvidenceContractMain.captureBreaches(
                pinned, doctored);
        assertEquals(1, changed.size(),
                "one flipped bit in a substituted screenshot is"
                        + " caught by its digest");
        assertTrue(changed.get(0).contains("provenance event"),
                changed.get(0));

        List<String> allVanished = EvidenceContractMain
                .vanishedCapturePins(java.util.Set.of());
        assertEquals(EvidenceContractMain.CAPTURES.size(),
                allVanished.size(),
                "every vanished pinned capture is its own breach");
        for (String breach : allVanished) {
            assertTrue(breach.contains("missing from the tree"),
                    breach);
        }
        assertEquals(List.of(), EvidenceContractMain
                        .vanishedCapturePins(
                                EvidenceContractMain.CAPTURES.keySet()),
                "and a tree holding every pinned capture is clean");
    }

    // ---- the restoration guarantees, regression-tested ---------------

    @Test
    void restorationSurvivesWhateverHappenedAndRemovesStrays()
            throws Exception {
        // The guarantee the review refused to accept as a rehearsal
        // in a pull-request narrative: drift on inspection imagery
        // is put back, inspection strays are removed, and files of
        // other classes are left for the newcomer breach to name.
        Path root = Files.createTempDirectory("evidence-restore");
        try {
            Path inspection = root.resolve("controls-thing.png");
            Path renderer = root.resolve("page-thing.png");
            Files.write(inspection, new byte[] {1, 2, 3});
            Files.write(renderer, new byte[] {4, 5, 6});
            var committed = EvidenceContractMain.snapshot(root);

            // A generation that drifts, strays, and THROWS - run
            // through the verifier's own outer finally, not a
            // cleanup called by hand (review). The failure must
            // still come out the other side.
            IllegalStateException died =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            IllegalStateException.class, () ->
                    EvidenceContractMain.generateUnderRestoration(
                            root, committed, () -> {
                                Files.write(inspection,
                                        new byte[] {9, 9, 9});
                                Files.write(root.resolve(
                                        "controls-stray.png"),
                                        new byte[] {7});
                                Files.write(root.resolve(
                                        "page-stray.png"),
                                        new byte[] {8});
                                List<String> breaches =
                                        EvidenceContractMain
                                                .newcomerBreaches(root,
                                                        committed);
                                assertEquals(2, breaches.size(),
                                        "every newcomer is named,"
                                                + " whatever its"
                                                + " class: "
                                                + breaches);
                                throw new IllegalStateException(
                                        "generator died mid-run");
                            }));
            assertEquals("generator died mid-run", died.getMessage(),
                    "the failure comes out the other side of the"
                            + " restoration, not swallowed by it");
            assertArrayEquals(new byte[] {1, 2, 3},
                    Files.readAllBytes(inspection),
                    "inspection drift went back to committed bytes");
            assertFalse(Files.exists(
                            root.resolve("controls-stray.png")),
                    "the inspection stray is gone");
            assertTrue(Files.exists(root.resolve("page-stray.png")),
                    "the renderer stray is left for its named breach"
                            + " - restoration never deletes what a"
                            + " human must rule on");
            assertArrayEquals(new byte[] {4, 5, 6},
                    Files.readAllBytes(renderer),
                    "and the renderer file is untouched");
        } finally {
            try (var tree = Files.walk(root)) {
                for (Path file : tree.sorted(
                        java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    @Test
    void aFailingRestorationRidesSuppressedBehindTheEvidenceFailure()
            throws Exception {
        // The standing suppression rule, held on the verifier's own
        // outer path (review): both halves fail, the evidence
        // failure is thrown, the cleanup's trouble is attached.
        Path root = Files.createTempDirectory("evidence-suppress");
        Path inspection = root.resolve("controls-x.png");
        Files.write(inspection, new byte[] {1});
        var committed = EvidenceContractMain.snapshot(root);
        // Restoration will fail: the tree it must walk is gone.
        try (var tree = Files.walk(root)) {
            for (Path f : tree.sorted(
                    java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(f);
            }
        }
        IllegalStateException primary =
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalStateException.class, () ->
                EvidenceContractMain.generateUnderRestoration(root,
                        committed, () -> {
                            throw new IllegalStateException(
                                    "the evidence failure");
                        }));
        assertEquals("the evidence failure", primary.getMessage(),
                "cleanup trouble must not replace the failure a"
                        + " maintainer needs to see");
        assertTrue(primary.getSuppressed().length >= 1,
                "and the restoration's own trouble rides suppressed: "
                        + java.util.Arrays.toString(
                                primary.getSuppressed()));
    }

    @Test
    void aCleanupFailureAloneStillSurfaces() throws Exception {
        // The primary==null path (review): a healthy generation
        // whose restoration then fails must not exit quietly.
        Path root = Files.createTempDirectory("evidence-cleanup-only");
        Files.write(root.resolve("controls-x.png"), new byte[] {1});
        var committed = EvidenceContractMain.snapshot(root);
        try (var tree = Files.walk(root)) {
            for (Path f : tree.sorted(
                    java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(f);
            }
        }
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> EvidenceContractMain.generateUnderRestoration(
                        root, committed, () -> { }),
                "a restoration that fails after a clean generation is"
                        + " a failure, not a shrug");
    }

    @Test
    void theResiduePinJudgesAllFourBranches() {
        String pinned = EvidenceContractMain
                .PROMOTED_WITHOUT_GENERATOR.get(0);
        assertEquals(List.of(),
                EvidenceContractMain.promotedPinBreaches(pinned, false),
                "a pinned file with no match is the recorded residue");
        assertEquals(1, EvidenceContractMain
                        .promotedPinBreaches(pinned, true).size(),
                "a pinned file that gains a match is a stale pin");
        assertEquals(1, EvidenceContractMain.promotedPinBreaches(
                        "docs/studies/bayer-notation/handmade.png",
                        false).size(),
                "an unmatched file outside the pin is a breach, never"
                        + " a silent baseline");
        assertEquals(List.of(),
                EvidenceContractMain.promotedPinBreaches(
                        "docs/studies/bayer-notation/ordinary.png",
                        true),
                "and a matched unpinned file is simply compared");

        List<String> allVanished = EvidenceContractMain
                .stalePinBreaches(java.util.Set.of());
        assertEquals(EvidenceContractMain.PROMOTED_WITHOUT_GENERATOR
                        .size(), allVanished.size(),
                "every vanished pinned entry is its own breach");
        for (int i = 0; i < allVanished.size(); i++) {
            assertTrue(allVanished.get(i).startsWith(
                            EvidenceContractMain
                                    .PROMOTED_WITHOUT_GENERATOR.get(i))
                            && allVanished.get(i).contains(
                                    "missing from the tree"),
                    "and names its own file: " + allVanished.get(i));
        }
        assertEquals(List.of(), EvidenceContractMain.stalePinBreaches(
                        new java.util.TreeSet<>(EvidenceContractMain
                                .PROMOTED_WITHOUT_GENERATOR)),
                "and a tree holding every pinned entry is clean");
    }

    @Test
    void theRealGatesArePinnedAndFailLoudlyWithTheirExactCommands() {
        // The REAL configuration, not one the test built for itself
        // (review): removing either gate from the verifier fails
        // here, and each breach message carries that gate's own
        // fetch command.
        var gates = EvidenceContractMain.GATED_GENERATORS;
        assertEquals(2, gates.size(),
                "exactly the two gated families the verifier found"
                        + " the hard way");
        var constellation = gates.get(
                "juranometria.tool.ConstellationStudyMain");
        assertEquals(Path.of("imports/raw/constellations"),
                constellation.input());
        assertEquals("scripts/download-constellation-sources.sh",
                constellation.fetch());
        var stars = gates.get(
                "juranometria.tool.StarIdentityStudyMain");
        assertEquals(Path.of("imports/raw/star-identities"),
                stars.input());
        assertTrue(stars.fetch().contains(
                        "scripts/download-constellation-sources.sh")
                        && stars.fetch().contains(
                                "scripts/download-catalogue-sources.sh"),
                "the star-identity family names both of its fetch"
                        + " commands: " + stars.fetch());

        for (var gate : gates.values()) {
            String breach = EvidenceContractMain.incompleteBreach(
                    "build/x", gate, false);
            assertTrue(breach.contains("VERIFICATION INCOMPLETE")
                            && breach.contains(gate.fetch()),
                    "absent inputs fail loudly with the exact"
                            + " command: " + breach);
            assertEquals(null, EvidenceContractMain.incompleteBreach(
                            "build/x", gate, true),
                    "and present inputs gate nothing");
        }
    }

    // ---- inspection imagery, by structure ----------------------------

    private static BufferedImage png(String path) throws Exception {
        BufferedImage image = ImageIO.read(Path.of(path).toFile());
        assertTrue(image != null, path + " decodes as an image");
        return image;
    }

    /** Mean luminance of the corner pixels, 0 dark to 255 light. */
    private static double cornerLuminance(BufferedImage image) {
        int[][] corners = {{2, 2},
                {image.getWidth() - 3, 2},
                {2, image.getHeight() - 3},
                {image.getWidth() - 3, image.getHeight() - 3}};
        double sum = 0;
        for (int[] corner : corners) {
            int rgb = image.getRGB(corner[0], corner[1]);
            sum += (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                    + (rgb & 0xff)) / 3.0;
        }
        return sum / corners.length;
    }

    @Test
    void theSessionPhotographsHoldTheirStructure() throws Exception {
        BufferedImage plain = png(
                "docs/studies/place-and-time/dialog-real.png");
        BufferedImage dark = png(
                "docs/studies/place-and-time/dialog-real-dark.png");
        BufferedImage enlarged = png(
                "docs/studies/place-and-time/dialog-real-enlarged.png");

        assertTrue(cornerLuminance(plain) > 160,
                "the plain photograph wears the light theme");
        assertTrue(cornerLuminance(dark) < 96,
                "the dark photograph wears the dark theme - a light"
                        + " image substituted under its name fails"
                        + " here");
        assertTrue(enlarged.getWidth() > plain.getWidth(),
                "enlarged text needs a wider packed dialog: "
                        + enlarged.getWidth() + " over "
                        + plain.getWidth());
        assertEquals(plain.getWidth(), dark.getWidth(),
                "theme changes the clothes, not the size");
        assertFalse(java.util.Arrays.equals(
                        Files.readAllBytes(Path.of(
                                "docs/studies/place-and-time/dialog-real.png")),
                        Files.readAllBytes(Path.of(
                                "docs/studies/place-and-time/dialog-real-dark.png"))),
                "and the variants really are different pictures");
        // What the pixels cannot prove - the right dialog, its
        // controls unclipped - the lifecycle test proves against the
        // packed production window itself; these checks keep the
        // committed illustrations from silently becoming something
        // else.
    }

    @Test
    void theWidgetRenderedInspectionSetsHoldTheirStructure()
            throws Exception {
        BufferedImage controls = png(
                "docs/studies/place-and-time/controls-dialog.png");
        BufferedImage controlsDark = png(
                "docs/studies/place-and-time/controls-dialog-dark.png");
        BufferedImage controlsEnlarged = png(
                "docs/studies/place-and-time/controls-dialog-enlarged.png");
        BufferedImage sidebar = png(
                "docs/studies/place-and-time/controls-sidebar-240.png");

        assertTrue(cornerLuminance(controls) > 160
                        && cornerLuminance(controlsDark) < 96,
                "the mock-up pair wears its two themes");
        assertTrue(controlsEnlarged.getWidth() > controls.getWidth(),
                "the enlarged mock-up is wider");
        assertEquals(240, sidebar.getWidth(),
                "the 240 px floor mock-up is the width its name"
                        + " claims");

        for (String tab : List.of("deep-sky-tab", "deep-sky-tab-dark",
                "deep-sky-tab-large-text")) {
            assertTrue(Files.exists(Path.of(
                            "docs/studies/deep-sky-vocabulary",
                            tab + ".png")),
                    "the vocabulary study's named artifact exists: "
                            + tab);
        }
        assertTrue(cornerLuminance(png(
                        "docs/studies/deep-sky-vocabulary/deep-sky-tab-dark.png"))
                        < 96,
                "and its dark variant is dark");
    }
}
