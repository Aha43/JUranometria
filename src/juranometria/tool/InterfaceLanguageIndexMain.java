package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The index a build makes of the interface languages it ships
 * (Sprint 33, issue #348).
 *
 * <p>Deliberately a second indexer rather than a mode of
 * {@link SkyLanguageIndexMain}. The two registries answer different
 * questions - what the controls speak, and what the sky is called -
 * and a reader chooses them separately. Sharing one indexer would
 * put the only thing keeping them apart inside a branch, where a
 * later edit could quietly merge them; separate paths and separate
 * schemas make cross-discovery structurally impossible instead of
 * merely unintended.
 *
 * <p>The differences are load-bearing, not cosmetic:
 *
 * <ul>
 *   <li>Chart packs live under {@code resources/sky-language},
 *       declare {@code schema=1}, and carry a names file. Interface
 *       descriptors live under {@code resources/interface-language},
 *       declare {@code interface-schema=1}, and carry no names.
 *   <li>The chart index has three columns; this one has two. Pointing
 *       either discovery at the other's index yields nothing usable
 *       rather than a plausible wrong answer.
 *   <li><strong>No chart pack is a legitimate build</strong> - the
 *       atlas draws the official Latin names with no pack installed.
 *       <strong>No interface descriptor is not.</strong> An
 *       application whose controls speak no language cannot start,
 *       and this says so here, where somebody built it, rather than
 *       leaving a reader to discover it.
 * </ul>
 *
 * <p>That last rule is the point of the whole file. English could
 * have been the answer discovery gave when it found nothing, and
 * everything would work today. The mechanism would never have run,
 * and the first person to add a second interface language would find
 * out only when it failed to appear.
 *
 * <p>Order is by language tag, always - a jar enumerates its entries
 * however it stored them, so an index built from that order would
 * list a selector's choices differently on two machines.
 */
public final class InterfaceLanguageIndexMain {

    private InterfaceLanguageIndexMain() {
    }

    /** Where interface descriptors live in the build output. */
    private static final String DESCRIPTORS = "resources/interface-language";

    /** What runtime reads to learn which interfaces exist. */
    static final String INDEX = "index.tsv";

    /** The marker that keeps a chart pack out of this index. */
    private static final String SCHEMA = "interface-schema";

    public static void main(String[] args) throws IOException {
        Path classes = Path.of(args.length > 0 ? args[0] : "build/classes");
        Path dir = classes.resolve(DESCRIPTORS);
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(DESCRIPTORS + " is not in"
                    + " the build output, so this build ships no"
                    + " interface language at all. Unlike a chart"
                    + " pack, an interface is not optional: the"
                    + " controls have to speak something. Ship at"
                    + " least en.manifest.");
        }

        Map<String, String> byTag = new TreeMap<>();
        Map<String, String> declaredIn = new LinkedHashMap<>();
        List<Path> manifests = new ArrayList<>();
        try (Stream<Path> found = Files.list(dir)) {
            found.filter(p -> p.getFileName().toString()
                            .endsWith(".manifest"))
                    .sorted()
                    .forEach(manifests::add);
        }

        for (Path manifest : manifests) {
            Map<String, String> stated = keyed(manifest);
            String file = manifest.getFileName().toString();
            if (!stated.containsKey(SCHEMA)) {
                throw new IllegalStateException(file + " declares no "
                        + SCHEMA + ", so it is not an interface"
                        + " descriptor. A chart language pack cannot"
                        + " register an interface: the sky's names and"
                        + " the controls' words are settings a reader"
                        + " chooses separately.");
            }
            String tag = stated.getOrDefault("tag", "").strip();
            if (tag.isBlank()) {
                throw new IllegalStateException(file + " states no"
                        + " language tag, so nothing could select it");
            }
            String already = declaredIn.put(tag, file);
            if (already != null) {
                throw new IllegalStateException("two descriptors claim"
                        + " the interface language " + tag + ": "
                        + already + " and " + file + "; one of them"
                        + " would silently win");
            }
            if ("true".equals(stated.getOrDefault("fixture", "").strip())) {
                throw new IllegalStateException(file + " declares"
                        + " itself a test fixture and is in the build"
                        + " output; fixtures live under test resources"
                        + " and must not ship");
            }
            // A declared strings file must ship (#350). "The words
            // are in <file>" and no such file is a language that
            // registers, is offered, and then says nothing of its
            // own - which reads to a reader as a language that did
            // not work rather than one that is not finished.
            String strings = stated.getOrDefault("strings", "").strip();
            if (!strings.isEmpty()
                    && !Files.isRegularFile(dir.resolve(strings))) {
                throw new IllegalStateException(file + " declares its"
                        + " words are in " + strings + ", which is not"
                        + " beside it. A language that registers and"
                        + " then has nothing to say reads as broken,"
                        + " not as unfinished.");
            }
            if ("en".equals(tag) && strings.isEmpty()) {
                throw new IllegalStateException(file + " declares no"
                        + " strings file. English is the language"
                        + " every other one falls back to, so it is"
                        + " the one language that cannot be a"
                        + " registration alone.");
            }
            byTag.put(tag, stated.getOrDefault("display-name", tag).strip());
        }

        if (byTag.isEmpty()) {
            throw new IllegalStateException(DESCRIPTORS + " holds no"
                    + " interface descriptor, so this build offers the"
                    + " reader no interface language. An empty chart"
                    + " index is a legitimate build; an empty"
                    + " interface index is not, because the controls"
                    + " have to speak something.");
        }

        StringBuilder index = new StringBuilder("""
                # Generated by juranometria.tool.InterfaceLanguageIndexMain.
                # Not committed: it is derived from the descriptors
                # present in this build, so registering a language is
                # enough and no list is maintained by hand. Sorted by
                # language tag, never by the order a directory or a
                # jar happens to enumerate.
                #
                # TWO COLUMNS, where the chart index has three. The
                # shapes differ so that neither discovery can read the
                # other's index and answer plausibly.
                tag\tdisplay-name
                """);
        byTag.forEach((tag, shown) ->
                index.append(tag).append('\t').append(shown).append('\n'));
        Files.writeString(dir.resolve(INDEX), index.toString(),
                StandardCharsets.UTF_8);

        System.out.printf(Locale.ROOT,
                "interface-language index: %d language%s - %s%n",
                byTag.size(), byTag.size() == 1 ? "" : "s",
                String.join(", ", byTag.keySet()));
    }

    private static Map<String, String> keyed(Path file) throws IOException {
        Map<String, String> stated = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int is = line.indexOf('=');
            if (is > 0) {
                stated.put(line.substring(0, is).strip(),
                        line.substring(is + 1).strip());
            }
        }
        return stated;
    }
}
