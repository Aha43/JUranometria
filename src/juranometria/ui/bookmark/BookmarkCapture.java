package juranometria.ui.bookmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.Place;
import juranometria.bookmark.SavedView;
import juranometria.bookmark.Subject;
import juranometria.chart.ChartViewState;
import juranometria.sky.Observer;

/**
 * What Add Bookmark… proposes from the chart as it stands (#681 stage 2,
 * rulings of 2026-11-02 on #497): the view always; the working selection's
 * members as the subjects, in their order, with its lead as the proposed
 * primary; the chart time offered as an explicit choice - pre-ticked, as
 * event time, exactly when a subject is a Solar System body (C-4); the
 * place offered as an explicit choice, never pre-ticked. Nothing here
 * captures silently: the dialog shows every value and the reader decides.
 *
 * <p>Swing-free, so that the proposal and the draft it makes are tested
 * without a window.
 */
public final class BookmarkCapture {

    /**
     * The proposal the dialog opens with.
     *
     * @param view        the current view: centre and field
     * @param subjects    the working selection's members, in order
     * @param primary     the working selection's lead, if any
     * @param instant     the chart time the box would include
     * @param timeTicked  whether the box starts ticked (C-4)
     * @param place       the chart's place the box would include
     */
    public record Proposal(SavedView view, List<Subject> subjects, Optional<Subject> primary,
                           Instant instant, boolean timeTicked, Place place) {
        public Proposal {
            subjects = List.copyOf(subjects);
        }

        /** Whether any subject is a Solar System body: why the time box starts ticked. */
        public boolean hasBody() {
            return subjects.stream().anyMatch(s -> s.kind() == Subject.Kind.BODY);
        }

        /**
         * Whether the view is the whole sky (#687, R-11): then the
         * chart time and the place are both kept, and the boxes say so.
         */
        public boolean wholeSky() {
            return view.wholeSky();
        }
    }

    private BookmarkCapture() {
    }

    /** The proposal for this view, working selection and chart context. */
    public static Proposal propose(ChartViewState view, List<String> members, String lead,
                                   Observer observer) {
        List<Subject> subjects = new ArrayList<>();
        for (String id : members) {
            subjects.add(subject(id));
        }
        Optional<Subject> primary = lead == null ? Optional.empty() : Optional.of(subject(lead));
        boolean body = subjects.stream().anyMatch(s -> s.kind() == Subject.Kind.BODY);
        // The whole sky stores no centre or field, and keeps its time
        // and place (#687, R-11): the time box starts ticked.
        SavedView saved = view.wholeSky() ? SavedView.wholeSkyView()
                : new SavedView(view.centre().raDegrees(), view.centre().decDegrees(),
                        view.fieldWidthDegrees());
        return new Proposal(saved, subjects, primary, observer.instant(),
                body || view.wholeSky(),
                new Place(observer.latitudeDegrees(), observer.eastLongitudeDegrees()));
    }

    /**
     * The kind of an atlas identity: Tycho-2 stars are {@code TYC …}; the
     * modules' bodies are lower-case words, dotted for moons
     * ({@code moon}, {@code jovian.io}); everything else is the deep-sky
     * catalogue's ({@code NGC 598}, {@code IC 434}).
     */
    public static Subject subject(String identity) {
        if (identity.startsWith("TYC ")) {
            return new Subject(Subject.Kind.STAR, identity);
        }
        if (identity.matches("[a-z]+(\\.[a-z]+)*")) {
            return new Subject(Subject.Kind.BODY, identity);
        }
        return new Subject(Subject.Kind.DEEP_SKY, identity);
    }

    /**
     * The draft the reader confirmed. Subjects are in the order the reader
     * left them; the primary is one of them or none; time and place are
     * included only if their boxes are ticked.
     */
    public static BookmarkLibrary.Draft draft(Proposal proposal, String title,
                                              List<Subject> subjects, Optional<Subject> primary,
                                              boolean includeTime, ChartTime.Purpose purpose,
                                              boolean includePlace, String note) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("a bookmark needs a title");
        }
        Optional<Subject> kept = primary.filter(subjects::contains);
        // A whole-sky bookmark keeps both, whatever the boxes say (#687).
        boolean time = includeTime || proposal.wholeSky();
        boolean where = includePlace || proposal.wholeSky();
        return new BookmarkLibrary.Draft(title.strip(), proposal.view(), subjects, kept,
                time ? Optional.of(new ChartTime(proposal.instant(), purpose)) : Optional.empty(),
                where ? Optional.of(proposal.place()) : Optional.empty(),
                note == null || note.isBlank() ? Optional.empty() : Optional.of(note));
    }
}
