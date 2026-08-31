package juranometria.catalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the pack recorded survives loading (issue #169).
 *
 * <p>The gate measured that 19.4% of deep-sky rows record no position
 * angle and 68.1% record no V magnitude, and that the loader used to
 * discard both facts - substituting a nominal size, a position angle
 * of exactly zero, and an unlabelled V-or-B magnitude. An inspector
 * built on that would state a size nobody measured and a blue
 * magnitude labelled visual for most of the catalogue.
 *
 * <p>These are the gate's numbers, executed against the loaded model
 * rather than the raw file, so the distinction cannot quietly go
 * missing again between the pack and the application.
 */
class DeepSkyHonestyTest {

    private static List<DeepSkyObject> everything() {
        TiledCatalogue catalogue = TiledCatalogue.load();
        List<DeepSkyObject> all = new ArrayList<>();
        // The fixed 30-degree tiling, swept whole.
        for (int ra = 0; ra < 360; ra += 20) {
            for (int dec = -80; dec <= 80; dec += 20) {
                all.addAll(catalogue.deepSkyObjectsIn(new SkyRegion(
                        new SkyPosition(ra, dec), 15.0)));
            }
        }
        return all.stream().distinct().toList();
    }

    @Test
    void theSilenceOfTheCatalogueSurvivesLoadingExactly() {
        // Exact counts, not ranges (review, P2). Percentage bands
        // wide enough to be safe are also wide enough to hide a data
        // regression: a pack that lost a thousand position angles
        // would still have passed "between 10% and 30%". These are
        // the pinned pack's own numbers, and they agree row for row
        // with the gate's measurement of the raw CSV.
        List<DeepSkyObject> objects = everything();

        assertEquals(13371, objects.size(),
                "the sweep reaches the whole bundled pack");
        assertEquals(2596, objects.stream()
                        .filter(dso -> !dso.recorded().hasPositionAngle())
                        .count(),
                "rows recording no position angle (19.4%)");
        assertEquals(1300, objects.stream()
                        .filter(dso -> !dso.recorded().hasSize()).count(),
                "rows recording no extent (9.7%)");
        assertEquals(2279, objects.stream()
                        .filter(dso -> dso.recorded().minorAxisArcmin() == null)
                        .count(),
                "rows recording no minor axis (17.0%)");
        assertEquals(4268, byBand(objects,
                        DeepSkyObject.Recorded.Band.VISUAL),
                "rows with a visual magnitude");
        assertEquals(7276, byBand(objects,
                        DeepSkyObject.Recorded.Band.BLUE),
                "rows where a blue magnitude stands in (54.4%)");
        assertEquals(1827, byBand(objects,
                        DeepSkyObject.Recorded.Band.NONE),
                "rows with no photometry at all (13.7%)");
        assertEquals(objects.size(),
                byBand(objects, DeepSkyObject.Recorded.Band.VISUAL)
                        + byBand(objects, DeepSkyObject.Recorded.Band.BLUE)
                        + byBand(objects, DeepSkyObject.Recorded.Band.NONE),
                "and every object has exactly one band");
    }

    private static long byBand(List<DeepSkyObject> objects,
                               DeepSkyObject.Recorded.Band band) {
        return objects.stream()
                .filter(dso -> dso.recorded().band() == band).count();
    }

    @Test
    void aSubstitutedValueIsNeverReportedAsARecordedOne() {
        for (DeepSkyObject dso : everything()) {
            if (!dso.recorded().hasPositionAngle()) {
                assertEquals(0.0, dso.positionAngleDegrees(),
                        "the display value is the documented"
                                + " substitution");
            }
            if (dso.recorded().hasPositionAngle()) {
                assertEquals(dso.recorded().positionAngleDegrees(),
                        dso.positionAngleDegrees(), 1e-9,
                        "and where the source spoke, the two agree");
            }
            if (dso.recorded().hasSize()) {
                assertEquals(dso.recorded().majorAxisArcmin(),
                        dso.majorAxisArcmin(), 1e-9,
                        "a recorded extent is drawn as recorded");
            } else {
                assertTrue(dso.majorAxisArcmin() > 0.0,
                        "and an absent one still draws something");
            }
            assertEquals(Double.isNaN(dso.magnitude()),
                    dso.recorded().band()
                            == DeepSkyObject.Recorded.Band.NONE,
                    "a magnitude and its band always agree: "
                            + dso.id());
        }
    }

    @Test
    void wellRecordedObjectsAreStillFullyDescribed() {
        // The other half of honesty: the pack's best rows must not be
        // reported as unknown either.
        DeepSkyObject m31 = everything().stream()
                .filter(dso -> dso.aliases().contains("M 31"))
                .findFirst().orElseThrow();

        assertTrue(m31.recorded().hasSize(),
                "M 31's extent is recorded");
        assertTrue(m31.recorded().hasPositionAngle(),
                "and its orientation");
        assertEquals(DeepSkyObject.Recorded.Band.VISUAL,
                m31.recorded().band(),
                "and its magnitude is a visual one");
    }
}
