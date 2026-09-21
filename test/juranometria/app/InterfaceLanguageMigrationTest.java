package juranometria.app;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import juranometria.geo.SkyNames;
import juranometria.ui.language.InterfaceLanguages;
import juranometria.ui.language.InterfaceRestartNotice;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageChoices;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upgrading from 2.0.0 (Sprint 33, release 2.1.0).
 *
 * <p>A reader who has been using 2.0.0 has preferences - an
 * appearance, chart switches, a place - and <strong>no language keys
 * at all</strong>, because neither setting existed. Everything this
 * release adds has to arrive around that silence without answering it
 * for them.
 *
 * <p>A fresh install cannot stand in for this. An empty node and a
 * 2.0.0 node both lack {@code language.interface}, and only one of
 * them can tell you whether the absence survived contact with a
 * reader who opened Settings, looked at it, and pressed OK. So the
 * premise here is deliberately un-meetable by a fresh install: the
 * node must already carry 2.0.0's own preferences, and the test
 * asserts that before it asserts anything else.
 *
 * <p>Why the absence is worth keeping rather than tidying away:
 * {@code SkyLanguageStore} already refuses to write a default when it
 * <em>reads</em>, in its own words - "the moment a read writes a
 * default, the reader's silence has been answered for them and can
 * never be migrated". A confirmation that wrote {@code en} would
 * answer it just as finally, and pin a reader to English by a click
 * they did not experience as a choice.
 */
class InterfaceLanguageMigrationTest {

    /** What 2.0.0 wrote, and what it never heard of. */
    private static final List<String> TWO_ZERO_KEYS = List.of(
            "appearance", "chart.deepSkyObjects", "chart.starNames",
            "chart.equatorialGrid");

