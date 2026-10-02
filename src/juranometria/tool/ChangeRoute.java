package juranometria.tool;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Which route a change takes through CI: narrow, interaction or wide
 * (issue #398, extended by #427/#428).
 *
 * <p><strong>Wide</strong> is the whole regime - every chart
 * generator reproduced, the native images and the portable archive.
 * <strong>Interaction</strong> is for work on the application's
 * interface that cannot reach chart ink: the unit and display suites,
 * the interface and report evidence reproduced, the native images
 * with packaged acceptance and the portable archive - but no chart
 * producer runs, and the run proves no chart picture or provenance
 * row moved. <strong>Narrow</strong> is prose, tests and code no
 * shipped program reaches.
 *
 * <p>Fail closed, as the owner ruled (#427): a path reached by a chart
 * producer is wide even if the interface reaches it too; a path, key
 * or row whose consumers cannot be resolved is wide; and the inputs
 * the chart regime consumes without compiling stay wide by name.
 * Both closures are derived - from the registered generators and the
 * compiled code - never listed.
 */
public final class ChangeRoute {

    /** The three routes, cheapest first. */
    public enum Route {
        /** Unit and display suites only. */
        NARROW,
        /** Interface work: no chart producer runs, nothing chart moves. */
        INTERACTION,
        /** Everything: evidence, native images, portable archive. */
        WIDE
    }

    /** One changed path and what was decided about it. */
    public record Finding(String path, Route route, String reason) {
    }

    /** A named rule: paths it matches take this route for this reason. */
    record NamedRule(String reason, Route route, Predicate<String> matches) {
    }

    /**
     * What the classifier needs to know about the tree, as one seam,
     * so the rules can be proved against a boundary a test states.
     */
    public interface Boundary {

        /** How a chart producer reaches this source, if one does. */
        Optional<String> chartChain(String source);

        /** How an interface root reaches this source, if one does. */
        Optional<String> interactionChain(String source);

        /**
         * Who owns a committed studies file: {@code WIDE} for a chart
         * producer's, {@code INTERACTION} for a reproduced generator's
         * or a photographer's, empty when nothing registered owns it.
         */
        Optional<Route> ownerOf(String studiesPath);

        /**
         * Which code reads a language key: a finding whose route is
         * {@code WIDE} when chart code can resolve it or nothing can,
         * {@code INTERACTION} when only interface code can.
         */
        Finding consumersOf(String key);

        /**
         * Which code reads a committed input file - a fixture, a
         * ledger - by naming its path: {@code WIDE} when chart code
         * does, {@code INTERACTION} when only interface code does,
         * empty when no compiled code names it.
         */
        Optional<Finding> readersOfFile(String path);

        /** The file as it is at the merge base, if it existed there. */
        Optional<String> base(String path);

        /** The file as it is now, if it exists. */
        Optional<String> head(String path);
    }

    /**
     * The inputs the chart regime consumes without compiling them,
     * and the two application entry points as ruled. Prefix or exact
     * paths, relative to the repository root.
     */
    static final List<NamedRule> NAMED = List.of(
            new NamedRule("committed reference image compared by the"
                    + " image workflow", Route.WIDE, under("docs/reference/")),
            new NamedRule("committed gallery pages, generated and"
                    + " published", Route.WIDE, under("docs/gallery/")),
            new NamedRule("packaging input", Route.WIDE, under("packaging/")),
            new NamedRule("build or packaging script", Route.WIDE,
                    under("scripts/")),
            new NamedRule("workflow: the gates themselves", Route.WIDE,
                    under(".github/workflows/")),
            new NamedRule("build input", Route.WIDE, exactly("Makefile")),
            new NamedRule("VERSION feeds committed artifacts", Route.WIDE,
                    exactly("VERSION")),
            new NamedRule("shipped in every package by make dist", Route.WIDE,
                    exactly("LICENSING.md")),
            new NamedRule("the packaged application's entry point: it"
                    + " composes modules into the chart (#427, I1)",
                    Route.WIDE,
                    exactly("src/juranometria/app/JUranometriaMain.java")),
            new NamedRule("the packaged application's qualification, which"
                    + " the interaction route runs on every platform",
                    Route.INTERACTION,
                    exactly("src/juranometria/app/PackagedAcceptanceMain.java")),
            new NamedRule("the guard itself", Route.WIDE,
                    exactly("src/juranometria/tool/RenderingClosure.java")),
            new NamedRule("the guard itself", Route.WIDE,
                    exactly("src/juranometria/tool/ChangeRoute.java")),
            new NamedRule("the guard itself", Route.WIDE,
                    exactly("src/juranometria/tool/ChangeClassifierMain.java")),
            new NamedRule("the guard itself", Route.WIDE,
                    exactly("src/juranometria/tool/ChangeBoundary.java")),
            new NamedRule("the guard itself", Route.WIDE,
                    exactly("src/juranometria/tool/EvidenceGenerators.java")),
            new NamedRule("the guard's registry of photographers", Route.WIDE,
                    exactly("src/juranometria/tool/InterfacePhotographers.java")),
            new NamedRule("the evidence contract and its registries",
                    Route.WIDE,
                    exactly("src/juranometria/tool/EvidenceContractMain.java")),
            new NamedRule("the provenance recorder", Route.WIDE,
                    exactly("src/juranometria/tool/EvidenceProvenanceMain.java")));

    /** The provenance record, judged row by row. */
    static final String PROVENANCE = "docs/studies/PROVENANCE.md";

    /**
     * The language-review ledger, judged row by row (#432). It lives
     * in a directory of its own so that no study generator appears to
     * read it merely by naming the folder it shares.
     */
    static final String LEDGER =
            "docs/studies/language-ledger/manual-review.tsv";

    /** The directory a ledger row's {@code file} column is relative to. */
    private static final String LEDGER_SOURCES = "src/juranometria/";

    /** The line that ends the ledger's preamble; only rows follow it. */
    private static final String LEDGER_HEADER =
            "identity\tfile\tliteral\tscanner-reason\tdisposition\twhy";

    private static final Pattern LEDGER_IDENTITY =
            Pattern.compile("[0-9a-f]{16}");

    private static final Pattern LEDGER_SOURCE =
            Pattern.compile("[A-Za-z0-9_]+(/[A-Za-z0-9_]+)*\\.java");

    private static final String LANGUAGES = "src/resources/interface-language/";

    private ChangeRoute() {
    }

    /**
     * Judges every changed path.
     *
     * @param changed paths relative to the repository root
     * @param tree    what the classifier knows about the tree
     * @param present whether a path still exists; a removed source
     *                cannot be traced, so it is wide by caution
     */
    public static List<Finding> classify(Collection<String> changed,
                                         Boundary tree,
                                         Predicate<String> present) {
        List<Finding> findings = new ArrayList<>();
        for (String path : changed) {
            findings.add(judge(path, tree, present));
        }
        return findings;
    }

    private static Finding judge(String path, Boundary tree,
                                 Predicate<String> present) {
        for (NamedRule rule : NAMED) {
            if (rule.matches().test(path)) {
                return new Finding(path, rule.route(), rule.reason());
            }
        }
        if (path.startsWith(LANGUAGES) && path.endsWith(".properties")) {
            return keys(path, tree);
        }
        if (path.startsWith("src/resources/")) {
            return new Finding(path, Route.WIDE, "bundled resource: read by"
                    + " renderers and generators, and shipped in every package");
        }
        if (path.equals(PROVENANCE)) {
            return rows(path, tree);
        }
        if (path.equals(LEDGER)) {
            return ledger(path, tree, present);
        }
        if (path.startsWith("docs/studies/")) {
            // A committed input is judged by who reads it, not by the
            // directory it happens to sit in (#428): a review ledger
            // beside a study's pictures is the ledger's readers'.
            if ("byte-exact-fixture".equals(TestEvidenceScan.artifactClass(
                    Path.of(path).getFileName().toString()))) {
                Optional<Finding> read = tree.readersOfFile(path);
                if (read.isPresent()) {
                    return new Finding(path, read.get().route(),
                            read.get().reason());
                }
            }
            Optional<Route> owner = tree.ownerOf(path);
            if (owner.isEmpty()) {
                return new Finding(path, Route.WIDE, "committed evidence"
                        + " no registered generator owns: unresolved");
            }
            return owner.get() == Route.WIDE
                    ? new Finding(path, Route.WIDE,
                            "committed evidence a chart producer owns")
                    : new Finding(path, Route.INTERACTION,
                            "committed evidence the interaction route"
                                    + " reproduces");
        }
        if (path.startsWith("src/")) {
            if (!present.test(path)) {
                return new Finding(path, Route.WIDE, "removed source: what"
                        + " depended on it at the base cannot be read from"
                        + " the build at the head");
            }
            Optional<String> chart = tree.chartChain(path);
            if (chart.isPresent()) {
                return new Finding(path, Route.WIDE,
                        "reached by a chart producer: " + chart.get());
            }
            Optional<String> interaction = tree.interactionChain(path);
            if (interaction.isPresent()) {
                return new Finding(path, Route.INTERACTION,
                        "reached only by the interface: " + interaction.get());
            }
            return new Finding(path, Route.NARROW,
                    "source no shipped program or generator reaches");
        }
        return new Finding(path, Route.NARROW,
                "not an input of the application or its evidence");
    }

    /**
     * A language file, key by key (#427, I2): interaction only when
     * every key that changed is read by interface code alone.
     */
    private static Finding keys(String path, Boundary tree) {
        Map<String, String> before = properties(tree.base(path));
        Optional<String> now = tree.head(path);
        if (now.isEmpty()) {
            return new Finding(path, Route.WIDE,
                    "language file removed: its readers cannot be judged");
        }
        Map<String, String> after = properties(now);
        TreeSet<String> changed = new TreeSet<>();
        for (String key : before.keySet()) {
            if (!before.get(key).equals(after.get(key))) {
                changed.add(key);
            }
        }
        for (String key : after.keySet()) {
            if (!after.get(key).equals(before.get(key))) {
                changed.add(key);
            }
        }
        if (changed.isEmpty()) {
            return new Finding(path, Route.INTERACTION,
                    "no key changed (comments or order only)");
        }
        for (String key : changed) {
            Finding read = tree.consumersOf(key);
            if (read.route() != Route.INTERACTION) {
                return new Finding(path, Route.WIDE,
                        "key " + key + ": " + read.reason());
            }
        }
        return new Finding(path, Route.INTERACTION, changed.size()
                + " changed key(s), every one read by interface code alone");
    }

    /**
     * The provenance record, row by row: interaction only when every
     * row that changed is for evidence the interaction route owns.
     */
    private static Finding rows(String path, Boundary tree) {
        Map<String, String> before = provenanceRows(tree.base(path));
        Map<String, String> after = provenanceRows(tree.head(path));
        if (before.isEmpty() && after.isEmpty()) {
            return new Finding(path, Route.WIDE,
                    "provenance record unreadable: unresolved");
        }
        TreeSet<String> changed = new TreeSet<>();
        for (String artifact : before.keySet()) {
            if (!before.get(artifact).equals(after.get(artifact))) {
                changed.add(artifact);
            }
        }
        for (String artifact : after.keySet()) {
            if (!after.get(artifact).equals(before.get(artifact))) {
                changed.add(artifact);
            }
        }
        for (String artifact : changed) {
            Optional<Route> owner = tree.ownerOf(artifact);
            if (owner.isEmpty() || owner.get() != Route.INTERACTION) {
                return new Finding(path, Route.WIDE, "provenance row for "
                        + artifact + (owner.isEmpty()
                                ? ", which nothing registered owns"
                                : ", a chart producer's picture"));
            }
        }
        return new Finding(path, Route.INTERACTION, changed.size()
                + " provenance row(s), every one for interface evidence");
    }

    /**
     * The language-review ledger, row by row (#432): interaction only
     * when every row that changed - added, removed or edited, before
     * and after - names a source only the interface reaches. A row on
     * chart or page language is wide; so is a row whose source is
     * shared with the chart, missing or unreached, a malformed line,
     * and any edit to the preamble or to the order of the rows. Before
     * any row is read, the ledger's own readers are judged, so chart
     * code that ever came to read the ledger makes every change wide.
     */
    private static Finding ledger(String path, Boundary tree,
                                  Predicate<String> present) {
        Optional<Finding> read = tree.readersOfFile(path);
        if (read.isPresent() && read.get().route() == Route.WIDE) {
            return new Finding(path, Route.WIDE, read.get().reason());
        }
        Optional<String> now = tree.head(path);
        if (now.isEmpty()) {
            return new Finding(path, Route.WIDE,
                    "ledger removed: what its rows covered cannot be judged");
        }
        Ledger before = Ledger.of(tree.base(path).orElse(""));
        Ledger after = Ledger.of(now.get());
        if (after.malformed() != null) {
            return new Finding(path, Route.WIDE,
                    "malformed ledger line: " + after.malformed());
        }
        if (before.malformed() != null) {
            return new Finding(path, Route.WIDE,
                    "the base's ledger is malformed: unresolved");
        }
        if (!before.preamble().equals(after.preamble())) {
            return new Finding(path, Route.WIDE, "an edit outside the"
                    + " ledger's rows (its preamble or header)");
        }
        TreeSet<String> changed = new TreeSet<>();
        for (String identity : before.rows().keySet()) {
            if (!before.rows().get(identity).equals(
                    after.rows().get(identity))) {
                changed.add(identity);
            }
        }
        for (String identity : after.rows().keySet()) {
            if (!after.rows().get(identity).equals(
                    before.rows().get(identity))) {
                changed.add(identity);
            }
        }
        if (changed.isEmpty()) {
            return new Finding(path, Route.WIDE, "the ledger changed but"
                    + " no row did (the rows' order): unresolved");
        }
        for (String identity : changed) {
            for (Ledger rows : List.of(before, after)) {
                String row = rows.rows().get(identity);
                if (row == null) {
                    continue;
                }
                Finding source = ledgerSource(row.split("\t", -1)[1],
                        tree, present);
                if (source.route() != Route.INTERACTION) {
                    return new Finding(path, Route.WIDE, "ledger row "
                            + identity + ": " + source.reason());
                }
            }
        }
        return new Finding(path, Route.INTERACTION, changed.size()
                + " ledger row(s), every one on a source only the"
                + " interface reaches");
    }

    /** Which closure a ledger row's source belongs to. */
    private static Finding ledgerSource(String file, Boundary tree,
                                        Predicate<String> present) {
        String source = LEDGER_SOURCES + file;
        if (!present.test(source)) {
            return new Finding(source, Route.WIDE, source
                    + " does not exist: an unknown or removed source");
        }
        Optional<String> chart = tree.chartChain(source);
        if (chart.isPresent()) {
            return new Finding(source, Route.WIDE,
                    "chart or page language, reached by a chart producer: "
                            + chart.get());
        }
        Optional<String> interaction = tree.interactionChain(source);
        if (interaction.isPresent()) {
            return new Finding(source, Route.INTERACTION,
                    "interface language: " + interaction.get());
        }
        return new Finding(source, Route.WIDE,
                source + " is reached by neither closure: unresolved");
    }

    /**
     * The ledger split into its preamble - every line up to and
     * including the header - and its rows by identity, in order. The
     * first line that is not a well-formed row after the header, or a
     * missing header, is kept as {@code malformed}.
     */
    record Ledger(List<String> preamble, Map<String, String> rows,
                  String malformed) {

        static Ledger of(String text) {
            List<String> preamble = new ArrayList<>();
            Map<String, String> rows = new LinkedHashMap<>();
            if (text.isEmpty()) {
                return new Ledger(preamble, rows, null);
            }
            if (text.indexOf('\r') >= 0) {
                return new Ledger(preamble, rows, "a carriage return");
            }
            String[] lines = text.split("\n", -1);
            int at = 0;
            boolean header = false;
            for (; at < lines.length && !header; at++) {
                preamble.add(lines[at]);
                header = lines[at].equals(LEDGER_HEADER);
            }
            if (!header) {
                return new Ledger(preamble, rows, "no header line");
            }
            for (; at < lines.length; at++) {
                String line = lines[at];
                if (line.isEmpty() && at == lines.length - 1) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                boolean wellFormed = columns.length == 6
                        && LEDGER_IDENTITY.matcher(columns[0]).matches()
                        && LEDGER_SOURCE.matcher(columns[1]).matches()
                        && SkyLanguageLedger.DISPOSITIONS.contains(columns[4])
                        && !columns[5].isBlank()
                        && !rows.containsKey(columns[0]);
                if (!wellFormed) {
                    return new Ledger(preamble, rows,
                            "line " + (at + 1) + ": " + line);
                }
                rows.put(columns[0], line);
            }
            return new Ledger(preamble, rows, null);
        }
    }

    static Map<String, String> properties(Optional<String> text) {
        Map<String, String> keys = new TreeMap<>();
        if (text.isEmpty()) {
            return keys;
        }
        Properties loaded = new Properties();
        try {
            loaded.load(new StringReader(text.get()));
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        for (String key : loaded.stringPropertyNames()) {
            keys.put(key, loaded.getProperty(key));
        }
        return keys;
    }

    /** The record's rows by artifact path: {@code | `path` | ... |}. */
    static Map<String, String> provenanceRows(Optional<String> text) {
        Map<String, String> rows = new TreeMap<>();
        if (text.isEmpty()) {
            return rows;
        }
        for (String line : text.get().split("\n")) {
            if (line.startsWith("| `")) {
                int end = line.indexOf('`', 3);
                if (end > 3) {
                    rows.put(line.substring(3, end), line);
                }
            }
        }
        return rows;
    }

    /** The route of the whole change: the widest any path takes. */
    public static Route routeOf(List<Finding> findings) {
        Route widest = Route.NARROW;
        for (Finding finding : findings) {
            if (finding.route().compareTo(widest) > 0) {
                widest = finding.route();
            }
        }
        return widest;
    }

    /** Whether a path exists as a regular file in the working tree. */
    public static boolean existsInTree(String path) {
        return Files.isRegularFile(Path.of(path));
    }

    private static Predicate<String> under(String prefix) {
        return path -> path.startsWith(prefix);
    }

    private static Predicate<String> exactly(String exact) {
        return exact::equals;
    }
}
