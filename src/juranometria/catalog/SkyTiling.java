package juranometria.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

/**
 * The deterministic all-sky partition from docs/decisions/all-sky-tiling.md:
 * a fixed 30-degree grid of 12 right-ascension columns by 6 declination
 * bands, 72 tiles named {@code r00-d0} through {@code r11-d5} (d0 starts at
 * the south pole). Every object lives in exactly one home tile, chosen by
 * its centre position, so queries can never produce duplicates.
 *
 * Tile selection for a cone query is conservative with a provable
 * completeness bound. A tile is selected when the query centre lies inside
 * it, or when any sample of its boundary lies within the query radius plus
 * {@link #SELECTION_PADDING_DEGREES}. The boundary is sampled every degree
 * along its edges, so consecutive samples are at most one degree of path
 * apart (exactly one degree along meridians, and one degree times
 * cos(dec) along parallels); the true nearest boundary point is therefore
 * within half a degree of some sample, and by the triangle inequality any
 * intersecting tile has a sample within radius + 0.5°. Independent RA/Dec
 * clamping is deliberately not used: near the poles right-ascension lines
 * converge and the nearest route into a tile runs through higher latitude,
 * which clamping misses (Codex review, Sprint 5). Over-selection only
 * costs reading a small extra tile; row-level region filtering keeps
 * correctness independent of selection precision either way.
 */
public final class SkyTiling {

    public static final double TILE_DEGREES = 30.0;
    public static final int RA_TILES = 12;
    public static final int DEC_BANDS = 6;

    /** Half the boundary sampling step; see the class contract. */
    public static final double SELECTION_PADDING_DEGREES = 0.5;

    /** Boundary sampling step in degrees of path along each edge. */
    private static final double BOUNDARY_SAMPLE_STEP_DEGREES = 1.0;

    private SkyTiling() {
    }

    /** The home tile of a position; the north pole belongs to band d5. */
    public static String tileId(SkyPosition position) {
        int raIndex = (int) (position.raDegrees() / TILE_DEGREES) % RA_TILES;
        int decIndex = Math.min(DEC_BANDS - 1,
                (int) ((position.decDegrees() + 90.0) / TILE_DEGREES));
        return tileId(raIndex, decIndex);
    }

    public static String tileId(int raIndex, int decIndex) {
        if (raIndex < 0 || raIndex >= RA_TILES || decIndex < 0 || decIndex >= DEC_BANDS) {
            throw new IllegalArgumentException(
                    "tile indices out of range: r" + raIndex + " d" + decIndex);
        }
        return String.format(Locale.ROOT, "r%02d-d%d", raIndex, decIndex);
    }

    /** Tiles intersecting the region, in stable r-then-d identity order. */
    public static List<String> tilesIntersecting(SkyRegion region) {
        List<String> tiles = new ArrayList<>();
        double reach = region.radiusDegrees() + SELECTION_PADDING_DEGREES;
        for (int raIndex = 0; raIndex < RA_TILES; raIndex++) {
            for (int decIndex = 0; decIndex < DEC_BANDS; decIndex++) {
                if (intersects(region.centre(), reach, raIndex, decIndex)) {
                    tiles.add(tileId(raIndex, decIndex));
                }
            }
        }
        return List.copyOf(tiles);
    }

    private static boolean intersects(SkyPosition centre, double reachDegrees,
                                      int raIndex, int decIndex) {
        double decLow = -90.0 + decIndex * TILE_DEGREES;
        double decHigh = decLow + TILE_DEGREES;
        double raLow = raIndex * TILE_DEGREES;
        double raHigh = raLow + TILE_DEGREES;

        if (centre.raDegrees() >= raLow && centre.raDegrees() < raHigh
                && centre.decDegrees() >= decLow && centre.decDegrees() <= decHigh) {
            return true;
        }
        // Sound quick reject: separation is at least the declination gap.
        if (centre.decDegrees() < decLow - reachDegrees
                || centre.decDegrees() > decHigh + reachDegrees) {
            return false;
        }
        // Meridian edges: one sample per degree of declination.
        for (double dec = decLow; dec <= decHigh + 1e-9;
                dec += BOUNDARY_SAMPLE_STEP_DEGREES) {
            double clampedDec = Math.min(90.0, dec);
            if (within(centre, raLow, clampedDec, reachDegrees)
                    || within(centre, raHigh % 360.0, clampedDec, reachDegrees)) {
                return true;
            }
        }
        // Parallel edges: one sample per degree of right ascension, which is
        // at most one degree of path (shorter toward the poles).
        for (double ra = raLow; ra <= raHigh + 1e-9;
                ra += BOUNDARY_SAMPLE_STEP_DEGREES) {
            double wrapped = ra % 360.0;
            if (within(centre, wrapped, decLow, reachDegrees)
                    || within(centre, wrapped, decHigh, reachDegrees)) {
                return true;
            }
        }
        return false;
    }

    private static boolean within(SkyPosition centre, double raDegrees,
                                  double decDegrees, double reachDegrees) {
        return centre.separationDegrees(
                new SkyPosition(raDegrees, decDegrees)) <= reachDegrees;
    }
}
