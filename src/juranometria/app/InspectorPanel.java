package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionDetails;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

/**
 * The chart inspector (Sprint 19, issue #170): what the thing the
 * reader pointed at actually is.
 *
 * <p>An <strong>observer</strong> of {@link SelectionModel}, never
 * its owner. It can be closed, or absent altogether, without
 * changing how selection is produced - the chart goes on answering
 * questions whether or not anything is listening.
 *
 * <p>It states only what the catalogue records, and says so when the
 * catalogue records nothing: measured over the bundled pack, 19.4%
 * of deep-sky objects have no position angle and 68.1% no visual
 * magnitude, so "not recorded" is the common case rather than the
 * exception. A blank is never allowed to read as a zero.
 *
 * <p>Nothing here moves the chart except {@code Centre here}, which
 * the reader presses deliberately.
 */
public final class InspectorPanel extends JPanel {

    private final Supplier<ChartScene> currentScene;
    private final SelectionModel selection;
    private final Consumer<Selection> centreOn;

    private final JLabel heading = new JLabel();
    private final JPanel facts = new JPanel();
    private final DefaultListModel<String> candidateNames =
            new DefaultListModel<>();
    private final JList<String> candidates = new JList<>(candidateNames);
    private final JScrollPane candidateScroll = new JScrollPane(candidates);
    /**
     * The reference frame, as canonical notation (#350).
     *
     * <p>Deliberately NOT an interface-language resource. A key whose
     * only valid translation is the original token invites the one
     * mistake it could ever suffer, and offers a translator a
     * decision they should not be asked to make. ICRS J2000 names a
     * coordinate system; it is the same six characters in every
     * language the atlas speaks.
     */
    static final String FRAME = "ICRS J2000";

    /**
     * Between two names for one candidate: {@code M 42 · NGC 1976}.
     *
     * <p>Canonical identity presentation, not grammar. The order is
     * atlas policy - preferred Messier identity, then catalogue
     * identity - and no language may reverse it, so this carries no
     * translation key.
     *
     * <p>It replaced three spaces, which were pretending to align a
     * column in a proportional font and did not. Real alignment
     * would need a list-cell renderer and a layout gap, not padding
     * inside a string (#350).
     */
    static final String IDENTITY_PAIR = " \u00b7 ";

    private final juranometria.ui.language.InterfaceText said;
    private final JButton centreHere;
    /**
     * The pane's own dismissal (issue #197). The toolbar toggle
     * remains the obvious way back, and this is where a reader looks
     * once the pane is open: its upper-right corner, as side panes
     * everywhere else are dismissed.
     */
    private final JButton close = new JButton(
            new com.formdev.flatlaf.extras.FlatSVGIcon(
                    "resources/icons/x.svg", 16, 16));
    private final Runnable unsubscribe;

    /** The two modes, and the chooser that appears when there are two. */
    public static final String SELECTED_MODE = "selected";
    public static final String PAGE_MODE = "page";
    private final java.awt.CardLayout deck = new java.awt.CardLayout();
    private final JPanel modes = new JPanel(deck);
    private final JPanel modeSwitch = new JPanel();
    private final javax.swing.JToggleButton showSelected =
            new javax.swing.JToggleButton("Selected");
    private final javax.swing.JToggleButton showPage =
            new javax.swing.JToggleButton("On this page");
    private final javax.swing.ButtonGroup modeGroup =
            new javax.swing.ButtonGroup();
    private String mode = SELECTED_MODE;
    /** What the heading says in the Selected mode, kept across a switch. */
    private String headingText = "Nothing selected";
    /** True while the panel is writing the list, so echoes are ignored. */
    private boolean updating;
    private boolean requested;
    private boolean fitsHere = true;
    private java.util.function.Consumer<Boolean> onVisibilityChange;
    private Runnable returnFocus = () -> { };
    private final Supplier<juranometria.render.ChartOptions> options;

    /** The working-set section (issue #261), empty until wired. */
    private final JPanel workingSet = new JPanel();
    private juranometria.chart.WorkingSelection working;
    private Supplier<juranometria.page.PageContents> pageContents;
    private Runnable unsubscribeWorking = () -> { };
    /** What the rows say, for tests; rebuilt with the section. */
    private final List<String> workingSetTexts = new java.util.ArrayList<>();
    private final java.util.Map<String, JButton> memberButtons =
            new java.util.HashMap<>();
    private final java.util.Map<String, JButton> removeButtons =
            new java.util.HashMap<>();
    private final JButton clearSelection;

    public InspectorPanel(SelectionModel selection,
                          Supplier<ChartScene> currentScene,
                          Supplier<juranometria.render.ChartOptions> options,
                          Consumer<Selection> centreOn) {
        // No language stated, so English - explicitly, never by a
        // lookup that would make the panel depend on when it was
        // built. The application states the reader's (#350).
        this(selection, currentScene, options, centreOn,
                juranometria.ui.language.InterfaceText.forLanguage(
                        juranometria.ui.language.InterfaceText.ENGLISH));
    }

