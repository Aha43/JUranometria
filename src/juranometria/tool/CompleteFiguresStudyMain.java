package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import juranometria.render.FigureEndpoints;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Complete constellation figures under an absolute stellar limit (#571,
 * road #698, ruled D-1 to D-7, docs/decisions/complete-figures.md): the rule
 * as built, measured on real 60°, 90°, 120° and globe pages.
 *
 * <p>Every page is painted by the production renderer. Counted on each: the
 * figure endpoints on the paper and which have a star drawn at them; the
 * segments with one and with two hidden endpoints; the drawn stars within
 * reach of a hidden endpoint, which a reader might take for its node; what a
 * click at a hidden endpoint selects; and the view report's own count. The
 * pre-#571 figures are stated as history, cited to the commit that measured
 * them.
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
                         int keptFaint, int mistakable, int clickSelectsStar, int reported) {
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        Files.createDirectories(DIR);
        System.setProperty("java.awt.headless", "true");
        p("# Complete figures under an absolute stellar limit: the measurements");
        p("");
        p("Sprint 58, issue #571, road #698, ruled D-1 to D-7"
                + " (`docs/decisions/complete-figures.md`): *complete constellation figures;"
                + " absolute stellar magnitude limit; no exceptional anchor stars.* Every page is"
                + " painted by the production renderer. Regenerate with `make"
                + " complete-figures-study`.");
        p("");
        p("A *hidden endpoint* is a figure endpoint on the paper with no star drawn at it."
                + " *Mistakable* counts hidden endpoints with an unrelated drawn star within "
                + fmt0(MISTAKEN_PX) + " px, which a reader might take for the node. *Click"
                + " selects a star* counts hidden endpoints where a click, through the"
                + " production hit test, selects a star, which only an ordinary drawn star within"
                + " reach can do. *Report* is the view report's"
                + " `figure-endpoints-below-limit` count, which leaves out endpoints under the"
                + " page's frame and those with no star in the pack.");
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
        p("## 1. The pages");
        p("");
        p("| page | limit | endpoints on paper | with a star | segments with one / two hidden"
                + " ends | stars drawn beyond the limit | mistakable | click selects a star |"
                + " report |");
        p("|---|---:|---:|---:|---:|---:|---:|---:|---:|");
        for (Page page : pages) {
            ChartScene scene = assemble(page.centre(), page.field(), page.limit());
            Count c = count(scene);
            write(paint(scene, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                    Set.of()), page.name() + ".png");
            p("| " + page.name() + " | V " + fmt1(page.limit()) + " | " + c.endpoints() + " | "
                    + c.withStar() + " | " + c.oneHidden() + " / " + c.twoHidden() + " | "
                    + c.keptFaint() + " | " + c.mistakable() + " | " + c.clickSelectsStar()
                    + " | " + c.reported() + " |");
        }
        p("");
        p("Before #571 the same pages kept every such star, however faint: 43 on Orion at 90°"
                + " (to V 4.88), 76 on Orion at 120° (to V 6.54), 58 on Ursa Major at 120° (to"
                + " V 6.58) and 22 on the globe (to V 6.54), each selectable by a click. Those"
                + " paintings stand side by side in this report at commit `f593dd5a`.");
        p("");
        for (Page page : pages) {
            p("### " + page.name());
            p("");
            p(page.caption());
            p("");
            p("![](" + page.name() + ".png)");
            p("");
        }
        progressive();
        emphasisAndHorizon();
        speechKeyAndPaper();
        figuresOnAndOff();
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
    }

    /** Section 2: pressing + reveals nodes through the ordinary stellar path. */
    private static void progressive() throws Exception {
        p("## 2. Raising the limit");
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
            ChartScene proposed = assemble(new SkyPosition(83.0, 0.0), 120.0, limit);
            Count c = count(proposed);
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
        ChartScene proposed = assemble(new SkyPosition(83.0, 0.0), 90.0,
                ChartViewState.defaultMagnitudeFor(90.0));
        p("## 3. Emphasis on the figures");
        p("");
        p("Orion at 90° with the constellation figures raised by Emphasis: the raised lines"
                + " reach the hidden endpoints exactly as the ordinary lines do.");
        p("");
        write(paint(proposed, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                Set.of(ChartStructure.CONSTELLATION_FIGURES)), "emphasis-90.png");
        p("![](emphasis-90.png)");
        p("");
        p("## 4. Below a horizon");
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
        ChartScene scene = assemble(new SkyPosition(83.0, 0.0), 120.0,
                ChartViewState.defaultMagnitudeFor(120.0));
        p("## 5. What the page says");
        p("");
        p("The page's spoken description names the limit and not the stars: \""
                + DrawnPage.of(scene).describe(EN) + "\" Hidden endpoints are not spoken (D-4);"
                + " the count stands only in the view report, when it is not zero.");
        p("");
        p("## 6. The magnitude key and paper");
        p("");
        double faintest = 0.0;
        for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT, EN)
                .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                faintest = Math.max(faintest, mark.star().magnitude());
            }
        }
        p("Orion at 120°, limit V " + fmt1(scene.limitingMagnitude()) + ": the faintest drawn"
                + " star is V " + fmt2(faintest) + " - within the limit the key states.");
        p("");
        write(paint(scene, withKey(), ChartRenderer.ReferenceLayer.NONE, Set.of()),
                "key-120.png");
        p("![](key-120.png)");
        p("");
        juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                Atlas.assembler()::assemble,
                new ChartViewState(new SkyPosition(83.0, 0.0), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                juranometria.sheet.PaperSize.A4, EN);
        Files.write(DIR.resolve("sheet-a4.png"), juranometria.sheet.SheetWriters.write(sheet,
                juranometria.sheet.SheetFormat.PNG, 96));
        p("The same page on A4: the lines complete, no star beyond the limit.");
        p("");
        p("![](sheet-a4.png)");
        p("");
    }

    /** Section 7: figures on or off changes only the lines. */
    private static void figuresOnAndOff() {
        ChartScene scene = assemble(new SkyPosition(83.0, 0.0), 120.0,
                ChartViewState.defaultMagnitudeFor(120.0));
        ChartOptions o = ChartOptions.DEFAULTS;
        ChartOptions off = new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(),
                false, o.constellationBoundaries(), o.constellationNames(), o.starNames(),
                o.bayerLetters(), o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(),
                o.magnitudeKey(), o.galaxies(), o.openClusters(), o.globularClusters(),
                o.nebulae(), o.planetaryNebulae(), o.palette());
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, EN);
        Set<String> on = new HashSet<>();
        Set<String> without = new HashSet<>();
        for (var mark : renderer.drawnMarks(scene, o)) {
            if (mark.star() != null) {
                on.add(mark.star().id());
            }
        }
        for (var mark : renderer.drawnMarks(scene, off)) {
            if (mark.star() != null) {
                without.add(mark.star().id());
            }
        }
        p("## 7. Figures on and off");
        p("");
        p("Orion at 120°: the stars drawn with the figures on and off are "
                + (on.equals(without) ? "the same " + on.size() + " stars"
                        : "**different**") + "; switching the figures changes only the lines.");
        p("");
    }

    private static ChartScene assemble(SkyPosition centre, double field, double limit) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, limit), WIDTH,
                HEIGHT);
    }

    /** Counts one painted page. */
    private static Count count(ChartScene scene) {
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
        // What a click at each hidden endpoint selects.
        Set<String> clicked = new HashSet<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var plane = projection.project(end);
                if (plane.isEmpty() || nodeAt(stars, end)) {
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
        int beyond = 0;
        for (ChartRenderer.DrawnMark m : stars) {
            if (m.star().magnitude() > scene.limitingMagnitude()
                    && !m.star().id().equals(scene.targetIdentity())) {
                beyond++;
            }
        }
        return new Count(endpoints, withStar, oneHidden, twoHidden, beyond, mistakable,
                clickStar, FigureEndpoints.belowLimit(scene, ChartOptions.DEFAULTS, marks));
    }

    /** Whether a drawn star is the endpoint's own, by the measured tolerance (#307). */
    private static boolean nodeAt(List<ChartRenderer.DrawnMark> stars, SkyPosition end) {
        for (ChartRenderer.DrawnMark m : stars) {
            SkyPosition at = m.star().position();
            double dRa = Math.abs(at.raDegrees() - end.raDegrees());
            dRa = Math.min(dRa, 360.0 - dRa) * Math.cos(Math.toRadians(end.decDegrees()));
            double dDec = at.decDegrees() - end.decDegrees();
            if (Math.hypot(dRa, dDec) <= FigureEndpoints.SAME_STAR_DEGREES) {
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
