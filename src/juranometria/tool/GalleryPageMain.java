package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;

/**
 * The gallery's module slides (Sprint 27, issue #252), composed by
 * the production component itself.
 *
 * <p>The gallery may not promote study previews as application
 * output, and the curation found it had to care: the place-and-time
 * ink study's pages predate #227 and paint a <em>candidate</em>
 * reference treatment, not the shipped {@code ReferenceInk}, and no
 * committed artifact shows working crosses at all, because crosses
 * and reference ink exist only in the live component's paint path.
 * So these slides are painted by a real {@link ChartComponent} with
 * the real overlay registry, the real {@link MeridianModule}, and
 * the real ink painters - the exact composition a reader's window
 * performs, offscreen.
 *
 * <p>The place and instant are the ones the committed dialog
 * photograph shows - Oslo, 59.913 north, 10.752 east, the 2026
 * March equinox evening 21:33 UTC - so the UI slide and the chart
 * slides tell one story. Deterministic per machine, like every
 * renderer study; regenerated and compared by the evidence
 * contracts.
 */
public final class GalleryPageMain {

    private GalleryPageMain() {
    }

    private static final File DIR = new File("docs/studies/gallery");

    /** The dialog photograph's own place and instant, restated. */
    private static final Instant WHEN = ZonedDateTime
            .of(2026, 3, 20, 21, 33, 0, 0, ZoneOffset.UTC).toInstant();
    private static final Observer OSLO =
            new Observer(59.913, 10.752, WHEN);

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();

        // --- On This Page: working crosses on the M31 page --------
        // Three marked rows from the released inventory, the lead
        // being NGC 206 - the star cloud the chart records but draws
        // no symbol for, which is exactly what a working cross is
        // for. Positions come from the scene's own catalogue
        // entries, never re-typed.
        ChartComponent marksChart = component(ChartViewState.DEFAULT);
        SwingUtilities.invokeAndWait(() -> {
            List<OverlayContribution> marks = new ArrayList<>();
            for (String id : new String[] {"NGC 206", "NGC 221",
                    "NGC 205"}) {
                marks.add(new OverlayContribution.Point(id,
                        "working mark on " + id,
                        positionOf(marksChart, id),
                        InkRole.INTERACTION));
            }
            marksChart.overlays().offer("on-this-page", () -> marks);
            marksChart.setHighlightedObject("NGC 206");
        });
        write(marksChart, "on-this-page-marks");

        // --- Place and Time: the real module's ink -----------------
        LocalSky sky = new LocalSky(OSLO);
        // The zenith page: the meridian runs through the point
        // overhead, wearing its ring and tick.
        slide(new ChartViewState(sky.zenith(), 24.0, 8.0, null, null),
                "place-and-time-zenith");
        // The horizon page: centred where the mathematical horizon
        // crosses the sky - the one line that divides what this
        // observer can see from what they cannot.
        slide(new ChartViewState(sky.horizon().around(72).get(9), 24.0,
                8.0, null, null), "place-and-time-horizon");

        System.out.println("gallery slides written to " + DIR.getPath());
    }

    /** One place-and-time slide through the production composition. */
    private static void slide(ChartViewState state, String name)
            throws Exception {
        ChartComponent chart = component(state);
        SwingUtilities.invokeAndWait(() -> {
            MeridianModule meridian = new MeridianModule(OSLO);
            chart.overlays().offer(MeridianModule.ID,
                    meridian::contributedGeometry);
        });
        write(chart, name);
    }

    /**
     * A sized component whose scene is settled: setSize posts the
     * resize event to the queue, so the assembly happens when the
     * queue drains - the component-test shape, not a guess.
     */
    private static ChartComponent component(ChartViewState state)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(900, 700);
            holder[0].setViewState(state);
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    private static SkyPosition positionOf(ChartComponent chart,
                                          String id) {
        for (DeepSkyObject dso : chart.currentScene().deepSkyObjects()) {
            if (id.equals(dso.id())) {
                return dso.position();
            }
        }
        throw new IllegalStateException(
                "the page does not carry " + id);
    }

    private static void write(ChartComponent chart, String name)
            throws Exception {
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
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        System.out.println("  " + name);
    }
}
