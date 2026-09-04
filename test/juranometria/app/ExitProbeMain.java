package juranometria.app;

import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartViewController;
import juranometria.ui.SearchField;

/**
 * A whole JVM that leaves through the toolbar's Exit button (Sprint
 * 23, issue #198).
 *
 * <p>Termination cannot be asserted in the suite that is asserting
 * it: a test that proves {@code System.exit} works takes the test
 * runner with it. So the real thing is proved in a process of its
 * own - this one - and
 * {@code ToolbarVersionAndExitTest} runs it and reads the exit code.
 *
 * <p>What is real here: the production {@link AtlasToolbar}, its
 * Exit button, the production {@link AppShutdown} and its
 * {@code System.exit}. What is not: the preferences node, which is a
 * scratch one, because a probe must not flush over a reader's
 * settings to prove that flushing happens.
 *
 * <p>If the shutdown path fails to terminate, this process falls
 * through to a non-zero exit, and the test says which.
 */
public final class ExitProbeMain {

    private ExitProbeMain() {
    }

    public static void main(String[] args) throws Exception {
        // The node's name comes from the parent test, which asserts
        // after this JVM ends that the deletion really reached the
        // backing store - a probe that only promised to clean up
        // was proving nothing (review).
        Preferences scratch = Preferences.userRoot().node(
                args.length > 0 ? args[0]
                        : "juranometria-exit-probe-" + System.nanoTime());
        // The probe's success path IS System.exit, so a cleanup
        // written after the click can never run on success - which
        // is how every passing probe run leaked a node until the
        // gate counted them (#241, #224). A shutdown hook runs on
        // both paths. Removal is flushed through the PARENT, because
        // flushing a removed node throws - the first version did
        // exactly that and swallowed it, so the deletion could die
        // in memory (review). Trouble is printed, not swallowed:
        // the parent captures this stream.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Preferences parent = scratch.parent();
                scratch.removeNode();
                parent.flush();
            } catch (Exception trouble) {
                System.err.println("exit probe cleanup failed: "
                        + trouble);
            }
        }));
        AppShutdown shutdown = new AppShutdown(
                () -> AppShutdown.flushPreferences(scratch),
                AppShutdown::disposeEveryWindow,
                () -> System.exit(0));
        boolean[] detached = {false};
        shutdown.onShutdown(() -> detached[0] = true);

        AtlasToolbar[] toolbar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController navigation = new ChartViewController();
            toolbar[0] = new AtlasToolbar(navigation,
                    new SearchField(Atlas.search(), Atlas.assembler(),
                            navigation),
                    null, AppInfo.version(), shutdown::request);
        });

        // A real activation of the real button.
        SwingUtilities.invokeAndWait(() -> toolbar[0].exitButton().doClick());

        // Only reached if the button did not take the application
        // out. Say which half failed, so the test can report it.
        Thread.sleep(2000);
        System.err.println(detached[0]
                ? "shutdown ran but did not terminate"
                : "the exit button did not reach the shutdown path");
        System.exit(3);
    }
}
