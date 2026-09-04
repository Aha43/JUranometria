package juranometria.app;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartOptionsDialogTest {

    @Test
    void checkboxesBindLivePreviewDependenciesAndTheProtocol() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            store.save(new ChartOptions(true, true, true, false, true));
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            int[] cancelled = new int[1];
            int[] confirmed = new int[1];
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> cancelled[0]++, () -> confirmed[0]++);

            // Controls display the persisted/current value honestly.
            assertTrue(box(content, "Deep-sky objects").isSelected());
            assertFalse(box(content, "Constellation boundaries").isSelected());
            assertTrue(box(content, "Deep-sky labels").isEnabled());

            // Every change previews live through the controller.
            box(content, "Constellation boundaries").doClick();
            assertTrue(controller.options().constellationBoundaries(),
                    "a click previews immediately");
            assertEquals(false, store.load().constellationBoundaries(),
                    "previewing persists nothing");

            // Dependency enablement: symbols off disables labels, which
            // remembers its state.
            box(content, "Deep-sky objects").doClick();
            assertFalse(box(content, "Deep-sky labels").isEnabled(),
                    "labels are effective only while symbols are on");
            assertTrue(box(content, "Deep-sky labels").isSelected(),
                    "the disabled checkbox remembers its state");
            assertFalse(controller.options().effectiveDeepSkyLabels());
            box(content, "Deep-sky objects").doClick();
            assertTrue(box(content, "Deep-sky labels").isEnabled());

            box(content, "Constellation figures").doClick();
            assertFalse(box(content, "Constellation names").isEnabled(),
                    "names are effective only while figures are on");

            // The equatorial grid: fourth Content control, no
            // dependency (pure view geometry), live preview.
            assertTrue(box(content, "Equatorial coordinate grid")
                            .isEnabled(),
                    "the grid control depends on nothing");
            box(content, "Equatorial coordinate grid").doClick();
            assertFalse(controller.options().equatorialGrid(),
                    "the grid toggle previews immediately");

            // The three identifier controls: independent by the
            // Sprint 17 decision, no dependency (star dots are never
            // optional), each previewing live and separately.
            for (String control : new String[] {"Star names",
                    "Bayer letters", "Flamsteed numbers"}) {
                assertTrue(box(content, control).isEnabled(),
                        control + " depends on nothing");
            }
            box(content, "Bayer letters").doClick();
            assertFalse(controller.options().bayerLetters(),
                    "the letter toggle previews immediately");
            assertTrue(controller.options().starNames(),
                    "and leaves the other identifier layers alone");
            assertTrue(controller.options().flamsteedNumbers());

            // Restore Defaults previews the released chart and re-enables
            // every dependent control.
            AboutDialogTest.button(content, "Restore Defaults").doClick();
            assertEquals(ChartOptions.DEFAULTS, controller.options());
            assertTrue(box(content, "Constellation names").isEnabled());
            assertTrue(box(content, "Constellation boundaries").isSelected());
            for (String control : new String[] {"Star names",
                    "Bayer letters", "Flamsteed numbers"}) {
                assertTrue(box(content, control).isSelected(),
                        "Restore Defaults includes " + control);
            }
            assertTrue(box(content, "Equatorial coordinate grid")
                            .isSelected(),
                    "Restore Defaults includes the grid option");

            // OK and Cancel run exactly their wired protocol actions.
            AboutDialogTest.button(content, "OK").doClick();
            assertEquals(1, confirmed[0]);
            AboutDialogTest.button(content, "Cancel").doClick();
            assertEquals(1, cancelled[0]);
        } finally {
            node.removeNode();
        }
    }

    @Test
    void blackSkyPreviewsLiveDependsOnNothingAndRestoresToPaper()
            throws Exception {
        // Sprint 26, issue #246: one persisted choice for the chart
        // ground on the Chart tab, previewing live like every other
        // control, reset by Restore Defaults, and touching no
        // application chrome - the look and feel before and after
        // the toggle is the same installed object.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });

            assertFalse(box(content, "Black sky").isSelected(),
                    "the released chart is white paper");
            assertTrue(box(content, "Black sky").isEnabled(),
                    "the ground depends on nothing");

            var lookAndFeel = javax.swing.UIManager.getLookAndFeel();
            box(content, "Black sky").doClick();
            assertEquals(juranometria.render.ChartPalette.BLACK_SKY,
                    controller.options().palette(),
                    "the ground previews immediately");
            assertEquals(juranometria.render.ChartPalette.WHITE_PAPER,
                    store.load().palette(),
                    "previewing persists nothing");
            assertTrue(lookAndFeel
                            == javax.swing.UIManager.getLookAndFeel(),
                    "a chart-ground change never alters application"
                            + " chrome");

            AboutDialogTest.button(content, "Restore Defaults").doClick();
            assertEquals(juranometria.render.ChartPalette.WHITE_PAPER,
                    controller.options().palette(),
                    "Restore Defaults returns to the released"
                            + " white-paper chart");
            assertFalse(box(content, "Black sky").isSelected(),
                    "and the control shows it");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theDialogIsSingleInstanceAndEscapeIsCancel() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "window behaviour needs a display; content is tested headless");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame("options-test");
                ChartOptionsDialog.open(frame[0], controller);
                ChartOptionsDialog.open(frame[0], controller);
            });
            flush();
            assertEquals(1, openDialogCount(),
                    "opening twice never multiplies the dialog");

            // Preview a change, then Escape: the revert protocol runs.
            JDialog dialog = findDialog();
            SwingUtilities.invokeAndWait(() ->
                    box(dialog.getContentPane(), "Deep-sky objects").doClick());
            assertFalse(controller.options().deepSkyObjects(),
                    "premise: a live preview is active");
            SwingUtilities.invokeAndWait(() -> {
                var action = dialog.getRootPane().getActionForKeyStroke(
                        javax.swing.KeyStroke.getKeyStroke(
                                java.awt.event.KeyEvent.VK_ESCAPE, 0));
                action.actionPerformed(new java.awt.event.ActionEvent(
                        dialog.getRootPane(), 0, "escape"));
            });
            flush();
            assertTrue(controller.options().deepSkyObjects(),
                    "Escape reverts the preview to the open-time snapshot");
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "Escape persists nothing");
            assertFalse(dialog.isDisplayable(), "Escape closes the dialog");

            // The production OK wiring: preview a change, press the real
            // OK button - the previewed value persists and the dialog
            // closes (a swapped constructor callback would fail here).
            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.open(frame[0], controller));
            flush();
            JDialog okDialog = findDialog();
            SwingUtilities.invokeAndWait(() -> {
                box(okDialog.getContentPane(), "Constellation boundaries")
                        .doClick();
                AboutDialogTest.button(okDialog.getContentPane(), "OK")
                        .doClick();
            });
            flush();
            assertFalse(okDialog.isDisplayable(), "OK closes the dialog");
            assertFalse(store.load().constellationBoundaries(),
                    "OK persists exactly the previewed options");
            assertFalse(controller.options().constellationBoundaries());

            // The production Cancel wiring: preview, press the real
            // Cancel - reverted, nothing persisted, dialog closed.
            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.open(frame[0], controller));
            flush();
            JDialog cancelDialog = findDialog();
            SwingUtilities.invokeAndWait(() -> {
                box(cancelDialog.getContentPane(), "Constellation figures")
                        .doClick();
                AboutDialogTest.button(cancelDialog.getContentPane(), "Cancel")
                        .doClick();
            });
            flush();
            assertFalse(cancelDialog.isDisplayable(), "Cancel closes");
            assertTrue(controller.options().constellationFigures(),
                    "Cancel reverts the preview");
            assertFalse(store.load().constellationBoundaries(),
                    "Cancel leaves the previously confirmed store alone");

            // The production window-close wiring: same revert protocol.
            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.open(frame[0], controller));
            flush();
            JDialog closeDialog = findDialog();
            SwingUtilities.invokeAndWait(() -> {
                box(closeDialog.getContentPane(), "Deep-sky objects")
                        .doClick();
                closeDialog.dispatchEvent(new java.awt.event.WindowEvent(
                        closeDialog,
                        java.awt.event.WindowEvent.WINDOW_CLOSING));
            });
            flush();
            assertFalse(closeDialog.isDisplayable(),
                    "the window close button closes the dialog");
            assertTrue(controller.options().deepSkyObjects(),
                    "window close reverts the preview like Cancel");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
            node.removeNode();
        }
    }

    private static int openDialogCount() {
        int count = 0;
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                count++;
            }
        }
        return count;
    }

    private static JDialog findDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    static JCheckBox box(java.awt.Component component, String accessibleName) {
        if (component instanceof JCheckBox checkBox
                && accessibleName.equals(checkBox
                        .getAccessibleContext().getAccessibleName())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JCheckBox found = box(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void theKeyboardRouteIsWholeAcrossTheFourTabs() throws Exception {
        // Every control that existed in 1.2.0 keeps the letter it
        // had, the five families take the five the gate chose, and no
        // two controls on one tab share one - which is the collision
        // that matters, since a mnemonic only reaches the tab in
        // front. That last point is why "Constellation figures" and
        // "Flamsteed numbers" may both keep Alt-F: they collided in
        // the single panel of 1.2.0, where both were visible at once,
        // and the tabs are what separates them.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });

            java.util.Map<String, Character> inherited =
                    new java.util.LinkedHashMap<>();
            inherited.put("Deep-sky objects", 'D');
            inherited.put("Deep-sky labels", 'l');
            inherited.put("Constellation figures", 'f');
            inherited.put("Constellation boundaries", 'b');
            inherited.put("Constellation names", 'n');
            inherited.put("Star names", 'S');
            inherited.put("Bayer letters", 'y');
            inherited.put("Flamsteed numbers", 'F');
            inherited.put("Equatorial coordinate grid", 'E');
            inherited.put("Title block", 'T');
            inherited.put("Stellar-magnitude key", 'k');
            for (java.util.Map.Entry<String, Character> kept
                    : inherited.entrySet()) {
                assertEquals(Character.toUpperCase(kept.getValue()),
                        (char) box(content, kept.getKey()).getMnemonic(),
                        kept.getKey() + " keeps the letter it had");
            }

            javax.swing.JTabbedPane tabs =
                    ChartOptionsDialog.tabsOf(content);
            for (int i = 0; i < tabs.getTabCount(); i++) {
                java.util.Map<Character, String> here =
                        new java.util.LinkedHashMap<>();
                for (JCheckBox check : checkBoxes(tabs.getComponentAt(i))) {
                    String clash = here.put((char) check.getMnemonic(),
                            check.getText());
                    assertEquals(null, clash,
                            tabs.getTitleAt(i) + ": " + clash + " and "
                                    + check.getText()
                                    + " share a letter");
                }
            }
        } finally {
            node.removeNode();
        }
    }

    @Test
    void twoControlsSeparatedByTabsMayShareALetter() throws Exception {
        // The inherited collision, recorded where it can be seen:
        // Alt-F reaches "Constellation figures" and "Flamsteed
        // numbers" alike, and it is the tabs that make each one
        // unambiguous on the tab that owns it.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });

            assertEquals(box(content, "Constellation figures")
                            .getMnemonic(),
                    box(content, "Flamsteed numbers").getMnemonic(),
                    "the inherited collision, still inherited");
            assertEquals('F', (char) box(content, "Flamsteed numbers")
                    .getMnemonic());

            javax.swing.JTabbedPane tabs =
                    ChartOptionsDialog.tabsOf(content);
            assertEquals("Stars", tabs.getTitleAt(1));
            assertEquals("Constellations", tabs.getTitleAt(2));
            assertNotNull(checkBoxes(tabs.getComponentAt(1)).stream()
                            .filter(box -> "Flamsteed numbers"
                                    .equals(box.getText()))
                            .findFirst().orElse(null),
                    "Flamsteed numbers lives on Stars");
            assertNotNull(checkBoxes(tabs.getComponentAt(2)).stream()
                            .filter(box -> "Constellation figures"
                                    .equals(box.getText()))
                            .findFirst().orElse(null),
                    "and Constellation figures on Constellations, so"
                            + " Alt-F is unambiguous on each");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theDeepSkyTabIsLegendAndControlAtOnce() throws Exception {
        // Sprint 21, issue #185: each family carries a checkbox, the
        // symbol the chart actually draws, its name, and a sentence
        // saying what it is - and the sentence is the accessible
        // description too, so nothing means anything only on hover.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });

            javax.swing.JTabbedPane tabs =
                    ChartOptionsDialog.tabsOf(content);
            assertEquals(4, tabs.getTabCount());
            assertEquals("Deep sky", tabs.getTitleAt(0));
            assertEquals("Stars", tabs.getTitleAt(1));
            assertEquals("Constellations", tabs.getTitleAt(2));
            assertEquals("Chart", tabs.getTitleAt(3));

            for (juranometria.render.SymbolFamily family
                    : juranometria.render.SymbolFamily.values()) {
                JCheckBox box = box(content, family.label());
                assertNotNull(box, family + " has a control");
                assertTrue(box.isSelected(), family + " starts drawn");
                assertEquals(family.prose(), box.getAccessibleContext()
                                .getAccessibleDescription(),
                        family + " explains itself to a screen reader");
                assertEquals(family.mnemonic(),
                        (char) box.getMnemonic(),
                        family + " answers to its letter");
            }
            assertEquals(5, chips(content).size(),
                    "one production symbol per family, drawn by the"
                            + " renderer rather than by an icon");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void aFamilyPreviewsLiveAndTheMasterGovernsWithoutErasing()
            throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });

            box(content, "Nebulae").doClick();
            assertFalse(controller.options().nebulae(),
                    "the family previews live on the chart");
            assertTrue(controller.options().galaxies(),
                    "and only that family");

            box(content, "Deep-sky objects").doClick();
            assertFalse(controller.options().deepSkyObjects());
            for (juranometria.render.SymbolFamily family
                    : juranometria.render.SymbolFamily.values()) {
                assertFalse(box(content, family.label()).isEnabled(),
                        family + " is ineffective while the master is"
                                + " off");
            }
            assertTrue(box(content, "Galaxies").isSelected(),
                    "but its choice is remembered, not erased");
            assertFalse(box(content, "Nebulae").isSelected(),
                    "and so is the one the reader switched off");

            box(content, "Deep-sky objects").doClick();
            assertTrue(controller.options().galaxies()
                            && !controller.options().nebulae(),
                    "so the master comes back to the chart the reader"
                            + " had: " + controller.options());
        } finally {
            node.removeNode();
        }
    }

    @Test
    void restoreDefaultsBringsEveryFamilyBack() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> { }, () -> { });
            box(content, "Galaxies").doClick();
            box(content, "Nebulae").doClick();
            box(content, "Deep-sky objects").doClick();

            button(content, "Restore Defaults").doClick();

            assertEquals(ChartOptions.DEFAULTS, controller.options(),
                    "Restore Defaults previews the released chart");
            for (juranometria.render.SymbolFamily family
                    : juranometria.render.SymbolFamily.values()) {
                JCheckBox box = box(content, family.label());
                assertTrue(box.isSelected(), family + " is back");
                assertTrue(box.isEnabled(),
                        family + " is usable again");
            }
            assertFalse(ChartOptionsStore.forNode(node).load()
                            .equals(ChartOptions.DEFAULTS)
                    && node.get("chart.galaxies", null) != null,
                    "and nothing is persisted until OK");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void cancelRevertsEveryFamilyToTheOpeningSnapshot() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            store.save(ChartOptions.DEFAULTS.withFamily(
                    juranometria.render.SymbolFamily.OPEN_CLUSTERS, false));
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            ChartOptions opening = controller.options();
            int[] cancelled = new int[1];
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> {
                        controller.revertTo(opening);
                        cancelled[0]++;
                    }, () -> { });

            box(content, "Galaxies").doClick();
            box(content, "Planetary nebulae").doClick();
            assertFalse(controller.options().galaxies());

            button(content, "Cancel").doClick();

            assertEquals(1, cancelled[0]);
            assertEquals(opening, controller.options(),
                    "the whole opening snapshot comes back, families"
                            + " included");
            assertFalse(controller.options().openClusters(),
                    "including the one that was already off");
        } finally {
            node.removeNode();
        }
    }

    /** Every checkbox inside a tab's component. */
    static java.util.List<JCheckBox> checkBoxes(
            java.awt.Component component) {
        java.util.List<JCheckBox> found = new java.util.ArrayList<>();
        if (component instanceof JCheckBox check) {
            found.add(check);
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                found.addAll(checkBoxes(child));
            }
        }
        return found;
    }

    private static java.util.List<juranometria.ui.SymbolChip> chips(
            java.awt.Component component) {
        java.util.List<juranometria.ui.SymbolChip> found =
                new java.util.ArrayList<>();
        if (component instanceof juranometria.ui.SymbolChip chip) {
            found.add(chip);
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                found.addAll(chips(child));
            }
        }
        return found;
    }

    static javax.swing.JButton button(java.awt.Component component,
                                      String text) {
        if (component instanceof javax.swing.JButton button
                && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JButton found = button(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