    /**
     * A reader upgrades, opens Settings, and changes nothing.
     *
     * <p>The principal release journey, through the real
     * confirmation seam - the dialog's own {@code settled(...)} -
     * because that is where consent is decided. Driving the session
     * directly would prove the store's rule while leaving the
     * dialog free to answer for the reader.
     *
     * <p>2.0.0 had <strong>neither</strong> language key: the
     * settings did not exist. So there is no chart preference to
     * preserve, and the claim is the one that can actually be made -
     * both questions stay unanswered, the sky goes on being named
     * the way 2.0.0 named it, and every preference that release did
     * write is still there.
     */
    @Test
    void confirmingSettingsUnchangedLeavesATwoZeroReaderAsTheyWere() {
        Preferences node = twoZeroNode();
        try {
            // The premise, un-meetable by a fresh install.
            assertTrue(storedKeys(node).containsAll(TWO_ZERO_KEYS),
                    "this node is shaped like 2.0.0: it carries the"
                            + " preferences that release wrote. A"
                            + " fresh install carries none of them,"
                            + " and would pass everything below"
                            + " without proving any of it");
            assertNull(node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "and neither language key, because 2.0.0 had"
                            + " neither setting");
            assertNull(node.get(SkyLanguageChoice.CHART_KEY, null));

            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());

            assertEquals(SkyLanguageChoice.ENGLISH,
                    session.current().interfaceLanguage(),
                    "the application starts in English, resolved from"
                            + " an absent key rather than a stored"
                            + " one");
            assertEquals(SkyLanguageChoice.LATIN,
                    session.namesOnTheChart(),
                    "and the sky is named exactly as 2.0.0 named it");
            assertFalse(session.current().everChosen(),
                    "with neither question answered - which is what"
                            + " keeps both migratable");

            // The real seam: build the dialog, touch nothing, and
            // confirm it the way OK does.
            JComponent content = settingsFor(session);
            confirm(content, session);

            assertNull(node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "pressing OK without acting on a selector leaves"
                            + " the interface key ABSENT. Writing"
                            + " \"en\" here would turn a reader's"
                            + " silence into a decision they never"
                            + " made, and no later default could ever"
                            + " reach them");
            assertNull(node.get(SkyLanguageChoice.CHART_KEY, null),
                    "and the sky's question is equally unanswered");
            assertEquals(SkyLanguageChoice.LATIN,
                    session.namesOnTheChart(),
                    "the page is the one they had");

            assertTrue(storedKeys(node).containsAll(TWO_ZERO_KEYS),
                    "and every preference they already had is still"
                            + " there");
            assertEquals("dark", node.get("appearance", null),
                    "with the value they had, not a default");
            assertEquals("false", node.get("chart.starNames", null),
                    "including the chart switches they had turned"
                            + " off");
        } finally {
            remove(node);
        }
    }

    /**
     * Preserving absence must not disable saving.
     *
     * <p>The obvious way to keep the key absent is to stop writing
     * it, which would break the feature this release is for. So the
     * other half is asserted in the same node: an actual choice
     * persists, and reads back.
     */
    @Test
    void anExplicitChoiceStillPersistsNormally() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());

            session.choose(session.current().withInterface("nb-NO"));

            assertEquals("nb-NO",
                    node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "a reader who chooses Norwegian gets it written"
                            + " down");
            assertEquals("nb-NO",
                    SkyLanguageStore.forNode(node)
                            .choice(Atlas.languages())
                            .interfaceLanguage(),
                    "and a fresh store reads it back - which is what"
                            + " the next start does");

            // And back again: choosing English EXPLICITLY is a
            // choice, and must be written, unlike the silence above.
            session.choose(session.current()
                    .withInterface(SkyLanguageChoice.ENGLISH));
            assertEquals(SkyLanguageChoice.ENGLISH,
                    node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "choosing English on purpose is not the same as"
                            + " never having chosen, and is stored");
        } finally {
            remove(node);
        }
    }

    /**
     * The chart language may be chosen while the interface is not.
     *
     * <p>The two settings are independent, and that independence has
     * to survive persistence: a reader who picks Norwegian names for
     * the sky and leaves the application in whatever it opens in has
     * still not chosen an interface language.
     */
    @Test
    void choosingOnlyTheChartLanguageLeavesTheInterfaceUnchosen() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());

            session.choose(session.current().withChart("nb-NO"));

            assertEquals("nb-NO",
                    node.get(SkyLanguageChoice.CHART_KEY, null),
                    "the sky is named in Norwegian");
            assertNull(node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "and the application's own language is still"
                            + " unchosen - the two settings are"
                            + " independent, including in what they"
                            + " write down");
        } finally {
            remove(node);
        }
    }

    /**
     * The reader opens the interface selector and picks the English
     * already showing.
     *
     * <p>The ruling's awkward case, and the reason consent is taken
     * from the control's own event rather than from comparing values:
     * nothing a reader can see changes, so no comparison of before
     * and after could tell this apart from never touching the
     * selector. It is still a decision, and it is written down.
     *
     * <p>And because nothing visible changed, it must <strong>not</strong>
     * interrupt them with a restart notice. Persistence changed;
     * presentation did not.
     */
    @Test
    void choosingTheEnglishAlreadyShowingIsAChoiceAndSaysNothing() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());
            String was = session.current().interfaceLanguage();

            JComponent content = settingsFor(session);
            select(interfaceBox(content), SkyLanguageChoice.ENGLISH);
            confirm(content, session);

            assertEquals(SkyLanguageChoice.ENGLISH,
                    node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "choosing English on purpose settles the question"
                            + " and is written, even though English"
                            + " was already what they were reading");
            assertNull(InterfaceRestartNotice.forChoice(was,
                            session.current().interfaceLanguage()),
                    "and nothing interrupts them, because the"
                            + " language they are looking at did not"
                            + " change");
        } finally {
            remove(node);
        }
    }

    /** Choosing Norwegian writes it, and says so exactly once. */
    @Test
    void choosingNorwegianWritesItAndSaysSoOnce() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());
            String was = session.current().interfaceLanguage();

            JComponent content = settingsFor(session);
            select(interfaceBox(content), "nb-NO");
            confirm(content, session);

            assertEquals("nb-NO",
                    node.get(SkyLanguageChoice.INTERFACE_KEY, null));
            assertNotNull(InterfaceRestartNotice.forChoice(was,
                            session.current().interfaceLanguage()),
                    "and they are told once that a restart applies it"
                            + " throughout");
        } finally {
            remove(node);
        }
    }

    /** An explicit choice survives later unrelated confirmations. */
    @Test
    void anExplicitChoiceSurvivesLaterUnrelatedConfirmations() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());

            JComponent chosen = settingsFor(session);
            select(interfaceBox(chosen), "nb-NO");
            confirm(chosen, session);

            // A second visit, touching nothing.
            JComponent later = settingsFor(session);
            confirm(later, session);

            assertEquals("nb-NO",
                    node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "once settled, a question stays settled - the"
                            + " rule preserves absence, not values");
        } finally {
            remove(node);
        }
    }

    /** Reading Settings and cancelling write nothing at all. */
    @Test
    void readingAndCancellingNeverWrite() {
        Preferences node = twoZeroNode();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session = SkyLanguageSession.begin(
                    store, Atlas.languages());

            JComponent content = settingsFor(session);
            select(interfaceBox(content), "nb-NO");
            // No confirm: this is Cancel, which never reaches the
            // handler at all.

            assertNull(node.get(SkyLanguageChoice.INTERFACE_KEY, null),
                    "cancelling writes nothing, however the selector"
                            + " was left");
            assertTrue(storedKeys(node).containsAll(TWO_ZERO_KEYS),
                    "and disturbs nothing else");
        } finally {
            remove(node);
        }
    }

    /**
     * The aggregate is exactly the two per-key facts, or-ed.
     *
     * <p>It used to be stored beside them, which let the
     * constructor build a state the model says cannot exist. Derived
     * now, and pinned here in all four combinations so the
     * derivation cannot drift back into a field.
     */
    @Test
    void everChosenIsTheOrOfTheTwoQuestions() {
        SkyLanguageChoice neither =
                SkyLanguageChoice.read(java.util.Map.of(),
                        Atlas.languages());
        assertFalse(neither.everChosen(),
                "two absent keys: nobody has answered anything");

        assertTrue(neither.withInterface("nb-NO").everChosen(),
                "an interface choice alone makes it true");
        assertFalse(neither.withInterface("nb-NO").chartChosen(),
                "without settling the sky's question");

        assertTrue(neither.withChart("nb-NO").everChosen(),
                "and a chart choice alone makes it true");
        assertFalse(neither.withChart("nb-NO").interfaceChosen(),
                "without settling the application's");

        assertTrue(neither.withInterface("nb-NO").withChart("nb-NO")
                        .everChosen(),
                "and both, of course");
    }

    // ---- the Settings seam ----------------------------------------

    private static JComponent settingsFor(SkyLanguageSession session) {
        return SettingsDialog.content(false, false,
                new SettingsDialog.Languages(session.current(),
                        session.available(), SkyNames.discover(),
                        InterfaceLanguages.discover()),
                confirmed -> session.choose(confirmed.language()));
    }

    private static void confirm(JComponent content,
                                SkyLanguageSession session) {
        session.choose(SettingsDialog.settled(content).language());
    }

    private static JComboBox<?> interfaceBox(JComponent content) {
        return boxes(content).get(0);
    }

    private static List<JComboBox<?>> boxes(java.awt.Container from) {
        List<JComboBox<?>> found = new java.util.ArrayList<>();
        for (java.awt.Component child : from.getComponents()) {
            if (child instanceof JComboBox<?> box) {
                found.add(box);
            }
            if (child instanceof java.awt.Container nested) {
                found.addAll(boxes(nested));
            }
        }
        return found;
    }

    private static void select(JComboBox<?> box, String token) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (((SkyLanguageChoices.Item) box.getItemAt(i)).token()
                    .equals(token)) {
                box.setSelectedIndex(i);
                return;
            }
        }
        throw new IllegalStateException(token + " is not offered");
    }

    // ---- the node -------------------------------------------------

    /** A preference node shaped like one 2.0.0 left behind. */
    private static Preferences twoZeroNode() {
        Preferences node = Preferences.userRoot()
                .node("juranometria-migration-" + System.nanoTime());
        node.put("appearance", "dark");
        node.put("chart.deepSkyObjects", "true");
        node.put("chart.starNames", "false");
        node.put("chart.equatorialGrid", "true");
        return node;
    }

    private static List<String> storedKeys(Preferences node) {
        try {
            return Arrays.asList(node.keys());
        } catch (java.util.prefs.BackingStoreException cannotRead) {
            throw new IllegalStateException("the node cannot be read",
                    cannotRead);
        }
    }

    private static void remove(Preferences node) {
        try {
            node.removeNode();
        } catch (java.util.prefs.BackingStoreException ignored) {
            // A scratch node that outlives the test costs nothing.
        }
    }
}
