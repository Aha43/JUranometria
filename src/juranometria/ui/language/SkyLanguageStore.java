package juranometria.ui.language;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * The reader's two language choices, remembered (Sprint 33, #348).
 *
 * <p>Two keys, settled by the gate and not re-decided here:
 *
 * <pre>
 *   language.interface = en | nb-NO | …whatever is installed
 *   language.chart     = follow-interface | latin | …whatever pack is found
 * </pre>
 *
 * <h2>Absence is a third state</h2>
 *
 * <p>A store with neither key is a reader who has <strong>never been
 * asked</strong>, and that is not the same as a reader who chose
 * {@code follow-interface}. They draw the same page today, which is
 * exactly what makes the 2.0 upgrade invisible; they stay apart so
 * that a future default change can migrate the first explicitly
 * instead of quietly redefining what somebody's silence meant.
 *
 * <p><strong>Reading writes nothing.</strong> A reader who opens the
 * atlas and never touches the setting keeps a store with no language
 * keys in it, so their silence survives to be migrated rather than
 * being answered on their behalf the first time the application
 * starts.
 *
 * <p><strong>Saving writes what was settled.</strong> This once
 * wrote both keys whenever either was chosen; per-key consent
 * replaced that, because confirming a dialog is not acting on every
 * control in it. Persisting half a
 * choice would leave the other an absence, and absence is reserved
 * for "never asked" - a reader who picked a chart language would
 * otherwise be recorded as never having been asked about the
 * interface.
 *
 * <p>Same discipline as {@code EclipticStore} and
 * {@code ChartOptionsStore}: an interface over an explicit node, so
 * tests use dedicated nodes and never a developer's real
 * preferences.
 */
public interface SkyLanguageStore {

    /** What the store holds, unresolved: absent keys stay absent. */
    Map<String, String> stated();

    /** Remembers a choice - each key only if it was settled. */
    void save(SkyLanguageChoice choice);

    /** Whether the reader has ever chosen either setting. */
    default boolean everChosen() {
        Map<String, String> stated = stated();
        return stated.containsKey(SkyLanguageChoice.INTERFACE_KEY)
                || stated.containsKey(SkyLanguageChoice.CHART_KEY);
    }

    /**
     * What to use: the reader's choice, resolved against what this
     * installation can offer.
     *
     * <p>A value this build cannot offer - a language uninstalled
     * since it was chosen, a hand-edited store, a store written by a
     * later version - resolves deterministically to English and
     * {@code follow-interface} rather than being obeyed. It is never
     * handed to {@code Locale.getDefault()} and never taken as an
     * arbitrary language tag.
     */
    default SkyLanguageChoice choice(SkyLanguageChoice.Available available) {
        return SkyLanguageChoice.read(stated(), available);
    }

    /** Pushes saved values out, so a fresh session can read them. */
    void flush();

    /** The store the running application uses. */
    static SkyLanguageStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** An implementation over an explicit node; tests use a test node. */
    static SkyLanguageStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException(
                    "a store is kept somewhere");
        }
        return new SkyLanguageStore() {

            @Override
            public Map<String, String> stated() {
                Map<String, String> stated = new LinkedHashMap<>();
                // Read with no default and put nothing back. An
                // absent key must stay absent: the moment a read
                // writes a default, the reader's silence has been
                // answered for them and can never be migrated.
                put(stated, SkyLanguageChoice.INTERFACE_KEY);
                put(stated, SkyLanguageChoice.CHART_KEY);
                return stated;
            }

            private void put(Map<String, String> stated, String key) {
                String held = node.get(key, null);
                if (held != null) {
                    stated.put(key, held);
                }
            }

            @Override
            public void save(SkyLanguageChoice choice) {
                if (choice == null) {
                    throw new IllegalArgumentException(
                            "a saved choice is a choice");
                }
                choice.toStore().forEach(node::put);
            }

            @Override
            public void flush() {
                try {
                    node.flush();
                } catch (BackingStoreException cannotFlush) {
                    // The same posture the other stores take: a
                    // preference that will not persist must not stop
                    // a reader using the atlas. They lose the memory
                    // of a choice, not the choice itself.
                    throw new IllegalStateException(
                            "could not save the language choice",
                            cannotFlush);
                }
            }
        };
    }
}
