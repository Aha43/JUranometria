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
        Path firstTrace = beside(first);
        Path secondTrace = beside(second);
        try {
            String one = generateInto(first, firstTrace);
            String two = generateInto(second, secondTrace);
            if (!one.equals(two)) {
                Path kept = retain(first, second, firstTrace,
                        secondTrace, difference(one, two));
                assertEquals(one, two,
                        "two runs of the export companion have to"
                                + " agree. A report that cannot"
                                + " reproduce on the machine that"
                                + " wrote it cannot be held to"
                                + " anything, anywhere. BOTH runs and"
                                + " their capture traces are kept at "
                                + kept);
            }
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
            // Only on success. A failure's directories are the
            // evidence, and deleting them is what left the two
            // recorded occurrences of #364 unreadable.
            remove(first);
            remove(second);
            java.nio.file.Files.deleteIfExists(firstTrace);
            java.nio.file.Files.deleteIfExists(secondTrace);
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

    /**
     * One run of the generator, in a directory of its own, traced.
     *
     * <p>The trace goes <strong>beside</strong> the directory, not
     * inside it: what is inside is the run's own output, and a file
     * that is neither image nor report would be one more thing to
     * explain when the two trees are compared.
     */
    private static String generateInto(Path directory, Path trace)
            throws Exception {
        Process run = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java")
                        .toString(),
                "-cp", System.getProperty("java.class.path"),
                "-Djuranometria.capture.trace=" + trace,
                "juranometria.tool.ExportSheetDialogSheetMain",
                directory.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(run.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("generator-output.log"),
                output, StandardCharsets.UTF_8);
        boolean finished = run.waitFor(5, TimeUnit.MINUTES);
        if (!finished || run.exitValue() != 0) {
            Path kept = retain(directory, directory, trace, trace,
                    "the generator did not succeed:\n" + output);
            assertTrue(finished,
                    "the generator finishes. Kept at " + kept);
            assertEquals(0, run.exitValue(),
                    "the generator succeeds. It said:\n" + output
                            + "\nKept at " + kept);
        }
        Path written = directory.resolve("export-strings.md");
        assertTrue(Files.exists(written),
                "and writes its report where it was told to");
        return Files.readString(written, StandardCharsets.UTF_8);
    }

    /** A trace file beside a run's directory, named for it. */
    private static Path beside(Path directory) {
        return directory.resolveSibling(
                directory.getFileName() + "-trace.tsv");
    }

    /** The lines on which two reports disagree, and nothing else. */
    private static String difference(String one, String two) {
        List<String> a = one.lines().toList();
        List<String> b = two.lines().toList();
        StringBuilder said = new StringBuilder();
        said.append("run A has ").append(a.size())
                .append(" lines, run B has ").append(b.size())
                .append("\n\n");
        for (int at = 0; at < Math.max(a.size(), b.size()); at++) {
            String x = at < a.size() ? a.get(at) : "<missing>";
            String y = at < b.size() ? b.get(at) : "<missing>";
            if (!x.equals(y)) {
                said.append("line ").append(at + 1).append('\n')
                        .append("  A: ").append(x).append('\n')
                        .append("  B: ").append(y).append('\n');
            }
        }
        return said.toString();
    }

    /**
     * Keeps everything a diagnosis needs, and says where.
     *
     * <p>#364's first two occurrences were both found after the runs
     * that produced them had been deleted - this method's absence is
     * the reason the artifact anybody wanted no longer existed. The
     * third was caught here, by this contract, and kept nothing
     * either: both scratch trees went in a {@code finally} and the
     * child JVMs ran without traces, so the one run that failed was
     * the one run that recorded nothing.
     */
    private static Path retain(Path first, Path second,
                               Path firstTrace, Path secondTrace,
                               String difference) throws Exception {
        Path kept = Path.of("build", "export-companion-evidence");
        if (Files.exists(kept)) {
            remove(kept);
        }
        Files.createDirectories(kept);
        copyInto(first, kept.resolve("run-A"));
        if (!second.equals(first)) {
            copyInto(second, kept.resolve("run-B"));
        }
        copyBeside(firstTrace, kept.resolve("run-A-trace.tsv"));
        if (!secondTrace.equals(firstTrace)) {
            copyBeside(secondTrace, kept.resolve("run-B-trace.tsv"));
        }
        Files.writeString(kept.resolve("DIFFERENCE.md"),
                "# The export companion did not reproduce\n\n"
                        + "Both complete runs are here, with the"
                        + " capture trace each one wrote. Every trace"
                        + " line names the sheet it belongs to, so a"
                        + " line maps to nb-NO / PNG / A4 by reading"
                        + " rather than by counting.\n\n"
                        + difference,
                StandardCharsets.UTF_8);
        return kept.toAbsolutePath();
    }

    private static void copyInto(Path from, Path to) throws Exception {
        try (var files = Files.walk(from)) {
            for (Path one : files.toList()) {
                Path at = to.resolve(from.relativize(one).toString());
                if (Files.isDirectory(one)) {
                    Files.createDirectories(at);
                } else {
                    Files.createDirectories(at.getParent());
                    Files.copy(one, at);
                }
            }
        }
    }

    private static void copyBeside(Path from, Path to) throws Exception {
        if (Files.exists(from)) {
            Files.createDirectories(to.getParent());
            Files.copy(from, to);
        }
    }

    private static void remove(Path directory) throws Exception {
        try (var files = Files.walk(directory)) {
            files.sorted(java.util.Comparator.reverseOrder())
                    .forEach(one -> one.toFile().delete());
        }
    }
}
