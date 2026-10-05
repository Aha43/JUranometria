package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import juranometria.tool.ChangeRoute.Finding;
import juranometria.tool.ChangeRoute.Route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts for the three routes (issue #398, extended by #427/#428 as
 * the owner ruled): every kind of change that can reach chart ink is
 * wide; interface work that cannot is interaction; prose, tests and
 * code nothing ships take the narrow route; and every case the rules
 * cannot resolve - a path in both closures, an unknown owner, a key
 * nothing names, a removed source - fails closed to wide.
 *
 * <p>Each case is judged against a boundary stated here, so the rule
 * and not the tree is what is proved; the last tests then hold the
 * real tree's boundary to the ruling's own examples.
 */
class ChangeRouteTest {

    /** A boundary a test can state completely. */
    private static final class Stated implements ChangeRoute.Boundary {
        final Map<String, String> chart = new HashMap<>();
        final Map<String, String> interaction = new HashMap<>();
        final Map<String, Route> owners = new HashMap<>();
        final Map<String, Finding> keys = new HashMap<>();
        final Map<String, Finding> fileReaders = new HashMap<>();
        final Map<String, String> base = new HashMap<>();
        final Map<String, String> head = new HashMap<>();

        @Override
        public Optional<String> chartChain(String source) {
            return Optional.ofNullable(chart.get(source));
        }

        @Override
        public Optional<String> interactionChain(String source) {
            return Optional.ofNullable(interaction.get(source));
        }

        @Override
        public Optional<Route> ownerOf(String path) {
            for (Map.Entry<String, Route> owner : owners.entrySet()) {
                if (path.startsWith(owner.getKey())) {
                    return Optional.of(owner.getValue());
                }
            }
            return Optional.empty();
        }

        @Override
        public Finding consumersOf(String key) {
            return keys.getOrDefault(key, new Finding(key, Route.WIDE,
                    "no compiled code names it: unresolved"));
        }

        @Override
        public Optional<Finding> readersOfFile(String path) {
            return Optional.ofNullable(fileReaders.get(path));
        }

        @Override
        public Optional<String> base(String path) {
            return Optional.ofNullable(base.get(path));
        }

        @Override
        public Optional<String> head(String path) {
            return Optional.ofNullable(head.get(path));
        }
    }

    private static final Predicate<String> PRESENT =
            path -> !path.contains("Removed");

    private static Stated tree() {
        Stated tree = new Stated();
        tree.chart.put("src/juranometria/render/ChartRenderer.java",
                "juranometria/tool/GalleryPageMain -> juranometria/render/ChartRenderer");
        tree.chart.put("src/juranometria/ui/ChartComponent.java",
                "juranometria/tool/GalleryPageMain -> juranometria/ui/ChartComponent");
        // Shared: the interface reaches it too, and chart wins.
        tree.interaction.put("src/juranometria/ui/ChartComponent.java",
                "juranometria/tool/ToolbarSheetMain -> juranometria/ui/ChartComponent");
        tree.interaction.put("src/juranometria/ui/AtlasToolbar.java",
                "juranometria/tool/ToolbarSheetMain -> juranometria/ui/AtlasToolbar");
        tree.interaction.put("src/juranometria/ui/ZoomInteraction.java",
                "juranometria/app/JUranometriaMain -> juranometria/ui/ZoomInteraction");
        tree.owners.put("docs/studies/interface-language/", Route.INTERACTION);
        tree.owners.put("docs/studies/control-explanations/", Route.INTERACTION);
        tree.owners.put("docs/studies/moon-on-the-chart/", Route.WIDE);
        tree.owners.put("docs/studies/sky-language/", Route.WIDE);
        tree.keys.put("toolbar.zoomIn.label", new Finding("toolbar.zoomIn.label",
                Route.INTERACTION, "read by juranometria/ui/AtlasToolbar"));
        tree.keys.put("page.body.moon", new Finding("page.body.moon",
                Route.WIDE, "chart code can resolve it (juranometria/ui/language/PageText)"));
        tree.fileReaders.put("docs/studies/solar-system/moon-events-2026.txt",
                new Finding("docs/studies/solar-system/moon-events-2026.txt",
                        Route.WIDE, "a committed input chart code reads"
                                + " (juranometria/tool/MoonEventsFixture)"));
        return tree;
    }

