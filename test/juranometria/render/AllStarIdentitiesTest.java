package juranometria.render;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.search.SearchResult;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * All star identities where they fit (#558), held to the rulings on #557
 * (docs/decisions/star-identity-density.md) on the pages the study
 * accepted: off, the page is the released page; on, every label the
 * released page places is still placed - each star with its full identity -
 * and the extra identities take only clean room; the three identity
 * switches still choose the forms; bare Flamsteed numbers stay below 12°.
 */
class AllStarIdentitiesTest {

    private static final ChartRenderer RENDERER = new ChartRenderer(
            juranometria.chart.StarSizePolicy.DEFAULT,
            PageText.in(InterfaceText.forLanguage("en")));

    private static SkyPosition star(String name) {
        return Atlas.search().search(name).stream()
                .filter(r -> r.kind() == SearchResult.Kind.STAR).findFirst()
                .orElseThrow().position();
    }

    private static ChartScene page(String star, double field) {
        return Atlas.assembler().assemble(new ChartViewState(star(star), field,
                ChartViewState.DEFAULT.limitingMagnitude()), 900, 700);
    }

    private static List<LabelPlacement.Placement> placed(ChartScene scene,
                                                          ChartOptions options) {
        return RENDERER.textPlacements(ChartRenderer.TextMetrics.offscreen(), scene,
                options);
    }

    @Test
    void offIsTheReleasedPageAndTheDefaultIsOff() {
        assertFalse(ChartOptions.DEFAULTS.allStarIdentities(),
                "off by default (ruling 1)");
        ChartScene scene = page("Mesarthim", 8.0);
        assertEquals(placed(scene, ChartOptions.DEFAULTS),
                placed(scene, ChartOptions.DEFAULTS.withAllStarIdentities(false)));
    }

    @Test
    void onKeepsEveryReleasedLabelAndAddsOnlyIntoCleanRoom() {
        for (String star : List.of("Alnilam", "Mesarthim", "Sadr", "Acamar")) {
            for (double field : new double[] {8.0, 24.0, 60.0}) {
                ChartScene scene = page(star, field);
                List<LabelPlacement.Placement> released = placed(scene,
                        ChartOptions.DEFAULTS);
                List<LabelPlacement.Placement> dense = placed(scene,
                        ChartOptions.DEFAULTS.withAllStarIdentities(true));
                Set<String> releasedPlaced = released.stream()
                        .filter(p -> p.at() != null).map(p -> p.request().family()
                                + "/" + p.request().id()).collect(Collectors.toSet());
                Set<String> densePlaced = dense.stream()
                        .filter(p -> p.at() != null).map(p -> p.request().family()
                                + "/" + p.request().id()).collect(Collectors.toSet());
                assertTrue(densePlaced.containsAll(releasedPlaced), star + " " + field
                        + "°: every star, deep-sky and constellation label the released"
                        + " page places is placed (ruling 2)");
                for (LabelPlacement.Placement extra
                        : dense.subList(released.size(), dense.size())) {
                    assertFalse(extra.underDuress(), star + " " + field + "°: "
                            + extra.request().text() + " is never forced (ruling 3)");
                    assertEquals(LabelPlacement.Family.STAR, extra.request().family());
                }
            }
        }
    }

    @Test
    void mesarthimIsNamedOnTheAriesPage() {
        ChartScene scene = page("Mesarthim", 8.0);
        assertTrue(placed(scene, ChartOptions.DEFAULTS).stream().noneMatch(
                p -> p.at() != null && p.request().text().startsWith("Mesarthim")),
                "the released page writes only its letter");
        assertTrue(placed(scene, ChartOptions.DEFAULTS.withAllStarIdentities(true))
                        .stream().anyMatch(p -> p.at() != null
                                && p.request().text().equals("Mesarthim γ¹")),
                "the density writes its name with its letter");
    }

    @Test
    void theThreeSwitchesStillChooseTheForms() {
        ChartScene scene = page("Alnilam", 8.0);
        ChartOptions on = ChartOptions.DEFAULTS.withAllStarIdentities(true);
        ChartOptions noNames = new ChartOptions(true, true, true, true, true, false,
                true, true, true, true, false, true, true, true, true, true,
                ChartPalette.WHITE_PAPER, true);
        for (LabelPlacement.Placement p : placed(scene, noNames)) {
            if (p.at() != null && p.request().family() == LabelPlacement.Family.STAR) {
                assertFalse(p.request().text().matches(".*[A-Z][a-z]{2,}.*"),
                        "names off: no name - " + p.request().text());
            }
        }
        ChartOptions none = new ChartOptions(true, true, true, true, true, false,
                false, false, true, true, false, true, true, true, true, true,
                ChartPalette.WHITE_PAPER, true);
        assertFalse(none.effectiveAllStarIdentities(),
                "all three off: nothing to widen (ruling 1)");
        assertTrue(placed(scene, none).stream().noneMatch(p -> p.at() != null
                        && p.request().family() == LabelPlacement.Family.STAR),
                "and no star label at all");
        assertTrue(on.effectiveAllStarIdentities());
    }

    @Test
    void bareFlamsteedNumbersStayBelowTwelveDegrees() {
        for (double field : new double[] {12.0, 24.0, 60.0}) {
            for (LabelPlacement.Placement p : placed(page("Alnilam", field),
                    ChartOptions.DEFAULTS.withAllStarIdentities(true))) {
                if (p.at() != null && p.request().family() == LabelPlacement.Family.STAR) {
                    assertFalse(p.request().text().matches("\\d+"),
                            field + "°: no bare number - " + p.request().text()
                                    + " (ruling 4)");
                }
            }
        }
        assertTrue(placed(page("Mesarthim", 8.0),
                        ChartOptions.DEFAULTS.withAllStarIdentities(true)).stream()
                        .anyMatch(p -> p.at() != null && p.request().text().matches("\\d+")),
                "below 12° the numbers join");
    }
}
