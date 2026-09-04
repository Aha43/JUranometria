package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.ui.ChartComponent;

/**
 * The working-selection gate's evidence (Sprint 27, issue #258):
 * what the two existing models do today, shown through the
 * production component with production painters and no production
 * change - and the decided presentation, previewed the same way.
 *
 * <p>The report is platform-stable prose and census; every number
 * that depends on a font or a machine lives in the decision
 * document with its machine stated (#257's lesson). The images are
 * component-composed like the gallery's slides: the real
 * {@code ChartComponent}, the real overlay registry, the real
 * selection ring and working-cross painters.
 */
public final class WorkingSelectionStudyMain {

    private WorkingSelectionStudyMain() {
    }

    private static final File DIR =
            new File("docs/studies/working-selection");

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        report();
        images();
    }

    // ---- the evidence images -------------------------------------

    /**
     * Both images sit on the released Andromeda page, whose
     * inventory carries drawn members (M 31, M 32) and the
     * symbol-less NGC 206 - the three presentations the decision
     * needs in one frame.
     */
    private static void images() throws Exception {
        // Today: the ring answers SelectionModel while the lead
        // cross answers WorkingMarksModel - two leads, honestly
        // photographed disagreeing. The ring sits on M 32; the
        // cross lead is NGC 206.
        ChartComponent today = component();
        SwingUtilities.invokeAndWait(() -> {
            offerCrosses(today, List.of("NGC 206"));
            today.setHighlightedObject("NGC 221");
        });
        write(today, "today-two-leads", List.of());
        // Decided: one model. Every drawn member wears the chart's
        // one selection ring; the on-page undrawn member wears the
        // one restrained cross, leading; off-page members (not
        // composable into a single frame, by definition) leave no
        // ink. Composed by calling the production ring painter once
        // per drawn member - the painters are unchanged; only the
        // number of calls previews the decision.
        ChartComponent decided = component();
        SwingUtilities.invokeAndWait(() -> {
            offerCrosses(decided, List.of("NGC 206"));
            decided.setHighlightedObject("NGC 206");
        });
        write(decided, "decided-members",
                List.of("NGC 224", "NGC 221"));
        System.out.println();
        System.out.println("Images beside this report:"
                + " `today-two-leads.png` (the ring on M 32 from one"
                + " model, the lead cross on NGC 206 from the other -"
                + " the disagreement the decision ends) and"
                + " `decided-members.png` (rings on M 31 and M 32,"
                + " the lead cross on NGC 206: one set, one lead,"
                + " one treatment each).");
    }

    private static ChartComponent component() throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(900, 700);
            holder[0].setViewState(ChartViewState.DEFAULT);
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    private static void offerCrosses(ChartComponent chart,
                                     List<String> identities) {
        List<OverlayContribution> points = new ArrayList<>();
        for (String identity : identities) {
            points.add(new OverlayContribution.Point(identity,
                    "working mark on " + identity,
                    positionOf(chart, identity), InkRole.INTERACTION));
        }
        chart.overlays().offer("on-this-page", () -> points);
    }

    private static SkyPosition positionOf(ChartComponent chart,
                                          String identity) {
        for (DeepSkyObject dso : chart.currentScene().deepSkyObjects()) {
            if (identity.equals(dso.id())) {
                return dso.position();
            }
        }
        throw new IllegalStateException("not on the page: " + identity);
    }

    /**
     * Paints the component, then previews additional member rings
     * by calling the production ring painter once per identity -
     * the unchanged painter, applied the decided number of times.
     */
    private static void write(ChartComponent chart, String name,
                              List<String> extraRings)
            throws Exception {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
                juranometria.render.ChartRenderer renderer =
                        new juranometria.render.ChartRenderer(
                                juranometria.chart.StarSizePolicy.DEFAULT);
                for (String identity : extraRings) {
                    renderer.drawSelectionHighlight(g,
                            chart.currentScene(), chart.chartOptions(),
                            identity);
                }
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
    }

    // ---- the report ----------------------------------------------

    private static void report() {
        System.out.println("# The working selection: what exists,"
                + " and what is decided");
        System.out.println();
        System.out.println("Generated by `make working-selection-study`"
                + " (juranometria.tool.WorkingSelectionStudyMain)."
                + " The census of today's models is read from the"
                + " sources this gate proposes to reconcile; the"
                + " decision itself is docs/decisions/"
                + "working-selection.md. Production behaviour is"
                + " unchanged by this gate.");
        System.out.println();
        System.out.println("## Today: two truths");
        System.out.println();
        System.out.println("- **SelectionModel** (chart package,"
                + " Sprint 19) holds one answer: nothing, empty sky,"
                + " or one object with the click's candidate list."
                + " Consumers: the Inspector (facts and the ambiguity"
                + " chooser), the application wiring that hands the"
                + " selected identity to the chart, and the chart's"
                + " single selection ring through it.");
        System.out.println("- **WorkingMarksModel** (page package,"
                + " Sprint 24) holds an ordered set of identities"
                + " with one lead, delivered whole and reentrantly."
                + " Consumers: the On-this-page table (rows follow"
                + " marks, marks follow rows), the module's cross"
                + " contributions (only for marks the page does not"
                + " draw), and Center here. **Pruned to the page on"
                + " every page change.**");
        System.out.println("- The chart's cross painter takes its"
                + " lead treatment from the *SelectionModel's*"
                + " identity, while Center here takes its lead from"
                + " the *marks model* - two leads that can disagree,"
                + " photographed disagreeing in"
                + " `today-two-leads.png`.");
        System.out.println();
        System.out.println("## Decided: one model, summarised");
        System.out.println();
        System.out.println("The full semantics table, the migration"
                + " of both models, the Accumulate control, the"
                + " Inspector surface and the mutation checks are the"
                + " decision document's; the shape is: the working"
                + " selection - membership, order, lead, whole-state"
                + " reentrant delivery - is one session-level model"
                + " grown from WorkingMarksModel's reviewed"
                + " semantics, promoted out of page scope and never"
                + " pruned by navigation; SelectionModel remains the"
                + " answering model for the last question (facts,"
                + " empty sky, candidates) with its identity driven"
                + " by the one lead; the chart draws the existing"
                + " ring once per drawn member and the existing cross"
                + " once per on-page undrawn member, nothing twice,"
                + " and nothing for off-page members, previewed with"
                + " unchanged painters in `decided-members.png`.");
    }
}
