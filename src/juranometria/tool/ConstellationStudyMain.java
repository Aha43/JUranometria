package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;
import juranometria.ui.SceneAssembler;

/**
 * The Sprint 7 constellation-geography study (issue #63): renders the
 * candidate line figures, boundaries, and names over real charts at the
 * released fields, through the real projection and page geometry, and
 * prints the measurements the decision needs - including RA-wrap,
 * polar, and crossing-segment worked examples.
 *
 * Reads the pinned d3-celestial GeoJSON sources from
 * imports/raw/constellations (see scripts/download-constellation-sources.sh);
 * run via "make constellation-study"; charts land in
 * build/constellation-study/.
 */
public final class ConstellationStudyMain {

    private static final double[] FIELDS = {8.0, 12.0, 18.0, 24.0, 36.0};
    private static final double LIMIT_V = 8.0;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** Overlay ink, deliberately quieter than star ink. */
    private static final Color LINE_INK = new Color(120, 120, 120);
    private static final Color BOUNDARY_INK = new Color(190, 190, 190);
    private static final Color NAME_INK = new Color(120, 120, 120);
    private static final java.awt.Stroke LINE_STROKE = new BasicStroke(1.0f);
    private static final java.awt.Stroke BOUNDARY_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {1.0f, 3.0f}, 0.0f);
    private static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    /** Subdivision step along overlay segments, degrees on the sky. */
    private static final double SEGMENT_STEP_DEGREES = 0.5;

    private record Target(String name, SkyPosition centre) {
    }

    private static final List<Target> TARGETS = List.of(
            new Target("m42", new SkyPosition(83.818667, -5.389667)),
            new Target("m31", new SkyPosition(10.684708, 41.268750)),
            new Target("m45", new SkyPosition(56.869167, 24.105278)),
            new Target("rawrap", new SkyPosition(359.457625, -32.591028)),
            new Target("polar", new SkyPosition(37.946619, 89.264135)));

    record Segment(String constellation, SkyPosition from, SkyPosition to) {
    }

    record Name(String constellation, String latin, int rank, SkyPosition anchor) {
    }

    private final List<Segment> figureSegments;
    private final List<Segment> boundarySegments;
    private final List<Name> names;

    private ConstellationStudyMain(List<Segment> figureSegments,
                                   List<Segment> boundarySegments,
                                   List<Name> names) {
        this.figureSegments = figureSegments;
        this.boundarySegments = boundarySegments;
        this.names = names;
    }

    public static void main(String[] args) throws Exception {
        File rawDir = new File(args.length > 0 ? args[0] : "imports/raw/constellations");
        File outDir = new File("build/constellation-study");
        outDir.mkdirs();

        ConstellationStudyMain study = new ConstellationStudyMain(
                loadSegments(new File(rawDir, "constellations.lines.json")),
                loadBoundaries(new File(rawDir, "constellations.bounds.json")),
                loadNames(new File(rawDir, "constellations.json")));
        System.out.printf(Locale.ROOT,
                "loaded: %d figure segments, %d boundary corner segments,"
                        + " %d names%n%n",
                study.figureSegments.size(), study.boundarySegments.size(),
                study.names.size());

        TiledCatalogue catalogue = TiledCatalogue.load();
        SceneAssembler assembler = SceneAssembler.allSky(
                catalogue, catalogue.manifest().maxObjectSemiExtentDegrees());
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        System.out.printf(Locale.ROOT, "%-7s %5s | %7s %7s %6s %6s | %5s%n",
                "target", "field", "figSeg", "bndSeg", "nameB", "nameA", "rnd");
        for (Target target : TARGETS) {
            for (double field : FIELDS) {
                study.study(assembler, renderer, target, field, outDir);
            }
            System.out.println();
        }
        study.workedExamples();
        System.out.println("Charts written to " + outDir);
    }

    private void study(SceneAssembler assembler, ChartRenderer renderer,
                       Target target, double field, File outDir) throws Exception {
        ChartViewState state = new ChartViewState(
                target.centre(), field, LIMIT_V, null, null);
        ChartScene scene = assembler.assemble(state, WIDTH, HEIGHT);
        var image = renderer.renderToImage(scene);
        GnomonicProjection projection = new GnomonicProjection(target.centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());

        long r0 = System.nanoTime();
        Graphics2D g = image.createGraphics();
        int figures;
        int boundaries;
        int labels;
        int anchorLabels;
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setClip(1, 1, WIDTH - 2, HEIGHT - 2);
            g.setColor(BOUNDARY_INK);
            g.setStroke(BOUNDARY_STROKE);
            boundaries = drawSegments(g, boundarySegments, projection, mapping);
            g.setColor(LINE_INK);
            g.setStroke(LINE_STROKE);
            figures = drawSegments(g, figureSegments, projection, mapping);
            labels = drawNames(g, projection, mapping);
            anchorLabels = countAnchorNames(projection, mapping);
        } finally {
            g.dispose();
        }
        long r1 = System.nanoTime();

        System.out.printf(Locale.ROOT, "%-7s %4.0f° | %7d %7d %6d %6d | %4.1f%n",
                target.name(), field, figures, boundaries, labels, anchorLabels,
                (r1 - r0) / 1e6);
        ImageIO.write(image, "png", new File(outDir, String.format(
                Locale.ROOT, "%s-%02.0fdeg.png", target.name(), field)));
    }

    /**
     * Draws each segment subdivided along the sky so segments whose
     * endpoints lie outside the page - or behind the tangent plane -
     * still contribute exactly their visible portion. Returns how many
     * segments left any ink inside the frame.
     */
    private static int drawSegments(Graphics2D g, List<Segment> segments,
                                    GnomonicProjection projection,
                                    ViewportMapping mapping) {
        int visible = 0;
        for (Segment segment : segments) {
            boolean inked = false;
            PixelPoint previous = null;
            int steps = Math.max(1, (int) Math.ceil(
                    angularSeparationDegrees(segment.from(), segment.to())
                            / SEGMENT_STEP_DEGREES));
            for (int i = 0; i <= steps; i++) {
                Optional<PlanePoint> plane = projection.project(
                        interpolate(segment.from(), segment.to(),
                                (double) i / steps));
                if (plane.isEmpty()) {
                    previous = null;
                    continue;
                }
                PixelPoint pixel = mapping.toPixel(plane.get());
                if (previous != null) {
                    if (onPage(previous) || onPage(pixel)) {
                        g.draw(new java.awt.geom.Line2D.Double(
                                previous.x(), previous.y(), pixel.x(), pixel.y()));
                        inked = true;
                    }
                }
                previous = pixel;
            }
            if (inked) {
                visible++;
            }
        }
        return visible;
    }

    /**
     * Candidate naming policy B: a constellation is named when its figure
     * leaves ink on the page, at the centroid of the visible figure
     * samples - deterministic, and the name always sits on the visible
     * part of its constellation. (Candidate A - the source's fixed label
     * anchor, drawn only when the anchor itself is on the page - loses
     * ORION on every M42-centred field, its anchor at dec +13 lying
     * off-page; both candidates' counts are printed for the decision.)
     */
    private int drawNames(Graphics2D g, GnomonicProjection projection,
                          ViewportMapping mapping) {
        g.setFont(NAME_FONT);
        g.setColor(NAME_INK);
        java.util.Map<String, double[]> visible = new java.util.LinkedHashMap<>();
        for (Segment segment : figureSegments) {
            int steps = Math.max(1, (int) Math.ceil(
                    angularSeparationDegrees(segment.from(), segment.to())
                            / SEGMENT_STEP_DEGREES));
            for (int i = 0; i <= steps; i++) {
                Optional<PlanePoint> plane = projection.project(
                        interpolate(segment.from(), segment.to(),
                                (double) i / steps));
                if (plane.isEmpty()) {
                    continue;
                }
                PixelPoint pixel = mapping.toPixel(plane.get());
                if (!onPage(pixel)) {
                    continue;
                }
                double[] sum = visible.computeIfAbsent(
                        segment.constellation(), key -> new double[3]);
                sum[0] += pixel.x();
                sum[1] += pixel.y();
                sum[2] += 1.0;
            }
        }
        int drawn = 0;
        for (Name name : names) {
            double[] sum = visible.get(name.constellation());
            if (sum == null) {
                continue;
            }
            String text = name.latin().toUpperCase(Locale.ROOT);
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (float) (sum[0] / sum[2] - width / 2.0),
                    (float) (sum[1] / sum[2]));
            drawn++;
        }
        return drawn;
    }

    /** Candidate naming policy A: the source anchor, when itself on page. */
    private int countAnchorNames(GnomonicProjection projection,
                                 ViewportMapping mapping) {
        int drawn = 0;
        for (Name name : names) {
            Optional<PlanePoint> plane = projection.project(name.anchor());
            if (plane.isPresent() && onPage(mapping.toPixel(plane.get()))) {
                drawn++;
            }
        }
        return drawn;
    }

    private static boolean onPage(PixelPoint pixel) {
        return pixel.x() >= 0 && pixel.x() < WIDTH
                && pixel.y() >= 0 && pixel.y() < HEIGHT;
    }

    /** Spherical linear interpolation between two sky positions. */
    static SkyPosition interpolate(SkyPosition from, SkyPosition to, double t) {
        double[] a = unit(from);
        double[] b = unit(to);
        double dot = Math.clamp(a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1.0, 1.0);
        double omega = Math.acos(dot);
        double sa;
        double sb;
        if (omega < 1e-9) {
            sa = 1.0 - t;
            sb = t;
        } else {
            sa = Math.sin((1.0 - t) * omega) / Math.sin(omega);
            sb = Math.sin(t * omega) / Math.sin(omega);
        }
        double x = sa * a[0] + sb * b[0];
        double y = sa * a[1] + sb * b[1];
        double z = sa * a[2] + sb * b[2];
        double ra = Math.toDegrees(Math.atan2(y, x));
        return new SkyPosition((ra + 360.0) % 360.0,
                Math.toDegrees(Math.asin(Math.clamp(z, -1.0, 1.0))));
    }

    static double angularSeparationDegrees(SkyPosition from, SkyPosition to) {
        double[] a = unit(from);
        double[] b = unit(to);
        return Math.toDegrees(Math.acos(Math.clamp(
                a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1.0, 1.0)));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    // ------------------------------------------------------------------
    // Source loading: d3-celestial GeoJSON, longitude -180..180 -> RA.

    private static double toRa(double longitude) {
        return (longitude + 360.0) % 360.0;
    }

    private static List<Segment> loadSegments(File file) throws Exception {
        List<Segment> segments = new ArrayList<>();
        for (Object feature : features(file)) {
            Map<String, Object> f = MiniJson.object(feature);
            String id = (String) f.get("id");
            Map<String, Object> geometry = MiniJson.object(f.get("geometry"));
            for (Object lineObject : MiniJson.array(geometry.get("coordinates"))) {
                List<Object> line = MiniJson.array(lineObject);
                for (int i = 1; i < line.size(); i++) {
                    segments.add(new Segment(id,
                            point(line.get(i - 1)), point(line.get(i))));
                }
            }
        }
        return segments;
    }

    private static List<Segment> loadBoundaries(File file) throws Exception {
        List<Segment> segments = new ArrayList<>();
        for (Object feature : features(file)) {
            Map<String, Object> f = MiniJson.object(feature);
            String id = (String) f.get("id");
            Map<String, Object> geometry = MiniJson.object(f.get("geometry"));
            for (Object ringObject : MiniJson.array(geometry.get("coordinates"))) {
                List<Object> ring = MiniJson.array(ringObject);
                for (int i = 1; i < ring.size(); i++) {
                    segments.add(new Segment(id,
                            point(ring.get(i - 1)), point(ring.get(i))));
                }
            }
        }
        return segments;
    }

    private static List<Name> loadNames(File file) throws Exception {
        List<Name> names = new ArrayList<>();
        for (Object feature : features(file)) {
            Map<String, Object> f = MiniJson.object(feature);
            Map<String, Object> properties = MiniJson.object(f.get("properties"));
            Map<String, Object> geometry = MiniJson.object(f.get("geometry"));
            names.add(new Name((String) f.get("id"),
                    (String) properties.get("la"),
                    Integer.parseInt((String) properties.get("rank")),
                    point(MiniJson.array(geometry.get("coordinates")))));
        }
        return names;
    }

    private static List<Object> features(File file) throws Exception {
        Map<String, Object> root = MiniJson.object(
                MiniJson.parse(Files.readString(file.toPath())));
        return MiniJson.array(root.get("features"));
    }

    private static SkyPosition point(Object coordinates) {
        List<Object> pair = MiniJson.array(coordinates);
        return new SkyPosition(toRa(MiniJson.number(pair.get(0))),
                MiniJson.number(pair.get(1)));
    }

    // ------------------------------------------------------------------

    private void workedExamples() {
        System.out.println("Worked examples:");
        // RA-wrap: figure segments whose endpoints straddle RA 0.
        int wraps = 0;
        Segment example = null;
        for (Segment segment : figureSegments) {
            double gap = Math.abs(segment.from().raDegrees()
                    - segment.to().raDegrees());
            if (gap > 180.0) {
                wraps++;
                if (example == null) {
                    example = segment;
                }
            }
        }
        System.out.printf(Locale.ROOT,
                "  RA-wrap figure segments: %d (e.g. %s %.4f deg -> %.4f deg"
                        + " crosses RA 0 and draws seamlessly on the rawrap"
                        + " pages)%n",
                wraps, example == null ? "none" : example.constellation(),
                example == null ? 0.0 : example.from().raDegrees(),
                example == null ? 0.0 : example.to().raDegrees());
        // Polar: the highest-declination boundary corner.
        Segment polarSegment = null;
        double highest = 0.0;
        for (Segment segment : boundarySegments) {
            double dec = Math.max(segment.from().decDegrees(),
                    segment.to().decDegrees());
            if (dec > highest) {
                highest = dec;
                polarSegment = segment;
            }
        }
        System.out.printf(Locale.ROOT,
                "  Highest boundary corner: %s at dec %.4f deg (the polar pages"
                        + " draw it through the pole-adjacent projection)%n",
                polarSegment == null ? "none" : polarSegment.constellation(),
                highest);
        // Crossing segment: endpoints outside the m42 36-degree page whose
        // segment still crosses it (Eridanus reaches across the south-west).
        System.out.println("  Crossing segments: subdivision every "
                + SEGMENT_STEP_DEGREES + " degrees draws exactly the visible"
                + " portion of segments whose endpoints lie off-page; counts"
                + " above tally any segment leaving ink, not endpoint"
                + " membership.");
    }
}
