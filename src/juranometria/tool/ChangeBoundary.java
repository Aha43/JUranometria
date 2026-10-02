package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import juranometria.tool.ChangeRoute.Finding;
import juranometria.tool.ChangeRoute.Route;

/**
 * The boundary the classifier judges a change against, read from the
 * compiled classes, the registered generators and the committed tree
 * (issue #428, as ruled on #427).
 *
 * <p>Two closures over one class graph. The <strong>chart</strong>
 * closure is everything the chart producers reach - every generator
 * that owns a committed chart picture ({@link EvidenceGenerators}),
 * the smoke render the image workflow compares, and the two resource
 * index writers. The <strong>interaction</strong> closure is
 * everything the interface reaches: the photographers by their
 * registry, the generators reproduced on every interaction run, and
 * the packaged application's two entry points. A source in both is
 * chart's. Language keys are judged by which closure's compiled code
 * can name them; committed evidence by which generator owns the
 * directory it lies in.
 */
public final class ChangeBoundary implements ChangeRoute.Boundary {

    /** Chart roots that are not evidence generators. */
    static final List<String> CHART_ROOTS = List.of(
            "juranometria/app/ChartImageMain",
            "juranometria/tool/SkyLanguageIndexMain",
            "juranometria/tool/InterfaceLanguageIndexMain");

    /** The packaged application: what ships, beside its evidence. */
    static final List<String> APPLICATION_ROOTS = List.of(
            "juranometria/app/JUranometriaMain",
            "juranometria/app/PackagedAcceptanceMain");

    private final RenderingClosure classes;
    private final Map<String, String> chart;
    private final Map<String, String> interaction;
    private final Set<String> chartOutputs;
    private final Set<String> interactionOutputs;
    private final Path root;
    private final Path base;

    private ChangeBoundary(RenderingClosure classes,
                           Map<String, String> chart,
                           Map<String, String> interaction,
                           Set<String> chartOutputs,
                           Set<String> interactionOutputs,
                           Path root, Path base) {
        this.classes = classes;
        this.chart = chart;
        this.interaction = interaction;
        this.chartOutputs = chartOutputs;
        this.interactionOutputs = interactionOutputs;
        this.root = root;
        this.base = base;
    }

    /**
     * @param classes the compiled head
     * @param root    the working tree, at the head
     * @param base    a directory holding the merge base's copy of
     *                every changed file that existed there, at its
     *                own relative path; null when there is none, so
     *                every key and row reads as new
     */
    public static ChangeBoundary of(RenderingClosure classes, Path root,
                                    Path base) throws IOException {
        EvidenceGenerators generators = EvidenceGenerators.of(classes,
                root.resolve("docs/studies"));
        List<String> chartRoots = new ArrayList<>(
                generators.of(EvidenceGenerators.Kind.CHART));
        chartRoots.addAll(CHART_ROOTS);
        List<String> interactionRoots = new ArrayList<>(
                generators.of(EvidenceGenerators.Kind.REPRODUCED));
        interactionRoots.addAll(
                generators.of(EvidenceGenerators.Kind.PHOTOGRAPHER));
        interactionRoots.addAll(APPLICATION_ROOTS);
        Set<String> interactionOutputs = new TreeSet<>(
                generators.outputsOf(EvidenceGenerators.Kind.REPRODUCED));
        interactionOutputs.addAll(
                generators.outputsOf(EvidenceGenerators.Kind.PHOTOGRAPHER));
        return new ChangeBoundary(classes, classes.reach(chartRoots),
                classes.reach(interactionRoots),
                generators.outputsOf(EvidenceGenerators.Kind.CHART),
                interactionOutputs, root, base);
    }

    @Override
    public Optional<String> chartChain(String source) {
        return chainIn(chart, source);
    }

    @Override
    public Optional<String> interactionChain(String source) {
        return chainIn(interaction, source);
    }

