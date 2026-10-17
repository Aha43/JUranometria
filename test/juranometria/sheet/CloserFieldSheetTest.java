package juranometria.sheet;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A closer field says its own width on the page, aloud and in an
 * export (#543, part of #481), and since #544 calls itself a closer field: 0.5 and 0.25, never the "0.3" or "0" the
 * ladder's formats would round it to, in both languages, with the
 * sheet's file name a reader can find again.
 */
class CloserFieldSheetTest {

    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));
    private static final juranometria.project.PageWords NORWEGIAN =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("nb-NO"));

    private static final SkyPosition PLEIADES = new SkyPosition(56.75, 24.12);

    @Test
    void theSpokenPageTheSheetAndItsNameSayACloserFieldsOwnWidth() {
        for (double field : ChartViewState.closerFieldSteps()) {
            String width = field == 0.5 ? "0.5" : "0.25";
            ChartViewState state = new ChartViewState(PLEIADES, field, 8.0);
            ChartScene scene = Atlas.assembler().assemble(state, 900, 700);
            DrawnPage page = DrawnPage.of(scene);

            assertTrue(page.describe(ENGLISH).contains(
                            "Closer field " + width + " degrees wide"),
                    page.describe(ENGLISH));
            assertTrue(page.describe(NORWEGIAN).contains(
                            "Nærfelt " + width + " grader bredt"),
                    page.describe(NORWEGIAN));

            SheetMetadata english = SheetMetadata.of(scene, state,
                    ChartOptions.DEFAULTS, PaperSize.A4, ENGLISH);
            assertTrue(english.description().contains(
                            "Closer field " + width + " degrees wide"),
                    english.description());
            SheetMetadata norwegian = SheetMetadata.of(scene, state,
                    ChartOptions.DEFAULTS, PaperSize.A4, NORWEGIAN);
            assertTrue(norwegian.description().contains(
                            "Nærfelt " + width + " grader bredt"),
                    norwegian.description());

            String name = SheetFileName.suggest(state, scene, SheetFormat.PNG);
            assertTrue(name.endsWith("-" + width + "deg.png"), name);
            assertTrue(name.matches("[a-z0-9.\\-]+"), name);
        }
    }

    @Test
    void theLadderStillPrintsWhatItAlwaysPrinted() {
        ChartViewState state = new ChartViewState(PLEIADES, 1.0, 8.0);
        ChartScene scene = Atlas.assembler().assemble(state, 900, 700);
        assertTrue(DrawnPage.of(scene).describe(ENGLISH)
                .contains("Field 1.0 degrees wide"));
        assertTrue(SheetMetadata.of(scene, state, ChartOptions.DEFAULTS,
                PaperSize.A4, ENGLISH).description()
                .contains("Field 1 degrees wide"));
        assertEquals(true, SheetFileName.suggest(state, scene, SheetFormat.PNG)
                .endsWith("-1deg.png"));
    }
}
