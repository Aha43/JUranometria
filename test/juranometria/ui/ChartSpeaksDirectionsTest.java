package juranometria.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.Cardinal;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.PixelPoint;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The packaged chart's accessible description speaks the cardinal
 * directions the page actually rendered (#359).
 *
 * <p>The ruling's words: a {@code DirectionPlacement} merely carrying
 * a spoken name is not yet accessibility. What is proven here is the
 * route - the localized full name reaches the component's existing
 * reader-facing accessible description, follows the paint that put
 * the mark there, and leaves when the horizon leaves.
 */
class ChartSpeaksDirectionsTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    @Test
    void theDescriptionCarriesEachRenderedDirectionAndOnlyThose() {
        String base = "M 31, the page.";
        assertEquals(base, ChartComponent.withDirections(base,
                        List.of()),
                "no rendered direction, no appended speech");
        ReferenceInk.DirectionPlacement north =
                new ReferenceInk.DirectionPlacement(Cardinal.NORTH,
                        "N", "North on your horizon",
                        new PixelPoint(450, 591),
                        new Rectangle2D.Double(459, 584, 8, 13));
        assertEquals(base + " North on your horizon.",
                ChartComponent.withDirections(base, List.of(north)),
                "a rendered direction is spoken by its full localized"
                        + " name, never its letter");
    }

    @Test
    void thePackagedChartSpeaksItsHorizonDirections() throws Exception {
        speaks("en", "on your horizon");
        speaks("nb-NO", "på horisonten din");
    }

    private static void speaks(String language, String phrase)
            throws Exception {
        juranometria.project.PageWords words =
                juranometria.ui.language.PageText.in(
                        juranometria.ui.language.InterfaceText
                                .forLanguage(language));
        ChartComponent[] holder = new ChartComponent[1];
        MeridianModule[] module = new MeridianModule[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            module[0] = new MeridianModule(OSLO);
            module[0].showing(true, true, true);
            holder[0].overlays().offer(MeridianModule.ID,
                    module[0]::contributedGeometry);
        });
        // Aim the released chart low towards north until a cardinal
        // actually renders - selected from stated candidates, not
        // hand-tuned to one lucky altitude.
        String described = null;
        for (double altitude : new double[] {8.0, 10.0, 12.0, 15.0,
                18.0}) {
            aim(holder[0], altitude);
            paintOnce(holder[0]);
            String said = description(holder[0]);
            if (said != null && said.contains(phrase)) {
                described = said;
                break;
            }
        }
        assertTrue(described != null,
                "some stated northward altitude at 36 degrees renders"
                        + " a cardinal, and the description speaks it"
                        + " (" + language + ")");
        assertFalse(described.contains(" N."),
                "and never the bare letter as speech");

        // The lifecycle half: horizon off, the marks leave the page,
        // and the description stops claiming them.
        SwingUtilities.invokeAndWait(
                () -> module[0].showing(true, false, true));
        paintOnce(holder[0]);
        String after = description(holder[0]);
        assertFalse(after != null && after.contains(phrase),
                "horizon off: no direction is spoken, because none is"
                        + " drawn (" + language + ")");
    }

    private static void aim(ChartComponent chart, double altitude)
            throws Exception {
        LocalSky sky = new LocalSky(OSLO);
        double[] z = unit(sky.zenith());
        double[] n = unit(sky.cardinal(Cardinal.NORTH));
        double alt = Math.toRadians(altitude);
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            v[i] = Math.cos(alt) * n[i] + Math.sin(alt) * z[i];
        }
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        double centreRa = ra < 0 ? ra + 360.0 : ra;
        SwingUtilities.invokeAndWait(() -> chart.setViewState(
                new ChartViewState(new SkyPosition(centreRa,
                        Math.toDegrees(Math.asin(v[2]))), 36.0, 8.0)));
    }

    private static void paintOnce(ChartComponent chart)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
    }

    private static String description(ChartComponent chart)
            throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> said[0] =
                chart.getAccessibleContext().getAccessibleDescription());
        return said[0];
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }
}
