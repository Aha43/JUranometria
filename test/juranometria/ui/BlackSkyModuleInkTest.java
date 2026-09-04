package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.SelectionModel;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module ink on the black sky, through the real component wiring
 * (Sprint 26, issue #246 review): {@code ChartComponent} is the one
 * place that hands the reader's palette to the reference-ink and
 * working-cross painters, and no other test would notice a call
 * site quietly pinned to white paper - crosses would vanish into
 * the black ground and reference lines would wear the wrong greys
 * while every painter-level test stayed green.
 *
 * <p>So these render the real component twice - without and with
 * the contribution - and reason over the <em>difference</em>: the
 * pixels the module's ink changed. Stars, furniture and their
 * antialiasing are identical in both renders and cannot enter the
 * diff, which is what makes the exact-grey claims below airtight
 * where a page-wide census would drown them in blends.
 */
class BlackSkyModuleInkTest {

    /**
     * The Sprint 25 journey's reviewed place and instant: the
     * meridian crosses the released page and the zenith sits on
     * clear sky. Stated, not anyone's home.
     */
    private static final Instant EQUINOX =
            Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer PLACE =
            new Observer(42.5, -130.994, EQUINOX);

    /** Every layer off, on the black sky: the module ink stands alone. */
    private static ChartOptions bareBlackSky() {
        return new ChartOptions(false, false, false, false, false,
                false, false, false, false)
                .withPalette(ChartPalette.BLACK_SKY);
    }

    private static ChartComponent component() throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(bareBlackSky());
        });
        flush();
        return holder[0];
    }

    private static int[] painted(ChartComponent component)
            throws Exception {
        int[][] holder = new int[1][];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(
                    component.getWidth(), component.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            component.paintComponent(image.createGraphics());
            holder[0] = image.getRGB(0, 0, image.getWidth(),
                    image.getHeight(), null, 0, image.getWidth());
        });
        return holder[0];
    }

    @Test
    void aWorkingCrossReachesTheComponentInBlackSkyInk()
            throws Exception {
        ChartComponent chart = component();
        int[] plain = painted(chart);

        // The same registry call the on-this-page module makes.
        Runnable[] withdraw = new Runnable[1];
        SwingUtilities.invokeAndWait(() -> withdraw[0] =
                chart.overlays().offer("on-this-page", () -> List.of(
                        new juranometria.module.OverlayContribution.Point(
                                "TEST", "working mark on TEST",
                                juranometria.page.PageExtent.offsetOf(
                                        chart.currentScene().viewport()
                                                .centre(), 1.5, 40.0),
                                juranometria.module.InkRole
                                        .INTERACTION))));
        int[] marked = painted(chart);
        SwingUtilities.invokeAndWait(withdraw[0]);

        int changed = 0;
        int brightest = 0;
        for (int i = 0; i < plain.length; i++) {
            if (plain[i] == marked[i]) {
                continue;
            }
            changed++;
            brightest = Math.max(brightest, marked[i] & 0xff);
        }
        // The mutation this kills: WorkingCrossInk handed white
        // paper draws a black cross on the black ground - the page
        // does not change at all, and a reader's mark has vanished.
        assertTrue(changed > 0,
                "the cross leaves visible ink on the black sky");
        // A one-pixel stroke at fractional coordinates antialiases,
        // so no pixel need carry the exact ink; what cannot be
        // faked is where the blend comes FROM. White interaction
        // ink blends downward from 255; any dimmer role colour
        // handed here by mistake tops out at its own grey.
        assertTrue(brightest >= 200,
                "and its brightest pixel blends from the interaction"
                        + " white, not from a dim grey: " + brightest);
    }

    @Test
    void theMeridianModuleReachesTheComponentInBlackSkyInk()
            throws Exception {
        ChartComponent chart = component();
        int[] plain = painted(chart);

        // The real module through the real host - the production
        // attachment, not a module-shaped stand-in.
        MeridianModule[] meridian = new MeridianModule[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartModuleHost host = new ChartModuleHost(chart,
                    new SelectionModel(), request -> { });
            meridian[0] = new MeridianModule(PLACE);
            meridian[0].attach(host);
        });
        int[] inked = painted(chart);
        SwingUtilities.invokeAndWait(meridian[0]::detach);

        int changed = 0;
        int referenceInk = 0;
        int labelInk = 0;
        int paperFigureInk = 0;
        int paperLabelInk = 0;
        for (int i = 0; i < plain.length; i++) {
            if (plain[i] == inked[i]) {
                continue;
            }
            changed++;
            if (inked[i] == ChartPalette.BLACK_SKY.figureInk()
                    .getRGB()) {
                referenceInk++;
            }
            if (inked[i] == ChartPalette.BLACK_SKY.gridLabelInk()
                    .getRGB()) {
                labelInk++;
            }
            if (inked[i] == ChartPalette.WHITE_PAPER.figureInk()
                    .getRGB()) {
                paperFigureInk++;
            }
            if (inked[i] == ChartPalette.WHITE_PAPER.gridLabelInk()
                    .getRGB()) {
                paperLabelInk++;
            }
        }
        assertTrue(changed > 0,
                "the meridian geometry leaves ink on the page");
        assertTrue(referenceInk > 0,
                "reference lines wear the black-sky figure ink,"
                        + " exact");
        assertTrue(labelInk > 0,
                "and their names the black-sky notation ink, exact");
        // The mutation these kill: ReferenceInk handed white paper
        // draws its lines in grey 120 and its labels in grey 150.
        // Inside the diff nothing else can produce those values -
        // the module's own inks are 115 and 88, whose blends toward
        // the ground only go down - so a single wrong-palette pixel
        // is a failure, not a statistic.
        assertEquals(0, paperFigureInk,
                "no reference pixel wears white paper's figure grey");
        assertEquals(0, paperLabelInk,
                "and none wears white paper's notation grey");
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
