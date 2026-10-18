package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;
import juranometria.search.SearchResult;
import juranometria.ui.ChartComponent;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * All star identities where they fit, as production draws it (Sprint 51,
 * issue #558, built to the rulings on #557 -
 * docs/decisions/star-identity-density.md).
 *
 * <p>The pages the study accepted - Aries, Orion, Cygnus and Eridanus at 8°,
 * Orion at 24° and 60° - each drawn twice by the production
 * {@code ChartComponent}: with the released default options, and with the
 * one switch on. Nothing here places a label; the chart does. The report
 * records the pages; how many labels each draws depends on this desktop's
 * fonts and stands in {@code platform.md}.
 */
public final class AllStarIdentitiesMain {

    private static final File DIR = new File("docs/studies/all-star-identities");
    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    private record Page(String name, String star, double field, String caption) {
    }

    private AllStarIdentitiesMain() {
    }

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "All star identities where they fit: what this desktop's fonts place",
                "Sprint 51, issue #558.");
        report.append("# All star identities where they fit, as built\n\n")
                .append("Sprint 51, issue #558, to the rulings on #557"
                        + " (`docs/decisions/star-identity-density.md`). Each page is the"
                        + " production `ChartComponent` at 900 × 700 px, limit V "
                        + fmt(ChartViewState.DEFAULT.limitingMagnitude())
                        + ", drawn with the released default options and again with *All"
                        + " star identities where they fit* on. The released page's"
                        + " labels keep their places; each star they name is written in"
                        + " full; further identities take only free room. How many labels"
                        + " each page draws is in `platform.md`. Regenerate with"
                        + " `make all-star-identities`.\n\n");
        platform.append("## Labels drawn\n\n")
                .append("| page | star labels, released | star labels, all identities |"
                        + " deep-sky labels, both | constellation names, both |\n")
                .append("|---|---:|---:|---:|---:|\n");
        List<Page> pages = List.of(
                new Page("aries-8", "Mesarthim", 8.0, "Aries at 8°, centred on Mesarthim -"
                        + " the issue's case: `γ¹` becomes *Mesarthim γ¹*."),
                new Page("orion-8", "Alnilam", 8.0, "Orion's belt at 8°."),
                new Page("cygnus-8", "Sadr", 8.0, "Cygnus at 8°, in the Milky Way."),
                new Page("eridanus-8", "Acamar", 8.0, "Eridanus at 8°, sparse."),
                new Page("orion-24", "Alnilam", 24.0, "Orion at 24°: the M 42 labels stay"
                        + " legible - nothing is forced over another."),
                new Page("orion-60", "Alnilam", 60.0, "Orion at 60°: names and letters"
                        + " where they fit, and no bare Flamsteed numbers above 12°."));
        for (Page page : pages) {
            report.append("## ").append(page.name()).append("\n\n")
                    .append(page.caption()).append("\n\n");
            int[] stars = new int[2];
            int[] deepSky = new int[2];
            int[] names = new int[2];
            for (boolean on : new boolean[] {false, true}) {
                ChartOptions options = ChartOptions.DEFAULTS.withAllStarIdentities(on);
                String file = page.name() + (on ? "-all" : "-released") + ".png";
                ChartComponent[] chart = new ChartComponent[1];
                BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
                SwingUtilities.invokeAndWait(() -> {
                    chart[0] = new ChartComponent(Atlas.assembler(), PageText.in(EN));
                    // The letterbox band, stated: the contract runs mains in
                    // one JVM, and the look and feel's grey is whichever ran last.
                    chart[0].setBackground(new java.awt.Color(0xEE, 0xEE, 0xEE));
                    chart[0].setSize(900, 700);
                    chart[0].setChartOptions(options);
                    chart[0].setViewState(new ChartViewState(find(page.star()), page.field(),
                            ChartViewState.DEFAULT.limitingMagnitude()));
                    Graphics2D g = image.createGraphics();
                    try {
                        chart[0].paint(g);
                    } finally {
                        g.dispose();
                    }
                });
                ImageIO.write(image, "png", new File(DIR, file));
                report.append(on ? "With all star identities:\n\n" : "Released:\n\n")
                        .append("![](").append(file).append(")\n\n");
                ChartRenderer renderer = new ChartRenderer(
                        juranometria.chart.StarSizePolicy.DEFAULT, PageText.in(EN));
                int i = on ? 1 : 0;
                for (LabelPlacement.Placement placed : renderer.textPlacements(
                        ChartRenderer.TextMetrics.offscreen(), chart[0].currentScene(),
                        chart[0].drawnOptions())) {
                    if (placed.at() == null) {
                        continue;
                    }
                    switch (placed.request().family()) {
                        case STAR, TARGET -> stars[i]++;
                        case DEEP_SKY -> deepSky[i]++;
                        case CONSTELLATION -> names[i]++;
                    }
                }
            }
            platform.append("| ").append(page.name()).append(" | ").append(stars[0])
                    .append(" | ").append(stars[1]).append(" | ")
                    .append(deepSky[0] == deepSky[1] ? Integer.toString(deepSky[0])
                            : deepSky[0] + " / " + deepSky[1])
                    .append(" | ").append(names[0] == names[1] ? Integer.toString(names[0])
                            : names[0] + " / " + names[1]).append(" |\n");
        }
        platform.append("\n");
        System.out.print(PlatformEvidence.portable(report.toString()));
        System.out.flush();
        PlatformEvidence.write(platform, new File(DIR, "platform.md").toString());
    }

    private static SkyPosition find(String name) {
        for (SearchResult result : Atlas.search().search(name)) {
            if (result.kind() == SearchResult.Kind.STAR) {
                return result.position();
            }
        }
        throw new IllegalStateException("the bundled catalogue has no star named " + name);
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
    }
}
