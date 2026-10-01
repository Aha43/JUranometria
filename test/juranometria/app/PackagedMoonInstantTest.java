package juranometria.app;

import org.junit.jupiter.api.Test;

import juranometria.tool.MoonEventsFixture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The packaged image carries no studies, so its Moon journey (#416)
 * restates the fixture's first-quarter instant; this holds the
 * restatement to the fixture row it names.
 */
class PackagedMoonInstantTest {

    @Test
    void thePackagedFirstQuarterIsTheFixturesRow() throws Exception {
        assertEquals(MoonEventsFixture.read().get("first-quarter-june").instant(),
                PackagedAcceptanceMain.MOON_FIRST_QUARTER);
    }

    @Test
    void thePackagedGalleryMomentIsTheGallerysOwn() {
        assertEquals(juranometria.tool.GalleryPageMain.WHEN,
                PackagedAcceptanceMain.GALLERY_MOMENT);
    }
}
