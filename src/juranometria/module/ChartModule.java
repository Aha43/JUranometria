package juranometria.module;

/**
 * A removable way to read the sky (Sprint 24, issue #215).
 *
 * <blockquote>JUranometria is a complete celestial chart whose
 * modules add removable ways to read the sky. Every module must
 * preserve the chart's independence and express its subject through
 * cartography.</blockquote>
 *
 * <p>A module owns its own domain state, controls, facts and
 * lifecycle. Removing one removes its feature and leaves no weakened
 * chart contract and no module-specific state behind - which is a
 * property the architecture tests assert rather than describe.
 */
public interface ChartModule {

    /** What this module is called, for diagnostics and menus. */
    String name();

    /** Given the chart's services. Called once, before any use. */
    void attach(ChartServices services);

    /**
     * Released. After this the module holds no chart state and
     * receives nothing further; anything it subscribed to is
     * unsubscribed here.
     */
    void detach();
}
