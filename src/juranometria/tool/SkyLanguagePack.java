package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * What a contributed language pack must be (Sprint 33, issue #347).
 *
 * <p>The gate has to prove a contributor can add a language without
 * private material and without touching code. This is the shape such
 * a contribution takes and the rules it is held to. It lives in the
 * tool package on purpose: #347 changes no reader behaviour, so this
 * defines and checks the contract that #348 will implement a service
 * against.
 *
 * <p>A pack is <strong>data plus an account of itself</strong>. The
 * data is 88 canonical abbreviations and their names in one language;
 * the account says which language, from where, under what licence,
 * retrieved when, and changed how. Neither half is optional, and the
 * refusals below are what stop a pack being nearly right:
 *
 * <ul>
 *   <li>eighty-seven names is not a language pack;</li>
 *   <li>an eighty-ninth constellation does not exist;</li>
 *   <li>a duplicated identity means one constellation was named
 *       twice and another not at all;</li>
 *   <li>a pack that declares itself a test fixture is refused by
 *       production, which is what keeps the schema from being
 *       satisfiable by a plausible-looking lie.</li>
 * </ul>
 */
public final class SkyLanguagePack {

    private SkyLanguagePack() {
    }

    /** Why a pack was refused. */
    public static final class Refused extends IllegalStateException {

        Refused(String because) {
            super(because);
        }
    }

    /** One language pack, read and checked. */
    public record Pack(String tag, String displayName, int schema,
                       boolean fixture, Map<String, String> names,
                       Map<String, String> provenance) {

        /** The name for a constellation, or the Latin one if absent. */
        public String nameOf(String abbreviation, String latinFallback) {
            // The deterministic fallback, stated once and in one
            // place: a page never shows a blank where a name belongs.
            return names.getOrDefault(abbreviation, latinFallback);
        }
    }

    /**
     * Reads a pack and holds it to the contract.
     *
     * @param canonical the 88 abbreviations, from the identity layer
     * @param production whether this is a production load, which
     *     refuses a pack declaring itself a fixture
     */
    public static Pack read(Path data, Path manifest,
                            Set<String> canonical, boolean production)
            throws IOException {
        Map<String, String> stated = keyed(manifest);
        String tag = stated.getOrDefault("tag", "").strip();
        if (tag.isBlank()) {
            throw new Refused("a pack states which language it is");
        }
        if (stated.getOrDefault("schema", "").isBlank()) {
            throw new Refused(tag + ": a pack states its schema"
                    + " version, so a later format can be told from an"
                    + " older one rather than guessed at");
        }
        boolean fixture = "true".equals(
                stated.getOrDefault("fixture", "").strip());
        if (fixture && production) {
            throw new Refused(tag + " declares itself a test fixture"
                    + " and cannot be loaded as a language. This"
                    + " refusal is the point: a pack may be honest"
                    + " about having no source, and then it is not a"
                    + " language a reader may choose.");
        }
        if (!fixture) {
            List<String> missing = new ArrayList<>();
            for (String required : List.of("source", "licence",
                    "retrieved", "transformations")) {
                if (stated.getOrDefault(required, "").isBlank()) {
                    missing.add(required);
                }
            }
            if (!missing.isEmpty()) {
                throw new Refused(tag + ": a contributed pack says"
                        + " where its names came from; missing "
                        + String.join(", ", missing));
            }
        }

        Map<String, String> names = new LinkedHashMap<>();
        Set<String> duplicated = new TreeSet<>();
        for (String line : Files.readAllLines(data)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            if (columns.length != 2 || columns[0].equals("IAU")) {
                continue;
            }
            if (names.put(columns[0].strip(), columns[1].strip()) != null) {
                duplicated.add(columns[0].strip());
            }
        }
        if (!duplicated.isEmpty()) {
            throw new Refused(tag + ": named twice, so something else"
                    + " is named not at all: " + duplicated);
        }
        Set<String> absent = new TreeSet<>(canonical);
        absent.removeAll(names.keySet());
        Set<String> invented = new LinkedHashSet<>(names.keySet());
        invented.removeAll(canonical);
        if (!absent.isEmpty()) {
            throw new Refused(tag + ": " + absent.size()
                    + " constellations unnamed, so this is not a"
                    + " language pack: " + absent);
        }
        if (!invented.isEmpty()) {
            throw new Refused(tag + ": names constellations that do"
                    + " not exist: " + invented);
        }
        return new Pack(tag,
                stated.getOrDefault("display-name", tag).strip(),
                Integer.parseInt(stated.get("schema").strip()),
                fixture, Map.copyOf(names), Map.copyOf(stated));
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
