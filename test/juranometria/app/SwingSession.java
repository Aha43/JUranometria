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
        LookAndFeel inherited = UIManager.getLookAndFeel();
        Object inheritedFont = fontOverride();
        try {
            body.run();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                // The look and feel first, then the exact override
                // that was found - a font somebody chose, or nothing
                // at all.
                if (inherited != null) {
                    try {
                        UIManager.setLookAndFeel(inherited);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "could not restore " + inherited.getName(),
                                e);
                    }
                }
                UIManager.put("defaultFont", inheritedFont);
            });
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
