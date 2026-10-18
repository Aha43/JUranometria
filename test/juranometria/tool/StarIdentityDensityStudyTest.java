package juranometria.tool;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.render.ChartRenderer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #557 study's own claims, held where it measures them: ordering C keeps
 * every label the released page places - stars, deep-sky objects and
 * constellation names - and only adds identities in clean room; and the
 * issue's case, Mesarthim, is a star the released policy labels by its
 * letter alone.
 */
class StarIdentityDensityStudyTest {

    private static final ChartRenderer RENDERER = new ChartRenderer(
            juranometria.chart.StarSizePolicy.DEFAULT,
            PageText.in(InterfaceText.forLanguage("en")));

    @Test
    void orderingCKeepsTheReleasedPageAndOnlyAdds() throws Exception {
        for (String star : new String[] {"Alnilam", "Mesarthim"}) {
            for (double field : new double[] {8.0, 24.0, 60.0}) {
                ChartScene scene = Atlas.assembler().assemble(new ChartViewState(
                        StarIdentityDensityStudyMain.find(star), field,
                        ChartViewState.DEFAULT.limitingMagnitude()), 900, 700);
                StarIdentityDensityStudyMain.Measured m =
                        StarIdentityDensityStudyMain.measure(RENDERER, scene);
                String page = star + " " + field + "°";
                assertEquals(m.releasedPlaced(), m.keptC(), page + ": every released star label");
                assertEquals(m.deepSky(), m.deepSkyC(), page + ": every deep-sky label");
                assertEquals(m.names(), m.namesC(), page + ": every constellation name");
                assertEquals(m.offered(), m.placedC() + m.refusedC(),
                        page + ": every extra identity is placed clean or refused");
                long duress = m.c().stream().skip(m.c().size() - m.placedC())
                        .filter(p -> p.underDuress()).count();
                assertEquals(0, duress, page + ": no extra identity under duress");
            }
        }
    }

    @Test
    void mesarthimIsARicherCaseAtTheNarrowFields() throws Exception {
        ChartScene scene = Atlas.assembler().assemble(new ChartViewState(
                StarIdentityDensityStudyMain.find("Mesarthim"), 8.0,
                ChartViewState.DEFAULT.limitingMagnitude()), 900, 700);
        StarIdentityDensityStudyMain.Measured m =
                StarIdentityDensityStudyMain.measure(RENDERER, scene);
        assertTrue(m.c().stream().anyMatch(p -> p.at() != null
                        && p.request().text().startsWith("Mesarthim")),
                "C writes Mesarthim's name");
        assertTrue(m.releasedPlacements().stream().noneMatch(p ->
                        p.request().text().startsWith("Mesarthim")),
                "the released page does not");
    }
}
