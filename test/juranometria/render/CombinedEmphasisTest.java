package juranometria.render;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetWriters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Any number of structures may be raised at once, and the released
 * one-target behaviour is unmoved (the multiple-emphasis follow-up
 * to #361).
 *
 * <p>Two halves. The first half pins the released behaviour: every
 * singleton page and the one-target export bytes, fingerprinted on
 * the released chart before this change, must reproduce exactly
 * through the set-shaped seam. The second half is the ruled
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

    // ---- the released fingerprints, captured on main 3176d93 -------

    private static final Map<String, String> RELEASED = Map.ofEntries(
            Map.entry("orion-42|white-paper|meridian",
                    "94ce86d9813221fd7dbbcaf615630b29ff11a0cecfa9d83005edec900c7073c8"),
            Map.entry("orion-42|white-paper|ecliptic",
                    "94ce86d9813221fd7dbbcaf615630b29ff11a0cecfa9d83005edec900c7073c8"),
            Map.entry("orion-42|white-paper|equatorial-grid",
                    "fd5cf22427631991dabc0158b7684bf20f8c1dcbc6b30c30ff33abbaba73be0e"),
            Map.entry("orion-42|white-paper|horizon",
                    "94ce86d9813221fd7dbbcaf615630b29ff11a0cecfa9d83005edec900c7073c8"),
            Map.entry("orion-42|white-paper|constellation-boundaries",
                    "f053b8041a12389ca8916a713c8fe4044f54408ea1aa920241589681b9bbf226"),
            Map.entry("orion-42|white-paper|constellation-figures",
                    "c3b6640aab037794db2710ebca9c0e41eff363f36be96e28e324ddafe36feb96"),
            Map.entry("orion-42|black-sky|meridian",
                    "776b45fb23965f1f1b1c5bca3d0b0c2e849e376ca11e4fe9dd2fb0aa2d701d73"),
            Map.entry("orion-42|black-sky|ecliptic",
                    "776b45fb23965f1f1b1c5bca3d0b0c2e849e376ca11e4fe9dd2fb0aa2d701d73"),
            Map.entry("orion-42|black-sky|equatorial-grid",
                    "4a9563aa8df4d4de109b241ba76a2f73f587aeefb73cb16f30a5ea4d805f5a91"),
            Map.entry("orion-42|black-sky|horizon",
                    "776b45fb23965f1f1b1c5bca3d0b0c2e849e376ca11e4fe9dd2fb0aa2d701d73"),
            Map.entry("orion-42|black-sky|constellation-boundaries",
                    "d00bc436091935b1da7a0e8983357b4e9c05a89dea80408d9c094c8903a8b2f0"),
            Map.entry("orion-42|black-sky|constellation-figures",
                    "ffbe55b2b49c1fbef5b500b05784de95cdb66fe346c7e1e217be977a5b2beb77"),
            Map.entry("sagittarius-120|white-paper|meridian",
                    "2a84eb0a49cc512c3fc919b2f2dc518df02b80ac865eaa699109c38c248717d4"),
            Map.entry("sagittarius-120|white-paper|ecliptic",
                    "2a84eb0a49cc512c3fc919b2f2dc518df02b80ac865eaa699109c38c248717d4"),
            Map.entry("sagittarius-120|white-paper|equatorial-grid",
                    "4d84df668a993353de10efb0827538d1e2f33a039c953fa2630ba35eebfc368e"),
            Map.entry("sagittarius-120|white-paper|horizon",
                    "2a84eb0a49cc512c3fc919b2f2dc518df02b80ac865eaa699109c38c248717d4"),
            Map.entry("sagittarius-120|white-paper|constellation-boundaries",
                    "4700a045776d46724d4e6f3b4767d07081d0d6a418f7c1e6c94fd7f1d6e14205"),
            Map.entry("sagittarius-120|white-paper|constellation-figures",
                    "d3242bba9de7b79956f054db777cae5234ea0cb068a32b8de40a05a8edc76727"),
            Map.entry("sagittarius-120|black-sky|meridian",
                    "b3bcd3cc6e90fefbb429a9de6a145c995baf339ed7e67d0a337bb1669eb6cb5e"),
            Map.entry("sagittarius-120|black-sky|ecliptic",
                    "b3bcd3cc6e90fefbb429a9de6a145c995baf339ed7e67d0a337bb1669eb6cb5e"),
            Map.entry("sagittarius-120|black-sky|equatorial-grid",
                    "c1c6f71ee6ca3bee178d2beec93934e6515339413db0d953d64cbb10824438b7"),
            Map.entry("sagittarius-120|black-sky|horizon",
                    "b3bcd3cc6e90fefbb429a9de6a145c995baf339ed7e67d0a337bb1669eb6cb5e"),
            Map.entry("sagittarius-120|black-sky|constellation-boundaries",
                    "44ea08927e96e3dc053b4158ba0d13af2a612403e3fde9bd4fe09400ecaeabf3"),
            Map.entry("sagittarius-120|black-sky|constellation-figures",
                    "50e015418104ad263d6fe02f7c2820a863bf798349c7fa1b03e5231654704702"));

    private static final Map<SheetFormat, String> RELEASED_EXPORT = Map.of(
            SheetFormat.SVG,
            "34cb425ca6c2d73b5721b1514ca5c4ce415f3bb7150f0ee7db9c71d8ce622156",
            SheetFormat.PDF,
            "f5b2eeabd1d6b9f7a29e81e610a649601defe34dd834fc324af2d86acfd7d98f",
            SheetFormat.PNG,
            "3717f24ece8854342cfbf284e8a21ba1945ca1968bdc9ebf8c052c5736e89266");

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

    // ---- the released one-target behaviour is unmoved ---------------

    @Test
    void everyReleasedSingletonPageReproducesThroughTheSetSeam()
            throws Exception {
        for (Page page : PAGES) {
            ChartScene scene = scene(page);
            for (ChartPalette ground : ChartPalette.values()) {
                ChartOptions options =
                        ChartOptions.DEFAULTS.withPalette(ground);
                for (ChartStructure target : ChartStructure.values()) {
                    BufferedImage image = RENDERER.renderToImage(
                            scene, options, Set.of(target));
                    assertEquals(
                            RELEASED.get(page.name() + "|"
                                    + ground.storedAs() + "|"
                                    + target.token()),
                            sha(image),
                            page.name() + " with only "
                                    + target.token() + " raised on "
                                    + ground.storedAs()
                                    + " must be the released page");
                }
            }
        }
    }

    @Test
    void theReleasedOneTargetExportBytesAreUnmoved() throws Exception {
        var recording = ChartSheet.record(Atlas.assembler()::assemble,
                new ChartViewState(new SkyPosition(83.0, 0.0), 42.0, 8.0),
                ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4,
                ENGLISH, Set.of(ChartStructure.EQUATORIAL_GRID));
        for (SheetFormat format : SheetFormat.values()) {
            assertEquals(RELEASED_EXPORT.get(format),
                    shaBytes(SheetWriters.write(recording, format, 300)),
                    "a one-structure emphasized " + format
                            + " export must carry the released bytes");
        }
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
        for (ChartPalette ground : ChartPalette.values()) {
            Renders r = renders(ground);
            int checkedGrid = 0;
            int checkedFigures = 0;
            for (int y = 0; y < r.height(); y++) {
                for (int x = 0; x < r.width(); x++) {
                    if (r.gridMask()[y][x]
                            && !near(r.figuresMask(), x, y)) {
                        assertEquals(r.grid().getRGB(x, y),
                                r.both().getRGB(x, y),
                                "away from the figures, the grid's"
                                        + " combined ink at (" + x + ","
                                        + y + ") on " + ground.storedAs()
                                        + " is its singleton ink");
                        checkedGrid++;
                    }
                    if (r.figuresMask()[y][x]
                            && !near(r.gridMask(), x, y)) {
                        assertEquals(r.figures().getRGB(x, y),
                                r.both().getRGB(x, y),
                                "away from the grid, the figures'"
                                        + " combined ink at (" + x + ","
                                        + y + ") on " + ground.storedAs()
                                        + " is their singleton ink");
                        checkedFigures++;
                    }
                }
            }
            assertTrue(checkedGrid > 0 && checkedFigures > 0,
                    "the contract must have pixels to hold on "
                            + ground.storedAs());
        }
    }

    /**
     * A combination introduces no changed region outside the
     * singleton changes and the structures' shared stroke and
     * antialiasing envelope.
     */
    @Test
    void aCombinationChangesNothingOutsideTheSingletonEnvelopes() {
        for (ChartPalette ground : ChartPalette.values()) {
            Renders r = renders(ground);
            for (int y = 0; y < r.height(); y++) {
                for (int x = 0; x < r.width(); x++) {
                    if (r.both().getRGB(x, y)
                            != r.canonical().getRGB(x, y)) {
                        assertTrue(near(r.gridMask(), x, y)
                                        || near(r.figuresMask(), x, y),
                                "a combined change at (" + x + "," + y
                                        + ") on " + ground.storedAs()
                                        + " lies outside every"
                                        + " singleton's envelope");
                    }
                }
            }
        }
    }

    /**
     * At crossings the existing painter order stands and each
     * structure keeps its own frozen accent: the figures are painted
     * after the grid, so wherever the figures' singleton laid down
     * their pure accent inside the grid's envelope, the combination
     * carries that same pixel - the lower stroke never rises above
     * the upper one, and no blended accent replaces either. Both
     * pure accents must also survive somewhere on the combined page.
     */
    @Test
    void crossingsKeepThePainterOrderAndTheFrozenAccents() {
        for (ChartPalette ground : ChartPalette.values()) {
            Renders r = renders(ground);
            int gridAccent = StructureStyle.accent(ground,
                    ChartStructure.EQUATORIAL_GRID).getRGB();
            int figuresAccent = StructureStyle.accent(ground,
                    ChartStructure.CONSTELLATION_FIGURES).getRGB();
            int crossings = 0;
            boolean gridAccentSeen = false;
            boolean figuresAccentSeen = false;
            for (int y = 0; y < r.height(); y++) {
                for (int x = 0; x < r.width(); x++) {
                    int combined = r.both().getRGB(x, y);
                    gridAccentSeen |= combined == gridAccent;
                    figuresAccentSeen |= combined == figuresAccent;
                    if (r.figures().getRGB(x, y) == figuresAccent
                            && near(r.gridMask(), x, y)) {
                        assertEquals(figuresAccent, combined,
                                "at the crossing (" + x + "," + y
                                        + ") on " + ground.storedAs()
                                        + " the later-painted figures"
                                        + " keep their frozen accent"
                                        + " on top");
                        crossings++;
                    }
                }
            }
            assertTrue(crossings > 0,
                    "the page must actually cross on "
                            + ground.storedAs());
            assertTrue(gridAccentSeen && figuresAccentSeen,
                    "both frozen accents survive on the combined page"
                            + " on " + ground.storedAs());
        }
    }

    // ---- the pixel arithmetic ---------------------------------------

    /**
     * The orion page at 42 degrees carries both an equatorial grid
     * and constellation figures, crossing many times; canonical,
     * each singleton, and the combination are rendered once per
     * ground and compared pixel for pixel.
     */
    private record Renders(BufferedImage canonical, BufferedImage grid,
                           BufferedImage figures, BufferedImage both,
                           boolean[][] gridMask,
                           boolean[][] figuresMask) {
        int width() {
            return canonical.getWidth();
        }
        int height() {
            return canonical.getHeight();
        }
    }

    private static Renders renders(ChartPalette ground) {
        ChartScene scene = scene(PAGES[0]);
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(ground);
        BufferedImage canonical = RENDERER.renderToImage(scene, options,
                Set.<ChartStructure>of());
        BufferedImage grid = RENDERER.renderToImage(scene, options,
                Set.of(ChartStructure.EQUATORIAL_GRID));
        BufferedImage figures = RENDERER.renderToImage(scene, options,
                Set.of(ChartStructure.CONSTELLATION_FIGURES));
        BufferedImage both = RENDERER.renderToImage(scene, options,
                Set.of(ChartStructure.EQUATORIAL_GRID,
                        ChartStructure.CONSTELLATION_FIGURES));
        return new Renders(canonical, grid, figures, both,
                changed(canonical, grid), changed(canonical, figures));
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

    private static String sha(BufferedImage image) throws Exception {
        java.io.ByteArrayOutputStream out =
                new java.io.ByteArrayOutputStream();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int p = image.getRGB(x, y);
                out.write(p >> 16);
                out.write(p >> 8);
                out.write(p);
            }
        }
        return shaBytes(out.toByteArray());
    }

    private static String shaBytes(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256")
                .digest(bytes)) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }
}
