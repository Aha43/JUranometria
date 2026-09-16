package juranometria.ui.language;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What language this session is showing (Sprint 33, issue #348).
 *
 * <p>The store answers <em>what the next session starts with</em>.
 * This answers <em>what this session currently shows</em>. Keeping
 * them apart is the whole point of the type, and both alternatives
 * were worse:
 *
 * <ul>
 *   <li>Resolving the language from the store at each assembly would
 *       make persisted storage an ambient runtime dependency. A page
 *       could come out different because something wrote to the
 *       preference node, with no application state having passed
 *       through the call - and two exports of "the same" page could
 *       disagree for a reason nothing recorded.
 *   <li>Freezing the choice inside {@code Atlas}'s static holder
 *       would make live selection need invalidation, and would have
 *       the composition root read preferences during class
 *       initialisation, adding both a door to the reader's settings
 *       and an order dependency at startup.
 * </ul>
 *
 * <p>So the store is read <strong>once</strong>, at startup, into a
 * choice this object owns. Changing the selector updates that choice,
 * saves both keys, and tells whoever is listening to rebuild the
 * page. Nothing else reads the store for the rest of the session.
 *
 * <p>The same shape as {@code AppearanceSession} and
 * {@code EclipticSession}: session-start policy stated in a place a
 * reader of the code can find, rather than spread through a
 * constructor and a menu handler.
 */
public final class SkyLanguageSession {

    private final SkyLanguageStore store;

    private final SkyLanguageChoice.Available available;

    private final List<Consumer<SkyLanguageChoice>> listeners =
            new ArrayList<>();

    private SkyLanguageChoice current;

    private SkyLanguageSession(SkyLanguageStore store,
                               SkyLanguageChoice.Available available,
                               SkyLanguageChoice current) {
        this.store = store;
        this.available = available;
        this.current = current;
    }

    /**
     * Reads the reader's remembered choice, once.
     *
     * <p>Resolved here against what this installation can offer, so
     * a language uninstalled since it was chosen becomes the fallback
     * at startup rather than at the first page that happens to look.
     */
    public static SkyLanguageSession begin(
            SkyLanguageStore store,
            SkyLanguageChoice.Available available) {
        if (store == null || available == null) {
            throw new IllegalArgumentException(
                    "a session needs a store to start from and the"
                            + " languages this build can offer");
        }
        return new SkyLanguageSession(store, available,
                store.choice(available));
    }

    /** What this session is showing, both settings. */
    public SkyLanguageChoice current() {
        return current;
    }

    /** What this installation can offer, for a selector to list. */
    public SkyLanguageChoice.Available available() {
        return available;
    }

    /**
     * The chart language this session's pages are drawn in.
     *
     * <p>Already resolved: {@code follow-interface} has been
     * followed, and an unavailable value has fallen back. A caller
     * hands this to scene assembly explicitly and needs to know
     * nothing about how it was decided.
     */
    public String namesOnTheChart() {
        return current.namesOnTheChart();
    }

    /** The language this session's controls speak. */
    public String interfaceLanguage() {
        return current.interfaceLanguage();
    }

    /**
     * What the reader chose, from now until they choose again.
     *
     * <p>Three effects, in one place so the chain is a named thing a
     * test can drive rather than three statements that each look
     * right alone: this session now shows the choice, the next
     * session will start with it, and the page is rebuilt.
     *
     * <p>Both keys are saved, never one - absence is reserved for
     * "never asked", and a reader who has just settled a language
     * must not be recorded as never having been asked about the
     * other.
     *
     * <p>Choosing what is already showing still saves and still
     * notifies. A reader who opens the selector and confirms the
     * current language has been asked and has answered, and that is
     * a different state from never having been asked - which is the
     * distinction the whole migration contract rests on.
     */
    public void choose(SkyLanguageChoice chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException(
                    "a chosen language is a choice");
        }
        current = chosen;
        store.save(chosen);
        store.flush();
        for (Consumer<SkyLanguageChoice> listener : List.copyOf(listeners)) {
            listener.accept(chosen);
        }
    }

    /**
     * Told when the language changes, so a page can be rebuilt.
     *
     * <p>Reconstruction rather than repaint: the names are resolved
     * into the scene when it is assembled, so a page already built
     * cannot show a language it was not built in.
     */
    public void onChange(Consumer<SkyLanguageChoice> listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "a listener is something to tell");
        }
        listeners.add(listener);
    }
}