    private static Finding one(Stated tree, String path) {
        return ChangeRoute.classify(List.of(path), tree, PRESENT).get(0);
    }

    @Test
    void whatTheChartRegimeConsumesWithoutCompilingIsWideByName() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("docs/reference/m31-stars.png", "reference image"),
                Map.entry("docs/gallery/index.html", "gallery"),
                Map.entry("src/resources/solar-system/de440-subset.bsp",
                        "bundled resource"),
                Map.entry("packaging/LICENSING.md", "packaging input"),
                Map.entry("scripts/build-app-image.sh", "packaging script"),
                Map.entry(".github/workflows/test.yml", "workflow"),
                Map.entry("Makefile", "build input"),
                Map.entry("VERSION", "VERSION feeds"),
                Map.entry("LICENSING.md", "shipped in every package"),
                Map.entry("src/juranometria/tool/RenderingClosure.java", "the guard"),
                Map.entry("src/juranometria/tool/ChangeRoute.java", "the guard"),
                Map.entry("src/juranometria/tool/ChangeBoundary.java", "the guard"),
                Map.entry("src/juranometria/tool/EvidenceGenerators.java", "the guard"),
                Map.entry("src/juranometria/tool/InterfacePhotographers.java",
                        "photographers"),
                Map.entry("src/juranometria/tool/EvidenceContractMain.java",
                        "evidence contract"),
                Map.entry("src/juranometria/tool/EvidenceProvenanceMain.java",
                        "provenance recorder"));
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            Finding f = one(tree(), e.getKey());
            if (f.route() != Route.WIDE || !f.reason().contains(e.getValue())) {
                wrong.add(e.getKey() + ": " + f.route() + " (" + f.reason() + ")");
            }
        }
        assertEquals(List.of(), wrong, "inputs the chart regime consumes"
                + " without compiling are wide whatever the closures say");
    }

    @Test
    void theEntryPointIsWideAndTheQualificationIsInteraction() {
        Finding entry = one(tree(), "src/juranometria/app/JUranometriaMain.java");
        assertEquals(Route.WIDE, entry.route(), "#427, I1: it composes the chart");
        assertTrue(entry.reason().contains("composes modules"), entry.reason());
        assertEquals(Route.INTERACTION, one(tree(),
                "src/juranometria/app/PackagedAcceptanceMain.java").route(),
                "the interaction route runs it on every platform");
    }

    @Test
    void aSourceTheChartReachesIsWideAndSaysHow() {
        Finding f = one(tree(), "src/juranometria/render/ChartRenderer.java");
        assertEquals(Route.WIDE, f.route());
        assertTrue(f.reason().startsWith("reached by a chart producer: ")
                && f.reason().contains("GalleryPageMain -> "), f.reason());
    }

    @Test
    void aSourceInBothClosuresIsWide() {
        Finding f = one(tree(), "src/juranometria/ui/ChartComponent.java");
        assertEquals(Route.WIDE, f.route(), "#427, condition 2: chart wins");
    }

    @Test
    void aSourceOnlyTheInterfaceReachesIsInteraction() {
        for (String path : List.of("src/juranometria/ui/AtlasToolbar.java",
                "src/juranometria/ui/ZoomInteraction.java")) {
            Finding f = one(tree(), path);
            assertEquals(Route.INTERACTION, f.route(), path + ": " + f.reason());
            assertTrue(f.reason().startsWith("reached only by the interface: "),
                    f.reason());
        }
    }

    @Test
    void aRemovedSourceIsWide() {
        Finding f = one(tree(), "src/juranometria/ui/RemovedThing.java");
        assertEquals(Route.WIDE, f.route());
        assertTrue(f.reason().startsWith("removed source"), f.reason());
    }

    @Test
    void proseTestsAndUnreachedCodeAreNarrow() {
        List<String> wrong = new ArrayList<>();
        for (String path : List.of("test/juranometria/ui/ZoomInteractionTest.java",
                "docs/decisions/interaction-ci.md", "docs/development.md",
                "CHANGELOG.md", "README.md",
                "src/juranometria/tool/SomeUnreachedTool.java")) {
            Finding f = one(tree(), path);
            if (f.route() != Route.NARROW) {
                wrong.add(path + ": " + f.route() + " (" + f.reason() + ")");
            }
        }
        assertEquals(List.of(), wrong);
    }

    @Test
    void committedEvidenceTakesItsOwnersRouteAndAnUnknownOwnerIsWide() {
        assertEquals(Route.INTERACTION, one(tree(),
                "docs/studies/interface-language/toolbar-strings.md").route());
        assertEquals(Route.INTERACTION, one(tree(),
                "docs/studies/control-explanations/measurements.md").route());
        assertEquals(Route.WIDE, one(tree(),
                "docs/studies/moon-on-the-chart/june-full-3.png").route());
        Finding unknown = one(tree(), "docs/studies/nobody/report.md");
        assertEquals(Route.WIDE, unknown.route());
        assertTrue(unknown.reason().contains("unresolved"), unknown.reason());
    }

    @Test
    void aCommittedInputIsJudgedByWhoReadsIt() {
        assertEquals(Route.WIDE, one(tree(),
                "docs/studies/solar-system/moon-events-2026.txt").route(),
                "a fixture a chart study reads");
        Stated tree = tree();
        tree.fileReaders.put("docs/studies/sky-language/review-input.tsv",
                new Finding("docs/studies/sky-language/review-input.tsv",
                        Route.INTERACTION, "a committed input only the suite holds"));
        assertEquals(Route.INTERACTION, one(tree,
                "docs/studies/sky-language/review-input.tsv").route(),
                "an input only the suite reads, though it sits beside chart pictures");
    }

    // ---- the language-review ledger, row by row (#432) -----------------

    private static final String PREAMBLE = "# The manual ledger.\n#\n"
            + "identity\tfile\tliteral\tscanner-reason\tdisposition\twhy\n";

    private static String row(String identity, String file, String literal) {
        return identity + "\t" + file + "\t" + literal
                + "\tdeclared name used, but never at a known sink"
                + "\tidentifier\ta stable key; no language changes it\n";
    }

    private static final String TOOLBAR =
            row("1111111111111111", "ui/AtlasToolbar.java", "zoomIn");
    private static final String PAGE =
            row("2222222222222222", "render/ChartRenderer.java", "ICRS");

    /** A ledger changed from {@code base} to {@code head}, judged. */
    private static Finding ledger(Stated tree, String base, String head) {
        if (base != null) {
            tree.base.put(ChangeRoute.LEDGER, base);
        }
        if (head != null) {
            tree.head.put(ChangeRoute.LEDGER, head);
        }
        return one(tree, ChangeRoute.LEDGER);
    }

    @Test
    void aLedgerRowOnInterfaceLanguageIsInteractionAndOnChartLanguageWide() {
        String base = PREAMBLE + TOOLBAR + PAGE;
        assertEquals(Route.INTERACTION, ledger(tree(), base, base
                + row("3333333333333333", "ui/AtlasToolbar.java", "zoomLock")).route(),
                "toolbar or dialog wording: an added row on an interface source");
        assertEquals(Route.INTERACTION, ledger(tree(), base,
                PREAMBLE + TOOLBAR.replace("a stable key", "the button's name")
                        + PAGE).route(),
                "an edited row on an interface source");
        assertEquals(Route.INTERACTION, ledger(tree(), base, PREAMBLE + PAGE).route(),
                "a removed row on an interface source");
        Finding page = ledger(tree(), base, PREAMBLE + TOOLBAR
                + PAGE.replace("a stable key", "notation"));
        assertEquals(Route.WIDE, page.route(), "chart or page wording");
        assertTrue(page.reason().contains("chart or page language"), page.reason());
    }

    @Test
    void aLedgerRowWhoseSourceIsSharedUnknownOrUnreachedIsWide() {
        String base = PREAMBLE + TOOLBAR;
        assertEquals(Route.WIDE, ledger(tree(), base, base
                + row("4444444444444444", "ui/ChartComponent.java", "chart")).route(),
                "a shared source: the chart reaches it too");
        Finding unknown = ledger(tree(), base, base
                + row("5555555555555555", "ui/RemovedPanel.java", "panel"));
        assertEquals(Route.WIDE, unknown.route(), "an unknown or removed source");
        assertTrue(unknown.reason().contains("does not exist"), unknown.reason());
        Finding unreached = ledger(tree(), base, base
                + row("6666666666666666", "tool/SomeStudyMain.java", "study"));
        assertEquals(Route.WIDE, unreached.route(), "a source neither closure reaches");
        assertTrue(unreached.reason().contains("unresolved"), unreached.reason());
        assertEquals(Route.WIDE, ledger(tree(), PREAMBLE + TOOLBAR
                        + row("4444444444444444", "ui/ChartComponent.java", "chart"),
                PREAMBLE + TOOLBAR).route(),
                "removing a row on a shared source is judged by that source too");
    }

    @Test
    void aLedgerChangeMixingChartAndInterfaceRowsIsWide() {
        String base = PREAMBLE + TOOLBAR + PAGE;
        assertEquals(Route.WIDE, ledger(tree(), base, base
                + row("3333333333333333", "ui/AtlasToolbar.java", "zoomLock")
                + row("7777777777777777", "render/ChartRenderer.java", "J2000")).route());
    }

    @Test
    void anythingButAWellFormedRowChangeInTheLedgerIsWide() {
        String base = PREAMBLE + TOOLBAR;
        Map<String, String> heads = new java.util.LinkedHashMap<>();
        heads.put("a row with five columns", base
                + "3333333333333333\tui/AtlasToolbar.java\tx\treason\tidentifier\n");
        heads.put("a row whose identity is not one", base
                + row("not-an-identity!", "ui/AtlasToolbar.java", "x"));
        heads.put("a row whose source is not a source path", base
                + row("3333333333333333", "../secrets/AtlasToolbar.java", "x"));
        heads.put("a disposition that is not one of the six", base
                + row("3333333333333333", "ui/AtlasToolbar.java", "x")
                        .replace("\tidentifier\t", "\treviewed\t"));
        heads.put("a duplicated identity", base + TOOLBAR);
        heads.put("a comment among the rows", base + "# a note\n");
        heads.put("a blank line among the rows", base + "\n"
                + row("3333333333333333", "ui/AtlasToolbar.java", "x"));
        heads.put("an edited preamble", base.replace("The manual ledger.",
                "The manual ledger, revised."));
        heads.put("an edited header", base.replace("\twhy\n", "\treason\n"));
        heads.put("a missing header", "# The manual ledger.\n" + TOOLBAR);
        heads.put("a carriage return", base.replace("\n", "\r\n"));
        heads.put("the rows reordered and nothing else",
                PREAMBLE + row("3333333333333333", "ui/AtlasToolbar.java", "x") + TOOLBAR);
        List<String> notWide = new ArrayList<>();
        for (Map.Entry<String, String> head : heads.entrySet()) {
            String start = head.getKey().equals("the rows reordered and nothing else")
                    ? PREAMBLE + TOOLBAR + row("3333333333333333", "ui/AtlasToolbar.java", "x")
                    : base;
            Finding f = ledger(tree(), start, head.getValue());
            if (f.route() != Route.WIDE) {
                notWide.add(head.getKey() + ": " + f.route() + " (" + f.reason() + ")");
            }
        }
        assertEquals(List.of(), notWide, "every structural edit fails closed");
    }

    @Test
    void aLedgerWithoutItsBaseOrWhoseReaderIsChartCodeIsWide() {
        assertEquals(Route.WIDE, ledger(tree(), null, PREAMBLE + TOOLBAR).route(),
                "without the base every line reads as new, the preamble too");
        assertEquals(Route.WIDE, ledger(tree(), PREAMBLE + TOOLBAR, null).route(),
                "the ledger removed");
        Stated read = tree();
        read.fileReaders.put(ChangeRoute.LEDGER, new Finding(ChangeRoute.LEDGER,
                Route.WIDE, "a committed input chart code reads (SomeChartStudy)"));
        assertEquals(Route.WIDE, ledger(read, PREAMBLE + TOOLBAR, PREAMBLE + TOOLBAR
                + row("3333333333333333", "ui/AtlasToolbar.java", "x")).route(),
                "chart code that reads the ledger makes every change to it wide");
    }

    @Test
    void aLanguageFileIsJudgedKeyByKeyAndFailsClosed() {
        String path = "src/resources/interface-language/en.properties";
        Stated tree = tree();
        tree.base.put(path, "toolbar.zoomIn.label = Zoom in\npage.body.moon = Moon\n");

        tree.head.put(path, "toolbar.zoomIn.label = Zoom in closer\npage.body.moon = Moon\n");
        assertEquals(Route.INTERACTION, one(tree, path).route(),
                "only an interface key changed");

        tree.head.put(path, "toolbar.zoomIn.label = Zoom in\npage.body.moon = The Moon\n");
        Finding chart = one(tree, path);
        assertEquals(Route.WIDE, chart.route(), "a chart key changed");
        assertTrue(chart.reason().contains("page.body.moon"), chart.reason());

        tree.head.put(path, "toolbar.zoomIn.label = Zoom in\npage.body.moon = Moon\n"
                + "nobody.reads.this = ?\n");
        Finding unknown = one(tree, path);
        assertEquals(Route.WIDE, unknown.route(), "a key nothing names");
        assertTrue(unknown.reason().contains("unresolved"), unknown.reason());

        tree.head.put(path, "# a comment\npage.body.moon = Moon\n"
                + "toolbar.zoomIn.label = Zoom in\n");
        assertEquals(Route.INTERACTION, one(tree, path).route(),
                "comments and order only: no key changed");

        Stated noBase = tree();
        noBase.head.put(path, "toolbar.zoomIn.label = Zoom in\npage.body.moon = Moon\n");
        assertEquals(Route.WIDE, one(noBase, path).route(),
                "without the base every key reads as new, so it can only widen");
    }

    @Test
    void theProvenanceRecordIsJudgedRowByRow() {
        String path = ChangeRoute.PROVENANCE;
        String photo = "| `docs/studies/interface-language/toolbar-en-1.png` | aa | 2026-10-01 | m | ToolbarSheetMain |";
        String chart = "| `docs/studies/moon-on-the-chart/june-full-3.png` | bb | 2026-10-01 | m | various |";
        Stated tree = tree();
        tree.base.put(path, "# Provenance\n" + photo + "\n" + chart + "\n");
        tree.head.put(path, "# Provenance\n" + photo.replace("aa", "cc") + "\n" + chart + "\n");
        assertEquals(Route.INTERACTION, one(tree, path).route(),
                "only a photograph's row moved");
        tree.head.put(path, "# Provenance\n" + photo + "\n" + chart.replace("bb", "dd") + "\n");
        assertEquals(Route.WIDE, one(tree, path).route(), "a chart picture's row moved");
    }

    @Test
    void theRouteOfAChangeIsTheWidestAnyPathTakes() {
        Stated tree = tree();
        assertEquals(Route.NARROW, ChangeRoute.routeOf(ChangeRoute.classify(
                List.of("docs/development.md", "test/juranometria/ui/XTest.java"),
                tree, PRESENT)));
        assertEquals(Route.INTERACTION, ChangeRoute.routeOf(ChangeRoute.classify(
                List.of("docs/development.md", "src/juranometria/ui/AtlasToolbar.java"),
                tree, PRESENT)));
        assertEquals(Route.WIDE, ChangeRoute.routeOf(ChangeRoute.classify(
                List.of("src/juranometria/ui/AtlasToolbar.java",
                        "src/juranometria/render/ChartRenderer.java"), tree, PRESENT)));
        assertEquals(Route.NARROW, ChangeRoute.routeOf(List.of()),
                "nothing changed reaches nothing");
    }

    @Test
    void theGuardNamesItsOwnFilesAndTheyExist() {
        List<String> missing = new ArrayList<>();
        for (ChangeRoute.NamedRule rule : ChangeRoute.NAMED) {
            for (String candidate : List.of(
                    "src/juranometria/tool/RenderingClosure.java",
                    "src/juranometria/tool/ChangeRoute.java",
                    "src/juranometria/tool/ChangeClassifierMain.java",
                    "src/juranometria/tool/ChangeBoundary.java",
                    "src/juranometria/tool/EvidenceGenerators.java",
                    "src/juranometria/tool/InterfacePhotographers.java",
                    "src/juranometria/app/JUranometriaMain.java",
                    "src/juranometria/app/PackagedAcceptanceMain.java")) {
                if (rule.matches().test(candidate)
                        && !Files.isRegularFile(Path.of(candidate))) {
                    missing.add(candidate);
                }
            }
        }
        assertEquals(List.of(), missing, "every file the rules name exists");
    }

    // ---- the real tree, held to the ruling's own examples -------------

    @Test
    void onTheRealTreeTheChartIsWideAndTheInterfaceIsInteraction()
            throws IOException {
        RenderingClosure classes = RenderingClosure.of(Path.of("build/classes"));
        ChangeBoundary real = ChangeBoundary.of(classes, Path.of("."), null);
        Map<String, Route> expected = new java.util.LinkedHashMap<>();
        expected.put("src/juranometria/render/ChartRenderer.java", Route.WIDE);
        expected.put("src/juranometria/ui/ChartComponent.java", Route.WIDE);
        expected.put("src/juranometria/ui/ReferenceInk.java", Route.WIDE);
        expected.put("src/juranometria/ui/AtlasToolbar.java", Route.INTERACTION);
        expected.put("src/juranometria/ui/ZoomInteraction.java", Route.INTERACTION);
        expected.put("docs/studies/moon-on-the-chart/june-full-3.png", Route.WIDE);
        expected.put("docs/studies/interface-language/toolbar-strings.md",
                Route.INTERACTION);
        expected.put("docs/studies/control-explanations/measurements.md",
                Route.INTERACTION);
        expected.put("docs/studies/solar-system/moon-events-2026.txt", Route.WIDE);
        expected.put("test/juranometria/ui/ZoomInteractionTest.java", Route.NARROW);
        List<Finding> findings = ChangeRoute.classify(expected.keySet(), real,
                ChangeRoute::existsInTree);
        List<String> wrong = new ArrayList<>();
        for (Finding f : findings) {
            if (f.route() != expected.get(f.path())) {
                wrong.add(f.path() + ": " + f.route() + " (" + f.reason() + ")");
            }
        }
        assertEquals(List.of(), wrong);
        // Keys: an interface key is interaction, a page word and a key
        // nothing names are wide.
        assertEquals(Route.INTERACTION, real.consumersOf("toolbar.zoomIn.tooltip").route());
        assertEquals(Route.WIDE, real.consumersOf("page.body.sun").route());
        assertEquals(Route.WIDE, real.consumersOf("nothing.names.this.key").route());
    }

    /**
     * The exact change #430 made to the ledger - two appended rows, for
     * the zoom lock's component name and preference key - is
     * interaction against the real closures. Its base is rebuilt from
     * the committed ledger without those two rows, which is byte for
     * byte the ledger at {@code 99f6077}: the move to its own
     * directory changed no byte, and #430 changed nothing else.
     */
    @Test
    void onTheRealTreeTheZoomLockRowsAreInteractionAndPageRowsWide()
            throws IOException {
        RenderingClosure classes = RenderingClosure.of(Path.of("build/classes"));
        ChangeBoundary real = ChangeBoundary.of(classes, Path.of("."), null);
        String head = Files.readString(Path.of(ChangeRoute.LEDGER));
        assertEquals(Path.of(ChangeRoute.LEDGER), SkyLanguageLedger.RECORD,
                "the classifier judges the ledger the scanner is held to");
        List<String> zoomLock = new ArrayList<>();
        StringBuilder base = new StringBuilder();
        for (String line : head.split("\n", -1)) {
            if (line.startsWith("e78c0407f7115bd3\tui/ChartControls.java\tzoomLock\t")
                    || line.startsWith("43f2af373668c4ab\tui/ZoomLockStore.java\tzoomLocked\t")) {
                zoomLock.add(line);
            } else {
                base.append(line).append('\n');
            }
        }
        assertEquals(2, zoomLock.size(), "both of #430's rows are committed");
        String before = base.substring(0, base.length() - 1);

        Finding found = ChangeRoute.classify(List.of(ChangeRoute.LEDGER),
                comparing(real, before, head), ChangeRoute::existsInTree).get(0);
        assertEquals(Route.INTERACTION, found.route(), found.reason());
        assertTrue(found.reason().startsWith("2 ledger row(s)"), found.reason());

        // A row on page wording - a sheet writer's - is wide.
        String sheetRow = null;
        for (String line : head.split("\n")) {
            if (line.contains("\tsheet/PdfSheetWriter.java\t")) {
                sheetRow = line;
                break;
            }
        }
        assertTrue(sheetRow != null, "the ledger holds a sheet writer's row");
        String edited = head.replace(sheetRow, sheetRow + " (revised)");
        Finding page = ChangeRoute.classify(List.of(ChangeRoute.LEDGER),
                comparing(real, head, edited), ChangeRoute::existsInTree).get(0);
        assertEquals(Route.WIDE, page.route(), page.reason());

        // The ledger has a reader of its own now, and no chart producer
        // names its directory; the directory rule is unchanged for its
        // former neighbours.
        assertEquals(Route.INTERACTION,
                real.readersOfFile(ChangeRoute.LEDGER).orElseThrow().route());
        assertEquals(Route.WIDE, real.readersOfFile(
                "docs/studies/sky-language/iau-constellations.tsv").orElseThrow().route(),
                "the IAU identities beside the chart's language study stay wide");
    }

    /** The real boundary, with a stated base and head for one file. */
    private static ChangeRoute.Boundary comparing(ChangeBoundary real,
                                                  String base, String head) {
        return new ChangeRoute.Boundary() {
            public Optional<String> chartChain(String s) { return real.chartChain(s); }
            public Optional<String> interactionChain(String s) { return real.interactionChain(s); }
            public Optional<Route> ownerOf(String p) { return real.ownerOf(p); }
            public Finding consumersOf(String k) { return real.consumersOf(k); }
            public Optional<Finding> readersOfFile(String p) { return real.readersOfFile(p); }
            public Optional<String> base(String p) { return Optional.of(base); }
            public Optional<String> head(String p) { return Optional.of(head); }
        };
    }

    @Test
    void theGeneratorsSplitAndEveryPhotographerIsRegistered() throws IOException {
        RenderingClosure classes = RenderingClosure.of(Path.of("build/classes"));
        EvidenceGenerators generators = EvidenceGenerators.of(classes,
                Path.of("docs/studies"));
        assertTrue(generators.of(EvidenceGenerators.Kind.CHART)
                .contains("juranometria/tool/GalleryPageMain"),
                "the gallery draws chart pictures");
        assertTrue(generators.of(EvidenceGenerators.Kind.REPRODUCED)
                .contains("juranometria/tool/ControlExplanationStudyMain"),
                "the control audit commits only reports");
        assertEquals(Set.copyOf(InterfacePhotographers.ALL.keySet().stream()
                        .map(name -> "juranometria/tool/" + name).toList()),
                Set.copyOf(generators.of(EvidenceGenerators.Kind.PHOTOGRAPHER)),
                "the photographers come from their registry");
        for (String photographer : generators.of(EvidenceGenerators.Kind.PHOTOGRAPHER)) {
            assertTrue(classes.classes().contains(photographer),
                    photographer + " is a compiled class");
        }
    }

    @Test
    void aRecipeOfSplicedValuesNamesNoKey() {
        assertTrue(ChangeBoundary.covers("menu.\u0001.label", "menu.ecliptic.label"));
        assertTrue(!ChangeBoundary.covers("\u0001\u0001\u0001", "menu.ecliptic.label"),
                "a recipe with no literal names nothing");
        assertTrue(!ChangeBoundary.covers("\u0001.\u0001", "menu.ecliptic.label"),
                "a dot between two values names nothing");
        assertTrue(!ChangeBoundary.covers("\u0001.label", "toolbar.zoomlock.label",
                Set.of("symbolfamily.")),
                "a suffix recipe names only what the closure can splice in front");
        assertTrue(ChangeBoundary.covers("\u0001.label", "symbolfamily.nebula.label",
                Set.of("symbolfamily.")));
        assertTrue(ChangeBoundary.covers("moontable", "moontable.phase.nearNew"),
                "a stem continued with a dot");
        assertTrue(ChangeBoundary.covers("page.body.", "page.body.sun"),
                "a prefix a caller appends to");
    }
}
