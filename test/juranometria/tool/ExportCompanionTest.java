package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one committed report nothing was holding (Sprint 33, #350).
 *
 * <p>`export-strings.md` drifted for four commits without a signal.
 * It printed an escape literally where the dialog had learned to
 * break the line, and its Norwegian byte count was 24 bytes behind
 * what the application had produced since the exported page learned
 * to describe itself. Nothing noticed because its generator is in no
 * registry: the evidence contract runs <strong>headless</strong>, and
 * this generator packs a real window because the explanation beneath
 * the format control wraps against font metrics. A generator the
 * contract cannot run is a generator the contract cannot hold.
 *
 * <p>So the guarantee is split in two, because the report is two
 * things at once and only one of them is portable.
 *
 * <ul>
 *   <li><strong>Nondeterminism</strong> is caught by running the
 *       generator twice into scratch directories and requiring the
 *       two reports to be identical. True on any machine, and it
 *       needs a display, so it self-aborts without one.</li>
 *   <li><strong>Staleness</strong> is caught by requiring every word
 *       the committed report quotes to be what the packs say
 *       <em>now</em>. Machine-independent, so it runs everywhere
 *       including CI - and it is the half that would have caught the
 *       four-commit drift.</li>
 * </ul>
 *
 * <p><strong>What is not covered, said plainly:</strong> the packed
 * pixel sizes and the exported byte counts. They are this desktop's
 * answer and this build's, the report says so and names the machine
 * it came from, and no test here holds them across two machines.
 */
class ExportCompanionTest {

    private static final Path COMMITTED = Path.of(
            "docs/studies/interface-language/export-strings.md");

    /**
     * Two runs, two scratch directories, one report.
     *
     * <p>The committed evidence is never written to: the generator
     * takes its output directory as an argument precisely so this
     * can be asked without restoring anything afterwards.
     *
     * <p>This is also what would have caught the random scratch path
     * the report used to print. That path made two runs differ by
     * construction, which is why the report could not be held to
     * anything at all.
     */
    @Test
    void theReportIsTheSameTwiceOnThisMachine() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "this generator packs a real window, because the"
                        + " explanation it photographs wraps against"
                        + " font metrics");
        Path first = Files.createTempDirectory("export-companion-1");
        Path second = Files.createTempDirectory("export-companion-2");
        try {
            String one = generateInto(first);
            String two = generateInto(second);
            assertEquals(one, two,
                    "two runs of the export companion have to agree."
                            + " A report that cannot reproduce on the"
                            + " machine that wrote it cannot be held"
                            + " to anything, anywhere");
            assertTrue(one.contains("Recorded on: `"),
                    "and it names the machine it came from, because"
                            + " its packed sizes and byte counts are"
                            + " that machine's answer");
            assertTrue(!one.contains("/var/folders/")
                            && !one.contains("/tmp/"),
                    "with no scratch path reaching the page: the"
                            + " writes are real, the folder the"
                            + " question names is reader data");
        } finally {
            remove(first);
            remove(second);
        }
    }

    /**
     * Every word the committed report quotes is a word the
     * application still says.
     *
     * <p>Machine-independent on purpose. The rows this walks are the
     * ones that carry the atlas's own sentences, and those are the
     * same on every desktop - so this is the half that can run in CI
     * and the half that catches a report left behind by a commit.
     */
    @Test
    void everySentenceTheReportQuotesIsWhatTheAtlasSaysNow()
            throws Exception {
        String said = Files.readString(COMMITTED, StandardCharsets.UTF_8);
        InterfaceText en = InterfaceText.forLanguage("en");
        InterfaceText nb = InterfaceText.forLanguage("nb-NO");

        // Keyed by the row label the generator writes, because that
        // is what a reader of the report sees. Each is checked in
        // BOTH languages: a row that had gone stale in one only
        // would otherwise be half-caught.
        List<String[]> rows = List.of(
                new String[] {"chooser title", "export.chooser.title"},
                new String[] {"replace title", "export.replace.title"},
                new String[] {"written title", "export.written.title"},
                new String[] {"refused title", "export.refused.title"});
        List<String> missing = new ArrayList<>();
        for (String[] row : rows) {
            for (InterfaceText words : List.of(en, nb)) {
                String sentence = words.say(row[1]);
                if (!said.contains("| " + row[0] + " | " + sentence
                        + " |")) {
                    missing.add(row[0] + ": \"" + sentence + "\"");
                }
            }
        }

        // The question is the row that was actually wrong, and the
        // shape it was wrong in: an escape printed literally rather
        // than broken. The generator renders a line break as " / ",
        // so the presence of a backslash-n is the defect itself.
        for (InterfaceText words : List.of(en, nb)) {
            String question = words.say("export.replace.question",
                    "orion.svg", "~/Documents").replace("\n", " / ");
            if (!said.contains("| replace question | " + question
                    + " |")) {
                missing.add("replace question: \"" + question + "\"");
            }
        }
        assertEquals(List.of(), missing,
                "the committed report quotes sentences the"
                        + " application no longer says. Regenerate it"
                        + " - this is exactly the drift that went"
                        + " four commits unseen");

        assertTrue(!said.contains("\\n"),
                "and no escape reaches the page as its own characters,"
                        + " which is how the defect looked before the"
                        + " parser learned to decode it");
    }

    /** One run of the generator, in a directory of its own. */
    private static String generateInto(Path directory) throws Exception {
        Process run = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java")
                        .toString(),
                "-cp", System.getProperty("java.class.path"),
                "juranometria.tool.ExportSheetDialogSheetMain",
                directory.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(run.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(run.waitFor(5, TimeUnit.MINUTES),
                "the generator finishes");
        assertEquals(0, run.exitValue(),
                "the generator succeeds. It said:\n" + output);
        Path written = directory.resolve("export-strings.md");
        assertTrue(Files.exists(written),
                "and writes its report where it was told to");
        return Files.readString(written, StandardCharsets.UTF_8);
    }

    private static void remove(Path directory) throws Exception {
        try (var files = Files.walk(directory)) {
            files.sorted(java.util.Comparator.reverseOrder())
                    .forEach(one -> one.toFile().delete());
        }
    }
}
