package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.ui.ChartComponent;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The Sun–Moon cartography contract, measured before it is ruled
 * (Sprint 37, issue #414).
 *
 * <p>Nothing here is production rendering. The atlas's own pages are
 * assembled and painted through the production composition, and this
 * study draws candidate marks <em>over</em> the painted image with its
 * own ink, so the owner can look at what a rule would mean on a real
 * page. The numbers come from the production projections and the
 * bundled ephemeris: how large a true-scale disc is on every page the
 * atlas draws, how the chart's north turns across a page, and where
 * the bodies stand at the instants the mockups show.
 *
 * <p>Written to {@code docs/studies/solar-cartography/} by
 * {@code make solar-cartography-study}.
 */
public final class SolarCartographyStudyMain {

    private static final File DIR = new File("docs/studies/solar-cartography");

    /** The gallery's own observer and instant, restated (GalleryPageMain). */
    private static final Instant GALLERY_WHEN = Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, GALLERY_WHEN);

    /** The released page and the gallery's: 900 × 700 px. */
    private static final int PAGE_W = 900;
    private static final int PAGE_H = 700;

    /** A nominal disc for the scale table: 32′, stated as such. */
    private static final double NOMINAL_DIAMETER_ARCMIN = 32.0;

    /** The fields the atlas draws, its own ladder. */
    private static final double[] FIELDS = {180.0, 120.0, 90.0, 60.0, 42.0,
            36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));

    private static final StringBuilder REPORT = new StringBuilder();
    private static SolarSystemService service;

    private SolarCartographyStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        service = SolarSystemService.load();
        p("# The Sun and the Moon on the page: measurements and mockups");
        p("");
        p("Sprint 37, issue #414. What a cartography rule would mean on the"
                + " atlas's own pages, measured through the production projections"
                + " and the bundled ephemeris, with candidate marks drawn by this"
                + " study over pages the production composition painted. Nothing"
                + " here is production rendering; every image is a mockup, and"
                + " says so in its corner. Regenerate with `make"
                + " solar-cartography-study`.");
        p("");
        scaleTable();
        northRotation();
        mockups();
        proposals();
        Files.writeString(new File(DIR, "measurements.md").toPath(),
                REPORT.toString(), StandardCharsets.UTF_8);
        System.out.println("solar cartography study written to " + DIR);
    }

    // ---- A. true angular scale on every page ---------------------------

    private static void scaleTable() {
        p("## A. A true-scale disc on every page the atlas draws");
        p("");
        p("The Sun's disc spans 31′ 28″ to 32′ 31″ over the year"
                + " (`docs/studies/solar-system/measurements.md`); the Moon's,"
                + " 29′ 08″ at the farthest apogee to 33′ 42″ at the nearest"
                + " perigee (`moon-measurements.md`). The table takes a nominal"
                + " **32′** disc and measures it through the projection each"
                + " field uses, at the page centre and at the page's corner,"
                + " where a tangent projection stretches most. Pages: the"
                + " released component and the gallery at 900 × 700 px; the"
                + " printable sheet on A4 and US Letter at " + PngSheetWriter.DEFAULT_RESOLUTION
                + " dpi, whose chart rectangles are "
                + String.format(Locale.ROOT, "%.1f × %.1f mm and %.1f × %.1f mm",
                        PaperSize.A4.chartWideMm(), PaperSize.A4.chartHighMm(),
                        PaperSize.LETTER.chartWideMm(), PaperSize.LETTER.chartHighMm())
                + ". The brightest star the atlas draws is a disc of "
                + String.format(Locale.ROOT, "%.0f", 2 * StarSizePolicy.DEFAULT.maximumRadiusPx())
                + " px; the faintest, "
                + String.format(Locale.ROOT, "%.1f", 2 * StarSizePolicy.DEFAULT.minimumRadiusPx())
                + " px, which is the atlas's minimum visible ink.");
        p("");
        p("| field | projection | 900 × 700 px: centre | corner | A4 sheet: centre | corner (mm) | Letter sheet: centre |");
        p("|---:|---|---:|---:|---:|---:|---:|");
        int a4W = PngSheetWriter.widePixels(PaperSize.A4, PngSheetWriter.DEFAULT_RESOLUTION);
        int a4H = (int) Math.round(PaperSize.A4.chartHighPoints() * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        int a4ChartW = (int) Math.round(PaperSize.A4.chartWidePoints() * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        int letterChartW = (int) Math.round(PaperSize.LETTER.chartWidePoints() * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        int letterH = (int) Math.round(PaperSize.LETTER.chartHighPoints() * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        double mmPerPx = 25.4 / PngSheetWriter.DEFAULT_RESOLUTION;
        for (double field : FIELDS) {
            double[] page = discPx(field, PAGE_W, PAGE_H);
            double[] a4 = discPx(field, a4ChartW, a4H);
            double[] letter = discPx(field, letterChartW, letterH);
            p(String.format(Locale.ROOT, "| %.0f° | %s | %.1f px | %.1f px | %.1f px = %.2f mm | %.2f mm | %.2f mm |",
                    field, juranometria.chart.ChartProjection.forField(field).displayName(),
                    page[0], page[1], a4[0], a4[0] * mmPerPx, a4[1] * mmPerPx,
                    letter[0] * mmPerPx));
        }
        p("");
        p("The sheet's pixel widths: A4 " + a4W + " px across the paper, " + a4ChartW
                + " px across the chart rectangle; Letter " + letterChartW
                + " px across the chart rectangle. At 42°, the sheet's own"
                + " field, the two discs on paper are about "
                + String.format(Locale.ROOT, "%.1f mm", discPx(42.0, a4ChartW, a4H)[0] * mmPerPx)
                + " across - a lentil, not a dot.");
        p("");
    }

    /**
     * A 32′ disc's diameter in pixels at the centre of a page and at
     * its corner, by projecting the disc's north and south limb points.
     */
    private static double[] discPx(double field, int w, int h) {
        SkyPosition centre = new SkyPosition(0.0, 0.0);
        DrawnPage page = emptyPage(centre, field, w, h);
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        double half = NOMINAL_DIAMETER_ARCMIN / 60.0 / 2.0;
        double atCentre = diameterPx(projection, mapping, centre, half);
        // The corner: a point on the page's diagonal, three quarters of
        // the way to the corner, so the disc itself is still on the page.
        double cornerPlaneX = (w / 2.0) * 0.75 / mapping.pixelsPerPlaneUnit();
        double cornerPlaneY = (h / 2.0) * 0.75 / mapping.pixelsPerPlaneUnit();
        SkyPosition corner = projection.unproject(new PlanePoint(cornerPlaneX, cornerPlaneY))
                .orElse(centre);
        double atCorner = diameterPx(projection, mapping, corner, half);
        return new double[] {atCentre, atCorner};
    }

    /**
     * A starless page of the given field and size, so its projection is
     * worked out where every page's is - in {@link DrawnPage#of} - and
     * carried with the page rather than asked of the viewport again.
     */
    private static DrawnPage emptyPage(SkyPosition centre, double field, int w, int h) {
        ChartViewport viewport = new ChartViewport(centre, field, w, h);
        return DrawnPage.of(new ChartScene(viewport, List.of(), List.of(),
                "study", 8.0));
    }

    private static double diameterPx(Projection projection, ViewportMapping mapping,
                                     SkyPosition at, double halfDegrees) {
        SkyPosition north = new SkyPosition(at.raDegrees(),
                Math.min(89.999, at.decDegrees() + halfDegrees));
        SkyPosition south = new SkyPosition(at.raDegrees(),
                Math.max(-89.999, at.decDegrees() - halfDegrees));
        PixelPoint a = mapping.toPixel(projection.project(north).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(south).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    // ---- B. how north turns across a page -------------------------------

    /** Page-up and page-left unit vectors of local north and east at a position. */
    private static double[][] tangents(Projection projection, ViewportMapping mapping,
                                       SkyPosition at) {
        double eps = 1.0 / 3600.0; // one arcsecond
        PixelPoint c = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint n = mapping.toPixel(projection.project(new SkyPosition(at.raDegrees(),
                Math.min(89.999, at.decDegrees() + eps))).orElseThrow());
        double cosDec = Math.max(1e-9, Math.cos(Math.toRadians(at.decDegrees())));
        PixelPoint e = mapping.toPixel(projection.project(new SkyPosition(
                juranometria.sky.SkyFrame.normalise(at.raDegrees() + eps / cosDec),
                at.decDegrees())).orElseThrow());
        double[] north = unit(n.x() - c.x(), n.y() - c.y());
        double[] east = unit(e.x() - c.x(), e.y() - c.y());
        return new double[][] {north, east};
    }

    private static double[] unit(double x, double y) {
        double l = Math.hypot(x, y);
        return new double[] {x / l, y / l};
    }

    /** The angle of a page vector from page-up, positive towards page-left (east). */
    private static double pageAngleDegrees(double[] v) {
        // pixel y grows downwards; page-up is (0, -1); page-left is (-1, 0)
        return Math.toDegrees(Math.atan2(-v[0], -v[1]));
    }

    private static void northRotation() {
        p("## B. Where north points on the page");
        p("");
        p("A position angle is measured from celestial north through east."
                + " On the page, north is straight up only at the centre of a"
                + " page whose centre is on the equator; elsewhere the projected"
                + " meridian turns. The bright-limb angle χ of the Moon table is"
                + " an angle from *celestial* north, so drawing the lit side"
                + " means turning χ by the angle between page-up and the"
                + " projected north at the Moon's own position - never"
                + " recomputing χ from J2000 positions, as ruled (#406, M2)."
                + " Measured here: the turn of north at the page's corner (three"
                + " quarters of the way along the diagonal) for pages centred on"
                + " the equator, at +45° and at +75° of declination.");
        p("");
        p("| field | centre dec 0°: corner turn | dec +45°: corner turn | dec +75°: corner turn | east ⟂ north at the +45° corner? |");
        p("|---:|---:|---:|---:|---:|");
        for (double field : new double[] {36.0, 24.0, 8.0, 3.0, 1.0}) {
            StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "| %.0f° |", field));
            double perpendicular = 90.0;
            for (double dec : new double[] {0.0, 45.0, 75.0}) {
                SkyPosition centre = new SkyPosition(83.0, dec);
                DrawnPage page = emptyPage(centre, field, PAGE_W, PAGE_H);
                Projection projection = page.projection();
                ViewportMapping mapping = new ViewportMapping(page);
                double x = (PAGE_W / 2.0) * 0.75 / mapping.pixelsPerPlaneUnit();
                double y = (PAGE_H / 2.0) * 0.75 / mapping.pixelsPerPlaneUnit();
                SkyPosition corner = projection.unproject(new PlanePoint(x, y)).orElseThrow();
                double[][] t = tangents(projection, mapping, corner);
                double turn = pageAngleDegrees(t[0]);
                row.append(String.format(Locale.ROOT, " %+.2f° |", turn));
                if (dec == 45.0) {
                    double between = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1,
                            t[0][0] * t[1][0] + t[0][1] * t[1][1]))));
                    perpendicular = between;
                }
            }
            row.append(String.format(Locale.ROOT, " %.2f° |", perpendicular));
            p(row.toString());
        }
        p("");
        p("**The rotation contract, stated.** At the body's projected position"
                + " P, take the unit page vectors n̂ (towards increasing"
                + " declination) and ê (towards increasing right ascension),"
                + " found by projecting the position displaced by one arcsecond"
                + " in each. The lit side's direction on the page is"
                + " cos χ · n̂ + sin χ · ê; the terminator's axis ratio is"
                + " |cos i|. The round trip recovers χ as atan2(d·ê, d·n̂) from"
                + " the drawn direction d, and a reference test holds it at the"
                + " page centre (where it is the identity on an equatorial"
                + " page), at the corners of the table above, and near the pole."
                + " Away from the centre ê and n̂ are not exactly perpendicular"
                + " on a tangent projection (the last column), which is why the"
                + " contract uses both vectors rather than one angle.");
        p("");
    }

    // ---- C. mockups on real pages ---------------------------------------

    private record Mark(String label, SkyPosition at, double diameterArcsec,
                        boolean sun, double illuminated, double chiDegrees,
                        boolean belowHorizon, double phaseAngleDegrees) {
    }

    private static void mockups() throws Exception {
        p("## C. Mockups on the atlas's own pages");
        p("");
        p("Each page is assembled and painted by the production composition"
                + " (`ChartComponent` over the bundled catalogue, the meridian and"
                + " ecliptic modules where the page has them); the candidate marks"
                + " are drawn over the painted page by this study, at true scale,"
                + " from the ephemeris's positions for the instant and observer"
                + " stated. The Sun is drawn as a ring with a centre dot - the"
                + " astronomical symbol's geometry at the disc's true size - and"
                + " the Moon as its disc with the terminator from the table's"
                + " illuminated fraction and bright-limb angle, turned into the"
                + " page's north basis by the contract of section B.");
        p("");
        Map<String, juranometria.tool.MoonEventsFixture.Event> events =
                juranometria.tool.MoonEventsFixture.read();
        Map<String, SeasonalEventsFixture.Event> seasons = SeasonalEventsFixture.read();

        // 1. The equinox page at the March equinox: the Sun on the ecliptic.
        Instant equinox = seasons.get("march-equinox").instant();
        Observer osloEquinox = OSLO.at(equinox);
        SunObservation sunE = sun(osloEquinox);
        MoonObservation moonE = moon(osloEquinox);
        page("equinox-sun-24", "The March equinox page at the IMCCE's equinox instant",
                new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, false, true, true,
                List.of(mark("Sun", sunE), mark("Moon", moonE)),
                "The Sun on the ecliptic at RA 0h, at true scale (a "
                        + px(sunE.angularDiameterArcseconds(), 24.0) + " disc), its"
                        + " label colliding with the ecliptic's own equinox landmark"
                        + " - a label question the contract must answer; the Moon,"
                        + " two days past new, is "
                        + String.format(Locale.ROOT, "%.1f", moonE.elongationDegrees())
                        + "° east of it and " + MoonTableWords.percent(moonE)
                        + " lit, off this page to the upper left, where the"
                        + " edge-hint candidate marks it.");

        // 2. The horizon page at the gallery's instant: the Sun below the horizon.
        LocalSky sky = new LocalSky(OSLO);
        SunObservation sunH = sun(OSLO);
        MoonObservation moonH = moon(OSLO);
        // Centred where the mathematical horizon passes nearest the
        // Moon, so a body just below the ground is on the page.
        SkyPosition horizonCentre = null;
        double nearest = Double.MAX_VALUE;
        for (SkyPosition on : sky.horizon().around(720)) {
            double sep = on.separationDegrees(moonH.astrometricJ2000());
            if (sep < nearest) {
                nearest = sep;
                horizonCentre = on;
            }
        }
        ChartViewState horizonPage = new ChartViewState(horizonCentre, 24.0, 8.0, null, null);
        page("horizon-omitted-24", "The horizon page nearest the Moon, 2026-03-20 21:33 UTC at Oslo: bodies below the horizon omitted",
                horizonPage, ENGLISH, ChartPalette.WHITE_PAPER, true, false, false,
                below(List.of(mark("Sun", sunH), mark("Moon", moonH)), sunH, moonH, false),
                "Candidate 1: a body below the drawn horizon is not drawn at all"
                        + " - the Sun stands at "
                        + String.format(Locale.ROOT, "%.1f", sunH.horizontal().altitudeDegrees())
                        + "° and the Moon at "
                        + String.format(Locale.ROOT, "%.1f", moonH.horizontal().altitudeDegrees())
                        + "°; only what is above the ground is inked.");
        page("horizon-dimmed-24", "The same page: bodies below the horizon dimmed",
                horizonPage, ENGLISH, ChartPalette.WHITE_PAPER, true, false, false,
                below(List.of(mark("Sun", sunH), mark("Moon", moonH)), sunH, moonH, true),
                "Candidate 2: a body below the horizon is drawn in the ground's"
                        + " dimmed ink, where it is, so a reader sees where it will"
                        + " rise from; the page must then say it is below the"
                        + " horizon.");

        // 3. The Moon at first quarter, centred, at 1°, 3° and 8°.
        Instant firstQuarter = events.get("first-quarter-june").instant();
        Observer osloFQ = OSLO.at(firstQuarter);
        MoonObservation moonFQ = moon(osloFQ);
        for (double field : new double[] {1.0, 3.0, 8.0}) {
            page(String.format(Locale.ROOT, "moon-first-quarter-%.0f", field),
                    String.format(Locale.ROOT, "The Moon at first quarter (Espenak, 2026-06-21 21:55 UTC), Oslo, %.0f° field", field),
                    new ChartViewState(moonFQ.astrometricJ2000(), field, 8.0, null, null),
                    field == 8.0 ? NORSK : ENGLISH,
                    field == 3.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    false, false, false, List.of(mark("Moon", moonFQ)),
                    "The disc " + px(moonFQ.angularDiameterArcseconds(), field)
                            + " across, " + MoonTableWords.percent(moonFQ)
                            + " lit, the bright limb at χ = "
                            + String.format(Locale.ROOT, "%.0f", moonFQ.brightLimbAngleDegrees())
                            + "° turned into the page's north basis"
                            + (field == 3.0 ? "; on the black sky" : "")
                            + (field == 8.0 ? "; the page's words in Norwegian" : "") + ".");
        }

        // 4. The Sun and the Moon within four degrees at the new Moon.
        Instant newMoon = events.get("new-moon-june").instant();
        Observer osloNew = OSLO.at(newMoon);
        SunObservation sunN = sun(osloNew);
        MoonObservation moonN = moon(osloNew);
        SkyPosition between = new SkyPosition(
                (sunN.astrometricJ2000().raDegrees() + moonN.astrometricJ2000().raDegrees()) / 2.0,
                (sunN.astrometricJ2000().decDegrees() + moonN.astrometricJ2000().decDegrees()) / 2.0);
        page("conjunction-8", "The new Moon (Espenak, 2026-06-15 02:54 UTC), Oslo, 8° field",
                new ChartViewState(between, 8.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, false, true, false,
                List.of(mark("Sun", sunN), mark("Moon", moonN)),
                "Both bodies at true scale, "
                        + String.format(Locale.ROOT, "%.1f", moonN.elongationDegrees())
                        + "° apart, the Moon " + MoonTableWords.percent(moonN)
                        + " lit: a disc with almost no lit side is its dark disc,"
                        + " inked, so the new Moon is neither invisible nor"
                        + " falsely bright. What draws over what when the two"
                        + " overlap arises at an eclipse only.");

        // 5. The Moon over the Pleiades: the closest approach in 2026.
        SkyPosition pleiades = new SkyPosition(56.75, 24.12);
        Instant closest = closestApproach(pleiades);
        Observer osloP = OSLO.at(closest);
        MoonObservation moonP = moon(osloP);
        page("moon-over-stars-3", "The Moon's closest approach to the Pleiades in 2026, Oslo, 3° field",
                new ChartViewState(pleiades, 3.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, false, false, false, List.of(mark("Moon", moonP)),
                "Found by stepping 2026 hourly for the least separation between"
                        + " the Moon's centre and the cluster's: " + closest
                        + " UTC, separation "
                        + String.format(Locale.ROOT, "%.2f", moonP.astrometricJ2000()
                                .separationDegrees(pleiades))
                        + "°. The disc covers stars: candidate rule, the disc is"
                        + " drawn over the stars it hides and its dark side is"
                        + " inked so the hidden stars are seen to be hidden rather"
                        + " than missing.");

        // 6. Just off the page: an edge hint.
        SkyPosition offCentre = new SkyPosition(juranometria.sky.SkyFrame.normalise(
                sunE.astrometricJ2000().raDegrees() + 13.5),
                sunE.astrometricJ2000().decDegrees());
        page("off-page-hint-24", "The equinox instant with the Sun 1.5° beyond the page's left edge",
                new ChartViewState(offCentre, 24.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, false, true, true, List.of(mark("Sun", sunE)),
                "Candidate: a body off the page leaves a small tick and its name"
                        + " at the edge nearest to it, as this study draws; the"
                        + " alternative is nothing, as for a star off the page.");
    }

    private static Mark mark(String label, SunObservation o) {
        return new Mark(label, o.astrometricJ2000(), o.angularDiameterArcseconds(),
                true, 1.0, 0.0, o.horizontal().altitudeDegrees() < 0.0, 0.0);
    }

    private static Mark mark(String label, MoonObservation o) {
        return new Mark(label, o.astrometricJ2000(), o.angularDiameterArcseconds(),
                false, o.illuminatedFraction(), o.brightLimbAngleDegrees(),
                o.horizontal().altitudeDegrees() < 0.0, o.phaseAngleDegrees());
    }

    private static List<Mark> below(List<Mark> marks, SunObservation sun,
                                    MoonObservation moon, boolean keep) {
        if (keep) {
            return marks;
        }
        List<Mark> shown = new ArrayList<>();
        for (Mark m : marks) {
            if (!m.belowHorizon()) {
                shown.add(m);
            }
        }
        return shown;
    }

    private static SunObservation sun(Observer o) {
        return (SunObservation) service.observe(Body.SUN, o);
    }

    private static MoonObservation moon(Observer o) {
        return (MoonObservation) service.observe(Body.MOON, o);
    }

    private static String px(double arcseconds, double field) {
        return String.format(Locale.ROOT, "%.0f px", arcseconds / 3600.0 * PAGE_W / field);
    }

    /** The hour of 2026 at which the Moon's centre is nearest a position, geocentrically. */
    private static Instant closestApproach(SkyPosition target) {
        Instant best = null;
        double least = Double.MAX_VALUE;
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        while (t.isBefore(end)) {
            MoonObservation m = moon(OSLO.at(t));
            double sep = m.astrometricJ2000().separationDegrees(target);
            if (sep < least) {
                least = sep;
                best = t;
            }
            t = t.plus(Duration.ofHours(1));
        }
        return best;
    }

    /** Words the mockup captions borrow from the Moon table's rounding. */
    private static final class MoonTableWords {
        static String percent(MoonObservation o) {
            return juranometria.ui.solar.MoonTableFormat.percent(o.illuminatedFraction());
        }
    }

    private static void page(String name, String title, ChartViewState state,
                             PageText words, ChartPalette palette,
                             boolean withMeridian, boolean withEcliptic,
                             boolean edgeHints, List<Mark> marks, String caption)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(PAGE_W, PAGE_H);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            if (withMeridian) {
                MeridianModule meridian = new MeridianModule(OSLO);
                holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            }
            if (withEcliptic) {
                EclipticModule ecliptic = new EclipticModule();
                ecliptic.showing(true);
                holder[0].overlays().offer(EclipticModule.ID, ecliptic::contributedGeometry);
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        ChartScene scene = chart.currentScene();
        DrawnPage drawn = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(drawn);
        Projection projection = drawn.projection();
        Graphics2D g = image.createGraphics();
        StringBuilder drawnNote = new StringBuilder();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean dark = palette == ChartPalette.BLACK_SKY;
            for (Mark m : marks) {
                var projected = projection.project(m.at());
                if (projected.isEmpty()) {
                    continue;
                }
                PixelPoint c = mapping.toPixel(projected.get());
                double radius = diameterPx(projection, mapping, m.at(), m.diameterArcsec() / 3600.0 / 2.0) / 2.0;
                boolean onPage = c.x() + radius > 0 && c.x() - radius < PAGE_W
                        && c.y() + radius > 0 && c.y() - radius < PAGE_H;
                if (!onPage) {
                    if (edgeHints) {
                        edgeHint(g, c, m, dark);
                    }
                    drawnNote.append(m.label()).append(" off the page at (")
                            .append(String.format(Locale.ROOT, "%.0f, %.0f", c.x(), c.y()))
                            .append(edgeHints ? "), edge hint drawn; " : "), nothing drawn; ");
                    continue;
                }
                Color ink = m.belowHorizon() ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                        : (dark ? Color.WHITE : Color.BLACK);
                if (m.sun()) {
                    sunMark(g, c, radius, ink);
                } else {
                    double[][] t = tangents(projection, mapping, m.at());
                    moonMark(g, c, radius, m, t, ink, dark);
                }
                g.setColor(ink);
                g.setFont(g.getFont().deriveFont(12f));
                g.drawString(m.label() + (m.belowHorizon() ? " (below the horizon)" : ""),
                        (float) (c.x() + radius + 4), (float) (c.y() + 4));
                drawnNote.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) r=%.1f px; ",
                        m.label(), c.x(), c.y(), radius));
            }
            g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(11f));
            g.drawString("MOCKUP - study ink over a production page (#414)", 8, PAGE_H - 8);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        p("### " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + drawnNote.toString().strip());
        p("");
    }

    private static void sunMark(Graphics2D g, PixelPoint c, double r, Color ink) {
        g.setColor(ink);
        g.setStroke(new BasicStroke(Math.max(1f, (float) (r / 12.0))));
        g.draw(new Ellipse2D.Double(c.x() - r, c.y() - r, 2 * r, 2 * r));
        double dot = Math.max(1.5, r / 6.0);
        g.fill(new Ellipse2D.Double(c.x() - dot, c.y() - dot, 2 * dot, 2 * dot));
    }

    private static void moonMark(Graphics2D g, PixelPoint c, double r, Mark m,
                                 double[][] tangents, Color ink, boolean dark) {
        double chi = Math.toRadians(m.chiDegrees());
        double[] n = tangents[0];
        double[] e = tangents[1];
        // The lit side's direction on the page: cos χ n̂ + sin χ ê.
        double dx = Math.cos(chi) * n[0] + Math.sin(chi) * e[0];
        double dy = Math.cos(chi) * n[1] + Math.sin(chi) * e[1];
        double angle = Math.atan2(dy, dx);
        Ellipse2D disc = new Ellipse2D.Double(c.x() - r, c.y() - r, 2 * r, 2 * r);
        Color unlit = dark ? new Color(40, 40, 40) : new Color(120, 120, 120);
        Color lit = dark ? Color.WHITE : new Color(250, 250, 240);
        g.setColor(unlit);
        g.fill(disc);
        // Lit region: the half-disc facing the bright limb, plus or minus
        // the half-ellipse of axis ratio |cos i| (Meeus 48, as drawn).
        double ratio = Math.abs(Math.cos(Math.toRadians(m.phaseAngleDegrees())));
        AffineTransform to = new AffineTransform();
        to.translate(c.x(), c.y());
        to.rotate(angle);
        Area half = new Area(new Rectangle2D.Double(0, -r - 1, r + 1, 2 * r + 2));
        half.intersect(new Area(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r)));
        Area ellipse = new Area(new Ellipse2D.Double(-r * ratio, -r, 2 * r * ratio, 2 * r));
        Area litArea = new Area(half);
        if (m.illuminated() >= 0.5) {
            Area far = new Area(new Rectangle2D.Double(-r - 1, -r - 1, r + 1, 2 * r + 2));
            far.intersect(ellipse);
            litArea.add(far);
        } else {
            Area near = new Area(new Rectangle2D.Double(0, -r - 1, r + 1, 2 * r + 2));
            near.intersect(ellipse);
            litArea.subtract(near);
        }
        litArea.transform(to);
        g.setColor(lit);
        g.fill(litArea);
        g.setColor(ink);
        g.setStroke(new BasicStroke(Math.max(1f, (float) (r / 40.0))));
        g.draw(disc);
        // A short line from the centre towards the bright limb, for the study only.
        g.setColor(new Color(200, 60, 60));
        g.draw(new Line2D.Double(c.x(), c.y(), c.x() + dx * r * 1.3, c.y() + dy * r * 1.3));
    }

    private static void edgeHint(Graphics2D g, PixelPoint c, Mark m, boolean dark) {
        double x = Math.max(6, Math.min(PAGE_W - 6, c.x()));
        double y = Math.max(6, Math.min(PAGE_H - 6, c.y()));
        g.setColor(dark ? Color.WHITE : Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(x - 5, y - 5, x + 5, y + 5));
        g.draw(new Line2D.Double(x - 5, y + 5, x + 5, y - 5));
        g.setFont(g.getFont().deriveFont(12f));
        g.drawString(m.label() + " ↤", (float) Math.min(x + 8, PAGE_W - 40), (float) y + 4);
    }

    // ---- D. the proposals -----------------------------------------------

    private static void proposals() {
        p("## D. What the measurements say, and what is proposed for ruling");
        p("");
        p("1. **True angular diameter versus a minimum visible mark.** On every"
                + " page the atlas draws, a true-scale disc is larger than the"
                + " atlas's minimum visible ink, and at 36° and below larger than"
                + " the brightest star it draws. There is no page on which the Sun"
                + " or the Moon would vanish at true scale, so no minimum mark is"
                + " needed and none is proposed: **the discs are drawn at true"
                + " scale, always, and never enlarged.** The one place a disc"
                + " becomes small is the whole-hemisphere globe, where it is still"
                + " above the minimum ink.");
        p("2. **Legibility at wide fields without false enlargement.** At 36° the"
                + " discs are about 13 px, which is what makes them findable, and"
                + " honest: the Moon does look larger than any star. What tells a"
                + " reader which body it is, is the mark's own ink, not its size: the"
                + " Sun as a ring with its centre dot, the Moon as a disc with its"
                + " terminator - the two astronomical signs drawn at the bodies'"
                + " real size, with no glyph and no font.");
        p("3. **Phase orientation after projection.** Section B's contract: the"
                + " page's n̂ and ê at the Moon's position, the lit direction"
                + " cos χ · n̂ + sin χ · ê, the terminator's axis ratio |cos i|, and"
                + " a round-trip reference test before rendering. The mockups at"
                + " 1°, 3° and 8° draw it.");
        p("4. **Off the page, below the horizon, under other ink.** Off the page:"
                + " proposal, nothing - a body is a page's object like a star, and"
                + " the tables say where it is; the edge-tick mockup is the"
                + " alternative. Below a drawn horizon: proposal, drawn dimmed in the"
                + " ground's ink with its status in the label, so a reader sees"
                + " where it will rise; the omitted mockup is the alternative. Under"
                + " other ink: the disc draws over the stars it hides, its dark side"
                + " inked, so hidden stars are seen to be hidden; the Sun's ring"
                + " draws over stars too, and daylight is not depicted in 4.0 - the"
                + " page is a chart, and the Sun on it says the sky is bright.");
        p("5. **Instant versus range.** Proposal: the page shows one instant, Place"
                + " and Time's, and states it in the label; a range is the table's,"
                + " not the page's, and tracks stay excluded.");
        p("6. **Selection and emphasis.** Proposal: the two bodies are one chart"
                + " module contributing its marks through the existing overlay seam,"
                + " not a planet framework; they are not selectable in 4.0 (the"
                + " inspector's subject stays the catalogue's), and they take no"
                + " part in the emphasis policy - a true-scale disc is its own"
                + " emphasis and needs none.");
        p("");
        p("Stop for the owner's visual and measurement ruling before any"
                + " production rendering.");
    }
}
