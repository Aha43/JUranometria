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
     * One photographer: what it writes, and what it holds still.
     *
     * <p>The kind is declared and never defaulted. Chart Options was
     * photographed at its packed 394 px and Place and Time at 326 -
     * both widths no reader meets - because the coordinator assumed
     * every window was a packed one, and an undeclared default is
     * how that assumption gets made again.
     */
    record Photographer(String companion,
                        SheetCapture.Kind kind) {
    }

    /**
     * Every generator that owns part of this directory.
     *
     * <p>One registry, in one place. The inventory checks below read
     * this and the committed directory and require them to describe
     * the same set - in both directions, because a generator nobody
     * registered and an artifact nobody generates are the same
     * defect seen from two ends.
     */
    private static final Map<String, Photographer> GENERATORS =
            new LinkedHashMap<>();

    static {
        // Dialogs that production only packs.
        GENERATORS.put("AboutSheetMain",
                new Photographer("about-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("ExportSheetDialogSheetMain",
                new Photographer("export-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("SwingChromeSheetMain",
                new Photographer("swing-chrome-strings.md",
                        SheetCapture.Kind.PACKED));
        // Components inside a packed study frame.
        GENERATORS.put("ChartKeyboardSheetMain",
                new Photographer("chartkeyboard-strings.md",
                        SheetCapture.Kind.PACKED));
        GENERATORS.put("InspectorSheetMain",
                new Photographer("inspector-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("MenuSheetMain",
                new Photographer("menu-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("OnThisPageSheetMain",
                new Photographer("onthispage-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("ToolbarSheetMain",
                new Photographer("toolbar-strings.md", SheetCapture.Kind.PACKED));
        // Dialogs whose size the application states.
        GENERATORS.put("ChartOptionsSheetMain",
                new Photographer("chartoptions-strings.md",
                        SheetCapture.Kind.APPLICATION_SIZED));
        GENERATORS.put("PlaceAndTimeSheetMain",
                new Photographer("placeandtime-strings.md",
                        SheetCapture.Kind.APPLICATION_SIZED));
        // No window to size.
        GENERATORS.put("SettingsSheetMain",
                new Photographer("settings-strings.md",
                        SheetCapture.Kind.FIXED_CANVAS));
        GENERATORS.put("PageLanguageSheetMain",
                new Photographer("page-language-strings.md",
                        SheetCapture.Kind.FIXED_CANVAS));
    }

    /** Every companion this registry owns. */
    private static List<String> companions() {
        return GENERATORS.values().stream()
                .map(Photographer::companion).toList();
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
        boolean[] keep = {false};
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
                if (!recordingMachine()) {
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
            if (!wrong.isEmpty()) {
                // Keep everything, BEFORE the assertion throws. Both
                // occurrences of #364 were discovered after the runs
                // that produced them had been deleted, so the one
                // artifact anybody wanted no longer existed.
                keep[0] = true;
                Path kept = retain(first, second, wrong);
                assertEquals(List.of(), wrong,
                        "the display-owned evidence does not match its"
                                + " generators. Both complete runs and"
                                + " their capture traces are kept at "
                                + kept.toAbsolutePath()
                                + " - inspect them rather than running"
                                + " this again");
            }
            assertEquals(List.of(), wrong,
                    "the display-owned evidence does not match its"
                            + " generators");
            // What this run covered, and what it did not. A narrower
            // check has to say so where the narrowing happens.
            System.out.println(recordingMachine()
                    ? "interface evidence: " + listing(first).size()
                            + " artifacts, reproduced twice here AND"
                            + " held to their committed bytes"
                    : "interface evidence: " + listing(first).size()
                            + " artifacts reproduced twice on this"
                            + " machine. Their committed bytes were"
                            + " recorded elsewhere and are NOT"
                            + " compared here - another desktop's"
                            + " font metrics are not a defect");
        } finally {
            // Only when there was nothing to look at.
            if (!keep[0]) {
                remove(first);
                remove(second);
            }
            Files.deleteIfExists(traceFor(first));
            Files.deleteIfExists(traceFor(second));
        }
    }

    /**
     * Keeps both runs where a person can open them.
     *
     * <p>A gate that finds a disagreement and then deletes the
     * evidence has told you only that something is wrong. This
     * writes both complete trees, the capture traces taken while
     * each layout was still settled, and a summary naming what
     * differed, into a directory that survives the test.
     */
    private static Path retain(Path first, Path second,
                               List<String> wrong) throws Exception {
        Path kept = Path.of("build", "interface-gate-evidence");
        remove(kept);
        Files.createDirectories(kept);
        copyTree(first, kept.resolve("run-1"));
        copyTree(second, kept.resolve("run-2"));
        for (Path tree : List.of(first, second)) {
            Path trace = traceFor(tree);
            if (Files.exists(trace)) {
                Files.copy(trace, kept.resolve(
                        tree == first ? "run-1-trace.tsv"
                                : "run-2-trace.tsv"));
            }
        }

        StringBuilder said = new StringBuilder(
                "# Display evidence disagreement\n\n"
                        + "Kept because the gate failed. Two complete"
                        + " runs, and the capture traces written while"
                        + " each layout was still settled.\n\n");
        for (String one : wrong) {
            said.append("- ").append(one).append('\n');
        }
        said.append("\n## Differing artifacts, byte by byte\n\n");
        for (String name : listing(kept.resolve("run-1"))) {
            Path a = kept.resolve("run-1").resolve(name);
            Path b = kept.resolve("run-2").resolve(name);
            if (Files.exists(b) && !java.util.Arrays.equals(
                    Files.readAllBytes(a), Files.readAllBytes(b))) {
                said.append("- `").append(name).append("`: ")
                        .append(Files.size(a)).append(" vs ")
                        .append(Files.size(b)).append(" bytes\n");
            }
        }
        Files.writeString(kept.resolve("SUMMARY.md"), said.toString(),
                StandardCharsets.UTF_8);
        return kept;
    }

    /** Where a run's capture trace goes: beside the tree, not in it. */
    private static Path traceFor(Path tree) {
        return tree.resolveSibling(tree.getFileName() + "-trace.tsv");
    }

    private static void copyTree(Path from, Path to) throws Exception {
        Files.createDirectories(to);
        try (var files = Files.list(from)) {
            for (Path one : files.toList()) {
                Files.copy(one, to.resolve(one.getFileName()));
            }
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
            for (Map.Entry<String, Photographer> entry
                    : GENERATORS.entrySet()) {
                Path alone = Files.createDirectory(
                        made.resolve(entry.getKey()));
                run(entry.getKey(), alone);
                List<String> wrote = listing(alone);
                if (!wrote.contains(entry.getValue().companion())) {
                    silent.add(entry.getKey() + " wrote no "
                            + entry.getValue().companion());
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
        for (String companion : companions()) {
            if (!Files.exists(COMMITTED.resolve(companion))) {
                missing.add(companion);
            }
        }
        assertEquals(List.of(), missing,
                "a registered generator's companion is committed");

        List<String> unregistered = new ArrayList<>();
        for (String name : listing(COMMITTED)) {
            if (name.endsWith("-strings.md")
                    && !companions().contains(name)) {
                unregistered.add(name);
            }
        }
        assertEquals(List.of(), unregistered,
                "a companion committed here without a registered"
                        + " generator is a report nothing checks -"
                        + " which is the state this whole class of"
                        + " evidence was in for an entire sprint");
    }

    /**
     * Whether this is the machine the committed evidence was
     * recorded on.
     *
     * <p>These are photographs of real widgets, so their bytes are a
     * desktop's answer as much as the atlas's - the portable
     * evidence contract says the same of its renderings, holding
     * them "to reproducing here, never to another machine's pixels".
     * This gate compared them to the committed bytes everywhere, and
     * CI duly reported 104 of 106 artifacts stale: Linux had simply
     * drawn them, correctly, in its own fonts.
     *
     * <p>So the two halves have different reaches, and it is worth
     * being exact about which:
     *
     * <ul>
     *   <li><strong>reproduced twice</strong> - everywhere. On CI's
     *       Linux display, all 106 agreed run to run, which is the
     *       claim that matters and the one nondeterminism breaks;</li>
     *   <li><strong>equals the committed bytes</strong> - only where
     *       those bytes were recorded. Elsewhere it would be
     *       comparing two machines and calling the difference a
     *       defect.</li>
     * </ul>
     *
     * <p>The run says which of the two it performed rather than
     * leaving a reader of the log to assume the stronger one.
     */
    private static boolean recordingMachine() {
        return System.getenv("CI") == null;
    }

    /**
     * A failure keeps what it found.
     *
     * <p>Both occurrences of #364 were discovered after the runs
     * that produced them had been deleted, so the one artifact
     * anybody wanted no longer existed. This is release
     * infrastructure now, and an evidence mechanism nobody has seen
     * work is not known to work.
     *
     * <p>Driven with synthetic trees rather than a real double
     * generation: what is being pinned is that a disagreement is
     * <em>kept</em>, not that the generators disagree.
     */
    @Test
    void aFailureKeepsBothRunsAndTheirTraces() throws Exception {
        Path first = Files.createTempDirectory("retain-1");
        Path second = Files.createTempDirectory("retain-2");
        try {
            Files.writeString(first.resolve("same.md"), "identical",
                    StandardCharsets.UTF_8);
            Files.writeString(second.resolve("same.md"), "identical",
                    StandardCharsets.UTF_8);
            Files.writeString(first.resolve("moved.png"), "one",
                    StandardCharsets.UTF_8);
            Files.writeString(second.resolve("moved.png"), "another",
                    StandardCharsets.UTF_8);
            Files.writeString(traceFor(first), "settled\tfirst\n",
                    StandardCharsets.UTF_8);
            Files.writeString(traceFor(second), "settled\tsecond\n",
                    StandardCharsets.UTF_8);

            Path kept = retain(first, second,
                    List.of("moved.png: two runs disagreed"));

            assertTrue(Files.exists(kept.resolve("run-1/moved.png")),
                    "the first run is kept whole");
            assertTrue(Files.exists(kept.resolve("run-2/moved.png")),
                    "and so is the second - a single artifact is not"
                            + " a pair, and a pair is the whole point");
            assertTrue(Files.exists(kept.resolve("run-1-trace.tsv"))
                            && Files.exists(
                                    kept.resolve("run-2-trace.tsv")),
                    "with the capture traces written while each"
                            + " layout was still settled");

            String summary = Files.readString(kept.resolve("SUMMARY.md"),
                    StandardCharsets.UTF_8);
            assertTrue(summary.contains("moved.png"),
                    "the summary names what differed: " + summary);
            assertTrue(!summary.contains("same.md"),
                    "and not what did not, so a reader is pointed"
                            + " somewhere: " + summary);
        } finally {
            remove(first);
            remove(second);
            remove(Path.of("build", "interface-gate-evidence"));
        }
    }

    /**
     * Every photographer declares its kind, and honours it.
     *
     * <p>Two halves, because either alone is decorative. The
     * generator's own {@code CAPTURE_KIND} constant is read
     * <strong>reflectively</strong>, so the compiler carries it and
     * this registry cannot drift from it; and the source must ask
     * the coordinator for the matching operation, so a declaration
     * cannot be a label over a capture that does something else.
     *
     * <p>All three kinds can fail. Declaring About a fixed canvas,
     * or Settings a packed window, breaks both halves - which the
     * mutations in `SheetCaptureSizingTest` pin.
     */
    @Test
    void everyPhotographerDeclaresItsKindAndHonoursIt()
            throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Photographer> entry
                : GENERATORS.entrySet()) {
            SheetCapture.Kind declared = entry.getValue().kind();

            SheetCapture.Kind own = (SheetCapture.Kind) Class
                    .forName("juranometria.tool." + entry.getKey())
                    .getField("CAPTURE_KIND").get(null);
            if (own != declared) {
                wrong.add(entry.getKey() + " declares " + own
                        + " and this registry says " + declared);
                continue;
            }

            String code = Files.readString(
                            Path.of("src/juranometria/tool",
                                    entry.getKey() + ".java"),
                            StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", " ")
                    .replaceAll("(?m)//.*$", " ");
            String operation = switch (declared) {
                case PACKED -> "SheetCapture.packed()";
                case APPLICATION_SIZED ->
                        "SheetCapture.applicationSized(";
                case FIXED_CANVAS -> "SheetCapture.fixedCanvas()";
            };
            boolean asks = code.contains(operation);
            // PageLanguage draws renderer output and calls the
            // coordinator for nothing, so its constant is its whole
            // declaration. Everything with a component hierarchy has
            // to ask.
            boolean capturesAnything =
                    code.contains("SheetCapture.take(")
                            || code.contains("SheetCapture.of(")
                            || code.contains("SheetCapture.write(");
            if (capturesAnything && !asks) {
                wrong.add(entry.getKey() + " declares " + declared
                        + " and never calls " + operation);
            }
            for (String other : List.of("SheetCapture.packed()",
                    "SheetCapture.applicationSized(",
                    "SheetCapture.fixedCanvas()")) {
                if (!other.equals(operation) && code.contains(other)) {
                    wrong.add(entry.getKey() + " declares " + declared
                            + " and calls " + other);
                }
            }
        }
        assertEquals(List.of(), wrong,
                "a kind is a choice the capture actually makes, not a"
                        + " row in a table. Both defects this"
                        + " distinction exists for were a coordinator"
                        + " assuming the kind");
    }

    /**
     * No photographer paints outside the coordinator's block.
     *
     * <p>The structural half of #364's repair. The kind contract
     * above says a photographer declares how big its window is; this
     * says it does not decide <em>when</em> the picture is taken.
     * Nine of these twelve settled through the coordinator and then
     * painted in event blocks of their own, one or more cycles
     * later, and two painted off the event thread altogether. Export
     * is the one that got caught: 326x206 pixels beside a settled
     * record that said 333x223.
     *
     * <p>Every {@code paint(} a photographer performs must lie
     * inside a call to {@code SheetCapture.take}, {@code of} or
     * {@code write} - textually inside, so a future generator that
     * reintroduces the gap fails here rather than once in sixteen
     * runs under load.
     */
    @Test
    void noPhotographerPaintsOutsideTheCoordinatorsBlock()
            throws Exception {
        List<String> wrong = new ArrayList<>();
        for (String name : GENERATORS.keySet()) {
            // Page Language draws renderer output directly and owns
            // no component hierarchy, so it has no block to be
            // inside. It is the declared exception, named here so
            // the exception is visible rather than implied.
            if (name.equals("PageLanguageSheetMain")) {
                continue;
            }
            String code = Files.readString(
                            Path.of("src/juranometria/tool",
                                    name + ".java"),
                            StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", " ")
                    .replaceAll("(?m)//.*$", " ");
            List<int[]> blocks = capturesIn(code);
            for (int at = code.indexOf(".paint("); at >= 0;
                    at = code.indexOf(".paint(", at + 1)) {
                boolean inside = false;
                for (int[] block : blocks) {
                    if (at > block[0] && at < block[1]) {
                        inside = true;
                        break;
                    }
                }
                if (!inside) {
                    wrong.add(name + " paints at offset " + at
                            + ", outside any SheetCapture call");
                }
            }
        }
        assertEquals(List.of(), wrong,
                "a photographer hands over what to draw and gets back"
                        + " an image. Where and when it is drawn is"
                        + " not its decision, because every one of"
                        + " those decisions was an event cycle"
                        + " something else could use");
    }

    /**
     * The extent of each {@code SheetCapture} call in a source file.
     *
     * <p>From the opening parenthesis to the one that closes it, so
     * "inside the call" means inside the lambda it was handed.
     */
    private static List<int[]> capturesIn(String code) {
        List<int[]> blocks = new ArrayList<>();
        for (String entry : List.of("SheetCapture.take(",
                "SheetCapture.of(", "SheetCapture.write(")) {
            for (int at = code.indexOf(entry); at >= 0;
                    at = code.indexOf(entry, at + 1)) {
                int open = at + entry.length() - 1;
                int depth = 0;
                for (int scan = open; scan < code.length(); scan++) {
                    char one = code.charAt(scan);
                    if (one == '(') {
                        depth++;
                    } else if (one == ')') {
                        depth--;
                        if (depth == 0) {
                            blocks.add(new int[] {open, scan});
                            break;
                        }
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * A thirteenth photographer cannot arrive undeclared.
     *
     * <p>Every generator that writes into this directory is in the
     * registry with a kind. The registry is the audit boundary, so
     * arriving without one has to be impossible rather than
     * discouraged.
     */
    @Test
    void noPhotographerOfThisEvidenceIsUndeclared() throws Exception {
        List<String> undeclared = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/juranometria/tool"))) {
            for (Path file : files.filter(f ->
                    f.toString().endsWith(".java")).toList()) {
                String code = Files.readString(file,
                        StandardCharsets.UTF_8);
                if (!code.contains("docs/studies/interface-language")) {
                    continue;
                }
                String name = file.getFileName().toString()
                        .replace(".java", "");
                if (!GENERATORS.containsKey(name)) {
                    undeclared.add(name);
                }
            }
        }
        assertEquals(List.of(), undeclared,
                "these write into the display-owned evidence"
                        + " directory and declare no capture kind."
                        + " Add them to GENERATORS with the kind they"
                        + " really are - packed, application-sized,"
                        + " or a fixed canvas");
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
                // A SIBLING of the tree, never inside it: a trace
                // is not an artifact, and one living among them
                // would be compared as though it were.
                "-Djuranometria.capture.trace=" + traceFor(into),
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
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(one -> one.toFile().delete());
        }
    }
}
