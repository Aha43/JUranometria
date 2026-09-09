package juranometria.app;

import java.awt.image.BufferedImage;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import juranometria.chart.ChartScene;
import juranometria.chart.WorkingSelection;
import juranometria.render.LabelPlacement;
import juranometria.sheet.SheetWriters;
import juranometria.ui.ChartComponent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sky.Observer;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reader exporting, end to end (Sprint 29, issue #286).
 *
 * <p>The route a club member takes: the File menu, the dialog, one
 * choice each of the three formats, and three files that are what
 * they claim. Each is opened again afterwards by something that did
 * not write it - an XML parser, a PDF content-stream reader, an image
 * decoder - and asked what it holds.
 *
 * <p>The chart used is the sprint's own case: the new 42-degree
 * field, with the meridian and the ecliptic showing, because a sheet
 * that only works on the released default is a sheet that has not
 * been tried.
 */
class ExportJourneyTest {

    private static final ChartViewState EQUINOX = new ChartViewState(
            new SkyPosition(0.0, 0.0), 42.0, 6.0);

    /** The observer's lines and the ecliptic, as the screen has them. */
    private static ChartRenderer.ReferenceLayer modules() {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return (g, painted) -> ReferenceInk.paint(g, painted,
                registry.collect(), ChartPalette.WHITE_PAPER);
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

    @Test
    void theReaderExportsAllThreeFormatsFromTheFileMenu(
            @TempDir Path folder) throws Exception {
        // ---- 1. the route exists where a reader looks for it -------
        List<String> asked = new ArrayList<>();
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(EQUINOX.centre(),
                EQUINOX.fieldWidthDegrees());
        JMenuBar bar = AppMenuBar.create(navigation, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { },
                () -> asked.add("export"));

        JMenuItem export = AppMenuBar.exportItem(bar);
        assertTrue(export != null, "1. File carries an export item");
        assertEquals("File", bar.getMenu(0).getText(),
                "in the File menu, where every application keeps"
                        + " 'make me a file'");
        assertEquals("Export Chart Sheet...", export.getText(),
                "named for what it does");
        assertTrue(export.getAccelerator() != null,
                "with the platform's own shortcut");
        export.doClick();
        assertEquals(List.of("export"), asked,
                "and the item runs the export, not something else");

        // ---- 2. the dialog, once, for three formats ----------------
        List<ExportSheet.Request> chosen = new ArrayList<>();
        for (SheetFormat format : SheetFormat.values()) {
            JComponent dialog = ExportSheetDialog.content(
                    ExportSheetSession.defaults(), chosen::add, () -> { });
            JComboBox<SheetFormat> box =
                    named(dialog, ExportSheetDialog.FORMAT_BOX);
            box.setSelectedItem(format);
            ((JButton) named(dialog, ExportSheetDialog.EXPORT_BUTTON))
                    .doClick();
        }
        assertEquals(List.of(SheetFormat.SVG, SheetFormat.PDF,
                        SheetFormat.PNG),
                chosen.stream().map(ExportSheet.Request::format).toList(),
                "2. the reader chooses each format through the real"
                        + " dialog");

        // ---- 3. three files, written -------------------------------
        List<Path> written = new ArrayList<>();
        for (ExportSheet.Request request : chosen) {
            File destination = folder.resolve("equinox").toFile();
            var outcome = assertInstanceOf(
                    ExportSheet.Outcome.Written.class,
                    ExportSheet.write(Atlas.assembler()::assemble, EQUINOX,
                            ChartOptions.DEFAULTS, modules(), request,
                            destination, existing -> true),
                    "3. " + request.format() + " is written");
            written.add(outcome.file());
        }
        assertEquals(3, written.stream().distinct().count(),
                "three formats are three files, not one overwritten");

        // ---- 4. each opened by something that did not write it -----
        String svg = Files.readString(written.get(0), StandardCharsets.UTF_8);
        var document = javax.xml.parsers.DocumentBuilderFactory
                .newInstance().newDocumentBuilder()
                .parse(written.get(0).toFile());
        assertEquals("svg", document.getDocumentElement().getNodeName(),
                "4. an XML parser reads the SVG as an SVG");
        assertTrue(svg.contains("width=\"841.89pt\""),
                "at A4's own size");

        String pdf = Files.readString(written.get(1),
                StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF-1.4"), "the PDF is a PDF");
        assertTrue(pdf.contains("/MediaBox [0 0 841.89 595.28]"),
                "stating A4 as its page");
        assertFalse(pdf.contains("/Image") || pdf.contains("/DCTDecode"),
                "with no image in it at all");
        String stream = pdf.substring(pdf.indexOf("stream\n") + 7,
                pdf.indexOf("endstream"));
        assertTrue(count(stream, " m\n") > 500,
                "and real path geometry: " + count(stream, " m\n")
                        + " subpaths");

        BufferedImage png = ImageIO.read(written.get(2).toFile());
        assertEquals(PngSheetWriter.widePixels(PaperSize.A4, 300),
                png.getWidth(), "the PNG is the whole sheet at 300 dpi");
        assertEquals(PngSheetWriter.highPixels(PaperSize.A4, 300),
                png.getHeight(), "in both directions");

        // ---- 5. and they are the same chart ------------------------
        sameChart(written.get(0), png);
    }

    /**
     * The formats against each other, through the sky: the marks
     * production drew for this sheet are in the SVG at their own
     * place, and the PNG has ink at the same places scaled by its own
     * resolution.
     */
    private void sameChart(Path svgFile, BufferedImage png)
            throws Exception {
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, EQUINOX, ChartOptions.DEFAULTS,
                modules(), PaperSize.A4);
        String svg = Files.readString(svgFile, StandardCharsets.UTF_8);
        List<double[]> inSvg = pathCentres(svg);

        GnomonicProjection projection = new GnomonicProjection(
                sheet.scene().viewport().centre());
        ViewportMapping mapping = new ViewportMapping(
                sheet.scene().viewport());
        double scale = 300 / 72.0;
        double margin = PaperSize.A4.marginPoints();

        int checked = 0;
        List<String> missingFromSvg = new ArrayList<>();
        List<String> blankInPng = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(sheet.scene(), sheet.options())) {
            var at = projection.project(mark.star() != null
                            ? mark.star().position()
                            : mark.deepSky().position())
                    .map(mapping::toPixel);
            if (at.isEmpty() || at.get().x() < 20 || at.get().y() < 20
                    || at.get().x() > PaperSize.A4.chartWideUnits() - 20
                    || at.get().y() > PaperSize.A4.chartHighUnits() - 20) {
                continue;
            }
            checked++;
            double x = at.get().x();
            double y = at.get().y();
            if (inSvg.stream().noneMatch(centre ->
                    Math.hypot(centre[0] - x, centre[1] - y) < 1.0)) {
                missingFromSvg.add("mark at " + x + "," + y);
            }
            // The same place on the raster, which is the sheet
            // coordinate plus the margin, times the resolution.
            int px = (int) Math.round((x + margin) * scale);
            int py = (int) Math.round((y + margin) * scale);
            // Unless production itself painted over it. The title
            // block is an opaque panel in the lower left, and a mark
            // under it is hidden on any raster - in the vector
            // formats the path is still there, beneath the panel,
            // which is the same layering a viewer will apply. So the
            // recording is asked whether anything filled over this
            // mark after it was drawn.
            if (coveredLater(sheet, x, y)) {
                continue;
            }
            // Within the mark's own extent, not a fixed box: a
            // deep-sky symbol is an open outline and its centre is
            // blank paper, so looking only at the middle of one asks
            // the wrong question.
            int reach = (int) Math.ceil(
                    Math.max(mark.reach(), 1.5) * scale) + 4;
            if (!inkNear(png, px, py, reach)) {
                blankInPng.add("blank at " + px + "," + py);
            }
        }
        assertTrue(checked > 50,
                "5. there are marks to compare across the formats: "
                        + checked);
        assertEquals(List.of(), missingFromSvg.stream().limit(3).toList(),
                missingFromSvg.size() + " of " + checked
                        + " marks are absent from the SVG");
        assertEquals(List.of(), blankInPng.stream().limit(3).toList(),
                blankInPng.size() + " of " + checked + " marks have no"
                        + " ink at the matching place in the PNG");
    }

    /**
     * Whether production drew an opaque fill over this point after
     * the mark itself - the title block, which is a panel and not a
     * transparency.
     */
    private static boolean coveredLater(SheetRecording sheet, double x,
                                        double y) {
        boolean seenMark = false;
        for (juranometria.sheet.SheetRecorder.Drawn drawn
                : sheet.recorder().drawn()) {
            java.awt.geom.Rectangle2D box = drawn.shape().getBounds2D();
            boolean isTheMark = Math.hypot(box.getCenterX() - x,
                    box.getCenterY() - y) < 1.0 && box.getWidth() < 20;
            if (isTheMark) {
                seenMark = true;
                continue;
            }
            if (seenMark && drawn.filled() && box.getWidth() > 20
                    && drawn.shape().contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inkNear(BufferedImage image, int x, int y,
                                   int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= image.getWidth()
                        || py >= image.getHeight()) {
                    continue;
                }
                int rgb = image.getRGB(px, py) & 0xffffff;
                int sum = ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                        + (rgb & 0xff);
                if (sum < 3 * 250) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<double[]> pathCentres(String svg) {
        List<double[]> centres = new ArrayList<>();
        Matcher each = Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(svg.substring(svg.indexOf("<g id=\"chart\"")));
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            Matcher number = Pattern.compile("-?\\d+(?:\\.\\d+)?")
                    .matcher(each.group(1));
            while (number.find()) {
                numbers.add(Double.parseDouble(number.group()));
            }
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (int i = 0; i + 1 < numbers.size(); i += 2) {
                minX = Math.min(minX, numbers.get(i));
                maxX = Math.max(maxX, numbers.get(i));
                minY = Math.min(minY, numbers.get(i + 1));
                maxY = Math.max(maxY, numbers.get(i + 1));
            }
            centres.add(new double[] {(minX + maxX) / 2.0,
                    (minY + maxY) / 2.0});
        }
        return centres;
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0;
                at = text.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    /** A crowded sky for the placement journey below. */
    private static final ChartViewState SAGITTARIUS = new ChartViewState(
            new SkyPosition(281.0, -26.0), 120.0,
            ChartViewState.defaultMagnitudeFor(120.0));

    /** Where that page puts its text, decided for the paper's extent. */
    private static List<LabelPlacement.Placement> decision() {
        ChartScene scene = Atlas.assembler().assemble(SAGITTARIUS,
                PaperSize.A4.chartWideUnits(), PaperSize.A4.chartHighUnits());
        return new ChartRenderer(StarSizePolicy.DEFAULT).textPlacements(
                ChartRenderer.TextMetrics.offscreen(), scene,
                ChartOptions.DEFAULTS.withPalette(ChartPalette.WHITE_PAPER));
    }

    @Test
    void theExportedSheetCarriesThePagesOwnPlacement(@TempDir Path folder)
            throws Exception {
        // This one shows the window and presses the real controls, so
        // it needs a display; the format readers it hands the files to
        // are put to the question headlessly in
        // PlacedTextTravelsToTheSheetTest.
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "a reader's export route drives a real window");
        List<LabelPlacement.Placement> drawn = new ArrayList<>();
        List<LabelPlacement.Placement> refused = new ArrayList<>();
        for (LabelPlacement.Placement one : decision()) {
            (one.omitted() ? refused : drawn).add(one);
        }
        assertTrue(drawn.size() > 80, "the page has text to carry: "
                + drawn.size() + " labels");
        assertFalse(refused.isEmpty(), "and text it refused to place,"
                + " which is the half no format can show by drawing"
                + " something");

        Map<SheetFormat, Path> written = new LinkedHashMap<>();
        JFrame window = null;
        java.util.prefs.Preferences node =
                java.util.prefs.Preferences.userRoot().node(
                        "juranometria/test/export-journey");
        try {
            window = exportingAtlas(folder, written, node);
            for (SheetFormat format : SheetFormat.values()) {
                exportThrough(window, format);
            }
        } finally {
            closeEverything(window);
            node.removeNode();
        }
        assertEquals(List.of(SheetFormat.SVG, SheetFormat.PDF,
                        SheetFormat.PNG),
                List.copyOf(written.keySet()),
                "the reader exported each format through the File menu");

        // The bytes the route wrote, and the one recording they are of.
        SheetRecording sheet = ChartSheet.record(Atlas.assembler()::assemble,
                SAGITTARIUS, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        for (SheetFormat format : SheetFormat.values()) {
            assertTrue(java.util.Arrays.equals(
                            Files.readAllBytes(written.get(format)),
                            SheetWriters.write(sheet, format, 300)),
                    "and " + format + " is this page's own recording,"
                            + " byte for byte");
        }

        // Read by the same three readers the oracle file puts to the
        // question, so what passes here is what fails there when a
        // writer stops carrying the decision.
        PlacedTextTravelsToTheSheetTest reader =
                new PlacedTextTravelsToTheSheetTest();
        reader.readTheSvg(Files.readString(written.get(SheetFormat.SVG),
                StandardCharsets.UTF_8), drawn, refused);
        reader.readThePdf(Files.readString(written.get(SheetFormat.PDF),
                StandardCharsets.ISO_8859_1), drawn, refused);
        reader.readThePng(ImageIO.read(written.get(SheetFormat.PNG).toFile()),
                sheet, drawn, refused);
    }

    // ---- the reader's own route --------------------------------------

    /**
     * The atlas, shown, with its real File menu wired to the real
     * export route - and only the platform's save surface replaced,
     * because a file chooser is the operating system's window.
     */
    private JFrame exportingAtlas(Path folder, Map<SheetFormat, Path> written,
                                  java.util.prefs.Preferences node)
            throws Exception {
        JFrame[] made = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController navigation = new ChartViewController(
                    Atlas.assembler()::fits);
            ChartComponent chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            navigation.recenter(SAGITTARIUS.centre(),
                    SAGITTARIUS.fieldWidthDegrees());
            ChartOptionsController options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            WorkingSelection working = new WorkingSelection();

            JFrame frame = new JFrame("Export journey");
            frame.add(chart);
            frame.setSize(1100, 800);
            ExportSheetSession.Surfaces real = ExportSheetSession.onScreen();
            ExportSheetSession.Surfaces surfaces =
                    new ExportSheetSession.Surfaces() {

                        @Override
                        public Optional<ExportSheet.Request> chooseWhat(
                                java.awt.Frame owner,
                                ExportSheet.Request initial) {
                            return real.chooseWhat(owner, initial);
                        }

                        @Override
                        public Optional<File> chooseWhere(
                                java.awt.Frame owner, String suggested) {
                            // The seam, and the only one: choosing a
                            // file is the platform's own window.
                            return Optional.of(
                                    folder.resolve(suggested).toFile());
                        }

                        @Override
                        public ExportSheet.ReplaceDecision replace(
                                java.awt.Frame owner) {
                            return real.replace(owner);
                        }

                        @Override
                        public void report(java.awt.Frame owner,
                                           ExportSheet.Outcome outcome) {
                            if (outcome instanceof ExportSheet.Outcome.Written
                                    done) {
                                written.put(formatOf(done.file()),
                                        done.file());
                            }
                            real.report(owner, outcome);
                        }
                    };
            frame.setJMenuBar(AppMenuBar.create(navigation, () -> { },
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> ExportSheetSession.open(frame, navigation, chart,
                            options, working, surfaces)));
            frame.setVisible(true);
            made[0] = frame;
        });
        flush();
        return made[0];
    }

    /** One export, driven the way a reader drives it. */
    private void exportThrough(JFrame window, SheetFormat format)
            throws Exception {
        JMenuItem export = AppMenuBar.exportItem(window.getJMenuBar());
        assertTrue(export != null, "the File menu carries the export item");
        assertEquals("Export Chart Sheet...", export.getText(),
                "named for what it does");
        SwingUtilities.invokeLater(export::doClick);

        JDialog dialog = awaitDialog("Export chart sheet");
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<?> box = find(dialog, JComboBox.class,
                    ExportSheetDialog.FORMAT_BOX);
            assertTrue(box != null, "the dialog offers the formats");
            box.setSelectedItem(format);
            JButton confirm = find(dialog, JButton.class,
                    ExportSheetDialog.EXPORT_BUTTON);
            assertTrue(confirm != null, "and an Export button");
            confirm.doClick();
        });
        // The route then says what it did, in its own dialog.
        JDialog told = awaitDialog("Chart sheet exported");
        SwingUtilities.invokeAndWait(() -> {
            JButton acknowledge = firstButton(told);
            if (acknowledge != null) {
                acknowledge.doClick();
            } else {
                told.setVisible(false);
            }
        });
        flush();
    }

    private static SheetFormat formatOf(Path file) {
        String name = file.getFileName().toString()
                .toLowerCase(Locale.ROOT);
        for (SheetFormat format : SheetFormat.values()) {
            if (name.endsWith("." + format.name()
                    .toLowerCase(Locale.ROOT))) {
                return format;
            }
        }
        throw new AssertionError("no format in " + name);
    }

    private JDialog awaitDialog(String title) throws Exception {
        for (int tries = 0; tries < 400; tries++) {
            JDialog[] found = new JDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog && dialog.isVisible()
                            && title.equals(dialog.getTitle())) {
                        found[0] = dialog;
                    }
                }
            });
            if (found[0] != null) {
                return found[0];
            }
            Thread.sleep(25);
        }
        throw new AssertionError("no dialog titled " + title + " appeared");
    }

    private static <T extends Component> T find(Container root,
                                                Class<T> kind, String name) {
        for (Component child : root.getComponents()) {
            if (kind.isInstance(child) && name.equals(child.getName())) {
                return kind.cast(child);
            }
            if (child instanceof Container inside) {
                T found = find(inside, kind, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton firstButton(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button) {
                return button;
            }
            if (child instanceof Container inside) {
                JButton found = firstButton(inside);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void closeEverything(JFrame window) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window open : Window.getWindows()) {
                if (open instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
            if (window != null) {
                window.dispose();
            }
        });
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

}
