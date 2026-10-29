package juranometria.ui.solar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import juranometria.solar.NeptuneService;
import juranometria.solar.NeptuneService.EastWest;
import juranometria.solar.NeptuneService.Moon;
import juranometria.solar.NeptuneService.MoonPlace;
import juranometria.solar.NeptuneService.NorthSouth;
import juranometria.solar.NeptuneService.NeptuneObservation;
import juranometria.solar.NeptuneService.VisibilityState;
import juranometria.solar.time.TimeScales;

/**
 * Triton and Nereid as rows (issue #656, the
 * table ruled on #654, N-9), in that fixed order, outward from Neptune -
 * the moon, its side, the east–west and north–south offsets with their
 * letters, the separation in arcseconds and in Neptune radii, and the state
 * in words, Jupiter's columns. For a range the rows are grouped by
 * instant, and each group is one Neptune result and its moons: a heading
 * line naming the instant, three lines of Neptune's summary - its
 * position, its figure, and its light and tilt, the card's ten values
 * labelled, so Neptune's own summary is never left out - and the two moon
 * rows beneath, or, where the moons do not answer (outside 2000 to 2100),
 * one line saying so.
 *
 * <p>The model holds what it was given and computes nothing: every
 * number is the service's, rounded; every word is the language's.
 */
public final class NeptuneMoonsModel extends AbstractTableModel {

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
        /** Neptune at the instant: position and height. */
        NEPTUNE_POSITION,
        /** Neptune at the instant: distance, figure and pole. */
        NEPTUNE_FIGURE,
        /** Neptune at the instant: illuminated fraction, phase angle and tilt. */
        NEPTUNE_LIGHT,
        /** The moons do not answer at this instant: said, not left out. */
        MOONS_REFUSED,
        /** One moon. */
        MOON
    }

    /** One line: an instant's heading, a line of Neptune's summary, a refusal, or a moon. */
    public record Line(Kind kind, String text, MoonPlace moon, NeptuneObservation neptune) {

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

    public NeptuneMoonsModel(SolarTableWords words) {
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
    public void show(List<NeptuneTableSession.Entry> entries) {
        List<Line> next = new ArrayList<>();
        boolean grouped = entries.size() > 1;
        for (NeptuneTableSession.Entry entry : entries) {
            if (grouped) {
                List<String> v = neptuneValues(entry.neptune(), words);
                next.add(new Line(Kind.HEADING, heading(entry), null, entry.neptune()));
                next.add(new Line(Kind.NEPTUNE_POSITION, words.say("range.neptune.position",
                        v.get(0), v.get(1), v.get(2), v.get(3)), null, entry.neptune()));
                next.add(new Line(Kind.NEPTUNE_FIGURE, words.say("range.neptune.figure",
                        v.get(4), v.get(5), v.get(8)), null, entry.neptune()));
                next.add(new Line(Kind.NEPTUNE_LIGHT, words.say("range.neptune.light",
                        v.get(6), v.get(7), v.get(9)), null, entry.neptune()));
                if (entry.configuration() == null) {
                    next.add(new Line(Kind.MOONS_REFUSED, words.say("range.moons.refused",
                            NeptuneService.MOONS_FIRST_DAY.toString(),
                            NeptuneService.MOONS_LAST_DAY.toString()), null, entry.neptune()));
                }
            }
            if (entry.configuration() == null) {
                continue;
            }
            for (MoonPlace moon : entry.configuration().moons()) {
                next.add(new Line(Kind.MOON, null, moon, entry.neptune()));
            }
        }
        lines = List.copyOf(next);
        fireTableDataChanged();
    }

    /** The heading an instant is shown under: the minute, its marks. */
    String heading(NeptuneTableSession.Entry entry) {
        StringBuilder text = new StringBuilder(words.say("instant.pattern",
                SunTableFormat.minute(entry.sample().instant())));
        if (entry.sample().appendedEnd()) {
            text.append(' ').append(words.say("appended"));
        }
        if (entry.neptune().timeConfidence() != TimeScales.Confidence.EXACT) {
            text.append(' ').append(words.say("estimated"));
        }
        return text.toString();
    }

    /** The lines shown, in order. */
    public List<Line> lines() {
        return Collections.unmodifiableList(lines);
    }

    /** How many moon rows, headings, summaries and refusals aside. */
    public int moonRows() {
        return (int) lines.stream().filter(l -> l.kind() == Kind.MOON).count();
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
            case 5 -> w.n(JovianTableFormat.radii(m.separationNeptuneRadii()));
            case 6 -> stateWords(m.state(), w);
            default -> throw new IndexOutOfBoundsException(column);
        };
    }

    /**
     * Neptune's ten summary values, spelled for the reader - the card's
     * lines in {@link NeptuneTableControls#CARD_LINES} order, and the same
     * values a range group shows: right ascension, declination, altitude,
     * azimuth, distance, diameters, illuminated fraction, the phase angle
     * (N-5: no phase words), the pole's position angle, and the tilt with
     * the pole tipped toward the observer (Mars's, N-9).
     */
    public static List<String> neptuneValues(NeptuneObservation u, SolarTableWords w) {
        return List.of(
                w.n(SunTableFormat.hms(u.astrometricJ2000())),
                SunTableFormat.dms(u.astrometricJ2000()),
                SunTableFormat.altitude(w.n(JovianTableFormat.tenthDegree(
                        u.horizontal().altitudeDegrees())),
                        u.horizontal().altitudeDegrees(), w.say("below")),
                w.n(JovianTableFormat.tenthDegree(u.horizontal().azimuthDegrees())),
                w.n(JovianTableFormat.astronomicalUnits(u.distanceAu())),
                w.n(JovianTableFormat.diameters(u.equatorialDiameterArcseconds(),
                        u.polarDiameterArcseconds())),
                w.n(MoonTableFormat.percent(u.illuminatedFraction())),
                w.n(NeptuneTableFormat.phaseAngle(u.phaseAngleDegrees())),
                w.n(JovianTableFormat.tenthDegree(u.poleAngleDegrees())),
                w.say(u.northPoleTowardObserver() ? "tilt.north" : "tilt.south",
                        w.n(NeptuneTableFormat.tilt(u.tiltDegrees()))));
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
