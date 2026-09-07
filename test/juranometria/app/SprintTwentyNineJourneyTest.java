package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
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
            ChartComponent[] chartHolder = new ChartComponent[1];
            ChartModuleHost[] hostHolder = new ChartModuleHost[1];
            List<juranometria.module.NavigationRequest> asked =
                    new ArrayList<>();
            AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
            SearchField[] searchHolder = new SearchField[1];

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                options.onChange(chart::setChartOptions);
                // The host owns the working selection, as it does in
                // the application, and the chart's own click writes
                // into it - not the test (PR #292 re-review).
                hostHolder[0] = new ChartModuleHost(chart,
                        new juranometria.chart.SelectionModel(),
                        asked::add);
                hostHolder[0].attach(new juranometria.ui.onthispage
                        .OnThisPageModule());
                juranometria.ui.SelectInteraction.install(chart,
                        new juranometria.chart.SelectionModel(),
                        hostHolder[0].workingSelection(),
                        hostHolder[0].selectionMode());
                hostHolder[0].workingSelection().onChange(change ->
                        chart.setWorkingSelection(change.members(),
                                change.lead()));
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
            ChartModuleHost host = hostHolder[0];
            WorkingSelection working = host.workingSelection();

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

            // ---- 4/5. the export surface, driven end to end -------
            // Not the item, then separately the dialog, then
            // separately the export: the menu item runs the real
            // route, and the route asks its questions through the
            // real dialog with its Export button pressed (PR #292
            // review). Only the file chooser is answered for the
            // reader, because a platform's save dialog is not this
            // application's to drive.
            ChartViewState exporting = onEdt(navigation::state);
            List<Path> written = new ArrayList<>();
            List<SheetFormat> chose = new ArrayList<>();
            for (SheetFormat format : SheetFormat.values()) {
                ExportSheetSession.Surfaces surfaces =
                        new ExportSheetSession.Surfaces() {

                    @Override
                    public java.util.Optional<ExportSheet.Request>
                            chooseWhat(java.awt.Frame owner,
                                    ExportSheet.Request initial) {
                        // The real dialog content, with the format
                        // chosen and the real Export button pressed.
                        List<ExportSheet.Request> chosen =
                                new ArrayList<>();
                        JComponent dialog = ExportSheetDialog.content(
                                initial, chosen::add, () -> { });
                        JComboBox<SheetFormat> box = named(dialog,
                                ExportSheetDialog.FORMAT_BOX);
                        box.setSelectedItem(format);
                        ((JButton) named(dialog,
                                ExportSheetDialog.EXPORT_BUTTON))
                                .doClick();
                        chosen.stream().findFirst().ifPresent(request ->
                                chose.add(request.format()));
                        return chosen.stream().findFirst();
                    }

                    @Override
                    public java.util.Optional<File> chooseWhere(
                            java.awt.Frame owner, String suggestedName) {
                        assertTrue(suggestedName.startsWith(
                                        "juranometria-")
                                        && suggestedName.endsWith("."
                                                + format.extension()),
                                "the save dialog is offered a name the"
                                        + " reader can find again: "
                                        + suggestedName);
                        return java.util.Optional.of(
                                folder.resolve("orion").toFile());
                    }

                    @Override
                    public ExportSheet.ReplaceDecision replace(
                            java.awt.Frame owner) {
                        return replacing -> true;
                    }

                    @Override
                    public void report(java.awt.Frame owner,
                            ExportSheet.Outcome outcome) {
                        written.add(assertInstanceOf(
                                ExportSheet.Outcome.Written.class,
                                outcome, "5. " + format
                                        + " is written").file());
                    }
                };

                List<String> opened = new ArrayList<>();
                JMenuBar bar = onEdt(() -> AppMenuBar.create(navigation,
                        () -> { }, () -> { }, () -> { }, () -> { },
                        () -> { }, () -> { }, () -> {
                            opened.add("export");
                            ExportSheetSession.open(null, navigation,
                                    chart, options, working, surfaces);
                        }));
                JMenuItem export = AppMenuBar.exportItem(bar);
                assertTrue(export != null && export.isEnabled(),
                        "4. File carries the export item");
                SwingUtilities.invokeAndWait(export::doClick);
                assertEquals(List.of("export"), opened,
                        "and pressing it runs the export route");
            }
            assertEquals(List.of(SheetFormat.SVG, SheetFormat.PDF,
                            SheetFormat.PNG), chose,
                    "5. each format is chosen in the real dialog");
            assertEquals(3, written.size(),
                    "and each one reported a written sheet");

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

            // ---- 5b. the marks a reader made, on the sheet -------
            // The export switch is off by default, so a journey that
            // never marks anything and never ticks the box exercises
            // none of it (PR #292 re-review). Here the reader marks
            // two objects on the chart itself and asks for them.
            // Marked by clicking them on the chart, which is how a
            // reader marks anything - writing to the model directly
            // proves nothing about the chart (PR #292 re-review, and
            // the same finding as #289 round one).
            ChartRenderer.DrawnMark bright = clickOn(chart, 0,
                    scene -> markOf(scene, 0.0, 5.0));
            ChartRenderer.DrawnMark faint = clickOn(chart,
                    juranometria.ui.SelectInteraction.toggleModifierMask(),
                    scene -> markOf(scene, 6.5, 7.8));
            List<String> marked = List.of(bright.star().id(),
                    faint.star().id());
            assertEquals(marked, onEdt(working::members),
                    "5b. the reader marked two objects by clicking"
                            + " them");

            // And then asked for fewer stars, which leaves the fainter
            // one on the page and no longer drawn - the case that gets
            // a cross rather than a ring, and the only way to reach it
            // without reaching past the chart.
            JButton fewer = button(toolbar, "Fewer stars");
            while (onEdt(() -> navigation.state().limitingMagnitude())
                    > 6.0) {
                ReaderInput.click(fewer);
            }
            assertTrue(faint.star().magnitude() > 6.0,
                    "5b. the fainter mark is now past the limit: "
                            + faint.star().magnitude());
            assertEquals(marked, onEdt(working::members),
                    "and both are still marked - changing what the"
                            + " chart draws never changes what the"
                            + " reader chose");

            List<Path> withMarks = new ArrayList<>();
            List<Boolean> boxTicked = new ArrayList<>();
            ExportSheetSession.Surfaces asking =
                    new ExportSheetSession.Surfaces() {

                @Override
                public java.util.Optional<ExportSheet.Request> chooseWhat(
                        java.awt.Frame owner, ExportSheet.Request initial) {
                    List<ExportSheet.Request> chosen = new ArrayList<>();
                    JComponent dialog = ExportSheetDialog.content(
                            initial, chosen::add, () -> { });
                    javax.swing.JCheckBox box = named(dialog,
                            ExportSheetDialog.WORKING_BOX);
                    assertFalse(box.isSelected(),
                            "5b. the switch starts off, as the gate"
                                    + " decided");
                    box.setSelected(true);
                    boxTicked.add(true);
                    ((JButton) named(dialog,
                            ExportSheetDialog.EXPORT_BUTTON)).doClick();
                    return chosen.stream().findFirst();
                }

                @Override
                public java.util.Optional<File> chooseWhere(
                        java.awt.Frame owner, String suggestedName) {
                    return java.util.Optional.of(
                            folder.resolve("marked").toFile());
                }

                @Override
                public ExportSheet.ReplaceDecision replace(
                        java.awt.Frame owner) {
                    return replacing -> true;
                }

                @Override
                public void report(java.awt.Frame owner,
                        ExportSheet.Outcome outcome) {
                    withMarks.add(assertInstanceOf(
                            ExportSheet.Outcome.Written.class, outcome,
                            "5b. the marked sheet is written").file());
                }
            };
            SwingUtilities.invokeAndWait(() -> ExportSheetSession.open(
                    null, navigation, chart, options, working, asking));
            assertEquals(List.of(true), boxTicked,
                    "5b. the reader ticked the switch in the real"
                            + " dialog");

            // Both marks are on that sheet, each in its own form:
            // a ring around the one the page draws, and a four-armed
            // cross around the one it does not.
            assertEquals(List.of(), marksMissingFrom(withMarks.get(0),
                            onEdt(navigation::state),
                            List.of(bright.star().id())),
                    "5b. the drawn object the reader marked is ringed"
                            + " on the sheet");
            assertTrue(crossedAt(withMarks.get(0),
                            onEdt(navigation::state),
                            faint.star().position()),
                    "5b. and the one the page no longer draws is"
                            + " crossed, not silently left off");

            // And the sheet made without the switch has none of them.
            Path unmarked = assertInstanceOf(
                    ExportSheet.Outcome.Written.class,
                    ExportSheetSession.exportTo(
                            folder.resolve("unmarked").toFile(),
                            new ExportSheet.Request(SheetFormat.SVG,
                                    PaperSize.A4, 300, false),
                            navigation, chart, options, working,
                            replacing -> true),
                    "an unticked export is written too").file();
            assertEquals(1, marksMissingFrom(unmarked,
                            onEdt(navigation::state),
                            List.of(bright.star().id())).size(),
                    "and carries none of the reader's marks, because"
                            + " they did not ask for them");
            assertFalse(crossedAt(unmarked, onEdt(navigation::state),
                            faint.star().position()),
                    "neither the rings nor the crosses");
            SwingUtilities.invokeAndWait(working::clear);
            while (onEdt(() -> navigation.state().limitingMagnitude())
                    < 8.0) {
                ReaderInput.click(button(toolbar, "More stars"));
            }

            // ---- 6b. a page the ecliptic actually crosses ---------
            // Orion is where the issue's journey goes and the
            // ecliptic passes above it, so its export cannot be
            // proved there and nothing on that page would fail if it
            // vanished (PR #292 review). A club member looking for
            // the zodiac goes where it is; so does this.
            SwingUtilities.invokeAndWait(() -> navigation.recenter(
                    new SkyPosition(0.0, 0.0), 42.0));
            SwingUtilities.invokeAndWait(() -> { });
            ChartViewState equinox = onEdt(navigation::state);
            List<Path> zodiac = new ArrayList<>();
            for (SheetFormat format : SheetFormat.values()) {
                var outcome = ExportSheetSession.exportTo(
                        folder.resolve("equinox").toFile(),
                        new ExportSheet.Request(format, PaperSize.A4,
                                300, false),
                        navigation, chart, options, working,
                        replacing -> true);
                zodiac.add(assertInstanceOf(
                        ExportSheet.Outcome.Written.class, outcome,
                        "6b. the equinox page exports as " + format)
                        .file());
            }

            // The ecliptic's own ink, by the dash it alone is drawn
            // with, in both vector formats - and its landmark
            // diamonds, which are the thing a reader looks for.
            String zodiacSvg = Files.readString(zodiac.get(0),
                    StandardCharsets.UTF_8);
            assertTrue(zodiacSvg.contains(
                            "stroke-dasharray=\"12.00,4.00,2.00,4.00\""),
                    "the ecliptic reaches the SVG as its own dash-dot"
                            + " line");
            String zodiacPdf = Files.readString(zodiac.get(1),
                    StandardCharsets.ISO_8859_1);
            assertTrue(zodiacPdf.contains("[12.00 4.00 2.00 4.00]"),
                    "and the PDF with the same dash");
            assertTrue(zodiacSvg.contains("March equinox")
                            || landmarkDiamonds(zodiacSvg) > 0,
                    "with the landmark the page is centred on");

            // And the same chart in all three, through the sky: what
            // production drew is where the sky says it goes, in the
            // SVG's paths and in the PNG's pixels alike.
            assertEquals(List.of(), formatsThatDisagree(zodiac, equinox),
                    "6b. the three formats carry the same chart");

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
            // Against the state the last export was made from: the
            // reader moved the chart to the equinox themselves, and
            // that is navigation rather than anything the export did.
            assertEquals(equinox, onEdt(navigation::state),
                    "9. exporting changed nothing about the chart");
            assertEquals(42.0, exporting.fieldWidthDegrees(),
                    "and the Orion sheets were made from the page the"
                            + " reader was on");
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
                    .skyFromPlane(scene.viewport(),
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

    /** A drawn star in a magnitude band, well inside the page. */
    private static ChartRenderer.DrawnMark markOf(ChartScene scene,
                                                  double brighter,
                                                  double fainter) {
        return new ChartRenderer(StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.star().magnitude() > brighter
                        && mark.star().magnitude() < fainter)
                .filter(mark -> mark.centre().x() > 200
                        && mark.centre().x() < 560
                        && mark.centre().y() > 120
                        && mark.centre().y() < 400)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no drawn star between magnitude " + brighter
                                + " and " + fainter + " on this page"));
    }

    /**
     * Choose a mark and click it in one event-thread turn, so the
     * press lands on the page the pixel came from.
     */
    private static ChartRenderer.DrawnMark clickOn(ChartComponent chart,
            int modifiers,
            java.util.function.Function<ChartScene,
                    ChartRenderer.DrawnMark> choose) throws Exception {
        ChartRenderer.DrawnMark[] chosen = new ChartRenderer.DrawnMark[1];
        ReaderInput.click(chart, () -> {
            chosen[0] = choose.apply(chart.currentScene());
            return new java.awt.Point(
                    (int) Math.round(chosen[0].centre().x()),
                    (int) Math.round(chosen[0].centre().y())
                            + chart.pageOffsetY());
        }, modifiers);
        return chosen[0];
    }

    /** Whether the sheet carries a four-armed cross about a position. */
    private static boolean crossedAt(Path sheet, ChartViewState state,
                                     SkyPosition position)
            throws Exception {
        juranometria.sheet.SheetRecording page =
                juranometria.sheet.ChartSheet.record(
                        Atlas.assembler()::assemble, state,
                        ChartOptions.DEFAULTS,
                        ChartRenderer.ReferenceLayer.NONE,
                        PaperSize.A4);
        var at = new GnomonicProjection(page.scene().viewport().centre())
                .project(position)
                .map(new ViewportMapping(page.scene().viewport())::toPixel)
                .orElseThrow();

        // Four short arms with a gap in the middle, together centred
        // on the object: none of them is centred on it alone.
        List<double[]> arms = pathBoxes(Files.readString(sheet,
                        StandardCharsets.UTF_8)).stream()
                .filter(box -> box[2] < 6.0)
                .filter(box -> Math.hypot(box[0] - at.x(),
                        box[1] - at.y()) < 9.0)
                .toList();
        if (arms.size() != 4) {
            return false;
        }
        double x = arms.stream().mapToDouble(box -> box[0]).average()
                .orElseThrow();
        double y = arms.stream().mapToDouble(box -> box[1]).average()
                .orElseThrow();
        return Math.hypot(x - at.x(), y - at.y()) < 1.0;
    }

    /**
     * Which of the marked objects have no ring on this sheet, where
     * the sky puts them.
     */
    private static List<String> marksMissingFrom(Path sheet,
            ChartViewState state, List<String> marked) throws Exception {
        juranometria.sheet.SheetRecording page =
                juranometria.sheet.ChartSheet.record(
                        Atlas.assembler()::assemble, state,
                        ChartOptions.DEFAULTS,
                        ChartRenderer.ReferenceLayer.NONE,
                        PaperSize.A4);
        String svg = Files.readString(sheet, StandardCharsets.UTF_8);
        // Centre and width together: a star's own disc is centred on
        // the object too, so ink at the right place proves nothing.
        // A ring is bigger than the mark it rings, and that is what
        // distinguishes a marked sheet from a plain one.
        List<double[]> ink = pathBoxes(svg);
        GnomonicProjection projection = new GnomonicProjection(
                page.scene().viewport().centre());
        ViewportMapping mapping = new ViewportMapping(
                page.scene().viewport());

        List<String> missing = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(page.scene(), page.options())) {
            if (mark.star() == null
                    || !marked.contains(mark.star().id())) {
                continue;
            }
            var at = projection.project(mark.star().position())
                    .map(mapping::toPixel).orElseThrow();
            double ring = 2.0 * Math.max(mark.reach() + 5.0, 7.0);
            boolean ringed = ink.stream().anyMatch(box ->
                    Math.hypot(box[0] - at.x(), box[1] - at.y()) < 0.51
                            && Math.abs(box[2] - ring) < 1.0);
            if (!ringed) {
                missing.add(mark.star().id());
            }
        }
        return missing;
    }

    /** How many of the ecliptic's open diamonds the sheet carries. */
    private static int landmarkDiamonds(String svg) {
        int found = 0;
        var each = java.util.regex.Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(svg);
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            var number = java.util.regex.Pattern
                    .compile("-?\\d+(?:\\.\\d+)?")
                    .matcher(each.group(1));
            while (number.find()) {
                numbers.add(Double.parseDouble(number.group()));
            }
            if (numbers.size() != 8) {
                continue;  // four vertices, closed
            }
            double minX = numbers.get(0);
            double maxX = minX;
            double minY = numbers.get(1);
            double maxY = minY;
            for (int i = 0; i + 1 < numbers.size(); i += 2) {
                minX = Math.min(minX, numbers.get(i));
                maxX = Math.max(maxX, numbers.get(i));
                minY = Math.min(minY, numbers.get(i + 1));
                maxY = Math.max(maxY, numbers.get(i + 1));
            }
            if (Math.abs(maxX - minX - 12.0) < 0.5
                    && Math.abs(maxY - minY - 12.0) < 0.5) {
                found++;
            }
        }
        return found;
    }

    /**
     * Which formats disagree with production about where the chart
     * is: every mark the renderer drew for this sheet, projected the
     * way the sheet projects it, looked for in the SVG's own paths
     * and in the PNG's own pixels.
     */
    private static List<String> formatsThatDisagree(List<Path> sheets,
                                                    ChartViewState state)
            throws Exception {
        juranometria.sheet.SheetRecording sheet =
                juranometria.sheet.ChartSheet.record(
                        Atlas.assembler()::assemble, state,
                        ChartOptions.DEFAULTS,
                        ChartRenderer.ReferenceLayer.NONE,
                        PaperSize.A4);
        String svg = Files.readString(sheets.get(0),
                StandardCharsets.UTF_8);
        BufferedImage png = javax.imageio.ImageIO.read(
                sheets.get(2).toFile());
        // Each mark is looked for as the shape it is - centred on
        // the object and the size the renderer drew it - not as ink
        // somewhere nearby. A tolerance of "any coordinate within a
        // star's radius" accepted a grid line's own points: with
        // every disc removed from the PDF, seventy of them still
        // read as present (PR #292 re-review).
        List<double[]> inSvg = pathBoxes(svg);
        List<double[]> inPdf = pdfPathBoxes(sheets.get(1));

        GnomonicProjection projection = new GnomonicProjection(
                sheet.scene().viewport().centre());
        ViewportMapping onPaper = new ViewportMapping(
                sheet.scene().viewport());
        double scale = 300 / 72.0;
        double margin = PaperSize.A4.marginPoints();

        int checked = 0;
        int missingFromSvg = 0;
        int missingFromPdf = 0;
        int blankInPng = 0;
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(sheet.scene(), sheet.options())) {
            var at = projection.project(mark.star() != null
                            ? mark.star().position()
                            : mark.deepSky().position())
                    .map(onPaper::toPixel);
            if (at.isEmpty() || at.get().x() < 30 || at.get().y() < 30
                    || at.get().x() > PaperSize.A4.chartWideUnits() - 30
                    || at.get().y() > PaperSize.A4.chartHighUnits() - 30) {
                continue;
            }
            double x = at.get().x();
            double y = at.get().y();
            if (paintedOver(sheet, x, y)) {
                continue;
            }
            checked++;
            double across = 2.0 * mark.reach();
            // A star is a filled disc and a deep-sky object is an
            // outline, so the kind is part of the mark's identity.
            // Without it a stroked line of the right length at the
            // right place answers for a disc, and one still did
            // after the size check was added (PR #292 re-review).
            boolean filled = mark.kind() == ChartRenderer.DrawnMark.Kind.STAR;
            if (!drawnAt(inSvg, x, y, across, filled)) {
                missingFromSvg++;
            }
            // The PDF's own coordinates are the chart's, after the
            // one flip at the top of its content stream, so a mark
            // is looked for where the sky puts it and at the size
            // the renderer gave it, just as in the SVG.
            if (!drawnAt(inPdf, x, y, across, filled)) {
                missingFromPdf++;
            }
            // The furniture is opaque and drawn last - the title
            // block owns the lower left, the magnitude key the upper
            // right - so a mark underneath one is hidden on a raster
            // while its path is still there in the vector formats,
            // beneath the panel, exactly as a viewer will draw it.
            int reach = (int) Math.ceil(
                    Math.max(mark.reach(), 1.5) * scale) + 4;
            if (!inkNear(png, (int) Math.round((x + margin) * scale),
                    (int) Math.round((y + margin) * scale), reach)) {
                blankInPng++;
            }
        }

        List<String> disagree = new ArrayList<>();
        if (checked < 50) {
            disagree.add("too few marks to compare: " + checked);
        }
        if (missingFromSvg > 0) {
            disagree.add("SVG (" + missingFromSvg + " of " + checked
                    + " marks absent)");
        }
        if (missingFromPdf > 0) {
            disagree.add("PDF (" + missingFromPdf + " of " + checked
                    + " marks absent)");
        }
        if (blankInPng > 0) {
            disagree.add("PNG (" + blankInPng + " of " + checked
                    + " marks with no ink)");
        }
        return disagree;
    }

    /** Whether an opaque panel covers this point, drawn after it. */
    private static boolean paintedOver(
            juranometria.sheet.SheetRecording sheet, double x, double y) {
        boolean seenTheMark = false;
        for (var drawn : sheet.recorder().drawn()) {
            java.awt.geom.Rectangle2D box = drawn.shape().getBounds2D();
            if (!seenTheMark && box.getWidth() < 20
                    && Math.hypot(box.getCenterX() - x,
                            box.getCenterY() - y) < 1.0) {
                seenTheMark = true;
                continue;
            }
            if (seenTheMark && drawn.filled() && box.getWidth() > 20
                    && drawn.shape().contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a shape of this size and kind is drawn at this place.
     *
     * <p>Place, size and kind together. Two of the three were not
     * enough: with every star disc removed, one mark was still
     * answered for by other ink of the same width in the same spot.
     */
    private static boolean drawnAt(List<double[]> boxes, double x,
                                   double y, double across,
                                   boolean filled) {
        return boxes.stream().anyMatch(box ->
                Math.hypot(box[0] - x, box[1] - y) < 0.51
                        && Math.abs(box[2] - across) < 0.6
                        && (box[3] > 0.5) == filled);
    }

    /**
     * Every subpath the PDF's content stream draws, as centre x,
     * centre y, width - in the chart's own coordinates.
     *
     * <p>The stream is written uncompressed, one operator to a line,
     * and flipped once at the top - so an {@code m} or {@code l}
     * carries the same numbers the SVG does.
     */
    private static List<double[]> pdfPathBoxes(Path pdf)
            throws Exception {
        String file = Files.readString(pdf,
                StandardCharsets.ISO_8859_1);
        String stream = file.substring(file.indexOf("stream\n") + 7,
                file.indexOf("endstream"));

        List<double[]> boxes = new ArrayList<>();
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean open = false;
        for (String line : stream.split("\n")) {
            if (line.endsWith(" m") || line.endsWith(" l")
                    || line.endsWith(" c")) {
                List<Double> numbers = new ArrayList<>();
                var number = java.util.regex.Pattern
                        .compile("-?\\d+(?:\\.\\d+)?").matcher(line);
                while (number.find()) {
                    numbers.add(Double.parseDouble(number.group()));
                }
                for (int i = 0; i + 1 < numbers.size(); i += 2) {
                    minX = Math.min(minX, numbers.get(i));
                    maxX = Math.max(maxX, numbers.get(i));
                    minY = Math.min(minY, numbers.get(i + 1));
                    maxY = Math.max(maxY, numbers.get(i + 1));
                }
                open = true;
            } else if (line.equals("W n") || line.equals("q")
                    || line.equals("Q")) {
                // A clip is a path too, and its corners would
                // otherwise be folded into the next shape's bounds -
                // which made every box the size of the page.
                minX = Double.MAX_VALUE;
                minY = Double.MAX_VALUE;
                maxX = -Double.MAX_VALUE;
                maxY = -Double.MAX_VALUE;
                open = false;
            } else if (open && (line.equals("f") || line.equals("S"))) {
                boxes.add(new double[] {(minX + maxX) / 2.0,
                        (minY + maxY) / 2.0, maxX - minX,
                        line.equals("f") ? 1.0 : 0.0});
                minX = Double.MAX_VALUE;
                minY = Double.MAX_VALUE;
                maxX = -Double.MAX_VALUE;
                maxY = -Double.MAX_VALUE;
                open = false;
            }
        }
        return boxes;
    }

    /** Each drawn path as centre x, centre y, width, filled. */
    private static List<double[]> pathBoxes(String svg) {
        List<double[]> boxes = new ArrayList<>();
        var each = java.util.regex.Pattern.compile(
                        "<path d=\"([^\"]+)\"([^/]*)/>")
                .matcher(svg.substring(svg.indexOf("<g id=\"chart\"")));
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            var number = java.util.regex.Pattern
                    .compile("-?\\d+(?:\\.\\d+)?")
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
            boxes.add(new double[] {(minX + maxX) / 2.0,
                    (minY + maxY) / 2.0, maxX - minX,
                    each.group(2).contains(" fill=\"#") ? 1.0 : 0.0});
        }
        return boxes;
    }

    private static List<double[]> pathCentres(String svg) {
        List<double[]> centres = new ArrayList<>();
        var each = java.util.regex.Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(svg.substring(svg.indexOf("<g id=\"chart\"")));
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            var number = java.util.regex.Pattern
                    .compile("-?\\d+(?:\\.\\d+)?")
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
                if (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                        + (rgb & 0xff) < 3 * 250) {
                    return true;
                }
            }
        }
        return false;
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
