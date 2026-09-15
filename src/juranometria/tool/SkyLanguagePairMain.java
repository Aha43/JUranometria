package juranometria.tool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;

/**
 * The owner's paired pages: Latin beside Norwegian (issue #347).
 *
 * <p>The gate asks a question no measurement answers - whether the
 * Norwegian sky reads naturally - so this renders pages a person can
 * look at. Seven, curated rather than exhaustive: the automated
 * study governs all fourteen, and a checkpoint that turned into an
 * image census would be read less carefully, not more.
 *
 * <p>Each sheet is <strong>two independently rendered pages</strong>
 * at the same centre, field, options, size and white-paper palette,
 * copied into the sheet at native resolution with nothing resized.
 * The halves are byte-identical to what separate images would hold,
 * and the generator asserts that rather than assuming it. The
 * language headings sit in a band <em>above</em> the pages, so the
 * comparison furniture cannot touch a pixel of what is being
 * compared.
 *
 * <p>An omission index accompanies them. Ten names are written in one
 * language and refused in the other, and on a full page an absence is
 * invisible by its nature - so each is shown as a native-scale crop
 * with the identity, the language that lost it, and the refusal that
 * decided it.
 */
public final class SkyLanguagePairMain {

    private SkyLanguagePairMain() {
    }

    private static final int WIDE_PX = 1100;

    private static final int HIGH_PX = 800;

    /** The band the headings live in, outside the page images. */
    private static final int HEADING_PX = 34;

    /** Neutral space between the two pages. */
    private static final int GUTTER_PX = 24;

    private static final File DIR =
            new File("docs/studies/sky-language");

    private static final Path NAMES =
            Path.of("docs/studies/sky-language/norwegian-names.tsv");

    /** The seven, chosen for what each one can show a person. */
    private static final Map<String, ChartViewState> PAGES =
            new LinkedHashMap<>();

