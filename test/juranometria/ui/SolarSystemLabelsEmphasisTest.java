package juranometria.ui;

import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.jovianchart.JovianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.chart.StarSizePolicy;
import juranometria.render.StructureStyle;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Solar System labels as one emphasis class (issue #526): while the
 * class is raised, every drawn body's and moon's name is written in the
 * atlas's existing deep red, and nothing else on the page moves - the
 * discs, marks, phase geometry and state marks keep their ink, a
 * refused name stays refused, a below-horizon name stays quieted, the
 * placements are the canonical page's. Held on a wide page with the
 * Sun, the Moon and Venus under a drawn horizon, on a close page with
 * Jupiter and its moons, in both languages and on both grounds; the
 * export carries the class's token and the same ink; the class is
 * offered only while a body is drawn, and settles when none is.
 */
class SolarSystemLabelsEmphasisTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    /** The packaged emphasis journey's observer and instant (#361). */
    private static final Observer OSLO_EQUINOX = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));
    /** The named triple transit the Jovian evidence is held to (#484). */
    private static final Observer OSLO_TRIPLE = new Observer(59.913, 10.752,
            Instant.parse("2026-12-11T22:45:00Z"));
    /** Espenak's 2012 Venus transit, to the hour (venus-events.txt): Venus's name refused inside the Sun's ink at 2° (#514). */
    private static final Observer OSLO_TRANSIT = new Observer(59.913, 10.752,
            Instant.parse("2012-06-06T01:00:00Z"));
    private static SolarSystemService solar;
    private static JovianSystemService jovian;

    @BeforeAll
    static void load() {
        solar = SolarSystemService.load();
        jovian = JovianSystemService.load();
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    /** The Sun, the Moon and Venus, through the production module, the horizon drawn. */
    private static OverlayRegistry solarSystem(Observer at, boolean horizon) {
        SolarSystemModule module = new SolarSystemModule(() -> at, () -> solar, () -> horizon);
        module.sunShowing(true);
        module.moonShowing(true);
        module.venusShowing(true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static OverlayRegistry jupiterAndMoons(Observer at) {
        JovianModule module = new JovianModule(() -> at, () -> jovian, () -> false);
        module.showing(true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(JovianModule.ID, module::contributedGeometry);
        return registry;
    }

    private static SkyPosition sunAt(Observer at) {
        return ((SolarSystemService.SunObservation) solar.observe(
                SolarSystemService.Body.SUN, at)).astrometricJ2000();
    }

    /** The page with both layers, the raised set stated: the bodies' placements come back. */
    private static List<ReferenceInk.BodyPlacement> paint(BufferedImage into, ChartScene scene,
            OverlayRegistry registry, ChartPalette palette, PageText words,
            Set<ChartStructure> raised) {
        List<ReferenceInk.BodyPlacement> placed = new ArrayList<>();
        java.awt.Graphics2D g = into.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
            renderer.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, layerScene, reserved) -> ReferenceInk.paint(layerG, layerScene,
                            registry.collect(), palette, words, reserved, raised),
                    (layerG, layerScene, reserved) -> {
                        DrawnPage page = DrawnPage.of(layerScene);
                        placed.addAll(ReferenceInk.paintBodies(layerG, page,
                                registry.collect(), palette, words, reserved,
                                ReferenceInk.referenceBoxes(page, registry.collect(), words,
                                        reserved, ChartStructure.membersOf(raised)),
                                ChartStructure.membersOf(raised)));
                    }, null, raised);
        } finally {
            g.dispose();
        }
        return placed;
    }

    private static BufferedImage blank() {
        return new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
    }

    /** Every pixel that differs between two paintings, as points. */
    private static List<int[]> differing(BufferedImage a, BufferedImage b) {
        List<int[]> out = new ArrayList<>();
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    out.add(new int[] {x, y});
                }
            }
        }
        return out;
    }

    private static boolean insideANameBox(int[] p, List<ReferenceInk.BodyPlacement> placed) {
        for (ReferenceInk.BodyPlacement body : placed) {
            Rectangle2D box = body.box();
            // Antialiased glyphs may bleed a pixel past the metrics' box.
            if (box != null && new Rectangle2D.Double(box.getX() - 1, box.getY() - 1,
                    box.getWidth() + 2, box.getHeight() + 2).contains(p[0] + 0.5, p[1] + 0.5)) {
                return true;
            }
        }
        return false;
    }

    /** The class raised against the canonical page: ink only, inside the names' boxes. */
    private static void onlyTheNamesMove(ChartScene scene, OverlayRegistry registry,
                                         ChartPalette palette, PageText words, String page) {
        BufferedImage canonical = blank();
        BufferedImage raised = blank();
        List<ReferenceInk.BodyPlacement> before = paint(canonical, scene, registry, palette,
                words, Set.of());
        List<ReferenceInk.BodyPlacement> after = paint(raised, scene, registry, palette,
                words, Set.of(ChartStructure.SOLAR_SYSTEM_LABELS));
        assertEquals(before.size(), after.size(), page + ": the same bodies drawn");
        int placedNames = 0;
        for (int i = 0; i < before.size(); i++) {
            ReferenceInk.BodyPlacement b = before.get(i);
            ReferenceInk.BodyPlacement a = after.get(i);
            assertEquals(b.identity(), a.identity());
            assertEquals(b.name(), a.name(), page + ": the same words");
            assertEquals(b.box(), a.box(), page + ": " + b.identity()
                    + "'s name placed or refused exactly as before - never forced");
            assertEquals(b.centre(), a.centre());
            assertEquals(b.belowHorizon(), a.belowHorizon());
            assertEquals(b.disc().getBounds2D(), a.disc().getBounds2D());
            if (b.box() != null) {
                placedNames++;
            }
        }
        assertTrue(placedNames > 0, page + ": at least one name is placed, so the class"
                + " has ink to change: " + before);
        List<int[]> moved = differing(canonical, raised);
        assertTrue(moved.size() > 20, page + ": the names rise in the deep red: "
                + moved.size() + " px");
        for (int[] p : moved) {
            assertTrue(insideANameBox(p, before), page + ": a pixel outside every name box"
                    + " changed at " + p[0] + "," + p[1] + " - the discs and marks must keep"
                    + " their ink");
        }
    }

    @Test
    void onTheWidePageTheSunTheMoonAndVenusTakeTheDeepRedAndNothingElseMoves() {
        ChartScene scene = page(sunAt(OSLO_EQUINOX), 120.0);
        OverlayRegistry registry = solarSystem(OSLO_EQUINOX, true);
        for (ChartPalette palette : ChartPalette.values()) {
            onlyTheNamesMove(scene, registry, palette, ENGLISH, "120° " + palette);
        }
        onlyTheNamesMove(scene, registry, ChartPalette.WHITE_PAPER, NORSK, "120° nb-NO");
    }

    @Test
    void aBelowHorizonNameStaysQuietedAndStillTakesTheRed() {
        // Oslo at 21:33 UTC on the March equinox: the Sun is far below
        // the drawn horizon, its name carrying the language's status.
        ChartScene scene = page(sunAt(OSLO_EQUINOX), 120.0);
        OverlayRegistry registry = solarSystem(OSLO_EQUINOX, true);
        BufferedImage canonical = blank();
        BufferedImage raised = blank();
        List<ReferenceInk.BodyPlacement> placed = paint(canonical, scene, registry,
                ChartPalette.WHITE_PAPER, ENGLISH, Set.of());
        paint(raised, scene, registry, ChartPalette.WHITE_PAPER, ENGLISH,
                Set.of(ChartStructure.SOLAR_SYSTEM_LABELS));
        ReferenceInk.BodyPlacement sun = placed.stream()
                .filter(b -> SolarSystemModule.SUN.equals(b.identity())).findFirst().orElseThrow();
        assertTrue(sun.belowHorizon() && sun.box() != null,
                "the Sun below the horizon, its name placed: " + sun);
        assertTrue(sun.name().contains(ENGLISH.bodyBelowHorizon()),
                "the status still carried: " + sun.name());
        // The quieted deep red and the quieted text ink are both present
        // in their boxes, and only there: the raised box differs from the
        // canonical one, and its darkest pixel is lighter than the full
        // accent, as quieting leaves it.
        java.awt.Color accent = StructureStyle.labelInk(ChartPalette.WHITE_PAPER, true,
                ChartPalette.WHITE_PAPER.textInk());
        java.awt.Color quietAccent = ChartRenderer.quiet(accent, ChartPalette.WHITE_PAPER.ground());
        int inBox = 0;
        boolean quietAccentSeen = false;
        boolean fullAccentSeen = false;
        for (int y = (int) sun.box().getMinY(); y < sun.box().getMaxY(); y++) {
            for (int x = (int) sun.box().getMinX(); x < sun.box().getMaxX(); x++) {
                if (canonical.getRGB(x, y) != raised.getRGB(x, y)) {
                    inBox++;
                }
                int rgb = raised.getRGB(x, y) & 0xffffff;
                quietAccentSeen |= rgb == (quietAccent.getRGB() & 0xffffff);
                fullAccentSeen |= rgb == (accent.getRGB() & 0xffffff);
            }
        }
        assertTrue(inBox > 10, "the Sun's name changed ink: " + inBox + " px");
        assertTrue(quietAccentSeen, "written in the quieted deep red, as the text ink is quieted");
        assertFalse(fullAccentSeen, "and never in the full accent below the horizon");
    }

    @Test
    void onTheClosePageJupitersMoonsNamesTakeTheRedWithJupiters() {
        JovianSystemService.JupiterObservation jupiter = jovian.observeJupiter(OSLO_TRIPLE);
        ChartScene scene = page(jupiter.astrometricJ2000(),
                ChartViewState.normalMinimumFieldDegrees());
        OverlayRegistry registry = jupiterAndMoons(OSLO_TRIPLE);
        for (ChartPalette palette : ChartPalette.values()) {
            onlyTheNamesMove(scene, registry, palette, ENGLISH, "1° Jupiter " + palette);
        }
        BufferedImage canonical = blank();
        BufferedImage raised = blank();
        List<ReferenceInk.BodyPlacement> placed = paint(canonical, scene, registry,
                ChartPalette.WHITE_PAPER, ENGLISH, Set.of());
        paint(raised, scene, registry, ChartPalette.WHITE_PAPER, ENGLISH,
                Set.of(ChartStructure.SOLAR_SYSTEM_LABELS));
        int moonNames = 0;
        for (ReferenceInk.BodyPlacement body : placed) {
            if (body.symbol() && body.box() != null && !body.identity().equals("jupiter")) {
                moonNames++;
                boolean changed = false;
                for (int y = (int) body.box().getMinY(); y < body.box().getMaxY() && !changed; y++) {
                    for (int x = (int) body.box().getMinX(); x < body.box().getMaxX(); x++) {
                        if (canonical.getRGB(x, y) != raised.getRGB(x, y)) {
                            changed = true;
                            break;
                        }
                    }
                }
                assertTrue(changed, body.identity() + "'s name takes the red with Jupiter's");
            }
        }
        assertTrue(moonNames >= 2, "the triple transit draws moons with names: " + placed);
    }

    @Test
    void aRefusedNameStaysRefused() {
        // Venus in front of the Sun at 2° (#514): every candidate box for
        // Venus's name lies inside the Sun's ink, so the name is refused -
        // and the class does not force it.
        Observer at = OSLO_TRANSIT;
        SolarSystemModule module = new SolarSystemModule(() -> at, () -> solar, () -> false);
        module.sunShowing(true);
        module.venusShowing(true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        ChartScene scene = page(sunAt(at), 2.0);
        List<ReferenceInk.BodyPlacement> raised = paint(blank(), scene, registry,
                ChartPalette.WHITE_PAPER, ENGLISH, Set.of(ChartStructure.SOLAR_SYSTEM_LABELS));
        ReferenceInk.BodyPlacement venus = raised.stream()
                .filter(b -> SolarSystemModule.VENUS.equals(b.identity())).findFirst().orElseThrow();
        assertTrue(venus.box() == null, "Venus's name is refused inside the Sun's ink,"
                + " raised or not: " + venus);
        onlyTheNamesMove(scene, registry, ChartPalette.WHITE_PAPER, ENGLISH, "2° transit");
    }

    @Test
    void theClassIsOfferedOnlyWhileABodyIsDrawnAndSettlesWhenNoneIs() throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        ChartModuleHost[] host = new ChartModuleHost[1];
        SolarSystemModule[] module = new SolarSystemModule[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart[0].setSize(900, 700);
            chart[0].setViewState(new ChartViewState(sunAt(OSLO_EQUINOX), 120.0, 8.0, null, null));
            host[0] = new ChartModuleHost(chart[0], new juranometria.chart.SelectionModel(),
                    request -> { });
        });
        assertFalse(chart[0].emphasisAvailable(ChartStructure.SOLAR_SYSTEM_LABELS),
                "no module contributes a body: nothing to raise");
        chart[0].toggleEmphasis(ChartStructure.SOLAR_SYSTEM_LABELS);
        assertEquals(Set.of(), chart[0].emphasizedSet(), "an unavailable ask settles");
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            module[0] = host[0].attach(new SolarSystemModule(() -> OSLO_EQUINOX, () -> solar,
                    () -> false));
            module[0].sunShowing(true);
        });
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        assertTrue(chart[0].emphasisAvailable(ChartStructure.SOLAR_SYSTEM_LABELS),
                "the Sun is contributed: the class can be raised");
        chart[0].toggleEmphasis(ChartStructure.SOLAR_SYSTEM_LABELS);
        chart[0].toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        assertEquals(Set.of(ChartStructure.EQUATORIAL_GRID, ChartStructure.SOLAR_SYSTEM_LABELS),
                chart[0].emphasizedSet(), "independent of the other choices");
        // The menu says it, in both languages, as the last row.
        for (String[] language : new String[][] {{"en", "Solar System labels"},
                {"nb-NO", "Solsystemetiketter"}}) {
            javax.swing.JPopupMenu menu = ChartActions.emphasisMenu(chart[0],
                    InterfaceText.forLanguage(language[0]));
            javax.swing.JCheckBoxMenuItem last = (javax.swing.JCheckBoxMenuItem)
                    menu.getComponent(menu.getComponentCount() - 1);
            assertEquals(language[1], last.getText());
            assertTrue(last.isSelected() && last.isEnabled(), language[0]);
        }
        javax.swing.SwingUtilities.invokeAndWait(() -> module[0].sunShowing(false));
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        assertEquals(Set.of(ChartStructure.EQUATORIAL_GRID), chart[0].emphasizedSet(),
                "the last body hidden: only the label class settles");
        javax.swing.SwingUtilities.invokeAndWait(host[0]::detachAll);
    }

    @Test
    void theExportCarriesTheTokenAndTheSameInk() throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart[0].setSize(900, 700);
            chart[0].setViewState(new ChartViewState(sunAt(OSLO_EQUINOX), 120.0, 8.0, null, null));
            SolarSystemModule module = new SolarSystemModule(() -> OSLO_EQUINOX, () -> solar,
                    () -> false);
            module.sunShowing(true);
            module.moonShowing(true);
            module.venusShowing(true);
            chart[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
        });
        Set<ChartStructure> raised = Set.of(ChartStructure.SOLAR_SYSTEM_LABELS);
        SheetRecording plain = ChartSheet.record(Atlas.assembler()::assemble,
                chart[0].viewState(), ChartOptions.DEFAULTS, SheetInk.reference(chart[0], Set.of()),
                SheetInk.bodies(chart[0], Set.of()), ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4, ENGLISH, Set.of());
        SheetRecording red = ChartSheet.record(Atlas.assembler()::assemble,
                chart[0].viewState(), ChartOptions.DEFAULTS, SheetInk.reference(chart[0], raised),
                SheetInk.bodies(chart[0], raised), ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4, ENGLISH, raised);
        assertEquals("solar-system-labels", red.metadata().emphasis(), "the class's token");
        String svg = new String(SheetWriters.write(red, SheetFormat.SVG, 300),
                StandardCharsets.UTF_8);
        assertTrue(svg.contains("<metadata>emphasis:solar-system-labels</metadata>"));
        assertTrue(svg.contains("#a42e2e"), "the deep red reaches the paper");
        String plainSvg = new String(SheetWriters.write(plain, SheetFormat.SVG, 300),
                StandardCharsets.UTF_8);
        assertFalse(plainSvg.contains("#a42e2e"), "and only when the class is raised");
        assertNotEquals(plainSvg, svg);
        for (SheetFormat format : SheetFormat.values()) {
            assertFalse(java.util.Arrays.equals(SheetWriters.write(plain, format, 150),
                    SheetWriters.write(red, format, 150)), format + " carries the ink");
        }
    }
}
