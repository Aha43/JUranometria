package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
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
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.Configuration;
import juranometria.solar.MarsService.DiscRelation;
import juranometria.solar.MarsService.MarsObservation;
import juranometria.solar.MarsService.Moon;
import juranometria.solar.MarsService.MoonPlace;
import juranometria.solar.MarsService.ShadowRelation;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Mars, Phobos and Deimos on the atlas's pages, measured, and the
 * candidate rules drawn (issue #605): what drawing the system honestly
 * needs, through the production projections and renderer, with this
 * study's own candidate marks drawn over pages the production composition
 * painted, as the Venus (#512) and Saturn (#578) studies did. No
 * production Mars layer exists and none is built here; every image is a
 * mock-up and says so in its corner.
 *
 * <p>The numbers come from the accepted Mars service (#602) under the
 * contract ruled on #601 and handed over by #604
 * ({@code docs/decisions/mars-handover.md}): each body's astrometric J2000
 * place, the equatorial and polar diameters, the illuminated fraction and
 * the lit side, and the moons' states on Mars's flattened figure. The
 * phased disc is the page's own lit region ({@code ReferenceInk.litRegion}),
 * as Venus's and Mercury's. The closer fields are painted through
 * viewports, as #539 painted them; the ladder still refuses them.
 *
 * <p>Deterministic: the bundled packs, the contract's named instants and
 * Espenak's rows it cites, Oslo at sea level, {@code Locale.ROOT} numbers.
 * The portable report goes to stdout ({@code make mars-cartography-study});
 * the font-dependent label placements to {@code platform.md}.
 */
public final class MarsCartographyStudyMain {

    private static final File DIR = new File("docs/studies/mars-cartography");
    static final int PAGE_W = 900;
    static final int PAGE_H = 700;
    /** The contract's observer: Oslo at sea level. */
    static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-25T12:00:00Z"));
    /** The candidate closer fields, as #539 painted them. */
    static final double[] CLOSER_FIELDS = {0.5, 0.25};

    // The named instants, each cited where the contract or the almanacs give it.
    /** Espenak's 2003 opposition hour (mars-events.txt), the closest of the century's named ones. */
    static final Instant OPPOSITION_2003 = Instant.parse("2003-08-28T16:00:00Z");
    /** Espenak's 2027 opposition hour, aphelic (mars-events.txt). */
    static final Instant OPPOSITION_2027 = Instant.parse("2027-02-19T15:00:00Z");
    /** Espenak's 2026 conjunction hour (mars-events.txt). */
    static final Instant CONJUNCTION_2026 = Instant.parse("2026-01-09T10:00:00Z");
    /** Mars's largest phase angle in the weekly Oslo matrix (measurements.md, 47.38°). */
    static final Instant GREATEST_PHASE = Instant.parse("2005-07-11T00:00:00Z");
    /** Horizons' Oslo rows for Phobos (matrix-7d-phobos-oslo.txt): U, u, t. */
    static final Instant PHOBOS_BEHIND_IN_SHADOW = Instant.parse("2005-02-27T00:00:00Z");
    static final Instant PHOBOS_IN_SHADOW = Instant.parse("2005-03-06T00:00:00Z");
    static final Instant PHOBOS_IN_FRONT = Instant.parse("2005-08-07T00:00:00Z");
    /** Deimos's greatest separation at the 2003 opposition, geocentric (measurements.md, 86.774″). */
    static final Instant DEIMOS_WIDEST_2003 = Instant.parse("2003-08-28T08:00:00Z");
    /** Espenak's 2100 row: Mars 2.5° S of the Pleiades (mars-raw almanac 2100), outside the moons' years. */
    static final Instant PLEIADES_2100 = Instant.parse("2100-03-01T16:09:00Z");
    /** The table's 2026-10-25 21:00 Oslo row: Mars below the horizon (table-measurements.md). */
    static final Instant BELOW_HORIZON = Instant.parse("2026-10-25T21:00:00Z");

    /** The atlas's smallest visible star mark, as a diameter. */
    private static final double MIN_INK_PX = 2.0 * StarSizePolicy.DEFAULT.minimumRadiusPx();
    /** Proposed: Mars's minimum cartographic mark, Jupiter's ruled 6 px (rule M). */
    static final double MARK_PX = JovianModule.MINIMUM_MARK_PX;
    /** Proposed: a moon's symbol, Jupiter's ruled 3 px - never an apparent size. */
    static final double MOON_SYMBOL_PX = JovianCartographyStudyMain.MOON_SYMBOL_PX;
    /** Proposed: two symbols are separable at this distance, centre to centre (Jupiter's). */
    static final double SEPARABLE_PX = JovianCartographyStudyMain.SEPARABLE_PX;
    /** The proposed page identity (no production key exists). */
    static final String MARS = "mars";

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    static MarsService service;
    static SolarSystemService solar;

    /** What the study decides for one moon on one page. */
    enum Decision { DRAWN, BEHIND, AT_MARS, IN_FRONT_UNRESOLVED, COLLIDES }

    private MarsCartographyStudyMain() {
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
        service = MarsService.load();
        solar = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM, "Mars's and its moons' labels, as this desktop's fonts place them",
                "Issue #605.");
        p("# Mars, Phobos and Deimos on the page: measurements and mock-ups");
        p("");
        p("Issue #605. What a cartography rule would mean on the atlas's own pages, measured"
                + " through the production projections and the accepted Mars service (#602) under"
                + " the contract ruled on #601 and handed over by #604, with candidate marks drawn by"
                + " this study over pages the production composition painted, as the Venus (#512) and"
                + " Saturn (#578) studies did. Nothing here is production rendering; every image is a"
                + " mock-up and says so in its corner. The closer fields are painted through"
                + " viewports, as #539 painted them; the ladder still refuses them. Regenerate with"
                + " `make mars-cartography-study`.");
        p("");
        try {
            Ranges r = ranges();
            scale(r);
            thresholds(r);
            frame(r);
            mockups();
            proposals(r);
            System.out.print(REPORT);
            System.out.flush();
            PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
        } finally {
            // The evidence contract runs every generator in one JVM under a measured
            // heap (#323): nothing this study loaded may outlive it.
            service = null;
            solar = null;
            REPORT.setLength(0);
            REPORT.trimToSize();
            PLATFORM.setLength(0);
            PLATFORM.trimToSize();
        }
    }

    // ---- the century's ranges --------------------------------------------------

    /** The extremes a page must hold, from weekly geocentric instants. */
    record Ranges(double eqMin, double eqMax, double polMax, double flatMax, double kMin,
                  String kMinAt, double defectMax, int marsSamples, double[] sepMin, double[] sepMax,
                  String[] sepMaxAt, double[] diamMax, double offsetDifference, List<Configuration> weekly) {
    }

    static Ranges ranges() {
        double eqMin = 1e9, eqMax = 0, polMax = 0, flatMax = 0, kMin = 1, defectMax = 0;
        String kMinAt = "";
        int n = 0;
        for (Instant t = Instant.parse("1900-01-01T00:00:00Z"); t.isBefore(Instant.parse("2101-01-01T00:00:00Z"));
                t = t.plus(Duration.ofDays(7))) {
            MarsObservation m = service.observeGeocentric(t);
            n++;
            eqMin = Math.min(eqMin, m.equatorialDiameterArcseconds());
            eqMax = Math.max(eqMax, m.equatorialDiameterArcseconds());
            polMax = Math.max(polMax, m.polarDiameterArcseconds());
            flatMax = Math.max(flatMax, m.equatorialDiameterArcseconds() - m.polarDiameterArcseconds());
            if (m.illuminatedFraction() < kMin) {
                kMin = m.illuminatedFraction();
                kMinAt = day(t);
            }
            defectMax = Math.max(defectMax, m.equatorialDiameterArcseconds() * (1.0 - m.illuminatedFraction()));
        }
        double[] sepMin = {1e9, 1e9};
        double[] sepMax = {0, 0};
        String[] sepMaxAt = {"", ""};
        double[] diamMax = {0, 0};
        double offsetDiff = 0;
        List<Configuration> weekly = new ArrayList<>();
        for (Instant t = Instant.parse("2000-01-01T00:00:00Z"); service.moonsAnswerAt(t);
                t = t.plus(Duration.ofDays(7))) {
            Configuration c = service.observeMoonsGeocentric(t);
            weekly.add(c);
            for (MoonPlace m : c.moons()) {
                int i = m.moon().ordinal();
                sepMin[i] = Math.min(sepMin[i], m.separationArcseconds());
                if (m.separationArcseconds() > sepMax[i]) {
                    sepMax[i] = m.separationArcseconds();
                    sepMaxAt[i] = day(t);
                }
                diamMax[i] = Math.max(diamMax[i], m.angularDiameterArcseconds());
                double[] j = j2000Offset(c.mars(), m);
                offsetDiff = Math.max(offsetDiff, Math.hypot(j[0] - m.xArcseconds(), j[1] - m.yArcseconds()));
            }
        }
        return new Ranges(eqMin, eqMax, polMax, flatMax, kMin, kMinAt, defectMax, n, sepMin, sepMax,
                sepMaxAt, diamMax, offsetDiff, weekly);
    }

    /** A moon's offset from Mars in J2000, arcseconds, east and north, from the two J2000 places. */
    static double[] j2000Offset(MarsObservation mars, MoonPlace m) {
        double dra = wrap(m.astrometricJ2000().raDegrees() - mars.astrometricJ2000().raDegrees())
                * Math.cos(Math.toRadians(mars.astrometricJ2000().decDegrees())) * 3600.0;
        double ddec = (m.astrometricJ2000().decDegrees() - mars.astrometricJ2000().decDegrees()) * 3600.0;
        return new double[] {dra, ddec};
    }

    private static List<Double> fields() {
        List<Double> fields = new ArrayList<>(ChartViewState.fieldWidthSteps());
        for (double f : CLOSER_FIELDS) {
            fields.add(f);
        }
        return fields;
    }

    // ---- A. true scale ----------------------------------------------------------

    private static void scale(Ranges r) {
        p("## A. True scale on every page");
        p("");
        p(String.format(Locale.ROOT, "Over 1900-2100 (%d weekly geocentric instants from the service):"
                + " Mars's equatorial diameter is %.2f-%.2f″, its polar diameter at the tilt at most"
                + " %.2f″ (the two differ by at most %.3f″); it is never less than %.2f %% lit (on %s),"
                + " and the dark part of its disc - the diameter times (1 − k) - is at most %.2f″."
                + " Over the moons' years (%d weekly instants from 2000 to the exclusive end; the opposition"
                + " windows' finer samples reach further, 35.02″ and 86.77″ in 2003) Phobos"
                + " stands %.2f-%.2f″ from Mars's centre (the most on %s) and Deimos %.2f-%.2f″ (on %s);"
                + " the largest moon disc is Phobos's %.3f″. The atlas's smallest star mark is %.1f px"
                + " across.", r.marsSamples, r.eqMin, r.eqMax, r.polMax, r.flatMax, 100 * r.kMin,
                r.kMinAt, r.defectMax, r.weekly.size(), r.sepMin[0], r.sepMax[0], r.sepMaxAt[0],
                r.sepMin[1], r.sepMax[1], r.sepMaxAt[1], r.diamMax[0], MIN_INK_PX));
        p("");
        p(String.format(Locale.ROOT, "Pixels on the 900 × 700 page at its centre (each field's own"
                + " projection) - the ladder's fields, then the two closer fields below it; millimetres"
                + " on the A4 sheet's chart area (%.0f mm wide).", PaperSize.A4.chartWideMm()));
        p("");
        p("| field | Mars px (min-max) | flattening px (max) | dark part px (max) | Phobos from Mars px (max) | Deimos from Mars px (max) | Mars on A4 mm (max) |");
        p("|---:|---|---:|---:|---:|---:|---:|");
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double mm = PaperSize.A4.chartWideMm() / (field * 3600.0);
            p(String.format(Locale.ROOT, "| %s%s | %.2f-%.2f | %.3f | %.2f | %.2f | %.2f | %.3f |",
                    fmt(field) + "°", field < ChartViewState.normalMinimumFieldDegrees() ? " (closer)" : "",
                    r.eqMin * per, r.eqMax * per, r.flatMax * per, r.defectMax * per, r.sepMax[0] * per,
                    r.sepMax[1] * per, r.eqMax * mm));
        }
        p("");
    }

    // ---- B. thresholds ----------------------------------------------------------------

    private static void thresholds(Ranges r) {
        p("## B. Where things become resolvable or separable");
        p("");
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p(String.format(Locale.ROOT, "**Mars against the %s px minimum mark (rule M).** The page scale"
                + " at its centre is %.4f px per arcsecond at 1° and scales with 1/field. Mars's largest"
                + " disc reaches %s px at %.2f°, its smallest at %.2f°: on the ordinary ladder the true"
                + " disc exceeds the mark only at 1°, and only near the closest oppositions.", fmt(MARK_PX),
                at1, fmt(MARK_PX), r.eqMax * at1 / MARK_PX, r.eqMin * at1 / MARK_PX));
        p("");
        p(String.format(Locale.ROOT, "**The phase.** The dark part reaches one pixel only at %.2f° and"
                + " narrower, and the flattening never reaches a pixel on any field offered (%.3f px at"
                + " 0.25°). Mars's phase is real but slight: on a 6 px mark at k = %.4f the dark part is"
                + " %.2f px.", r.defectMax * at1, r.flatMax * at1 / 0.25, r.kMin, MARK_PX * (1 - r.kMin)));
        p("");
        p(String.format(Locale.ROOT, "**Each moon decided on its own (Jupiter's rule 4).** Over the %d"
                + " weekly geocentric configurations of the moons' years, on each field: behind Mars,"
                + " omitted; at the chart's normal minimum field (%s°) every moon not behind is drawn"
                + " (Jupiter's floor); otherwise a moon in front is drawn only on a drawn disc of at least"
                + " %s px, a moon whose %s px symbol comes within %s px of Mars's drawn edge is not drawn"
                + " (at Mars), and a second moon within %s px of one already drawn is not drawn (collides);"
                + " priority in front, clear, shadowed, then Phobos before Deimos. Mars's drawn disc is the"
                + " larger of its true disc and the %s px mark.", r.weekly.size(),
                fmt(ChartViewState.normalMinimumFieldDegrees()), fmt(2 * SEPARABLE_PX), fmt(MOON_SYMBOL_PX),
                fmt(SEPARABLE_PX / 2.0), fmt(SEPARABLE_PX), fmt(MARK_PX)));
        p("");
        p("The first five columns are the geometry alone, without Jupiter's floor: whether a moon's"
                + " symbol would stand clear of Mars's drawn mark and of the other moon. The last two are"
                + " what Jupiter's floor rule draws, on the fields it covers (the normal minimum and"
                + " narrower), where every moon not behind Mars is drawn whatever it overlaps.");
        p("");
        p("| field | Phobos clear of the mark | Deimos clear of the mark | both clear and apart | Phobos in front, resolvable | collides | Phobos drawn by the floor | Deimos drawn by the floor |");
        p("|---:|---:|---:|---:|---:|---:|---:|---:|");
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            boolean floored = field <= ChartViewState.normalMinimumFieldDegrees() + 1e-9;
            int phobos = 0, deimos = 0, both = 0, inFront = 0, collides = 0, phobosFloor = 0, deimosFloor = 0;
            for (Configuration c : r.weekly) {
                List<Decision> d = decide(false, c, per);
                boolean p0 = d.get(0) == Decision.DRAWN;
                boolean p1 = d.get(1) == Decision.DRAWN;
                phobos += p0 && c.moons().get(0).discRelation() != DiscRelation.IN_FRONT ? 1 : 0;
                inFront += p0 && c.moons().get(0).discRelation() == DiscRelation.IN_FRONT ? 1 : 0;
                deimos += p1 ? 1 : 0;
                both += p0 && p1 ? 1 : 0;
                collides += (d.get(0) == Decision.COLLIDES ? 1 : 0) + (d.get(1) == Decision.COLLIDES ? 1 : 0);
                if (floored) {
                    List<Decision> f = decide(true, c, per);
                    phobosFloor += f.get(0) == Decision.DRAWN ? 1 : 0;
                    deimosFloor += f.get(1) == Decision.DRAWN ? 1 : 0;
                }
            }
            int total = r.weekly.size();
            p(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s | %s |", fmt(field) + "°",
                    pct(phobos, total), pct(deimos, total), pct(both, total), pct(inFront, total),
                    pct(collides, total), floored ? pct(phobosFloor, total) : "-",
                    floored ? pct(deimosFloor, total) : "-"));
        }
        p("");
        p("Percentages are of all weekly configurations; a moon behind Mars is counted in none of the"
                + " columns shown.");
        p("");
    }

    static String pct(int n, int total) {
        return String.format(Locale.ROOT, "%.1f %%", 100.0 * n / total);
    }

    /** The study's decision for each moon at a field, from the J2000 offsets, Jupiter's rule. */
    static List<Decision> decide(double field, Configuration c, double perArcsec) {
        return decide(field <= ChartViewState.normalMinimumFieldDegrees() + 1e-9, c, perArcsec);
    }

    /** The same, with Jupiter's floor applied or not: without it, the geometry alone decides. */
    static List<Decision> decide(boolean floor, Configuration c, double perArcsec) {
        double markPx = Math.max(c.mars().equatorialDiameterArcseconds() * perArcsec, MARK_PX);
        Decision[] out = new Decision[2];
        List<Integer> order = new ArrayList<>(List.of(0, 1));
        order.sort((a, b) -> Integer.compare(priority(c.moons().get(a)), priority(c.moons().get(b))));
        List<double[]> drawn = new ArrayList<>();
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i : order) {
            MoonPlace m = c.moons().get(i);
            double[] o = j2000Offset(c.mars(), m);
            double[] at = {o[0] * perArcsec, o[1] * perArcsec};
            if (m.discRelation() == DiscRelation.BEHIND) {
                out[i] = Decision.BEHIND;
                continue;
            }
            if (floor) {
                out[i] = Decision.DRAWN;
            } else if (m.discRelation() == DiscRelation.IN_FRONT) {
                out[i] = markPx >= 2.0 * SEPARABLE_PX ? Decision.DRAWN : Decision.IN_FRONT_UNRESOLVED;
            } else if (Math.hypot(at[0], at[1]) - markPx / 2.0 < SEPARABLE_PX / 2.0 + r) {
                out[i] = Decision.AT_MARS;
            } else {
                out[i] = Decision.DRAWN;
                for (double[] other : drawn) {
                    if (Math.hypot(at[0] - other[0], at[1] - other[1]) < SEPARABLE_PX) {
                        out[i] = Decision.COLLIDES;
                    }
                }
            }
            if (out[i] == Decision.DRAWN) {
                drawn.add(at);
            }
        }
        return List.of(out);
    }

    static int priority(MoonPlace m) {
        int state = m.discRelation() == DiscRelation.IN_FRONT ? 0
                : m.shadowRelation() == ShadowRelation.SUNLIT ? 1 : 2;
        return state * 10 + m.moon().ordinal();
    }

    // ---- C. the frame -------------------------------------------------------------------

    private static void frame(Ranges r) {
        p("## C. The frame: each body at its own J2000 place");
        p("");
        p(String.format(Locale.ROOT, "The chart is drawn in J2000. As for Jupiter and Saturn (#482, #578),"
                + " each moon is proposed at its own astrometric J2000 place, never at Mars's place plus"
                + " the table's apparent-of-date offsets: over the moons' years the two differ by at most"
                + " %.3f″ (%.2f px at 0.25°), the precession and aberration the table's offsets carry."
                + " Mars's pole and tilt are not drawn: no surface is shown, and the flattening is under"
                + " a pixel on every field.", r.offsetDifference,
                r.offsetDifference * JovianCartographyStudyMain.pxPerArcsec(0.25, PAGE_W, PAGE_H)));
        p("");
    }

    // ---- D. the mock-ups ------------------------------------------------------------------

    record Scene(Instant when, boolean geocentric, boolean horizon, boolean sun) {
    }

    private static void mockups() throws Exception {
        p("## D. Mock-ups on the atlas's own pages");
        p("");
        p("Each page is painted by the production renderer over the bundled catalogue. Over it this"
                + " study draws, from the service's J2000 places: **Mars by rule M** - the larger of its"
                + " true equatorial disc and a " + fmt(MARK_PX) + " px mark, phased by the page's own lit"
                + " region, Venus's and Mercury's way, in the page's ink with no colour; **each moon** as a"
                + " " + fmt(MOON_SYMBOL_PX) + " px symbol at its own J2000 place, decided on its own"
                + " (section B): filled when clear, filled over a ground ring when in front, an outline"
                + " when in Mars's shadow, not drawn when behind; **labels** through the production"
                + " `LabelPlacement`, each refused alone. Below the horizon the marks take the subdued"
                + " tone. Where the system is a few pixels across an inset magnifies it - a **study"
                + " magnification, not a field the atlas offers** - drawing Mars's true disc rather than"
                + " the mark, so the moons stand where they truly are.");
        p("");
        // 1. The field ladder at the 2003 opposition.
        for (double field : new double[] {8.0, 3.0, 1.0, 0.5, 0.25}) {
            page("opposition-2003-" + fmt(field), String.format(Locale.ROOT,
                            "The 2003 opposition (Espenak's hour, %s UTC), from the Earth's centre, %s° field",
                            when(OPPOSITION_2003), fmt(field)),
                    new Scene(OPPOSITION_2003, true, false, false), field,
                    field == 3.0 ? NORSK : ENGLISH, field == 1.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    field >= 3.0 ? 12 : 0,
                    "The century's closest named opposition: Mars at its largest, the moons at their widest.");
        }
        // 2. The aphelic 2027 opposition.
        for (double field : new double[] {1.0, 0.25}) {
            page("opposition-2027-" + fmt(field), String.format(Locale.ROOT,
                            "The 2027 aphelic opposition (%s UTC), from the Earth's centre, %s° field",
                            when(OPPOSITION_2027), fmt(field)),
                    new Scene(OPPOSITION_2027, true, false, false), field, ENGLISH, ChartPalette.WHITE_PAPER,
                    field == 1.0 ? 12 : 0, "Mars about half its 2003 size; the moons closer to it.");
        }
        // 3. The greatest phase.
        page("greatest-phase-0.25", "Mars at its greatest phase angle (" + day(GREATEST_PHASE) + "), Oslo, 0.25° field",
                new Scene(GREATEST_PHASE, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "The slimmest Mars of the century: the dark part on the side away from the Sun.");
        page("greatest-phase-1", "Mars at its greatest phase angle (" + day(GREATEST_PHASE) + "), Oslo, 1° field",
                new Scene(GREATEST_PHASE, false, false, false), 1.0, NORSK, ChartPalette.BLACK_SKY, 16,
                "On the 6 px mark the phase is a fraction of a pixel; the inset shows it.");
        // 4. The conjunction, with the Sun.
        MarsObservation atConjunction = service.observe(OSLO.at(CONJUNCTION_2026));
        page("conjunction-2026-3", "The 2026 conjunction (Espenak's hour, " + when(CONJUNCTION_2026) + " UTC), Oslo, 3° field, with the Sun",
                new Scene(CONJUNCTION_2026, false, false, true), 3.0, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                atConjunction.phase() == MarsService.Phase.BEHIND_SUN
                        ? "Mars behind the Sun: the Sun's opaque disc is painted after the mark and covers it (#514)."
                        : String.format(Locale.ROOT, "At the published hour Mars is %.2f° from the Sun's centre -"
                                + " the conjunction is in ecliptic longitude - so the mark stands beside the"
                                + " production Sun, not behind it; only inside the Sun's disc would the Sun cover it.",
                                atConjunction.elongationDegrees()));
        // 5. Phobos's states, Horizons' Oslo rows.
        page("phobos-behind-in-shadow-0.25", "Phobos behind Mars in its shadow (Horizons U, " + day(PHOBOS_BEHIND_IN_SHADOW) + "), Oslo, 0.25° field",
                new Scene(PHOBOS_BEHIND_IN_SHADOW, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "A moon behind Mars is not drawn; its state is spoken, never drawn.");
        page("phobos-in-shadow-0.25", "Phobos in Mars's shadow (Horizons u, " + day(PHOBOS_IN_SHADOW) + "), Oslo, 0.25° field",
                new Scene(PHOBOS_IN_SHADOW, false, false, false), 0.25, NORSK, ChartPalette.BLACK_SKY, 0,
                "Eclipsed: the symbol as an outline.");
        page("phobos-in-front-0.25", "Phobos in front of Mars (Horizons t, " + day(PHOBOS_IN_FRONT) + "), Oslo, 0.25° field",
                new Scene(PHOBOS_IN_FRONT, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "In transit, on a disc too small to show it: the symbol over a ground ring, or unresolved by rule.");
        // 6. Deimos at its widest.
        page("deimos-widest-0.25", "Deimos at its greatest separation of the 2003 opposition (" + when(DEIMOS_WIDEST_2003) + " UTC), from the Earth's centre, 0.25° field",
                new Scene(DEIMOS_WIDEST_2003, true, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "The widest the system ever stands on a page.");
        // 7. A crowded field outside the moons' years.
        page("pleiades-2100-8", "Mars 2.5° S of the Pleiades (Espenak, " + when(PLEIADES_2100) + " UTC), Oslo, 8° field",
                new Scene(PLEIADES_2100, false, false, false), 8.0, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "A crowded catalogue field, after the moons' years: Mars alone, as the table shows it.");
        // 8. Below the horizon.
        page("below-horizon-8", "Mars below the horizon (" + when(BELOW_HORIZON) + " UTC), Oslo, 8° field, the horizon drawn",
                new Scene(BELOW_HORIZON, false, true, false), 8.0, ENGLISH, ChartPalette.WHITE_PAPER, 12,
                "Below the drawn horizon the marks and labels take the subdued tone (#615).");
        // 9. Printable.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        page("printable-a4-1", String.format(Locale.ROOT, "The 2003 opposition on the A4 sheet's chart area at 150 dpi (%d × %d px), 1° field", sheetW, sheetH),
                new Scene(OPPOSITION_2003, true, false, false), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, 0, sheetW, sheetH,
                "On paper the system reads at 1° without magnification.");
    }

    private static void page(String name, String title, Scene scene, double field, PageText words,
                             ChartPalette palette, int inset, String caption) throws Exception {
        page(name, title, scene, field, words, palette, inset, PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, Scene scene, double field, PageText words,
                             ChartPalette palette, int inset, int w, int h, String caption) throws Exception {
        Observer observer = OSLO.at(scene.when());
        MarsObservation mars = scene.geocentric() ? service.observeGeocentric(scene.when()) : service.observe(observer);
        Configuration c = !service.moonsAnswerAt(scene.when()) ? null
                : scene.geocentric() ? service.observeMoonsGeocentric(scene.when()) : service.observeMoons(observer);
        ChartScene chart = SaturnCartographyStudyMain.scene(mars.astrometricJ2000(), field, w, h);
        OverlayRegistry registry = new OverlayRegistry();
        if (scene.horizon()) {
            MeridianModule meridian = new MeridianModule(observer);
            registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        }
        if (scene.sun()) {
            SolarSystemModule module = new SolarSystemModule(() -> observer, () -> solar, scene::horizon);
            module.sunShowing(true);
            registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(palette);
        Set<ChartStructure> raised = Set.of();
        boolean dark = palette == ChartPalette.BLACK_SKY;
        boolean below = scene.horizon() && mars.horizontal() != null && mars.horizontal().altitudeDegrees() < 0;
        StringBuilder note = new StringBuilder();
        StringBuilder labelNote = new StringBuilder();
        Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
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
                        boolean marsBehindSun = scene.sun() && mars.phase() == MarsService.Phase.BEHIND_SUN;
                        if (!marsBehindSun) {
                            ReferenceInk.paintBodies(layerG, page, registry.collect(), palette, words, reserved,
                                    ReferenceInk.referenceBoxes(page, registry.collect(), words, reserved,
                                            ChartStructure.membersOf(raised)), ChartStructure.membersOf(raised));
                        }
                        drawSystem(layerG, page, field, 1.0, 0, 0, mars, c, palette, below, obstacles,
                                new LabelPlacement.Page(new Rectangle2D.Double(0, 0, w, h), "the paper"),
                                tm.labels(), note, labelNote, w, h);
                        if (marsBehindSun) {
                            note.append("the Sun painted after Mars, nearer; ");
                            ReferenceInk.paintBodies(layerG, page, registry.collect(), palette, words, reserved,
                                    ReferenceInk.referenceBoxes(page, registry.collect(), words, reserved,
                                            ChartStructure.membersOf(raised)), ChartStructure.membersOf(raised));
                        }
                    }, null, raised);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (inset > 0) {
                DrawnPage drawnPage = DrawnPage.of(chart);
                ViewportMapping mapping = new ViewportMapping(drawnPage);
                var projected = drawnPage.projection().project(mars.astrometricJ2000());
                if (projected.isPresent()) {
                    PixelPoint centre = mapping.toPixel(projected.get());
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
                    drawSystem(gi, drawnPage, 0.0, inset, bx + box / 2.0 - centre.x() * inset,
                            by + box / 2.0 - centre.y() * inset, mars, c, palette, below, new ArrayList<>(),
                            new LabelPlacement.Page(new Rectangle2D.Double(bx + 2, by + 2, box - 4, box - 20), "the inset"),
                            ChartRenderer.TextMetrics.of(gi).labels(), new StringBuilder(), new StringBuilder(), w, h);
                    gi.dispose();
                    g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.setFont(g.getFont().deriveFont(10f));
                    g.drawString(String.format(Locale.ROOT, "study magnification ×%d - not a field the atlas offers", inset),
                            bx + 6, by + box - 6);
                }
            }
            g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString("MOCKUP - study ink over a production page (#605)", 8, h - 8);
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
            q("Each page's labels as this desktop's fonts placed them; a label not named took its first"
                    + " (right-hand) candidate.");
            q("");
        }
        q("- **" + title + "** (`" + name + ".png`): "
                + (labelNote.length() == 0 ? "every label at its first candidate."
                        : labelNote.toString().strip().replaceAll(";$", ".")));
    }

    record Drawn(String id, String label, double x, double y, double reach, boolean below) {
    }

    private static void drawSystem(Graphics2D g, DrawnPage page, double field, double zoom, double ox, double oy,
                                   MarsObservation mars, Configuration c, ChartPalette palette, boolean below,
                                   List<LabelPlacement.Obstacle> obstacles, LabelPlacement.Page paper,
                                   java.awt.FontMetrics font, StringBuilder note, StringBuilder labelNote,
                                   int w, int h) {
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        var projected = projection.project(mars.astrometricJ2000());
        if (projected.isEmpty()) {
            note.append("Mars off the page, nothing drawn; ");
            return;
        }
        PixelPoint mc = mapping.toPixel(projected.get());
        double x = mc.x() * zoom + ox;
        double y = mc.y() * zoom + oy;
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, mars.astrometricJ2000());
        double truePx = mars.equatorialDiameterArcseconds() * per;
        // A magnified inset shows the true geometry: Mars's true disc, never the page's mark.
        double drawnPx = zoom == 1.0 ? Math.max(truePx, MARK_PX) : truePx;
        double r = drawnPx / 2.0 * zoom;
        boolean dark = palette == ChartPalette.BLACK_SKY;
        Color ink = below ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170)) : palette.starInk();
        Color lighter = brightness(palette.ground()) >= brightness(ink) ? palette.ground() : ink;
        Color darker = lighter == palette.ground() ? ink : palette.ground();
        Ellipse2D disc = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g.setColor(mix(darker, lighter, ReferenceInk.darkSide()));
        g.fill(disc);
        PageBasis basis = PageBasis.at(page, mars.astrometricJ2000()).orElse(null);
        if (basis != null) {
            g.setColor(lighter);
            g.fill(ReferenceInk.litRegion(new PixelPoint(x, y), r, basis, new OverlayContribution.Lit(
                    mars.illuminatedFraction(), mars.brightLimbAngleDegrees(), mars.phaseAngleDegrees())));
        }
        g.setColor(ink);
        g.setStroke(new BasicStroke(r >= 4.0 ? 1.0f : 0.75f));
        g.draw(disc);
        note.append(String.format(Locale.ROOT, "Mars by rule M, true disc %.2f px, drawn %.2f px, k = %.3f%s; ",
                truePx, drawnPx, mars.illuminatedFraction(), below ? ", below the horizon" : ""));
        List<Drawn> drawn = new ArrayList<>();
        drawn.add(new Drawn(MARS, "Mars", x, y, r, below));
        if (c == null) {
            note.append("the moons not computed at this instant (2000-2049 only); ");
        } else {
            List<Decision> decisions = decide(zoom == 1.0 ? field : 0.0, c, per);
            double sr = MOON_SYMBOL_PX / 2.0;
            for (int i = 0; i < 2; i++) {
                MoonPlace m = c.moons().get(i);
                String moonName = name(m.moon());
                if (decisions.get(i) != Decision.DRAWN) {
                    note.append(moonName).append(' ').append(decisions.get(i).name().toLowerCase(Locale.ROOT)
                            .replace('_', ' ')).append(", not drawn; ");
                    continue;
                }
                PixelPoint pc = mapping.toPixel(projection.project(m.astrometricJ2000()).orElseThrow());
                double mx = pc.x() * zoom + ox;
                double my = pc.y() * zoom + oy;
                Ellipse2D dot = new Ellipse2D.Double(mx - sr, my - sr, 2 * sr, 2 * sr);
                boolean dim = m.shadowRelation() != ShadowRelation.SUNLIT;
                if (m.discRelation() == DiscRelation.IN_FRONT) {
                    g.setColor(palette.ground());
                    g.fill(new Ellipse2D.Double(mx - sr - 1, my - sr - 1, 2 * sr + 2, 2 * sr + 2));
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
                note.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) %s; ", moonName, mx, my,
                        m.discRelation() == DiscRelation.IN_FRONT ? "in front" : dim ? "shadowed" : "clear"));
                drawn.add(new Drawn(m.moon().name().toLowerCase(Locale.ROOT), moonName, mx, my, sr, below));
            }
        }
        List<LabelPlacement.Obstacle> occupied = new ArrayList<>(obstacles);
        for (Drawn d : drawn) {
            occupied.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK, d.id(),
                    new Ellipse2D.Double(d.x() - d.reach(), d.y() - d.reach(), 2 * d.reach(), 2 * d.reach())));
        }
        List<LabelPlacement.Request> requests = new ArrayList<>();
        for (int i = 0; i < drawn.size(); i++) {
            Drawn d = drawn.get(i);
            requests.add(new LabelPlacement.Request(LabelPlacement.Family.STAR, d.id(), d.label(), d.x(), d.y(),
                    JovianCartographyStudyMain.around(d.x(), d.y(), d.reach(), font.stringWidth(d.label()),
                            font.getHeight(), font.getAscent()), d.id(), null, false, i));
        }
        g.setFont(font.getFont());
        for (LabelPlacement.Placement placed : JovianCartographyStudyMain.placeRefusing(w, h, occupied, paper, requests)) {
            if (placed.omitted()) {
                if (zoom == 1.0) {
                    labelNote.append(placed.request().text()).append("'s label refused; ");
                }
                continue;
            }
            g.setColor(ink);
            Rectangle2D box = placed.at();
            g.drawString(placed.request().text(), (float) box.getX(), (float) (box.getY() + font.getAscent()));
            if (zoom == 1.0 && placed.candidate() != 0) {
                labelNote.append(String.format(Locale.ROOT, "%s's label at candidate %d; ",
                        placed.request().text(), placed.candidate()));
            }
        }
    }

    // ---- E. what the study proposes ------------------------------------------------------------

    private static void proposals(Ranges r) {
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p("## E. What the measurements support");
        p("");
        p(String.format(Locale.ROOT, "1. **Rule M, phased.** Mars's true disc is under the %s px mark on"
                + " every ladder field but 1° (where it exceeds it only near the closest oppositions):"
                + " the mark, phased by the page's lit region, stands for Mars, as Venus's and Mercury's"
                + " do; the true disc takes over wherever it is larger.", fmt(MARK_PX)));
        p(String.format(Locale.ROOT, "2. **No colour.** The phase is real but slight (the dark part at"
                + " most %.2f″, %.2f px at 1°); nothing in the contract measures a colour, so the mark is"
                + " drawn in the page's ink, as every other body.", r.defectMax, r.defectMax * at1));
        p("3. **Moons decided on their own**, Jupiter's 3 px symbols and rules (section B). By the"
                + " geometry alone Phobos stands clear of Mars's drawn mark almost only on the closer fields"
                + " (in 0.3 % of weeks at 1°), and"
                + " Deimos from 2°. Jupiter's floor would draw both at 1° and narrower even where they"
                + " overlap the mark, so whether that floor applies to Mars is a question in its own right.");
        p("4. **Jupiter's vocabulary A**, with Mars's name; no shadow on Mars, no ring of any kind.");
        p("5. **Selection and Centre on chart** for Mars as for the other planets; whether a moon is"
                + " selectable is the owner's.");
        p("6. **The closer fields** are where the moons separate (section B); eligibility is the owner's.");
        p("7. **Below the horizon, black sky, Norwegian and paper** as every body's (#615, #526).");
        p("8. **The split:** Mars (the planet, phased by rule M) first, then the moons.");
        p("");
    }

    // ---- helpers -----------------------------------------------------------------------------------

    static String name(Moon m) {
        return m == Moon.PHOBOS ? "Phobos" : "Deimos";
    }

    private static String day(Instant t) {
        return t.toString().substring(0, 10);
    }

    private static String when(Instant t) {
        return t.toString().substring(0, 16).replace('T', ' ');
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v)
                : String.format(Locale.ROOT, "%s", Double.toString(v));
    }

    static double wrap(double degrees) {
        return ((degrees + 540.0) % 360.0) - 180.0;
    }

    private static double brightness(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static Color mix(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }
}
