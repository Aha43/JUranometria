package juranometria.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.SkyLanguageChoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What this installation offers the reader (Sprint 33, issue #348).
 *
 * <p>Two registries answering two domains, brought together in one
 * place. {@code Atlas} is that place and the only one: interface
 * languages come from the descriptors that ship, chart languages from
 * the packs that ship, and nothing else in the application holds
 * both.
 *
 * <p>The promise being kept is that the settings are independent. A
 * reader may have the sky in Norwegian and the menus in English,
 * because those are separate choices about separate things - not a
 * half-finished translation. So a chart pack must never appear as an
 * interface language, and this checks the composition as the
 * application actually performs it, not a hand-built stand-in.
 */
class InstalledLanguagesTest {

    @Test
    void theApplicationOffersEachSettingFromItsOwnRegistry() {
        SkyLanguageChoice.Available offered = Atlas.languages();

        assertEquals(Set.of("en"), offered.interfaceLanguages(),
                "the controls speak the languages descriptors ship"
                        + " for");
        assertEquals(Set.of("nb-NO"), offered.chartLanguages(),
                "and the sky can be named in the languages packs ship"
                        + " for");
    }

    /** The two lists do not leak into each other. */
    @Test
    void aChartLanguageIsNotAnInterfaceLanguage() {
        SkyLanguageChoice.Available offered = Atlas.languages();

        assertFalse(offered.interfaceLanguages().contains("nb-NO"),
                "a Norwegian sky does not put Norwegian in the menus:"
                        + " the reader chose one thing, not two");
        assertFalse(offered.chartLanguages().contains("en"),
                "and registering an English interface does not claim"
                        + " the atlas can name the sky in English,"
                        + " which no pack ships names for");
    }

    /**
     * The composition the reader actually gets.
     *
     * <p>A stored Norwegian chart choice, resolved against what the
     * application really offers rather than a fixture: Norwegian sky,
     * English controls, both from their own registry.
     */
    @Test
    void aStoredNorwegianSkyResolvesAgainstWhatIsInstalled() {
        SkyLanguageChoice choice = SkyLanguageChoice.read(
                Map.of(SkyLanguageChoice.INTERFACE_KEY, "en",
                        SkyLanguageChoice.CHART_KEY, "nb-NO"),
                Atlas.languages());

        assertEquals("nb-NO", choice.namesOnTheChart());
        assertEquals("en", choice.interfaceLanguage());
    }

    /**
     * A language this build does not ship is not honoured.
     *
     * <p>Held against the real availability, so it would break the
     * day somebody made discovery answer from something other than
     * the installed resources.
     */
    @Test
    void aLanguageThisBuildDoesNotShipFallsBack() {
        SkyLanguageChoice choice = SkyLanguageChoice.read(
                Map.of(SkyLanguageChoice.INTERFACE_KEY, "sv-SE",
                        SkyLanguageChoice.CHART_KEY, "sv-SE"),
                Atlas.languages());

        assertEquals(SkyLanguageChoice.ENGLISH, choice.interfaceLanguage());
        assertEquals(SkyLanguageChoice.LATIN, choice.namesOnTheChart());
    }

    /**
     * Neither registry knows the other exists.
     *
     * <p>The separation is only structural if the two types cannot
     * consult each other. If {@code InterfaceLanguages} could read a
     * chart pack, every behavioural check above would still pass
     * while the guarantee had quietly become a convention.
     */
    @Test
    void neitherRegistryImportsTheOther() throws IOException {
        String interfaces = Files.readString(Path.of(
                "src/juranometria/ui/language/InterfaceLanguages.java"));
        String charts = Files.readString(Path.of(
                "src/juranometria/geo/SkyNames.java"));

        assertFalse(code(interfaces).contains("SkyNames"),
                "the interface registry cannot consult the chart"
                        + " packs, so a pack can never become an"
                        + " interface language anywhere");
        assertFalse(code(charts).contains("InterfaceLanguages"),
                "and the chart registry cannot consult the interface"
                        + " descriptors");
        assertTrue(interfaces.contains("interface-language")
                        && !code(interfaces).contains("sky-language"),
                "each reads its own directory only");
        assertTrue(charts.contains("sky-language")
                        && !code(charts).contains("interface-language"),
                "in both directions");
    }

    private static String code(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
    }
}
