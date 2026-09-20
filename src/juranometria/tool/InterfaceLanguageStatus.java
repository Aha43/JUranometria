package juranometria.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the interface manifest says about a language, in the words its
 * companions print (Sprint 33, issue #350).
 *
 * <p>Twelve generators describe the Norwegian interface and every one
 * of them used to state its status in a string literal of its own.
 * That is twelve places to forget: the manifest could say one thing
 * and a companion another, and the drift would read exactly like
 * evidence. A reader has no way to tell a stale sentence from a
 * current one.
 *
 * <p>So the status is read from the descriptor that owns it and
 * formatted <strong>once</strong>. A companion cannot claim a status
 * the manifest does not, because it no longer has a sentence to claim
 * it with.
 *
 * <p>An unrecognised status <strong>refuses</strong> rather than
 * printing itself. A manifest that said {@code status=verified} would
 * otherwise produce a companion asserting a word nobody had defined -
 * and this vocabulary is deliberately small, because each word is a
 * claim about who has read the translation and on what basis.
 */
public final class InterfaceLanguageStatus {

    private InterfaceLanguageStatus() {
    }

    private static final String MANIFESTS =
            "/resources/interface-language/";

    /**
     * The status word recorded in a language's manifest.
     *
     * @param tag the language tag, as the manifest's {@code tag} field
     *     spells it
     */
    public static String of(String tag) {
        return field(tag, "status");
    }

    /** What the language calls itself, from the same descriptor. */
    public static String displayName(String tag) {
        return field(tag, "display-name");
    }

    /**
     * The paragraph a companion prints about this language.
     *
     * <p>The whole paragraph, not a word dropped into prose each
     * generator writes for itself: prose around a correct word is
     * exactly what goes stale. `SettingsSheetMain` carried "Nothing
     * here has been reviewed by a person who reads it" beside its
     * status, and that sentence would have survived a status change
     * untouched.
     */
    public static String statement(String tag) {
        String status = of(tag);
        String name = displayName(tag);
        return switch (status) {
            case "draft" -> "**" + name + " — draft.** Nothing here"
                    + " has been read by a person who reads the"
                    + " language. Recorded in `" + tag + ".manifest`,"
                    + " and read from there rather than written here.";
            case "reviewed" -> "**" + name + " — reviewed.** A person"
                    + " who reads the language has been through these"
                    + " surfaces and said these are the words an atlas"
                    + " should use. This is linguistic judgement, not"
                    + " corroboration against a source: unlike the"
                    + " constellation names next door, these are"
                    + " original translations written for this"
                    + " application and there is no citation to check"
                    + " them against. Recorded in `" + tag
                    + ".manifest`, and read from there rather than"
                    + " written here.";
            case "complete" -> "**" + name + " — complete.** This is"
                    + " the language the application's own words are"
                    + " written in, so there is no translation to"
                    + " review. Recorded in `" + tag + ".manifest`,"
                    + " and read from there rather than written here.";
            default -> throw new IllegalStateException(
                    "the manifest for \"" + tag + "\" says status=\""
                            + status + "\", which is not one of draft,"
                            + " reviewed or complete. A companion will"
                            + " not print a claim nobody has defined:"
                            + " each of those words says who has read"
                            + " the translation and on what basis, and"
                            + " a fourth would say nothing while"
                            + " looking like it said something.");
        };
    }

    /** One field of one manifest, refusing a descriptor without it. */
    private static String field(String tag, String name) {
        Map<String, String> stated = read(tag);
        String value = stated.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("the manifest for \"" + tag
                    + "\" declares no " + name + ". A companion that"
                    + " guessed one would be inventing the very thing"
                    + " the descriptor exists to record.");
        }
        return value;
    }

    private static Map<String, String> read(String tag) {
        String path = MANIFESTS + tag + ".manifest";
        try (InputStream in =
                     InterfaceLanguageStatus.class
                             .getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("no interface manifest"
                        + " at " + path + ". A companion describes a"
                        + " language the application ships; if the"
                        + " descriptor is not on the classpath, the"
                        + " language is not shipped.");
            }
            Map<String, String> stated = new LinkedHashMap<>();
            try (BufferedReader lines = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) {
                    String said = line.strip();
                    int is = said.indexOf('=');
                    if (said.isEmpty() || said.startsWith("#")
                            || is < 0) {
                        continue;
                    }
                    stated.put(said.substring(0, is).strip(),
                            said.substring(is + 1).strip());
                }
            }
            return stated;
        } catch (IOException e) {
            throw new IllegalStateException("the interface manifest at "
                    + path + " could not be read", e);
        }
    }
}
