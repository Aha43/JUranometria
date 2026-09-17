package juranometria.tool;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.render.SymbolFamily;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.SymbolFamilyText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The companion report says the words the dialog says
 * (Sprint 33, issue #350).
 *
 * <p>The report is what a person reads when deciding whether a
 * translation is any good, so a report that garbles the words is
 * worse than a garbled dialog: the dialog would be caught by looking
 * at it, and the report is what looking at it is supposed to mean.
 *
 * <p>It was garbled. The dialog wraps long descriptions by putting
 * {@code <br>} between lines, and the extractor deleted tags rather
 * than replacing them, so every line boundary ate a space:
 *
 * <pre>
 *   angitt i katalogen  ->  angitt ikatalogen
 *   For eksempel: M 13  ->  Foreksempel: M13
 *   H II-regioner       ->  HII-regioner
 * </pre>
 *
 * <p>Caught by a reviewer reading it, not by any test - so the
 * boundaries are held here, in both languages, against the real
 * sentences rather than a fixture that happens to have no wrap in it.
 */
class ChartOptionsSheetTextTest {

    /** A wrap must not weld the words on either side of it. */
    @Test
    void aLineBreakLeavesTheWordsOnEitherSideApart() {
        assertEquals("angitt i katalogen",
                ChartOptionsSheetMain.plainText(
                        "<html>angitt i<br>katalogen</html>"),
                "a break between two words is a space, not nothing");
        assertEquals("For eksempel: M 13, M 22",
                ChartOptionsSheetMain.plainText(
                        "<html>For<br>eksempel: M<br>13, M 22</html>"),
                "including inside a catalogue designation, where the"
                        + " space is part of the identifier");
        assertEquals("H II-regioner",
                ChartOptionsSheetMain.plainText(
                        "<html>H<br>II-regioner</html>"));
    }

    /**
     * Every real description survives extraction, in both languages.
     *
     * <p>Wrapped at a width narrow enough to force breaks inside the
     * long ones, then compared word for word against the sentence
     * the dialog was given. A fixture with no wrap in it would prove
     * nothing about the thing that went wrong.
     */
    @Test
    void everyRealDescriptionSurvivesWrappingInBothLanguages() {
        List<String> damaged = new ArrayList<>();
        for (String language : List.of("en", "nb-NO")) {
            SymbolFamilyText words = SymbolFamilyText.in(
                    InterfaceText.forLanguage(language));
            for (SymbolFamily family : SymbolFamily.values()) {
                String whole = words.description(family);
                String extracted = ChartOptionsSheetMain.plainText(
                        wrappedLikeTheDialog(whole));
                if (!extracted.equals(whole)) {
                    damaged.add(language + " " + family + ":\n  was \""
                            + whole + "\"\n  got \"" + extracted + "\"");
                }
            }
        }
        assertEquals(List.of(), damaged,
                "a wrapped sentence extracts back to the sentence it"
                        + " was");
    }

    /** The premise: these sentences really do wrap. */
    @Test
    void theseSentencesAreLongEnoughToWrap() {
        int wrapped = 0;
        for (SymbolFamily family : SymbolFamily.values()) {
            String whole = SymbolFamilyText.in(
                            InterfaceText.forLanguage("nb-NO"))
                    .description(family);
            if (wrappedLikeTheDialog(whole).contains("<br>")) {
                wrapped++;
            }
        }
        assertTrue(wrapped >= 5,
                "every family description breaks at least once at this"
                        + " width, so the test above is exercising the"
                        + " failure it was written for: " + wrapped);
    }

    /**
     * The dialog's own wrapping shape.
     *
     * <p>Breaks on spaces into {@code <br>}-separated lines, which is
     * what {@code ChartOptionsDialog.wrapped} produces against font
     * metrics. Reproduced here by word count rather than by pixels so
     * the test needs no font, and because what is under test is the
     * EXTRACTION, not the measurement.
     */
    private static String wrappedLikeTheDialog(String prose) {
        StringBuilder html = new StringBuilder("<html>");
        String[] words = prose.split(" ");
        for (int i = 0; i < words.length; i++) {
            html.append(words[i]);
            if (i < words.length - 1) {
                html.append(i % 6 == 5 ? "<br>" : " ");
            }
        }
        return html.append("</html>").toString();
    }
}
