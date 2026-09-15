package juranometria.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What silence means, and what a reader's choice means (issue #347).
 *
 * <p>The upgrade has to be invisible. A reader who has been using 2.0
 * and never opens the language setting must see the page they had:
 * English controls, Latin names on the chart. That is easy to get
 * right by accident and easy to get wrong later, which is why the
 * meanings are pinned here before anything is built on them.
 *
 * <p>The distinction that costs nothing today and everything later:
 * <strong>absence is not {@code follow-interface}</strong>. They
 * behave identically now, and they mean different things - "never
 * asked" against "asked to follow". A store that conflated them
 * could never change its default without silently moving the chart
 * of a reader who never agreed to anything.
 */
class SkyLanguageChoiceTest {

    private static final String NORWEGIAN = "nb-NO";

    /** What a Norwegian-capable installation offers. */
    private static final SkyLanguageChoice.Available INSTALLED =
            new SkyLanguageChoice.Available(
                    Set.of(SkyLanguageChoice.ENGLISH, NORWEGIAN),
                    Set.of(NORWEGIAN));

    /** A 2.0 store, untouched, is the page a 2.0 reader had. */
    @Test
    void aStoreWithNoLanguageKeysReadsAsEnglishAndLatin() {
        SkyLanguageChoice choice = SkyLanguageChoice.read(Map.of(
                "chart.equatorialGrid", "true",
                "appearance", "light"), INSTALLED);

        assertEquals(SkyLanguageChoice.ENGLISH, choice.interfaceLanguage(),
                "a missing interface language reads as English");
        assertEquals(SkyLanguageChoice.FOLLOW, choice.chartLanguage(),
                "and a missing chart language follows it");
        assertEquals(SkyLanguageChoice.LATIN, choice.namesOnTheChart(),
                "so the chart draws the Latin names it always drew,"
                        + " which is what makes the upgrade invisible");
        assertFalse(choice.everChosen(),
                "and nothing here was ever chosen by anybody");
    }

    /** Absence and an explicit follow behave the same, and differ. */
    @Test
    void absenceAndExplicitFollowAgreeTodayWithoutBeingTheSameThing() {
        SkyLanguageChoice silent = SkyLanguageChoice.read(Map.of(), INSTALLED);
        SkyLanguageChoice asked = SkyLanguageChoice.read(Map.of(
                SkyLanguageChoice.INTERFACE_KEY, SkyLanguageChoice.ENGLISH,
                SkyLanguageChoice.CHART_KEY, SkyLanguageChoice.FOLLOW),
                INSTALLED);

        assertEquals(silent.namesOnTheChart(), asked.namesOnTheChart(),
                "the reader sees the same page either way, today");
        assertEquals(silent.interfaceLanguage(), asked.interfaceLanguage(),
                "and the same interface");
        assertFalse(silent.everChosen(), "but one was never asked");
        assertTrue(asked.everChosen(),
                "and the other chose to follow - which is what lets a"
                        + " future default change migrate the first"
                        + " explicitly instead of quietly redefining"
                        + " what its silence meant");
    }

    /** Saving writes the whole choice, never half of it. */
    @Test
    void savingPersistsBothKeysExplicitly() {
        Map<String, String> stored = new LinkedHashMap<>(
                SkyLanguageChoice.read(Map.of(), INSTALLED)
                        .withInterface(NORWEGIAN)
                        .toStore());

        assertEquals(Map.of(
                        SkyLanguageChoice.INTERFACE_KEY, "nb-NO",
                        SkyLanguageChoice.CHART_KEY, "follow-interface"),
                stored,
                "changing one setting persists both, because leaving"
                        + " the other absent would say 'never asked'"
                        + " about something the reader just decided");
    }

    /** Following is live: the chart moves with the interface. */
    @Test
    void followingTheInterfaceChangesTheNamesOnTheChart() {
        SkyLanguageChoice following = SkyLanguageChoice.read(Map.of(
                SkyLanguageChoice.INTERFACE_KEY, NORWEGIAN,
                SkyLanguageChoice.CHART_KEY, SkyLanguageChoice.FOLLOW),
                INSTALLED);
        assertEquals(NORWEGIAN,
                following.namesOnTheChart(),
                "a Norwegian interface that is followed gives a"
                        + " Norwegian sky");

        SkyLanguageChoice pinned = following.withChart(
                SkyLanguageChoice.LATIN);
        assertEquals(SkyLanguageChoice.LATIN, pinned.namesOnTheChart(),
                "while an explicit chart choice stays put whatever the"
                        + " interface does - a reader learning the"
                        + " canonical names in their own language");
        assertEquals(NORWEGIAN,
                pinned.interfaceLanguage(),
                "and the interface is untouched by that");
    }

