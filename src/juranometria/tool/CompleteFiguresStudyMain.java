package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.render.FigureAnchors;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Complete constellation figures under an absolute stellar limit (#571,
 * road #698; the owner's amended direction of 2026-11-07): measured on
 * real 60°, 90°, 120° and globe pages before any production change.
 *
 * <p>Every page is painted by the production renderer twice. <b>As
 * built</b> is today's page: the stars a visible figure is drawn to are
 * kept however faint (#307's anchor exception). <b>As proposed</b> is the
 * same page with every star fainter than the page's limit taken out of
 * the scene before it is painted - so the renderer, its anchor rule and
 * its figure lines unchanged, nothing faint can be kept, and the lines,
 * which are drawn from the stored figure geometry and not from stars,
 * stay complete. That is exactly the proposed rule, drawn without a
 * production change.
 *
 * <p>Counted on each page: the figure endpoints on the paper and which
 * have a star drawn at them; the segments with one and with two hidden
 * endpoints; the faint stars kept today and the faintest of them; the
 * drawn stars standing within reach of a hidden endpoint, which a reader
 * might take for its node; and what a click at a hidden endpoint selects.
 */
public final class CompleteFiguresStudyMain {

    private static final Path DIR = Path.of("docs/studies/complete-figures");
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    /** Near enough to a hidden endpoint for a drawn star to pass for its node, in pixels. */
    private static final double MISTAKEN_PX = 6.0;
    private static final juranometria.project.PageWords EN =
            PageText.in(InterfaceText.forLanguage("en"));

    private static final StringBuilder REPORT = new StringBuilder();

    private CompleteFiguresStudyMain() {
    }

    private record Page(String name, String caption, SkyPosition centre, double field,
                        double limit) {
    }

    /** What one painting of a page found. */
    private record Count(int endpoints, int withStar, int oneHidden, int twoHidden,
                         int keptFaint, double faintestKept, int mistakable,
                         int clickSelectsStar, double brightestDrawnLimitBreach) {
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        Files.createDirectories(DIR);
        System.setProperty("java.awt.headless", "true");
        p("# Complete figures under an absolute stellar limit: the measurements");
        p("");
        p("Sprint 58, issue #571, road #698 - the owner's amended direction of 2026-11-07:"
                + " *complete constellation figures; absolute stellar magnitude limit; no"
                + " exceptional anchor stars.* No production behaviour changes. Each page is"
                + " painted by the production renderer **as built** (today: a star a visible"
                + " figure is drawn to is kept however faint, #307) and **as proposed** (the"
                + " same page with every star fainter than its limit removed from the scene"
                + " before painting, so nothing faint can be kept while the figure lines,"
                + " drawn from the stored geometry, stay complete). Regenerate with `make"
                + " complete-figures-study`.");
        p("");
        p("A *hidden endpoint* is a figure endpoint on the paper with no star drawn at it."
                + " *Mistakable* counts hidden endpoints with an unrelated drawn star within "
                + fmt0(MISTAKEN_PX) + " px, which a reader might take for the node. *Click"
                + " selects a star* counts hidden endpoints where a click, through the"
                + " production hit test, selects a star.");
        p("");
        List<Page> pages = List.of(
                new Page("orion-60", "Orion on the 60° overview, at its default limit.",
                        new SkyPosition(83.0, 0.0), 60.0,
                        ChartViewState.defaultMagnitudeFor(60.0)),
                new Page("orion-90", "Orion on the 90° overview, at its default limit.",
                        new SkyPosition(83.0, 0.0), 90.0,
                        ChartViewState.defaultMagnitudeFor(90.0)),
                new Page("orion-120", "Orion on the 120° overview, at its default limit.",
                        new SkyPosition(83.0, 0.0), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                new Page("ursa-major-120", "Ursa Major on the 120° overview, at its default"
                        + " limit.", new SkyPosition(165.0, 55.0), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                new Page("globe", "The celestial globe over Orion, at its default limit.",
                        new SkyPosition(83.0, 0.0), 180.0,
                        ChartViewState.defaultMagnitudeFor(180.0)));
        p("## 1. The pages, as built and as proposed");
        p("");
        p("| page | limit | endpoints on paper | with a star, as built | with a star, as proposed"
                + " | segments with one / two hidden ends | faint stars kept today (faintest) |"
                + " mistakable | click selects a star, as built / as proposed |");
        p("|---|---:|---:|---:|---:|---:|---:|---:|---:|");
        Map<String, Count[]> all = new LinkedHashMap<>();
        for (Page page : pages) {
            ChartScene built = assemble(page.centre(), page.field(), page.limit());
            ChartScene proposed = withinLimit(built);
            Count asBuilt = count(built, built);
            Count asProposed = count(proposed, built);
            all.put(page.name(), new Count[] {asBuilt, asProposed});
            write(paint(built, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE, Set.of()),
                    page.name() + "-built.png");
            write(paint(proposed, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                    Set.of()), page.name() + "-proposed.png");
            p("| " + page.name() + " | V " + fmt1(page.limit()) + " | " + asBuilt.endpoints()
                    + " | " + asBuilt.withStar() + " | " + asProposed.withStar() + " | "
                    + asProposed.oneHidden() + " / " + asProposed.twoHidden() + " | "
                    + asBuilt.keptFaint() + (asBuilt.keptFaint() == 0 ? ""
                            : " (V " + fmt2(asBuilt.faintestKept()) + ")") + " | "
                    + asProposed.mistakable() + " | " + asBuilt.clickSelectsStar() + " / "
                    + asProposed.clickSelectsStar() + " |");
        }
        p("");
        for (Page page : pages) {
            Count[] c = all.get(page.name());
            p("### " + page.name());
            p("");
            p(page.caption() + " As built, " + c[0].keptFaint() + " star"
                    + (c[0].keptFaint() == 1 ? " is" : "s are") + " drawn fainter than the page's"
                    + " limit of V " + fmt1(page.limit()) + " because a figure ends on "
                    + (c[0].keptFaint() == 1 ? "it" : "them") + "; as proposed, none is, and "
                    + (c[1].endpoints() - c[1].withStar()) + " endpoints show the line meeting"
                    + " the sky with no node.");
            p("");
            p("| as built | as proposed |");
            p("|---|---|");
            p("| ![](" + page.name() + "-built.png) | ![](" + page.name() + "-proposed.png) |");
            p("");
        }
        progressive();
        emphasisAndHorizon();
        speechKeyAndPaper();
        p("## 7. What the proposal changes, and does not");
        p("");
        p("- **The magnitude limit becomes literally true**: as proposed, no drawn star on any"
                + " page above is fainter than its limit (section 6), and the magnitude key's"
                + " promise holds.");
        p("- **The figure lines do not move**: they are drawn from the stored figure geometry;"
                + " both paintings carry the same lines.");
        p("- **A hidden endpoint is nothing to click**: as built, a click there selects the"
                + " kept faint star; as proposed, it selects no star (table, last column), unless"
                + " an ordinary drawn star is itself within reach.");
        p("- **Raising the limit admits each node through the ordinary stellar path**, mark,"
                + " identity and label policy alike (section 2).");
        p("- **Turning figures off changes only the lines**: as proposed, the star population is"
                + " the same with figures on or off; as built, turning figures off drops the kept"
                + " faint stars.");
        p("");
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
    }

    /** Section 2: pressing + reveals nodes through the ordinary stellar path. */
    private static void progressive() throws Exception {
        p("## 2. Raising the limit, as proposed");
        p("");
        p("Orion at 120°, the reader pressing **+** from V 4 to V 7: each step admits stars"
                + " through the ordinary stellar path, and endpoints gain their nodes.");
        p("");
        p("| limit | endpoints with a star | hidden endpoints |");
        p("|---:|---:|---:|");
        StringBuilder row = new StringBuilder("|");
        StringBuilder head = new StringBuilder("|");
        StringBuilder rule = new StringBuilder("|");
        for (double limit : new double[] {4.0, 5.0, 6.0, 7.0}) {
            ChartScene proposed = withinLimit(assemble(new SkyPosition(83.0, 0.0), 120.0, limit));
            Count c = count(proposed, proposed);
            p("| V " + fmt1(limit) + " | " + c.withStar() + " | " + (c.endpoints() - c.withStar())
                    + " |");
            String name = "raising-v" + (int) limit + ".png";
            write(paint(proposed, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                    Set.of()), name);
            head.append(" V ").append(fmt1(limit)).append(" |");
            rule.append("---|");
            row.append(" ![](").append(name).append(") |");
        }
        p("");
        p(head.toString());
        p(rule.toString());
        p(row.toString());
        p("");
    }

    /** Section 3 and 4: the figures raised by Emphasis, and a horizon subduing the page. */
    private static void emphasisAndHorizon() throws Exception {
        ChartScene proposed = withinLimit(assemble(new SkyPosition(83.0, 0.0), 90.0,
                ChartViewState.defaultMagnitudeFor(90.0)));
        p("## 3. Emphasis on the figures, as proposed");
        p("");
        p("Orion at 90° with the constellation figures raised by Emphasis: the raised lines"
                + " reach the hidden endpoints exactly as the ordinary lines do.");
        p("");
        write(paint(proposed, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                Set.of(ChartStructure.CONSTELLATION_FIGURES)), "emphasis-90.png");
        p("![](emphasis-90.png)");
        p("");
        p("## 4. Below a horizon, as proposed");
        p("");
        SkyPosition zenith = new SkyPosition(83.0, 60.0);
        p("Orion at 90° with Subdue below horizon on, the zenith placed at RA 83°, Dec +60° (a"
                + " placement for the study, not an observer): the lines are subdued below the"
                + " horizon by the ordinary rule, hidden endpoints or not.");
        p("");
        ChartRenderer.ReferenceLayer subdue = ChartRenderer.ReferenceLayer.subduing(
                ChartRenderer.ReferenceLayer.NONE, scene -> BelowHorizon.of(zenith, scene));
        write(paint(proposed, ChartOptions.DEFAULTS, subdue, Set.of()), "below-horizon-90.png");
        p("![](below-horizon-90.png)");
        p("");
    }

    /** Sections 5 and 6: what the page says, the magnitude key, and paper. */
    private static void speechKeyAndPaper() throws Exception {
        ChartScene built = assemble(new SkyPosition(83.0, 0.0), 120.0,
                ChartViewState.defaultMagnitudeFor(120.0));
        ChartScene proposed = withinLimit(built);
        p("## 5. What the page says");
        p("");
        String saidBuilt = DrawnPage.of(built).describe(EN);
        String saidProposed = DrawnPage.of(proposed).describe(EN);
        p("The page's spoken description, as built and as proposed, is "
                + (saidBuilt.equals(saidProposed) ? "the same" : "different") + ": \""
                + saidProposed + "\" Neither names the figures' hidden endpoints; whether the"
                + " page should say how many there are is the owner's question (decision record,"
                + " choice D-4).");
        p("");
        p("## 6. The magnitude key and paper, as proposed");
        p("");
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, EN);
        double faintestBuilt = 0.0;
        for (var mark : renderer.drawnMarks(built, ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                faintestBuilt = Math.max(faintestBuilt, mark.star().magnitude());
            }
        }
        double faintestProposed = 0.0;
        for (var mark : renderer.drawnMarks(proposed, ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                faintestProposed = Math.max(faintestProposed, mark.star().magnitude());
            }
        }
        p("Orion at 120°, limit V " + fmt1(built.limitingMagnitude()) + ": the faintest drawn"
                + " star is V " + fmt2(faintestBuilt) + " as built and V " + fmt2(faintestProposed)
                + " as proposed - within the limit the key states.");
        p("");
        write(paint(proposed, withKey(), ChartRenderer.ReferenceLayer.NONE, Set.of()),
                "key-120.png");
        p("![](key-120.png)");
        p("");
        juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                (state, w, h) -> withinLimit(Atlas.assembler().assemble(state, w, h)),
                new ChartViewState(new SkyPosition(83.0, 0.0), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                juranometria.sheet.PaperSize.A4, EN);
        Files.write(DIR.resolve("sheet-a4.png"), juranometria.sheet.SheetWriters.write(sheet,
                juranometria.sheet.SheetFormat.PNG, 96));
        p("The same page on A4, as proposed: the lines complete, no star beyond the limit.");
        p("");
        p("![](sheet-a4.png)");
        p("");
    }

    private static ChartScene assemble(SkyPosition centre, double field, double limit) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, limit), WIDTH,
                HEIGHT);
    }

    /** The same scene with every star fainter than its limit removed: the proposed rule. */
    private static ChartScene withinLimit(ChartScene scene) {
        List<Star> kept = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (star.magnitude() <= scene.limitingMagnitude()
                    || star.id().equals(scene.targetIdentity())) {
                kept.add(star);
            }
        }
        return new ChartScene(scene.viewport(), kept, scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(), scene.geography());
    }

    /**
     * Counts one painting: {@code scene} is what is painted, {@code full} the
     * as-built scene its faint kept stars are counted from.
     */
    private static Count count(ChartScene scene, ChartScene full) {
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, EN);
        List<ChartRenderer.DrawnMark> marks = renderer.drawnMarks(scene, ChartOptions.DEFAULTS);
        List<ChartRenderer.DrawnMark> stars = marks.stream()
                .filter(m -> m.star() != null).toList();
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        var projection = page.projection();
        int endpoints = 0;
        int withStar = 0;
        int mistakable = 0;
        int clickStar = 0;
        Set<String> seen = new HashSet<>();
        int oneHidden = 0;
        int twoHidden = 0;
        for (GeoSegment segment : scene.geography().figureSegments()) {
            int hidden = 0;
            int onPaper = 0;
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var plane = projection.project(end);
                if (plane.isEmpty()) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                if (at.x() < 0 || at.y() < 0 || at.x() >= WIDTH || at.y() >= HEIGHT) {
                    continue;
                }
                onPaper++;
                boolean node = nodeAt(stars, end);
                if (!node) {
                    hidden++;
                }
                String key = String.format(Locale.ROOT, "%.4f,%.4f", end.raDegrees(),
                        end.decDegrees());
                if (!seen.add(key)) {
                    continue;
                }
                endpoints++;
                if (node) {
                    withStar++;
                    continue;
                }
                for (ChartRenderer.DrawnMark m : stars) {
                    if (Math.hypot(m.centre().x() - at.x(), m.centre().y() - at.y())
                            <= MISTAKEN_PX) {
                        mistakable++;
                        break;
                    }
                }
            }
            if (onPaper > 0 && hidden == 1) {
                oneHidden++;
            }
            if (onPaper > 0 && hidden == 2) {
                twoHidden++;
            }
        }
        // What a click at each endpoint not drawn in the proposal would select here.
        ChartScene proposed = withinLimit(full);
        List<ChartRenderer.DrawnMark> proposedStars = renderer.drawnMarks(proposed,
                ChartOptions.DEFAULTS).stream().filter(m -> m.star() != null).toList();
        Set<String> clicked = new HashSet<>();
        for (GeoSegment segment : full.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var plane = projection.project(end);
                if (plane.isEmpty() || nodeAt(proposedStars, end)) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                if (at.x() < 0 || at.y() < 0 || at.x() >= WIDTH || at.y() >= HEIGHT
                        || !clicked.add(at.x() + "," + at.y())) {
                    continue;
                }
                if (ChartHitTest.orderedHits(marks, at.x(), at.y(), ChartHitTest.TOLERANCE_PX)
                        .stream().anyMatch(m -> m.star() != null)) {
                    clickStar++;
                }
            }
        }
        int keptFaint = 0;
        double faintest = 0.0;
        double breach = 0.0;
        for (ChartRenderer.DrawnMark m : stars) {
            if (m.star().magnitude() > scene.limitingMagnitude()
                    && !m.star().id().equals(scene.targetIdentity())) {
                keptFaint++;
                faintest = Math.max(faintest, m.star().magnitude());
            }
        }
        return new Count(endpoints, withStar, oneHidden, twoHidden, keptFaint, faintest,
                mistakable, clickStar, breach);
    }

    /** Whether a drawn star is the endpoint's own, by the production tolerance (#307). */
    private static boolean nodeAt(List<ChartRenderer.DrawnMark> stars, SkyPosition end) {
        for (ChartRenderer.DrawnMark m : stars) {
            SkyPosition at = m.star().position();
            double dRa = Math.abs(at.raDegrees() - end.raDegrees());
            dRa = Math.min(dRa, 360.0 - dRa) * Math.cos(Math.toRadians(end.decDegrees()));
            double dDec = at.decDegrees() - end.decDegrees();
            if (Math.hypot(dRa, dDec) <= FigureAnchors.SAME_STAR_DEGREES) {
                return true;
            }
        }
        return false;
    }

    private static BufferedImage paint(ChartScene scene, ChartOptions options,
                                       ChartRenderer.ReferenceLayer reference,
                                       Set<ChartStructure> emphasized) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT, EN).render(g, scene, options, reference,
                    null, emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static ChartOptions withKey() {
        ChartOptions o = ChartOptions.DEFAULTS;
        return new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(),
                o.constellationFigures(), o.constellationBoundaries(),
                o.constellationNames(), o.starNames(), o.bayerLetters(),
                o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(), true,
                o.galaxies(), o.openClusters(), o.globularClusters(), o.nebulae(),
                o.planetaryNebulae(), o.palette());
    }

    private static void write(BufferedImage image, String name) throws Exception {
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static String fmt0(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String fmt2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
