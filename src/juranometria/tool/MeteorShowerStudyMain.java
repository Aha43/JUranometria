package juranometria.tool;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import juranometria.bookmark.BookmarkDocument;
import juranometria.bookmark.Node;
import juranometria.chart.SkyPosition;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService;
import juranometria.tool.MeteorStudyData.GmnSample;
import juranometria.tool.MeteorStudyData.MdcSolution;
import juranometria.tool.MeteorStudyData.MonthDay;
import juranometria.tool.MeteorStudyData.Radiant;
import juranometria.tool.MeteorStudyData.Row;
import juranometria.tool.MeteorStudyData.Shower;

/**
 * The meteor-shower measurement study (Sprint 60, issue #715, road
 * #714): what the authorities publish, how a radiant is placed at a
 * chart time, what a date means, which showers a first release could
 * carry and what the atlas can already say about a shower night. It
 * measures and decides nothing; the proposals are in
 * {@code docs/decisions/meteor-showers.md}.
 *
 * <p>Standard output is {@code docs/studies/meteor-showers/measurements.md}:
 * numbers from the approved raw sources and the production services,
 * deterministic. Regenerate with {@code make meteor-shower-study}.
 */
public final class MeteorShowerStudyMain {

    static final double OSLO_LAT = 59.91;
    static final double OSLO_LON = 10.75;
    /** #564's candidate set, in the order of the year. */
    static final List<String> CANDIDATES = List.of("QUA", "LYR", "ETA", "SDA", "PER", "ORI",
            "LEO", "GEM", "URS");
    static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private static PrintStream out;
    private static MeteorStudyData data;

    private MeteorShowerStudyMain() {
    }

    private static void p(String s) {
        out.println(s);
    }

    private static String f(String format, Object... args) {
        return MeteorStudyData.f(format, args);
    }

    public static void main(String[] args) throws Exception {
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        data = MeteorStudyData.load(SolarSystemService.load());

        p("# Meteor showers at chart time: measurements (#715)");
        p("");
        p("Sprint 60, issue #715, road #714. Numbers only: every value below is read from"
                + " an owner-approved raw source held to its checksum, or computed by the"
                + " atlas's production services (the DE440 Sun and Moon, the J2000 ecliptic,"
                + " the local sky). Nothing is typed from memory. The proposals these support"
                + " are in `docs/decisions/meteor-showers.md`. Regenerate with"
                + " `make meteor-shower-study`.");
        p("");
        p("Credit for every IMO value: Source: International Meteor Organization (IMO) -"
                + " www.imo.net. IAU values: the IAU Meteor Data Center shower database."
                + " GMN values: the Global Meteor Network, CC BY 4.0.");
        p("");
        inputs();
        coverage();
        editions();
        timeStandard();
        calendarDrift();
        tableAgainstItself();
        rules();
        beyondRows();
        showerSet();
        night();
        association();
        bookmark();
    }

    // ------------------------------------------------------------------ 1

    private static void inputs() {
        p("## 1. Inputs");
        p("");
        p("| file under `imports/raw/meteor-showers/` | bytes | SHA-256 |");
        p("|---|---|---|");
        for (Map.Entry<String, String> e : MeteorStudyData.SHA256.entrySet()) {
            p(f("| `%s` | %d | `%s` |", e.getKey(), data.sizes.get(e.getKey()), e.getValue()));
        }
        p("");
        p("The IAU list states its own date: last update " + data.mdcLastUpdate + ".");
        p("");
    }

    // ------------------------------------------------------------------ 2

