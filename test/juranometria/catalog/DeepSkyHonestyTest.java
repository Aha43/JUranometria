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
    void theSilenceOfTheCatalogueSurvivesLoading() {
        List<DeepSkyObject> objects = everything();
        assertTrue(objects.size() > 5000,
                "the sweep must cover most of the pack: "
                        + objects.size());

        long noPositionAngle = objects.stream()
                .filter(dso -> !dso.recorded().hasPositionAngle()).count();
        long noSize = objects.stream()
                .filter(dso -> !dso.recorded().hasSize()).count();
        long noVisual = objects.stream()
                .filter(dso -> dso.recorded().band()
                        != DeepSkyObject.Recorded.Band.VISUAL).count();
        long blueStandingIn = objects.stream()
                .filter(dso -> dso.recorded().band()
                        == DeepSkyObject.Recorded.Band.BLUE).count();

        double paShare = 100.0 * noPositionAngle / objects.size();
        double sizeShare = 100.0 * noSize / objects.size();
        double visualShare = 100.0 * noVisual / objects.size();
        double blueShare = 100.0 * blueStandingIn / objects.size();

        assertTrue(paShare > 10.0 && paShare < 30.0,
                "the gate measured 19.4% with no position angle: "
                        + paShare);
        assertTrue(sizeShare > 4.0 && sizeShare < 16.0,
                "and 9.7% with no recorded extent: " + sizeShare);
        assertTrue(visualShare > 58.0 && visualShare < 78.0,
                "and 68.1% with no V magnitude: " + visualShare);
        assertTrue(blueShare > 44.0 && blueShare < 64.0,
                "of which 54.4% carry a B magnitude instead: "
                        + blueShare);
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
