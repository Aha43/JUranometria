package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.chart.SkyPosition;
import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.ShowerNight;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.project.PixelPoint;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.meteor.MeteorShowerSession;
import juranometria.ui.meteor.MeteorShowerText;

/**
 * Meteor showers at chart time, as built (#716; written for the article
 * #717): the application's own chart with the production modules and the
 * production {@link MeteorShowerModule} attached, the production
 * {@link MeteorShowerSession} and the panel's own words, at stated chart
 * times from stated places.
 *
 * <p>Nothing here reads a raw source: every value comes from the bundled
 * pack and the production services, so the report reproduces wherever the
 * atlas builds, CI included. The study that chose the rules, with its
 * mocked mark, is {@code docs/studies/meteor-showers}; this is the view a
 * reader has.
 *
 * <p>Standard output is {@code docs/studies/meteor-radiants/measurements.md};
 * the pages and {@code platform.md} are written beside it. Regenerate with
 * {@code make meteor-radiant-study}.
 */
public final class MeteorRadiantStudyMain {

    static final Path OUT = Path.of("docs/studies/meteor-radiants");
    static final int W = 1440;
    static final int H = 900;
    static final int WHOLE_W = 2000;
    static final int WHOLE_H = 1000;
    static final double FIELD = 60.0;

    /** A place, as the report names it. */
    record Place(String name, double latitude, double eastLongitude) {
        Observer at(Instant instant) {
            return new Observer(latitude, eastLongitude, instant);
        }

        String stated() {
            return f("%s (%.2f %s, %.2f %s)", name, Math.abs(latitude),
                    latitude < 0 ? "S" : "N", Math.abs(eastLongitude),
                    eastLongitude < 0 ? "W" : "E");
        }
    }

    static final Place OSLO = new Place("Oslo", MeteorShowerStudyMain.OSLO_LAT,
            MeteorShowerStudyMain.OSLO_LON);
    /** The southern place the atlas's other studies use (the Moon, Mercury and Mars tables). */
    static final Place CAPE_TOWN = new Place("Cape Town", -33.93, 18.42);

