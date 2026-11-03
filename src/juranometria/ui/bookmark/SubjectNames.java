package juranometria.ui.bookmark;

import java.util.function.Function;

import juranometria.bookmark.Subject;
import juranometria.project.PageWords;
import juranometria.search.LocalSearch;
import juranometria.search.SearchResult;

/**
 * A subject's name as the interface shows it (#681): a body's in the
 * page's language, a star's or a deep-sky object's as the atlas's search
 * labels it. Looked up at each reading; the stored identity is never
 * replaced by a name, and an identity this catalogue does not know is
 * shown as itself.
 */
public final class SubjectNames {

    private SubjectNames() {
    }

    /** The names for this page language and catalogue. */
    public static Function<Subject, String> of(PageWords words, LocalSearch search) {
        return subject -> switch (subject.kind()) {
            case BODY -> words.bodyName(subject.id());
            case STAR, DEEP_SKY -> {
                for (SearchResult result : search.search(subject.id())) {
                    if (result.identity().equals(subject.id())) {
                        yield result.label();
                    }
                }
                yield subject.id();
            }
        };
    }
}
