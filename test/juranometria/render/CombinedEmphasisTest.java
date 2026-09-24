package juranometria.render;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetWriters;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Any number of structures may be raised at once, and the released
 * one-target behaviour is unmoved (the multiple-emphasis follow-up
 * to #361).
 *
 * <p>Two halves. The first half holds the released one-target
 * route and the one-member set route equal on whatever runtime runs
 * it - every singleton page, and every singleton export in all three
 * formats - with each route deciding membership its own way. The
 * second half is the ruled
 * combination contract: away from crossings each emphasized
 * structure reproduces its singleton pixels exactly; a combination
 * introduces no changed region outside the singleton changes and
 * the structures' shared stroke and antialiasing envelope; and at
 * crossings the existing painter order stands, each structure
 * independently in its frozen style, with no blended accent and no
 * reordered layer. The exact union of the singleton masks is
 * deliberately <em>not</em> demanded at crossings - existing draw
 * order and antialiasing may legitimately hide or composite part of
 * the lower stroke there.
 */
class CombinedEmphasisTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);

    /**
     * How far compositing can reach beyond a stroke's own changed
     * pixels: the 0.6&nbsp;px emphasis gain plus antialiasing stays
     * within two pixels of the ink that caused it.
     */
    private static final int ENVELOPE = 2;

    private record Page(String name, SkyPosition centre, double field) { }

    private static final Page[] PAGES = {
        new Page("orion-42", new SkyPosition(83.0, 0.0), 42.0),
        new Page("sagittarius-120", new SkyPosition(271.0, -24.0), 120.0),
    };

    private static ChartScene scene(Page page) {
        return Atlas.assembler().assemble(
                new ChartViewState(page.centre(), page.field(),
                        ChartViewState.defaultMagnitudeFor(page.field())),
                900, 700);
    }

    // ---- the one-target route and the set route agree ---------------
    //
    // The migration itself was proved once, on the capture machine:
    // all 24 released singleton pages and all three export formats
    // byte-identical before and after the refactor (recorded in
    // docs/decisions/structure-emphasis.md). One machine's
    // rasterization cannot be another's contract, so what holds
    // permanently is equivalence on whatever runtime runs this: the
    // released one-target route - membership by identity with its one
    // target, metadata from that target's own token - against the
    // one-member set route, drawn side by side in the same JVM.

    /**
     * Every structure alone, on both pages and both grounds, with the
     * meridian and ecliptic modules contributing so the module
     * structures ink too: the two routes paint the same bytes.
     */
    @Test
    void everySingletonPaintsTheSameThroughBothRoutes() {
        java.util.EnumSet<ChartStructure> inked =
                java.util.EnumSet.noneOf(ChartStructure.class);
        for (Page page : PAGES) {
            ChartScene scene = scene(page);
            java.util.List<OverlayRegistry.Owned> modules = modules();
            for (ChartPalette ground : ChartPalette.values()) {
                ChartOptions options =
                        ChartOptions.DEFAULTS.withPalette(ground);
                BufferedImage canonical = viaOneTarget(scene, options,
                        modules, null);
                assertTrue(identical(canonical, viaSet(scene, options,
                                modules, Set.of())),
                        page.name() + " on " + ground.storedAs()
                                + ": no target and the empty set are"
                                + " the same canonical page");
                for (ChartStructure target : ChartStructure.values()) {
                    BufferedImage one = viaOneTarget(scene, options,
                            modules, target);
                    BufferedImage set = viaSet(scene, options, modules,
                            Set.of(target));
                    assertTrue(identical(one, set),
                            page.name() + " with only " + target.token()
                                    + " raised on " + ground.storedAs()
                                    + ": the one-target and set routes"
                                    + " must paint the same bytes");
                    if (!identical(one, canonical)) {
                        inked.add(target);
                    }
                }
            }
        }
        assertEquals(java.util.EnumSet.allOf(ChartStructure.class), inked,
                "every structure must actually change the page somewhere,"
                        + " or its agreement proves nothing");
    }

    /**
     * Every structure alone, exported in every format by both routes
     * from the same page: the same bytes, and the bare released
     * token in the metadata.
     */
    @Test
    void everySingletonExportsTheSameBytesThroughBothRoutes()
            throws Exception {
        ChartViewState state = new ChartViewState(
                PAGES[1].centre(), PAGES[1].field(),
                ChartViewState.defaultMagnitudeFor(PAGES[1].field()));
        java.util.List<OverlayRegistry.Owned> modules = modules();
        for (ChartStructure target : ChartStructure.values()) {
            SheetRecording one = ChartSheet.record(
                    Atlas.assembler()::assemble, state,
                    ChartOptions.DEFAULTS,
                    (g, scene, reserved) -> ReferenceInk.paint(g, scene,
                            modules, ChartPalette.WHITE_PAPER, ENGLISH,
                            reserved, target),
                    ChartRenderer.ReferenceLayer.NONE, PaperSize.A4,
                    ENGLISH, target);
            SheetRecording set = ChartSheet.record(
                    Atlas.assembler()::assemble, state,
                    ChartOptions.DEFAULTS,
                    (g, scene, reserved) -> ReferenceInk.paint(g, scene,
                            modules, ChartPalette.WHITE_PAPER, ENGLISH,
                            reserved, Set.of(target)),
                    ChartRenderer.ReferenceLayer.NONE, PaperSize.A4,
                    ENGLISH, Set.of(target));
            assertEquals(target.token(), one.metadata().emphasis(),
                    "the one-target route records the bare token");
            assertEquals(target.token(), set.metadata().emphasis(),
                    "and a one-member set records the same bare token");
            for (SheetFormat format : SheetFormat.values()) {
                org.junit.jupiter.api.Assertions.assertArrayEquals(
                        SheetWriters.write(one, format, 300),
                        SheetWriters.write(set, format, 300),
                        target.token() + " as " + format
                                + ": both routes write the same bytes");
            }
        }
    }

    @Test
    void severalTargetsRecordTheOrderedJoin() {
        SheetRecording several = ChartSheet.record(
                Atlas.assembler()::assemble,
                new ChartViewState(PAGES[1].centre(), PAGES[1].field(),
                        ChartViewState.defaultMagnitudeFor(
                                PAGES[1].field())),
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4, ENGLISH,
                Set.of(ChartStructure.HORIZON, ChartStructure.MERIDIAN,
                        ChartStructure.CONSTELLATION_FIGURES));
        assertEquals("meridian+horizon+constellation-figures",
                several.metadata().emphasis(),
                "several targets record the enum-ordered '+' join");
    }

    /** The two routes, each shaped its own way from end to end. */
    private static BufferedImage viaOneTarget(ChartScene scene,
            ChartOptions options,
            java.util.List<OverlayRegistry.Owned> modules,
            ChartStructure target) {
        BufferedImage image = blank(scene);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options,
                    (layerG, painted, reserved) -> ReferenceInk.paint(
                            layerG, painted, modules, options.palette(),
                            ENGLISH, reserved, target),
                    null, target);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage viaSet(ChartScene scene,
            ChartOptions options,
            java.util.List<OverlayRegistry.Owned> modules,
            Set<ChartStructure> targets) {
        BufferedImage image = blank(scene);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options,
                    (layerG, painted, reserved) -> ReferenceInk.paint(
                            layerG, painted, modules, options.palette(),
                            ENGLISH, reserved, targets),
                    null, targets);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * The meridian module with its horizon and cardinals, and the
     * ecliptic with its landmarks: an observer whose meridian stands
     * at the orion page's right ascension, so both pages carry every
     * module structure.
     */
    private static java.util.List<OverlayRegistry.Owned> modules() {
        OverlayRegistry registry = new OverlayRegistry();
        juranometria.meridian.MeridianModule meridian =
                new juranometria.meridian.MeridianModule(
                        new juranometria.sky.Observer(80.0, -58.7,
                                java.time.Instant.parse(
                                        "2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(juranometria.meridian.MeridianModule.ID,
                meridian::contributedGeometry);
        juranometria.ecliptic.EclipticModule ecliptic =
                new juranometria.ecliptic.EclipticModule();
        ecliptic.showing(true);
        registry.offer(juranometria.ecliptic.EclipticModule.ID,
                ecliptic::contributedGeometry);
        return registry.collect();
    }

    private static BufferedImage blank(ChartScene scene) {
        return new BufferedImage(scene.viewport().widthPx(),
                scene.viewport().heightPx(), BufferedImage.TYPE_INT_RGB);
    }

    private static boolean identical(BufferedImage one,
                                     BufferedImage other) {
        return java.util.Arrays.equals(
                one.getRGB(0, 0, one.getWidth(), one.getHeight(), null, 0,
                        one.getWidth()),
                other.getRGB(0, 0, other.getWidth(), other.getHeight(),
                        null, 0, other.getWidth()));
    }

    // ---- the token set is one deterministic representation ----------

    @Test
    void theJoinedTokensAreEnumOrderedAndTheSingletonIsBare() {
        assertEquals(Optional.empty(),
                ChartStructure.joinedTokens(Set.of()),
                "an empty set names nothing");
        assertEquals(Optional.empty(), ChartStructure.joinedTokens(null),
                "and so does no set at all");
        assertEquals(Optional.of("equatorial-grid"),
                ChartStructure.joinedTokens(
                        Set.of(ChartStructure.EQUATORIAL_GRID)),
                "one structure is the bare released token");
        assertEquals(Optional.of("meridian+horizon+constellation-figures"),
                ChartStructure.joinedTokens(Set.of(
                        ChartStructure.CONSTELLATION_FIGURES,
                        ChartStructure.MERIDIAN,
                        ChartStructure.HORIZON)),
                "several are joined in enum order, however the set"
                        + " iterates");
    }

    // ---- the ruled combination contract -----------------------------

    /**
     * Away from crossings, each emphasized structure reproduces its
     * singleton pixels exactly: at every pixel a structure changed
     * alone, outside the other structure's stroke-and-antialiasing
     * envelope, the combination equals that structure's singleton.
     */
    @Test
    void awayFromCrossingsEachStructureIsExactlyItsSingleton() {
        for (Pair pair : pairs()) {
            int checkedEarlier = 0;
            int checkedLater = 0;
            for (int y = 0; y < pair.height(); y++) {
                for (int x = 0; x < pair.width(); x++) {
                    if (pair.earlierMask()[y][x]
                            && !near(pair.laterMask(), x, y)) {
                        assertEquals(pair.earlier().getRGB(x, y),
                                pair.both().getRGB(x, y),
                                pair.name() + ": away from the other"
                                        + " structure, combined ink at ("
                                        + x + "," + y
                                        + ") is the singleton ink");
                        checkedEarlier++;
                    }
                    if (pair.laterMask()[y][x]
                            && !near(pair.earlierMask(), x, y)) {
                        assertEquals(pair.later().getRGB(x, y),
                                pair.both().getRGB(x, y),
                                pair.name() + ": away from the other"
                                        + " structure, combined ink at ("
                                        + x + "," + y
                                        + ") is the singleton ink");
                        checkedLater++;
                    }
                }
            }
            assertTrue(checkedEarlier > 0 && checkedLater > 0,
                    pair.name() + ": the contract must have pixels"
                            + " to hold");
        }
    }

    /**
     * A combination introduces no changed region outside the
     * singleton changes and the structures' shared stroke and
     * antialiasing envelope.
     */
    @Test
    void aCombinationChangesNothingOutsideTheSingletonEnvelopes() {
        for (Pair pair : pairs()) {
            for (int y = 0; y < pair.height(); y++) {
                for (int x = 0; x < pair.width(); x++) {
                    if (pair.both().getRGB(x, y)
                            != pair.canonical().getRGB(x, y)) {
                        assertTrue(near(pair.earlierMask(), x, y)
                                        || near(pair.laterMask(), x, y),
                                pair.name() + ": a combined change at ("
                                        + x + "," + y
                                        + ") lies outside every"
                                        + " singleton's envelope");
                    }
                }
            }
        }
    }

    /**
     * At crossings the existing painter order stands and each
     * structure keeps its own frozen accent: wherever the
     * later-painted structure's singleton laid down its pure accent
     * inside the earlier structure's envelope, the combination
     * carries that same pixel - the lower stroke never rises above
     * the upper one, and no blended accent replaces either. Every
     * pair must actually cross. A thin antialiased stroke may never
     * cover a whole pixel, so pure-accent presence is only demanded
     * of the later stroke at its crossings; the earlier structure's
     * fidelity is the first contract's business.
     */
    @Test
    void crossingsKeepThePainterOrderAndTheFrozenAccents() {
        for (Pair pair : pairs()) {
            int crossings = 0;
            boolean crossingRegion = false;
            for (int y = 0; y < pair.height(); y++) {
                for (int x = 0; x < pair.width(); x++) {
                    int combined = pair.both().getRGB(x, y);
                    boolean nearEarlier = near(pair.earlierMask(), x, y);
                    crossingRegion |= nearEarlier
                            && near(pair.laterMask(), x, y);
                    if (pair.later().getRGB(x, y) == pair.laterAccent()
                            && nearEarlier) {
                        assertEquals(pair.laterAccent(), combined,
                                pair.name() + ": at the crossing (" + x
                                        + "," + y + ") the later-painted"
                                        + " structure keeps its frozen"
                                        + " accent on top");
                        crossings++;
                    }
                }
            }
            assertTrue(crossingRegion,
                    pair.name() + ": the structures must actually"
                            + " cross on this page");
            assertTrue(crossings > 0,
                    pair.name() + ": the later stroke must show its"
                            + " pure accent at a crossing");
        }
    }

    // ---- the pixel arithmetic ---------------------------------------

    /**
     * One ruled combination: canonical, each singleton, and the
     * pair, rendered on the same page, with the painter order
     * stated - {@code later} is the structure the chart paints
     * after {@code earlier}.
     */
    private record Pair(String name, BufferedImage canonical,
                        BufferedImage earlier, BufferedImage later,
                        BufferedImage both, boolean[][] earlierMask,
                        boolean[][] laterMask, int earlierAccent,
                        int laterAccent) {
        int width() {
            return canonical.getWidth();
        }
        int height() {
            return canonical.getHeight();
        }
    }

    /**
     * The ruled crossing pages, on both grounds: the grid under the
     * figures (both chart layers, orion at 42 degrees), the grid
     * under the ecliptic (a module's line of reference paints above
     * the grid, on the sagittarius page the ecliptic actually
     * crosses), and a meridian under a horizon (two module circles
     * crossing on the orion page).
     */
    private static java.util.List<Pair> pairs() {
        java.util.List<Pair> pairs = new java.util.ArrayList<>();
        OverlayContribution ecliptic =
                new OverlayContribution.GreatCircle("ecliptic",
                        "Ecliptic", new SkyPosition(270.0, 66.56),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE);
        OverlayContribution meridian =
                new OverlayContribution.GreatCircle("meridian",
                        "Meridian", new SkyPosition(173.0, 0.0),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE);
        OverlayContribution horizon =
                new OverlayContribution.GreatCircle("horizon",
                        "Mathematical horizon",
                        new SkyPosition(83.0, 85.0),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE);
        for (ChartPalette ground : ChartPalette.values()) {
            pairs.add(pair("grid+figures on " + ground.storedAs(),
                    scene(PAGES[0]), ground, java.util.List.of(),
                    ChartStructure.EQUATORIAL_GRID,
                    ChartStructure.CONSTELLATION_FIGURES));
            pairs.add(pair("grid+ecliptic on " + ground.storedAs(),
                    scene(PAGES[1]), ground,
                    java.util.List.of(ecliptic),
                    ChartStructure.EQUATORIAL_GRID,
                    ChartStructure.ECLIPTIC));
            pairs.add(pair("meridian+horizon on " + ground.storedAs(),
                    scene(PAGES[0]), ground,
                    java.util.List.of(meridian, horizon),
                    ChartStructure.MERIDIAN, ChartStructure.HORIZON));
        }
        return pairs;
    }

    private static Pair pair(String name, ChartScene scene,
                             ChartPalette ground,
                             java.util.List<OverlayContribution>
                                     contributions,
                             ChartStructure earlier,
                             ChartStructure later) {
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(ground);
        BufferedImage canonical =
                painted(scene, options, contributions, Set.of());
        BufferedImage earlierAlone = painted(scene, options,
                contributions, Set.of(earlier));
        BufferedImage laterAlone = painted(scene, options,
                contributions, Set.of(later));
        BufferedImage both = painted(scene, options, contributions,
                Set.of(earlier, later));
        return new Pair(name, canonical, earlierAlone, laterAlone, both,
                changed(canonical, earlierAlone),
                changed(canonical, laterAlone),
                StructureStyle.accent(ground, earlier).getRGB(),
                StructureStyle.accent(ground, later).getRGB());
    }

    private static BufferedImage painted(ChartScene scene,
                                         ChartOptions options,
                                         java.util.List
                                                 <OverlayContribution>
                                                 contributions,
                                         Set<ChartStructure> emphasized) {
        if (contributions.isEmpty()) {
            return RENDERER.renderToImage(scene, options, emphasized);
        }
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("test-module", () -> contributions);
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options,
                    (layerG, painted, reserved) -> ReferenceInk.paint(
                            layerG, painted, registry.collect(),
                            options.palette(), ENGLISH, reserved,
                            emphasized),
                    null, emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static boolean[][] changed(BufferedImage canonical,
                                       BufferedImage emphasized) {
        boolean[][] mask =
                new boolean[canonical.getHeight()][canonical.getWidth()];
        for (int y = 0; y < canonical.getHeight(); y++) {
            for (int x = 0; x < canonical.getWidth(); x++) {
                mask[y][x] = canonical.getRGB(x, y)
                        != emphasized.getRGB(x, y);
            }
        }
        return mask;
    }

    /** Whether any changed pixel lies within the shared envelope. */
    private static boolean near(boolean[][] mask, int x, int y) {
        for (int dy = -ENVELOPE; dy <= ENVELOPE; dy++) {
            for (int dx = -ENVELOPE; dx <= ENVELOPE; dx++) {
                int ny = y + dy;
                int nx = x + dx;
                if (ny >= 0 && ny < mask.length && nx >= 0
                        && nx < mask[0].length && mask[ny][nx]) {
                    return true;
                }
            }
        }
        return false;
    }
}
