package juranometria.ui.bookmark;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.Node;
import juranometria.bookmark.Place;
import juranometria.bookmark.SavedView;
import juranometria.bookmark.Subject;
import juranometria.chart.SelectionModel;
import juranometria.meridian.MeridianModule;
import juranometria.search.LocalSearch;
import juranometria.search.SearchResult;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.BodyOnChart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opening a bookmark (#681 stage 3; rulings of 2026-11-02 on #497,
 * docs/decisions/bookmarks.md section 4), on the atlas's own controller,
 * chart, module host, page inventory, chart context and working
 * selection, without a window: the view always; place and time only when
 * stored, in one installation; the reader's limit kept; a deep-sky or a
 * star primary drawn as the single target and selected; no primary, no
 * change of selection; an unknown identity reported, never substituted;
 * a subject's Solar System switch turned on; a closer field only where
 * offered.
 */
class BookmarkOpenerTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));
    private static final LocalSearch SEARCH = Atlas.search();

    private static SearchResult find(String query) {
        return SEARCH.search(query).get(0);
    }

    /** A bookmark-opening bench over the real pipeline. */
    private static final class Bench {
        ChartViewController controller;
        ChartComponent chart;
        ChartModuleHost host;
        MeridianModule meridian;
        int installs;
        boolean closerOffered;
        final Map<String, FakeSwitch> switches = new HashMap<>();
        BookmarkOpener opener;

        BookmarkOpener.Report open(Node.Bookmark bookmark) throws Exception {
            BookmarkOpener.Report[] out = new BookmarkOpener.Report[1];
            SwingUtilities.invokeAndWait(() -> out[0] = opener.open(bookmark));
            return out[0];
        }
    }

    private static Bench bench() throws Exception {
        Bench b = new Bench();
        b.switches.put("moon", new FakeSwitch());
        b.switches.put("jovian.jupiter", new FakeSwitch());
        SwingUtilities.invokeAndWait(() -> {
            b.controller = new ChartViewController();
            b.chart = new ChartComponent(Atlas.assembler(), PageText.in(InterfaceText.forLanguage("en")));
            b.chart.setSize(900, 700);
            b.controller.onChange(b.chart::setViewState);
            b.chart.setViewState(b.controller.state());
            b.host = new ChartModuleHost(b.chart, new SelectionModel(), request -> { });
            b.meridian = b.host.attach(new MeridianModule(OSLO));
            b.meridian.context().onChange(change -> b.installs++);
            Map<String, BodyOnChart> switches = new HashMap<>(b.switches);
            b.opener = new BookmarkOpener(b.controller, b.meridian, b.host.workingSelection(),
                    b.host::inventory, () -> {
                        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        b.chart.paint(g);
                        g.dispose();
                    }, candidate -> b.closerOffered, switches,
                    subject -> SEARCH.search(subject.id()).stream()
                            .filter(r -> r.identity().equals(subject.id()))
                            .map(SearchResult::label).findFirst());
        });
        return b;
    }

    private static Node.Bookmark bookmark(SearchResult at, double field, List<Subject> subjects,
                                          Subject primary, ChartTime time, Place place) {
        return new Node.Bookmark("b", "t", new SavedView(at.position().raDegrees(),
                at.position().decDegrees(), field), subjects, Optional.ofNullable(primary),
                Optional.ofNullable(time), Optional.ofNullable(place), Optional.empty());
    }

    @Test
    void aDeepSkyPrimaryOpensAsTheTargetIsSelectedAndTheLimitIsKept() throws Exception {
        Bench b = bench();
        SwingUtilities.invokeAndWait(() -> {
            b.controller.decreaseMagnitudeLimit();
            b.controller.decreaseMagnitudeLimit();
        });
        double limit = b.controller.state().limitingMagnitude();
        SearchResult m33 = find("M 33");
        Subject dso = new Subject(Subject.Kind.DEEP_SKY, m33.identity());
        BookmarkOpener.Report report = b.open(bookmark(m33, 2, List.of(dso), dso, null, null));
        assertEquals(m33.position(), b.controller.state().centre());
        assertEquals(2.0, b.controller.state().fieldWidthDegrees());
        assertEquals(limit, b.controller.state().limitingMagnitude(), "the reader's limit");
        assertEquals(m33.identity(), b.controller.state().targetIdentity(), "the named target");
        assertTrue(report.selected());
        assertEquals(m33.identity(), b.host.workingSelection().lead());
        assertEquals(BookmarkOpener.Outcome.DRAWN, report.subjects().get(0).outcome());
        assertEquals(0, b.installs, "no stored place or time: the context is not touched");
        assertEquals(OSLO, b.meridian.observer());
    }

    @Test
    void aStarPrimaryFainterThanTheLimitIsDrawnAsTheTargetAndANonPrimaryIsNot() throws Exception {
        Bench b = bench();
        SwingUtilities.invokeAndWait(() -> {
            while (b.controller.state().limitingMagnitude() > 4.0) {
                b.controller.decreaseMagnitudeLimit();
            }
        });
        SearchResult pleione = find("Pleione");
        Subject star = new Subject(Subject.Kind.STAR, pleione.identity());
        BookmarkOpener.Report asPrimary = b.open(bookmark(pleione, 2, List.of(star), star, null, null));
        assertEquals(4.0, b.controller.state().limitingMagnitude(), "the limit is not raised");
        assertEquals(BookmarkOpener.Outcome.DRAWN, asPrimary.subjects().get(0).outcome(),
                "C-3: the primary star is drawn as the single target");
        assertTrue(asPrimary.selected());

        Bench c = bench();
        SwingUtilities.invokeAndWait(() -> {
            while (c.controller.state().limitingMagnitude() > 4.0) {
                c.controller.decreaseMagnitudeLimit();
            }
        });
        BookmarkOpener.Report asSubject = c.open(bookmark(pleione, 2, List.of(star), null, null, null));
        assertEquals(BookmarkOpener.Outcome.FAINTER_THAN_LIMIT, asSubject.subjects().get(0).outcome(),
                "a non-primary star fainter than the limit is reported, not forced");
        assertFalse(asSubject.selected());
    }

    @Test
    void withoutAPrimaryTheSelectionIsNotTouchedAndPlaceAndTimeGoInOnce() throws Exception {
        Bench b = bench();
        SearchResult aldebaran = find("Aldebaran");
        SearchResult m33 = find("M 33");
        SwingUtilities.invokeAndWait(() -> b.host.workingSelection()
                .replaceWith(List.of(aldebaran.identity()), aldebaran.identity()));
        Instant then = Instant.parse("2026-11-24T11:54:00Z");
        BookmarkOpener.Report report = b.open(bookmark(m33, 8,
                List.of(new Subject(Subject.Kind.DEEP_SKY, m33.identity())), null,
                new ChartTime(then, ChartTime.Purpose.EVENT), new Place(78.223, 15.647)));
        assertFalse(report.selected());
        assertEquals(aldebaran.identity(), b.host.workingSelection().lead(), "no primary: unchanged");
        assertEquals(1, b.installs, "place and time in one installation");
        assertEquals(new Observer(78.223, 15.647, then), b.meridian.observer());
    }

    @Test
    void onlyTheStoredPartOfTheContextChanges() throws Exception {
        Bench b = bench();
        SearchResult m33 = find("M 33");
        Instant then = Instant.parse("2026-11-24T11:54:00Z");
        b.open(bookmark(m33, 8, List.of(), null, new ChartTime(then, ChartTime.Purpose.OBSERVATION), null));
        assertEquals(new Observer(OSLO.latitudeDegrees(), OSLO.eastLongitudeDegrees(), then),
                b.meridian.observer(), "the instant, the place left as it was");
        Bench c = bench();
        c.open(bookmark(m33, 8, List.of(), null, null, new Place(78.223, 15.647)));
        assertEquals(new Observer(78.223, 15.647, OSLO.instant()), c.meridian.observer(),
                "the place, the instant left as it was");
    }

    @Test
    void anUnknownIdentityIsReportedAndNothingIsSubstituted() throws Exception {
        Bench b = bench();
        SearchResult aldebaran = find("Aldebaran");
        SwingUtilities.invokeAndWait(() -> b.host.workingSelection()
                .replaceWith(List.of(aldebaran.identity()), aldebaran.identity()));
        Subject gone = new Subject(Subject.Kind.STAR, "TYC 9999-9999-9");
        BookmarkOpener.Report report = b.open(bookmark(aldebaran, 8, List.of(gone), gone, null, null));
        assertEquals(aldebaran.position(), b.controller.state().centre(), "the view opens");
        assertFalse(report.selected());
        assertEquals(aldebaran.identity(), b.host.workingSelection().lead(), "no substitute selected");
        assertEquals(BookmarkOpener.Outcome.NOT_IN_CATALOGUE, report.subjects().get(0).outcome());
    }

    @Test
    void aBodySubjectTurnsItsSwitchOnAndAMoonItsPlanets() throws Exception {
        Bench b = bench();
        SearchResult m33 = find("M 33");
        BookmarkOpener.Report report = b.open(bookmark(m33, 8, List.of(
                new Subject(Subject.Kind.BODY, "moon"), new Subject(Subject.Kind.BODY, "jovian.io")),
                null, null, null));
        assertTrue(b.switches.get("moon").showing);
        assertTrue(b.switches.get("jovian.jupiter").showing, "Io is drawn with Jupiter");
        assertEquals(List.of("moon", "jovian.jupiter"), report.switchedOn());
        assertEquals("jovian.jupiter", BookmarkOpener.planetOf("jovian.io"));
        assertEquals("saturn", BookmarkOpener.planetOf("saturn.titan"));
        assertEquals("mars", BookmarkOpener.planetOf("mars.phobos"));
        assertEquals("moon", BookmarkOpener.planetOf("moon"));
    }

    @Test
    void aCloserFieldOpensOnlyWhereItIsOffered() throws Exception {
        Bench b = bench();
        SearchResult m33 = find("M 33");
        BookmarkOpener.Report refused = b.open(bookmark(m33, 0.25, List.of(), null, null, null));
        assertTrue(refused.closerRefused());
        assertEquals(1.0, b.controller.state().fieldWidthDegrees());
        Bench c = bench();
        c.closerOffered = true;
        BookmarkOpener.Report offered = c.open(bookmark(m33, 0.25, List.of(), null, null, null));
        assertFalse(offered.closerRefused());
        assertEquals(0.25, c.controller.state().fieldWidthDegrees());
    }

    /** A Solar System switch that remembers being turned on. */
    private static final class FakeSwitch implements BodyOnChart {
        boolean showing;
        final List<Consumer<Boolean>> listeners = new ArrayList<>();

        @Override
        public boolean showing() {
            return showing;
        }

        @Override
        public void show(boolean shown) {
            showing = shown;
            listeners.forEach(l -> l.accept(shown));
        }

        @Override
        public void onChange(Consumer<Boolean> listener) {
            listeners.add(listener);
            listener.accept(showing);
        }
    }
}
