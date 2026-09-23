package juranometria.meridian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.Cardinal;
import juranometria.chart.SkyPosition;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * The observer's N, E, S and W on the mathematical horizon
 * (issue #359).
 *
 * <p>What is held here, in the ruling's own order: the geometry is
 * derived and chirality-pinned; the marks ride the horizon option;
 * the drawn letter and the spoken name are separate and each
 * language's own; a letter that cannot find a clean adjacent box
 * takes the whole landmark with it; the 180-degree globe carries all
 * four at its limb with the letters inward; Oslo's matched pages
 * swap the observer's east and west while celestial east stays left;
 * the equatorial and southern controls prove the sides are derived
 * rather than remembered; and the exported sheet draws the same
 * landmark decision the screen drew.
 */
class CardinalMarksTest {

    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO =
            new Observer(59.913, 10.752, WHEN);
    private static final Observer EQUATOR =
            new Observer(0.0, 10.752, WHEN);
    private static final Observer CAPE_TOWN =
            new Observer(-33.920, 18.420, WHEN);

    /** English and Norwegian, stated (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText
                            .forLanguage("en"));
    private static final juranometria.project.PageWords NORWEGIAN =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText
                            .forLanguage("nb-NO"));

    // ---- geometry: derived, on the horizon, chirality pinned -----

    @Test
    void everyCardinalLiesOnTheHorizonAtRightAngles() {
        for (Observer observer
                : new Observer[] {OSLO, EQUATOR, CAPE_TOWN}) {
            LocalSky sky = new LocalSky(observer);
            SkyPosition[] four = new SkyPosition[4];
            int at = 0;
            for (Cardinal direction : Cardinal.values()) {
                SkyPosition point = sky.cardinal(direction);
                assertTrue(Math.abs(sky.altitudeDegrees(point)) < 1e-9,
                        direction + " lies on the horizon at latitude "
                                + observer.latitudeDegrees());
                four[at++] = point;
            }
            for (int i = 0; i < 4; i++) {
                assertEquals(90.0,
                        separation(four[i], four[(i + 1) % 4]), 1e-9,
                        "neighbouring cardinals are a quarter turn"
                                + " apart at latitude "
                                + observer.latitudeDegrees());
            }
            // The chirality pin. East is the rising side: six hours
            // east of the observer's meridian, so its right
            // ascension leads the zenith's by ninety degrees. The
            // two cross products differ by exactly an east-west
            // swap, and no altitude or separation check can tell
            // them apart - this can.
            double lead = (four[1].raDegrees()
                    - sky.zenith().raDegrees() + 360.0) % 360.0;
            assertEquals(90.0, lead, 1e-6,
                    "east leads the zenith by a quarter turn of right"
                            + " ascension at latitude "
                            + observer.latitudeDegrees());
        }
    }

    @Test
    void theGeographicPoleIsRefused() {
        LocalSky pole = new LocalSky(new Observer(90.0, 0.0, WHEN));
        assertThrows(IllegalArgumentException.class,
                () -> pole.cardinal(Cardinal.NORTH),
                "at the pole every horizon point is every direction");
    }

    // ---- lifecycle: the marks ride the horizon option ------------

    @Test
    void theMarksRideTheHorizonOption() {
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, true, true);
        assertEquals(4, cardinalsIn(module.contributedGeometry()).size(),
                "horizon on: all four are offered");

        module.showing(true, false, true);
        assertEquals(0, cardinalsIn(module.contributedGeometry()).size(),
                "horizon off: none, however much other reference ink"
                        + " stays on - the horizon is what they are"
                        + " directions ON");

        module.showing(false, false, false);
        assertEquals(0, cardinalsIn(module.contributedGeometry()).size(),
                "everything off: the module offers nothing at all");
        // A detached module's geometry is withdrawn from the
        // registry by ChartModuleHost, the lifecycle every module
        // shares; what is #359's own is only the coupling above.
    }

    // ---- language: drawn letter and spoken name, separated -------

    @Test
    void aDirectionSpeaksThroughThePageAndNeverForItself() {
        OverlayContribution.DirectionMark mark =
                new OverlayContribution.DirectionMark(Cardinal.EAST,
                        new SkyPosition(120.0, 0.0));
        // The variant promises no accessible name at all - safe to
        // hold and inspect, resolved by type where language lives.
        assertEquals("cardinal-east", mark.identity());

        assertEquals("N", ENGLISH.directionLetter(Cardinal.NORTH));
        assertEquals("E", ENGLISH.directionLetter(Cardinal.EAST));
        assertEquals("S", ENGLISH.directionLetter(Cardinal.SOUTH));
        assertEquals("W", ENGLISH.directionLetter(Cardinal.WEST));
        assertEquals("N", NORWEGIAN.directionLetter(Cardinal.NORTH));
        assertEquals("Ø", NORWEGIAN.directionLetter(Cardinal.EAST));
        assertEquals("S", NORWEGIAN.directionLetter(Cardinal.SOUTH));
        assertEquals("V", NORWEGIAN.directionLetter(Cardinal.WEST));

        for (Cardinal direction : Cardinal.values()) {
            for (juranometria.project.PageWords words
                    : new juranometria.project.PageWords[] {
                            ENGLISH, NORWEGIAN}) {
                String letter = words.directionLetter(direction);
                String spoken = words.directionSpoken(direction);
                assertNotEquals(letter, spoken,
                        "the drawn abbreviation is never the spoken"
                                + " name");
                assertTrue(spoken.length() > letter.length() + 4,
                        "the spoken name is full and explicit: "
                                + spoken);
            }
        }
        assertEquals("East on your horizon",
                ENGLISH.directionSpoken(Cardinal.EAST));
        assertEquals("Øst på horisonten din",
                NORWEGIAN.directionSpoken(Cardinal.EAST));
        // And the page's own orientation names its frame, so the two
        // easts cannot be confused (#359's wording correction).
        assertTrue(ENGLISH.titleFacts("36", "6.0", "gnomonic")
                        .contains("Celestial north up \u00b7 celestial east"
                                + " left"),
                "the caption says which north and which east");
        assertTrue(NORWEGIAN.titleFacts("36", "6.0", "gnomonisk")
                        .contains("Himmelsk nord opp \u00b7 himmelsk"
                                + " øst til venstre"),
                "in Norwegian as well");
    }

    @Test
    void everyRenderedDirectionCarriesItsLanguagesWords() {
        LocalSky sky = new LocalSky(OSLO);
        ChartViewport viewport =
                new ChartViewport(sky.zenith(), 180.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, true, true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(MeridianModule.ID, module::contributedGeometry);

        for (juranometria.project.PageWords words
                : new juranometria.project.PageWords[] {
                        ENGLISH, NORWEGIAN}) {
            List<ReferenceInk.DirectionPlacement> placements =
                    ReferenceInk.directionPlacements(
                            DrawnPage.of(scene), registry.collect(),
                            words, List.of());
            assertFalse(placements.isEmpty(),
                    "the bare globe renders directions to speak"
                            + " about");
            BufferedImage image = painted(scene, registry, words,
                    List.of());
            for (ReferenceInk.DirectionPlacement placed : placements) {
                assertEquals(words.directionLetter(placed.cardinal()),
                        placed.letter(),
                        "the decision carries the language's own"
                                + " letter");
                assertEquals(words.directionSpoken(placed.cardinal()),
                        placed.spokenName(),
                        "and its full spoken name");
                assertNotEquals(placed.letter(), placed.spokenName(),
                        "which is never the drawn abbreviation");
                assertTrue(placed.spokenName().length() > 8,
                        "and is a real phrase: " + placed.spokenName());
                assertTrue(inkNear(image,
                                placed.box().getCenterX(),
                                placed.box().getCenterY(), 8) > 0,
                        "and the page inks the letter exactly where"
                                + " the decision says: "
                                + placed.cardinal());
            }
        }
    }

    // ---- placement: clean adjacent box, or the whole mark goes ---

    @Test
    void aLetterWithNoCleanAdjacentBoxTakesTheWholeLandmarkWithIt() {
        LocalSky sky = new LocalSky(OSLO);
        SkyPosition north = sky.cardinal(Cardinal.NORTH);
        ChartViewport viewport =
                new ChartViewport(north, 36.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("test-cardinal", () -> List.of(
                new OverlayContribution.DirectionMark(Cardinal.NORTH,
                        north)));

        BufferedImage clean = painted(scene, registry, ENGLISH,
                List.of());
        assertTrue(inkNear(clean, 450, 350, 12) > 0,
                "with clean space the diamond is drawn at its exact"
                        + " point");
        assertTrue(inkNear(clean, 450, 350, 30)
                        > inkNear(clean, 450, 350, 12),
                "and its letter beside it");

        BufferedImage crowded = painted(scene, registry, ENGLISH,
                List.of(new Rectangle2D.Double(380.0, 280.0, 140.0,
                        140.0)));
        assertEquals(0, inkNear(crowded, 450, 350, 40),
                "with every adjacent box reserved, the landmark is"
                        + " omitted whole - no unexplained diamond,"
                        + " no letter through other ink");
    }

    // ---- the globe: all four at the limb, letters inward ---------

    @Test
    void theGlobeCarriesAllFourAtItsLimbWithLettersInward() {
        LocalSky sky = new LocalSky(OSLO);
        ChartViewport viewport =
                new ChartViewport(sky.zenith(), 180.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        OverlayRegistry registry = new OverlayRegistry();
        List<OverlayContribution> marks = new ArrayList<>();
        for (Cardinal direction : Cardinal.values()) {
            marks.add(new OverlayContribution.DirectionMark(direction,
                    sky.cardinal(direction)));
        }
        registry.offer("test-cardinals", () -> List.copyOf(marks));

        BufferedImage image = painted(scene, registry, ENGLISH,
                List.of());
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        double cx = 450.0;
        double cy = 350.0;
        for (Cardinal direction : Cardinal.values()) {
            PixelPoint at = page.projection()
                    .project(sky.cardinal(direction))
                    .map(mapping::toPixel).orElseThrow();
            double fromCentre = Math.hypot(at.x() - cx, at.y() - cy);
            assertTrue(inkNear(image, at.x(), at.y(), 9) > 0,
                    direction + " sits at its exact limb point");
            // The letter is inward: some ink beyond the diamond,
            // and every changed pixel no further from the centre
            // than the limb itself - nothing outside suggesting a
            // page-edge direction.
            assertTrue(inkNear(image, at.x(), at.y(), 30)
                            > inkNear(image, at.x(), at.y(), 9),
                    direction + " carries a letter beside the mark");
            assertEquals(0, inkOutsideRadius(image, cx, cy,
                            fromCentre + 10.0, at, 40),
                    direction + " puts nothing outside the limb");
        }
    }

    // ---- precedence: names yield only to accepted cardinals ------

    @Test
    void lineNamesYieldOnlyToAcceptedCardinals() {
        LocalSky sky = new LocalSky(OSLO);
        // A horizon-centred page: the horizon's own name used to sit
        // at its curve's end, which is exactly where a cardinal
        // lives. Bare sky, so the cardinals are accepted.
        ChartViewport viewport = new ChartViewport(
                sky.cardinal(Cardinal.NORTH), 180.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, true, true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(MeridianModule.ID, module::contributedGeometry);
        DrawnPage page = DrawnPage.of(scene);

        List<ReferenceInk.DirectionPlacement> cardinals =
                ReferenceInk.directionPlacements(page,
                        registry.collect(), ENGLISH, List.of());
        assertFalse(cardinals.isEmpty(),
                "the bare page accepts cardinals to outrank with");
        for (ReferenceInk.NamePlacement name
                : ReferenceInk.namePlacements(page, registry.collect(),
                        ENGLISH, List.of())) {
            for (ReferenceInk.DirectionPlacement cardinal : cardinals) {
                assertFalse(name.box().intersects(cardinal.box()),
                        name.name() + " gave way to the accepted "
                                + cardinal.cardinal());
            }
        }

        // And the only-when-accepted half: blanket every cardinal
        // candidate with reserved ink, so none is accepted - the
        // names must land exactly where a cardinal-free page puts
        // them, because a mark that is not there outranks nothing.
        List<java.awt.Shape> blanket = List.of(
                new java.awt.geom.Rectangle2D.Double(0, 0, 900, 700));
        List<ReferenceInk.DirectionPlacement> none =
                ReferenceInk.directionPlacements(page,
                        registry.collect(), ENGLISH, blanket);
        assertTrue(none.isEmpty(), "the blanket refuses every letter");
        MeridianModule bareModule = new MeridianModule(OSLO);
        bareModule.showing(true, true, true);
        OverlayRegistry withoutMarks = new OverlayRegistry();
        withoutMarks.offer(MeridianModule.ID, () -> bareModule
                .contributedGeometry().stream()
                .filter(each -> !(each instanceof
                        juranometria.module.OverlayContribution
                                .DirectionMark))
                .toList());
        assertEquals(
                ReferenceInk.namePlacements(page, withoutMarks.collect(),
                        ENGLISH, List.of()).toString(),
                ReferenceInk.namePlacements(page, registry.collect(),
                        ENGLISH, blanket).toString(),
                "no accepted cardinal, no moved name");
    }

    // ---- Oslo's matched pages, and the controls ------------------

    @Test
    void osloSwapsTheObserversEastAndWestBetweenMatchedPages() {
        // The issue's own demonstration, recorded as Oslo's and not
        // as a law of latitude: looking north below the pole the
        // page is pole-inverted, so the observer's east leaves the
        // right edge; looking south it leaves the left. Celestial
        // east stays left on both - the page never turned.
        assertEquals("right", sideLeaving(OSLO, 0.0, Cardinal.EAST));
        assertEquals("left", sideLeaving(OSLO, 0.0, Cardinal.WEST));
        assertEquals("left", sideLeaving(OSLO, 180.0, Cardinal.EAST));
        assertEquals("right", sideLeaving(OSLO, 180.0, Cardinal.WEST));
        assertTrue(celestialEastLandsLeft(OSLO, 0.0)
                        && celestialEastLandsLeft(OSLO, 180.0),
                "celestial east is left on both matched pages");
    }

    @Test
    void theControlsProveTheSidesAreDerivedNotRemembered() {
        // Measured in discovery and pinned here: at the equator the
        // observer's east leaves the LEFT edge looking north, and at
        // Cape Town the north and south answers are Oslo's mirrored.
        // An implementation that remembered "east is right when
        // looking north" would pass Oslo and fail here.
        assertEquals("left", sideLeaving(EQUATOR, 0.0, Cardinal.EAST));
        assertEquals("left", sideLeaving(EQUATOR, 180.0,
                Cardinal.EAST));
        assertEquals("left", sideLeaving(CAPE_TOWN, 0.0,
                Cardinal.EAST));
        assertEquals("right", sideLeaving(CAPE_TOWN, 180.0,
                Cardinal.EAST));
    }

    // ---- helpers -------------------------------------------------

    private static List<OverlayContribution> cardinalsIn(
            List<OverlayContribution> offered) {
        List<OverlayContribution> found = new ArrayList<>();
        for (OverlayContribution contribution : offered) {
            if (contribution
                    instanceof OverlayContribution.DirectionMark) {
                found.add(contribution);
            }
        }
        return found;
    }

    private static BufferedImage painted(ChartScene scene,
            OverlayRegistry registry,
            juranometria.project.PageWords words,
            List<java.awt.Shape> reserved) {
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 900, 700);
            ReferenceInk.paint(g, scene, registry.collect(),
                    ChartPalette.WHITE_PAPER, words,
                    List.copyOf(reserved));
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int inkNear(BufferedImage image, double x, double y,
                               int radius) {
        int count = 0;
        for (int j = (int) y - radius; j <= y + radius; j++) {
            for (int i = (int) x - radius; i <= x + radius; i++) {
                if (i < 0 || j < 0 || i >= image.getWidth()
                        || j >= image.getHeight()) {
                    continue;
                }
                if ((image.getRGB(i, j) & 0xffffff) != 0xffffff) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Ink outside the stated radius from the centre, near a point. */
    private static int inkOutsideRadius(BufferedImage image, double cx,
            double cy, double radius, PixelPoint near, int window) {
        int count = 0;
        for (int j = (int) near.y() - window;
                j <= near.y() + window; j++) {
            for (int i = (int) near.x() - window;
                    i <= near.x() + window; i++) {
                if (i < 0 || j < 0 || i >= image.getWidth()
                        || j >= image.getHeight()) {
                    continue;
                }
                if (Math.hypot(i - cx, j - cy) <= radius) {
                    continue;
                }
                if ((image.getRGB(i, j) & 0xffffff) != 0xffffff) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The x-side a direction's projection leaves the page on. */
    private static String sideLeaving(Observer observer,
                                      double azimuthDegrees,
                                      Cardinal direction) {
        LocalSky sky = new LocalSky(observer);
        SkyPosition centre = lookFrom(sky, azimuthDegrees, 25.0);
        ChartViewport viewport =
                new ChartViewport(centre, 90.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        DrawnPage page = DrawnPage.of(scene);
        PixelPoint at = page.projection()
                .project(sky.cardinal(direction))
                .map(new ViewportMapping(page)::toPixel).orElseThrow();
        return at.x() < 450.0 ? "left" : "right";
    }

    private static boolean celestialEastLandsLeft(Observer observer,
            double azimuthDegrees) {
        LocalSky sky = new LocalSky(observer);
        SkyPosition centre = lookFrom(sky, azimuthDegrees, 25.0);
        ChartViewport viewport =
                new ChartViewport(centre, 90.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint centrePx = page.projection().project(centre)
                .map(mapping::toPixel).orElseThrow();
        PixelPoint eastPx = page.projection()
                .project(new SkyPosition(
                        (centre.raDegrees() + 2.0) % 360.0,
                        centre.decDegrees()))
                .map(mapping::toPixel).orElseThrow();
        return eastPx.x() < centrePx.x();
    }

    /** The sky position at this azimuth and altitude, derived. */
    private static SkyPosition lookFrom(LocalSky sky,
            double azimuthDegrees, double altitudeDegrees) {
        double[] z = unit(sky.zenith());
        double[] n = unit(sky.cardinal(Cardinal.NORTH));
        double[] e = unit(sky.cardinal(Cardinal.EAST));
        double az = Math.toRadians(azimuthDegrees);
        double alt = Math.toRadians(altitudeDegrees);
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            v[i] = Math.cos(alt) * (Math.cos(az) * n[i]
                    + Math.sin(az) * e[i]) + Math.sin(alt) * z[i];
        }
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        if (ra < 0) {
            ra += 360.0;
        }
        return new SkyPosition(ra,
                Math.toDegrees(Math.asin(Math.max(-1.0,
                        Math.min(1.0, v[2])))));
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double separation(SkyPosition a, SkyPosition b) {
        double[] u = unit(a);
        double[] v = unit(b);
        double dot = u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
        return Math.toDegrees(Math.acos(
                Math.max(-1.0, Math.min(1.0, dot))));
    }
}
