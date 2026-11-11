package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.chart.SkyPosition;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.tool.MeteorStudyData.Radiant;
import juranometria.tool.MeteorStudyData.Shower;

/**
 * The meteor-shower study's pages (Sprint 60, issue #715): a radiant at
 * a chart time on the application's own ordinary chart and whole sky.
 *
 * <p>Two kinds of ink, kept apart and named on every page. <em>The
 * seam's ink</em> is real: a tool-only module contributes the radiant
 * as an {@link OverlayContribution.Point} through the existing module
 * seam, and the production renderer draws, names, subdues and seams it
 * with no production change. <em>The candidate glyph</em> is a mock
 * the tool paints over a production page at the radiant's projected
 * place, because a radiant mark in page units does not exist in the
 * application; building it is the implementation's work, not the
 * study's.
 *
 * <p>Standard output is {@code docs/studies/meteor-showers/pages.md};
 * the pictures are written beside it. Regenerate with
 * {@code make meteor-shower-pages}.
 */
public final class MeteorShowerPagesMain {

    static final Path OUT = Path.of("docs/studies/meteor-showers");
    static final int W = 1440;
    static final int H = 900;
    static final int WHOLE_W = 2000;
    static final int WHOLE_H = 1000;

    /** Draft names for the pages only; the terminology is a proposal (decision record, R-13). */
    static final Map<String, String[]> NAMES = Map.ofEntries(
            Map.entry("QUA", new String[] {"Quadrantids", "Kvadrantidene"}),
            Map.entry("LYR", new String[] {"Lyrids", "Lyridene"}),
            Map.entry("ETA", new String[] {"Eta Aquariids", "Eta-akvaridene"}),
            Map.entry("SDA", new String[] {"Southern Delta Aquariids", "Sørlige delta-akvarider"}),
            Map.entry("PER", new String[] {"Perseids", "Perseidene"}),
            Map.entry("ORI", new String[] {"Orionids", "Orionidene"}),
            Map.entry("LEO", new String[] {"Leonids", "Leonidene"}),
            Map.entry("GEM", new String[] {"Geminids", "Geminidene"}),
            Map.entry("URS", new String[] {"Ursids", "Ursidene"}));

    /** The tool-only module: radiants as points through the existing seam. */
    static final class RadiantModule implements ChartModule {
        private final List<OverlayContribution> points = new ArrayList<>();
        private Runnable withdraw;

        void show(String code, String name, SkyPosition at) {
            points.add(new OverlayContribution.Point("meteor-shower." + code, name, at,
                    OverlayContribution.Mark.LANDMARK, InkRole.REFERENCE_LINE));
        }

        @Override
        public String name() {
            return "meteor-showers.study";
        }

        @Override
        public void attach(ChartServices services) {
            withdraw = services.contribute(name(), () -> List.copyOf(points));
        }

        @Override
        public void detach() {
            if (withdraw != null) {
                withdraw.run();
            }
        }
    }

    private static PrintStream out;
    private static MeteorStudyData data;
    private static WholeSkyComposition.Services services;

    private MeteorShowerPagesMain() {
    }

    private static void p(String s) {
        out.println(s);
    }

    private static String f(String format, Object... args) {
        return MeteorStudyData.f(format, args);
    }

