package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.NavigationRequest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meridian's contextual altitude readout on production pages (#563,
 * ruled R-1 to R-8 on #675 stage 5): drawn only while altitude ticks are
 * on and no ruled tick is on the page, at the visible point nearest the
 * page centre, its altitude to the degree, spoken in the page's words;
 * never at the zenith; nothing where a ruled tick shows.
 */
class MeridianReadoutInkTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));
    private static final LocalSky SKY = new LocalSky(OSLO);

    private record Page(ChartComponent chart, MeridianModule module) {
    }

    private static Page page(SkyPosition centre, double field, String language, ChartPalette palette)
            throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        MeridianModule[] module = new MeridianModule[1];
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler(),
                    PageText.in(InterfaceText.forLanguage(language)));
            chart[0].setSize(900, 700);
            chart[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            chart[0].setViewState(new ChartViewState(centre, field,
                    ChartViewState.defaultMagnitudeFor(field)));
            List<NavigationRequest> asked = new ArrayList<>();
            ChartModuleHost host = new ChartModuleHost(chart[0], new SelectionModel(), asked::add);
            module[0] = host.attach(new MeridianModule(OSLO));
            module[0].showing(true, true, false);
        });
        return new Page(chart[0], module[0]);
    }

    private static BufferedImage paint(ChartComponent chart) throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    /** Pixels that differ between two paints within a radius of the page centre. */
    private static int differencesNearTheCentre(BufferedImage a, BufferedImage b, int radius) {
        int n = 0;
        for (int y = 350 - radius; y <= 350 + radius; y++) {
            for (int x = 450 - radius; x <= 450 + radius; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static String spoken(ChartComponent chart) throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() ->
                said[0] = chart.getAccessibleContext().getAccessibleDescription());
        return said[0];
    }

    @Test
    void betweenTwoTicksAtANarrowFieldTheReadoutIsDrawnNearTheCentreAndSpoken() throws Exception {
        Page page = page(SKY.onMeridian(35.37, false), 4.0, "en", ChartPalette.WHITE_PAPER);
        BufferedImage without = paint(page.chart());
        SwingUtilities.invokeAndWait(() -> page.module().ticksShowing(true));
        BufferedImage with = paint(page.chart());
        assertTrue(differencesNearTheCentre(without, with, 40) > 4,
                "the readout's stroke (and its figure, where it found room) is inked near the centre");
        String said = spoken(page.chart());
        assertTrue(said.contains("Meridian altitude here: about 35°."),
                "and the page says what it reads: " + said);
    }

    @Test
    void whereARuledTickIsOnThePageThereIsNoReadout() throws Exception {
        Page page = page(SKY.onMeridian(35.37, false), 12.0, "en", ChartPalette.WHITE_PAPER);
        BufferedImage without = paint(page.chart());
        SwingUtilities.invokeAndWait(() -> page.module().ticksShowing(true));
        BufferedImage with = paint(page.chart());
        assertEquals(0, differencesNearTheCentre(without, with, 40),
                "the 30° and 40° ticks are on the page, far from the centre; nothing is drawn at it");
        assertFalse(spoken(page.chart()).contains("Meridian altitude here"),
                "and nothing is spoken");
    }

    @Test
    void theZenithIsNeverARedoutAndTheHorizonSideStillGetsOne() throws Exception {
        Page nearZenith = page(SKY.onMeridian(89.8, false), 2.0, "en", ChartPalette.WHITE_PAPER);
        SwingUtilities.invokeAndWait(() -> nearZenith.module().ticksShowing(true));
        paint(nearZenith.chart());
        assertFalse(spoken(nearZenith.chart()).contains("Meridian altitude here"),
                "89.8° rounds to 90°, the zenith's, which is never stated");
        Page nearHorizon = page(SKY.onMeridian(3.7, false), 4.0, "en", ChartPalette.WHITE_PAPER);
        SwingUtilities.invokeAndWait(() -> nearHorizon.module().ticksShowing(true));
        paint(nearHorizon.chart());
        assertTrue(spoken(nearHorizon.chart()).contains("Meridian altitude here: about 4°."),
                "just above the horizon the reading is 4°");
    }

    @Test
    void theReadoutSpeaksNorwegianOnANorwegianPageAndIsDrawnOnTheBlackSky() throws Exception {
        Page norsk = page(SKY.onMeridian(35.37, false), 2.0, "nb-NO", ChartPalette.BLACK_SKY);
        BufferedImage without = paint(norsk.chart());
        SwingUtilities.invokeAndWait(() -> norsk.module().ticksShowing(true));
        BufferedImage with = paint(norsk.chart());
        assertTrue(differencesNearTheCentre(without, with, 40) > 4, "drawn on the black sky too");
        assertTrue(spoken(norsk.chart()).contains("Meridianens høyde her: omtrent 35°."),
                "in the page's language: " + spoken(norsk.chart()));
    }

    @Test
    void withTicksOffThereIsNoReadoutEither() throws Exception {
        Page page = page(SKY.onMeridian(35.37, false), 4.0, "en", ChartPalette.WHITE_PAPER);
        paint(page.chart());
        assertFalse(spoken(page.chart()).contains("Meridian altitude here"),
                "the readout rides the altitude ticks (R-6): off with them");
    }
}
