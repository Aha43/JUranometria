package juranometria.ui.companion;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

import juranometria.ui.language.InterfaceText;

/**
 * The Controller's pages (#507, Sprint 47): one row of tabs, each
 * holding content that scrolls on its own. The first tab, Chart, is
 * always there and holds the collapsible sections the window has had
 * since #434 - the chart's controls, Place and Time, Chart Options; a
 * host adds further tabs, Solar System first, whose content it builds
 * itself, so a tab that is itself a subject carries no outer
 * collapsible group repeating its own name.
 *
 * <p><strong>Choosing a tab computes nothing.</strong> Every tab's
 * content is built when the Controller is, over authorities it shows
 * and never drives; the strip only shows one page and hides the rest,
 * and what a hidden page's controls hold - a computed Venus, a typed
 * instant - stays as it was. Which tab was chosen is presentation,
 * remembered in the {@link CompanionStore} beside the sections'
 * collapse; a profile that never chose opens Chart.
 *
 * <p>A component rather than the window, so the packaged acceptance,
 * which has no display, can hold the tabs to their contract where the
 * window itself is the display suite's to prove.
 */
public final class CompanionTabs extends JTabbedPane {

    /**
     * The Chart tab's id: the key of its title, under which nothing is
     * remembered but its choice.
     */
    public static final String CHART = "companion.tab.chart";

    private final CompanionStore store;
    private final InterfaceText said;
    private final JPanel sections = new JPanel();
    private final JScrollPane chartScroll;
    private final List<CompanionSection> held = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private final List<JComponent> contents = new ArrayList<>();

    public CompanionTabs(InterfaceText said, CompanionStore store) {
        this.store = store;
        this.said = said;
        setName("companionTabs");
        getAccessibleContext().setAccessibleName(said.say("companion.tabs.a11y"));
        // The two titles are the whole meaning; a tooltip repeating a
        // tab's own word is a box in the way of reading it.
        juranometria.ui.Explain.selfExplanatory(this, said.say("companion.tabs.explain"));
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        chartScroll = scrolling(sections);
        ids.add(CHART);
        contents.add(sections);
        addTab(said.say(CHART), chartScroll);
        addChangeListener(event -> {
            int at = getSelectedIndex();
            if (at >= 0) {
                store.saveTab(ids.get(at));
            }
        });
    }

    /**
     * Adds a section to the Chart tab, collapsed as it was last left -
     * or, never chosen, as the host says.
     */
    public CompanionSection addSection(String id, String title, JComponent content,
                                       boolean collapsedIfNeverChosen) {
        CompanionSection section = new CompanionSection(id, title, content,
                said, store.collapsed(id, collapsedIfNeverChosen), collapsed -> {
                    store.saveCollapsed(id, collapsed);
                    sections.revalidate();
                });
        held.add(section);
        sections.add(section);
        return section;
    }

    /** The Chart tab's sections, in order. */
    public List<CompanionSection> sections() {
        return List.copyOf(held);
    }

    /**
     * Adds a tab after the ones there, holding content the host built;
     * chosen at once if it is the tab the reader last chose.
     *
     * @param id      a stable name, under which its choice is remembered
     * @param title   its tab, in the window's language
     * @param content what it holds; told nothing of the strip
     */
    public void addPage(String id, String title, JComponent content) {
        if (ids.contains(id)) {
            throw new IllegalArgumentException("a tab is added once: " + id);
        }
        ids.add(id);
        contents.add(content);
        addTab(title, scrolling(content));
        if (store.tab().filter(id::equals).isPresent()) {
            setSelectedIndex(ids.size() - 1);
        }
    }

    /** The tabs' ids, in order: Chart first. */
    public List<String> ids() {
        return List.copyOf(ids);
    }

    /** The chosen tab's id. */
    public String chosen() {
        return ids.get(getSelectedIndex());
    }

    /** Chooses a tab by id, as a reader's press or arrow key would. */
    public void choose(String id) {
        int at = ids.indexOf(id);
        if (at < 0) {
            throw new IllegalArgumentException("no such tab: " + id);
        }
        setSelectedIndex(at);
    }

    /** What a tab holds, as the host handed it. */
    public JComponent content(String id) {
        int at = ids.indexOf(id);
        if (at < 0) {
            throw new IllegalArgumentException("no such tab: " + id);
        }
        return contents.get(at);
    }

    /**
     * The narrowest the strip may be drawn without drawing any page's
     * content narrower than it asks: the widest content, its scroll
     * bar, and whatever the look and feel puts around a page - read
     * from the strip itself as the difference between what it asks
     * and what its widest page asks, not written down.
     */
    int contentWidth() {
        int content = 0;
        int page = 0;
        for (int i = 0; i < getTabCount(); i++) {
            content = Math.max(content, contents.get(i).getPreferredSize().width);
            page = Math.max(page, getComponentAt(i).getPreferredSize().width);
        }
        int around = Math.max(0, getPreferredSize().width - page);
        return content + chartScroll.getVerticalScrollBar().getPreferredSize().width
                + around;
    }

    /**
     * The size the tabs ask for: the widest content with its scroll
     * bar, and the tallest page under the strip.
     */
    Dimension wanted() {
        return new Dimension(contentWidth(), getPreferredSize().height);
    }

    private static JScrollPane scrolling(JComponent content) {
        JScrollPane scroll = new JScrollPane(new WidthTracking(content),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }
}
