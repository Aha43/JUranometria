package juranometria.app;

import javax.swing.JRootPane;

import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;

/**
 * How the running atlas hands its own switches to the chart keyboard
 * (Sprint 31, issue #312).
 *
 * <p>The wiring itself, in one place, because it is the thing worth
 * testing. Three of the twenty switches do not belong to the chart's
 * options at all - they belong to modules, each with its own seam and
 * its own promise about what is remembered - and the adapters between
 * them and the palette are exactly where a keystroke can quietly stop
 * meaning what the reader's own control means.
 *
 * <p>Written inline in {@code main}, those adapters could only be
 * tested by building a different window and hoping it matched
 * (review, #312). Named here, the application installs them and a
 * test drives the same ones: what a letter reaches, whether the chart
 * repaints, and whether anything was reassembled to make it.
 */
public final class ChartKeyboardSession {

    private ChartKeyboardSession() {
    }

    /**
     * Binds the prefix on this window and gives the palette the
     * atlas's own transitions.
     *
     * @param root the chart window's root pane, which owns the prefix
     * @param options the controller the dialog and the chart share
     * @param ecliptic the attached module, for what is showing
     * @param eclipticToggle the View menu's own switch, which is
     *     where the remembering lives
     * @param observer the attached module whose lines Place and Time
     *     switches
     */
    public static void install(JRootPane root,
                               ChartOptionsController options,
                               EclipticModule ecliptic,
                               Runnable eclipticToggle,
                               MeridianModule observer) {
        if (ecliptic == null || eclipticToggle == null
                || observer == null) {
            throw new IllegalArgumentException(
                    "the chart keyboard is wired to the modules the"
                            + " atlas is running");
        }
        ChartKeyboard.install(root, ChartSwitches.of(options,
                new ChartSwitches.Ecliptic() {
                    @Override
                    public boolean showing() {
                        return ecliptic.showing();
                    }

                    @Override
                    public void toggle() {
                        // The menu item's own switch and nothing
                        // else. Painting the frame again as well
                        // looked harmless and was not: it made the
                        // letter cost more than the menu item for
                        // the same change, and the module already
                        // asks the chart to redraw when it starts or
                        // stops showing (review, #312).
                        eclipticToggle.run();
                    }
                },
                new ChartSwitches.ObserverLines() {
                    @Override
                    public boolean meridianShowing() {
                        return observer.meridianShowing();
                    }

                    @Override
                    public boolean horizonShowing() {
                        return observer.horizonShowing();
                    }

                    @Override
                    public void showing(boolean showMeridian,
                                        boolean showHorizon) {
                        observer.showing(showMeridian, showHorizon,
                                observer.zenithShowing());
                    }
                }),
                keyboard -> keyboard.showIn(root));
    }
}
