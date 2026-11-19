package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.chart.SkyPosition;
import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.BelowHorizon;
import juranometria.render.CardinalLandmark;
import juranometria.render.ChartPalette;
import juranometria.render.EquatorialGrid;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService;
import juranometria.ui.ReferenceInk;
import juranometria.ui.meteor.MeteorShowerSession;

/**
 * Ecliptic-longitude ticks and a current solar-longitude marker as
 * chart furniture: the visual and semantic study (#719, road #726).
 *
 * <p><strong>A study, and a mock.</strong> Every page is the
 * application's own chart, composed as the application composes it,
 * with the ecliptic shown. The graduation and the marker are then
 * drawn over the painted page by this tool, from the page's own
 * projection: no module contributes them, the chart cannot ink them,
 * and nothing in the production tree knows what an ecliptic longitude
 * tick is. What the mock decides - where a stroke goes, whether a
 * figure finds a clean box - is stated in the record as the mock's
 * rule, and is the rule a production version would be held to or
 * ruled away from.
 *
 * <p>Nothing here reads a raw source: every position is the production
 * services', every instant is stated, and the four seasonal instants
 * are the cited fixture's ({@link SeasonalEventsFixture}).
 *
 * <p>Standard output is
 * {@code docs/studies/ecliptic-longitude/measurements.md}; the pages
 * and {@code platform.md} are written beside it. Regenerate with
 * {@code make ecliptic-longitude-study}.
 */
public final class EclipticLongitudeStudyMain {

    static final Path OUT = Path.of("docs/studies/ecliptic-longitude");
    static final int W = 1440;
    static final int H = 900;
    static final int WHOLE_W = 2000;
    static final int WHOLE_H = 1000;
    static final double FIELD = 60.0;

    static final double OSLO_LAT = MeteorShowerStudyMain.OSLO_LAT;
    static final double OSLO_LON = MeteorShowerStudyMain.OSLO_LON;
    static final double CAPE_LAT = -33.93;
    static final double CAPE_LON = 18.42;

    /** The stroke's half length, page pixels: the meridian graduation's own (#515). */
    static final double HALF_TICK = 3.0;
    /** A stroke that carries a figure is this long either side: the mock's major tick. */
    static final double HALF_MAJOR = 6.0;
    /**
     * The mock's second, hairline-tolerant figure rule: the share of a
     * figure's box that may be inked on the page as painted, each pixel
     * weighed by how far it stands from the ground.
     */
    static final double TOLERATED_INK = 0.04;
    /** Clear ground between a stroke's end and its figure. */
    static final double FIGURE_GAP = 3.0;
    /** The long tick candidate's half length. */
    static final double LONG_HALF = 10.0;
    /** The caret candidate: the least distance from the line to a caret's tip. */
    static final double CARET_TIP = 6.0;
    static final double CARET_HEIGHT = 7.0;
    static final double CARET_HALF_BASE = 4.0;
    /** Clear ground between the Sun's drawn limb and a caret's tip. */
    static final double CARET_CLEAR_OF_SUN = 3.0;

    static final Font FIGURE_FONT = EquatorialGrid.GRID_LABEL_FONT;

    static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    /** The chart time the as-built meteor study's Geminid pages use. */
    static final Instant DECEMBER = Instant.parse("2027-12-14T20:00:00Z");
    /** A daylight chart time from Oslo, the Sun 53 degrees up near the June solstice. */
    static final Instant JUNE_NOON = Instant.parse("2027-06-15T11:00:00Z");
    static final Instant PERSEIDS_NIGHT = Instant.parse("2027-08-13T00:00:00Z");

    /** A graduation: a stroke every {@code step}, a figure every {@code figureEvery}. */
    record Scale(double step, double figureEvery, boolean atLandmarks, boolean uniform) {
        Scale(double step, double figureEvery, boolean atLandmarks) {
            this(step, figureEvery, atLandmarks, false);
        }

        String name() {
            return f("%.0f-%.0f%s%s", step, figureEvery, atLandmarks ? "-landmarks" : "",
                    uniform ? "-uniform" : "");
        }

        boolean figured(double longitude) {
            return Math.abs(longitude / figureEvery - Math.rint(longitude / figureEvery)) < 1e-9;
        }

        double half(double longitude) {
            return !uniform && figured(longitude) ? HALF_MAJOR : HALF_TICK;
        }
    }

    static final Scale PROPOSED = new Scale(10.0, 30.0, false);

    static final List<Scale> DENSITIES = List.of(new Scale(30.0, 30.0, false),
            new Scale(10.0, 30.0, false), new Scale(10.0, 30.0, false, true),
            new Scale(10.0, 10.0, false), new Scale(5.0, 30.0, false),
            new Scale(10.0, 30.0, true));

    enum Marker { NONE, LONG_TICK, CARETS }

    /** What the mock drew on one page. */
    record Drawn(String name, int ticksOnPage, int ticksBelowHorizon, int figuresAsked,
                 int figuresPlaced, int figuresStrict, List<String> refused, PixelPoint marker, String markerFigure,
                 boolean markerFigurePlaced, PixelPoint sunCentre, double sunRadiusPx,
                 double nearestLandmarkPx, String nearestLandmark, double nearestRadiantPx,
                 double leastSpacingPx, double greatestSpacingPx) {
    }

    private static PrintStream out;
    private static StringBuilder platform;
    private static WholeSkyComposition.Services services;
    private static MeteorShowers showers;

    private EclipticLongitudeStudyMain() {
    }

    private static void p(String s) {
        out.println(s);
    }

    private static void plat(String s) {
        platform.append(s).append('\n');
    }

