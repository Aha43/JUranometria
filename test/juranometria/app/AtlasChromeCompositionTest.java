package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.SelectionMode;
import juranometria.ui.ChartViewController;
import juranometria.ui.InspectorToggle;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application hands its session language to the controls a reader
 * actually touches (Sprint 33, issue #350).
 *
 * <p>This test exists because everything else was green while the
 * application was wrong. The toolbar had been externalised,
 * translated, and proved by eight passing contracts - and a reader who
 * chose Norwegian still saw an English toolbar, because
 * {@code JUranometriaMain} called a constructor that quietly defaulted
 * to English.
 *
 * <p>Not one component contract could have seen it. Every one of them
 * passes a language explicitly, which is exactly what makes them good
 * component tests: they ask <em>can this surface speak Norwegian?</em>
 * The question nobody was asking is <em>does the application hand it
 * Norwegian?</em>, and that question needs the composition, not the
 * component.
 *
 * <p>So this starts where the application starts - a stored language
 * choice - and goes through the same seam. A source grep would not
 * have caught the original defect either: it was valid Java calling a
 * real constructor.
 */
class AtlasChromeCompositionTest {

    /** A Norwegian session reaches both surfaces. */
    @Test
    void aNorwegianSessionGivesBothControlsNorwegian() throws Exception {
        List<String> said = new ArrayList<>();
        inSession("nb-NO", controls -> {
            said.addAll(wordsOf(controls.toolbar()));
            said.addAll(wordsOf(controls.searchField()));
        });

        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        InterfaceText english = InterfaceText.forLanguage("en");

        // One string from each surface, chosen because the two
        // languages really do differ there - asserted below, so this
        // cannot pass by the values happening to coincide.
        String barSays = norsk.say("toolbar.zoomIn.a11y");
        String fieldSays = norsk.say("search.a11y");
        assertTrue(!barSays.equals(english.say("toolbar.zoomIn.a11y")),
                "the premise: the toolbar's word differs by language");
        assertTrue(!fieldSays.equals(english.say("search.a11y")),
                "and so does the field's");

        assertTrue(said.contains(barSays),
                "the toolbar speaks the session's language: expected \""
                        + barSays + "\" among " + said);
        assertTrue(said.contains(fieldSays),
                "and so does the search field: expected \"" + fieldSays
                        + "\" among " + said);
        assertTrue(!said.contains(english.say("toolbar.zoomIn.a11y")),
                "with no English left on either - which is the whole"
                        + " defect: externalised, translated, proved by"
                        + " component tests, and composed in English");
    }

    /**
     * A Norwegian session reaches the menu bar too.
     *
     * <p>The menu was externalised after the toolbar, and the toolbar
     * taught the lesson: a surface can be translated, proved by its
     * own contracts, and still handed English by the application. So
     * this goes through the seam the application uses, from a stored
     * choice, and reads the bar it produces.
     */
    @Test
    void aNorwegianSessionGivesTheMenuNorwegian() throws Exception {
        List<String> said = new ArrayList<>();
        inSession("nb-NO", controls -> said.addAll(menuWordsOf(
                controls.menuBar(new ChartViewController(), () -> { },
                        () -> { }, () -> { }, () -> { }, () -> { },
                        () -> { }, () -> { }))));

        // Now load-bearing. While the Norwegian menu values did not
        // exist every key fell back to English, so a Norwegian
        // session's menu was byte-identical to an English one and
        // this could not fail whatever the seam did. That was
        // recorded as owed rather than reported as proof; the values
        // have landed, and the debt is discharged here.
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        InterfaceText english = InterfaceText.forLanguage("en");
        String fileMenu = norsk.say("menu.file.label");

        assertTrue(!fileMenu.equals(english.say("menu.file.label")),
                "the premise: the File menu's name differs by"
                        + " language - " + fileMenu);
        assertTrue(said.contains(fileMenu),
                "the composed menu speaks the session's language:"
                        + " expected \"" + fileMenu + "\" among " + said);
        assertTrue(!said.contains(english.say("menu.file.label")),
                "with no English menu left on it");
        assertEquals(menuWordsOf(
                        AppMenuBar.create(new ChartViewController(),
                                () -> { }, () -> { }, () -> { }, () -> { },
                                () -> { }, () -> { }, () -> { }, norsk)),
                said,
                "and word for word what that language says");
    }

    /** An English session still gets English, through the same seam. */
    @Test
    void anEnglishSessionGivesBothControlsEnglish() throws Exception {
        List<String> said = new ArrayList<>();
        inSession("en", controls -> {
            said.addAll(wordsOf(controls.toolbar()));
            said.addAll(wordsOf(controls.searchField()));
        });
        InterfaceText english = InterfaceText.forLanguage("en");
        assertTrue(said.contains(english.say("toolbar.zoomIn.a11y"))
                        && said.contains(english.say("search.a11y")),
                "the seam is not hard-wired to Norwegian either: "
                        + said);
    }

    /** The seam refuses to build controls with no session at all. */
    @Test
    void thereIsNoWayToBuildTheseControlsWithoutALanguage() {
        IllegalArgumentException refused = org.junit.jupiter.api.Assertions
                .assertThrows(IllegalArgumentException.class,
                        () -> AtlasChrome.of(null,
                                new ChartViewController(), Atlas.search(),
                                Atlas.assembler(), new InspectorToggle(),
                                "0.0.0", () -> { }, new SelectionMode()));
        assertTrue(refused.getMessage().contains("language"),
                "and says why: " + refused.getMessage());
    }

    // ---- the application's own starting point -------------------------

    private interface Check {
        void on(AtlasChrome controls);
    }

    /**
     * Builds the controls from a stored choice, as startup does.
     *
     * <p>A real preference node, because the session's whole job is to
     * read one once; a fixture that answered from memory would prove
     * the seam works when handed an answer, which is not the claim.
     */
    private static void inSession(String language, Check check)
            throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-composition-" + System.nanoTime());
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            store.save(store.choice(Atlas.languages())
                    .withInterface(language));
            SkyLanguageSession session = SkyLanguageSession.begin(store,
                    Atlas.languages());
            assertEquals(language, session.interfaceLanguage(),
                    "the premise: the session really carries the stored"
                            + " choice");
            AtlasChrome[] controls = new AtlasChrome[1];
            SwingUtilities.invokeAndWait(() -> controls[0] =
                    AtlasChrome.of(session, new ChartViewController(),
                            Atlas.search(), Atlas.assembler(),
                            new InspectorToggle(), "0.0.0", () -> { },
                            new SelectionMode()));
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> check.on(controls[0]));
        } finally {
            node.removeNode();
        }
    }

    /**
     * Every word a menu says.
     *
     * <p>A menu keeps its items in a popup rather than as children,
     * so walking the bar's component tree finds the menus and none of
     * their items.
     */
    private static List<String> menuWordsOf(javax.swing.JMenuBar bar) {
        List<String> said = new ArrayList<>();
        for (int m = 0; m < bar.getMenuCount(); m++) {
            javax.swing.JMenu menu = bar.getMenu(m);
            if (menu == null) {
                continue;
            }
            add(said, menu.getText());
            add(said, menu.getAccessibleContext().getAccessibleName());
            add(said, menu.getAccessibleContext().getAccessibleDescription());
            for (int i = 0; i < menu.getItemCount(); i++) {
                javax.swing.JMenuItem item = menu.getItem(i);
                if (item == null) {
                    continue;
                }
                add(said, item.getText());
                add(said, item.getAccessibleContext().getAccessibleName());
                add(said, item.getAccessibleContext()
                        .getAccessibleDescription());
            }
        }
        return said;
    }

    /** Every word a surface says, on every channel. */
    private static List<String> wordsOf(Container from) {
        List<String> said = new ArrayList<>();
        if (from instanceof JComponent self) {
            record(self, said);
        }
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                record(widget, said);
            }
            if (child instanceof Container nested) {
                said.addAll(wordsOf(nested));
            }
        }
        return said;
    }

    private static void record(JComponent widget, List<String> said) {
        if (widget instanceof AbstractButton button) {
            add(said, button.getText());
        }
        if (widget instanceof JLabel label) {
            add(said, label.getText());
        }
        add(said, widget.getToolTipText());
        if (widget.getAccessibleContext() != null) {
            add(said, widget.getAccessibleContext().getAccessibleName());
            add(said, widget.getAccessibleContext()
                    .getAccessibleDescription());
        }
    }

    private static void add(List<String> said, String text) {
        if (text != null && !text.isBlank()) {
            said.add(text.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim());
        }
    }
}
