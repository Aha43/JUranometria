package juranometria.tool;

import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import juranometria.bookmark.BookmarkDocument;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.Library;
import juranometria.bookmark.Node;
import juranometria.bookmark.Place;
import juranometria.bookmark.SavedView;
import juranometria.bookmark.Subject;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.CurveRun;
import juranometria.project.DrawnPage;
import juranometria.project.GreatCirclePage;
import juranometria.project.HammerProjection;
import juranometria.project.PageRegion;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.sheet.PaperSize;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.ui.ReferenceInk;

/**
 * The whole sky at one chart time, as built (#687, ruled on #686).
 *
 * <p>Every page is the application's own: a production chart with the
 * production modules attached, entered through the one whole-sky
 * switch, drawn by the Hammer projection as a real kind. What this
 * measures is the ruling held - nine of nine bodies, the central
 * meridian at the zenith's ecliptic longitude, every great circle cut
 * once at the seam and nowhere else, line ink quieted on the true side
 * of the horizon, no name stranded, the ruled words on the page and
 * spoken, Open here landing where it says - and what it costs.
 *
 * <p>{@code measurements.md} (standard output) holds geometry, counts
 * that follow from geometry, and words: pinned, because none of it
 * depends on a machine's fonts. {@code platform.md} holds what does -
 * placed names, label counts, time and heap - and is held to
 * reproducing here. Regenerate with {@code make whole-sky-study}.
 */
public final class WholeSkyStudyMain {

    static final Path OUT = Path.of("docs/studies/whole-sky");
    static final int MONITOR_W = 2000;
    static final int MONITOR_H = 1000;
    static final int LAPTOP_W = 1440;
    static final int LAPTOP_H = 720;
    /** The reader's limit before entering; the whole sky takes V 5.0 from it (R-1's rule). */
    static final double READER_LIMIT = 8.0;
    /** The ordinary field the reader leaves, and Open here returns to. */
    static final double ORDINARY_FIELD = 60.0;
    static final double OSLO_LAT = 59.91;
    static final double OSLO_LON = 10.75;
    /** A name farther than this from any of its figure's ink is stranded. */
    static final double STRANDED_PX = 60.0;

    // Per run, set at the top of main: the evidence contract runs this
    // main twice in one JVM with System.out captured.
    private static PrintStream outStream;
    private static StringBuilder platform;

    private static void p(String s) {
        outStream.println(s);
    }

    private static void plat(String s) {
        platform.append(s).append('\n');
    }

