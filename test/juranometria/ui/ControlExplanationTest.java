package juranometria.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.ChartKeys;
import juranometria.app.SwingSession;
import juranometria.tool.ControlExplanationStudyMain;
import juranometria.tool.ControlExplanationStudyMain.Control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That every control a reader can operate has been thought about
 * (Sprint 31, issue #311).
 *
 * <p>The issue asks for an audit and not a count, and the difference
 * is what this holds. A count of {@code setToolTipText} calls rises
 * when somebody adds a tooltip and says nothing at all about the
 * control nobody noticed; so the gate walks the surfaces the
 * application builds - the same walk the study writes its document
 * from, because two walkers would be two answers - and fails on a
 * control that carries no decision.
 *
 * <p>Three further rules, each of which a careful person breaks by
 * accident:
 *
 * <ul>
 *   <li>a description that is the tooltip read back tells a reader
 *       who cannot see the control nothing they did not already
 *       have;</li>
 *   <li>a tooltip naming a keystroke the application does not answer
 *       is a lie a reader finds by pressing it;</li>
 *   <li>and a tooltip long enough to explain something subtle is,
 *       unwrapped, a ribbon wider than the window.</li>
 * </ul>
 */
class ControlExplanationTest {

    private static final Path REPORT = Path.of(
            "docs/studies/control-explanations/measurements.md");

    @Test
    void everyOperableControlCarriesADecision() throws Exception {
        SwingSession.restoring(() -> {
            List<Control> controls = ControlExplanationStudyMain.audit();
            List<Control> undecided = controls.stream()
                    .filter(control -> "UNDECIDED".equals(control.how()))
                    .toList();
            assertEquals(List.of(), undecided,
                    "every control a reader can operate has been"
                            + " decided about - explained by a tooltip,"
                            + " by one that follows the state, or"
                            + " deliberately left to its own visible"
                            + " words. A new control with none is this"
                            + " test's whole reason for existing.");
            assertTrue(controls.size() > 80,
                    "and the walk reached the surfaces rather than"
                            + " quietly finding nothing: "
                            + controls.size());
        });
    }

    @Test
    void theDocumentSaysWhatTheWalkFound() throws Exception {
        SwingSession.restoring(() -> {
            List<Control> controls = ControlExplanationStudyMain.audit();
            String report = Files.readString(REPORT);
            long hovered = count(controls, "hovered");
            long dynamic = count(controls, "dynamic");
            long own = count(controls, "self-explanatory");
            for (String claim : List.of(
                    "**" + controls.size() + " operable controls**",
                    "**" + hovered + "** hovered",
                    "**" + dynamic + "** dynamic",
                    "**" + own + "** left to their own visible words",
                    "**0 undecided**")) {
                assertTrue(report.contains(claim),
                        "the audit states what the walk found: "
                                + claim);
            }
        });
    }

    @Test
    void nobodySaysTheSameWordsToBothAudiences() throws Exception {
        SwingSession.restoring(() -> {
            for (Control control : ControlExplanationStudyMain.audit()) {
                if (control.hovered() == null) {
                    continue;
                }
                assertFalse(control.hovered().equals(control.heard()),
                        control.surface() + " " + control.seen()
                                + ": the description is the tooltip"
                                + " read back, which is nothing to a"
                                + " reader who cannot see the tooltip");
                assertNotNull(control.heard(),
                        control.surface() + " " + control.seen()
                                + ": and there is a description at"
                                + " all");
            }
        });
    }

    @Test
    void aTooltipTooLongToReadOnOneLineIsWrapped() throws Exception {
        // Swing draws a tooltip as one line however long it is. The
        // sentences that explain the subtle controls are the long
        // ones, so those are exactly the tooltips that would run off
        // the side of the window - and further at enlarged text.
        SwingSession.restoring(() -> {
            for (Control control : ControlExplanationStudyMain.audit()) {
                String hovered = control.hovered();
                if (hovered == null
                        || hovered.length() <= Explain.WRAP_OVER) {
                    continue;
                }
                assertTrue(hovered.startsWith("<html>")
                                && hovered.contains("width: "
                                        + Explain.WRAP_WIDTH_PX + "px"),
                        control.surface() + " " + control.seen()
                                + ": a tooltip of " + hovered.length()
                                + " characters is laid out in a stated"
                                + " width, so it grows downwards"
                                + " rather than off the screen: "
                                + hovered);
            }
        });
    }

    @Test
    void noTooltipNamesAKeystrokeTheAtlasDoesNotAnswer() throws Exception {
        SwingSession.restoring(() -> {
            // What the registry says the atlas answers.
            List<String> bound = new java.util.ArrayList<>(
                    Shortcuts.all().stream().map(Shortcuts.Shortcut::text)
                            .toList());
            for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
                bound.add(toggle.sequence());
            }

            // Every parenthesised keystroke a reader is shown.
            java.util.regex.Pattern named = java.util.regex.Pattern
                    .compile("\\(([^()]*"
                            + java.util.regex.Pattern.quote(
                                    Shortcuts.menuModifierText())
                            + "[^()]*)\\)");
            int found = 0;
            for (Control control : ControlExplanationStudyMain.audit()) {
                for (String text : new String[] {control.hovered(),
                        control.heard()}) {
                    if (text == null) {
                        continue;
                    }
                    var matcher = named.matcher(text);
                    while (matcher.find()) {
                        String claimed = matcher.group(1).trim();
                        found++;
                        assertTrue(bound.contains(claimed),
                                control.surface() + " " + control.seen()
                                        + " promises " + claimed
                                        + ", which the atlas does not"
                                        + " bind. Bound: " + bound);
                    }
                }
            }
            assertTrue(found >= 20,
                    "and the surfaces really do quote keys, so this is"
                            + " evidence rather than a search that"
                            + " matched nothing: " + found);
        });
    }

    @Test
    void everyShortcutTheRegistryNamesIsOneTheMenuActuallyBinds() {
        JMenuBar bar = AppMenuBar.create(new ChartViewController(),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { });
        for (Shortcuts.Shortcut shortcut : Shortcuts.all()) {
            assertTrue(accelerators(bar).contains(shortcut.stroke()),
                    shortcut.label() + " is named by the registry, so"
                            + " the menu answers it: " + shortcut.text()
                            + " among " + accelerators(bar));
        }
    }

    @Test
    void theSeamRefusesTheThreeWaysToSayNothing() {
        JButton button = new JButton("Cancel");
        button.getAccessibleContext().setAccessibleName("Cancel");
        assertThrows(IllegalArgumentException.class, () ->
                        Explain.control(button, "Close this window",
                                "Close this window"),
                "the description may not be the tooltip read back");
        assertThrows(IllegalArgumentException.class, () ->
                        Explain.control(button, "Close this window",
                                "Cancel"),
                "nor the control's own name heard twice");
        assertThrows(IllegalArgumentException.class, () ->
                        Explain.control(button, "Close this window", ""),
                "and a control with nothing to say is not explained");
    }

    @Test
    void aControlThatHasGoneGreySaysWhyAndNotJustWhatItWouldDo()
            throws Exception {
        // The case the issue names and the easiest one to leave out.
        // A control that goes grey and says nothing leaves a reader
        // to guess whether the atlas is broken or they have simply
        // not done the thing it waits for.
        SwingSession.restoring(() -> {
            ChartComponent chart = new ChartComponent(
                    juranometria.app.Atlas.assembler());
            chart.setSize(900, 700);
            chart.setViewState(
                    juranometria.chart.ChartViewState.DEFAULT);
            ChartModuleHost host = new ChartModuleHost(chart,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            javax.swing.JComponent panel = host.attach(
                    new juranometria.ui.onthispage.OnThisPageModule())
                    .panel();

            JButton centre = button(panel, "Center here");
            JButton clear = button(panel, "Clear marks");
            assertNotNull(centre, "the panel has its Center here");
            assertNotNull(clear, "and its Clear marks");
            assertFalse(centre.isEnabled(),
                    "nothing is marked, so it is grey");
            assertFalse(clear.isEnabled(), "and so is the other");

            assertTrue(centre.getToolTipText().contains("first"),
                    "and it says what has to happen before it works: "
                            + centre.getToolTipText());
            String heardCentre = centre.getAccessibleContext()
                    .getAccessibleDescription();
            assertTrue(heardCentre.startsWith("Unavailable"),
                    "in a sentence that begins with the fact a reader"
                            + " who cannot see the grey has no other"
                            + " way of learning: " + heardCentre);
            String heardClear = clear.getAccessibleContext()
                    .getAccessibleDescription();
            assertTrue(heardClear.startsWith("Unavailable"),
                    "and the same for the other: " + heardClear);
            assertFalse(heardClear.equals(clear.getToolTipText()),
                    "each in its own words");

            // And it changes back. A sentence that stayed on
            // "unavailable" after the reader marked something would
            // be worse than none.
            host.workingSelection().add("M 31");
            assertTrue(clear.isEnabled(), "a mark makes it pressable");
            assertFalse(clear.getAccessibleContext()
                            .getAccessibleDescription()
                            .startsWith("Unavailable"),
                    "and it stops saying it is not: "
                            + clear.getAccessibleContext()
                                    .getAccessibleDescription());
        });
    }

    @Test
    void aZoomAtTheEndOfItsLadderSaysThereIsNoFurther() throws Exception {
        SwingSession.restoring(() -> {
            ChartViewController navigation = new ChartViewController();
            AtlasToolbar toolbar = new AtlasToolbar(navigation,
                    new SearchField(juranometria.app.Atlas.search(),
                            juranometria.app.Atlas.assembler(),
                            navigation), null, "0.0.0", () -> { });
            JButton zoomIn = button(toolbar, "Zoom in");
            assertNotNull(zoomIn, "the toolbar has its zoom in");
            while (navigation.canZoomIn()) {
                navigation.zoomIn();
            }
            assertFalse(zoomIn.isEnabled(),
                    "the narrowest field the atlas draws");
            assertTrue(zoomIn.getAccessibleContext()
                            .getAccessibleDescription()
                            .contains("unavailable"),
                    "and the control says so rather than going quiet: "
                            + zoomIn.getAccessibleContext()
                                    .getAccessibleDescription());
        });
    }

    private static JButton button(java.awt.Container root, String text) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JButton found
                    && (text.equals(found.getText())
                            || text.equals(found.getAccessibleContext()
                                    .getAccessibleName()))) {
                return found;
            }
            if (child instanceof java.awt.Container inside) {
                JButton found = button(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static long count(List<Control> controls, String how) {
        return controls.stream()
                .filter(control -> how.equals(control.how())).count();
    }

    private static List<KeyStroke> accelerators(JMenuBar bar) {
        List<KeyStroke> strokes = new java.util.ArrayList<>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            for (int item = 0; item < menu.getItemCount(); item++) {
                JMenuItem found = menu.getItem(item);
                if (found != null && found.getAccelerator() != null) {
                    strokes.add(found.getAccelerator());
                }
                if (found instanceof JCheckBoxMenuItem checked
                        && checked.getAccelerator() != null) {
                    strokes.add(checked.getAccelerator());
                }
            }
        }
        return strokes;
    }
}