    private static void coverage() {
        p("## 2. What each source covers");
        p("");
        int solutions = data.mdc.values().stream().mapToInt(List::size).sum();
        TreeMap<String, Integer> byStatus = new TreeMap<>();
        for (List<MdcSolution> list : data.mdc.values()) {
            byStatus.merge(list.get(0).status(), 1, Integer::sum);
        }
        long gmnSamples = data.gmn.values().stream().mapToLong(List::size).sum();
        p("| source | showers | what it holds for each |");
        p("|---|---|---|");
        p(f("| IMO calendar 2027, Table 5 | %d and the Antihelion Source | activity dates,"
                + " maximum date and solar longitude, radiant at maximum (whole degrees),"
                + " V-infinity, population index r, ZHR |", data.imo2027.size()));
        long rows = data.table6.values().stream().mapToLong(List::size).sum();
        p(f("| IMO calendar 2027, Table 6 | %d of them and the Antihelion Source | the radiant"
                + " in whole degrees at dates five days apart: %d rows |",
                data.table6.keySet().stream().filter(data.imo2027::containsKey).count(), rows));
        p(f("| IAU MDC established list | %d codes, %d solutions (first-solution status: %s)"
                + " | name, code, status, and per published solution: solar longitude, radiant,"
                + " often a daily drift, geocentric velocity, orbit, sometimes a parent body |",
                data.mdc.size(), solutions, byStatus));
        p(f("| GMN shower table | %d codes, %d samples | the Sun-centred ecliptic radiant,"
                + " geocentric velocity and measured dispersion every 0.2 degrees of solar"
                + " longitude |", data.gmn.size(), gmnSamples));
        p("");
        List<String> notMdc = new ArrayList<>();
        List<String> notGmn = new ArrayList<>();
        List<String> noRows = new ArrayList<>();
        for (String code : data.imo2027.keySet()) {
            if (!data.mdc.containsKey(code)) {
                notMdc.add(code);
            }
            if (!data.gmn.containsKey(code)) {
                notGmn.add(code);
            }
            if (data.table6.getOrDefault(code, List.of()).size() < 2) {
                noRows.add(code + " (" + data.table6.getOrDefault(code, List.of()).size() + ")");
            }
        }
        p("- IMO working-list showers absent from the IAU established list: "
                + String.join(", ", notMdc) + ".");
        p("- IMO working-list showers absent from the GMN table: " + String.join(", ", notGmn)
                + ".");
        p("- IMO working-list showers with fewer than two rows in Table 6 (rows): "
                + String.join(", ", noRows) + ".");
        p("");
    }

    // ------------------------------------------------------------------ 3

    private static void editions() {
        p("## 3. What changed between the 2026 and 2027 editions of the IMO working list");
        p("");
        p("| shower | field | 2026 edition | 2027 edition |");
        p("|---|---|---|---|");
        int dateOnly = 0;
        int solarChanged = 0;
        for (Shower b : data.imo2027.values()) {
            Shower a = data.imo2026.get(b.code());
            if (a == null) {
                p(f("| %s | (whole row) | absent | added |", b.code()));
                continue;
            }
            if (!a.solarLongitudeText().equals(b.solarLongitudeText())) {
                solarChanged++;
                p(f("| %s | solar longitude of maximum | %s | %s |", b.code(),
                        a.solarLongitudeText(), b.solarLongitudeText()));
            }
            if (!a.maximum().equals(b.maximum())) {
                dateOnly++;
                p(f("| %s | date of maximum | %s | %s |", b.code(), a.maximum(), b.maximum()));
            }
            if (!a.start().equals(b.start()) || !a.end().equals(b.end())) {
                p(f("| %s | activity | %s - %s | %s - %s |", b.code(), a.start(), a.end(),
                        b.start(), b.end()));
            }
            if (a.ra() != b.ra() || a.dec() != b.dec()) {
                p(f("| %s | radiant at maximum | %.0f %+.0f | %.0f %+.0f |", b.code(), a.ra(),
                        a.dec(), b.ra(), b.dec()));
            }
            if (a.vInfinity() != b.vInfinity()) {
                p(f("| %s | V-infinity | %.0f | %.0f |", b.code(), a.vInfinity(),
                        b.vInfinity()));
            }
            if (!a.populationIndex().equals(b.populationIndex())) {
                p(f("| %s | r | %s | %s |", b.code(), a.populationIndex(),
                        b.populationIndex()));
            }
            if (!a.zhr().equals(b.zhr())) {
                p(f("| %s | ZHR | %s | %s |", b.code(), a.zhr(), b.zhr()));
            }
        }
        p("");
        p(f("Of the %d showers in both editions, the solar longitude of maximum changed for"
                + " %d and the calendar date of maximum for %d. The radiant table (Table 6)"
                + " differs between the editions in one entry (DRA, Oct 10).",
                data.imo2026.size(), solarChanged, dateOnly));
        p("");
    }