    private static String f(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    /**
     * The chart times, found by the application in the #686 study
     * (docs/studies/all-sky/measurements.md, section 2: a search over
     * the modules' own contributions, never typed) and carried here by
     * citation.
     */
    record Case(String id, Instant instant, String found) {
        Observer oslo() {
            return new Observer(OSLO_LAT, OSLO_LON, instant);
        }
    }

    static final List<Case> CASES = List.of(
            new Case("evening", Instant.parse("2026-12-15T21:00:00Z"),
                    "an Oslo evening with six of the eight bodies other than the Sun"
                            + " up in darkness"),
            new Case("gathering", Instant.parse("2027-04-25T00:00:00Z"),
                    "the seven planets within 138.7 degrees of ecliptic longitude"),
            new Case("spread", Instant.parse("2029-01-03T00:00:00Z"),
                    "no gap among the nine bodies wider than 64.2 degrees"));

    /** One page the study draws. */
    record Spec(String name, Case c, ChartPalette palette, String language,
                boolean subdue, int widthPx, int heightPx, String caption) {
    }

    static List<Spec> specs() {
        Case evening = CASES.get(0);
        List<Spec> specs = new ArrayList<>();
        specs.add(new Spec("evening-en-white", evening, ChartPalette.WHITE_PAPER, "en", true,
                MONITOR_W, MONITOR_H, "the evening case, English, white paper, external monitor"));
        specs.add(new Spec("evening-nb-black", evening, ChartPalette.BLACK_SKY, "nb-NO", true,
                MONITOR_W, MONITOR_H, "the same, Norwegian, black sky"));
        specs.add(new Spec("evening-en-black", evening, ChartPalette.BLACK_SKY, "en", true,
                MONITOR_W, MONITOR_H, "English, black sky"));
        specs.add(new Spec("evening-nb-white", evening, ChartPalette.WHITE_PAPER, "nb-NO", true,
                MONITOR_W, MONITOR_H, "Norwegian, white paper"));
        specs.add(new Spec("evening-en-white-laptop", evening, ChartPalette.WHITE_PAPER, "en",
                true, LAPTOP_W, LAPTOP_H, "the evening case at a laptop's extent"));
        specs.add(new Spec("evening-en-white-subdue-off", evening, ChartPalette.WHITE_PAPER,
                "en", false, MONITOR_W, MONITOR_H, "Subdue below horizon off"));
        specs.add(new Spec("gathering-en-white", CASES.get(1), ChartPalette.WHITE_PAPER, "en",
                true, MONITOR_W, MONITOR_H, "the gathering case"));
        specs.add(new Spec("spread-en-white", CASES.get(2), ChartPalette.WHITE_PAPER, "en",
                true, MONITOR_W, MONITOR_H, "the spread case"));
        return specs;
    }

    /**
     * A run's end counts as on the outline when its normalised residual is
     * within this. The residual is machine precision (1e-16 on one platform,
     * 2e-16 on another), so the report states the claim, not the noise.
     */
    private static final double ON_THE_OUTLINE_TOLERANCE = 1e-12;
    private static final String ON_THE_OUTLINE = "1e-12";

    /** One great circle on a page: how the sampled route drew it. */
    record Curve(String name, int runs, boolean closed, double endResidualMax,
                 double longestChordPx) {
    }

    /** What one page measured. */
    record Measured(double centralLongitude, double zenithLatitude, double zenithFromCentre,
                    Map<String, ReferenceInk.BodyPlacement> bodies, List<String> stranded,
                    int namedConstellations, int figureJumps, int gridJumps, int gridRaFigures,
                    int gridDecFigures, int gridFiguresOnOutline, int gridCurves,
                    int gridEndsOnOutline, int gridEndsOffOutline,
                    double subdueDisagreement, List<String> title, String spoken,
                    List<Curve> curves, long paintMillis) {
    }

    record Drawn(Spec spec, WholeSkyComposition.Page page, Measured measured) {
    }

    public static void main(String[] args) throws Exception {
        outStream = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        platform = new StringBuilder();
        Files.createDirectories(OUT);
        WholeSkyComposition.Services services = new WholeSkyComposition.Services();

        p("# The whole sky at one chart time, as built (#687)");
        p("");
        p("Sprint 59, issue #687, road #703; ruled on #686 on 2026-11-08"
                + " (`docs/decisions/all-sky-projection.md`, section 7). Every page here is the"
                + " application's own: a production chart with the production modules attached,"
                + " entered through the one Whole sky switch, drawn by the Hammer projection as"
                + " a real kind. Geometry, counts that follow from geometry, and words are"
                + " pinned here; what depends on this machine's fonts is in `platform.md`."
                + " Regenerate with `make whole-sky-study`.");
        p("");

        PlatformEvidence.preface(platform,
                "The whole sky at one chart time (#687) - platform record",
                "Sprint 59, issue #687.");
        plat("Counts of placed text on this machine's fonts, from `make whole-sky-study`."
                + " Held to reproducing here, not pinned to bytes. The run's time and heap"
                + " are printed to standard error and recorded nowhere, because they"
                + " reproduce on no machine.");
        plat("");
        plat("| page | bodies named | names refused | constellation names anchored | stranded"
                + " | RA figures | Dec figures |");
        plat("|---|---|---|---|---|---|---|");

        List<Drawn> drawn = new ArrayList<>();
        for (Spec spec : specs()) {
            Drawn d = draw(spec, services);
            drawn.add(d);
            long named = d.measured().bodies().values().stream()
                    .filter(b -> b.box() != null).count();
            List<String> refused = d.measured().bodies().values().stream()
                    .filter(b -> b.box() == null).map(ReferenceInk.BodyPlacement::identity)
                    .sorted().toList();
            plat(f("| `%s` | %d | %s | %d | %d | %d | %d |", spec.name(), named,
                    refused.isEmpty() ? "-" : String.join(", ", refused),
                    d.measured().namedConstellations(), d.measured().stranded().size(),
                    d.measured().gridRaFigures(), d.measured().gridDecFigures()));
            System.err.printf(Locale.ROOT, "%s painted in %d ms%n", spec.name(),
                    d.measured().paintMillis());
        }

        rulingSection(drawn);
        casesSection();
        accountingSection(drawn);
        meridianSection(drawn);
        seamSection(drawn);
        subdueSection(drawn);
        wordsSection(drawn);
        bookmarkSection();
        journeySection(services);
        pagesSection(drawn);
        costSection();
        boundarySection(services);

        PlatformEvidence.write(platform, OUT.resolve("platform.md").toString());
    }

    // ------------------------------------------------------------------ drawing

    private static Drawn draw(Spec spec, WholeSkyComposition.Services services)
            throws Exception {
        long t0 = System.nanoTime();
        WholeSkyComposition.Page page = WholeSkyComposition.paint(
                new WholeSkyComposition.Ask(spec.c().oslo(), spec.palette(), spec.language(),
                        READER_LIMIT, true, spec.subdue(), ORDINARY_FIELD, spec.widthPx(),
                        spec.heightPx()), services);
        long paintMillis = (System.nanoTime() - t0) / 1_000_000L;
        Measured[] out = new Measured[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                out[0] = measure(spec, page, paintMillis);
                ImageIO.write(page.image(), "png", OUT.resolve(spec.name() + ".png").toFile());
            } catch (Exception e) {
                throw new RuntimeException(spec.name(), e);
            }
        });
        return new Drawn(spec, page, out[0]);
    }

    private static Measured measure(Spec spec, WholeSkyComposition.Page page,
                                    long paintMillis) {
        ChartScene scene = page.chart().currentScene();
        DrawnPage drawnPage = DrawnPage.of(scene);
        Projection projection = drawnPage.projection();
        Projection.WholeSphere whole = projection.wholeSphere().orElseThrow();
        Observer observer = page.meridian().observer();
        LocalSky sky = new LocalSky(observer);
        SkyPosition zenith = sky.zenith();
        Ecliptic.Coordinates zenithEcliptic = Ecliptic.toEcliptic(zenith);

        Map<String, ReferenceInk.BodyPlacement> bodies = new LinkedHashMap<>();
        for (ReferenceInk.BodyPlacement b : page.chart().renderedBodies()) {
            bodies.put(b.identity(), b);
        }
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, page.words());
        ChartOptions options = page.chart().drawnOptions();
        double jump = 0.4 * spec.widthPx();
        int figureJumps = 0;
        int named = 0;
        List<String> stranded = new ArrayList<>();
        for (ChartRenderer.FigureInk ink : renderer.figureInk(scene, options).values()) {
            figureJumps += jumps(ink.ink().getPathIterator(null), jump);
            if (ink.nameAnchor() != null) {
                named++;
                if (nearestInk(ink.ink().getPathIterator(null), ink.nameAnchor()) > STRANDED_PX) {
                    stranded.add(ink.constellationId());
                }
            }
        }
        stranded.sort(String::compareTo);
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D sg = scratch.createGraphics();
        sg.setFont(ChartRenderer.labelFont());
        EquatorialGrid.Grid grid = renderer.gridFor(sg.getFontMetrics(), scene, options);
        sg.dispose();
        int gridJumps = 0;
        for (List<List<PixelPoint>> family : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> run : family) {
                for (int i = 1; i < run.size(); i++) {
                    if (Math.abs(run.get(i).x() - run.get(i - 1).x()) > jump) {
                        gridJumps++;
                    }
                }
            }
        }
        ViewportMapping mapping = new ViewportMapping(drawnPage);

        // The graticule's notation (R-15): how many figures of each
        // kind, whether any box reaches the outline (none may: an RA
        // figure is wholly outside it, a Dec figure wholly inside),
        // and the grid's own seam ends - every piece end is either on
        // the outline to 1e-9 or one of a curve's two sampling ends.
        java.awt.geom.Ellipse2D outline = EquatorialGrid.outlineOf(projection, mapping);
        java.awt.FontMetrics gridMetrics = EquatorialGrid.labelMetrics();
        int gridRa = 0;
        int gridDec = 0;
        int gridOnOutline = 0;
        for (EquatorialGrid.Label label : grid.labels()) {
            java.awt.geom.Rectangle2D box = EquatorialGrid.labelBounds(label, gridMetrics);
            if (label.text().contains("h")) {
                gridRa++;
            } else {
                gridDec++;
            }
            if (outline.intersects(box) && !outline.contains(box)) {
                gridOnOutline++;
            }
        }
        int endsOn = 0;
        int endsOff = 0;
        for (List<List<PixelPoint>> family : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> run : family) {
                for (PixelPoint end : List.of(run.get(0), run.get(run.size() - 1))) {
                    double ex = (end.x() - outline.getCenterX()) / (outline.getWidth() / 2.0);
                    double ey = (end.y() - outline.getCenterY()) / (outline.getHeight() / 2.0);
                    if (Math.abs(ex * ex + ey * ey - 1.0) < 1e-9) {
                        endsOn++;
                    } else {
                        endsOff++;
                    }
                }
            }
        }
        int gridCurves = (int) Math.round(360.0 / grid.spec().raStepDegrees())
                + 2 * (int) Math.floor(89.5 / grid.spec().decStepDegrees()) + 1;

        // The sampled route's curves: the ecliptic, the equator, the
        // horizon and the meridian, as the reference layer asks for them.
        PageRegion region = mapping.regionFor(scene.viewport(), projection);
        List<Curve> curves = new ArrayList<>();
        curves.add(curve("ecliptic", projection, mapping, region, Ecliptic.POLE));
        curves.add(curve("celestial equator", projection, mapping, region,
                new SkyPosition(0.0, 90.0)));
        curves.add(curve("horizon", projection, mapping, region, sky.horizon().pole()));
        curves.add(curve("meridian", projection, mapping, region, sky.meridian().pole()));

        // Line ink is split by BelowHorizon's region, built from the
        // horizon's drawn runs; compare it with the true side of the
        // horizon at sample pixels inside the outline.
        double disagreement = Double.NaN;
        if (spec.subdue()) {
            BelowHorizon split = BelowHorizon.of(zenith, scene);
            java.awt.Shape below = split.belowRegion();
            int sampled = 0;
            int wrong = 0;
            for (int py = 4; py < spec.heightPx(); py += 8) {
                for (int px = 4; px < spec.widthPx(); px += 8) {
                    SkyPosition at = ChartHitTest.skyAt(drawnPage, px, py);
                    if (at == null) {
                        continue;
                    }
                    sampled++;
                    if (below.contains(px, py) != (sky.altitudeDegrees(at) < 0.0)) {
                        wrong++;
                    }
                }
            }
            disagreement = sampled == 0 ? 0.0 : (double) wrong / sampled;
        }
        return new Measured(whole.centralLongitudeDegrees(), zenithEcliptic.latitudeDegrees(),
                projection.centre().separationDegrees(zenith), bodies, stranded, named,
                figureJumps, gridJumps, gridRa, gridDec, gridOnOutline, gridCurves, endsOn,
                endsOff, disagreement,
                renderer.titleBlockLines(scene),
                page.chart().getAccessibleContext().getAccessibleDescription(), curves,
                paintMillis);
    }

    private static Curve curve(String name, Projection projection, ViewportMapping mapping,
                               PageRegion region, SkyPosition pole) {
        List<CurveRun> runs = GreatCirclePage.clip(projection, mapping, region, pole);
        boolean closed = false;
        double residual = 0.0;
        double longest = 0.0;
        for (CurveRun run : runs) {
            CurveRun.Polyline line = (CurveRun.Polyline) run;
            closed |= line.closed();
            for (Optional<PixelPoint> end : List.of(line.from(), line.to())) {
                if (end.isPresent()) {
                    double x = (end.get().x() - region.limbX()) / region.limbRadius();
                    double y = (end.get().y() - region.limbY()) / region.limbRadiusAcross();
                    residual = Math.max(residual, Math.abs(x * x + y * y - 1.0));
                }
            }
            List<PixelPoint> points = line.points();
            for (int i = 1; i < points.size(); i++) {
                longest = Math.max(longest, Math.hypot(points.get(i).x() - points.get(i - 1).x(),
                        points.get(i).y() - points.get(i - 1).y()));
            }
        }
        return new Curve(name, runs.size(), closed, residual, longest);
    }

    // ------------------------------------------------------------------ sections

    private static void rulingSection(List<Drawn> drawn) {
        p("## 1. The ruling, held");
        p("");
        p("| ruling | as built | held by |");
        p("|---|---|---|");
        p("| R-1 admit | the whole sky is a chart the application draws | every page here |");
        p("| R-2 Hammer | `ChartProjection.HAMMER`, `HammerProjection` in `juranometria.project` | `HammerProjectionTest` (round trip, equal area, the outline) |");
        p("| R-3 ecliptic horizontal | the ecliptic is the middle line, drawn straight in one run | section 5 |");
        p("| R-4 observer-aligned | the central meridian is the zenith's ecliptic longitude; the zenith keeps its latitude | section 4 |");
        p("| R-5 the seam | a join rule (`Projection.mayJoin`), never a hidden sliver: every curve cut once, no chord across the page, no name stranded | sections 5 and 10, `platform.md` |");
        p("| R-6 curves | the sampled route (`GreatCirclePage`, `CurveRun.Polyline`) for the one projection without conics; every conic page byte-identical | `SampledCurveRouteTest`, the interface evidence gate |");
        p("| R-7 horizon and Subdue | the true horizon, line ink split by its drawn runs | section 6 |");
        p("| R-8 Open here | the ordinary chart at the field left, through the controller's recentre | section 9 |");
        p("| R-9 zoom, pan, Home | refused with their explanations; Home the ordinary Home; the meridian readout refused | `WholeSkyControllerTest`, `WholeSkyPageTest` |");
        p("| R-10 not persisted | the session holds the mode; nothing stores it | `WholeSkySession` |");
        p("| R-11 bookmarks B-2 | `\"view\": {\"mode\": \"whole-sky\"}`, time and place required, schema version 1 | section 8, `WholeSkyBookmarkTest` |");
        p("| R-12 words | the ruled sentence and the central longitude; no Centre or Field claim | section 7 |");
        p("| R-13 one toggle | Whole sky on the toolbar, the Controller and the Chart menu, over one `WholeSkySession` | `ChartControls`, `AppMenuBar` |");
        p("| R-14 On this page | refused in the mode, pointing to Open here | `WholeSkyPageTest` |");
        p("| R-15 grid notation | outline-aware, refusing independently: RA figures on the paper"
                + " where a meridian meets the outline, outward along its normal; Dec figures"
                + " inside the sky where a parallel crosses the central meridian; none on the"
                + " outline, none across the seam | section 5, `WholeSkyGridTest` |");
        p("| architecture A | one `Projection` interface, widened by an outline, a join rule, a sampled curve route and a whole-sphere statement; the modules unchanged | `HammerProjectionTest` |");
        p("");
    }

    private static void casesSection() {
        p("## 2. Cases");
        p("");
        p("Oslo (" + OSLO_LAT + " N, " + OSLO_LON + " E), the three chart times the #686 study"
                + " found by searching the modules' own contributions"
                + " (`docs/studies/all-sky/measurements.md`, section 2), carried by citation.");
        p("");
        p("| case | instant (UTC) | found as |");
        p("|---|---|---|");
        for (Case c : CASES) {
            p(f("| %s | %s | %s |", c.id(), c.instant(), c.found()));
        }
        p("");
        p("The reader's limit before entering is V " + f("%.1f", READER_LIMIT)
                + "; the whole sky takes the brighter of that and its own V 5.0. The ordinary"
                + " field left is " + f("%.0f", ORDINARY_FIELD) + " degrees.");
        p("");
    }

    private static void accountingSection(List<Drawn> drawn) {
        p("## 3. Accounting: nine of nine");
        p("");
        p("Every enabled body on every page, with its place in the map's frame: ecliptic"
                + " longitude from the central meridian (positive left), ecliptic latitude, and"
                + " its altitude.");
        p("");
        for (Drawn d : drawn) {
            if (!d.spec().name().endsWith("-en-white") || d.spec().widthPx() != MONITOR_W) {
                continue;
            }
            Case c = d.spec().c();
            LocalSky sky = new LocalSky(c.oslo());
            double central = d.measured().centralLongitude();
            p(f("### %s: %d bodies drawn", c.id(), d.measured().bodies().size()));
            p("");
            p("| body | from the central meridian | ecliptic latitude | altitude | drawn as |");
            p("|---|---|---|---|---|");
            for (ReferenceInk.BodyPlacement b : d.measured().bodies().values()) {
                SkyPosition at = ChartHitTest.skyAt(DrawnPage.of(d.page().chart().currentScene()),
                        b.centre().x(), b.centre().y());
                Ecliptic.Coordinates e = Ecliptic.toEcliptic(at);
                double off = ((e.longitudeDegrees() - central) % 360.0 + 540.0) % 360.0 - 180.0;
                p(f("| %s | %+.1f | %+.1f | %+.1f%s | %s |", b.identity(), off,
                        e.latitudeDegrees(), sky.altitudeDegrees(at),
                        b.belowHorizon() ? " (below)" : "",
                        b.symbol() ? "symbol" : "disc"));
            }
            p("");
        }
    }

    private static void meridianSection(List<Drawn> drawn) {
        p("## 4. The central meridian (R-4)");
        p("");
        p("Observer-aligned: the central meridian is the ecliptic longitude of the zenith,"
                + " and the zenith lies on it at its own ecliptic latitude - not at the map's"
                + " geometric centre.");
        p("");
        p("| case | central longitude | zenith's ecliptic longitude | zenith's ecliptic latitude"
                + " | zenith from the map centre |");
        p("|---|---|---|---|---|");
        for (Drawn d : drawn) {
            if (!d.spec().name().endsWith("-en-white") || d.spec().widthPx() != MONITOR_W) {
                continue;
            }
            SkyPosition zenith = new LocalSky(d.spec().c().oslo()).zenith();
            p(f("| %s | %.4f | %.4f | %+.2f | %.2f degrees |", d.spec().c().id(),
                    d.measured().centralLongitude(),
                    Ecliptic.toEcliptic(zenith).longitudeDegrees(),
                    d.measured().zenithLatitude(), d.measured().zenithFromCentre()));
        }
        p("");
    }

    private static void seamSection(List<Drawn> drawn) {
        p("## 5. The seam and the sampled curves (R-5, R-6)");
        p("");
        p("Each reference great circle as the curve seam draws it on the whole sky: the number"
                + " of runs (one, when the seam cuts it once), whether any closed, whether every"
                + " run's end lies on the outline (`|x^2/a^2 + y^2/b^2 - 1|` at most"
                + " `" + ON_THE_OUTLINE + "` after refinement to the seam; the residual itself is"
                + " machine precision and differs between platforms, so it is not pinned), and"
                + " the longest chord between samples in pixels. Figure pieces and grid pieces"
                + " that would cross more than 40 per cent of the page are counted as jumps.");
        p("");
        p("| page | curve | runs | closed | on the outline (<= " + ON_THE_OUTLINE
                + ") | longest chord px |");
        p("|---|---|---|---|---|---|");
        for (Drawn d : drawn) {
            for (Curve c : d.measured().curves()) {
                p(f("| `%s` | %s | %d | %s | %s | %.2f |", d.spec().name(), c.name(), c.runs(),
                        c.closed() ? "yes" : "no",
                        c.endResidualMax() <= ON_THE_OUTLINE_TOLERANCE ? "yes" : "no",
                        c.longestChordPx()));
            }
        }
        p("");
        p("| page | figure jumps | grid jumps | constellation names anchored | stranded |");
        p("|---|---|---|---|---|");
        for (Drawn d : drawn) {
            p(f("| `%s` | %d | %d | %d | %s |", d.spec().name(), d.measured().figureJumps(),
                    d.measured().gridJumps(), d.measured().namedConstellations(),
                    d.measured().stranded().isEmpty() ? "none"
                            : String.join(", ", d.measured().stranded())));
        }
        p("");
        p("**The graticule and its notation (R-15).** The grid's own seam ends are refined to"
                + " the outline by bisection on the join rule, so every piece end is either on the"
                + " outline to 1e-9 or one of its curve's two sampling ends (a meridian's pole"
                + " ends, a parallel's start and stop at 0h): the ends off the outline are"
                + " exactly two per curve. The notation is outline-aware and refuses"
                + " independently - an RA figure stands on the paper outward along the outline's"
                + " normal where a meridian meets it, which is only ever at the seam; a Dec figure"
                + " stands inside the sky where a parallel crosses the central meridian; a figure"
                + " whose box would reach the outline, the paper's edge, furniture or another"
                + " figure is not placed. No figure's box reaches the outline, so none straddles"
                + " the seam. The counts of figures placed depend on this machine's fonts and are"
                + " in `platform.md`; what is pinned here is the geometry.");
        p("");
        p("| page | grid curves | piece ends on the outline | ends off the outline"
                + " | figures reaching the outline |");
        p("|---|---|---|---|---|");
        for (Drawn d : drawn) {
            p(f("| `%s` | %d | %d | %d (two per curve) | %d |", d.spec().name(),
                    d.measured().gridCurves(), d.measured().gridEndsOnOutline(),
                    d.measured().gridEndsOffOutline(), d.measured().gridFiguresOnOutline()));
        }
        p("");
    }

    private static void subdueSection(List<Drawn> drawn) {
        p("## 6. Subdue below horizon (R-7)");
        p("");
        p("Line ink is split by the region below the drawn horizon; marks and labels are toned"
                + " by their own position. The region is compared with the true side of the"
                + " horizon at every eighth pixel inside the outline.");
        p("");
        p("| page | sample pixels disagreeing |");
        p("|---|---|");
        for (Drawn d : drawn) {
            p(f("| `%s` | %s |", d.spec().name(), d.spec().subdue()
                    ? f("%.3f%% of the sky", 100.0 * d.measured().subdueDisagreement())
                    : "Subdue off: nothing is quieted"));
        }
        p("");
    }

    private static void wordsSection(List<Drawn> drawn) {
        p("## 7. Words (R-12)");
        p("");
        for (Drawn d : drawn) {
            if (!d.spec().name().equals("evening-en-white")
                    && !d.spec().name().equals("evening-nb-black")) {
                continue;
            }
            p(f("### `%s`", d.spec().name()));
            p("");
            p("Title block:");
            p("");
            for (String line : d.measured().title()) {
                p("- " + line);
            }
            p("");
            p("Spoken: \"" + d.measured().spoken() + "\"");
            p("");
            PaperSize paper = PaperSize.A4;
            String sheet = d.page().words().wholeSkySheetDescription(
                    d.page().words().projection(HammerProjection.NAME),
                    DrawnPage.centralLongitudeText(DrawnPage.of(d.page().chart().currentScene())
                            .projection().wholeSphere().orElseThrow()),
                    f("%.1f", d.page().controller().state().limitingMagnitude()),
                    d.page().words().paper(paper.identity(), f("%.1f", paper.wideMm()),
                            f("%.1f", paper.highMm()), f("%.1f", paper.marginMm()),
                            f("%.1f", paper.chartWideMm()), f("%.1f", paper.chartHighMm())),
                    d.page().words().ground(d.spec().palette().storedAs()));
            p("Exported sheet description: \"" + sheet + "\"");
            p("");
        }
        p("The view report's whole-sky lines (`field-degrees: whole-sky`, `view`,"
                + " `orientation`, `central-meridian`, `central-longitude-degrees`) are held"
                + " by `WholeSkyPageTest`; the report is a text feature outside the rendering"
                + " closure and is not drawn here.");
        p("");
    }

    private static void bookmarkSection() {
        p("## 8. A whole-sky bookmark (R-11, B-2)");
        p("");
        Case evening = CASES.get(0);
        Node.Bookmark bookmark = new Node.Bookmark("8a1f0f2e-0000-4000-8000-000000000687",
                "The whole sky, an Oslo evening", SavedView.wholeSkyView(),
                List.of(new Subject(Subject.Kind.BODY, "saturn")), Optional.empty(),
                Optional.of(new ChartTime(evening.instant(), ChartTime.Purpose.OBSERVATION)),
                Optional.of(new Place(OSLO_LAT, OSLO_LON)), Optional.empty());
        String text = BookmarkDocument.text(new Library("6.5.0-study", List.of(bookmark)));
        p("Written in the version-1 schema with the one optional member, no centre and no"
                + " field; its chart time and place are kept, because the page derives from"
                + " them. An older atlas keeps the record exactly as needing attention"
                + " (`an unknown field \"mode\"`); every ordinary bookmark is written as before.");
        p("");
        p("```json");
        p(text.strip());
        p("```");
        p("");
        BookmarkDocument.Read read = BookmarkDocument.read(text);
        Library back = ((BookmarkDocument.Usable) read).library();
        p("Read back: " + (back.root().get(0) instanceof Node.Bookmark b && b.view().wholeSky()
                ? "a whole-sky bookmark, byte-identical when written again: "
                        + text.equals(BookmarkDocument.text(back))
                : "NOT a whole-sky bookmark"));
        p("");
    }

    private static void journeySection(WholeSkyComposition.Services services) throws Exception {
        p("## 9. The journey: Open here (R-8)");
        p("");
        Case evening = CASES.get(0);
        WholeSkyComposition.Page page = WholeSkyComposition.paint(
                new WholeSkyComposition.Ask(evening.oslo(), ChartPalette.WHITE_PAPER, "en",
                        READER_LIMIT, true, true, ORDINARY_FIELD, MONITOR_W, MONITOR_H), services);
        ReferenceInk.BodyPlacement saturn = page.chart().renderedBodies().stream()
                .filter(b -> b.identity().equals("saturn")).findFirst().orElseThrow();
        SkyPosition where = ChartHitTest.skyAt(DrawnPage.of(page.chart().currentScene()),
                saturn.centre().x(), saturn.centre().y());
        double fieldBefore = ORDINARY_FIELD;
        boolean selectable = page.chart().selectableBodyAt(saturn.centre().x() + 1.0,
                saturn.centre().y()).map(b -> b.identity().equals("saturn")).orElse(false);
        ImageIO.write(page.image(), "png", OUT.resolve("journey-1-whole-sky.png").toFile());
        BufferedImage opened = WholeSkyComposition.openHere(page, where, MONITOR_W, MONITOR_H);
        ImageIO.write(opened, "png", OUT.resolve("journey-2-open-here.png").toFile());
        ChartViewState after = page.controller().state();
        p("From the evening page, Saturn is selected a pixel off its mark (" + selectable
                + ") and Open here is taken. The controller leaves the whole sky for the"
                + " ordinary chart at the field the reader left:");
        p("");
        p(f("- field before entering: %.0f degrees; field after Open here: %.0f degrees;"
                + " whole sky after: %s", fieldBefore, after.fieldWidthDegrees(), after.wholeSky()));
        p(f("- centred %.6f degrees from Saturn's position, by the %s projection",
                after.centre().separationDegrees(where), after.projection().displayName()));
        p("- `journey-1-whole-sky.png` before, `journey-2-open-here.png` after");
        p("");
        javax.swing.SwingUtilities.invokeAndWait(() -> page.session().leave());
        p("Leaving the whole sky without choosing restores the ordinary page exactly: "
                + page.controller().state().equals(after) + " (the state after Open here was"
                + " already ordinary, so there was nothing to restore; the session's return is"
                + " held by `WholeSkyControllerTest`).");
        p("");
    }

    private static void pagesSection(List<Drawn> drawn) {
        p("## 10. Pages");
        p("");
        p("| page | case | extent | palette | language | Subdue | bodies |");
        p("|---|---|---|---|---|---|---|");
        for (Drawn d : drawn) {
            p(f("| `%s.png` | %s | %dx%d | %s | %s | %s | %d |", d.spec().name(),
                    d.spec().c().id(), d.spec().widthPx(), d.spec().heightPx(),
                    d.spec().palette().storedAs(), d.spec().language(),
                    d.spec().subdue() ? "on" : "off", d.measured().bodies().size()));
        }
        p("| `journey-1-whole-sky.png` | evening | 2000x1000 | white-paper | en | on | 9 |");
        p("| `journey-2-open-here.png` | evening | 2000x1000 | white-paper | en | on | the ordinary 60-degree chart about Saturn |");
        p("");
        for (Drawn d : drawn) {
            p("- `" + d.spec().name() + ".png`: " + d.spec().caption());
        }
        p("");
    }

    private static void costSection() {
        long peak = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                peak += pool.getPeakUsage().getUsed();
            }
        }
        System.err.printf(Locale.ROOT,
                "heap peak over the run: %.2f GB (the sum of the heap pools' peak usage)%n",
                peak / 1e9);
        p("## 11. Cost");
        p("");
        p("Draw times per page and the heap peak are printed to standard error by the run,"
                + " and pinned nowhere: the #686 study measured 8 s and 0.95 GB for 20"
                + " candidate pages on the owner's machine, and the evidence heap limit is"
                + " re-measured before any evidence-heavy road (`juranometria-heap-limit`).");
        p("");
    }

    /** One place to stand near a pole: a step in latitude and in east longitude, in degrees. */
    record Step(String stands, double north, double east, boolean longitudeDetermined) {

        /**
         * A determined step in latitude keeps the pole on the observer's
         * meridian, so that meridian is the central meridian and the seam.
         */
        boolean meridianThroughThePole() {
            return longitudeDetermined && east == 0.0;
        }
    }

    /**
     * Where the central-meridian rule runs out (#709): the zenith at and
     * around each ecliptic pole. Within about 1e-12 degrees of a pole
     * the zenith's ecliptic longitude is decided by rounding, so those
     * rows state only what must hold whatever longitude was chosen;
     * from 1e-9 degrees out the longitude is the zenith's own and is
     * printed. Then how the central meridian moves through a sidereal
     * day by latitude, which is the same rule seen from real places.
     */
    private static void boundarySection(WholeSkyComposition.Services services) throws Exception {
        Instant when = CASES.get(0).instant();
        p("## 12. The zenith at and near an ecliptic pole (#709)");
        p("");
        p("The central meridian is the zenith's ecliptic longitude (R-4), which is undefined at"
                + " an ecliptic pole and turns fast near one. Each row is the production page"
                + " composed for an observer whose zenith stands where the row says, at "
                + when + ": at the pole to rounding (`WholeSkyBoundary.observerUnder`), or a"
                + " step from it in latitude or in east longitude. Within about 1e-12 degrees"
                + " of the pole the longitude is decided by rounding and is not printed: every"
                + " meridian passes through the pole, so any choice is a correct map, and the"
                + " rule does not pick one. `yes` means, column by column: the zenith is"
                + " drawn within a millionth of a pixel of the central meridian; the ecliptic,"
                + " the celestial equator, the horizon and the observer's meridian are finite"
                + " with no chord between samples over 8 px; the ecliptic and the equator are"
                + " one run each; the region below the drawn horizon disagrees with the true"
                + " side of it on under 0.5 per cent of the sky, sampled off the middle line;"
                + " and, where a determined step in latitude keeps the pole on the observer's"
                + " meridian, every point of that meridian is on the central meridian's line or"
                + " on the outline.");
        p("");
        p("| pole | the zenith stands | paints | central longitude | bodies | zenith on the"
                + " central meridian | circles finite, no long chord | ecliptic and equator in"
                + " one run each | Subdue true | meridian on the central line and the outline |");
        p("|---|---|---|---|---|---|---|---|---|---|");
        List<Step> steps = List.of(
                new Step("at the pole", 0.0, 0.0, false),
                new Step("1e-12 in latitude", 1e-12, 0.0, false),
                new Step("-1e-12 in latitude", -1e-12, 0.0, false),
                new Step("1e-12 in longitude", 0.0, 1e-12, false),
                new Step("-1e-12 in longitude", 0.0, -1e-12, false),
                new Step("1e-9 in latitude", 1e-9, 0.0, true),
                new Step("-1e-9 in latitude", -1e-9, 0.0, true),
                new Step("1e-9 in longitude", 0.0, 1e-9, true),
                new Step("-1e-9 in longitude", 0.0, -1e-9, true),
                new Step("0.001 in latitude", 1e-3, 0.0, true),
                new Step("0.1 in latitude", 0.1, 0.0, true),
                new Step("-1 in latitude", -1.0, 0.0, true));
        for (SkyPosition pole : List.of(WholeSkyBoundary.NORTH_POLE, WholeSkyBoundary.SOUTH_POLE)) {
            String name = pole == WholeSkyBoundary.NORTH_POLE ? "north" : "south";
            Observer at = WholeSkyBoundary.observerUnder(pole, when);
            for (Step step : steps) {
                Observer observer = new Observer(at.latitudeDegrees() + step.north(),
                        at.eastLongitudeDegrees() + step.east(), when);
                String row;
                try {
                    WholeSkyComposition.Page page = WholeSkyComposition.paint(
                            new WholeSkyComposition.Ask(observer, ChartPalette.WHITE_PAPER, "en",
                                    READER_LIMIT, true, true, ORDINARY_FIELD, MONITOR_W,
                                    MONITOR_H), services);
                    WholeSkyBoundary.Held[] held = new WholeSkyBoundary.Held[1];
                    javax.swing.SwingUtilities.invokeAndWait(() -> held[0] =
                            WholeSkyBoundary.measure(page, MONITOR_W, MONITOR_H));
                    WholeSkyBoundary.Held h = held[0];
                    boolean circles = h.circles().values().stream().allMatch(
                            c -> c.finite() && c.longestChordPx() < 8.0);
                    row = f("yes | %s | %d | %s | %s | %s | %s | %s",
                            step.longitudeDetermined()
                                    ? f("%.1f", h.centralLongitudeDegrees())
                                    : "decided by rounding",
                            h.bodies(),
                            Math.abs(h.zenithOffCentralMeridianPx()) < 1e-6 ? "yes" : "no",
                            circles ? "yes" : "no",
                            h.circles().get("ecliptic").runs() == 1
                                    && h.circles().get("celestial equator").runs() == 1
                                    ? "yes" : "no",
                            h.subdueDisagreement() < 0.005 ? "yes" : "no",
                            !step.meridianThroughThePole() ? "-"
                                    : h.meridianPointsAstray() == 0 ? "yes" : "no");
                    if (pole == WholeSkyBoundary.NORTH_POLE && step.stands().equals(
                            "1e-9 in longitude")) {
                        ImageIO.write(page.image(), "png", OUT.resolve("pole-north.png").toFile());
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    row = "NO: " + cause.getMessage() + " | - | - | - | - | - | - | -";
                }
                p(f("| %s | %s | %s |", name, step.stands(), row));
            }
        }
        p("");
        p("- `pole-north.png`: the page with the zenith 1e-9 degrees of east longitude from the"
                + " north ecliptic pole. The horizon lies on the ecliptic, and the northern"
                + " ecliptic hemisphere is the sky above it.");
        p("");
        p("When the observer's meridian passes through the map's pole - the latitude rows - half"
                + " of it is the central meridian and the other half is the seam itself. It is"
                + " drawn on the outline, in as many runs as rounding puts its samples on one"
                + " edge or the other; the count is not pinned. Section 5's one run with its"
                + " ends on the outline describes a circle that crosses the seam, not one that"
                + " lies along it.");
        p("");
        double critical = SkyFrame.toOfDate(WholeSkyBoundary.NORTH_POLE,
                SkyFrame.julianDate(when)).decDegrees();
        p("**The same rule through one sidereal day, by latitude.** East longitude " + OSLO_LON
                + ", from " + when + ", the zenith's ecliptic longitude every second of chart"
                + " time. The critical latitude, where the zenith passes through the pole, is"
                + " the J2000 ecliptic pole's declination of date: " + f("%.4f", critical)
                + " at this instant. Equatorward of it the central meridian goes all the way"
                + " round, unevenly; poleward of it the central meridian swings and returns.");
        p("");
        p("| latitude | zenith nearest its ecliptic pole | the central meridian | fastest turn,"
                + " degrees per minute | shortest time for a quarter turn |");
        p("|---|---|---|---|---|");
        for (double latitude : new double[] {0.0, 30.0, OSLO_LAT, 65.0, 66.0, 66.5, 66.6, 67.0,
                68.0, 70.0, 75.0, 80.0, -66.5, -67.0}) {
            WholeSkyBoundary.Day day = WholeSkyBoundary.day(latitude, OSLO_LON, when);
            double fastest = day.fastestDegreesPerMinute();
            int quarter = day.quarterTurnSeconds();
            p(f("| %s | %.2f degrees | %s | %s | %s |",
                    latitude == Math.rint(latitude) ? f("%.0f", latitude) : String.valueOf(latitude),
                    day.nearestPoleDegrees(),
                    day.goesRound() ? "goes all the way round"
                            : f("swings %.0f degrees and back, about longitude %.0f",
                                    day.swingDegrees(), day.swingAboutDegrees()),
                    fastest < 10.0 ? f("%.2f", fastest)
                            : fastest < 100.0 ? f("%.1f", fastest) : f("%.0f", fastest),
                    quarter < 0 ? "never turns a quarter"
                            : quarter < 180 ? quarter + " s" : f("%.0f min", quarter / 60.0)));
        }
        p("");
        p("Every one of these pages is a whole-sky map with the zenith on its central meridian."
                + " What the latitude changes is how the map moves with chart time: near a polar"
                + " circle it turns a quarter in minutes or less once a day, and poleward of the"
                + " circle the seam stays in one half of the ecliptic.");
        p("");
    }

    // ------------------------------------------------------------------ geometry helpers

    private static int jumps(PathIterator path, double jump) {
        double[] c = new double[6];
        double lastX = Double.NaN;
        int jumps = 0;
        while (!path.isDone()) {
            int kind = path.currentSegment(c);
            if (kind == PathIterator.SEG_LINETO && !Double.isNaN(lastX)
                    && Math.abs(c[0] - lastX) > jump) {
                jumps++;
            }
            lastX = kind == PathIterator.SEG_MOVETO || kind == PathIterator.SEG_LINETO
                    ? c[0] : Double.NaN;
            path.next();
        }
        return jumps;
    }

    private static double nearestInk(PathIterator path, PixelPoint anchor) {
        double[] c = new double[6];
        double best = Double.MAX_VALUE;
        double fromX = 0;
        double fromY = 0;
        while (!path.isDone()) {
            int kind = path.currentSegment(c);
            if (kind == PathIterator.SEG_LINETO) {
                best = Math.min(best, java.awt.geom.Line2D.ptSegDist(fromX, fromY, c[0], c[1],
                        anchor.x(), anchor.y()));
            }
            fromX = c[0];
            fromY = c[1];
            path.next();
        }
        return best;
    }
}
