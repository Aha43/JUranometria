package juranometria.render;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The reviewed star-label scale and notation policy at its exact
 * boundaries (docs/decisions/star-identity.md and
 * docs/decisions/bayer-notation.md): which identity form labels a
 * star in each field band, in which notation, the pairing of names
 * with letters, the post-omega Latin hold-back, the priority order,
 * and the searched star's threshold-free label.
 */
class StarLabelPolicyTest {

    static final SkyPosition ANYWHERE = new SkyPosition(0.0, 0.0);
    static final StarIdentity FULL = new StarIdentity(
            "Name", "α", "58", "Ori");
    static final StarIdentity BAYER_ONLY = new StarIdentity(
            null, "β", null, "Ori");
    static final StarIdentity LATIN_ONLY = new StarIdentity(
            null, "b", null, "Ori");
    static final StarIdentity COMPONENT = new StarIdentity(
            "Acrux", "α1", null, "Cru");
    static final StarIdentity FLAMSTEED_ONLY = new StarIdentity(
            null, null, "35", "And");

    private static Star star(double magnitude, StarIdentity identity) {
        return new Star("TYC 1-1-1", ANYWHERE, magnitude, identity);
    }

    @Test
    void componentDigitsAreRaisedFromTheCatalogueValue() {
        // The digits live inside the catalogue's own bayer string;
        // the notation lifts that value's trailing run and never
        // re-derives a designation from rendered text.
        assertEquals("π³", StarLabelPolicy.bayerNotation(
                new StarIdentity(null, "π3", null, "Ori")));
        assertEquals("α¹", StarLabelPolicy.bayerNotation(COMPONENT));
        assertEquals("b¹", StarLabelPolicy.bayerNotation(
                new StarIdentity(null, "b1", null, "Ori")));
        assertEquals("α", StarLabelPolicy.bayerNotation(
                new StarIdentity(null, "α", null, "Ori")),
                "a letter with no component is untouched");
    }

    @Test
    void aDigitsOnlyDesignationIsOmittedNotRenderedBroken() {
        // The data boundary rejects dishonest rows at load; a value
        // that survived yet carries no letter to draw is omitted
        // rather than rendered as a bare floating superscript.
        StarIdentity degenerate = new StarIdentity(null, "1", null, "Ori");
        assertNull(StarLabelPolicy.bayerNotation(degenerate));
        assertNull(new StarLabelPolicy(8.0).labelFor(star(1.0, degenerate)),
                "no letter, no label - never broken notation");
        assertNull(StarLabelPolicy.guaranteedLabelFor(
                        star(1.0, degenerate)),
                "not even for the searched star");
    }

    @Test
    void wideFieldsCarryNamesAndGreekLettersAtTheirLimits() {
        StarLabelPolicy at36 = new StarLabelPolicy(36.0);
        // Names to V 2.5, letters to V 3.5 - both boundaries, both
        // directions.
        assertEquals("Name α", at36.labelFor(star(2.5, FULL)),
                "at the name limit both forms travel together");
        assertEquals("α", at36.labelFor(star(2.51, FULL)),
                "past the name limit the letter carries on alone");
        assertEquals("β", at36.labelFor(star(3.5, BAYER_ONLY)),
                "V 3.5 is exactly inside the wide-field letter limit");
        assertNull(at36.labelFor(star(3.51, BAYER_ONLY)),
                "V 3.51 is exactly outside");
        assertNull(at36.labelFor(star(0.5, FLAMSTEED_ONLY)),
                "no Flamsteed numbers at wide fields, however bright");
        assertEquals("Name α", new StarLabelPolicy(24.0)
                        .labelFor(star(2.5, FULL)),
                "the 24-degree page belongs to the wide band");
    }

    @Test
    void midFieldsRaiseTheLetterLimitOnly() {
        StarLabelPolicy at18 = new StarLabelPolicy(18.0);
        assertEquals("Name α", at18.labelFor(star(3.0, FULL)),
                "names to V 3.0");
        assertEquals("α", at18.labelFor(star(3.01, FULL)));
        assertEquals("β", at18.labelFor(star(4.5, BAYER_ONLY)),
                "letters to V 4.5 in the mid band");
        assertNull(at18.labelFor(star(4.51, BAYER_ONLY)));
        assertNull(at18.labelFor(star(0.5, FLAMSTEED_ONLY)),
                "Flamsteed waits for the regional pages");
        assertEquals("β", new StarLabelPolicy(12.0)
                        .labelFor(star(4.5, BAYER_ONLY)),
                "the 12-degree page belongs to the mid band");
    }

