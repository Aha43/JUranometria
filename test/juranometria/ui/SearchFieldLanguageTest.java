package juranometria.ui;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Search speaks the reader's language and spells the catalogue's
 * identities exactly as the catalogue does (Sprint 33, issue #350).
 *
 * <p>The defect this surface carried was the worst instance of one the
 * sprint kept finding. A failure's explanation was built as
 *
 * <pre>
 *   "Nothing to choose: " + message.toLowerCase(ROOT)
 *           + ". Try another name, or coordinates."
 * </pre>
 *
 * <p>which takes a whole sentence, lowercases it by an English rule,
 * and splices it into another sentence as a clause. German would
 * capitalise the noun; Norwegian would not phrase the result that way
 * at all. Each message now owns a label and an explanation, written
 * whole, neither derived from the other.
 */
class SearchFieldLanguageTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB =
            InterfaceText.forLanguage("nb-NO");

    /** No sentence here is assembled from pieces of another. */
    @Test
    void searchBuildsNoSentenceFromFragments() throws Exception {
        String code = Files.readString(
                Path.of("src/juranometria/ui/SearchField.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");

        assertTrue(!code.contains("toLowerCase"),
                "no English casing rule is applied to a sentence a"
                        + " translation owns");
        assertTrue(!code.contains("\" and marks it\"")
                        && !code.contains("\"Nothing to choose: \""),
                "and no prose is concatenated around a value");
    }

    /** The field's two registers are written separately. */
    @Test
    void thePlaceholderAndTheSpokenNameAreIndependent() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the field is wired when it is built");
        for (InterfaceText said : List.of(EN, NB)) {
            inField(said, field -> {
                assertEquals(said.say("search.placeholder"),
                        field.getClientProperty(
                                "JTextField.placeholderText"),
                        "the placeholder is compact guidance inside a"
                                + " narrow box");
                assertEquals(said.say("search.a11y"),
                        field.getAccessibleContext().getAccessibleName(),
                        "and the spoken name states the whole purpose");
                assertNotEquals(field.getClientProperty(
                                "JTextField.placeholderText"),
                        field.getAccessibleContext().getAccessibleName(),
                        "two registers, neither derived from the other");
            });
        }
    }

    /**
     * The four examples are the catalogue's, not a translator's.
     *
     * <p>They are designations and a coordinate pair. A translation
     * handed a sentence with them already written into it would own a
     * copy of the catalogue's spelling, and the copy would be the one
     * that went stale.
     */
    @Test
    void theExamplesAreCanonicalAndIdenticalInBothLanguages()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the hover is wired when the field is built");
        for (InterfaceText said : List.of(EN, NB)) {
            inField(said, field -> {
                String hover = field.getToolTipText();
                for (String example : List.of("M 31", "NGC 224",
                        "TYC 2801-2090-1", "0:42:44 +41:16:09")) {
                    assertTrue(hover.contains(example),
                            example + " reaches a reader unchanged: "
                                    + hover);
                }
            });
        }
        assertNotEquals(
                EN.say("search.hover", "a", "b", "c", "d"),
                NB.say("search.hover", "a", "b", "c", "d"),
                "while the sentence around them is each language's own");
    }

    /** A result is named by the catalogue and explained by the language. */
    @Test
    void aResultKeepsItsIdentityAndGetsALanguageExplanation()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the result list is built when a query is handled");
        for (InterfaceText said : List.of(EN, NB)) {
            List<String> shown = new ArrayList<>();
            List<String> spoken = new ArrayList<>();
            inFieldAfter(said, "NGC", (field, popup) -> {
                for (JMenuItem item : itemsOf(popup)) {
                    shown.add(item.getText());
                    spoken.add(item.getAccessibleContext()
                            .getAccessibleDescription());
                }
            });
            assertTrue(shown.size() > 3,
                    "the premise: that query really does offer a"
                            + " choice - " + shown.size());
            assertTrue(shown.stream().anyMatch(s -> s.contains(" · ")),
                    "a result is named by its identity pair, which is"
                            + " canonical presentation: " + shown);
            String first = shown.get(0).split(" · ")[0];
            assertTrue(spoken.contains(
                            said.say("search.result.explain", first)),
                    "and explained by a whole pattern taking that"
                            + " canonical label: " + spoken.get(0));
        }
    }

    /** A query that finds nothing says so, and says what to do. */
    @Test
    void aQueryThatFindsNothingSaysSoInBothLanguages() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the message is built when a query is handled");
        for (InterfaceText said : List.of(EN, NB)) {
            inFieldAfter(said, "qqzzx", (field, popup) -> {
                List<JMenuItem> items = itemsOf(popup);
                assertEquals(1, items.size(),
                        "one item, and it is the message");
                JMenuItem item = items.get(0);
                assertEquals(said.say("search.nomatch.label"),
                        item.getText());
                assertEquals(said.say("search.nomatch.explain"),
                        item.getAccessibleContext()
                                .getAccessibleDescription());
                assertTrue(!item.isEnabled(),
                        "and it stays disabled: unlike the toolbar's"
                                + " version label, this really is"
                                + " something a reader cannot choose");
            });
        }
    }

    /**
     * A result outside the installed catalogue's coverage.
     *
     * <p>The shipped pack is all-sky, and {@code NO_FIT} is returned
     * only when no field width fits a result's position - which
     * cannot happen when every position fits at some rung. A regional
     * catalogue can still produce it, so it is reached here by
     * building one: a coverage cone around Orion, and a result in
     * Andromeda. The branch is genuinely entered rather than the
     * strings being asserted in isolation.
     */
    @Test
    void aResultOutsideCoverageSaysSoInBothLanguages() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the message is built when a query is handled");
        for (InterfaceText said : List.of(EN, NB)) {
            // A regional pack: a ten-degree cone around Orion, which
            // is what a catalogue that does not cover Andromeda looks
            // like. Built rather than mocked, so the branch is
            // entered by the same code the application runs.
            SceneAssembler regional = new SceneAssembler(
                    juranometria.catalog.TiledCatalogue.load(),
                    new SkyPosition(83.8, -5.4), 10.0, 0.5);
            List<JMenuItem> items = new ArrayList<>();
            inFieldAfter(said, "M 31", regional,
                    (field, popup) -> items.addAll(itemsOf(popup)));

            assertEquals(1, items.size(),
                    "the premise: a regional catalogue really does"
                            + " refuse this position - " + items);
            assertEquals(said.say("search.nofit.label"),
                    items.get(0).getText(),
                    "the label says it was found but not covered");
            assertEquals(said.say("search.nofit.explain"),
                    items.get(0).getAccessibleContext()
                            .getAccessibleDescription(),
                    "and the explanation says the chart did not move,"
                            + " rather than telling a reader to try"
                            + " another name for something already"
                            + " found");
        }
        assertNotEquals(EN.say("search.nofit.explain"),
                NB.say("search.nofit.explain"),
                "and neither language silently falls back to the"
                        + " other");
    }

    // ---- building the real field -------------------------------------

    private interface FieldCheck {
        void on(SearchField field);
    }

    private interface PopupCheck {
        void on(SearchField field, JPopupMenu popup);
    }

    private static void inField(InterfaceText said, FieldCheck check)
            throws Exception {
        inFieldAfter(said, null, Atlas.assembler(),
                (field, popup) -> check.on(field));
    }

    private static void inFieldAfter(InterfaceText said, String query,
                                     PopupCheck check) throws Exception {
        inFieldAfter(said, query, Atlas.assembler(), check);
    }

    private static void inFieldAfter(InterfaceText said, String query,
                                     SceneAssembler assembler,
                                     PopupCheck check) throws Exception {
        JFrame[] owner = new JFrame[1];
        SearchField[] field = new SearchField[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartViewController controller =
                        new ChartViewController(assembler::fits);
                field[0] = new SearchField(Atlas.search(), assembler,
                        controller, said);
                owner[0] = new JFrame("search");
                JPanel content = new JPanel(new java.awt.BorderLayout());
                content.add(field[0], java.awt.BorderLayout.NORTH);
                owner[0].setContentPane(content);
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            if (query != null) {
                SwingUtilities.invokeAndWait(() -> field[0].handle(query));
                SwingUtilities.invokeAndWait(() -> { });
            }
            SwingUtilities.invokeAndWait(() ->
                    check.on(field[0], popupOf(field[0])));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    /**
     * The popup, which is a window of its own.
     *
     * <p>Walking the field's children finds nothing: the result list
     * is not a child of the field, and an inventory that looked only
     * there would have reported the idle state as the whole surface.
     */
    private static JPopupMenu popupOf(SearchField field) {
        try {
            java.lang.reflect.Field popup =
                    SearchField.class.getDeclaredField("popup");
            popup.setAccessible(true);
            return (JPopupMenu) popup.get(field);
        } catch (ReflectiveOperationException unreachable) {
            throw new IllegalStateException(unreachable);
        }
    }

    private static List<JMenuItem> itemsOf(Container from) {
        List<JMenuItem> found = new ArrayList<>();
        if (from == null) {
            return found;
        }
        for (Component child : from.getComponents()) {
            if (child instanceof JMenuItem item) {
                found.add(item);
            }
            if (child instanceof Container nested
                    && !(child instanceof JMenuItem)) {
                found.addAll(itemsOf(nested));
            }
        }
        return found;
    }
}
