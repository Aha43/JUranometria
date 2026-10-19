package juranometria.ui.solar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import juranometria.solar.SaturnSystemService.EastWest;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.NorthSouth;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.solar.time.TimeScales;

/**
 * The seven ruled moons as rows (issue #576, the table ruled on #574):
 * Mimas, Enceladus, Tethys, Dione, Rhea, Titan and Iapetus in that fixed
 * order, outward from Saturn - the moon, its side, the east–west and
 * north–south offsets with their letters, the separation in arcseconds
 * and in Saturn radii, and the state in words, Jupiter's columns. For a
 * range the rows are grouped by instant, and each group is one Saturn
 * result and its seven moons: a heading line naming the instant, three
 * lines of Saturn's summary - its position, its figure and its rings,
 * the card's eleven values labelled, so Saturn's own summary is never
 * left out - and the seven moon rows beneath.
 *
 * <p>The model holds what it was given and computes nothing: every
 * number is the service's, rounded; every word is the language's.
 */
public final class SaturnMoonsModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** The columns, by key suffix under {@code column.}. */
    public static final List<String> COLUMNS = List.of("moon", "side", "eastwest",
            "northsouth", "separation", "radii", "state");

    /** Preferred widths, in pixels, one per column. */
    static final int[] WIDTHS = {90, 52, 84, 92, 134, 104, 196};

    /** The key of each state's words, in the enum's order. */
    static final List<String> STATE_KEYS = List.of("state.behindShadow",
            "state.behind", "state.inFront", "state.shadow", "state.partlyShadow",
            "state.clear");

    /** What a line is. */
    public enum Kind {
        /** A range group's instant. */
        HEADING,
        /** Saturn at the instant: position and height. */
        SATURN_POSITION,
        /** Saturn at the instant: distance and figure. */
        SATURN_FIGURE,
        /** Saturn at the instant: its rings. */
        SATURN_RINGS,
        /** One moon. */
        MOON
    }

    /** One line: an instant's heading, a line of Saturn's summary, or a moon. */
    public record Line(Kind kind, String text, MoonPlace moon, SaturnObservation saturn) {

        /** Whether the line spans the table rather than filling its columns. */
        public boolean isHeading() {
            return kind != Kind.MOON;
        }

        /** The text a spanning line shows; null for a moon. */
        public String heading() {
            return text;
        }
    }

    private final SolarTableWords words;
    private List<Line> lines = List.of();

    public SaturnMoonsModel(SolarTableWords words) {
        if (words == null) {
            throw new IllegalArgumentException("a table says its words in some language");
        }
        this.words = words;
    }

    /** The words this model says. */
    public SolarTableWords words() {
        return words;
    }

    /** Shows entries: their moons alone for one instant, grouped under headings for many. */
    public void show(List<SaturnTableSession.Entry> entries) {
        List<Line> next = new ArrayList<>();
        boolean grouped = entries.size() > 1;
        for (SaturnTableSession.Entry entry : entries) {
            if (entry.configuration() == null) {
                continue;
            }
            if (grouped) {
                List<String> v = saturnValues(entry.saturn(), words);
                next.add(new Line(Kind.HEADING, heading(entry), null, entry.saturn()));
                next.add(new Line(Kind.SATURN_POSITION, words.say("range.saturn.position",
                        v.get(0), v.get(1), v.get(2), v.get(3)), null, entry.saturn()));
                next.add(new Line(Kind.SATURN_FIGURE, words.say("range.saturn.figure",
                        v.get(4), v.get(5), v.get(6), v.get(7)), null, entry.saturn()));
                next.add(new Line(Kind.SATURN_RINGS, words.say("range.saturn.rings",
                        v.get(8), v.get(9), v.get(10)), null, entry.saturn()));
            }
            for (MoonPlace moon : entry.configuration().moons()) {
                next.add(new Line(Kind.MOON, null, moon, entry.saturn()));
            }
        }
        lines = List.copyOf(next);
        fireTableDataChanged();
    }

    /** The heading an instant is shown under: the minute, its marks. */
    String heading(SaturnTableSession.Entry entry) {
        StringBuilder text = new StringBuilder(words.say("instant.pattern",
                SunTableFormat.minute(entry.sample().instant())));
        if (entry.sample().appendedEnd()) {
            text.append(' ').append(words.say("appended"));
        }
        if (entry.saturn().timeConfidence() != TimeScales.Confidence.EXACT) {
            text.append(' ').append(words.say("estimated"));
        }
        return text.toString();
    }

    /** The lines shown, in order. */
    public List<Line> lines() {
        return Collections.unmodifiableList(lines);
    }

    /** How many moon rows, headings aside. */
    public int moonRows() {
        return (int) lines.stream().filter(l -> !l.isHeading()).count();
    }

    @Override
    public int getRowCount() {
        return lines.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.size();
    }

    @Override
    public String getColumnName(int column) {
        return words.say("column." + COLUMNS.get(column));
    }

    /** What a column means, for its header cell and a screen reader. */
    public String columnExplanation(int column) {
        return words.say("column." + COLUMNS.get(column) + ".explain");
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Line line = lines.get(row);
        if (line.isHeading()) {
            return column == 0 ? line.heading() : "";
        }
        return cell(line.moon(), column, words);
    }

    /** One moon's cell, spelled for the reader. */
    public static String cell(MoonPlace m, int column, SolarTableWords w) {
        return switch (column) {
            case 0 -> moonName(m.moon(), w);
            case 1 -> w.say(m.eastWest() == EastWest.EAST ? "side.east" : "side.west");
            case 2 -> w.n(JovianTableFormat.offset(m.xArcseconds(),
                    w.say(m.eastWest() == EastWest.EAST ? "letter.east" : "letter.west")));
            case 3 -> w.n(JovianTableFormat.offset(m.yArcseconds(),
                    w.say(m.northSouth() == NorthSouth.NORTH ? "letter.north" : "letter.south")));
            case 4 -> w.n(JovianTableFormat.arcseconds(m.separationArcseconds()));
            case 5 -> w.n(JovianTableFormat.radii(m.separationSaturnRadii()));
            case 6 -> stateWords(m.state(), w);
            default -> throw new IndexOutOfBoundsException(column);
        };
    }

    /**
     * Saturn's eleven summary values, spelled for the reader - the card's
     * lines in {@link SaturnTableControls#CARD_LINES} order, and the same
     * values a range group shows: right ascension, declination, altitude,
     * azimuth, distance, diameters, illuminated fraction, the pole's
     * position angle, the rings' opening to the observer and to the Sun
     * (each with its face, the second naming the unlit face when it is
     * seen), and the rings' apparent size - the A Ring's outer edge
     * across its major and minor axes.
     */
    public static List<String> saturnValues(SaturnObservation s, SolarTableWords w) {
        double outer = s.ringRadiiKm().get(s.ringRadiiKm().size() - 1);
        String sun = face(s.sunRingOpeningDegrees(), w);
        if (s.unlitFaceSeen()) {
            sun = w.say("face.unlit", sun);
        }
        return List.of(
                w.n(SunTableFormat.hms(s.astrometricJ2000())),
                SunTableFormat.dms(s.astrometricJ2000()),
                SunTableFormat.altitude(w.n(JovianTableFormat.tenthDegree(
                        s.horizontal().altitudeDegrees())),
                        s.horizontal().altitudeDegrees(), w.say("below")),
                w.n(JovianTableFormat.tenthDegree(s.horizontal().azimuthDegrees())),
                w.n(JovianTableFormat.astronomicalUnits(s.distanceAu())),
                w.n(JovianTableFormat.diameters(s.equatorialDiameterArcseconds(),
                        s.polarDiameterArcseconds())),
                w.n(MoonTableFormat.percent(s.illuminatedFraction())),
                w.n(JovianTableFormat.tenthDegree(s.poleAngleDegrees())),
                face(s.ringOpeningDegrees(), w),
                sun,
                w.n(SaturnTableFormat.ringSize(2.0 * s.ringSemiMajorArcseconds(outer),
                        2.0 * s.ringSemiMinorArcseconds(outer))));
    }

    /** An opening with its face: north, south or edge-on. */
    static String face(double degrees, SolarTableWords w) {
        String size = w.n(SaturnTableFormat.opening(degrees));
        if (SaturnTableFormat.edgeOn(degrees)) {
            return w.say("face.edgeOn", size);
        }
        return w.say(degrees > 0.0 ? "face.north" : "face.south", size);
    }

    /** How many instants the lines group (a range's headings). */
    public int groups() {
        return (int) lines.stream().filter(l -> l.kind() == Kind.HEADING).count();
    }

    /** A moon's name in the language. */
    public static String moonName(Moon moon, SolarTableWords w) {
        return w.say("moon." + moon.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** A state in the language's words. */
    public static String stateWords(VisibilityState state, SolarTableWords w) {
        return w.say(STATE_KEYS.get(state.ordinal()));
    }
}