    @Test
    void regionalFieldsCarryEveryFormAtItsLimit() {
        StarLabelPolicy at8 = new StarLabelPolicy(8.0);
        assertEquals("Name α", at8.labelFor(star(4.5, FULL)));
        assertEquals("α", at8.labelFor(star(4.51, FULL)),
                "past the name limit, the letter alone");
        assertEquals("β", at8.labelFor(star(5.0, BAYER_ONLY)));
        assertNull(at8.labelFor(star(5.01, BAYER_ONLY)));
        assertEquals("35", at8.labelFor(star(5.0, FLAMSTEED_ONLY)),
                "the released Flamsteed limit is unchanged");
        assertNull(at8.labelFor(star(5.01, FLAMSTEED_ONLY)));
    }

    @Test
    void postOmegaLatinLettersWaitForTheRegionalFields() {
        // Legitimate Bayer designations that read as stray capitals
        // among Greek on a constellation page.
        assertNull(new StarLabelPolicy(36.0).labelFor(star(1.0, LATIN_ONLY)),
                "no Latin letter at 36 degrees, however bright");
        assertNull(new StarLabelPolicy(12.0).labelFor(star(1.0, LATIN_ONLY)),
                "nor at the bottom of the mid band");
        assertEquals("b", new StarLabelPolicy(8.0)
                        .labelFor(star(4.9, LATIN_ONLY)),
                "Latin letters arrive with the regional detail");
        // A named star with a Latin letter shows the name alone on a
        // constellation page, and the pair when the letter is due.
        StarIdentity namedLatin = new StarIdentity("Namedstar", "b", null, "Ori");
        assertEquals("Namedstar",
                new StarLabelPolicy(36.0).labelFor(star(2.0, namedLatin)));
        assertEquals("Namedstar b",
                new StarLabelPolicy(8.0).labelFor(star(4.0, namedLatin)));
    }

    @Test
    void namesAndLettersTravelTogetherIncludingComponents() {
        assertEquals("Acrux α¹",
                new StarLabelPolicy(18.0).labelFor(star(1.3, COMPONENT)),
                "the pair carries the raised component");
        assertEquals("Name α",
                new StarLabelPolicy(36.0).labelFor(star(0.5, FULL)));
    }

    @Test
    void priorityFallsFromNameThroughLetterToNumber() {
        StarLabelPolicy at8 = new StarLabelPolicy(8.0);
        assertEquals("Name α", at8.labelFor(star(4.0, FULL)),
                "name and letter first, together");
        assertEquals("α", at8.labelFor(star(4.8, FULL)),
                "past the name limit the letter alone carries the star");
        // The letter and number limits coincide at 8 degrees, so a
        // lettered star never falls through to its number: the
        // Flamsteed form exists for stars that have no letter at all.
        assertEquals("β", at8.labelFor(star(5.0, BAYER_ONLY)));
        assertNull(at8.labelFor(star(5.01, FULL)),
                "past every limit, a fully identified star is silent");
        assertEquals("35", at8.labelFor(star(5.0, FLAMSTEED_ONLY)),
                "the number speaks for a star with nothing else");
        assertNull(at8.labelFor(new Star("TYC 1-1-1", ANYWHERE, 1.0)),
                "no identity, no label - never invented");
        assertNull(at8.labelFor(star(7.0, FULL)), "past every limit");
    }

    @Test
    void theGuaranteedLabelIsTheBestIdentityFreeOfThresholds() {
        assertEquals("Name α",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, FULL)),
                "the searched star shows the pair it has");
        assertEquals("Acrux α¹",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, COMPONENT)));
        assertEquals("β",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, BAYER_ONLY)));
        assertEquals("b",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, LATIN_ONLY)),
                "the hold-back is a density rule, not an identity rule:"
                        + " a searched Latin-lettered star still names"
                        + " itself");
        assertEquals("35", StarLabelPolicy.guaranteedLabelFor(
                star(9.0, FLAMSTEED_ONLY)));
        assertNull(StarLabelPolicy.guaranteedLabelFor(
                        new Star("TYC 1-1-1", ANYWHERE, 1.0)),
                "a searched star with no identity gains no label");
    }
}
