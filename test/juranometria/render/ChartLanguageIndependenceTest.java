package juranometria.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageInventory;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetRecorder;
import juranometria.sheet.SheetRecording;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Changing the chart's language changes words, not the sky
 * (Sprint 33, issue #347).
 *
 * <p>A reader who switches the chart to Norwegian is asking for
 * different names. They are not asking for a different page: the same
 * stars, the same objects, the same marks under the same pointer, the
 * same inventory. Only the words, where they sit, and which of them
 * fit may differ.
 *
 * <p>Both languages here read <strong>one canonical scene</strong>.
 * Only the name map is resolved twice, and each side then makes its
 * own placement decision through the production renderer. The
 * Norwegian page is never derived by transforming the Latin page's
 * output - a comparison built that way agrees with itself by
 * construction and proves nothing.
 *
 * <p>The premise is asserted before the conclusion. A bilingual
 * fixture where every name happened to be identical would pass every
 * assertion below while comparing Latin with Latin, so each page must
 * first be shown to actually differ in at least one name.
 *
 * <p><strong>Provisional.</strong> The Norwegian names are the
 * owner's 88-row lead, not verified provenance (#347). They are
 * adequate for this contract, which is about identity and geometry
 * rather than spelling, but no measurement taken against them is the
 * gate's answer until the verified catalogue replaces them.
 */
class ChartLanguageIndependenceTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final int WIDE_PX = 1000;

    private static final int HIGH_PX = 700;

    /**
     * Names from the owner's lead, for the constellations these
     * pages carry. Provisional: {@code Sydkorset} versus
     * {@code Sørkorset} is an open question on #347, and no name
     * here is settled until a citable source says so.
     */
    private static final Map<String, String> LEAD = Map.ofEntries(
            Map.entry("Sgr", "Skytten"), Map.entry("Sco", "Skorpionen"),
            Map.entry("Oph", "Slangebæreren"), Map.entry("Ser", "Slangen"),
            Map.entry("Aql", "Ørnen"), Map.entry("Cap", "Steinbukken"),
            Map.entry("Sct", "Skjoldet"), Map.entry("CrA", "Sørlige krone"),
            Map.entry("Lib", "Vekten"), Map.entry("Tel", "Teleskopet"),
            Map.entry("Ori", "Orion"), Map.entry("Tau", "Tyren"),
            Map.entry("Gem", "Tvillingene"), Map.entry("Mon", "Enhjørningen"),
            Map.entry("CMa", "Store hund"), Map.entry("CMi", "Lille hund"),
            Map.entry("Lep", "Haren"), Map.entry("Eri", "Floden"),
            Map.entry("Cet", "Hvalen"), Map.entry("Ari", "Væren"),
            Map.entry("Per", "Perseus"), Map.entry("Aur", "Kusken"),
            Map.entry("And", "Andromeda"), Map.entry("Peg", "Pegasus"),
            Map.entry("UMa", "Store bjørn"), Map.entry("UMi", "Lille bjørn"),
            Map.entry("Dra", "Dragen"), Map.entry("Cas", "Kassiopeia"),
            Map.entry("Cep", "Kefeus"), Map.entry("Lyr", "Lyren"),
            Map.entry("Cyg", "Svanen"), Map.entry("Her", "Herkules"),
            Map.entry("Boo", "Oksedriveren"), Map.entry("Vir", "Jomfruen"),
            Map.entry("Leo", "Løven"), Map.entry("Cnc", "Krepsen"),
            Map.entry("Hya", "Vannslangen"), Map.entry("Crv", "Ravnen"),
            Map.entry("Cen", "Kentauren"), Map.entry("Cru", "Sydkorset"),
            Map.entry("Car", "Kjølen"), Map.entry("Vel", "Seilet"),
            Map.entry("Pup", "Akterstavnen"), Map.entry("Psc", "Fiskene"),
            Map.entry("Aqr", "Vannmannen"), Map.entry("Sge", "Pilen"),
            Map.entry("Vul", "Reven"), Map.entry("Del", "Delfinen"),
            Map.entry("Lac", "Øglen"), Map.entry("Tri", "Triangelet"));

    /** Pages the study measures, at the fields it measures them. */
    private static final Map<String, ChartViewState> PAGES =
            new LinkedHashMap<>(Map.of(
                    "crowded Sagittarius", new ChartViewState(
                            new SkyPosition(271.0, -24.0), 18.0, 8.0),
                    "sparse Orion", new ChartViewState(
                            new SkyPosition(83.8, 0.0), 18.0, 8.0),
                    "the north pole", new ChartViewState(
                            new SkyPosition(0.0, 89.0), 42.0, 8.0),
                    "the RA seam", new ChartViewState(
                            new SkyPosition(0.0, 20.0), 42.0, 8.0),
                    // The globe, without which a whole family of
                    // non-text ink is never painted at all: the limb
                    // is drawn only where the sky stops short of the
                    // paper. A mutation tying the limb's stroke to
                    // the length of the names on the page survived
                    // every flat fixture, because none of them draws
                    // a limb.
                    "the globe rim", new ChartViewState(
                            new SkyPosition(271.0, -24.0), 180.0, 8.0)));

    @Test
    void theSkyDoesNotMoveWhenOnlyItsNamesChange() {
        int pagesCompared = 0;
        for (Map.Entry<String, ChartViewState> page : PAGES.entrySet()) {
            String where = page.getKey();
            ChartScene latin = Atlas.assembler()
                    .assemble(page.getValue(), WIDE_PX, HIGH_PX);
            ChartScene norwegian = named(latin, localised(latin));

            // The premise. Without it every assertion below could be
            // satisfied by comparing the Latin page with itself.
            assertTrue(differingNames(latin, norwegian) >= 1,
                    where + " must actually be bilingual for this"
                            + " comparison to mean anything: "
                            + latin.geography().latinNames());

            assertEquals(markIdentities(latin), markIdentities(norwegian),
                    where + " draws the same objects, in the same"
                            + " order, whatever they are called");
            List<String> latinHits = marksUnderThePointer(latin);
            // A grid that answers "nothing" everywhere agrees with
            // itself perfectly. The premise is that these probes
            // actually land on marks, so equality is about the
            // answers and not about their absence.
            long answered = latinHits.stream()
                    .filter(hit -> !hit.endsWith("=-")).count();
            assertTrue(answered >= 5,
                    where + " must be probed where there is something"
                            + " to find: " + answered + " of "
                            + latinHits.size() + " probes hit a mark");
            assertEquals(latinHits, marksUnderThePointer(norwegian),
                    where + " answers a click with the same object"
                            + " whatever the chart language");
            assertEquals(inventoryIdentities(latin),
                    inventoryIdentities(norwegian),
                    where + " lists the same contents, by identity");

            // Marks and hit testing say nothing about the grid, the
            // figures, the boundaries, the modules, the limb or the
            // furniture. Those are compared as the painting itself:
            // the recorded operation stream with the text removed,
            // in order, because language must not reorder non-text
            // painting either.
            List<String> latinInk = nonTextInk(page.getValue(), false);
            List<String> norwegianInk = nonTextInk(page.getValue(), true);
            assertInkIsWorthComparing(where, latinInk);
            assertEquals(latinInk, norwegianInk,
                    where + " paints the same non-text ink, in the"
                            + " same order, whatever the names are");
            pagesCompared++;
        }
        assertEquals(PAGES.size(), pagesCompared,
                "every page was compared, not merely the first");
    }

    @Test
    void theNameMapKeepsItsKeysAndItsOrderWhateverTheLanguage() {
        // The named contract: iteration order reaches the renderer,
        // so overlapping name text stacks identically on every render
        // (PR #69). Because the key is the constellation's identity
        // and not its name, that order cannot follow the language -
        // and #348 must not make it.
        ChartScene latin = Atlas.assembler().assemble(
                PAGES.get("crowded Sagittarius"), WIDE_PX, HIGH_PX);
        ChartScene norwegian = named(latin, localised(latin));

        assertEquals(List.copyOf(latin.geography().latinNames().keySet()),
                List.copyOf(norwegian.geography().latinNames().keySet()),
                "chart language changes values, never keys or"
                        + " iteration order");
        assertTrue(!List.copyOf(latin.geography().latinNames().values())
                        .equals(List.copyOf(
                                norwegian.geography().latinNames().values())),
                "while the values it carries do change, or this test"
                        + " is comparing one language with itself");
    }

    @Test
    void onlyTheWordsAndWhereTheySitMayDiffer() {
        // What a reader is allowed to notice: different words, in
        // different places, and some that no longer fit. Measured
        // rather than demanded equal - the point of the study is the
        // size of this difference, not its absence.
        ChartScene latin = Atlas.assembler().assemble(
                PAGES.get("crowded Sagittarius"), WIDE_PX, HIGH_PX);
        ChartScene norwegian = named(latin, localised(latin));

        List<String> latinText = constellationText(latin);
        List<String> norwegianText = constellationText(norwegian);
        assertTrue(!latinText.equals(norwegianText),
                "the names on the page are the reader's language: "
                        + latinText + " against " + norwegianText);
        assertTrue(!norwegianText.isEmpty(),
                "and Norwegian names are actually written, rather"
                        + " than all omitted - which would make the"
                        + " page identical for the wrong reason");
    }

    /** The same scene, with its constellation names resolved anew. */
    private static ChartScene named(ChartScene scene,
                                    Map<String, String> names) {
        SceneGeography geography = scene.geography();
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                new SceneGeography(geography.figureSegments(),
                        geography.boundarySegments(), names));
    }

    /**
     * The lead's name for each constellation this page carries.
     *
     * <p>Falls back to the Latin name where the lead has no entry,
     * which is the deterministic fallback #348 must implement: a
     * page never shows a blank where a name belongs.
     */
    private static Map<String, String> localised(ChartScene scene) {
        Map<String, String> names = new LinkedHashMap<>();
        scene.geography().latinNames().forEach((id, latin) ->
                names.put(id, LEAD.getOrDefault(id, latin)));
        return names;
    }

    private static long differingNames(ChartScene latin,
                                       ChartScene norwegian) {
        return latin.geography().latinNames().entrySet().stream()
                .filter(e -> !e.getValue().equals(
                        norwegian.geography().latinNames().get(e.getKey())))
                .count();
    }

    /** Every drawn mark, by kind and catalogue identity, in order. */
    private static List<String> markIdentities(ChartScene scene) {
        List<String> marks = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                        .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            marks.add(mark.kind() + ":" + mark.subject());
        }
        return marks;
    }

    /**
     * The premise for the ink comparison.
     *
     * <p>Two empty streams are equal, and a recording that captured
     * nothing would satisfy the rule perfectly while proving that
     * the grid, the figures and the furniture were never looked at.
     * {@code Drawn} carries no family label, so coverage is asserted
     * through properties only a real page has: many operations, both
     * filled and stroked ink, and several distinct colours - a page
     * with stars but no grid, or grid but no marks, fails this.
     */
    private static void assertInkIsWorthComparing(String where,
                                                  List<String> ink) {
        assertTrue(ink.size() >= 200,
                where + " records a whole page of non-text ink: "
                        + ink.size() + " operations");
        assertTrue(ink.stream().anyMatch(o -> o.startsWith("fill")),
                where + " fills something - marks and furniture");
        assertTrue(ink.stream().anyMatch(o -> o.startsWith("draw")),
                where + " strokes something - grid, figures,"
                        + " boundaries, limb");
        assertTrue(ink.stream().map(o -> o.split("\\|")[2]).distinct()
                        .count() >= 3,
                where + " paints in several inks, so more than one"
                        + " layer is present: " + ink.size());
    }

    /**
     * The page's recorded painting, text removed.
     *
     * <p>Recorded through the production sheet route, so this is the
     * same painting a reader exports. Both languages assemble the
     * same canonical scene at the same size through
     * {@link ChartSheet.Pages}; only the name map differs.
     */
    private static List<String> nonTextInk(ChartViewState state,
                                           boolean localised) {
        ChartSheet.Pages pages = (asked, wide, high) -> {
            ChartScene scene = Atlas.assembler().assemble(asked, wide, high);
            return localised ? named(scene, localised(scene)) : scene;
        };
        SheetRecording recording = ChartSheet.record(pages, state,
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4, ENGLISH);
        List<String> ink = new ArrayList<>();
        for (SheetRecorder.Operation operation
                : recording.recorder().operations()) {
            if (operation instanceof SheetRecorder.Drawn drawn) {
                ink.add((drawn.filled() ? "fill" : "draw")
                        + "|" + drawn.shape().getBounds2D()
                        + "|" + drawn.colour().getRGB()
                        + "|" + stroke(drawn.stroke()));
            }
        }
        return ink;
    }

    /**
     * A stroke as its values, never as its default string.
     *
     * <p>{@code BasicStrokeSpec} is a record holding a
     * {@code float[]} dash, so its generated {@code toString} prints
     * the array's identity hash. Two identical dash patterns then
     * stringify differently, and 57 boundary lines looked like a
     * language-dependent difference when nothing about them had
     * changed at all. Comparing the values is the fix; loosening the
     * comparison would have thrown away real signal with the noise.
     */
    private static String stroke(SheetRecorder.BasicStrokeSpec spec) {
        if (spec == null) {
            return "none";
        }
        return spec.width() + "/" + spec.cap() + "/" + spec.join()
                + "/" + spec.miterLimit()
                + "/" + java.util.Arrays.toString(spec.dash())
                + "/" + spec.dashPhase();
    }

    /** What a click answers, over a grid covering the page. */
    private static List<String> marksUnderThePointer(ChartScene scene) {
        List<String> hits = new ArrayList<>();
        ChartHitTest test = new ChartHitTest(
                new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH));
        for (int x = 50; x < WIDE_PX; x += 90) {
            for (int y = 50; y < HIGH_PX; y += 90) {
                ChartHitTest.Hit hit =
                        test.at(scene, ChartOptions.DEFAULTS, x, y);
                hits.add(x + "," + y + "="
                        + (hit == null ? "-" : String.valueOf(hit)));
            }
        }
        return hits;
    }

    private static List<String> inventoryIdentities(ChartScene scene) {
        PageContents contents =
                PageInventory.of(scene, ChartOptions.DEFAULTS);
        List<String> identities = new ArrayList<>();
        for (PageEntry entry : contents.entries()) {
            identities.add(entry.identity());
        }
        return identities;
    }

    /** The constellation names actually written on this page. */
    private static List<String> constellationText(ChartScene scene) {
        List<String> written = new ArrayList<>();
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            for (LabelPlacement.Placement placed
                    : new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                            .textPlacements(
                                    ChartRenderer.TextMetrics.of(g),
                                    scene, ChartOptions.DEFAULTS)) {
                if (!placed.omitted() && placed.request().family()
                        == LabelPlacement.Family.CONSTELLATION) {
                    written.add(placed.request().text());
                }
            }
        } finally {
            g.dispose();
        }
        return written;
    }
}
