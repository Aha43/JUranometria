package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService;

/**
 * The meteor-shower study's inputs and the one radiant evaluation it
 * measures (Sprint 60, issue #715). Tool code only: nothing in the
 * application reads this, and nothing here is a production catalogue.
 *
 * <p>The inputs are the owner-approved raw sources under
 * {@code imports/raw/meteor-showers} (gitignored), each held to its
 * SHA-256: the IMO 2026 and 2027 Meteor Shower Calendars as
 * transcribed tables, the IAU Meteor Data Center's list of established
 * showers, and the Global Meteor Network's shower table. A missing or
 * changed input stops the study; nothing is typed here from memory.
 */
final class MeteorStudyData {

    static final Path RAW = Path.of("imports/raw/meteor-showers");

    /** Every input with the checksum it was fetched or transcribed with (2026-11-11). */
    static final Map<String, String> SHA256 = new LinkedHashMap<>();

    static {
        SHA256.put("imo-cal2027.pdf",
                "9e6b9a1609fb89ebb8571756b15670b2ccf22e64ed22ae999ff604705d773eec");
        SHA256.put("imo-cal2026.pdf",
                "fde5388889ebda9fe13436d793da5e9935ae46b99edf20e0b19f7fe32ce1ed9f");
        SHA256.put("iaumdc-streamestablisheddata.txt",
                "67fdf79e83c2e690f35cb0eb6e0b8a5db7b4416bedc264eb61a5fc056dcad0f4");
        SHA256.put("gmn_shower_table_20230518.txt",
                "0f9ae2675c9f6b48b3cc1ea6f83a4b7d39e8910b1eace4f7fd73e1dbe5a98595");
        SHA256.put("transcribed/imo-2026-table5.tsv",
                "ba7b4859f24c9934c3eb1ff95a67cb0a64b8bb46412b0b4b59b03205e584a3b7");
        SHA256.put("transcribed/imo-2027-table5.tsv",
                "8a4760ba8bfafee56061dd34ab71ec446585ceb66255f905f662cb75c31b2833");
        SHA256.put("transcribed/imo-2027-table6.tsv",
                "37a44e58ebfc8bab69b1ca50d28bc9c836e0ec6f395b61f012f17c230961ecc1");
    }

    static final List<String> MONTHS = List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    /** A month and day as the calendar prints them; no year. */
    record MonthDay(int month, int day) {
        static MonthDay parse(String s) {
            return new MonthDay(MONTHS.indexOf(s.substring(0, 3)) + 1,
                    Integer.parseInt(s.substring(3)));
        }

        /** 00:00 UTC of this date in a year; "Jan 0" is 31 December of the year before. */
        Instant in(int year) {
            return LocalDate.of(year, month, 1).plusDays(day - 1L)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        @Override
        public String toString() {
            return MONTHS.get(month - 1) + " " + (day < 10 ? "0" : "") + day;
        }
    }

    /** One row of the IMO working list (Table 5). */
    record Shower(String code, int iauNumber, MonthDay start, MonthDay end, MonthDay maximum,
                  String solarLongitudeText, double solarLongitude, double ra, double dec,
                  double vInfinity, String populationIndex, String zhr) {
        SkyPosition radiantAtMaximum() {
            return new SkyPosition(ra, dec);
        }

        /** The listed ZHR as a number; a variable shower has none. */
        double zhrNumber() {
            String digits = zhr.replace("+", "");
            return digits.chars().allMatch(Character::isDigit) ? Double.parseDouble(digits)
                    : Double.NaN;
        }
    }

    /** One row of the IMO radiant table (Table 6): a date and a whole-degree position. */
    record Row(MonthDay date, double solarLongitude, SkyPosition at) {
    }

    /** One solution of the IAU MDC list. */
    record MdcSolution(String code, String status, String name, String activity,
                       double solarLongitude, double ra, double dec, Double dRa, Double dDe,
                       String parent) {
    }

    /** One sample of the GMN table. */
    record GmnSample(double solarLongitude, double sceLongitude, double sceLatitude,
                     double vg, double dispersion) {
        SkyPosition at() {
            return Ecliptic.toEquatorial(SkyFrame.normalise(sceLongitude + solarLongitude),
                    sceLatitude);
        }
    }

    final Map<String, Shower> imo2026 = new LinkedHashMap<>();
    final Map<String, Shower> imo2027 = new LinkedHashMap<>();
    final Map<String, List<Row>> table6 = new LinkedHashMap<>();
    final Map<String, List<MdcSolution>> mdc = new LinkedHashMap<>();
    final Map<String, List<GmnSample>> gmn = new LinkedHashMap<>();
    final Map<String, Long> sizes = new LinkedHashMap<>();
    final SolarSystemService solar;
    String mdcLastUpdate = "";

