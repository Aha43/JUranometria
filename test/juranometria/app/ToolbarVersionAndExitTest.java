package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartViewController;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toolbar's version and its way out (Sprint 23, issue #198).
 *
 * <p>Knowing the exact version matters when reporting a defect or
 * checking an upgrade, and until now it meant opening Help - About.
 * The toolbar has room to say it quietly. And an application with one
 * exit surface on three platforms is easier to explain than one that
 * relies on whichever window decoration the desktop happens to draw.
 */
class ToolbarVersionAndExitTest {

    private static AtlasToolbar toolbar(String version, Runnable exit)
            throws Exception {
        AtlasToolbar[] made = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController navigation = new ChartViewController();
            made[0] = new AtlasToolbar(navigation,
                    new SearchField(Atlas.search(), Atlas.assembler(),
                            navigation),
                    null, version, exit);
        });
        return made[0];
    }

    private static List<String> labelsIn(Component component) {
        List<String> text = new ArrayList<>();
        if (component instanceof JLabel label && label.getText() != null) {
            text.add(label.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                text.addAll(labelsIn(child));
            }
        }
        return text;
    }

    @Test
    void theToolbarShowsExactlyTheVersionItIsGiven() throws Exception {
        // Distinctive on purpose: a toolbar that quietly printed
        // AppInfo's own version would pass a test that used the real
        // one, whatever it was actually reading.
        AtlasToolbar toolbar = toolbar("9.8.7-probe", () -> { });

        assertEquals("v9.8.7-probe", toolbar.versionText(),
                "the toolbar says what it was handed, and formats no"
                        + " version of its own");
        assertTrue(labelsIn(toolbar).contains("v9.8.7-probe"),
                "and a reader can see it: " + labelsIn(toolbar));
    }

    @Test
    void theToolbarAndAboutCannotDisagreeAboutTheVersion()
            throws Exception {
        // Both are fed from AppInfo.version(). This asserts the two
        // halves separately - what About prints, and what the toolbar
        // does with the same string - so a change to either shows up
        // as a difference rather than as two matching mistakes.
        String version = AppInfo.version();
        List<String>[] about = new List[1];
        SwingUtilities.invokeAndWait(
                () -> about[0] = labelsIn(AboutDialog.compactContent(
                        () -> { })));
        assertTrue(about[0].stream().anyMatch(t -> t.contains(version)),
                "About prints AppInfo's version: " + about[0]);

        AtlasToolbar toolbar = toolbar(version, () -> { });
        assertEquals("v" + version, toolbar.versionText(),
                "and the toolbar prints the same one");

        // And production really does hand it that one, rather than
        // some other string that happens to look right today.
        String main = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/juranometria/app/JUranometriaMain.java"));
        assertTrue(main.contains("AppInfo.version(), shutdown::request"),
                "the application passes About's own source to the"
                        + " toolbar; if this line moves, the two can"
                        + " drift and this test is how you find out");
    }

    @Test
    void theVersionIsStatusTextRatherThanAControl() throws Exception {
        AtlasToolbar toolbar = toolbar("1.2.3", () -> { });
        JLabel version = null;
        for (Component child : toolbar.getComponents()) {
            if (child instanceof JLabel label
                    && "v1.2.3".equals(label.getText())) {
                version = label;
            }
        }
        assertNotNull(version, "the version is a label");
        assertFalse(version.isFocusable(),
                "a reader tabbing through the toolbar must not stop on"
                        + " something that does nothing");
        assertEquals("JUranometria version 1.2.3",
                version.getAccessibleContext().getAccessibleName(),
                "but assistive technology is still told what it says");
    }

    @Test
    void theVersionYieldsFirstWhenTheToolbarIsSqueezed() throws Exception {
        AtlasToolbar toolbar = toolbar("1.2.3", () -> { });
        SwingUtilities.invokeAndWait(
                () -> toolbar.setAvailableWidth(2000));
        assertTrue(toolbar.isVersionShowing(),
                "the premise: room enough shows it");

        SwingUtilities.invokeAndWait(() -> toolbar.setAvailableWidth(320));

        assertFalse(toolbar.isVersionShowing(),
                "the copy a reader can find in About is what gives"
                        + " way, not a control");
        assertTrue(toolbar.exitButton().isVisible(),
                "the way out stays: nothing that does something is"
                        + " given up for something that says something");
        assertEquals("v1.2.3", toolbar.versionText(),
                "and it is hidden whole rather than truncated into an"
                        + " ambiguous version");

        SwingUtilities.invokeAndWait(
                () -> toolbar.setAvailableWidth(2000));
        assertTrue(toolbar.isVersionShowing(), "and it comes back");
    }

    @Test
    void theExitButtonSaysWhatItIsAndAsksRatherThanTerminates()
            throws Exception {
        List<String> asked = new ArrayList<>();
        AtlasToolbar toolbar = toolbar("1.2.3", () -> asked.add("exit"));

        assertEquals("Exit JUranometria",
                toolbar.exitButton().getAccessibleContext()
                        .getAccessibleName());
        assertEquals("Exit JUranometria",
                toolbar.exitButton().getToolTipText());
        assertTrue(toolbar.exitButton().isFocusable(),
                "reachable without a pointer");

        SwingUtilities.invokeAndWait(toolbar.exitButton()::doClick);

        assertEquals(List.of("exit"), asked,
                "the button asks the application to leave; it does not"
                        + " terminate anything itself, so one path can"
                        + " serve every exit surface");
    }

    @Test
    void exitIsTheRightmostControlWithTheVersionBeforeIt()
            throws Exception {
        AtlasToolbar toolbar = toolbar("1.2.3", () -> { });
        Component[] children = toolbar.getComponents();

        assertEquals(toolbar.exitButton(), children[children.length - 1],
                "the way out is the last thing on the bar");
        assertTrue(children[children.length - 2] instanceof JLabel label
                        && "v1.2.3".equals(label.getText()),
                "with the version immediately before it");
    }

    @Test
    void everyToolbarButtonCanBeReachedByKeyboard() throws Exception {
        // Each of these is built asking to be focusable, and FlatLaf's
        // toolbars take it away again when the button is added - their
        // convention, applied silently. The effect was that no control
        // on this bar could be reached without a pointer, though the
        // code had said otherwise since the toolbar was written.
        // Asserted under the look and feel the application actually
        // runs, because under any other the bug does not appear.
        SwingSession.restoring(() -> {
            SwingUtilities.invokeAndWait(
                    com.formdev.flatlaf.FlatLightLaf::setup);
            AtlasToolbar toolbar = toolbar("1.2.3", () -> { });
            List<String> unreachable = new ArrayList<>();
            for (Component child : toolbar.getComponents()) {
                if (child instanceof javax.swing.AbstractButton button
                        && !button.isFocusable()) {
                    unreachable.add(button.getAccessibleContext()
                            .getAccessibleName());
                }
            }
            assertEquals(List.of(), unreachable,
                    "a reader without a pointer must be able to reach"
                            + " every control on the bar: " + unreachable);
        });
    }

    // ---- the shutdown path itself -----------------------------------

    @Test
    void everySurfaceLeavesInTheSameOrderAndOnlyOnce() throws Exception {
        List<String> order = new ArrayList<>();
        Preferences scratch = Preferences.userRoot()
                .node("juranometria-shutdown-" + System.nanoTime());
        try {
            AppShutdown shutdown = new AppShutdown(
                    () -> order.add("terminate"), scratch);
            shutdown.onShutdown(() -> order.add("detach-first"));
            shutdown.onShutdown(() -> order.add("detach-second"));

            shutdown.request();

            assertEquals(List.of("detach-second", "detach-first",
                            "terminate"), order,
                    "detachments run newest first, then the"
                            + " application ends");
            assertTrue(shutdown.isRequested(), "and it says so");

            // A reader who presses the button and then reaches for
            // the close box is making one request, not two.
            shutdown.request();
            assertEquals(3, order.size(),
                    "asking twice does nothing the second time: "
                            + order);
        } finally {
            scratch.removeNode();
        }
    }

    @Test
    void oneDetachmentThatThrowsDoesNotKeepTheReaderInTheApplication()
            throws Exception {
        List<String> order = new ArrayList<>();
        Preferences scratch = Preferences.userRoot()
                .node("juranometria-shutdown-throw-" + System.nanoTime());
        try {
            AppShutdown shutdown = new AppShutdown(
                    () -> order.add("terminate"), scratch);
            shutdown.onShutdown(() -> order.add("quiet"));
            shutdown.onShutdown(() -> {
                throw new IllegalStateException("a listener misbehaving");
            });

            shutdown.request();

            assertEquals(List.of("quiet", "terminate"), order,
                    "leaving is not the moment to argue: the rest of"
                            + " the shutdown still runs, and the"
                            + " application still ends");
        } finally {
            scratch.removeNode();
        }
    }

    @Test
    void arealJvmLeavesThroughTheButton() throws Exception {
        // Termination cannot be proved in the suite that would be
        // terminated, so it is proved in a process of its own. This
        // needs no display, so CI runs it rather than only a
        // developer's machine.
        List<String> command = new ArrayList<>(List.of(
                System.getProperty("java.home") + "/bin/java",
                "-Djava.awt.headless=true",
                "-cp", "build/classes:build/test-classes:lib/*",
                "juranometria.app.ExitProbeMain"));
        Process probe = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean ended = probe.waitFor(90, TimeUnit.SECONDS);
        String said = new String(probe.getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        if (!ended) {
            probe.destroyForcibly();
        }
        assertTrue(ended,
                "a real activation of the Exit button must end the"
                        + " application: " + said);
        assertEquals(0, probe.exitValue(),
                "and end it cleanly: " + said);
    }
}