    /**
     * A value this version does not know is replaced, not obeyed.
     *
     * <p>Never handed to {@code Locale.getDefault()} and never taken
     * as an arbitrary language tag. A store edited by hand, or
     * written by a later version, must leave the reader with a
     * working atlas rather than a machine-dependent one.
     */
    @Test
    void anUnknownOrMalformedValueFallsBackDeterministically() {
        for (String nonsense : new String[] {"", "  ", "de", "la",
                "nb_NO", "en-US", "../../etc/passwd", "follow"}) {
            SkyLanguageChoice choice = SkyLanguageChoice.read(Map.of(
                    SkyLanguageChoice.INTERFACE_KEY, nonsense,
                    SkyLanguageChoice.CHART_KEY, nonsense), INSTALLED);

            assertEquals(SkyLanguageChoice.ENGLISH,
                    choice.interfaceLanguage(),
                    "\"" + nonsense + "\" is not a language this"
                            + " version knows");
            assertEquals(SkyLanguageChoice.FOLLOW, choice.chartLanguage(),
                    "and not a chart mode either");
            assertFalse(INSTALLED.offers(
                            SkyLanguageChoice.INTERFACE_KEY, nonsense),
                    "and it stays diagnosable rather than being"
                            + " silently normalised into something"
                            + " that looks chosen: \"" + nonsense + "\"");
        }
    }

    /** Latin is a chart mode, and not a claim about the interface. */
    @Test
    void latinIsNomenclatureRatherThanAnInterfaceLanguage() {
        assertFalse(INSTALLED.offers(
                        SkyLanguageChoice.INTERFACE_KEY,
                        SkyLanguageChoice.LATIN),
                "the interface cannot be set to latin: the atlas does"
                        + " not speak Latin, it writes Latin names");
        assertFalse(INSTALLED.offers(
                        SkyLanguageChoice.CHART_KEY, "la"),
                "and the chart mode is not the language tag la,"
                        + " which would make a claim about the"
                        + " interface that is not true");
        assertTrue(INSTALLED.offers(
                        SkyLanguageChoice.CHART_KEY,
                        SkyLanguageChoice.LATIN),
                "latin is a chart nomenclature mode");
    }

    /**
     * A language this class has never heard of is selectable.
     *
     * <p>The contract the decision document promises: a translation
     * arrives as reviewed data, and nothing in Java changes. An
     * earlier version of this class accepted only the tags compiled
     * into it, so the setting silently refused what the loader
     * accepted - the document was true of one half and false of the
     * other. Swedish appears here in no constant, no enum and no
     * switch; it is available because the installation has it.
     */
    @Test
    void aLanguageThisClassDoesNotNameCanStillBeChosen() {
        String swedish = "sv-SE";
        SkyLanguageChoice.Available withSwedish =
                new SkyLanguageChoice.Available(
                        Set.of(SkyLanguageChoice.ENGLISH, NORWEGIAN, swedish),
                        Set.of(NORWEGIAN, swedish));

        SkyLanguageChoice chosen = SkyLanguageChoice
                .read(Map.of(), withSwedish)
                .withInterface(swedish)
                .withChart(swedish);

        assertEquals(swedish, chosen.interfaceLanguage());
        assertEquals(swedish, chosen.namesOnTheChart(),
                "a pack nobody compiled in draws the sky");
        assertEquals(Map.of(SkyLanguageChoice.INTERFACE_KEY, swedish,
                        SkyLanguageChoice.CHART_KEY, swedish),
                chosen.toStore(),
                "and it persists like any other choice");

        // And the same tag is refused where it is not installed,
        // which is what makes the first half a measurement of
        // availability rather than of nothing.
        assertThrows(IllegalArgumentException.class,
                () -> SkyLanguageChoice.read(Map.of(), INSTALLED)
                        .withInterface(swedish),
                "while an installation without Swedish cannot offer it");
    }

    /**
     * An invalid choice cannot be built, so it cannot be stored.
     *
     * <p>The store was validated on the way in and not on the way
     * out. Reading nonsense fell back safely, and then any caller
     * could hand {@code withInterface} a path fragment and
     * {@code toStore} would write it - so the malformed-value test
     * above passed while the door beside it stood open.
     */
    @Test
    void anUnavailableChoiceCannotBeCreatedOrPersisted() {
        SkyLanguageChoice held = SkyLanguageChoice.read(Map.of(), INSTALLED);

        for (String refused : new String[] {"de", "la", "nb_NO",
                "../../etc/passwd", "", "sv-SE"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> held.withInterface(refused),
                    "the interface cannot be set to \"" + refused + "\"");
        }
        for (String refused : new String[] {"de", "la", "follow",
                "../../etc/passwd", "sv-SE"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> held.withChart(refused),
                    "nor the chart to \"" + refused + "\"");
        }

        assertEquals(Map.of(SkyLanguageChoice.INTERFACE_KEY, "en",
                        SkyLanguageChoice.CHART_KEY, "follow-interface"),
                held.toStore(),
                "and after all of that the choice is unchanged, so"
                        + " nothing invalid could reach a store");
    }

