package juranometria.app;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one licensing map, and what any summary must say of it
 * (Sprint 33, issue #350).
 *
 * <p>It lived inside {@code AboutDialogTest} as a private table until
 * a second reader needed it. Copying it would have made two maps that
 * could drift, which is the failure the map exists to prevent - so it
 * is one class, used by every test that holds a summary to it, in
 * whatever language the summary is written.
 *
 * <p>Three claims, and the third is the one that cannot be
 * automated:
 *
 * <ul>
 *   <li>every source family names its licence <strong>in its own
 *       paragraph</strong>, so a swapped assignment cannot pass by
 *       having both strings present somewhere;</li>
 *   <li>the licence identifiers are <strong>exact</strong> -
 *       {@code CC BY-NC 3.0 IGO} is a name, not a description, and no
 *       language translates it;</li>
 *   <li>the Tycho-2 consequence is stated, and stated in
 *       <strong>this language's approved words</strong>.</li>
 * </ul>
 *
 * <p><strong>Why the third is a pinned sentence and not a
 * heuristic.</strong> An earlier version asked that the Tycho
 * paragraph be longer than 120 characters, which a long attribution
 * that never mentioned the restriction would have passed, and which a
 * translation reversing the restriction would also have passed. This
 * is legal-facing prose: a language that ships a summary must have an
 * owner-approved statement of the consequence recorded here, and a
 * language without one cannot ship a summary at all. That is
 * deliberate friction. Adding a language to a legal statement is a
 * decision somebody makes, not a string a test infers.
 */
public final class LicensingMap {

    private LicensingMap() {
    }

    /** The fact: which licence belongs to which source family. */
    public static final Map<String, String> LICENCES = Map.of(
            "code", "MIT",
            "tycho", "CC BY-NC 3.0 IGO",
            "openngc", "CC BY-SA 4.0",
            "constellations", "BSD-3-Clause",
            "starnames", "BSD-3-Clause",
            "icons", "MIT");

    /** The order they are checked in, so a failure reads predictably. */
    public static final List<String> FAMILIES = List.of("code", "tycho",
            "openngc", "constellations", "starnames", "icons");

    /**
     * How each language names a family, as {@code |}-separated words.
     *
     * <p><strong>Only the finding is per language.</strong> Which
     * licence belongs to which family is a fact and is stated once
     * above. How a family is <em>named</em> is not: an English
     * summary says "code" and a Norwegian one says "kode", and
     * {@code constellation} and {@code star names} find nothing at
     * all in a Norwegian document. {@code tycho}, {@code openngc} and
     * {@code tabler} happen to be identities and are the same
     * everywhere.
     *
     * <p>This is the difference between one map expressed in two
     * languages and two maps that can drift apart.
     */
    public static final Map<String, Map<String, String>> KEYWORDS =
            Map.of(
                    "en", Map.of(
                            "code", "code",
                            "tycho", "tycho",
                            "openngc", "openngc",
                            "constellations", "constellation",
                            "starnames", "star names|star-identity",
                            "icons", "tabler|icons"),
                    "nb-NO", Map.of(
                            "code", "kode",
                            "tycho", "tycho",
                            "openngc", "openngc",
                            "constellations", "stjernebilde",
                            "starnames", "stjernenavn",
                            "icons", "tabler|ikon"));

    /**
     * The approved statement of the Tycho-2 consequence, per language.
     *
     * <p>Each is a phrase the owner has approved as saying that the
     * packaged whole may be used and redistributed non-commercially
     * only, for as long as that data is included. A language absent
     * from this map may not ship a summary.
     *
     * <p><strong>It must be the operative clause, not a word.</strong>
     * This was {@code "non-commercially"} for one run, and a fixture
     * reversing the restriction to "commercially or
     * non-commercially" passed it - the approved phrase survived
     * inside its own negation. A phrase that can appear in a sentence
     * meaning the opposite is not a check. No substring test is proof
     * against a determined translator, which is why the real control
     * is that a person approves the phrase before a language may ship
     * a summary at all; this makes the automated half hard to pass by
     * accident.
     */
    public static final Map<String, String> CONSEQUENCE = Map.of(
            "en", "non-commercially only",
            "nb-NO", "bare brukes og videredistribueres til"
                    + " ikke-kommersielle formål");

    /** Holds one summary, in one language, to the whole map. */
    public static void assertSummary(String language, String summary) {
        assertTrue(KEYWORDS.containsKey(language)
                        && CONSEQUENCE.containsKey(language),
                "a summary shipped for \"" + language + "\", and it is"
                        + " not registered as an approved language for"
                        + " the licensing map. Both the words that"
                        + " locate each source family and the"
                        + " statement of the Tycho-2 consequence are"
                        + " approved by a person, or the summary does"
                        + " not ship: that statement is the reader's"
                        + " only warning that the packaged whole is"
                        + " non-commercial.");
        assertPairings(language, language, summary);

        String tycho = paragraphWith(summary,
                KEYWORDS.get(language).get("tycho"));
        assertTrue(!tycho.isBlank(),
                language + ": some paragraph names the Tycho-2 data");
        String approved = CONSEQUENCE.get(language);
        assertTrue(tycho.contains(normalize(approved)),
                language + ": the paragraph naming Tycho-2 must state"
                        + " the consequence in this language's"
                        + " approved words (\"" + approved + "\"), and"
                        + " says: " + tycho);
    }

    /**
     * Each licence beside the source it belongs to, in English.
     *
     * <p>The form the English documents - {@code LICENSING.md} and
     * its packaged twin - are held to.
     */
    public static void assertPairings(String document, String text) {
        assertPairings(document, "en", text);
    }

    /** The same, found by a stated language's own words. */
    public static void assertPairings(String document, String language,
                                      String text) {
        Map<String, String> keywords = KEYWORDS.get(language);
        assertTrue(keywords != null,
                document + ": no approved keywords for \"" + language
                        + "\"");
        for (String family : FAMILIES) {
            String paragraph = paragraphWith(text, keywords.get(family));
            assertTrue(paragraph.contains(
                            normalize(LICENCES.get(family))),
                    document + ": the paragraph naming '" + family
                            + "' (found by \"" + keywords.get(family)
                            + "\") must carry " + LICENCES.get(family)
                            + ", and says: " + paragraph);
        }
    }

    /** Every licence identifier, exactly as it is spelled. */
    public static List<String> identifiers() {
        return List.of("MIT", "CC BY-NC 3.0 IGO", "CC BY-SA 4.0",
                "BSD-3-Clause");
    }

    /**
     * The first normalized paragraph mentioning any of the
     * {@code |}-separated keywords; empty if none does, which fails
     * the containing assertion.
     */
    public static String paragraphWith(String text, String keywords) {
        for (String paragraph : text.split("\\n\\s*\\n|\\n(?=\\|)")) {
            String normalized = normalize(paragraph);
            for (String keyword : keywords.split("\\|")) {
                if (normalized.contains(normalize(keyword))) {
                    return normalized;
                }
            }
        }
        return "";
    }

    /**
     * Comparison form: lower case, with whitespace and hyphens gone.
     *
     * <p>Taken unchanged from the test that owned this map, and
     * unchanged for a reason - it is what lets a pairing match across
     * a line break in a summary and across a table row in
     * {@code LICENSING.md} alike. A gentler form that only collapsed
     * whitespace was tried while extracting this class and broke the
     * {@code LICENSING.md} check immediately.
     *
     * <p>It is also why an approved consequence must be the operative
     * clause: this form erases the difference between
     * {@code non-commercially} and {@code noncommercially}, so a
     * phrase short enough to sit inside its own negation is worth
     * nothing here.
     */
    public static String normalize(String text) {
        return text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[\\s-]+", "");
    }
}
