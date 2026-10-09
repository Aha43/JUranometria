package juranometria.ui.solar;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import juranometria.chart.SkyPosition;
import juranometria.ui.Explain;
import juranometria.ui.language.MnemonicText;

/**
 * Centre on chart: one navigation action for every Solar System body
 * and every host that shows it (Sprint 45, issue #483).
 *
 * <p>From the Controller's group or a body's dialog, pressing it
 * <ol>
 *   <li>applies the host's <em>typed</em> query to the body's shared
 *       session - never a result displayed earlier - so the press is
 *       also a Compute, and every host shows what it computed;</li>
 *   <li>stops there when the session refused (no observer, a date
 *       outside the body's years, a range it cannot read): the refusal
 *       is already the status line's, and nothing on the chart
 *       changes;</li>
 *   <li>turns the body's chart layer on when it is off, through the
 *       body's own switch, so its persistence and notification are the
 *       switch's;</li>
 *   <li>centres the chart on the result's astrometric J2000 place - the
 *       frame the chart is drawn in - at the body's field (the normal
 *       minimum field, 1°, for the Sun and the Moon); and</li>
 *   <li>brings the chart window forward, leaving the Controller or the
 *       dialog where it is.</li>
 * </ol>
 *
 * <p>One instance, built by the application over the chart, and every
 * host's button calls it with its body's {@link Target}: what is
 * body-specific is only the computation and the layer. Nothing here
 * subscribes to anything, so a host built again adds no listener.
 * The field is the target's, not a constant, so a later body may ask
 * for a smaller one when the chart offers it (#481).
 */
public final class CentreOnChart {

    /** The bodies a target can be for, for counting presses per body. */
    public enum Body {
        SUN, MOON, JUPITER
    }

    /** What the action moves: the application's chart window. */
    public interface Chart {

        /** Centres the chart on a J2000 place at a field, in one change. */
        void centre(SkyPosition j2000, double fieldWidthDegrees);

        /** Brings the chart window forward, closing nothing. */
        void bringForward();

        /** The normal minimum field the chart offers, in degrees. */
        double normalMinimumFieldDegrees();
    }

    /** One body, as one host offers it. */
    public interface Target {

        /** Which body. */
        Body body();

        /**
         * Applies the host's typed query to the body's shared session and
         * answers the J2000 place to centre on, or null when the session
         * refused.
         */
        SkyPosition applyTypedAndLocate();

        /** The body's chart layer. */
        BodyOnChart layer();

        /** The field to centre at, given the chart's normal minimum. */
        default double fieldWidthDegrees(double normalMinimumFieldDegrees) {
            return normalMinimumFieldDegrees;
        }
    }

    private final Chart chart;
    private final Map<Body, Integer> presses = new EnumMap<>(Body.class);
    private final Map<Body, Integer> centred = new EnumMap<>(Body.class);

    public CentreOnChart(Chart chart) {
        if (chart == null) {
            throw new IllegalArgumentException("the action moves a chart");
        }
        this.chart = chart;
    }

    /**
     * Centres the chart on the target's body, as above.
     *
     * @return true when the chart was centred; false when the session refused
     */
    public boolean centre(Target target) {
        if (target == null) {
            throw new IllegalArgumentException("a target to centre on");
        }
        presses.merge(target.body(), 1, Integer::sum);
        SkyPosition place = target.applyTypedAndLocate();
        if (place == null) {
            return false;
        }
        if (!target.layer().showing()) {
            target.layer().show(true);
        }
        chart.centre(place, target.fieldWidthDegrees(chart.normalMinimumFieldDegrees()));
        chart.bringForward();
        centred.merge(target.body(), 1, Integer::sum);
        return true;
    }

    /** How many times a body was asked for, from any host. */
    public int presses(Body body) {
        return presses.getOrDefault(body, 0);
    }

    /** How many times the chart was centred on a body. */
    public int centred(Body body) {
        return centred.getOrDefault(body, 0);
    }

    /**
     * The button a host shows: its words from the body's key family
     * (the label, the spoken name and the explanation), its access
     * letter where the host has letters, and Enter as well as Space
     * pressing it while it has the keyboard.
     */
    public JButton button(Target target, SolarTableWords said, String name,
                          MnemonicText letters) {
        JButton button = new JButton(said.say("centre.label"));
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(said.say("centre.a11y"));
        if (letters != null) {
            letters.apply(button, said.key("centre.mnemonic"));
        }
        Explain.control(button, said.say("centre.hover"), said.say("centre.explain"));
        pressableByEnter(button);
        button.addActionListener(e -> centre(target));
        return button;
    }

    /** Enter presses a focused button as Space does. */
    static void pressableByEnter(AbstractButton button) {
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("ENTER"), "pressed");
        button.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("released ENTER"), "released");
    }
}
