package juranometria.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeographyDetailPolicyTest {

    @Test
    void theReleasedCloseFieldsStayFreeOfGeography() {
        for (double field : new double[] {1.0, 2.0, 3.0, 4.0, 6.0, 8.0}) {
            GeographyDetailPolicy policy = new GeographyDetailPolicy(field);
            assertFalse(policy.figuresDrawn(), "no figures at " + field);
            assertFalse(policy.namesDrawn(), "no names at " + field);
            assertFalse(policy.boundariesDrawn(), "no boundaries at " + field);
        }
    }

    @Test
    void figuresAndNamesBeginExactlyAtTwelveDegrees() {
        GeographyDetailPolicy at12 = new GeographyDetailPolicy(12.0);
        assertTrue(at12.figuresDrawn());
        assertTrue(at12.namesDrawn());
        assertFalse(at12.boundariesDrawn(),
                "boundaries wait for 18 degrees");
    }

    @Test
    void boundariesBeginExactlyAtEighteenDegrees() {
        GeographyDetailPolicy at18 = new GeographyDetailPolicy(18.0);
        assertTrue(at18.figuresDrawn());
        assertTrue(at18.namesDrawn());
        assertTrue(at18.boundariesDrawn());
        for (double field : new double[] {24.0, 36.0}) {
            GeographyDetailPolicy wide = new GeographyDetailPolicy(field);
            assertTrue(wide.figuresDrawn() && wide.namesDrawn()
                    && wide.boundariesDrawn(), "all layers at " + field);
        }
    }

    @Test
    void anInvalidFieldIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeographyDetailPolicy(0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new GeographyDetailPolicy(Double.NaN));
    }
}