    public static void main(String[] args) throws Exception {
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        data = MeteorStudyData.load(SolarSystemService.load());
        services = new WholeSkyComposition.Services();
        Files.createDirectories(OUT);

        p("# Meteor showers at chart time: pages (#715)");
        p("");
        p("Sprint 60, issue #715. Every page is the application's own chart with its"
                + " production modules attached, at a stated chart time from Oslo (59.91 N,"
                + " 10.75 E). *Seam ink* is real: the radiant goes through the existing module"
                + " seam as a named point and the production renderer does the rest."
                + " *Candidate glyph* is a mock painted by the study over the production page."
                + " Positions are pinned geometry; whether a name was placed depends on this"
                + " machine's fonts. Regenerate with `make meteor-shower-pages`.");
        p("");
        p("| page | chart time (UTC) | what is drawn | ink | shower | solar longitude | radiant"
                + " J2000 | altitude | on the page at |");
        p("|---|---|---|---|---|---|---|---|---|");

        Instant perseids = Instant.parse("2027-08-13T00:00:00Z");
        Instant geminids = Instant.parse("2027-12-14T20:00:00Z");
        Instant etaAquariids = Instant.parse("2027-05-06T23:00:00Z");

        page("perseids-ordinary-seam-en-white", perseids, List.of("PER"), "PER", 60.0,
                ChartPalette.WHITE_PAPER, "en", false);
        page("perseids-ordinary-glyph-en-white", perseids, List.of("PER"), "PER", 60.0,
                ChartPalette.WHITE_PAPER, "en", true);
        page("perseids-ordinary-glyph-nb-black", perseids, List.of("PER"), "PER", 60.0,
                ChartPalette.BLACK_SKY, "nb-NO", true);
        page("perseids-close-glyph-en-white", perseids, List.of("PER"), "PER", 12.0,
                ChartPalette.WHITE_PAPER, "en", true);
        page("eta-aquariids-below-horizon-seam-en-white", etaAquariids, List.of("ETA"), "ETA",
                60.0, ChartPalette.WHITE_PAPER, "en", false);
        page("geminids-whole-sky-seam-en-white", geminids, List.of("GEM"), null, 0.0,
                ChartPalette.WHITE_PAPER, "en", false);
        page("geminids-whole-sky-glyph-en-white", geminids, List.of("GEM"), null, 0.0,
                ChartPalette.WHITE_PAPER, "en", true);
        List<String> active = new ArrayList<>();
        for (Shower s : data.imo2027.values()) {
            if (data.active(s, geminids)) {
                active.add(s.code());
            }
        }
        page("december-whole-sky-every-active-seam-en-white", geminids, active, null, 0.0,
                ChartPalette.WHITE_PAPER, "en", false);
        p("");
        glyphSheet();
        p("`glyph-sheet.png`: the candidate marks at their true page size, twice enlarged"
                + " beneath, on both grounds, beside the two marks the seam has today (the"
                + " landmark diamond and the place ring).");
        p("");
    }

