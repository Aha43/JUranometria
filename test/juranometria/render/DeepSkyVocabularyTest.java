package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deep-sky symbol vocabulary the gate decided (Sprint 21, issue
 * #184): which catalogue types share one symbol, which draw nothing,
 * and how many rows of the bundled pack each of those answers covers.
 *
 * <p>These are the decision's own numbers, executed. The decision
 * says the five reader-facing families are exactly the renderer's
 * five drawn symbols; if a type is ever moved between symbols, or a
 * sixth symbol appears with no family to name it, the document
 * becomes wrong and these tests say so.
 */
class DeepSkyVocabularyTest {

    /** The five families, named by the symbol each one is. */
    private static final Map<ChartRenderer.Symbol, String> FAMILIES =
            Map.of(ChartRenderer.Symbol.ELLIPSE, "Galaxies",
                    ChartRenderer.Symbol.DOTTED_CIRCLE, "Open clusters",
                    ChartRenderer.Symbol.CROSSED_CIRCLE,
                    "Globular clusters",
                    ChartRenderer.Symbol.BOX, "Nebulae",
                    ChartRenderer.Symbol.PLANETARY, "Planetary nebulae");

    private static List<DeepSkyObject> wholePack() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        List<DeepSkyObject> all = new ArrayList<>();
        for (int ra = 0; ra < 360; ra += 20) {
            for (int dec = -80; dec <= 80; dec += 20) {
                all.addAll(catalogue.deepSkyObjectsIn(new SkyRegion(
                        new SkyPosition(ra, dec), 15.0)));
            }
        }
        return all.stream().distinct().toList();
    }

    @Test
    void everyTypeDrawsTheSymbolTheDecisionSaysItDraws() {
        // Written out type by type rather than derived, so that
        // moving a type between families has to be done here too,
        // where the decision document can be corrected with it.
        assertEquals(ChartRenderer.Symbol.ELLIPSE,
                ChartRenderer.symbolForType(DsoType.GALAXY));
        assertEquals(ChartRenderer.Symbol.ELLIPSE,
                ChartRenderer.symbolForType(DsoType.GALAXY_PAIR));
        assertEquals(ChartRenderer.Symbol.ELLIPSE,
                ChartRenderer.symbolForType(DsoType.GALAXY_TRIPLET));
        assertEquals(ChartRenderer.Symbol.ELLIPSE,
                ChartRenderer.symbolForType(DsoType.GALAXY_GROUP));
        assertEquals(ChartRenderer.Symbol.DOTTED_CIRCLE,
                ChartRenderer.symbolForType(DsoType.OPEN_CLUSTER));
        assertEquals(ChartRenderer.Symbol.CROSSED_CIRCLE,
                ChartRenderer.symbolForType(DsoType.GLOBULAR_CLUSTER));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.CLUSTER_WITH_NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.HII_REGION));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.EMISSION_NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.REFLECTION_NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.DARK_NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX,
                ChartRenderer.symbolForType(DsoType.SUPERNOVA_REMNANT));
        assertEquals(ChartRenderer.Symbol.PLANETARY,
                ChartRenderer.symbolForType(DsoType.PLANETARY_NEBULA));

        for (DsoType undrawn : new DsoType[] {DsoType.NOVA, DsoType.STAR,
                DsoType.DOUBLE_STAR, DsoType.STELLAR_ASSOCIATION,
                DsoType.OTHER}) {
            assertEquals(ChartRenderer.Symbol.NONE,
                    ChartRenderer.symbolForType(undrawn),
                    undrawn + " is deliberately undrawn");
        }
    }

    @Test
    void everyDrawnSymbolHasExactlyOneFamilyToNameIt() {
        // The failure this prevents: a symbol the chart draws that no
        // family names, which would put marks on the page with no
        // control over them and no explanation of what they are.
        for (ChartRenderer.Symbol symbol : ChartRenderer.Symbol.values()) {
            boolean named = FAMILIES.containsKey(symbol);
            assertEquals(symbol != ChartRenderer.Symbol.NONE, named,
                    symbol + " must be named by exactly one family");
        }
        assertEquals(5, FAMILIES.size(),
                "five families, five drawn symbols");
    }

    @Test
    void theBundledPacksCountsReconcileExactly() {
        List<DeepSkyObject> pack = wholePack();
        Map<DsoType, Integer> counts = new EnumMap<>(DsoType.class);
        for (DsoType type : DsoType.values()) {
            counts.put(type, 0);
        }
        for (DeepSkyObject dso : pack) {
            counts.merge(dso.type(), 1, Integer::sum);
        }

        // The decision's table, row for row. Exact counts rather than
        // bands: a band wide enough to be safe is wide enough to hide
        // a pack regression (Sprint 19 review).
        assertEquals(13371, pack.size(), "the whole bundled pack");
        assertEquals(10521, counts.get(DsoType.GALAXY));
        assertEquals(231, counts.get(DsoType.GALAXY_PAIR));
        assertEquals(26, counts.get(DsoType.GALAXY_TRIPLET));
        assertEquals(13, counts.get(DsoType.GALAXY_GROUP));
        assertEquals(663, counts.get(DsoType.OPEN_CLUSTER));
        assertEquals(208, counts.get(DsoType.GLOBULAR_CLUSTER));
        assertEquals(67, counts.get(DsoType.CLUSTER_WITH_NEBULA));
        assertEquals(83, counts.get(DsoType.HII_REGION));
        assertEquals(94, counts.get(DsoType.NEBULA));
        assertEquals(8, counts.get(DsoType.EMISSION_NEBULA));
        assertEquals(38, counts.get(DsoType.REFLECTION_NEBULA));
        assertEquals(2, counts.get(DsoType.DARK_NEBULA));
        assertEquals(11, counts.get(DsoType.SUPERNOVA_REMNANT));
        assertEquals(130, counts.get(DsoType.PLANETARY_NEBULA));
        assertEquals(3, counts.get(DsoType.NOVA));
        assertEquals(546, counts.get(DsoType.STAR));
        assertEquals(244, counts.get(DsoType.DOUBLE_STAR));
        assertEquals(64, counts.get(DsoType.STELLAR_ASSOCIATION));
        assertEquals(419, counts.get(DsoType.OTHER));

        Map<ChartRenderer.Symbol, Integer> byFamily =
                new EnumMap<>(ChartRenderer.Symbol.class);
        for (ChartRenderer.Symbol symbol : ChartRenderer.Symbol.values()) {
            byFamily.put(symbol, 0);
        }
        for (DeepSkyObject dso : pack) {
            byFamily.merge(ChartRenderer.symbolFor(dso), 1, Integer::sum);
        }
        assertEquals(10791, byFamily.get(ChartRenderer.Symbol.ELLIPSE),
                "Galaxies");
        assertEquals(663, byFamily.get(ChartRenderer.Symbol.DOTTED_CIRCLE),
                "Open clusters");
        assertEquals(208, byFamily.get(ChartRenderer.Symbol.CROSSED_CIRCLE),
                "Globular clusters");
        assertEquals(303, byFamily.get(ChartRenderer.Symbol.BOX),
                "Nebulae");
        assertEquals(130, byFamily.get(ChartRenderer.Symbol.PLANETARY),
                "Planetary nebulae");
        assertEquals(1276, byFamily.get(ChartRenderer.Symbol.NONE),
                "deliberately undrawn");

        int drawable = 10791 + 663 + 208 + 303 + 130;
        assertEquals(12095, drawable, "the drawable rows");
        assertEquals(pack.size(), drawable + 1276,
                "and the two together are the whole pack, exactly");
    }

    @Test
    void aFamilyIsExactlyWhatTheChartDrawsTheSameWay() {
        // What makes the grouping honest: two types in one family are
        // not merely related, they are indistinguishable on the page.
        // All four galaxy types, since they are the family a reader
        // is most likely to doubt.
        assertImage(DsoType.GALAXY, DsoType.GALAXY_PAIR, true);
        assertImage(DsoType.GALAXY, DsoType.GALAXY_TRIPLET, true);
        assertImage(DsoType.GALAXY, DsoType.GALAXY_GROUP, true);
        assertImage(DsoType.NEBULA, DsoType.SUPERNOVA_REMNANT, true);
        // And two families really do draw differently.
        assertImage(DsoType.NEBULA, DsoType.PLANETARY_NEBULA, false);
        assertImage(DsoType.OPEN_CLUSTER, DsoType.GLOBULAR_CLUSTER, false);
    }

    @Test
    void theGalaxyExemplarIsAnEllipseAndItIsTilted() {
        // The gate review's P1: sharing the painter is not enough if
        // the parameters handed to it remove the symbol's defining
        // geometry. Measured from the pixels that were actually
        // drawn, not from the numbers that were asked for.
        BufferedImage drawn = swatch(DsoType.GALAXY);
        double[] shape = principalAxes(drawn);
        double elongation = shape[1] / shape[0];
        double tilt = shape[2];

        assertTrue(elongation < 0.85,
                "the galaxy exemplar must not read as a circle;"
                        + " measured minor/major " + round(elongation));
        assertTrue(tilt > 15.0 && tilt < 75.0,
                "and it must be visibly tilted; measured "
                        + Math.round(tilt) + "°");

        // The round families are round, and that is not an accident
        // of these numbers either.
        assertTrue(principalAxes(swatch(DsoType.GLOBULAR_CLUSTER))[1]
                        / principalAxes(swatch(DsoType.GLOBULAR_CLUSTER))[0]
                        > 0.9,
                "a globular cluster is a circle in the chart and in"
                        + " the legend alike");
    }

    @Test
    void theExemplarsAreThePacksOwnMedianProportions() {
        // The constants in ChartRenderer.legendShapeFor claim to be
        // the pack's median axis ratios. This is where that claim is
        // re-measured against the catalogue rather than trusted.
        List<DeepSkyObject> pack = wholePack();
        for (ChartRenderer.Symbol symbol : new ChartRenderer.Symbol[] {
                ChartRenderer.Symbol.ELLIPSE,
                ChartRenderer.Symbol.DOTTED_CIRCLE,
                ChartRenderer.Symbol.BOX}) {
            List<Double> ratios = new ArrayList<>();
            for (DeepSkyObject dso : pack) {
                if (ChartRenderer.symbolFor(dso) != symbol) {
                    continue;
                }
                Double major = dso.recorded().majorAxisArcmin();
                Double minor = dso.recorded().minorAxisArcmin();
                if (major != null && minor != null && major > 0.0) {
                    ratios.add(minor / major);
                }
            }
            java.util.Collections.sort(ratios);
            double median = ratios.get(ratios.size() / 2);
            double exemplar =
                    ChartRenderer.legendShapeFor(symbol).minorFraction();
            assertEquals(Math.round(median * 20.0) / 20.0, exemplar, 1e-9,
                    symbol + ": the exemplar is the pack's median"
                            + " (" + round(median) + ") to a twentieth");
        }

        // The two the painter draws round whatever the pack says.
        assertEquals(1.0, ChartRenderer.legendShapeFor(
                ChartRenderer.Symbol.CROSSED_CIRCLE).minorFraction());
        assertEquals(1.0, ChartRenderer.legendShapeFor(
                ChartRenderer.Symbol.PLANETARY).minorFraction());
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    /**
     * The major axis, minor axis and tilt of whatever a swatch drew,
     * from the second moments of its ink. A circle returns a ratio of
     * one; the tilt of a circle means nothing and is not asserted.
     */
    private static double[] principalAxes(BufferedImage image) {
        double sumX = 0;
        double sumY = 0;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xff) < 250) {
                    sumX += x;
                    sumY += y;
                    n++;
                }
            }
        }
        double meanX = sumX / n;
        double meanY = sumY / n;
        double xx = 0;
        double yy = 0;
        double xy = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xff) < 250) {
                    xx += (x - meanX) * (x - meanX);
                    yy += (y - meanY) * (y - meanY);
                    xy += (x - meanX) * (y - meanY);
                }
            }
        }
        xx /= n;
        yy /= n;
        xy /= n;
        double half = Math.sqrt(Math.pow((xx - yy) / 2.0, 2) + xy * xy);
        double major = Math.sqrt(Math.max(1e-9, (xx + yy) / 2.0 + half));
        double minor = Math.sqrt(Math.max(1e-9, (xx + yy) / 2.0 - half));
        double tilt = Math.toDegrees(0.5 * Math.atan2(2 * xy, xx - yy));
        return new double[] {major, minor, Math.abs(tilt)};
    }

    @Test
    void anUndrawnTypeGetsNoInventedMarkInTheLegendEither() {
        for (DsoType type : DsoType.values()) {
            boolean inked = inkPixels(swatch(type)) > 0;
            assertEquals(ChartRenderer.symbolForType(type)
                            != ChartRenderer.Symbol.NONE, inked,
                    type + ": the legend draws exactly what the chart"
                            + " draws, including nothing");
        }
    }

    @Test
    void theFiveSymbolsStayApartAtTheSizeALegendRowGives() {
        // The gate measured the nearest pair at 34% of their combined
        // ink at 11 px. This is the claim that would break first if a
        // symbol were redrawn: two families that came to look alike
        // would fall towards 0%.
        List<DsoType> examples = List.of(DsoType.GALAXY,
                DsoType.OPEN_CLUSTER, DsoType.GLOBULAR_CLUSTER,
                DsoType.NEBULA, DsoType.PLANETARY_NEBULA);
        for (int i = 0; i < examples.size(); i++) {
            for (int j = i + 1; j < examples.size(); j++) {
                double apart = difference(swatch(examples.get(i)),
                        swatch(examples.get(j)));
                assertTrue(apart >= 25.0,
                        examples.get(i) + " and " + examples.get(j)
                                + " share too much ink: "
                                + Math.round(apart) + "%");
            }
        }
    }

    @Test
    void groupingIntoFamiliesTakesNothingFromTheSourceType() {
        // The chart draws a lone galaxy and a triplet identically;
        // the catalogue's own word for each must survive that, or the
        // Inspector loses a fact the pack recorded.
        List<DeepSkyObject> pack = wholePack();
        DeepSkyObject galaxy = firstOf(pack, DsoType.GALAXY);
        DeepSkyObject triplet = firstOf(pack, DsoType.GALAXY_TRIPLET);

        assertEquals(ChartRenderer.symbolFor(galaxy),
                ChartRenderer.symbolFor(triplet),
                "one family on the page");
        assertNotEquals(galaxy.type(), triplet.type(),
                "two types in the reader's hands");
        assertEquals(DsoType.GALAXY_TRIPLET, triplet.type(),
                "and the type is the catalogue's, not the family's");
    }

    private static DeepSkyObject firstOf(List<DeepSkyObject> pack,
                                         DsoType type) {
        return pack.stream().filter(dso -> dso.type() == type)
                .findFirst().orElseThrow();
    }

    private static void assertImage(DsoType a, DsoType b, boolean same) {
        boolean identical = identical(swatch(a), swatch(b));
        assertEquals(same, identical,
                a + " and " + b + (same ? " must draw alike"
                        : " must not draw alike"));
        if (!same) {
            assertFalse(identical);
        }
    }

    /** One type's legend symbol, drawn the way the mock-up draws it. */
    private static BufferedImage swatch(DsoType type) {
        BufferedImage image = new BufferedImage(60, 60,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 60, 60);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ChartRenderer.drawLegendSymbol(g, type, 30.0, 30.0, 11.0);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static boolean identical(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int inkPixels(BufferedImage image) {
        int inked = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xff) < 250) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /** The share of inked pixels two symbols do not share. */
    private static double difference(BufferedImage a, BufferedImage b) {
        int differing = 0;
        int inked = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                boolean inkA = (a.getRGB(x, y) & 0xff) < 250;
                boolean inkB = (b.getRGB(x, y) & 0xff) < 250;
                if (inkA || inkB) {
                    inked++;
                    if (inkA != inkB) {
                        differing++;
                    }
                }
            }
        }
        return inked == 0 ? 0.0 : 100.0 * differing / inked;
    }
}
