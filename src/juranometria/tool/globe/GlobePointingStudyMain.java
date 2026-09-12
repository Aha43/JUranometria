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
                "%8s %8s %12s %12s %14s %12s%n",
                "radius", "sky out", "1px radial", "1px tangent",
                "pixel spread", "round trip");

        for (double radius : RADII) {
            PixelPoint at = new PixelPoint(
                    middle + radius * discRadius, middle);
            SkyPosition here = skyAt(page, mapping, at);
            if (here == null) {
                System.out.printf(Locale.ROOT,
                        "%8.3f %8s %12s %12s %14s %12s%n",
                        radius, "-", "no sky", "no sky", "no sky",
                        "-");
                continue;
            }
            double out = centre.separationDegrees(here);

            Double radial = stepDegrees(page, mapping, here,
                    new PixelPoint(at.x() + 1.0, at.y()));
            Double tangent = stepDegrees(page, mapping, here,
                    new PixelPoint(at.x(), at.y() + 1.0));
            String spread = spreadDegrees(page, mapping, at);
            String trip = roundTrip(page, mapping, here);

            System.out.printf(Locale.ROOT,
                    "%8.3f %7.2f° %12s %12s %14s %12s%n",
                    radius, out, arcmin(radial), arcmin(tangent),
                    spread, trip);
        }
        System.out.println();
        System.out.println("A dash where the inverse has no answer:"
                + " that pixel is not sky, and a reader");
        System.out.println("pointing at it is pointing past the edge"
                + " of the world.");
    }

    /** The sky under a pixel, or null where there is none. */
    private static SkyPosition skyAt(DrawnPage page,
                                     ViewportMapping mapping,
                                     PixelPoint at) {
        PlanePoint plane = PanSolver.planeFromPixel(page, at);
        Optional<SkyPosition> sky = page.projection().unproject(plane);
        return sky.orElse(null);
    }

    /** How far the sky moves for this one-pixel step. */
    private static Double stepDegrees(DrawnPage page,
                                      ViewportMapping mapping,
                                      SkyPosition from, PixelPoint to) {
        SkyPosition there = skyAt(page, mapping, to);
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
    private static String spreadDegrees(DrawnPage page,
                                        ViewportMapping mapping,
                                        PixelPoint at) {
        List<PixelPoint> corners = List.of(
                new PixelPoint(at.x() - 0.5, at.y() - 0.5),
                new PixelPoint(at.x() + 0.5, at.y() - 0.5),
                new PixelPoint(at.x() - 0.5, at.y() + 0.5),
                new PixelPoint(at.x() + 0.5, at.y() + 0.5));
        List<SkyPosition> sky = new java.util.ArrayList<>();
        for (PixelPoint corner : corners) {
            SkyPosition one = skyAt(page, mapping, corner);
            if (one == null) {
                return "past the limb";
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
        return arcmin(widest);
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
    private static String roundTrip(DrawnPage page,
                                    ViewportMapping mapping,
                                    SkyPosition from) {
        Optional<PlanePoint> plane = page.projection().project(from);
        if (plane.isEmpty()) {
            return "-";
        }
        PixelPoint exact = mapping.toPixel(plane.get());
        PixelPoint pixel = new PixelPoint(Math.round(exact.x()),
                Math.round(exact.y()));
        SkyPosition back = skyAt(page, mapping, pixel);
        if (back == null) {
            return "lost";
        }
        double error = from.separationDegrees(back) * 3600.0;
        return String.format(Locale.ROOT, "%.2e\"", error);
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
