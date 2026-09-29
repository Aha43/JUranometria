package juranometria.ui.solar;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Observation;
import juranometria.solar.TimeRange;
import juranometria.solar.time.TimeScales;
import juranometria.ui.language.InterfaceText;

/**
 * A body's computed quantities as rows and columns (issue #400 for the
 * Sun, #408 for the Moon): one row per instant, the columns the body's
 * contract froze, every cell a string already spelled in
 * {@code Locale.ROOT} with the language's own words only where words
 * belong - a status below the horizon, a phase, the marks on an instant.
 *
 * <p>The model holds rows it was given and computes nothing: what a
 * row says about the body is the service's answer, and what a column
 * is called is the language's. The shape - rows over instants, a mark
 * for an appended end, a mark for an estimated clock - is the same for
 * every body; what differs is the {@link SolarTable} it is built over.
 */
public final class SolarTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** One row: the sample it answers and the answer. */
    public record Row(TimeRange.Sample sample, Observation observation) {
    }

    private final SolarTable table;
    private final SolarTableWords words;
    private List<Row> rows = List.of();

    public SolarTableModel(InterfaceText said, SolarTable table) {
        if (said == null || table == null) {
            throw new IllegalArgumentException("a table says its words in"
                    + " some language, for some body");
        }
        this.table = table;
        this.words = new SolarTableWords(said, table.stem());
    }

    /** The body this model is over. */
    public SolarTable table() {
        return table;
    }

    /** The words, body first. */
    public SolarTableWords words() {
        return words;
    }

    /** The language's decimal separator for what the reader sees. */
    public String decimal() {
        return words.decimal();
    }

    /** Replaces every row. */
    public void show(List<Row> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    /** Replaces every row with the service's rows for a range. */
    public void show(List<SolarSystemService.Row> answered, boolean fromService) {
        this.rows = answered.stream().map(r -> new Row(r.sample(),
                r.observation())).toList();
        fireTableDataChanged();
    }

    /** The rows shown, in order. */
    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }

    /** The observation behind a row. */
    public Observation observationAt(int row) {
        return rows.get(row).observation();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return table.columns().size();
    }

    @Override
    public String getColumnName(int column) {
        return words.say("column." + table.columns().get(column));
    }

    /** What a column means, for its header cell and a screen reader. */
    public String columnExplanation(int column) {
        String suffix = "column." + table.columns().get(column) + ".explain";
        return column == 0
                ? words.say(suffix, words.say("appended"))
                : words.say(suffix);
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
        return column == 0 ? instant(r)
                : table.cell(r.observation(), column, words);
    }

    private String instant(Row r) {
        StringBuilder text = new StringBuilder(
                SunTableFormat.minute(r.observation().instant()));
        if (r.sample().appendedEnd()) {
            text.append(' ').append(words.say("appended"));
        }
        if (r.observation().timeConfidence() != TimeScales.Confidence.EXACT) {
            text.append(' ').append(words.say("estimated"));
        }
        return text.toString();
    }
}
