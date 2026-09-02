package juranometria.module;

/**
 * What a module's contributed geometry is <em>for</em>, so the chart
 * can decide how to ink it (Sprint 24, issue #215).
 *
 * <p>The roles are <strong>geometric, not domain</strong>. A role
 * named "working cross" or "meridian" would teach the core the
 * modules it is meant not to know; a role named "interaction" or
 * "reference line" tells it only how restrained the ink should be
 * and where it belongs in the stack. That is what lets one seam
 * serve a table's crosses, a future meridian and a future planetary
 * path without the core learning any of them.
 */
public enum InkRole {

    /**
     * Ink that answers a reader's gesture rather than the sky:
     * restrained, unlabelled, outside label collision policy, and
     * absent from ordinary and reference rendering.
     */
    INTERACTION,

    /**
     * A line of reference the sky does not draw - a meridian, a
     * horizon, the zenith.
     */
    REFERENCE_LINE,

    /** The path of something that moves against the fixed sky. */
    TRACK
}
