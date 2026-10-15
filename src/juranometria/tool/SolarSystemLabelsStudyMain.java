package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The Solar System labels as one emphasis class (Sprint 49, issue
 * #526), on production pages: the component with the Solar System or
 * the Jovian module attached, each page drawn canonical and with the
 * class raised, side by side, and the difference counted - every
 * changed pixel inside a drawn name's box, none outside; a refused name
 * refused on both; a below-horizon name quieted on both; the moons'
 * names with Jupiter's. Wide and close fields, both grounds, both
 * languages. Written to {@code docs/studies/solar-system-labels/} by
 * {@code make solar-system-labels-study}: the portable report to
 * stdout, the pages beside it, and the font-dependent counts in
 * {@code platform.md}.
 */
public final class SolarSystemLabelsStudyMain {

    private static final File DIR = new File("docs/studies/solar-system-labels");
    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    /** The packaged emphasis journey's observer (#361): Oslo on the March equinox evening. */
    private static final Observer OSLO_EQUINOX = new Observer(59.91, 10.75,
            Instant.parse("2026-03-20T21:33:00Z"));
    /** The named triple transit (#484). */
    private static final Observer OSLO_TRIPLE = new Observer(59.91, 10.75,
            Instant.parse("2026-12-11T22:45:00Z"));
    /** Espenak's 2012 Venus transit, to the hour (venus-events.txt). */
    private static final Observer OSLO_TRANSIT = new Observer(59.91, 10.75,
            Instant.parse("2012-06-06T01:00:00Z"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private SolarSystemLabelsStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        DIR.mkdirs();
        SolarSystemService solar = SolarSystemService.load();
        JovianSystemService jovian = JovianSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "The Solar System labels raised, as this desktop's fonts place and ink them",
                "Sprint 49, issue #526.");
        q("## Changed pixels on each page");
        q("");
        q("Every changed pixel lies inside a drawn name's box (widened by one pixel for"
                + " antialiasing); a name not listed as placed was refused on its own and"
                + " stays refused with the class raised.");
        q("");
        q("| page | names placed | names refused | changed px inside boxes | outside |");
        q("|---|---|---|---|---|");
        p("# The Solar System labels as one emphasis class");
        p("");
        p("Sprint 49, issue #526. One emphasis choice, *Solar System labels* /"
                + " *Solsystemetiketter*, raises the label of every drawn Solar System body -"
                + " the Sun, the Moon, every drawn planet, the Galilean moons when their"
                + " names are drawn - in the atlas's existing deep red, the meridian's frozen"
                + " accent (#361): `#a42e2e` on white paper, `#e8907e` on the black sky."
                + " Labels only: the discs, marks, phase geometry and state marks keep their"
                + " ink and rules; a name the placement refuses is not forced; a name below"
                + " the drawn horizon is quieted exactly as the text ink is; the"
                + " label-placement order is unchanged. Each page below is the production"
                + " composition drawn canonical (left) and with the class raised (right);"
                + " the counts of changed pixels are this desktop's fonts' and stand in"
                + " `platform.md`. The class joins the set the reader raises - transient,"
                + " never persisted, like the six line structures - and an export carries"
                + " its token `solar-system-labels`. Regenerate with"
                + " `make solar-system-labels-study`.");
        p("");
        SkyPosition sun = ((SolarSystemService.SunObservation) solar.observe(
                SolarSystemService.Body.SUN, OSLO_EQUINOX)).astrometricJ2000();
        SkyPosition jupiter = jovian.observeJupiter(OSLO_TRIPLE).astrometricJ2000();
        SkyPosition transitSun = ((SolarSystemService.SunObservation) solar.observe(
                SolarSystemService.Body.SUN, OSLO_TRANSIT)).astrometricJ2000();
        p("## A wide page: the Sun, the Moon and Venus under the horizon");
        p("");
        p("Oslo, 2026-03-20 21:33 UTC - the packaged emphasis journey's instant - at 120°,"
                + " the meridian module's horizon drawn: the Sun is below it, its name"
                + " carrying the language's status, quieted on both pages.");
        p("");
        page("wide-120-paper", "120°, white paper, English", new ChartViewState(sun, 120.0,
                8.0, null, null), ENGLISH, ChartPalette.WHITE_PAPER, solar, jovian, Kind.SOLAR);
        page("wide-120-black", "120°, black sky, English", new ChartViewState(sun, 120.0,
                8.0, null, null), ENGLISH, ChartPalette.BLACK_SKY, solar, jovian, Kind.SOLAR);
        page("wide-120-paper-nb", "120°, white paper, Norwegian", new ChartViewState(sun,
                120.0, 8.0, null, null), NORSK, ChartPalette.WHITE_PAPER, solar, jovian,
                Kind.SOLAR);
        p("## A close page: Jupiter and its moons at the minimum field");
        p("");
        p("Oslo, 2026-12-11 22:45 UTC, the named triple transit, at 1°: Jupiter's name and"
                + " the drawn moons' take the red together; a moon whose name the placement"
                + " refused keeps no name on either page.");
        p("");
        page("close-1-paper", "1°, white paper, English", new ChartViewState(jupiter,
                ChartViewState.normalMinimumFieldDegrees(), 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, solar, jovian, Kind.JOVIAN);
        page("close-1-black", "1°, black sky, English", new ChartViewState(jupiter,
                ChartViewState.normalMinimumFieldDegrees(), 8.0, null, null), ENGLISH,
                ChartPalette.BLACK_SKY, solar, jovian, Kind.JOVIAN);
        p("## A refused name: Venus in front of the Sun");
        p("");
        p("Oslo, 2012-06-06 01:00 UTC at 2° (#514): every candidate box for Venus's name"
                + " lies inside the Sun's ink, so the name is refused - and the class does"
                + " not force it; only the Sun's name takes the red.");
        p("");
        page("transit-2-paper", "2°, white paper, English", new ChartViewState(transitSun,
                2.0, 8.0, null, null), ENGLISH, ChartPalette.WHITE_PAPER, solar, jovian,
                Kind.TRANSIT);
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    private enum Kind { SOLAR, JOVIAN, TRANSIT }

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, SolarSystemService solar,
                             JovianSystemService jovian, Kind kind) throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartModuleHost[] host = new ChartModuleHost[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            host[0] = new ChartModuleHost(holder[0], new juranometria.chart.SelectionModel(),
                    request -> { });
            switch (kind) {
                case SOLAR -> {
                    MeridianModule meridian = host[0].attach(new MeridianModule(OSLO_EQUINOX));
                    meridian.showing(true, true, false);
                    SolarSystemModule module = new SolarSystemModule(() -> OSLO_EQUINOX,
                            () -> solar, meridian::horizonShowing);
                    module.sunShowing(true);
                    module.moonShowing(true);
                    module.venusShowing(true);
                    holder[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
                }
                case JOVIAN -> {
                    JovianModule module = new JovianModule(() -> OSLO_TRIPLE, () -> jovian,
                            () -> false);
                    module.showing(true);
                    holder[0].overlays().offer(JovianModule.ID, module::contributedGeometry);
                }
                case TRANSIT -> {
                    SolarSystemModule module = new SolarSystemModule(() -> OSLO_TRANSIT,
                            () -> solar, () -> false);
                    module.sunShowing(true);
                    module.venusShowing(true);
                    holder[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
                }
                default -> throw new IllegalStateException(kind.toString());
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        try {
            BufferedImage canonical = paint(chart);
            List<ReferenceInk.BodyPlacement> placed = chart.renderedBodies();
            chart.toggleEmphasis(ChartStructure.SOLAR_SYSTEM_LABELS);
            if (!chart.emphasizedSet().contains(ChartStructure.SOLAR_SYSTEM_LABELS)) {
                throw new IllegalStateException(name + ": the class could not be raised");
            }
            BufferedImage raised = paint(chart);
            List<ReferenceInk.BodyPlacement> again = chart.renderedBodies();
            int inside = 0;
            int outside = 0;
            for (int y = 0; y < canonical.getHeight(); y++) {
                for (int x = 0; x < canonical.getWidth(); x++) {
                    if (canonical.getRGB(x, y) == raised.getRGB(x, y)) {
                        continue;
                    }
                    if (insideANameBox(x, y, placed)) {
                        inside++;
                    } else {
                        outside++;
                    }
                }
            }
            BufferedImage side = new BufferedImage(1800, 700, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = side.createGraphics();
            try {
                g.drawImage(canonical, 0, 0, null);
                g.drawImage(raised, 900, 0, null);
            } finally {
                g.dispose();
            }
            ImageIO.write(side, "png", new File(DIR, name + ".png"));
            List<String> placedNames = new ArrayList<>();
            List<String> refusedNames = new ArrayList<>();
            boolean same = placed.size() == again.size();
            for (int i = 0; i < placed.size() && same; i++) {
                same = placed.get(i).identity().equals(again.get(i).identity())
                        && java.util.Objects.equals(placed.get(i).box(), again.get(i).box());
            }
            for (ReferenceInk.BodyPlacement body : placed) {
                (body.box() == null ? refusedNames : placedNames).add(body.identity()
                        + (body.belowHorizon() ? " (below the horizon)" : ""));
            }
            p("### " + title);
            p("");
            p("![](" + name + ".png)");
            p("");
            p("Canonical left, the class raised right. Placed: " + (placedNames.isEmpty()
                    ? "none" : String.join(", ", placedNames)) + ". Refused: "
                    + (refusedNames.isEmpty() ? "none" : String.join(", ", refusedNames))
                    + ". The placements are " + (same ? "identical" : "DIFFERENT")
                    + " on both pages; every changed pixel lies inside a placed name's box:"
                    + " " + (outside == 0 ? "yes" : "NO, " + outside + " outside") + ".");
            p("");
            q(String.format(Locale.ROOT, "| %s | %d | %d | %d | %d |", name,
                    placedNames.size(), refusedNames.size(), inside, outside));
        } finally {
            SwingUtilities.invokeAndWait(host[0]::detachAll);
        }
    }

    private static boolean insideANameBox(int x, int y, List<ReferenceInk.BodyPlacement> placed) {
        for (ReferenceInk.BodyPlacement body : placed) {
            Rectangle2D box = body.box();
            if (box != null && new Rectangle2D.Double(box.getX() - 1, box.getY() - 1,
                    box.getWidth() + 2, box.getHeight() + 2).contains(x + 0.5, y + 0.5)) {
                return true;
            }
        }
        return false;
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
}