    private static String f(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    public static void main(String[] args) throws Exception {
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        platform = new StringBuilder();
        services = new WholeSkyComposition.Services();
        showers = new MeteorShowers(MeteorShowerPack.load(),
                instant -> services.solar().solarLongitudeJ2000Degrees(instant));
        Files.createDirectories(OUT);

        p("# Ecliptic-longitude ticks and a solar-longitude marker: the study (#719)");
        p("");
        p("Road #726, issue #719. Generated by `make ecliptic-longitude-study`"
                + " (`juranometria.tool.EclipticLongitudeStudyMain`). **A study and a mock:**"
                + " every page is the application's own chart, composed as the application"
                + " composes it, with the ecliptic shown; the graduation and the marker are"
                + " drawn over the painted page by the study tool, from the page's own"
                + " projection. No production code draws them. Positions are the production"
                + " services'; the four seasonal instants are the cited fixture"
                + " `docs/studies/solar-system/seasons-2026.txt` (IMCCE). Which ticks lie on a"
                + " page is the sky's and the projection's and is stated here; whether a"
                + " figure found a clean box depends on the fonts in front of the rasteriser"
                + " and is this machine's record in `platform.md`.");
        p("");
        p(f("The mock's rules, as page pixels: a stroke across the ecliptic %.0f either side"
                + " of it (the meridian graduation's length, #515), in the ecliptic's own ink,"
                + " subdued below the horizon as the line is; a stroke that carries"
                + " a figure is %.0f either side; a figure in the grid-label font, %.0f clear"
                + " of the stroke's end, on the ecliptic's north side or, that refused, its"
                + " south side, written only where the page as painted is all but bare under"
                + " its box. The page's own writer places against boxes and catalogue ink,"
                + " not pixels, so the mock's refusals are an estimate and are this"
                + " machine's record.", HALF_TICK, HALF_MAJOR, FIGURE_GAP));
        p("");

        PlatformEvidence.preface(platform,
                "Ecliptic-longitude ticks and a solar-longitude marker (#719) - platform record",
                "Road #726, issue #719.");
        plat("What depends on this machine's fonts, from `make ecliptic-longitude-study`."
                + " Held to reproducing here, not pinned to bytes.");
        plat("");
        Font font = FIGURE_FONT;
        plat(f("The figure font is `%s` %d pt, resolved here to `%s`. It %s draw λ (U+03BB)"
                + " and %s draw ☉ (U+2609).", font.getName(), font.getSize(),
                font.getFontName(Locale.ROOT), font.canDisplay(0x03BB) ? "can" : "cannot",
                font.canDisplay(0x2609) ? "can" : "cannot"));
        plat("");
        plat(f("Two rules are recorded for each figure. *Bare ground*: some offered side of"
                + " the stroke has no ink at all under the figure's box. *Written*: the mock's"
                + " drawing rule, which lets a figure cross a hairline - no more than %.0f%% of"
                + " the box inked, each pixel weighed by its contrast, and below the horizon"
                + " weighed up by the subdued ink's share. The pages are drawn by the second.",
                TOLERATED_INK * 100.0));
        plat("");
        plat("| page | figures asked | on bare ground | written | refused | marker's figure |");
        plat("|---|---|---|---|---|---|");

        List<Drawn> whole = wholeSkyPages();
        List<Drawn> density = densityPages();
        List<Drawn> ordinary = ordinaryPages();
        List<Drawn> seasons = seasonPages();
        pagesSection(whole, density, ordinary, seasons);
        spacingSection(whole.get(1));
        seamSection();
        fieldSection();
        frameSection();
        placeAndTimeSection();
        sunSection(ordinary);
        landmarkSection();
        radiantSection();
        exportSection();
        behaviourSection();
        rulingsSection();

        PlatformEvidence.write(platform, OUT.resolve("platform.md").toString());
    }

    // ------------------------------------------------------------------ composing

    private static Observer oslo(Instant when) {
        return new Observer(OSLO_LAT, OSLO_LON, when);
    }

    /** One page asked of the mock. */
    private record Ask(String name, Observer observer, ChartPalette palette, String language,
                       boolean whole, double field, SkyPosition centre, Scale scale,
                       Marker marker, String shower, boolean subdueMock) {
    }

    private static Ask whole(String name, Instant when, ChartPalette palette, String language,
                             Scale scale, Marker marker, String shower) {
        return new Ask(name, oslo(when), palette, language, true, FIELD, null, scale, marker,
                shower, true);
    }

    private static Ask unsubdued(Ask ask, String name) {
        return new Ask(name, ask.observer(), ask.palette(), ask.language(), ask.whole(),
                ask.field(), ask.centre(), ask.scale(), ask.marker(), ask.shower(), false);
    }

    private static Ask field(String name, Instant when, double field, SkyPosition centre,
                             Scale scale, Marker marker) {
        return new Ask(name, oslo(when), ChartPalette.WHITE_PAPER, "en", false, field, centre,
                scale, marker, null, true);
    }

    private static SkyPosition sunPlace(Observer observer) {
        return Ecliptic.toEquatorial(sunLongitude(observer), 0.0);
    }

    /** The Sun's J2000 ecliptic longitude as the chart draws it for this observer. */
    private static double sunLongitude(Observer observer) {
        return sun(observer).eclipticLongitudeJ2000Degrees();
    }

    private static SolarSystemService.SunObservation sun(Observer observer) {
        return (SolarSystemService.SunObservation) services.solar()
                .observe(SolarSystemService.Body.SUN, observer);
    }

    private static Drawn draw(Ask ask) throws Exception {
        WholeSkyComposition.Page page = WholeSkyComposition.paint(new WholeSkyComposition.Ask(
                ask.observer(), ask.palette(), ask.language(), 8.0, true, true, FIELD,
                ask.whole() ? WHOLE_W : W, ask.whole() ? WHOLE_H : H), services);
        int w = ask.whole() ? WHOLE_W : W;
        int h = ask.whole() ? WHOLE_H : H;
        Drawn[] result = new Drawn[1];
        BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            java.util.function.Supplier<Observer> now =
                    () -> page.meridian().attached() ? page.meridian().observer() : null;
            if (ask.shower() != null) {
                MeteorShowerModule module = page.host().attach(new MeteorShowerModule(now,
                        () -> showers, page.meridian()::subduing));
                new MeteorShowerSession(now, () -> showers, services::solar, module)
                        .select(ask.shower());
            }
            if (!ask.whole()) {
                page.session().leave();
                page.controller().recenter(ask.centre(), ask.field());
            }
            image[0] = WholeSkyComposition.paintNow(page.chart(), w, h);
            result[0] = mock(ask, page, image[0]);
        });
        ImageIO.write(image[0], "png", OUT.resolve(ask.name() + ".png").toFile());
        Drawn d = result[0];
        plat(f("| `%s` | %d | %d | %d | %s | %s |", d.name(), d.figuresAsked(),
                d.figuresStrict(), d.figuresPlaced(), d.refused().isEmpty() ? "none" : String.join(", ", d.refused()),
                d.marker() == null ? "-" : d.markerFigurePlaced() ? "placed" : "refused"));
        System.err.println(ask.name() + " written");
        return d;
    }

    // ------------------------------------------------------------------ the mock

    /** Draws the graduation and the marker over the painted page; on the event thread. */
    private static Drawn mock(Ask ask, WholeSkyComposition.Page page, BufferedImage image) {
        DrawnPage drawn = DrawnPage.of(page.chart().currentScene());
        Projection projection = drawn.projection();
        ViewportMapping mapping = new ViewportMapping(drawn);
        BelowHorizon below = page.chart().belowHorizon(page.chart().currentScene());
        ChartPalette palette = ask.palette();
        Color ground = palette.ground();
        int w = image.getWidth();
        int h = image.getHeight();

        // The Sun as the page drew it: the mock's self-check, and the
        // disc the marker must stand clear of.
        ReferenceInk.BodyPlacement sunMark = null;
        List<PixelPoint> radiants = new ArrayList<>();
        for (ReferenceInk.BodyPlacement placed : page.chart().renderedBodies()) {
            if (placed.identity().equals("sun")) {
                sunMark = placed;
            } else if (ask.shower() != null && showers.shower(ask.shower()).orElseThrow()
                    .identity().equals(placed.identity())) {
                radiants.add(placed.centre());
            }
        }
        double lambda = sunLongitude(ask.observer());
        PixelPoint marker = pixel(projection, mapping, lambda, 0.0);
        double sunRadius = 0.0;
        if (sunMark != null) {
            Rectangle2D disc = sunMark.disc().getBounds2D();
            sunRadius = Math.max(disc.getWidth(), disc.getHeight()) / 2.0;
            if (marker != null && Math.hypot(marker.x() - sunMark.centre().x(),
                    marker.y() - sunMark.centre().y()) > 1.5) {
                throw new IllegalStateException("the mock's projection disagrees with the"
                        + " page's Sun on " + ask.name() + ": " + marker + " against "
                        + sunMark.centre());
            }
        }

        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FIGURE_FONT);
        FontMetrics metrics = g.getFontMetrics();
        BufferedImage bare = copy(image);

        int ticks = 0;
        int belowCount = 0;
        int asked = 0;
        int placed = 0;
        int[] strict = new int[1];
        List<String> refused = new ArrayList<>();
        List<Rectangle2D> taken = new ArrayList<>();
        List<double[]> onPage = new ArrayList<>();
        try {
            // The marker first: its glyph and its figure outrank the
            // graduation's figures, which give way to it.
            String markerFigure = null;
            boolean markerFigurePlaced = false;
            if (ask.marker() != Marker.NONE && marker != null && inside(marker, w, h)) {
                double[] n = normal(projection, mapping, lambda);
                SkyPosition at = Ecliptic.toEquatorial(lambda, 0.0);
                BelowHorizon markerBelow = ask.subdueMock() ? below : BelowHorizon.NONE;
                Color ink = tone(markerBelow, at, palette.textInk(), ground);
                g.setColor(ink);
                double reach;
                if (ask.marker() == Marker.LONG_TICK) {
                    g.setStroke(new BasicStroke(1.6f));
                    g.draw(new Line2D.Double(marker.x() - n[0] * LONG_HALF,
                            marker.y() - n[1] * LONG_HALF, marker.x() + n[0] * LONG_HALF,
                            marker.y() + n[1] * LONG_HALF));
                    reach = LONG_HALF;
                } else {
                    double tip = Math.max(CARET_TIP, sunRadius + CARET_CLEAR_OF_SUN);
                    for (int side : new int[] {1, -1}) {
                        g.fill(caret(marker, n, side, tip));
                    }
                    reach = tip + CARET_HEIGHT;
                }
                markerFigure = f("λ☉ %.1f°", lambda);
                g.setColor(tone(markerBelow, at, palette.textInk(), ground));
                Rectangle2D box = place(g, bare, below, metrics, marker, n, reach, markerFigure,
                        ground, taken, new int[] {-1, 1}, new int[1]);
                markerFigurePlaced = box != null;
            }

            Scale scale = ask.scale();
            if (scale != null) {
                g.setStroke(new BasicStroke(1.0f));
                int count = (int) Math.round(360.0 / scale.step());
                for (int k = 0; k < count; k++) {
                    double longitude = k * scale.step();
                    PixelPoint at = pixel(projection, mapping, longitude, 0.0);
                    if (at == null || !inside(at, w, h)) {
                        continue;
                    }
                    boolean landmark = longitude % 90.0 == 0.0;
                    if (landmark && !scale.atLandmarks()) {
                        continue;
                    }
                    double[] n = normal(projection, mapping, longitude);
                    if (n == null) {
                        continue;
                    }
                    SkyPosition sky = Ecliptic.toEquatorial(longitude, 0.0);
                    ticks++;
                    if (below.active() && below.below(sky)) {
                        belowCount++;
                    }
                    onPage.add(new double[] {longitude, at.x(), at.y()});
                    g.setColor(tone(below, sky, palette.figureInk(), ground));
                    double half = scale.half(longitude);
                    g.draw(new Line2D.Double(at.x() - n[0] * half, at.y() - n[1] * half,
                            at.x() + n[0] * half, at.y() + n[1] * half));
                }
                for (double[] tick : onPage) {
                    double longitude = tick[0];
                    if (!scale.figured(longitude)) {
                        continue;
                    }
                    asked++;
                    PixelPoint at = new PixelPoint(tick[1], tick[2]);
                    double[] n = normal(projection, mapping, longitude);
                    SkyPosition sky = Ecliptic.toEquatorial(longitude, 0.0);
                    g.setColor(tone(below, sky, palette.gridLabelInk(), ground));
                    String figure = f("%.0f°", longitude);
                    if (place(g, bare, below, metrics, at, n, scale.half(longitude), figure,
                            ground, taken, new int[] {1, -1}, strict) != null) {
                        placed++;
                    } else {
                        refused.add(figure);
                    }
                }
            }

            double least = Double.MAX_VALUE;
            double greatest = 0.0;
            for (int i = 1; i < onPage.size(); i++) {
                double[] a = onPage.get(i - 1);
                double[] b = onPage.get(i);
                if (Math.abs(b[0] - a[0] - (scale == null ? 0.0 : scale.step())) > 1e-9) {
                    continue;
                }
                double apart = Math.hypot(b[1] - a[1], b[2] - a[2]);
                if (apart > w / 2.0) {
                    continue;
                }
                least = Math.min(least, apart);
                greatest = Math.max(greatest, apart);
            }

            double nearestLandmark = Double.MAX_VALUE;
            String nearestName = "-";
            double nearestRadiant = Double.MAX_VALUE;
            if (marker != null) {
                for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
                    PixelPoint at = pixel(projection, mapping, landmark.longitudeDegrees(), 0.0);
                    if (at != null && inside(at, w, h)) {
                        double apart = Math.hypot(at.x() - marker.x(), at.y() - marker.y());
                        if (apart < nearestLandmark) {
                            nearestLandmark = apart;
                            nearestName = landmark.name();
                        }
                    }
                }
                for (PixelPoint radiant : radiants) {
                    nearestRadiant = Math.min(nearestRadiant,
                            Math.hypot(radiant.x() - marker.x(), radiant.y() - marker.y()));
                }
            }
            return new Drawn(ask.name(), ticks, belowCount, asked, placed, strict[0],
                    List.copyOf(refused),
                    ask.marker() == Marker.NONE || marker == null || !inside(marker, w, h)
                            ? null : marker,
                    markerFigure, markerFigurePlaced,
                    sunMark == null ? null : sunMark.centre(), sunRadius, nearestLandmark,
                    nearestName, nearestRadiant, least, greatest);
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(),
                image.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static Color tone(BelowHorizon below, SkyPosition at, Color ink, Color ground) {
        return below.active() && below.below(at) ? BelowHorizon.subdued(ink, ground) : ink;
    }

    private static boolean inside(PixelPoint at, int w, int h) {
        return at.x() >= 0 && at.y() >= 0 && at.x() < w && at.y() < h;
    }

    private static PixelPoint pixel(Projection projection, ViewportMapping mapping,
                                    double longitude, double latitude) {
        return projection.project(Ecliptic.toEquatorial(longitude, latitude))
                .map(mapping::toPixel).orElse(null);
    }

    /**
     * The unit normal to the ecliptic on this page at a longitude,
     * pointing to the ecliptic's north side; null where the projection
     * refuses the neighbourhood.
     */
    private static double[] normal(Projection projection, ViewportMapping mapping,
                                   double longitude) {
        PixelPoint at = pixel(projection, mapping, longitude, 0.0);
        PixelPoint north = pixel(projection, mapping, longitude, 0.1);
        if (at == null || north == null) {
            return null;
        }
        double dx = north.x() - at.x();
        double dy = north.y() - at.y();
        double length = Math.hypot(dx, dy);
        if (length == 0.0 || length > 50.0) {
            return null;
        }
        return new double[] {dx / length, dy / length};
    }

    private static Shape caret(PixelPoint at, double[] n, int side, double tip) {
        double tx = -n[1];
        double ty = n[0];
        Path2D caret = new Path2D.Double();
        caret.moveTo(at.x() + side * n[0] * tip, at.y() + side * n[1] * tip);
        double bx = at.x() + side * n[0] * (tip + CARET_HEIGHT);
        double by = at.y() + side * n[1] * (tip + CARET_HEIGHT);
        caret.lineTo(bx + tx * CARET_HALF_BASE, by + ty * CARET_HALF_BASE);
        caret.lineTo(bx - tx * CARET_HALF_BASE, by - ty * CARET_HALF_BASE);
        caret.closePath();
        return caret;
    }

    /**
     * Writes a figure beside a mark, on the first of the offered sides
     * whose box is clear of every figure the mock has written and, on
     * the page as painted, carries no more ink than a crossing hairline
     * ({@link #TOLERATED_INK}); null when both are refused. {@code strict}
     * is raised when some offered side was bare ground altogether.
     */
    private static Rectangle2D place(Graphics2D g, BufferedImage bare, BelowHorizon below,
                                     FontMetrics metrics, PixelPoint at, double[] n,
                                     double reach, String figure, Color ground,
                                     List<Rectangle2D> taken, int[] sides, int[] strict) {
        double width = metrics.stringWidth(figure);
        double height = metrics.getAscent() + metrics.getDescent();
        Rectangle2D chosen = null;
        boolean bareSomewhere = false;
        for (int side : sides) {
            double away = reach + FIGURE_GAP
                    + 0.5 * (Math.abs(n[0]) * width + Math.abs(n[1]) * height);
            double cx = at.x() + side * n[0] * away;
            double cy = at.y() + side * n[1] * away;
            Rectangle2D box = new Rectangle2D.Double(cx - width / 2.0, cy - height / 2.0,
                    width, height);
            if (touches(box, taken)) {
                continue;
            }
            double ink = inked(bare, box, ground);
            if (ink < 0.0) {
                continue;
            }
            if (below.active() && below.belowAt(cx, cy)) {
                ink /= BelowHorizon.KEPT;
            }
            if (ink == 0.0) {
                bareSomewhere = true;
            }
            if (chosen == null && ink <= TOLERATED_INK) {
                chosen = box;
            }
        }
        if (bareSomewhere) {
            strict[0]++;
        }
        if (chosen != null) {
            taken.add(chosen);
            g.drawString(figure, (float) chosen.getMinX(),
                    (float) (chosen.getMaxY() - metrics.getDescent()));
        }
        return chosen;
    }

    private static boolean touches(Rectangle2D box, List<Rectangle2D> taken) {
        for (Rectangle2D other : taken) {
            if (other.intersects(box.getMinX() - 1, box.getMinY() - 1, box.getWidth() + 2,
                    box.getHeight() + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The share of a box that is inked on the page as painted, each
     * pixel weighed by its distance from the ground; negative when the
     * box leaves the page.
     */
    private static double inked(BufferedImage bare, Rectangle2D box, Color ground) {
        int x0 = (int) Math.floor(box.getMinX()) - 1;
        int y0 = (int) Math.floor(box.getMinY()) - 1;
        int x1 = (int) Math.ceil(box.getMaxX()) + 1;
        int y1 = (int) Math.ceil(box.getMaxY()) + 1;
        if (x0 < 0 || y0 < 0 || x1 >= bare.getWidth() || y1 >= bare.getHeight()) {
            return -1.0;
        }
        double sum = 0.0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int rgb = bare.getRGB(x, y);
                int d = Math.max(Math.abs(((rgb >> 16) & 0xFF) - ground.getRed()),
                        Math.max(Math.abs(((rgb >> 8) & 0xFF) - ground.getGreen()),
                                Math.abs((rgb & 0xFF) - ground.getBlue())));
                sum += d / 255.0;
            }
        }
        return sum / ((x1 - x0 + 1.0) * (y1 - y0 + 1.0));
    }

    /** A crop of a written page, enlarged without smoothing, written beside it. */
    private static void crop(String from, String to, PixelPoint about, int half, int times)
            throws Exception {
        BufferedImage page = ImageIO.read(OUT.resolve(from + ".png").toFile());
        int x = (int) Math.max(0, Math.min(page.getWidth() - 2 * half, about.x() - half));
        int y = (int) Math.max(0, Math.min(page.getHeight() - 2 * half, about.y() - half));
        BufferedImage part = page.getSubimage(x, y, 2 * half, 2 * half);
        BufferedImage large = new BufferedImage(2 * half * times, 2 * half * times,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = large.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(part, 0, 0, 2 * half * times, 2 * half * times, null);
        g.dispose();
        ImageIO.write(large, "png", OUT.resolve(to + ".png").toFile());
    }

    // ------------------------------------------------------------------ the pages

    private static List<Drawn> wholeSkyPages() throws Exception {
        List<Drawn> drawn = new ArrayList<>();
        drawn.add(draw(whole("whole-1-none", DECEMBER, ChartPalette.WHITE_PAPER, "en", null,
                Marker.NONE, "GEM")));
        drawn.add(draw(whole("whole-2-scale", DECEMBER, ChartPalette.WHITE_PAPER, "en", PROPOSED,
                Marker.NONE, "GEM")));
        drawn.add(draw(whole("whole-3-scale-carets", DECEMBER, ChartPalette.WHITE_PAPER, "en",
                PROPOSED, Marker.CARETS, "GEM")));
        drawn.add(draw(whole("whole-3-scale-long-tick", DECEMBER, ChartPalette.WHITE_PAPER, "en",
                PROPOSED, Marker.LONG_TICK, "GEM")));
        drawn.add(draw(whole("whole-3-scale-carets-nb-black", DECEMBER, ChartPalette.BLACK_SKY,
                "nb-NO", PROPOSED, Marker.CARETS, "GEM")));
        drawn.add(draw(unsubdued(whole("x", DECEMBER, ChartPalette.WHITE_PAPER, "en", PROPOSED,
                Marker.CARETS, "GEM"), "whole-3-scale-carets-full-ink")));
        drawn.add(draw(unsubdued(whole("x", DECEMBER, ChartPalette.WHITE_PAPER, "en", PROPOSED,
                Marker.LONG_TICK, "GEM"), "whole-3-scale-long-tick-full-ink")));
        crop("whole-3-scale-carets-full-ink", "crop-whole-carets-full-ink",
                drawn.get(5).marker(), 60, 5);
        crop("whole-3-scale-long-tick-full-ink", "crop-whole-long-tick-full-ink",
                drawn.get(6).marker(), 60, 5);
        crop("whole-3-scale-carets", "crop-whole-carets", drawn.get(2).marker(), 60, 5);
        crop("whole-3-scale-long-tick", "crop-whole-long-tick", drawn.get(3).marker(), 60, 5);
        crop("whole-1-none", "crop-whole-none", drawn.get(2).marker(), 60, 5);
        return drawn;
    }

    private static List<Drawn> densityPages() throws Exception {
        List<Drawn> drawn = new ArrayList<>();
        for (Scale scale : DENSITIES) {
            drawn.add(draw(whole("density-" + scale.name(), DECEMBER, ChartPalette.WHITE_PAPER,
                    "en", scale, Marker.NONE, null)));
        }
        return drawn;
    }

    private static List<Drawn> ordinaryPages() throws Exception {
        List<Drawn> drawn = new ArrayList<>();
        SkyPosition juneSun = sunPlace(oslo(JUNE_NOON));
        SkyPosition decemberSun = sunPlace(oslo(DECEMBER));
        drawn.add(draw(field("field-60-1-none", JUNE_NOON, 60.0, juneSun, null, Marker.NONE)));
        drawn.add(draw(field("field-60-2-scale", JUNE_NOON, 60.0, juneSun, PROPOSED,
                Marker.NONE)));
        drawn.add(draw(field("field-60-3-scale-carets", JUNE_NOON, 60.0, juneSun, PROPOSED,
                Marker.CARETS)));
        drawn.add(draw(field("field-60-3-scale-long-tick", JUNE_NOON, 60.0, juneSun, PROPOSED,
                Marker.LONG_TICK)));
        drawn.add(draw(field("field-120-3-scale-carets", JUNE_NOON, 120.0, juneSun, PROPOSED,
                Marker.CARETS)));
        drawn.add(draw(field("field-24-3-scale-carets", JUNE_NOON, 24.0, juneSun, PROPOSED,
                Marker.CARETS)));
        drawn.add(draw(field("field-8-3-scale-carets", JUNE_NOON, 8.0, juneSun, PROPOSED,
                Marker.CARETS)));
        drawn.add(draw(field("field-8-3-step-1-carets", JUNE_NOON, 8.0, juneSun,
                new Scale(1.0, 5.0, false), Marker.CARETS)));
        drawn.add(draw(field("field-60-below-horizon-carets", DECEMBER, 60.0, decemberSun,
                PROPOSED, Marker.CARETS)));
        crop("field-60-3-scale-carets", "crop-field-60-carets", drawn.get(2).marker(), 60, 5);
        crop("field-60-3-scale-long-tick", "crop-field-60-long-tick", drawn.get(3).marker(), 60,
                5);
        return drawn;
    }

    /** The whole sky at the four seasonal instants of the cited fixture: the marker at a landmark. */
    private static List<Drawn> seasonPages() throws Exception {
        List<Drawn> drawn = new ArrayList<>();
        Map<String, SeasonalEventsFixture.Event> events = SeasonalEventsFixture.read();
        SeasonalEventsFixture.Event march = events.get("march-equinox");
        drawn.add(draw(whole("season-march-equinox-carets", march.instant(),
                ChartPalette.WHITE_PAPER, "en", PROPOSED, Marker.CARETS, null)));
        crop("season-march-equinox-carets", "crop-march-equinox-carets", drawn.get(0).marker(),
                60, 5);
        drawn.add(draw(new Ask("season-march-equinox-field-8-carets", oslo(march.instant()),
                ChartPalette.WHITE_PAPER, "en", false, 8.0, sunPlace(oslo(march.instant())),
                new Scale(1.0, 5.0, false), Marker.CARETS, null, true)));
        return drawn;
    }

    // ------------------------------------------------------------------ sections

    private static void pagesSection(List<Drawn> whole, List<Drawn> density, List<Drawn> ordinary,
                                     List<Drawn> seasons) {
        p("## 1. The three pages, on the whole sky and on an ordinary field");
        p("");
        p(f("From Oslo (%.2f N, %.2f E). The whole sky is %d by %d pixels at chart time %s"
                + " UTC, the Geminids selected so that a radiant stands on the page; the"
                + " ordinary field is %d by %d, centred on the Sun at chart time %s UTC, the"
                + " Sun above the horizon. The horizon is drawn and Subdue below horizon is"
                + " on. The scale on every page here is a stroke every %.0f degrees and a"
                + " figure every %.0f, none at the four landmarks.", OSLO_LAT, OSLO_LON,
                WHOLE_W, WHOLE_H, MINUTE.format(DECEMBER), W, H, MINUTE.format(JUNE_NOON),
                PROPOSED.step(), PROPOSED.figureEvery()));
        p("");
        p("| page | what it shows | ticks on page | of them below the horizon | figures asked"
                + " | solar longitude | marker at | Sun drawn at | Sun's drawn radius |");
        p("|---|---|---|---|---|---|---|---|---|");
        String[] what = {"1: no longitude notation", "2: ticks and figures",
            "3: the same scale and the marker as two carets",
            "3: the same scale and the marker as a long tick",
            "3 with carets, Norwegian, black sky",
            "3 with carets, the marker in full ink below the horizon",
            "3 with a long tick, the marker in full ink below the horizon"};
        for (int i = 0; i < whole.size(); i++) {
            row(whole.get(i), what[i], oslo(DECEMBER));
        }
        String[] fields = {"1: no longitude notation, 60 degrees", "2: ticks and figures",
            "3: scale and carets", "3: scale and long tick", "3 with carets, 120 degrees",
            "3 with carets, 24 degrees", "3 with carets, 8 degrees",
            "8 degrees, a stroke every degree and a figure every five",
            "60 degrees, the Sun below the horizon (December)"};
        for (int i = 0; i < ordinary.size(); i++) {
            row(ordinary.get(i), fields[i], oslo(i == ordinary.size() - 1 ? DECEMBER : JUNE_NOON));
        }
        p("");
        p("Enlarged five times, the marker's neighbourhood on the whole sky and on the"
                + " 60 degree field: `crop-whole-none.png`, `crop-whole-carets.png`,"
                + " `crop-whole-long-tick.png`, `crop-whole-carets-full-ink.png`,"
                + " `crop-whole-long-tick-full-ink.png`, `crop-field-60-carets.png`,"
                + " `crop-field-60-long-tick.png`.");
        p("");
        p("### Density, on the whole sky");
        p("");
        p("The same whole-sky page, no shower selected, six graduations. *Uniform* draws every"
                + " stroke at the short length; otherwise a stroke that carries a figure is the"
                + " long one.");
        p("");
        p("| page | stroke every | figure every | at the landmarks | strokes | ticks on page"
                + " | figures asked | least spacing, px | greatest spacing, px |");
        p("|---|---|---|---|---|---|---|---|---|");
        Scale[] scales = DENSITIES.toArray(new Scale[0]);
        for (int i = 0; i < density.size(); i++) {
            Drawn d = density.get(i);
            p(f("| `%s.png` | %.0f | %.0f | %s | %s | %d | %d | %s | %s |", d.name(),
                    scales[i].step(), scales[i].figureEvery(),
                    scales[i].atLandmarks() ? "yes" : "no",
                    scales[i].uniform() ? "uniform" : "long where figured",
                    d.ticksOnPage(), d.figuresAsked(), spacing(d.leastSpacingPx()),
                    spacing(d.greatestSpacingPx())));
        }
        p("");
        p("### The marker at a landmark");
        p("");
        Drawn equinox = seasons.get(0);
        Drawn close = seasons.get(1);
        p(f("The whole sky at the March equinox of the cited fixture (`%s.png`, enlarged in"
                + " `crop-march-equinox-carets.png`): the marker stands %.1f pixels from the"
                + " %s landmark's diamond, whose half width is %.1f. On an 8 degree field"
                + " centred on the Sun at the same instant (`%s.png`) it stands %.0f pixels"
                + " from it.", equinox.name(), equinox.nearestLandmarkPx(),
                equinox.nearestLandmark(), CardinalLandmark.DIAMOND, close.name(),
                close.nearestLandmarkPx()));
        p("");
    }

    private static String spacing(double px) {
        return px == Double.MAX_VALUE || px == 0.0 ? "-" : f("%.1f", px);
    }

    private static void row(Drawn d, String what, Observer observer) {
        p(f("| `%s.png` | %s | %d | %d | %d | %.2f | %s | %s | %.1f |", d.name(), what,
                d.ticksOnPage(), d.ticksBelowHorizon(), d.figuresAsked(), sunLongitude(observer),
                d.marker() == null ? "-" : f("%.0f, %.0f", d.marker().x(), d.marker().y()),
                d.sunCentre() == null ? "not on page"
                        : f("%.0f, %.0f", d.sunCentre().x(), d.sunCentre().y()),
                d.sunRadiusPx()));
    }

    private static void spacingSection(Drawn scale) {
        p("## 2. The whole sky's ruler is not uniform");
        p("");
        p(f("The whole-sky projection keeps areas, not distances along the ecliptic. On"
                + " `%s.png` two neighbouring strokes %.0f degrees apart stand between %.1f"
                + " and %.1f pixels apart: widest at the centre, closest at the two ends."
                + " A reader can count strokes anywhere on the ruler; a reader cannot"
                + " interpolate between two figures by eye with the same error everywhere.",
                scale.name(), PROPOSED.step(), scale.leastSpacingPx(),
                scale.greatestSpacingPx()));
        p("");
    }

    private static void seamSection() throws Exception {
        p("## 3. Zero, 360 and the seam");
        p("");
        p("The scale has no end: 0 degrees is the March equinox and there is no 360. The"
                + " whole-sky page does have two ends, and where they fall on the scale is the"
                + " chart time's and the place's, because the page is centred on the ecliptic"
                + " longitude of the zenith (R-4 of the whole-sky rulings).");
        p("");
        p("| chart time (UTC), from Oslo | longitude at the centre | longitude at both ends"
                + " | nearest stroke to an end | its distance from the end, px |");
        p("|---|---|---|---|---|");
        for (Instant when : List.of(DECEMBER, JUNE_NOON, PERSEIDS_NIGHT,
                Instant.parse("2027-12-14T23:00:00Z"))) {
            WholeSkyComposition.Page page = WholeSkyComposition.paint(
                    new WholeSkyComposition.Ask(oslo(when), ChartPalette.WHITE_PAPER, "en", 8.0,
                            true, true, FIELD, WHOLE_W, WHOLE_H), services);
            String[] row = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                DrawnPage drawn = DrawnPage.of(page.chart().currentScene());
                ViewportMapping mapping = new ViewportMapping(drawn);
                double centre = Ecliptic.toEcliptic(drawn.projection().centre())
                        .longitudeDegrees();
                double end = SkyFrame.normalise(centre + 180.0);
                double nearest = Math.rint(end / PROPOSED.step()) * PROPOSED.step();
                if (Math.abs(nearest - end) < 1e-9) {
                    nearest += PROPOSED.step();
                }
                PixelPoint at = pixel(drawn.projection(), mapping, nearest, 0.0);
                PixelPoint limbA = pixel(drawn.projection(), mapping, end - 1e-6, 0.0);
                PixelPoint limbB = pixel(drawn.projection(), mapping, end + 1e-6, 0.0);
                double toEnd = Math.min(Math.hypot(at.x() - limbA.x(), at.y() - limbA.y()),
                        Math.hypot(at.x() - limbB.x(), at.y() - limbB.y()));
                row[0] = f("| %s | %.2f | %.2f | %.0f | %.1f |", MINUTE.format(when), centre,
                        end, SkyFrame.normalise(nearest), toEnd);
            });
            p(row[0]);
        }
        p("");
        p("A stroke exactly at the seam would have to be drawn at both ends or at neither."
                + " With a stroke every ten degrees that happens only when the zenith's"
                + " ecliptic longitude is a whole multiple of ten, which the three-hour step"
                + " in the last row shows is a matter of minutes of chart time.");
        p("");
    }

    private static void fieldSection() throws Exception {
        p("## 4. An ordinary field shows only its part of the scale");
        p("");
        p(f("How many strokes lie on a %d by %d page centred on the ecliptic (on the Sun at"
                + " %s UTC), by field and by step. The ecliptic runs through the centre, so"
                + " these are the most a page of that field can carry.", W, H,
                MINUTE.format(JUNE_NOON)));
        p("");
        p("| field | every 30 | every 10 | every 5 | every 1 | pixels per degree at the centre |");
        p("|---|---|---|---|---|---|");
        Observer observer = oslo(JUNE_NOON);
        SkyPosition centre = sunPlace(observer);
        double lambda = sunLongitude(observer);
        for (double field : new double[] {120.0, 60.0, 24.0, 8.0, 2.0}) {
            WholeSkyComposition.Page page = WholeSkyComposition.paint(
                    new WholeSkyComposition.Ask(observer, ChartPalette.WHITE_PAPER, "en", 8.0,
                            true, true, FIELD, W, H), services);
            String[] row = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                page.session().leave();
                page.controller().recenter(centre, field);
                WholeSkyComposition.paintNow(page.chart(), W, H);
                DrawnPage drawn = DrawnPage.of(page.chart().currentScene());
                ViewportMapping mapping = new ViewportMapping(drawn);
                StringBuilder counts = new StringBuilder();
                for (double step : new double[] {30.0, 10.0, 5.0, 1.0}) {
                    int on = 0;
                    for (int k = 0; k * step < 360.0 - 1e-9; k++) {
                        PixelPoint at = pixel(drawn.projection(), mapping, k * step, 0.0);
                        if (at != null && inside(at, W, H)) {
                            on++;
                        }
                    }
                    counts.append(f(" %d |", on));
                }
                PixelPoint a = pixel(drawn.projection(), mapping, lambda - 0.5, 0.0);
                PixelPoint b = pixel(drawn.projection(), mapping, lambda + 0.5, 0.0);
                row[0] = f("| %.0f |%s %.1f |", field, counts,
                        Math.hypot(a.x() - b.x(), a.y() - b.y()));
            });
            p(row[0]);
        }
        p("");
        p("A page that does not reach the ecliptic carries none of it: the Perseid radiant"
                + " at +57.6 declination stands some 38 degrees of ecliptic latitude from"
                + " the line, and a 60 degree page centred on it shows no ecliptic at all.");
        p("");
    }

    /** The apparent longitude from the true equinox of date, from the production apparent place. */
    private static double ofDateLongitude(SolarSystemService.SunObservation sun) {
        double jd = SkyFrame.julianDate(sun.instant());
        double t = SkyFrame.centuries(jd);
        double eps = Math.toRadians(SkyFrame.meanObliquityDegrees(t)
                + SkyFrame.nutationDegrees(t)[1]);
        double ra = Math.toRadians(sun.apparentOfDate().raDegrees());
        double dec = Math.toRadians(sun.apparentOfDate().decDegrees());
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(
                Math.sin(ra) * Math.cos(eps) + Math.tan(dec) * Math.sin(eps), Math.cos(ra))));
    }

    private static double signed(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }

    private static void frameSection() throws Exception {
        p("## 5. Which longitude: the scale's equinox and the marker's");
        p("");
        p("The atlas's ecliptic is the mean ecliptic of J2000 and its landmarks are J2000's"
                + " (`docs/decisions/ecliptic.md`); the solar longitude the meteor authorities"
                + " publish and the atlas evaluates is the Sun's geocentric longitude from the"
                + " J2000 equinox (R-5 of `docs/decisions/meteor-showers.md`). A reader may"
                + " instead expect the longitude of date, which is 0 at the March equinox."
                + " The two at the four seasonal instants of the cited fixture, geocentric"
                + " and for the chart's place:");
        p("");
        p("| event | instant (UTC) | apparent longitude of date | solar longitude, J2000"
                + " | the difference | on the whole sky, px | on an 8 degree page, px |");
        p("|---|---|---|---|---|---|---|");
        Map<String, SeasonalEventsFixture.Event> events = SeasonalEventsFixture.read();
        double wholePerDegree = wholeSkyPixelsPerDegreeAtCentre();
        double closePerDegree = W / 8.0;
        for (SeasonalEventsFixture.Event event : events.values()) {
            SolarSystemService.SunObservation sun = sun(oslo(event.instant()));
            double ofDate = ofDateLongitude(sun);
            double j2000 = services.solar().solarLongitudeJ2000Degrees(event.instant());
            double apart = signed(ofDate - j2000);
            p(f("| %s | %s | %.4f | %.4f | %.4f | %.1f | %.0f |", event.name(),
                    MINUTE.format(event.instant()), ofDate, j2000, apart,
                    Math.abs(apart) * wholePerDegree, Math.abs(apart) * closePerDegree));
        }
        p("");
        p(f("The whole sky is taken at %.2f pixels per degree, its scale at the centre of the"
                + " page; the 8 degree page at %.0f. The difference is the precession since"
                + " J2000 with aberration and nutation: it grows by about 0.014 degrees a"
                + " year.", wholePerDegree, closePerDegree));
        p("");
        p("The difference across the years the Sun is computed for, at each year's 1 July"
                + " 00:00 UTC:");
        p("");
        p("| year | apparent longitude of date less solar longitude J2000 |");
        p("|---|---|");
        for (int year : new int[] {1900, 1950, 2000, 2026, 2027, 2050, 2100}) {
            Instant when = Instant.parse(year + "-07-01T00:00:00Z");
            SolarSystemService.SunObservation sun = sun(oslo(when));
            p(f("| %d | %+.4f |", year, signed(ofDateLongitude(sun)
                    - services.solar().solarLongitudeJ2000Degrees(when))));
        }
        p("");
    }

    private static double wholeSkyPixelsPerDegreeAtCentre() throws Exception {
        WholeSkyComposition.Page page = WholeSkyComposition.paint(
                new WholeSkyComposition.Ask(oslo(DECEMBER), ChartPalette.WHITE_PAPER, "en", 8.0,
                        true, true, FIELD, WHOLE_W, WHOLE_H), services);
        double[] out = new double[1];
        SwingUtilities.invokeAndWait(() -> {
            DrawnPage drawn = DrawnPage.of(page.chart().currentScene());
            ViewportMapping mapping = new ViewportMapping(drawn);
            double centre = Ecliptic.toEcliptic(drawn.projection().centre()).longitudeDegrees();
            PixelPoint a = pixel(drawn.projection(), mapping, centre - 0.5, 0.0);
            PixelPoint b = pixel(drawn.projection(), mapping, centre + 0.5, 0.0);
            out[0] = Math.hypot(a.x() - b.x(), a.y() - b.y());
        });
        return out[0];
    }

    private static void placeAndTimeSection() throws Exception {
        p("## 6. Chart time moves the marker; place does not");
        p("");
        double wholePerDegree = wholeSkyPixelsPerDegreeAtCentre();
        p("The solar longitude the atlas evaluates is geocentric. The Sun the chart draws is"
                + " the observer's. The greatest difference between the two longitudes over"
                + " 24 hours sampled every ten minutes, from two places:");
        p("");
        p("| day (UTC) | place | greatest difference, arcseconds | on an 8 degree page, px |");
        p("|---|---|---|---|");
        for (Instant day : List.of(DECEMBER, JUNE_NOON)) {
            for (double[] place : new double[][] {{OSLO_LAT, OSLO_LON}, {CAPE_LAT, CAPE_LON},
                {0.0, 0.0}}) {
                double greatest = 0.0;
                for (int i = 0; i < 144; i++) {
                    Instant when = day.plus(Duration.ofMinutes(10L * i));
                    double topocentric = sunLongitude(new Observer(place[0], place[1], when));
                    greatest = Math.max(greatest, Math.abs(signed(topocentric
                            - services.solar().solarLongitudeJ2000Degrees(when))));
                }
                p(f("| %s | %.2f, %.2f | %.1f | %.2f |", MINUTE.format(day).substring(0, 10),
                        place[0], place[1], greatest * 3600.0, greatest * W / 8.0));
            }
        }
        p("");
        p("Against that, the marker's movement with chart time:");
        p("");
        p("| from (UTC) | after | solar longitude moves | on the whole sky, px"
                + " | on a 60 degree page, px | on an 8 degree page, px |");
        p("|---|---|---|---|---|---|");
        for (Instant from : List.of(DECEMBER, JUNE_NOON)) {
            for (Duration step : List.of(Duration.ofHours(1), Duration.ofDays(1),
                    Duration.ofDays(30))) {
                double moves = signed(services.solar().solarLongitudeJ2000Degrees(from.plus(step))
                        - services.solar().solarLongitudeJ2000Degrees(from));
                p(f("| %s | %s | %.3f | %.1f | %.1f | %.0f |", MINUTE.format(from),
                        step.toHours() < 24 ? step.toHours() + " h" : step.toDays() + " d",
                        moves, moves * wholePerDegree, moves * W / 60.0, moves * W / 8.0));
            }
        }
        p("");
    }

    private static void sunSection(List<Drawn> ordinary) {
        p("## 7. The marker and the Sun are the same place");
        p("");
        p("The Sun's centre, as the chart draws it, lies on the scale at the solar longitude:"
                + " that is what the solar longitude is. So with the Sun shown the marker"
                + " never stands apart from it, and the Sun's drawn disc is true to scale"
                + " (C1), from a pixel and a half on the whole sky to most of a close page:");
        p("");
        p("| page | Sun's drawn radius, px | a long tick's half length | carets' tips from the"
                + " line |");
        p("|---|---|---|---|");
        for (Drawn d : ordinary) {
            if (d.marker() == null || d.name().contains("long-tick")
                    || d.name().contains("below") || d.name().contains("step-1")) {
                continue;
            }
            p(f("| `%s.png` | %.1f | %.0f | %.1f |", d.name(), d.sunRadiusPx(), LONG_HALF,
                    Math.max(CARET_TIP, d.sunRadiusPx() + CARET_CLEAR_OF_SUN)));
        }
        p("");
        p(f("A long tick of fixed length is inside the Sun's disc once the radius passes %.0f"
                + " pixels, a field of about %.0f degrees on this page. The carets are"
                + " mocked standing %.0f pixels clear of the drawn limb, so they move apart"
                + " as the disc grows and always bracket it.", LONG_HALF,
                0.2666 * W / LONG_HALF, CARET_CLEAR_OF_SUN));
        p("");
        p("How far the Sun's centre stands off the J2000 ecliptic the scale is drawn on, the"
                + " greatest over each year sampled daily at 00:00 UTC, geocentric direction"
                + " as Oslo sees it:");
        p("");
        p("| year | greatest ecliptic latitude of the Sun, arcseconds | on an 8 degree page, px |");
        p("|---|---|---|");
        for (int year : new int[] {1900, 2000, 2027, 2100}) {
            double greatest = 0.0;
            Instant start = Instant.parse(year + "-01-01T00:00:00Z");
            for (int d = 0; d < 365; d++) {
                Observer observer = oslo(start.plus(Duration.ofDays(d)));
                SkyPosition sun = Ecliptic.toEquatorial(sunLongitude(observer), 0.0);
                SkyPosition drawn = sun(observer).astrometricJ2000();
                greatest = Math.max(greatest, Math.abs(Ecliptic.latitudeDegrees(drawn)));
                if (sun.separationDegrees(drawn) > 1.0) {
                    throw new IllegalStateException("the Sun is not near its own longitude");
                }
            }
            p(f("| %d | %.0f | %.1f |", year, greatest * 3600.0, greatest * W / 8.0));
        }
        p("");
    }

    private static void landmarkSection() throws Exception {
        p("## 8. The marker and the four landmarks");
        p("");
        double wholePerDegree = wholeSkyPixelsPerDegreeAtCentre();
        double reach = CardinalLandmark.DIAMOND + CARET_HALF_BASE;
        p(f("The marker passes each landmark once a year. A caret's half base is %.1f pixels"
                + " and a landmark's diamond reaches %.1f from its point, so the two touch"
                + " while they are less than %.1f pixels apart. For how long, in 2027:",
                CARET_HALF_BASE, CardinalLandmark.DIAMOND, reach));
        p("");
        p("| page | pixels per degree | degrees within which they touch | days a year, the"
                + " four landmarks together |");
        p("|---|---|---|---|");
        for (double[] page : new double[][] {{0.0, wholePerDegree}, {120.0, W / 120.0},
            {60.0, W / 60.0}, {24.0, W / 24.0}, {8.0, W / 8.0}}) {
            double within = reach / page[1];
            int hours = 0;
            Instant start = Instant.parse("2027-01-01T00:00:00Z");
            for (int h = 0; h < 365 * 24; h++) {
                double lambda = services.solar().solarLongitudeJ2000Degrees(
                        start.plus(Duration.ofHours(h)));
                double toLandmark = Math.abs(signed(lambda - Math.rint(lambda / 90.0) * 90.0));
                if (toLandmark < within) {
                    hours++;
                }
            }
            p(f("| %s | %.2f | %.2f | %.1f |", page[0] == 0.0 ? "whole sky, at its centre"
                    : f("%.0f degrees", page[0]), page[1], within, hours / 24.0));
        }
        p("");
    }

    private static void radiantSection() throws Exception {
        p("## 9. The marker and the radiants");
        p("");
        double wholePerDegree = wholeSkyPixelsPerDegreeAtCentre();
        p(f("A radiant's mark reaches %.0f pixels from its point (#716). The least distance on"
                + " the sky between each shower's radiant and the Sun's place on the scale"
                + " while the shower is active, sampled at every tenth of a degree of solar"
                + " longitude:", ReferenceInk.RADIANT_OUTER));
        p("");
        p("| shower | least distance from the Sun's place, degrees | at solar longitude"
                + " | on the whole sky at its centre scale, px |");
        p("|---|---|---|---|");
        double least = Double.MAX_VALUE;
        for (MeteorShower s : showers.showers()) {
            double span = SkyFrame.normalise(s.endLongitude() - s.startLongitude());
            double nearest = Double.MAX_VALUE;
            double at = 0.0;
            for (double x = 0.0; x <= span; x += 0.1) {
                double lambda = SkyFrame.normalise(s.startLongitude() + x);
                double apart = MeteorShowers.radiantAt(s, lambda).at()
                        .separationDegrees(Ecliptic.toEquatorial(lambda, 0.0));
                if (apart < nearest) {
                    nearest = apart;
                    at = lambda;
                }
            }
            least = Math.min(least, nearest);
            p(f("| %s | %.1f | %.1f | %.0f |", s.code(), nearest, at, nearest * wholePerDegree));
        }
        p("");
        p(f("No radiant of the nine comes within %.0f degrees of the Sun's place while its"
                + " shower is active.", Math.floor(least)));
        p("");
    }

    private static void exportSection() {
        p("## 10. Page units and export");
        p("");
        p(f("Every size in the mock is a page-pixel constant, as the meridian graduation's"
                + " stroke (%.0f either side) and the radiant's rays (%.1f to %.1f) are: a"
                + " stroke of %.0f, a figure in the %d point grid-label font, a caret %.0f"
                + " high on a base of %.0f. None of them scales with the field. Export draws"
                + " the page through the one reference layer, as it does the meridian's"
                + " graduation; a production graduation contributed as geometry would be"
                + " exported by the same route with no export code of its own.", HALF_TICK,
                ReferenceInk.RADIANT_INNER, ReferenceInk.RADIANT_OUTER, HALF_TICK,
                FIGURE_FONT.getSize(), CARET_HEIGHT, 2.0 * CARET_HALF_BASE));
        p("");
    }

    private static void behaviourSection() {
        p("## 11. What already exists to build on");
        p("");
        p("- **The meridian's graduation (#515)** is the precedent in every respect but one:"
                + " a tick is furniture, carries no words, is selected by nothing, is in no"
                + " inventory, takes a clean box for its figure or goes without, and rides"
                + " its line. Its contribution type states an altitude between 0 and 90, so"
                + " it cannot carry a longitude as it stands.");
        p("- **The ecliptic module has no date** by decision: it has no observer and no"
                + " instant, and the ecliptic type offers no epoch parameter. A scale in"
                + " J2000 longitudes needs neither. A marker needs the chart time, which the"
                + " ecliptic module was built not to have.");
        p("- **The solar longitude is already computed and worded**: the meteor panel states"
                + " it at chart time in both languages (\"solar longitude\", \"sollengde\"),"
                + " and the Sun's own table carries its J2000 ecliptic longitude.");
        p("- **The ecliptic's switch is remembered between sessions**; the observer's lines"
                + " and the meridian's ticks are off at every session start.");
        p("");
    }

    private static void rulingsSection() {
        p("## 12. Questions for the ruling");
        p("");
        p("The questions, each with a proposal and what this record measured for it, are in"
                + " `README.md` beside this record. The owner rules; nothing is built before"
                + " that.");
        p("");
    }
}
