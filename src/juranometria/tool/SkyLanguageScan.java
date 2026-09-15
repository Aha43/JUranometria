package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                          String route, String reason) {

        /** A literal the rules placed; no limitation to name. */
        Literal(String path, int line, String text, Kind kind, String route) {
            this(path, line, text, kind, route, "");
        }

        /** Which surface publishes this string. */
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

    private static final Pattern LOCALE_CALL =
            Pattern.compile("Locale\\.(ROOT|getDefault|US|ENGLISH|of)");

    /** Scans the production sources under this tree. */
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
                literals.add(new Literal(path, places.get(which)[0], text,
                        kindOf(text, whole, route), route));
            }
        }
        return new File(path, List.copyOf(followConstants(lines, literals)),
                List.copyOf(locales));
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
                        literal.text(), reaches, name + " → reader"));
            } else {
                String reason = name == null
                        ? narrow(literal)
                        : why.getOrDefault(name, narrow(literal));
                followed.add(new Literal(literal.path(), literal.line(),
                        literal.text(), Kind.UNCLASSIFIED, literal.route(),
                        reason));
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
        Surface surface = surfaceOf(literal.path(), Kind.VISIBLE_PROSE);
        if (surface == Surface.DEVELOPER_TOOLS) {
            return Unresolved.TOOL_OUTPUT;
        }
        if (STRUCTURAL.matcher(text).find()) {
            return Unresolved.STRUCTURAL;
        }
        if (RESOURCE.matcher(text).matches()) {
            return Unresolved.RESOURCE_KEY;
        }
        if (NOTATION_ISH.matcher(text).matches()) {
            return Unresolved.NOTATION_FRAGMENT;
        }
        if (INTERNAL.matcher(text).matches()) {
            return Unresolved.INTERNAL_IDENTITY;
        }
        return Unresolved.NO_ROUTE;
    }

    /** SVG/PDF/XML scaffolding and markup fragments. */
    private static final Pattern STRUCTURAL = Pattern.compile(
            "^</?[a-zA-Z][\\w:-]*|/>|^\\d+ \\d+ (?:obj|R)$|^stream$"
                    + "|^endobj$|^%PDF|xmlns|^<<|>>$|viewBox|^[a-z-]+=\"$"
                    + "|^(?:M|L|C|Z|A) ?$|font-|stroke-|text-anchor");

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

    private static boolean hasLetters(String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }
}
