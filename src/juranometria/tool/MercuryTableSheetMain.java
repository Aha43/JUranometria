package juranometria.tool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.solar.SolarTable;
import juranometria.ui.solar.SolarTableDialog;

/**
 * Every word the Mercury table says, photographed (Sprint 49, issue #530).
 *
 * <p>The production dialog, built through
 * {@code SolarTableDialog.packedForStudy} over Mercury's columns, a stated
 * observer and the bundled packs, in both languages, in the states a
 * reader meets: the single instant at the 2026 greatest eastern
 * elongation, a daily range through the 2026 inferior conjunction with
 * its appended end, a range refused for leaving the interval, and the
 * 2019 transit with the lit side not well-defined; one dark capture per
 * language. The companion records the window's own two channels and
 * every shown, hovered and spoken string, plus the column explanations a
 * screen reader hears from the headings. Venus's sheet is its twin.
 */
public final class MercuryTableSheetMain {

    public static final SheetCapture.Kind CAPTURE_KIND =
            SheetCapture.Kind.PACKED;

    private static Path out = Path.of("docs/studies/interface-language");

    /** @param span the range's span index, @param step its step index (a range from the chart time, #612) */
    private record State(String name, String title, Observer observer,
                         boolean range, int span, int step, String note,
                         boolean stale) {

        /** An instant state: no range. */
        State(String name, String title, Observer observer, String note) {
            this(name, title, observer, false, 1, 2, note, false);
        }
    }

    /** Espenak's 2026 greatest eastern elongation, to the hour (mercury-events.txt). */
    private static final Observer ELONGATION = new Observer(59.91, 10.75,
            Instant.parse("2026-02-19T18:00:00Z"));

    private static final List<State> STATES = List.of(
            new State("instant", "One instant: Oslo at the 2026 greatest eastern elongation",
                    ELONGATION,
                    "The fresh state: a single row at the instant Place and Time holds -"
                            + " Espenak's hour of the greatest eastern elongation. Mercury is near"
                            + " half lit, east of the Sun in the evening sky, its lit side towards"
                            + " the Sun in the west-southwest."),
            new State("range", "A daily range through the 2026 inferior conjunction",
                    new Observer(59.91, 10.75, Instant.parse("2026-03-03T11:00:00Z")),
                    true, 1, 2,
                    "A week of daily rows from the chart time, the first row the chart"
                            + " time itself. The disc is at its largest and least lit, and"
                            + " the side of the Sun changes from E to W - by right ascension, so"
                            + " before the conjunction a reader may be told of; the table names"
                            + " no conjunction. Mercury's apparition turns in days where Venus's"
                            + " takes weeks.", false),
            new State("refused", "A range that leaves the interval",
                    new Observer(59.91, 10.75, Instant.parse("2100-12-28T00:00:00Z")),
                    true, 1, 2,
                    "A week from the chart time ends after the last civil day Mercury is"
                            + " computed for. The table is empty and the status line says why,"
                            + " naming the interval; nothing is extrapolated.", false),
            new State("transit", "Oslo during the 2019 transit",
                    new Observer(59.91, 10.75, Instant.parse("2019-11-11T15:20:00Z")),
                    "The NASA/GSFC catalogue's minute of greatest transit, while Mercury crossed"
                            + " the Sun's disc: the phase reads in front of the Sun, and the lit"
                            + " side is stated as not well-defined rather than as a false angle;"
                            + " from Oslo the Sun had just set."),
            new State("stale", "The chart moved on after computing", ELONGATION,
                    false, 1, 2,
                    "The chart time moved six hours on after this instant was"
                            + " computed (#612, ruling P1): the rows stay, dimmed,"
                            + " under the stale message, until Compute; the note"
                            + " states the chart time and place the rows are for.",
                    true));

