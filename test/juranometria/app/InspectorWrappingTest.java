package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.ui.WrappedText;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An explanation keeps its ending in either language
 * (Sprint 33, issue #350).
 *
 * <p>The Inspector's width was settled against English, and English is
 * short: "No catalogued object within reach of that point." measures
 * 297 px in the panel's own font. The Norwegian sentence saying the
 * same thing measures 400. It ran off the edge, and what a reader lost
 * was not decoration - the sentence stopped before it said what was
 * missing.
 *
 * <p>The repair is to break the sentence, not to shorten the
 * translation to fit an English measurement. Owner ruling, 2026-09-18:
 * there is ample vertical room, so the explanation wraps. The general
 * hint above it is left as it is - it behaves as a hint, and is
 * truncated in both languages rather than in one.
 *
 * <p>What is held here is both halves: that the breaking is correct,
 * and that the sentence it is applied to genuinely did not fit. A wrap
 * test over a sentence that always fitted would pass for ever and read
 * exactly like a repair.
 */
class InspectorWrappingTest {

    /** The sentence the panel could not finish. */
    private static final String KEY = "inspector.emptysky.fact";

    /**
     * The premise: this really is a sentence English hides.
     *
     * <p>Measured in the font a fact is drawn in, against the width a
     * fact is given. If a change ever makes both fit, this test says
     * so here rather than letting the wrap tests below quietly stop
     * exercising anything.
     */
    @Test
    void theNorwegianSentenceOverrunsAWidthEnglishFits() {
        FontMetrics metrics = factMetrics();
        int english = metrics.stringWidth(said("en"));
        int norsk = metrics.stringWidth(said("nb-NO"));

        assertTrue(norsk > english + 60,
                "the translation is substantially longer than the"
                        + " English the width was settled against: "
                        + english + " px against " + norsk);
        assertTrue(norsk > 320,
                "and longer than the panel is wide, which is what"
                        + " truncated it: " + norsk + " px");
    }

    /** A break falls between words, and never inside one. */
    @Test
    void everyLineFitsAndNoWordIsCut() {
        FontMetrics metrics = factMetrics();
        for (String language : List.of("en", "nb-NO")) {
            String whole = said(language);
            String broken = WrappedText.html(whole, 296, metrics);
            List<String> tooWide = new ArrayList<>();
            for (String line : linesOf(broken)) {
                if (metrics.stringWidth(line) > 296) {
                    tooWide.add(line + " (" + metrics.stringWidth(line)
                            + " px)");
                }
            }
            assertEquals(List.of(), tooWide,
                    "no line of the " + language + " sentence is wider"
                            + " than the space it was measured for");
            assertEquals(whole, String.join(" ", linesOf(broken)),
                    "and the lines rejoin into the sentence they came"
                            + " from, word for word, in " + language);
        }
    }

    /**
     * A break is a space, not nothing.
     *
     * <p>Chart Options learned this the expensive way: its companion
     * report deleted {@code <br>} rather than replacing it, and
     * "angitt i katalogen" was published as "angitt ikatalogen". The
     * same welding is possible here, so the same boundary is held.
     */
    @Test
    void theWordsOnEitherSideOfABreakStayApart() {
        FontMetrics metrics = factMetrics();
        String broken = WrappedText.html(said("nb-NO"), 296, metrics);

        assertTrue(broken.contains("<br>"),
                "the premise: the Norwegian sentence does break at"
                        + " this width - " + broken);
        assertTrue(!broken.replace("<br>", " ").contains("  "),
                "and the break replaces a space rather than adding a"
                        + " second one: " + broken);
        assertTrue(String.join(" ", linesOf(broken))
                        .contains("innenfor rekkevidde"),
                "so the words spanning the break are still two words: "
                        + broken);
    }

    /**
     * The panel answers with the sentence, not with its markup.
     *
     * <p>Line breaks belong to a width. A test, an inventory or a
     * translation review asking what the panel says is asking about
     * the words, and a screen reader must not inherit a layout's
     * decisions either.
     */
    @Test
    void theSpokenAndReportedTextIsTheWholeSentence() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the panel has to be laid out at a real width for"
                        + " wrapping to mean anything");
        for (String language : List.of("en", "nb-NO")) {
            inEmptySky(language, (panel, labels) -> {
                assertTrue(panel.lines().contains(said(language)),
                        "lines() reports the whole sentence in "
                                + language + ": " + panel.lines());
                List<String> spoken = new ArrayList<>();
                for (JLabel label : labels) {
                    String name = label.getAccessibleContext()
                            .getAccessibleName();
                    if (name != null) {
                        spoken.add(name);
                    }
                }
                assertTrue(spoken.contains(said(language)),
                        "and speaks it whole in " + language + ": "
                                + spoken);
                for (String name : spoken) {
                    assertTrue(!name.contains("<"),
                            "with no markup in the spoken channel: "
                                    + name);
                }
            });
        }
    }

    /**
     * Laid out for real, the explanation fits inside the panel.
     *
     * <p>The measurement that matters, and the one a headless test
     * cannot make: {@code validate()} is a no-op on a component that
     * is not displayable, so a panel measured without a frame reports
     * a width nothing will honour.
     */
    @Test
    void theExplanationFitsThePanelItIsDrawnIn() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a width only exists once the panel has one");
        inEmptySky("nb-NO", (panel, labels) -> {
            List<JLabel> broken = labels.stream()
                    .filter(l -> l.getText().startsWith("<html>"))
                    .toList();
            assertEquals(1, broken.size(),
                    "the explanation is the one fact that wraps;"
                            + " designations and coordinates are short"
                            + " and fixed and would read as two values"
                            + " if broken");
            JLabel explanation = broken.get(0);
            assertTrue(explanation.getText().contains("<br>"),
                    "and it really did break: "
                            + explanation.getText());
            assertTrue(explanation.getPreferredSize().width
                            <= explanation.getParent().getWidth(),
                    "so it fits the column it is drawn in - "
                            + explanation.getPreferredSize().width
                            + " px inside "
                            + explanation.getParent().getWidth());
        });
    }

    /** What that key says in a language stated explicitly. */
    private static String said(String language) {
        return InterfaceText.forLanguage(language).say(KEY);
    }

    /** The font a fact is drawn in, measured without a frame. */
    private static FontMetrics factMetrics() {
        JLabel fact = new JLabel("x");
        return fact.getFontMetrics(fact.getFont());
    }

    /** The lines of a wrapped label, markup removed. */
    private static List<String> linesOf(String html) {
        String body = html.replace("<html>", "").replace("</html>", "");
        return List.of(body.split("<br>"));
    }

    /** Runs a check over the empty-sky state in a real frame. */
    private static void inEmptySky(String language, Check check)
            throws Exception {
        JFrame[] owner = new JFrame[1];
        InspectorPanel[] panel = new InspectorPanel[1];
        try {
            ChartViewState where = new ChartViewState(
                    new SkyPosition(83.8, 0.0), 42.0,
                    ChartViewState.defaultMagnitudeFor(42.0));
            ChartScene scene = Atlas.assemblerNamedIn("latin")
                    .assemble(where, 900, 700);
            SelectionModel selection = new SelectionModel();
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("wrap");
                panel[0] = new InspectorPanel(selection, () -> scene,
                        () -> ChartOptions.DEFAULTS, chosen -> { },
                        InterfaceText.forLanguage(language));
                owner[0].setContentPane(panel[0]);
                selection.selectEmptySky(new SkyPosition(84.0, -1.5));
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            List<JLabel> labels = new ArrayList<>();
            collect(panel[0], labels);
            check.on(panel[0], labels);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    private static void collect(Container from, List<JLabel> found) {
        for (Component child : from.getComponents()) {
            if (child instanceof JLabel label) {
                found.add(label);
            }
            if (child instanceof Container nested) {
                collect(nested, found);
            }
        }
    }

    /** A check made against a laid-out panel. */
    private interface Check {
        void on(InspectorPanel panel, List<JLabel> labels);
    }
}