    // ------------------------------------------------------------------ 4

    private static void timeStandard() {
        p("## 4. What the listed date of maximum is");
        p("");
        p("The instant in 2027 at which the production Sun reaches each listed solar longitude"
                + " (equinox J2000), against the date the 2027 edition prints. The calendar's"
                + " text states one time of day outright - the Quadrantids peak \"on January 4"
                + " shortly after 03h UT\" (p. 3) - which is the one printed clock time this"
                + " section can be held to.");
        p("");
        p("| shower | listed solar longitude | Sun reaches it (UTC) | listed date | same UTC"
                + " date |");
        p("|---|---|---|---|---|");
        int same = 0;
        for (Shower s : data.imo2027.values()) {
            Instant when = data.whenSolarLongitude(s.solarLongitude(),
                    s.maximum().in(2027).plusSeconds(43200));
            MonthDay got = new MonthDay(when.atZone(ZoneOffset.UTC).getMonthValue(),
                    when.atZone(ZoneOffset.UTC).getDayOfMonth());
            boolean ok = got.equals(s.maximum());
            same += ok ? 1 : 0;
            if (CANDIDATES.contains(s.code())) {
                p(f("| %s | %s | %s | %s | %s |", s.code(), s.solarLongitudeText(),
                        UTC.format(when), s.maximum(), ok ? "yes" : "no"));
            }
        }
        p("");
        p(f("The rows are #564's nine candidates; the count is the whole working list. %d of"
                + " %d listed dates are the UTC date on which the Sun reaches the listed solar"
                + " longitude. A longitude printed to a whole degree is known to half a degree,"
                + " which is 12 hours; to one decimal, 1.2 hours.", same, data.imo2027.size()));
        p("");
    }

    // ------------------------------------------------------------------ 5

