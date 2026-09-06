package juranometria.tool;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

/**
 * A stereographic projection, for the Sprint 29 gate to measure
 * against the atlas's gnomonic one (issue #283).
 *
 * <p><strong>Study only.</strong> It lives in the tool package
 * deliberately: the gate must change no production behaviour, and
 * nothing decides to adopt this until the measurements say so. If
 * the gate chooses it, #284 moves a reviewed implementation into
 * {@code juranometria.project}; if not, this is deleted with the
 * study that needed it.
 *
 * <p>Projected from the point opposite the centre, so
 * {@code r = 2 tan(theta/2)}: conformal, defined everywhere except
 * the antipode, and mapping great circles to <em>circles</em> rather
 * than to straight lines. That last property is the one the gate
 * exists to price.
 */
public final class StereographicCandidate {

    /** Nothing projects from the antipode itself. */
    private static final double MIN_DENOMINATOR = 1e-12;

    private final SkyPosition centre;
    private final double centreRaRadians;
    private final double sinCentreDec;
    private final double cosCentreDec;

    public StereographicCandidate(SkyPosition centre) {
        this.centre = centre;
        this.centreRaRadians = Math.toRadians(centre.raDegrees());
        double centreDec = Math.toRadians(centre.decDegrees());
        this.sinCentreDec = Math.sin(centreDec);
        this.cosCentreDec = Math.cos(centreDec);
    }

    public SkyPosition centre() {
        return centre;
    }

    /**
     * Projects onto the plane, in units where a point an angle
     * {@code theta} from the centre lies at {@code 2 tan(theta/2)}.
     */
    public Optional<PlanePoint> project(SkyPosition position) {
        double raOffset =
                Math.toRadians(position.raDegrees()) - centreRaRadians;
        double dec = Math.toRadians(position.decDegrees());
        double sinDec = Math.sin(dec);
        double cosDec = Math.cos(dec);
        double cosRaOffset = Math.cos(raOffset);

        double denominator = 1.0 + sinCentreDec * sinDec
                + cosCentreDec * cosDec * cosRaOffset;
        if (denominator < MIN_DENOMINATOR) {
            return Optional.empty();
        }
        double k = 2.0 / denominator;
        return Optional.of(new PlanePoint(
                k * cosDec * Math.sin(raOffset),
                k * (cosCentreDec * sinDec
                        - sinCentreDec * cosDec * cosRaOffset)));
    }

    /**
     * Plane units per pixel so that the page's horizontal half-width
     * holds exactly half the field, as the gnomonic mapping does.
     *
     * <p>Stated here rather than reused from {@code ViewportMapping},
     * whose scale is {@code tan} and therefore gnomonic's. Both
     * candidates put the same sky at the left and right edges, which
     * is what makes the comparison a comparison.
     */
    public static double pixelsPerPlaneUnit(int widthPx,
                                            double fieldWidthDegrees) {
        double halfField = Math.toRadians(fieldWidthDegrees) / 2.0;
        return widthPx / (2.0 * 2.0 * Math.tan(halfField / 2.0));
    }
}
