package juranometria.ui.bookmark;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.LibraryState;
import juranometria.bookmark.Node;
import juranometria.bookmark.Subject;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * Add Bookmark… (#681 stage 2; rulings of 2026-11-02 on #497): saves the
 * current view, with the objects, time and place the reader chooses, into
 * the bookmark library. Modal, like Export: the title, the folder, the view
 * line (the view is always saved), the subjects in the reader's order with
 * Move up, Move down and Remove, the main object (or none), the explicit
 * Include chart time and Include place boxes with their values written out,
 * the note, Cancel and Add. Enter adds; Escape, Cancel and the close box add
 * nothing.
 */
public final class AddBookmarkDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** The Add button's name, for journeys. */
    public static final String ADD = "bookmarkAdd";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private AddBookmarkDialog(Window owner, InterfaceText said, Content content) {
        super(owner, said.say("bookmark.add.title"), ModalityType.APPLICATION_MODAL);
        getAccessibleContext().setAccessibleName(said.say("bookmark.add.title"));
        getAccessibleContext().setAccessibleDescription(said.say("bookmark.add.explain"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        content.whenDone(this::dispose);
        setContentPane(content);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(content.add);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog over the chart window; adds only on the reader's Add. */
    public static void open(Window owner, InterfaceText said, BookmarkLibrary library,
                            BookmarkCapture.Proposal proposal, Function<Subject, String> nameOf,
                            String suggestedTitle) {
        new AddBookmarkDialog(owner, said,
                new Content(said, library, proposal, nameOf, suggestedTitle)).setVisible(true);
    }

    /** The dialog's content, constructible without a window for tests and photographs. */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        private final InterfaceText said;
        private final BookmarkLibrary library;
        private final BookmarkCapture.Proposal proposal;
        final JTextField title;
        final JComboBox<FolderChoice> folder;
        final DefaultListModel<Subject> subjects = new DefaultListModel<>();
        final JList<Subject> list;
        final JComboBox<Object> primary;
        final JCheckBox time;
        final JRadioButton event;
        final JRadioButton observation;
        final JCheckBox place;
        final JTextArea note;
        final JButton add;
        final JLabel status;
        private Runnable done = () -> { };

        public Content(InterfaceText said, BookmarkLibrary library, BookmarkCapture.Proposal proposal,
                       Function<Subject, String> nameOf, String suggestedTitle) {
            super(new BorderLayout(0, 10));
            this.said = said;
            this.library = library;
            this.proposal = proposal;
            MnemonicText letters = MnemonicText.in(said);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(3, 3, 3, 3);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            title = new JTextField(suggestedTitle, 28);
            labelled(form, c, "bookmark.add.name", title, letters);

            folder = new JComboBox<>(folders(library, said).toArray(new FolderChoice[0]));
            labelled(form, c, "bookmark.add.folder", folder, letters);

            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 2;
            JLabel view = new JLabel(said.say("bookmark.add.view",
                    juranometria.chart.SkyFormat.formatRa(proposal.view().raDegrees()) + ", "
                            + juranometria.chart.SkyFormat.formatDec(proposal.view().decDegrees()),
                    field(proposal.view().fieldWidthDegrees())));
            form.add(view, c);

            for (Subject s : proposal.subjects()) {
                subjects.addElement(s);
            }
            list = new JList<>(subjects);
            list.setVisibleRowCount(Math.max(3, Math.min(6, subjects.size())));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                              boolean selected, boolean focused) {
                    return super.getListCellRendererComponent(l, nameOf.apply((Subject) value),
                            index, selected, focused);
                }
            });
            list.getAccessibleContext().setAccessibleName(said.say("bookmark.add.subjects.a11y"));
            Explain.control(list, said.say("bookmark.add.subjects.hover"),
                    said.say("bookmark.add.subjects.explain"));
            c.gridy++;
            JLabel subjectsLabel = new JLabel(said.say(subjects.isEmpty()
                    ? "bookmark.add.subjects.none" : "bookmark.add.subjects.label"));
            subjectsLabel.setLabelFor(list);
            form.add(subjectsLabel, c);
            if (!subjects.isEmpty()) {
                c.gridy++;
                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.add(new JScrollPane(list), BorderLayout.CENTER);
                JPanel moves = new JPanel();
                moves.setLayout(new BoxLayout(moves, BoxLayout.Y_AXIS));
                moves.add(button("bookmark.add.up", () -> shift(-1)));
                moves.add(button("bookmark.add.down", () -> shift(1)));
                moves.add(button("bookmark.add.remove", this::removeSelected));
                row.add(moves, BorderLayout.EAST);
                form.add(row, c);
                if (!subjects.isEmpty()) {
                    list.setSelectedIndex(0);
                }
            }

            primary = new JComboBox<>();
            primary.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                              boolean selected, boolean focused) {
                    String shown = value instanceof Subject s ? nameOf.apply(s)
                            : said.say("bookmark.add.primary.none");
                    return super.getListCellRendererComponent(l, shown, index, selected, focused);
                }
            });
            refreshPrimary(proposal.primary());
            if (!subjects.isEmpty()) {
                c.gridwidth = 1;
                labelled(form, c, "bookmark.add.primary", primary, letters);
            }

            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 2;
            form.add(new JSeparator(), c);

            time = new JCheckBox(said.say("bookmark.add.time.label", WHEN.format(proposal.instant())),
                    proposal.timeTicked());
            letters.apply(time, "bookmark.add.time.mnemonic");
            time.getAccessibleContext().setAccessibleName(said.say("bookmark.add.time.a11y",
                    WHEN.format(proposal.instant())));
            Explain.control(time, said.say("bookmark.add.time.hover"), said.say("bookmark.add.time.explain"));
            c.gridy++;
            form.add(time, c);
            event = new JRadioButton(said.say("bookmark.add.time.event"), true);
            observation = new JRadioButton(said.say("bookmark.add.time.observation"));
            ButtonGroup purpose = new ButtonGroup();
            purpose.add(event);
            purpose.add(observation);
            Explain.control(event, said.say("bookmark.add.time.event.hover"),
                    said.say("bookmark.add.time.event.explain"));
            Explain.control(observation, said.say("bookmark.add.time.observation.hover"),
                    said.say("bookmark.add.time.observation.explain"));
            JPanel purposes = new JPanel(new FlowLayout(FlowLayout.LEADING, 18, 0));
            purposes.add(event);
            purposes.add(observation);
            c.gridy++;
            form.add(purposes, c);
            if (proposal.hasBody()) {
                JLabel why = new JLabel(said.say("bookmark.add.time.why"));
                why.setFont(why.getFont().deriveFont(why.getFont().getSize2D() - 1f));
                c.gridy++;
                form.add(why, c);
            }
            time.addItemListener(e -> purposeFollows());
            purposeFollows();

            String lat = degrees(proposal.place().latitudeDegrees());
            String lon = degrees(proposal.place().eastLongitudeDegrees());
            place = new JCheckBox(said.say("bookmark.add.place.label", lat, lon), false);
            letters.apply(place, "bookmark.add.place.mnemonic");
            place.getAccessibleContext().setAccessibleName(said.say("bookmark.add.place.a11y", lat, lon));
            Explain.control(place, said.say("bookmark.add.place.hover"), said.say("bookmark.add.place.explain"));
            c.gridy++;
            form.add(place, c);

            note = new JTextArea(3, 30);
            note.setLineWrap(true);
            note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName(said.say("bookmark.add.note.a11y"));
            Explain.control(note, said.say("bookmark.add.note.hover"), said.say("bookmark.add.note.explain"));
            c.gridy++;
            JLabel noteLabel = new JLabel(said.say("bookmark.add.note.label"));
            noteLabel.setLabelFor(note);
            letters.apply(noteLabel, "bookmark.add.note.mnemonic");
            form.add(noteLabel, c);
            c.gridy++;
            form.add(new JScrollPane(note), c);

            add(form, BorderLayout.CENTER);

            status = new JLabel(" ");
            add = new JButton(said.say("bookmark.add.ok.label"));
            add.setName(ADD);
            add.getAccessibleContext().setAccessibleName(said.say("bookmark.add.ok.a11y"));
            Explain.control(add, said.say("bookmark.add.ok.hover"), said.say("bookmark.add.ok.explain"));
            add.addActionListener(e -> add());
            JButton cancel = new JButton(said.say("bookmark.add.cancel.label"));
            cancel.getAccessibleContext().setAccessibleName(said.say("bookmark.add.cancel.a11y"));
            Explain.selfExplanatory(cancel, said.say("bookmark.add.cancel.explain"));
            cancel.addActionListener(e -> done.run());
            JPanel foot = new JPanel(new BorderLayout(8, 0));
            foot.add(status, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
            buttons.add(cancel);
            buttons.add(add);
            foot.add(buttons, BorderLayout.EAST);
            add(foot, BorderLayout.SOUTH);
            refuseIfUnavailable();
        }

        void whenDone(Runnable done) {
            this.done = done;
        }

        private void labelled(JPanel form, GridBagConstraints c, String stem, JComponent field,
                              MnemonicText letters) {
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 1;
            JLabel label = new JLabel(said.say(stem + ".label"));
            label.setLabelFor(field);
            letters.apply(label, stem + ".mnemonic");
            form.add(label, c);
            c.gridx = 1;
            field.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
            Explain.control(field, said.say(stem + ".hover"), said.say(stem + ".explain"));
            form.add(field, c);
            c.gridx = 0;
        }

        private JButton button(String stem, Runnable action) {
            JButton b = new JButton(said.say(stem + ".label"));
            b.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
            Explain.control(b, said.say(stem + ".hover"), said.say(stem + ".explain"));
            b.addActionListener(e -> action.run());
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            return b;
        }

        /** Moves the chosen subject; package-visible for tests. */
        void shift(int by) {
            int at = list.getSelectedIndex();
            int to = at + by;
            if (at < 0 || to < 0 || to >= subjects.size()) {
                return;
            }
            Subject moved = subjects.remove(at);
            subjects.add(to, moved);
            list.setSelectedIndex(to);
        }

        private void removeSelected() {
            int at = list.getSelectedIndex();
            if (at < 0) {
                return;
            }
            Object chosen = primary.getSelectedItem();
            subjects.remove(at);
            refreshPrimary(chosen instanceof Subject s ? Optional.of(s) : Optional.empty());
            if (!subjects.isEmpty()) {
                list.setSelectedIndex(Math.min(at, subjects.size() - 1));
            }
        }

        private void refreshPrimary(Optional<Subject> chosen) {
            primary.removeAllItems();
            primary.addItem(Optional.empty());
            for (int i = 0; i < subjects.size(); i++) {
                primary.addItem(subjects.get(i));
            }
            primary.setSelectedIndex(0);
            if (chosen.isPresent()) {
                for (int i = 0; i < subjects.size(); i++) {
                    if (subjects.get(i).equals(chosen.get())) {
                        primary.setSelectedIndex(i + 1);
                    }
                }
            }
        }

        private void purposeFollows() {
            event.setEnabled(time.isSelected());
            observation.setEnabled(time.isSelected());
        }

        private void refuseIfUnavailable() {
            String reason = switch (library.state()) {
                case LibraryState.Available a -> null;
                case LibraryState.Newer n -> said.say("bookmark.refused.newer");
                case LibraryState.Unreadable u -> said.say("bookmark.refused.unreadable");
            };
            if (reason != null) {
                add.setEnabled(false);
                say(reason);
            }
        }

        private void say(String sentence) {
            status.setText(sentence);
            status.getAccessibleContext().setAccessibleName(sentence);
        }

        /** The draft the dialog would add now; package-visible for tests. */
        BookmarkLibrary.Draft draft() {
            List<Subject> ordered = new ArrayList<>();
            for (int i = 0; i < subjects.size(); i++) {
                ordered.add(subjects.get(i));
            }
            Object chosen = primary.getSelectedItem();
            return BookmarkCapture.draft(proposal, title.getText(), ordered,
                    chosen instanceof Subject s ? Optional.of(s) : Optional.empty(),
                    time.isSelected(),
                    observation.isSelected() ? ChartTime.Purpose.OBSERVATION : ChartTime.Purpose.EVENT,
                    place.isSelected(), note.getText());
        }

        /** Adds the bookmark, or says why not; package-visible for tests. */
        void add() {
            if (title.getText().isBlank()) {
                say(said.say("bookmark.refused.title"));
                title.requestFocusInWindow();
                return;
            }
            FolderChoice into = (FolderChoice) folder.getSelectedItem();
            try {
                library.add(into.id(), Integer.MAX_VALUE, draft());
                done.run();
            } catch (IOException | IllegalStateException | IllegalArgumentException refused) {
                say(said.say("bookmark.refused.notsaved", refused.getMessage()));
            }
        }
    }

    /** A folder the bookmark can go into, shown with its path. */
    record FolderChoice(String id, String shown) {
        @Override
        public String toString() {
            return shown;
        }
    }

    static List<FolderChoice> folders(BookmarkLibrary library, InterfaceText said) {
        List<FolderChoice> out = new ArrayList<>();
        String root = said.say("bookmark.root");
        out.add(new FolderChoice(BookmarkLibrary.ROOT, root));
        if (library.state() instanceof LibraryState.Available a) {
            walk(a.library().root(), root, out);
        }
        return out;
    }

    private static void walk(List<Node> nodes, String path, List<FolderChoice> out) {
        for (Node n : nodes) {
            if (n instanceof Node.Folder f) {
                String shown = path + " ▸ " + f.title();
                out.add(new FolderChoice(f.id(), shown));
                walk(f.children(), shown, out);
            }
        }
    }

    private static String field(double degrees) {
        return (degrees == Math.rint(degrees) ? Long.toString((long) degrees) : Double.toString(degrees)) + "°";
    }

    private static String degrees(double value) {
        return String.format(Locale.ROOT, "%.3f°", value);
    }
}
