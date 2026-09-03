package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Gnomonic (tangent-plane) projection of ICRS positions onto a chart plane
 * centred on a sky position.
 *
 * Positions 90 degrees or more from the centre have no tangent-plane image
 * and project to {@link Optional#empty()}. Positions inside that hemisphere
 * always project, even when they fall outside a viewport; clipping is not a
 * projection concern.
 */
public final class GnomonicProjection {

    /**
     * Projections are undefined at 90 degrees from the centre, where the
     * denominator reaches zero; treat anything this close as unprojectable.
     */
    private static final double MIN_COS_ANGULAR_DISTANCE = 1e-12;

    private final SkyPosition centre;
    private final double centreRaRadians;
    private final double sinCentreDec;
    private final double cosCentreDec;

    public GnomonicProjection(SkyPosition centre) {
        this.centre = centre;
        this.centreRaRadians = Math.toRadians(centre.raDegrees());
        double centreDecRadians = Math.toRadians(centre.decDegrees());
        this.sinCentreDec = Math.sin(centreDecRadians);
        this.cosCentreDec = Math.cos(centreDecRadians);
    }

    /** The position the plane is tangent at. */
    public SkyPosition centre() {
        return centre;
    }

    /** Projects a sky position onto the tangent plane. */
    public Optional<PlanePoint> project(SkyPosition position) {
        double raOffset = Math.toRadians(position.raDegrees()) - centreRaRadians;
        double dec = Math.toRadians(position.decDegrees());
        double sinDec = Math.sin(dec);
        double cosDec = Math.cos(dec);

        double cosAngularDistance =
                sinCentreDec * sinDec + cosCentreDec * cosDec * Math.cos(raOffset);
        if (cosAngularDistance < MIN_COS_ANGULAR_DISTANCE) {
            return Optional.empty();
        }

        double xiEast = cosDec * Math.sin(raOffset) / cosAngularDistance;
        double etaNorth =
                (cosCentreDec * sinDec - sinCentreDec * cosDec * Math.cos(raOffset))
                        / cosAngularDistance;
        return Optional.of(new PlanePoint(xiEast, etaNorth));
    }
}
