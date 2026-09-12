package juranometria.tool.globe;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.PanSolver;
import juranometria.project.ViewportMapping;

/**
 * What a pixel is worth on a globe (Sprint 32, issue #301).
 *
 * <p>Sprint 30 refused the orthographic projection for the chart
 * ladder partly because <em>it cannot be pointed at</em>: a page unit
 * at the corner of a 90-degree page covers twelve arcminutes of sky
 * against four at the centre, and it goes on coarsening all the way to
 * the limb. This measures how far, and in which direction.
 *
 * <p><strong>Radially and tangentially apart</strong>, because the
 * distortion is directional and one "arcminutes per pixel" figure
 * would hide it. On {@code r = sin(theta)} a tangential step should
 * cost a constant arc of sky while a radial one goes as
 * {@code 1 / cos(theta)} and diverges at the limb - so a reader can
 * point along the limb about as well as anywhere, and across it not at
 * all. That is the expectation; what follows is the measurement,
 * taken through the production projection and the production mapping
 * rather than from the algebra.
 *
 * <p>Pointing at <em>ink</em> is a different question and is not asked
 * here: clicking a drawn mark identifies a catalogue object outright,
 * however coarse the coordinate inversion is at that place.
 */
public final class GlobePointingStudyMain {

    private GlobePointingStudyMain() {
    }

    static final File DIR = new File("build/globe-study/pointing");

    private static final int SIDE_PX = 900;
    private static final double FRAME = 0.90;

    /** Where on the disc the pointer is asked about. */
    private static final double[] RADII =
            {0.0, 0.2, 0.5, 0.8, 0.9, 0.95, 0.99, 0.999, 1.0};

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        SkyPosition centre = new SkyPosition(266.0, -28.0);
        DrawnPage page = Atlas.assembler().assembleForStudy(
                centre, 180.0, 5.0, "Sagittarius",
                new GlobeProjection(centre), SIDE_PX, SIDE_PX);
        ViewportMapping mapping = new ViewportMapping(page);
        double discRadius = FRAME * SIDE_PX / 2.0;
        double middle = SIDE_PX / 2.0;