    private Optional<String> chainIn(Map<String, String> reached,
                                     String source) {
        for (String type : reached.keySet()) {
            if (source.equals(classes.sourceOf(type).orElse(null))) {
                List<String> chain = new ArrayList<>();
                for (String at = type; at != null; at = reached.get(at)) {
                    chain.add(0, at);
                }
                return Optional.of(String.join(" -> ", chain));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Route> ownerOf(String studiesPath) {
        if (chartOutputs.stream().anyMatch(studiesPath::startsWith)) {
            return Optional.of(Route.WIDE);
        }
        if (interactionOutputs.stream().anyMatch(studiesPath::startsWith)) {
            return Optional.of(Route.INTERACTION);
        }
        return Optional.empty();
    }

    @Override
    public Finding consumersOf(String key) {
        Optional<String> chartReader = readerIn(chart, key);
        if (chartReader.isPresent()) {
            return new Finding(key, Route.WIDE,
                    "chart code can resolve it (" + chartReader.get() + ")");
        }
        Optional<String> reader = readerIn(interaction, key);
        if (reader.isPresent()) {
            return new Finding(key, Route.INTERACTION,
                    "read by " + reader.get());
        }
        return new Finding(key, Route.WIDE,
                "no compiled code names it: unresolved");
    }

    @Override
    public Optional<Finding> readersOfFile(String path) {
        // The file's own path, or its own directory - a class that
        // names the directory may walk it and read everything there,
        // so it counts as a reader. A broader prefix ("docs/studies/")
        // names no file in particular and counts as none.
        String directory = path.contains("/")
                ? path.substring(0, path.lastIndexOf('/')) : "";
        java.util.function.BiPredicate<String, String> names =
                (constant, file) -> constant.equals(file)
                        || (!directory.isEmpty() && (constant.equals(directory)
                                || constant.equals(directory + "/")));
        Optional<String> chartReader = readerIn(chart, path, names);
        if (chartReader.isPresent()) {
            return Optional.of(new Finding(path, Route.WIDE,
                    "a committed input chart code reads ("
                            + chartReader.get() + ")"));
        }
        Optional<String> reader = readerIn(interaction, path, names);
        if (reader.isPresent()) {
            return Optional.of(new Finding(path, Route.INTERACTION,
                    "a committed input only interface code reads ("
                            + reader.get() + ")"));
        }
        // Named, but by code neither closure reaches: no generator and
        // no shipped program reads it, so only the suite holds it.
        Map<String, String> everything = new LinkedHashMap<>();
        for (String type : classes.classes()) {
            everything.put(type, null);
        }
        return readerIn(everything, path, names).map(type -> new Finding(path,
                Route.INTERACTION, "a committed input only the suite holds,"
                        + " through " + type + ", which no generator or"
                        + " shipped program reaches"));
    }

    /** The first class in a closure whose constants can name the key. */
    private Optional<String> readerIn(Map<String, String> reached, String key) {
        Set<String> plain = plainConstants(reached);
        return readerIn(reached, key,
                (constant, named) -> covers(constant, named, plain));
    }

    private final Map<Map<String, String>, Set<String>> plainCache =
            new java.util.IdentityHashMap<>();

    /** Every constant of a closure that is not a recipe. */
    private Set<String> plainConstants(Map<String, String> reached) {
        return plainCache.computeIfAbsent(reached, closure -> {
            Set<String> plain = new java.util.HashSet<>();
            for (String type : closure.keySet()) {
                for (String text : classes.stringsOf(type)) {
                    if (text.indexOf('\u0001') < 0 && !text.isEmpty()) {
                        plain.add(text);
                    }
                }
            }
            return plain;
        });
    }

    private Optional<String> readerIn(Map<String, String> reached, String key,
            java.util.function.BiPredicate<String, String> names) {
        for (String type : reached.keySet()) {
            for (String text : classes.stringsOf(type)) {
                if (names.test(text, key)) {
                    return Optional.of(type);
                }
            }
        }
        return Optional.empty();
    }

    private static final Pattern KEY_CHARACTERS =
            Pattern.compile("[A-Za-z0-9._-]+");

    /**
     * Whether a string constant can name a key, given every plain
     * constant of the same closure: the key itself; a prefix ending in
     * a dot that a caller appends to; a stem the key continues with a
     * dot; or a concatenation recipe whose literal pieces are made of
     * what keys are made of. A recipe that begins with a spliced value
     * - {@code "\u0001.label"} - names a key only when that value
     * could itself come from a plain constant of the closure, because
     * a value nothing in the closure can name, the closure cannot
     * splice. Over-approximates in the safe direction for the chart,
     * where a covered key is wide.
     */
    static boolean covers(String constant, String key, Set<String> plain) {
        if (constant.isEmpty()) {
            return false;
        }
        if (constant.indexOf('\u0001') < 0) {
            return plainCovers(constant, key);
        }
        String[] pieces = constant.split("\u0001", -1);
        boolean keyShaped = false;
        for (String piece : pieces) {
            if (piece.isEmpty()) {
                continue;
            }
            if (!KEY_CHARACTERS.matcher(piece).matches()) {
                return false;
            }
            keyShaped |= piece.indexOf('.') >= 0
                    && piece.chars().anyMatch(Character::isLetter);
        }
        if (!keyShaped) {
            return false;
        }
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0) {
                pattern.append("(.+)");
            }
            pattern.append(Pattern.quote(pieces[i]));
        }
        java.util.regex.Matcher match =
                Pattern.compile(pattern.toString()).matcher(key);
        if (!match.matches()) {
            return false;
        }
        if (!pieces[0].isEmpty()) {
            return true;
        }
        String spliced = match.group(1);
        for (String value : plain) {
            if (plainCovers(value, spliced)) {
                return true;
            }
        }
        return false;
    }

    /** The same, for a constant alone (no closure to splice from). */
    static boolean covers(String constant, String key) {
        return covers(constant, key, Set.of());
    }

    private static boolean plainCovers(String constant, String key) {
        if (constant.isEmpty()) {
            return false;
        }
        if (constant.equals(key)) {
            return true;
        }
        if (constant.endsWith(".") && key.startsWith(constant)) {
            return true;
        }
        return key.startsWith(constant + ".");
    }

    @Override
    public Optional<String> base(String path) {
        return base == null ? Optional.empty() : read(base.resolve(path));
    }

    @Override
    public Optional<String> head(String path) {
        return read(root.resolve(path));
    }

    private static Optional<String> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            return Optional.empty();
        }
    }

    /** The two closures' sizes, for the summary. */
    Map<String, Integer> sizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("chart", chart.size());
        sizes.put("interaction", interaction.size());
        return sizes;
    }
}
