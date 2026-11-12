package juranometria.ui.bookmark;

import org.junit.jupiter.api.Test;

import juranometria.bookmark.Subject;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A meteor shower as a bookmark's subject (#716, R-20): the version-1
 * {@code body} kind with the shower's identity, named as the page names
 * the shower, and "switched on" by selecting that shower - never by a
 * planet's switch.
 */
class ShowerBookmarkTest {

    @Test
    void aShowerSubjectIsNamedAsThePageNamesTheShower() {
        Subject perseids = new Subject(Subject.Kind.BODY, "meteor-shower.PER");
        assertEquals("Perseids", SubjectNames.of(
                PageText.in(InterfaceText.forLanguage("en")), null).apply(perseids));
        assertEquals("Perseidene", SubjectNames.of(
                PageText.in(InterfaceText.forLanguage("nb-NO")), null).apply(perseids));
    }

    @Test
    void aShowerIsDrawnByItsOwnSelectionAndABodyStillByItsPlanet() {
        assertEquals("meteor-shower.PER", BookmarkOpener.planetOf("meteor-shower.PER"));
        assertEquals("jovian.jupiter", BookmarkOpener.planetOf("jovian.io"));
        assertEquals("saturn", BookmarkOpener.planetOf("saturn.titan"));
        assertEquals("moon", BookmarkOpener.planetOf("moon"));
    }
}
