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
 * Tile selection for a cone query is deliberately conservative: a tile is
 * selected when the clamped nearest point of its bounds lies within the
 * query radius plus {@link #SELECTION_PADDING_DEGREES}. Over-selection only
 * costs reading a small extra tile; under-selection is prevented by the
 * padding, and row-level region filtering keeps correctness independent of
 * selection precision either way.
 */
public final class SkyTiling {

    public static final double TILE_DEGREES = 30.0;
    public static final int RA_TILES = 12;
    public static final int DEC_BANDS = 6;

    /** Covers the clamped-nearest-point approximation on the sphere. */
    public static final double SELECTION_PADDING_DEGREES = 0.5;

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
                if (region.centre().separationDegrees(
                        nearestBoundPoint(region.centre(), raIndex, decIndex)) <= reach) {
                    tiles.add(tileId(raIndex, decIndex));
                }
            }
        }
        return List.copyOf(tiles);
    }

    /** The clamped nearest point of a tile's bounds to a position. */
    private static SkyPosition nearestBoundPoint(SkyPosition from, int raIndex, int decIndex) {
        double decLow = -90.0 + decIndex * TILE_DEGREES;
        double decHigh = decLow + TILE_DEGREES;
        double dec = Math.max(decLow, Math.min(decHigh, from.decDegrees()));

        double raLow = raIndex * TILE_DEGREES;
        double raHigh = raLow + TILE_DEGREES;
        double ra = from.raDegrees();
        if (ra < raLow || ra >= raHigh) {
            // Outside the column: clamp to the circularly nearer edge.
            double toLow = circularDistance(ra, raLow);
            double toHigh = circularDistance(ra, raHigh % 360.0);
            ra = toLow <= toHigh ? raLow : raHigh % 360.0;
        }
        return new SkyPosition(ra, dec);
    }

    private static double circularDistance(double a, double b) {
        double difference = Math.abs(a - b) % 360.0;
        return Math.min(difference, 360.0 - difference);
    }
}
