package juranometria.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One truth, two names (issue #260): the services' Sprint 24 view
 * re-addresses exactly the session model - never a second writable
 * state that could disagree.
 */
class OneWorkingSelectionTruthTest {

    @Test
    void theMarksViewAndTheWorkingSelectionAreTheSameState() {
        TestChartServices services = new TestChartServices();
        assertSame(services.workingSelection(),
                services.workingMarks().model(),
                "the adapter is a view of the one model");
        services.workingMarks().mark("M 31");
        assertEquals(java.util.List.of("M 31"),
                services.workingSelection().members(),
                "a write through the old name lands in the one truth");
        services.workingSelection().add("M 32");
        assertEquals(java.util.List.of("M 31", "M 32"),
                services.workingMarks().marks(),
                "and a write through the new name is visible to the"
                        + " old one - because there is only one state");
        assertEquals("M 32", services.workingMarks().lead());
    }
}