    private MeteorStudyData(SolarSystemService solar) {
        this.solar = solar;
    }

    static MeteorStudyData load(SolarSystemService solar) throws IOException {
        MeteorStudyData data = new MeteorStudyData(solar);
        for (Map.Entry<String, String> e : SHA256.entrySet()) {
            Path file = RAW.resolve(e.getKey());
            if (!Files.isRegularFile(file)) {
                throw new IOException("the study's approved input is absent: " + file
                        + " (see docs/studies/meteor-showers/sources.md)");
            }
            PinnedInputs.verifyChecksum(file, e.getValue());
            data.sizes.put(e.getKey(), Files.size(file));
        }
        readTable5(RAW.resolve("transcribed/imo-2026-table5.tsv"), data.imo2026);
        readTable5(RAW.resolve("transcribed/imo-2027-table5.tsv"), data.imo2027);
        data.readTable6(RAW.resolve("transcribed/imo-2027-table6.tsv"));
        data.readMdc(RAW.resolve("iaumdc-streamestablisheddata.txt"));
        data.readGmn(RAW.resolve("gmn_shower_table_20230518.txt"));
        return data;
    }

    private static void readTable5(Path file, Map<String, Shower> into) throws IOException {
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t");
            into.put(f[0], new Shower(f[0], Integer.parseInt(f[1]), MonthDay.parse(f[2]),
                    MonthDay.parse(f[3]), MonthDay.parse(f[4]), f[5], Double.parseDouble(f[5]),
                    Double.parseDouble(f[6]), Double.parseDouble(f[7]),
                    Double.parseDouble(f[8]), f[9], f[10]));
        }
    }