    /** A pack may not claim to be one of the two chart modes. */
    @Test
    void aPackCannotCallItselfAModeTheAtlasOwns() {
        for (String mode : SkyLanguageChoice.CHART_MODES) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SkyLanguageChoice.Available(
                            Set.of(SkyLanguageChoice.ENGLISH),
                            Set.of(mode)),
                    "\"" + mode + "\" is the atlas's own behaviour,"
                            + " not a language a contributor supplies");
        }
        assertThrows(IllegalArgumentException.class,
                () -> new SkyLanguageChoice.Available(Set.of(NORWEGIAN),
                        Set.of()),
                "and an installation always has the English the other"
                        + " languages fall back to");
    }

    /**
     * Following asks for a language the sky can actually be drawn in.
     *
     * <p>The two sets are separate because neither implies the
     * other. An installation may carry a Swedish interface and no
     * Swedish constellation pack; following it would then have named
     * a language nothing answers to, and the reader would have got a
     * tag where a sky should be. It falls back to the Latin names
     * every build carries.
     */
    @Test
    void followingFallsBackToLatinWhenNoChartPackAnswers() {
        String swedish = "sv-SE";
        SkyLanguageChoice.Available interfaceOnly =
                new SkyLanguageChoice.Available(
                        Set.of(SkyLanguageChoice.ENGLISH, swedish),
                        Set.of(NORWEGIAN));

        SkyLanguageChoice following = SkyLanguageChoice.read(Map.of(
                        SkyLanguageChoice.INTERFACE_KEY, swedish,
                        SkyLanguageChoice.CHART_KEY, SkyLanguageChoice.FOLLOW),
                interfaceOnly);

        assertEquals(swedish, following.interfaceLanguage(),
                "the interface is Swedish, which is installed");
        assertEquals(SkyLanguageChoice.FOLLOW, following.chartLanguage(),
                "and the reader did ask the chart to follow it");
        assertEquals(SkyLanguageChoice.LATIN, following.namesOnTheChart(),
                "but there is no Swedish sky to follow it into, so the"
                        + " chart draws the Latin names rather than a"
                        + " tag nothing answers to");

        // And the same choice on an installation that HAS the pack
        // follows it - so the assertion above measures the missing
        // pack rather than some general refusal to follow.
        SkyLanguageChoice.Available both =
                new SkyLanguageChoice.Available(
                        Set.of(SkyLanguageChoice.ENGLISH, swedish),
                        Set.of(NORWEGIAN, swedish));
        assertEquals(swedish, SkyLanguageChoice.read(Map.of(
                        SkyLanguageChoice.INTERFACE_KEY, swedish,
                        SkyLanguageChoice.CHART_KEY,
                        SkyLanguageChoice.FOLLOW), both)
                .namesOnTheChart(),
                "where the pack exists, following reaches it");
    }

    /**
     * A sky may be offered in a language the interface does not speak.
     *
     * <p>The converse, and the independence the reader was promised:
     * English controls with a Norwegian sky is a reader learning the
     * Norwegian names, not a misconfiguration to be corrected.
     */
    @Test
    void aChartPackIsSelectableWithoutAnInterfaceInThatLanguage() {
        SkyLanguageChoice.Available chartOnly =
                new SkyLanguageChoice.Available(
                        Set.of(SkyLanguageChoice.ENGLISH),
                        Set.of(NORWEGIAN));

        SkyLanguageChoice reader = SkyLanguageChoice
                .read(Map.of(), chartOnly)
                .withChart(NORWEGIAN);

        assertEquals(SkyLanguageChoice.ENGLISH, reader.interfaceLanguage(),
                "the controls speak the only language installed");
        assertEquals(NORWEGIAN, reader.namesOnTheChart(),
                "while the sky is Norwegian, which is the independence"
                        + " the reader was promised");
        assertThrows(IllegalArgumentException.class,
                () -> reader.withInterface(NORWEGIAN),
                "and a chart pack does not make the interface"
                        + " available - neither set implies the other");
    }
}
