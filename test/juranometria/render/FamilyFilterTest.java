package juranometria.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five deep-sky families as the reader's own switches (Sprint 21,
 * issue #185).
 *
 * <p>A family is one drawn symbol, so what a family flag governs is
 * exactly the set of marks the chart draws that way. These are the
 * boundaries that would let it go wrong: a family that hides
 * something else's marks, a label that outlives its symbol, a
 * searched target lost behind a switch, or a symbol-less type given a
 * mark it never had.
 */
class FamilyFilterTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    /** A page carrying members of several families at once. */
    private static ChartScene sagittarius() {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(271.0, -24.0), 8.0, 8.0, null, null),
                900, 700);
    }

    private static ChartScene m31() {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(10.68, 41.27), 8.0, 8.0, null, null),
                900, 700);
    }

    /** The deep-sky marks a page draws, counted by family. */
    private static Map<SymbolFamily, Integer> marksByFamily(
            ChartScene scene, ChartOptions options) {
        Map<SymbolFamily, Integer> counts =
                new EnumMap<>(SymbolFamily.class);
        for (SymbolFamily family : SymbolFamily.values()) {
            counts.put(family, 0);
        }
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, options)) {
            if (mark.deepSky() == null) {
                continue;
            }
            SymbolFamily family = SymbolFamily.of(mark.deepSky());
            assertTrue(family != null,
                    "a drawn mark always belongs to a family: "
                            + mark.deepSky().id());
            counts.merge(family, 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void eachFamilyHidesItsOwnMarksAndNobodyElsesOnEveryPage() {
        for (ChartScene scene : List.of(m31(), sagittarius())) {
            Map<SymbolFamily, Integer> all =
                    marksByFamily(scene, ChartOptions.DEFAULTS);
            for (SymbolFamily off : SymbolFamily.values()) {
                Map<SymbolFamily, Integer> some = marksByFamily(scene,
                        ChartOptions.DEFAULTS.withFamily(off, false));
                assertEquals(0, some.get(off),
                        off + " switched off draws nothing");
                for (SymbolFamily other : SymbolFamily.values()) {
                    if (other != off) {
                        assertEquals(all.get(other), some.get(other),
                                "switching off " + off + " left "
                                        + other + " alone");
                    }
                }
            }
        }
    }

    @Test
    void theMasterGovernsAndTheFamiliesRemember() {
        ChartScene scene = sagittarius();
        ChartOptions galaxiesOff =
                ChartOptions.DEFAULTS.withFamily(SymbolFamily.GALAXIES,
                        false);
        ChartOptions masterOff = new ChartOptions(false,
                galaxiesOff.deepSkyLabels(),
                galaxiesOff.constellationFigures(),
                galaxiesOff.constellationBoundaries(),
                galaxiesOff.constellationNames(), galaxiesOff.starNames(),
                galaxiesOff.bayerLetters(), galaxiesOff.flamsteedNumbers(),
                galaxiesOff.equatorialGrid(), galaxiesOff.titleBlock(),
                galaxiesOff.magnitudeKey(), galaxiesOff.galaxies(),
                galaxiesOff.openClusters(), galaxiesOff.globularClusters(),
                galaxiesOff.nebulae(), galaxiesOff.planetaryNebulae());

        for (SymbolFamily family : SymbolFamily.values()) {
            assertEquals(0, marksByFamily(scene, masterOff).get(family),
                    "the master off draws no " + family);
        }
        // The flags kept their values while ineffective, so the
        // reader gets back the chart they had rather than a reset.
        assertFalse(masterOff.galaxies(), "galaxies stayed switched off");
        assertTrue(masterOff.nebulae(), "and the others stayed on");
        assertFalse(masterOff.effectiveFamily(ChartRenderer.Symbol.BOX),
                "though none of them draws while the master is off");
    }

    @Test
    void everyCombinationKeepsLabelledInsideDrawn() {
        // 2^5 family combinations times the master times the label
        // permission: the invariant is not that labels are hidden, it
        // is that a label never outlives the symbol it names.
        // Both sides are the renderer's own published decisions -
        // the sets the drawing itself iterates - so this compares
        // what the page does, not a second copy of the rules.
        ChartScene scene = sagittarius();
        for (int bits = 0; bits < 32; bits++) {
            for (boolean master : new boolean[] {true, false}) {
                for (boolean labels : new boolean[] {true, false}) {
                    ChartOptions options = combination(bits, master,
                            labels);
                    List<String> drawn = new ArrayList<>();
                    for (DeepSkyObject dso
                            : RENDERER.drawnDeepSky(scene, options)) {
                        drawn.add(dso.id());
                    }
                    for (DeepSkyObject labelled
                            : RENDERER.labelledDeepSky(scene, options)) {
                        assertTrue(drawn.contains(labelled.id()),
                                labelled.id() + " is labelled but not"
                                        + " drawn, with " + options);
                    }
                    // And the published marks are the drawn set, less
                    // whatever the paper clips away.
                    for (ChartRenderer.DrawnMark mark
                            : RENDERER.drawnMarks(scene, options)) {
                        if (mark.deepSky() != null) {
                            assertTrue(drawn.contains(mark.deepSky().id()),
                                    mark.deepSky().id() + " is marked"
                                            + " but not drawn");
                        }
                    }
                }
            }
        }
    }

    private static ChartOptions combination(int bits, boolean master,
                                            boolean labels) {
        return new ChartOptions(master, labels, true, true, true, true,
                true, true, true, true, false,
                (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0,
                (bits & 8) != 0, (bits & 16) != 0);
    }

    @Test
    void aSearchedTargetSurvivesItsOwnFamilyBeingSwitchedOff() {
        // The standing honesty rule since Sprint 12: a chart that
        // names a target draws it. A family switch is one more thing
        // that must not make the title block lie.
        for (SymbolFamily family : SymbolFamily.values()) {
            DeepSkyObject example = null;
            ChartScene page = null;
            for (ChartScene scene : List.of(m31(), sagittarius())) {
                // A mark the page really draws, so that losing it
                // afterwards can only be the family switch's doing.
                for (ChartRenderer.DrawnMark mark
                        : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
                    if (mark.deepSky() != null
                            && SymbolFamily.of(mark.deepSky()) == family) {
                        example = mark.deepSky();
                        page = scene;
                        break;
                    }
                }
                if (example != null) {
                    break;
                }
            }
            assertTrue(example != null, "a page drawing a " + family);

            ChartScene targeted = Atlas.assembler().assemble(
                    new ChartViewState(page.viewport().centre(),
                            page.viewport().fieldWidthDegrees(), 8.0,
                            example.id(), example.id()), 900, 700);
            ChartOptions hidden = ChartOptions.DEFAULTS
                    .withFamily(family, false);
            String id = example.id();
            assertTrue(RENDERER.drawnMarks(targeted, hidden).stream()
                            .anyMatch(mark -> mark.deepSky() != null
                                    && mark.deepSky().id().equals(id)),
                    family + " hidden must still draw the target " + id);

            ChartOptions masterOff = new ChartOptions(false, true, true,
                    true, true, true, true, true, true, true, false,
                    false, false, false, false, false);
            assertTrue(RENDERER.drawnMarks(targeted, masterOff).stream()
                            .anyMatch(mark -> mark.deepSky() != null
                                    && mark.deepSky().id().equals(id)),
                    "and so must the master switched off");
        }
    }

    @Test
    void aSymbollessTypeIsNeverGivenAMarkByAnySwitch() {
        // The five undrawn classes: novae, stellar and double-star
        // entries, associations, and OpenNGC's unclassified rows.
        // No combination of options, and no target exemption, may
        // invent a mark the chart has never had.
        for (DsoType type : new DsoType[] {DsoType.NOVA, DsoType.STAR,
                DsoType.DOUBLE_STAR, DsoType.STELLAR_ASSOCIATION,
                DsoType.OTHER}) {
            assertEquals(ChartRenderer.Symbol.NONE,
                    ChartRenderer.symbolForType(type));
            assertEquals(null, SymbolFamily.of(type),
                    type + " belongs to no family, so no switch"
                            + " reaches it");
            assertFalse(ChartOptions.DEFAULTS.effectiveFamily(
                            ChartRenderer.symbolForType(type)),
                    type + " draws nothing even with everything on");
        }

        // And in the page itself: a symbol-less object named as the
        // target is still not drawn.
        ChartScene scene = sagittarius();
        DeepSkyObject symbolless = scene.deepSkyObjects().stream()
                .filter(dso -> !ChartRenderer.hasSymbol(dso))
                .findFirst().orElse(null);
        if (symbolless != null) {
            ChartScene targeted = Atlas.assembler().assemble(
                    new ChartViewState(scene.viewport().centre(), 8.0, 8.0,
                            symbolless.id(), symbolless.id()), 900, 700);
            String id = symbolless.id();
            assertTrue(RENDERER.drawnMarks(targeted, ChartOptions.DEFAULTS)
                            .stream().noneMatch(mark ->
                                    mark.deepSky() != null
                                            && mark.deepSky().id()
                                                    .equals(id)),
                    "the target exemption grants no invented mark");
        }
    }

    @Test
    void hitTestingSeesExactlyWhatThePageDraws() {
        // drawnMarks is the one published list, so a family switched
        // off cannot leave a clickable ghost behind.
        ChartScene scene = sagittarius();
        ChartOptions noNebulae =
                ChartOptions.DEFAULTS.withFamily(SymbolFamily.NEBULAE, false);
        ChartHitTest hitTest = new ChartHitTest(RENDERER);
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.deepSky() == null
                    || SymbolFamily.of(mark.deepSky())
                            != SymbolFamily.NEBULAE) {
                continue;
            }
            ChartHitTest.Hit hit = hitTest.at(scene, noNebulae,
                    mark.centre().x(), mark.centre().y());
            boolean namesIt = hit != null && hit.candidates().stream()
                    .anyMatch(candidate -> candidate.catalogueId()
                            .equals(mark.deepSky().id()));
            assertFalse(namesIt,
                    "a hidden nebula must not be selectable: "
                            + mark.deepSky().id());
        }
    }

    @Test
    void aFamilyToggleIsRepaintOnly() {
        // Options are presentation: the same assembled scene answers
        // for every combination, so no toggle can reach the
        // catalogue, the assembler, or navigation.
        ChartScene scene = sagittarius();
        ChartScene same = scene;
        for (SymbolFamily family : SymbolFamily.values()) {
            RENDERER.drawnMarks(scene,
                    ChartOptions.DEFAULTS.withFamily(family, false));
            assertSame(same, scene,
                    "the scene is never rebuilt for " + family);
        }
        assertEquals(scene.deepSkyObjects().size(),
                same.deepSkyObjects().size(),
                "and the scene's contents are untouched by any option");
    }
}
