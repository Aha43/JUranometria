package juranometria.tool.allsky;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlaneConic;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;

/**
 * A whole-sky map presented through the atlas's existing
 * {@link Projection} interface, so that the #686 study door can hand
 * it to the production renderer (study only).
 *
 * <p>This class is the first half of the study's architecture
 * comparison. Every method below says whether the interface let it
 * answer honestly. The ones that could not are the interface's
 * azimuthal shape showing through, and each is a line in the
 * decision record's fitness table:
 *
 * <ul>
 * <li>{@code project}, {@code unproject}, {@code name}, {@code centre}:
 * honest.</li>
 * <li>{@code project} near the seam: the interface has one answer for
 * "no point here" (empty), and a pass that joins points uses it as
 * "stop the line". A seamed map needs a second answer - "a point, but
 * do not join it to its neighbour across the cut" - which the
 * interface cannot say. The adapter refuses a {@link #SEAM_SLIVER_DEGREES}
 * sliver either side of the seam so that joined passes break there,
 * and pays for it: anything inside the sliver is not drawn. The study
 * counts both what still crosses and what the sliver hides.</li>
 * <li>{@code planeRadius} / {@code angleAtPlaneRadius}: no honest
 * answer. A whole-sky map is not radially symmetric; how far out an
 * angle lands depends on the direction. The adapter answers a
 * declared nominal law, chosen only so the existing page sizing gives
 * a 2:1 page that holds the whole outline and the existing scene
 * assembly fetches the whole sky: {@code 0..90 -> 0..halfWidth} along
 * the half-field, then {@code 90..180 -> halfWidth..corner}, where
 * corner is the half-diagonal of the 2:1 box.</li>
 * <li>{@code limitDegrees}, {@code usefulCornerDegrees}: answered 180,
 * which is honest about reach and says nothing about where the map
 * degrades (that is direction-dependent too).</li>
 * <li>{@code visiblePlaneRadius}: answered infinity, which is a lie of
 * convenience. The map does have an edge - an ellipse or a rectangle -
 * but the interface can only say "a disc of this radius" or "no edge",
 * and a disc would clip a 2:1 outline wrongly. The page is sized so
 * the outline exactly fills the paper, and the paper rectangle clips.</li>
 * <li>{@code greatCircle}: no honest answer. On these maps a great
 * circle is not a conic, and the interface's curve vocabulary
 * (#298) is conics only. Answering empty means "does not appear", so
 * every reference circle - ecliptic, equator line, meridian, horizon -
 * and the below-horizon subdue built from the horizon vanish. The
 * study records that loss and draws those curves with study ink,
 * captioned as such.</li>
 * </ul>
 */
public final class AllSkyProjection implements Projection {

    /** How close to the seam the adapter refuses a point, in degrees of longitude. */
    public static final double SEAM_SLIVER_DEGREES = 1.0;

    private final AllSkyMap map;
    private final double halfWidth;
    private final double corner;

    public AllSkyProjection(AllSkyMap map) {
        this.map = map;
        this.halfWidth = map.form().halfWidth();
        this.corner = Math.hypot(halfWidth, halfWidth / 2.0);
    }

    public AllSkyMap map() {
        return map;
    }

    /** The reader-name identity the page words are asked for. */
    @Override
    public String name() {
        return "allsky-" + map.form().name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public SkyPosition centre() {
        return map.centre();
    }

    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        if (map.seamDistanceDegrees(position) < SEAM_SLIVER_DEGREES) {
            return Optional.empty();
        }
        double[] xy = map.project(position);
        return Optional.of(new PlanePoint(xy[0], xy[1]));
    }

    @Override
    public Optional<SkyPosition> unproject(PlanePoint point) {
        return Optional.ofNullable(map.unproject(point.xiEast(), point.etaNorth()));
    }

    @Override
    public double planeRadius(double angleDegrees) {
        if (angleDegrees <= 90.0) {
            return halfWidth * angleDegrees / 90.0;
        }
        return halfWidth + (corner - halfWidth) * (angleDegrees - 90.0) / 90.0;
    }

    @Override
    public double angleAtPlaneRadius(double planeRadius) {
        if (planeRadius <= halfWidth) {
            return 90.0 * planeRadius / halfWidth;
        }
        return Math.min(180.0, 90.0 + 90.0 * (planeRadius - halfWidth) / (corner - halfWidth));
    }

    @Override
    public double limitDegrees() {
        return 180.0;
    }

    @Override
    public double usefulCornerDegrees() {
        return 180.0;
    }

    @Override
    public double visiblePlaneRadius() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        return Optional.empty();
    }
}