    /** The same, speaking a language the caller states (#350). */
    public InspectorPanel(SelectionModel selection,
                          Supplier<ChartScene> currentScene,
                          Supplier<juranometria.render.ChartOptions> options,
                          Consumer<Selection> centreOn,
                          juranometria.ui.language.InterfaceText said) {
        this.said = said;
        // Built here, not in a field initialiser: those run before
        // the constructor body, so a button whose label comes from a
        // resource cannot be one (#350).
        this.centreHere = new JButton(said.say("inspector.centre.label"));
        this.clearSelection = new JButton(said.say("inspector.clear.label"));
        if (selection == null || currentScene == null || options == null
                || centreOn == null) {
            throw new IllegalArgumentException(
                    "the inspector needs a selection model, the current"
                            + " page, what that page draws, and a way"
                            + " to centre the chart");
        }
        this.options = options;
        this.selection = selection;
        this.currentScene = currentScene;
        this.centreOn = centreOn;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(PREFERRED_PANEL_WIDTH, 400));
        setMinimumSize(new Dimension(240, 200));
        getAccessibleContext().setAccessibleName(said.say("inspector.a11y"));
        getAccessibleContext().setAccessibleDescription(
                said.say("inspector.explain"));

        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setAlignmentX(0.0f);
        heading.getAccessibleContext().setAccessibleName(
                said.say("inspector.heading.a11y"));

