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
    void aRingedPrimaryIsMeasuredByItsFigureNotByADisc() {
        // Saturn's rings 40 px across and 16 px high (#579, ruling 3 on
        // #578): a moon 23 px out along the major axis is 3 px from the
        // rings' edge, at the figure; the same distance along the minor
        // axis is 15 px clear of it. In front needs the globe's width,
        // not the rings'.
        java.util.function.ToDoubleFunction<double[]> rings = offset -> {
            double len = Math.hypot(offset[0], offset[1]);
            double cos = offset[0] / len;
            double sin = Math.abs(offset[1]) / len;
            return 20.0 * 8.0 / Math.hypot(8.0 * cos, 20.0 * sin);
        };
        Satellite titan = new Satellite("saturn.titan", "Titan", new SkyPosition(0, 0), "saturn",
                Relation.CLEAR, false, 0, 3.0, false, InkRole.BODY);
        Satellite rhea = new Satellite("saturn.rhea", "Rhea", new SkyPosition(0, 0), "saturn",
                Relation.CLEAR, false, 1, 3.0, false, InkRole.BODY);
        assertEquals(List.of(AT_PRIMARY, DRAWN), SatelliteMarks.decide(false, 18.0, rings,
                List.of(titan, rhea), List.of(new double[] {23, 0}, new double[] {0, 23})));
        Satellite inFront = new Satellite("saturn.titan", "Titan", new SkyPosition(0, 0),
                "saturn", Relation.IN_FRONT, false, 0, 3.0, false, InkRole.BODY);
        assertEquals(List.of(IN_FRONT_UNRESOLVED), SatelliteMarks.decide(false, 11.0, rings,
                List.of(inFront), List.<double[]>of(new double[] {1, 0})),
                "a globe under 12 px cannot hold a mark, however wide the rings");
        assertEquals(List.of(DRAWN), SatelliteMarks.decide(false, 12.0, rings,
                List.of(inFront), List.<double[]>of(new double[] {1, 0})));
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
