package juranometria.ui.solar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import juranometria.solar.JovianSystemService.EastWest;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.NorthSouth;
import juranometria.solar.JovianSystemService.VisibilityState;
import juranometria.solar.time.TimeScales;

/**
 * The four Galilean moons as rows (issue #474, the table ruled on
 * #472): Io, Europa, Ganymede and Callisto in that fixed order - the
 * moon, its side, the east–west and north–south offsets with their
 * letters, the separation in arcseconds and in Jupiter radii, and the
 * state in words. For a range the rows are grouped by instant, and
 * each group is one Jupiter result and its four moons (the owner's
 * checkpoint finding on #474): a heading line naming the instant, two
 * lines of Jupiter's summary - the card's eight values, labelled - and
 * the four moon rows beneath.
 *
 * <p>The model holds what it was given and computes nothing: every
 * number is the service's, rounded; every word is the language's.
 */
public final class JovianMoonsModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** The columns, by key suffix under {@code column.}. */
    public static final List<String> COLUMNS = List.of("moon", "side", "eastwest",
            "northsouth", "separation", "radii", "state");

    /** Preferred widths, in pixels, one per column. */
    static final int[] WIDTHS = {84, 52, 84, 92, 134, 104, 196};

    /** The key of each state's words, in the enum's order. */
    static final List<String> STATE_KEYS = List.of("state.behindShadow",
            "state.behind", "state.inFront", "state.shadow", "state.partlyShadow",
            "state.clear");

    /** What a line is. */
    public enum Kind {
        /** A range group's instant. */
        HEADING,
        /** Jupiter at the instant: position and height. */
        JUPITER_POSITION,
        /** Jupiter at the instant: distance and figure. */
        JUPITER_FIGURE,
        /** One moon. */
        MOON
    }

    /** One line: an instant's heading, a line of Jupiter's summary, or a moon. */
    public record Line(Kind kind, String text, MoonPlace moon,
                       juranometria.solar.JovianSystemService.JupiterObservation jupiter) {

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

    public JovianMoonsModel(SolarTableWords words) {
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
    public void show(List<JovianTableSession.Entry> entries) {
        List<Line> next = new ArrayList<>();
        boolean grouped = entries.size() > 1;
        for (JovianTableSession.Entry entry : entries) {
            if (entry.configuration() == null) {
                continue;
            }
            if (grouped) {
                List<String> v = jupiterValues(entry.jupiter(), words);
                next.add(new Line(Kind.HEADING, heading(entry), null, entry.jupiter()));
                next.add(new Line(Kind.JUPITER_POSITION, words.say("range.jupiter.position",
                        v.get(0), v.get(1), v.get(2), v.get(3)), null, entry.jupiter()));
                next.add(new Line(Kind.JUPITER_FIGURE, words.say("range.jupiter.figure",
                        v.get(4), v.get(5), v.get(6), v.get(7)), null, entry.jupiter()));
            }
            for (MoonPlace moon : entry.configuration().moons()) {
                next.add(new Line(Kind.MOON, null, moon, entry.jupiter()));
            }
        }
        lines = List.copyOf(next);
        fireTableDataChanged();
    }

    /** The heading an instant is shown under: the minute, its marks. */
    String heading(JovianTableSession.Entry entry) {
        StringBuilder text = new StringBuilder(words.say("instant.pattern",
                SunTableFormat.minute(entry.sample().instant())));
        if (entry.sample().appendedEnd()) {
            text.append(' ').append(words.say("appended"));
        }
        if (entry.jupiter().timeConfidence() != TimeScales.Confidence.EXACT) {
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
            case 5 -> w.n(JovianTableFormat.radii(m.separationJupiterRadii()));
            case 6 -> stateWords(m.state(), w);
            default -> throw new IndexOutOfBoundsException(column);
        };
    }

    /**
     * Jupiter's eight summary values, spelled for the reader - the
     * card's lines in {@link JovianTableControls#CARD_LINES} order, and
     * the same values a range group shows: right ascension, declination,
     * altitude, azimuth, distance, diameters, illuminated fraction, the
     * pole's position angle.
     */
    public static List<String> jupiterValues(
            juranometria.solar.JovianSystemService.JupiterObservation j, SolarTableWords w) {
        return List.of(
                w.n(SunTableFormat.hms(j.astrometricJ2000())),
                SunTableFormat.dms(j.astrometricJ2000()),
                SunTableFormat.altitude(w.n(JovianTableFormat.tenthDegree(
                        j.horizontal().altitudeDegrees())),
                        j.horizontal().altitudeDegrees(), w.say("below")),
                w.n(JovianTableFormat.tenthDegree(j.horizontal().azimuthDegrees())),
                w.n(JovianTableFormat.astronomicalUnits(j.distanceAu())),
                w.n(JovianTableFormat.diameters(j.equatorialDiameterArcseconds(),
                        j.polarDiameterArcseconds())),
                w.n(MoonTableFormat.percent(j.illuminatedFraction())),
                w.n(JovianTableFormat.tenthDegree(j.poleAngleDegrees())));
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
