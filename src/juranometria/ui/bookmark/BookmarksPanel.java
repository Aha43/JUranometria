package juranometria.ui.bookmark;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.Library;
import juranometria.bookmark.LibraryState;
import juranometria.bookmark.Node;
import juranometria.bookmark.Subject;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;

/**
 * The Bookmarks tab of the Controller (#681 stage 3; rulings of
 * 2026-11-02 on #497): the whole library as a tree under the permanent
 * root *Bookmarks* / *Bokmerker*, the details of the chosen entry -
 * choosing never opens - and the actions Open, Edit..., Delete...,
 * New Folder..., Move up and Move down. A timed bookmark is marked ◷; a
 * record this version cannot use is marked ⚠ with words, shows its
 * reasons, and can only be deleted. Enter opens; Delete asks. A folder's
 * deletion keeps its contents in its parent unless the reader explicitly
 * chooses to delete them.
 */
public final class BookmarksPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** The tab's stable id in the Controller. */
    public static final String TAB = "bookmarks";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC);

    /** What the panel needs from the application, beyond the library. */
    public interface Host {
        /** Opens the bookmark on the chart and says what became of its subjects. */
        BookmarkOpener.Report open(Node.Bookmark bookmark);

        /** The chart's view and context now, for Edit. */
        BookmarkCapture.Proposal now();
    }

    private final InterfaceText said;
    private final BookmarkLibrary library;
    private final Host host;
    private final Function<Subject, String> nameOf;
    final JTree tree;
    final JTextArea details;
    final JLabel message;
    final JButton open;
    final JButton edit;
    final JButton delete;
    final JButton newFolder;
    final JButton up;
    final JButton down;
    final JButton libraryMenu;
    private final JPanel body;
    private final JPanel unavailable;
    private final JPanel south;

    public BookmarksPanel(InterfaceText said, BookmarkLibrary library, Host host,
                          Function<Subject, String> nameOf) {
        super(new BorderLayout(0, 6));
        this.said = said;
        this.library = library;
        this.host = host;
        this.nameOf = nameOf;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode(said.say("bookmark.root"))));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setVisibleRowCount(12);
        tree.setCellRenderer(new Renderer());
        tree.getAccessibleContext().setAccessibleName(said.say("bookmark.tree.a11y"));
        Explain.control(tree, said.say("bookmark.tree.hover"), said.say("bookmark.tree.explain"));
        tree.addTreeSelectionListener(e -> chosenChanged());
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openBookmark");
        tree.getActionMap().put("openBookmark", action(this::openChosen));
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteBookmark");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteBookmark");
        tree.getActionMap().put("deleteBookmark", action(this::deleteChosen));
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && chosen().orElse(null) instanceof Node.Bookmark) {
                    openChosen();
                }
            }
        });

        details = new JTextArea(6, 28);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setOpaque(false);
        details.getAccessibleContext().setAccessibleName(said.say("bookmark.details.a11y"));
        Explain.selfExplanatory(details, said.say("bookmark.details.explain"));
        JPanel detailsBox = new JPanel(new BorderLayout());
        detailsBox.setBorder(BorderFactory.createTitledBorder(said.say("bookmark.details.title")));
        detailsBox.add(details, BorderLayout.CENTER);

        message = new JLabel(" ");

        open = button("bookmark.open", this::openChosen);
        edit = button("bookmark.edit", this::editChosen);
        delete = button("bookmark.delete", this::deleteChosen);
        newFolder = button("bookmark.newFolder", this::newFolder);
        up = button("bookmark.up", () -> shift(-1));
        down = button("bookmark.down", () -> shift(1));
        libraryMenu = button("bookmark.library", this::showLibraryMenu);
        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 4));
        JPanel first = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        first.add(open);
        first.add(edit);
        first.add(delete);
        JPanel second = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        second.add(newFolder);
        second.add(up);
        second.add(down);
        // The library's own actions on a row of their own, so the
        // Controller's narrow width never hides them.
        JPanel third = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        third.add(libraryMenu);
        actions.add(first);
        actions.add(second);
        actions.add(third);

        south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        detailsBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        south.add(detailsBox);
        south.add(message);
        south.add(actions);

        body = new JPanel(new BorderLayout(0, 6));
        body.add(new JScrollPane(tree), BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);
        unavailable = new JPanel(new BorderLayout());

        library.onChange(() -> SwingUtilities.invokeLater(this::rebuild));
        rebuild();
    }

    private javax.swing.Action action(Runnable run) {
        return new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                run.run();
            }
        };
    }

    private JButton button(String stem, Runnable run) {
        JButton b = new JButton(said.say(stem + ".label"));
        b.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
        Explain.control(b, said.say(stem + ".hover"), said.say(stem + ".explain"));
        b.addActionListener(e -> run.run());
        return b;
    }

    /** Rebuilds the tree from the library, keeping the chosen entry and the open folders. */
    void rebuild() {
        removeAll();
        switch (library.state()) {
            case LibraryState.Available a -> {
                String chosenId = chosen().map(Node::id).orElse(null);
                Set<String> expanded = expandedIds();
                boolean first = tree.getRowCount() <= 1 && expanded.isEmpty();
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(said.say("bookmark.root"));
                fill(root, a.library().root());
                tree.setModel(new DefaultTreeModel(root));
                for (int row = 0; row < tree.getRowCount(); row++) {
                    TreePath path = tree.getPathForRow(row);
                    Object node = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (path.getPathCount() == 1 || first
                            || (node instanceof Node.Folder f && expanded.contains(f.id()))) {
                        tree.expandRow(row);
                    }
                }
                if (chosenId != null) {
                    select(chosenId);
                }
                if (message.getParent() != south) {
                    south.add(message, 1);
                }
                add(body, BorderLayout.CENTER);
            }
            case LibraryState.Newer n -> showUnavailable(said.say("bookmark.state.newer"), List.of());
            case LibraryState.Unreadable u -> {
                List<JButton> ways = new ArrayList<>();
                u.backup().ifPresent(offer -> ways.add(button("bookmark.recover", () -> recover(offer))));
                ways.add(button("bookmark.restore", this::restoreFromFile));
                showUnavailable(said.say("bookmark.state.unreadable")
                        + u.backup().map(offer -> " " + said.say("bookmark.state.backup",
                                offer.bookmarks(), offer.folders(), WHEN.format(offer.written()))).orElse(""), ways);
            }
        }
        chosenChanged();
        revalidate();
        repaint();
    }

    private void showUnavailable(String sentence, List<JButton> ways) {
        unavailable.removeAll();
        JTextArea words = new JTextArea(sentence);
        words.setEditable(false);
        words.setLineWrap(true);
        words.setWrapStyleWord(true);
        words.setOpaque(false);
        words.getAccessibleContext().setAccessibleName(sentence);
        unavailable.add(words, BorderLayout.NORTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 4));
        for (JButton way : ways) {
            row.add(way);
        }
        unavailable.add(row, BorderLayout.CENTER);
        unavailable.add(message, BorderLayout.SOUTH);
        add(unavailable, BorderLayout.CENTER);
    }

    /** How a file is chosen: the system's chooser, or a test's answer. */
    interface Chooser {
        /** A file to write the export to, starting from a suggested name; empty if cancelled. */
        Optional<java.nio.file.Path> save(Component parent, String suggested);

        /** A file to restore from; empty if cancelled. */
        Optional<java.nio.file.Path> open(Component parent);
    }

    Chooser chooser = new Chooser() {
        @Override
        public Optional<java.nio.file.Path> save(Component parent, String suggested) {
            javax.swing.JFileChooser c = new javax.swing.JFileChooser();
            c.setDialogTitle(said.say("bookmark.export.title"));
            c.setSelectedFile(new java.io.File(suggested));
            return c.showSaveDialog(parent) == javax.swing.JFileChooser.APPROVE_OPTION
                    ? Optional.of(c.getSelectedFile().toPath()) : Optional.empty();
        }

        @Override
        public Optional<java.nio.file.Path> open(Component parent) {
            javax.swing.JFileChooser c = new javax.swing.JFileChooser();
            c.setDialogTitle(said.say("bookmark.restore.title"));
            return c.showOpenDialog(parent) == javax.swing.JFileChooser.APPROVE_OPTION
                    ? Optional.of(c.getSelectedFile().toPath()) : Optional.empty();
        }
    };

    /** The clock that dates an export's suggested name. */
    java.time.Clock clock = java.time.Clock.systemUTC();

    private void showLibraryMenu() {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem export = new javax.swing.JMenuItem(said.say("bookmark.export.label"));
        export.getAccessibleContext().setAccessibleName(said.say("bookmark.export.a11y"));
        Explain.selfExplanatory(export, said.say("bookmark.export.explain"));
        export.addActionListener(e -> exportLibrary());
        javax.swing.JMenuItem restore = new javax.swing.JMenuItem(said.say("bookmark.restore.label"));
        restore.getAccessibleContext().setAccessibleName(said.say("bookmark.restore.a11y"));
        Explain.selfExplanatory(restore, said.say("bookmark.restore.explain"));
        restore.addActionListener(e -> restoreFromFile());
        menu.add(export);
        menu.add(restore);
        menu.show(libraryMenu, 0, libraryMenu.getHeight());
    }

    /** Exports the whole library to a chosen file; asks before replacing one. */
    void exportLibrary() {
        if (!(library.state() instanceof LibraryState.Available a)) {
            return;
        }
        String suggested = said.say("bookmark.export.name",
                java.time.LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).toString());
        Optional<java.nio.file.Path> to = chooser.save(this, suggested);
        if (to.isEmpty()) {
            return;
        }
        if (java.nio.file.Files.exists(to.get())) {
            Object[] options = {said.say("bookmark.export.replace"), said.say("bookmark.add.cancel.label")};
            if (asker.ask(this, said.say("bookmark.export.exists", to.get().getFileName()),
                    said.say("bookmark.export.title"), options) != 0) {
                return;
            }
        }
        try {
            library.exportTo(to.get());
            say(said.say("bookmark.export.done", a.library().bookmarks().size(),
                    a.library().folders().size(), to.get().getFileName()));
        } catch (java.io.IOException | RuntimeException refused) {
            say(said.say("bookmark.export.failed", refused.getMessage()));
        }
    }

    /** Restores the library from a chosen file, after a summary and the reader's yes. */
    void restoreFromFile() {
        Optional<java.nio.file.Path> from = chooser.open(this);
        if (from.isEmpty()) {
            return;
        }
        try {
            BookmarkLibrary.RestoreSummary summary = library.inspect(from.get());
            if (summary instanceof BookmarkLibrary.RestoreSummary.Refused refused) {
                say(said.say("bookmark.restore.refused", from.get().getFileName(), refused.reason()));
                return;
            }
            BookmarkLibrary.RestoreSummary.Restorable r = (BookmarkLibrary.RestoreSummary.Restorable) summary;
            String question = said.say("bookmark.restore.question", from.get().getFileName(),
                    r.bookmarks(), r.folders(), r.needingAttention(),
                    r.writtenBy().isBlank() ? "?" : r.writtenBy());
            Object[] options = {said.say("bookmark.restore.replace"), said.say("bookmark.add.cancel.label")};
            if (asker.ask(this, question, said.say("bookmark.restore.title"), options) != 0) {
                return;
            }
            java.nio.file.Path kept = library.restoreFrom(from.get());
            say(said.say("bookmark.restore.done", r.bookmarks(), kept.getFileName()));
        } catch (java.io.IOException | RuntimeException refused) {
            say(said.say("bookmark.restore.failed", refused.getMessage()));
        }
    }

    private void recover(LibraryState.BackupOffer offer) {
        Object[] options = {said.say("bookmark.recover.yes"), said.say("bookmark.add.cancel.label")};
        if (asker.ask(this, said.say("bookmark.recover.question", offer.bookmarks(), offer.folders(),
                WHEN.format(offer.written())), said.say("bookmark.recover.title"), options) != 0) {
            return;
        }
        try {
            library.recoverFromBackup();
            say(said.say("bookmark.recover.done"));
        } catch (java.io.IOException | RuntimeException refused) {
            say(said.say("bookmark.restore.failed", refused.getMessage()));
        }
    }

    private static void fill(DefaultMutableTreeNode parent, List<Node> nodes) {
        for (Node n : nodes) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(n, n instanceof Node.Folder);
            parent.add(child);
            if (n instanceof Node.Folder f) {
                fill(child, f.children());
            }
        }
    }

    private Set<String> expandedIds() {
        Set<String> out = new HashSet<>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            Object node = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (node instanceof Node.Folder f && tree.isExpanded(path)) {
                out.add(f.id());
            }
        }
        return out;
    }

    /** Chooses an entry, for photographs and studies. */
    public void selectForStudy(String id) {
        select(id);
    }

    /** Chooses the entry with this id, opening the folders above it. */
    void select(String id) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        java.util.Enumeration<javax.swing.tree.TreeNode> all = root.depthFirstEnumeration();
        while (all.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) all.nextElement();
            if (n.getUserObject() instanceof Node node && node.id().equals(id)) {
                TreePath path = new TreePath(n.getPath());
                tree.expandPath(path.getParentPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
        }
    }

    /** The chosen entry; empty for the root or nothing. */
    Optional<Node> chosen() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return Optional.empty();
        }
        Object node = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return node instanceof Node n ? Optional.of(n) : Optional.empty();
    }

    private void chosenChanged() {
        Optional<Node> chosen = chosen();
        boolean available = library.state() instanceof LibraryState.Available;
        open.setEnabled(available && chosen.orElse(null) instanceof Node.Bookmark);
        edit.setEnabled(available && (chosen.orElse(null) instanceof Node.Bookmark
                || chosen.orElse(null) instanceof Node.Folder));
        delete.setEnabled(available && chosen.isPresent());
        newFolder.setEnabled(available);
        libraryMenu.setEnabled(available);
        up.setEnabled(available && chosen.isPresent());
        down.setEnabled(available && chosen.isPresent());
        details.setText(chosen.map(this::describe).orElse(said.say("bookmark.details.none")));
        details.setCaretPosition(0);
    }

    /** The details of an entry, in the interface's words. */
    String describe(Node node) {
        return switch (node) {
            case Node.Folder f -> said.say("bookmark.details.folder", f.title(), f.children().size());
            case Node.Kept k -> said.say("bookmark.details.kept", String.join("; ", k.reasons()));
            case Node.Bookmark b -> {
                List<String> lines = new ArrayList<>();
                lines.add(b.title());
                lines.add(said.say("bookmark.details.view",
                        juranometria.chart.SkyFormat.formatRa(b.view().raDegrees()) + ", "
                                + juranometria.chart.SkyFormat.formatDec(b.view().decDegrees()),
                        field(b.view().fieldWidthDegrees())));
                if (!b.subjects().isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (Subject s : b.subjects()) {
                        names.add((b.primary().filter(s::equals).isPresent() ? "★ " : "") + nameOf.apply(s));
                    }
                    lines.add(said.say("bookmark.details.subjects", String.join(", ", names)));
                }
                b.time().ifPresent(t -> lines.add(said.say(t.purpose() == ChartTime.Purpose.EVENT
                        ? "bookmark.details.event" : "bookmark.details.observation", WHEN.format(t.instant()))));
                b.place().ifPresent(p -> lines.add(said.say("bookmark.details.place",
                        String.format(Locale.ROOT, "%.3f°", p.latitudeDegrees()),
                        String.format(Locale.ROOT, "%.3f°", p.eastLongitudeDegrees()))));
                b.note().ifPresent(lines::add);
                yield String.join("\n", lines);
            }
        };
    }

    private static String field(double degrees) {
        return (degrees == Math.rint(degrees) ? Long.toString((long) degrees) : Double.toString(degrees)) + "°";
    }

    /** Opens the chosen bookmark and says what became of its subjects. */
    void openChosen() {
        if (!(chosen().orElse(null) instanceof Node.Bookmark b)) {
            return;
        }
        BookmarkOpener.Report report = host.open(b);
        say(reportWords(report));
    }

    /** The open report, in the interface's words. */
    String reportWords(BookmarkOpener.Report report) {
        List<String> parts = new ArrayList<>();
        parts.add(said.say(report.selected() ? "bookmark.opened.selected" : "bookmark.opened"));
        if (report.closerRefused()) {
            parts.add(said.say("bookmark.opened.closer"));
        }
        for (BookmarkOpener.SubjectReport s : report.subjects()) {
            if (s.outcome() != BookmarkOpener.Outcome.DRAWN) {
                parts.add(said.say("bookmark.outcome." + s.outcome().name().toLowerCase(Locale.ROOT),
                        nameOf.apply(s.subject())));
            }
        }
        return String.join(" ", parts);
    }

    private void say(String sentence) {
        message.setText("<html>" + sentence.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        message.getAccessibleContext().setAccessibleName(sentence);
    }

    private void editChosen() {
        Optional<Node> chosen = chosen();
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (chosen.orElse(null) instanceof Node.Bookmark b) {
            BookmarkCapture.Proposal now = host.now();
            AddBookmarkDialog.edit(owner, said, library,
                    new AddBookmarkDialog.Editing(b, parentOf(b.id()), now.view(), now), nameOf);
            select(b.id());
        } else if (chosen.orElse(null) instanceof Node.Folder f) {
            editFolder(f);
        }
    }

    private void editFolder(Node.Folder folder) {
        javax.swing.JTextField name = new javax.swing.JTextField(folder.title(), 24);
        List<AddBookmarkDialog.FolderChoice> places = new ArrayList<>();
        for (AddBookmarkDialog.FolderChoice c : AddBookmarkDialog.folders(library, said)) {
            if (!c.id().equals(folder.id()) && !within(folder, c.id())) {
                places.add(c);
            }
        }
        JComboBox<AddBookmarkDialog.FolderChoice> where =
                new JComboBox<>(places.toArray(new AddBookmarkDialog.FolderChoice[0]));
        String parent = parentOf(folder.id());
        for (int i = 0; i < where.getItemCount(); i++) {
            if (where.getItemAt(i).id().equals(parent)) {
                where.setSelectedIndex(i);
            }
        }
        name.getAccessibleContext().setAccessibleName(said.say("bookmark.folder.name.a11y"));
        where.getAccessibleContext().setAccessibleName(said.say("bookmark.folder.in.a11y"));
        JPanel form = new JPanel(new GridLayout(4, 1, 0, 4));
        form.add(new JLabel(said.say("bookmark.folder.name.label")));
        form.add(name);
        form.add(new JLabel(said.say("bookmark.folder.in.label")));
        form.add(where);
        int answer = JOptionPane.showConfirmDialog(this, form, said.say("bookmark.folder.edit.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION || name.getText().isBlank()) {
            return;
        }
        try {
            // One change: the name and the place kept together, or neither.
            String into = ((AddBookmarkDialog.FolderChoice) where.getSelectedItem()).id();
            library.renameAndMove(folder.id(), name.getText().strip(), into);
        } catch (IOException | IllegalArgumentException | IllegalStateException refused) {
            say(said.say("bookmark.refused.notsaved", refused.getMessage()));
        }
    }

    private boolean within(Node.Folder folder, String id) {
        return new Library("", folder.children()).find(id).isPresent();
    }

    private void newFolder() {
        String title = JOptionPane.showInputDialog(this, said.say("bookmark.folder.name.label"),
                said.say("bookmark.newFolder.title"), JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.isBlank()) {
            return;
        }
        Optional<Node> chosen = chosen();
        String parent = chosen.orElse(null) instanceof Node.Folder f ? f.id()
                : chosen.map(n -> parentOf(n.id())).orElse(BookmarkLibrary.ROOT);
        try {
            String id = library.addFolder(parent, Integer.MAX_VALUE, title.strip());
            SwingUtilities.invokeLater(() -> select(id));
        } catch (IOException | IllegalArgumentException | IllegalStateException refused) {
            say(said.say("bookmark.refused.notsaved", refused.getMessage()));
        }
    }

    /** Deletes the chosen entry after asking; a folder's contents are kept unless deletion is chosen. */
    void deleteChosen() {
        Optional<Node> chosen = chosen();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            if (chosen.get() instanceof Node.Folder f) {
                Object[] options = {said.say("bookmark.delete.folder.keep"),
                        said.say("bookmark.delete.folder.all"), said.say("bookmark.add.cancel.label")};
                Library inside = new Library("", f.children());
                int answer = asker.ask(this, said.say("bookmark.delete.folder.question", f.title(),
                        inside.bookmarks().size(), inside.folders().size(), inside.kept().size()),
                        said.say("bookmark.delete.title"), options);
                if (answer == 0) {
                    library.removeFolder(f.id(), BookmarkLibrary.Contents.KEEP);
                } else if (answer == 1) {
                    library.removeFolder(f.id(), BookmarkLibrary.Contents.DELETE);
                }
            } else {
                String what = chosen.get() instanceof Node.Bookmark b ? b.title() : said.say("bookmark.kept.name");
                Object[] options = {said.say("bookmark.delete.confirm"), said.say("bookmark.add.cancel.label")};
                int answer = asker.ask(this, said.say("bookmark.delete.question", what),
                        said.say("bookmark.delete.title"), options);
                if (answer == 0) {
                    library.remove(chosen.get().id());
                }
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException refused) {
            say(said.say("bookmark.refused.notsaved", refused.getMessage()));
        }
    }

    /** How a question is asked: the option pane, or a test's answer. */
    interface Asker {
        int ask(Component parent, String question, String title, Object[] options);
    }

    Asker asker = (parent, question, title, options) -> JOptionPane.showOptionDialog(parent, question, title,
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

    /** Moves the chosen entry within its folder; package-visible for tests. */
    void shift(int by) {
        Optional<Node> chosen = chosen();
        if (chosen.isEmpty() || !(library.state() instanceof LibraryState.Available a)) {
            return;
        }
        String id = chosen.get().id();
        String parent = parentOf(id);
        List<Node> siblings = parent.equals(BookmarkLibrary.ROOT) ? a.library().root()
                : ((Node.Folder) a.library().find(parent).orElseThrow()).children();
        int at = -1;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).id().equals(id)) {
                at = i;
            }
        }
        int to = at + by;
        if (at < 0 || to < 0 || to >= siblings.size()) {
            return;
        }
        try {
            library.move(id, parent, to);
            SwingUtilities.invokeLater(() -> select(id));
        } catch (IOException | IllegalArgumentException | IllegalStateException refused) {
            say(said.say("bookmark.refused.notsaved", refused.getMessage()));
        }
    }

    /** The id of the folder holding an entry; the root's for a top-level one. */
    String parentOf(String id) {
        if (!(library.state() instanceof LibraryState.Available a)) {
            return BookmarkLibrary.ROOT;
        }
        return parentIn(a.library().root(), BookmarkLibrary.ROOT, id).orElse(BookmarkLibrary.ROOT);
    }

    private static Optional<String> parentIn(List<Node> nodes, String parent, String id) {
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                return Optional.of(parent);
            }
            if (n instanceof Node.Folder f) {
                Optional<String> inner = parentIn(f.children(), f.id(), id);
                if (inner.isPresent()) {
                    return inner;
                }
            }
        }
        return Optional.empty();
    }

    /** The tree's rows: a title, ◷ for a timed bookmark, ⚠ and words for a record needing attention. */
    private final class Renderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean focused) {
            Object node = ((DefaultMutableTreeNode) value).getUserObject();
            String shown = switch (node) {
                case Node.Folder f -> f.title();
                case Node.Bookmark b -> b.title() + (b.time().isPresent() ? "  ◷" : "");
                case Node.Kept k -> "⚠ " + said.say("bookmark.kept.name") + " - " + said.say("bookmark.kept.attention");
                default -> String.valueOf(node);
            };
            Component c = super.getTreeCellRendererComponent(t, shown, selected, expanded,
                    node instanceof Node.Bookmark || node instanceof Node.Kept, row, focused);
            if (node instanceof Node.Folder || !(node instanceof Node)) {
                setIcon(expanded ? getOpenIcon() : getClosedIcon());
            }
            return c;
        }
    }
}
