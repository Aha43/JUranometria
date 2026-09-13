package juranometria.sheet;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A globe exports as the page a reader actually reached (Sprint 32,
 * issue #331, step five).
 *
 * <p>For the length of the celestial-globe gate the sheet path could
 * not be asked about a hemisphere at all: a view state refused a
 * 180-degree field, so the study reached the writers through a door
 * of its own, {@code ChartSheet.recordForStudy}. #329 put the rung on
 * the ladder and the door's reason went with it. These hold what the
 * ordinary route now produces, so the door could be removed rather
 * than merely stop being called.
 *
 * <p>The page must say one thing about itself everywhere it speaks -
 * to a reader who can see it, to a reader who cannot, and to whoever
 * opens the file later - and a globe must not put its sky on the
 * paper outside its limb in any format it is written to.
 */
class GlobeSheetTest {

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    private static final PaperSize PAPER = PaperSize.A4;

    /** The globe as a reader reaches it: the settled rung and limit. */
    private static ChartViewState globe() {
        return new ChartViewState(SAGITTARIUS, 180.0, 5.0);
    }

    @Test
    void theOrdinaryRouteCanExportAHemisphere() throws Exception {
        // The removal's premise, asserted rather than assumed: a view
        // state describes a globe, and the production recorder takes
        // one. If this ever stopped being true the study would need
        // its door back, and would say so here first.
        SheetRecording sheet = recorded(settled());
        assertNotNull(sheet);
        assertEquals(180.0, globe().fieldWidthDegrees(), 1.0e-9,
                "a view state carries the globe's own rung");

        for (SheetFormat format : SheetFormat.values()) {
            byte[] written = SheetWriters.write(sheet, format, 300);
            assertTrue(written.length > 0,
                    format + " writes something");
        }
    }

    @Test
    void everyFormatNamesTheProjectionThatDrewThePage() throws Exception {
        SheetRecording sheet = recorded(settled());
        for (SheetFormat format : SheetFormat.values()) {
            String said = new String(
                    SheetWriters.write(sheet, format, 300),
                    StandardCharsets.ISO_8859_1);
            assertTrue(said.contains("orthographic"),
                    format + " says what drew the page");
            assertFalse(said.contains("gnomonic"),
                    format + " does not claim a projection it was not"
                            + " drawn by");
            assertFalse(said.contains("stereographic"),
                    format + " does not claim a projection it was not"
                            + " drawn by");
        }
    }

    @Test
    void theSheetAndTheSpokenPageAgreeAboutWhatThisIs() throws Exception {
        // One identity, however the page is asked. The description a
        // reader who cannot see the chart is given comes from
        // DrawnPage; the sheet's metadata is built beside it; both
        // must name the same projection, or the atlas says one thing
        // on screen and another in the file.
        ChartScene scene = Atlas.assembler().assemble(globe(),
                PAPER.chartWideUnits(), PAPER.chartHighUnits());
        String spoken = DrawnPage.of(scene).describe();
        assertTrue(spoken.contains("orthographic"),
                "the page tells a reader who cannot see it what drew"
                        + " it: " + spoken);

        String written = new String(
                SheetWriters.write(recorded(settled()), SheetFormat.SVG,
                        300),
                StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("orthographic"),
                "and the file agrees");
    }

    @Test
    void theSheetDrawsThePageTheReaderWasShown() throws Exception {
        // A sheet is a copy of a page, so it must be a copy of the
        // page the reader saw and not of the switches behind it. On a
        // globe those differ: a hemisphere turns constellation
        // boundaries off, and the sheet used to be written from the
        // raw switches - so an exported globe carried a layer the
        // screen had deliberately suppressed.
        ChartScene scene = Atlas.assembler().assemble(globe(),
                PAPER.chartWideUnits(), PAPER.chartHighUnits());
        ChartOptions readers = ChartOptions.DEFAULTS;
        ChartOptions onScreen = readers.onPage(DrawnPage.of(scene));

        // The premise: on this page the two really do differ, so the
        // comparison below has something to catch.
        assertTrue(readers.constellationBoundaries(),
                "the reader's own options draw boundaries");
        assertFalse(onScreen.constellationBoundaries(),
                "and the globe's page does not");

        assertEquals(inkOf(recorded(onScreen)), inkOf(recorded(readers)),
                "the sheet written from the reader's switches must be"
                        + " the sheet written from the page they were"
                        + " shown");
    }

    @Test
    void noSkyInkReachesThePaperBeyondTheLimb() throws Exception {
        // Two masks, never one. The page draws furniture out there by
        // design - the frame at the paper's edge, the title block,
        // the key - so the same sheet with every piece of sky
        // switched off is measured and subtracted. What is left is
        // what the sky itself put on the paper, and it must be
        // nothing.
        int all = beyondTheLimb(recorded(settled()));
        int furniture = beyondTheLimb(recorded(furnitureOnly()));
        assertEquals(furniture, all,
                "the written sheet carries " + (all - furniture)
                        + " px of sky beyond its limb, where the"
                        + " page's own furniture accounts for "
                        + furniture);

        // Stated precisely, because it would be easy to read this as
        // a test of the clip and it is not. On a globe the settled
        // page has nothing that can leave the disc: a figure is drawn
        // between stars that are all inside a convex circle, the grid
        // is bounded by PageRegion, the names are placed inside, and
        // boundaries are off. Removing the sky clip changes this
        // sheet by nothing at all, and GlobeClipTest is what holds
        // the clip, on a page where ink does reach past the limb.
        //
        // What this holds is the end-to-end property: whatever the
        // screen does, the written file carries no sky on the paper.
        assertTrue(furniture > 0,
                "the page has furniture to subtract: " + furniture);
    }

    @Test
    void aPdfStatesItsLimitRatherThanPassingSilently() throws Exception {
        // The stated limit, kept stated. A PDF carries neither text
        // elements nor pixels in a form this suite can read without a
        // parser it has no business writing, so its geometry is not
        // checked - and that is recorded here so a later reader does
        // not mistake three green formats for three checked ones.
        byte[] written = SheetWriters.write(recorded(settled()),
                SheetFormat.PDF, 300);
        assertTrue(written.length > 0, "the PDF is written");
        assertTrue(new String(written, StandardCharsets.ISO_8859_1)
                        .contains("orthographic"),
                "and its metadata is readable, which is what this"
                        + " format is held to");
    }

    /** Every inked pixel of this sheet, written as a PNG. */
    private static int inkOf(SheetRecording sheet) throws Exception {
        BufferedImage page = ImageIO.read(new ByteArrayInputStream(
                SheetWriters.write(sheet, SheetFormat.PNG, 300)));
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int inked = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) != ground) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /** Pixels beyond the limb in this sheet, written as a PNG. */
    private static int beyondTheLimb(SheetRecording sheet)
            throws Exception {
        BufferedImage page = ImageIO.read(new ByteArrayInputStream(
                SheetWriters.write(sheet, SheetFormat.PNG, 300)));
        double centreX = PAPER.chartWideUnits() / 2.0;
        double centreY = PAPER.chartHighUnits() / 2.0;
        double limb = 0.90 * Math.min(PAPER.chartWideUnits(),
                PAPER.chartHighUnits()) / 2.0;
        double scale = page.getWidth() / (centreX * 2.0);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int outside = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                if (Math.hypot(x / scale - centreX, y / scale - centreY)
                        > limb) {
                    outside++;
                }
            }
        }
        return outside;
    }

    private static SheetRecording recorded(ChartOptions options) {
        return ChartSheet.record(Atlas.assembler()::assemble, globe(),
                options, ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, PAPER);
    }

    /** The globe's settled page: every family on, boundaries off. */
    private static ChartOptions settled() {
        return new ChartOptions(true, true, true, false, true, true,
                true, true, true, true, true, true, true, true, true,
                true, ChartPalette.WHITE_PAPER);
    }

    /** The same page with every piece of sky switched off. */
    private static ChartOptions furnitureOnly() {
        ChartOptions on = settled();
        return new ChartOptions(false, false, false, false, false,
                false, false, false, false, on.titleBlock(),
                on.magnitudeKey(), false, false, false, false, false,
                on.palette());
    }
}