    private MercuryTableSheetMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && !args[0].isBlank()) {
            out = Path.of(args[0]);
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("a packed dialog needs a display");
            System.exit(1);
        }
        Files.createDirectories(out);
        SolarSystemService service = SolarSystemService.load();
        StringBuilder said = new StringBuilder();
        said.append("""
                # Every word the Mercury table says

                Issue #530. Generated by `juranometria.tool.MercuryTableSheetMain`
                from compiled application classes and their classpath
                resources, over the bundled Solar System and planets packs.
                Packaged acceptance separately verifies that the packs and the
                interface language resources ship in the application image.

                %s

                ## The window, not a panel

                Each capture is the production dialog built through
                `SolarTableDialog.packedForStudy` over Mercury's columns,
                which has no "English if omitted" overload. The title and
                the window description are read from the `JDialog` itself;
                every shown, hovered and spoken string is walked from the
                content; the column headings' explanations, which a screen
                reader hears and a hover shows, are listed from the model.

                ## What is translated here, and what is not

                Every number is notation: right ascension, declination,
                degrees, astronomical units and million kilometres, the
                solid body's diameter, the percentage and the instant are
                formatted in `Locale.ROOT` by `SunTableFormat` and
                `MoonTableFormat` and are identical in both languages,
                with the language's own decimal separator. The words are the
                language's: the headings, the status line, the refusals, the
                status below the horizon, the phase, the side of the Sun and
                the compass point of the lit side. The shell's words are the
                same keys the Sun and Moon tables say (`solartable.*`, the
                compass points and the side letters among them); Mercury's own
                are `mercurytable.*`, the phase words ruled on #528 - the
                bands Venus's table uses, as ruled.

                """.formatted(InterfaceLanguageStatus.statement("nb-NO")));
        int sheet = 1;
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText text = InterfaceText.forLanguage(language);
            said.append("## ").append(language.equals("en") ? "English"
                    : "Norsk bokmål").append(" (`").append(language)
                    .append("`)\n\n");
            for (State state : STATES) {
                said.append("### ").append(state.title()).append("\n\n")
                        .append(state.note()).append("\n\n")
                        .append(draw(text, service, state, false,
                                out.resolve("mercurytable-" + language + "-"
                                        + (sheet++) + "-" + state.name()
                                        + ".png")))
                        .append('\n');
            }
            said.append("### The instant, in the dark appearance\n\n")
                    .append(draw(text, service, STATES.get(0), true,
                            out.resolve("mercurytable-" + language + "-"
                                    + (sheet++) + "-instant-dark.png")))
                    .append('\n');
            said.append(columns(text));
        }
        Files.writeString(out.resolve("mercurytable-strings.md"),
                said.toString(), StandardCharsets.UTF_8);
        System.out.println("mercury table sheets: " + (sheet - 1) + " images and "
                + out.resolve("mercurytable-strings.md"));
    }

    /** What each heading says to a screen reader, per language. */
    private static String columns(InterfaceText said) {
        StringBuilder out = new StringBuilder(
                "#### The headings, and what they explain\n\n"
                        + "| heading | explanation |\n|---|---|\n");
        juranometria.ui.solar.SolarTableModel model =
                new juranometria.ui.solar.SolarTableModel(said, SolarTable.mercury());
        for (int c = 0; c < model.getColumnCount(); c++) {
            out.append("| ").append(model.getColumnName(c)).append(" | ")
                    .append(model.columnExplanation(c).replace("|", "\\|"))
                    .append(" |\n");
        }
        return out.append('\n').toString();
    }

    private static String draw(InterfaceText said, SolarSystemService service,
                               State state, boolean dark, Path to)
            throws Exception {
        JFrame[] owner = new JFrame[1];
        SolarTableDialog[] dialog = new SolarTableDialog[1];
        SheetCapture.tracing(to.getFileName() + " state=" + state.name());
        try {
            SwingUtilities.invokeAndWait(() -> {
                juranometria.app.UiTheme.apply(dark);
                owner[0] = new JFrame("study");
                // Over the chart's holder (#612): the row action is offered,
                // and a state may move the chart on after computing.
                juranometria.context.ChartContextHolder chart =
                        new juranometria.context.ChartContextHolder(state.observer());
                juranometria.ui.solar.SolarTableSession session =
                        new juranometria.ui.solar.SolarTableSession(chart::observer, service,
                                SolarTable.mercury());
                session.followContext(chart);
                if (state.stale()) {
                    // Computed, then the chart moved on - before the dialog
                    // packs, so it packs once, at its final size.
                    session.apply(juranometria.ui.solar.SolarTableSession.Query.instant());
                    chart.install(state.observer().at(
                            state.observer().instant().plus(java.time.Duration.ofHours(6))));
                }
                dialog[0] = SolarTableDialog.packedForStudy(owner[0], session, said);
                if (state.range()) {
                    dialog[0].content().rangeView.setSelected(true);
                    dialog[0].content().span.setSelectedIndex(state.span());
                    dialog[0].content().step.setSelectedIndex(state.step());
                    dialog[0].content().update();
                }
            });
            return capture(dialog[0], state, to);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) {
                    dialog[0].dispose();
                }
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    /** The state this sheet claims, asked inside the coordinator's block. */
    private static String disagreement(SolarTableDialog dialog, State state) {
        SolarTableDialog.Content content = dialog.content();
        if (content.rangeView.isSelected() != state.range()) {
            return "the view is " + (content.rangeView.isSelected()
                    ? "the range" : "the instant") + ", not the state's";
        }
        if (state.range() && content.span.getSelectedIndex() != state.span()) {
            return "the span is " + content.span.getSelectedItem() + ", not the state's";
        }
        if (content.controls().stale() != state.stale()) {
            return "the table is " + (content.controls().stale() ? "stale" : "current")
                    + ", not the state's";
        }
        return null;
    }

    private static String capture(SolarTableDialog dialog, State state, Path to)
            throws Exception {
        Set<String> said = new LinkedHashSet<>();
        JComponent content = (JComponent) dialog.getContentPane();
        BufferedImage image = SheetCapture.take(dialog, content,
                SheetCapture.packed(),
                held -> {
                    String wrong = disagreement(dialog, state);
                    return wrong == null ? null
                            : "it claims \"" + state.title() + "\" and " + wrong;
                },
                () -> {
                    BufferedImage drawn = new BufferedImage(
                            Math.max(1, content.getWidth()),
                            Math.max(1, content.getHeight()),
                            BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = drawn.createGraphics();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, drawn.getWidth(), drawn.getHeight());
                        content.paint(g);
                    } finally {
                        g.dispose();
                    }
                    add(said, dialog.getTitle(), "");
                    add(said, dialog.getAccessibleContext()
                            .getAccessibleDescription(), "");
                    collect(content, said);
                    return drawn;
                });
        ImageIO.write(image, "png", to.toFile());
        StringBuilder out = new StringBuilder();
        out.append("![](").append(to.getFileName()).append(")\n\n")
                .append("Packed ").append(image.getWidth()).append(" × ")
                .append(image.getHeight()).append(" px.\n\n")
                .append("| shown, hovered or spoken |\n|---|\n");
        for (String one : said) {
            out.append("| ").append(one.replace("|", "\\|")).append(" |\n");
        }
        return out.toString();
    }

    private static void collect(Container from, Set<String> said) {
        if (from instanceof JComponent self) {
            record(self, said);
        }
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                record(widget, said);
            }
            if (child instanceof Container nested) {
                collect(nested, said);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void record(JComponent widget, Set<String> said) {
        String off = widget.isEnabled() ? "" : " [unavailable]";
        if (widget instanceof AbstractButton button) {
            add(said, button.getText(), off);
        }
        if (widget instanceof JLabel label) {
            add(said, label.getText(), off);
        }
        if (widget instanceof JComboBox) {
            JComboBox<Object> box = (JComboBox<Object>) widget;
            ListCellRenderer<Object> renderer =
                    (ListCellRenderer<Object>) box.getRenderer();
            JList<Object> list = new JList<>();
            for (int i = 0; i < box.getItemCount(); i++) {
                Component cell = renderer.getListCellRendererComponent(
                        list, box.getItemAt(i), i, false, false);
                if (cell instanceof JLabel label) {
                    add(said, label.getText(), off);
                }
            }
        }
        if (widget instanceof JTable table) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                add(said, table.getColumnName(c), off);
            }
            for (int r = 0; r < table.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < table.getColumnCount(); c++) {
                    row.append(c == 0 ? "" : " · ")
                            .append(table.getValueAt(r, c));
                }
                add(said, row.toString(), off);
            }
        }
        add(said, widget.getToolTipText(), off);
        if (widget.getAccessibleContext() != null) {
            add(said, widget.getAccessibleContext().getAccessibleName(), off);
            add(said, widget.getAccessibleContext().getAccessibleDescription(),
                    off);
        }
    }

    private static void add(Set<String> said, String text, String off) {
        if (text == null || text.isBlank()) {
            return;
        }
        String plain = text.replaceAll("<[^>]+>", "").replace('\n', ' ')
                .replaceAll("\\s+", " ").strip();
        if (!plain.isEmpty()) {
            said.add(plain + off);
        }
    }
}