    private void readTable6(Path file) throws IOException {
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t");
            MonthDay date = new MonthDay(Integer.parseInt(f[1]), Integer.parseInt(f[2]));
            table6.computeIfAbsent(f[0], k -> new ArrayList<>()).add(new Row(date,
                    solarLongitude(date.in(2027)),
                    new SkyPosition(Double.parseDouble(f[3]), Double.parseDouble(f[4]))));
        }
        // In the order the Earth meets them: by solar longitude from the shower's maximum.
        for (Map.Entry<String, List<Row>> e : table6.entrySet()) {
            Shower s = imo2027.get(e.getKey());
            double anchor = s == null ? 0.0 : s.solarLongitude();
            e.getValue().sort(Comparator.comparingDouble(
                    r -> signed(r.solarLongitude() - anchor)));
        }
    }

    private void readMdc(Path file) throws IOException {
        // The file is UTF-8 with a few stray Latin-1 bytes in its comment lines.
        String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        for (String line : text.split("\n")) {
            if (line.startsWith(":") && line.contains("Last update:")) {
                mdcLastUpdate = line.substring(line.indexOf("Last update:") + 12,
                        line.indexOf("Modified")).trim();
            }
            if (!line.startsWith("\"")) {
                continue;
            }
            String[] f = line.split("\\|");
            for (int i = 0; i < f.length; i++) {
                f[i] = f[i].trim().replace("\"", "").trim();
            }
            try {
                mdc.computeIfAbsent(f[3], k -> new ArrayList<>()).add(new MdcSolution(f[3],
                        f[4], f[6], f[7], Double.parseDouble(f[10]), Double.parseDouble(f[11]),
                        Double.parseDouble(f[12]), number(f[13]), number(f[14]), f[31]));
            } catch (NumberFormatException e) {
                // a solution with no radiant or no solar longitude compares with nothing
            }
        }
    }

    private static Double number(String s) {
        try {
            return s.isEmpty() ? null : Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void readGmn(Path file) throws IOException {
        for (String line : Files.readAllLines(file, StandardCharsets.US_ASCII)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            double[] v = new double[5];
            for (int i = 0; i < 5; i++) {
                v[i] = f[i].equals("nan") ? Double.NaN : Double.parseDouble(f[i]);
            }
            if (Double.isNaN(v[1]) || Double.isNaN(v[2])) {
                continue; // a sample with no radiant compares with nothing
            }
            gmn.computeIfAbsent(f[6], k -> new ArrayList<>()).add(
                    new GmnSample(v[0], v[1], v[2], v[3], v[4]));
        }
    }

    // ------------------------------------------------------------------ the Sun

    /**
     * The Sun's ecliptic longitude from the fixed J2000 equinox at an
     * instant, from the production Sun (DE440), read at latitude 0,
     * longitude 0: the atlas has no geocentric form, and the place
     * moves the value by at most the Sun's parallax, 8.8 arcseconds.
     */
    double solarLongitude(Instant instant) {
        return ((SolarSystemService.SunObservation) solar.observe(SolarSystemService.Body.SUN,
                new Observer(0.0, 0.0, instant))).eclipticLongitudeJ2000Degrees();
    }

    /** The instant near a guess at which the Sun reaches a solar longitude, to the second. */
    Instant whenSolarLongitude(double target, Instant guess) {
        long t = guess.getEpochSecond();
        for (int i = 0; i < 12; i++) {
            double miss = signed(target - solarLongitude(Instant.ofEpochSecond(t)));
            long step = Math.round(miss / 0.985647 * 86400.0);
            t += step;
            if (step == 0) {
                break;
            }
        }
        return Instant.ofEpochSecond(t);
    }

    // ------------------------------------------------------------------ the radiant

    /** How an evaluation was reached, so no value hides its footing. */
    enum Footing {
        /** Between two printed rows. */
        BETWEEN_ROWS,
        /** Beyond the first or last printed row, carried on along the nearest pair. */
        BEYOND_ROWS,
        /** One printed position and no drift: held where it is printed. */
        FIXED
    }

    record Radiant(SkyPosition at, Footing footing, double daysBeyondRows) {
    }

    /**
     * The measured candidate rule: the IMO radiant table read against
     * solar longitude, great-circle interpolation between printed rows,
     * the nearest pair carried on beyond them, and a shower with fewer
     * than two rows held at its printed position.
     */
    Radiant radiantAt(String code, double solarLongitude) {
        List<Row> rows = table6.getOrDefault(code, List.of());
        Shower s = imo2027.get(code);
        if (rows.size() < 2) {
            return new Radiant(rows.isEmpty() ? s.radiantAtMaximum() : rows.get(0).at(),
                    Footing.FIXED, 0.0);
        }
        double anchor = s.solarLongitude();
        double x = signed(solarLongitude - anchor);
        int i = 0;
        while (i < rows.size() - 2 && signed(rows.get(i + 1).solarLongitude() - anchor) < x) {
            i++;
        }
        Row a = rows.get(i);
        Row b = rows.get(i + 1);
        double xa = signed(a.solarLongitude() - anchor);
        double xb = signed(b.solarLongitude() - anchor);
        double f = (x - xa) / (xb - xa);
        Footing footing = f < 0.0 || f > 1.0 ? Footing.BEYOND_ROWS : Footing.BETWEEN_ROWS;
        double beyond = f < 0.0 ? xa - x : f > 1.0 ? x - xb : 0.0;
        return new Radiant(along(a.at(), b.at(), f), footing, beyond / 0.985647);
    }

    /** The point a fraction of the way along the great circle from a to b (beyond it when outside 0..1). */
    static SkyPosition along(SkyPosition a, SkyPosition b, double f) {
        double[] va = SkyFrame.toVector(a);
        double[] vb = SkyFrame.toVector(b);
        double omega = Math.toRadians(a.separationDegrees(b));
        if (omega < 1e-12) {
            return a;
        }
        double sa = Math.sin((1.0 - f) * omega) / Math.sin(omega);
        double sb = Math.sin(f * omega) / Math.sin(omega);
        return SkyFrame.toPosition(new double[] {sa * va[0] + sb * vb[0],
                sa * va[1] + sb * vb[1], sa * va[2] + sb * vb[2]});
    }

    /** The comparison rule: the listed maximum carried at a fixed Sun-centred ecliptic position. */
    SkyPosition fixedSunCentred(Shower s, double solarLongitude) {
        Ecliptic.Coordinates c = Ecliptic.toEcliptic(s.radiantAtMaximum());
        return Ecliptic.toEquatorial(SkyFrame.normalise(
                c.longitudeDegrees() + solarLongitude - s.solarLongitude()), c.latitudeDegrees());
    }

    /** The GMN sample at a solar longitude, or null when the table has none within 0.11 degrees. */
    GmnSample gmnAt(String code, double solarLongitude) {
        GmnSample best = null;
        for (GmnSample g : gmn.getOrDefault(code, List.of())) {
            if (best == null || Math.abs(signed(g.solarLongitude() - solarLongitude))
                    < Math.abs(signed(best.solarLongitude() - solarLongitude))) {
                best = g;
            }
        }
        return best != null && Math.abs(signed(best.solarLongitude() - solarLongitude)) <= 0.11
                ? best : null;
    }

    /** Whether a solar longitude lies inside a shower's activity as dated in a year. */
    boolean active(Shower s, Instant instant) {
        int year = instant.atZone(ZoneOffset.UTC).getYear();
        for (int y = year - 1; y <= year; y++) {
            Instant from = s.start().in(y);
            Instant to = s.end().in(s.end().month() < s.start().month() ? y + 1 : y)
                    .plusSeconds(86400);
            if (!instant.isBefore(from) && instant.isBefore(to)) {
                return true;
            }
        }
        return false;
    }

    static double signed(double degrees) {
        double d = degrees % 360.0;
        return d > 180.0 ? d - 360.0 : d <= -180.0 ? d + 360.0 : d;
    }

    static String f(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
