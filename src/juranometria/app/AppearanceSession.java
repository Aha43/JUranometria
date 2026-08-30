package juranometria.app;

/**
 * The session's appearance policy, carrying the {@code --dark} launch
 * override as a first-class fact instead of collapsing it into a
 * boolean at startup (Sprint 11 Codex review, P1). The contract:
 *
 * <ul>
 * <li>the saved preference decides an ordinary launch;</li>
 * <li>an active override keeps <em>this whole session</em> dark - the
 *     Settings dialog may persist an explicitly chosen preference for
 *     the next ordinary launch, but cannot turn the overridden
 *     session light;</li>
 * <li>the dialog preselects the <em>saved</em> preference, never the
 *     override's effect, so merely confirming it can never convert
 *     the command-line override into a stored Dark preference.</li>
 * </ul>
 */
public final class AppearanceSession {

    private final AppearanceStore store;
    private final boolean overrideDark;

    public AppearanceSession(AppearanceStore store, boolean overrideDark) {
        if (store == null) {
            throw new IllegalArgumentException("store is required");
        }
        this.store = store;
        this.overrideDark = overrideDark;
    }

    /** The appearance this session starts (and, if overridden, stays) in. */
    public boolean startupDark() {
        return overrideDark || store.storedDark();
    }

    /** Whether the {@code --dark} override pins this session dark. */
    public boolean overrideActive() {
        return overrideDark;
    }

    /** What the Settings dialog preselects: the saved preference only. */
    public boolean savedDark() {
        return store.storedDark();
    }

    /**
     * The production Settings confirmation: persists the explicit
     * choice for future launches and returns the appearance this
     * session must now show - the choice, unless the override pins
     * the session dark.
     */
    public boolean confirmChoice(boolean choseDark) {
        store.save(choseDark ? AppearanceStore.DARK : AppearanceStore.LIGHT);
        return overrideDark || choseDark;
    }
}