    static {
        PAGES.put("sagittarius-18", new ChartViewState(
                new SkyPosition(271.0, -24.0), 18.0,
                ChartViewState.defaultMagnitudeFor(18.0)));
        PAGES.put("orion-18", new ChartViewState(
                new SkyPosition(83.8, 0.0), 18.0,
                ChartViewState.defaultMagnitudeFor(18.0)));
        PAGES.put("orion-120", new ChartViewState(
                new SkyPosition(83.8, 0.0), 120.0,
                ChartViewState.defaultMagnitudeFor(120.0)));
        PAGES.put("sagittarius-120", new ChartViewState(
                new SkyPosition(271.0, -24.0), 120.0,
                ChartViewState.defaultMagnitudeFor(120.0)));
        PAGES.put("ra-seam-42", new ChartViewState(
                new SkyPosition(0.0, 20.0), 42.0,
                ChartViewState.defaultMagnitudeFor(42.0)));
        PAGES.put("sagittarius-globe-180", new ChartViewState(
                new SkyPosition(271.0, -24.0), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0)));
        PAGES.put("north-pole-globe-180", new ChartViewState(
                new SkyPosition(0.0, 89.0), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0)));
        // Added after owner testing. The checkpoint asked whether
        // "Sørlige vannslange" - eighteen characters against Hydrus's
        // six - stays comfortable at a globe's rim, and none of the
        // first seven sheets could answer it: on the Sagittarius
        // globe Hyi is one of the Norwegian OMISSIONS, so the index
        // proved its absence while no page showed the name written.
        // The study says it is written and moved here; owner testing
        // has to see that rather than infer it from a report.
        PAGES.put("south-pole-globe-180", new ChartViewState(
                new SkyPosition(0.0, -89.0), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0)));
    }

    /**
     * Every page the study measures, for the omission index.
     *
     * <p>The sheets are curated; the index is not. Three of the ten
     * asymmetric omissions fall on pages outside the seven, and an
     * index that quietly showed seven would leave exactly the cases
     * a full page cannot show in the first place invisible - which
     * is the whole reason the index exists.
     */
    private static final Map<String, ChartViewState> ALL_PAGES =
            new LinkedHashMap<>();

    static {
        ALL_PAGES.putAll(PAGES);
        ALL_PAGES.put("sagittarius-42", new ChartViewState(
                new SkyPosition(271.0, -24.0), 42.0,
                ChartViewState.defaultMagnitudeFor(42.0)));
        ALL_PAGES.put("orion-42", new ChartViewState(
                new SkyPosition(83.8, 0.0), 42.0,
                ChartViewState.defaultMagnitudeFor(42.0)));
        ALL_PAGES.put("orion-globe-180", new ChartViewState(
                new SkyPosition(83.8, 0.0), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0)));
        ALL_PAGES.put("ra-seam-180", new ChartViewState(
                new SkyPosition(0.0, 20.0), 180.0,
                ChartViewState.defaultMagnitudeFor(180.0)));
        ALL_PAGES.put("north-pole-42", new ChartViewState(
                new SkyPosition(0.0, 89.0), 42.0,
                ChartViewState.defaultMagnitudeFor(42.0)));
        ALL_PAGES.put("south-pole-42", new ChartViewState(
                new SkyPosition(0.0, -89.0), 42.0,
                ChartViewState.defaultMagnitudeFor(42.0)));

    }

    /** One name written in one language and refused in the other. */
    private record Omission(String page, String id, String lost,
                            String kept, String refusal,
                            Rectangle2D where, boolean onTheLatinPage) {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> names = readNames();
        ChartOptions options = ChartOptions.DEFAULTS
                .withPalette(ChartPalette.WHITE_PAPER);
        List<Omission> omissions = new ArrayList<>();

        for (Map.Entry<String, ChartViewState> page : PAGES.entrySet()) {
            ChartScene latin = Atlas.assembler()
                    .assemble(page.getValue(), WIDE_PX, HIGH_PX);
            ChartScene norsk = named(latin, localised(latin, names));
            sheet(page.getKey(), draw(latin, options),
                    draw(norsk, options));
        }
        for (Map.Entry<String, ChartViewState> page
                : ALL_PAGES.entrySet()) {
            ChartScene latin = Atlas.assembler()
                    .assemble(page.getValue(), WIDE_PX, HIGH_PX);
            ChartScene norsk = named(latin, localised(latin, names));
            omissions.addAll(omissionsOn(page.getKey(), latin, norsk,
                    options));
        }
        index(omissions, options);

        System.out.printf(Locale.ROOT,
                "sky-language paired pages: %d sheets, %d omissions "
                        + "indexed from %d pages%n", PAGES.size(),
                omissions.size(), ALL_PAGES.size());
        if (omissions.size() != 10) {
            // The study measured ten. An index that showed fewer
            // would be a curated view of the thing curation cannot
            // show, and nobody would notice the difference.
            throw new IllegalStateException("the study measures ten"
                    + " asymmetric omissions and this index holds "
                    + omissions.size() + "; they must be the same"
                    + " ten, or one of the two is wrong");
        }
    }

    /** One page, rendered exactly as it would be on its own. */
    private static BufferedImage draw(ChartScene scene,
                                      ChartOptions options) {
        BufferedImage canvas = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * Two pages side by side, neither of them touched.
     *
     * <p>The halves are copied pixel for pixel and then checked back
     * against their sources. A sheet that quietly resized or
     * recoloured one side would be a comparison of the sheet rather
     * than of the atlas, and it would look fine.
     */
    private static void sheet(String slug, BufferedImage left,
                              BufferedImage right) throws IOException {
        int wide = WIDE_PX * 2 + GUTTER_PX;
        int high = HIGH_PX + HEADING_PX;
        BufferedImage sheet = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setColor(new Color(0xF2, 0xF2, 0xF2));
            g.fillRect(0, 0, wide, high);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0x33, 0x33, 0x33));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            g.drawString("Latin", 4, HEADING_PX - 12);
            g.drawString("Norsk bokmål", WIDE_PX + GUTTER_PX + 4,
                    HEADING_PX - 12);
            g.drawImage(left, 0, HEADING_PX, null);
            g.drawImage(right, WIDE_PX + GUTTER_PX, HEADING_PX, null);
        } finally {
            g.dispose();
        }
        identical(sheet, left, 0, HEADING_PX, slug + " left");
        identical(sheet, right, WIDE_PX + GUTTER_PX, HEADING_PX,
                slug + " right");
        ImageIO.write(sheet, "png", new File(DIR, "pair-" + slug + ".png"));
    }

    /** Refuses a sheet whose half is not the page it claims to show. */
    private static void identical(BufferedImage sheet,
                                  BufferedImage half, int atX, int atY,
                                  String which) {
        for (int y = 0; y < half.getHeight(); y++) {
            for (int x = 0; x < half.getWidth(); x++) {
                if (sheet.getRGB(atX + x, atY + y) != half.getRGB(x, y)) {
                    throw new IllegalStateException(which
                            + " differs from the page it shows at "
                            + x + "," + y + "; a sheet that alters what"
                            + " it compares is a comparison of itself");
                }
            }
        }
    }

    /** Where one language writes a name and the other does not. */
    private static List<Omission> omissionsOn(String page,
                                              ChartScene latin,
                                              ChartScene norsk,
                                              ChartOptions options) {
        Map<String, LabelPlacement.Placement> onLatin =
                constellationText(latin, options);
        Map<String, LabelPlacement.Placement> onNorsk =
                constellationText(norsk, options);
        List<Omission> found = new ArrayList<>();
        for (String id : onLatin.keySet()) {
            LabelPlacement.Placement was = onLatin.get(id);
            LabelPlacement.Placement now = onNorsk.get(id);
            boolean writtenLatin = was != null && !was.omitted();
            boolean writtenNorsk = now != null && !now.omitted();
            if (writtenLatin == writtenNorsk) {
                continue;
            }
            LabelPlacement.Placement kept = writtenLatin ? was : now;
            LabelPlacement.Placement refused = writtenLatin ? now : was;
            found.add(new Omission(page, id,
                    writtenLatin ? "Norsk bokmål" : "Latin",
                    kept.request().text(), refusal(refused),
                    kept.at(), writtenLatin));
        }
        return found;
    }

    private static String refusal(LabelPlacement.Placement refused) {
        if (refused == null) {
            return "not asked for on this page";
        }
        if (refused.refusals() == null || refused.refusals().isEmpty()) {
            return "refused with no reason recorded";
        }
        // The refusal's own words are long; the caption has to fit
        // on the sheet or the reason a reader is looking for runs
        // off the edge of it.
        String first = String.valueOf(refused.refusals().get(0));
        Matcher kind = Pattern.compile("kind=(\\w+)").matcher(first);
        Matcher by = Pattern.compile("by=([^,\\]]+)").matcher(first);
        return refused.refusals().size() + " candidates refused; first "
                + (kind.find() ? kind.group(1) : "refused")
                + (by.find() ? " (" + by.group(1).strip() + ")" : "");
    }

    /**
     * The omission index: what an absence looks like, up close.
     *
     * <p>An omission is invisible on a full page by definition - the
     * word simply is not there - so each is cropped at native scale
     * around where the other language wrote it, which is the only
     * way a person can judge whether the space left behind is
     * honest.
     */
    private static void index(List<Omission> omissions,
                              ChartOptions options) throws IOException {
        if (omissions.isEmpty()) {
            return;
        }
        int cropWide = 420;
        int cropHigh = 130;
        int rowHigh = cropHigh + 46;
        BufferedImage sheet = new BufferedImage(cropWide * 2 + GUTTER_PX,
                rowHigh * omissions.size(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        Map<String, String> names = readNames();
        try {
            g.setColor(new Color(0xF2, 0xF2, 0xF2));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int row = 0;
            for (Omission omission : omissions) {
                ChartScene latin = Atlas.assembler().assemble(
                        ALL_PAGES.get(omission.page()), WIDE_PX, HIGH_PX);
                ChartScene norsk = named(latin, localised(latin, names));
                BufferedImage both = crop(draw(latin, options),
                        omission.where(), cropWide, cropHigh);
                BufferedImage other = crop(draw(norsk, options),
                        omission.where(), cropWide, cropHigh);
                int atY = row * rowHigh + 40;
                g.drawImage(both, 0, atY, null);
                g.drawImage(other, cropWide + GUTTER_PX, atY, null);
                g.setColor(new Color(0x33, 0x33, 0x33));
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.drawString(String.format(Locale.ROOT,
                                "%s · %s · omitted in %s · %s",
                                omission.page(), omission.id(),
                                omission.lost(), omission.refusal()),
                        4, row * rowHigh + 26);
                row++;
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(sheet, "png", new File(DIR, "omissions.png"));
    }

    /** A native-scale window around a place on the page. */
    private static BufferedImage crop(BufferedImage page, Rectangle2D at,
                                      int wide, int high) {
        int x = (int) Math.max(0, Math.min(WIDE_PX - wide,
                at.getCenterX() - wide / 2.0));
        int y = (int) Math.max(0, Math.min(HIGH_PX - high,
                at.getCenterY() - high / 2.0));
        return page.getSubimage(x, y, wide, high);
    }

    private static Map<String, LabelPlacement.Placement>
            constellationText(ChartScene scene, ChartOptions options) {
        Map<String, LabelPlacement.Placement> placed =
                new LinkedHashMap<>();
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            for (LabelPlacement.Placement one
                    : new ChartRenderer(StarSizePolicy.DEFAULT)
                            .textPlacements(ChartRenderer.TextMetrics.of(g),
                                    scene, options)) {
                if (one.request().family()
                        == LabelPlacement.Family.CONSTELLATION) {
                    placed.put(one.request().id(), one);
                }
            }
        } finally {
            g.dispose();
        }
        return placed;
    }

    private static ChartScene named(ChartScene scene,
                                    Map<String, String> names) {
        SceneGeography geography = scene.geography();
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                new SceneGeography(geography.figureSegments(),
                        geography.boundarySegments(), names));
    }

    private static Map<String, String> localised(ChartScene scene,
                                                 Map<String, String> pack) {
        Map<String, String> names = new LinkedHashMap<>();
        scene.geography().latinNames().forEach((id, latin) ->
                names.put(id, pack.getOrDefault(id, latin)));
        return names;
    }

    private static Map<String, String> readNames() throws IOException {
        Map<String, String> pack = new LinkedHashMap<>();
        for (String line : Files.readAllLines(NAMES)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            if (columns.length == 2 && !columns[0].equals("IAU")) {
                pack.put(columns[0], columns[1]);
            }
        }
        return pack;
    }
}
