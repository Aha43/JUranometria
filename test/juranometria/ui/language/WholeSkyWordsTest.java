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
        // The whole line, in both languages: a prefix pin let the fragment
        // "..., the zenith's" / "..., senits" pass to the owner's hands.
        assertEquals("Central meridian: zenith's ecliptic longitude 65.0° · Stars to V 5.0",
                EN.wholeSkyTitleFacts("65.0", "5.0"));
        assertEquals("Sentralmeridian: senits ekliptiske lengde 65.0° · Stjerner til V 5.0",
                NB.wholeSkyTitleFacts("65.0", "5.0"));
    }

    @Test
    void thePageSpeaksItsOwnSentenceAndNeverTheOrdinaryClaims() {
        ChartScene scene = Atlas.assembler().assemble(
                ChartViewState.wholeSky(HammerProjection.centreFor(65.0), 8.0), 2000, 1000);
        DrawnPage page = DrawnPage.of(scene);
        String spoken = page.describe(EN);
        assertTrue(spoken.startsWith("Whole sky. Hammer projection, ecliptic north up,"
                + " ecliptic longitude increases left at centre."), spoken);
        assertTrue(spoken.contains("Central meridian at the zenith's ecliptic longitude,"
                + " 65.0 degrees. Stars to V 5.0."), spoken);
        assertFalse(spoken.contains("ICRS") || spoken.contains("Field ")
                || spoken.contains("Centre RA"), "no ordinary claim: " + spoken);
        String norsk = page.describe(NB);
        assertTrue(norsk.startsWith("Hele himmelen. Hammer-projeksjon, ekliptisk nord opp,"
                + " ekliptisk lengde øker mot venstre i midten."), norsk);
        assertTrue(norsk.contains("Sentralmeridian ved senits ekliptiske lengde, 65.0 grader."
                + " Stjerner til V 5.0."), norsk);
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
        assertTrue(sheet.contains(" Central meridian at the zenith's ecliptic longitude,"
                + " 65.0 degrees. Stars to V 5.0. A4 landscape. Ground: white-paper."), sheet);
        String norsk = NB.wholeSkySheetDescription("Hammer", "65.0", "5.0",
                "A4 i liggende format", "svart himmel");
        assertTrue(norsk.contains(" Sentralmeridian ved senits ekliptiske lengde, 65.0 grader."
                + " Stjerner til V 5.0. A4 i liggende format. Bakgrunn: svart himmel."), norsk);
    }
}
