package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * The companion-window study (Sprint 39, issue #433): what the
 * Place and Time content needs from a window beside the chart, before
 * any production companion exists.
 *
 * <p>Every mock-up holds the <em>production</em> Place and Time
 * content - {@link PlaceAndTimeDialog#contentForStudy}, in the
 * language stated - inside a study-only companion shell: a section
 * heading that collapses, a scroll pane that never scrolls sideways,
 * and, in one pair, the proposed line that says why an input was
 * refused. The shell is a proposal, painted off screen; the
 * implementation's photographs will be of real windows (#350's
 * review). The images carry the {@code controls-} prefix: widget
 * inspection imagery, held to substance and not to bytes.
 *
 * <p>Standard output is the portable report - what the content is,
 * the same on every desktop. How wide its words are drawn is one
 * machine's answer and goes to {@code platform.md} beside it.
 */
public final class CompanionWindowStudyMain {

    static final Path DIR = Path.of("docs/studies/companion-window");

    /** The widths the shell is drawn at: narrow, middle, the dialog's floor. */
    static final int[] WIDTHS = {300, 360, 420};

    /** The tallest a mock-up is drawn; taller content scrolls. */
    static final int MAX_HEIGHT = 560;

    /** The screens the placement is drawn on, logical pixels. */
    static final Dimension[] SCREENS = {
        new Dimension(1280, 800), new Dimension(1440, 900),
        new Dimension(1920, 1080), new Dimension(2560, 1440)};

    /** The languages and appearances every content measure is taken in. */
    static final String[] LANGUAGES = {"en", "nb-NO"};

    /**
     * The proposed refusal line, in both languages. Study words: the
     * production keys arrive with the implementation, through the
     * ordinary language files and their review.
     */
    static final String[][] REFUSED = {
        {"en", "Latitude must be between −90° and 90°. Kept 59.913."},
        {"nb-NO", "Breddegraden må ligge mellom −90° og 90°. Beholdt 59.913."}};

    private CompanionWindowStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-companion-" + System.nanoTime());
        // The look and feel is the process's, and the evidence contract
        // runs every generator in one: what this one sets is put back.
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        // So is a default-font override, and an earlier generator in the
        // same process may have left one (#362): inherited, it drew this
        // study four pixels narrower and eighteen shorter than it draws
        // alone. The study owns its fonts - none - and puts back what it
        // found.
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The companion window's content, as this desktop draws it",
                "Sprint 39, issue #433.");
        try {
            preface(report, oslo);
            platform.append("## How wide Place and Time's words are drawn\n\n")
                    .append("The production content's preferred size, and the"
                            + " narrowest\ncompanion (content, its scroll bar"
                            + " and the shell's insets) in which\nno label,"
                            + " button, box or field is drawn narrower than"
                            + " it asks.\n\n")
                    .append("| language | appearance | content preferred |"
                            + " narrowest untruncated companion |\n")
                    .append("|---|---|---:|---:|\n");
            List<String> images = new ArrayList<>();
            for (String language : LANGUAGES) {
                InterfaceText said = InterfaceText.forLanguage(language);
                for (boolean dark : new boolean[] {false, true}) {
                    String appearance = dark ? "dark" : "light";
                    Dimension preferred = onEdt(() -> {
                        juranometria.app.UiTheme.apply(dark);
                        return content(oslo, node, said).getPreferredSize();
                    });
                    int narrowest = onEdt(() -> narrowest(oslo, node, said));
                    platform.append("| `").append(language).append("` | ")
                            .append(appearance).append(" | ")
                            .append(preferred.width).append(" × ")
                            .append(preferred.height).append(" | ")
                            .append(narrowest).append(" px |\n");
                    for (int width : WIDTHS) {
                        String name = "controls-companion-" + language + "-"
                                + appearance + "-" + width + ".png";
                        onEdt(() -> {
                            write(shell(oslo, node, said, width, false, null),
                                    width, name);
                            return null;
                        });
                        images.add(name);
                    }
                }
                inventory(report, language, onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    return content(oslo, node, said);
                }));
            }
            // The collapsed section and the proposed refusal line, light,
            // at the middle width, in both languages.
            for (String[] refused : REFUSED) {
                InterfaceText said = InterfaceText.forLanguage(refused[0]);
                String collapsed = "controls-companion-" + refused[0]
                        + "-collapsed-360.png";
                String status = "controls-companion-" + refused[0]
                        + "-refused-360.png";
                onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    write(shell(oslo, node, said, 360, true, null), 360,
                            collapsed);
                    write(shell(oslo, node, said, 360, false, refused[1]), 360,
                            status);
                    return null;
                });
                images.add(collapsed);
                images.add(status);
            }
            Dimension chart = onEdt(CompanionWindowStudyMain::chartSize);
            for (Dimension screen : SCREENS) {
                String name = "controls-companion-placement-" + screen.width
                        + "x" + screen.height + ".png";
                ImageIO.write(placement(screen, chart), "png",
                        DIR.resolve(name).toFile());
                images.add(name);
            }
            placementReport(report, chart);
            report.append("## The images\n\n");
            for (String image : images) {
                report.append("- `").append(image).append("`\n");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", font);
                try {
                    UIManager.setLookAndFeel(before);
                } catch (javax.swing.UnsupportedLookAndFeelException gone) {
                    throw new IllegalStateException(gone);
                }
            });
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    private static void preface(StringBuilder out,
                                PlaceAndTimeSheetMain.State state) {
        out.append("# The companion window, measured before it exists\n\n")
                .append("Sprint 39, issue #433. Generated by\n"
                        + "`juranometria.tool.CompanionWindowStudyMain` from"
                        + " the compiled application.\n\n")
                .append("Every mock-up holds the production Place and Time"
                        + " content\n(`PlaceAndTimeDialog.contentForStudy`)"
                        + " inside a study-only shell. The\nshell is the"
                        + " proposal; the content is what ships today. The"
                        + " state is\n`PlaceAndTimeSheetMain`'s `")
                .append(state.name()).append("` arrangement at its frozen"
                        + " instant `")
                .append(PlaceAndTimeSheetMain.WHEN).append("`, read from"
                        + " that photographer\nrather than restated.\n\n")
                .append("How wide the words are drawn - the narrowest"
                        + " companion that truncates\nnothing - is one"
                        + " machine's answer, in `platform.md` beside this.\n\n");
    }

    /** What the content is: every control, its kind and what it says. */
    private static void inventory(StringBuilder out, String language,
                                  JComponent content) {
        out.append("## The content in `").append(language).append("`\n\n")
                .append("| kind | shown | accessible name |\n|---|---|---|\n");
        List<Component> all = new ArrayList<>();
        walk(content, all);
        int controls = 0;
        for (Component component : all) {
            String kind;
            String shown;
            if (component instanceof AbstractButton button) {
                kind = button.getClass().getSimpleName();
                shown = button.getText();
            } else if (component instanceof JTextField field) {
                kind = "JTextField";
                shown = field.getText();
            } else {
                continue;
            }
            controls++;
            String spoken = component.getAccessibleContext()
                    .getAccessibleName();
            out.append("| ").append(kind).append(" | ")
                    .append(cell(shown)).append(" | ")
                    .append(cell(spoken)).append(" |\n");
        }
        out.append("\n**").append(controls).append(" operable controls.**"
                + " A companion that hosts this content offers\nexactly"
                + " these; it adds only its own shell.\n\n");
    }

    private static String cell(String text) {
        return text == null ? "—" : text.replace("|", "\\|")
                .replace("\n", " ");
    }

    /** The production content, in its own state. */
    private static JComponent content(PlaceAndTimeSheetMain.State state,
                                      Preferences node, InterfaceText said) {
        MeridianModule module = new MeridianModule(new Observer(
                state.latitude(), state.eastLongitude(),
                PlaceAndTimeSheetMain.WHEN));
        module.showing(state.meridian(), state.horizon(), state.zenith());
        PlaceStore store = PlaceStore.forNode(node);
        store.save(state.latitude(), state.eastLongitude());
        return PlaceAndTimeDialog.contentForStudy(module, store, said);
    }

    /**
     * The study-only shell: a section heading that collapses, the
     * content in a scroll pane that never scrolls sideways, and an
     * optional refusal line beneath it.
     */
    private static JPanel shell(PlaceAndTimeSheetMain.State state,
                                Preferences node, InterfaceText said,
                                int width, boolean collapsed, String refused) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel heading = new JLabel((collapsed ? "▸ " : "▾ ")
                + said.say("placeandtime.title"));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 4, 6, 4)));
        shell.add(heading, BorderLayout.NORTH);
        if (!collapsed) {
            JScrollPane scroll = new JScrollPane(new WidthTracking(
                    content(state, node, said)),
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            shell.add(scroll, BorderLayout.CENTER);
        }
        if (refused != null) {
            JLabel line = new JLabel("<html>" + refused + "</html>");
            line.setForeground(UIManager.getColor("Component.error.focusedBorderColor"));
            line.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));
            shell.add(line, BorderLayout.SOUTH);
        }
        int height = Math.min(MAX_HEIGHT, collapsed ? 48
                : shell.getPreferredSize().height);
        shell.setSize(width, height);
        layOut(shell);
        return shell;
    }

    /**
     * The content held to the viewport's width, so a narrow companion
     * lays it out narrower rather than clipping its trailing edge -
     * which is what never scrolling sideways has to mean. Without it
     * the viewport keeps the content at its preferred width and cuts
     * it off, and no control is drawn narrower than it asks: the
     * first run of this study measured a 160-pixel companion as
     * untruncated for exactly that reason.
     */
    private static final class WidthTracking extends JPanel
            implements javax.swing.Scrollable {

        WidthTracking(JComponent content) {
            super(new BorderLayout());
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible,
                                              int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible,
                                               int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** The narrowest shell in which nothing is drawn narrower than it asks. */
    private static int narrowest(PlaceAndTimeSheetMain.State state,
                                 Preferences node, InterfaceText said) {
        for (int width = 160; width <= 800; width += 2) {
            JPanel shell = shell(state, node, said, width, false, null);
            if (truncated(shell).isEmpty()) {
                return width;
            }
        }
        return -1;
    }

    /** Every control drawn narrower than it asks to be. */
    static List<String> truncated(Container root) {
        List<Component> all = new ArrayList<>();
        walk(root, all);
        List<String> short_ = new ArrayList<>();
        for (Component component : all) {
            if ((component instanceof JLabel || component instanceof AbstractButton
                    || component instanceof JTextField)
                    && component.isVisible()
                    && component.getWidth() < component.getPreferredSize().width) {
                short_.add(component.getClass().getSimpleName() + " "
                        + component.getWidth() + "/"
                        + component.getPreferredSize().width);
            }
        }
        return short_;
    }

    private static void walk(Component at, List<Component> into) {
        into.add(at);
        if (at instanceof Container container) {
            for (Component child : container.getComponents()) {
                walk(child, into);
            }
        }
    }

    /** Lays a component tree out off screen, parents before children. */
    private static void layOut(Component at) {
        if (at instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    private static void write(JPanel shell, int width, String name)
            throws java.io.IOException {
        BufferedImage image = new BufferedImage(width, shell.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            shell.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
    }

    /** The chart's own preferred size, read from the production component. */
    private static Dimension chartSize() {
        return new juranometria.ui.ChartComponent(
                juranometria.app.Atlas.assembler(),
                juranometria.ui.language.PageText.in(
                        InterfaceText.forLanguage("en"))).getPreferredSize();
    }

    /**
     * Where the companion goes on one screen: beside the main window
     * at its packed chart size when the two fit side by side, and
     * otherwise over the main window's trailing edge, which is what an
     * owned window does on a screen too small for both.
     */
    static Rectangle[] place(Dimension screen, Dimension chart, int companion) {
        int gap = 8;
        Rectangle main = new Rectangle(chart);
        boolean beside = chart.width + gap + companion <= screen.width;
        int top = Math.max(0, (screen.height - chart.height) / 2);
        if (beside) {
            main.setLocation((screen.width - chart.width - gap - companion) / 2, top);
            return new Rectangle[] {main, new Rectangle(
                    main.x + main.width + gap, top, companion,
                    Math.min(chart.height, screen.height))};
        }
        main.setLocation(Math.max(0, (screen.width - chart.width) / 2), top);
        int x = Math.min(screen.width - companion, main.x + main.width - companion);
        return new Rectangle[] {main, new Rectangle(x, top, companion,
                Math.min(chart.height, screen.height))};
    }

    /** The two cases drawn on every screen: the packed window, and maximised. */
    static final String[] CASES = {"packed", "maximised"};

    private static Dimension windowFor(String which, Dimension screen,
                                       Dimension chart) {
        return which.equals("maximised") ? screen : chart;
    }

    private static BufferedImage placement(Dimension screen, Dimension chart) {
        double scale = 360.0 / screen.width;
        int w = 360;
        int h = (int) Math.round(screen.height * scale);
        BufferedImage image = new BufferedImage(2 * w + 16, h + 28,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D whole = image.createGraphics();
        whole.setColor(Color.WHITE);
        whole.fillRect(0, 0, image.getWidth(), image.getHeight());
        whole.dispose();
        for (int c = 0; c < CASES.length; c++) {
            Graphics2D g = image.createGraphics();
            g.translate(c * (w + 16), 0);
            try {
                drawCase(g, screen, windowFor(CASES[c], screen, chart),
                        CASES[c], scale, w, h);
            } finally {
                g.dispose();
            }
        }
        return image;
    }

    private static void drawCase(Graphics2D g, Dimension screen,
                                 Dimension window, String which,
                                 double scale, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h + 28);
        g.setColor(new Color(0xE8EBEF));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x8A9099));
        g.drawRect(0, 0, w - 1, h - 1);
        Rectangle[] at = place(screen, window, 360);
        String[] names = {"chart window", "companion"};
        Color[] fills = {new Color(0x20283A), new Color(0xF7F8FA)};
        Color[] inks = {Color.WHITE, new Color(0x20283A)};
        for (int i = 0; i < 2; i++) {
            Rectangle r = at[i];
            int x = (int) Math.round(r.x * scale);
            int y = (int) Math.round(r.y * scale);
            int rw = (int) Math.round(r.width * scale);
            int rh = (int) Math.round(r.height * scale);
            g.setColor(fills[i]);
            g.fillRect(x, y, rw, rh);
            g.setColor(new Color(0x5A6270));
            g.setStroke(new BasicStroke(1f));
            g.drawRect(x, y, rw, rh);
            g.setColor(inks[i]);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString(names[i], x + 6, y + 15);
        }
        // The owned companion is drawn on top: an owned window stays
        // above its owner, which is the overlap case's whole meaning.
        g.setColor(new Color(0x20283A));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(screen.width + " × " + screen.height + ", " + which
                + ": " + (at[1].x >= at[0].x + at[0].width
                        ? "beside" : "over the trailing edge"),
                4, h + 19);
    }

    private static void placementReport(StringBuilder out, Dimension chart) {
        out.append("## Placement at four screen sizes\n\n")
                .append("The chart window drawn at its chart's own preferred"
                        + " size, ")
                .append(chart.width).append(" × ").append(chart.height)
                .append(" (read from\n`ChartComponent`), with a 360-pixel"
                        + " companion beside it.\n\n")
                .append("A real chart window is larger than its chart -"
                        + " the toolbar, the title bar and an\nopen"
                        + " inspector add to it - so the packed case is the"
                        + " most room a\ncompanion can expect beside an"
                        + " unmaximised window. A maximised window\nleaves"
                        + " none, and the owned companion stands over its"
                        + " trailing edge.\n\n")
                .append("| screen | packed chart window | maximised |\n"
                        + "|---|---|---|\n");
        for (Dimension screen : SCREENS) {
            out.append("| ").append(screen.width).append(" × ")
                    .append(screen.height);
            for (String which : CASES) {
                Rectangle[] at = place(screen, windowFor(which, screen, chart), 360);
                out.append(" | ").append(at[1].x >= at[0].x + at[0].width
                        ? "beside" : "over the trailing edge");
            }
            out.append(" |\n");
        }
        out.append("\n");
    }

    private interface EdtWork<T> {
        T run() throws Exception;
    }

    private static <T> T onEdt(EdtWork<T> work) throws Exception {
        Object[] result = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = work.run();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }
}
