package juranometria.chart;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartSceneTest {

    private static final ChartViewport VIEWPORT = new ChartViewport(
            new SkyPosition(10.684708, 41.268750), 8.0, 900, 700);

    @Test
    void aBlankTargetIdentityIsRejectedAtTheSceneBoundary() {
        // PR #59 follow-up: #56's rendering policy matches on this value,
        // so the scene admits a real identity or none - never blank.
        assertThrows(IllegalArgumentException.class, () -> new ChartScene(
                VIEWPORT, List.of(), List.of(), "M31 region", 8.0, "   "));
        assertEquals("NGC 224", new ChartScene(
                VIEWPORT, List.of(), List.of(), "M31 region", 8.0, "NGC 224")
                .targetIdentity());
        assertEquals(null, new ChartScene(
                VIEWPORT, List.of(), List.of(), "M31 region", 8.0)
                .targetIdentity(), "the targetless convenience scene is legal");
    }
}
