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
        // Every step recorded, not just the two that were easy to
        // observe (review): a promised order nothing watches is a
        // comment, and this class's whole claim is the order.
        List<String> order = new ArrayList<>();
        AppShutdown shutdown = new AppShutdown(
                () -> order.add("flush"),
                () -> order.add("dispose"),
                () -> order.add("terminate"));
        shutdown.onShutdown(() -> order.add("detach-first"));
        shutdown.onShutdown(() -> order.add("detach-second"));

        shutdown.request();

        assertEquals(List.of("detach-second", "detach-first", "flush",
                        "dispose", "terminate"), order,
                "detachments newest first, then the reader's choices"
                        + " reach disk, then the windows go, then the"
                        + " application ends");
        // And the class's own statement of that order agrees with
        // what it just did, so the documentation cannot drift from
        // the behaviour without this failing.
        assertEquals(AppShutdown.STEPS,
                List.of("detach", "flush", "dispose", "terminate"),
                "the published order is the order that ran");
        assertTrue(shutdown.isRequested(), "and it says so");

        // A reader who presses the button and then reaches for the
        // close box is making one request, not two.
        shutdown.request();
        assertEquals(5, order.size(),
                "asking twice does nothing the second time: " + order);
    }

    @Test
    void noStepThatFailsCanKeepTheReaderInTheApplication()
            throws Exception {
        // Each step is guarded on its own, so a failure does not
        // skip the steps after it, and termination is unconditional.
        // The preference backend is the real risk here: flush()
        // declares a checked exception and can also fail at runtime,
        // and the first version guarded only the checked one - so a
        // backing store that threw would strand the application
        // half-closed, windows still up, never terminating (review).
        for (String failing : List.of("detach", "flush", "dispose")) {
            List<String> order = new ArrayList<>();
            Runnable boom = () -> {
                throw new IllegalStateException(
                        "the " + failing + " step misbehaving");
            };
            AppShutdown shutdown = new AppShutdown(
                    "flush".equals(failing) ? boom
                            : () -> order.add("flush"),
                    "dispose".equals(failing) ? boom
                            : () -> order.add("dispose"),
                    () -> order.add("terminate"));
            shutdown.onShutdown("detach".equals(failing) ? boom
                    : () -> order.add("detach"));

            shutdown.request();

            assertTrue(order.contains("terminate"),
                    "a failing " + failing + " step must still let the"
                            + " application end: " + order);
            for (String later : List.of("flush", "dispose")) {
                if (!later.equals(failing)) {
                    assertTrue(order.contains(later),
                            "and must not skip " + later + ": " + order);
                }
            }
        }
    }

    @Test
    void evenAnErrorOnTheWayOutStillLetsTheApplicationEnd()
            throws Exception {
        // The guard around each step deliberately does not catch
        // Error - an OutOfMemoryError is not something to swallow.
        // Termination sits in a finally for exactly that case, so the
        // application still ends rather than hanging with its windows
        // up.
        List<String> order = new ArrayList<>();
        AppShutdown shutdown = new AppShutdown(
                () -> {
                    throw new StackOverflowError("out of room");
                },
                () -> order.add("dispose"),
                () -> order.add("terminate"));

        try {
            shutdown.request();
        } catch (Error expected) {
            // Propagates, as it should; production has already gone.
        }

        assertTrue(order.contains("terminate"),
                "termination is unconditional: " + order);
    }

    @Test
    void thePreferenceFlushSurvivesAStoreThatIsNoLongerThere()
            throws Exception {
        // The real flush, against the real failure mode: a node
        // removed underneath it. Preferences.flush() throws
        // IllegalStateException for a removed node, which is exactly
        // the runtime failure the guard now covers.
        Preferences doomed = Preferences.userRoot()
                .node("juranometria-removed-" + System.nanoTime());
        doomed.put("chart.deepSkyObjects", "true");
        doomed.removeNode();

        AppShutdown.flushPreferences(doomed);
        assertFalse(doomed.nodeExists(""),
                "the premise: the node really is gone");
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
