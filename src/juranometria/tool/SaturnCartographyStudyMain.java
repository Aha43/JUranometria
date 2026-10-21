package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PageWords;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.render.LabelPlacement;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sky.Observer;
import juranometria.solar.SaturnPack;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.DiscRelation;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.SaturnSystemService.ShadowRelation;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Saturn, its rings and the seven ruled moons on the atlas's pages,
 * measured, and the candidate rules drawn (Sprint 53, issue #578): what
 * drawing the system honestly needs, through the production projections
 * and the production renderer, with the study's own candidate marks
 * drawn over pages the production composition painted, as Jupiter's
 * study did (#482) and Mercury's (#539). No production Saturn layer
 * exists and none is proposed here beyond the rulings; every image is a
 * mock-up and says so in its corner.
 *
 * <p>The numbers come from the accepted Saturn service (#575) and the
 * ruled contract ({@code docs/decisions/saturn-system.md}): the
 * planetocentric ring opening B, the opening to the Sun B′, the two
 * flat annuli of the PDS radii, the polar diameter at B, the flattened
 * figure's six states, and each body's own astrometric J2000 place. The
 * closer fields are painted the way #539 painted them - a viewport below
 * the ladder is a thing a measurement may ask for - and the ladder
 * itself still refuses them.
 *
 * <p>Deterministic: the bundled packs, the contract's named instants,
 * instants the study derives from the service by stated rules, Oslo at
 * sea level, {@code Locale.ROOT} numbers. The portable report goes to
 * stdout ({@code make saturn-cartography-study}); the font-dependent
 * placements to {@code platform.md}; every page is written beside them.
 */
public final class SaturnCartographyStudyMain {

    private static final File DIR = new File("docs/studies/saturn-cartography");
    static final int PAGE_W = 900;
    static final int PAGE_H = 700;
    /** The contract's card instant (#574): the 2026 Oslo day nearest the Earth. */
    static final Instant CARD = Instant.parse("2026-10-05T00:00:00Z");
    /** The contract's named ring-plane crossing by the Earth (service-accuracy.md). */
    static final Instant EARTH_CROSSING = Instant.parse("2025-03-23T17:51:00Z");
    /** Titan's first greatest elongation of August 2026 (the contract's fixture). */
    static final Instant TITAN_ELONGATION = Instant.parse("2026-08-01T18:00:00Z");
    /** The contract's observer: Oslo at sea level. */
    static final Observer OSLO = new Observer(59.91, 10.75, CARD);
    /** The candidate closer fields, as #539 painted them. */
    static final double[] CLOSER_FIELDS = {0.5, 0.25};

    /** The atlas's smallest visible star mark, as a diameter. */
    private static final double MIN_INK_PX = 2.0 * StarSizePolicy.DEFAULT.minimumRadiusPx();
    /** The largest star mark, as a diameter. */
    private static final double MAX_STAR_PX = 2.0 * StarSizePolicy.DEFAULT.maximumRadiusPx();
    /**
     * Proposed: Saturn's minimum cartographic mark, Jupiter's ruled 6 px -
     * a circle standing for the whole figure, globe and rings, while the
     * rings' major axis is under it.
     */
    static final double SATURN_MARK_PX = JovianModule.MINIMUM_MARK_PX;
    /** Proposed: a moon's symbol, Jupiter's ruled 3 px - never an apparent size. */
    static final double MOON_SYMBOL_PX = JovianCartographyStudyMain.MOON_SYMBOL_PX;
    /** Proposed: two symbols are separable at this page distance, centre to centre (Jupiter's). */
    static final double SEPARABLE_PX = JovianCartographyStudyMain.SEPARABLE_PX;
    /** Proposed: rings whose minor axis is under this many pixels are drawn as a 1 px line - edge-on. */
    static final double EDGE_ON_PX = 2.0;
    /** The line's weight when the rings are edge-on. */
    static final double LINE_PX = 1.0;
    /** Proposed: the A Ring's outer edge gets a hairline outline only where the rings' minor axis is at least this. */
    static final double OUTLINE_MINOR_PX = 6.0;
    /** The proposed page identity (no production key exists). */
    static final String SATURN = "saturn";

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    /** The accepted service; set by main, and by the study's test. */
    static SaturnSystemService service;
    static SolarSystemService solar;

    private SaturnCartographyStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        DIR.mkdirs();
        service = SaturnSystemService.load();
        solar = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "Saturn's moon labels, as this desktop's fonts place them",
                "Sprint 53, issue #578.");
        p("# Saturn, its rings and the seven moons on the page: measurements and mock-ups");
        p("");
        p("Sprint 53, issue #578. What a cartography rule would mean on the atlas's own"
                + " pages, measured through the production projections and the accepted"
                + " Saturn service (#575) under the contract ruled on #574, with candidate"
                + " marks drawn by this study over pages the production composition painted,"
                + " as Jupiter's study did (#482) and Mercury's (#539). Nothing here is"
                + " production rendering; every image is a mock-up and says so in its corner."
                + " The closer fields are painted through viewports, as #539 painted them;"
                + " the ladder still refuses them. Regenerate with `make"
                + " saturn-cartography-study`.");
        p("");
        Ranges r = ranges();
        scale(r);
        thresholds(r);
        frame(r);
        mockups(r);
        rulings(r);
        System.out.print(REPORT);
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    // ---- the century's ranges ------------------------------------------

    /** What a page must hold: the extremes over the pack's years, weekly, geocentric. */
    record Ranges(double eqMin, double eqMax, double polMin, double polMax,
                  double ringMajorMin, double ringMajorMax, double ringMinorMax,
                  double gapMin, double gapMax, double cassiniMax,
                  double bMax, String bMaxAt, double bMin, String bMinAt, double flatMax,
                  int edgeOnWeeks, int unlitWeeks, int saturnSamples,
                  double[] moonMax, double extent, String extentAt, double titanMax,
                  double tableDifference, double poleAgreement, double poleTurnMax,
                  int moonSamples) {
    }

    static double aOuterKm() {
        return service.pack().constants().ringOuterRadiusKm();
    }

    static Ranges ranges() {
        SaturnPack.Constants k = service.pack().constants();
        double eqMin = 1e9, eqMax = 0, polMin = 1e9, polMax = 0;
        double rMajMin = 1e9, rMajMax = 0, rMinMax = 0, gapMin = 1e9, gapMax = 0, cassMax = 0;
        double bMax = -1e9, bMin = 1e9, flatMax = 0;
        String bMaxAt = "", bMinAt = "";
        int edgeOn = 0, unlit = 0, n = 0;
        Instant t = Instant.parse("1900-01-01T00:00:00Z");
        Instant end = Instant.parse("2101-01-01T00:00:00Z");
        while (t.isBefore(end)) {
            SaturnObservation s = service.observeSaturnGeocentric(t);
            n++;
            eqMin = Math.min(eqMin, s.equatorialDiameterArcseconds());
            eqMax = Math.max(eqMax, s.equatorialDiameterArcseconds());
            polMin = Math.min(polMin, s.polarDiameterArcseconds());
            polMax = Math.max(polMax, s.polarDiameterArcseconds());
            double major = 2.0 * s.ringSemiMajorArcseconds(k.ringOuterRadiusKm());
            double minor = 2.0 * s.ringSemiMinorArcseconds(k.ringOuterRadiusKm());
            rMajMin = Math.min(rMajMin, major);
            rMajMax = Math.max(rMajMax, major);
            rMinMax = Math.max(rMinMax, minor);
            double gap = s.ringSemiMajorArcseconds(k.ringInnerRadiusKm())
                    - s.equatorialDiameterArcseconds() / 2.0;
            gapMin = Math.min(gapMin, gap);
            gapMax = Math.max(gapMax, gap);
            cassMax = Math.max(cassMax, s.ringSemiMajorArcseconds(k.ringCassiniOuterRadiusKm())
                    - s.ringSemiMajorArcseconds(k.ringBOuterRadiusKm()));
            if (s.ringOpeningDegrees() > bMax) {
                bMax = s.ringOpeningDegrees();
                bMaxAt = day(t);
            }
            if (s.ringOpeningDegrees() < bMin) {
                bMin = s.ringOpeningDegrees();
                bMinAt = day(t);
            }
            if (minor * JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H) < EDGE_ON_PX) {
                edgeOn++;
            }
            flatMax = Math.max(flatMax, s.equatorialDiameterArcseconds() - s.polarDiameterArcseconds());
            if (s.unlitFaceSeen()) {
                unlit++;
            }
            t = t.plus(Duration.ofDays(7));
        }
        double[] mMax = new double[Moon.values().length];
        double extent = 0, titanMax = 0, tableDiff = 0, poleAgree = 0, poleTurn = 0;
        String extentAt = "";
        int m = 0;
        t = Instant.parse("2000-01-01T00:00:00Z");
        while (t.isBefore(end)) {
            Configuration c = service.observeMoonsGeocentric(t);
            m++;
            for (MoonPlace place : c.moons()) {
                int i = place.moon().ordinal();
                mMax[i] = Math.max(mMax[i], place.angularDiameterArcseconds());
                if (place.separationArcseconds() > extent) {
                    extent = place.separationArcseconds();
                    extentAt = name(place.moon()) + " " + day(t);
                }
                if (place.moon() == Moon.TITAN) {
                    titanMax = Math.max(titanMax, place.separationArcseconds());
                }
                double[] j2000 = j2000Offset(c.saturn(), place);
                tableDiff = Math.max(tableDiff, Math.hypot(j2000[0] - place.xArcseconds(),
                        j2000[1] - place.yArcseconds()));
            }
            double jd = service.timeScales().tt(t).jdTt();
            double derived = service.poleAngleJ2000Degrees(c.saturn());
            double carried = JovianChartGeometry.ofDateAngleCarriedToJ2000(
                    c.saturn().apparentOfDate(), c.saturn().poleAngleDegrees(), jd);
            poleAgree = Math.max(poleAgree, Math.abs(wrap(derived - carried)));
            poleTurn = Math.max(poleTurn, Math.abs(wrap(derived - c.saturn().poleAngleDegrees())));
            t = t.plus(Duration.ofDays(7));
        }
        return new Ranges(eqMin, eqMax, polMin, polMax, rMajMin, rMajMax, rMinMax, gapMin,
                gapMax, cassMax, bMax, bMaxAt, bMin, bMinAt, flatMax, edgeOn, unlit, n, mMax, extent,
                extentAt, titanMax, tableDiff, poleAgree, poleTurn, m);
    }

    /** A moon's offset from Saturn in J2000, arcseconds, east and north, from the two J2000 places. */
    static double[] j2000Offset(SaturnObservation s, MoonPlace m) {
        double dra = wrap(m.astrometricJ2000().raDegrees() - s.astrometricJ2000().raDegrees())
                * Math.cos(Math.toRadians(s.astrometricJ2000().decDegrees())) * 3600.0;
        double ddec = (m.astrometricJ2000().decDegrees() - s.astrometricJ2000().decDegrees()) * 3600.0;
        return new double[] {dra, ddec};
    }

    // ---- A. true scale ----------------------------------------------------

    private static List<Double> fields() {
        List<Double> fields = new ArrayList<>(ChartViewState.fieldWidthSteps());
        for (double f : CLOSER_FIELDS) {
            fields.add(f);
        }
        return fields;
    }

    private static void scale(Ranges r) {
        p("## A. True scale on every page");
        p("");
        p(String.format(Locale.ROOT, "Over 1900-2100 (%d weekly geocentric instants from the"
                + " service): Saturn's equatorial diameter is %.1f-%.1f″ and its polar diameter"
                + " at the ring opening %.1f-%.1f″; the A Ring's outer edge spans %.1f-%.1f″"
                + " across its major axis and at most %.1f″ across its minor axis (at the"
                + " widest opening, B = %+.2f° on %s; the widest the other way %+.2f° on %s);"
                + " the gap between the globe's limb and the C Ring's inner edge is"
                + " %.2f-%.2f″; the Cassini Division is at most %.2f″ wide. Over 2000-2100"
                + " (%d weekly instants) the largest moon disc is Titan's %.2f″; the system's"
                + " largest extent is a moon %.0f″ (%.1f′) from Saturn's centre (%s), Titan's"
                + " %.0f″. The atlas's smallest star mark is %.1f px across and its largest"
                + " %.1f px (the production star policy).", r.saturnSamples, r.eqMin, r.eqMax,
                r.polMin, r.polMax, r.ringMajorMin, r.ringMajorMax, r.ringMinorMax,
                r.bMax, r.bMaxAt, r.bMin, r.bMinAt, r.gapMin, r.gapMax, r.cassiniMax,
                r.moonSamples, r.moonMax[Moon.TITAN.ordinal()], r.extent, r.extent / 60.0,
                r.extentAt, r.titanMax, MIN_INK_PX, MAX_STAR_PX));
        p("");
        int a4W = (int) Math.round(PaperSize.A4.chartWidePoints()
                * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        p(String.format(Locale.ROOT, "Pixels on the 900 × 700 page at its centre (each field's"
                + " own projection) - the ladder's fields, then the two closer fields below"
                + " it; millimetres on the A4 sheet's chart area (%.0f mm wide, %d px at"
                + " %d dpi).", PaperSize.A4.chartWideMm(), a4W, PngSheetWriter.DEFAULT_RESOLUTION));
        p("");
        p("| field | globe equator px (min-max) | polar px (max) | rings major px (min-max) | rings minor px (max) | globe-ring gap px (max) | Cassini px (max) | Titan px | extent px | rings on A4 mm (max) |");
        p("|---:|---|---:|---|---:|---:|---:|---:|---:|---:|");
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double mmPerArcsec = PaperSize.A4.chartWideMm() / (field * 3600.0);
            p(String.format(Locale.ROOT, "| %s%s | %.2f-%.2f | %.2f | %.2f-%.2f | %.2f | %.2f | %.2f | %.3f | %.0f | %.2f |",
                    fieldName(field), field < ChartViewState.normalMinimumFieldDegrees()
                            ? " (closer)" : "", r.eqMin * per, r.eqMax * per, r.polMax * per,
                    r.ringMajorMin * per, r.ringMajorMax * per, r.ringMinorMax * per,
                    r.gapMax * per, r.cassiniMax * per, r.moonMax[Moon.TITAN.ordinal()] * per,
                    r.extent * per, r.ringMajorMax * mmPerArcsec));
        }
        p("");
    }

    static String fieldName(double field) {
        return fmt(field) + "°";
    }

    // ---- B. thresholds ------------------------------------------------------

    private static void thresholds(Ranges r) {
        p("## B. Where things become resolvable or separable");
        p("");
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p(String.format(Locale.ROOT, "- **The rings' major axis reaches a pixel** at %.1f° at"
                + " its largest and %.1f° at its smallest, and **Jupiter's ruled %.0f px"
                + " minimum mark** at %.2f° and %.2f°: on the ordinary ladder the true rings"
                + " are wider than the mark only at 1°, where they are %.1f-%.1f px across.",
                r.ringMajorMax * at1, r.ringMajorMin * at1, SATURN_MARK_PX,
                r.ringMajorMax * at1 / SATURN_MARK_PX, r.ringMajorMin * at1 / SATURN_MARK_PX,
                r.ringMajorMin * at1, r.ringMajorMax * at1));
        p(String.format(Locale.ROOT, "- **The globe reaches the atlas's smallest star mark**"
                + " (%.1f px) at %.1f° and %.1f°, and the %.0f px mark at %.2f° and %.2f°: on"
                + " every ladder field the globe alone is under the mark (%.1f-%.1f px at 1°);"
                + " it passes it only on the closer fields.", MIN_INK_PX, r.eqMax * at1 / MIN_INK_PX,
                r.eqMin * at1 / MIN_INK_PX, SATURN_MARK_PX, r.eqMax * at1 / SATURN_MARK_PX,
                r.eqMin * at1 / SATURN_MARK_PX, r.eqMin * at1, r.eqMax * at1));
        p(String.format(Locale.ROOT, "- **The rings separate from the globe** - the gap"
                + " between the limb and the C Ring's inner edge a whole pixel - only at %.2f°"
                + " at the largest gap and %.2f° at the smallest: at 1° the gap is %.2f-%.2f px,"
                + " so the globe and the rings merge into one figure; at 0.5° it is %.2f-%.2f px"
                + " and at 0.25° %.2f-%.2f px.", r.gapMax * at1, r.gapMin * at1,
                r.gapMin * at1, r.gapMax * at1, r.gapMin * at1 * 2, r.gapMax * at1 * 2,
                r.gapMin * at1 * 4, r.gapMax * at1 * 4));
        p(String.format(Locale.ROOT, "- **The Cassini Division** is at most %.2f px at 1°,"
                + " %.2f px at 0.5° and %.2f px at 0.25°: the gap between the ruled annuli"
                + " is under a pixel on every ladder field and about one pixel on the"
                + " closest page. The annuli are drawn as the contract rules them; where the"
                + " gap is under a pixel they read as one band.", r.cassiniMax * at1,
                r.cassiniMax * at1 * 2, r.cassiniMax * at1 * 4));
        p(String.format(Locale.ROOT, "- **Edge-on.** In %d of the %d weeks of 1900-2100 the"
                + " rings' minor axis is under %.0f px at 1° (%.1f %%): too thin for a filled"
                + " band with a hole, so the rings are drawn as a 1 px line through the globe."
                + " In %d weeks (%.1f %%) the unlit face is seen.",
                r.edgeOnWeeks, r.saturnSamples, EDGE_ON_PX, 100.0 * r.edgeOnWeeks / r.saturnSamples,
                r.unlitWeeks, 100.0 * r.unlitWeeks / r.saturnSamples));
        p(String.format(Locale.ROOT, "- **Saturn's oblateness** - the equatorial and polar"
                + " diameters a whole pixel apart at one instant - is resolved at %.2f° on the"
                + " 900 px page at the most (%.2f px apart at 1°): on the closer fields only."
                + " The rings carry the figure's shape on every page where they are drawn true.",
                r.flatMax * at1, r.flatMax * at1));
        p(String.format(Locale.ROOT, "- **The moons' true discs are below a pixel on every"
                + " page** (Titan, the largest, %.2f px at 1°, %.2f px at 0.25°): any moon"
                + " mark is a symbol. Proposed symbol %.0f px, Jupiter's; two symbols are"
                + " separable %.0f px apart (centre to centre).",
                r.moonMax[Moon.TITAN.ordinal()] * at1, r.moonMax[Moon.TITAN.ordinal()] * at1 * 4,
                MOON_SYMBOL_PX, SEPARABLE_PX));
        p(String.format(Locale.ROOT, "- **The inner moons keep to the rings' line.** At 1° the"
                + " A Ring's semi-major axis is %.1f-%.1f px while Mimas's and Enceladus's"
                + " separations reach at most %.1f and %.1f px (the weekly 1900-2100 maxima of"
                + " 32.2″ and 40.9″ in the contract's `measurements.md`): their marks stand"
                + " within a few pixels of the figure's edge along its major axis, and inside"
                + " the ring ellipse when the rings are open (the *over the rings* column"
                + " below).", r.ringMajorMin * at1 / 2, r.ringMajorMax * at1 / 2,
                32.2 * at1, 40.9 * at1));
        p("");
        p("**Each moon decided on its own (Jupiter's ruled rule 4, with the rings as the"
                + " primary's drawn extent).** Over 2026 (every 6 hours at Oslo, 1 460"
                + " configurations, 10 220 moon places) each moon is decided independently by"
                + " the study's `decide`: behind Saturn, omitted; at the chart's normal minimum"
                + " field (" + fmt(ChartViewState.normalMinimumFieldDegrees())
                + "°, read from the chart and never assumed permanent) and on the closer fields"
                + " every other moon drawn at its exact J2000 place, overlap allowed; above it, a"
                + " moon drawn only when its own " + (int) MOON_SYMBOL_PX + " px mark is"
                + " distinguishable - " + (int) SEPARABLE_PX + " px clear of the drawn figure's"
                + " edge (the ring ellipse where the rings are drawn true, the "
                + (int) SATURN_MARK_PX + " px mark otherwise) and of every higher-priority moon"
                + " already drawn (in front, then clear, then shadowed; then by size, Titan,"
                + " Rhea, Iapetus, Dione, Tethys, Enceladus, Mimas), and a moon in front only"
                + " when the drawn globe is at least " + (int) (2 * SEPARABLE_PX) + " px. A"
                + " collision suppresses only the lower-priority mark. Shares are of the moon"
                + " places that are not behind Saturn. *Over the rings* counts drawn moons whose"
                + " page position lies inside the ring ellipse and outside the globe - geometry,"
                + " not a state (ruling 5 of #574).");
        p("");
        p("| field | drawn | at the figure's edge | collides | in front, unresolved | over the rings | configurations with a moon drawn | one suppressed, another drawn |");
        p("|---:|---:|---:|---:|---:|---:|---:|---:|");
        List<Configuration> year = year6h();
        List<Double> ladder = new ArrayList<>(List.of(12.0, 8.0, 6.0, 4.0, 3.0, 2.0,
                ChartViewState.normalMinimumFieldDegrees()));
        for (double f : CLOSER_FIELDS) {
            ladder.add(f);
        }
        java.awt.FontMetrics font;
        Graphics2D probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            font = ChartRenderer.TextMetrics.of(probe).labels();
        } finally {
            probe.dispose();
        }
        List<String> labelRows = new ArrayList<>();
        for (double field : ladder) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            int[] count = new int[Decision.values().length];
            int seen = 0, anyDrawn = 0, independent = 0, overRings = 0;
            int placed = 0, refused = 0, mixed = 0, allRefused = 0, moved = 0;
            for (Configuration c : year) {
                Figure fig = figure(c.saturn(), per, 0.0);
                List<double[]> off = pageOffsets(c, per, fig);
                List<Decision> d = decide(field, ChartViewState.normalMinimumFieldDegrees(),
                        fig, c.moons(), off);
                int drawnHere = 0;
                boolean suppressed = false;
                for (int i = 0; i < d.size(); i++) {
                    Decision x = d.get(i);
                    count[x.ordinal()]++;
                    if (x != Decision.BEHIND) {
                        seen++;
                    }
                    if (x == Decision.DRAWN) {
                        drawnHere++;
                        if (fig.rings && fig.overRings(off.get(i))) {
                            overRings++;
                        }
                    }
                    suppressed |= x == Decision.COLLIDES || x == Decision.AT_SATURN
                            || x == Decision.IN_FRONT_UNRESOLVED;
                }
                anyDrawn += drawnHere > 0 ? 1 : 0;
                independent += suppressed && drawnHere > 0 ? 1 : 0;
                int[] l = labels(fig, c, off, d, font);
                placed += l[0];
                refused += l[1];
                moved += l[2];
                mixed += l[0] > 0 && l[1] > 0 ? 1 : 0;
                allRefused += l[0] == 0 && l[1] > 0 ? 1 : 0;
            }
            p(String.format(Locale.ROOT, "| %s%s | %.1f %% | %.1f %% | %.1f %% | %.1f %% | %.1f %% | %.1f %% | %d |",
                    fieldName(field), field < ChartViewState.normalMinimumFieldDegrees() ? " (closer)" : "",
                    100.0 * count[Decision.DRAWN.ordinal()] / seen,
                    100.0 * count[Decision.AT_SATURN.ordinal()] / seen,
                    100.0 * count[Decision.COLLIDES.ordinal()] / seen,
                    100.0 * count[Decision.IN_FRONT_UNRESOLVED.ordinal()] / seen,
                    100.0 * overRings / seen, 100.0 * anyDrawn / year.size(), independent));
            labelRows.add(String.format(Locale.ROOT, "| %s | %d | %d | %d | %d | %d |", fieldName(field),
                    placed, moved, refused, mixed, allRefused));
            if (allRefused > 0) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "at %s %d configurations lose every moon label (Jupiter's ruling 6 forbids it)",
                        fieldName(field), allRefused));
            }
        }
        p("");
        p("**Labels, decided separately from marks (Jupiter's ruled rule 6).** For every drawn"
                + " moon, a label request through the production `LabelPlacement`, with"
                + " production's eight adjacent candidates (LabelGeometry's order, 3 px gap),"
                + " the drawn figure, every drawn mark and Saturn's label as obstacles, on an"
                + " otherwise empty 900 × 700 page; a label is refused when none of its"
                + " candidates is free and withdrawn, so it blocks no other. How many labels are"
                + " placed, moved or refused depends on this desktop's font metrics, which is"
                + " what production uses, so those counts are **this machine's answer** and are"
                + " in `platform.md`. What holds on every platform, enforced each time this"
                + " study runs (it refuses to finish otherwise): **no configuration at any field"
                + " loses every moon label.**");
        p("");
        q("## Moon labels over 2026, by field");
        q("");
        q("Every 6 hours at Oslo, 1 460 configurations, each moon decided by the portable"
                + " report's rule; the label font is "
                + JovianCartographyStudyMain.fontIdentity(font) + ". *Moved* counts labels that"
                + " took a candidate other than the right-hand one; a refused label is one"
                + " production would have placed under duress.");
        q("");
        q("| field | moon labels placed | of which moved | refused | configurations with both | configurations with every moon label refused |");
        q("|---:|---:|---:|---:|---:|---:|");
        labelRows.forEach(SaturnCartographyStudyMain::q);
        q("");
        q("**No configuration loses every moon label** at any field on this machine.");
        q("");
        p("**A finding for the rulings:** at 1° and on the closer fields the seven moons crowd"
                + " within a few tens of pixels of the figure, and on this machine a third of"
                + " their labels are refused (`platform.md`): every mark is drawn, but the names"
                + " of the inner moons often have no clean box. Jupiter's four moons never met"
                + " this at 1°.");
        p("");
    }

    private static List<Configuration> year6h() {
        List<Configuration> year = new ArrayList<>();
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            year.add(service.observeMoons(OSLO.at(t)));
        }
        return year;
    }

    /**
     * Saturn's drawn figure on a page, in pixels about its centre: the
     * rings' outer ellipse (semi-axes {@code a}, {@code b}, major axis
     * along {@code majorPx}, a unit page vector) when the rings are drawn
     * true, else the minimum mark as a circle; and the globe's ellipse.
     */
    record Figure(boolean rings, double a, double b, double[] majorPx, double globeEq,
                  double globePol, double[] polePx, double markPx) {

        /** The drawn figure's extent along a page direction from its centre. */
        double radiusAlong(double[] offset) {
            if (!rings) {
                return markPx / 2.0;
            }
            double len = Math.hypot(offset[0], offset[1]);
            if (len == 0) {
                return a;
            }
            double cos = (offset[0] * majorPx[0] + offset[1] * majorPx[1]) / len;
            double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
            double denom = Math.hypot(b * cos, a * sin);
            return denom == 0 ? a : a * b / denom;
        }

        /** Whether a page offset lies inside the ring ellipse and outside the globe. */
        boolean overRings(double[] offset) {
            double len = Math.hypot(offset[0], offset[1]);
            if (len == 0) {
                return false;
            }
            double cos = (offset[0] * majorPx[0] + offset[1] * majorPx[1]) / len;
            double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
            boolean insideRings = (len * cos / a) * (len * cos / a) + (len * sin / b) * (len * sin / b) < 1.0;
            double gc = (offset[0] * polePx[0] + offset[1] * polePx[1]) / len;
            double gs = Math.sqrt(Math.max(0.0, 1.0 - gc * gc));
            boolean insideGlobe = (len * gc / (globePol / 2)) * (len * gc / (globePol / 2))
                    + (len * gs / (globeEq / 2)) * (len * gs / (globeEq / 2)) < 1.0;
            return insideRings && !insideGlobe;
        }
    }

    /**
     * The figure at a scale, with the pole pointing page-up (east left)
     * when no page is given: the frame the statistics use. A page gives
     * its own pole direction through {@link #figureOn}.
     */
    static Figure figure(SaturnObservation s, double perArcsec, double pageUpAngle) {
        double[] pole = {-Math.sin(Math.toRadians(pageUpAngle)), -Math.cos(Math.toRadians(pageUpAngle))};
        return figureWith(s, perArcsec, pole);
    }

    static Figure figureWith(SaturnObservation s, double perArcsec, double[] polePx) {
        double aOuter = aOuterKm();
        double a = s.ringSemiMajorArcseconds(aOuter) * perArcsec;
        double b = Math.max(s.ringSemiMinorArcseconds(aOuter) * perArcsec, LINE_PX / 2.0);
        double[] major = {-polePx[1], polePx[0]};
        boolean rings = 2.0 * a >= SATURN_MARK_PX;
        return new Figure(rings, a, b, major, s.equatorialDiameterArcseconds() * perArcsec,
                s.polarDiameterArcseconds() * perArcsec, polePx, SATURN_MARK_PX);
    }

    /** The figure on a real page: the pole derived in the chart's frame, turned by the page's tangents. */
    static Figure figureOn(SaturnObservation s, Projection projection, ViewportMapping mapping,
                           double zoom) {
        double per = pxPerArcsecAt(projection, mapping, s.astrometricJ2000()) * zoom;
        double[] up = JovianChartGeometry.pageDirection(projection, mapping, s.astrometricJ2000(),
                service.poleAngleJ2000Degrees(s));
        return figureWith(s, per, up);
    }

    /** Each moon's page offset from Saturn's centre at a scale, in the figure's frame (east left, north up). */
    static List<double[]> pageOffsets(Configuration c, double perArcsec, Figure fig) {
        List<double[]> out = new ArrayList<>();
        for (MoonPlace m : c.moons()) {
            double[] off = j2000Offset(c.saturn(), m);
            out.add(new double[] {-off[0] * perArcsec, -off[1] * perArcsec});
        }
        return out;
    }

    /** Why a moon is or is not drawn on a page (Jupiter's ruling 4, with Saturn's figure). */
    enum Decision {
        DRAWN, BEHIND, AT_SATURN, COLLIDES, IN_FRONT_UNRESOLVED
    }

    /** The order a collision is settled in: in front, then clear, then shadowed; then the larger moon. */
    static final List<Moon> SIZE_ORDER = List.of(Moon.TITAN, Moon.RHEA, Moon.IAPETUS, Moon.DIONE,
            Moon.TETHYS, Moon.ENCELADUS, Moon.MIMAS);

    static int priority(MoonPlace m) {
        int state = m.discRelation() == DiscRelation.IN_FRONT ? 0
                : m.shadowRelation() == ShadowRelation.SUNLIT ? 1 : 2;
        return state * 10 + SIZE_ORDER.indexOf(m.moon());
    }

    static List<Decision> decide(double fieldDegrees, double normalMinimumDegrees, Figure fig,
                                 List<MoonPlace> moons, List<double[]> offsetsPx) {
        Decision[] out = new Decision[moons.size()];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < moons.size(); i++) {
            order.add(i);
        }
        order.sort((x, y) -> Integer.compare(priority(moons.get(x)), priority(moons.get(y))));
        boolean floor = fieldDegrees <= normalMinimumDegrees + 1e-9;
        List<double[]> drawn = new ArrayList<>();
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i : order) {
            MoonPlace m = moons.get(i);
            double[] at = offsetsPx.get(i);
            if (m.discRelation() == DiscRelation.BEHIND) {
                out[i] = Decision.BEHIND;
                continue;
            }
            if (floor) {
                out[i] = Decision.DRAWN;
                drawn.add(at);
                continue;
            }
            if (m.discRelation() == DiscRelation.IN_FRONT) {
                out[i] = fig.globeEq >= 2.0 * SEPARABLE_PX ? Decision.DRAWN
                        : Decision.IN_FRONT_UNRESOLVED;
            } else if (Math.hypot(at[0], at[1]) - fig.radiusAlong(at) < SEPARABLE_PX / 2.0 + r) {
                out[i] = Decision.AT_SATURN;
            } else {
                out[i] = Decision.DRAWN;
                for (double[] other : drawn) {
                    if (Math.hypot(at[0] - other[0], at[1] - other[1]) < SEPARABLE_PX) {
                        out[i] = Decision.COLLIDES;
                        break;
                    }
                }
            }
            if (out[i] == Decision.DRAWN) {
                drawn.add(at);
            }
        }
        return List.of(out);
    }

    /** The label outcome for one configuration on an empty page: placed, refused, moved off the right. */
    private static int[] labels(Figure fig, Configuration c, List<double[]> off, List<Decision> d,
                                java.awt.FontMetrics font) {
        double cx = PAGE_W / 2.0;
        double cy = PAGE_H / 2.0;
        double r = MOON_SYMBOL_PX / 2.0;
        List<LabelPlacement.Obstacle> marks = new ArrayList<>();
        List<LabelPlacement.Request> requests = new ArrayList<>();
        double reach = fig.rings ? fig.a : fig.markPx / 2.0;
        marks.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK, SATURN,
                new Ellipse2D.Double(cx - reach, cy - reach, 2 * reach, 2 * reach)));
        requests.add(new LabelPlacement.Request(LabelPlacement.Family.STAR, SATURN, "Saturn", cx, cy,
                JovianCartographyStudyMain.around(cx, cy, reach, font.stringWidth("Saturn"),
                        font.getHeight(), font.getAscent()), SATURN, null, false, 0));
        for (int i = 0; i < d.size(); i++) {
            if (d.get(i) != Decision.DRAWN) {
                continue;
            }
            String id = c.moons().get(i).moon().name();
            String text = name(c.moons().get(i).moon());
            double x = cx + off.get(i)[0];
            double y = cy + off.get(i)[1];
            marks.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK, id,
                    new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r)));
            requests.add(new LabelPlacement.Request(LabelPlacement.Family.STAR, id, text, x, y,
                    JovianCartographyStudyMain.around(x, y, r, font.stringWidth(text),
                            font.getHeight(), font.getAscent()), id, null, false, i + 1));
        }
        int[] out = new int[3];
        for (LabelPlacement.Placement placed : JovianCartographyStudyMain.placeRefusing(PAGE_W, PAGE_H,
                marks, LabelPlacement.Page.paper(PAGE_W, PAGE_H), requests)) {
            if (placed.request().id().equals(SATURN)) {
                continue;
            }
            if (placed.omitted()) {
                out[1]++;
            } else {
                out[0]++;
                out[2] += placed.candidate() != 0 ? 1 : 0;
            }
        }
        return out;
    }

    // ---- C. the frame contract ------------------------------------------------

    private static void frame(Ranges r) {
        p("## C. The frame contract");
        p("");
        p("The chart is J2000 (ICRS); the table's X/Y, pole angle and ring opening are"
                + " apparent and of date. As Jupiter's contract rules and `JovianChartFrameTest`"
                + " holds, the study rotates nothing by an assumed correction:");
        p("");
        p("- **Positions**: each body's own astrometric J2000 place from the service - Saturn's"
                + " and each moon's - through the page's ordinary projection.");
        p(String.format(Locale.ROOT, "- **The pole**: the PCK pole vector is stated in the ICRS,"
                + " so its direction at Saturn's J2000 place is derived there"
                + " (`SaturnSystemService.poleAngleJ2000Degrees`, Jupiter's rule) and turned onto"
                + " the page by the page's projected north and east. Over 2000-2100 the derived"
                + " angle agrees with the table's of-date pole angle carried back by the atlas's"
                + " own rotation to **%.4f°**; the two frames' angles differ by up to %.2f°, which"
                + " is why the table's angle is never drawn as it stands.", r.poleAgreement, r.poleTurnMax));
        p("- **The rings on the page**: the major axis lies along the pole's page direction"
                + " turned by 90°; each edge's semi-axes are asin(R/Δ) and asin(R/Δ)·|sin B| in"
                + " the page's pixels per arcsecond at Saturn - the contract's B, planetocentric,"
                + " never Horizons' planetodetic latitude. The polar diameter is the contract's,"
                + " taken at B.");
        p("- **The near half**: the half of the rings in front of the globe lies away from the"
                + " projected north pole when B > 0 and toward it when B < 0 (a ring point toward"
                + " the observer projects at −sin B cos B along the projected pole; the contract's"
                + " rule). The study paints the far half, then the globe, then the near half.");
        p(String.format(Locale.ROOT, "- **The table's offsets are not the page's**: a moon's J2000"
                + " offset from Saturn differs from the table's apparent X/Y by up to **%.2f″**"
                + " over 2000-2100, so a page draws each moon at its own J2000 place, never at"
                + " Saturn's place plus X/Y.", r.tableDifference));
        p("- **Round trips**: `SaturnCartographyTest` holds that every body's J2000 place"
                + " projects onto the 1°, 0.5° and 0.25° pages and back within a microarcsecond,"
                + " that the ring ellipse's axes on the page are the contract's semi-axes at the"
                + " page's scale, and that the near half is on the ruled side of the pole.");
        p("");
    }

    // ---- D. mock-ups ------------------------------------------------------------

    /** A page's Saturn: the configuration and whether the horizon is drawn for it. */
    record Scene(Configuration c, boolean horizon, Observer observer, boolean sun) {
    }

    private static Instant widestOpening(boolean north) {
        Instant best = null;
        double extreme = 0;
        for (Instant t = Instant.parse("2000-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2101-01-01T00:00:00Z")); t = t.plus(Duration.ofDays(7))) {
            double b = service.observeSaturnGeocentric(t).ringOpeningDegrees();
            if (north ? b > extreme : b < extreme) {
                extreme = b;
                best = t;
            }
        }
        return best;
    }

    /** The 2025 day between the Earth's and the Sun's crossings where the unlit face is seen most openly. */
    private static Instant unlitFace() {
        Instant best = null;
        double widest = -1;
        for (Instant t = Instant.parse("2025-03-01T00:00:00Z");
                t.isBefore(Instant.parse("2025-06-01T00:00:00Z")); t = t.plus(Duration.ofDays(1))) {
            SaturnObservation s = service.observeSaturnGeocentric(t);
            if (s.unlitFaceSeen() && Math.abs(s.ringOpeningDegrees()) > widest) {
                widest = Math.abs(s.ringOpeningDegrees());
                best = t;
            }
        }
        return best;
    }

    /** The hours of 2026 at Oslo in the search order: from July, when the rings are open, then the year's start. */
    private static Iterable<Instant> searchHours() {
        List<Instant> hours = new ArrayList<>();
        Instant july = Instant.parse("2026-07-01T00:00:00Z");
        Instant end = Instant.parse("2027-01-01T00:00:00Z");
        for (Instant t = july; t.isBefore(end); t = t.plus(Duration.ofHours(1))) {
            hours.add(t);
        }
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z"); t.isBefore(july); t = t.plus(Duration.ofHours(1))) {
            hours.add(t);
        }
        return hours;
    }

    /** The first hour at Oslo, from July 2026, with a moon in the state. */
    private static Configuration firstWith(VisibilityState state, Moon which) {
        for (Instant t : searchHours()) {
            Configuration c = service.observeMoons(OSLO.at(t));
            for (MoonPlace m : c.moons()) {
                if (m.state() == state && (which == null || m.moon() == which)) {
                    return c;
                }
            }
        }
        throw new IllegalStateException("no 2026 hour at Oslo with a moon " + state);
    }

    /** The 2026 day at Oslo with Iapetus farthest from Saturn. */
    private static Instant iapetusFarthest() {
        Instant best = null;
        double most = 0;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofDays(1))) {
            double sep = service.observeMoons(OSLO.at(t)).moon(Moon.IAPETUS).separationArcseconds();
            if (sep > most) {
                most = sep;
                best = t;
            }
        }
        return best;
    }

    /** The 2026 configuration (6-hourly, Oslo) with all seven moons clear and the closest pair nearest. */
    private static Configuration crowded(List<Configuration> year) {
        Configuration best = null;
        double least = Double.MAX_VALUE;
        for (Configuration c : year) {
            if (c.moons().stream().anyMatch(m -> m.state() != VisibilityState.CLEAR_OF_SATURN)) {
                continue;
            }
            for (int a = 0; a < c.moons().size(); a++) {
                for (int b = a + 1; b < c.moons().size(); b++) {
                    double[] oa = j2000Offset(c.saturn(), c.moons().get(a));
                    double[] ob = j2000Offset(c.saturn(), c.moons().get(b));
                    double d = Math.hypot(oa[0] - ob[0], oa[1] - ob[1]);
                    if (d < least) {
                        least = d;
                        best = c;
                    }
                }
            }
        }
        return best;
    }

    /** The day of a year with Saturn's elongation from the Sun greatest (opposition) or least (conjunction). */
    private static Instant elongationExtreme(int year, boolean greatest) {
        Instant best = null;
        double extreme = greatest ? -1 : 1e9;
        for (Instant t = Instant.parse(year + "-01-01T00:00:00Z");
                t.isBefore(Instant.parse((year + 1) + "-01-01T00:00:00Z")); t = t.plus(Duration.ofDays(1))) {
            double e = service.observeSaturnGeocentric(t).elongationDegrees();
            if (greatest ? e > extreme : e < extreme) {
                extreme = e;
                best = t;
            }
        }
        return best;
    }

    /** The first 2026 December evening hour at Oslo with Saturn between 1° and 4° below the horizon. */
    private static Instant belowTheHorizon() {
        for (Instant t = Instant.parse("2026-12-01T12:00:00Z");; t = t.plus(Duration.ofMinutes(10))) {
            double alt = service.observeSaturn(OSLO.at(t)).horizontal().altitudeDegrees();
            if (alt < -1.0 && alt > -4.0) {
                return t;
            }
        }
    }

    private static String when(Instant t) {
        return t.toString().replace("T", " ").replace(":00Z", " UTC");
    }

    private static String day(Instant t) {
        return t.toString().substring(0, 10);
    }

    private static String states(Configuration c) {
        StringBuilder s = new StringBuilder("States: ");
        for (MoonPlace m : c.moons()) {
            s.append(name(m.moon())).append(' ').append(word(m.state())).append("; ");
        }
        return s.toString().strip();
    }

    static String name(Moon m) {
        return m.name().charAt(0) + m.name().substring(1).toLowerCase(Locale.ROOT);
    }

    /** The state in the table's English words. */
    static String word(VisibilityState state) {
        return switch (state) {
            case BEHIND_SATURN_IN_ITS_SHADOW -> "behind Saturn, in its shadow";
            case BEHIND_SATURN -> "behind Saturn";
            case IN_FRONT_OF_SATURN -> "in front of Saturn";
            case IN_SATURNS_SHADOW -> "in Saturn's shadow";
            case PARTLY_IN_SATURNS_SHADOW -> "partly in Saturn's shadow";
            case CLEAR_OF_SATURN -> "clear of Saturn";
        };
    }

    private static String ringsCaption(SaturnObservation s, double per) {
        return String.format(Locale.ROOT, "B = %+.2f° (%s face%s), B′ = %+.2f°; the A Ring's"
                + " outer edge %.1f × %.1f px, the globe %.1f × %.1f px here.",
                s.ringOpeningDegrees(), s.ringOpeningDegrees() >= 0 ? "north" : "south",
                s.unlitFaceSeen() ? ", the unlit face seen" : "", s.sunRingOpeningDegrees(),
                2 * s.ringSemiMajorArcseconds(aOuterKm()) * per,
                2 * s.ringSemiMinorArcseconds(aOuterKm()) * per,
                s.equatorialDiameterArcseconds() * per, s.polarDiameterArcseconds() * per);
    }

    private static void mockups(Ranges r) throws Exception {
        p("## D. Mock-ups on the atlas's own pages");
        p("");
        p("Each page is painted by the production renderer over the production composition"
                + " (the bundled catalogue through `Atlas.assembler()`, the Sun from the"
                + " production Solar System module where a page has it, the meridian module"
                + " where a page has a horizon; a closer field through a viewport, as #539)."
                + " Over it this study draws, from the service's J2000 places for the instant"
                + " and observer stated, the candidate rules of section E: **Saturn's figure**"
                + " true - the far half of the two ruled annuli, the globe at its equatorial and"
                + " polar diameters with the minor axis along the pole derived in the chart's"
                + " frame, the near half over it - where the rings' major axis is at least the "
                + (int) SATURN_MARK_PX + " px minimum, else the " + (int) SATURN_MARK_PX
                + " px mark, a cartographic symbol for the whole figure; a ring edge thinner"
                + " than a pixel drawn as a line (edge-on); the rings in a dimmed ink when the"
                + " unlit face is seen; **each moon** as a " + (int) MOON_SYMBOL_PX
                + " px symbol at its own J2000 place, decided on its own (section B), in"
                + " Jupiter's state vocabulary A; **labels** through the production"
                + " `LabelPlacement` with its eight adjacent candidates and the page's own text,"
                + " the figure and the marks as obstacles, each refused alone when no candidate"
                + " is free. Where the true system is a few pixels across, an inset magnifies"
                + " it - a **study magnification, not a field the atlas offers**. Where each"
                + " page's labels went depends on this desktop's fonts and is in `platform.md`.");
        p("");
        List<Configuration> year = year6h();
        Configuration card = service.observeMoons(OSLO.at(CARD));

        // 1. The field series at the card's instant: the ladder, then the closer fields.
        for (double field : new double[] {36.0, 8.0, 3.0, 1.0, 0.5, 0.25}) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            page(String.format(Locale.ROOT, "field-%s", fmt(field)),
                    String.format(Locale.ROOT, "The card's instant, %s at Oslo, %s field%s",
                            when(CARD), fieldName(field),
                            field < 1.0 ? " (a closer field, painted through a viewport)" : ""),
                    card.saturn().astrometricJ2000(), field, field == 8.0 || field == 0.5 ? NORSK : ENGLISH,
                    field == 3.0 || field == 0.25 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    new Scene(card, false, OSLO.at(CARD), false), field == 1.0 ? 12 : field == 3.0 ? 30 : 0,
                    ringsCaption(card.saturn(), per) + " " + (field > 1.0
                            ? "Above the normal minimum field the rings are under the mark and the figure is the symbol; each moon is drawn only where its own mark is distinguishable."
                            : field == 1.0 ? "At the normal minimum field the rings are drawn true around the globe and every moon not behind Saturn is drawn at its exact place."
                            : "On a closer field the globe separates from the rings and the inner moons stand clear of the ring ellipse."));
        }
        // 2. The ring opening: widest north, widest south, edge-on, the unlit face - at 1° and 0.25°.
        Instant north = widestOpening(true);
        Instant south = widestOpening(false);
        Instant unlit = unlitFace();
        for (double field : new double[] {1.0, 0.25}) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            for (Object[] k : new Object[][] {
                    {"open-north", north, "The rings open widest to the north face (2000-2100, weekly geocentric)"},
                    {"open-south", south, "The rings open widest to the south face (2000-2100, weekly geocentric)"},
                    {"edge-on", EARTH_CROSSING, "The Earth's ring-plane crossing, the contract's named case"},
                    {"unlit", unlit, "Between the Earth's and the Sun's 2025 crossings: the unlit face is seen"}}) {
                Instant t = (Instant) k[1];
                Configuration c = service.observeMoons(OSLO.at(t));
                page(k[0] + "-" + fmt(field), k[2] + ", " + when(t) + ", Oslo, " + fieldName(field),
                        c.saturn().astrometricJ2000(), field, k[0].equals("unlit") ? NORSK : ENGLISH,
                        k[0].equals("open-south") ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                        new Scene(c, false, OSLO.at(t), false), field == 1.0 ? 12 : 0,
                        ringsCaption(c.saturn(), per) + " " + states(c));
            }
        }
        // 3. State vocabulary A with Saturn's figure, one real 2026 configuration per state.
        vocabulary();
        // 4. The named moon configurations: Titan's elongation, Iapetus farthest, the crowded inner moons.
        Configuration titan = service.observeMoons(OSLO.at(TITAN_ELONGATION));
        page("titan-elongation-1", "Titan at greatest elongation, " + when(TITAN_ELONGATION) + ", Oslo, 1°",
                titan.saturn().astrometricJ2000(), 1.0, ENGLISH, ChartPalette.WHITE_PAPER,
                new Scene(titan, false, OSLO.at(TITAN_ELONGATION), false), 12,
                String.format(Locale.ROOT, "Titan %.1f″ from Saturn, %.1f px here. %s",
                        titan.moon(Moon.TITAN).separationArcseconds(),
                        titan.moon(Moon.TITAN).separationArcseconds()
                                * JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H), states(titan)));
        Instant far = iapetusFarthest();
        Configuration iapetus = service.observeMoons(OSLO.at(far));
        for (double field : new double[] {3.0, 1.0}) {
            page("iapetus-far-" + fmt(field), "Iapetus at its farthest 2026 day, " + when(far) + ", Oslo, " + fieldName(field),
                    iapetus.saturn().astrometricJ2000(), field, NORSK, ChartPalette.WHITE_PAPER,
                    new Scene(iapetus, false, OSLO.at(far), false), 0,
                    String.format(Locale.ROOT, "Iapetus %.0f″ (%.1f′) from Saturn, %.0f px here: the system's extent on the page. %s",
                            iapetus.moon(Moon.IAPETUS).separationArcseconds(),
                            iapetus.moon(Moon.IAPETUS).separationArcseconds() / 60.0,
                            iapetus.moon(Moon.IAPETUS).separationArcseconds()
                                    * JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H), states(iapetus)));
        }
        Configuration crowd = crowded(year);
        for (double field : new double[] {1.0, 0.25}) {
            page("crowded-" + fmt(field), "The most crowded 2026 configuration with all seven moons clear, " + when(crowd.instant()) + ", Oslo, " + fieldName(field),
                    crowd.saturn().astrometricJ2000(), field, ENGLISH,
                    field == 0.25 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    new Scene(crowd, false, OSLO.at(crowd.instant()), false), field == 1.0 ? 12 : 0,
                    "Each mark and each label is decided on its own: a label with no free candidate is refused alone, and the others stand. " + states(crowd));
        }
        // 5. A moon in front, behind, in shadow and partly in shadow, at 0.25° and 1°.
        for (Object[] k : new Object[][] {
                {"in-front", VisibilityState.IN_FRONT_OF_SATURN},
                {"behind", VisibilityState.BEHIND_SATURN},
                {"shadow", VisibilityState.IN_SATURNS_SHADOW},
                {"shadow-partial", VisibilityState.PARTLY_IN_SATURNS_SHADOW}}) {
            Configuration c = firstWith((VisibilityState) k[1], null);
            for (double field : new double[] {0.25, 1.0}) {
                page(k[0] + "-" + fmt(field), "A moon " + word((VisibilityState) k[1])
                        + " (the first hour at Oslo from July 2026), " + when(c.instant()) + ", " + fieldName(field),
                        c.saturn().astrometricJ2000(), field, field == 1.0 ? NORSK : ENGLISH,
                        ChartPalette.WHITE_PAPER, new Scene(c, false, OSLO.at(c.instant()), false),
                        field == 1.0 ? 12 : 0, states(c));
            }
        }
        // 6. Below a drawn horizon, and with the horizon hidden.
        Instant low = belowTheHorizon();
        Configuration lc = service.observeMoons(OSLO.at(low));
        page("horizon-drawn-8", "Saturn just below the drawn horizon, " + when(low) + ", Oslo, 8°",
                lc.saturn().astrometricJ2000(), 8.0, ENGLISH, ChartPalette.WHITE_PAPER,
                new Scene(lc, true, OSLO.at(low), false), 0,
                String.format(Locale.ROOT, "Saturn at %.1f° altitude, drawn in the ground's dimmed ink with its status, as the Sun's, the Moon's and Jupiter's ruling (C4) has it.",
                        lc.saturn().horizontal().altitudeDegrees()));
        page("horizon-hidden-8", "The same instant with the horizon hidden",
                lc.saturn().astrometricJ2000(), 8.0, NORSK, ChartPalette.WHITE_PAPER,
                new Scene(lc, false, OSLO.at(low), false), 0,
                "With no horizon drawn the celestial chart draws Saturn normally (C4).");
        // 7. A crowded catalogue field: the 2018 opposition in Sagittarius, rings wide open.
        Instant opp = elongationExtreme(2018, true);
        Configuration oc = service.observeMoons(OSLO.at(opp));
        for (double field : new double[] {8.0, 3.0}) {
            page("sagittarius-" + fmt(field), "The 2018 opposition in Sagittarius, " + when(opp) + ", Oslo, " + fieldName(field),
                    oc.saturn().astrometricJ2000(), field, field == 3.0 ? NORSK : ENGLISH,
                    field == 8.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    new Scene(oc, false, OSLO.at(opp), false), field == 3.0 ? 30 : 0,
                    String.format(Locale.ROOT, "A crowded catalogue field with the rings open %.1f°: the figure's label takes a free adjacent place or is refused; it never overwrites a star's. %s",
                            oc.saturn().ringOpeningDegrees(), states(oc)));
        }
        // 8. Conjunction with the Sun, the Sun drawn by the production module.
        Instant conj = elongationExtreme(2026, false);
        Configuration cc = service.observeMoons(OSLO.at(conj));
        page("conjunction-8", "Saturn's 2026 conjunction with the Sun, " + when(conj) + ", Oslo, 8°, the Sun on",
                cc.saturn().astrometricJ2000(), 8.0, ENGLISH, ChartPalette.WHITE_PAPER,
                new Scene(cc, false, OSLO.at(conj), true), 0,
                String.format(Locale.ROOT, "Saturn %.2f° from the Sun's centre on the year's day of least elongation (daily, geocentric): outside the Sun's disc, so both are drawn. The production Sun is painted opaque in the bodies layer after the study's symbol, so a symbol that did fall inside the disc would be covered, as #514 rules for Venus. No Saturn rule is proposed for conjunction beyond painting order and the table's elongation.",
                        cc.saturn().elongationDegrees()));
        // 9. Near a page edge, at 3°.
        SkyPosition offCentre = JovianChartGeometry.offset(card.saturn().astrometricJ2000(), 90.0, -1.40);
        page("page-edge-3", "The system near the page's left edge, 3° field",
                offCentre, 3.0, ENGLISH, ChartPalette.WHITE_PAPER,
                new Scene(card, false, OSLO.at(CARD), false), 0,
                "Bodies off the page are not drawn, as stars are not (C4).");
        // 10. Printable scale: the A4 sheet's chart area at 150 dpi, 8° and 1°.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        for (double field : new double[] {8.0, 1.0}) {
            page("printable-a4-" + fmt(field), String.format(Locale.ROOT, "The A4 sheet's chart area at 150 dpi (%d × %d px), %s field", sheetW, sheetH, fieldName(field)),
                    card.saturn().astrometricJ2000(), field, ENGLISH, ChartPalette.WHITE_PAPER,
                    new Scene(card, false, OSLO.at(CARD), false), 0, sheetW, sheetH,
                    String.format(Locale.ROOT, "The rings' major axis is %.2f mm on the sheet%s.",
                            2 * card.saturn().ringSemiMajorArcseconds(aOuterKm()) * PaperSize.A4.chartWideMm() / (field * 3600.0),
                            field == 8.0 ? ": the minimum mark stands for the figure" : ": the rings and the globe are drawn true, as on the screen at 1°"));
        }
    }

    /**
     * State vocabulary A with Saturn's figure, on white paper above and
     * black sky below: one cell per state from a real 2026 configuration
     * at Oslo, and two geometry-only cells - a moon over the rings, nearer
     * than Saturn, and one behind them - at a study magnification.
     */
    private static void vocabulary() throws Exception {
        Object[][] cells = {
                {"clear of Saturn", firstWith(VisibilityState.CLEAR_OF_SATURN, Moon.TITAN), Moon.TITAN},
                {"in front of Saturn", firstWith(VisibilityState.IN_FRONT_OF_SATURN, null), null},
                {"behind Saturn", firstWith(VisibilityState.BEHIND_SATURN, null), null},
                {"in Saturn's shadow", firstWith(VisibilityState.IN_SATURNS_SHADOW, null), null},
                {"partly in shadow", firstWith(VisibilityState.PARTLY_IN_SATURNS_SHADOW, null), null},
                {"over the rings (nearer)", overRings(true), null},
                {"behind the rings (farther)", overRings(false), null}};
        int cell = 190;
        int w = cell * cells.length;
        int h = 2 * cell + 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        StringBuilder note = new StringBuilder();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, cell + 10);
            g.setColor(new Color(10, 10, 20));
            g.fillRect(0, cell + 10, w, h - cell - 10);
            double scale = 1.5; // pixels per arcsecond: a study magnification
            for (int row = 0; row < 2; row++) {
                boolean dark = row == 1;
                for (int col = 0; col < cells.length; col++) {
                    Configuration c = (Configuration) cells[col][1];
                    Moon which = (Moon) cells[col][2];
                    MoonPlace m = which != null ? c.moon(which) : chosen(c, (String) cells[col][0]);
                    double cx = col * cell + cell / 2.0;
                    double cy = row * cell + cell / 2.0 + 10;
                    // north up, east left, as the atlas's pages: the pole's page direction from its J2000 angle
                    double pa = Math.toRadians(service.poleAngleJ2000Degrees(c.saturn()));
                    double[] polePx = {-Math.sin(pa), -Math.cos(pa)};
                    Figure fig = figureWith(c.saturn(), scale, polePx);
                    StringBuilder ignore = new StringBuilder();
                    paintFigure(g, cx, cy, c.saturn(), fig, dark, false, ignore);
                    double[] off = j2000Offset(c.saturn(), m);
                    double mx = cx - off[0] * scale;
                    double my = cy - off[1] * scale;
                    paintMoon(g, mx, my, m, dark, false);
                    g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
                    if (row == 0) {
                        g.drawString((String) cells[col][0], (int) (col * cell + 6), 14);
                    }
                    g.drawString(name(m.moon()) + ", " + day(c.instant()) + (dark ? " · sky" : " · paper"),
                            (int) (col * cell + 6), (int) (row * cell + cell + 4));
                    if (row == 0) {
                        note.append(String.format(Locale.ROOT, "%s: %s at %s, offset %.1f″ east, %.1f″ north, state %s; ",
                                cells[col][0], name(m.moon()), when(c.instant()), off[0], off[1],
                                word(m.state())));
                    }
                }
            }
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.drawString("MOCKUP - state vocabulary A with Saturn's figure, on white paper and black sky; study magnification 1.5 px/″, north up, east left (#578)",
                    6, h - 6);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "states-vocabulary.png"));
        p("### State vocabulary A with Saturn's figure, on both palettes");
        p("");
        p("![](states-vocabulary.png)");
        p("");
        p("One cell per state, each a real 2026 configuration at Oslo (the first hour from July"
                + " with the state, when the rings are open), Saturn's figure true"
                + " (the far half of the rings, the globe, the near half) and the moon at its"
                + " true J2000 offset, at a study magnification of 1.5 px per arcsecond, on"
                + " white paper above and black sky below: clear a filled dot; in front a filled"
                + " dot ringed in the paper's own colour over the globe; behind not drawn;"
                + " wholly or partly in shadow a hollow ring (the table keeps the distinction)."
                + " The last two cells are geometry, not states: a clear moon whose sight line"
                + " crosses the rings, nearer than Saturn and farther than it, both drawn as"
                + " clear - the contract rules no ring-obscuration state (ruling 5), so the"
                + " symbol is painted over the rings either way and spoken as clear. Drawn: "
                + note.toString().strip());
        p("");
    }

    /** The moon a vocabulary cell is about: the one in the named state, or the chosen geometry. */
    private static MoonPlace chosen(Configuration c, String cellName) {
        for (MoonPlace m : c.moons()) {
            String state = m.state().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            if (cellName.equals("in front of Saturn") && m.state() == VisibilityState.IN_FRONT_OF_SATURN
                    || cellName.equals("behind Saturn") && m.state() == VisibilityState.BEHIND_SATURN
                    || cellName.equals("in Saturn's shadow") && m.state() == VisibilityState.IN_SATURNS_SHADOW
                    || cellName.equals("partly in shadow") && m.state() == VisibilityState.PARTLY_IN_SATURNS_SHADOW) {
                return m;
            }
            if (cellName.startsWith("over the rings") && m.state() == VisibilityState.CLEAR_OF_SATURN
                    && overRingsGeometry(c, m) && m.depthKm() < 0) {
                return m;
            }
            if (cellName.startsWith("behind the rings") && m.state() == VisibilityState.CLEAR_OF_SATURN
                    && overRingsGeometry(c, m) && m.depthKm() > 0) {
                return m;
            }
            assert state != null;
        }
        throw new IllegalStateException("no moon for the cell " + cellName + " at " + c.instant());
    }

    /** Whether a clear moon's sight line crosses the ruled rings: inside the A Ring's ellipse, outside the globe. */
    static boolean overRingsGeometry(Configuration c, MoonPlace m) {
        double pa = Math.toRadians(service.poleAngleJ2000Degrees(c.saturn()));
        double[] polePx = {-Math.sin(pa), -Math.cos(pa)};
        Figure fig = figureWith(c.saturn(), 1.0, polePx);
        double[] off = j2000Offset(c.saturn(), m);
        return fig.overRings(new double[] {-off[0], -off[1]});
    }

    /** The first hour at Oslo, from July 2026, with a clear moon over the rings, nearer or farther than Saturn. */
    private static Configuration overRings(boolean nearer) {
        for (Instant t : searchHours()) {
            Configuration c = service.observeMoons(OSLO.at(t));
            for (MoonPlace m : c.moons()) {
                if (m.state() == VisibilityState.CLEAR_OF_SATURN && overRingsGeometry(c, m)
                        && (nearer ? m.depthKm() < 0 : m.depthKm() > 0)) {
                    return c;
                }
            }
        }
        throw new IllegalStateException("no 2026 hour with a clear moon over the rings");
    }

    // ---- painting ---------------------------------------------------------------

    /** A page at any field, through a viewport, from the catalogue the nearest ladder field queries (#539). */
    static ChartScene scene(SkyPosition centre, double field, int w, int h) {
        double ladder = field;
        if (field < ChartViewState.normalMinimumFieldDegrees()) {
            ladder = ChartViewState.normalMinimumFieldDegrees();
        }
        ChartScene queried = Atlas.assembler().assemble(new ChartViewState(centre, ladder, 8.0, null, null), w, h);
        if (ladder == field) {
            return queried;
        }
        return new ChartScene(new ChartViewport(centre, field, w, h), queried.stars(),
                queried.deepSkyObjects(), queried.title(), queried.limitingMagnitude(),
                queried.targetIdentity(), queried.geography());
    }

    private static void page(String name, String title, SkyPosition centre, double field, PageText words,
                             ChartPalette palette, Scene scene, int inset, String caption) throws Exception {
        page(name, title, centre, field, words, palette, scene, inset, PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, SkyPosition centre, double field, PageText words,
                             ChartPalette palette, Scene scene, int inset, int w, int h, String caption)
            throws Exception {
        ChartScene chart = scene(centre, field, w, h);
        OverlayRegistry registry = new OverlayRegistry();
        if (scene.horizon()) {
            MeridianModule meridian = new MeridianModule(scene.observer());
            registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        }
        if (scene.sun()) {
            SolarSystemModule module = new SolarSystemModule(scene::observer, () -> solar, scene::horizon);
            module.sunShowing(true);
            registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(palette);
        Set<ChartStructure> raised = Set.of();
        boolean dark = palette == ChartPalette.BLACK_SKY;
        DrawnPage drawnPage = DrawnPage.of(chart);
        ViewportMapping mapping = new ViewportMapping(drawnPage);
        Projection projection = drawnPage.projection();
        StringBuilder note = new StringBuilder();
        StringBuilder labelNote = new StringBuilder();
        Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
            // The study's figure goes between the reference layer and the
            // production bodies layer, so the production Sun (where on)
            // paints opaque over a symbol behind it - #514's rule.
            renderer.render(g, chart, options,
                    (layerG, layerScene, reserved) -> ReferenceInk.paint(layerG, layerScene,
                            registry.collect(), palette, words, reserved, raised),
                    (layerG, layerScene, reserved) -> {
                        DrawnPage page = DrawnPage.of(layerScene);
                        ChartRenderer.TextMetrics tm = ChartRenderer.TextMetrics.of(layerG);
                        List<LabelPlacement.Obstacle> obstacles = new ArrayList<>(
                                renderer.textObstacles(tm, layerScene, options));
                        for (LabelPlacement.Placement placed : renderer.textPlacements(tm, layerScene, options)) {
                            if (!placed.omitted()) {
                                obstacles.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.TEXT,
                                        placed.request().id(), placed.at()));
                            }
                        }
                        layerG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        layerG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        drawSystem(layerG, page.projection(), new ViewportMapping(page), scene, field, 1.0, 0, 0,
                                dark, obstacles, new LabelPlacement.Page(new Rectangle2D.Double(0, 0, w, h), "the paper"),
                                tm.labels(), note, labelNote, w, h);
                        ReferenceInk.paintBodies(layerG, page, registry.collect(), palette, words, reserved,
                                ReferenceInk.referenceBoxes(page, registry.collect(), words, reserved,
                                        ChartStructure.membersOf(raised)), ChartStructure.membersOf(raised));
                    }, null, raised);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (inset > 0) {
                var projected = projection.project(scene.c().saturn().astrometricJ2000());
                if (projected.isPresent()) {
                    PixelPoint c = mapping.toPixel(projected.get());
                    int box = 300;
                    int bx = w - box - 12;
                    int by = 12;
                    g.setColor(dark ? new Color(10, 10, 20) : new Color(252, 252, 248));
                    g.fillRect(bx, by, box, box);
                    g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.drawRect(bx, by, box, box);
                    Graphics2D gi = (Graphics2D) g.create();
                    gi.clipRect(bx + 1, by + 1, box - 1, box - 1);
                    gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    ChartRenderer.TextMetrics tm = ChartRenderer.TextMetrics.of(gi);
                    drawSystem(gi, projection, mapping, scene, 0.0, inset,
                            bx + box / 2.0 - c.x() * inset, by + box / 2.0 - c.y() * inset, dark,
                            new ArrayList<>(), new LabelPlacement.Page(
                                    new Rectangle2D.Double(bx + 2, by + 2, box - 4, box - 20), "the inset"),
                            tm.labels(), new StringBuilder(), new StringBuilder(), w, h);
                    gi.dispose();
                    g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.setFont(g.getFont().deriveFont(10f));
                    g.drawString(String.format(Locale.ROOT, "study magnification ×%d - not a field the atlas offers", inset),
                            bx + 6, by + box - 6);
                }
            }
            g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString("MOCKUP - study ink over a production page (#578)", 8, h - 8);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        p("### " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn on the page: " + note.toString().strip()
                + " Where its labels went on this machine is in `platform.md`.");
        p("");
        if (PLATFORM.indexOf("## Labels on the mock-up pages") < 0) {
            q("## Labels on the mock-up pages");
            q("");
            q("Each page's labels as this desktop's fonts placed them; a label not named took"
                    + " its first (right-hand) candidate.");
            q("");
        }
        q("- **" + title + "** (`" + name + ".png`): "
                + (labelNote.length() == 0 ? "every label at its first candidate."
                        : labelNote.toString().strip().replaceAll(";$", ".")));
    }

    /** Pixels per arcsecond at a place on a page, through the page's own projection. */
    static double pxPerArcsecAt(Projection projection, ViewportMapping mapping, SkyPosition at) {
        PixelPoint a = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(
                JovianChartGeometry.offset(at, 0.0, 10.0 / 3600.0)).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) / 10.0;
    }

    /** A body this study drew, with where its label goes. */
    record Drawn(String id, String label, double x, double y, double reach, boolean belowHorizon) {
    }

    /** The near half-plane of the rings on the page: away from the projected pole when B > 0, toward it when B < 0. */
    static Shape nearHalf(double size, double[] polePx, double b) {
        double[] d = b > 0 ? new double[] {-polePx[0], -polePx[1]} : polePx;
        Shape half = new Rectangle2D.Double(-size, 0, 2 * size, size);   // page +y side of the origin
        double phi = Math.atan2(-d[0], d[1]);
        return AffineTransform.getRotateInstance(phi).createTransformedShape(half);
    }

    private static Shape ellipse(double a, double b, double[] majorPx) {
        AffineTransform t = AffineTransform.getRotateInstance(Math.atan2(majorPx[1], majorPx[0]));
        return t.createTransformedShape(new Ellipse2D.Double(-a, -b, 2 * a, 2 * b));
    }

    /**
     * Saturn's figure about (x, y): the far half of the rings, the globe,
     * the near half over it; or the minimum mark. Returns the drawn
     * extent's reach for labels.
     */
    static double paintFigure(Graphics2D g, double x, double y, SaturnObservation s, Figure fig,
                              boolean dark, boolean below, StringBuilder note) {
        Color ink = below ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                : (dark ? Color.WHITE : Color.BLACK);
        Color globe = below ? (dark ? new Color(70, 68, 60) : new Color(225, 222, 215))
                : (dark ? new Color(205, 190, 150) : new Color(225, 212, 175));
        Color ringNear = below ? (dark ? new Color(80, 78, 70) : new Color(205, 205, 200))
                : s.unlitFaceSeen() ? (dark ? new Color(120, 118, 110) : new Color(190, 188, 180))
                : (dark ? new Color(190, 172, 125) : new Color(170, 150, 100));
        Color ringFar = below ? ringNear : s.unlitFaceSeen() ? (dark ? new Color(90, 88, 82) : new Color(215, 213, 208))
                : (dark ? new Color(150, 136, 100) : new Color(205, 190, 145));
        AffineTransform keep = g.getTransform();
        g.translate(x, y);
        g.setStroke(new BasicStroke(1f));
        if (!fig.rings) {
            Ellipse2D disc = new Ellipse2D.Double(-fig.markPx / 2, -fig.markPx / 2, fig.markPx, fig.markPx);
            g.setColor(globe);
            g.fill(disc);
            g.setColor(ink);
            g.draw(disc);
            g.setTransform(keep);
            note.append(String.format(Locale.ROOT, "Saturn at (%.0f, %.0f) as the %.0f px mark (a cartographic symbol for the whole figure; the rings' true major axis %.1f px); ",
                    x, y, fig.markPx, 2 * fig.a));
            return fig.markPx / 2;
        }
        SaturnPack.Constants k = service.pack().constants();
        double per = fig.a / s.ringSemiMajorArcseconds(k.ringOuterRadiusKm());
        double sinB = Math.abs(Math.sin(Math.toRadians(s.ringOpeningDegrees())));
        boolean edgeOn = 2 * fig.a * sinB < EDGE_ON_PX;
        Area rings = new Area();
        double[][] bands = {{k.ringInnerRadiusKm(), k.ringBOuterRadiusKm()},
                {k.ringCassiniOuterRadiusKm(), k.ringOuterRadiusKm()}};
        for (double[] band : bands) {
            double ao = s.ringSemiMajorArcseconds(band[1]) * per;
            double ai = s.ringSemiMajorArcseconds(band[0]) * per;
            if (edgeOn) {
                continue;
            }
            Area a = new Area(ellipse(ao, ao * sinB, fig.majorPx));
            a.subtract(new Area(ellipse(ai, ai * sinB, fig.majorPx)));
            rings.add(a);
        }
        Shape globeShape = ellipse(fig.globeEq / 2, fig.globePol / 2, fig.majorPx);
        if (edgeOn) {
            // The globe, then a line over it along the major axis, the rings'
            // full extent: seen edge-on, the near half crosses the globe.
            g.setColor(globe);
            g.fill(globeShape);
            g.setColor(ink);
            g.draw(globeShape);
            g.setColor(ringNear);
            g.setStroke(new BasicStroke((float) LINE_PX));
            g.draw(new Line2D.Double(-fig.a * fig.majorPx[0], -fig.a * fig.majorPx[1],
                    fig.a * fig.majorPx[0], fig.a * fig.majorPx[1]));
            g.setStroke(new BasicStroke(1f));
        } else {
            Area near = new Area(rings);
            near.intersect(new Area(nearHalf(10000, fig.polePx, s.ringOpeningDegrees())));
            Area farArea = new Area(rings);
            farArea.subtract(near);
            g.setColor(ringFar);
            g.fill(farArea);
            g.setColor(globe);
            g.fill(globeShape);
            g.setColor(ink);
            g.draw(globeShape);
            g.setColor(ringNear);
            g.fill(near);
            if (fig.a >= 10 && 2 * fig.a * sinB >= OUTLINE_MINOR_PX) {
                g.setColor(ink);
                g.draw(ellipse(fig.a, fig.a * sinB, fig.majorPx));
            }
        }
        g.setTransform(keep);
        note.append(String.format(Locale.ROOT, "Saturn at (%.0f, %.0f) true: rings %.1f × %.1f px%s, globe %.1f × %.1f px, B %+.1f°%s; ",
                x, y, 2 * fig.a, 2 * fig.a * sinB, edgeOn ? " (edge-on: a line)" : "", fig.globeEq, fig.globePol,
                s.ringOpeningDegrees(), s.unlitFaceSeen() ? " (the unlit face, dimmed ring ink)" : ""));
        return fig.a;
    }

    /** One moon's mark in vocabulary A. */
    static void paintMoon(Graphics2D g, double mx, double my, MoonPlace m, boolean dark, boolean below) {
        Color ink = below ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                : (dark ? Color.WHITE : Color.BLACK);
        Color paper = dark ? new Color(10, 10, 20) : Color.WHITE;
        double r = MOON_SYMBOL_PX / 2.0;
        Ellipse2D dot = new Ellipse2D.Double(mx - r, my - r, 2 * r, 2 * r);
        boolean dim = m.shadowRelation() != ShadowRelation.SUNLIT;
        if (m.discRelation() == DiscRelation.BEHIND) {
            return;
        }
        if (m.discRelation() == DiscRelation.IN_FRONT) {
            g.setColor(paper);
            g.fill(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
            g.setColor(ink);
            g.fill(dot);
        } else if (dim) {
            g.setColor(ink);
            g.setStroke(new BasicStroke(1f));
            g.draw(dot);
        } else {
            g.setColor(ink);
            g.fill(dot);
        }
    }

    private static void drawSystem(Graphics2D g, Projection projection, ViewportMapping mapping,
                                   Scene scene, double fieldDegrees, double zoom, double ox, double oy,
                                   boolean dark, List<LabelPlacement.Obstacle> obstacles,
                                   LabelPlacement.Page paper, java.awt.FontMetrics font,
                                   StringBuilder note, StringBuilder labelNote, int w, int h) {
        Configuration c = scene.c();
        SaturnObservation s = c.saturn();
        var projected = projection.project(s.astrometricJ2000());
        if (projected.isEmpty()) {
            note.append("Saturn off the page, nothing drawn; ");
            return;
        }
        PixelPoint sc = mapping.toPixel(projected.get());
        double x = sc.x() * zoom + ox;
        double y = sc.y() * zoom + oy;
        Figure fig = figureOn(s, projection, mapping, zoom);
        if (zoom == 1.0 && !(x > -fig.a && x < w + fig.a && y > -fig.a && y < h + fig.a)) {
            note.append("Saturn off the page, nothing drawn; ");
            return;
        }
        boolean below = scene.horizon() && s.horizontal() != null && s.horizontal().altitudeDegrees() < 0;
        double reach = paintFigure(g, x, y, s, fig, dark, below, note);
        List<Drawn> drawn = new ArrayList<>();
        drawn.add(new Drawn(SATURN, "Saturn" + (below ? " (below the horizon)" : ""), x, y, reach, below));
        List<double[]> at = new ArrayList<>();
        List<double[]> offsets = new ArrayList<>();
        for (MoonPlace m : c.moons()) {
            PixelPoint mc = mapping.toPixel(projection.project(m.astrometricJ2000()).orElseThrow());
            double mx = mc.x() * zoom + ox;
            double my = mc.y() * zoom + oy;
            at.add(new double[] {mx, my});
            offsets.add(new double[] {mx - x, my - y});
        }
        List<Decision> decisions = zoom == 1.0
                ? decide(fieldDegrees, ChartViewState.normalMinimumFieldDegrees(), fig, c.moons(), offsets)
                : decide(0.0, ChartViewState.normalMinimumFieldDegrees(), fig, c.moons(), offsets);
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i = 0; i < c.moons().size(); i++) {
            MoonPlace m = c.moons().get(i);
            Decision d = decisions.get(i);
            if (d != Decision.DRAWN) {
                note.append(name(m.moon())).append(' ').append(d.name().toLowerCase(Locale.ROOT)
                        .replace('_', ' ')).append(", not drawn; ");
                continue;
            }
            double mx = at.get(i)[0];
            double my = at.get(i)[1];
            paintMoon(g, mx, my, m, dark, below);
            boolean dim = m.shadowRelation() != ShadowRelation.SUNLIT;
            note.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) %s%s; ", name(m.moon()), mx, my,
                    m.discRelation() == DiscRelation.IN_FRONT ? "in front" : dim ? "shadowed" : "clear",
                    fig.rings && fig.overRings(offsets.get(i)) ? (m.depthKm() < 0 ? ", over the rings" : ", behind the rings") : ""));
            drawn.add(new Drawn(m.moon().name().toLowerCase(Locale.ROOT), name(m.moon()), mx, my, r, below));
        }
        List<LabelPlacement.Obstacle> occupied = new ArrayList<>(obstacles);
        for (Drawn d : drawn) {
            occupied.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK, d.id(),
                    new Ellipse2D.Double(d.x() - d.reach(), d.y() - d.reach(), 2 * d.reach(), 2 * d.reach())));
        }
        List<LabelPlacement.Request> requests = new ArrayList<>();
        for (int i = 0; i < drawn.size(); i++) {
            Drawn d = drawn.get(i);
            double width = font.stringWidth(d.label());
            requests.add(new LabelPlacement.Request(LabelPlacement.Family.STAR, d.id(), d.label(), d.x(), d.y(),
                    JovianCartographyStudyMain.around(d.x(), d.y(), d.reach(), width, font.getHeight(),
                            font.getAscent()), d.id(), null, false, i));
        }
        g.setFont(font.getFont());
        for (LabelPlacement.Placement placed
                : JovianCartographyStudyMain.placeRefusing(w, h, occupied, paper, requests)) {
            if (placed.omitted()) {
                if (zoom == 1.0) {
                    labelNote.append(placed.request().text()).append("'s label refused; ");
                }
                continue;
            }
            boolean under = false;
            for (Drawn d : drawn) {
                under |= d.id().equals(placed.request().id()) && d.belowHorizon();
            }
            g.setColor(under ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                    : (dark ? Color.WHITE : Color.BLACK));
            Rectangle2D box = placed.at();
            g.drawString(placed.request().text(), (float) box.getX(), (float) (box.getY() + font.getAscent()));
            if (zoom == 1.0 && placed.candidate() != 0) {
                labelNote.append(String.format(Locale.ROOT, "%s's label at candidate %d; ",
                        placed.request().text(), placed.candidate()));
            }
        }
    }

    // ---- E. the rulings -------------------------------------------------------------

    private static void rulings(Ranges r) {
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p("## E. The proposal, for the rulings (#578's eight)");
        p("");
        p(String.format(Locale.ROOT, "1. **True scale against a minimum mark.** Saturn's mark is"
                + " the figure - globe and rings - at true scale, or Jupiter's %.0f px minimum"
                + " as a circle standing for the whole figure, whichever is larger, measured by"
                + " the rings' major axis (section A): on the ordinary ladder the true figure is"
                + " drawn only at 1°, where the rings are %.1f-%.1f px across, and on the closer"
                + " fields; from 2° up the symbol stands in. Evidence and accessible text call"
                + " the symbol *a cartographic symbol, not Saturn's apparent diameter*. The"
                + " transition is a switch, not Jupiter's blend: a circle cannot grow into a"
                + " ringed figure.", SATURN_MARK_PX, r.ringMajorMin * at1, r.ringMajorMax * at1));
        p(String.format(Locale.ROOT, "2. **The rings.** The two ruled annuli (C Ring inner to B"
                + " Ring outer; Cassini Division outer to A Ring outer), flat, their semi-axes"
                + " asin(R/Δ) and asin(R/Δ)·|sin B| at the page's scale, the major axis along"
                + " the pole's page direction turned 90°; filled, with the near half painted"
                + " over the globe and the far half behind it by the contract's −sin B cos B"
                + " rule (section C); a hairline outline on the A Ring's outer edge only where"
                + " the figure is at least 20 px wide and the rings' minor axis at least %.0f px."
                + " Where the minor axis is under %.0f px the rings are a 1 px line through the"
                + " globe (edge-on: %.1f %% of the century's weeks at 1°). When the unlit face"
                + " is seen the rings take a dimmed"
                + " ink, so the page says what the table says; the Cassini gap is drawn only"
                + " where it is a pixel (the closest page). No shadows, as ruling 4 of #574"
                + " keeps them out of 5.4.0.", OUTLINE_MINOR_PX, EDGE_ON_PX,
                100.0 * r.edgeOnWeeks / r.saturnSamples));
        p(String.format(Locale.ROOT, "3. **The moons.** Seven %.0f px symbols, Jupiter's rule 4"
                + " with the drawn figure as the primary's extent: behind Saturn omitted; at the"
                + " normal minimum field and the closer fields every other moon drawn at its"
                + " exact J2000 place; above it only where its mark is %.0f px clear of the"
                + " figure's drawn edge and of every higher-priority moon (in front, clear,"
                + " shadowed; then Titan, Rhea, Iapetus, Dione, Tethys, Enceladus, Mimas); a"
                + " moon in front only over a globe at least %.0f px wide - on the closer fields"
                + " only (section B). A collision suppresses only the lower-priority mark."
                + " Labels through the adjacent candidates, refused alone, never under duress -"
                + " and often refused at 1°, where seven names crowd a figure a dozen pixels"
                + " wide (section B's finding): the owner may prefer a rule that names only the"
                + " moons whose marks stand clear of the figure.",
                MOON_SYMBOL_PX, SEPARABLE_PX, 2 * SEPARABLE_PX));
        p("4. **Vocabulary on the chart.** Jupiter's vocabulary A for the six states (section D's"
                + " strip). No ring-obscuration state, as #574's ruling 5: a clear moon whose"
                + " sight line crosses the rings is painted over them whether nearer or farther,"
                + " and spoken as clear. No satellite shadow spots, no ring shadow, no globe"
                + " shadow. Spoken: *Saturn (a cartographic symbol, not Saturn's apparent"
                + " diameter)* while the symbol stands in, *Saturn* when the figure is true; each"
                + " moon with the table's state words (`saturntable.state.*`) and the symbol note.");
        p("5. **Selection and Centre on chart.** Saturn's identity on the page is `saturn`; a"
                + " selected Saturn is an occurrence (#513), as Jupiter's. The moons are"
                + " `saturn.mimas` … `saturn.iapetus`, symbols that consume no click, as the"
                + " Galilean moons; proposed not selectable, as Jupiter's are not. Centre on chart"
                + " lands at 1°, as for every body. *Saturn on the chart* joins View > Solar"
                + " System after Jupiter on the chart, and the Controller's Saturn group gains"
                + " Show on chart and Centre on chart.");
        p(String.format(Locale.ROOT, "6. **The closer fields.** Saturn becomes eligible for 0.5° and"
                + " 0.25°, joining Mercury, Venus and Jupiter: at 0.25° the rings are %.1f-%.1f px"
                + " and the globe separates from them (the gap %.2f-%.2f px), the inner moons"
                + " stand clear of the ring ellipse, and a moon in front is resolved (section B)."
                + " The pages are painted in section D.", r.ringMajorMin * at1 * 4,
                r.ringMajorMax * at1 * 4, r.gapMin * at1 * 4, r.gapMax * at1 * 4));
        p("7. **Screen, paper, accessibility and export.** Below a drawn horizon the figure and"
                + " its moons take the ground's dimmed ink with the status; off the page nothing"
                + " is drawn; the symbol never erases a star and a symbol in front of an opaque"
                + " disc paints over it (#514); export draws through the same module in page"
                + " units, so the A4 sheet agrees with the screen (section D's printable pages);"
                + " the Solar System labels class (#526) raises Saturn's and the moons' names.");
        p("8. **The production split.** #579 in the ruled order: the figure (globe and rings,"
                + " the switch, Centre on chart, the words) with its packaged checkpoint, then"
                + " the moons; both wide-route PRs with the on-the-chart evidence; the closer-field"
                + " eligibility with the figure. Then #580 and #581.");
        p("");
    }

    static String fmt(double degrees) {
        return degrees == Math.rint(degrees) ? String.valueOf((long) degrees) : String.valueOf(degrees);
    }

    static double wrap(double degrees) {
        double d = degrees % 360.0;
        if (d > 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }
}
