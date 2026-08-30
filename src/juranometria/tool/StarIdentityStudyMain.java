package juranometria.tool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.DeepSkyObject;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

/**
 * The Sprint 13 star-identity study (issue #112): joins the pinned
 * d3-celestial star names (keyed by Hipparcos number) to the bundled
 * bright-sky pack through the raw Tycho-2 HIP cross-reference (main
 * files and supplement), prints the join measurement report with
 * every exception category, and renders candidate label policies over
 * real pages with a prototype label pass - deterministic,
 * brightness-ordered, collision-rejecting. Run via
 * "make star-identity-study"; charts land in build/star-identity-study/.
 */
public final class StarIdentityStudyMain {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Color NAME_INK = new Color(34, 34, 34);

    record Identity(String tyc, double vmag, String name, String bayer,
                    String flamsteed, String constellation) {
    }

    /** Per-field magnitude limits: proper names, Bayer, Flamsteed. */
    record Policy(String label, double name36, double name18, double name8,
                  double bayer36, double bayer18, double bayer8,
                  double flamsteed36, double flamsteed8) {
        double nameLimit(double field) {
            return field >= 24.0 ? name36 : field >= 12.0 ? name18 : name8;
        }
        double bayerLimit(double field) {
            return field >= 24.0 ? bayer36
                    : field >= 12.0 ? bayer18 : bayer8;
        }
        double flamsteedLimit(double field) {
            return field >= 12.0 ? flamsteed36 : flamsteed8;
        }
    }

    static final Policy CHOSEN = new Policy("chosen", 2.0, 3.0, 4.5,
            Double.NEGATIVE_INFINITY, 3.0, 4.5,
            Double.NEGATIVE_INFINITY, 5.0);
    static final Policy EVERYTHING = new Policy("everything",
            9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0);

    public static void main(String[] args) throws Exception {
        File outDir = new File("build/star-identity-study");
        outDir.mkdirs();

        verifyPinnedInputs();
        Map<String, Identity> identities = joinIdentities();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        record Page(String name, double ra, double dec, double field) {
        }
        List<Page> pages = List.of(
                new Page("orion-08", 83.818667, -5.389667, 8.0),
                new Page("orion-18", 83.818667, -1.0, 18.0),
                new Page("orion-36", 83.818667, 0.0, 36.0),
                new Page("polaris-08", 37.946619, 89.264135, 8.0),
                new Page("polaris-36", 37.946619, 89.264135, 36.0),
                new Page("pleiades-08", 56.869167, 24.105278, 8.0),
                new Page("crux-18", 186.649563, -63.099093, 18.0),
                new Page("m31-08", 10.684708, 41.268750, 8.0));

        System.out.printf(Locale.ROOT, "%-12s %6s | %6s %6s %6s | %8s%n",
                "page", "field", "named", "greek", "flams", "rejected");
        for (Page page : pages) {
            study(renderer, identities, page.name(), new SkyPosition(
                    page.ra(), page.dec()), page.field(), CHOSEN, outDir);
        }
        // The bad alternative, rendered for the decision: everything
        // labelled at 36 degrees.
        study(renderer, identities, "orion-36-everything",
                new SkyPosition(83.818667, 0.0), 36.0, EVERYTHING, outDir);
        System.out.println("Charts written to " + outDir);
    }

