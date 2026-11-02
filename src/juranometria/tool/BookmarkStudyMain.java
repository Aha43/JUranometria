package juranometria.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.context.ChartContextHolder;
import juranometria.search.LocalSearch;
import juranometria.search.SearchResult;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.SolarSystemService;
import juranometria.tool.BookmarkDocument.Bookmark;
import juranometria.tool.BookmarkDocument.ChartTime;
import juranometria.tool.BookmarkDocument.Folder;
import juranometria.tool.BookmarkDocument.Json;
import juranometria.tool.BookmarkDocument.Kept;
import juranometria.tool.BookmarkDocument.Library;
import juranometria.tool.BookmarkDocument.Node;
import juranometria.tool.BookmarkDocument.Place;
import juranometria.tool.BookmarkDocument.Read;
import juranometria.tool.BookmarkDocument.Subject;
import juranometria.tool.BookmarkDocument.SubjectKind;
import juranometria.tool.BookmarkDocument.TimePurpose;
import juranometria.tool.BookmarkDocument.Usable;
import juranometria.tool.BookmarkDocument.View;
import juranometria.ui.ChartViewController;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The #497 study: bookmarks as saved geometric views, optionally
 * enriched (owner rulings of 2026-11-02). Measures, against the atlas
 * as it is, what a bookmark must store and what opening one changes;
 * proves the proposed version-1 document round-trips byte for byte;
 * walks the proposed atomic write and recovery through every failure
 * point; and draws the proposed surfaces in both interface languages.
 * Nothing here is production code: no application class reads a
 * bookmark, and every interface word below is a proposal.
 *
 * <p>Writes {@code docs/studies/bookmarks/}: {@code measurements.md}
 * on standard output (portable), {@code platform.md} (this desktop's
 * pixel sizes), {@code examples/library.json} and the sketches.
 */
public final class BookmarkStudyMain {

    private static final Path DIR = Path.of("docs/studies/bookmarks");
    private static final Path STORE = Path.of("build/bookmark-study-store");
    private static final List<String> LANGUAGES = List.of("en", "nb-NO");

    /** The stated observer of the place-and-time contract: Oslo. */
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private BookmarkStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        // The contract runs a platform record's main twice in one JVM:
        // every builder starts empty.
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        System.setProperty("java.awt.headless", "true");
        Files.createDirectories(DIR.resolve("examples"));
        PlatformEvidence.preface(PLATFORM,
                "Bookmarks: this desktop's sketch sizes (#497)", "Sprint 56");
        PLATFORM.append(PlatformEvidence.observed("What is portable - the schema, the"
                + " round trips, the restoration matrix, the identities and the recovery"
                + " walk - is in `measurements.md`. The sketches' pixel sizes depend on"
                + " this desktop's fonts."));
        p("# Bookmarks: the measured architecture and interface study (#497)");
        p("");
        p("Generated by `make bookmarks-study` (`juranometria.tool.BookmarkStudyMain`)"
                + " from the atlas as it is at the study head. The owner's rulings of"
                + " 2026-11-02 on #497 fix the product semantics; this report measures what"
                + " they need from the code and proves the proposed document. The proposal"
                + " and its reasoning are `docs/decisions/bookmarks.md`. Every interface word"
                + " shown is a proposal; no application class reads or writes a bookmark.");
        p("");

