package juranometria.page;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.module.OverlayContribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The page's drawn bodies in its contents (#513): entries the paint
 * adds and replaces, found by identity like any other, always drawn
 * and named, never the scene's.
 */
class PageContentsTest {

    private static PageEntry.Body body(String identity, Instant instant, boolean selectable) {
        return new PageEntry.Body(identity, identity.toUpperCase(java.util.Locale.ROOT),
                new SkyPosition(10.0, 20.0),
                new OverlayContribution.Moment(instant, 59.913, 10.752), selectable, 0.5);
    }

    @Test
    void bodiesAreEntriesThePaintReplacesWhole() {
        Instant first = Instant.parse("2026-03-20T21:33:00Z");
        PageContents empty = PageContents.EMPTY;
        PageContents one = empty.withBodies(List.of(body("moon", first, true)));
        assertEquals(1, one.bodies().size());
        assertTrue(one.holds("moon"), "found by identity");
        assertEquals(PageVisibility.DRAWN, one.find("moon").orElseThrow().visibility());
        assertTrue(one.find("moon").orElseThrow().named());
        assertEquals(1, one.tally().get(PageVisibility.DRAWN));

        PageContents later = one.withBodies(List.of(body("moon", first.plusSeconds(3600), true),
                body("sun", first.plusSeconds(3600), false)));
        assertEquals(2, later.bodies().size(), "replaced whole, not accumulated");
        assertEquals(first.plusSeconds(3600),
                later.bodies().get(0).moment().instant(), "the later moment");
        assertEquals(0, later.withBodies(List.of()).bodies().size(), "and withdrawn whole");
        assertEquals(List.of(), later.deepSky(), "a body is no deep-sky object");
        assertEquals(List.of(), later.namedStars(), "and no star");
    }

    @Test
    void aBodyEntryStatesWhatItIs() {
        assertThrows(IllegalArgumentException.class, () -> new PageEntry.Body("", "Moon",
                new SkyPosition(1, 2), null, false, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PageEntry.Body("moon", null,
                new SkyPosition(1, 2), null, false, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Body(
                "moon", "Moon", new SkyPosition(1, 2), 1800.0, null, false, 384400.0,
                juranometria.module.InkRole.BODY, null, true),
                "a selectable body states its moment");
    }
}
