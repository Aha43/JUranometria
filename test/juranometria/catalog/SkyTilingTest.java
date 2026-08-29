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
    void theSouthPoleCounterexampleIsSelected() {
        // Codex review, Sprint 5 (verbatim): a 2-degree query at
        // (0.0, -88.5) contains (129.845078, -89.325920), whose home tile
        // is r04-d0; independent RA/Dec clamping put the tile 2.598 degrees
        // away and skipped it. Polar RA convergence means the nearest route
        // into the tile runs through higher latitude.
        // The review's coordinates were approximate and land 0.03 mas
        // outside the radius; the declination is nudged one arcsecond
        // further south to sit just inside it.
        SkyRegion region = new SkyRegion(new SkyPosition(0.0, -88.5), 2.0);
        SkyPosition contained = new SkyPosition(129.845078, -89.326200);
        assertTrue(region.contains(contained), "the counterexample point is in the query");
        assertEquals("r04-d0", SkyTiling.tileId(contained));
        assertTrue(SkyTiling.tilesIntersecting(region).contains("r04-d0"),
                "the contained point's home tile must be selected");
    }

    @Test
    void selectionIsCompleteAcrossTheWholeSkyAtRepresentativeRadii() {
        // For every sampled query, every contained position's home tile
        // must be selected - swept over both poles, the RA wrap, and RA/Dec
        // boundary crossings, at the query radii the assembler produces.
        double[] radii = {0.5, 2.0, 7.06, 12.0};
        double[] centreRas = {0.0, 14.9, 29.95, 195.5, 359.9};
        double[] centreDecs = {-89.5, -88.5, -60.05, -59.9, -30.0, -0.05,
                29.95, 45.0, 59.9, 88.5, 89.5};
        int checked = 0;
        for (double radius : radii) {
            for (double centreRa : centreRas) {
                for (double centreDec : centreDecs) {
                    SkyRegion region = new SkyRegion(
                            new SkyPosition(centreRa, centreDec), radius);
                    List<String> tiles = SkyTiling.tilesIntersecting(region);
                    for (double bearing = 0; bearing < 360; bearing += 30) {
                        for (double fraction : new double[] {0.35, 0.9, 0.999}) {
                            SkyPosition position = destination(
                                    region.centre(), bearing, radius * fraction);
                            if (region.contains(position)) {
                                assertTrue(tiles.contains(SkyTiling.tileId(position)),
                                        "home tile missing for " + position
                                                + " in " + region);
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 5000, "the sweep must exercise a real sample");
    }

    /** Great-circle destination from a start along a bearing (degrees). */
    private static SkyPosition destination(SkyPosition from, double bearingDegrees,
                                           double distanceDegrees) {
        double lat1 = Math.toRadians(from.decDegrees());
        double lon1 = Math.toRadians(from.raDegrees());
        double bearing = Math.toRadians(bearingDegrees);
        double distance = Math.toRadians(distanceDegrees);
        double sinLat2 = Math.sin(lat1) * Math.cos(distance)
                + Math.cos(lat1) * Math.sin(distance) * Math.cos(bearing);
        double lat2 = Math.asin(Math.max(-1.0, Math.min(1.0, sinLat2)));
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearing) * Math.sin(distance) * Math.cos(lat1),
                Math.cos(distance) - Math.sin(lat1) * sinLat2);
        double ra = ((Math.toDegrees(lon2) % 360.0) + 360.0) % 360.0;
        double dec = Math.max(-90.0, Math.min(90.0, Math.toDegrees(lat2)));
        return new SkyPosition(ra, dec);
    }
}