        System.out.println("# What a pixel is worth on a globe");
        System.out.println();
        System.out.println("Measured through the production"
                + " projection and mapping, radially and");
        System.out.println("tangentially apart. Pointing at drawn ink"
                + " is a different question and is not");
        System.out.println("asked here.");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "%8s %7s %9s %9s %9s %9s %9s %9s %8s%n",
                "radius", "sky out", "radial", "worst",
                "tangent", "worst", "spread", "worst", "no sky");

        for (double radius : RADII) {
            sample(page, mapping, centre, discRadius, middle, radius);
        }
        System.out.println();
        System.out.println("A dash where the inverse has no answer:"
                + " that pixel is not sky, and a reader");
        System.out.println("pointing at it is pointing past the edge"
                + " of the world.");

        System.out.println();
        System.out.println("## Recentring");
        System.out.println();
        System.out.println("The centre a click asks for is the sky"
                + " under it, so how far the centre moves per");
        System.out.println("pixel of input is the table above."
                + " Reversal is asked separately: recentre on a");
        System.out.println("point, then click where the old centre"
                + " now lies and see what returns.");
        System.out.println();
        System.out.printf(Locale.ROOT, "%8s %14s %16s%n",
                "radius", "reversal", "returned");
        for (double radius : RADII) {
            reversal(page, mapping, centre, discRadius, middle, radius);
        }

        System.out.println();
        System.out.println("## Dragging to pan");
        System.out.println();
        System.out.println("Not measurable here, and that is the"
                + " finding: PanSolver.solveCentre takes a");
        System.out.println("ChartProjection kind, so there is no way"
                + " to ask it about a globe at all. The");
        System.out.println("solver is keyed on the enum rather than"
                + " on a projection. Giving it the page, as");
        System.out.println("the rest of the render path now takes"
                + " one, belongs to #330.");
    }

    /**
     * One radius, sampled around the disc and across the pixel grid.
     *
     * <p>A single sample is a fortunate or unfortunate pixel: where
     * the exact point falls between pixel centres changes the round
     * trip entirely, which is why the first run of this study
     * reported round-trip errors that rose and fell with no pattern.
     * So each radius is asked at several azimuths and several
     * sub-pixel phases, and what is reported is the median and the
     * worst rather than whichever pixel came first.
     */
    private static void sample(DrawnPage page, ViewportMapping mapping,
                               SkyPosition centre, double discRadius,
                               double middle, double radius) {
        List<Double> radial = new java.util.ArrayList<>();
        List<Double> tangent = new java.util.ArrayList<>();
        List<Double> spread = new java.util.ArrayList<>();
        List<Double> trip = new java.util.ArrayList<>();
        int noSky = 0;
        int asked = 0;

        for (int turn = 0; turn < AZIMUTHS; turn++) {
            double angle = 2.0 * Math.PI * turn / AZIMUTHS;
            for (double phase : PHASES) {
                asked++;
                double away = radius * discRadius + phase;
                PixelPoint at = new PixelPoint(
                        middle + away * Math.cos(angle),
                        middle + away * Math.sin(angle));
                SkyPosition here = skyAt(page, at);
                if (here == null) {
                    noSky++;
                    continue;
                }
                double outX = Math.cos(angle);
                double outY = Math.sin(angle);
                Double out = stepDegrees(page, here, new PixelPoint(
                        at.x() + outX, at.y() + outY));
                Double along = stepDegrees(page, here, new PixelPoint(
                        at.x() - outY, at.y() + outX));
                if (out != null) {
                    radial.add(out);
                }
                if (along != null) {
                    tangent.add(along);
                }
                Double covered = spreadDegrees(page, at);
                if (covered != null) {
                    spread.add(covered);
                } else {
                    noSky++;
                }
                Double returned = roundTrip(page, mapping, here);
                if (returned != null) {
                    trip.add(returned);
                }
            }
        }

        SkyPosition at = skyAt(page, new PixelPoint(
                middle + radius * discRadius, middle));
        System.out.printf(Locale.ROOT,
                "%8.3f %7s %9s %9s %9s %9s %9s %9s %6d/%d%n",
                radius,
                at == null ? "-" : String.format(Locale.ROOT, "%.2f",
                        centre.separationDegrees(at)),
                arcmin(median(radial)), arcmin(worst(radial)),
                arcmin(median(tangent)), arcmin(worst(tangent)),
                arcmin(median(spread)), arcmin(worst(spread)),
                noSky, asked);
    }

    /**
     * Recentre on a point, then ask for the old centre back.
     *
     * <p>A gesture a reader cannot undo is a gesture they will not
     * trust. The old centre is projected onto the page the recentring
     * made, snapped to the pixel a click would carry, and unprojected:
     * what comes back is where the reader would land trying to return.
     */
    private static void reversal(DrawnPage page, ViewportMapping mapping,
                                 SkyPosition centre, double discRadius,
                                 double middle, double radius) {
        PixelPoint at = new PixelPoint(
                middle + radius * discRadius, middle);
        SkyPosition asked = skyAt(page, at);
        if (asked == null) {
            System.out.printf(Locale.ROOT, "%8.3f %14s %16s%n",
                    radius, "no sky", "refused");
            return;
        }
        // The page the recentring makes, and the old centre on it.
        GlobeProjection after = new GlobeProjection(asked);
        Optional<PlanePoint> was = after.project(centre);
        if (was.isEmpty()) {
            System.out.printf(Locale.ROOT, "%8.3f %14s %16s%n",
                    radius, "off the globe",
                    "cannot be asked for");
            return;
        }
        double perUnit = mapping.pixelsPerPlaneUnit();
        PixelPoint onNew = new PixelPoint(
                Math.round(middle - was.get().xiEast() * perUnit),
                Math.round(middle - was.get().etaNorth() * perUnit));
        PlanePoint back = new PlanePoint(
                (middle - onNew.x()) / perUnit,
                (middle - onNew.y()) / perUnit);
        Optional<SkyPosition> returned = after.unproject(back);
        System.out.printf(Locale.ROOT, "%8.3f %14s %16s%n",
                radius,
                returned.isEmpty() ? "lost"
                        : String.format(Locale.ROOT, "%.2f'",
                                centre.separationDegrees(
                                        returned.get()) * 60.0),
                returned.isEmpty() ? "-" : "yes");
    }

    private static Double median(List<Double> of) {
        if (of.isEmpty()) {
            return null;
        }
        List<Double> sorted = new java.util.ArrayList<>(of);
        sorted.sort(Double::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    private static Double worst(List<Double> of) {
        return of.isEmpty() ? null
                : of.stream().mapToDouble(Double::doubleValue).max()
                        .getAsDouble();
    }

    /** How many directions round the disc each radius is asked in. */
    private static final int AZIMUTHS = 16;

    /** Where between pixel centres the point is placed. */
    private static final double[] PHASES = {0.0, 0.25, 0.5, 0.75};

    /** The sky under a pixel, or null where there is none. */
    private static SkyPosition skyAt(DrawnPage page,
                                     PixelPoint at) {
        PlanePoint plane = PanSolver.planeFromPixel(page, at);
        Optional<SkyPosition> sky = page.projection().unproject(plane);
        return sky.orElse(null);
    }

    /** How far the sky moves for this one-pixel step. */
    private static Double stepDegrees(DrawnPage page,
                                      SkyPosition from, PixelPoint to) {
        SkyPosition there = skyAt(page, to);
        return there == null ? null : from.separationDegrees(there);
    }

    /**
     * How much sky the pointer's own pixel covers: the widest
     * separation between the sky at its four corners.
     *
     * <p>Reported as unbounded when a corner has no sky at all, which
     * is what happens to a pointer straddling the limb - part of it is
     * on the globe and part of it is nowhere.
     */
    private static Double spreadDegrees(DrawnPage page,
                                        PixelPoint at) {
        List<PixelPoint> corners = List.of(
                new PixelPoint(at.x() - 0.5, at.y() - 0.5),
                new PixelPoint(at.x() + 0.5, at.y() - 0.5),
                new PixelPoint(at.x() - 0.5, at.y() + 0.5),
                new PixelPoint(at.x() + 0.5, at.y() + 0.5));
        List<SkyPosition> sky = new java.util.ArrayList<>();
        for (PixelPoint corner : corners) {
            SkyPosition one = skyAt(page, corner);
            if (one == null) {
                return null;          // part of it is not sky
            }
            sky.add(one);
        }
        double widest = 0.0;
        for (int i = 0; i < sky.size(); i++) {
            for (int j = i + 1; j < sky.size(); j++) {
                widest = Math.max(widest,
                        sky.get(i).separationDegrees(sky.get(j)));
            }
        }
        return widest;
    }

    /**
     * Sky to plane to pixel and back again, <strong>through the pixel
     * a mouse event would actually deliver</strong>.
     *
     * <p>Without the rounding this measures nothing a reader could
     * experience: a double-precision loop only proves that the same
     * arithmetic reverses itself, which the projections were already
     * shown to do to 3.3e-13 degrees in Sprint 30. A pointing device
     * reports whole pixels, so the sky is carried to a pixel, snapped
     * to the one a click would carry, and taken back - and what
     * returns is how well a reader can name the place they pointed
     * at.
     */
    private static Double roundTrip(DrawnPage page,
                                    ViewportMapping mapping,
                                    SkyPosition from) {
        Optional<PlanePoint> plane = page.projection().project(from);
        if (plane.isEmpty()) {
            return null;
        }
        PixelPoint exact = mapping.toPixel(plane.get());
        PixelPoint pixel = new PixelPoint(Math.round(exact.x()),
                Math.round(exact.y()));
        SkyPosition back = skyAt(page, pixel);
        return back == null ? null : from.separationDegrees(back);
    }

    private static String arcmin(Double degrees) {
        if (degrees == null) {
            return "no sky";
        }
        double minutes = degrees * 60.0;
        return minutes >= 100.0
                ? String.format(Locale.ROOT, "%.0f'", minutes)
                : String.format(Locale.ROOT, "%.2f'", minutes);
    }
}
