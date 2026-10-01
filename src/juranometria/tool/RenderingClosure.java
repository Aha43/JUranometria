package juranometria.tool;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every source file that can alter what the chart renders or what
 * the evidence generators produce, found by following references
 * from the classes that draw the committed and CI-compared pixels.
 *
 * <p>The rendering-neutral gate (issue #398) lets a change skip the
 * expensive rendering regime only when a mechanical boundary proves
 * the change cannot reach a renderer, a chart contribution or a
 * generator of committed evidence. This is that boundary. It is
 * computed, never listed: a hand-kept list of renderer paths is
 * exactly the kind of record that stays true until the day it does
 * not, and nothing would say so.
 *
 * <p>The roots are the programs whose output is held to committed
 * bytes or compared on CI: the evidence contract and its registries
 * (which name every study generator), the provenance recorder, the
 * smoke render the image workflow compares, and every interface
 * photographer. From those, every {@code juranometria} class the
 * compiled code refers to is followed, transitively, and each class
 * reached is mapped to the source file that compiled it. A change to
 * any of those files is a change something rendering-shaped can see.
 *
 * <p>It reads compiled classes rather than source, for the reason
 * {@code RemovableModelBoundaryTest} gives: a source scan can be
 * fooled by a name in a comment and can miss a type reached without
 * an import, while the constant pool records what the code actually
 * refers to. Every UTF-8 entry is scanned, so a class named only in
 * a method descriptor, or by its dotted name in a string handed to
 * {@code Class.forName} - which is how the evidence registries name
 * their generators - is followed too. That over-approximates in the
 * safe direction: a class mentioned in a string it never loads is
 * still treated as reachable.
 */
public final class RenderingClosure {

    /**
     * The roots, as the class file spells them. Named here, not
     * discovered: these are the programs whose output CI compares,
     * and a new one is a deliberate act that belongs in this list and
     * in the decision record.
     */
    static final List<String> ROOTS = List.of(
            "juranometria/tool/EvidenceContractMain",
            "juranometria/tool/EvidenceProvenanceMain",
            "juranometria/app/ChartImageMain",
            // Run by `make classes`: they write the language indexes
            // into the build that every renderer and generator then
            // reads. Nothing refers to them by class - the Makefile
            // names them - so they are roots by name.
            "juranometria/tool/SkyLanguageIndexMain",
            "juranometria/tool/InterfaceLanguageIndexMain");

    /**
     * Interface photographers are roots by name: every generator the
     * interface evidence gate registers is a {@code *SheetMain} in
     * the tool package, and the gate's own test holds that spelling.
     */
    static final Pattern PHOTOGRAPHER =
            Pattern.compile("juranometria/tool/[A-Za-z0-9]+SheetMain");

    private static final Pattern SLASHED =
            Pattern.compile("juranometria/[A-Za-z0-9_$/]+");

    private static final Pattern DOTTED =
            Pattern.compile("juranometria\\.[A-Za-z0-9_$.]+");

    private final Map<String, String> sourceOf;
    private final Map<String, Set<String>> refers;
    private final Set<String> roots;
    private final Map<String, String> reachedFrom;
    private final Set<String> sources;

    private RenderingClosure(Map<String, String> sourceOf,
                             Map<String, Set<String>> refers,
                             Set<String> roots,
                             Map<String, String> reachedFrom) {
        this.sourceOf = sourceOf;
        this.refers = refers;
        this.roots = roots;
        this.reachedFrom = reachedFrom;
        Set<String> reached = new TreeSet<>();
        for (String type : reachedFrom.keySet()) {
            String source = sourceOf.get(type);
            if (source != null) {
                reached.add(source);
            }
        }
        this.sources = Collections.unmodifiableSet(reached);
    }

    /** Reads every compiled class under {@code classes}. */
    public static RenderingClosure of(Path classes) throws IOException {
        Map<String, String> sourceOf = new TreeMap<>();
        Map<String, Set<String>> refers = new TreeMap<>();
        Map<String, Set<String>> strings = new TreeMap<>();
        try (Stream<Path> tree = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) tree
                    .filter(p -> p.toString().endsWith(".class"))
                    .sorted()::iterator) {
                ClassFile parsed = ClassFile.read(file);
                if (!parsed.name.startsWith("juranometria/")) {
                    continue;
                }
                sourceOf.put(parsed.name, parsed.sourcePath());
                refers.put(parsed.name, parsed.referenced);
                strings.put(parsed.name, parsed.strings);
            }
        }
        Set<String> roots = new TreeSet<>();
        for (String type : sourceOf.keySet()) {
            if (ROOTS.contains(type)
                    || PHOTOGRAPHER.matcher(type).matches()) {
                roots.add(type);
            }
        }
        for (String root : ROOTS) {
            if (!sourceOf.containsKey(root)) {
                throw new IllegalStateException("root " + root
                        + " is not among the compiled classes under "
                        + classes + "; the boundary cannot be computed"
                        + " from an incomplete build");
            }
        }
        Map<String, String> reachedFrom = new LinkedHashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String root : roots) {
            reachedFrom.put(root, null);
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            String type = queue.remove();
            for (String next : refers.getOrDefault(type, Set.of())) {
                if (sourceOf.containsKey(next)
                        && !reachedFrom.containsKey(next)) {
                    reachedFrom.put(next, type);
                    queue.add(next);
                }
            }
        }
        RenderingClosure closure =
                new RenderingClosure(sourceOf, refers, roots, reachedFrom);
        closure.strings = strings;
        return closure;
    }

    /** Every class's string constants, by class name (#428). */
    private Map<String, Set<String>> strings = Map.of();

    /** The string constants a compiled class holds (#428). */
    public Set<String> stringsOf(String type) {
        return Collections.unmodifiableSet(
                strings.getOrDefault(type, Set.of()));
    }

    /**
     * The classes reached from these roots, roots included, by the
     * same references the default closure follows (#428): the graph
     * is one, and which programs it is read from is the question.
     * A root that is not a compiled class is refused, because a
     * boundary computed from a root that is not there proves nothing.
     */
    public Map<String, String> reach(java.util.Collection<String> from) {
        Map<String, String> reached = new LinkedHashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String root : from) {
            if (!sourceOf.containsKey(root)) {
                throw new IllegalStateException("root " + root
                        + " is not among the compiled classes; the"
                        + " boundary cannot be computed from it");
            }
            if (reached.putIfAbsent(root, null) == null
                    && !queue.contains(root)) {
                queue.add(root);
            }
        }
        while (!queue.isEmpty()) {
            String type = queue.remove();
            for (String next : refers.getOrDefault(type, Set.of())) {
                if (sourceOf.containsKey(next) && !reached.containsKey(next)) {
                    reached.put(next, type);
                    queue.add(next);
                }
            }
        }
        return reached;
    }

    /** The roots found among the compiled classes. */
    public Set<String> roots() {
        return Collections.unmodifiableSet(roots);
    }

    /** Every compiled {@code juranometria} class, by name. */
    public Set<String> classes() {
        return Collections.unmodifiableSet(sourceOf.keySet());
    }

    /** Every class reached from a root, roots included. */
    public Set<String> reached() {
        return Collections.unmodifiableSet(reachedFrom.keySet());
    }

    /**
     * The source files, relative to the repository root, that
     * compiled into a reached class.
     */
    public Set<String> sources() {
        return sources;
    }

    /** The source file a class compiled from, if it is known. */
    public Optional<String> sourceOf(String type) {
        return Optional.ofNullable(sourceOf.get(type));
    }

    /**
     * Every {@code juranometria} type a compiled class mentions,
     * whether or not it is reached. What a chain is made of, so a
     * test can check that each link is a reference the class file
     * really carries.
     */
    public Set<String> refersTo(String type) {
        return Collections.unmodifiableSet(
                refers.getOrDefault(type, Set.of()));
    }

    /**
     * How a source file is reached: the chain of classes from a root
     * to one of the file's classes, root first. Empty when the file
     * is outside the closure.
     */
    public Optional<List<String>> chainTo(String source) {
        for (Map.Entry<String, String> entry : reachedFrom.entrySet()) {
            if (source.equals(sourceOf.get(entry.getKey()))) {
                List<String> chain = new ArrayList<>();
                for (String at = entry.getKey(); at != null;
                        at = reachedFrom.get(at)) {
                    chain.add(0, at);
                }
                return Optional.of(chain);
            }
        }
        return Optional.empty();
    }

    /**
     * What one class file says: its own name, the source file it was
     * compiled from, and every {@code juranometria} type its constant
     * pool mentions.
     */
    static final class ClassFile {
        final String name;
        final String sourceFile;
        final Set<String> referenced;
        /**
         * Every string constant the class holds - literals, and the
         * recipes the compiler writes for a concatenation, with
         * {@code \u0001} where each value is spliced in (issue #428:
         * what language keys and output paths a class can name).
         */
        final Set<String> strings;

        private ClassFile(String name, String sourceFile,
                          Set<String> referenced, Set<String> strings) {
            this.name = name;
            this.sourceFile = sourceFile;
            this.referenced = referenced;
            this.strings = strings;
        }

        /**
         * {@code src/<package>/<SourceFile>}: a nested or
         * package-private class compiles from the file its
         * {@code SourceFile} attribute names, not from a file of its
         * own name, so the attribute is the only honest answer.
         */
        String sourcePath() {
            int slash = name.lastIndexOf('/');
            String pkg = slash < 0 ? "" : name.substring(0, slash + 1);
            return "src/" + pkg + sourceFile;
        }

        static ClassFile read(Path file) throws IOException {
            try (InputStream in = Files.newInputStream(file);
                    DataInputStream data = new DataInputStream(in)) {
                if (data.readInt() != 0xCAFEBABE) {
                    throw new IOException(file + " is not a class file");
                }
                data.readUnsignedShort(); // minor
                data.readUnsignedShort(); // major
                int count = data.readUnsignedShort();
                String[] utf8 = new String[count];
                int[] classNameIndex = new int[count];
                List<Integer> stringIndex = new ArrayList<>();
                for (int i = 1; i < count; i++) {
                    int tag = data.readUnsignedByte();
                    switch (tag) {
                        case 1 -> utf8[i] = data.readUTF();
                        case 3, 4 -> data.readInt();
                        case 5, 6 -> {
                            data.readLong();
                            i++; // takes two slots
                        }
                        case 7 -> classNameIndex[i] = data.readUnsignedShort();
                        case 8 -> stringIndex.add(data.readUnsignedShort());
                        case 16, 19, 20 -> data.readUnsignedShort();
                        case 9, 10, 11, 12, 17, 18 -> data.readInt();
                        case 15 -> {
                            data.readUnsignedByte();
                            data.readUnsignedShort();
                        }
                        default -> throw new IOException(file
                                + ": unknown constant pool tag " + tag);
                    }
                }
                data.readUnsignedShort(); // access flags
                int thisClass = data.readUnsignedShort();
                String name = utf8[classNameIndex[thisClass]];
                data.readUnsignedShort(); // super
                int interfaces = data.readUnsignedShort();
                for (int i = 0; i < interfaces; i++) {
                    data.readUnsignedShort();
                }
                skipMembers(data); // fields
                skipMembers(data); // methods
                String sourceFile = null;
                int attributes = data.readUnsignedShort();
                for (int i = 0; i < attributes; i++) {
                    String attribute = utf8[data.readUnsignedShort()];
                    int length = data.readInt();
                    if ("SourceFile".equals(attribute)) {
                        sourceFile = utf8[data.readUnsignedShort()];
                    } else {
                        data.skipNBytes(length);
                    }
                }
                if (sourceFile == null) {
                    throw new IOException(file + " carries no SourceFile"
                            + " attribute; compile with debug"
                            + " information so the class can be"
                            + " traced to its source");
                }
                Set<String> referenced = new LinkedHashSet<>();
                for (String text : utf8) {
                    if (text == null) {
                        continue;
                    }
                    Matcher slashed = SLASHED.matcher(text);
                    while (slashed.find()) {
                        referenced.add(slashed.group());
                    }
                    Matcher dotted = DOTTED.matcher(text);
                    while (dotted.find()) {
                        String found = dotted.group();
                        // A dotted name ends where the sentence does.
                        while (found.endsWith(".")) {
                            found = found.substring(0, found.length() - 1);
                        }
                        referenced.add(found.replace('.', '/'));
                    }
                }
                referenced.remove(name);
                Set<String> strings = new LinkedHashSet<>();
                for (int index : stringIndex) {
                    if (utf8[index] != null) {
                        strings.add(utf8[index]);
                    }
                }
                return new ClassFile(name, sourceFile, referenced, strings);
            }
        }

        private static void skipMembers(DataInputStream data)
                throws IOException {
            int members = data.readUnsignedShort();
            for (int m = 0; m < members; m++) {
                data.readUnsignedShort(); // access
                data.readUnsignedShort(); // name
                data.readUnsignedShort(); // descriptor
                int attributes = data.readUnsignedShort();
                for (int a = 0; a < attributes; a++) {
                    data.readUnsignedShort(); // attribute name
                    data.skipNBytes(data.readInt());
                }
            }
        }
    }
}
