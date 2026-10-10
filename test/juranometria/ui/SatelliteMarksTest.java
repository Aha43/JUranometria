package juranometria.ui;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution.Satellite;
import juranometria.module.OverlayContribution.Satellite.Relation;

import static juranometria.ui.SatelliteMarks.Decision.AT_PRIMARY;
import static juranometria.ui.SatelliteMarks.Decision.BEHIND;
import static juranometria.ui.SatelliteMarks.Decision.COLLIDES;
import static juranometria.ui.SatelliteMarks.Decision.DRAWN;
import static juranometria.ui.SatelliteMarks.Decision.IN_FRONT_UNRESOLVED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The owner's ruling 4 on #482, as the page applies it (#485): each moon
 * decided on its own. Behind is omitted; at the normal minimum field every
 * other moon is drawn, overlap allowed; above it a moon is drawn only where
 * its own mark is distinguishable, and a collision suppresses only the
 * lower-precedence mark - never the system.
 */
class SatelliteMarksTest {

    private static Satellite moon(String id, Relation relation, boolean shadowed, int order) {
        return new Satellite("jovian." + id, id, new SkyPosition(0, 0), "jovian.jupiter",
                relation, shadowed, order, 3.0, false, InkRole.BODY);
    }

    private static final Satellite GANYMEDE = moon("ganymede", Relation.CLEAR, false, 0);
    private static final Satellite CALLISTO = moon("callisto", Relation.CLEAR, false, 1);
    private static final Satellite IO = moon("io", Relation.CLEAR, false, 2);
    private static final Satellite EUROPA = moon("europa", Relation.CLEAR, false, 3);

    @Test
    void atTheNormalMinimumEveryMoonNotBehindIsDrawnOverlapAllowed() {
        List<Satellite> moons = List.of(moon("io", Relation.IN_FRONT, false, 2),
                moon("europa", Relation.IN_FRONT, false, 3), GANYMEDE,
                moon("callisto", Relation.BEHIND, false, 1));
        // Io and Europa 2 px apart on Jupiter's disc: unresolved, drawn anyway.
        List<double[]> at = List.of(new double[] {-4, 1}, new double[] {-2, 0},
                new double[] {70, -29}, new double[] {1, 1});
        assertEquals(List.of(DRAWN, DRAWN, DRAWN, BEHIND),
                SatelliteMarks.decide(true, 10.1, moons, at));
    }

    @Test
    void aboveItACollisionSuppressesOnlyTheLowerPrecedenceMark() {
        // Ganymede and Callisto 11 px apart and clear of Jupiter; Io 3 px
        // from Callisto; Europa at Jupiter's edge.
        List<Satellite> moons = List.of(IO, EUROPA, GANYMEDE, CALLISTO);
        List<double[]> at = List.of(new double[] {17, -4}, new double[] {4, 0},
                new double[] {25, -4}, new double[] {14, -4});
        assertEquals(List.of(COLLIDES, AT_PRIMARY, DRAWN, DRAWN),
                SatelliteMarks.decide(false, 6.0, moons, at),
                "one crowded pair loses one mark; every other distinguishable moon stays");
    }

    @Test
    void inFrontAboveTheFloorNeedsADiscThatCanHoldIt() {
        List<Satellite> moons = List.of(moon("io", Relation.IN_FRONT, false, 2));
        List<double[]> at = List.<double[]>of(new double[] {1, 0});
        assertEquals(List.of(IN_FRONT_UNRESOLVED), SatelliteMarks.decide(false, 6.0, moons, at));
        assertEquals(List.of(DRAWN), SatelliteMarks.decide(false, 12.0, moons, at));
    }

    @Test
    void precedenceIsInFrontThenClearThenShadowedThenTheModulesOrder() {
        Satellite shadowed = moon("io", Relation.CLEAR, true, 2);
        Satellite clear = moon("europa", Relation.CLEAR, false, 3);
        // Equal places: the clear moon keeps its mark over the shadowed one.
        List<double[]> at = List.of(new double[] {30, 0}, new double[] {32, 0});
        assertEquals(List.of(COLLIDES, DRAWN),
                SatelliteMarks.decide(false, 6.0, List.of(shadowed, clear), at));
        // Same state: the module's order (Ganymede before Europa).
        assertEquals(List.of(DRAWN, COLLIDES), SatelliteMarks.decide(false, 6.0,
                List.of(GANYMEDE, EUROPA), List.of(new double[] {30, 0}, new double[] {32, 0})));
    }
}