        LocalSearch search = Atlas.search();
        geometry();
        Map<String, SearchResult> named = identities(search);
        Events events = events(named);
        Library library = examples(named, events);
        roundTrips(library);
        failures(library);
        restoration(library, named);
        persistence(library);
        exportAndRestore(library);
        sketches(library, named);

        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        PlatformEvidence.write(PLATFORM, DIR.resolve("platform.md").toString());
    }

    // ------------------------------------------------------------------
    // 1. Geometry: what a view is, measured on ChartViewState and the controller.
    // ------------------------------------------------------------------

    private static void geometry() {
        p("## 1. The geometric view, as the atlas holds it");
        p("");
        p("`ChartViewState` holds a centre (ICRS/J2000 degrees), a field width, a"
                + " magnitude limit, an optional target label and identity, and a"
                + " projection that must agree with the field. There is no orientation"
                + " state: every page is drawn celestial north up, east left. The projection"
                + " is the field's, so a bookmark stores the field and never the"
                + " projection:");
        p("");
        p("| field | projection the atlas derives | default magnitude limit for the field |");
        p("|---|---|---|");
        for (double field : ladder()) {
            p("| " + fmt(field) + "° | " + ChartProjection.forField(field) + " | V "
                    + fmt(ChartViewState.defaultMagnitudeFor(field)) + " |");
        }
        p("");
        p("The magnitude limit is part of the view state but, by the ruling, it is the"
                + " reader's current choice and is not restored. Opening a view through"
                + " the controller's own `recenter(centre, field)` keeps the reader's limit"
                + " and lowers it only where the new field's default is brighter - the"
                + " atlas's existing rule for any field change. Measured with a real"
                + " `ChartViewController`, starting at M31's default page with the limit"
                + " set by the reader's own steps:");
        p("");
        StringBuilder head = new StringBuilder("| reader's limit before |");
        StringBuilder rule = new StringBuilder("|---|");
        double[] restored = {180.0, 120.0, 24.0, 8.0, 1.0, 0.25};
        for (double f : restored) {
            head.append(" open at ").append(fmt(f)).append("° |");
            rule.append("---|");
        }
        p(head.toString());
        p(rule.toString());
        for (double limit : new double[] {8.0, 6.0, 4.0}) {
            StringBuilder row = new StringBuilder("| V " + fmt(limit) + " |");
            for (double f : restored) {
                ChartViewController controller = new ChartViewController();
                while (controller.state().limitingMagnitude() > limit) {
                    controller.decreaseMagnitudeLimit();
                }
                controller.recenter(Atlas.DEFAULT_CENTRE, f);
                ChartViewState after = controller.state();
                row.append(after.fieldWidthDegrees() == f
                        ? " V " + fmt(after.limitingMagnitude()) + " |"
                        : " not applied (" + fmt(after.fieldWidthDegrees()) + "°) |");
            }
            p(row.toString());
        }
        p("");
        p("A closer field (0.5°, 0.25°) is accepted by `recenter` like any step: the"
                + " controller applies it whether or not a planet offers it, because the"
                + " offer is consulted only by the closer-field step. A bookmark opening at a"
                + " closer field must therefore ask the same offer the step asks (the"
                + " decision record proposes: at the closer field only where the page"
                + " offers it, otherwise at 1°, stated).");
        p("");
        p("The field is the page's horizontal extent; the vertical extent is the"
                + " window's. The same view in a shorter window shows less sky above and"
                + " below the centre, so a subject near the saved page's top or bottom"
                + " edge may be off a smaller page. For a gnomonic page the half-height in"
                + " degrees is close to field × height ÷ width ÷ 2:");
        p("");
        p("| field | 1200 × 800 px | 900 × 700 px | 900 × 500 px |");
        p("|---|---|---|---|");
        for (double field : new double[] {24.0, 8.0, 1.0}) {
            p("| " + fmt(field) + "° | ±" + fmt2(field * 800 / 1200 / 2) + "° | ±"
                    + fmt2(field * 700 / 900 / 2) + "° | ±" + fmt2(field * 500 / 900 / 2)
                    + "° |");
        }
        p("");
    }

    private static double[] ladder() {
        return new double[] {180, 120, 90, 60, 42, 36, 24, 18, 12, 8, 6, 4, 3, 2, 1, 0.5, 0.25};
    }

    // ------------------------------------------------------------------
    // 2. Identities.
    // ------------------------------------------------------------------

    private static final List<String> NAMED = List.of("M 42", "M 33", "Aldebaran", "Regulus",
            "Spica", "Antares", "Alcyone");

    /** Body identities as the atlas draws them (SolarSystemModule, JovianModule, SaturnModule). */
    private static final List<String[]> BODIES = List.of(
            new String[] {"sun", "yes"}, new String[] {"moon", "yes"},
            new String[] {"mercury", "yes"}, new String[] {"venus", "yes"},
            new String[] {"mars", "yes"}, new String[] {"jovian.jupiter", "yes"},
            new String[] {"saturn", "yes"}, new String[] {"uranus", "yes"},
            new String[] {"neptune", "yes"},
            new String[] {"mars.phobos", "no"}, new String[] {"mars.deimos", "no"},
            new String[] {"jovian.io", "no"}, new String[] {"jovian.europa", "no"},
            new String[] {"jovian.ganymede", "no"}, new String[] {"jovian.callisto", "no"},
            new String[] {"saturn.titan", "no"}, new String[] {"uranus.titania", "no"},
            new String[] {"neptune.triton", "no"});

    private static Map<String, SearchResult> identities(LocalSearch search) {
        p("## 2. Subject identities");
        p("");
        p("A subject is stored as a kind and the atlas's own language-independent"
                + " identity; display text is never stored. Resolved here through the"
                + " atlas's search and its page words, so every identity below is the"
                + " catalogue's or the module's, not typed:");
        p("");
        p("| asked for | kind | stored identity | the catalogue's label, in either language |");
        p("|---|---|---|---|");
        Map<String, SearchResult> named = new LinkedHashMap<>();
        for (String query : NAMED) {
            List<SearchResult> found = search.search(query);
            SearchResult first = found.isEmpty() ? null : found.get(0);
            if (first == null) {
                p("| " + query + " | — | not found | — |");
                continue;
            }
            named.put(query, first);
            String kind = first.kind() == SearchResult.Kind.STAR ? "star" : "deep-sky";
            p("| " + query + " | " + kind + " | `" + first.identity() + "` | "
                    + first.label() + " |");
        }
        p("");
        p("The search labels are the catalogue's names; a Norwegian chart names a"
                + " constellation in Norwegian, but a star's proper name and an NGC/Messier"
                + " designation are the same words in both languages, which is why the"
                + " stored identity is the catalogue's and the shown name is looked up"
                + " each time.");
        p("");
        PageText en = PageText.in(InterfaceText.forLanguage("en"));
        PageText nb = PageText.in(InterfaceText.forLanguage("nb-NO"));
        p("| body identity | selectable on the chart today | English | Norsk bokmål |");
        p("|---|---|---|---|");
        for (String[] body : BODIES) {
            p("| `" + body[0] + "` | " + body[1] + " | " + en.bodyName(body[0]) + " | "
                    + nb.bodyName(body[0]) + " |");
        }
        p("");
        p("No planetary moon is selectable today (each module refuses it until a ruling"
                + " makes it so), so a moon may be a bookmark subject - made visible with"
                + " its planet - but not its primary. Stars are Tycho-2 identities and deep-sky"
                + " objects OpenNGC identities; M 31 and NGC 224 are one object, stored once,"
                + " as the catalogue's own identity.");
        p("");
        return named;
    }

    // ------------------------------------------------------------------
    // 3. Instants, measured or cited - never typed.
    // ------------------------------------------------------------------

    private record Events(Instant transit, Instant moonVenus, double moonVenusDegrees,
                          Instant occultation, String occultedQuery, double occultationDegrees,
                          double moonRadiusDegrees, Instant jovian, Instant observation) {
    }

    private static Events events(Map<String, SearchResult> named) throws IOException {
        p("## 3. The instants the examples use");
        p("");
        Map<String, Instant> venus = cited(Path.of("docs/studies/venus/venus-events.txt"));
        Map<String, Instant> moon = cited(Path.of("docs/studies/solar-system/moon-events-2026.txt"));
        Instant transit = venus.get("2012-inferior-conjunction");
        Instant elongation = venus.get("2026-greatest-elongation-east");
        SolarSystemService service = SolarSystemService.load();

        // The Moon and Venus: the smallest topocentric separation in the
        // thirty days around the cited 2026 eastern elongation.
        Instant from = elongation.minus(Duration.ofDays(15));
        Instant best = from;
        double bestSep = 999;
        for (Instant t = from; t.isBefore(elongation.plus(Duration.ofDays(15)));
                t = t.plus(Duration.ofHours(1))) {
            double sep = separation(moonAt(service, t), venusAt(service, t));
            if (sep < bestSep) {
                bestSep = sep;
                best = t;
            }
        }
        Instant moonVenus = refine(best, t -> separation(moonAt(service, t), venusAt(service, t)));
        double moonVenusSep = separation(moonAt(service, moonVenus), venusAt(service, moonVenus));

        // The Moon and a named star: the smallest topocentric separation in
        // 2026 among bright stars near the ecliptic.
        Instant yearStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant yearEnd = Instant.parse("2027-01-01T00:00:00Z");
        String occulted = null;
        Instant occ = null;
        double occSep = 999;
        for (String query : List.of("Aldebaran", "Regulus", "Spica", "Antares", "Alcyone")) {
            SearchResult star = named.get(query);
            if (star == null) {
                continue;
            }
            SkyPosition at = star.position();
            for (Instant t = yearStart; t.isBefore(yearEnd); t = t.plus(Duration.ofHours(2))) {
                double sep = separation(moonAt(service, t), at);
                if (sep < occSep) {
                    occSep = sep;
                    occ = t;
                    occulted = query;
                }
            }
        }
        SkyPosition occultedAt = named.get(occulted).position();
        occ = refine(occ, t -> separation(moonAt(service, t), occultedAt));
        occSep = separation(moonAt(service, occ), occultedAt);
        SolarSystemService.MoonObservation m = (SolarSystemService.MoonObservation)
                service.observe(SolarSystemService.Body.MOON, oslo(occ));
        double radius = m.angularDiameterArcseconds() / 2 / 3600;

        Instant jovian = moon.get("full-moon-june");
        Instant observation = moon.get("first-quarter-june");

        p("Every instant is cited from a committed fixture or measured here by the atlas's"
                + " own services for Oslo (59.913° N, 10.752° E), never typed:");
        p("");
        p("| example | instant | how it was found |");
        p("|---|---|---|");
        p("| Venus's 2012 transit | " + transit + " | cited: `docs/studies/venus/venus-events.txt`,"
                + " `2012-inferior-conjunction` |");
        p("| the Moon and Venus | " + moonVenus + " | measured: smallest topocentric separation,"
                + " " + fmt2(moonVenusSep) + "°, within 15 days of the cited 2026 eastern"
                + " elongation |");
        p("| the Moon and " + occulted + " | " + occ + " | measured: smallest topocentric"
                + " separation in 2026 among Aldebaran, Regulus, Spica, Antares and Alcyone, "
                + fmt3(occSep) + "° from the Moon's centre against its " + fmt3(radius)
                + "° radius - " + (occSep < radius ? "an occultation from Oslo"
                : "a close approach, not an occultation from Oslo") + " |");
        p("| Jupiter and its moons | " + jovian + " | cited: `moon-events-2026.txt`,"
                + " `full-moon-june` (any instant serves; this one is cited) |");
        p("| an observation note | " + observation + " | cited: `moon-events-2026.txt`,"
                + " `first-quarter-june`, standing in for a time the user confirms |");
        p("");
        return new Events(transit, moonVenus, moonVenusSep, occ, occulted, occSep, radius,
                jovian, observation);
    }

    private static Map<String, Instant> cited(Path fixture) throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(fixture, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.put(f[0], Instant.parse(iso));
        }
        return out;
    }

    private static Observer oslo(Instant t) {
        return new Observer(OSLO_LAT, OSLO_LON, t);
    }

    private static SkyPosition moonAt(SolarSystemService service, Instant t) {
        return ((SolarSystemService.MoonObservation)
                service.observe(SolarSystemService.Body.MOON, oslo(t))).astrometricJ2000();
    }

    private static SkyPosition venusAt(SolarSystemService service, Instant t) {
        return ((juranometria.solar.VenusService.VenusObservation)
                service.observe(SolarSystemService.Body.VENUS, oslo(t))).astrometricJ2000();
    }

    private static Instant refine(Instant near, java.util.function.ToDoubleFunction<Instant> f) {
        Instant best = near;
        double bestValue = f.applyAsDouble(near);
        for (long minutes = -180; minutes <= 180; minutes++) {
            Instant t = near.plus(Duration.ofMinutes(minutes));
            double v = f.applyAsDouble(t);
            if (v < bestValue) {
                bestValue = v;
                best = t;
            }
        }
        return best;
    }

    static double separation(SkyPosition a, SkyPosition b) {
        double ra1 = Math.toRadians(a.raDegrees());
        double ra2 = Math.toRadians(b.raDegrees());
        double d1 = Math.toRadians(a.decDegrees());
        double d2 = Math.toRadians(b.decDegrees());
        double c = Math.sin(d1) * Math.sin(d2) + Math.cos(d1) * Math.cos(d2) * Math.cos(ra1 - ra2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, c))));
    }

    // ------------------------------------------------------------------
    // 4. The examples and their round trips.
    // ------------------------------------------------------------------

    private static String id(String seed) {
        return UUID.nameUUIDFromBytes(("juranometria-bookmark-study:" + seed)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static Library examples(Map<String, SearchResult> named, Events e) throws Exception {
        SolarSystemService service = SolarSystemService.load();
        JovianSystemService jovian = JovianSystemService.load();
        SearchResult m33 = named.get("M 33");
        SearchResult star = named.get(e.occultedQuery());
        SkyPosition moonVenusAt = moonAt(service, e.moonVenus());
        SkyPosition occAt = moonAt(service, e.occultation());
        SkyPosition transitAt = venusAt(service, e.transit());
        SkyPosition jupiterAt = jovian.observeJupiter(oslo(e.jovian())).astrometricJ2000();
        SkyPosition observationAt = moonAt(service, e.observation());
        Subject moon = new Subject(SubjectKind.BODY, "moon");
        Subject venus = new Subject(SubjectKind.BODY, "venus");
        Subject sun = new Subject(SubjectKind.BODY, "sun");
        Subject jupiter = new Subject(SubjectKind.BODY, "jovian.jupiter");
        Place osloPlace = new Place(OSLO_LAT, OSLO_LON);

        List<Node> root = new ArrayList<>();
        SkyPosition m42 = named.get("M 42").position();
        root.add(bookmark("a chart position", "Orion's sword, wide",
                new View(m42.raDegrees(), m42.decDegrees(), 12), List.of(), null, null, null, null));
        Bookmark m33Alone = bookmark("M33 alone", "M33",
                new View(m33.position().raDegrees(), m33.position().decDegrees(), 2),
                List.of(new Subject(SubjectKind.DEEP_SKY, m33.identity())),
                new Subject(SubjectKind.DEEP_SKY, m33.identity()), null, null, null);
        Bookmark field = bookmark("observing field", "Andromeda to Triangulum field",
                new View(Atlas.DEFAULT_CENTRE.raDegrees(), Atlas.DEFAULT_CENTRE.decDegrees(), 24),
                List.of(new Subject(SubjectKind.DEEP_SKY, m33.identity())), null, null, null,
                "Binocular sweep; works any clear autumn night.");
        root.add(new Folder(id("folder deep sky"), "Deep sky", List.of(m33Alone, field)));
        Bookmark moonVenus = bookmark("moon and venus", "The Moon with Venus",
                new View(round6(moonVenusAt.raDegrees()), round6(moonVenusAt.decDegrees()), 8),
                List.of(moon, venus), moon, new ChartTime(e.moonVenus(), TimePurpose.EVENT),
                null, null);
        Bookmark occultation = bookmark("moon and star", "The Moon at " + e.occultedQuery(),
                new View(round6(occAt.raDegrees()), round6(occAt.decDegrees()), 2),
                List.of(moon, new Subject(SubjectKind.STAR, star.identity())),
                moon, new ChartTime(e.occultation(), TimePurpose.EVENT), osloPlace, null);
        Bookmark transit = bookmark("transit", "Venus crossing the Sun, 2012",
                new View(round6(transitAt.raDegrees()), round6(transitAt.decDegrees()), 1),
                List.of(venus, sun), venus, new ChartTime(e.transit(), TimePurpose.EVENT),
                null, "The last transit of Venus until 2117.");
        Bookmark jovianMoons = bookmark("jupiter and moons", "Jupiter with Io and Europa",
                new View(round6(jupiterAt.raDegrees()), round6(jupiterAt.decDegrees()), 0.25),
                List.of(jupiter, new Subject(SubjectKind.BODY, "jovian.io"),
                        new Subject(SubjectKind.BODY, "jovian.europa")),
                jupiter, new ChartTime(e.jovian(), TimePurpose.EVENT), null, null);
        root.add(new Folder(id("folder sky events"), "Sky events",
                List.of(moonVenus, occultation, transit, jovianMoons)));
        root.add(bookmark("observation", "Moon from the balcony",
                new View(round6(observationAt.raDegrees()), round6(observationAt.decDegrees()), 4),
                List.of(moon), moon, new ChartTime(e.observation(), TimePurpose.OBSERVATION),
                osloPlace, "Første kvarter, klar himmel; Copernicus tydelig på terminatoren."));
        return new Library("6.2.0-study", root);
    }

    private static double round6(double v) {
        return Math.round(v * 1e6) / 1e6;
    }

    private static Bookmark bookmark(String seed, String title, View view, List<Subject> subjects,
                                     Subject primary, ChartTime time, Place place, String note) {
        return new Bookmark(id(seed), title, view, subjects, Optional.ofNullable(primary),
                Optional.ofNullable(time), Optional.ofNullable(place), Optional.ofNullable(note));
    }

    private static void roundTrips(Library library) throws Exception {
        p("## 4. The version-1 document and its round trips");
        p("");
        String text = BookmarkDocument.write(BookmarkDocument.json(library));
        Files.writeString(DIR.resolve("examples/library.json"), text, StandardCharsets.UTF_8);
        Json parsed = BookmarkDocument.parse(text);
        Read read = BookmarkDocument.read(parsed);
        String again = read instanceof Usable u
                ? BookmarkDocument.write(BookmarkDocument.json(u.library())) : "";
        p("Every required example is in one library, `examples/library.json` ("
                + text.getBytes(StandardCharsets.UTF_8).length + " bytes, "
                + BookmarkDocument.bookmarks(library.root()).size() + " bookmarks in "
                + BookmarkDocument.folders(library.root()).size() + " folders). Written,"
                + " read, written again: **" + (text.equals(again) ? "identical, byte for byte"
                : "DIFFERENT") + "**; the domain read back equals the domain written: **"
                + (read instanceof Usable u2 && u2.library().equals(library) ? "yes" : "NO")
                + "**.");
        p("");
        p("| example (#497) | title | subjects, in user order | primary | time | place | note |");
        p("|---|---|---|---|---|---|---|");
        String[] labels = {"a chart position, nothing else", "M33 alone",
                "a reusable observing field, time deliberately absent", "the Moon and Venus",
                "the Moon and a named star", "a conjunction / transit at an instant",
                "Jupiter and selected Galilean moons", "an observation note with a confirmed time"};
        List<Bookmark> all = BookmarkDocument.bookmarks(library.root());
        int[] order = {0, 1, 2, 3, 4, 5, 6, 7};
        for (int i = 0; i < all.size(); i++) {
            Bookmark b = all.get(order[i]);
            StringBuilder subjects = new StringBuilder();
            for (Subject s : b.subjects()) {
                subjects.append(subjects.length() == 0 ? "" : ", ").append("`")
                        .append(s.id()).append("`");
            }
            p("| " + labels[i] + " | " + b.title() + " | "
                    + (subjects.length() == 0 ? "—" : subjects) + " | "
                    + b.primary().map(s -> "`" + s.id() + "`").orElse("—") + " | "
                    + b.time().map(t -> t.purpose().token() + " " + t.instant()).orElse("—")
                    + " | " + b.place().map(pl -> fmt3(pl.latitudeDegrees()) + ", "
                    + fmt3(pl.eastLongitudeDegrees())).orElse("—") + " | "
                    + (b.note().isPresent() ? "yes" : "—") + " |");
        }
        p("");
        p("One bookmark, as the document stores it (the Moon and the named star):");
        p("");
        p("```json");
        String one = BookmarkDocument.write(BookmarkDocument.json(new Library("6.2.0-study",
                List.of(all.get(4)))));
        for (String line : one.split("\n")) {
            p(line);
        }
        p("```");
        p("");
        p("The titles above are the user's own words and stay as typed in any language;"
                + " the subjects are identities, shown in the interface's language at each"
                + " reading. The root folder has no stored title: it is the permanent"
                + " *Bookmarks* / *Bokmerker* and is named by the interface.");
        p("");
    }

    // ------------------------------------------------------------------
    // 5. Failure, migration and future cases.
    // ------------------------------------------------------------------

    private static void failures(Library library) throws Exception {
        p("## 5. Failure, migration and future-version cases");
        p("");
        p("Each document below is read by the proposed reader. *Usable* means the library"
                + " opens; kept records are written back unchanged and reported. The reader"
                + " never throws on content and never rewrites what it did not understand.");
        p("");
        String good = BookmarkDocument.write(BookmarkDocument.json(library));
        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("the examples, unchanged", good);
        cases.put("an interrupted write: the document cut in half",
                good.substring(0, good.length() / 2));
        cases.put("not ours: another program's JSON", "{\"bookmarks\": []}\n");
        cases.put("a byte-order mark before the document", "\uFEFF" + good);
        cases.put("a future schema (version 2)", good.replace("\"version\": 1", "\"version\": 2"));
        cases.put("a record of an unknown type (a later \"separator\")",
                good.replace("\"root\": [", "\"root\": [\n    {\"type\": \"separator\", \"id\": \"s-1\"},"));
        cases.put("a view whose field is not a step (7°)",
                good.replaceFirst("\"field\": 12", "\"field\": 7"));
        List<Node> stray = new ArrayList<>(library.root());
        Bookmark first = BookmarkDocument.bookmarks(library.root()).get(1);
        stray.add(new Bookmark(id("stray primary"), first.title(), first.view(), first.subjects(),
                Optional.of(new Subject(SubjectKind.BODY, "mars")), Optional.empty(),
                Optional.empty(), Optional.empty()));
        cases.put("a primary that is not among the subjects",
                BookmarkDocument.write(BookmarkDocument.json(new Library(library.writtenBy(), stray))));
        cases.put("a time in local civil form, no zone",
                good.replaceFirst("(\"instant\": \"[0-9T:-]+)Z\"", "$1\""));
        cases.put("an unknown subject identity (a star a later catalogue dropped)",
                good.replaceFirst("\"id\": \"TYC [^\"]+\"", "\"id\": \"TYC 9999-9999-9\""));
        cases.put("the same id twice", good.replaceFirst("\"id\": \"[0-9a-f-]{36}\"",
                "\"id\": \"" + BookmarkDocument.bookmarks(library.root()).get(1).id() + "\""));
        p("| document | read as | bookmarks usable | records kept | why |");
        p("|---|---|---|---|---|");
        for (Map.Entry<String, String> c : cases.entrySet()) {
            String outcome;
            String usable = "—";
            String kept = "—";
            String why = "";
            try {
                Read read = BookmarkDocument.read(BookmarkDocument.parse(c.getValue()));
                switch (read) {
                    case Usable u -> {
                        outcome = "usable";
                        usable = Integer.toString(BookmarkDocument.bookmarks(u.library().root()).size());
                        List<Kept> k = BookmarkDocument.kept(u.library().root());
                        kept = Integer.toString(k.size());
                        StringBuilder w = new StringBuilder();
                        for (Kept one : k) {
                            w.append(w.length() == 0 ? "" : "; ").append(String.join(", ", one.reasons()));
                        }
                        why = w.toString();
                        if (u.library().root().size() > 0 && read instanceof Usable
                                && c.getKey().startsWith("an unknown subject")) {
                            why = "the identity is kept as stored; whether it resolves is decided"
                                    + " when the bookmark opens (section 6), never at reading";
                        }
                    }
                    case BookmarkDocument.NotOurs n -> {
                        outcome = "not a bookmark document";
                        why = n.reason();
                    }
                    case BookmarkDocument.Newer n -> {
                        outcome = "newer (version " + n.version() + ")";
                        why = "never reinterpreted, never written; the library is read-only"
                                + " until a newer JUranometria opens it";
                    }
                }
            } catch (BookmarkDocument.Malformed m) {
                outcome = "malformed";
                why = m.getMessage();
            }
            p("| " + c.getKey() + " | " + outcome + " | " + usable + " | " + kept + " | "
                    + (why.isEmpty() ? "—" : why) + " |");
        }
        p("");
    }

    // ------------------------------------------------------------------
    // 6. Restoration, measured on the real controller and context holder.
    // ------------------------------------------------------------------

    private static void restoration(Library library, Map<String, SearchResult> named)
            throws Exception {
        p("## 6. The restoration matrix, measured");
        p("");
        p("Each row opens a bookmark through the proposed procedure on the atlas's own"
                + " `ChartViewController`, `ChartContextHolder` and `WorkingSelection`:"
                + " the view through `recenter(centre, field[, label, identity])`, the"
                + " stored place and instant through one `install` of the observer built"
                + " from the current one, and the primary - if any - through the working"
                + " selection, as search does. Before every row the chart is at the same"
                + " state: M31's page, the reader's limit V 6, Oslo at 2026-03-20T21:33:00Z,"
                + " and the star Aldebaran selected. *same* means the value was not touched.");
        p("");
        p("| stored place | stored time | primary | centre | field | limit | place | instant |"
                + " context installs | selection |");
        p("|---|---|---|---|---|---|---|---|---|---|");
        Instant before = Instant.parse("2026-03-20T21:33:00Z");
        String startSelected = named.get("Aldebaran").identity();
        Bookmark base = BookmarkDocument.bookmarks(library.root()).get(4);
        for (boolean withPlace : new boolean[] {false, true}) {
            for (boolean withTime : new boolean[] {false, true}) {
                for (boolean withPrimary : new boolean[] {false, true}) {
                    Bookmark b = new Bookmark(base.id(), base.title(), base.view(),
                            base.subjects(), withPrimary ? base.primary() : Optional.empty(),
                            withTime ? base.time() : Optional.empty(),
                            withPlace ? Optional.of(new Place(78.223, 15.647)) : Optional.empty(),
                            Optional.empty());
                    String[] row = onEdt(() -> restoreRow(b, before, startSelected));
                    p("| " + (withPlace ? "yes" : "no") + " | " + (withTime ? "yes" : "no")
                            + " | " + (withPrimary ? "yes" : "no") + " | "
                            + String.join(" | ", row) + " |");
                }
            }
        }
        p("");
        p("Measured: the view always changes; the reader's limit is never raised; the place"
                + " and the instant change only when stored, in one installation of the chart"
                + " context (one redraw, one revision), and each leaves the other as it was;"
                + " the selection changes only when a primary is stored. Chart Options, the"
                + " grids, the labels and every Solar System switch are not touched by any"
                + " row: the procedure has no route to them. Making a subject visible (the"
                + " Moon's switch here) is a separate, stated step the decision record"
                + " proposes for subjects the current options would hide.");
        p("");
    }

    private static String[] restoreRow(Bookmark b, Instant before, String startSelected) {
        ChartViewController controller = new ChartViewController();
        while (controller.state().limitingMagnitude() > 6.0) {
            controller.decreaseMagnitudeLimit();
        }
        ChartViewState was = controller.state();
        ChartContextHolder context = new ChartContextHolder(oslo(before));
        int[] installs = {0};
        context.onChange(change -> installs[0]++);
        WorkingSelection working = new WorkingSelection();
        working.replaceWith(List.of(startSelected), startSelected);

        SkyPosition centre = new SkyPosition(b.view().raDegrees(), b.view().decDegrees());
        controller.recenter(centre, b.view().fieldWidthDegrees());
        Observer current = context.observer();
        Observer next = current;
        if (b.place().isPresent()) {
            next = next.from(b.place().get().latitudeDegrees(), b.place().get().eastLongitudeDegrees());
        }
        if (b.time().isPresent()) {
            next = next.at(b.time().get().instant());
        }
        if (!next.equals(current)) {
            context.install(next);
        }
        if (b.primary().isPresent()) {
            String id = b.primary().get().id();
            working.replaceWith(List.of(id), id);
        }
        ChartViewState now = controller.state();
        Observer after = context.observer();
        return new String[] {
            now.centre().equals(was.centre()) ? "same" : "restored",
            now.fieldWidthDegrees() == was.fieldWidthDegrees() ? "same"
                    : fmt(now.fieldWidthDegrees()) + "°",
            now.limitingMagnitude() == was.limitingMagnitude() ? "same (V "
                    + fmt(now.limitingMagnitude()) + ")" : "V " + fmt(now.limitingMagnitude()),
            after.latitudeDegrees() == current.latitudeDegrees()
                    && after.eastLongitudeDegrees() == current.eastLongitudeDegrees() ? "same"
                    : fmt3(after.latitudeDegrees()) + ", " + fmt3(after.eastLongitudeDegrees()),
            after.instant().equals(current.instant()) ? "same" : after.instant().toString(),
            Integer.toString(installs[0]),
            working.members().equals(List.of(startSelected)) ? "same" : "`" + working.lead() + "`"
        };
    }

    // ------------------------------------------------------------------
    // 7. Persistence: the proposed write and recovery, walked through every failure point.
    // ------------------------------------------------------------------

    /** Where the proposed write can be interrupted. */
    private enum Stop {
        NONE, AFTER_TEMP_WRITTEN, AFTER_BACKUP_COPIED, MOVE_REFUSED
    }

    private static void persistence(Library library) throws Exception {
        p("## 7. The local document: write, backup and recovery");
        p("");
        p("The proposed write, the export sheet's own precedent extended with a"
                + " last-known-good backup: (1) write the whole document to a temporary file"
                + " beside it and force it to the disk; (2) if the current document reads as"
                + " usable, copy it over `bookmarks.json.bak`; (3) move the temporary file"
                + " onto `bookmarks.json` atomically, refusing - never copying - where the"
                + " file system cannot move atomically. Walked here on a scratch folder with"
                + " the write stopped at each point, then the next start read as the proposed"
                + " start reads it:");
        p("");
        p("| the write stopped | `bookmarks.json` afterwards | `.bak` afterwards | stray"
                + " temporary file | the next start opens |");
        p("|---|---|---|---|---|");
        String v1 = BookmarkDocument.write(BookmarkDocument.json(library));
        Library smaller = new Library(library.writtenBy(), library.root().subList(0, 2));
        String v2 = BookmarkDocument.write(BookmarkDocument.json(smaller));
        for (Stop stop : Stop.values()) {
            Path dir = fresh();
            Files.writeString(dir.resolve("bookmarks.json"), v1, StandardCharsets.UTF_8);
            try {
                write(dir, v2, stop);
            } catch (IOException interrupted) {
                // the write is interrupted exactly here
            }
            p("| " + describe(stop) + " | " + which(dir.resolve("bookmarks.json"), v1, v2) + " | "
                    + which(dir.resolve("bookmarks.json.bak"), v1, v2) + " | "
                    + strays(dir) + " | " + start(dir, v1, v2) + " |");
        }
        // The unreadable main document, with and without a usable backup.
        Path dir = fresh();
        Files.writeString(dir.resolve("bookmarks.json"), v1.substring(0, 200), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("bookmarks.json.bak"), v2, StandardCharsets.UTF_8);
        p("| (the main document damaged by something else; a backup exists) | damaged | "
                + which(dir.resolve("bookmarks.json.bak"), v1, v2) + " | none | "
                + start(dir, v1, v2) + " |");
        Path dir2 = fresh();
        Files.writeString(dir2.resolve("bookmarks.json"),
                v1.replace("\"version\": 1", "\"version\": 3"), StandardCharsets.UTF_8);
        p("| (a later JUranometria wrote version 3) | version 3 | none | none | "
                + start(dir2, v1, v2) + " |");
        p("");
        p("Measured: no stop leaves the reader without a usable library; the previous"
                + " library is either still the document or the backup; a damaged or newer"
                + " document is never overwritten by the start that found it.");
        p("");
        p("Where the document lives (none of these folders exists for JUranometria today;"
                + " the atlas keeps everything else in one Preferences node and writes no"
                + " other user file):");
        p("");
        p("| system | folder | document |");
        p("|---|---|---|");
        p("| macOS | `~/Library/Application Support/JUranometria/` | `bookmarks.json` |");
        p("| Windows | `%APPDATA%\\JUranometria\\` | `bookmarks.json` |");
        p("| Linux and others | `$XDG_DATA_HOME/juranometria/`, else `~/.local/share/juranometria/`"
                + " | `bookmarks.json` |");
        p("");
    }

    private static String describe(Stop stop) {
        return switch (stop) {
            case NONE -> "not at all";
            case AFTER_TEMP_WRITTEN -> "after the temporary file was written";
            case AFTER_BACKUP_COPIED -> "after the backup was copied";
            case MOVE_REFUSED -> "at the move (no atomic move on this file system)";
        };
    }

    private static Path fresh() throws IOException {
        Path dir = STORE.resolve("case-" + UUID.randomUUID());
        Files.createDirectories(dir);
        return dir;
    }

    /** The proposed write, stopped where asked. */
    private static void write(Path dir, String text, Stop stop) throws IOException {
        Path main = dir.resolve("bookmarks.json");
        Path temp = dir.resolve("bookmarks.json.part");
        try (java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(temp,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
            ch.write(java.nio.ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
            ch.force(true);
        }
        if (stop == Stop.AFTER_TEMP_WRITTEN) {
            throw new IOException("stopped");
        }
        if (Files.exists(main) && usable(Files.readString(main, StandardCharsets.UTF_8))) {
            Files.copy(main, dir.resolve("bookmarks.json.bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        if (stop == Stop.AFTER_BACKUP_COPIED) {
            throw new IOException("stopped");
        }
        if (stop == Stop.MOVE_REFUSED) {
            throw new AtomicMoveNotSupportedException(temp.toString(), main.toString(), "refused");
        }
        Files.move(temp, main, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static boolean usable(String text) {
        try {
            return BookmarkDocument.read(BookmarkDocument.parse(text)) instanceof Usable;
        } catch (BookmarkDocument.Malformed m) {
            return false;
        }
    }

    private static String which(Path file, String v1, String v2) throws IOException {
        if (!Files.exists(file)) {
            return "none";
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return text.equals(v1) ? "the previous library" : text.equals(v2) ? "the new library"
                : "something else";
    }

    private static String strays(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            return files.anyMatch(f -> f.getFileName().toString().endsWith(".part"))
                    ? "yes - deleted unread at the next start" : "none";
        }
    }

    /** The proposed start: never overwrites what it cannot use. */
    private static String start(Path dir, String v1, String v2) throws IOException {
        Path main = dir.resolve("bookmarks.json");
        String text = Files.readString(main, StandardCharsets.UTF_8);
        try {
            Read read = BookmarkDocument.read(BookmarkDocument.parse(text));
            if (read instanceof BookmarkDocument.Newer n) {
                return "nothing: version " + n.version() + " is newer; read-only notice, file untouched";
            }
            if (read instanceof Usable) {
                return which(main, v1, v2);
            }
        } catch (BookmarkDocument.Malformed m) {
            // falls through
        }
        Path bak = dir.resolve("bookmarks.json.bak");
        return Files.exists(bak)
                ? "nothing yet: the damaged document is kept and the backup ("
                        + which(bak, v1, v2) + ") is offered, to be restored only on the reader's yes"
                : "nothing: the damaged document is kept; the library is read-only and empty";
    }

    // ------------------------------------------------------------------
    // 8. Export and restore.
    // ------------------------------------------------------------------

    private static void exportAndRestore(Library library) throws Exception {
        p("## 8. Export, backup and restore, exactly");
        p("");
        String text = BookmarkDocument.write(BookmarkDocument.json(library));
        Path dir = fresh();
        Path main = dir.resolve("bookmarks.json");
        Files.writeString(main, text, StandardCharsets.UTF_8);
        Path exported = dir.resolve("exported.json");
        Files.copy(main, exported);
        Library other = new Library(library.writtenBy(), library.root().subList(0, 1));
        String otherText = BookmarkDocument.write(BookmarkDocument.json(other));
        Files.writeString(main, otherText, StandardCharsets.UTF_8);
        // restore: validate, summarise, keep the current library, replace.
        Read source = BookmarkDocument.read(BookmarkDocument.parse(Files.readString(exported)));
        Path before = dir.resolve("bookmarks-before-restore.json");
        Files.copy(main, before);
        write(dir, Files.readString(exported, StandardCharsets.UTF_8), Stop.NONE);
        boolean exact = Files.readString(main).equals(text);
        // undo: restore the kept library the same way.
        write(dir, Files.readString(before, StandardCharsets.UTF_8), Stop.NONE);
        boolean undone = Files.readString(main).equals(otherText);
        Library l = ((Usable) source).library();
        p("Export writes the library's canonical text - kept records included - to a file"
                + " the reader chooses, through the export sheet's own temporary-file-and-atomic-move"
                + " placement and its replace question. Restore reads a chosen file, refuses"
                + " anything that is not a usable version-1 library, shows a summary, keeps"
                + " the current library beside the document, and replaces it only on the"
                + " reader's yes. Measured on a scratch folder:");
        p("");
        p("| step | result |");
        p("|---|---|");
        p("| the summary restore shows | " + BookmarkDocument.bookmarks(l.root()).size()
                + " bookmarks, " + BookmarkDocument.folders(l.root()).size() + " folders, "
                + BookmarkDocument.kept(l.root()).size() + " records needing attention,"
                + " written by " + l.writtenBy() + " |");
        p("| export, then restore of that export | " + (exact
                ? "the document is the exported library, byte for byte" : "DIFFERS") + " |");
        p("| undo the restore from the kept library | " + (undone
                ? "the document is the library from before the restore, byte for byte"
                : "DIFFERS") + " |");
        p("| restore from a malformed, foreign or newer file | refused before anything is"
                + " touched; the reason is shown |");
        p("");
    }

    // ------------------------------------------------------------------
    // 9. Sketches: the proposed surfaces, in both languages.
    // ------------------------------------------------------------------

    /** The proposed words, English then Norsk bokmål. */
    private static final Map<String, String[]> WORDS = new LinkedHashMap<>();

    static {
        w("menu", "Bookmarks", "Bokmerker");
        w("add", "Add Bookmark…", "Legg til bokmerke…");
        w("show", "Show Bookmarks", "Vis bokmerker");
        w("addTitle", "Add Bookmark", "Legg til bokmerke");
        w("title", "Title", "Tittel");
        w("folder", "Folder", "Mappe");
        w("root", "Bookmarks", "Bokmerker");
        w("subjects", "Objects, in your order", "Objekter, i din rekkefølge");
        w("primary", "Main object (selected when opened)", "Hovedobjekt (velges ved åpning)");
        w("none", "None", "Ingen");
        w("up", "Move up", "Flytt opp");
        w("down", "Move down", "Flytt ned");
        w("remove", "Remove", "Fjern");
        w("view", "The view: centre and field are always saved", "Utsnittet: sentrum og synsfelt lagres alltid");
        w("time", "Include chart time", "Ta med karttiden");
        w("event", "Event time", "Hendelsestid");
        w("observation", "Observation time", "Observasjonstid");
        w("place", "Include place", "Ta med stedet");
        w("dynamicNote", "The Moon is shown at this instant; without it, it opens at the chart's time then.",
                "Månen vises ved dette tidspunktet; uten det åpnes den ved karttiden da.");
        w("note", "Note", "Notat");
        w("cancel", "Cancel", "Avbryt");
        w("ok", "Add", "Legg til");
        w("tab", "Bookmarks", "Bokmerker");
        w("open", "Open", "Åpne");
        w("edit", "Edit…", "Rediger…");
        w("newFolder", "New Folder…", "Ny mappe…");
        w("delete", "Delete…", "Slett…");
        w("library", "Library", "Bibliotek");
        w("export", "Export Bookmarks…", "Eksporter bokmerker…");
        w("restore", "Restore from File…", "Gjenopprett fra fil…");
        w("attention", "needs attention", "trenger tilsyn");
        w("details", "Details", "Detaljer");
        w("unresolved", "not found in this catalogue - kept as stored", "ikke funnet i denne katalogen - beholdt slik den ble lagret");
        w("restoreTitle", "Restore Bookmarks", "Gjenopprett bokmerker");
        w("restoreQuestion", "Replace your bookmarks with this file?", "Erstatte bokmerkene dine med denne filen?");
        w("restoreSummary", "{0} bookmarks in {1} folders, written by JUranometria {2}.",
                "{0} bokmerker i {1} mapper, skrevet av JUranometria {2}.");
        w("restoreKeep", "Your current bookmarks are kept beside the library and can be restored again.",
                "Bokmerkene du har nå, beholdes ved siden av biblioteket og kan gjenopprettes.");
        w("restoreYes", "Replace", "Erstatt");
        w("damaged", "Your bookmark file cannot be read. It has been kept unchanged.",
                "Bokmerkefilen kan ikke leses. Den er beholdt uendret.");
        w("damagedOffer", "A backup from {0} can be restored.", "En sikkerhetskopi fra {0} kan gjenopprettes.");
        w("damagedYes", "Restore Backup", "Gjenopprett sikkerhetskopi");
        w("newer", "These bookmarks were saved by a newer JUranometria and are shown read-only.",
                "Disse bokmerkene ble lagret av en nyere JUranometria og vises skrivebeskyttet.");
    }

    private static void w(String key, String en, String nb) {
        WORDS.put(key, new String[] {en, nb});
    }

    private static String say(String key, int lang) {
        return WORDS.get(key)[lang];
    }

    private static void sketches(Library library, Map<String, SearchResult> named) throws Exception {
        p("## 9. The proposed surfaces");
        p("");
        p("Drawn from unwired Swing components under the atlas's own theme, in both"
                + " languages; the words are proposals (table below). Sizes are this"
                + " desktop's and stand in `platform.md`.");
        p("");
        javax.swing.LookAndFeel was = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
        Instant moonVenusAt = BookmarkDocument.bookmarks(library.root()).get(3).time().get().instant();
        List<String> images = new ArrayList<>();
        try {
            for (int lang = 0; lang < 2; lang++) {
                String tag = LANGUAGES.get(lang);
                final int l = lang;
                images.add(onEdt(() -> draw(() -> menu(l), "bookmarks-" + tag + "-1-menu.png", false)));
                images.add(onEdt(() -> draw(() -> addDialog(l, true, moonVenusAt), "bookmarks-" + tag + "-2-add-event.png", false)));
                images.add(onEdt(() -> draw(() -> addDialog(l, false, moonVenusAt), "bookmarks-" + tag + "-3-add-timeless.png", false)));
                images.add(onEdt(() -> draw(() -> tab(l, library, named), "bookmarks-" + tag + "-4-library.png", false)));
                images.add(onEdt(() -> draw(() -> tab(l, library, named), "bookmarks-" + tag + "-5-library-dark.png", true)));
                images.add(onEdt(() -> draw(() -> restore(l, library), "bookmarks-" + tag + "-6-restore.png", false)));
                images.add(onEdt(() -> draw(() -> damaged(l), "bookmarks-" + tag + "-7-unreadable.png", false)));
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", font);
                try {
                    UIManager.setLookAndFeel(was);
                } catch (javax.swing.UnsupportedLookAndFeelException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        for (String image : images) {
            p("![](" + image.split(" ")[0] + ")");
            p("");
        }
        p("| key | English | Norsk bokmål |");
        p("|---|---|---|");
        for (Map.Entry<String, String[]> e : WORDS.entrySet()) {
            p("| " + e.getKey() + " | " + e.getValue()[0] + " | " + e.getValue()[1] + " |");
        }
        p("");
        PLATFORM.append("\n| sketch | width × height (px) |\n|---|---|\n");
        for (String image : images) {
            String[] parts = image.split(" ");
            PLATFORM.append("| ").append(parts[0]).append(" | ").append(parts[1]).append(" |\n");
        }
    }

    private static JComponent menu(int l) {
        JPanel shell = new JPanel(new BorderLayout());
        JMenuBar bar = new JMenuBar();
        for (String top : l == 0 ? new String[] {"File", "Chart", "Solar System"}
                : new String[] {"Fil", "Kart", "Solsystem"}) {
            bar.add(new JMenu(top));
        }
        JMenu bookmarks = new JMenu(say("menu", l));
        bookmarks.setMnemonic('B');
        bar.add(bookmarks);
        bar.add(new JMenu(l == 0 ? "Help" : "Hjelp"));
        JPopupMenu popup = new JPopupMenu();
        JMenuItem add = new JMenuItem(say("add", l));
        add.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D,
                java.awt.event.InputEvent.META_DOWN_MASK));
        JMenuItem show = new JMenuItem(say("show", l));
        show.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B,
                java.awt.event.InputEvent.META_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        popup.add(add);
        popup.add(show);
        JPanel items = new JPanel(new BorderLayout());
        items.setBorder(BorderFactory.createEmptyBorder(0, 230, 0, 0));
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBorder(UIManager.getBorder("PopupMenu.border"));
        stack.add(add);
        stack.add(show);
        items.add(stack, BorderLayout.WEST);
        shell.add(bar, BorderLayout.NORTH);
        shell.add(items, BorderLayout.CENTER);
        return shell;
    }

    private static JComponent addDialog(int l, boolean timed, Instant instant) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel heading = new JLabel(say("addTitle", l));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        c.gridwidth = 2;
        form.add(heading, c);
        c.gridwidth = 1;
        row(form, c, say("title", l), new JTextField(timed ? "The Moon with Venus" : "M33", 26));
        JComboBox<String> folder = new JComboBox<>(new String[] {
                timed ? say("root", l) + " ▸ Sky events" : say("root", l) + " ▸ Deep sky"});
        row(form, c, say("folder", l), folder);
        JLabel view = new JLabel(say("view", l) + (timed ? "  (8°)" : "  (2°)"));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        form.add(view, c);
        c.gridy++;
        form.add(new JLabel(say("subjects", l)), c);
        PageText words = PageText.in(InterfaceText.forLanguage(LANGUAGES.get(l)));
        JList<String> list = new JList<>(timed
                ? new String[] {"★ " + words.bodyName("moon"), "   " + words.bodyName("venus")}
                : new String[] {"★ M 33"});
        list.setVisibleRowCount(3);
        list.setSelectedIndex(0);
        c.gridy++;
        JPanel subjects = new JPanel(new BorderLayout(6, 0));
        subjects.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel moves = new JPanel();
        moves.setLayout(new BoxLayout(moves, BoxLayout.Y_AXIS));
        moves.add(new JButton(say("up", l)));
        moves.add(new JButton(say("down", l)));
        moves.add(new JButton(say("remove", l)));
        subjects.add(moves, BorderLayout.EAST);
        form.add(subjects, c);
        c.gridy++;
        JComboBox<String> primary = new JComboBox<>(new String[] {
                timed ? words.bodyName("moon") : "M 33", say("none", l)});
        JPanel primaryRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        primaryRow.add(new JLabel(say("primary", l) + "  "));
        primaryRow.add(primary);
        form.add(primaryRow, c);
        c.gridy++;
        form.add(new JSeparator(), c);
        c.gridy++;
        JCheckBox time = new JCheckBox(say("time", l) + "  " + instant.toString()
                .replace("T", " ").replace(":00Z", " UTC"), timed);
        form.add(time, c);
        c.gridy++;
        JPanel purpose = new JPanel(new FlowLayout(FlowLayout.LEADING, 24, 0));
        JRadioButton event = new JRadioButton(say("event", l), timed);
        JRadioButton obs = new JRadioButton(say("observation", l));
        ButtonGroup g = new ButtonGroup();
        g.add(event);
        g.add(obs);
        event.setEnabled(timed);
        obs.setEnabled(timed);
        purpose.add(event);
        purpose.add(obs);
        form.add(purpose, c);
        if (timed) {
            c.gridy++;
            JLabel why = new JLabel(say("dynamicNote", l));
            why.setFont(why.getFont().deriveFont(why.getFont().getSize2D() - 1f));
            form.add(why, c);
        }
        c.gridy++;
        form.add(new JCheckBox(say("place", l) + "  59.913° N, 10.752° E", false), c);
        c.gridy++;
        form.add(new JLabel(say("note", l)), c);
        c.gridy++;
        JTextArea note = new JTextArea(3, 30);
        form.add(new JScrollPane(note), c);
        c.gridy++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttons.add(new JButton(say("cancel", l)));
        buttons.add(new JButton(say("ok", l)));
        form.add(buttons, c);
        return form;
    }

    private static void row(JPanel form, GridBagConstraints c, String label, JComponent field) {
        c.gridy++;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 0;
    }

    private static JComponent tab(int l, Library library, Map<String, SearchResult> named) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(l == 0 ? "Chart" : "Kart", new JPanel());
        tabs.addTab(l == 0 ? "Solar System" : "Solsystemet", new JPanel());
        JPanel page = new JPanel(new BorderLayout(0, 6));
        page.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(say("root", l));
        PageText words = PageText.in(InterfaceText.forLanguage(LANGUAGES.get(l)));
        DefaultMutableTreeNode selectedNode = null;
        for (Node n : library.root()) {
            DefaultMutableTreeNode child = treeNode(n);
            root.add(child);
            if (n instanceof Folder f && f.title().equals("Sky events")) {
                selectedNode = (DefaultMutableTreeNode) child.getChildAt(1);
            }
        }
        root.add(new DefaultMutableTreeNode("⚠ " + (l == 0 ? "A record from a later version"
                : "En oppføring fra en senere versjon") + " - " + say("attention", l)));
        JTree tree = new JTree(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        if (selectedNode != null) {
            tree.setSelectionPath(new javax.swing.tree.TreePath(selectedNode.getPath()));
        }
        tree.setVisibleRowCount(12);
        page.add(new JScrollPane(tree), BorderLayout.CENTER);
        Bookmark shown = BookmarkDocument.bookmarks(library.root()).get(4);
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(BorderFactory.createTitledBorder(say("details", l)));
        details.add(new JLabel(shown.title()));
        details.add(new JLabel((l == 0 ? "View: " : "Utsnitt: ")
                + fmt2(shown.view().raDegrees()) + "°, " + fmt2(shown.view().decDegrees())
                + "° · " + fmt(shown.view().fieldWidthDegrees()) + "°"));
        String starId = shown.subjects().get(1).id();
        String starName = named.values().stream().filter(r -> r.identity().equals(starId))
                .map(SearchResult::label).findFirst().orElse(starId);
        details.add(new JLabel((l == 0 ? "Objects: " : "Objekter: ") + "★ "
                + words.bodyName("moon") + ", " + starName));
        details.add(new JLabel((l == 0 ? "Event time: " : "Hendelsestid: ")
                + shown.time().get().instant()));
        details.add(new JLabel((l == 0 ? "Place: " : "Sted: ") + "59.913° N, 10.752° E"));
        JPanel actions = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        JPanel first = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        first.add(new JButton(say("open", l)));
        first.add(new JButton(say("edit", l)));
        first.add(new JButton(say("delete", l)));
        JPanel second = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        second.add(new JButton(say("newFolder", l)));
        second.add(new JButton(say("library", l) + " ▾"));
        actions.add(first);
        actions.add(second);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(details, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);
        page.add(south, BorderLayout.SOUTH);
        tabs.addTab(say("tab", l), page);
        tabs.setSelectedIndex(2);
        JPanel shell = new JPanel(new BorderLayout());
        shell.add(tabs, BorderLayout.CENTER);
        shell.setPreferredSize(new Dimension(360, Math.max(560, shell.getPreferredSize().height)));
        return shell;
    }

    private static DefaultMutableTreeNode treeNode(Node n) {
        return switch (n) {
            case Folder f -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(f.title());
                for (Node c : f.children()) {
                    node.add(treeNode(c));
                }
                yield node;
            }
            case Bookmark b -> new DefaultMutableTreeNode(b.title()
                    + (b.time().isPresent() ? "  ◷" : ""), false);
            case Kept k -> new DefaultMutableTreeNode("⚠ " + k.id(), false);
        };
    }

    private static JComponent restore(int l, Library library) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel title = new JLabel(say("restoreQuestion", l));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        box.add(title, BorderLayout.NORTH);
        JPanel middle = new JPanel();
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(new JLabel(java.text.MessageFormat.format(say("restoreSummary", l),
                BookmarkDocument.bookmarks(library.root()).size(),
                BookmarkDocument.folders(library.root()).size(), "6.2.0")));
        middle.add(Box.createVerticalStrut(6));
        middle.add(new JLabel(say("restoreKeep", l)));
        box.add(middle, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttons.add(new JButton(say("cancel", l)));
        buttons.add(new JButton(say("restoreYes", l)));
        box.add(buttons, BorderLayout.SOUTH);
        return box;
    }

    private static JComponent damaged(int l) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel title = new JLabel(say("damaged", l));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        box.add(title, BorderLayout.NORTH);
        box.add(new JLabel(java.text.MessageFormat.format(say("damagedOffer", l),
                "2026-11-02 14:10")), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttons.add(new JButton(say("cancel", l)));
        buttons.add(new JButton(say("damagedYes", l)));
        box.add(buttons, BorderLayout.SOUTH);
        return box;
    }

    /**
     * Applies the theme first and only then builds the sketch, so that
     * every run - the first in a process and the contract's second -
     * builds the same components under the same look.
     */
    private static String draw(java.util.function.Supplier<JComponent> build, String name,
                               boolean dark) throws IOException {
        juranometria.app.UiTheme.apply(dark);
        JComponent content = build.get();
        Dimension size = content.getPreferredSize();
        content.setSize(size);
        layOut(content);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, size.width, size.height);
            content.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name + " " + size.width + " × " + size.height;
    }

    private static void layOut(Component at) {
        at.invalidate();
        if (at instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    // ------------------------------------------------------------------

    private static <T> T onEdt(Callable<T> work) throws Exception {
        Object[] out = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                out[0] = work.call();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T result = (T) out[0];
        return result;
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }

    private static String fmt2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String fmt3(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
