package juranometria.app;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * The global Swing state a test borrows, and gives back.
 *
 * <p>Public because the tests that disturb a session are not all in
 * one package: a font sweep written beside the Inspector reached for
 * {@code UIManager.getFont} and put its answer back, which installs
 * the look and feel's own font as an override nobody chose (issue
 * #217 review). One implementation, wherever it is needed.
 *
 * <p>A look and feel and a {@code defaultFont} override are not a
 * test's own: they belong to the JVM the whole suite shares. A test
 * that installs one and walks away hands the next test a session
 * nobody chose - which is how order-dependent failures are made, and
 * why this lives in one place rather than being written out again
 * wherever a test needs a theme.
 *
 * <p>Both halves have already been got wrong once each: restoring by
 * applying the light theme (which passes every run that happened to
 * start light), and restoring the font by clearing it (which deletes
 * a choice rather than returning it). What is captured here is what
 * was there, and what is put back is the same.
 */
public final class SwingSession {

    private SwingSession() {
    }

    /** Something a test does that disturbs the global look and feel. */
    public interface Body {
        void run() throws Exception;
    }

    /**
     * Runs a body and puts the look and feel and the default-font
     * override back the way they were - whatever they were.
     *
     * <p>The state is captured before the body can fail, so a failure
     * inside it cannot leak a theme into whatever runs next.
     */
    public static void restoring(Body body) throws Exception {
        Held inherited = capture();
        try {
            body.run();
        } finally {
            inherited.restore();
        }
    }

    /**
     * What the session held, for a disturbance that spans JUnit
     * fixtures: capture in {@code BeforeEach}, hand it back in
     * {@code AfterEach}. Five journeys carried their own copy of
     * this pair before the gate counted them (#241, #224).
     */
    public record Held(LookAndFeel lookAndFeel, Object fontOverride) {

        /** Puts back exactly what was captured. */
        public void restore() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                // The look and feel first, then the exact override
                // that was found - a font somebody chose, or nothing
                // at all.
                if (lookAndFeel != null) {
                    try {
                        UIManager.setLookAndFeel(lookAndFeel);
                        // Live components wear the restored theme
                        // too - the journeys' own copies all did
                        // this, and a window left behind in the
                        // wrong clothes is a trace.
                        com.formdev.flatlaf.FlatLaf.updateUI();
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "could not restore "
                                        + lookAndFeel.getName(), e);
                    }
                }
                UIManager.put("defaultFont", fontOverride);
            });
        }
    }

    /** The look and feel and font override as they stand. */
    public static Held capture() {
        return new Held(UIManager.getLookAndFeel(), fontOverride());
    }

    /**
     * Runs a body and puts the default locale back - whatever it
     * was. The gate (#241) found the same capture-and-restore
     * written out in every rendering test that formats numbers;
     * this is that shape, once.
     */
    public static void restoringLocale(Body body) throws Exception {
        java.util.Locale inherited = java.util.Locale.getDefault();
        try {
            body.run();
        } finally {
            java.util.Locale.setDefault(inherited);
        }
    }

    /** Runs a body and puts the default time zone back. */
    public static void restoringTimeZone(Body body) throws Exception {
        java.util.TimeZone inherited = java.util.TimeZone.getDefault();
        try {
            body.run();
        } finally {
            java.util.TimeZone.setDefault(inherited);
        }
    }

    /**
     * Runs a body and puts the current repaint manager back. Global
     * like the look and feel, and restored the same way: what was
     * there, not what a fresh JVM would have had.
     */
    public static void restoringRepaintManager(Body body)
            throws Exception {
        javax.swing.RepaintManager inherited =
                javax.swing.RepaintManager.currentManager(null);
        try {
            body.run();
        } finally {
            javax.swing.RepaintManager.setCurrentManager(inherited);
        }
    }

    /** A body handed a scratch preference node. */
    public interface NodeBody {
        void run(java.util.prefs.Preferences node) throws Exception;
    }

    /**
     * Runs a body with a dedicated scratch preference node, and
     * removes the node afterwards <em>whatever happens</em> - the
     * gate (#241) found two nodes that outlived their tests because
     * removal sat on the success path only. The name is prefixed
     * and salted, so parallel runs cannot collide and nothing can
     * reach the reader's real store by mistake.
     */
    public static void scratchPreferences(String prefix, NodeBody body)
            throws Exception {
        java.util.prefs.Preferences node =
                java.util.prefs.Preferences.userRoot().node(
                        prefix + "-" + System.nanoTime());
        try {
            body.run(node);
        } finally {
            try {
                node.removeNode();
            } catch (IllegalStateException alreadyRemoved) {
                // A body that removed the node itself - the broken-
                // store fixtures do - has done this guard's work.
            }
        }
    }

    /**
     * The {@code defaultFont} somebody has chosen, or null when
     * nobody has.
     *
     * <p>Reading the value cannot answer this: a look and feel
     * publishes a default font of its own, and an override laid over
     * it reads back the same way. Comparing values cannot answer it
     * either, because a session is entitled to choose the font the
     * theme already uses - that is still a choice, and it still
     * outlives the theme it was made under.
     *
     * <p>What answers it is presence. {@code UIManager}'s own table
     * holds overrides only; the look and feel's values live in tables
     * behind it, and {@code containsKey} does not consult them.
     * Nothing here is written, so asking costs nothing.
     */
    public static Object fontOverride() {
        return UIManager.getDefaults().containsKey("defaultFont")
                ? UIManager.get("defaultFont")
                : null;
    }
}
