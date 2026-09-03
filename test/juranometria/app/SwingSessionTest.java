package juranometria.app;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The borrowed Swing state really is given back (PR #188 review).
 *
 * <p>Several tests install a look and feel, and some enlarge the
 * default font, because the questions they ask - does a control name
 * itself, does a dialog fit a short screen - are questions about the
 * application's own chrome. All of that is global to the JVM the
 * suite shares, so leaving any of it behind hands the next test a
 * session nobody chose. {@link SwingSession} is the one place that
 * borrowing happens; this is where it is held to giving back.
 *
 * <p>Both halves have been got wrong once each, and both mistakes
 * pass a careless test: restoring by applying the light theme passes
 * every run that happened to start light, and restoring the font by
 * clearing it passes unless somebody had chosen one.
 */
class SwingSessionTest {

    /** A font nobody would choose by accident. */
    private static final FontUIResource DISTINCTIVE =
            new FontUIResource("Serif", Font.BOLD, 21);

    @Test
    void theThemeThatWasThereComesBack() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        SwingSession.restoring(() -> {
            // Stand in for a session that was not using the light
            // theme, so restoring the light theme would be visibly
            // wrong rather than accidentally right.
            SwingUtilities.invokeAndWait(() -> UiTheme.apply(true));
            String stoodIn = UIManager.getLookAndFeel().getName();
            float stoodInText = UIManager.getFont("Label.font").getSize2D();

            SwingSession.restoring(() -> SwingUtilities.invokeAndWait(() -> {
                UiTheme.apply(false);
                UIManager.put("defaultFont", DISTINCTIVE);
                UiTheme.apply(false);
            }));

            assertEquals(stoodIn, UIManager.getLookAndFeel().getName(),
                    "the theme the session had is the theme it has");
            assertEquals(stoodInText,
                    UIManager.getFont("Label.font").getSize2D(), 0.01f,
                    "and no enlarged font was left behind");
        });
    }

    @Test
    void aFontSomeoneElseChoseComesBackExactly() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        SwingSession.restoring(() -> {
            SwingUtilities.invokeAndWait(
                    () -> UIManager.put("defaultFont", DISTINCTIVE));

            SwingSession.restoring(() -> SwingUtilities.invokeAndWait(
                    () -> UiTheme.apply(false)));

            assertEquals(DISTINCTIVE, UIManager.get("defaultFont"),
                    "the override this session had chosen is back,"
                            + " exactly - clearing it would have"
                            + " deleted a choice rather than returning"
                            + " it");
        });
    }

    @Test
    void andNoOverrideIsInventedWhereThereWasNone() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        // The half that cannot be seen by reading the key back: a
        // look and feel publishes a default font of its own, and an
        // override laid over it answers the same. What tells them
        // apart is a change of theme - an override survives one and
        // pins the font against it. Metal declares no default font,
        // so after a restoration that invented nothing there is
        // nothing left to find.
        SwingSession.restoring(() -> {
            SwingUtilities.invokeAndWait(() -> UiTheme.apply(false));
            assertNotNull(UIManager.get("defaultFont"),
                    "the theme publishes a font of its own, which is"
                            + " what makes this worth checking");
            assertNull(SwingSession.fontOverride(),
                    "and publishing one is not the same as choosing"
                            + " one");

            SwingSession.restoring(() -> SwingUtilities.invokeAndWait(
                    () -> UiTheme.apply(true)));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(
                            new javax.swing.plaf.metal.MetalLookAndFeel());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            assertNull(UIManager.get("defaultFont"),
                    "an override invented by the restoration would"
                            + " have survived this change of theme");
        });
    }

    @Test
    void andSoDoesAChosenFontThatMatchesTheThemeAnyway() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        // The case a value comparison cannot see: a session that
        // chose the font the theme was already using. It reads back
        // identically to no choice at all - and it is still a choice,
        // because it outlives the theme it was made under, which is
        // exactly what choosing it was for.
        SwingSession.restoring(() -> {
            SwingUtilities.invokeAndWait(
                    () -> juranometria.app.UiTheme.apply(false));
            java.awt.Font themes =
                    (java.awt.Font) UIManager.get("defaultFont");
            // From the theme's own font rather than its family:
            // on Linux the family is "SansSerif" while the name is
            // "sansserif", so rebuilding from the family produces a
            // font that is NOT equal and the premise below fails -
            // found by the display CI of #209, where this test had
            // never run.
            javax.swing.plaf.FontUIResource sameButChosen =
                    new javax.swing.plaf.FontUIResource(themes);
            assertEquals(themes, sameButChosen,
                    "the point of this test: the chosen font and the"
                            + " theme's are equal");
            SwingUtilities.invokeAndWait(
                    () -> UIManager.put("defaultFont", sameButChosen));

            SwingSession.restoring(() -> SwingUtilities.invokeAndWait(
                    () -> UiTheme.apply(false)));

            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(
                            new javax.swing.plaf.metal.MetalLookAndFeel());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            assertEquals(sameButChosen, UIManager.get("defaultFont"),
                    "a chosen font that matched the theme is still a"
                            + " choice, and must survive a change of"
                            + " theme");
        });
    }

    @org.junit.jupiter.api.Test
    void localeAndTimeZoneComeBackExactlyEvenWhenTheBodyFails() {
        java.util.Locale locale = java.util.Locale.getDefault();
        java.util.TimeZone zone = java.util.TimeZone.getDefault();
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () ->
                        SwingSession.restoringLocale(() ->
                                SwingSession.restoringTimeZone(() -> {
                                    java.util.Locale.setDefault(
                                            java.util.Locale
                                                    .forLanguageTag("tr-TR"));
                                    java.util.TimeZone.setDefault(
                                            java.util.TimeZone.getTimeZone(
                                                    "Pacific/Kiritimati"));
                                    throw new IllegalStateException("boom");
                                })));
        org.junit.jupiter.api.Assertions.assertEquals(locale,
                java.util.Locale.getDefault(),
                "a failing body cannot leak a locale");
        org.junit.jupiter.api.Assertions.assertEquals(zone,
                java.util.TimeZone.getDefault(),
                "or a time zone");
    }

    @org.junit.jupiter.api.Test
    void theRepaintManagerComesBackTheSameInstance() throws Exception {
        javax.swing.RepaintManager inherited =
                javax.swing.RepaintManager.currentManager(null);
        SwingSession.restoringRepaintManager(() ->
                javax.swing.RepaintManager.setCurrentManager(
                        new javax.swing.RepaintManager()));
        org.junit.jupiter.api.Assertions.assertSame(inherited,
                javax.swing.RepaintManager.currentManager(null),
                "what was there, not a fresh one");
    }

    @org.junit.jupiter.api.Test
    void aScratchNodeIsRemovedWhateverHappensAndToleratesTheFixture()
            throws Exception {
        String[] name = new String[1];
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () ->
                        SwingSession.scratchPreferences(
                                "juranometria-scratch-guard-test",
                                node -> {
                                    name[0] = node.name();
                                    node.put("k", "v");
                                    throw new IllegalStateException("boom");
                                }));
        org.junit.jupiter.api.Assertions.assertFalse(
                java.util.prefs.Preferences.userRoot()
                        .nodeExists(name[0]),
                "the node is gone even though the body failed - the"
                        + " gap that leaked two nodes before #224");

        // And a body that removes the node as its own fixture - the
        // broken-store tests do - is not punished for it.
        SwingSession.scratchPreferences(
                "juranometria-scratch-guard-test", node -> {
                    node.put("k", "v");
                    node.removeNode();
                });
    }

    @org.junit.jupiter.api.Test
    void aFailingCleanupIsSuppressedBehindTheFailureThatMatters() {
        // Both halves fail: the reader must see the body's failure,
        // with the cleanup's trouble attached as suppressed - never
        // the other way round, which is what the first wrappers did
        // (review).
        IllegalStateException primary =
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalStateException.class, () ->
                                SwingSession.guarded(() -> {
                                    throw new IllegalStateException(
                                            "the failure that matters");
                                }, () -> {
                                    throw new IllegalStateException(
                                            "cleanup trouble");
                                }));
        org.junit.jupiter.api.Assertions.assertEquals(
                "the failure that matters", primary.getMessage(),
                "the body's failure is the one thrown");
        org.junit.jupiter.api.Assertions.assertEquals(1,
                primary.getSuppressed().length,
                "with the cleanup's trouble attached");
        org.junit.jupiter.api.Assertions.assertEquals(
                "cleanup trouble",
                primary.getSuppressed()[0].getMessage());

        // And a cleanup failing alone still surfaces.
        IllegalStateException alone =
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalStateException.class, () ->
                                SwingSession.guarded(() -> { }, () -> {
                                    throw new IllegalStateException(
                                            "cleanup trouble");
                                }));
        org.junit.jupiter.api.Assertions.assertEquals(
                "cleanup trouble", alone.getMessage());
    }

    @org.junit.jupiter.api.Test
    void whatWasCapturedIsWhatComesBack() throws Exception {
        SwingSession.Held before = SwingSession.capture();
        SwingSession.Held held = SwingSession.capture();
        javax.swing.UIManager.put("defaultFont",
                new java.awt.Font(java.awt.Font.SANS_SERIF,
                        java.awt.Font.PLAIN, 23));
        held.restore();
        org.junit.jupiter.api.Assertions.assertEquals(
                before.fontOverride(),
                SwingSession.fontOverride(),
                "the exact override that was found - a font somebody"
                        + " chose, or nothing at all");
        org.junit.jupiter.api.Assertions.assertSame(
                before.lookAndFeel(),
                javax.swing.UIManager.getLookAndFeel(),
                "and the same look and feel instance");
    }
}