    private static void study(ChartRenderer renderer,
                              Map<String, Identity> identities, String name,
                              SkyPosition centre, double field, Policy policy,
                              File outDir) throws Exception {
        ChartViewState state = new ChartViewState(centre, field, 8.0, null, null);
        ChartScene scene = Atlas.assembler().assemble(state, WIDTH, HEIGHT);
        // The proposed production order, honoured by the prototype: the
        // base page renders WITHOUT deep-sky labels (via the existing
        // chart option), star labels go on next - yielding to the
        // deep-sky label boxes and the title block, which are seeded
        // into the collision set - and the deep-sky labels are then
        // drawn above the star labels, matching the decided layer
        // order: stars < star labels < DSO labels < title block.
        var image = renderer.renderToImage(scene, new ChartOptions(
                true, false, true, true, true));
        GnomonicProjection projection = new GnomonicProjection(centre);
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        StarSizePolicy sizes = StarSizePolicy.DEFAULT;

        // Deterministic label pass: brightest first with a stable TYC
        // tie-break for equal magnitudes, collision-rejecting.
        List<Star> stars = new ArrayList<>(scene.stars());
        stars.sort(java.util.Comparator
                .comparingDouble(Star::magnitude)
                .thenComparing(Star::id));
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(LABEL_FONT);
        g.setColor(NAME_INK);
        List<Rectangle2D> occupied = new ArrayList<>();
        occupied.add(titleBlockRect(g, scene));
        List<Object[]> dsoLabels = dsoLabelBoxes(g, scene, projection, mapping);
        for (Object[] dsoLabel : dsoLabels) {
            occupied.add((Rectangle2D) dsoLabel[1]);
        }
        int named = 0;
        int greek = 0;
        int flams = 0;
        int rejected = 0;
        for (Star star : stars) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            Identity identity = identities.get(star.id());
            if (identity == null) {
                continue;
            }
            String text = null;
            int kind = 0;
            if (identity.name() != null
                    && star.magnitude() <= policy.nameLimit(field)) {
                text = identity.name();
                kind = 1;
            } else if (identity.bayer() != null
                    && star.magnitude() <= policy.bayerLimit(field)) {
                text = identity.bayer();
                kind = 2;
            } else if (identity.flamsteed() != null
                    && star.magnitude() <= policy.flamsteedLimit(field)) {
                text = identity.flamsteed();
                kind = 3;
            }
            if (text == null) {
                continue;
            }
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            if (pixel.x() < 0 || pixel.x() >= WIDTH
                    || pixel.y() < 0 || pixel.y() >= HEIGHT) {
                continue;
            }
            double radius = sizes.radiusFor(star.magnitude());
            var metrics = g.getFontMetrics();
            double x = pixel.x() + radius + 3.0;
            double y = pixel.y() + metrics.getAscent() / 2.0 - 1.0;
            Rectangle2D box = new Rectangle2D.Double(x - 2,
                    y - metrics.getAscent(), metrics.stringWidth(text) + 4,
                    metrics.getHeight());
            boolean collides = false;
            for (Rectangle2D other : occupied) {
                if (other.intersects(box)) {
                    collides = true;
                    break;
                }
            }
            if (collides) {
                rejected++;
                continue;
            }
            occupied.add(box);
            g.drawString(text, (float) x, (float) y);
            if (kind == 1) {
                named++;
            } else if (kind == 2) {
                greek++;
            } else {
                flams++;
            }
        }
        // Deep-sky labels above the star labels, per the decided order.
        for (Object[] dsoLabel : dsoLabels) {
            Rectangle2D box = (Rectangle2D) dsoLabel[1];
            g.drawString((String) dsoLabel[0], (float) (box.getX() + 2),
                    (float) (box.getY() + g.getFontMetrics().getAscent()));
        }
        g.dispose();
        System.out.printf(Locale.ROOT, "%-12s %5.0f° | %6d %6d %6d | %8d%n",
                name, field, named, greek, flams, rejected);
        ImageIO.write(image, "png", new File(outDir, name + ".png"));
    }

    /** The renderer's title-block rectangle, replicated for seeding. */
    private static Rectangle2D titleBlockRect(Graphics2D g, ChartScene scene) {
        var metrics = g.getFontMetrics(LABEL_FONT);
        // The block's height is three lines plus padding; its width is
        // bounded by the longest line - a page-width overestimate of the
        // left corner region is a safe, simple seed for the study.
        int height = 3 * metrics.getHeight() + 16 + 24;
        return new Rectangle2D.Double(0, HEIGHT - height, 320, height);
    }

    /** The deep-sky labels the released policy draws, with their boxes. */
    private static List<Object[]> dsoLabelBoxes(Graphics2D g, ChartScene scene,
                                                GnomonicProjection projection,
                                                ViewportMapping mapping) {
        var policy = new RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        var metrics = g.getFontMetrics(LABEL_FONT);
        List<Object[]> labels = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!policy.labelled(dso)) {
                continue;
            }
            var plane = projection.project(dso.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            String text = dso.aliases().stream()
                    .filter(alias -> alias.startsWith("M "))
                    .findFirst().orElse(dso.id());
            double majorPx = Math.max(6.0, Math.toRadians(
                    dso.majorAxisArcmin() / 60.0) * mapping.pixelsPerPlaneUnit());
            double x = pixel.x() + majorPx / 2.0 + 5.0;
            double y = pixel.y() - metrics.getAscent() / 2.0;
            labels.add(new Object[] {text, new Rectangle2D.Double(x - 2, y,
                    metrics.stringWidth(text) + 4, metrics.getHeight())});
        }
        return labels;
    }

    /** Every raw input this study measures from must verify first. */
    static void verifyPinnedInputs() throws Exception {
        String starnames = PinnedInputs.sha256Hex(Files.readAllBytes(
                Path.of("imports/raw/star-identities/starnames.json")));
        String expected =
                "19c84bc885f8a97c3b8e1f6a380084c575a9758dedfe35256e911a823ec3a695";
        if (!expected.equals(starnames)) {
            throw new IllegalStateException(
                    "starnames.json fails its pinned SHA-256: " + starnames);
        }
        for (Map.Entry<String, String> pinned
                : PinnedInputs.SHA256.entrySet()) {
            if (!pinned.getKey().startsWith("tyc2.dat.")
                    && !pinned.getKey().startsWith("suppl_1")) {
                continue;
            }
            Path file = Path.of("imports/raw", pinned.getKey());
            String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
            if (!pinned.getValue().equals(actual)) {
                throw new IllegalStateException(pinned.getKey()
                        + " fails its pinned SHA-256: " + actual);
            }
        }
        System.out.println("pinned inputs verified: starnames.json and all"
                + " Tycho-2 raw files (SHA-256)");
    }

    /** The reproducible join: names by HIP through raw Tycho-2. */
    static Map<String, Identity> joinIdentities() throws Exception {
        Map<String, List<String>> hipToTyc = new HashMap<>();
        for (Path path : Files.list(Path.of("imports/raw")).sorted().toList()) {
            String file = path.getFileName().toString();
            if (file.startsWith("tyc2.dat.")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new GZIPInputStream(
                                Files.newInputStream(path)),
                                StandardCharsets.ISO_8859_1))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String hip = line.substring(142, 148).trim();
                        if (!hip.isEmpty()) {
                            hipToTyc.computeIfAbsent(hip,
                                    key -> new ArrayList<>()).add(tycOf(line));
                        }
                    }
                }
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(
                        Path.of("imports/raw/suppl_1.dat.gz"))),
                StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");
                String hipField = fields[fields.length - 1].trim();
                String digits = hipField.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    hipToTyc.computeIfAbsent(String.valueOf(
                                    Integer.parseInt(digits)),
                            key -> new ArrayList<>()).add(tycOf(line));
                }
            }
        }

        Map<String, Double> pack = new HashMap<>();
        for (Path tile : Files.walk(Path.of(
                "src/resources/catalog/bright-sky/tiles")).toList()) {
            if (!tile.getFileName().toString().equals("stars.csv")) {
                continue;
            }
            for (String line : Files.readAllLines(tile)) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                pack.put(parts[0], Double.parseDouble(parts[3]));
            }
        }

        Map<String, Object> names = MiniJson.object(MiniJson.parse(
                Files.readString(Path.of(
                        "imports/raw/star-identities/starnames.json"))));
        Map<String, Identity> byTyc = new HashMap<>();
        int joined = 0;
        int unmatched = 0;
        int multi = 0;
        for (Map.Entry<String, Object> entry : names.entrySet()) {
            Map<String, Object> value = MiniJson.object(entry.getValue());
            List<String> tycs = new ArrayList<>();
            for (String tyc : hipToTyc.getOrDefault(entry.getKey(), List.of())) {
                if (pack.containsKey(tyc)) {
                    tycs.add(tyc);
                }
            }
            if (tycs.isEmpty()) {
                unmatched++;
                continue;
            }
            joined++;
            // The exception policy: the identity attaches to the
            // brightest packed component, never duplicated.
            tycs.sort(java.util.Comparator.comparingDouble(pack::get));
            if (tycs.size() > 1) {
                multi++;
            }
            String tyc = tycs.get(0);
            byTyc.put(tyc, new Identity(tyc, pack.get(tyc),
                    blankToNull(value.get("name")),
                    blankToNull(value.get("bayer")),
                    blankToNull(value.get("flam")),
                    blankToNull(value.get("c"))));
        }
        System.out.printf(Locale.ROOT,
                "join: %d/%d identities matched to the pack (%d unmatched -"
                        + " fainter than V 8 or outside Tycho-2; %d"
                        + " multi-component systems attached to their"
                        + " brightest packed component)%n%n",
                joined, names.size(), unmatched, multi);
        return byTyc;
    }

    private static String blankToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static String tycOf(String line) {
        String[] parts = line.substring(0, 12).trim().split("\\s+");
        return "TYC " + Integer.parseInt(parts[0]) + "-"
                + Integer.parseInt(parts[1]) + "-" + Integer.parseInt(parts[2]);
    }
}
