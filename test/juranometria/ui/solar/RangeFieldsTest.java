package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.context.ChartContextHolder;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The range's fields follow the view (#675, stage 4, ruled on #674): in
 * every Solar System panel, in both hosts, the span and the step are
 * shown only while <em>Over a time range</em> is selected; switching
 * away hides them without erasing the draft, moving the chart time or
 * computing; Compute stays whatever the view.
 */
class RangeFieldsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final Instant AUGUST = Instant.parse("2026-08-15T06:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, AUGUST);
    private static SolarSystemService solar;
    private static JovianSystemService jovian;

    @BeforeAll
    static void load() {
        solar = SolarSystemService.load();
        jovian = JovianSystemService.load();
    }

    private static Component named(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container inner) {
                Component found = named(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("no component named " + name);
    }

    /** Visible through every ancestor up to the arrangement's root. */
    private static boolean shown(Component component, Container root) {
        for (Component c = component; c != null && c != root; c = c.getParent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void exercise(JComponent host, javax.swing.JRadioButton instant,
                                 javax.swing.JRadioButton range,
                                 javax.swing.JComboBox<String> span,
                                 javax.swing.JComboBox<String> step,
                                 Component compute,
                                 java.util.function.IntSupplier computations,
                                 String what) {
        assertTrue(instant.isSelected(), what + ": the instant view to begin with");
        assertFalse(shown(span, host), what + ": the span is hidden under At chart time");
        assertFalse(shown(step, host), what + ": and the step");
        assertTrue(shown(compute, host), what + ": Compute is shown whatever the view");
        int before = computations.getAsInt();
        int spanAt = 3;
        int stepAt = 0;
        range.doClick();
        assertTrue(shown(span, host), what + ": Over a time range reveals the span");
        assertTrue(shown(step, host), what + ": and the step");
        assertTrue(shown(compute, host), what + ": Compute is still there");
        span.setSelectedIndex(spanAt);
        step.setSelectedIndex(stepAt);
        instant.doClick();
        assertFalse(shown(span, host), what + ": switching away hides the span");
        assertFalse(shown(step, host), what + ": and the step");
        assertEquals(spanAt, span.getSelectedIndex(), what + ": the draft's span is kept");
        assertEquals(stepAt, step.getSelectedIndex(), what + ": and its step");
        range.doClick();
        assertTrue(shown(span, host), what + ": and comes back as it was");
        assertEquals(spanAt, span.getSelectedIndex(), what + ": the same span");
        assertEquals(before, computations.getAsInt(),
                what + ": switching views computes nothing; Compute does");
    }

    @Test
    void theSunAndPlanetsControllerGroupsShowTheRangeFieldsOnlyUnderOverATimeRange()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChartContextHolder chart = new ChartContextHolder(OSLO);
            SolarTableSession session = new SolarTableSession(chart::observer, solar,
                    SolarTable.venus());
            session.followContext(chart);
            SolarTableControls controls = new SolarTableControls(session, EN, null, false);
            JComponent host = controls.inController();
            exercise(host, controls.instantView, controls.rangeView, controls.span,
                    controls.step, controls.compute, session::computations,
                    "Venus in the Controller");
        });
    }

    @Test
    void theSunAndMoonDialogShowsTheRangeFieldsOnlyUnderOverATimeRange() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SolarTableDialog.Content content = SolarTableDialog.content(() -> OSLO, solar,
                    EN, SolarTable.sun());
            exercise(content, content.instantView, content.rangeView, content.span,
                    content.step, content.compute, () -> 0, "the Sun's dialog");
        });
    }

    @Test
    void aPlanetsDialogAndControllerGroupShowTheRangeFieldsOnlyUnderOverATimeRange()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JovianTableSession session = new JovianTableSession(() -> OSLO, () -> jovian);
            JovianTableControls dialog = new JovianTableControls(session, EN, true);
            JComponent inDialog = dialog.inDialog();
            exercise(inDialog, dialog.instantView, dialog.rangeView, dialog.span,
                    dialog.step, dialog.compute, session::computations, "Jupiter's dialog");
            JovianTableControls group = new JovianTableControls(session, EN, false);
            JComponent inController = group.inController();
            exercise(inController, group.instantView, group.rangeView, group.span,
                    group.step, group.compute, session::computations,
                    "Jupiter in the Controller");
        });
    }
}
