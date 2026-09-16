package juranometria.ui.language;

import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the store remembers, and what it refuses to (issue #348).
 *
 * <p>The contracts the gate settled, held against a real preference
 * node before anything in the interface can select a language. The
 * upgrade has to be invisible: a reader who has been using 2.0 and
 * never opens the setting must see the page they had, and their
 * silence must survive to be migrated rather than being answered on
 * their behalf at first launch.
 *
 * <p>Every test works in its own scratch node and removes it in a
 * {@code finally}, the shape {@code ChartOptionsStoreTest} already
 * keeps. Nothing here touches {@code juranometria}, the node the
 * reader's own settings live in; and a node merely cleared on the way
 * in would still outlive the run, which is the debt
 * {@code TestEvidenceScan} watches for and caught here.
 */
class SkyLanguageStoreTest {

    private static final String NORWEGIAN = "nb-NO";

    /** What a Norwegian-capable installation offers. */
    private static final SkyLanguageChoice.Available INSTALLED =
            new SkyLanguageChoice.Available(
                    Set.of(SkyLanguageChoice.ENGLISH),
                    Set.of(NORWEGIAN));

    /** A 2.0 store: other preferences, no language keys. */
    @Test
    void aTwoPointZeroStoreOpensToEnglishAndLatinNames()
            throws Exception {
        Preferences node = scratch();
        try {
            node.put("chart.equatorialGrid", "true");
            node.put("appearance", "light");
            SkyLanguageStore store = SkyLanguageStore.forNode(node);

            SkyLanguageChoice choice = store.choice(INSTALLED);
            assertEquals(SkyLanguageChoice.ENGLISH,
                    choice.interfaceLanguage(),
                    "the controls speak English, as they did");
            assertEquals(SkyLanguageChoice.LATIN, choice.namesOnTheChart(),
                    "and the chart draws the Latin names it always drew");
            assertFalse(store.everChosen(),
                    "nobody has chosen anything here");
        } finally {
            node.removeNode();
        }
    }

    /**
     * Reading writes nothing.
     *
     * <p>The contract that makes a future default change possible. If
     * a read stored a default, the reader's silence would have been
     * answered at first launch and no later migration could tell an
     * answered silence from a stated choice.
     */
    @Test
    void readingAnAbsentKeyLeavesItAbsent() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);

            store.choice(INSTALLED);
            store.stated();
            store.everChosen();
            store.choice(INSTALLED).namesOnTheChart();

            assertEquals(0, node.keys().length,
                    "after four reads the store is still empty: "
                            + java.util.Arrays.toString(node.keys()));
        } finally {
            node.removeNode();
        }
    }

    /** Saving writes both keys, never half a choice. */
    @Test
    void savingWritesTheWholeChoice() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);

            store.save(SkyLanguageChoice.read(Map.of(), INSTALLED)
                    .withChart(NORWEGIAN));

            assertEquals(Map.of(
                            SkyLanguageChoice.INTERFACE_KEY, "en",
                            SkyLanguageChoice.CHART_KEY, "nb-NO"),
                    store.stated(),
                    "choosing a chart language records the interface"
                            + " too, because leaving it absent would"
                            + " say the reader was never asked about"
                            + " something they have just settled");
            assertTrue(store.everChosen());
        } finally {
            node.removeNode();
        }
    }

    /** Absence and an explicit follow agree today and stay apart. */
    @Test
    void absenceAndExplicitFollowDrawTheSamePageAndRemainDistinct()
            throws Exception {
        Preferences silent = scratch();
        Preferences asked = scratch();
        try {
            SkyLanguageStore.forNode(asked).save(
                    SkyLanguageChoice.read(Map.of(), INSTALLED)
                            .withChart(SkyLanguageChoice.FOLLOW));

            SkyLanguageStore never = SkyLanguageStore.forNode(silent);
            SkyLanguageStore chose = SkyLanguageStore.forNode(asked);

            assertEquals(never.choice(INSTALLED).namesOnTheChart(),
                    chose.choice(INSTALLED).namesOnTheChart(),
                    "the same page either way, today");
            assertFalse(never.everChosen(), "but one was never asked");
            assertTrue(chose.everChosen(),
                    "and the other chose to follow - which is what a"
                            + " future default change would migrate"
                            + " differently");
        } finally {
            silent.removeNode();
            asked.removeNode();
        }
    }

    /** A chart language is selectable without an interface in it. */
    @Test
    void norwegianNamesDoNotImplyANorwegianInterface() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);

            store.save(SkyLanguageChoice.read(Map.of(), INSTALLED)
                    .withChart(NORWEGIAN));
            SkyLanguageChoice choice = store.choice(INSTALLED);

            assertEquals(NORWEGIAN, choice.namesOnTheChart(),
                    "the sky is Norwegian");
            assertEquals(SkyLanguageChoice.ENGLISH,
                    choice.interfaceLanguage(),
                    "while the controls speak the only interface"
                            + " language installed - the independence"
                            + " the reader was promised, not a"
                            + " misconfiguration");
        } finally {
            node.removeNode();
        }
    }

    /** A value this build cannot offer is replaced, not obeyed. */
    @Test
    void anUnavailableStoredValueFallsBackAndStaysDiagnosable()
            throws Exception {
        for (String nonsense : new String[] {"sv-SE", "de", "la",
                "nb_NO", "", "  ", "follow"}) {
            Preferences node = scratch();
            try {
                node.put(SkyLanguageChoice.INTERFACE_KEY, nonsense);
                node.put(SkyLanguageChoice.CHART_KEY, nonsense);
                SkyLanguageStore store = SkyLanguageStore.forNode(node);

                SkyLanguageChoice choice = store.choice(INSTALLED);
                assertEquals(SkyLanguageChoice.ENGLISH,
                        choice.interfaceLanguage(),
                        "\"" + nonsense + "\" is not an interface this"
                                + " build offers");
                assertEquals(SkyLanguageChoice.LATIN,
                        choice.namesOnTheChart(),
                        "and the chart falls back to Latin rather than"
                                + " to a tag nothing answers to");
                assertTrue(store.everChosen(),
                        "the store still records that somebody wrote"
                                + " something here, so the odd value is"
                                + " diagnosable rather than erased");
                assertEquals(nonsense, store.stated()
                                .get(SkyLanguageChoice.INTERFACE_KEY),
                        "and what they wrote is still readable: a store"
                                + " silently normalised on read could"
                                + " not be investigated");
            } finally {
                node.removeNode();
            }
        }
    }

    /**
     * A saved choice survives a restart.
     *
     * <p>Two stores over one node, with nothing carried between them
     * in memory, is what a second session sees.
     */
    @Test
    void aChoiceSurvivesTheSessionThatMadeIt() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore first = SkyLanguageStore.forNode(node);
            first.save(SkyLanguageChoice.read(Map.of(), INSTALLED)
                    .withChart(NORWEGIAN));
            first.flush();

            SkyLanguageStore later = SkyLanguageStore.forNode(
                    Preferences.userRoot()
                            .node(node.absolutePath().substring(1)));
            assertEquals(NORWEGIAN,
                    later.choice(INSTALLED).namesOnTheChart(),
                    "the sky a reader chose is the sky they get back");
        } finally {
            node.removeNode();
        }
    }

    /**
     * A node of this run's own, never the reader's.
     *
     * <p>Named by nanoTime, as the other store tests are, so a run
     * that died before its {@code finally} cannot leave state a later
     * one mistakes for its own.
     */
    private static Preferences scratch() {
        return Preferences.userRoot().node(
                "juranometria-test-sky-language-" + System.nanoTime());
    }
}
