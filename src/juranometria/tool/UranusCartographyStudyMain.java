package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
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
import juranometria.chart.StarSizePolicy;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
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
import juranometria.solar.SolarSystemService;
import juranometria.solar.UranusService;
import juranometria.solar.UranusService.Configuration;
import juranometria.solar.UranusService.DiscRelation;
import juranometria.solar.UranusService.Moon;
import juranometria.solar.UranusService.MoonPlace;
import juranometria.solar.UranusService.ShadowRelation;
import juranometria.solar.UranusService.UranusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Uranus and its five ruled moons on the atlas's pages, measured, and the
 * candidate rules drawn (issue #637): what drawing the system honestly
 * needs, through the production projections and renderer, with this
 * study's own candidate marks drawn over pages the production composition
 * painted, as the Venus (#512), Saturn (#578) and Mars (#605) studies did.
 * No production Uranus layer exists and none is built here; every image is
 * a mock-up and says so in its corner.
 *
 * <p>The numbers come from the accepted Uranus service (#634) under the
 * contract ruled on #633 and handed over by #636
 * ({@code docs/decisions/uranus-handover.md}): each body's astrometric
 * J2000 place, the equatorial and polar diameters, the illuminated
 * fraction, the planetocentric tilt, the pole's J2000 position angle and
 * the moons' states on Uranus's flattened figure. The rings are the PDS
 * Ring-Moon Systems Node's radii as {@code uranus-system.md} quotes them,
 * drawn only at their true projected size, never enlarged. The closer
 * fields are painted through viewports, as #539 painted them; the ladder
 * still refuses them.
 *
 * <p>Deterministic: the bundled packs, the contract's named instants and
 * Espenak's rows it cites, Oslo at sea level, {@code Locale.ROOT} numbers.
 * The portable report goes to stdout ({@code make uranus-cartography-study});
 * the font-dependent label placements to {@code platform.md}.
 */
public final class UranusCartographyStudyMain {

    private static final File DIR = new File("docs/studies/uranus-cartography");
    static final int PAGE_W = 900;
    static final int PAGE_H = 700;
    /** The contract's observer: Oslo at sea level, at the study's "today". */
    static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-27T12:00:00Z"));
    /** The candidate closer fields, as #539 painted them. */
    static final double[] CLOSER_FIELDS = {0.5, 0.25};

    // The named instants, each cited where the contract or the almanacs give it.
    /** Espenak's 2026 opposition hour (uranus-events.txt). */
    static final Instant OPPOSITION_2026 = Instant.parse("2026-11-25T23:00:00Z");
    /** Espenak's 2026 conjunction hour (uranus-events.txt). */
    static final Instant CONJUNCTION_2026 = Instant.parse("2026-05-22T16:00:00Z");
    /** Uranus's largest equatorial diameter in the weekly Oslo matrix (measurements.md, 4.07631″). */
    static final Instant LARGEST = Instant.parse("2051-03-13T00:00:00Z");
    /** Titania's and Oberon's greatest separations in the weekly Oslo matrix (measurements.md). */
    static final Instant OBERON_WIDEST = Instant.parse("2052-03-23T00:00:00Z");
    /** Horizons' Oslo rows (matrix-7d-*-oslo.txt): Miranda U and t, Ariel u, Umbriel O. */
    static final Instant MIRANDA_BEHIND_IN_SHADOW = Instant.parse("2005-03-19T00:00:00Z");
    static final Instant MIRANDA_IN_FRONT = Instant.parse("2005-05-28T00:00:00Z");
    static final Instant ARIEL_IN_SHADOW = Instant.parse("2006-02-04T00:00:00Z");
    static final Instant UMBRIEL_BEHIND = Instant.parse("2009-12-05T00:00:00Z");
    /** The last hour before the moons' years (#633, U-6a): Uranus alone. */
    static final Instant BEFORE_THE_MOONS = Instant.parse("1999-12-31T23:00:00Z");
    /** Horizons' "today" row at Oslo: Uranus 8.05° below the horizon (named-uranus-oslo.txt). */
    static final Instant BELOW_HORIZON = Instant.parse("2026-10-27T12:00:00Z");

    /**
     * The rings' radii from the PDS Ring-Moon Systems Node's table, as
     * {@code uranus-system.md} ("The rings, found") quotes them: the Six
     * ring, innermost of the nine classical rings, and Epsilon, outermost.
     */
    static final double SIX_RING_KM = 41_837.0;
    static final double EPSILON_RING_KM = 51_149.0;

    /** The atlas's smallest visible star mark, as a diameter. */
    private static final double MIN_INK_PX = 2.0 * StarSizePolicy.DEFAULT.minimumRadiusPx();
    /** Proposed: Uranus's minimum cartographic mark, Jupiter's ruled 6 px (rule M). */
    static final double MARK_PX = JovianModule.MINIMUM_MARK_PX;
    /** Proposed: a moon's symbol, Jupiter's ruled 3 px - never an apparent size. */
    static final double MOON_SYMBOL_PX = JovianCartographyStudyMain.MOON_SYMBOL_PX;
    /** Proposed: two symbols are separable at this distance, centre to centre (Jupiter's). */
    static final double SEPARABLE_PX = JovianCartographyStudyMain.SEPARABLE_PX;
    /** The proposed page identity (no production key exists). */
    static final String URANUS = "uranus";
    static final int MOONS = Moon.values().length;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    static UranusService service;
    static SolarSystemService solar;

    /** What the study decides for one moon on one page. */
    enum Decision { DRAWN, BEHIND, AT_URANUS, IN_FRONT_UNRESOLVED, COLLIDES }

    private UranusCartographyStudyMain() {
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
        service = UranusService.load();
        solar = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM, "Uranus's and its moons' labels, as this desktop's fonts place them",
                "Issue #637.");
        p("# Uranus and its five moons on the page: measurements and mock-ups");
        p("");
        p("Issue #637. What a cartography rule would mean on the atlas's own pages, measured"
                + " through the production projections and the accepted Uranus service (#634) under"
                + " the contract ruled on #633 and handed over by #636, with candidate marks drawn by"
                + " this study over pages the production composition painted, as the Venus (#512),"
                + " Saturn (#578) and Mars (#605) studies did. Nothing here is production rendering;"
                + " every image is a mock-up and says so in its corner. The closer fields are painted"
                + " through viewports, as #539 painted them; the ladder still refuses them. Regenerate"
                + " with `make uranus-cartography-study`.");
        p("");
        try {
            Ranges r = ranges();
            scale(r);
            thresholds(r);
            rings(r);
            frame(r);
            mockups(r);
            proposals(r);
            System.out.print(REPORT);
            System.out.flush();
            PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
        } finally {
            // The evidence contract runs every generator in one JVM under a measured
            // heap (#323, #635): nothing this study loaded may outlive it.
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
    record Ranges(double eqMin, double eqMax, double flatMax, double kMin, String kMinAt,
                  double defectMax, double tiltMaxAbs, String tiltMaxAt, double tiltMinAbs,
                  String tiltMinAt, Instant poleOn, Instant equatorOn, double ringMax, double ringMin,
                  int uranusSamples, double[] sepMin, double[] sepMax, String[] sepMaxAt,
                  double[] diamMax, double offsetDifference, List<Configuration> weekly) {
    }

    static Ranges ranges() {
        double eqMin = 1e9, eqMax = 0, flatMax = 0, kMin = 1, defectMax = 0;
        double tiltMax = -1, tiltMin = 1e9, ringMax = 0, ringMin = 1e9;
        String kMinAt = "", tiltMaxAt = "", tiltMinAt = "";
        Instant poleOn = null, equatorOn = null;
        int n = 0;
        for (Instant t = Instant.parse("1900-01-01T00:00:00Z"); t.isBefore(Instant.parse("2101-01-01T00:00:00Z"));
                t = t.plus(Duration.ofDays(7))) {
            UranusObservation u = service.observeGeocentric(t);
            n++;
            eqMin = Math.min(eqMin, u.equatorialDiameterArcseconds());
            eqMax = Math.max(eqMax, u.equatorialDiameterArcseconds());
            flatMax = Math.max(flatMax, u.equatorialDiameterArcseconds() - u.polarDiameterArcseconds());
            if (u.illuminatedFraction() < kMin) {
                kMin = u.illuminatedFraction();
                kMinAt = day(t);
            }
            defectMax = Math.max(defectMax, u.equatorialDiameterArcseconds() * (1.0 - u.illuminatedFraction()));
            double tilt = Math.abs(u.tiltDegrees());
            if (tilt > tiltMax) {
                tiltMax = tilt;
                tiltMaxAt = day(t);
                poleOn = t;
            }
            if (tilt < tiltMin) {
                tiltMin = tilt;
                tiltMinAt = day(t);
                equatorOn = t;
            }
            double ring = ringDiameterArcseconds(u, EPSILON_RING_KM);
            ringMax = Math.max(ringMax, ring);
            ringMin = Math.min(ringMin, ring);
        }
        double[] sepMin = new double[MOONS];
        double[] sepMax = new double[MOONS];
        String[] sepMaxAt = new String[MOONS];
        double[] diamMax = new double[MOONS];
        java.util.Arrays.fill(sepMin, 1e9);
        java.util.Arrays.fill(sepMaxAt, "");
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
                double[] j = j2000Offset(c.uranus(), m);
                offsetDiff = Math.max(offsetDiff, Math.hypot(j[0] - m.xArcseconds(), j[1] - m.yArcseconds()));
            }
        }
        return new Ranges(eqMin, eqMax, flatMax, kMin, kMinAt, defectMax, tiltMax, tiltMaxAt, tiltMin,
                tiltMinAt, poleOn, equatorOn, ringMax, ringMin, n, sepMin, sepMax, sepMaxAt, diamMax,
                offsetDiff, weekly);
    }

    /** A ring's apparent diameter across its major axis, arcseconds: 2·asin(R/Δ). */
    static double ringDiameterArcseconds(UranusObservation u, double radiusKm) {
        return 2.0 * Math.toDegrees(Math.asin(radiusKm / u.distanceKm())) * 3600.0;
    }

    /** A moon's offset from Uranus in J2000, arcseconds, east and north, from the two J2000 places. */
    static double[] j2000Offset(UranusObservation uranus, MoonPlace m) {
        double dra = wrap(m.astrometricJ2000().raDegrees() - uranus.astrometricJ2000().raDegrees())
                * Math.cos(Math.toRadians(uranus.astrometricJ2000().decDegrees())) * 3600.0;
        double ddec = (m.astrometricJ2000().decDegrees() - uranus.astrometricJ2000().decDegrees()) * 3600.0;
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
        StringBuilder moons = new StringBuilder();
        for (Moon m : Moon.values()) {
            int i = m.ordinal();
            moons.append(String.format(Locale.ROOT, "%s %.2f-%.2f″ (the most on %s)%s", name(m), r.sepMin[i],
                    r.sepMax[i], r.sepMaxAt[i], i < MOONS - 1 ? ", " : ""));
        }
        p(String.format(Locale.ROOT, "Over 1900-2100 (%d weekly geocentric instants from the service):"
                + " Uranus's equatorial diameter is %.2f-%.2f″, its polar diameter at the tilt never more"
                + " than %.3f″ smaller; it is never less than %.4f %% lit (on %s), and the dark part of its"
                + " disc - the diameter times (1 − k) - is at most %.4f″. The planetocentric tilt reaches"
                + " %.2f° (on %s: the Earth almost over a pole) and comes within %.2f° of the equator (on"
                + " %s). The Epsilon ring, the outermost (%.0f km), is %.2f-%.2f″ across. Over the moons'"
                + " years (%d weekly instants, 2000-2100) the moons stand from Uranus's centre: %s; the"
                + " largest moon disc is %.3f″. The atlas's smallest star mark is %.1f px across.",
                r.uranusSamples, r.eqMin, r.eqMax, r.flatMax, 100 * r.kMin, r.kMinAt, r.defectMax,
                r.tiltMaxAbs, r.tiltMaxAt, r.tiltMinAbs, r.tiltMinAt, EPSILON_RING_KM, r.ringMin, r.ringMax,
                r.weekly.size(), moons, java.util.Arrays.stream(r.diamMax).max().orElse(0), MIN_INK_PX));
        p("");
        p(String.format(Locale.ROOT, "Pixels on the 900 × 700 page at its centre (each field's own"
                + " projection) - the ladder's fields, then the two closer fields below it; millimetres"
                + " on the A4 sheet's chart area (%.0f mm wide).", PaperSize.A4.chartWideMm()));
        p("");
        StringBuilder head = new StringBuilder("| field | Uranus px (min-max) | flattening px (max) | dark part px (max) |");
        StringBuilder rule = new StringBuilder("|---:|---|---:|---:|");
        for (Moon m : Moon.values()) {
            head.append(' ').append(name(m)).append(" from Uranus px (max) |");
            rule.append("---:|");
        }
        head.append(" Epsilon ring px (max) | Uranus on A4 mm (max) |");
        rule.append("---:|---:|");
        p(head.toString());
        p(rule.toString());
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double mm = PaperSize.A4.chartWideMm() / (field * 3600.0);
            StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "| %s%s | %.2f-%.2f | %.3f | %.3f |",
                    fmt(field) + "°", field < ChartViewState.normalMinimumFieldDegrees() ? " (closer)" : "",
                    r.eqMin * per, r.eqMax * per, r.flatMax * per, r.defectMax * per));
            for (int i = 0; i < MOONS; i++) {
                row.append(String.format(Locale.ROOT, " %.2f |", r.sepMax[i] * per));
            }
            row.append(String.format(Locale.ROOT, " %.2f | %.3f |", r.ringMax * per, r.eqMax * mm));
            p(row.toString());
        }
        p("");
    }

    // ---- B. thresholds ----------------------------------------------------------------

    private static void thresholds(Ranges r) {
        p("## B. Where things become resolvable or separable");
        p("");
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p(String.format(Locale.ROOT, "**Uranus against the %s px minimum mark (rule M).** The page scale"
                + " at its centre is %.4f px per arcsecond at 1° and scales with 1/field. Uranus's largest"
                + " disc would reach %s px only at %.3f°, its smallest at %.3f° - narrower than every field"
                + " offered, the closer ones included: **on every page the mark stands for Uranus.**",
                fmt(MARK_PX), at1, fmt(MARK_PX), r.eqMax * at1 / MARK_PX, r.eqMin * at1 / MARK_PX));
        p("");
        p(String.format(Locale.ROOT, "**The phase and the figure.** The dark part is at most %.4f px at"
                + " 0.25°; on a %s px mark at k = %.5f it is %.3f px. The flattening is at most %.3f px at"
                + " 0.25°. Neither is ever visible: a phased or an oblate mark would draw nothing a reader"
                + " could see.", r.defectMax * at1 / 0.25, fmt(MARK_PX), r.kMin, MARK_PX * (1 - r.kMin),
                r.flatMax * at1 / 0.25));
        p("");
        p(String.format(Locale.ROOT, "**Each moon decided on its own (Jupiter's rule 4, Mars's R-3).** Over"
                + " the %d weekly geocentric configurations of the moons' years, on each field: behind"
                + " Uranus, omitted; a moon in front is drawn only on a drawn disc of at least %s px; a moon"
                + " whose %s px symbol comes within %s px of Uranus's drawn edge is not drawn (at Uranus);"
                + " and a moon within %s px of one already drawn is not drawn (collides); priority in front,"
                + " clear, shadowed, then outward from Uranus. Uranus's drawn disc is the larger of its true"
                + " disc and the %s px mark.", r.weekly.size(), fmt(2 * SEPARABLE_PX), fmt(MOON_SYMBOL_PX),
                fmt(SEPARABLE_PX / 2.0), fmt(SEPARABLE_PX), fmt(MARK_PX)));
        p("");
        p("The moons' columns and *all five* are the geometry alone, Mars's rule: whether a moon's symbol"
                + " would stand clear of Uranus's drawn mark and of the moons already drawn. The last column"
                + " is what Jupiter's floor rule draws, on the fields it covers (the normal minimum and"
                + " narrower), where every moon not behind Uranus is drawn whatever it overlaps: the share"
                + " of moon places drawn.");
        p("");
        StringBuilder head = new StringBuilder("| field |");
        StringBuilder rule = new StringBuilder("|---:|");
        for (Moon m : Moon.values()) {
            head.append(' ').append(name(m)).append(" clear |");
            rule.append("---:|");
        }
        head.append(" some moon drawn | all five drawn | collides | moon places drawn by the floor |");
        rule.append("---:|---:|---:|---:|");
        p(head.toString());
        p(rule.toString());
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            boolean floored = field <= ChartViewState.normalMinimumFieldDegrees() + 1e-9;
            int[] clear = new int[MOONS];
            int some = 0, all = 0, collides = 0, floorDrawn = 0, floorPlaces = 0;
            for (Configuration c : r.weekly) {
                List<Decision> d = decide(false, c, per);
                int drawn = 0;
                for (int i = 0; i < MOONS; i++) {
                    if (d.get(i) == Decision.DRAWN) {
                        clear[i]++;
                        drawn++;
                    }
                    collides += d.get(i) == Decision.COLLIDES ? 1 : 0;
                }
                some += drawn > 0 ? 1 : 0;
                all += drawn == MOONS ? 1 : 0;
                if (floored) {
                    List<Decision> f = decide(true, c, per);
                    for (Decision x : f) {
                        floorDrawn += x == Decision.DRAWN ? 1 : 0;
                        floorPlaces++;
                    }
                }
            }
            int total = r.weekly.size();
            StringBuilder row = new StringBuilder("| " + fmt(field) + "° |");
            for (int i = 0; i < MOONS; i++) {
                row.append(' ').append(pct(clear[i], total)).append(" |");
            }
            row.append(String.format(Locale.ROOT, " %s | %s | %s | %s |", pct(some, total), pct(all, total),
                    pct(collides, total * MOONS), floored ? pct(floorDrawn, floorPlaces) : "-"));
            p(row.toString());
        }
        p("");
        p("A moon's column and *some*/*all* are shares of the weekly configurations; *collides* is a"
                + " share of all moon places; a moon behind Uranus is drawn in none.");
        p("");
    }

    static String pct(int n, int total) {
        return String.format(Locale.ROOT, "%.1f %%", 100.0 * n / total);
    }

    /** The study's decision for each moon, with Jupiter's floor applied or not. */
    static List<Decision> decide(boolean floor, Configuration c, double perArcsec) {
        double markPx = Math.max(c.uranus().equatorialDiameterArcseconds() * perArcsec, MARK_PX);
        Decision[] out = new Decision[MOONS];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < MOONS; i++) {
            order.add(i);
        }
        order.sort((a, b) -> Integer.compare(priority(c.moons().get(a)), priority(c.moons().get(b))));
        List<double[]> drawn = new ArrayList<>();
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i : order) {
            MoonPlace m = c.moons().get(i);
            double[] o = j2000Offset(c.uranus(), m);
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
                out[i] = Decision.AT_URANUS;
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

    // ---- C. the rings ---------------------------------------------------------------------

    private static void rings(Ranges r) {
        p("## C. The rings at their true projected size");
        p("");
        p(String.format(Locale.ROOT, "The nine classical rings lie in Uranus's equator, from the Six ring"
                + " (%.0f km) to Epsilon (%.0f km), with widths of a few to 96 km (`uranus-system.md`)."
                + " Their opening to us is the planetocentric tilt, which the table already gives. On a page"
                + " the Epsilon ring would be an ellipse %.2f-%.2f″ across its major axis, its minor axis"
                + " that times |sin tilt|. In pixels (the major axis, at its largest; a ring's width is far"
                + " under a pixel on every field):", SIX_RING_KM, EPSILON_RING_KM, r.ringMin, r.ringMax));
        p("");
        p("| field | Epsilon across, px (max) | beyond the 6 px mark by, px | Six ring across, px (max) |");
        p("|---:|---:|---:|---:|");
        for (double field : fields()) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double eps = r.ringMax * per;
            double six = r.ringMax * SIX_RING_KM / EPSILON_RING_KM * per;
            p(String.format(Locale.ROOT, "| %s | %.2f | %s | %.2f |", fmt(field) + "°", eps,
                    eps > MARK_PX ? String.format(Locale.ROOT, "%.2f", (eps - MARK_PX) / 2.0) : "inside it", six));
        }
        p("");
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H);
        p(String.format(Locale.ROOT, "The Epsilon ring is wider than Uranus's %s px mark only where the field"
                + " is narrower than %.2f°: on the ordinary ladder it lies inside the mark, and only the"
                + " 0.25° closer field draws it %.2f px beyond the mark's edge. Drawn true, it would be a"
                + " hairline ellipse, at an equator-on week a line; it is never enlarged without an explicit"
                + " cartographic ruling (#637).", fmt(MARK_PX), r.ringMax * at1 / MARK_PX,
                (r.ringMax * at1 / 0.25 - MARK_PX) / 2.0));
        p("");
    }

    // ---- D. the frame -------------------------------------------------------------------

    private static void frame(Ranges r) {
        p("## D. The frame: each body at its own J2000 place");
        p("");
        p(String.format(Locale.ROOT, "The chart is drawn in J2000. As for Jupiter, Saturn and Mars, each"
                + " moon is proposed at its own astrometric J2000 place, never at Uranus's place plus the"
                + " table's apparent-of-date offsets: over the moons' years the two differ by at most"
                + " %.3f″ (%.2f px at 0.25°). Where a mock-up draws a ring it is oriented by the pole's J2000"
                + " position angle (`UranusService.poleAngleJ2000Degrees`) turned onto the page by the"
                + " page's own projected north and east, its major axis across the pole.", r.offsetDifference,
                r.offsetDifference * JovianCartographyStudyMain.pxPerArcsec(0.25, PAGE_W, PAGE_H)));
        p("");
    }

    // ---- E. the mock-ups ------------------------------------------------------------------

    record Scene(Instant when, boolean geocentric, boolean horizon, boolean sun, boolean ring) {
    }

    private static void mockups(Ranges poles) throws Exception {
        p("## E. Mock-ups on the atlas's own pages");
        p("");
        p("Each page is painted by the production renderer over the bundled catalogue. Over it this"
                + " study draws, from the service's J2000 places: **Uranus by rule M** - the larger of its"
                + " true equatorial disc and a " + fmt(MARK_PX) + " px mark, unphased and in the page's"
                + " ink with no colour; **each moon** as a " + fmt(MOON_SYMBOL_PX) + " px symbol at its own"
                + " J2000 place, decided on its own (section B): filled when clear, filled over a ground ring"
                + " when in front, an outline when in Uranus's shadow, not drawn when behind; where named,"
                + " **the Epsilon ring** as a hairline at its true projected size (section C); **labels**"
                + " through the production `LabelPlacement`, each refused alone. Below the horizon the marks"
                + " take the subdued tone. Where the system is a few pixels across an inset magnifies it - a"
                + " **study magnification, not a field the atlas offers** - drawing Uranus's true disc"
                + " rather than the mark, so the moons stand where they truly are.");
        p("");
        // 1. The field ladder at the 2026 opposition.
        for (double field : new double[] {8.0, 2.0, 1.0, 0.5, 0.25}) {
            page("opposition-2026-" + fmt(field), String.format(Locale.ROOT,
                            "The 2026 opposition (Espenak's hour, %s UTC), from the Earth's centre, %s° field",
                            when(OPPOSITION_2026), fmt(field)),
                    new Scene(OPPOSITION_2026, true, false, false, false), field,
                    field == 2.0 ? NORSK : ENGLISH, field == 1.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    field >= 2.0 ? 24 : field == 1.0 ? 12 : 0,
                    "The next opposition: the five moons beside the mark, the field narrowing.");
        }
        // 2. Uranus at its largest, the moons at their widest.
        page("largest-0.25", "Uranus at its largest (" + day(LARGEST) + "), Oslo, 0.25° field",
                new Scene(LARGEST, false, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "The century's largest disc, still inside the 6 px mark.");
        page("oberon-widest-1", "Titania and Oberon at their widest (" + day(OBERON_WIDEST) + "), Oslo, 1° field",
                new Scene(OBERON_WIDEST, false, false, false, false), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, 12,
                "The widest the system stands on the ladder's narrowest field.");
        page("oberon-widest-0.25", "Titania and Oberon at their widest (" + day(OBERON_WIDEST) + "), Oslo, 0.25° field",
                new Scene(OBERON_WIDEST, false, false, false, false), 0.25, NORSK, ChartPalette.BLACK_SKY, 0,
                "The widest the system ever stands on a page.");
        // 3. The rings, pole-on and equator-on, at their true size.
        page("ring-pole-on-0.25", "The Epsilon ring at its true size, the Earth nearly over a pole ("
                        + day(poles.poleOn()) + "), from the Earth's centre, 0.25° field",
                new Scene(poles.poleOn(), true, false, false, true), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 6,
                "Pole-on, the ring is nearly a circle: on the page a hairline just outside the mark, at the narrowest"
                        + " field the study paints; the inset shows it around the true disc.");
        page("ring-equator-on-0.25", "The Epsilon ring at its true size, the Earth nearly in the equator ("
                        + day(poles.equatorOn()) + "), from the Earth's centre, 0.25° field",
                new Scene(poles.equatorOn(), true, false, false, true), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 6,
                "Equator-on, the ring is a line through the mark; the inset shows it across the true disc.");
        page("ring-pole-on-1", "The Epsilon ring at its true size, pole-on (" + day(poles.poleOn())
                        + "), from the Earth's centre, 1° field",
                new Scene(poles.poleOn(), true, false, false, true), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, 16,
                "On the ladder's narrowest field the ring lies inside the mark; the inset shows where.");
        // 4. The conjunction, with the Sun.
        UranusObservation atConjunction = service.observe(OSLO.at(CONJUNCTION_2026));
        page("conjunction-2026-3", "The 2026 conjunction (Espenak's hour, " + when(CONJUNCTION_2026) + " UTC), Oslo, 3° field, with the Sun",
                new Scene(CONJUNCTION_2026, false, false, true, false), 3.0, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                atConjunction.behindSun()
                        ? "Uranus behind the Sun: the Sun's opaque disc is painted after the mark and covers it (#514)."
                        : String.format(Locale.ROOT, "At the published hour Uranus is %.2f° from the Sun's centre -"
                                + " the conjunction is in ecliptic longitude - so the mark stands beside the"
                                + " production Sun, not behind it; only inside the Sun's disc would the Sun cover it.",
                                atConjunction.elongationDegrees()));
        // 5. The states, Horizons' Oslo rows.
        page("miranda-behind-in-shadow-0.25", "Miranda behind Uranus in its shadow (Horizons U, " + day(MIRANDA_BEHIND_IN_SHADOW) + "), Oslo, 0.25° field",
                new Scene(MIRANDA_BEHIND_IN_SHADOW, false, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 6,
                "A moon behind Uranus is not drawn, on the page or in the inset; its state is spoken, never drawn.");
        page("ariel-in-shadow-0.25", "Ariel in Uranus's shadow (Horizons u, " + day(ARIEL_IN_SHADOW) + "), Oslo, 0.25° field",
                new Scene(ARIEL_IN_SHADOW, false, false, false, false), 0.25, NORSK, ChartPalette.BLACK_SKY, 6,
                "An eclipsed moon stands beside the limb, in the shadow's width: even at 0.25° it is not clear"
                        + " of the mark, so the page does not draw it. The inset shows the outline it would take.");
        page("miranda-in-front-0.25", "Miranda in front of Uranus (Horizons t, " + day(MIRANDA_IN_FRONT) + "), Oslo, 0.25° field",
                new Scene(MIRANDA_IN_FRONT, false, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 6,
                "In transit, on a disc far too small to show it: unresolved by rule on the page; the inset shows"
                        + " the symbol over its ground ring on the true disc.");
        page("umbriel-behind-0.25", "Umbriel behind Uranus (Horizons O, " + day(UMBRIEL_BEHIND) + "), Oslo, 0.25° field",
                new Scene(UMBRIEL_BEHIND, false, false, false, false), 0.25, ENGLISH, ChartPalette.WHITE_PAPER, 6,
                "Occulted: not drawn, and spoken.");
        // 6. Before the moons' years.
        page("before-the-moons-1", "Uranus before the moons' years (" + when(BEFORE_THE_MOONS) + " UTC), from the Earth's centre, 1° field",
                new Scene(BEFORE_THE_MOONS, true, false, false, false), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, 0,
                "Uranus alone, as the table shows it.");
        // 7. Below the horizon.
        page("below-horizon-8", "Uranus below the horizon (" + when(BELOW_HORIZON) + " UTC), Oslo, 8° field, the horizon drawn",
                new Scene(BELOW_HORIZON, false, true, false, false), 8.0, ENGLISH, ChartPalette.WHITE_PAPER, 24,
                "Below the drawn horizon the marks and labels take the subdued tone (#615).");
        // 8. Printable.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        page("printable-a4-1", String.format(Locale.ROOT, "The 2026 opposition on the A4 sheet's chart area at 150 dpi (%d × %d px), 1° field", sheetW, sheetH),
                new Scene(OPPOSITION_2026, true, false, false, false), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, 0, sheetW, sheetH,
                "On paper the outer moons stand clear at 1° without magnification.");
    }

    private static void page(String name, String title, Scene scene, double field, PageText words,
                             ChartPalette palette, int inset, String caption) throws Exception {
        page(name, title, scene, field, words, palette, inset, PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, Scene scene, double field, PageText words,
                             ChartPalette palette, int inset, int w, int h, String caption) throws Exception {
        Observer observer = OSLO.at(scene.when());
        UranusObservation uranus = scene.geocentric() ? service.observeGeocentric(scene.when()) : service.observe(observer);
        Configuration c = !service.moonsAnswerAt(scene.when()) ? null
                : scene.geocentric() ? service.observeMoonsGeocentric(scene.when()) : service.observeMoons(observer);
        ChartScene chart = SaturnCartographyStudyMain.scene(uranus.astrometricJ2000(), field, w, h);
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
        boolean below = scene.horizon() && uranus.horizontal() != null && uranus.horizontal().altitudeDegrees() < 0;
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
                        boolean behindSun = scene.sun() && uranus.behindSun();
                        if (!behindSun) {
                            ReferenceInk.paintBodies(layerG, page, registry.collect(), palette, words, reserved,
                                    ReferenceInk.referenceBoxes(page, registry.collect(), words, reserved,
                                            ChartStructure.membersOf(raised)), ChartStructure.membersOf(raised));
                        }
                        drawSystem(layerG, page, field, 1.0, 0, 0, uranus, c, scene.ring(), palette, below,
                                obstacles, new LabelPlacement.Page(new Rectangle2D.Double(0, 0, w, h), "the paper"),
                                tm.labels(), note, labelNote, w, h);
                        if (behindSun) {
                            note.append("the Sun painted after Uranus, nearer; ");
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
                var projected = drawnPage.projection().project(uranus.astrometricJ2000());
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
                            by + box / 2.0 - centre.y() * inset, uranus, c, scene.ring(), palette, below,
                            new ArrayList<>(),
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
            g.drawString("MOCKUP - study ink over a production page (#637)", 8, h - 8);
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
                                   UranusObservation uranus, Configuration c, boolean ring, ChartPalette palette,
                                   boolean below, List<LabelPlacement.Obstacle> obstacles, LabelPlacement.Page paper,
                                   java.awt.FontMetrics font, StringBuilder note, StringBuilder labelNote,
                                   int w, int h) {
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        var projected = projection.project(uranus.astrometricJ2000());
        if (projected.isEmpty()) {
            note.append("Uranus off the page, nothing drawn; ");
            return;
        }
        PixelPoint uc = mapping.toPixel(projected.get());
        double x = uc.x() * zoom + ox;
        double y = uc.y() * zoom + oy;
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, uranus.astrometricJ2000());
        double truePx = uranus.equatorialDiameterArcseconds() * per;
        // A magnified inset shows the true geometry: Uranus's true disc, never the page's mark.
        double drawnPx = zoom == 1.0 ? Math.max(truePx, MARK_PX) : truePx;
        double r = drawnPx / 2.0 * zoom;
        boolean dark = palette == ChartPalette.BLACK_SKY;
        Color ink = below ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170)) : palette.starInk();
        Color lighter = brightness(palette.ground()) >= brightness(ink) ? palette.ground() : ink;
        Ellipse2D disc = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        // Unphased: the dark part is never a visible fraction of a pixel (section B).
        g.setColor(lighter);
        g.fill(disc);
        g.setColor(ink);
        g.setStroke(new BasicStroke(r >= 4.0 ? 1.0f : 0.75f));
        g.draw(disc);
        note.append(String.format(Locale.ROOT, "Uranus by rule M, true disc %.2f px, drawn %.2f px, unphased%s; ",
                truePx, drawnPx, below ? ", below the horizon" : ""));
        if (ring) {
            double[] up = JovianChartGeometry.pageDirection(projection, mapping, uranus.astrometricJ2000(),
                    service.poleAngleJ2000Degrees(uranus));
            double a = ringDiameterArcseconds(uranus, EPSILON_RING_KM) / 2.0 * per * zoom;
            double b = a * Math.abs(Math.sin(Math.toRadians(uranus.tiltDegrees())));
            AffineTransform keep = g.getTransform();
            g.translate(x, y);
            g.rotate(Math.atan2(up[1], up[0]) + Math.PI / 2.0);
            g.setColor(ink);
            g.setStroke(new BasicStroke(0.75f));
            g.draw(new Ellipse2D.Double(-a, -b, 2 * a, 2 * b));
            g.setTransform(keep);
            note.append(String.format(Locale.ROOT, "the Epsilon ring at its true size, %.2f × %.2f px, tilt %.1f°; ",
                    2 * a / zoom, 2 * b / zoom, uranus.tiltDegrees()));
        }
        List<Drawn> drawn = new ArrayList<>();
        drawn.add(new Drawn(URANUS, "Uranus", x, y, r, below));
        if (c == null) {
            note.append("the moons not computed at this instant (2000-2100 only); ");
        } else {
            // Mars's rule, the proposal: the geometry alone decides, on every field.
            List<Decision> decisions = decide(false, c, per);
            double sr = MOON_SYMBOL_PX / 2.0;
            for (int i = 0; i < MOONS; i++) {
                MoonPlace m = c.moons().get(i);
                String moonName = name(m.moon());
                boolean drawIt = zoom == 1.0 ? decisions.get(i) == Decision.DRAWN
                        : m.discRelation() != DiscRelation.BEHIND;
                if (!drawIt) {
                    if (zoom == 1.0) {
                        note.append(moonName).append(' ').append(decisions.get(i).name().toLowerCase(Locale.ROOT)
                                .replace('_', ' ')).append(", not drawn; ");
                    }
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
                if (zoom == 1.0) {
                    note.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) %s; ", moonName, mx, my,
                            m.discRelation() == DiscRelation.IN_FRONT ? "in front" : dim ? "shadowed" : "clear"));
                }
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

    // ---- F. what the study proposes ------------------------------------------------------------

    private static void proposals(Ranges r) {
        p("## F. What the measurements support");
        p("");
        p(String.format(Locale.ROOT, "1. **Rule M, the mark always.** Uranus's true disc never reaches the %s px"
                + " mark on any field offered, the closer ones included (section B): the mark stands for"
                + " Uranus on every page, a symbol, spoken as one.", fmt(MARK_PX)));
        p("2. **Unphased, round, no colour.** The dark part and the flattening are fractions of a pixel"
                + " even at 0.25° (section B); nothing in the contract measures a colour, so the mark is drawn"
                + " full, round and in the page's ink, as every other body.");
        p("3. **The pole and tilt are not drawn.** On a 6 px symbol they have nothing to orient; the table"
                + " gives both in numbers.");
        p("4. **The rings: table-only.** The Epsilon ring lies inside the mark on every ladder field and is"
                + " a hairline just beyond it at 0.25° (section C); its opening is the tilt the table already"
                + " gives. Never enlarged.");
        p("5. **Moons decided on their own, Mars's rule** (section B): drawn only where a moon's symbol"
                + " stands clear of Uranus's mark and of the moons already drawn, on every field.");
        p("6. **Jupiter's vocabulary A**, with Uranus's name; no shadow on Uranus.");
        p("7. **Selection and Centre on chart** for Uranus as for the other planets; the moons not"
                + " selectable, as Mars's.");
        p("8. **The closer fields** are where the inner moons separate (section B), Miranda only there;"
                + " eligibility is the owner's.");
        p("9. **Below the horizon, black sky, Norwegian and paper** as every body's (#615, #526).");
        p("10. **The split:** Uranus (the mark) first, then the moons.");
        p("");
    }

    // ---- helpers -----------------------------------------------------------------------------------

    static String name(Moon m) {
        String n = m.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
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
}
