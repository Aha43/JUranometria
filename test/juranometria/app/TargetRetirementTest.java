package juranometria.app;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The retirement rule itself (Sprint 23, issue #196), with no window
 * and no display, so it is exercised by CI rather than only on a
 * developer's machine - the journeys that drive it through real
 * controls abort where there is no display.
 *
 * <p>The rule is about a <strong>transition</strong>. Asking merely
 * whether the target's family is hidden retires a target on every
 * later change while it stays hidden, which is the defect the review
 * found: hide Galaxies, search M 33 - which names it again, as the
 * decided rule says it should - then toggle anything at all, and the
 * target is gone to an action that had nothing to do with it.
 */
class TargetRetirementTest {

    private static final String M33 = "NGC 598";

    private static DeepSkyObject galaxy(String id) {
        return new DeepSkyObject(id, List.of(), DsoType.GALAXY,
                new SkyPosition(23.462, 30.660), 62.09, 38.02, 23.0,
                5.7, 1);
    }

    /** A page naming one galaxy as its searched target. */
    private static ChartScene targeting(String id) {
        return new ChartScene(
                new ChartViewport(new SkyPosition(23.462, 30.660),
                        8.0, 900, 700),
                List.of(), List.of(galaxy(M33)), "M 33", 8.0, id);
    }

    private static ChartOptions galaxies(boolean shown) {
        return ChartOptions.DEFAULTS.withFamily(SymbolFamily.GALAXIES,
                shown);
    }

    private static ChartOptions master(boolean on) {
        ChartOptions d = ChartOptions.DEFAULTS;
        return new ChartOptions(on, d.deepSkyLabels(),
                d.constellationFigures(), d.constellationBoundaries(),
                d.constellationNames(), d.starNames(), d.bayerLetters(),
                d.flamsteedNumbers(), d.equatorialGrid(), d.titleBlock(),
                d.magnitudeKey(), d.galaxies(), d.openClusters(),
                d.globularClusters(), d.nebulae(), d.planetaryNebulae());
    }

    @Test
    void onlyTheShownToHiddenTransitionRetiresTheTarget() {
        ChartScene scene = targeting(M33);

        assertTrue(TargetRetirement.retires(scene, galaxies(true),
                        galaxies(false)),
                "hiding the family the target belongs to retires it");
        assertFalse(TargetRetirement.retires(scene, galaxies(false),
                        galaxies(false)),
                "but it is retired once, not on every later change"
                        + " while the family stays hidden - the case"
                        + " that let a freshly searched target be"
                        + " taken away by an unrelated toggle");
        assertFalse(TargetRetirement.retires(scene, galaxies(false),
                        galaxies(true)),
                "showing the family again takes nothing away");
        assertFalse(TargetRetirement.retires(scene, galaxies(true),
                        galaxies(true)),
                "and a change that leaves the family shown is not"
                        + " about the target at all");
    }

    @Test
    void aChangeToAnotherFamilyLeavesTheTargetAlone() {
        ChartScene scene = targeting(M33);
        // The reported reproduction, in its smallest form: Galaxies
        // already hidden, the reader searches M 33, then hides
        // Nebulae. Nothing about the second action concerns M 33.
        ChartOptions hidden = galaxies(false);
        ChartOptions andNebulae =
                hidden.withFamily(SymbolFamily.NEBULAE, false);

        assertFalse(TargetRetirement.retires(scene, hidden, andNebulae),
                "hiding nebulae is not a statement about a galaxy");
    }

    @Test
    void theMasterSwitchIsTheSameTransition() {
        ChartScene scene = targeting(M33);

        assertTrue(TargetRetirement.retires(scene, master(true),
                        master(false)),
                "switching deep-sky objects off is the same"
                        + " transition one level up");
        assertFalse(TargetRetirement.retires(scene, master(false),
                        master(false)),
                "and it too is a transition rather than a state");
    }

    @Test
    void aPageWithNoTargetHasNothingToRetire() {
        ChartScene anonymous = new ChartScene(
                new ChartViewport(new SkyPosition(23.462, 30.660),
                        8.0, 900, 700),
                List.of(), List.of(galaxy(M33)), "coordinates", 8.0);

        assertFalse(TargetRetirement.retires(anonymous, galaxies(true),
                        galaxies(false)),
                "an untitled page has no target to lose");
        assertFalse(TargetRetirement.retires(targeting("NGC 224"),
                        galaxies(true), galaxies(false)),
                "and a target the page does not carry is not this"
                        + " page's business");
    }
}