    private static void page(String name, Instant when, List<String> codes, String centreOn,
                             double field, ChartPalette palette, String language,
                             boolean glyph) throws Exception {
        Observer oslo = new Observer(MeteorShowerStudyMain.OSLO_LAT,
                MeteorShowerStudyMain.OSLO_LON, when);
        double sol = data.solarLongitude(when);
        boolean whole = centreOn == null;
        WholeSkyComposition.Page page = WholeSkyComposition.paint(new WholeSkyComposition.Ask(
                oslo, palette, language, 8.0, true, true, 60.0, whole ? WHOLE_W : W,
                whole ? WHOLE_H : H), services);
        RadiantModule module = new RadiantModule();
        List<String[]> lines = new ArrayList<>();
        BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            for (String code : codes) {
                Radiant r = data.radiantAt(code, sol);
                String[] names = NAMES.getOrDefault(code, new String[] {code, code});
                String label = language.equals("en") ? names[0] : names[1];
                if (!glyph) {
                    module.show(code, label, r.at());
                }
            }
            page.host().attach(module);
            if (!whole) {
                page.session().leave();
                page.controller().recenter(data.radiantAt(centreOn, sol).at(), field);
            }
            int w = whole ? WHOLE_W : W;
            int h = whole ? WHOLE_H : H;
            image[0] = WholeSkyComposition.paintNow(page.chart(), w, h);
            DrawnPage drawn = DrawnPage.of(page.chart().currentScene());
            ViewportMapping mapping = new ViewportMapping(drawn);
            Graphics2D g = image[0].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (String code : codes) {
                Radiant r = data.radiantAt(code, sol);
                PixelPoint at = drawn.projection().project(r.at()).map(mapping::toPixel)
                        .orElse(null);
                double altitude = new LocalSky(oslo).altitudeDegrees(r.at());
                String[] names = NAMES.getOrDefault(code, new String[] {code, code});
                String label = language.equals("en") ? names[0] : names[1];
                if (glyph && at != null) {
                    Color ink = palette == ChartPalette.WHITE_PAPER ? new Color(40, 40, 40)
                            : new Color(225, 225, 225);
                    if (altitude < 0) {
                        ink = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 102);
                    }
                    g.setColor(ink);
                    rays(g, at.x(), at.y(), 1.0, 0);
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    g.drawString(label, (float) at.x() + 14f, (float) at.y() - 8f);
                }
                lines.add(new String[] {code, f("%.2f", sol),
                        f("%.1f %+.1f (%s)", r.at().raDegrees(), r.at().decDegrees(),
                                r.footing().name().toLowerCase(java.util.Locale.ROOT)
                                        .replace('_', ' ')),
                        f("%+.1f", altitude),
                        at == null ? "not on the page" : f("%.0f, %.0f", at.x(), at.y())});
            }
            g.dispose();
        });
        ImageIO.write(image[0], "png", OUT.resolve(name + ".png").toFile());
        String what = whole ? "the whole sky" : f("ordinary chart, %.0f degrees wide", field);
        for (String[] l : lines) {
            p(f("| `%s.png` | %s | %s, %s, %s | %s | %s | %s | %s | %s | %s |", name,
                    MeteorShowerStudyMain.UTC.format(when), what, language,
                    palette == ChartPalette.WHITE_PAPER ? "white paper" : "black sky",
                    glyph ? "candidate glyph (mock)" : "seam ink (real)", l[0], l[1], l[2], l[3],
                    l[4]));
        }
        System.err.println(name + " written");
    }

    /**
     * The candidate marks. Variant 0: six short rays about an empty
     * centre. Variant 1: eight rays, alternately long and short.
     * Variant 2: a small ring with six rays.
     */
    static void rays(Graphics2D g, double x, double y, double scale, int variant) {
        g.setStroke(new BasicStroke((float) (1.2 * scale), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        int n = variant == 1 ? 8 : 6;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(360.0 / n * i - 90.0);
            double inner = (variant == 2 ? 5.0 : 3.5) * scale;
            double outer = (variant == 1 && i % 2 == 1 ? 7.0 : 10.0) * scale;
            g.draw(new Line2D.Double(x + inner * Math.cos(a), y + inner * Math.sin(a),
                    x + outer * Math.cos(a), y + outer * Math.sin(a)));
        }
        if (variant == 2) {
            double r = 3.0 * scale;
            g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        }
    }

    private static void glyphSheet() throws Exception {
        int w = 900;
        int h = 300;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        String[] names = {"A six rays", "B eight rays", "C ring and rays", "landmark today",
            "place today"};
        for (int ground = 0; ground < 2; ground++) {
            int x0 = ground * w / 2;
            g.setColor(ground == 0 ? Color.WHITE : new Color(12, 12, 16));
            g.fillRect(x0, 0, w / 2, h);
            Color ink = ground == 0 ? new Color(40, 40, 40) : new Color(225, 225, 225);
            g.setColor(ink);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            for (int i = 0; i < 5; i++) {
                double x = x0 + 48 + i * 90;
                for (int row = 0; row < 2; row++) {
                    double scale = row == 0 ? 1.0 : 2.0;
                    double y = row == 0 ? 70 : 170;
                    if (i < 3) {
                        rays(g, x, y, scale, i);
                    } else if (i == 3) {
                        g.setStroke(new BasicStroke((float) scale));
                        double r = 5 * scale;
                        java.awt.geom.Path2D d = new java.awt.geom.Path2D.Double();
                        d.moveTo(x, y - r);
                        d.lineTo(x + r, y);
                        d.lineTo(x, y + r);
                        d.lineTo(x - r, y);
                        d.closePath();
                        g.draw(d);
                    } else {
                        g.setStroke(new BasicStroke((float) scale));
                        double r = 5 * scale;
                        g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                        g.draw(new Line2D.Double(x, y - r, x, y - r - 4 * scale));
                    }
                }
                g.drawString(names[i], (float) x - 36f, 250f);
            }
        }
        g.dispose();
        ImageIO.write(image, "png", OUT.resolve("glyph-sheet.png").toFile());
    }
}
