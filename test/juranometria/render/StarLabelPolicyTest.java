package juranometria.render;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The reviewed star-label scale policy at its exact boundaries
 * (docs/decisions/star-identity.md): which identity form labels a
 * star in each field band, priority between forms, and the searched
 * star's threshold-free best identity.
 */
class StarLabelPolicyTest {

    static final SkyPosition ANYWHERE = new SkyPosition(0.0, 0.0);
    static final StarIdentity FULL = new StarIdentity(
            "Name", "α", "58", "Ori");
    static final StarIdentity BAYER_ONLY = new StarIdentity(
            null, "β", null, "Ori");
    static final StarIdentity FLAMSTEED_ONLY = new StarIdentity(
            null, null, "35", "And");

    private static Star star(double magnitude, StarIdentity identity) {
        return new Star("TYC 1-1-1", ANYWHERE, magnitude, identity);
    }

    @Test
    void wideFieldsCarryOnlyBrightProperNames() {
        StarLabelPolicy at36 = new StarLabelPolicy(36.0);
        assertEquals("Name", at36.labelFor(star(2.0, FULL)),
                "V 2.0 is exactly inside the wide-field name limit");
        assertNull(at36.labelFor(star(2.01, FULL)),
                "V 2.01 is exactly outside");
        assertNull(at36.labelFor(star(0.0, BAYER_ONLY)),
                "no Bayer at wide fields, however bright");
        assertNull(at36.labelFor(star(0.0, FLAMSTEED_ONLY)),
                "no Flamsteed at wide fields");
        assertEquals("Name", new StarLabelPolicy(24.0)
                        .labelFor(star(2.0, FULL)),
                "the 24-degree page belongs to the wide band");
    }

    @Test
    void midFieldsAddBayerAtTheSharedLimit() {
        StarLabelPolicy at18 = new StarLabelPolicy(18.0);
        assertEquals("Name", at18.labelFor(star(3.0, FULL)),
                "names to V 3.0");
        assertEquals("β", at18.labelFor(star(3.0, BAYER_ONLY)),
                "Bayer to V 3.0, the Greek letter as itself");
        assertNull(at18.labelFor(star(3.01, BAYER_ONLY)));
        assertNull(at18.labelFor(star(0.0, FLAMSTEED_ONLY)),
                "Flamsteed waits for the regional pages");
        assertEquals("β", new StarLabelPolicy(12.0)
                        .labelFor(star(3.0, BAYER_ONLY)),
                "the 12-degree page belongs to the mid band");
    }

    @Test
    void regionalFieldsCarryAllThreeFormsAtTheirLimits() {
        StarLabelPolicy at8 = new StarLabelPolicy(8.0);
        assertEquals("Name", at8.labelFor(star(4.5, FULL)));
        assertEquals("β", at8.labelFor(star(4.5, BAYER_ONLY)));
        assertNull(at8.labelFor(star(4.51, BAYER_ONLY)));
        assertEquals("35", at8.labelFor(star(5.0, FLAMSTEED_ONLY)),
                "Flamsteed to V 5.0");
        assertNull(at8.labelFor(star(5.01, FLAMSTEED_ONLY)));
    }

    @Test
    void priorityIsNameThenBayerThenFlamsteed() {
        StarLabelPolicy at8 = new StarLabelPolicy(8.0);
        assertEquals("Name", at8.labelFor(star(4.0, FULL)),
                "the proper name outranks both designations");
        // Between the name and Bayer limits nothing changes at 8
        // degrees (they share 4.5); between Bayer and Flamsteed the
        // Flamsteed form steps in for a star that has one.
        assertEquals("58", at8.labelFor(star(4.8, FULL)),
                "past the name/Bayer limit the Flamsteed number labels");
        assertNull(at8.labelFor(star(4.8, BAYER_ONLY)),
                "a star with no Flamsteed has nothing left to say");
    }

    @Test
    void starsWithoutIdentityOrBelowEveryLimitStaySilent() {
        assertNull(new StarLabelPolicy(8.0).labelFor(
                new Star("TYC 1-1-1", ANYWHERE, 1.0)),
                "no identity, no label - never invented");
        assertNull(new StarLabelPolicy(8.0).labelFor(star(7.0, FULL)));
    }

    @Test
    void theGuaranteedLabelIsTheBestIdentityFreeOfThresholds() {
        assertEquals("Name",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, FULL)));
        assertEquals("β",
                StarLabelPolicy.guaranteedLabelFor(star(9.0, BAYER_ONLY)));
        assertEquals("35", StarLabelPolicy.guaranteedLabelFor(
                star(9.0, FLAMSTEED_ONLY)));
        assertNull(StarLabelPolicy.guaranteedLabelFor(
                new Star("TYC 1-1-1", ANYWHERE, 1.0)),
                "a searched star with no identity gains no label");
    }
}