    static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);
    static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT).withZone(ZoneOffset.UTC);
    static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    /**
     * One page.
     *
     * @param centreAt the chart time whose radiant the ordinary page is
     *        centred on, so that pages of one shower share a centre; null
     *        for the whole sky
     */
    record Spec(String name, String code, Instant when, Instant centreAt, String language,
                ChartPalette palette, String what) {
        boolean whole() {
            return centreAt == null;
        }
    }

    static final Instant PERSEIDS_NIGHT = Instant.parse("2027-08-13T00:00:00Z");
    static final Instant PERSEIDS_EARLY = Instant.parse("2027-07-20T00:00:00Z");
    static final Instant PERSEIDS_LATE = Instant.parse("2027-08-20T00:00:00Z");
    static final Instant GEMINIDS_EVENING = Instant.parse("2027-12-14T20:00:00Z");
    static final Instant ETA_NIGHT = Instant.parse("2027-05-06T23:00:00Z");

    static List<Spec> specs() {
        return List.of(
                new Spec("perseids-night-en-white", "PER", PERSEIDS_NIGHT, PERSEIDS_NIGHT, "en",
                        ChartPalette.WHITE_PAPER, "the night of the nominal maximum"),
                new Spec("perseids-night-nb-black", "PER", PERSEIDS_NIGHT, PERSEIDS_NIGHT,
                        "nb-NO", ChartPalette.BLACK_SKY, "the same page, Norwegian, black sky"),
                new Spec("perseids-early-en-white", "PER", PERSEIDS_EARLY, PERSEIDS_NIGHT, "en",
                        ChartPalette.WHITE_PAPER, "the same field 24 days earlier"),
                new Spec("perseids-late-en-white", "PER", PERSEIDS_LATE, PERSEIDS_NIGHT, "en",
                        ChartPalette.WHITE_PAPER, "the same field 7 days later"),
                new Spec("eta-aquariids-below-horizon-en-white", "ETA", ETA_NIGHT, ETA_NIGHT,
                        "en", ChartPalette.WHITE_PAPER, "a radiant below the horizon"),
                new Spec("geminids-evening-en-white", "GEM", GEMINIDS_EVENING, GEMINIDS_EVENING,
                        "en", ChartPalette.WHITE_PAPER, "a radiant with the Moon in the field"),
                new Spec("geminids-whole-sky-en-white", "GEM", GEMINIDS_EVENING, null, "en",
                        ChartPalette.WHITE_PAPER, "the whole sky at the same chart time"));
    }

    /** What a page drew of the radiant, read back from the page. */
    record Drawn(Spec spec, MeteorShowerSession.Facts facts, ReferenceInk.BodyPlacement mark,
                 String spoken, List<String> panel) {
    }

    // Per run, set at the top of main: the evidence contract runs this
    // main twice in one JVM with System.out captured.
    private static PrintStream out;
    private static StringBuilder platform;
    private static WholeSkyComposition.Services services;
    private static MeteorShowers showers;

    private MeteorRadiantStudyMain() {
    }

    private static void p(String s) {
        out.println(s);
    }

    private static void plat(String s) {
        platform.append(s).append('\n');
    }

    private static String f(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    public static void main(String[] args) throws Exception {
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        platform = new StringBuilder();
        services = new WholeSkyComposition.Services();
        showers = new MeteorShowers(MeteorShowerPack.load(),
                instant -> services.solar().solarLongitudeJ2000Degrees(instant));
        Files.createDirectories(OUT);

        p("# Meteor showers at chart time, as built (#716)");
        p("");
        p("Sprint 60, issue #716, road #714; ruled on #715 on 2026-11-11 and at the hands-on"
                + " checkpoint of 2026-11-14 (`docs/decisions/meteor-showers.md`). Written for"
                + " the reader article #717. Every page is the application's own chart with"
                + " its production modules attached, the selected shower drawn by the"
                + " production meteor module; every sentence quoted is the panel's or the"
                + " page's own; every value is read from the bundled pack or computed by the"
                + " production services. No raw source is read. What depends on this"
                + " machine's fonts is in `platform.md`. Regenerate with"
                + " `make meteor-radiant-study`.");
        p("");
        MeteorShowerPack pack = showers.pack();
        p(pack.credit() + ". Edition: " + pack.edition() + ", working list correct as of "
                + pack.correctAsOf() + ". Identities and parent bodies: the IAU Meteor Data"
                + " Center. Positions between the printed rows, instants and everything for a"
                + " place are JUranometria's calculations.");
        p("");

        PlatformEvidence.preface(platform,
                "Meteor showers at chart time, as built (#716) - platform record",
                "Sprint 60, issue #716; written for #717.");
        plat("Whether the radiant's name found room on this machine's fonts, from"
                + " `make meteor-radiant-study`. Held to reproducing here, not pinned to"
                + " bytes.");
        plat("");
        plat("| page | name on the page | placed |");
        plat("|---|---|---|");

        packSection();
        List<Drawn> drawn = new ArrayList<>();
        for (Spec spec : specs()) {
            Drawn d = draw(spec);
            drawn.add(d);
            plat(f("| `%s` | %s | %s |", spec.name(), d.mark() == null ? "-" : d.mark().name(),
                    d.mark() == null ? "no mark" : d.mark().box() == null ? "refused" : "yes"));
            System.err.println(spec.name() + " written");
        }
        pagesSection(drawn);
        notDrawnSection();
        driftSection();
        maximaSection();
        atMaximumSection();
        nightSection();
        moonSection();
        wordsSection(drawn);

        PlatformEvidence.write(platform, OUT.resolve("platform.md").toString());
    }

    // ------------------------------------------------------------------ composing

    /** The production module and session on a composed page, as the application wires them. */
    private record Wired(WholeSkyComposition.Page page, MeteorShowerModule module,
                         MeteorShowerSession session) {
    }

    private static Wired wire(Observer observer, ChartPalette palette, String language,
                              boolean whole) throws Exception {
        WholeSkyComposition.Page page = WholeSkyComposition.paint(new WholeSkyComposition.Ask(
                observer, palette, language, 8.0, true, true, FIELD, whole ? WHOLE_W : W,
                whole ? WHOLE_H : H), services);
        Wired[] wired = new Wired[1];
        SwingUtilities.invokeAndWait(() -> {
            java.util.function.Supplier<Observer> now =
                    () -> page.meridian().attached() ? page.meridian().observer() : null;
            MeteorShowerModule module = page.host().attach(new MeteorShowerModule(now,
                    () -> showers, page.meridian()::subduing));
            MeteorShowerSession session = new MeteorShowerSession(now, () -> showers,
                    services::solar, module);
            wired[0] = new Wired(page, module, session);
        });
        return wired[0];
    }

    private static Drawn draw(Spec spec) throws Exception {
        Wired wired = wire(OSLO.at(spec.when()), spec.palette(), spec.language(), spec.whole());
        MeteorShower shower = showers.shower(spec.code()).orElseThrow();
        Drawn[] result = new Drawn[1];
        BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            wired.session().select(spec.code());
            if (!spec.whole()) {
                wired.page().session().leave();
                wired.page().controller().recenter(
                        showers.radiantAt(shower, spec.centreAt()).at(), FIELD);
            }
            int w = spec.whole() ? WHOLE_W : W;
            int h = spec.whole() ? WHOLE_H : H;
            image[0] = WholeSkyComposition.paintNow(wired.page().chart(), w, h);
            ReferenceInk.BodyPlacement mark = null;
            for (ReferenceInk.BodyPlacement placed : wired.page().chart().renderedBodies()) {
                if (placed.identity().equals(shower.identity())) {
                    mark = placed;
                }
            }
            MeteorShowerSession.Facts facts = wired.session().facts().orElseThrow();
            result[0] = new Drawn(spec, facts, mark,
                    wired.page().chart().getAccessibleContext().getAccessibleDescription(),
                    new MeteorShowerText(wired.page().said()).lines(facts, showers.pack()));
        });
        ImageIO.write(image[0], "png", OUT.resolve(spec.name() + ".png").toFile());
        return result[0];
    }

    /** The session's facts for a shower at a place and time, with no page painted twice. */
    private static MeteorShowerSession.Facts facts(String code, Observer observer)
            throws Exception {
        Wired wired = wire(observer, ChartPalette.WHITE_PAPER, "en", false);
        MeteorShowerSession.Facts[] facts = new MeteorShowerSession.Facts[1];
        SwingUtilities.invokeAndWait(() -> {
            wired.session().select(code);
            facts[0] = wired.session().facts().orElseThrow();
        });
        return facts[0];
    }

    private static List<String> panel(String code, Observer observer, String language)
            throws Exception {
        Wired wired = wire(observer, ChartPalette.WHITE_PAPER, language, false);
        List<String> lines = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            MeteorShowerText text = new MeteorShowerText(wired.page().said());
            if (code == null) {
                lines.addAll(text.unselected(wired.session()));
            } else {
                wired.session().select(code);
                lines.addAll(text.lines(wired.session().facts().orElseThrow(), showers.pack()));
            }
        });
        return lines;
    }

    /** What a page composed as the pages are, centred on the radiant, says aloud. */
    private static String spoken(String code, Observer observer, String language)
            throws Exception {
        Wired wired = wire(observer, ChartPalette.WHITE_PAPER, language, false);
        MeteorShower shower = showers.shower(code).orElseThrow();
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            wired.session().select(code);
            wired.page().session().leave();
            wired.page().controller().recenter(
                    showers.radiantAt(shower, observer.instant()).at(), FIELD);
            WholeSkyComposition.paintNow(wired.page().chart(), W, H);
            said[0] = wired.page().chart().getAccessibleContext().getAccessibleDescription();
        });
        return said[0];
    }

    private static Instant maximumOf(MeteorShower shower, int year) {
        return showers.nominalMaximumAtOrAfter(shower,
                Instant.parse(year + "-01-01T00:00:00Z")).orElseThrow();
    }

    // ------------------------------------------------------------------ sections

    private static void packSection() {
        InterfaceText english = InterfaceText.forLanguage("en");
        InterfaceText norwegian = InterfaceText.forLanguage("nb-NO");
        p("## 1. The nine showers the atlas carries");
        p("");
        p("As the bundled pack holds them. Activity and the maximum are the calendar's dates"
                + " in its own year, 2027; the solar longitudes beside them are what the atlas"
                + " evaluates. ZHR, population index and entry speed are printed as the source"
                + " prints them.");
        p("");
        p("| code | name | Norwegian name | activity | solar longitude | maximum at | ZHR"
                + " | population index | km/s | radiant rows | parent body |");
        p("|---|---|---|---|---|---|---|---|---|---|---|");
        for (MeteorShower s : showers.showers()) {
            p(f("| %s | %s | %s | %s - %s | %.1f - %.1f | %s | %s | %s | %.0f | %d | %s |",
                    s.code(), new MeteorShowerText(english).name(s),
                    new MeteorShowerText(norwegian).name(s), s.activityStart(), s.activityEnd(),
                    s.startLongitude(), s.endLongitude(), s.maximumLongitudeText(), s.zhr(),
                    s.populationIndex(), s.vInfinity(), s.rows().size(),
                    s.parentBody().isEmpty() ? "-" : s.parentBody()));
        }
        p("");
    }

    private static void pagesSection(List<Drawn> drawn) {
        p("## 2. The pages");
        p("");
        p(f("Every page is from %s. An ordinary page is %.0f degrees wide and %d by %d pixels;"
                + " the whole sky is %d by %d. The horizon is drawn and Subdue below horizon is"
                + " on. Pages of one shower share one centre, the radiant at the first page's"
                + " chart time, so that the mark's movement is the radiant's.",
                OSLO.stated(), FIELD, W, H, WHOLE_W, WHOLE_H));
        p("");
        p("| page | what it shows | chart time (UTC) | shower | solar longitude | radiant J2000"
                + " | footing | altitude | Sun altitude | drawn at | drawn as below the horizon |");
        p("|---|---|---|---|---|---|---|---|---|---|---|");
        for (Drawn d : drawn) {
            MeteorShowerSession.Facts facts = d.facts();
            PixelPoint at = d.mark() == null ? null : d.mark().centre();
            p(f("| `%s.png` | %s | %s | %s | %.2f | %.1f %+.1f | %s | %+.1f | %+.1f | %s | %s |",
                    d.spec().name(), d.spec().what(), MINUTE.format(d.spec().when()),
                    d.spec().code(), facts.solarLongitude(), facts.radiant().at().raDegrees(),
                    facts.radiant().at().decDegrees(), footing(facts.radiant()),
                    facts.radiantAltitudeDegrees(), facts.sunAltitudeDegrees(),
                    at == null ? "not drawn" : f("%.0f, %.0f", at.x(), at.y()),
                    d.mark() == null ? "-" : d.mark().belowHorizon() ? "yes" : "no"));
        }
        p("");
        Drawn early = drawn.get(2);
        Drawn night = drawn.get(0);
        Drawn late = drawn.get(3);
        p(f("On the three Perseid pages of one field the mark stands %.0f pixels apart between"
                + " %s and %s, and %.0f between %s and %s: on the sky %.1f and %.1f degrees.",
                apart(early, night), DAY.format(early.spec().when()),
                DAY.format(night.spec().when()), apart(night, late),
                DAY.format(night.spec().when()), DAY.format(late.spec().when()),
                early.facts().radiant().at().separationDegrees(night.facts().radiant().at()),
                night.facts().radiant().at().separationDegrees(late.facts().radiant().at())));
        p("");
    }

    private static double apart(Drawn a, Drawn b) {
        return Math.hypot(a.mark().centre().x() - b.mark().centre().x(),
                a.mark().centre().y() - b.mark().centre().y());
    }

    private static String footing(MeteorShowers.Radiant radiant) {
        return radiant.footing().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static void notDrawnSection() throws Exception {
        p("## 3. Selected, and not drawn");
        p("");
        p("The Perseids stay selected while the chart time leaves their activity. The module"
                + " then offers no radiant, and the page draws none.");
        p("");
        p("| chart time (UTC) | solar longitude | active | radiants the module offers |");
        p("|---|---|---|---|");
        for (Instant when : List.of(PERSEIDS_NIGHT, Instant.parse("2027-08-26T00:00:00Z"),
                GEMINIDS_EVENING)) {
            Wired wired = wire(OSLO.at(when), ChartPalette.WHITE_PAPER, "en", false);
            int[] offered = new int[1];
            SwingUtilities.invokeAndWait(() -> {
                wired.session().select("PER");
                offered[0] = wired.module().contributedGeometry().size();
            });
            MeteorShower per = showers.shower("PER").orElseThrow();
            p(f("| %s | %.2f | %s | %d |", MINUTE.format(when), showers.solarLongitude(when),
                    showers.active(per, when) ? "yes" : "no", offered[0]));
        }
        p("");
    }

    private static void driftSection() {
        p("## 4. The radiant moves");
        p("");
        MeteorShower per = showers.shower("PER").orElseThrow();
        SkyPosition atMaximum = showers.radiantAt(per, maximumOf(per, 2027)).at();
        p("The Perseid radiant at 00:00 UTC through the 2027 activity, as the production"
                + " service places it, and its distance from where it stands at the nominal"
                + " maximum.");
        p("");
        p("| date (00:00 UTC) | solar longitude | right ascension | declination | footing"
                + " | from its place at maximum |");
        p("|---|---|---|---|---|---|");
        for (String day : List.of("2027-07-18", "2027-07-20", "2027-07-25", "2027-07-30",
                "2027-08-04", "2027-08-09", "2027-08-13", "2027-08-17", "2027-08-20",
                "2027-08-24")) {
            Instant when = Instant.parse(day + "T00:00:00Z");
            MeteorShowers.Radiant r = showers.radiantAt(per, when);
            p(f("| %s | %.2f | %.1f | %+.1f | %s | %.1f |", day, showers.solarLongitude(when),
                    r.at().raDegrees(), r.at().decDegrees(), footing(r),
                    r.at().separationDegrees(atMaximum)));
        }
        p("");
        p("Every shower, from the first to the last solar longitude of its activity: how far"
                + " the radiant travels on the sky, and its mean rate. One degree of solar"
                + " longitude is about one day.");
        p("");
        p("| shower | at the start | at the end | travels | degrees of solar longitude"
                + " | per degree |");
        p("|---|---|---|---|---|---|");
        double least = Double.MAX_VALUE;
        double most = 0.0;
        for (MeteorShower s : showers.showers()) {
            SkyPosition first = MeteorShowers.radiantAt(s, s.startLongitude()).at();
            SkyPosition last = MeteorShowers.radiantAt(s, s.endLongitude()).at();
            double span = ((s.endLongitude() - s.startLongitude()) % 360.0 + 360.0) % 360.0;
            double travels = 0.0;
            SkyPosition before = first;
            for (int i = 1; i <= 200; i++) {
                SkyPosition here = MeteorShowers.radiantAt(s,
                        (s.startLongitude() + span * i / 200.0) % 360.0).at();
                travels += before.separationDegrees(here);
                before = here;
            }
            least = Math.min(least, travels);
            most = Math.max(most, travels);
            p(f("| %s | %.1f %+.1f | %.1f %+.1f | %.1f | %.1f | %.2f |", s.code(),
                    first.raDegrees(), first.decDegrees(), last.raDegrees(), last.decDegrees(),
                    travels, span, travels / span));
        }
        p("");
        p(f("Across the nine the radiant travels between %.1f and %.1f degrees during the"
                + " activity.", least, most));
        p("");
    }

    private static void maximaSection() {
        p("## 5. The nominal maximum is an instant, and it is not a date");
        p("");
        p("The instant at which the production Sun reaches each shower's listed solar"
                + " longitude, in three consecutive years, and as the panel words it: no"
                + " finer than the listing warrants.");
        p("");
        p("| shower | listed solar longitude | 2026 (UTC) | 2027 (UTC) | 2028 (UTC)"
                + " | the panel, for 2027 |");
        p("|---|---|---|---|---|---|");
        MeteorShowerText text = new MeteorShowerText(InterfaceText.forLanguage("en"));
        int moved = 0;
        for (MeteorShower s : showers.showers()) {
            Instant a = maximumOf(s, 2026);
            Instant b = maximumOf(s, 2027);
            Instant c = maximumOf(s, 2028);
            if (!DAY.format(a).substring(5).equals(DAY.format(b).substring(5))) {
                moved++;
            }
            p(f("| %s | %s | %s | %s | %s | %s |", s.code(), s.maximumLongitudeText(),
                    MINUTE.format(a), MINUTE.format(b), MINUTE.format(c), text.maximum(s, b)));
        }
        p("");
        p(f("For %d of the nine the nominal maximum falls on a different UTC calendar date in"
                + " 2027 than in 2026.", moved));
        p("");
    }

    private static void atMaximumSection() throws Exception {
        p("## 6. At the nominal maximum, from two places");
        p("");
        p("What the panel's action *Set chart time to nominal maximum* finds in 2027: the"
                + " radiant's altitude and the Sun's at that instant, from " + OSLO.stated()
                + " and from " + CAPE_TOWN.stated() + ".");
        p("");
        p("| shower | nominal maximum 2027 (UTC) | radiant altitude, Oslo | Sun altitude, Oslo"
                + " | radiant altitude, Cape Town | Sun altitude, Cape Town |");
        p("|---|---|---|---|---|---|");
        int daylightOslo = 0;
        int belowOslo = 0;
        for (MeteorShower s : showers.showers()) {
            Instant when = maximumOf(s, 2027);
            MeteorShowerSession.Facts north = facts(s.code(), OSLO.at(when));
            MeteorShowerSession.Facts south = facts(s.code(), CAPE_TOWN.at(when));
            if (north.sunAltitudeDegrees() > 0.0) {
                daylightOslo++;
            }
            if (north.radiantAltitudeDegrees() < 0.0) {
                belowOslo++;
            }
            p(f("| %s | %s | %+.1f | %+.1f | %+.1f | %+.1f |", s.code(), MINUTE.format(when),
                    north.radiantAltitudeDegrees(), north.sunAltitudeDegrees(),
                    south.radiantAltitudeDegrees(), south.sunAltitudeDegrees()));
        }
        p("");
        p(f("From Oslo, %d of the nine nominal maxima of 2027 fall with the Sun above the"
                + " horizon and %d with the radiant below it.", daylightOslo, belowOslo));
        p("");
    }

    private static void nightSection() throws Exception {
        p("## 7. The night around the nominal maximum");
        p("");
        p("The panel's night summary with the chart time at each 2027 nominal maximum: the"
                + " hours the Sun stands more than 6, 12 and 18 degrees below the horizon from"
                + " local mean noon to the next, sampled every five minutes, and the"
                + " radiant's highest altitude while the Sun is more than 12 degrees down and"
                + " the shower is active. The sine of that altitude is geometry, not a rate:"
                + " it is the share of a stream arriving from that height that crosses level"
                + " ground, against the same stream arriving from overhead.");
        p("");
        for (Place place : List.of(OSLO, CAPE_TOWN)) {
            p("From " + place.stated() + ":");
            p("");
            p("| shower | night of (UTC) | Sun below 6 | below 12 | below 18 | Sun's lowest"
                    + " | radiant's highest in the dark | at (UTC) | sine of that altitude |");
            p("|---|---|---|---|---|---|---|---|---|");
            for (MeteorShower s : showers.showers()) {
                MeteorShowerSession.Facts facts = facts(s.code(), place.at(maximumOf(s, 2027)));
                ShowerNight night = facts.night().orElseThrow();
                Optional<Double> highest = night.radiantHighestDegrees();
                p(f("| %s | %s / %s | %s | %s | %s | %+.1f | %s | %s | %s |", s.code(),
                        DAY.format(night.from()), DAY.format(night.to()),
                        hours(night.below6()), hours(night.below12()), hours(night.below18()),
                        night.sunLowestDegrees(),
                        highest.map(h -> f("%+.1f", h)).orElse("no such dark"),
                        highest.isPresent() ? CLOCK.format(night.radiantHighestAt()) : "-",
                        highest.filter(h -> h > 0.0)
                                .map(h -> f("%.2f", Math.sin(Math.toRadians(h)))).orElse("-")));
            }
            p("");
        }
    }

    private static String hours(java.time.Duration duration) {
        return duration.isZero() ? "none" : f("%.1f h", duration.getSeconds() / 3600.0);
    }

    private static void moonSection() throws Exception {
        p("## 8. The Moon at the nominal maximum, year by year");
        p("");
        p("The Moon at the instant of each nominal maximum in three consecutive years, from "
                + OSLO.stated() + ": its illuminated fraction, its altitude and its distance"
                + " from the radiant, from the production Moon. The shower's listed values are"
                + " the same in every year; the Moon is not.");
        p("");
        p("| shower | 2026: lit, altitude, from the radiant | 2027 | 2028 |");
        p("|---|---|---|---|");
        for (MeteorShower s : showers.showers()) {
            StringBuilder row = new StringBuilder("| " + s.code() + " |");
            for (int year = 2026; year <= 2028; year++) {
                MeteorShowerSession.Facts facts = facts(s.code(), OSLO.at(maximumOf(s, year)));
                row.append(f(" %.0f%%, %+.1f, %.0f |",
                        facts.moon().illuminatedFraction() * 100.0,
                        facts.moon().horizontal().altitudeDegrees(),
                        facts.moonToRadiantDegrees()));
            }
            p(row.toString());
        }
        p("");
    }

    private static void wordsSection(List<Drawn> drawn) throws Exception {
        p("## 9. Words");
        p("");
        p("The panel, word for word, for the chart's place and time. Each block is the whole"
                + " of what the panel says.");
        p("");
        block("The Perseids on the night of the nominal maximum (`perseids-night-en-white`)",
                drawn.get(0).panel());
        block("The same, in Norwegian (`perseids-night-nb-black`)", drawn.get(1).panel());
        block("The Perseids a week later, their maximum passed (`perseids-late-en-white`)",
                drawn.get(3).panel());
        block("The Eta Aquariids below the horizon (`eta-aquariids-below-horizon-en-white`)",
                drawn.get(4).panel());
        block("The Geminids with the Moon near the radiant (`geminids-evening-en-white`)",
                drawn.get(5).panel());
        block("The Geminids, in Norwegian, at the same chart time",
                panel("GEM", OSLO.at(GEMINIDS_EVENING), "nb-NO"));
        block("The Perseids selected in December, not active",
                panel("PER", OSLO.at(GEMINIDS_EVENING), "en"));
        block("The same, in Norwegian", panel("PER", OSLO.at(GEMINIDS_EVENING), "nb-NO"));
        block("No shower selected, in December", panel(null, OSLO.at(GEMINIDS_EVENING), "en"));
        block("The same, in Norwegian", panel(null, OSLO.at(GEMINIDS_EVENING), "nb-NO"));
        p("The page, aloud. The spoken description of three pages, whole:");
        p("");
        for (int i : new int[] {0, 1, 4}) {
            p(f("- `%s`: \"%s\"", drawn.get(i).spec().name(), drawn.get(i).spoken()));
        }
        p(f("- the Eta Aquariid page composed in Norwegian (not saved): \"%s\"",
                spoken("ETA", OSLO.at(ETA_NIGHT), "nb-NO")));
        p("");
        InterfaceText english = InterfaceText.forLanguage("en");
        InterfaceText norwegian = InterfaceText.forLanguage("nb-NO");
        p("The interface's own names for what the article mentions:");
        p("");
        p("| key | English | Norwegian |");
        p("|---|---|---|");
        for (String key : List.of("meteor.title", "menu.meteorshowers.label",
                "meteor.selector.label", "meteor.selector.none", "meteor.setMaximum.label",
                "meteor.centre.label", "meteor.explain", "selected.radiant",
                "inspector.occurrence.radiant")) {
            p(f("| `%s` | %s | %s |", key, english.say(key), norwegian.say(key)));
        }
        p("");
    }

    private static void block(String title, List<String> lines) {
        p("**" + title + "**");
        p("");
        for (String line : lines) {
            p(line.isEmpty() ? ">" : "> " + line);
        }
        p("");
    }
}
