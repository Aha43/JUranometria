package juranometria.tool;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The black-sky derivation, pinned (Sprint 26, issue #246): the
 * committed {@code ChartPalette.BLACK_SKY} is exactly what the
 * equal-contrast rule derives from the white-paper palette - the
 * verify-icons pattern, in the suite so a drifted value or a
 * drifted rule fails every push, not only a study run.
 */
class BlackSkyStudyTest {

    @Test
    void everyBlackSkyInkIsTheEqualContrastPartnerOfItsPaperInk() {
        record Pair(String name, int paper, int black) {
        }
        for (Pair role : new Pair[] {
                new Pair("star", ChartPalette.WHITE_PAPER.starInk().getRed(),
                        ChartPalette.BLACK_SKY.starInk().getRed()),
                new Pair("text", ChartPalette.WHITE_PAPER.textInk().getRed(),
                        ChartPalette.BLACK_SKY.textInk().getRed()),
                new Pair("frame", ChartPalette.WHITE_PAPER.frameInk().getRed(),
                        ChartPalette.BLACK_SKY.frameInk().getRed()),
                new Pair("figure", ChartPalette.WHITE_PAPER.figureInk().getRed(),
                        ChartPalette.BLACK_SKY.figureInk().getRed()),
                new Pair("boundary",
                        ChartPalette.WHITE_PAPER.boundaryInk().getRed(),
                        ChartPalette.BLACK_SKY.boundaryInk().getRed()),
                new Pair("constellation name",
                        ChartPalette.WHITE_PAPER.constellationNameInk().getRed(),
                        ChartPalette.BLACK_SKY.constellationNameInk().getRed()),
                new Pair("galaxy fill",
                        ChartPalette.WHITE_PAPER.galaxyFill().getRed(),
                        ChartPalette.BLACK_SKY.galaxyFill().getRed()),
                new Pair("deep-sky outline",
                        ChartPalette.WHITE_PAPER.deepSkyOutline().getRed(),
                        ChartPalette.BLACK_SKY.deepSkyOutline().getRed()),
                new Pair("nebula outline",
                        ChartPalette.WHITE_PAPER.nebulaOutline().getRed(),
                        ChartPalette.BLACK_SKY.nebulaOutline().getRed()),
                new Pair("grid", ChartPalette.WHITE_PAPER.gridInk().getRed(),
                        ChartPalette.BLACK_SKY.gridInk().getRed()),
                new Pair("grid label",
                        ChartPalette.WHITE_PAPER.gridLabelInk().getRed(),
                        ChartPalette.BLACK_SKY.gridLabelInk().getRed()),
                new Pair("selection",
                        ChartPalette.WHITE_PAPER.selectionInk().getRed(),
                        ChartPalette.BLACK_SKY.selectionInk().getRed()),
                new Pair("interaction",
                        ChartPalette.WHITE_PAPER.interactionInk().getRed(),
                        ChartPalette.BLACK_SKY.interactionInk().getRed())}) {
            assertEquals(
                    BlackSkyStudyMain.equalContrastOnBlack(role.paper()),
                    role.black(),
                    role.name() + ": the pinned black-sky value is"
                            + " what the rule derives from paper grey "
                            + role.paper());
        }
    }

    @Test
    void theGroundsAreExactAndTheNebulaKeepsItsFloor() {
        assertEquals(java.awt.Color.WHITE,
                ChartPalette.WHITE_PAPER.ground());
        assertEquals(java.awt.Color.BLACK,
                ChartPalette.BLACK_SKY.ground());
        double floor = BlackSkyStudyMain.contrast(
                ChartPalette.BLACK_SKY.nebulaOutline().getRed(), 0);
        assertTrue(floor >= 3.0,
                "the nebula box keeps the 3:1 floor the deep-sky"
                        + " vocabulary decision set: " + floor);
    }

    @Test
    void theStoredTokenRoundTripsAndTheUnknownMeansWhitePaper() {
        assertEquals(ChartPalette.BLACK_SKY,
                ChartPalette.stored(
                        ChartPalette.BLACK_SKY.storedAs()));
        assertEquals(ChartPalette.WHITE_PAPER,
                ChartPalette.stored(
                        ChartPalette.WHITE_PAPER.storedAs()));
        assertEquals(ChartPalette.WHITE_PAPER,
                ChartPalette.stored(null),
                "an absent token is the released chart");
        assertEquals(ChartPalette.WHITE_PAPER,
                ChartPalette.stored("BLACK-SKY"),
                "tokens are exact; anything else falls back rather"
                        + " than guessing");
    }
}
