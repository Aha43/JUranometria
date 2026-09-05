package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.SkyFrame;
import juranometria.ui.ChartComponent;

/**
 * The ecliptic cartography study (Sprint 28, issue #271): the fixed
 * mean ecliptic of J2000 rendered by the real chart, so the gate
 * decides line, labels, layering, and named marks by looking at
 * production-painted pages rather than at a mock-up.
 *
 * <p>Painted by a live {@link ChartComponent} with the real overlay
 * registry and the real reference-ink painter - the exact path a
 * reader's window uses. The ecliptic is contributed as the existing
 * {@code GreatCircle} plus four {@code Point} marks for the cardinal
 * landmarks, which is the whole of the geometry the module will owe.
 *
 * <p>These pages show the ecliptic in the <em>existing</em>
 * treatments, and exist to show that both are wrong for it: the
 * {@code LINE} stroke is the meridian's own, and the {@code Point}
 * symbol is the zenith's ring and tick - a <em>place</em> overhead,
 * which an equinox is not. Those are the two additions the decision
 * names for issue #273; {@link EclipticCandidateStudyMain} draws the
 * candidates and the chosen replacements.
 */
public final class EclipticInkStudyMain {

    private EclipticInkStudyMain() {
    }

    private static final File DIR = new File("docs/studies/ecliptic");

    private static final double EPS0 = SkyFrame.meanObliquityDegrees(0.0);
    private static final SkyPosition POLE =
            new SkyPosition(270.0, 90.0 - EPS0);

    /**
     * The geometry the module will contribute: the circle by its
     * pole, and a point for each named landmark.
     *
     * <p>Offered to the chart and inked by the real
     * {@code ReferenceInk}, so these pages prove the existing
     * {@code GreatCircle} and {@code Point} carry the ecliptic with no
     * new geometry type. What they also show - which is why the gate
     * needed them - is that the existing <em>treatments</em> are
     * wrong for it: the line is the meridian's own stroke and the
     * marks are the zenith's own symbol.
     * {@link EclipticCandidateStudyMain} draws the replacements.
     */
    private static List<OverlayContribution> ecliptic() {
        return List.of(
                new OverlayContribution.GreatCircle("ecliptic", "Ecliptic",
                        POLE, OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE),
                new OverlayContribution.Point("march-equinox",
                        "March equinox", new SkyPosition(0.0, 0.0),
                        // PLACE deliberately: these pages exist to
                        // show the treatment the gate rejected -
                        // an equinox wearing the zenith's symbol.
                        OverlayContribution.Mark.PLACE,
                        InkRole.REFERENCE_LINE),
                new OverlayContribution.Point("june-solstice",
                        "June solstice", new SkyPosition(90.0, EPS0),
                        // PLACE deliberately: these pages exist to
                        // show the treatment the gate rejected -
                        // an equinox wearing the zenith's symbol.
                        OverlayContribution.Mark.PLACE,
                        InkRole.REFERENCE_LINE),
                new OverlayContribution.Point("september-equinox",
                        "September equinox", new SkyPosition(180.0, 0.0),
                        // PLACE deliberately: these pages exist to
                        // show the treatment the gate rejected -
                        // an equinox wearing the zenith's symbol.
                        OverlayContribution.Mark.PLACE,
                        InkRole.REFERENCE_LINE),
                new OverlayContribution.Point("december-solstice",
                        "December solstice", new SkyPosition(270.0, -EPS0),
                        // PLACE deliberately: these pages exist to
                        // show the treatment the gate rejected -
                        // an equinox wearing the zenith's symbol.
                        OverlayContribution.Mark.PLACE,
                        InkRole.REFERENCE_LINE));
    }

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.out.println("ecliptic cartography study pages:");
        // Vernal equinox at the RA-0 seam: the crossing of ecliptic
        // and equator, and the wrap the projection must survive.
        page("equinox", new SkyPosition(0.0, 0.0), 24.0, false);
        // Summer solstice: the northern extremum, its named mark.
        page("solstice", new SkyPosition(90.0, EPS0), 24.0, false);
        // The whole arc at the widest field the pole page allows.
        page("wide", new SkyPosition(0.0, 0.0), 36.0, false);
        // A dense Milky Way field the ecliptic crosses near the
        // winter solstice (Sagittarius), for legibility where it
        // meets crowded catalogue ink.
        page("dense", new SkyPosition(270.0, -EPS0), 18.0, false);
        // A tight field at the equinox: named-mark collision.
        page("narrow", new SkyPosition(0.0, 0.0), 8.0, false);
        // A high-declination page: the ecliptic is far away and must
        // simply be absent, clipped without an invented chord.
        page("polar", new SkyPosition(0.0, 88.0), 8.0, false);
        // Both grounds carry the same geometry in palette-owned ink.
        page("equinox-black", new SkyPosition(0.0, 0.0), 24.0, true);
        page("wide-black", new SkyPosition(0.0, 0.0), 36.0, true);
        System.out.println("written to " + DIR.getPath());
    }

    private static void page(String name, SkyPosition centre,
                             double field, boolean blackSky)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(900, 700);
            if (blackSky) {
                holder[0].setChartOptions(ChartOptions.DEFAULTS
                        .withPalette(ChartPalette.BLACK_SKY));
            }
            holder[0].setViewState(new ChartViewState(centre, field, 8.0));
            holder[0].overlays().offer("ecliptic",
                    EclipticInkStudyMain::ecliptic);
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, "page-" + name + ".png"));
        System.out.println("  page-" + name + " ("
                + (blackSky ? "black sky, " : "white paper, ")
                + String.format(java.util.Locale.ROOT, "%.0f°", field)
                + ")");
    }
}
