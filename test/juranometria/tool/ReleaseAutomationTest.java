package juranometria.tool;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The release automation of issue #88, proved without publishing
 * anything: version and changelog extraction, artifact naming and
 * agreement, checksums, release-note assembly, and - the part that
 * matters most - that every disagreement fails BEFORE a release
 * could exist.
 *
 * The scripts are the same ones the workflow runs, so this is a
 * rehearsal of the real path rather than a model of it.
 */
class ReleaseAutomationTest {

    private record Run(int status, String output) {
    }

    private static Run run(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        return new Run(process.waitFor(), output);
    }

    private static Run metadata(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "scripts/release-metadata.sh";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        return run(command);
    }

    /** A checkout carrying just what the release scripts read. */
    private static Path tree(String version, String changelog)
            throws IOException {
        Path root = Files.createTempDirectory("release-tree");
        Files.writeString(root.resolve("VERSION"), version + "\n");
        Files.writeString(root.resolve("CHANGELOG.md"), changelog);
        return root;
    }

    private static String changelogWith(String version, String body) {
        return "# Changelog\n\n## [Unreleased]\n\n## [" + version
                + "] - 2026-09-01\n\n" + body
                + "\n## [0.1.0] - 2026-08-28\n\n- The first chart.\n";
    }

    @Test
    void theReleasedTreeAgreesWithItsOwnTag() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-metadata.sh")));
        String version = Files.readString(Path.of("VERSION")).trim();

        Run agreed = metadata("check", "v" + version, ".");

