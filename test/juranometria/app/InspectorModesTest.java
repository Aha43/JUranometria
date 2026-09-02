package juranometria.app;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Inspector's two modes (Sprint 24, issue #216).
 *
 * <p>The panel gains a second thing to show and must lose nothing:
 * the same close button, the same Escape, the same requested
 * visibility, the same behaviour when the window narrows, and the
 * same accessible naming. A reader who never opens the new mode
 * should not be able to tell it is there.
 *
 * <p>It is also given the view rather than building it, so the
 * Inspector never learns what a table is - the module boundary,
 * asserted from the other side.
 */
class InspectorModesTest {

    private record Fixture(InspectorPanel panel, SelectionModel model,
                           ChartScene scene, List<Selection> centred) {
    }

    private static Fixture inspector() throws Exception {
        ChartScene scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);
        SelectionModel model = new SelectionModel();
        List<Selection> centred = new ArrayList<>();
        InspectorPanel[] panel = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new InspectorPanel(
                model, () -> scene,
                () -> juranometria.render.ChartOptions.DEFAULTS,
                centred::add));
        return new Fixture(panel[0], model, scene, centred);
    }

    /** Something to show, standing in for a module's own view. */
    private static JPanel someModuleView() {
        JPanel view = new JPanel();
        view.add(new JLabel("a module's view"));
        return view;
    }

    @Test
    void withoutAModuleTheresNoChooserAndNothingHasChanged()
            throws Exception {
        Fixture fixture = inspector();

        assertFalse(fixture.panel().modeChooserShown(),
                "one mode needs no chooser: a reader with no module"
                        + " installed sees the Inspector they had");
        assertEquals(InspectorPanel.SELECTED_MODE, fixture.panel().mode());
    }

    @Test
    void installingAModulesViewOffersTheSecondMode() throws Exception {
        Fixture fixture = inspector();
        SwingUtilities.invokeAndWait(() ->
                fixture.panel().showPageView(someModuleView()));

        assertTrue(fixture.panel().modeChooserShown(),
                "the chooser appears when there is something to choose");
        assertEquals("On this page", fixture.panel().pageModeButton()
                        .getAccessibleContext().getAccessibleName(),
                "and it is named for a reader who cannot see it");
    }

    @Test
    void goingToThePageAndBackFindsTheSameObjectStillNamed()
            throws Exception {
        Fixture fixture = inspector();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().showPageView(someModuleView());
            fixture.model().select(new Selection.Object(
                    Selection.Object.Kind.DEEP_SKY, "NGC 224",
                    fixture.scene().viewport().centre()));
        });
        String named = headingOf(fixture.panel());

        SwingUtilities.invokeAndWait(() ->
                fixture.panel().showMode(InspectorPanel.PAGE_MODE));
        assertEquals("On this page", headingOf(fixture.panel()),
                "the heading says which mode a reader is in");

        SwingUtilities.invokeAndWait(() ->
                fixture.panel().showMode(InspectorPanel.SELECTED_MODE));
        assertEquals(named, headingOf(fixture.panel()),
                "and coming back finds the object they were reading"
                        + " about still named above its facts");
    }

    @Test
    void theCloseButtonAndTheRequestedVisibilitySurviveTheSecondMode()
            throws Exception {
        Fixture fixture = inspector();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().showPageView(someModuleView());
            fixture.panel().setRequestedVisible(true);
            fixture.panel().showMode(InspectorPanel.PAGE_MODE);
        });
        assertTrue(fixture.panel().isRequestedVisible());

        SwingUtilities.invokeAndWait(() ->
                fixture.panel().closeButton().doClick());

        assertFalse(fixture.panel().isRequestedVisible(),
                "the close button still closes the panel, whichever"
                        + " mode is showing");
    }

    @Test
    void theNarrowWindowRuleIsUnchangedByTheSecondMode() throws Exception {
        Fixture fixture = inspector();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().showPageView(someModuleView());
            fixture.panel().setRequestedVisible(true);
            fixture.panel().showMode(InspectorPanel.PAGE_MODE);
            fixture.panel().setAvailableWidth(560);
        });

        assertFalse(InspectorPanel.fitsBeside(560),
                "a 560 px window cannot hold both, as before");
        assertFalse(fixture.panel().canShow(),
                "so the chart keeps the page and the panel yields -"
                        + " the second mode does not buy the panel more"
                        + " room");
    }

    @Test
    void theInspectorNeverLearnsWhatItIsShowing() throws Exception {
        // The boundary from the panel's side: it is handed a
        // component. A panel that imported the module would make the
        // module unremovable, whatever the module said about itself.
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/juranometria/app/InspectorPanel.java"));
        String code = source.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
        assertFalse(code.contains("onthispage"),
                "the Inspector is given a view, and never asks what it"
                        + " is");
    }

    private static String headingOf(InspectorPanel panel) throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() ->
                said[0] = firstLabel(panel));
        return said[0];
    }

    /** The heading is the first label in the panel's own header. */
    private static String firstLabel(java.awt.Container root) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JLabel label) {
                return label.getText();
            }
            if (child instanceof java.awt.Container container) {
                String found = firstLabel(container);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
