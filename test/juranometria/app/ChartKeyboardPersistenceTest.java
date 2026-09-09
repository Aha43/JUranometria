package juranometria.app;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What survives the session, and what the keyboard may not promise
 * (Sprint 31, issue #312).
 *
 * <p>The atlas keeps three different promises about the switches on
 * the palette, and a keyboard route must keep each of them exactly as
 * it found it:
 *
 * <ul>
 *   <li>the chart's own layers are <strong>stored</strong>, so the
 *       page a reader left is the page they come back to;</li>
 *   <li>the ecliptic is <strong>stored</strong> too, by its own
 *       session, and has been since it was built;</li>
 *   <li>the observer's lines are <strong>not stored at all</strong> -
 *       Place and Time saves a latitude and a longitude and nothing
 *       about what is drawn - so they last as long as the window
 *       does and no longer.</li>
 * </ul>
 *
 * <p>That last one is the reason this test exists. A keyboard that
 * quietly began saving the meridian would be making a promise
 * production does not make, and the first anybody would know of it is
 * a reader whose chart came back with lines they had left behind.
 */
class ChartKeyboardPersistenceTest {

    @Test
    void aLetterCommitsTheChartsOwnLayersTheWayTheDialogsOkDoes() {
        // The palette has no OK and no Cancel, so a letter is the
        // whole gesture: applied and stored in one press, as the View
        // menu's Ecliptic item has behaved since #274.
        List<ChartOptions> saved = new ArrayList<>();
        ChartOptions[] held = {ChartOptions.DEFAULTS};
        ChartOptionsController options = new ChartOptionsController(
                new ChartOptionsStore() {
                    @Override
                    public ChartOptions load() {
                        return held[0];
                    }

                    @Override
                    public void save(ChartOptions next) {
                        held[0] = next;
                        saved.add(next);
                    }
                });
        ChartKeyboard keyboard = ChartKeyboard.of(
                ChartSwitches.of(options, new NoEcliptic(),
                        new NoLines()));

        keyboard.press('T');
        assertFalse(options.options().titleBlock(),
                "the chart follows at once");
        assertEquals(1, saved.size(),
                "and the choice is stored, once");
        assertFalse(saved.get(saved.size() - 1).titleBlock(),
                "as the reader left it");

        assertFalse(new ChartOptionsController(new ChartOptionsStore() {
            @Override
            public ChartOptions load() {
                return held[0];
            }

            @Override
            public void save(ChartOptions next) {
                held[0] = next;
            }
        }).options().titleBlock(),
                "so a session that starts again reads what they chose");
    }

    @Test
    void theEclipticIsStoredByItsOwnSessionAndNotBySomethingNew() {
        boolean[] showing = {false};
        List<Boolean> saved = new ArrayList<>();
        ChartKeyboard keyboard = ChartKeyboard.of(ChartSwitches.of(
                new ChartOptionsController(new ChartOptionsStore() {
                    @Override
                    public ChartOptions load() {
                        return ChartOptions.DEFAULTS;
                    }

                    @Override
                    public void save(ChartOptions options) {
                    }
                }),
                new ChartSwitches.Ecliptic() {
                    @Override
                    public boolean showing() {
                        return showing[0];
                    }

                    @Override
                    public void toggle() {
                        // Exactly what the View menu's item runs, which
                        // is where the saving lives.
                        showing[0] = !showing[0];
                        saved.add(showing[0]);
                    }
                },
                new NoLines()));

        keyboard.press('I');
        assertTrue(showing[0], "the ecliptic is drawn");
        assertEquals(List.of(true), saved,
                "through its own session's switch, which is what"
                        + " stores it - the keyboard adds no second"
                        + " path to the store");
    }

    @Test
    void theObserversLinesLastExactlyAsLongAsTheyDidBefore() {
        // Place and Time stores a place, not a picture: nothing about
        // the meridian, the horizon or the zenith is written down. The
        // keyboard switches them the way the dialog does and stores
        // nothing, because storing them would be a new promise.
        boolean[] lines = {false, false};
        List<String> stored = new ArrayList<>();
        ChartKeyboard keyboard = ChartKeyboard.of(ChartSwitches.of(
                new ChartOptionsController(new ChartOptionsStore() {
                    @Override
                    public ChartOptions load() {
                        return ChartOptions.DEFAULTS;
                    }

                    @Override
                    public void save(ChartOptions options) {
                        stored.add("chart options");
                    }
                }),
                new NoEcliptic(),
                new ChartSwitches.ObserverLines() {
                    @Override
                    public boolean meridianShowing() {
                        return lines[0];
                    }

                    @Override
                    public boolean horizonShowing() {
                        return lines[1];
                    }

                    @Override
                    public void showing(boolean meridian, boolean horizon) {
                        lines[0] = meridian;
                        lines[1] = horizon;
                    }
                }));

        keyboard.press('R');
        keyboard.press('H');
        assertTrue(lines[0] && lines[1],
                "both lines are drawn for the reader who asked");
        assertEquals(List.of(), stored,
                "and nothing was written down for them, which is the"
                        + " promise production already makes: the"
                        + " lines last as long as the window");
    }

    /** An ecliptic nobody has switched on. */
    private static final class NoEcliptic
            implements ChartSwitches.Ecliptic {

        private boolean showing;

        @Override
        public boolean showing() {
            return showing;
        }

        @Override
        public void toggle() {
            showing = !showing;
        }
    }

    /** Observer lines nobody is watching. */
    private static final class NoLines
            implements ChartSwitches.ObserverLines {

        private boolean meridian;
        private boolean horizon;

        @Override
        public boolean meridianShowing() {
            return meridian;
        }

        @Override
        public boolean horizonShowing() {
            return horizon;
        }

        @Override
        public void showing(boolean showMeridian, boolean showHorizon) {
            meridian = showMeridian;
            horizon = showHorizon;
        }
    }
}
