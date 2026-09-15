package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import juranometria.catalog.Sha256;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every string the production sources hold, and what kind of string
 * it is (Sprint 33, issue #347).
 *
 * <p>The sprint gate asks which reader-visible words a localised sky
 * would have to reach. Counting string literals answers a different
 * and much easier question: the production packages hold about 1,900
 * of them, and the overwhelming majority are preference keys, format
 * patterns, file extensions, diagnostics and astronomical notation
 * that must not be translated at all. A number that large is not an
 * audit, it is a corpus. This classifies it.
 *
 * <p>The rules are textual and deliberately simple - a literal is
 * judged by the call it sits in - so the same rule applied twice
 * gives the same answer and a fixture can prove a rule catches what
 * it names. Textual rules are also why {@link Kind#UNCLASSIFIED}
 * exists and is reported rather than absorbed: a literal the rules
 * cannot place is evidence about the rules, and forcing it into the
 * nearest category to reach a tidy zero would hide exactly the
 * strings an audit is for.
 *
 * <p>Reader-visible text does not arrive only through setters. It is
 * handed to constructors ({@code new JLabel("...")}), composed by
 * {@code String.format}, returned from table models, built by menu
 * factories and attached as accessible descriptions. A rule set that
 * knew only {@code setText} would report a confident, wrong, small
 * number, so the sinks below name all of those routes and
 * {@code SkyLanguageScanTest} seeds a string through each one.
 *
 * <p>Process locale is not product language, and the distinction is
 * classified rather than assumed. {@code Locale.ROOT} on a number,
 * a case fold, a file name or an evidence record is usually correct
 * precisely because it must not follow the reader - swapping those
 * for an interface locale would make evidence unreproducible and
 * file names machine-dependent. Those uses are reported as
 * {@link Locale#DETERMINISTIC}; uses that format something a reader
 * reads are reported as {@link Locale#READER_FACING}.
 */
public final class SkyLanguageScan {

    private SkyLanguageScan() {
    }

    /** What a string literal is for. */
    public enum Kind {
        /** Words a reader reads, and a localised build must carry. */
        VISIBLE_PROSE,
        /** Words only a screen reader speaks; still reader-visible. */
        ACCESSIBILITY,
        /** Astronomical notation: never translated, by convention. */
        NOTATION,
        /** Stable identifiers: preference keys, ids, family names. */
        IDENTIFIER,
        /** Format patterns and separators carrying no words. */
        FORMAT,
        /** File names, extensions, paths, MIME and command names. */
        FILENAME,
        /** Diagnostics: exceptions, logs, developer-facing failure. */
        DIAGNOSTIC,
        /** Markup, page-description operators, serialisation. */
        STRUCTURAL,
        /** The rules could not place it; reported, never absorbed. */
        UNCLASSIFIED
    }

    /**
     * Which surface a string belongs to.
     *
     * <p>Compiled source is not reader runtime. The generators under
     * {@code tool/} and the packaged acceptance harness are built
     * from the same tree and ship inside the same image, but their
     * words reach a build log, never a reader, and letting hundreds
     * of them into the queue would bury the surface that matters.
     *
     * <p>The two diagnostic surfaces are split by tracing rather
     * than by assumption. {@code StartupFailure.describe} walks a
     * failure's cause chain and puts up to four {@code getMessage}
     * strings <em>verbatim</em> into a {@code JOptionPane} - "what
     * failed, in the loader's own words". So an exception thrown on
     * the loading path is read by a reader, in a dialog, at the
     * worst possible moment, and calling it developer-only because
     * it is thrown deep in production would be wrong.
     */
    public enum Surface {
        /** The running application's own windows and controls. */
        APPLICATION,
        /** Sheets a reader exports and opens elsewhere. */
        EXPORT,
        /** Generators, studies and the acceptance harness. */
        DEVELOPER_TOOLS,
        /** Thrown where {@code StartupFailure} can quote it aloud. */
        READER_REACHABLE_DIAGNOSTIC,
        /** Diagnostics that reach a log, a test or a terminal only. */
        CONFINED_DIAGNOSTIC
    }

    /**
     * Packages whose failures the startup dialog can quote.
     *
     * <p>Named and reviewable rather than inferred: these are the
     * loaders {@code Atlas} runs before a window exists, so a
     * failure in them propagates to {@code JUranometriaMain} and is
     * shown. A reviewer can check this list against the launch path
     * without re-deriving it.
     */
    private static final List<String> QUOTED_ON_FAILURE = List.of(
            "juranometria/catalog/", "juranometria/geo/",
            "juranometria/search/", "juranometria/app/Atlas",
            "juranometria/app/AppInfo", "juranometria/app/StartupFailure",
            "juranometria/app/JUranometriaMain");

    /** How a locale argument is used. */
    public enum Locale {
        /** Deterministic by intent: numbers, folds, names, evidence. */
        DETERMINISTIC,
        /** Formats something a reader reads. */
        READER_FACING,
        /** The process default, which follows the machine. */
        AMBIENT
    }

    /**
     * One classified literal.
     *
     * <p>{@code reason} is empty for a literal the rules placed. For
     * an {@link Kind#UNCLASSIFIED} one it names the limitation that
     * stopped them, because a single undifferentiated bucket is
     * numerically honest and practically useless: #350 needs to know
     * whether a string was missed because no route was recognised or
     * because it travels through a collection this scan cannot
     * follow, and those two are inspected in different ways.
     */
    public record Literal(String path, int line, String text, Kind kind,
                          String route, String reason, String context) {

        /** A literal the rules placed; no limitation to name. */
        Literal(String path, int line, String text, Kind kind, String route) {
            this(path, line, text, kind, route, "", "");
        }

        Literal(String path, int line, String text, Kind kind, String route,
                String reason) {
            this(path, line, text, kind, route, reason, "");
        }

        /**
         * How firmly this classification is known.
         *
         * <p>A traced literal is published by a named call. A
         * recognised one is a sentence in a reader-facing class that
         * no rule could follow to a call - included because leaving
         * real words out of the queue is the worse error, but it is
         * a <em>candidate</em>, not a proven reader string, and the
         * label has to survive into #350 so a heuristic never
         * hardens into a product claim.
         */
        public Confidence confidence() {
            return route().startsWith("recognised")
                    ? Confidence.RECOGNISED : Confidence.TRACED;
        }

        /** Where the wording is defined. */
        public Surface surface() {
            return surfaceOf(path, kind);
        }

        /** Whether a reader can meet this string at all. */
        public boolean readersOwn() {
            return (kind == Kind.VISIBLE_PROSE
                        || kind == Kind.ACCESSIBILITY)
                    && surface() != Surface.DEVELOPER_TOOLS
                    || surface() == Surface.READER_REACHABLE_DIAGNOSTIC;
        }

        /**
         * Every surface this string can reach, sorted.
         *
         * <p>Derived from what the string is and who owns it. Chart
         * and page text is replayed into the sheet writers by one
         * recording, so a word drawn on the chart is emitted by all
         * three formats as well as the screen; a sheet's own
         * metadata reaches the three files but never the screen; an
         * accessible description is spoken and not drawn.
         */
        public java.util.SortedSet<Emitter> emitters() {
            java.util.SortedSet<Emitter> reaches =
                    new java.util.TreeSet<>();
            switch (kind) {
                case ACCESSIBILITY -> reaches.add(Emitter.ACCESSIBILITY);
                case FILENAME -> reaches.add(Emitter.FILENAME);
                case DIAGNOSTIC -> {
                    reaches.add(Emitter.LOG);
                    if (surface() == Surface.READER_REACHABLE_DIAGNOSTIC) {
                        reaches.add(Emitter.SCREEN);
                    }
                }
                default -> {
                    if (surface() == Surface.EXPORT) {
                        reaches.add(Emitter.SVG);
                        reaches.add(Emitter.PDF);
                        reaches.add(Emitter.PNG);
                    } else if (surface() == Surface.APPLICATION) {
                        reaches.add(Emitter.SCREEN);
                        // Text the chart draws is replayed into every
                        // writer from one recording (Sprint 29), so a
                        // name on the page is also a name on paper.
                        if (path.contains("/render/")
                                || path.contains("/chart/")
                                || path.contains("/page/")
                                || path.contains("/project/")) {
                            reaches.add(Emitter.SVG);
                            reaches.add(Emitter.PDF);
                            reaches.add(Emitter.PNG);
                        }
                    }
                }
            }
            return reaches;
        }

        /** The declaration site: where the wording is defined. */
        public String owner() {
            return path;
        }

        /**
         * A stable name for this occurrence.
         *
         * <p>Built from the file, the literal and the statement
         * around it - never the line number, which moves whenever
         * anything above it is edited and would invalidate a whole
         * ledger for an unrelated change. Because the statement is
         * part of it, editing the code around an unresolved literal
         * DOES change its identity, which is the point: a reviewed
         * disposition should not survive the code it was made about.
         */
        public String identity() {
            return Sha256.hex((path + "\u0000" + text + "\u0000"
                    + context).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .substring(0, 16);
        }
    }

    /**
     * Where a string can reach a reader.
     *
     * <p>Distinct from the <em>owner</em>, which is the declaration
     * site. One chart title is owned once and emitted many times:
     * onto the screen, into the accessible description, and into
     * all three exported formats. Recording only one of those would
     * make a translation look cheaper than it is - and would hide
     * that changing it changes files a reader has already saved.
     *
     * <p>Export writers are emitters and never owners. They author
     * no language; they carry what another surface wrote.
     */
    public enum Emitter {
        SCREEN, ACCESSIBILITY, SVG, PDF, PNG, FILENAME, LOG
    }

    /** How firmly a classification is known. */
    public enum Confidence {
        /** A named call publishes it. */
        TRACED,
        /** Recognised as prose; a candidate, not a proven string. */
        RECOGNISED
    }

    /** Which surface a path's strings belong to. */
    static Surface surfaceOf(String path, Kind kind) {
        String unix = path.replace('\\', '/');
        boolean tool = unix.contains("/tool/")
                || unix.contains("PackagedAcceptanceMain");
        if (kind == Kind.DIAGNOSTIC) {
            if (tool) {
                return Surface.CONFINED_DIAGNOSTIC;
            }
            for (String quoted : QUOTED_ON_FAILURE) {
                if (unix.contains(quoted)) {
                    return Surface.READER_REACHABLE_DIAGNOSTIC;
                }
            }
            return Surface.CONFINED_DIAGNOSTIC;
        }
        if (tool) {
            return Surface.DEVELOPER_TOOLS;
        }
        if (unix.contains("/sheet/")) {
            return Surface.EXPORT;
        }
        return Surface.APPLICATION;
    }

    /** Why the rules could not place a literal. */
    public static final class Unresolved {

        /**
         * The statement makes no call this scan recognises.
         *
         * <p>Kept only for a literal no narrower reason fits. This
         * phrase describes an ABSENCE OF EVIDENCE, not an accepted
         * limitation, so a large count here is a statement about the
         * rules rather than about the application - which is why the
         * reasons below exist to drain it.
         */
        public static final String NO_ROUTE =
                "unresolved: no route found, needs manual inspection";

        /** SVG, PDF or XML scaffolding, not words. */
        public static final String STRUCTURAL =
                "structural syntax or serialization fragment";

        /** Notation the sky uses, which no language changes. */
        public static final String NOTATION_FRAGMENT =
                "astronomical notation";

        /** A resource path or a preference key. */
        public static final String RESOURCE_KEY =
                "resource path or key";

        /** An enum name, command word or internal identity. */
        public static final String INTERNAL_IDENTITY =
                "internal enum or command identity";

        /** Printed by a generator or the acceptance harness. */
        public static final String TOOL_OUTPUT =
                "developer-tool output";

        /** Thrown in production, never displayed to a reader. */
        public static final String CONFINED_DIAGNOSTIC =
                "production diagnostic with no reader display route";

        /** Declared text read out of a collection, alias or index. */
        public static final String ALIAS_FLOW =
                "collection/alias flow not traced";

        /** Declared text whose name never reaches a known sink. */
        public static final String NO_SINK =
                "declared name used, but never at a known sink";

        /** Declared text with no use anywhere in its own file. */
        public static final String NO_USE =
                "declared name has no use in this file";

        private Unresolved() {
        }
    }

    /** One classified locale use. */
    public record LocaleUse(String path, int line, String call,
                            Locale locale) {
    }

    /** What one source file holds. */
    public record File(String path, List<Literal> literals,
                       List<LocaleUse> locales) {

        /** How many literals of this kind the file holds. */
        public long count(Kind kind) {
            return literals.stream().filter(l -> l.kind() == kind).count();
        }
    }

    /**
     * Calls that hand a string to a reader, and the kind it becomes.
     *
     * <p>Ordered: the first marker a line contains decides, so the
     * accessibility sinks are listed before the general text sinks
     * for a line that carries both.
     */
    private static final Map<String, Kind> SINKS = new LinkedHashMap<>();

    static {
        // Accessibility: spoken, never seen, and still the reader's.
        SINKS.put("setAccessibleName", Kind.ACCESSIBILITY);
        SINKS.put("setAccessibleDescription", Kind.ACCESSIBILITY);
        SINKS.put("AccessibleContext", Kind.ACCESSIBILITY);
        SINKS.put("accessibleDescription", Kind.ACCESSIBILITY);
        // Plain setters - the obvious route, and the smallest one.
        SINKS.put("setText", Kind.VISIBLE_PROSE);
        SINKS.put("setTitle", Kind.VISIBLE_PROSE);
        SINKS.put("setToolTipText", Kind.VISIBLE_PROSE);
        SINKS.put("setLabel", Kind.VISIBLE_PROSE);
        SINKS.put("setDialogTitle", Kind.VISIBLE_PROSE);
        SINKS.put("setApproveButtonText", Kind.VISIBLE_PROSE);
        // Constructors, which carry most of this application's words.
        SINKS.put("new JLabel", Kind.VISIBLE_PROSE);
        SINKS.put("new JButton", Kind.VISIBLE_PROSE);
        SINKS.put("new JMenu", Kind.VISIBLE_PROSE);
        SINKS.put("new JMenuItem", Kind.VISIBLE_PROSE);
        SINKS.put("new JCheckBoxMenuItem", Kind.VISIBLE_PROSE);
        SINKS.put("new JRadioButtonMenuItem", Kind.VISIBLE_PROSE);
        SINKS.put("new JCheckBox", Kind.VISIBLE_PROSE);
        SINKS.put("new JRadioButton", Kind.VISIBLE_PROSE);
        SINKS.put("new JToggleButton", Kind.VISIBLE_PROSE);
        SINKS.put("new JDialog", Kind.VISIBLE_PROSE);
        SINKS.put("new JFrame", Kind.VISIBLE_PROSE);
        SINKS.put("new TitledBorder", Kind.VISIBLE_PROSE);
        SINKS.put("createTitledBorder", Kind.VISIBLE_PROSE);
        // Dialogs and table models: words that never meet a setter.
        SINKS.put("showMessageDialog", Kind.VISIBLE_PROSE);
        SINKS.put("showConfirmDialog", Kind.VISIBLE_PROSE);
        SINKS.put("showOptionDialog", Kind.VISIBLE_PROSE);
        SINKS.put("showInputDialog", Kind.VISIBLE_PROSE);
        SINKS.put("getColumnName", Kind.VISIBLE_PROSE);
        SINKS.put("columnName", Kind.VISIBLE_PROSE);
        // Menu and control factories this repository writes itself.
        SINKS.put("menuItem(", Kind.VISIBLE_PROSE);
        SINKS.put("checkItem(", Kind.VISIBLE_PROSE);
        SINKS.put("toggle(", Kind.VISIBLE_PROSE);
        SINKS.put("explain(", Kind.VISIBLE_PROSE);
        SINKS.put("Explain.", Kind.VISIBLE_PROSE);
        // An Action carries its own menu text; a dialog takes its
        // title through super(); and this codebase writes its own
        // small control factories. All three were publishing words
        // the audit could not see.
        SINKS.put("new AbstractAction", Kind.VISIBLE_PROSE);
        SINKS.put("putValue(Action.NAME", Kind.VISIBLE_PROSE);
        SINKS.put("super(owner", Kind.VISIBLE_PROSE);
        SINKS.put("checkBox(", Kind.VISIBLE_PROSE);
        SINKS.put("addTab(", Kind.VISIBLE_PROSE);
        SINKS.put("iconButton(", Kind.VISIBLE_PROSE);
        SINKS.put("Shortcuts.saying(", Kind.VISIBLE_PROSE);
    }

    /** Astronomical notation, which no language changes. */
    private static final Pattern NOTATION = Pattern.compile(
            "^(?:[hms°'\"]|[Ͱ-Ͽ]+|·|V|B|RA|Dec|ICRS|J2000"
                    + "|TYC|NGC|IC|HIP|M|deg|mag|\\s*[hms]\\s*)$");

    /** Identifier shapes: dotted keys, ids, enum-like constants. */
    private static final Pattern IDENTIFIER = Pattern.compile(
            "^(?:[a-z][a-zA-Z0-9]*(?:\\.[a-z][a-zA-Z0-9]*)+"
                    + "|[A-Z][A-Z0-9_]{2,}|[A-Z][a-z]{2}|juranometria.*)$");

    /** Format patterns, separators, and punctuation-only strings. */
    private static final Pattern FORMAT = Pattern.compile(
            "^(?:[^\\p{L}]*|.*%[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z].*"
                    + "|\\\\[nrt]|<[^>]+>)$");

    /** File names, extensions, paths and command names. */
    private static final Pattern FILENAME = Pattern.compile(
            "^(?:[\\w./-]*\\.(?:png|svg|pdf|txt|md|json|jar|zip|csv|dat"
                    + "|properties|html)|[\\w-]+/[\\w./-]+|--?[a-z-]+)$");

    /**
     * Statements whose literal is developer-facing.
     *
     * <p>{@code require(} is this repository's own acceptance
     * assertion, and it carries more prose than any reader surface:
     * {@code PackagedAcceptanceMain} alone holds 285 sentences that
     * only ever reach a build log. Counting those as reader text
     * would have made a localisation look three times larger than
     * it is.
     */
    private static final Pattern DIAGNOSTIC_LINE = Pattern.compile(
            "throw new |Exception\\(|System\\.err|printStackTrace"
                    + "|assert|\\.warning\\(|\\.severe\\(|\\.log\\("
                    + "|require\\(|System\\.out");

    private static final Pattern LITERAL =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * A type or member declaration, for identity.
     *
     * <p>Two methods in one file can hold the identical statement -
     * the same {@code new JLabel("Close")} in two dialogs - and an
     * identity built from file, literal and statement alone would
     * merge them into one entry, so one review would silently stand
     * for two occurrences. The enclosing member separates them.
     */
    private static final Pattern MEMBER = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|static|final|abstract"
                    + "|sealed|default)\\s+)*(?:class|interface|enum|record"
                    + "|[\\w.<>\\[\\],? ]+\\s+\\w+\\s*\\()[^;]*");

    /** The enclosing type, so a member signature is qualified. */
    private static final Pattern TYPE = Pattern.compile(
            "(?:class|interface|enum|record)\\s+(\\w+)");

    private static final Pattern LOCALE_CALL =
            Pattern.compile("Locale\\.(ROOT|getDefault|US|ENGLISH|of)");

    /**
     * Scans the production sources under this tree.
     *
     * <p>Refuses a colliding identity rather than merging the two
     * occurrences behind it. A merged identity would let one
     * reviewed disposition stand silently for a string nobody
     * looked at, which is the failure the ledger exists to prevent.
     */
    public static List<File> scan(Path tree) throws IOException {
        List<File> files = new ArrayList<>();
        Path src = tree.resolve("src/juranometria");
        try (Stream<Path> walk = Files.walk(src)) {
            List<Path> sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    // The tool package is scanned and PARTITIONED, not
                    // excluded. Dropping it silently would leave the
                    // report unable to say how large the developer
                    // surface is, and a later reader of the audit
                    // could not tell an excluded file from an absent
                    // one.
                    .sorted()
                    .toList();
            for (Path source : sources) {
                files.add(classify(tree, source));
            }
        }
        Map<String, Literal> seen = new LinkedHashMap<>();
        for (File file : files) {
            for (Literal literal : file.literals()) {
                Literal already = seen.put(literal.identity(), literal);
                if (already != null) {
                    throw new IllegalStateException(
                            "two occurrences share one identity, so a"
                                    + " review of either would stand for"
                                    + " both: " + already.path() + ":"
                                    + already.line() + " and "
                                    + literal.path() + ":"
                                    + literal.line() + " \""
                                    + literal.text() + "\"");
                }
            }
        }
        return files;
    }

    /**
     * Classifies source given directly, rather than read from the
     * tree.
     *
     * <p>Exists so the calibration fixture can be
     * <em>synthetic input to the scanner</em> and never a file under
     * {@code src/}. A fixture living in the production tree would be
     * walked by {@link #scan}, and the audit would begin counting
     * its own measuring equipment - inflating the reader-visible
     * total with strings no reader can ever see.
     */
    public static File classify(String path, List<String> lines) {
        return classify(path, lines, true);
    }

    private static File classify(Path tree, Path source) throws IOException {
        return classify(tree.relativize(source).toString(),
                Files.readAllLines(source), true);
    }

    private static File classify(String path, List<String> lines,
                                 boolean followConstants) {
        List<Literal> literals = new ArrayList<>();
        List<LocaleUse> locales = new ArrayList<>();
        boolean inBlockComment = false;
        StringBuilder statement = new StringBuilder();
        String type = "";
        String member = "";
        List<int[]> held = new ArrayList<>();
        List<String> heldText = new ArrayList<>();
        int open = 0;
        for (int at = 0; at < lines.size(); at++) {
            String line = lines.get(at);
            String trimmed = line.strip();
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }
            if (trimmed.startsWith("/*")) {
                inBlockComment = !trimmed.contains("*/");
                continue;
            }
            if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                continue;
            }
            Matcher names = TYPE.matcher(line);
            if (names.find()) {
                type = names.group(1);
                member = "";
            }
            Matcher declares = MEMBER.matcher(line);
            if (declares.find()) {
                member = declares.group().strip();
            }
            Matcher locale = LOCALE_CALL.matcher(line);
            while (locale.find()) {
                locales.add(new LocaleUse(path, at + 1, locale.group(),
                        localeUse(line, locale.group(1))));
            }
            // The statement this line belongs to, not the line. A
            // call that reaches the reader is routinely written over
            // four lines - the sink on the first, the words on the
            // third - and judging each physical line alone lost the
            // route for every one of them. The statement is grown
            // until its parentheses balance, so the words are read
            // together with the call that publishes them.
            if (open == 0) {
                statement.setLength(0);
            }
            statement.append(' ').append(trimmed);
            open += depthOf(trimmed);
            if (open > 0) {
                // Mid-statement: its literals are classified when the
                // whole statement is in hand, on the closing line.
                Matcher pending = LITERAL.matcher(line);
                while (pending.find()) {
                    held.add(new int[] {at + 1, pending.start()});
                    heldText.add(pending.group(1));
                }
                continue;
            }
            Matcher found = LITERAL.matcher(line);
            List<int[]> places = new ArrayList<>(held);
            List<String> texts = new ArrayList<>(heldText);
            while (found.find()) {
                places.add(new int[] {at + 1, found.start()});
                texts.add(found.group(1));
            }
            held.clear();
            heldText.clear();
            String whole = statement.toString();
            String route = routeOf(whole);
            for (int which = 0; which < texts.size(); which++) {
                String text = texts.get(which);
                if (text.isEmpty()) {
                    continue;
                }
                // The ordinal within the statement. One statement can
                // hold the same literal twice - a separator written
                // between three parts - and those are two occurrences
                // a reviewer may decide differently about, so they
                // must not share an identity.
                literals.add(new Literal(path, places.get(which)[0], text,
                        kindOf(text, whole, route), route, "",
                        type + "#" + member + " |" + which + "| "
                                + whole.strip().replaceAll("\\s+", " ")));
            }
        }
        return new File(path, List.copyOf(followConstants(lines,
                numbered(literals))), List.copyOf(locales));
    }

    /**
     * Words held in a named constant and published elsewhere.
     *
     * <p>{@code AboutDialog} keeps its notice titles in a
     * {@code static final String[][]} and its summary in a
     * {@code DESCRIPTION} constant, both declared far from the call
     * that shows them; {@code InspectorPanel} keeps
     * {@code headingText} as a field and hands it to
     * {@code setText} later. Judged where they are written, all of
     * those are anonymous strings.
     *
     * <p><strong>The boundary, stated rather than implied.</strong>
     * This is not data-flow analysis and cannot follow a word
     * through a method call, a collection, a return value or another
     * class. It follows one shape only: a {@code String} or
     * {@code String[]} declared with literal text in this file,
     * whose declared name later appears in this same file in a
     * statement that reaches a reader. Anything else stays
     * {@link Kind#UNCLASSIFIED}, which is why that count is reported
     * and not explained away.
     */
    private static List<Literal> followConstants(List<String> lines,
                                                 List<Literal> literals) {
        Pattern declaration = Pattern.compile(
                "(?:static\\s+)?(?:final\\s+)?String(?:\\s*\\[\\s*\\])*"
                        + "\\s+(\\w+)\\s*=");
        // Which name each literal was declared under, if any.
        Map<Integer, String> declaredAt = new LinkedHashMap<>();
        String current = null;
        int depth = 0;
        for (int at = 0; at < lines.size(); at++) {
            String line = lines.get(at);
            Matcher found = declaration.matcher(line);
            if (depth == 0 && found.find()) {
                current = found.group(1);
            }
            if (current != null) {
                declaredAt.put(at + 1, current);
                depth += braceDepth(line);
                if (depth <= 0 && line.strip().endsWith(";")) {
                    current = null;
                    depth = 0;
                }
            }
        }
        // Names this file later hands to a reader-visible call.
        Map<String, Kind> published = new LinkedHashMap<>();
        for (String name : Set.copyOf(declaredAt.values())) {
            for (String line : lines) {
                String route = routeOf(line);
                if (!route.isEmpty()
                        && Pattern.compile("\\b" + Pattern.quote(name) + "\\b")
                                .matcher(line).find()) {
                    published.put(name, SINKS.get(route));
                    break;
                }
            }
        }
        // How each declared name is used, for the literals this pass
        // cannot resolve. The reason is the whole point: "collection
        // flow not traced" sends a reviewer to a for-each, "no known
        // sink" sends them to a call list, and "no route" sends them
        // to the statement itself. One bucket would send them nowhere.
        Map<String, String> why = new LinkedHashMap<>();
        for (String name : Set.copyOf(declaredAt.values())) {
            if (published.containsKey(name)) {
                continue;
            }
            Pattern alias = Pattern.compile(
                    "for\\s*\\([^)]*:\\s*" + Pattern.quote(name) + "\\s*\\)"
                            + "|" + Pattern.quote(name) + "\\s*\\[");
            Pattern mention = Pattern.compile(
                    "\\b" + Pattern.quote(name) + "\\b");
            String reason = Unresolved.NO_USE;
            for (int at = 0; at < lines.size(); at++) {
                String line = lines.get(at);
                if (declaredAt.get(at + 1) != null
                        && declaredAt.get(at + 1).equals(name)) {
                    continue;
                }
                if (alias.matcher(line).find()) {
                    reason = Unresolved.ALIAS_FLOW;
                    break;
                }
                if (mention.matcher(line).find()) {
                    reason = Unresolved.NO_SINK;
                }
            }
            why.put(name, reason);
        }
        List<Literal> followed = new ArrayList<>();
        for (Literal literal : literals) {
            String name = declaredAt.get(literal.line());
            Kind reaches = name == null ? null : published.get(name);
            if (literal.kind() != Kind.UNCLASSIFIED) {
                followed.add(literal);
            } else if (reaches != null && hasLetters(literal.text())) {
                followed.add(new Literal(literal.path(), literal.line(),
                        literal.text(), reaches, name + " → reader", "",
                        literal.context()));
            } else {
                String reason = name == null
                        ? narrow(literal)
                        : why.getOrDefault(name, narrow(literal));
                // A named population is CLASSIFIED, not unresolved.
                // Leaving markup and notation in the unclassified
                // bucket with an explanatory string understated what
                // the rules actually knew, and made the residue look
                // four times larger than the part needing a person.
                Kind placed = switch (reason) {
                    case Unresolved.STRUCTURAL -> Kind.STRUCTURAL;
                    case Unresolved.NOTATION_FRAGMENT -> Kind.NOTATION;
                    case Unresolved.RESOURCE_KEY -> Kind.FILENAME;
                    case Unresolved.INTERNAL_IDENTITY -> Kind.IDENTIFIER;
                    default -> Kind.UNCLASSIFIED;
                };
                String route = literal.route();
                if (placed == Kind.UNCLASSIFIED
                        && Unresolved.NO_ROUTE.equals(reason)) {
                    // Recognised, though not traced. A sentence in a
                    // reader-facing class is the reader's even when
                    // it is concatenated into a message or carried by
                    // an enum constant, and marking it UNKNOWN would
                    // leave real words out of the translation queue -
                    // the dangerous direction of the two. The ROUTE
                    // says how weakly it is known, so a reviewer can
                    // tell a traced call from a recognised sentence.
                    if (readerFacing(literal.path())
                            && looksLikeProse(literal.text())) {
                        placed = Kind.VISIBLE_PROSE;
                        route = "recognised as prose; no sink traced";
                    } else if (oneToken(literal.text())) {
                        placed = Kind.IDENTIFIER;
                        route = "single token in a reader-facing class";
                    }
                }
                followed.add(new Literal(literal.path(), literal.line(),
                        literal.text(), placed, route,
                        placed == Kind.UNCLASSIFIED ? reason : "",
                        literal.context()));
            }
        }
        return followed;
    }

    /**
     * The narrowest reason that fits an unplaced literal.
     *
     * <p>"No route found" is where a literal lands when nothing else
     * explains it, and a pile of them is a finding about this
     * classifier rather than an accepted limitation. Each rule below
     * removes a population whose nature is known - SVG scaffolding,
     * a resource path, a generator's own output - leaving a residue
     * small enough for #350 to read one by one.
     */
    private static String narrow(Literal literal) {
        String text = literal.text();
        // Judged on the text without its escapes or indentation. An
        // SVG element written as "  <defs>\n" is scaffolding
        // whatever whitespace surrounds it, and a PDF operator is a
        // page-description instruction, not a word.
        String plain = bare(text).strip();
        Surface surface = surfaceOf(literal.path(), Kind.VISIBLE_PROSE);
        if (surface == Surface.DEVELOPER_TOOLS) {
            return Unresolved.TOOL_OUTPUT;
        }
        if (STRUCTURAL.matcher(plain).find()
                || plain.indexOf('<') >= 0 || plain.indexOf('>') >= 0
                || PDF_OPERATOR.matcher(plain).matches()) {
            return Unresolved.STRUCTURAL;
        }
        if (GREEK_NAME.matcher(plain).matches()) {
            return Unresolved.NOTATION_FRAGMENT;
        }
        if (RESOURCE.matcher(text).matches()) {
            return Unresolved.RESOURCE_KEY;
        }
        if (NOTATION_ISH.matcher(text).matches()) {
            return Unresolved.NOTATION_FRAGMENT;
        }
        if (INTERNAL.matcher(text).matches()
                || STYLE_TOKEN.matcher(plain).matches()) {
            return Unresolved.INTERNAL_IDENTITY;
        }
        if (!hasLetters(text)) {
            return Unresolved.STRUCTURAL;
        }
        return Unresolved.NO_ROUTE;
    }

    /** SVG/PDF/XML scaffolding and markup fragments. */
    private static final Pattern STRUCTURAL = Pattern.compile(
            "^</?[a-zA-Z][\\w:-]*|/>|^\\d+ \\d+ (?:obj|R)$|^stream$"
                    + "|^endobj$|^%PDF|xmlns|^<<|>>$|viewBox|^[a-z-]+=\"$"
                    + "|^(?:M|L|C|Z|A) ?$|font-|stroke-|text-anchor");

    /**
     * PDF page-description operators, named rather than guessed.
     *
     * <p>A one- or two-letter token in a PDF writer is an
     * instruction to the page, not a word: {@code rg} sets a colour,
     * {@code f} fills, {@code S} strokes. Listing them by name keeps
     * this from swallowing a genuine two-letter label.
     */
    private static final Pattern PDF_OPERATOR = Pattern.compile(
            "^(?:endstream|endobj|stream|xref|trailer|startxref"
                    + "|BT|ET|Tf|Td|TJ|Tj|Tw|re|rg|RG|cm|gs|Do|scn"
                    + "|W|n|f|S|s|Q|q|h|m|l|c|v|y|B|b)$");

    /**
     * The Greek letters, spelled out.
     *
     * <p>Search accepts "alpha Orionis" as well as the symbol, so
     * these are the grammar's vocabulary rather than words on a
     * page. Translating them would break the search a reader types.
     */
    private static final Pattern GREEK_NAME = Pattern.compile(
            "^(?:alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota"
                    + "|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma"
                    + "|tau|upsilon|phi|chi|psi|omega)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Look-and-feel client properties and style tokens.
     *
     * <p>{@code FlatLaf.styleClass}, {@code JButton.buttonType} and
     * the {@code h2} / {@code close} values they take are addressed
     * to the toolkit, not to a reader. They look like words and are
     * not.
     */
    private static final Pattern STYLE_TOKEN = Pattern.compile(
            "^(?:(?:FlatLaf|JButton|JComponent|Component|TitlePane"
                    + "|Table|List|Tree|ScrollPane|TextField)\\.[\\w.]+"
                    + "|h[1-6]|close|small|large|mini|monospaced"
                    + "|roundRect|square|borderless|toolBarButton)$");

    /** Resource paths, preference keys, MIME types. */
    private static final Pattern RESOURCE = Pattern.compile(
            "^(?:/[\\w./-]+|[\\w-]+/[\\w-]+|[a-z][\\w]*\\.[\\w.]+"
                    + "|[A-Za-z]+\\.(?:png|svg|pdf|txt|md|json))$");

    /** Bare notation, units and single-symbol text. */
    private static final Pattern NOTATION_ISH = Pattern.compile(
            "^(?:[^\\p{L}]{0,4}|[hms°'\"·×±]+|[A-Z]{1,4}|[Ͱ-Ͽ]+"
                    + "|\\s*(?:deg|mag|px|arcmin|arcsec)\\s*)$");

    /** Enum-ish and command-ish words with no spaces. */
    private static final Pattern INTERNAL = Pattern.compile(
            "^(?:[A-Z][A-Z0-9_]+|[a-z]+(?:[A-Z][a-z0-9]*)+|--?[\\w-]+"
                    + "|[a-z]+(?:-[a-z]+)+)$");

    /** Whether this file publishes to a reader at all. */
    private static boolean readerFacing(String path) {
        Surface where = surfaceOf(path, Kind.VISIBLE_PROSE);
        return where == Surface.APPLICATION || where == Surface.EXPORT;
    }

    /**
     * Whether a literal reads as a sentence or phrase.
     *
     * <p>Two or more words, at least one of them long enough not to
     * be an abbreviation, and no shape that identifies it as a key,
     * a path or a token. Fragments count: reader messages in this
     * codebase are routinely built from pieces like
     * {@code " already exists in "}.
     */
    private static boolean looksLikeProse(String text) {
        String plain = bare(text).strip();
        if (plain.length() < 4 || IDENTIFIER.matcher(plain).matches()
                || STYLE_TOKEN.matcher(plain).matches()
                || RESOURCE.matcher(plain).matches()) {
            return false;
        }
        long words = Pattern.compile("\\p{L}{2,}").matcher(plain)
                .results().count();
        return words >= 2 || (words == 1 && plain.contains(" ")
                && Pattern.compile("\\p{L}{4,}").matcher(plain).find());
    }

    /** A single bare word or dotted key, with no sentence around it. */
    private static boolean oneToken(String text) {
        String plain = bare(text).strip();
        return !plain.isEmpty() && !plain.contains(" ")
                && plain.matches("[\\w.$-]+");
    }

    /**
     * Distinguishes occurrences nothing else can tell apart.
     *
     * <p>A file can hold the same literal, in the same member, in
     * two textually identical statements - the same
     * {@code "ICRS J2000"} written twice in one panel. Those are two
     * occurrences a reviewer may decide differently about, so they
     * cannot share an identity; but numbering them by LINE would
     * churn every identity below any edit, which is what the
     * statement-based identity was chosen to avoid. They are
     * numbered instead by how many identical occurrences precede
     * them in the same file, which only changes when one of those
     * occurrences is itself added or removed.
     */
    private static List<Literal> numbered(List<Literal> literals) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<Literal> numbered = new ArrayList<>();
        for (Literal literal : literals) {
            String key = literal.context() + "\u0000" + literal.text();
            int nth = seen.merge(key, 1, Integer::sum) - 1;
            numbered.add(nth == 0 ? literal
                    : new Literal(literal.path(), literal.line(),
                            literal.text(), literal.kind(), literal.route(),
                            literal.reason(),
                            literal.context() + " ~" + nth));
        }
        return numbered;
    }

    /** Brace depth a line adds, ignoring quoted text. */
    private static int braceDepth(String line) {
        int depth = 0;
        boolean inString = false;
        for (int at = 0; at < line.length(); at++) {
            char c = line.charAt(at);
            if (c == '\\') {
                at++;
            } else if (inString) {
                inString = c != '"';
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /** Parenthesis depth a line adds, ignoring quoted text. */
    private static int depthOf(String line) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int at = 0; at < line.length(); at++) {
            char c = line.charAt(at);
            if (c == '\\') {
                at++;
            } else if (inString) {
                inString = c != '"';
            } else if (inChar) {
                inChar = c != '\'';
            } else if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }
        return depth;
    }

    /**
     * The reader-visible call this statement makes, or the empty
     * string.
     *
     * <p>A constructor sink is matched through any qualification,
     * because this codebase writes both {@code new JToggleButton}
     * and {@code new javax.swing.JToggleButton} and the second is
     * the same reader surface as the first. Matching the literal
     * text alone missed every qualified one.
     */
    private static String routeOf(String statement) {
        for (String sink : SINKS.keySet()) {
            if (statement.contains(sink)) {
                return sink;
            }
            if (sink.startsWith("new ")) {
                String type = sink.substring(4);
                if (Pattern.compile("new\\s+(?:[\\w]+\\.)*"
                        + Pattern.quote(type) + "\\b")
                        .matcher(statement).find()) {
                    return sink;
                }
            }
        }
        return "";
    }

    private static Kind kindOf(String text, String line, String route) {
        // A reader-visible sink decides first: a word handed to a
        // label is the reader's word whatever it looks like.
        if (!route.isEmpty() && hasLetters(text)) {
            return SINKS.get(route);
        }
        if (DIAGNOSTIC_LINE.matcher(line).find() && hasLetters(text)) {
            return Kind.DIAGNOSTIC;
        }
        if (NOTATION.matcher(text).matches()) {
            return Kind.NOTATION;
        }
        if (FILENAME.matcher(text).matches()) {
            return Kind.FILENAME;
        }
        if (IDENTIFIER.matcher(text).matches()) {
            return Kind.IDENTIFIER;
        }
        // A template is only a pattern if the holes are all it is.
        // "Centre RA %.4f, Dec %+.4f (ICRS/J2000). Field %.0f
        // degrees wide" carries seven words a reader reads, and
        // matching on the presence of a specifier classified the
        // whole sentence as punctuation - which is how the exported
        // sheet's own description came to be reported as owning no
        // reader text at all.
        if (FORMAT.matcher(text).matches() && !prosePastTheHoles(text)) {
            return Kind.FORMAT;
        }
        // Prose the rules cannot attribute to a reader-visible route.
        // Recorded as unclassified rather than guessed: it may be a
        // word a reader reads through a route this scan does not
        // know, which is the finding an audit exists to surface.
        return Kind.UNCLASSIFIED;
    }

    private static Locale localeUse(String line, String which) {
        // The process default follows the machine, which is neither
        // deterministic nor the reader's stated choice.
        if (which.equals("getDefault")) {
            return Locale.AMBIENT;
        }
        // A locale read from the reader's own language choice is the
        // one that SHOULD follow them. No such door exists yet - the
        // preference is #348's to build - so this branch is expected
        // to find nothing today, and that zero is the baseline the
        // sprint changes rather than an assertion that it cannot.
        if (line.contains("chartLanguage") || line.contains("interfaceLanguage")
                || line.contains("readerLocale") || line.contains("Language.")) {
            return Locale.READER_FACING;
        }
        // ROOT, US and ENGLISH are pinned by intent: a number, a case
        // fold, a file name or an evidence record must not move with
        // the machine's language, or evidence stops reproducing and
        // file names become machine-dependent.
        return Locale.DETERMINISTIC;
    }

    /** Whether words survive once format specifiers are removed. */
    private static boolean prosePastTheHoles(String text) {
        String bare = text.replaceAll("%[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]",
                " ").replaceAll("%[ns%]", " ");
        long words = Pattern.compile("\\p{L}{3,}").matcher(bare)
                .results().count();
        return words >= 2;
    }

    /**
     * Whether any letter survives once escapes are removed.
     *
     * <p>A Java literal's backslash-n reaches this scan as the two
     * characters {@code \\} and {@code n}, and {@code n} is a
     * letter - so a row of equals signs ending in a newline looked
     * like prose, and a separator was offered for translation.
     */
    private static boolean hasLetters(String text) {
        return bare(text).codePoints().anyMatch(Character::isLetter);
    }

    /** The literal with its escape sequences removed. */
    private static String bare(String text) {
        return text.replaceAll("\\\\[nrtbf0\"'\\\\]", " ")
                .replaceAll("\\\\u[0-9a-fA-F]{4}", " ");
    }
}
