package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.SwingSession;
import juranometria.app.UiTheme;
import juranometria.chart.SelectionMode;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reader resting the pointer on things, in a real window
 * (Sprint 31, issue #311).
 *
 * <p>The audit reads what a control was <em>given</em> to say. This
 * reads what a reader is actually <em>shown</em>, which is a
 * different question: Swing's own tooltip manager asks a component
 * {@code getToolTipText(event)} at the point the pointer is over, and
 * a control that is never laid out, never shown, or answers only from
 * a field would pass an audit and show a reader nothing.
 *
 * <p>Four kinds of control, because they fail differently: an
 * icon-only toolbar button with no words at all, a field whose
 * expected input is the explanation, a checkbox in a dialog that
 * quotes a keyboard route, and the compact control on the tab strip
 * that appears only when the titles do not fit.
 */
class ControlExplanationJourneyTest {

    @Test
    void aReaderRestsThePointerOnTheControlsAndIsTold() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a tooltip is read off a shown window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            JFrame[] window = new JFrame[1];
            SwingSession.guarded(() -> {
                ChartViewController navigation = new ChartViewController(
                        Atlas.assembler()::fits);
                SearchField search = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                AtlasToolbar[] toolbar = new AtlasToolbar[1];
                SwingUtilities.invokeAndWait(() -> {
                    toolbar[0] = new AtlasToolbar(navigation, search,
                            new InspectorToggle(), "0.0.0", () -> { },
                            new SelectionMode());
                    JFrame frame = new JFrame("control explanations");
                    frame.setLayout(new BorderLayout());
                    frame.add(toolbar[0], BorderLayout.NORTH);
                    frame.setSize(900, 200);
                    window[0] = frame;
                    frame.setVisible(true);
                });
                flush();

                // 1. An icon with no words on it at all. Whatever it
                // says is the only thing a reader has.
                JButton zoomIn = button(toolbar[0], "Zoom in");
                assertNotNull(zoomIn, "the toolbar has a zoom-in button");
                String hovered = ReaderInput.hover(zoomIn);
                assertNotNull(hovered,
                        "an icon-only control tells a reader who rests"
                                + " the pointer on it what it does");
                assertTrue(hovered.contains("Zoom in"),
                        "and says what it does: " + hovered);
                assertTrue(hovered.contains(Shortcuts.text(
                                Shortcuts.ZOOM_IN)),
                        "with the keys that do the same thing, in this"
                                + " platform's own words: " + hovered);

                // 2. A field, where the explanation is what to type.
                String field = ReaderInput.hover(search);
                assertNotNull(field, "the search field explains itself");
                assertTrue(field.contains("M 31"),
                        "with an example of what it takes, which is"
                                + " what a reader about to type needs: "
                                + field);

                // 3. A dialog's checkbox, quoting the keyboard route
                // to the same switch - the promise #312 made and this
                // issue keeps.
                ChartOptionsController options = new ChartOptionsController(
                        new ChartOptionsStore() {
                            @Override
                            public ChartOptions load() {
                                return ChartOptions.DEFAULTS;
                            }

                            @Override
                            public void save(ChartOptions next) {
                            }
                        });
                SwingUtilities.invokeAndWait(() ->
                        ChartOptionsDialog.open(window[0], options));
                flush();
                JDialog dialog = dialogTitled("Chart Options");
                assertNotNull(dialog, "the reader's own dialog opens");
                try {
                    JCheckBox box = onEdt(() ->
                            checkBox(dialog, "Deep-sky objects"));
                    assertNotNull(box, "with the switch on it");
                    String said = ReaderInput.hover(box);
                    assertNotNull(said,
                            "a checkbox whose label names a layer says"
                                    + " what the layer is");
                    assertTrue(said.contains(juranometria.app.ChartKeys
                                    .toggle("chart.deepSkyObjects")
                                    .sequence()),
                            "and names the keys that reach the same"
                                    + " switch from the chart: " + said);

                    // 4. The compact control a reader cannot guess:
                    // the tab strip's overflow button, which carries
                    // no words of its own in any window width.
                    JButton overflow = onEdt(() ->
                            tabStripButton(dialog));
                    assertNotNull(overflow,
                            "the tab strip has its own control");
                    String strip = onEdt(overflow::getToolTipText);
                    assertNotNull(strip,
                            "which says what it is for, since nothing"
                                    + " on it does");
                    assertFalse(strip.equals(onEdt(() -> overflow
                                    .getAccessibleContext()
                                    .getAccessibleDescription())),
                            "in its own words rather than the"
                                    + " description read back");

                    // And the one surface that deliberately says
                    // nothing on hover: OK needs no tooltip, because
                    // the word on it is the whole of it.
                    JButton ok = onEdt(() -> button(dialog, "OK"));
                    assertNotNull(ok, "the dialog has its OK");
                    assertNull(ReaderInput.hover(ok),
                            "a control whose own word is the whole"
                                    + " meaning shows no box over it");
                    assertNotNull(onEdt(() -> ok.getAccessibleContext()
                                    .getAccessibleDescription()),
                            "and still says what it does to a reader"
                                    + " who cannot see the word");
                } finally {
                    SwingUtilities.invokeAndWait(dialog::dispose);
                    flush();
                }
            }, () -> SwingUtilities.invokeAndWait(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (window[0] != null) {
                    window[0].dispose();
                }
            }));
        });
    }

    private static JButton button(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton found
                    && (name.equals(found.getText())
                            || name.equals(found.getAccessibleContext()
                                    .getAccessibleName()))) {
                return found;
            }
            if (child instanceof Container inside) {
                JButton found = button(inside, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JCheckBox checkBox(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox found
                    && text.equals(found.getText())) {
                return found;
            }
            if (child instanceof Container inside) {
                JCheckBox found = checkBox(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** The tab strip's own control, whatever the look and feel calls it. */
    private static JButton tabStripButton(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton found
                    && found.getClass().getName().contains("Tabs")) {
                return found;
            }
            if (child instanceof Container inside) {
                JButton found = tabStripButton(inside);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JDialog dialogTitled(String title) throws Exception {
        JDialog[] found = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Window open : Window.getWindows()) {
                if (open instanceof JDialog dialog && dialog.isVisible()
                        && title.equals(dialog.getTitle())) {
                    found[0] = dialog;
                }
            }
        });
        return found[0];
    }

    private static <T> T onEdt(Callable<T> ask) throws Exception {
        Object[] answer = new Object[1];
        Exception[] trouble = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                answer[0] = ask.call();
            } catch (Exception failure) {
                trouble[0] = failure;
            }
        });
        if (trouble[0] != null) {
            throw trouble[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) answer[0];
        return typed;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
