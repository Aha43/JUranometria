package juranometria.ui.onthispage;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.page.PageVisibility;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageVisibilityText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * On This Page speaks the reader's language, and says the catalogue's
 * words unchanged (Sprint 33, issue #350).
 *
 * <p>Four defects are held here, all of them found by drawing the
 * surface rather than by reading its source:
 *
 * <ul>
 *   <li>the column headers stayed English. {@code getColumnName} is
 *       called while the {@code JTable} field is being initialised -
 *       before the constructor has been given a language - so it
 *       answers with an empty string, and the constructor rebuilds
 *       the columns once it has one. Miss that rebuild and the table
 *       ships with four blank headers;
 *   <li>the count of unnamed stars carried a hard-coded {@code <br>}
 *       placed where English wanted one;
 *   <li>removing it exposed an older layout fault: a
 *       {@code BoxLayout} column aligns its children against each
 *       other, and this label at 0.0 beside a panel at the default
 *       0.5 was laid out 161 px wide at x=159 while asking for 230.
 *       The English break had been hiding it;
 *   <li>a row's order must follow {@link PageVisibility}, not the
 *       spelling of its translation.
 * </ul>
 */
class OnThisPageLanguageTest {

    private static final String NORWEGIAN = "nb-NO";

    /** Every column is named, in the language being read. */
    @Test
    void theColumnHeadersAreTranslatedAndNeverBlank() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the table builds its columns when it is realised");
        for (String language : List.of("en", NORWEGIAN)) {
            withPanel(language, panel -> {
                JTable table = tableIn(panel);
                List<String> names = new ArrayList<>();
                for (int c = 0; c < table.getColumnCount(); c++) {
                    names.add(table.getColumnName(c));
                }
                assertEquals(4, names.size(),
                        "four decided columns in " + language);
                assertTrue(names.stream().noneMatch(String::isBlank),
                        "none of them blank - a header asked for"
                                + " before the language arrived would"
                                + " be, and would stay that way: "
                                + names);
                InterfaceText said = InterfaceText.forLanguage(language);
                assertEquals(List.of(said.say("onthispage.column.object"),
                                said.say("onthispage.column.magnitude"),
                                said.say("onthispage.column.from"),
                                said.say("onthispage.column.chart")),
                        names,
                        "and each is the word this language uses");
            });
        }
        // The premise: the two languages really do differ here, so a
        // table that ignored the language could not pass both.
        assertNotEquals(
                InterfaceText.forLanguage("en").say("onthispage.column.chart"),
                InterfaceText.forLanguage(NORWEGIAN)
                        .say("onthispage.column.chart"));
    }

    /**
     * The catalogue's own words are the same in both languages.
     *
     * <p>Designations, star proper names, magnitudes and photometric
     * bands are data. {@code SkyNames} holds constellation names
     * only; a star's proper name comes from {@code StarIdentity} and
     * is canonical identity in this issue (owner ruling, 2026-09-18).
     */
    @Test
    void catalogueIdentitiesAreIdenticalInBothLanguages() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the rows come from a realised table");
        List<String> english = new ArrayList<>();
        List<String> norsk = new ArrayList<>();
        withPanel("en", panel -> english.addAll(identities(panel)));
        withPanel(NORWEGIAN, panel -> norsk.addAll(identities(panel)));

        assertTrue(english.size() > 8,
                "the premise: there are rows to compare - "
                        + english.size());
        assertEquals(english, norsk,
                "every object is named, ordered and spelled the same"
                        + " in both languages: a translation moves the"
                        + " chrome, never the catalogue");
    }

    /** Rows follow the enum's order, not the alphabet of a language. */
    @Test
    void theStatesSortByIdentityAndNotByTranslatedSpelling() {
        PageVisibilityText norsk = PageVisibilityText.in(
                InterfaceText.forLanguage(NORWEGIAN));
        List<String> inEnumOrder = new ArrayList<>();
        for (PageVisibility state : PageVisibility.values()) {
            inEnumOrder.add(norsk.label(state));
        }
        List<String> alphabetical = new ArrayList<>(inEnumOrder);
        java.util.Collections.sort(alphabetical);

        assertNotEquals(alphabetical, inEnumOrder,
                "the premise: Norwegian's spelling really would"
                        + " reorder these - " + inEnumOrder);
        assertEquals("Tegnet", inEnumOrder.get(0),
                "and drawn stays first, because the order runs from"
                        + " drawn to least drawn and means something");
    }

    /** The two registers are written, not derived from each other. */
    @Test
    void theShortWordAndTheWholeAnswerAreSeparatelyWritten() {
        for (String language : List.of("en", NORWEGIAN)) {
            PageVisibilityText words = PageVisibilityText.in(
                    InterfaceText.forLanguage(language));
            for (PageVisibility state : PageVisibility.values()) {
                String label = words.label(state);
                String explanation = words.explanation(state);
                assertTrue(!label.isBlank() && !explanation.isBlank(),
                        state + " has both registers in " + language);
                assertNotEquals(label, explanation,
                        state + " says the short word and the whole"
                                + " answer differently in " + language);

            }
        }
    }

    /**
     * Nothing the Norwegian surface says is English.
     *
     * <p>Written because two accessible names survived a whole pass
     * untranslated - {@code Centre here} and {@code Clear marks} were
     * set from literals beside the very buttons whose visible labels
     * came from resources. A reader never saw them: they went to the
     * spoken channel only, which is the half that goes unnoticed
     * longest, and only reading the generated companion caught it.
     *
     * <p>So the check walks every channel the sheet walks - label,
     * hover, spoken name, spoken description, column heading - and
     * holds the Norwegian surface against the English words it should
     * no longer contain. Catalogue identities are exempt by
     * construction: this compares CHROME, and a designation is not
     * chrome.
     */
    @Test
    void noEnglishPhraseSurvivesIntoTheNorwegianSurface() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the spoken channel exists only on a realised panel");
        InterfaceText english = InterfaceText.forLanguage("en");
        List<String> englishPhrases = new ArrayList<>();
        for (String key : List.of("onthispage.centre.label",
                "onthispage.centre.a11y", "onthispage.clear.label",
                "onthispage.clear.a11y", "onthispage.column.object",
                "onthispage.column.chart", "onthispage.column.from",
                "onthispage.table.a11y", "onthispage.heading",
                "onthispage.magnitude.none", "onthispage.empty",
                "onthispage.unnamed.a11y")) {
            englishPhrases.add(english.say(key));
        }

        List<String> offenders = new ArrayList<>();
        withPanel(NORWEGIAN, panel -> {
            List<String> spoken = chromeOf(panel);
            assertTrue(spoken.size() > 12,
                    "the premise: the surface really does say things"
                            + " through these channels - "
                            + spoken.size());
            for (String phrase : englishPhrases) {
                for (String said : spoken) {
                    if (said.equals(phrase)) {
                        offenders.add("\"" + phrase + "\"");
                    }
                }
            }
        });
        assertEquals(List.of(), offenders,
                "the Norwegian surface says no English phrase, on any"
                        + " channel - including the spoken one, where"
                        + " \"Centre here\" and \"Clear marks\""
                        + " hid through an entire translation pass");
    }

    /** The count of unnamed stars is laid out whole, not clipped. */
    @Test
    void theCountOfUnnamedStarsGetsTheWidthItAsksFor() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a width only exists once the panel has one");
        withPanel(NORWEGIAN, panel -> {
            JLabel counted = null;
            for (JLabel label : labelsIn(panel)) {
                if (panel.sentenceOf(label) != null
                        && label.isVisible()) {
                    counted = label;
                }
            }
            assertTrue(counted != null,
                    "the premise: this page has unnamed stars to"
                            + " count");
            assertTrue(counted.getWidth()
                            >= counted.getPreferredSize().width,
                    "the line is given at least the width it asks"
                            + " for - it was laid out "
                            + counted.getWidth() + " px wide while"
                            + " asking for "
                            + counted.getPreferredSize().width
                            + ", and lost its ending");
            assertEquals(0, counted.getX(),
                    "and starts at the column's left edge, rather"
                            + " than being pushed sideways by a"
                            + " sibling's alignment");
        });
    }

    // ---- building the real surface ----------------------------------

    private interface Check {
        void on(OnThisPageTable panel);
    }

    private static void withPanel(String language, Check check)
            throws Exception {
        JFrame[] owner = new JFrame[1];
        OnThisPageTable[] panel = new OnThisPageTable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart =
                        new ChartComponent(Atlas.assembler());
                chart.setSize(900, 700);
                chart.setViewState(ChartViewState.DEFAULT);
                ChartModuleHost host = new ChartModuleHost(chart,
                        new SelectionModel(), request -> { });
                panel[0] = host.attach(new OnThisPageModule(
                        InterfaceText.forLanguage(language))).panel();
                owner[0] = new JFrame("language");
                owner[0].setContentPane(panel[0]);
                panel[0].setPreferredSize(new java.awt.Dimension(320, 420));
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> check.on(panel[0]));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    /**
     * What the catalogue contributes to a row, and nothing else.
     *
     * <p>The magnitude CELL is deliberately not here. Where a source
     * recorded no magnitude the cell says so in words, and those
     * words are translated - the silence is prose wearing a number's
     * clothes, which is how it stayed English through two passes.
     * The recorded value beside it is data and is compared.
     */
    private static List<String> identities(OnThisPageTable panel) {
        List<String> found = new ArrayList<>();
        for (OnThisPageTable.Row row : panel.rows()) {
            found.add(row.identity() + " | " + row.name() + " | "
                    + row.from() + " | " + row.magnitudeValue());
        }
        return found;
    }

    /** Every channel the surface speaks through, catalogue aside. */
    private static List<String> chromeOf(Container from) {
        List<String> said = new ArrayList<>();
        if (from instanceof javax.swing.JComponent self) {
            sayings(self, said);
        }
        for (Component child : from.getComponents()) {
            if (child instanceof javax.swing.JComponent widget) {
                sayings(widget, said);
            }
            if (child instanceof Container nested) {
                said.addAll(chromeOf(nested));
            }
        }
        return said;
    }

    private static void sayings(javax.swing.JComponent widget,
                                List<String> said) {
        if (widget instanceof javax.swing.AbstractButton button) {
            add(said, button.getText());
        }
        if (widget instanceof JLabel label) {
            add(said, label.getText());
        }
        if (widget instanceof JTable table) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                add(said, table.getColumnName(c));
            }
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

    private static JTable tableIn(Container from) {
        for (Component child : from.getComponents()) {
            if (child instanceof JTable table) {
                return table;
            }
            if (child instanceof Container nested) {
                JTable found = tableIn(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<JLabel> labelsIn(Container from) {
        List<JLabel> found = new ArrayList<>();
        for (Component child : from.getComponents()) {
            if (child instanceof JLabel label) {
                found.add(label);
            }
            if (child instanceof Container nested) {
                found.addAll(labelsIn(nested));
            }
        }
        return found;
    }
}
