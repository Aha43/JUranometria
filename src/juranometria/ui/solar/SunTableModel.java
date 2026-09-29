package juranometria.ui.solar;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.language.InterfaceText;

/**
 * The Sun's computed quantities as rows and columns (issue #400): one
 * row per instant, the eight columns the contract froze, every cell a
 * string already spelled in {@code Locale.ROOT} with the language's
 * own words only where words belong - the status below the horizon,
 * the marks on an instant.
 *
 * <p>The model holds rows it was given and computes nothing: what a
 * row says about the Sun is the service's answer, and what a column
 * is called is the language's. A body other than the Sun would bring
 * its own observation type and its own columns; the shape - rows over
 * instants, a mark for an appended end, a mark for an estimated clock
 * - is the same.
 */
public final class SunTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** The columns, in order, by language key stem. */
    static final List<String> COLUMNS = List.of(
            "suntable.column.instant", "suntable.column.ra",
            "suntable.column.dec", "suntable.column.longitude",
            "suntable.column.altitude", "suntable.column.azimuth",
            "suntable.column.distance", "suntable.column.diameter");

    /** One row: the sample it answers and the answer. */
    public record Row(TimeRange.Sample sample, SunObservation observation) {
    }

    private final InterfaceText said;
    private final String decimal;
    private List<Row> rows = List.of();

    public SunTableModel(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException("a table says its words in"
                    + " some language");
        }
        this.said = said;
        this.decimal = said.say("suntable.decimal");
    }

    /** The language's decimal separator for what the reader sees. */
    public String decimal() {
        return decimal;
    }

    private String n(String rootNumber) {
        return SunTableFormat.decimal(rootNumber, decimal);
    }

    /** Replaces every row. */
    public void show(List<Row> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    /** Replaces every row with the service's rows for a range. */
    public void show(List<SolarSystemService.Row> answered, boolean fromService) {
        this.rows = answered.stream().map(r -> new Row(r.sample(),
                (SunObservation) r.observation())).toList();
        fireTableDataChanged();
    }

    /** The rows shown, in order. */
    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }

    /** The observation behind a row. */
    public SunObservation observationAt(int row) {
        return rows.get(row).observation();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.size();
    }

    @Override
    public String getColumnName(int column) {
        return said.say(COLUMNS.get(column));
    }

    /** What a column means, for its header cell and a screen reader. */
    public String columnExplanation(int column) {
        String stem = COLUMNS.get(column);
        return stem.equals("suntable.column.instant")
                ? said.say(stem + ".explain", said.say("suntable.appended"))
                : said.say(stem + ".explain");
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
        Row r = rows.get(row);
        SunObservation o = r.observation();
        return switch (column) {
            case 0 -> instant(r);
            case 1 -> n(SunTableFormat.hms(o.astrometricJ2000()));
            case 2 -> SunTableFormat.dms(o.astrometricJ2000());
            case 3 -> n(SunTableFormat.degrees(o.eclipticLongitudeJ2000Degrees()));
            case 4 -> SunTableFormat.altitude(
                    n(SunTableFormat.degrees(o.horizontal().altitudeDegrees())),
                    o.horizontal().altitudeDegrees(), said.say("suntable.below"));
            case 5 -> n(SunTableFormat.degrees(o.horizontal().azimuthDegrees()));
            case 6 -> said.say("suntable.distance.pattern",
                    n(SunTableFormat.astronomicalUnits(o.distanceAu())),
                    n(SunTableFormat.millionKilometres(o.distanceKm())));
            case 7 -> n(SunTableFormat.minutesSeconds(o.angularDiameterArcseconds()));
            default -> throw new IndexOutOfBoundsException(column);
        };
    }

    private String instant(Row r) {
        StringBuilder text = new StringBuilder(
                SunTableFormat.minute(r.observation().instant()));
        if (r.sample().appendedEnd()) {
            text.append(' ').append(said.say("suntable.appended"));
        }
        if (r.observation().timeConfidence() != TimeScales.Confidence.EXACT) {
            text.append(' ').append(said.say("suntable.estimated"));
        }
        return text.toString();
    }
}
