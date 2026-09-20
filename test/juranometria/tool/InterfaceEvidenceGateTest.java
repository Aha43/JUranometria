package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate over a whole evidence class (Sprint 33, issue #350).
 *
 * <p>Everything under {@code docs/studies/interface-language} is
 * <strong>display-owned</strong>: real widgets, photographed. The
 * portable evidence contract cannot hold any of it, and that is not
 * an oversight - it runs headless, and these generators pack real
 * windows because their labels wrap against font metrics. So for the
 * whole of #350 this class had no gate at all, and it showed:
 *
 * <ul>
 *   <li>`export-strings.md` drifted for four commits, printing an
 *       escape literally and quoting a byte count 24 bytes behind
 *       the application;</li>
 *   <li>`chartoptions-nb-NO-1.png` reproduced its committed bytes
 *       about once in six runs;</li>
 *   <li>the two chooser sheets photographed the <em>home directory
 *       of whoever ran them</em>, so 11,019 pixels changed between
 *       two commits because files had come and gone on a desk.</li>
 * </ul>
 *
 * <p>Each was found by hand, one at a time. This is the check that
 * would have found all three at once, and will find the fourth.
 *
 * <p><strong>Where this belongs, and why not the contract.</strong>
 * The evidence classes are explained in {@code EvidenceContractMain};
 * this class is the one that needs eyes and a screen. It lives in the
 * display test path, self-aborts without a display, and the contract
 * is documented as deliberately not owning it. Two gates, one
 * boundary, stated in both places.
 */
class InterfaceEvidenceGateTest {

    private static final Path COMMITTED =
            Path.of("docs/studies/interface-language");

    /**
     * Every generator that owns part of this directory, and the
     * companion it writes.
     *
     * <p>One registry, in one place. The inventory checks below read
     * this and the committed directory and require them to describe
     * the same set - in both directions, because a generator nobody
     * registered and an artifact nobody generates are the same
     * defect seen from two ends.
     */
    private static final Map<String, String> GENERATORS =
            new LinkedHashMap<>();

    static {
        GENERATORS.put("AboutSheetMain", "about-strings.md");
        GENERATORS.put("ChartKeyboardSheetMain",
                "chartkeyboard-strings.md");
        GENERATORS.put("ChartOptionsSheetMain",
                "chartoptions-strings.md");
        GENERATORS.put("ExportSheetDialogSheetMain",
                "export-strings.md");
        GENERATORS.put("InspectorSheetMain", "inspector-strings.md");
        GENERATORS.put("MenuSheetMain", "menu-strings.md");
        GENERATORS.put("OnThisPageSheetMain", "onthispage-strings.md");
        GENERATORS.put("PageLanguageSheetMain",
                "page-language-strings.md");
        GENERATORS.put("PlaceAndTimeSheetMain",
                "placeandtime-strings.md");
        GENERATORS.put("SettingsSheetMain", "settings-strings.md");
        GENERATORS.put("SwingChromeSheetMain",
                "swing-chrome-strings.md");
        GENERATORS.put("ToolbarSheetMain", "toolbar-strings.md");
    }

    /**
     * Two full generations, compared with each other and with what
     * is committed.
     *
     * <p>Two rather than one because the two failures this class
     * exists for are different: a single run against committed
     * catches <em>staleness</em>, and only run against run catches
     * <em>nondeterminism</em>. A generator that produces a different
     * answer every time would pass a single comparison whenever it
     * happened to agree, which is exactly how a one-in-six flicker
     * survived a whole sprint.
     */
    @Test
    void everyCommittedArtifactIsWhatItsGeneratorProducesTwice()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "these generators pack real windows");
        Path first = Files.createTempDirectory("interface-gate-1");
        Path second = Files.createTempDirectory("interface-gate-2");
        try {
            generateAll(first);
            generateAll(second);

            List<String> unstable = new ArrayList<>();
            List<String> stale = new ArrayList<>();
            for (String name : new TreeSet<>(listing(first))) {
                byte[] one = Files.readAllBytes(first.resolve(name));
                Path other = second.resolve(name);
                if (!Files.exists(other)
                        || !java.util.Arrays.equals(one,
                                Files.readAllBytes(other))) {
                    unstable.add(name);
                    continue;
                }
                Path committed = COMMITTED.resolve(name);
                if (!Files.exists(committed)
                        || !java.util.Arrays.equals(one,
                                Files.readAllBytes(committed))) {
                    stale.add(name);
                }
            }
            // Both lists in ONE assertion. Asserting them one after
            // the other means the first failure hides the second,
            // and a reader of the failure would fix what they were
            // shown and be surprised again - which is what happened
            // the first time this was mutation-proved.
            List<String> wrong = new ArrayList<>();
            unstable.forEach(one -> wrong.add(one + ": two runs on"
                    + " this machine disagreed, so it is not evidence"
                    + " of anything"));
            stale.forEach(one -> wrong.add(one + ": committed bytes"
                    + " are not what the generator produces -"
                    + " regenerate it, do not edit it"));
            assertEquals(List.of(), wrong,
                    "the display-owned evidence does not match its"
                            + " generators");
        } finally {
            remove(first);
            remove(second);
        }
    }

    /**
     * Nothing is committed here that no registered generator makes.
     *
     * <p>The direction that catches a thirteenth companion arriving
     * without a registration - and with it, every image that
     * companion's generator draws.
     */
    @Test
    void everyCommittedArtifactHasARegisteredGenerator()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the generators have to run to say what they own");
        Path made = Files.createTempDirectory("interface-gate-owns");
        try {
            generateAll(made);
            List<String> unowned = new ArrayList<>(
                    new TreeSet<>(listing(COMMITTED)));
            unowned.removeAll(listing(made));
            assertEquals(List.of(), unowned,
                    "these files are committed as evidence and"
                            + " nothing in the registry produces"
                            + " them. Either a generator is missing"
                            + " from GENERATORS, or the files are"
                            + " residue that should not be committed");
        } finally {
            remove(made);
        }
    }

    /** And every registered generator produces a complete set. */
    @Test
    void everyRegisteredGeneratorProducesWhatItClaims()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the generators have to run to say what they own");
        List<String> silent = new ArrayList<>();
        Path made = Files.createTempDirectory("interface-gate-claims");
        try {
            for (Map.Entry<String, String> entry
                    : GENERATORS.entrySet()) {
                Path alone = Files.createDirectory(
                        made.resolve(entry.getKey()));
                run(entry.getKey(), alone);
                List<String> wrote = listing(alone);
                if (!wrote.contains(entry.getValue())) {
                    silent.add(entry.getKey() + " wrote no "
                            + entry.getValue());
                } else if (wrote.size() < 2
                        && !entry.getKey().equals("SettingsSheetMain")) {
                    silent.add(entry.getKey() + " wrote a companion"
                            + " and no images");
                }
            }
            assertEquals(List.of(), silent,
                    "a registered generator has to produce the"
                            + " companion it is registered for, and"
                            + " the images beside it");
        } finally {
            remove(made);
        }
    }

    /** Everything the registry names is committed, and nothing else. */
    @Test
    void theRegistryAndTheCommittedCompanionsAgree() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String companion : GENERATORS.values()) {
            if (!Files.exists(COMMITTED.resolve(companion))) {
                missing.add(companion);
            }
        }
        assertEquals(List.of(), missing,
                "a registered generator's companion is committed");

        List<String> unregistered = new ArrayList<>();
        for (String name : listing(COMMITTED)) {
            if (name.endsWith("-strings.md")
                    && !GENERATORS.containsValue(name)) {
                unregistered.add(name);
            }
        }
        assertEquals(List.of(), unregistered,
                "a companion committed here without a registered"
                        + " generator is a report nothing checks -"
                        + " which is the state this whole class of"
                        + " evidence was in for an entire sprint");
    }

    /** Runs every registered generator into one directory. */
    private static void generateAll(Path into) throws Exception {
        for (String generator : GENERATORS.keySet()) {
            run(generator, into);
        }
    }

    private static void run(String generator, Path into)
            throws Exception {
        Process made = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java")
                        .toString(),
                "-cp", System.getProperty("java.class.path"),
                "juranometria.tool." + generator, into.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(made.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(made.waitFor(10, TimeUnit.MINUTES),
                generator + " finishes");
        assertEquals(0, made.exitValue(), generator
                + " succeeded. It said:\n" + output);
    }

    private static List<String> listing(Path directory)
            throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(one -> one.getFileName().toString())
                    .sorted().toList();
        }
    }

    private static void remove(Path directory) throws Exception {
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(one -> one.toFile().delete());
        }
    }
}
