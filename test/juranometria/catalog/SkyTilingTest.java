package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyTilingTest {

    @Test
    void homeTileIdentityIsDeterministic() {
        // The worked examples from the decision document.
        assertEquals("r00-d4", SkyTiling.tileId(new SkyPosition(10.684708, 41.268750)),
                "M31 lives in the first RA column, band [30, 60)");
        assertEquals("r11-d4", SkyTiling.tileId(new SkyPosition(359.99, 41.0)));
        assertEquals("r00-d4", SkyTiling.tileId(new SkyPosition(0.0, 30.0)),
                "a band boundary belongs to the band it starts");
        assertEquals("r02-d5", SkyTiling.tileId(new SkyPosition(83.8, 90.0)),
                "the north pole clamps into band d5");
        assertEquals("r02-d0", SkyTiling.tileId(new SkyPosition(83.8, -90.0)));
        assertThrows(IllegalArgumentException.class, () -> SkyTiling.tileId(12, 0));
    }

    @Test
    void anOrdinaryRegionSelectsItsSingleTile() {
        // The default M31 query (radius ~7 degrees) sits inside one tile.
        List<String> tiles = SkyTiling.tilesIntersecting(
                new SkyRegion(new SkyPosition(15.0, 45.0), 7.0));
        assertEquals(List.of("r00-d4"), tiles);
    }

    @Test
    void regionsAcrossTheRaWrapSelectBothSides() {
        List<String> tiles = SkyTiling.tilesIntersecting(
                new SkyRegion(new SkyPosition(0.5, 45.0), 5.0));
        assertTrue(tiles.contains("r00-d4"));
        assertTrue(tiles.contains("r11-d4"), "the wrap neighbour must be selected");
    }

    @Test
    void regionsOnADeclinationBoundarySelectBothBands() {
        List<String> tiles = SkyTiling.tilesIntersecting(
                new SkyRegion(new SkyPosition(15.0, 29.0), 3.0));
        assertTrue(tiles.contains("r00-d3"));
        assertTrue(tiles.contains("r00-d4"));
    }

    @Test
    void aPolarRegionSelectsEveryTopBandTile() {
        List<String> tiles = SkyTiling.tilesIntersecting(
                new SkyRegion(new SkyPosition(10.0, 89.0), 2.0));
        for (int raIndex = 0; raIndex < SkyTiling.RA_TILES; raIndex++) {
            assertTrue(tiles.contains(SkyTiling.tileId(raIndex, 5)),
                    "a cone over the pole touches every RA column of the top band");
        }
        assertTrue(tiles.stream().allMatch(id -> id.endsWith("-d5")),
                "nothing below the top band is nearby");
    }

    @Test
    void selectionIsCompleteForHomeTilesOfContainedPositions() {
        // Any position inside the region must live in a selected tile -
        // the completeness half of the conservative-selection contract.
        SkyRegion region = new SkyRegion(new SkyPosition(29.9, 59.8), 6.0);
        List<String> tiles = SkyTiling.tilesIntersecting(region);
        for (double ra = 20; ra <= 40; ra += 0.7) {
            for (double dec = 52; dec <= 66; dec += 0.7) {
                SkyPosition position = new SkyPosition(ra, dec);
                if (region.contains(position)) {
                    assertTrue(tiles.contains(SkyTiling.tileId(position)),
                            "home tile of contained " + ra + "," + dec + " missing");
                }
            }
        }
    }
}
