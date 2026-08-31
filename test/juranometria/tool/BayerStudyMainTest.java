package juranometria.tool;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Bayer-Flamsteed notation contract of
 * docs/decisions/bayer-notation.md, locked before production
 * implementation (#154): conventional notation built from the
 * STRUCTURED identity, the band thresholds, the name-and-letter
 * pairing, the post-omega Latin hold-back, and the priority order.
 */
class BayerStudyMainTest {

    private static final SkyPosition ANYWHERE = new SkyPosition(0.0, 0.0);

    private static Star star(double magnitude, String name, String bayer,
                             String flamsteed) {
        return new Star("TYC 1-1-1", ANYWHERE, magnitude,
                new StarIdentity(name, bayer, flamsteed, "Ori"));
    }

    @Test
    void componentDigitsRiseFromTheStructuredIdentity() {
        // The digits are part of the identity's structured bayer
        // value (the catalogue string "π1", not a separate field);
        // the notation lifts that value's trailing digit run, and
        // never re-derives a designation from rendered label text.
        assertEquals("π³", notation("π3"));
        assertEquals("α¹", notation("α1"));
        assertEquals("θ¹", notation("θ1"));
        assertEquals("μ²", notation("μ2"));
        assertEquals("b¹", notation("b1"), "Latin letters raise too");
        assertEquals("α", notation("α"), "a plain letter is untouched");
        assertEquals("A", notation("A"));
        // Every digit the pack uses has a superscript form.
        for (int digit = 1; digit <= 9; digit++) {
            String raised = notation("π" + digit);
            assertEquals(2, raised.codePointCount(0, raised.length()),
                    "letter plus one raised digit: " + raised);
            assertTrue(!raised.contains(String.valueOf(digit)),
                    "the ASCII digit is gone: " + raised);
        }
    }

    private static String notation(String bayer) {
        return BayerStudyMain.bayerNotation(
                new StarIdentity(null, bayer, null, "Ori"));
    }

    @Test
    void wideFieldsCarryGreekLettersAtTheDecidedLimit() {
        // The headline change: letters reach the constellation pages.
        assertEquals("γ", text(star(3.5, null, "γ", null), 36.0));
        assertNull(text(star(3.51, null, "γ", null), 36.0),
                "V 3.51 is outside the wide-field letter limit");
        assertEquals("γ", text(star(4.5, null, "γ", null), 18.0));
        assertNull(text(star(4.51, null, "γ", null), 18.0));
        assertEquals("γ", text(star(5.0, null, "γ", null), 8.0));
        assertNull(text(star(5.01, null, "γ", null), 8.0));
    }

    @Test
    void postOmegaLatinLettersWaitForTheRegionalFields() {
        // Legitimate Bayer designations, but they read as stray
        // capitals among Greek on a constellation page.
        assertNull(text(star(2.0, null, "b", null), 36.0),
                "no Latin letter at 36 degrees, however bright");
        assertNull(text(star(2.0, null, "A", null), 18.0),
                "nor at 18");
        assertEquals("b", text(star(4.9, null, "b", null), 8.0),
                "Latin letters arrive with the regional detail");
    }

    @Test
    void namesAndLettersTravelTogetherWhenBothQualify() {
        assertEquals("Betelgeuse α",
                text(star(0.5, "Betelgeuse", "α", "58"), 36.0));
        assertEquals("Acrux α¹",
                text(star(1.3, "Acrux", "α1", null), 18.0),
                "the pair carries the raised component too");
        // Only the name qualifies at this magnitude and field.
        assertEquals("Namedstar",
                text(star(2.5, "Namedstar", "χ", null), 36.0)
                        .replace(" χ", ""),
                "a name alone when the letter is past its limit");
        // Only the letter qualifies.
        assertEquals("γ", text(star(3.4, null, "γ", null), 36.0));
    }

    @Test
    void priorityFallsFromNameThroughLetterToFlamsteed() {
        // Flamsteed keeps its released regional-only limit and is the
        // last resort - the decision's measured weakest notation.
        assertNull(text(star(4.9, null, null, "58"), 18.0),
                "no Flamsteed numbers outside the regional fields");
        assertEquals("58", text(star(4.9, null, null, "58"), 8.0));
        assertNull(text(star(5.01, null, null, "58"), 8.0),
                "the released V 5.0 Flamsteed limit is unchanged");
        assertEquals("γ", text(star(4.9, null, "γ", "58"), 8.0),
                "a letter outranks a number");
        assertNull(text(new Star("TYC 1-1-1", ANYWHERE, 1.0), 8.0),
                "no identity, no label - never invented");
    }

    private static String text(Star star, double field) {
        String[] label = BayerStudyMain.labelFor(star,
                BayerStudyMain.PROPOSED, field);
        return label == null ? null : label[0];
    }
}
