package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement study measures what a reader gets (issue #349).
 *
 * <p>It used not to. The study read {@code nb-NO.tsv} itself and
 * overlaid the names onto a rebuilt {@code ChartScene}, so it never
 * touched {@code SkyNames}, the generated index, or the fallback
 * rule. Every figure it recorded described a faithful <em>equivalent</em>
 * of what the atlas draws rather than what the atlas draws - the same
 * shape of error as a study that measures a density production never
 * reaches.
 *
 * <p>The equivalence was real, and measuring it is what made the
 * conversion safe: across the study's pages the two name maps agreed
 * on every entry, in the same order. That is asserted here rather
 * than remembered, from the reviewed catalogue read independently, so
 * three things all have to stay true together - discovery finds the
 * shipped pack, the fallback leaves nothing unnamed, and the study
 * still goes through both.
 *
 * <p>A return to the bypass therefore stops being a quiet change of
 * subject and becomes a failing test.
 */
class SkyLanguagePlacementPathTest {

    private static final String NORWEGIAN = "nb-NO";

    /** The reviewed catalogue, read without going through discovery. */
    private static final Path NAMES =
            Path.of("src/resources/sky-language/nb-NO.tsv");

    /**
     * Production naming agrees with the reviewed catalogue, page by
     * page, on every page the study promises.
     */
    @Test
    void theStudysPagesAreNamedByProductionDiscovery() throws Exception {
        Map<String, String> reviewed = reviewed();
        assertEquals(88, reviewed.size(),
                "the premise: the reviewed catalogue names all 88");

        List<String> disagreements = new ArrayList<>();
        int compared = 0;
        TreeSet<String> everNamed = new TreeSet<>();

        for (Map.Entry<String, ChartViewState> page : pages().entrySet()) {
            ChartScene latin = Atlas.assemblerNamedIn(
                            juranometria.geo.SkyNames.LATIN)
                    .assemble(page.getValue(), 1400, 900);
            ChartScene norsk = Atlas.assemblerNamedIn(NORWEGIAN)
                    .assemble(page.getValue(), 1400, 900);

            Map<String, String> expected = new LinkedHashMap<>();
            latin.geography().latinNames().forEach((id, official) ->
                    expected.put(id, reviewed.getOrDefault(id, official)));
            Map<String, String> production = norsk.geography().latinNames();

            compared += production.size();
            everNamed.addAll(production.keySet());
            for (String id : expected.keySet()) {
                if (!expected.get(id).equals(production.get(id))) {
                    disagreements.add(page.getKey() + " " + id
                            + ": catalogue \"" + expected.get(id)
                            + "\", production \"" + production.get(id)
                            + "\"");
                }
            }
            assertEquals(new ArrayList<>(expected.keySet()),
                    new ArrayList<>(production.keySet()),
                    "and in the identities' own order, on "
                            + page.getKey() + " - iteration order"
                            + " reaches the renderer");
        }

        assertEquals(List.of(), disagreements,
                "every name the atlas draws is the reviewed one,"
                        + " resolved through discovery and fallback"
                        + " rather than copied past them");
        assertTrue(compared > 400,
                "over a corpus worth the name: " + compared
                        + " names compared");
        assertEquals(88, everNamed.size(),
                "and the pages between them name all 88 identities,"
                        + " so no accidental fallback can hide behind"
                        + " a convenient choice of page: " + everNamed.size());
    }

    /**
     * The study's own page set, not a copy of it.
     *
     * <p>Read from the generator, so adding or losing a fixture there
     * changes what this checks. A private list here would agree with
     * the study only until somebody edited one of them.
     */
    private static Map<String, ChartViewState> pages() throws Exception {
        java.lang.reflect.Field field =
                SkyLanguageStudyMain.class.getDeclaredField("PAGES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ChartViewState> pages =
                (Map<String, ChartViewState>) field.get(null);
        assertTrue(pages.size() >= 17,
                "the study promises seventeen pages: " + pages.size());
        return pages;
    }

    private static Map<String, String> reviewed() throws Exception {
        Map<String, String> names = new LinkedHashMap<>();
        for (String line : Files.readAllLines(NAMES)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            if (columns.length != 2 || columns[0].equals("IAU")) {
                continue;
            }
            names.put(columns[0].strip(), columns[1].strip());
        }
        return names;
    }
}