    private static void calendarDrift() {
        p("## 5. A solar longitude is not a calendar date");
        p("");
        p("The UTC instant at which the Sun reaches the listed solar longitude of maximum,"
                + " by year, for #564's candidate showers. The pack serves 1900-2100.");
        p("");
        int[] years = {1900, 1950, 2000, 2024, 2025, 2026, 2027, 2028, 2050, 2100};
        StringBuilder head = new StringBuilder("| shower |");
        StringBuilder rule = new StringBuilder("|---|");
        for (int y : years) {
            head.append(' ').append(y).append(" |");
            rule.append("---|");
        }
        p(head + " span |");
        p(rule + "---|");
        for (String code : CANDIDATES) {
            Shower s = data.imo2027.get(code);
            StringBuilder line = new StringBuilder("| " + code + " |");
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int y : years) {
                Instant when = data.whenSolarLongitude(s.solarLongitude(),
                        s.maximum().in(y).plusSeconds(43200));
                double dayOfYear = Duration.between(s.maximum().in(y), when).toSeconds()
                        / 86400.0;
                min = Math.min(min, dayOfYear);
                max = Math.max(max, dayOfYear);
                line.append(' ').append(DateTimeFormatter.ofPattern("MMM dd HH'h'")
                        .withZone(ZoneOffset.UTC).format(when)).append(" |");
            }
            p(line + f(" %.1f d |", max - min));
        }
        p("");
        p("The same reading the other way: the Sun's longitude (J2000) at 00:00 UTC on"
                + " 13 August.");
        p("");
        StringBuilder l = new StringBuilder("|");
        StringBuilder h = new StringBuilder("|");
        StringBuilder r = new StringBuilder("|");
        for (int y : years) {
            h.append(' ').append(y).append(" |");
            r.append("---|");
            l.append(f(" %.2f |", data.solarLongitude(new MonthDay(8, 13).in(y))));
        }
        p(h.toString());
        p(r.toString());
        p(l.toString());
        p("");
    }

    // ------------------------------------------------------------------ 6

    private static void tableAgainstItself() {
        p("## 6. The radiant table against the working list");
        p("");
        p("Table 6 read at the listed solar longitude of maximum, against the radiant Table 5"
                + " prints for the maximum. Both are whole degrees; a difference under a"
                + " degree is rounding.");
        p("");
        p("| shower | Table 5 at maximum | Table 6 read at maximum | footing | apart |");
        p("|---|---|---|---|---|");
        int within1 = 0;
        int within2 = 0;
        List<String> beyond2 = new ArrayList<>();
        for (Shower s : data.imo2027.values()) {
            Radiant r = data.radiantAt(s.code(), s.solarLongitude());
            double apart = r.at().separationDegrees(s.radiantAtMaximum());
            within1 += apart <= 1.0 ? 1 : 0;
            within2 += apart <= 2.0 ? 1 : 0;
            if (apart > 2.0) {
                beyond2.add(f("%s %.1f", s.code(), apart));
            }
            if (CANDIDATES.contains(s.code())) {
                p(f("| %s | %.0f %+.0f | %.1f %+.1f | %s | %.1f |", s.code(), s.ra(), s.dec(),
                        r.at().raDegrees(), r.at().decDegrees(), footing(r), apart));
            }
        }
        p("");
        p(f("The rows are the nine candidates. Across the whole working list of %d: %d within"
                + " 1 degree, %d within 2 degrees; farther apart: %s.", data.imo2027.size(),
                within1, within2, String.join(", ", beyond2)));
        p("");
        p("The calendar states the equinox of its solar longitudes (2000.0) and prints none"
                + " for its radiants. Were the printed radiants of the equinox of date instead of"
                + " J2000, each would lie this far from where the study plots it, at the 2027"
                + " maximum (the production precession and nutation):");
        p("");
        StringBuilder codes = new StringBuilder("|");
        StringBuilder rule = new StringBuilder("|");
        StringBuilder values = new StringBuilder("|");
        for (String code : CANDIDATES) {
            Shower s = data.imo2027.get(code);
            double jd = SkyFrame.julianDate(data.whenSolarLongitude(s.solarLongitude(),
                    s.maximum().in(2027).plusSeconds(43200)));
            codes.append(' ').append(code).append(" |");
            rule.append("---|");
            values.append(f(" %.2f |", SkyFrame.toJ2000(s.radiantAtMaximum(), jd)
                    .separationDegrees(s.radiantAtMaximum())));
        }
        p(codes.toString());
        p(rule.toString());
        p(values.toString());
        p("");
    }

    private static String footing(Radiant r) {
        return switch (r.footing()) {
            case BETWEEN_ROWS -> "between rows";
            case BEYOND_ROWS -> f("%.1f d beyond rows", r.daysBeyondRows());
            case FIXED -> "one position";
        };
    }

    // ------------------------------------------------------------------ 7

    private static void rules() {
        p("## 7. Two evaluation rules against the printed rows and two independent sources");
        p("");
        p("At every printed row of Table 6: how far the row lies from (a) the listed maximum"
                + " carried at a fixed Sun-centred ecliptic position, and (b) the GMN table at"
                + " the same solar longitude, with GMN's own measured dispersion beside it."
                + " Then, at the listed maximum: Table 5 against GMN and against the IAU"
                + " list's solutions, each moved to the same solar longitude by its own daily"
                + " drift where it states one (a solution with no drift is compared only"
                + " within 1.5 degrees of solar longitude). Degrees of arc.");
        p("");
        p("| shower | rows | fixed Sun-centred: mean / worst | GMN: rows met, mean / worst"
                + " (dispersion) | at maximum: GMN | at maximum: IAU solutions, median /"
                + " worst (n) |");
        p("|---|---|---|---|---|---|");
        for (Shower s : data.imo2027.values()) {
            List<Row> rows = data.table6.getOrDefault(s.code(), List.of());
            double sumFixed = 0;
            double worstFixed = 0;
            double sumGmn = 0;
            double worstGmn = 0;
            double sumDisp = 0;
            int met = 0;
            for (Row row : rows) {
                double d = row.at().separationDegrees(
                        data.fixedSunCentred(s, row.solarLongitude()));
                sumFixed += d;
                worstFixed = Math.max(worstFixed, d);
                GmnSample g = data.gmnAt(s.code(), row.solarLongitude());
                if (g != null) {
                    double dg = row.at().separationDegrees(g.at());
                    sumGmn += dg;
                    worstGmn = Math.max(worstGmn, dg);
                    sumDisp += g.dispersion();
                    met++;
                }
            }
            GmnSample atMax = data.gmnAt(s.code(), s.solarLongitude());
            List<Double> iau = new ArrayList<>();
            for (MdcSolution m : data.mdc.getOrDefault(s.code(), List.of())) {
                double dl = MeteorStudyData.signed(s.solarLongitude() - m.solarLongitude());
                if (m.dRa() != null && m.dDe() != null) {
                    iau.add(s.radiantAtMaximum().separationDegrees(new SkyPosition(
                            SkyFrame.normalise(m.ra() + m.dRa() * dl),
                            Math.max(-90, Math.min(90, m.dec() + m.dDe() * dl)))));
                } else if (Math.abs(dl) < 1.5) {
                    iau.add(s.radiantAtMaximum().separationDegrees(
                            new SkyPosition(m.ra(), m.dec())));
                }
            }
            iau.sort(null);
            p(f("| %s | %d | %s | %s | %s | %s |", s.code(), rows.size(),
                    rows.isEmpty() ? "-" : f("%.1f / %.1f", sumFixed / rows.size(), worstFixed),
                    met == 0 ? "none" : f("%d, %.1f / %.1f (%.1f)", met, sumGmn / met, worstGmn,
                            sumDisp / met),
                    atMax == null ? "no sample" : f("%.1f",
                            s.radiantAtMaximum().separationDegrees(atMax.at())),
                    iau.isEmpty() ? "none" : f("%.1f / %.1f (%d)", iau.get(iau.size() / 2),
                            iau.get(iau.size() - 1), iau.size())));
        }
        p("");
    }

    // ------------------------------------------------------------------ 8

    private static void beyondRows() {
        p("## 8. How much of each activity interval the printed rows cover");
        p("");
        p("The activity interval as dated in the 2027 edition, in solar longitude, against the"
                + " first and last printed row: how many days of activity lie before the"
                + " first row and after the last, where a rule must carry the radiant on."
                + " Where GMN has a sample at the activity's end, the carried-on radiant is"
                + " compared with it.");
        p("");
        p("| shower | activity (2027) | days before first row | days after last row | carried"
                + " on vs GMN at start / end |");
        p("|---|---|---|---|---|");
        List<String> farBeyond = new ArrayList<>();
        for (Shower s : data.imo2027.values()) {
            int year = s.start().month() > s.end().month() ? 2026 : 2027;
            Instant from = s.start().in(year);
            Instant to = s.end().in(2027).plusSeconds(86400);
            double l0 = data.solarLongitude(from);
            double l1 = data.solarLongitude(to);
            Radiant r0 = data.radiantAt(s.code(), l0);
            Radiant r1 = data.radiantAt(s.code(), l1);
            GmnSample g0 = data.gmnAt(s.code(), l0);
            GmnSample g1 = data.gmnAt(s.code(), l1);
            double before = r0.footing() == MeteorStudyData.Footing.BEYOND_ROWS
                    ? r0.daysBeyondRows() : 0.0;
            double after = r1.footing() == MeteorStudyData.Footing.BEYOND_ROWS
                    ? r1.daysBeyondRows() : 0.0;
            if (Math.max(before, after) > 5.0) {
                farBeyond.add(f("%s %.1f", s.code(), Math.max(before, after)));
            }
            if (!CANDIDATES.contains(s.code())) {
                continue;
            }
            p(f("| %s | %s - %s | %s | %s | %s / %s |", s.code(), s.start(), s.end(),
                    before > 0 ? f("%.1f", before) : "0", after > 0 ? f("%.1f", after) : "0",
                    g0 == null ? "-" : f("%.1f", r0.at().separationDegrees(g0.at())),
                    g1 == null ? "-" : f("%.1f", r1.at().separationDegrees(g1.at()))));
        }
        p("");
        p("The rows are the nine candidates. In the rest of the working list, the showers"
                + " whose activity runs more than five days beyond their printed rows (days): "
                + String.join(", ", farBeyond) + ".");
        p("");
    }

    // ------------------------------------------------------------------ 9

    private static void showerSet() {
        p("## 9. Which showers a strength rule selects");
        p("");
        p("The 2027 working list counted by listed ZHR, and the showers at 10 or more in"
                + " full, with what the IAU list says of each. `Var` is the calendar's word for"
                + " a possibly periodic shower with no annual rate. The rest of the working"
                + " list is counted, not reproduced: it is the IMO's to publish.");
        p("");
        int[] bands = new int[4];
        for (Shower s : data.imo2027.values()) {
            double z = s.zhrNumber();
            bands[Double.isNaN(z) ? 3 : z >= 10 ? 0 : z >= 5 ? 1 : 2]++;
        }
        p("| listed ZHR | showers |");
        p("|---|---|");
        p(f("| 10 or more | %d |", bands[0]));
        p(f("| 5 to 9 | %d |", bands[1]));
        p(f("| under 5 | %d |", bands[2]));
        p(f("| `Var` | %d |", bands[3]));
        p("");
        p("| shower | ZHR | activity | maximum | V-inf km/s | r | in the IAU established list |"
                + " IAU name | IAU parent | #564 |");
        p("|---|---|---|---|---|---|---|---|---|---|");
        List<Shower> order = new ArrayList<>(data.imo2027.values());
        order.removeIf(s -> !(s.zhrNumber() >= 10));
        order.sort((x, y) -> Double.compare(y.zhrNumber(), x.zhrNumber()));
        for (Shower s : order) {
            List<MdcSolution> m = data.mdc.getOrDefault(s.code(), List.of());
            String parent = m.stream().map(MdcSolution::parent).filter(x -> !x.isEmpty())
                    .findFirst().orElse("-");
            p(f("| %s | %s | %s - %s | %s | %.0f | %s | %s | %s | %s | %s |", s.code(), s.zhr(),
                    s.start(), s.end(), s.maximum(), s.vInfinity(), s.populationIndex(),
                    m.isEmpty() ? "no" : "yes", m.isEmpty() ? "-" : m.get(0).name(), parent,
                    CANDIDATES.contains(s.code()) ? "yes" : ""));
        }
        p("");
        // How many are active at once: the clutter a draw-all rule would meet.
        int most = 0;
        String mostDay = "";
        for (int d = 0; d < 365; d++) {
            Instant day = Instant.parse("2027-01-01T00:00:00Z").plusSeconds(d * 86400L);
            int n = 0;
            for (Shower s : data.imo2027.values()) {
                n += data.active(s, day) ? 1 : 0;
            }
            if (n > most) {
                most = n;
                mostDay = UTC.format(day).substring(0, 10);
            }
        }
        p(f("Most working-list showers active on one date of 2027: %d, first on %s.", most,
                mostDay));
        p("");
    }

    // ------------------------------------------------------------------ 10

    private static void night() {
        p("## 10. What the atlas can already say about a shower night");
        p("");
        p("For #564's candidates at the instant of the listed 2027 maximum, from Oslo"
                + " (59.91 N, 10.75 E): the radiant by the rule of section 6, its geometric"
                + " altitude from the production local sky, and the Sun and Moon from the"
                + " production service. After 2027-06-28 the pinned leap-second file has"
                + " expired and altitudes carry the service's stated estimate.");
        p("");
        p("| shower | chart time (UTC) | radiant J2000 | radiant altitude | Sun altitude |"
                + " Moon lit | Moon altitude | Moon to radiant |");
        p("|---|---|---|---|---|---|---|---|");
        for (String code : CANDIDATES) {
            Shower s = data.imo2027.get(code);
            Instant when = data.whenSolarLongitude(s.solarLongitude(),
                    s.maximum().in(2027).plusSeconds(43200));
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON, when);
            SkyPosition radiant = data.radiantAt(code, s.solarLongitude()).at();
            var sun = (SolarSystemService.SunObservation) data.solar.observe(
                    SolarSystemService.Body.SUN, oslo);
            var moon = (SolarSystemService.MoonObservation) data.solar.observe(
                    SolarSystemService.Body.MOON, oslo);
            p(f("| %s | %s | %.1f %+.1f | %+.1f | %+.1f | %.0f%% (%s) | %+.1f | %.0f |", code,
                    UTC.format(when), radiant.raDegrees(), radiant.decDegrees(),
                    new LocalSky(oslo).altitudeDegrees(radiant),
                    sun.horizontal().altitudeDegrees(), moon.illuminatedFraction() * 100.0,
                    moon.phase().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '),
                    moon.horizontal().altitudeDegrees(),
                    moon.astrometricJ2000().separationDegrees(radiant)));
        }
        p("");
        p("The night around each maximum, from Oslo, sampled every five minutes from 12:00 UTC"
                + " before the maximum's date to 12:00 UTC after it: how long the Sun stands"
                + " more than 18, 12 and 6 degrees below the horizon, and the radiant's"
                + " highest altitude while the Sun is more than 12 degrees down. The atlas"
                + " has no darkness search today; this is the production Sun sampled.");
        p("");
        p("| shower | night of (UTC) | Sun below -18 | below -12 | below -6 | Sun's lowest |"
                + " radiant's highest in the -12 dark |");
        p("|---|---|---|---|---|---|---|");
        for (String code : CANDIDATES) {
            Shower s = data.imo2027.get(code);
            Instant max = data.whenSolarLongitude(s.solarLongitude(),
                    s.maximum().in(2027).plusSeconds(43200));
            // The night nearest the maximum: noon to noon around the midnight closest to it.
            Instant midnight = Instant.ofEpochSecond(
                    Math.round(max.getEpochSecond() / 86400.0) * 86400L);
            Instant from = midnight.minusSeconds(43200);
            int b18 = 0;
            int b12 = 0;
            int b6 = 0;
            double lowest = 90;
            double radiantBest = -90;
            for (int i = 0; i < 288; i++) {
                Instant t = from.plusSeconds(i * 300L);
                Observer o = new Observer(OSLO_LAT, OSLO_LON, t);
                double sunAlt = ((SolarSystemService.SunObservation) data.solar.observe(
                        SolarSystemService.Body.SUN, o)).horizontal().altitudeDegrees();
                lowest = Math.min(lowest, sunAlt);
                b18 += sunAlt < -18 ? 1 : 0;
                b12 += sunAlt < -12 ? 1 : 0;
                b6 += sunAlt < -6 ? 1 : 0;
                if (sunAlt < -12) {
                    radiantBest = Math.max(radiantBest, new LocalSky(o).altitudeDegrees(
                            data.radiantAt(code, data.solarLongitude(t)).at()));
                }
            }
            p(f("| %s | %s / %s | %s | %s | %s | %+.1f | %s |", code,
                    UTC.format(from).substring(0, 10),
                    UTC.format(from.plusSeconds(86400)).substring(0, 10), hours(b18),
                    hours(b12), hours(b6), lowest,
                    b12 == 0 ? "no such dark" : f("%+.1f", radiantBest)));
        }
        p("");
    }

    /** A number as the bookmark writer prints it: two decimals at most, no trailing zeros. */
    private static String plain(double value) {
        return new java.math.BigDecimal(f("%.2f", value)).stripTrailingZeros().toPlainString();
    }

    private static String hours(int fiveMinuteSamples) {
        return fiveMinuteSamples == 0 ? "none" : f("%.1f h", fiveMinuteSamples / 12.0);
    }

    // ------------------------------------------------------------------ 11

    private static void association() {
        p("## 11. Why a direction alone is not membership");
        p("");
        p("The IMO calendar (section 7, by Rainer Arlt, Table 1) gives the radiant diameter"
                + " to assume when a plotted meteor is associated with a shower, by the"
                + " meteor's distance from the radiant. For a meteor that belongs to no"
                + " shower, travelling in a direction unrelated to the radiant, the chance"
                + " that its path prolonged backwards crosses that circle follows from"
                + " geometry alone: the circle of radius rho seen from distance D spans"
                + " 2 asin(sin rho / sin D) of the 360 degrees of possible directions.");
        p("");
        p("| distance from radiant | IMO optimum diameter | directions that line up | chance"
                + " for an unrelated meteor |");
        p("|---|---|---|---|");
        double[][] table1 = {{15, 14}, {30, 17}, {50, 20}, {70, 23}};
        for (double[] row : table1) {
            double span = 2 * Math.toDegrees(Math.asin(Math.sin(Math.toRadians(row[1] / 2))
                    / Math.sin(Math.toRadians(row[0]))));
            p(f("| %.0f | %.0f | %.1f of 360 | %.1f%% |", row[0], row[1], span,
                    span / 360.0 * 100.0));
        }
        p("");
        p("The calendar adds that direction is not the only criterion: the meteor's angular"
                + " speed must also match the shower's velocity (its Tables 2 and 3). An"
                + " atlas that holds only a plotted direction holds neither the speed nor the"
                + " path length.");
        p("");
    }

    // ------------------------------------------------------------------ 12

    private static void bookmark() {
        p("## 12. A shower at a chart time in the version-1 bookmark document");
        p("");
        Shower per = data.imo2027.get("PER");
        Instant when = data.whenSolarLongitude(per.solarLongitude(),
                per.maximum().in(2027).plusSeconds(43200));
        SkyPosition radiant = data.radiantAt("PER", per.solarLongitude()).at();
        String text = "{\n  \"format\": \"juranometria-bookmarks\",\n  \"version\": 1,\n"
                + "  \"writtenBy\": \"study\",\n  \"root\": [\n    {\n"
                + "      \"type\": \"bookmark\",\n"
                + "      \"id\": \"00000000-0000-4000-8000-000000000715\",\n"
                + "      \"title\": \"Perseids 2027 from Oslo\",\n"
                + f("      \"view\": {\n        \"ra\": %s,\n        \"dec\": %s,\n"
                        + "        \"field\": 60\n      },\n", plain(radiant.raDegrees()),
                        plain(radiant.decDegrees()))
                + "      \"subjects\": [\n        {\n          \"kind\": \"body\",\n"
                + "          \"id\": \"meteor-shower.PER\"\n        }\n      ],\n"
                + "      \"primary\": {\n        \"kind\": \"body\",\n"
                + "        \"id\": \"meteor-shower.PER\"\n      },\n"
                + "      \"time\": {\n        \"instant\": \"" + when + "\",\n"
                + "        \"purpose\": \"event\"\n      },\n"
                + "      \"place\": {\n        \"latitude\": 59.91,\n"
                + "        \"eastLongitude\": 10.75\n      }\n    }\n  ]\n}\n";
        BookmarkDocument.Read read = BookmarkDocument.read(text);
        p("The production reader of 6.5.0, given a bookmark whose subject is a shower's"
                + " identity in the existing `body` kind, with the chart time and place:");
        p("");
        p("```json");
        p(text.stripTrailing());
        p("```");
        p("");
        if (read instanceof BookmarkDocument.Usable usable) {
            Node node = usable.library().root().get(0);
            p("- read as: " + (node instanceof Node.Bookmark ? "a usable bookmark"
                    : "a record needing attention: " + node));
            p("- written back byte for byte: "
                    + (BookmarkDocument.text(usable.library()).equals(text) ? "yes" : "no"));
        } else {
            p("- read as: " + read);
        }
        p("");
    }
}
