package juranometria.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What the constellations are called, in the reader's chart language
 * (Sprint 33, issue #348).
 *
 * <p>One seam answers one question: <em>what is this constellation
 * called</em>. Consumers ask it and carry no language switch of their
 * own - the gate's contract, and the reason a second language never
 * has to be threaded through the renderer.
 *
 * <p><strong>Adding a pack is enough.</strong> Languages are not
 * listed anywhere: each pack under {@code resources/sky-language}
 * carries its own names and its own provenance, the build writes a
 * sorted index of what it found, and this reads that index. Nothing
 * here, in {@code SceneAssembler}, in placement or in the renderer
 * needs editing for a language to appear.
 *
 * <p><strong>Fallback is resolved before the scene sees it.</strong>
 * The map handed out always names all the constellations asked for:
 * where a pack is silent, the official Latin name stands in. A page
 * never shows a blank where a name belongs, and no consumer has to
 * know which language answered.
 *
 * <p>Identity is never translated. The map is keyed by IAU
 * abbreviation and ordered by it, because iteration order reaches the
 * renderer and overlapping name text must stack identically whatever
 * language is chosen.
 */
public final class SkyNames {

    /** The chart mode that asks for the official Latin names. */
    public static final String LATIN = "latin";

    private static final String INDEX =
            "/resources/sky-language/index.tsv";

    private static final String PACKS = "/resources/sky-language/";

    /** One language this installation can draw the sky in. */
    public record Pack(String tag, String displayName,
                       Map<String, String> names) {

        public Pack {
            names = Map.copyOf(names);
        }
    }

    private final Map<String, Pack> packs;

    private SkyNames(Map<String, Pack> packs) {
        this.packs = packs;
    }

    /** The packs this build carries, discovered from its own index. */
    public static SkyNames discover() {
        return discover(SkyNames.class::getResourceAsStream);
    }

    /**
     * The same, from a given resource route.
     *
     * <p>Missing index means no pack shipped, which is a legitimate
     * build: the atlas draws Latin names and offers no chart
     * language. A malformed one is not legitimate and says so.
     */
    public static SkyNames discover(Resources resources) {
        Map<String, Pack> found = new TreeMap<>();
        List<String> rows = read(resources, INDEX);
        if (rows == null) {
            return new SkyNames(Map.of());
        }
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("#")) {
                continue;
            }
            String[] columns = row.split("\t");
            if (columns.length != 3 || columns[0].equals("tag")) {
                continue;
            }
            String tag = columns[0].strip();
            List<String> names = read(resources, PACKS + columns[1].strip());
            if (names == null) {
                throw new IllegalStateException("the index offers "
                        + tag + " but its names, " + columns[1]
                        + ", are not in this build; a language a reader"
                        + " can choose and the atlas cannot draw is"
                        + " worse than one it never offered");
            }
            found.put(tag, new Pack(tag, columns[2].strip(),
                    parse(tag, names)));
        }
        return new SkyNames(Collections.unmodifiableMap(found));
    }

    /** Where resources come from; the classpath, in production. */
    @FunctionalInterface
    public interface Resources {

        InputStream open(String path);
    }

    /** The chart languages this installation offers, sorted by tag. */
    public List<String> chartLanguages() {
        return List.copyOf(packs.keySet());
    }

    /** What a language calls itself, for a selector. */
    public String displayName(String tag) {
        Pack pack = packs.get(tag);
        return pack == null ? tag : pack.displayName();
    }

    /**
     * Every name the chart should draw, with fallback resolved.
     *
     * <p>Keyed and ordered by IAU abbreviation, for every identity
     * asked about. {@link #LATIN}, an unknown tag and a pack that
     * happens to be silent about a constellation all resolve the
     * same way: to the official Latin name, which every build
     * carries whatever languages are installed.
     */
    public Map<String, String> namesFor(String chartLanguage,
                                        Map<String, String> latin) {
        Pack pack = packs.get(chartLanguage);
        Map<String, String> named = new TreeMap<>();
        latin.forEach((id, official) -> named.put(id,
                pack == null ? official
                        : pack.names().getOrDefault(id, official)));
        return Collections.unmodifiableMap(named);
    }

    private static Map<String, String> parse(String tag,
                                             List<String> rows) {
        Map<String, String> names = new LinkedHashMap<>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("#")) {
                continue;
            }
            String[] columns = row.split("\t");
            if (columns.length != 2 || columns[0].equals("IAU")) {
                continue;
            }
            if (names.put(columns[0].strip(), columns[1].strip()) != null) {
                throw new IllegalStateException(tag + " names "
                        + columns[0].strip() + " twice, so another"
                        + " constellation is named not at all");
            }
        }
        if (names.isEmpty()) {
            throw new IllegalStateException(tag + " carries no names");
        }
        return names;
    }

    private static List<String> read(Resources resources, String path) {
        try (InputStream stream = resources.open(path)) {
            if (stream == null) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException cannotRead) {
            throw new IllegalStateException(
                    "could not read " + path, cannotRead);
        }
    }
}
