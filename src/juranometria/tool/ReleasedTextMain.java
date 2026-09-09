package juranometria.tool;

import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;

/**
 * Every piece of text on every released page, and where it sits
 * (Sprint 31, issue #314).
 *
 * <p>The released-page digests say <em>that</em> a page changed. This
 * says <em>what</em> changed on it, which is what a reader of the
 * pull request needs: one line per label, so moving a name shows up
 * as one line moving and losing one shows up as one line going.
 *
 * <p>Recorded before the families migrated to the shared placement
 * and again after, on the same machine, so the difference is the
 * decision's and not the font stack's. The boxes are measured text,
 * so this file - like the pixel column of the released-page digests -
 * is a fact about the platform named in its header as well as about
 * this repository.
 */
public final class ReleasedTextMain {

    private ReleasedTextMain() {
    }

    /** The steps the atlas offers, and the pages the study holds. */
    private static final double[] FIELDS =
            {36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    private static final double[][] CENTRES = {
            {10.684708, 41.268750}, {83.0, 0.0},
            {0.0, 89.0}, {359.9, 0.0}};

    private static final int WIDE = 900;
    private static final int HIGH = 700;

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        out.append("# Released page text, listed\n\n");
        out.append("What this file is: every piece of text the atlas"
                + " draws on every released\npage, with the box it"
                + " occupies. The released-page digests say that a page"
                + "\nchanged; this says what changed on it.\n\n");
        out.append("One line per label. A label that moves moves one"
                + " line; a label the page\nstops drawing takes its"
                + " line with it; a label the page starts drawing"
                + " adds\none. The `at` column is which of the label's"
                + " stated candidate positions it\ntook - `0` is where"
                + " the atlas has always drawn it - and `duress` marks"
                + " a\nlabel placed on the least bad of its candidates"
                + " because none was free.\n\n");
        out.append("Boxes are measured text, so this file is a fact"
                + " about the platform below as\nwell as about this"
                + " repository, exactly as the pixel column of"
                + "\n`released-pages.txt` is.\n\n");
        out.append("Recorded on: `" + WiderFieldStudyMain.platform()
                + "`\n\n");
        out.append("Regenerate with `make released-text`.\n\n");
        out.append("## The lines\n\n");
        out.append("field  ra           dec          family        "
                + " at  duress  x        y        w      h     "
                + " what\n");

        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (double field : FIELDS) {
            for (double[] centre : CENTRES) {
                ChartScene scene = Atlas.assembler().assemble(
                        new ChartViewState(
                                new SkyPosition(centre[0], centre[1]),
                                field, 8.0), WIDE, HIGH);
                for (LabelPlacement.Placement placement
                        : renderer.textPlacements(
                                ChartRenderer.TextMetrics.offscreen(),
                                scene, ChartOptions.DEFAULTS)) {
                    out.append(line(field, centre, placement));
                }
            }
        }
        System.out.print(out);
    }

    private static String line(double field, double[] centre,
                               LabelPlacement.Placement placement) {
        LabelPlacement.Request request = placement.request();
        String family = switch (request.family()) {
            case TARGET -> "target";
            case STAR -> "star";
            case DEEP_SKY -> "deep sky";
            case CONSTELLATION -> "constellation";
        };
        if (placement.omitted()) {
            return String.format(Locale.ROOT,
                    "%-6.0f %-12.6f %-12.6f %-14s %-3s %-7s %-8s %-8s"
                            + " %-6s %-6s %s%n",
                    field, centre[0], centre[1], family, "-", "-", "-",
                    "-", "-", "-", "not drawn: " + request.text());
        }
        return String.format(Locale.ROOT,
                "%-6.0f %-12.6f %-12.6f %-14s %-3d %-7s %-8.2f %-8.2f"
                        + " %-6.2f %-6.2f %s%n",
                field, centre[0], centre[1], family, placement.candidate(),
                placement.underDuress() ? "duress" : "-",
                placement.at().getX(), placement.at().getY(),
                placement.at().getWidth(), placement.at().getHeight(),
                request.text());
    }
}
