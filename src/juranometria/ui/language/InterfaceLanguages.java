package juranometria.ui.language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Which languages the controls can speak (Sprint 33, issue #348).
 *
 * <p>The interface half of the reader's two language settings, and a
 * deliberately separate thing from {@code SkyNames}, which answers
 * what the sky is called. This lives in the UI language layer rather
 * than beside the geography: the two registries share their indexing
 * mechanics and nothing else, and a reader picks them apart on
 * purpose - Norwegian constellation names under English menus is a
 * supported combination, not a misconfiguration.
 *
 * <p><strong>Neither registry imports the other.</strong> They are
 * brought together once, where the application composes itself, and
 * nowhere else. A chart pack appearing in this list would be the
 * exact defect the separation exists to prevent, so there is no code
 * path here that could produce one: this reads its own directory,
 * its own index, and an index shaped differently from the chart one.
 *
 * <p><strong>Availability comes from descriptors, never from an
 * absence.</strong> English is offered because
 * {@code resources/interface-language/en.manifest} ships, not because
 * no other language does. Discovery has no fallback and no built-in
 * default: a build with no index has failed to run its indexer, and
 * saying so beats answering "en" from a mechanism that never ran.
 * The indexer refuses to produce an empty index, so the empty case
 * should be unreachable in any build that completed - and if it is
 * reached anyway, this reports it rather than papering over it.
 */
public final class InterfaceLanguages {

    private static final String INDEX =
            "/resources/interface-language/index.tsv";

    private final Map<String, String> displayNames;

    private InterfaceLanguages(Map<String, String> displayNames) {
        this.displayNames = displayNames;
    }

    /** The interface languages this build carries, from its index. */
    public static InterfaceLanguages discover() {
        return discover(InterfaceLanguages.class::getResourceAsStream);
    }

    /**
     * The same, from a given resource route.
     *
     * <p>A missing index is an incomplete build and is reported as
     * one. This is the opposite of the chart side, where no pack is
     * a legitimate build that draws Latin names - the atlas can be
     * shipped with no chart language and cannot be shipped with no
     * interface language.
     */
    public static InterfaceLanguages discover(Resources resources) {
        List<String> rows = read(resources, INDEX);
        if (rows == null) {
            throw new IllegalStateException("this build carries no"
                    + " interface-language index, so nothing says"
                    + " which languages the controls can speak."
                    + " InterfaceLanguageIndexMain did not run, or"
                    + " its output did not reach the image. Guessing"
                    + " English here would hide that.");
        }
        Map<String, String> found = new TreeMap<>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("#")) {
                continue;
            }
            String[] columns = row.split("\t");
            // Two columns, and a chart index has three. A wrong-shaped
            // row is skipped rather than half-read, so pointing this
            // at the chart index yields nothing instead of a
            // plausible answer built from a names filename.
            if (columns.length != 2 || columns[0].equals("tag")) {
                continue;
            }
            String tag = columns[0].strip();
            if (found.put(tag, columns[1].strip()) != null) {
                throw new IllegalStateException("the interface index"
                        + " lists " + tag + " twice, so one of them"
                        + " would silently win");
            }
        }
        if (found.isEmpty()) {
            throw new IllegalStateException("the interface-language"
                    + " index carries no language, so the controls"
                    + " would speak nothing. The indexer refuses to"
                    + " write an empty index, so this index was not"
                    + " written by it.");
        }
        return new InterfaceLanguages(
                Collections.unmodifiableMap(found));
    }

    /** Where resources come from; the classpath, in production. */
    @FunctionalInterface
    public interface Resources {

        InputStream open(String path);
    }

    /**
     * The interface languages this installation offers.
     *
     * <p>Sorted by tag, because a selector listing its choices in one
     * order here and another there is a difference a reader can see
     * and nothing can explain.
     */
    public List<String> tags() {
        return List.copyOf(displayNames.keySet());
    }

    /** The same, for building an availability set. */
    public Set<String> tagSet() {
        return Set.copyOf(displayNames.keySet());
    }

    /**
     * What a language calls itself, for a selector.
     *
     * <p>Descriptor data. Whether a selector should show this autonym
     * or a name translated into the language the reader is currently
     * reading is #350's question, and answering it changes nothing
     * about which languages are available.
     */
    public String displayName(String tag) {
        return displayNames.getOrDefault(tag, tag);
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
