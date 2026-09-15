package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A study says what its data says (Sprint 33, issue #347).
 *
 * <p>The placement study is measured against an owner-supplied lead,
 * and the whole value of calling it provisional depends on that word
 * being impossible to lose by accident. The first attempt printed the
 * banner from Java and then checked that the banner was there - the
 * same code writing and reading its own claim, which is a check that
 * cannot fail.
 *
 * <p>Status now travels with the data, in a manifest beside the lead,
 * and these hold the four properties that makes worth having. The
 * failure they exist to prevent is a real and dull one: somebody
 * verifies the catalogue, and a banner compiled into a generator goes
 * on describing work that has been done - or stops describing work
 * that has not.
 */
class SkyLanguageProvenanceTest {

    private static final Path MANIFEST =
            Path.of("docs/studies/sky-language/lead.manifest");

    private static final Path REPORT =
            Path.of("docs/studies/sky-language/placement.md");

    /** The report repeats the status its input states. */
    @Test
    void theReportSaysWhatTheManifestSays() throws IOException {
        String stated = stated().get("status");
        String report = Files.readString(REPORT);

        assertTrue(report.contains(stated.toUpperCase(java.util.Locale.ROOT)),
                "the committed report states the status its manifest"
                        + " declares (" + stated + ")");
        if ("provisional".equals(stated)) {
            assertTrue(report.lines().findFirst().orElse("")
                            .contains("PROVISIONAL"),
                    "and a provisional study says so in its title,"
                            + " where a reader meets it first");
        }
    }

    /** Provisional data cannot produce a verified report. */
    @Test
    void provisionalDataCannotCallItselfVerified() {
        Map<String, String> manifest = new LinkedHashMap<>(stated());
        manifest.put("status", "provisional");

        assertTrue(provenance(manifest).contains("provisional"),
                "a provisional manifest yields a provisional study,"
                        + " whatever the generator would prefer");
    }

    /**
     * "Verified" without an account of itself is refused.
     *
     * <p>This is the clause that stops the word being free. A
     * manifest may claim the names were checked against a citable
     * source only by saying which source, under what licence, on
     * what date, and what was changed on the way.
     */
    @Test
    void verifiedWithoutProvenanceRefusesToRun() {
        for (String missing
                : List.of("source", "licence", "retrieved",
                        "transformations")) {
            Map<String, String> manifest = new LinkedHashMap<>();
            manifest.put("status", "verified");
            manifest.put("source", "a named catalogue");
            manifest.put("licence", "CC BY 4.0");
            manifest.put("retrieved", "2026-09-14");
            manifest.put("transformations", "none");
            manifest.put(missing, "");

            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> provenanceOrThrow(manifest),
                    "a verified claim missing its " + missing
                            + " must be refused, not recorded");
            assertTrue(refused.getMessage().contains(missing),
                    "and the refusal names what is missing: "
                            + refused.getMessage());
        }
    }

    /** A status that is neither of the two is not a status. */
    @Test
    void anUnknownStatusIsRefusedRatherThanAssumedProvisional() {
        Map<String, String> manifest = new LinkedHashMap<>(stated());
        manifest.put("status", "probably fine");

        // Deliberately not lenient. Treating an unrecognised word as
        // provisional would be the safe-looking choice that quietly
        // accepts a typo in the one field whose whole job is to be
        // read carefully.
        assertThrows(IllegalStateException.class,
                () -> provenanceOrThrow(manifest),
                "an unrecognised status is refused, not rounded down");
    }

    /** The manifest is pinned in the report, so changing it goes stale. */
    @Test
    void theReportRecordsWhichManifestItRead() throws IOException {
        String digest = juranometria.catalog.Sha256.hex(
                Files.readAllBytes(MANIFEST));

        assertTrue(Files.readString(REPORT).contains(digest),
                "the report names the manifest it read, so editing"
                        + " the provenance without regenerating is a"
                        + " contract breach rather than a quiet"
                        + " disagreement");
    }

    private static Map<String, String> stated() {
        try {
            Map<String, String> manifest = new LinkedHashMap<>();
            for (String line : Files.readAllLines(MANIFEST)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                int is = line.indexOf('=');
                if (is > 0) {
                    manifest.put(line.substring(0, is).strip(),
                            line.substring(is + 1).strip());
                }
            }
            return manifest;
        } catch (IOException cannotRead) {
            throw new AssertionError("the manifest is missing", cannotRead);
        }
    }

    /** The study's own reading of a manifest, reached reflectively. */
    private static String provenance(Map<String, String> manifest) {
        return provenanceOrThrow(manifest).toString();
    }

    private static Object provenanceOrThrow(Map<String, String> manifest) {
        try {
            Class<?> type = Class.forName(
                    "juranometria.tool.SkyLanguageStudyMain$Provenance");
            var of = type.getDeclaredMethod("of", Map.class);
            of.setAccessible(true);
            return of.invoke(null, manifest);
        } catch (ReflectiveOperationException failed) {
            if (failed.getCause() instanceof RuntimeException thrown) {
                throw thrown;
            }
            throw new AssertionError(
                    "the study's provenance reader could not be"
                            + " reached: " + failed, failed);
        }
    }
}
