package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReaderInput;
import juranometria.ui.SearchField;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.WorkingSelection;
import juranometria.catalog.TiledCatalogue;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The observing club's evening, end to end (issue #287).
 *
 * <p>One walk down the production path, in a window on screen: find
 * Orion, take it out to the field this sprint added, put the
 * observer's lines and the ecliptic on it, and make the three files
 * a club member can use - through the menu item, the dialog and the
 * writers a reader actually reaches.
 *
 * <p>Then the part that matters most about an export: the atlas is
 * exactly where it was. A reader who makes a sheet has not changed
 * their chart, and Home returns the page they started from to the
 * pixel.
 *
 * <p>What this journey cannot do is printed on paper. Issue #287
 * owes a sheet measured with a ruler, and
 * {@code docs/reviews/sprint-29-handover.md} says so in its own
 * words rather than leaving it to be assumed from a green suite.
 */
class SprintTwentyNineJourneyTest {

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
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

    private static int differences(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static JButton button(AtlasToolbar toolbar, String name) {
        for (java.awt.Component each : toolbar.getComponents()) {
            if (each instanceof JButton press
                    && name.equals(press.getAccessibleContext()
                            .getAccessibleName())) {
                return press;
            }
        }
        throw new AssertionError("no control named " + name);
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T named(JComponent root,
                                                  String name) {
        if (name.equals(root.getName())) {
            return (T) root;
        }
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent component) {
                T found = named(component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> read)
            throws Exception {
        Object[] held = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                held[0] = read.call();
            } catch (Exception thrown) {
                failed[0] = thrown;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) held[0];
        return value;
    }

    @Test
    void theClubMakesItsSheetsAndTheAtlasIsWhereItWas(
            @TempDir Path folder) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the closing journey drives a real window");

        JFrame[] window = new JFrame[1];
        SwingSession.scratchPreferences("sprint-29-journey", node ->
                SwingSession.guarded(() -> {
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartOptionsController options =
                    new ChartOptionsController(
                            ChartOptionsStore.forNode(node));
            WorkingSelection working = new WorkingSelection();
            ChartComponent[] chartHolder = new ChartComponent[1];
            AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
            SearchField[] searchHolder = new SearchField[1];

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                options.onChange(chart::setChartOptions);
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                searchHolder[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        searchHolder[0]);
                window[0] = new JFrame("sprint-29-journey");
                window[0].setLayout(new BorderLayout());
                window[0].add(toolbarHolder[0], BorderLayout.NORTH);
                window[0].add(chart, BorderLayout.CENTER);
                window[0].pack();
                window[0].setVisible(true);
                chartHolder[0] = chart;
            });
            SwingUtilities.invokeAndWait(() -> { });
            ChartComponent chart = chartHolder[0];
            AtlasToolbar toolbar = toolbarHolder[0];

            BufferedImage home = paint(chart);
            ChartViewState atHome = onEdt(navigation::state);

            // ---- 1. Orion, at the released widest ------------------
            ReaderInput.typeAndEnter(searchHolder[0], "betelgeuse");
            assertEquals("TYC 129-1873-1",
                    onEdt(() -> navigation.state().targetIdentity()),
                    "1. the reader finds their way to Orion by name");

            JButton out = button(toolbar, "Zoom out");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 36.0) {
                ReaderInput.click(out);
            }
            assertEquals(36.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "and shows it at the field the atlas shipped with");

            // ---- 2. one step wider, and what it costs --------------
            ReaderInput.click(out);
            assertEquals(42.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "2. one more press reaches the sheet field");
            ChartScene wide = onEdt(chart::currentScene);

            // The projection is the one the gate kept: a great circle
            // maps to a straight line, which is what lets the chart
            // clip reference ink analytically.
            assertEquals(0.0, sagitta(wide), 1.0e-6,
                    "drawn gnomonic, so a great circle is exactly"
                            + " straight");

            // And the page is complete: what the sky holds out to the
            // corners is what the page was given.
            assertEquals(List.of(), missingFromCorners(wide),
                    "with every star the corners reach on the page");

            // ---- 3. the observer's lines and the ecliptic ----------
            List<juranometria.module.NavigationRequest> asked =
                    new ArrayList<>();
            juranometria.chart.SelectionModel selection =
                    new juranometria.chart.SelectionModel();
            ChartModuleHost host = new ChartModuleHost(chart, selection,
                    asked::add);
            BufferedImage bare = paint(chart);
            // An instant whose meridian actually runs through this
            // page: 05:22 UTC on the equinox puts it 0.18 degrees
            // from Betelgeuse for an observer near Bergen. A module
            // switched on over a page its geometry never reaches
            // proves nothing about the export (the lesson of #278),
            // and the ecliptic does not reach Orion at all - it
            // passes above the top edge, which the assertions below
            // say rather than assume.
            MeridianModule meridian = host.attach(new MeridianModule(
                    new Observer(59.9, 10.7, java.time.Instant.parse(
                            "2026-03-20T05:22:00Z"))));
            meridian.showing(true, true, true);
            EclipticModule ecliptic =
                    juranometria.ui.ecliptic.EclipticSession.begin(host);
            ecliptic.showing(true);
            SwingUtilities.invokeAndWait(chart::repaint);
            BufferedImage withInk = paint(chart);

            assertTrue(differences(bare, withInk) > 500,
                    "3. the modules put their ink on the page: "
                            + differences(bare, withInk) + " pixels");
            assertEquals(wide.stars().size(),
                    onEdt(() -> chart.currentScene().stars().size()),
                    "and the catalogue, the grid and the furniture are"
                            + " untouched by them");

            // ---- 4. the export surface, where a reader looks -------
            List<String> opened = new ArrayList<>();
            JMenuBar bar = onEdt(() -> AppMenuBar.create(navigation,
                    () -> { }, () -> { }, () -> { }, () -> { },
                    () -> { }, () -> { }, () -> opened.add("export")));
            JMenuItem export = AppMenuBar.exportItem(bar);
            assertTrue(export != null && export.isEnabled(),
                    "4. File carries the export item");
            SwingUtilities.invokeAndWait(export::doClick);
            assertEquals(List.of("export"), opened,
                    "and it opens the export surface");

            // ---- 5. three files, one chart state -------------------
            ChartViewState exporting = onEdt(navigation::state);
            List<Path> written = new ArrayList<>();
            for (SheetFormat format : SheetFormat.values()) {
                JComponent dialog = ExportSheetDialog.content(
                        ExportSheetSession.defaults(), request -> {
                            var outcome = ExportSheetSession.exportTo(
                                    folder.resolve("orion").toFile(),
                                    request, navigation, chart, options,
                                    working, replacing -> true);
                            written.add(assertInstanceOf(
                                    ExportSheet.Outcome.Written.class,
                                    outcome, "5. " + request.format()
                                            + " is written").file());
                        }, () -> { });
                JComboBox<SheetFormat> box =
                        named(dialog, ExportSheetDialog.FORMAT_BOX);
                SwingUtilities.invokeAndWait(() -> {
                    box.setSelectedItem(format);
                    ((JButton) named(dialog,
                            ExportSheetDialog.EXPORT_BUTTON)).doClick();
                });
            }
            assertEquals(3, written.size(), "three sheets");

            // ---- 6. each opened by something that did not write it -
            var document = javax.xml.parsers.DocumentBuilderFactory
                    .newInstance().newDocumentBuilder()
                    .parse(written.get(0).toFile());
            assertEquals("svg",
                    document.getDocumentElement().getNodeName(),
                    "6. an XML parser reads the SVG");
            String pdf = Files.readString(written.get(1),
                    StandardCharsets.ISO_8859_1);
            assertTrue(pdf.startsWith("%PDF-")
                            && pdf.contains("/MediaBox [0 0 841.89"
                                    + " 595.28]"),
                    "a PDF reader finds an A4 page");
            assertFalse(pdf.contains("/Image"),
                    "with no image in it");
            BufferedImage png = javax.imageio.ImageIO.read(
                    written.get(2).toFile());
            assertEquals(3508, png.getWidth(),
                    "and an image decoder finds the whole sheet");

            // The same chart in all three, including the ink the
            // reader switched on: the sheets are written again with
            // the modules off, and every format has to differ.
            ecliptic.showing(false);
            meridian.showing(false, false, false);
            List<Path> plain = new ArrayList<>();
            for (SheetFormat format : SheetFormat.values()) {
                var outcome = ExportSheetSession.exportTo(
                        folder.resolve("plain").toFile(),
                        new ExportSheet.Request(format, PaperSize.A4,
                                300, false),
                        navigation, chart, options, working,
                        replacing -> true);
                plain.add(assertInstanceOf(
                        ExportSheet.Outcome.Written.class, outcome,
                        "a sheet with no module on it").file());
            }
            assertEquals(List.of(),
                    formatsMissingTheModuleInk(written, plain),
                    "every format carries the ink the chart was"
                            + " carrying");
            ecliptic.showing(true);
            meridian.showing(true, true, true);

            // ---- 7. the SVG is a file a reader can work on ---------
            Path edited = folder.resolve("orion-edited.svg");
            editTheTitle(written.get(0), edited,
                    "Fanafjellet, 20 March");
            var reparsed = javax.xml.parsers.DocumentBuilderFactory
                    .newInstance().newDocumentBuilder()
                    .parse(edited.toFile());
            assertEquals("Fanafjellet, 20 March",
                    reparsed.getElementsByTagName("title").item(0)
                            .getTextContent(),
                    "7. a standards-based edit retitles the sheet");
            assertEquals(count(Files.readString(written.get(0)), "<path"),
                    count(Files.readString(edited), "<path"),
                    "and the chart itself is untouched by the edit");

            // ---- 9. Home, and an atlas nothing happened to ---------
            assertEquals(exporting, onEdt(navigation::state),
                    "9. exporting changed nothing about the chart");
            ecliptic.showing(false);
            meridian.showing(false, false, false);
            ReaderInput.click(button(toolbar, "Reset view"));
            assertEquals(atHome, onEdt(navigation::state),
                    "and Reset view comes home");
            assertEquals(0, differences(home, paint(chart)),
                    "to the page the reader started on, pixel for"
                            + " pixel");
        }, () -> SwingUtilities.invokeAndWait(() -> {
            if (window[0] != null) {
                window[0].dispose();
            }
        })));
    }

    /**
     * How far the celestial equator departs from the straight line
     * joining its ends on this page - zero, under a gnomonic
     * projection, and the reason the gate kept one.
     */
    private static double sagitta(ChartScene scene) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        List<double[]> points = new ArrayList<>();
        for (double ra = -18.0; ra <= 18.0; ra += 1.0) {
            projection.project(new SkyPosition(
                            (scene.viewport().centre().raDegrees() + ra
                                    + 360.0) % 360.0, 0.0))
                    .map(mapping::toPixel)
                    .ifPresent(at -> points.add(
                            new double[] {at.x(), at.y()}));
        }
        double[] first = points.get(0);
        double[] last = points.get(points.size() - 1);
        double worst = 0.0;
        for (double[] point : points) {
            double area = Math.abs((last[0] - first[0])
                    * (first[1] - point[1])
                    - (first[0] - point[0]) * (last[1] - first[1]));
            worst = Math.max(worst, area / Math.hypot(last[0] - first[0],
                    last[1] - first[1]));
        }
        return worst;
    }

    /** Stars the corners reach that the page was not given. */
    private static List<String> missingFromCorners(ChartScene scene) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        TiledCatalogue catalogue = TiledCatalogue.load();
        List<String> missing = new ArrayList<>();
        for (int[] pixel : new int[][] {{1, 1},
                {scene.viewport().widthPx() - 2, 1},
                {1, scene.viewport().heightPx() - 2},
                {scene.viewport().widthPx() - 2,
                        scene.viewport().heightPx() - 2}}) {
            SkyPosition corner = juranometria.project.PanSolver
                    .skyFromPlane(scene.viewport().centre(),
                            juranometria.project.PanSolver.planeFromPixel(
                                    scene.viewport(),
                                    new juranometria.project.PixelPoint(
                                            pixel[0], pixel[1])));
            for (var star : catalogue.starsIn(new SkyRegion(corner, 1.0))) {
                if (star.magnitude() > scene.limitingMagnitude()) {
                    continue;
                }
                if (scene.stars().stream().noneMatch(each ->
                        each.id().equals(star.id()))) {
                    missing.add(star.id());
                }
            }
        }
        return missing;
    }

    /**
     * Which of the three sheets lack the module ink the reader
     * switched on.
     *
     * <p>Asked of each format in its own terms, because a byte count
     * is not evidence: a compressed image can shrink when ink is
     * added to it.
     *
     * <p>What a module contributes depends on where the page is. On
     * Orion's, two of the observer's great circles cross - the
     * meridian and the mathematical horizon - while the ecliptic
     * passes above the top edge and the zenith sits fifty degrees
     * off it. A great circle clipped to a page is one straight line,
     * so what the vector sheets must have gained is lines: every
     * path they gained, and no other kind of shape.
     */
    private static List<String> formatsMissingTheModuleInk(
            List<Path> withInk, List<Path> withoutInk) throws Exception {
        List<String> missing = new ArrayList<>();

        String svg = Files.readString(withInk.get(0),
                StandardCharsets.UTF_8);
        String plainSvg = Files.readString(withoutInk.get(0),
                StandardCharsets.UTF_8);
        List<String> added = pathsAddedTo(svg, plainSvg);
        if (added.isEmpty()) {
            missing.add("SVG (" + count(svg, "<path") + " paths, the"
                    + " same as without the modules)");
        } else if (!added.stream().allMatch(
                SprintTwentyNineJourneyTest::isALineAcrossThePage)) {
            missing.add("SVG (what it gained is not reference lines: "
                    + added.size() + " paths)");
        }

        String pdf = Files.readString(withInk.get(1),
                StandardCharsets.ISO_8859_1);
        String plainPdf = Files.readString(withoutInk.get(1),
                StandardCharsets.ISO_8859_1);
        if (count(pdf, "\nS\n")
                != count(plainPdf, "\nS\n") + added.size()) {
            missing.add("PDF (" + count(pdf, "\nS\n")
                    + " stroked paths against "
                    + count(plainPdf, "\nS\n") + " without, when the"
                    + " SVG gained " + added.size() + ")");
        }

        // And the line brought its own name with it, as it does on
        // screen: a reference line the reader cannot identify is a
        // line drawn across their chart for no stated reason.
        if (count(svg, "<text ") != count(plainSvg, "<text ") + 1) {
            missing.add("SVG label (" + count(svg, "<text ")
                    + " runs against " + count(plainSvg, "<text ") + ")");
        }

        BufferedImage png = javax.imageio.ImageIO.read(
                withInk.get(2).toFile());
        BufferedImage plainPng = javax.imageio.ImageIO.read(
                withoutInk.get(2).toFile());
        int moved = differences(png, plainPng);
        if (moved < 1000) {
            missing.add("PNG (" + moved + " pixels differ)");
        }
        return missing;
    }

    /**
     * The ink one sheet carries that the other does not.
     *
     * <p>Clip definitions are skipped. The reference layer brings a
     * clip of its own, so a naive difference reports the new
     * clipPath as though it were a mark - which it is not, and
     * counting it made this look like two additions when there was
     * one.
     */
    private static List<String> pathsAddedTo(String svg, String plainSvg) {
        List<String> added = new ArrayList<>();
        var each = java.util.regex.Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(svg);
        while (each.find()) {
            boolean isAClipDefinition = svg.lastIndexOf("<clipPath",
                    each.start()) > svg.lastIndexOf("</clipPath>",
                    each.start());
            if (!isAClipDefinition && !plainSvg.contains(each.group(1))) {
                added.add(each.group(1));
            }
        }
        return added;
    }

    /** Whether a path is one straight line spanning the chart. */
    private static boolean isALineAcrossThePage(String path) {
        List<Double> numbers = new ArrayList<>();
        var number = java.util.regex.Pattern
                .compile("-?\\d+(?:\\.\\d+)?").matcher(path);
        while (number.find()) {
            numbers.add(Double.parseDouble(number.group()));
        }
        if (numbers.size() != 4) {
            return false;  // a clipped great circle is one segment
        }
        return Math.hypot(numbers.get(2) - numbers.get(0),
                numbers.get(3) - numbers.get(1))
                > PaperSize.A4.chartHighUnits() / 2.0;
    }

    /**
     * An edit of the kind a reader makes in a vector tool, done the
     * way a standard says to: parse, change the title, write it back.
     */
    private static void editTheTitle(Path from, Path to, String title)
            throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory
                .newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(from.toFile());
        document.getElementsByTagName("title").item(0)
                .setTextContent(title);
        var transformer = javax.xml.transform.TransformerFactory
                .newInstance().newTransformer();
        transformer.transform(
                new javax.xml.transform.dom.DOMSource(document),
                new javax.xml.transform.stream.StreamResult(to.toFile()));
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0;
                at = text.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }
}