        assertEquals(0, agreed.status(),
                "the repository as it stands must be releasable: "
                        + agreed.output());
    }

    @Test
    void everyDisagreementFailsBeforeAnythingCouldBePublished()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-metadata.sh")));
        Path tree = tree("1.2.3", changelogWith("1.2.3", "- Something.\n"));
        try {
            // A tag that is not a version at all.
            for (String malformed : List.of("v1.2", "1.2.3", "v1.2.3.4",
                    "vX.Y.Z", "v1.2.3-rc1", "release-1.2.3")) {
                Run run = metadata("check", malformed, tree.toString());
                assertEquals(2, run.status(),
                        "'" + malformed + "' must be refused: "
                                + run.output());
            }

            // A tag the tree does not claim: the classic mis-tag.
            Run mismatch = metadata("check", "v1.2.4", tree.toString());
            assertEquals(3, mismatch.status(), mismatch.output());
            assertTrue(mismatch.output().contains("VERSION 1.2.3"),
                    "naming both sides: " + mismatch.output());

            // A version with no changelog section, and one whose
            // section exists but says nothing.
            Path unlogged = tree("1.2.3",
                    "# Changelog\n\n## [Unreleased]\n\n"
                            + "## [1.0.0] - 2026-08-01\n\n- Older.\n");
            Run missing = metadata("check", "v1.2.3", unlogged.toString());
            assertEquals(4, missing.status(), missing.output());

            // A heading without its date is an unfinished release
            // note, and a loose match would also accept a NEIGHBOUR
            // version's section (automation review).
            Path undated = tree("1.2.3",
                    "# Changelog\n\n## [1.2.3]\n\n- Something.\n");
            Run notDated = metadata("check", "v1.2.3", undated.toString());
            assertEquals(4, notDated.status(), notDated.output());
            assertTrue(notDated.output().contains("not dated"),
                    "and says which of the two problems it is: "
                            + notDated.output());

            Path neighbour = tree("1.2.3",
                    "# Changelog\n\n## [1.2.30] - 2026-09-01\n\n"
                            + "- A different version entirely.\n");
            Run wrongSection = metadata("check", "v1.2.3",
                    neighbour.toString());
            assertEquals(4, wrongSection.status(),
                    "1.2.30's section is not 1.2.3's: "
                            + wrongSection.output());
            cleanUp(undated);
            cleanUp(neighbour);

            Path empty = tree("1.2.3", changelogWith("1.2.3", "\n"));
            Run blank = metadata("check", "v1.2.3", empty.toString());
            assertEquals(4, blank.status(),
                    "a section of blank lines is no section: "
                            + blank.output());

            // Not a checkout at all.
            Run elsewhere = metadata("check", "v1.2.3",
                    Files.createTempDirectory("empty").toString());
            assertEquals(5, elsewhere.status(), elsewhere.output());
            cleanUp(unlogged);
            cleanUp(empty);
        } finally {
            cleanUp(tree);
        }
    }

    @Test
    void theNotesAreTheChangelogSectionPlusWhatEveryReleaseMustSay()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-metadata.sh")));
        Path tree = tree("1.2.3", changelogWith("1.2.3",
                "### Added\n\n- The lettered sky.\n\n"
                        + "### Fixed\n\n- A damaged download says so.\n"));
        try {
            Run notes = metadata("notes", "v1.2.3", tree.toString());
            assertEquals(0, notes.status(), notes.output());
            String text = notes.output();

            assertTrue(text.contains("The lettered sky.")
                            && text.contains("A damaged download says so."),
                    "the release says what the changelog says: " + text);
            assertFalse(text.contains("The first chart."),
                    "and only this version's section: " + text);
            assertFalse(text.contains("[Unreleased]"));

            for (String artifact : List.of("macos-arm64", "macos-x64",
                    "windows-x64", "linux-x64", "portable")) {
                assertTrue(text.contains(
                                "JUranometria-1.2.3-" + artifact + ".zip"),
                        "every download is named, at this version: "
                                + artifact);
            }
            assertTrue(text.contains("SHA256SUMS.txt"));
            assertTrue(text.contains("CC BY-NC 3.0 IGO")
                            && text.contains("non-commercially only"),
                    "the non-commercial consequence travels with every"
                            + " release: " + text);
            assertTrue(text.contains("Classpath Exception"),
                    "as does the bundled runtime's licence");
            assertTrue(text.contains("Gatekeeper")
                            && text.contains("SmartScreen"),
                    "and the unsigned reality");
            assertTrue(text.contains("network requests of any kind"),
                    "and the offline promise");
        } finally {
            cleanUp(tree);
        }
    }

    @Test
    void theFiveArtifactsAreCheckedAndSummedOrTheReleaseStops()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-artifacts.sh")));
        Path staging = Files.createTempDirectory("release-artifacts");
        try {
            for (String cell : List.of("macos-arm64", "macos-x64",
                    "windows-x64", "linux-x64")) {
                image(staging.resolve("JUranometria-1.2.3-" + cell + ".zip"),
                        "1.2.3");
            }
            portable(staging.resolve("JUranometria-1.2.3-portable.zip"),
                    "1.2.3");

            Run good = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(0, good.status(), good.output());

            Path sums = staging.resolve("SHA256SUMS.txt");
            assertTrue(Files.exists(sums), "checksums are written");
            List<String> lines = Files.readAllLines(sums);
            assertEquals(5, lines.size(), "one line per artifact");
            for (String line : lines) {
                assertTrue(line.matches("^[0-9a-f]{64}\\s+\\*?"
                                + "JUranometria-1\\.2\\.3-[a-z0-9-]+\\.zip$"),
                        "a real digest beside a contract name: " + line);
            }

            // A cell that failed to build must stop the release.
            Files.delete(staging.resolve(
                    "JUranometria-1.2.3-windows-x64.zip"));
            Run incomplete = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(6, incomplete.status(),
                    "a partial set is never published: "
                            + incomplete.output());
            assertTrue(incomplete.output().contains("windows-x64"),
                    incomplete.output());

            // Something that is not part of the contract must not ride
            // along unnoticed.
            image(staging.resolve("JUranometria-1.2.3-windows-x64.zip"),
                    "1.2.3");
            Files.writeString(staging.resolve("extra.zip"), "not ours");
            Run stray = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(6, stray.status(), stray.output());
            Files.delete(staging.resolve("extra.zip"));

            // A stray whose name is a SUBSTRING of a real one: the
            // membership test must be exact, not "contained in"
            // (automation review).
            Files.copy(staging.resolve("JUranometria-1.2.3-portable.zip"),
                    staging.resolve("ia-1.2.3-portable.zip"));
            Run substring = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(6, substring.status(),
                    "a name that is merely part of an expected name is"
                            + " still a stray: " + substring.output());
            Files.delete(staging.resolve("ia-1.2.3-portable.zip"));

            // An archive built from the wrong tree: right name, wrong
            // contents. This is the one a human eye would miss.
            image(staging.resolve("JUranometria-1.2.3-linux-x64.zip"),
                    "1.2.2");
            Run mismatched = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(7, mismatched.status(),
                    "an artifact must carry the version it is named"
                            + " for: " + mismatched.output());
            assertTrue(mismatched.output().contains("linux-x64"),
                    mismatched.output());

            // A version that merely STARTS with the one being
            // released: 1.2.30 is not 1.2.3.
            image(staging.resolve("JUranometria-1.2.3-linux-x64.zip"),
                    "1.2.30");
            Run prefix = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(7, prefix.status(),
                    "a longer version must not satisfy a shorter one: "
                            + prefix.output());
            assertTrue(prefix.output().contains("'1.2.30'"),
                    "naming what it actually found: " + prefix.output());

            // A decoy build-info.txt elsewhere in the archive must
            // not answer on the real one's behalf.
            decoyed(staging.resolve("JUranometria-1.2.3-linux-x64.zip"),
                    "1.2.2", "1.2.3");
            Run decoy = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString());
            assertEquals(7, decoy.status(),
                    "two build-info.txt files are an archive that"
                            + " cannot identify itself: " + decoy.output());

            // And a portable archive whose directory merely starts
            // with the released version.
            image(staging.resolve("JUranometria-1.2.3-linux-x64.zip"),
                    "1.2.3");
            portable(staging.resolve("JUranometria-1.2.3-portable.zip"),
                    "1.2.30");
            Run portablePrefix = run("scripts/release-artifacts.sh",
                    "1.2.3", staging.toString());
            assertEquals(7, portablePrefix.status(),
                    "JUranometria-1.2.30/ is not JUranometria-1.2.3/: "
                            + portablePrefix.output());
        } finally {
            cleanUp(staging);
        }
    }

    @Test
    void aReleaseIsPublishedOnlyFromAnAnnotatedTagOnMergedWork()
            throws Exception {
        // Provenance, on real repositories (automation review):
        // version agreement says nothing about where a commit came
        // from, and an annotated tag on an unmerged branch would
        // otherwise publish work no review ever saw.
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-provenance.sh")));
        Path repo = Files.createTempDirectory("release-provenance");
        try {
            git(repo, "init", "-q", "-b", "main");
            git(repo, "config", "user.email", "test@example.invalid");
            git(repo, "config", "user.name", "Release Test");
            Files.writeString(repo.resolve("VERSION"), "1.2.3\n");
            git(repo, "add", "-A");
            git(repo, "commit", "-q", "-m", "release 1.2.3");
            String merged = capture(repo, "rev-parse", "HEAD");

            // The good case: annotated, pointing here, on main.
            git(repo, "tag", "-a", "v1.2.3", "-m", "JUranometria 1.2.3");
            assertEquals(0, provenance(repo, "v1.2.3", merged).status());

            // A lightweight tag is not a release tag.
            git(repo, "tag", "v1.2.4-light");
            Run light = provenance(repo, "v1.2.4-light", merged);
            assertEquals(8, light.status(), light.output());
            assertTrue(light.output().contains("annotated"),
                    light.output());

            // An annotated tag on work that never reached main: the
            // blocker this test exists for.
            git(repo, "checkout", "-q", "-b", "side");
            Files.writeString(repo.resolve("VERSION"), "1.2.4\n");
            git(repo, "commit", "-qam", "unmerged work");
            String unmerged = capture(repo, "rev-parse", "HEAD");
            git(repo, "tag", "-a", "v1.2.4", "-m", "JUranometria 1.2.4");
            Run unreviewed = provenance(repo, "v1.2.4", unmerged);
            assertEquals(10, unreviewed.status(),
                    "an unmerged commit must never be published: "
                            + unreviewed.output());
            assertTrue(unreviewed.output().contains("not on main"),
                    unreviewed.output());

            // A tag force-moved after the build started: the same
            // check, run again before publishing, must refuse.
            git(repo, "checkout", "-q", "main");
            Files.writeString(repo.resolve("VERSION"), "1.2.5\n");
            git(repo, "commit", "-qam", "later work on main");
            git(repo, "tag", "-f", "-a", "v1.2.3", "-m", "moved");
            Run moved = provenance(repo, "v1.2.3", merged);
            assertEquals(9, moved.status(),
                    "artifacts built for one commit are never"
                            + " published under a tag that now names"
                            + " another: " + moved.output());
            assertTrue(moved.output().contains("the tag moved"),
                    moved.output());

            Run absent = provenance(repo, "v9.9.9", merged);
            assertEquals(8, absent.status(), absent.output());
        } finally {
            cleanUp(repo);
        }
    }

    private static Run provenance(Path repo, String tag, String sha)
            throws Exception {
        return run(Path.of("scripts/release-provenance.sh").toAbsolutePath()
                        .toString(), tag, sha, repo.toString(), "main");
    }

    private static void git(Path repo, String... arguments)
            throws Exception {
        String[] command = new String[arguments.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = repo.toString();
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        Run run = run(command);
        assertEquals(0, run.status(),
                String.join(" ", command) + ": " + run.output());
    }

    private static String capture(Path repo, String... arguments)
            throws Exception {
        String[] command = new String[arguments.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = repo.toString();
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        return run(command).output().trim();
    }

    @Test
    void aVersionHasExactlyOneChangelogSection() throws Exception {
        // Two dated sections for one version used to be silently
        // combined, publishing notes nobody wrote (automation
        // review).
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-metadata.sh")));
        Path tree = tree("1.2.3",
                "# Changelog\n\n## [1.2.3] - 2026-09-01\n\n- First.\n\n"
                        + "## [1.2.3] - 2026-09-02\n\n- Second.\n");
        try {
            Run duplicated = metadata("check", "v1.2.3", tree.toString());
            assertEquals(4, duplicated.status(), duplicated.output());
            assertTrue(duplicated.output().contains("2 sections"),
                    "counted and named: " + duplicated.output());
        } finally {
            cleanUp(tree);
        }
    }

    /** An application image: a build-info.txt is what states its version. */
    private static void image(Path zip, String version) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(
                    "JUranometria.app/Contents/app/build-info.txt"));
            zos.write(("JUranometria " + version
                    + " (app-image; jpackage version label 1.0.0)\n"
                    + "packager: 21.0.12.1\n")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }



    // ------------------------------------------------------------------
    // The duplicate delivery of issue #195. GitHub delivered the
    // v1.3.0 tag push twice, a second apart; the loser refused to
    // publish and left a red X on a release that was entirely
    // correct. These fixtures are that incident: the four native
    // checksums below are the REAL ones the two runs produced, and
    // they differ, because jpackage does not build byte-identically
    // across runners. Only the portable archive - built by `make
    // dist` with one fixed timestamp, sorted entries and zip -X -
    // came out identical, which is why it and not the set carries
    // the identity claim.

    /** What run 33548294309 staged, and could not publish. */
    private static final String[] MINE_NATIVE = {
            "26624cffd34e7cea65b4b0e8ac6f7efaa792b1fb37d34691e4f9b7cec4b2910d",
            "2335d53a12b5b48106b6740ac947ba88eed4260150adc75c70051f7d4164fed6",
            "2ca329b0542d7d7298b349b8b845e61a0719e000eda7bcea462f1a80aecae16e",
            "1678b265b643b55dc3c71f4c1ef5ee67688589cfd17ceb276bc39f8a6a108c17"};

    /** What run 33548293832 published one second earlier. */
    private static final String[] THEIRS_NATIVE = {
            "205508c9c5d99af3ad8ca6b1eff3e41efb6b5390a7353ad04c67ebfacb23c5af",
            "66ffd48d8e35918c516cddcfda2c8a554215154caf5b89235487daa2d6a79aa2",
            "b05e58bef507aa9eb829dd7e3b9040fe05ce03e25a9ccbd44a87534ef1a9edb3",
            "9e1abfe96698e1df4cc997670c351561e840f1b42797afb1a10e9eda39000d6b"};

    private static final String[] CELLS = {
            "macos-arm64", "macos-x64", "windows-x64", "linux-x64"};

    private static final String V = "1.3.0";

    private static String archive(String cell) {
        return "JUranometria-" + V + "-" + cell + ".zip";
    }

    private static String sha256(Path file) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String manifest(String[] natives, String portableSum) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < CELLS.length; i++) {
            text.append(natives[i]).append("  ")
                    .append(archive(CELLS[i])).append("\n");
        }
        return text.append(portableSum).append("  ")
                .append(archive("portable")).append("\n").toString();
    }

    /**
     * The whole situation on disk: what this run staged, and what
     * the workflow read back from the release that beat it. Returns
     * the root; staging and published sit inside it.
     */
    private static Path delivery(String[] theirNatives,
                                 boolean sameSource) throws Exception {
        Path root = Files.createTempDirectory("duplicate");
        Path staging = Files.createDirectory(root.resolve("staging"));
        Path published = Files.createDirectory(root.resolve("published"));

        // The one archive that is reproducible by construction. The
        // published copy is a real file, hashed from its bytes.
        portable(published.resolve(archive("portable")), V);
        String theirs = sha256(published.resolve(archive("portable")));
        String mine = sameSource ? theirs
                : "0000000000000000000000000000000000000000000000000000000000000000";

        Files.writeString(staging.resolve("SHA256SUMS.txt"),
                manifest(MINE_NATIVE, mine));
        Files.writeString(published.resolve("SHA256SUMS.txt"),
                manifest(theirNatives, theirs));

        StringBuilder assets = new StringBuilder();
        for (String cell : CELLS) {
            assets.append(archive(cell)).append("\n");
        }
        assets.append(archive("portable")).append("\n")
                .append("SHA256SUMS.txt\n");
        Files.writeString(published.resolve("assets.txt"), assets.toString());
        return root;
    }

    private static Run duplicate(Path root) throws Exception {
        return run("scripts/release-duplicate.sh", V,
                root.resolve("staging").toString(),
                root.resolve("published").toString());
    }

    @Test
    void aDuplicateDeliveryOfOneTagPushFinishesWithNothingToDo()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        Path root = delivery(THEIRS_NATIVE, true);
        try {
            // The premise, and the reason the obvious check fails:
            // this is a correct release whose four native archives
            // genuinely do not match what this run built.
            for (int i = 0; i < CELLS.length; i++) {
                assertFalse(MINE_NATIVE[i].equals(THEIRS_NATIVE[i]),
                        CELLS[i] + " must differ between the two runs,"
                                + " or this fixture is not the incident");
            }

            Run verdict = duplicate(root);

            assertEquals(0, verdict.status(),
                    "a duplicate delivery must finish green: "
                            + verdict.output());
            assertTrue(verdict.output().contains("already released"),
                    verdict.output());
            assertTrue(verdict.output().contains("Nothing is uploaded"),
                    "it must say it changed nothing: " + verdict.output());
            assertTrue(verdict.output().contains("NOT compared"),
                    "it must state what it did not compare: "
                            + verdict.output());
        } finally {
            cleanUp(root);
        }
    }

    @Test
    void aReleaseBuiltFromOtherSourceStillFailsLoudly() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        // Everything else identical to the benign case; only the
        // reproducible archive disagrees. If this passed, the guard
        // would have been traded away rather than sharpened.
        Path root = delivery(THEIRS_NATIVE, false);
        try {
            Run verdict = duplicate(root);

            assertEquals(8, verdict.status(),
                    "a release this run did not build must fail: "
                            + verdict.output());
            assertTrue(verdict.output().contains("different source"),
                    verdict.output());
            assertTrue(verdict.output().contains("portable"),
                    "it must name what differs: " + verdict.output());
            assertTrue(verdict.output().contains("Nothing was uploaded"),
                    "even failing, it must say nothing was touched: "
                            + verdict.output());
        } finally {
            cleanUp(root);
        }
    }

    @Test
    void everyBrokenPublishedReleaseIsAConflictRatherThanADuplicate()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        // Each of these starts from the benign fixture and breaks
        // exactly one thing, so the failure is attributable.
        for (String what : List.of("missing-asset", "extra-asset",
                "manifest-omits-an-archive", "manifest-lies",
                "portable-not-downloaded", "no-manifest")) {
            Path root = delivery(THEIRS_NATIVE, true);
            try {
                Path published = root.resolve("published");
                Path assets = published.resolve("assets.txt");
                Path theirs = published.resolve("SHA256SUMS.txt");
                switch (what) {
                    case "missing-asset" -> Files.writeString(assets,
                            Files.readString(assets)
                                    .replace(archive("linux-x64") + "\n", ""));
                    case "extra-asset" -> Files.writeString(assets,
                            Files.readString(assets) + "JUranometria.exe\n");
                    case "manifest-omits-an-archive" -> Files.writeString(
                            theirs, Files.readString(theirs)
                                    .replace(archive("windows-x64"),
                                            "something-else.zip"));
                    case "manifest-lies" -> Files.writeString(theirs,
                            manifest(THEIRS_NATIVE, "dead"
                                    + "beef".repeat(14)));
                    case "portable-not-downloaded" -> Files.delete(
                            published.resolve(archive("portable")));
                    case "no-manifest" -> Files.delete(theirs);
                    default -> throw new IllegalStateException(what);
                }

                Run verdict = duplicate(root);

                assertEquals(8, verdict.status(),
                        what + " must be reported as a conflict: "
                                + verdict.output());
                assertTrue(verdict.output().contains("DIFFERENT release"),
                        what + ": " + verdict.output());
            } finally {
                cleanUp(root);
            }
        }
    }

    @Test
    void aRunThatStagedNothingIsItsOwnBugRatherThanAConflict()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        Path root = delivery(THEIRS_NATIVE, true);
        try {
            Files.delete(root.resolve("staging/SHA256SUMS.txt"));

            Run verdict = duplicate(root);

            // Distinguished deliberately: 9 says this run is broken,
            // 8 says the published release is. Reporting a local bug
            // as a conflict would send a reader to the wrong place.
            assertEquals(9, verdict.status(), verdict.output());
            assertFalse(verdict.output().contains("DIFFERENT release"),
                    verdict.output());
        } finally {
            cleanUp(root);
        }
    }

    /**
     * An image carrying the real build-info.txt plus a decoy
     * elsewhere: reading every match at once would let the decoy
     * satisfy the version check.
     */
    private static void decoyed(Path zip, String real, String decoy)
            throws IOException {
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(
                    "JUranometria/app/build-info.txt"));
            zos.write(("JUranometria " + real + " (app-image)\n")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(
                    "JUranometria/runtime/legal/build-info.txt"));
            zos.write(("JUranometria " + decoy + " (app-image)\n")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /** The portable archive: the directory the reader unpacks names it. */
    private static void portable(Path zip, String version)
            throws IOException {
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("JUranometria-" + version + "/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(
                    "JUranometria-" + version + "/JUranometria.jar"));
            zos.write("not really a jar".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private static void cleanUp(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var tree = Files.walk(directory)) {
            for (Path path : tree.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
