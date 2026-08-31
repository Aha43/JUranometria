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
