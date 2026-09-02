package juranometria.app;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * Whether switching a deep-sky family off retires the chart's
 * searched target (Sprint 23, issue #196).
 *
 * <p>A chart titled for a symbol-capable target draws that target
 * whatever the family switches say - the target-honesty invariant, and
 * internally consistent. To a reader it was not: switching
 * <strong>Galaxies</strong> off removed every galaxy but the one the
 * chart happened to be titled for, with nothing on the surface saying
 * why, and choosing another object through point-and-identify did not
 * remove its privilege either, because selection and target are
 * separate.
 *
 * <p>So the explicit hide wins. Hiding the family a target belongs to
 * is as explicit a request as searching for it was, and it is the
 * later one.
 *
 * <p>This is the decision alone, with no Swing and no navigation in
 * it, so the wiring, the tests and the packaged journey all read the
 * same rule rather than three copies of it. It asks production for
 * every part of the answer: {@link ChartRenderer#symbolForType} for
 * the symbol an object draws and
 * {@link ChartOptions#effectiveFamily} for whether that symbol is
 * permitted - which already folds in the <strong>Deep-sky
 * objects</strong> master switch, so switching everything off follows
 * the same rule as switching one family off, by construction rather
 * than by a second branch that could drift.
 */
public final class TargetRetirement {

    private TargetRetirement() {
    }

    /**
     * Connects the rule: an options change that retires the target
     * does so before the chart is told what to draw, so the page is
     * assembled once, already knowing it has no target.
     *
     * <p>Production and every journey install it through this one
     * call. A rule the application wires by hand and a test wires by
     * hand again is two rules that can drift, and the second one
     * always passes.
     */
    public static void connect(ChartOptionsController options,
                               juranometria.ui.ChartComponent chart,
                               juranometria.ui.ChartViewController navigation) {
        if (options == null || chart == null || navigation == null) {
            throw new IllegalArgumentException(
                    "options, the chart and navigation are all required");
        }
        // The options in force before each change, because the rule
        // is about a transition and not about a state (review). The
        // first notification hands back what is already current, so
        // it compares equal to itself and retires nothing.
        java.util.concurrent.atomic.AtomicReference<ChartOptions> before =
                new java.util.concurrent.atomic.AtomicReference<>(
                        options.options());
        options.onChange(next -> {
            if (retires(chart.currentScene(), before.get(), next)) {
                navigation.retireTarget();
            }
            before.set(next);
            chart.setChartOptions(next);
        });
    }

    /**
     * Does this options change retire the scene's target?
     *
     * <p>The question is about a <strong>transition</strong>, not a
     * state: did <em>this</em> change hide the target's family? A
     * rule that merely asked whether the family is hidden would
     * retire a target on every later change while it stayed hidden -
     * so a reader who hides Galaxies, searches M 33 (which names it
     * again, as the decided rule says it should), and then toggles
     * Nebulae or the grid would lose the target to an action that had
     * nothing to do with it (review).
     *
     * <p>True only when there is a target, the page carries it, it
     * draws a symbol at all, and that symbol's family was permitted
     * before this change and is not permitted after it. An object the
     * atlas draws no symbol for was never kept by the exemption and
     * has nothing to lose.
     *
     * <p>Permission is asked of {@link ChartOptions#effectiveFamily},
     * which folds in the master switch, so a master-switch transition
     * is the same transition by construction.
     */
    public static boolean retires(ChartScene scene, ChartOptions before,
                                  ChartOptions next) {
        if (scene == null || before == null || next == null
                || scene.targetIdentity() == null) {
            return false;
        }
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!scene.targetIdentity().equals(dso.id())) {
                continue;
            }
            ChartRenderer.Symbol symbol =
                    ChartRenderer.symbolForType(dso.type());
            if (symbol == ChartRenderer.Symbol.NONE) {
                return false;
            }
            return before.effectiveFamily(symbol)
                    && !next.effectiveFamily(symbol);
        }
        return false;
    }
}
