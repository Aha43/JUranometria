package juranometria.ui.language;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.project.DrawnPage;
import juranometria.project.HammerProjection;
import juranometria.project.PageWords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole sky's words (#687, ruled R-12 on #686): the ruled sentence
 * in both languages, the central ecliptic longitude stated, and never
 * the ordinary chart's Centre or Field claims.
 */
class WholeSkyWordsTest {

    private static final PageWords EN = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageWords NB = PageText.in(InterfaceText.forLanguage("nb-NO"));

    @Test
    void theRuledSentenceInBothLanguages() {
        assertEquals("Hammer", EN.projection("hammer"));
        assertEquals("Hammer", NB.projection("hammer"));
        assertEquals("Whole sky · Hammer · ecliptic north up · ecliptic longitude increases"
                + " left at centre", EN.wholeSkyTitleLine(EN.projection("hammer")));
        assertEquals("Hele himmelen · Hammer · ekliptisk nord opp · ekliptisk lengde øker"
                + " mot venstre i midten", NB.wholeSkyTitleLine(NB.projection("hammer")));
        assertEquals("Whole sky", EN.wholeSkySubject());
        assertEquals("Hele himmelen", NB.wholeSkySubject());
        String facts = EN.wholeSkyTitleFacts("65.0", "5.0");
        assertTrue(facts.contains("65.0°") && facts.contains("V 5.0"), facts);
        assertTrue(facts.toLowerCase().contains("zenith"), "the central-meridian rule: " + facts);
        String nb = NB.wholeSkyTitleFacts("65.0", "5.0");
        assertTrue(nb.contains("65.0°") && nb.contains("V 5.0") && nb.contains("senit"), nb);
    }

    @Test
    void thePageSpeaksItsOwnSentenceAndNeverTheOrdinaryClaims() {
        ChartScene scene = Atlas.assembler().assemble(
                ChartViewState.wholeSky(HammerProjection.centreFor(65.0), 8.0), 2000, 1000);
        DrawnPage page = DrawnPage.of(scene);
        String spoken = page.describe(EN);
        assertTrue(spoken.startsWith("Whole sky. Hammer projection, ecliptic north up,"
                + " ecliptic longitude increases left at centre."), spoken);
        assertTrue(spoken.contains("Central meridian at ecliptic longitude 65.0 degrees"), spoken);
        assertTrue(spoken.contains("Stars to V 5.0"), spoken);
        assertFalse(spoken.contains("ICRS") || spoken.contains("Field ")
                || spoken.contains("Centre RA"), "no ordinary claim: " + spoken);
        String norsk = page.describe(NB);
        assertTrue(norsk.startsWith("Hele himmelen. Hammer-projeksjon, ekliptisk nord opp,"
                + " ekliptisk lengde øker mot venstre i midten."), norsk);
        assertFalse(norsk.contains("Sentrum RA") || norsk.contains("Felt "), norsk);
        assertEquals("65.0", DrawnPage.centralLongitudeText(
                page.projection().wholeSphere().orElseThrow()));
        assertTrue(page.bounded(), "an edge to its sky");
    }

    @Test
    void theSheetDescriptionStatesTheSameFacts() {
        String sheet = EN.wholeSkySheetDescription("Hammer", "65.0", "5.0",
                "A4 landscape", "white-paper");
        assertTrue(sheet.startsWith("Whole sky. Hammer, ecliptic north up, ecliptic longitude"
                + " increases left at centre."), sheet);
        assertTrue(sheet.contains("65.0 degrees") && sheet.contains("V 5.0")
                && sheet.contains("A4 landscape") && sheet.contains("white-paper"), sheet);
    }
}
