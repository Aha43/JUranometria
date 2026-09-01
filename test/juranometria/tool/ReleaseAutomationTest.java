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
                    staging.toString(), COMMIT);
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
                    staging.toString(), COMMIT);
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
                    staging.toString(), COMMIT);
            assertEquals(6, stray.status(), stray.output());
            Files.delete(staging.resolve("extra.zip"));

            // A stray whose name is a SUBSTRING of a real one: the
            // membership test must be exact, not "contained in"
            // (automation review).
            Files.copy(staging.resolve("JUranometria-1.2.3-portable.zip"),
                    staging.resolve("ia-1.2.3-portable.zip"));
            Run substring = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString(), COMMIT);
            assertEquals(6, substring.status(),
                    "a name that is merely part of an expected name is"
                            + " still a stray: " + substring.output());
            Files.delete(staging.resolve("ia-1.2.3-portable.zip"));

            // An archive built from the wrong tree: right name, wrong
            // contents. This is the one a human eye would miss.
            image(staging.resolve("JUranometria-1.2.3-linux-x64.zip"),
                    "1.2.2");
            Run mismatched = run("scripts/release-artifacts.sh", "1.2.3",
                    staging.toString(), COMMIT);
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
                    staging.toString(), COMMIT);
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
                    staging.toString(), COMMIT);
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
                    "1.2.3", staging.toString(), COMMIT);
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
                    + "source: " + COMMIT + "\n"
                    + "packager: 21.0.12.1\n")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }



    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    // The duplicate delivery of issue #195. GitHub delivered the
    // v1.3.0 tag push twice, a second apart; the loser refused to
    // publish and left a red X on a release that was entirely
    // correct. These fixtures are that incident: the four native
    // archives genuinely differ between the two runs, because
    // jpackage does not build byte-identically across runners.
    //
    // So three things stand in for a byte comparison, and each of
    // them is proved here to be load-bearing: every published
    // archive is hashed against the published manifest, every image
    // must record this run's own source commit, and the portable
    // archive - which `make dist` does build reproducibly - must
    // match exactly.

    private static final String CELLS_V = "1.3.0";

    private static final String[] CELLS = {
            "macos-arm64", "macos-x64", "windows-x64", "linux-x64"};

    /** The commit this run believes it is publishing. */
    private static final String COMMIT =
            "9f2c1ab4d5e6708192a3b4c5d6e7f8091a2b3c4d";

    private static String archive(String cell) {
        return "JUranometria-" + CELLS_V + "-" + cell + ".zip";
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

    /**
     * An application image that records the source commit it was
     * packaged from. The filler makes two builds of the same commit
     * differ in bytes, which is what the real images do.
     */
    private static void imageFrom(Path zip, String commit, String filler)
            throws IOException {
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(
                    "JUranometria.app/Contents/app/build-info.txt"));
            zos.write(("JUranometria " + CELLS_V + " (app-image)\n"
                    + (commit.isEmpty() ? "" : "source: " + commit + "\n")
                    + "packager: 21.0.12.1\n")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(
                    "JUranometria.app/Contents/runtime/release"));
            zos.write(filler.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /** The checksums a release publishes over an actual directory. */
    private static void writeManifest(Path directory) throws Exception {
        StringBuilder text = new StringBuilder();
        for (String cell : CELLS) {
            text.append(sha256(directory.resolve(archive(cell))))
                    .append("  ").append(archive(cell)).append("\n");
        }
        text.append(sha256(directory.resolve(archive("portable"))))
                .append("  ").append(archive("portable")).append("\n");
        Files.writeString(directory.resolve("SHA256SUMS.txt"),
                text.toString());
    }

    /**
     * The whole situation on disk: what this run staged, and what
     * the workflow downloaded from the release that beat it to the
     * tag. The two sides carry genuinely different image bytes, as
     * two runs of one commit really do.
     */
    private static Path delivery() throws Exception {
        Path root = Files.createTempDirectory("duplicate");
        Path staging = Files.createDirectory(root.resolve("staging"));
        Path published = Files.createDirectory(root.resolve("published"));
        for (Path side : List.of(staging, published)) {
            String filler = side == staging ? "runner A" : "runner B";
            for (String cell : CELLS) {
                imageFrom(side.resolve(archive(cell)), COMMIT,
                        filler + " " + cell);
            }
            // Reproducible by construction, so both sides are equal.
            portable(side.resolve(archive("portable")), CELLS_V);
            writeManifest(side);
        }
        StringBuilder assets = new StringBuilder();
        for (String cell : CELLS) {
            assets.append(archive(cell)).append("\n");
        }
        assets.append(archive("portable")).append("\n")
                .append("SHA256SUMS.txt\n");
        Files.writeString(published.resolve("assets.txt"),
                assets.toString());
        return root;
    }

    private static Run duplicate(Path root) throws Exception {
        return run("scripts/release-duplicate.sh", CELLS_V,
                root.resolve("staging").toString(),
                root.resolve("published").toString(), COMMIT);
    }

    @Test
    void theStagedSetMustRecordTheSourceCommitItWasBuiltFrom()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-artifacts.sh")));
        // The duplicate guard compares published releases by the
        // commit each image records. A field nothing asserts is a
        // field that quietly stops being written, so the publishing
        // side requires it too.
        Path staged = Files.createTempDirectory("staged");
        try {
            for (String cell : CELLS) {
                imageFrom(staged.resolve(archive(cell)), COMMIT, cell);
            }
            portable(staged.resolve(archive("portable")), CELLS_V);

            Run agreed = run("scripts/release-artifacts.sh", CELLS_V,
                    staged.toString(), COMMIT);
            assertEquals(0, agreed.status(),
                    "images recording this commit must pass: "
                            + agreed.output());

            // The commit is REQUIRED, not merely honoured when
            // supplied (#195 follow-up review). A verifier that
            // skips the check when an argument is omitted is a
            // verifier whose check will eventually be skipped, so
            // omitting it must be refused outright rather than
            // quietly verifying less.
            Run omitted = run("scripts/release-artifacts.sh", CELLS_V,
                    staged.toString());
            assertEquals(64, omitted.status(),
                    "omitting the commit must be a usage error, never"
                            + " a silent pass: " + omitted.output());
            assertTrue(omitted.output().contains("<commit>"),
                    omitted.output());

            // One image rebuilt from another tree, everything else
            // untouched.
            imageFrom(staged.resolve(archive("windows-x64")),
                    "0000000000000000000000000000000000000000",
                    "windows-x64");
            Run refused = run("scripts/release-artifacts.sh", CELLS_V,
                    staged.toString(), COMMIT);
            assertEquals(7, refused.status(),
                    "a foreign source commit must stop the release: "
                            + refused.output());
            assertTrue(refused.output().contains(archive("windows-x64")),
                    refused.output());

            // And an image predating the field at all.
            imageFrom(staged.resolve(archive("windows-x64")), "",
                    "windows-x64");
            Run absent = run("scripts/release-artifacts.sh", CELLS_V,
                    staged.toString(), COMMIT);
            assertEquals(7, absent.status(),
                    "a missing source line must fail closed: "
                            + absent.output());
        } finally {
            cleanUp(staged);
        }
    }

    @Test
    void aDuplicateDeliveryOfOneTagPushFinishesWithNothingToDo()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        Path root = delivery();
        try {
            // The premise, and the reason a byte comparison of the
            // set would be the wrong check: the images genuinely
            // differ between the two runs of one commit.
            for (String cell : CELLS) {
                assertFalse(sha256(root.resolve("staging/" + archive(cell)))
                                .equals(sha256(root.resolve(
                                        "published/" + archive(cell)))),
                        cell + " must differ between the two runs, or"
                                + " this fixture is not the incident");
            }
            assertEquals(sha256(root.resolve("staging/"
                            + archive("portable"))),
                    sha256(root.resolve("published/"
                            + archive("portable"))),
                    "the portable archive is the reproducible one");

            Run verdict = duplicate(root);

            assertEquals(0, verdict.status(),
                    "a duplicate delivery must finish green: "
                            + verdict.output());
            assertTrue(verdict.output().contains("already released"),
                    verdict.output());
            assertTrue(verdict.output().contains("Nothing is uploaded"),
                    "it must say it changed nothing: " + verdict.output());
            assertTrue(verdict.output().contains("NOT compared byte"),
                    "it must state what it did not compare: "
                            + verdict.output());
        } finally {
            cleanUp(root);
        }
    }

    @Test
    void aPublishedArchiveThatIsNotItsOwnBytesIsAConflict()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        // The case the review named: an archive substituted under
        // its own name, still a perfectly valid image recording the
        // right source commit, with the published manifest untouched.
        // Nothing but its bytes gives it away - so every archive is
        // fetched and hashed, not just the portable one.
        for (String damaged : List.of("macos-arm64", "windows-x64",
                "linux-x64", "macos-x64")) {
            Path root = delivery();
            try {
                Path file = root.resolve("published/" + archive(damaged));
                String before = sha256(file);
                imageFrom(file, COMMIT, "substituted after publication");
                assertFalse(before.equals(sha256(file)),
                        "the premise: the bytes must actually differ");

                Run verdict = duplicate(root);

                assertEquals(8, verdict.status(),
                        "a substituted " + damaged + " must be caught: "
                                + verdict.output());
                assertTrue(verdict.output().contains(archive(damaged)),
                        "it must name what differs: " + verdict.output());
                assertTrue(verdict.output().contains("hashes to"),
                        "it must report the bytes, not the name: "
                                + verdict.output());
            } finally {
                cleanUp(root);
            }
        }

        // And a truncated one, which is the same defect arriving by
        // accident rather than by hand.
        Path root = delivery();
        try {
            Files.write(root.resolve("published/" + archive("portable")),
                    "truncated".getBytes(StandardCharsets.UTF_8));

            Run verdict = duplicate(root);

            assertEquals(8, verdict.status(), verdict.output());
        } finally {
            cleanUp(root);
        }
    }

    @Test
    void aReleasePackagedFromAnotherSourceCommitIsAConflict()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        // The portable archive can be identical while the packaging
        // scripts or the bundled runtime have moved underneath it.
        // The commit each image records is what notices.
        // A wrong commit, and an image that records none at all -
        // an older build predating this line. Both fail closed.
        for (String stated : List.of(
                "0000000000000000000000000000000000000000", "")) {
            Path root = delivery();
            try {
                Path published = root.resolve("published");
                imageFrom(published.resolve(archive("linux-x64")), stated,
                        "runner B linux-x64");
                writeManifest(published);
                assertEquals(
                        sha256(root.resolve("staging/" + archive("portable"))),
                        sha256(published.resolve(archive("portable"))),
                        "the premise: the portable archive still matches");

                Run verdict = duplicate(root);

                assertEquals(8, verdict.status(),
                        "a different source commit must be caught even when"
                                + " the portable archive matches: "
                                + verdict.output());
                assertTrue(verdict.output().contains("different source"),
                        verdict.output());
                assertTrue(verdict.output().contains(archive("linux-x64")),
                        "it must name the image: " + verdict.output());
            } finally {
                cleanUp(root);
            }
        }
    }

    @Test
    void aReleaseBuiltFromOtherSourceStillFailsLoudly() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        Path root = delivery();
        try {
            Path published = root.resolve("published");
            portable(published.resolve(archive("portable")), "9.9.9");
            writeManifest(published);

            Run verdict = duplicate(root);

            assertEquals(8, verdict.status(),
                    "a release this run did not build must fail: "
                            + verdict.output());
            assertTrue(verdict.output().contains("different source"),
                    verdict.output());
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
        // Each starts from the benign fixture and breaks exactly one
        // thing, so the failure is attributable.
        for (String what : List.of("missing-asset", "extra-asset",
                "manifest-omits-an-archive", "manifest-lies",
                "archive-not-downloaded", "no-manifest")) {
            Path root = delivery();
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
                            Files.readString(theirs).replace(
                                    sha256(published.resolve(
                                            archive("macos-x64"))),
                                    "dead" + "beef".repeat(14)));
                    case "archive-not-downloaded" -> Files.delete(
                            published.resolve(archive("macos-arm64")));
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
    void aRunWhoseOwnStagingIsWrongSaysSoRatherThanBlamingTheRelease()
            throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(
                Path.of("scripts/release-duplicate.sh")));
        // Exit 9 says this run is broken, 8 says the published
        // release is. Reporting a local fault as a conflict would
        // send a reader to inspect a healthy release.
        for (String what : List.of("no-manifest", "missing-archive",
                "manifest-disagrees-with-its-own-bytes")) {
            Path root = delivery();
            try {
                Path staging = root.resolve("staging");
                switch (what) {
                    case "no-manifest" -> Files.delete(
                            staging.resolve("SHA256SUMS.txt"));
                    case "missing-archive" -> Files.delete(
                            staging.resolve(archive("macos-x64")));
                    // The manifest is intact and internally plausible;
                    // only the bytes it names have moved. Trusting the
                    // manifest would miss this entirely.
                    case "manifest-disagrees-with-its-own-bytes" ->
                            Files.write(staging.resolve(archive("linux-x64")),
                                    "rebuilt after the sums were written"
                                            .getBytes(StandardCharsets.UTF_8));
                    default -> throw new IllegalStateException(what);
                }

                Run verdict = duplicate(root);

                assertEquals(9, verdict.status(),
                        what + " is this run's own fault: "
                                + verdict.output());
                assertFalse(verdict.output().contains("DIFFERENT release"),
                        what + ": " + verdict.output());
            } finally {
                cleanUp(root);
            }
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
            zos.write(("JUranometria " + real + " (app-image)\n"
                    + "source: " + COMMIT + "\n")
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
