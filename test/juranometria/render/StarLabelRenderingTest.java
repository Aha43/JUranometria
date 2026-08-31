package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The star-label pass at the renderer (issue #115): deterministic
 * collision outcomes, determinism across input iteration order,
 * locale, and theme, the searched-star guarantee across the option
 * toggle with no invented symbol, and the option's repaint-only
 * nature.
 */
class StarLabelRenderingTest {

    static final SkyPosition CENTRE = new SkyPosition(83.818667, -5.389667);
    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    static final Star BRIGHT = new Star("TYC 1-1-1", CENTRE, 1.0,
            new StarIdentity("Brightstar", "α", null, "Ori"));
    /** Close beside BRIGHT: their label boxes overlap. */
    static final Star DIM = new Star("TYC 1-2-1",
            new SkyPosition(83.75, -5.45), 1.5,
            new StarIdentity("Dimstar", "β", null, "Ori"));
    static final Star DIM_ANONYMOUS = new Star("TYC 1-2-1",
            new SkyPosition(83.75, -5.45), 1.5);
    /** Far corner, fainter than every label threshold. */
    static final Star FAINT = new Star("TYC 2-1-1",
            new SkyPosition(85.5, -3.5), 6.5,
            new StarIdentity("Faintstar", null, "99", "Ori"));
    static final Star FAINT_ANONYMOUS = new Star("TYC 2-1-1",
            new SkyPosition(85.5, -3.5), 6.5);

    private static ChartScene scene(List<Star> stars, String targetIdentity) {
        return new ChartScene(new ChartViewport(CENTRE, 18.0, 900, 700),
                List.copyOf(stars), List.of(), "Star label test", 8.0,
                targetIdentity, SceneGeography.EMPTY);
    }

    private static int[] pixels(ChartScene scene, ChartOptions options) {
        BufferedImage image = RENDERER.renderToImage(scene, options);
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    @Test
    void collisionsResolveDeterministicallyBrightestFirst() {
        // The dimmer neighbour's label box overlaps the brighter's and
        // is omitted - so the page with the colliding identity renders
        // exactly as if the dim star had no identity at all.
        int[] withIdentity = pixels(
                scene(List.of(BRIGHT, DIM), null), ChartOptions.DEFAULTS);
        int[] withoutIdentity = pixels(
                scene(List.of(BRIGHT, DIM_ANONYMOUS), null),
                ChartOptions.DEFAULTS);
        assertArrayEquals(withIdentity, withoutIdentity,
                "the losing label is omitted, deterministically");
        // Sanity: the winning label does draw.
        int[] bothAnonymous = pixels(scene(List.of(
                        new Star("TYC 1-1-1", CENTRE, 1.0), DIM_ANONYMOUS),
                null), ChartOptions.DEFAULTS);
        assertFalse(java.util.Arrays.equals(withIdentity, bothAnonymous),
                "the brighter star's label is on the page");
    }

    @Test
    void eachIdentifierLayerTogglesIndependently() {
        // Three layers, three separate effects: a named+lettered
        // star, a letter-only star, and a number-only star on one
        // page, each disappearing with its own control and no other.
        Star named = new Star("TYC 9-1-1", CENTRE, 1.0,
                new StarIdentity("Namedstar", "α", null, "Ori"));
        Star lettered = new Star("TYC 9-2-1", new SkyPosition(84.6, -4.4),
                3.0, new StarIdentity(null, "β", null, "Ori"));
        Star numbered = new Star("TYC 9-3-1", new SkyPosition(83.0, -6.4),
                4.0, new StarIdentity(null, null, "58", "Ori"));
        ChartScene page = new ChartScene(
                new ChartViewport(CENTRE, 8.0, 900, 700),
                List.of(named, lettered, numbered), List.of(),
                "Identifier options", 8.0, null, SceneGeography.EMPTY);
        var probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
                .createGraphics();
        var metrics = probe.getFontMetrics(ChartRenderer.labelFont());
        probe.dispose();
        var mapping = new juranometria.project.ViewportMapping(
                page.viewport());
        var projection = new juranometria.project.GnomonicProjection(CENTRE);

        record Case(String label, ChartOptions options,
                    List<String> expected) {
        }
        for (Case sample : List.of(
                new Case("all on", ChartOptions.DEFAULTS,
                        List.of("Namedstar α", "β", "58")),
                new Case("names off", new ChartOptions(true, true, true,
                        true, true, false, true, true, true),
                        List.of("α", "β", "58")),
                new Case("letters off", new ChartOptions(true, true, true,
                        true, true, true, false, true, true),
                        List.of("Namedstar", "58")),
                new Case("numbers off", new ChartOptions(true, true, true,
                        true, true, true, true, false, true),
                        List.of("Namedstar α", "β")),
                new Case("all identifiers off", new ChartOptions(true, true,
                        true, true, true, false, false, false, true),
                        List.of()))) {
            var texts = RENDERER.starLabelPlacements(metrics, page,
                            sample.options(),
                            new RegionalDetailPolicy(page,
                                    mapping.pixelsPerPlaneUnit()),
                            projection, mapping).stream()
                    .map(ChartRenderer.StarLabelPlacement::text).sorted()
                    .toList();
            assertEquals(sample.expected().stream().sorted().toList(), texts,
                    sample.label());
        }
    }

    @Test
    void theSearchedStarSurvivesEveryIdentifierToggle() {
        // The exemption is unchanged by the split: with all three
        // layers off the searched star still names itself, in the
        // decided notation.
        Star target = new Star("TYC 9-4-1", new SkyPosition(84.2, -5.0),
                6.5, new StarIdentity("Faintstar", "χ", null, "Ori"));
        ChartScene page = new ChartScene(
                new ChartViewport(CENTRE, 8.0, 900, 700),
                List.of(target), List.of(), "Target exemption", 8.0,
                "TYC 9-4-1", SceneGeography.EMPTY);
        var probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
                .createGraphics();
        var metrics = probe.getFontMetrics(ChartRenderer.labelFont());
        probe.dispose();
        var mapping = new juranometria.project.ViewportMapping(
                page.viewport());
        var texts = RENDERER.starLabelPlacements(metrics, page,
                        new ChartOptions(true, true, true, true, true,
                                false, false, false, true),
                        new RegionalDetailPolicy(page,
                                mapping.pixelsPerPlaneUnit()),
                        new juranometria.project.GnomonicProjection(CENTRE),
                        mapping).stream()
                .map(ChartRenderer.StarLabelPlacement::text).toList();
        assertEquals(List.of("Faintstar χ"), texts,
                "the searched star keeps its full identity");
    }

    @Test
    void labelOutputIsIndependentOfInputIterationOrder() {
        List<Star> forward = List.of(BRIGHT, DIM, FAINT);
        List<Star> backward = new ArrayList<>(forward);
        Collections.reverse(backward);
        assertArrayEquals(
                pixels(scene(forward, null), ChartOptions.DEFAULTS),
                pixels(scene(backward, null), ChartOptions.DEFAULTS),
                "the pass sorts; scene order must not matter");
    }

    @Test
    void notationRendersThroughTheProductionPassAndIsDeterministic()
            throws Exception {
        // The Sprint 17 notation at the renderer: a component letter
        // draws raised, a named star draws its pair, and the page is
        // identical across locales and themes (a Turkish locale is
        // the classic case-folding trap for Greek text).
        Star acrux = new Star("TYC 9-9-1", CENTRE, 1.3,
                new StarIdentity("Acrux", "α1", null, "Cru"));
        Star pi3 = new Star("TYC 9-9-2", new SkyPosition(84.5, -4.6), 3.2,
                new StarIdentity(null, "π3", null, "Ori"));
        ChartScene page = scene(List.of(acrux, pi3), null);
        var probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
                .createGraphics();
        var metrics = probe.getFontMetrics(ChartRenderer.labelFont());
        probe.dispose();
        var mapping = new juranometria.project.ViewportMapping(
                page.viewport());
        var texts = RENDERER.starLabelPlacements(metrics, page,
                        ChartOptions.DEFAULTS,
                        new RegionalDetailPolicy(page,
                                mapping.pixelsPerPlaneUnit()),
                        new juranometria.project.GnomonicProjection(CENTRE),
                        mapping).stream()
                .map(ChartRenderer.StarLabelPlacement::text).toList();
        assertTrue(texts.contains("Acrux α¹"),
                "the pair with its raised component: " + texts);
        assertTrue(texts.contains("π³"),
                "the raised component alone: " + texts);

        int[] reference = pixels(page, ChartOptions.DEFAULTS);
        assertArrayEquals(reference, pixels(page, ChartOptions.DEFAULTS));
        Locale locale = Locale.getDefault();
        javax.swing.LookAndFeel laf = javax.swing.UIManager.getLookAndFeel();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            javax.swing.UIManager.setLookAndFeel(
                    new com.formdev.flatlaf.FlatDarkLaf());
            assertArrayEquals(reference, pixels(page, ChartOptions.DEFAULTS),
                    "Greek notation is identical in every locale and theme");
        } finally {
            Locale.setDefault(locale);
            javax.swing.UIManager.setLookAndFeel(laf);
        }
    }

    @Test
    void labelOutputIsIndependentOfLocaleThemeAndRepetition() throws Exception {
        ChartScene scene = scene(List.of(BRIGHT, DIM, FAINT), "TYC 2-1-1");
        int[] reference = pixels(scene, ChartOptions.DEFAULTS);
        assertArrayEquals(reference, pixels(scene, ChartOptions.DEFAULTS),
                "repeated renders are identical");

        Locale locale = Locale.getDefault();
        javax.swing.LookAndFeel laf =
                javax.swing.UIManager.getLookAndFeel();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            javax.swing.UIManager.setLookAndFeel(
                    new com.formdev.flatlaf.FlatDarkLaf());
            assertArrayEquals(reference,
                    pixels(scene, ChartOptions.DEFAULTS),
                    "the chart is paper and ink in every locale and theme");
        } finally {
            Locale.setDefault(locale);
            javax.swing.UIManager.setLookAndFeel(laf);
        }
    }

    @Test
    void theSearchedStarKeepsItsBestLabelAcrossTheToggleWithNoNewSymbol() {
        ChartOptions labelsOff = new ChartOptions(
                true, true, true, true, true, false);
        // Off + searched: the faint star, past every threshold, still
        // carries its best identity - and the render is identical to
        // the option-on page where the ordinary pass would say nothing
        // about it either way.
        int[] offSearched = pixels(
                scene(List.of(BRIGHT, FAINT), "TYC 2-1-1"), labelsOff);
        int[] offUnsearched = pixels(
                scene(List.of(BRIGHT, FAINT), null), labelsOff);
        assertFalse(java.util.Arrays.equals(offSearched, offUnsearched),
                "the guaranteed label survives the toggle");

        // No invented anything: a searched star with no identity
        // renders exactly as unsearched - no label, no new symbol.
        assertArrayEquals(
                pixels(scene(List.of(BRIGHT, FAINT_ANONYMOUS), "TYC 2-1-1"),
                        labelsOff),
                pixels(scene(List.of(BRIGHT, FAINT_ANONYMOUS), null),
                        labelsOff),
                "an identityless searched star gains nothing");
    }

    @Test
    void theToggleChangesPixelsOnly() {
        ChartScene scene = scene(List.of(BRIGHT, DIM, FAINT), null);
        int[] on = pixels(scene, ChartOptions.DEFAULTS);
        int[] off = pixels(scene, new ChartOptions(
                true, true, true, true, true, false));
        assertFalse(java.util.Arrays.equals(on, off),
                "the option changes the page");
        // The same immutable scene renders both ways - the option is
        // consumed at the renderer alone, so scene identity, queries,
        // and navigation state cannot be involved.
        assertArrayEquals(on, pixels(scene, ChartOptions.DEFAULTS));
    }
}
