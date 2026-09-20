package juranometria.ui.language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The words the application says, in the reader's language
 * (Sprint 33, issue #350).
 *
 * <h2>Keys, not sentences</h2>
 *
 * <p>Every reader-visible string is asked for by an explicit key.
 * Nothing is assembled from translated fragments: where a sentence
 * takes a value it is one pattern with numbered arguments, because
 * languages do not agree on word order and a sentence glued together
 * in English order is a sentence only in English.
 *
 * <pre>
 *   settings.language.follow = Follow interface — currently {0}
 * </pre>
 *
 * <h2>Fallback is English, and it is visible</h2>
 *
 * <p>A key a translation has not reached yet resolves to the English
 * text. That is the gate's rule, and the important half is what it
 * refuses: a missing key never reaches a reader as a key.
 * {@code settings.ok.label} is not a word in any language, and
 * showing one would turn a translation gap into a defect the reader
 * has to interpret.
 *
 * <p>A key that <strong>English</strong> does not define is a
 * different thing entirely - not a gap but a mistake, since English
 * is the language every other one falls back to. It throws, and a
 * contract catches it before a reader could.
 *
 * <p><strong>The system locale decides nothing here.</strong> Lookup
 * follows the reader's stored choice and the English fallback, never
 * {@code Locale.getDefault()}: an atlas whose words changed because
 * of the machine it was started on would be answering a question
 * nobody asked. The chosen language IS used for formatting numbers
 * inside a pattern, which is what it is for.
 */
public final class InterfaceText {

    /** The language every other one falls back to. */
    public static final String ENGLISH = "en";

    private static final String STRINGS = "/resources/interface-language/";

    private final String tag;

    private final Map<String, String> chosen;

    private final Map<String, String> english;

    private final Locale formatting;

    private InterfaceText(String tag, Map<String, String> chosen,
                          Map<String, String> english) {
        this.tag = tag;
        this.chosen = chosen;
        this.english = english;
        this.formatting = Locale.forLanguageTag(tag);
    }

    /** The words for a language, from the classpath. */
    public static InterfaceText forLanguage(String tag) {
        return forLanguage(tag, InterfaceText.class::getResourceAsStream);
    }

    /** The same, from a given resource route. */
    public static InterfaceText forLanguage(String tag,
                                            Resources resources) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException(
                    "a language is a choice, and this one is blank");
        }
        Map<String, String> english = read(resources, ENGLISH);
        if (english == null || english.isEmpty()) {
            throw new IllegalStateException("this build carries no"
                    + " English interface text, and English is what"
                    + " every other language falls back to. Without it"
                    + " a missing translation has nowhere to go.");
        }
        if (ENGLISH.equals(tag)) {
            return new InterfaceText(tag, english, english);
        }
        Map<String, String> chosen = read(resources, tag);
        if (chosen == null) {
            // A language registered with no strings of its own is a
            // legitimate state - #348 shipped exactly that for
            // English before this existed - and it reads entirely in
            // English rather than failing.
            chosen = Map.of();
        }
        return new InterfaceText(tag, chosen, english);
    }

    /** Where resources come from; the classpath, in production. */
    @FunctionalInterface
    public interface Resources {

        InputStream open(String path);
    }

    /**
     * What THIS language says for a key, or null if it says nothing.
     *
     * <p>{@link #say} falls back to English, which is right for a
     * surface that would rather show a reader something than nothing.
     * It is wrong where the question is "has this language written
     * this yet?" - the startup failure reporter has to know, because
     * it may show a language's own headline only when that language
     * can also supply the whole remedy beneath it, and an English
     * headline arriving here would be indistinguishable from a
     * translated one (#350).
     */
    public String sayIfDefined(String key) {
        return chosen.get(key);
    }

    /** Which language these words are in. */
    public String language() {
        return tag;
    }

    /**
     * What to say for a key.
     *
     * @throws IllegalStateException if English does not define it,
     *     because that is a mistake rather than a translation gap
     */
    public String say(String key) {
        String said = chosen.get(key);
        if (said != null) {
            return said;
        }
        String fallback = english.get(key);
        if (fallback == null) {
            throw new IllegalStateException("no interface text is"
                    + " defined for \"" + key + "\", in " + tag
                    + " or in English. A key with no English behind it"
                    + " has nothing to fall back to, and must never"
                    + " reach a reader as itself.");
        }
        return fallback;
    }

    /**
     * The same, with values placed into the pattern.
     *
     * <p>{@link MessageFormat}, so a translation decides where its
     * values go. The arguments are numbered rather than positional
     * in the text, which is the whole point: Norwegian may need them
     * in an order English does not.
     */
    public String say(String key, Object... values) {
        if (values == null || values.length == 0) {
            return say(key);
        }
        String pattern = say(key);
        checkArguments(key, pattern, values.length);
        try {
            return new MessageFormat(pattern, formatting).format(values);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("the interface text for \""
                    + key + "\" is not a usable pattern: \"" + pattern
                    + "\"", malformed);
        }
    }

    /**
     * Refuses a pattern that asks for a value it was not given.
     *
     * <p>The gap this closes: the English/Norwegian comparison checks
     * that two patterns agree with EACH OTHER, which cannot establish
     * that either agrees with its caller. A pattern written {@code
     * {2}} and called with two arguments printed the placeholder
     * itself to a reader, in both languages equally, and no contract
     * saw it.
     *
     * <p>Unused supplied arguments are allowed - a translation may
     * legitimately not need a value English uses - and a repeated
     * placeholder is allowed, because saying a value twice is a
     * choice a language may need to make.
     */
    private static void checkArguments(String key, String pattern,
                                       int supplied) {
        Matcher placeholder = ARGUMENT.matcher(pattern);
        while (placeholder.find()) {
            int index = Integer.parseInt(placeholder.group(1));
            if (index >= supplied) {
                throw new IllegalStateException("the interface text for"
                        + " \"" + key + "\" asks for argument {" + index
                        + "} but was given " + supplied
                        + (supplied == 1 ? " argument" : " arguments")
                        + ": \"" + pattern + "\". A placeholder with"
                        + " nothing behind it reaches a reader as"
                        + " itself.");
            }
        }
    }

    /** A numbered placeholder: the {0} of a MessageFormat pattern. */
    private static final Pattern ARGUMENT =
            Pattern.compile("\\{(\\d+)[^}]*}");

    /** Every key this language defines, for a contract to check. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(
                new TreeMap<>(chosen).keySet());
    }

    /** Every key English defines - the complete set. */
    public Set<String> englishKeys() {
        return Collections.unmodifiableSet(
                new TreeMap<>(english).keySet());
    }

    /** The pattern behind a key in this language, if it has one. */
    public String patternIn(String key) {
        return chosen.get(key);
    }

    /** The English pattern behind a key. */
    public String englishPattern(String key) {
        return english.get(key);
    }

    /**
     * One escape, and only one: {@code \n} becomes a line break.
     *
     * <p>This file is one line per key, and a value that needs two
     * lines had no way to say so. The overwrite question did say so -
     * it has carried {@code .\nReplace it?} since the export
     * surface was written - and a reader was shown the backslash and
     * the n, in English and in Norwegian alike. The resource was
     * right; the reader of it was not.
     *
     * <p><strong>Deliberately not general escaping.</strong> Java
     * properties decode tabs, unicode escapes, line
     * continuations and more, and adopting all of that would make
     * every existing value in every language pay for one surface's
     * shape - and would quietly change any value that happens to
     * contain a backslash. So exactly one escape is supported, and
     * anything else is <strong>refused with its key and its file</strong>
     * rather than silently altered or silently printed. The startup
     * remedies stay whole documents; this does not invite them back
     * (#350).
     *
     * <p>Decoding happens on the pattern, before {@code MessageFormat}
     * inserts anything, so an argument containing a backslash - a
     * Windows path a reader chose - is untouched.
     */
    private static String decode(String path, String key, String value) {
        int at = value.indexOf('\\');
        if (at < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char next = i + 1 < value.length() ? value.charAt(i + 1) : 0;
            if (next != 'n') {
                throw new IllegalStateException(path + " defines \""
                        + key + "\" with the escape \"\\" + (next == 0
                                ? "" : String.valueOf(next))
                        + "\", which this reader does not support. It"
                        + " understands \\n and nothing else, on"
                        + " purpose: a value that quietly meant"
                        + " something other than it said is how a"
                        + " reader came to be shown a backslash and an"
                        + " n for two years (#350).");
            }
            out.append('\n');
            i++;
        }
        return out.toString();
    }

    private static Map<String, String> read(Resources resources,
                                            String tag) {
        String path = STRINGS + tag + ".properties";
        try (InputStream stream = resources.open(path)) {
            if (stream == null) {
                return null;
            }
            Map<String, String> said = new LinkedHashMap<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    int is = line.indexOf('=');
                    if (is <= 0) {
                        throw new IllegalStateException(path
                                + " carries a line that is neither a"
                                + " comment nor a key=value pair: \""
                                + line + "\"");
                    }
                    String key = line.substring(0, is).strip();
                    if (said.put(key, decode(path, key,
                            line.substring(is + 1).strip())) != null) {
                        throw new IllegalStateException(path
                                + " defines \"" + key + "\" twice, so"
                                + " one of the two is never said");
                    }
                }
            }
            return said;
        } catch (IOException cannotRead) {
            throw new IllegalStateException(
                    "could not read " + path, cannotRead);
        }
    }
}
