package juranometria.app;

import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import juranometria.geo.SkyNames;
import juranometria.ui.language.InterfaceLanguages;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageChoices;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The language controls in Settings (Sprint 33, issue #348).
 *
 * <p>Two selectors, populated from two registries, for two settings a
 * reader chooses separately. The dialog's content is built headless
 * here, as the appearance half already is, so what each control says
 * and what OK does with it can be asserted without a window.
 *
 * <p>The contract that needed the most care is <strong>Follow
 * interface</strong>. With one interface language installed it draws
 * exactly the page Latin draws, and hiding it would have been
 * tempting. It stays, because a reader whose stored choice is
 * {@code follow-interface} has asked for a living relationship to
 * their interface language: a selector that could not show it would
 * turn that into a fixed Latin the moment they opened Settings and
 * pressed OK, and nothing would have reported the change.
 */
class SettingsLanguageTest {

    private static final String NORWEGIAN = "nb-NO";

    /** Both selectors, listing what their own registry offers. */
    @Test
    void eachSelectorIsPopulatedFromItsOwnRegistry() {
        List<SkyLanguageChoices.Item> interfaces =
                SkyLanguageChoices.forTheInterface(
                        InterfaceLanguages.discover());
        List<SkyLanguageChoices.Item> charts =
                SkyLanguageChoices.forTheChart(SkyNames.discover(),
                        Atlas.languages(), SkyLanguageChoice.ENGLISH);

        assertEquals(List.of("en"), interfaces.stream()
                        .map(SkyLanguageChoices.Item::token).toList(),
                "the interface selector lists the descriptors that"
                        + " ship - one today, and still a selector,"
                        + " because a control fed by the real registry"
                        + " proves the registry reaches the interface"
                        + " where a hard-coded label would not");
        assertEquals(List.of("follow-interface", "latin", NORWEGIAN),
                charts.stream().map(SkyLanguageChoices.Item::token)
                        .toList(),
                "and the chart selector lists the two modes the atlas"
                        + " owns, then the packs that ship");
        assertFalse(interfaces.stream()
                        .anyMatch(i -> i.token().equals(NORWEGIAN)),
                "a chart pack is not an interface language");
    }

    /** Follow says what it currently draws. */
    @Test
    void followSaysWhatItCurrentlyResolvesTo() {
        SkyLanguageChoices.Item follow = SkyLanguageChoices.forTheChart(
                SkyNames.discover(), Atlas.languages(),
                SkyLanguageChoice.ENGLISH).get(0);

        assertEquals("Follow interface — currently Latin (IAU)",
                follow.label(),
                "with only an English interface installed, following"
                        + " it draws Latin - said on the item, because"
                        + " two choices that look identical and are"
                        + " not deserve an explanation rather than a"
                        + " mystery");
        assertEquals(SkyLanguageChoice.FOLLOW, follow.token(),
                "while what gets stored is still the relationship,"
                        + " not the language it currently resolves to");
    }

    /**
     * And it would say something else on a Norwegian installation.
     *
     * <p>The parenthetical is computed, not written down. Asserted
     * against a build that offers a Norwegian interface, so the claim
     * that it follows the installation is tested rather than assumed
     * from a build where it cannot vary.
     */
    @Test
    void followWouldNameTheInterfaceLanguageWhenOneIsInstalled() {
        SkyLanguageChoice.Available bilingual =
                new SkyLanguageChoice.Available(
                        java.util.Set.of("en", NORWEGIAN),
                        java.util.Set.of(NORWEGIAN));

        SkyLanguageChoices.Item follow = SkyLanguageChoices.forTheChart(
                SkyNames.discover(), bilingual, NORWEGIAN).get(0);

        assertEquals("Follow interface — currently Norsk bokmål",
                follow.label(),
                "the item reads differently on a differently"
                        + " installed build");
        assertEquals(SkyLanguageChoice.FOLLOW, follow.token(),
                "with the stored token unchanged, which is the whole"
                        + " point of following something");
    }

    /** Opening and cancelling writes nothing. */
    @Test
    void openingAndCancellingWritesNothing() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, Atlas.languages());

            // Built, looked at, and abandoned: the confirm callback is
            // the only path that writes, and Cancel does not take it.
            JComponent content = SettingsDialog.content(false, false,
                    new SettingsDialog.Languages(session.current(),
                            session.available(), SkyNames.discover(),
                            InterfaceLanguages.discover()),
                    confirmed -> { });
            chartBox(content).setSelectedIndex(2);

            assertEquals(Map.of(), store.stated(),
                    "nothing was written - and in particular the"
                            + " reader is still recorded as never"
                            + " having been asked, which is the state"
                            + " a future default change migrates");
            assertFalse(store.everChosen());
        } finally {
            node.removeNode();
        }
    }

    /**
     * Confirming an explicit Follow persists Follow.
     *
     * <p>The accidental migration this design exists to prevent. A
     * reader who chose to follow their interface opens Settings,
     * changes nothing, and presses OK; what must not happen is their
     * relationship being silently rewritten as the Latin it draws.
     */
    @Test
    void confirmingAnUnchangedFollowPersistsFollowNotLatin()
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            store.save(SkyLanguageChoice.read(Map.of(), Atlas.languages())
                    .withChart(SkyLanguageChoice.FOLLOW));
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, Atlas.languages());
            assertEquals(SkyLanguageChoice.LATIN,
                    session.namesOnTheChart(),
                    "the premise: following draws Latin today, so"
                            + " nothing on screen distinguishes the"
                            + " two");

            confirm(session);

            assertEquals(SkyLanguageChoice.FOLLOW,
                    store.stated().get(SkyLanguageChoice.CHART_KEY),
                    "and confirming keeps the relationship. Storing"
                            + " latin here would silently convert a"
                            + " living choice into a fixed one, and"
                            + " the reader would never see it happen");
        } finally {
            node.removeNode();
        }
    }

    /** Choosing Norwegian names updates, saves both keys, rebuilds. */
    @Test
    void choosingNorwegianNamesSavesBothKeysAndRebuildsThePage()
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, Atlas.languages());
            List<String> rebuilt = new java.util.ArrayList<>();
            session.onChange(c -> rebuilt.add(c.namesOnTheChart()));

            JComponent content = SettingsDialog.content(false, false,
                    new SettingsDialog.Languages(session.current(),
                            session.available(), SkyNames.discover(),
                            InterfaceLanguages.discover()),
                    confirmed -> session.choose(confirmed.language()));
            select(chartBox(content), NORWEGIAN);
            confirming(content, session);

            assertEquals(NORWEGIAN, session.namesOnTheChart(),
                    "the session shows it");
            assertEquals(Map.of(SkyLanguageChoice.INTERFACE_KEY, "en",
                            SkyLanguageChoice.CHART_KEY, NORWEGIAN),
                    store.stated(), "both keys are written");
            assertEquals(List.of(NORWEGIAN), rebuilt,
                    "and the page was rebuilt once, in the new"
                            + " language");
        } finally {
            node.removeNode();
        }
    }

    /** Norwegian names under an English interface is a valid state. */
    @Test
    void norwegianNamesRemainValidUnderAnEnglishInterface()
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, Atlas.languages());

            JComponent content = SettingsDialog.content(false, false,
                    new SettingsDialog.Languages(session.current(),
                            session.available(), SkyNames.discover(),
                            InterfaceLanguages.discover()),
                    confirmed -> session.choose(confirmed.language()));
            select(chartBox(content), NORWEGIAN);
            confirming(content, session);

            assertEquals(NORWEGIAN, session.namesOnTheChart());
            assertEquals(SkyLanguageChoice.ENGLISH,
                    session.interfaceLanguage(),
                    "the sky is Norwegian and the menus are English -"
                        + " the independence the reader was promised,"
                        + " not a half-finished translation");
        } finally {
            node.removeNode();
        }
    }

    /**
     * Each control says which language it is about.
     *
     * <p>"Language" alone would leave a reader who cannot see the
     * layout guessing which of the two they are about to change, and
     * the two do different things.
     */
    @Test
    void bothControlsExplainWhichLanguageTheyGovern() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageSession session = SkyLanguageSession.begin(
                    SkyLanguageStore.forNode(node), Atlas.languages());
            JComponent content = SettingsDialog.content(false, false,
                    new SettingsDialog.Languages(session.current(),
                            session.available(), SkyNames.discover(),
                            InterfaceLanguages.discover()),
                    confirmed -> { });

            JComboBox<?> interfaceBox = boxes(content).get(0);
            JComboBox<?> chartBox = boxes(content).get(1);

            assertEquals("Interface language", interfaceBox
                    .getAccessibleContext().getAccessibleName());
            assertEquals("Names on chart", chartBox
                    .getAccessibleContext().getAccessibleName());

            String saidOfInterface = spoken(interfaceBox);
            assertTrue(saidOfInterface.contains("menus")
                            && saidOfInterface.contains("does not"
                                    + " change the names printed on"
                                    + " the chart"),
                    "the interface control says it is not the chart: "
                            + saidOfInterface);
            String saidOfChart = spoken(chartBox);
            assertTrue(saidOfChart.contains("chart")
                            && saidOfChart.contains("Separate from the"
                                    + " interface language"),
                    "and the chart control says it is not the"
                            + " interface: " + saidOfChart);

            // And each has a visible caption bound to it, so the name
            // is not only heard.
            assertTrue(captions(content).containsAll(
                            List.of("Interface language",
                                    "Names on chart")),
                    "both captions are on screen: "
                            + captions(content));
        } finally {
            node.removeNode();
        }
    }

    // ---- driving the dialog ----------------------------------------

    private static void confirm(SkyLanguageSession session) {
        JComponent content = SettingsDialog.content(false, false,
                new SettingsDialog.Languages(session.current(),
                        session.available(), SkyNames.discover(),
                        InterfaceLanguages.discover()),
                confirmed -> session.choose(confirmed.language()));
        confirming(content, session);
    }

    /**
     * What OK would confirm, taken from the dialog's own seam.
     *
     * <p>Not a synthetic click. What each selector means is visible
     * here in tokens, which is what these tests are about; that the
     * OK button reaches this seam is a different claim, asserted by
     * a reader pressing the real button in a real window
     * (PublicFaceJourneyTest). Driving the button here would prove
     * both at once and would grow a back-door count that is allowed
     * to shrink and not to grow - and a bound that bends when it is
     * inconvenient is not a bound.
     */
    private static void confirming(JComponent content,
                                   SkyLanguageSession session) {
        session.choose(SettingsDialog.settled(content).language());
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

    private static JComboBox<?> chartBox(JComponent content) {
        return boxes(content).get(1);
    }

    private static List<JComboBox<?>> boxes(JComponent content) {
        List<JComboBox<?>> found = new java.util.ArrayList<>();
        for (JComboBox<?> box : find(content, JComboBox.class)) {
            found.add(box);
        }
        return found;
    }

    private static List<String> captions(JComponent content) {
        List<String> said = new java.util.ArrayList<>();
        for (JLabel label : find(content, JLabel.class)) {
            said.add(label.getText());
        }
        return said;
    }

    private static String spoken(JComponent control) {
        String description = control.getAccessibleContext()
                .getAccessibleDescription();
        return description == null ? "" : description;
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> List<T> find(JComponent root,
                                                       Class<T> kind) {
        List<T> found = new java.util.ArrayList<>();
        if (kind.isInstance(root)) {
            found.add((T) root);
        }
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent component) {
                found.addAll(find(component, kind));
            }
        }
        return found;
    }

    private static Preferences scratch() {
        return Preferences.userRoot().node(
                "juranometria-test-sky-language-" + System.nanoTime());
    }
}