        facts.setLayout(new BoxLayout(facts, BoxLayout.Y_AXIS));
        facts.setAlignmentX(0.0f);
        facts.setFocusable(true);
        // An explanatory sentence is broken against the width it
        // actually has, so it is re-broken whenever that width
        // changes rather than once against whatever it was first.
        facts.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                rewrapFacts();
            }
        });
        facts.getAccessibleContext().setAccessibleName(
                said.say("inspector.details.a11y"));

        candidates.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        candidates.getAccessibleContext().setAccessibleName(
                said.say("inspector.candidates.a11y"));
        juranometria.ui.Explain.control(candidates,
                said.say("inspector.candidates.hover"),
                said.say("inspector.candidates.explain"));
        candidates.addListSelectionListener(event -> {
            if (updating || event.getValueIsAdjusting()) {
                return;
            }
            int index = candidates.getSelectedIndex();
            if (index >= 0 && index < selection.candidates().size()) {
                selection.chooseCandidate(index);
            }
        });
        candidateScroll.setAlignmentX(0.0f);
        candidateScroll.setPreferredSize(new Dimension(280, 96));

        centreHere.getAccessibleContext().setAccessibleName(
                said.say("inspector.centre.a11y"));
        juranometria.ui.Explain.control(centreHere,
                said.say("inspector.centre.hover"),
                said.say("inspector.centre.explain"));
        centreHere.setAlignmentX(0.0f);
        centreHere.addActionListener(event -> centreOn.accept(
                selection.selection()));

        close.getAccessibleContext().setAccessibleName(
                said.say("inspector.close.a11y"));
        // Through the language's own joining pattern (#350): the
        // words were already translated here, but where the keystroke
        // sat and whether it was bracketed were still decided in
        // English for every language at once.
        juranometria.ui.Explain.control(close,
                juranometria.ui.language.ShortcutText.in(said).withKeystroke(
                        said.say("inspector.close.a11y"),
                        juranometria.ui.Shortcuts.INSPECTOR),
                said.say("inspector.close.explain"));
        // Quiet: an icon and its hover, not a bordered button
        // competing with the heading beside it.
        close.putClientProperty("JButton.buttonType", "toolBarButton");
        close.setFocusable(true);
        // Straight to the requested-visibility the toolbar toggle
        // writes, so there is one wish and one switch. Not
        // setVisible: that would hide the pane behind the toggle's
        // back and leave the toolbar and the menu claiming a panel
        // that is not there.
        close.addActionListener(event -> dismiss());

        // The heading and its dismissal on one line, the button
        // pushed to the trailing edge.
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setAlignmentX(0.0f);
        header.add(heading);
        header.add(Box.createHorizontalGlue());
        header.add(close);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                close.getPreferredSize().height));

        // The two modes. "Selected" is what the Inspector has
        // always been; "On this page" is a module's, handed in from
        // outside so this panel never learns what a table is. The
        // switch appears only when something has been handed in -
        // one mode needs no chooser.
        JPanel selectedMode = new JPanel();
        selectedMode.setLayout(new BoxLayout(selectedMode, BoxLayout.Y_AXIS));
        selectedMode.setAlignmentX(0.0f);
        selectedMode.add(candidateScroll);
        selectedMode.add(gap(10));
        selectedMode.add(facts);
        selectedMode.add(gap(12));
        workingSet.setLayout(new BoxLayout(workingSet, BoxLayout.Y_AXIS));
        workingSet.setAlignmentX(0.0f);
        workingSet.setVisible(false);
        workingSet.getAccessibleContext().setAccessibleName(
                said.say("inspector.workingset.a11y"));
        workingSet.getAccessibleContext().setAccessibleDescription(
                said.say("inspector.workingset.explain"));
        selectedMode.add(workingSet);
        selectedMode.add(stretch());
        selectedMode.add(centreHere);
        modes.add(selectedMode, SELECTED_MODE);

        // Two equal halves of whatever room there is, rather than
        // two natural widths in a row that runs off the edge. At
        // 320 px and 18 pt the row overflowed and the second mode
        // read "O" - a control a reader can neither read nor reach,
        // and the one that opens the whole feature (#217 review).
        modeSwitch.setLayout(new java.awt.GridLayout(1, 2, 6, 0));
        modeSwitch.setAlignmentX(0.0f);
        modeSwitch.setVisible(false);
        showSelected.setSelected(true);
        modeGroup.add(showSelected);
        modeGroup.add(showPage);
        showSelected.getAccessibleContext().setAccessibleName(
                said.say("inspector.tab.selected"));
        juranometria.ui.Explain.control(showSelected,
                said.say("inspector.tab.selected.hover"),
                said.say("inspector.tab.selected.explain"));
        showPage.getAccessibleContext().setAccessibleName(
                said.say("inspector.tab.page"));
        juranometria.ui.Explain.control(showPage,
                said.say("inspector.tab.page.hover"),
said.say("inspector.tab.page.explain"));
        showSelected.addActionListener(event -> showMode(SELECTED_MODE));
        showPage.addActionListener(event -> showMode(PAGE_MODE));
        modeSwitch.add(showSelected);
        modeSwitch.add(showPage);
        fitModeChooser();

        modes.setAlignmentX(0.0f);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setAlignmentX(0.0f);
        body.add(header);
        body.add(gap(10));
        body.add(modeSwitch);
        body.add(gap(10));
        body.add(modes);
        add(body, BorderLayout.CENTER);

        // Escape closes the inspector, as it closes every dialog.
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dismiss();
            }
        });

        // Enter settles on the candidate the reader walked to with
        // the arrow keys and moves focus into the facts, which is
        // where the answer they were after is written.
        candidates.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "settle");
        candidates.getActionMap().put("settle",
                new javax.swing.AbstractAction() {
                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent event) {
                        focusTarget().requestFocusInWindow();
                    }
                });

        // Closed until the reader asks for it, and its own state
        // says so from the start: a panel that is visible by Swing's
        // default while reporting that nobody asked for it would
        // announce its first appearance to nobody.
        setVisible(false);
        this.unsubscribe = selection.onChange(this::show);
    }

    /**
     * The width the reviewed layout needs beside the panel: the
     * chart keeps at least 400 px of page, the panel at least 240.
     * Below that the panel yields - the chart never becomes a sliver
     * so that a panel can keep its width.
     */
    public static final int MINIMUM_CHART_WIDTH = 400;
    public static final int MINIMUM_PANEL_WIDTH = 240;
    public static final int PREFERRED_PANEL_WIDTH = 320;

    /** Whether a window this wide can show both (issue #170). */
    public static boolean fitsBeside(int windowWidth) {
        return windowWidth >= MINIMUM_CHART_WIDTH + MINIMUM_PANEL_WIDTH;
    }

    /**
     * What the reader asked for, which is not always what fits.
     * Toggling records the wish; the window's width decides whether
     * it can be honoured, and a window that widens again restores
     * what the reader wanted rather than making them ask twice.
     */
    public void setRequestedVisible(boolean wanted) {
        this.requested = wanted;
        applyVisibility();
    }

    /** Whether the reader currently wants the inspector. */
    public boolean isRequestedVisible() {
        return requested;
    }

    /**
     * Whether the window is wide enough to show the panel at all -
     * what a control must know before claiming the panel is there
     * (issue #180).
     */
    public boolean canShow() {
        return fitsHere;
    }

    /**
     * The width the panel may take beside a window this wide: what
     * is left once the chart has its 400 px, never more than the
     * preferred 320 and never less than the floor of 240.
     *
     * <p>Without this the rule was only half kept (review): 640 px
     * passed {@link #fitsBeside}, and then the panel took its full
     * preferred 320, leaving the chart 320 - the very squeeze the
     * decision said the panel would absorb.
     */
    public static int widthBeside(int windowWidth) {
        int spare = windowWidth - MINIMUM_CHART_WIDTH;
        return Math.max(MINIMUM_PANEL_WIDTH,
                Math.min(PREFERRED_PANEL_WIDTH, spare));
    }

    /** Tells the panel how much room the window has. */
    public void setAvailableWidth(int windowWidth) {
        boolean fits = fitsBeside(windowWidth);
        if (fits) {
            // Take the leftovers, not the preference: the chart's
            // 400 px comes first.
            int width = widthBeside(windowWidth);
            setPreferredSize(new Dimension(width, getPreferredSize().height));
            setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        }
        if (fits != fitsHere) {
            this.fitsHere = fits;
        }
        applyVisibility();
    }

    private void applyVisibility() {
        boolean shown = requested && fitsHere;
        if (shown != isVisible()) {
            setVisible(shown);
        }
        // Announced whenever it might have changed, not only when it
        // did (review): a checkbox menu item flips itself when it is
        // clicked, so a toggle that a narrow window refuses must
        // still be told what actually happened - otherwise the menu
        // shows a panel that is not there.
        if (onVisibilityChange != null) {
            onVisibilityChange.accept(shown);
        }
    }

    /** Told whenever the panel appears or disappears, for the menu. */
    public void onVisibilityChange(java.util.function.Consumer<Boolean> sink) {
        this.onVisibilityChange = sink;
    }

    /**
     * Re-reads the page. Called when the chart has been navigated:
     * the selection has not changed, but what the page can say about
     * it has - an object the reader panned away from is no longer
     * described as though it were still in front of them.
     */
    public void refresh() {
        show(new SelectionModel.Change(selection.selection(),
                selection.candidates(), selection.currentIndex()));
        // The membership has not changed, but which members this
        // page holds has - the off-page words follow the page.
        rebuildWorkingSet();
    }

    /**
     * Where Enter takes the reader from the candidate list: into the
     * facts, which is where the answer they were walking towards is
     * written - not onward to the button that would move the chart
     * (review).
     */
    public JComponent focusTarget() {
        return facts;
    }

    /**
     * Closes the pane the way both of its own controls close it -
     * Escape, and the heading's button - through the requested
     * visibility the toolbar toggle writes, handing the reader back
     * to the chart rather than leaving focus in a panel that is no
     * longer there.
     *
     * <p>What this does not touch: navigation, the searched target,
     * the chart options, and the selection - which survives with its
     * candidate list, so reopening shows the same answer.
     *
     * <p>What it does cause, and is not hidden here (review): the
     * chart takes the pane's width back, and
     * {@code ChartComponent} assembles a page whenever it is
     * resized. So a scene is assembled and the catalogue is asked -
     * for the same centre, field width, limiting magnitude and
     * target, at a new size. That is the chart filling the paper,
     * not this control navigating, and the toolbar toggle has always
     * done exactly the same.
     */
    private void dismiss() {
        setRequestedVisible(false);
        returnFocus.run();
    }

    /** The pane's own close control; for tests. */
    /**
     * Puts a module's view in the second mode.
     *
     * <p>The panel is given the component and never asks what it is:
     * the Inspector keeps its own mode, its close button, its
     * Escape, its narrow-window behaviour and its accessible naming
     * whether or not a module is installed.
     */
    public void showPageView(JComponent view) {
        if (view == null) {
            throw new IllegalArgumentException("a view to show");
        }
        modes.add(view, PAGE_MODE);
        modeSwitch.setVisible(true);
        revalidate();
    }

    /**
     * Wires the <strong>Working set</strong> section (issue #261,
     * mock-ups reviewed by the #258 gate): every member across
     * pages, in joining order, the lead marked and bold, off-page
     * members labelled in words rather than colour alone, a
     * per-member remove, and Clear selection. Choosing a member
     * makes it the lead; the section never drops a member because
     * the chart moved.
     *
     * <p>Handed in like the page view: the panel observes the model
     * and asks the page only which members it holds, so the
     * Inspector keeps working - without the section - when nothing
     * wires it.
     */
    public void showWorkingSet(
            juranometria.chart.WorkingSelection working,
            Supplier<juranometria.page.PageContents> pageContents) {
        if (working == null || pageContents == null) {
            throw new IllegalArgumentException(
                    "the section needs the working selection and the"
                            + " page it is judged against");
        }
        if (this.working != null) {
            throw new IllegalStateException(
                    "the working set is wired once: a second wiring"
                            + " would double every control's action");
        }
        this.working = working;
        this.pageContents = pageContents;
        clearSelection.getAccessibleContext().setAccessibleName(
                "Clear selection");
        juranometria.ui.Explain.control(clearSelection,
                "Remove every working mark",
                "Empties the whole working selection. The page and"
                        + " your place in it are left alone.");
        clearSelection.addActionListener(event -> working.clear());
        this.unsubscribeWorking =
                working.onChange(change -> rebuildWorkingSet());
    }

    /**
     * The section, rebuilt whole from the model and the current
     * page: membership and order from the one truth, on-page or not
     * from the inventory - never a private copy of either.
     */
    private void rebuildWorkingSet() {
        if (working == null) {
            return;
        }
        workingSet.removeAll();
        workingSetTexts.clear();
        memberButtons.clear();
        removeButtons.clear();
        List<String> members = working.members();
        workingSet.setVisible(!members.isEmpty());
        if (!members.isEmpty()) {
            JLabel title = new JLabel(said.say("inspector.workingset.title",
                    String.valueOf(members.size())));
            title.putClientProperty("FlatLaf.styleClass", "h4");
            title.setAlignmentX(0.0f);
            workingSet.add(title);
            juranometria.page.PageContents page = pageContents.get();
            for (String member : members) {
                boolean leads = member.equals(working.lead());
                // The inventory is the one page boundary (review):
                // it carries an entry for every on-paper object,
                // unnamed stars included - only the table's listing
                // filters them - while the scene deliberately holds
                // a query margin beyond the paper, so asking the
                // scene would call a star "on this page" that is
                // not.
                boolean offPage = page == null
                        || page.find(member).isEmpty();
                workingSet.add(memberRow(member, leads, offPage));
                workingSetTexts.add((leads ? "◉ " : "") + member
                        + (offPage ? " — off this page" : ""));
            }
            clearSelection.setAlignmentX(0.0f);
            workingSet.add(Box.createVerticalStrut(6));
            workingSet.add(clearSelection);
        }
        workingSet.revalidate();
        workingSet.repaint();
        revalidate();
        repaint();
    }

    /** One member's row: lead mark, name, off-page word, remove. */
    private JComponent memberRow(String member, boolean leads,
                                 boolean offPage) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(0.0f);
        JLabel leadMark = new JLabel(leads ? "◉ " : "   ");
        row.add(leadMark);
        JButton name = new JButton(member);
        name.putClientProperty("JButton.buttonType", "toolBarButton");
        name.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        if (leads) {
            name.setFont(name.getFont().deriveFont(java.awt.Font.BOLD));
        }
        // Four whole forms, not a name with English clauses stuck
        // on it. The old shape appended ", lead" and ", off this
        // page" in that order with that punctuation, which is one
        // language's way of qualifying a noun (#350).
        name.getAccessibleContext().setAccessibleName(said.say(
                leads ? (offPage ? "inspector.member.a11y.lead.offpage"
                                 : "inspector.member.a11y.lead")
                      : (offPage ? "inspector.member.a11y.offpage"
                                 : "inspector.member.a11y"),
                member));
        juranometria.ui.Explain.control(name,
                // "Show M 31's facts" put an English possessive on a
                // catalogue designation. A designation takes no
                // inflection in any language the atlas speaks.
                said.say("inspector.member.hover", member),
                said.say("inspector.member.explain"));
        name.addActionListener(event -> working.lead(member));
        row.add(name);
        if (offPage) {
            // The gap is the layout's, not the sentence's. This text
            // used to begin with a space, which did the spacing and
            // would have been stripped by the first translator who
            // trimmed their line - taking the gap with it (#350).
            row.add(Box.createHorizontalStrut(4));
            JLabel off = new JLabel(said.say("inspector.member.offpage"));
            off.setFont(off.getFont().deriveFont(java.awt.Font.ITALIC));
            off.setEnabled(false);
            row.add(off);
        }
        row.add(Box.createHorizontalGlue());
        // The glyph is a control mark, not prose: it stays the same
        // in every language, and only what is SAID about it is
        // localised (#350).
        JButton remove = new JButton("\u2715");
        remove.putClientProperty("JButton.buttonType", "toolBarButton");
        remove.getAccessibleContext().setAccessibleName(
                said.say("inspector.member.remove", member));
        juranometria.ui.Explain.control(remove,
                said.say("inspector.member.remove.hover", member),
                said.say("inspector.member.remove.explain", member));
        remove.addActionListener(event -> working.remove(member));
        row.add(remove);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(name.getPreferredSize().height,
                        remove.getPreferredSize().height)));
        memberButtons.put(member, name);
        removeButtons.put(member, remove);
        return row;
    }

    /** The section's rows as words, top to bottom; for tests. */
    public List<String> workingSetLines() {
        return List.copyOf(workingSetTexts);
    }

    /** Whether the section is on screen at all. */
    public boolean workingSetShown() {
        return workingSet.isVisible();
    }

    public JButton clearSelectionButton() {
        return clearSelection;
    }

    public JButton workingSetMemberButton(String identity) {
        return memberButtons.get(identity);
    }

    public JButton workingSetRemoveButton(String identity) {
        return removeButtons.get(identity);
    }

    /**
     * The heading for the Selected mode. Remembered rather than
     * written straight to the label, so a reader who looks at the
     * page and comes back finds the object they were reading about
     * still named above its facts.
     */
    private void setSelectedHeading(String text) {
        headingText = text;
        if (SELECTED_MODE.equals(mode)) {
            heading.setText(text);
        }
    }

    /**
     * The chooser decides its shape whenever the panel is laid out.
     *
     * <p>Not from a resize event: those arrive through the event
     * queue, so a panel that has just been sized has not been told
     * yet - and the chooser went on laying itself out for the width
     * it used to have. Laying out is the moment the room is known.
     */
    @Override
    public void doLayout() {
        fitModeChooser();
        super.doLayout();
    }

    /**
     * Side by side while both names fit; stacked when they do not.
     *
     * <p>The names are the modes, and a mode a reader cannot read is
     * a mode they will not find. At enlarged text "On this page"
     * needs more than half of a 320 px sidebar, so the two share the
     * width only while that is honest and take a line each when it
     * is not - two rows of readable control being worth more than
     * one row of "S…" and "O…".
     */
    private void fitModeChooser() {
        int available = getWidth() - getInsets().left - getInsets().right;
        // Twice the wider name, not the sum of the two: equal
        // halves mean the wider one decides, and comparing the sum
        // left "On this page" 16 px short of its own half.
        int needed = 2 * Math.max(showSelected.getPreferredSize().width,
                showPage.getPreferredSize().width) + 6;
        boolean sideBySide = available <= 0 || available >= needed;
        modeSwitch.setLayout(sideBySide
                ? new java.awt.GridLayout(1, 2, 6, 0)
                : new java.awt.GridLayout(2, 1, 0, 4));
        int rows = sideBySide ? 1 : 2;
        int height = Math.max(showSelected.getPreferredSize().height,
                showPage.getPreferredSize().height) * rows
                + (rows - 1) * 4;
        modeSwitch.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        modeSwitch.setPreferredSize(new Dimension(
                modeSwitch.getPreferredSize().width, height));
    }

    /**
     * Vertical space that does not drag the column off the left.
     *
     * <p>A {@code BoxLayout} lines its children up on one axis, and
     * a plain strut sits at 0.5 while the panels beside it sit at
     * 0.0 - so the column's axis lands somewhere in between and
     * every left-aligned child is pushed in and squeezed. That is
     * how the mode chooser came to be 171 px wide inside a 288 px
     * panel, and why its second button read "O" (#217 review).
     */
    private static java.awt.Component gap(int height) {
        java.awt.Component strut = Box.createVerticalStrut(height);
        ((JComponent) strut).setAlignmentX(0.0f);
        return strut;
    }

    private static java.awt.Component stretch() {
        java.awt.Component glue = Box.createVerticalGlue();
        ((JComponent) glue).setAlignmentX(0.0f);
        return glue;
    }

    /**
     * Whether the reader is being offered a choice of modes.
     *
     * <p>Asked of the panel rather than of the buttons: a Swing
     * component reports itself visible even when the container
     * holding it is not, so a test that asked the buttons would be
     * told a chooser is on screen when nothing is.
     */
    public boolean modeChooserShown() {
        return modeSwitch.isVisible();
    }

    /** Which mode the reader is in. */
    public String mode() {
        return mode;
    }

    /** Switches mode, as the chooser does. */
    public void showMode(String which) {
        if (!SELECTED_MODE.equals(which) && !PAGE_MODE.equals(which)) {
            throw new IllegalArgumentException("no such mode: " + which);
        }
        mode = which;
        deck.show(modes, which);
        (SELECTED_MODE.equals(which) ? showSelected : showPage)
                .setSelected(true);
        heading.setText(SELECTED_MODE.equals(which) ? headingText
                : "On this page");
        revalidate();
        repaint();
    }

    public javax.swing.JToggleButton selectedModeButton() {
        return showSelected;
    }

    public javax.swing.JToggleButton pageModeButton() {
        return showPage;
    }

    public JButton closeButton() {
        return close;
    }

    /** Where focus goes when the inspector closes: the chart. */
    public void onClose(Runnable focusChart) {
        this.returnFocus = focusChart == null ? () -> { } : focusChart;
    }

    /** Stops observing - the panel can be discarded safely. */
    public void dispose() {
        unsubscribe.run();
        unsubscribeWorking.run();
    }

    /**
     * The panel's current text, top to bottom; for tests.
     *
     * <p>A wrapped fact answers with the sentence it was given, not
     * with the markup layout broke it into. The line breaks belong to
     * a width; the words do not, and a reader of this list - a test,
     * an inventory, a translation review - is asking about the words.
     * Chart Options learned this the other way round: its companion
     * report deleted {@code <br>} rather than replacing it and welded
     * "angitt i katalogen" into "angitt ikatalogen", which a person
     * caught by reading it and no test did.
     */
    public List<String> lines() {
        List<String> lines = new java.util.ArrayList<>();
        lines.add(heading.getText());
        for (java.awt.Component component : facts.getComponents()) {
            if (component instanceof JLabel label) {
                Object whole = label.getClientProperty(WRAPPED_FACT);
                lines.add(whole == null ? label.getText() : (String) whole);
            }
        }
        return List.copyOf(lines);
    }

    /** The candidate list as the reader sees it; for tests. */
    public List<String> candidateLines() {
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < candidateNames.size(); i++) {
            lines.add(candidateNames.get(i));
        }
        return List.copyOf(lines);
    }

    private void show(SelectionModel.Change change) {
        ChartScene scene = currentScene.get();
        facts.removeAll();
        updating = true;
        candidateNames.clear();
        for (Selection.Object candidate : change.candidates()) {
            candidateNames.addElement(nameOf(scene, candidate));
        }
        candidateScroll.setVisible(change.isAmbiguous());
        if (change.currentIndex() >= 0) {
            candidates.setSelectedIndex(change.currentIndex());
        }
        updating = false;

        Selection current = change.selection();
        centreHere.setEnabled(!(current instanceof Selection.None));
        if (current instanceof Selection.None) {
            setSelectedHeading(said.say("inspector.none.heading"));
            fact(said.say("inspector.none.hint"));
        } else if (current instanceof Selection.EmptySky empty) {
            setSelectedHeading(said.say("inspector.emptysky.heading"));
            fact(coordinates(empty.position()));
            fact(FRAME);
            fact("");
            fact(said.say("inspector.emptysky.fact"), true);
        } else {
            Selection.Object object = (Selection.Object) current;
            if (change.isAmbiguous()) {
                // The count is data beside the heading, not a word
                // inflected by it. Embedding a plural rule would
                // encode English's - and then Norwegian's, and then
                // every language whose rules this schema does not
                // model (#350).
                setSelectedHeading(said.say("inspector.objects.here",
                        String.valueOf(change.candidates().size())));
            }
            SelectionDetails.star(scene, current)
                    .ifPresentOrElse(star -> describeStar(star, change),
                            () -> SelectionDetails.deepSky(scene, current)
                                    .ifPresentOrElse(
                                            dso -> describeDeepSkyIfDrawn(
                                                    scene, dso, object, change),
                                            () -> describeAbsent(object)));
        }
        rewrapFacts();
        revalidate();
        repaint();
    }

    private void describeStar(Star star, SelectionModel.Change change) {
        StarIdentity identity = star.identity();
        if (!change.isAmbiguous()) {
            // The best name this star has: its own, then its Bayer
            // letter, then its Flamsteed number, then its catalogue
            // identifier. Heading a lettered star merely "Star"
            // withholds something the atlas knows.
            setSelectedHeading(bestName(identity, star.id()));
        }
        // Whole forms, not a designation with an English word
        // bracketed onto it (#350). The designation is canonical
        // data and goes in as an argument; the framing around it is
        // the translation's.
        fact(identity != null && identity.bayer() != null
                ? said.say("inspector.star.bayer", identity.bayer())
                : said.say("inspector.star.bayer.none"));
        fact(identity != null && identity.flamsteed() != null
                ? said.say("inspector.star.flamsteed", identity.flamsteed())
                : said.say("inspector.star.flamsteed.none"));
        fact(star.id());
        fact("");
        fact(said.say("inspector.magnitude.visual",
                magnitudeValue(star.magnitude())));
        fact(coordinates(star.position()));
        fact(FRAME);
    }

    private void describeDeepSky(DeepSkyObject dso,
                                 SelectionModel.Change change) {
        if (!change.isAmbiguous()) {
            setSelectedHeading(messierName(dso).orElse(dso.id()));
        }
        // Say each name once: an object headed by its own catalogue
        // id should not then list that id as a fact about itself.
        if (!heading.getText().equals(dso.id())) {
            fact(dso.id());
        }
        String others = dso.aliases().stream()
                .filter(alias -> !alias.equals(heading.getText()))
                .collect(java.util.stream.Collectors.joining(", "));
        if (!others.isEmpty()) {
            fact(others);
        }
        fact(readableType(dso));
        fact("");
        fact(magnitudeLine(dso, said));
        fact(sizeLine(dso, said));
        fact(coordinates(dso.position()));
        fact(FRAME);
    }

    /**
     * The selection names something this page no longer draws - the
     * reader panned away. The panel says exactly that instead of
     * inventing facts or going quiet.
     */
    /**
     * A selected object the page no longer draws is reported as
     * absent, not described as though it were there (issue #196).
     *
     * <p>Selection outlives presentation deliberately - it is
     * UI-independent state a future module will read - so switching
     * a family off does not clear what the reader chose. But the
     * panel must not go on reciting the facts of a symbol that is no
     * longer on the paper. The question is asked of production's own
     * rule, {@link juranometria.render.ChartRenderer#permitted}, so
     * the panel and the drawing cannot come to disagree about what is
     * on the page.
     */
    private void describeDeepSkyIfDrawn(ChartScene scene,
                                        juranometria.chart.DeepSkyObject dso,
                                        Selection.Object object,
                                        SelectionModel.Change change) {
        if (juranometria.render.ChartRenderer.permitted(scene, dso,
                options.get())) {
            describeDeepSky(dso, change);
        } else {
            // Held by the page, but the reader's own switches are
            // keeping it off the chart. A different fact from being
            // absent, and the one a reader can act on (#350).
            describeUnshown(object,
                    said.say("inspector.hidden.byoptions"));
        }
    }

    private void describeAbsent(Selection.Object object) {
        // The identity is not in this scene at all. "Not on this page
        // ANY MORE" assumed a navigation history the state does not
        // prove: the reader may never have been anywhere else.
        describeUnshown(object, said.say("inspector.absent"));
    }

    /**
     * An object the chart is not showing, and why.
     *
     * <p>Two callers, two different reasons, one shape. Hidden by a
     * switch is something the reader can undo; absent from the page
     * is something they cannot, and telling them the same sentence
     * for both leaves the actionable case looking hopeless.
     */
    private void describeUnshown(Selection.Object object, String why) {
        setSelectedHeading(object.catalogueId());
        fact(coordinates(object.position()));
        fact(FRAME);
        fact("");
        fact(why, true);
    }

    /**
     * The magnitude with its band named - never a blue magnitude
     * labelled visual, and never a blank where the catalogue is
     * silent.
     */
    static String magnitudeLine(DeepSkyObject dso,
                                juranometria.ui.language.InterfaceText said) {
        return switch (dso.recorded().band()) {
            case VISUAL -> said.say("inspector.magnitude.visual",
                    magnitudeValue(dso.magnitude()));
            case BLUE -> said.say("inspector.magnitude.blue",
                    magnitudeValue(dso.magnitude()));
            case NONE -> said.say("inspector.magnitude.none");
        };
    }

    /**
     * A magnitude, formatted the same way everywhere.
     *
     * <p>Locale.ROOT on purpose. A magnitude is a measurement the
     * atlas states, not a number the reader's desktop may punctuate:
     * 2.96 is 2.96 in every language this atlas speaks, and a comma
     * there would read as a different value to anyone comparing it
     * with a catalogue.
     */
    static String magnitudeValue(double magnitude) {
        return String.format(Locale.ROOT, "%.2f", magnitude);
    }

    /** The extent and orientation, or the fact that neither is known. */
    static String sizeLine(DeepSkyObject dso,
                           juranometria.ui.language.InterfaceText said) {
        if (!dso.recorded().hasSize()) {
            return said.say("inspector.size.none");
        }
        // Four whole forms, not an extent with a clause appended.
        // The measurements are arguments, formatted Locale.ROOT for
        // the same reason magnitudes are: an arcminute figure is a
        // value the atlas states, not a number the desktop
        // punctuates (#350).
        // Four forms. A single recorded axis may still carry a
        // position angle: it states the orientation of the extent
        // that WAS measured, even where the minor axis is unknown.
        // The shipped pack happens to contain none, but the data
        // model permits it, and dropping the form would discard
        // recorded information from a later catalogue (#350).
        boolean twoAxes = dso.recorded().minorAxisArcmin() != null;
        String major = String.format(Locale.ROOT, "%.1f",
                dso.recorded().majorAxisArcmin());
        String minor = twoAxes
                ? String.format(Locale.ROOT, "%.1f",
                        dso.recorded().minorAxisArcmin())
                : null;
        if (!dso.recorded().hasPositionAngle()) {
            return twoAxes
                    ? said.say("inspector.size.axes", major, minor)
                    : said.say("inspector.size.across", major);
        }
        String angle = String.format(Locale.ROOT, "%.0f",
                dso.recorded().positionAngleDegrees());
        return twoAxes
                ? said.say("inspector.size.axes.pa", major, minor, angle)
                : said.say("inspector.size.across.pa", major, angle);
    }

    /** The catalogue's type in words, not as an enum constant. */
    static String readableType(DeepSkyObject dso) {
        String name = dso.type().name().toLowerCase(Locale.ROOT)
                .replace('_', ' ');
        if (name.equals("other")) {
            return "type not classified";
        }
        // One type does not survive lowercasing: an H II region is
        // named after a spectroscopic notation, not a word, and
        // "hii region" reads as a typo (Sprint 21 gate, #184).
        return name.equals("hii region") ? "H II region" : name;
    }

    /** The most telling name a star has, never less than its id. */
    static String bestName(StarIdentity identity, String catalogueId) {
        if (identity == null) {
            return catalogueId;
        }
        if (identity.name() != null) {
            return identity.name();
        }
        if (identity.bayer() != null) {
            return identity.bayer() + "  (Bayer)";
        }
        if (identity.flamsteed() != null) {
            return identity.flamsteed() + "  (Flamsteed)";
        }
        return catalogueId;
    }

    private static java.util.Optional<String> messierName(DeepSkyObject dso) {
        return dso.aliases().stream().filter(a -> a.startsWith("M "))
                .findFirst();
    }

    private String nameOf(ChartScene scene, Selection.Object candidate) {
        if (candidate.kind() == Selection.Object.Kind.STAR) {
            return SelectionDetails.star(scene, candidate)
                    .map(star -> {
                        StarIdentity identity = star.identity();
                        return String.format(Locale.ROOT, "%s   V %.1f",
                                bestName(identity, star.id()),
                                star.magnitude());
                    })
                    .orElse(candidate.catalogueId());
        }
        return SelectionDetails.deepSky(scene, candidate)
                .map(dso -> messierName(dso)
                        .map(m -> m + IDENTITY_PAIR + dso.id())
                        .orElse(dso.id()))
                .orElse(candidate.catalogueId());
    }

    private void fact(String text) {
        fact(text, false);
    }

    /**
     * A fact, optionally allowed to break across lines.
     *
     * <p>Designations, coordinates and magnitudes are short and fixed,
     * and a break inside one would read as two values. An EXPLANATION
     * is a sentence, and a sentence that runs past the panel's edge
     * loses its ending - which a translation is likelier to do,
     * because this width was settled against English (#350). There is
     * ample vertical room here, so the sentence wraps rather than the
     * translation being cut to fit an English measurement.
     *
     * <p>The spoken name stays the whole sentence either way. A screen
     * reader must not inherit a layout's line breaks.
     */
    private void fact(String text, boolean explanatory) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(0.0f);
        if (!text.isEmpty()) {
            label.getAccessibleContext().setAccessibleName(text);
        }
        if (explanatory && !text.isEmpty()) {
            label.putClientProperty(WRAPPED_FACT, text);
        }
        facts.add(label);
    }

    /** Marks a fact whose text may be broken across lines. */
    private static final String WRAPPED_FACT =
            "juranometria.inspector.wrappedFact";

    /**
     * Breaks the explanatory facts against the width they have.
     *
     * <p>Done after layout, as Chart Options does it: a label knows
     * its width only once it has one, and measuring before that wraps
     * against a number nothing will honour. The unwrapped sentence is
     * kept on the label, so a second pass re-breaks the original
     * rather than re-breaking its own markup.
     */
    private void rewrapFacts() {
        int available = facts.getWidth();
        if (available <= 40) {
            return;
        }
        boolean changed = false;
        for (java.awt.Component child : facts.getComponents()) {
            if (!(child instanceof JLabel label)) {
                continue;
            }
            Object prose = label.getClientProperty(WRAPPED_FACT);
            if (prose == null) {
                continue;
            }
            String broken = juranometria.ui.WrappedText.html(
                    (String) prose, available,
                    label.getFontMetrics(label.getFont()));
            if (!broken.equals(label.getText())) {
                label.setText(broken);
                changed = true;
            }
        }
        if (changed) {
            facts.revalidate();
        }
    }

    private static String coordinates(SkyPosition position) {
        double hours = position.raDegrees() / 15.0;
        int h = (int) hours;
        double minutes = (hours - h) * 60.0;
        char sign = position.decDegrees() < 0 ? '−' : '+';
        double absolute = Math.abs(position.decDegrees());
        int d = (int) absolute;
        int m = (int) Math.round((absolute - d) * 60.0);
        return String.format(Locale.ROOT, "%dh %04.1fm   %c%d° %02d′",
                h, minutes, sign, d, m);
    }

    /** The panel, wrapped so callers need not know its type. */
    public JComponent component() {
        return this;
    }
}
