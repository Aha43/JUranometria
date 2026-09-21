package juranometria.ui.language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What this session shows, and what the next one will start with
 * (Sprint 33, issue #348).
 *
 * <p>Two questions with two answers, deliberately not the same
 * object. The store is read once, at the beginning; from then on the
 * session owns the answer. Nothing in a running application asks the
 * preference node again, so a page cannot change because something
 * wrote to storage without any application state passing through the
 * call - and two exports of one page cannot disagree for a reason
 * nothing recorded.
 */
class SkyLanguageSessionTest {

    private static final String NORWEGIAN = "nb-NO";

    private static final SkyLanguageChoice.Available INSTALLED =
            new SkyLanguageChoice.Available(
                    Set.of(SkyLanguageChoice.ENGLISH),
                    Set.of(NORWEGIAN));

    /** A session starts in what the reader last chose. */
    @Test
    void aSessionStartsInTheLanguageTheReaderLeftItIn() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            store.save(SkyLanguageChoice.read(Map.of(), INSTALLED)
                    .withChart(NORWEGIAN));

            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, INSTALLED);

            assertEquals(NORWEGIAN, session.namesOnTheChart(),
                    "the sky they left it showing");
            assertEquals(SkyLanguageChoice.ENGLISH,
                    session.interfaceLanguage());
        } finally {
            node.removeNode();
        }
    }

    /**
     * The store is read once and never again.
     *
     * <p>The contract the whole type exists for. A store written to
     * behind the session's back must not change what the session is
     * showing: the reader did not choose it, no application state
     * carried it, and a page that moved for that reason could not be
     * accounted for afterwards.
     */
    @Test
    void theStoreIsReadOnceAndNotConsultedAgain() throws Exception {
        Preferences node = scratch();
        try {
            CountingStore store =
                    new CountingStore(SkyLanguageStore.forNode(node));
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, INSTALLED);
            int atStartup = store.reads;

            // Everything a running application asks of it, repeatedly.
            for (int each = 0; each < 5; each++) {
                session.namesOnTheChart();
                session.interfaceLanguage();
                session.current();
                session.available();
            }
            assertEquals(atStartup, store.reads,
                    "a session that re-read the store would make"
                            + " persisted storage an ambient"
                            + " dependency of every page it draws");

            // And a write from elsewhere does not reach this session.
            SkyLanguageStore.forNode(node).save(
                    SkyLanguageChoice.read(Map.of(), INSTALLED)
                            .withChart(NORWEGIAN));
            assertEquals(SkyLanguageChoice.LATIN,
                    session.namesOnTheChart(),
                    "what this session shows changed only when the"
                            + " reader changed it, which nobody did");
        } finally {
            node.removeNode();
        }
    }

    /**
     * Choosing does three things, together.
     *
     * <p>This session shows it, the next session will start with it,
     * and the page is rebuilt. Driven as one call rather than
     * rehearsed as three statements, because three statements that
     * each look right are exactly how the wiring between them goes
     * missing.
     */
    @Test
    void choosingShowsItSavesItAndRebuildsThePage() throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, INSTALLED);
            List<String> rebuilds = new ArrayList<>();
            session.onChange(choice ->
                    rebuilds.add(choice.namesOnTheChart()));

            session.choose(session.current().withChart(NORWEGIAN));

            assertEquals(NORWEGIAN, session.namesOnTheChart(),
                    "this session now shows it");
            assertEquals(Map.of(SkyLanguageChoice.CHART_KEY, NORWEGIAN),
                    store.stated(),
                    "the next session will start with it - and with"
                            + " the interface question still open,"
                            + " because choosing the sky's names did"
                            + " not answer it");
            assertEquals(List.of(NORWEGIAN), rebuilds,
                    "and the page was rebuilt, in the new language."
                            + " Names are resolved into a scene when it"
                            + " is assembled, so a page already drawn"
                            + " cannot show a language it was not"
                            + " built in - a repaint would not do");
        } finally {
            node.removeNode();
        }
    }

    /**
     * Confirming without acting on a selector is not an answer.
     *
     * <p>This test used to assert the opposite, and the behaviour it
     * protected has not been dropped - it has moved to where it can
     * be true. A reader who <em>opens the selector and picks what is
     * already showing</em> has answered, and
     * {@code InterfaceLanguageMigrationTest} holds exactly that
     * through the real control, where the act is visible.
     *
     * <p>What could not be true is the version asserted here:
     * {@code choose(current())} is what the dialog does when the
     * reader touched nothing, and treating it as an answer made
     * every confirmation answer every question in the dialog. An
     * upgrading reader who pressed OK was recorded as having chosen
     * English, which is the one thing the absent key exists to
     * prevent.
     *
     * <p>So the distinction every future default change depends on
     * is still here - it is simply drawn around the act rather than
     * around the dialog.
     */
    @Test
    void confirmingWhatIsAlreadyShowingIsNotAnAnswerOnItsOwn()
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, INSTALLED);
            assertFalse(store.everChosen(), "nobody has been asked");

            session.choose(session.current());

            assertFalse(store.everChosen(),
                    "and nobody has answered yet: confirming a dialog"
                            + " is not acting on every control in it."
                            + " The reader who really does pick the"
                            + " language already showing is held by"
                            + " InterfaceLanguageMigrationTest,"
                            + " through the selector itself");
        } finally {
            node.removeNode();
        }
    }

    /** A session starts already resolved against what is installed. */
    @Test
    void anUninstalledChoiceIsResolvedAtStartupNotAtTheFirstPage()
            throws Exception {
        Preferences node = scratch();
        try {
            node.put(SkyLanguageChoice.CHART_KEY, "sv-SE");
            SkyLanguageSession session = SkyLanguageSession.begin(
                    SkyLanguageStore.forNode(node), INSTALLED);

            assertEquals(SkyLanguageChoice.LATIN,
                    session.namesOnTheChart(),
                    "a language uninstalled since it was chosen falls"
                            + " back where the session begins, not at"
                            + " whichever page first happens to look");
        } finally {
            node.removeNode();
        }
    }

    /** Counts what the session asks of storage. */
    private static final class CountingStore implements SkyLanguageStore {

        private final SkyLanguageStore real;
        private int reads;

        private CountingStore(SkyLanguageStore real) {
            this.real = real;
        }

        @Override
        public Map<String, String> stated() {
            reads++;
            return real.stated();
        }

        @Override
        public void save(SkyLanguageChoice choice) {
            real.save(choice);
        }

        @Override
        public void flush() {
            real.flush();
        }
    }

    private static Preferences scratch() {
        return Preferences.userRoot().node(
                "juranometria-test-sky-language-" + System.nanoTime());
    }
}
