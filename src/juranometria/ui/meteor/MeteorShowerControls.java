package juranometria.ui.meteor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import juranometria.meteor.MeteorShower;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;

/**
 * The meteor-shower panel (#716, section 4 of
 * {@code docs/decisions/meteor-showers.md}): a selector in the order of
 * the year, the facts for the selected shower at the chart's place and
 * time, and the two actions - set the chart time to the next nominal
 * maximum, and centre on the radiant.
 *
 * <p>One class for both hosts, the dialog and the Controller's group,
 * over one session: selecting in one shows in the other. It holds no
 * state of its own beyond what it displays.
 */
public final class MeteorShowerControls extends JPanel {

    private static final long serialVersionUID = 1L;

    public final JComboBox<String> selector = new JComboBox<>();
    public final JTextArea facts = new JTextArea();
    public final JButton setMaximum;
    public final JButton centre;

    private final transient MeteorShowerSession session;
    private final transient InterfaceText said;
    private final transient MeteorShowerText text;
    private final transient List<String> codes = new ArrayList<>();
    private transient Runnable unfollow = () -> { };
    private boolean refreshing;
    private boolean awake;

    public MeteorShowerControls(MeteorShowerSession session, InterfaceText said) {
        if (session == null || said == null) {
            throw new IllegalArgumentException("a session, in a language");
        }
        this.session = session;
        this.said = said;
        this.text = new MeteorShowerText(said);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel label = new JLabel(said.say("meteor.selector.label"));
        label.setLabelFor(selector);
        selector.setName("meteorShowerSelector");
        selector.getAccessibleContext().setAccessibleName(said.say("meteor.selector.a11y"));
        Explain.control(selector, said.say("meteor.selector.hover"),
                said.say("meteor.selector.explain"));
        selector.addActionListener(event -> {
            int index = selector.getSelectedIndex();
            if (!refreshing && index >= 0) {
                session.select(codes.get(index));
            }
        });
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(label, BorderLayout.WEST);
        top.add(selector, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        facts.setName("meteorShowerFacts");
        facts.setEditable(false);
        facts.setLineWrap(true);
        facts.setWrapStyleWord(true);
        facts.setOpaque(false);
        facts.setFont(label.getFont());
        facts.setColumns(44);
        facts.getAccessibleContext().setAccessibleName(said.say("meteor.facts.a11y"));
        add(facts, BorderLayout.CENTER);

        setMaximum = new JButton(said.say("meteor.setMaximum.label"));
        setMaximum.setName("meteorShowerSetMaximum");
        setMaximum.getAccessibleContext().setAccessibleName(said.say("meteor.setMaximum.a11y"));
        Explain.control(setMaximum, said.say("meteor.setMaximum.hover"),
                said.say("meteor.setMaximum.explain"));
        setMaximum.addActionListener(event -> session.setChartTimeToMaximum());
        centre = new JButton(said.say("meteor.centre.label"));
        centre.setName("meteorShowerCentre");
        centre.getAccessibleContext().setAccessibleName(said.say("meteor.centre.a11y"));
        Explain.control(centre, said.say("meteor.centre.hover"),
                said.say("meteor.centre.explain"));
        centre.addActionListener(event -> session.centreOnRadiant());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        actions.setOpaque(false);
        actions.add(setMaximum);
        actions.add(centre);
        add(actions, BorderLayout.SOUTH);

        // Nothing is read or computed until a reader first sees the panel
        // (the Controller builds its groups collapsed, and building one
        // must load no pack); from then on it follows the session.
        setMaximum.setEnabled(false);
        centre.setEnabled(false);
        unfollow = session.onChange(() -> {
            if (awake) {
                refresh();
            }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing() && !awake) {
                refresh();
            }
        });
    }

    /** Whether the panel has been shown or read yet; until then it has computed nothing. */
    public boolean awake() {
        return awake;
    }

    /** Reads the session again: the selector's marks, the facts, the actions. */
    public void refresh() {
        awake = true;
        refreshing = true;
        try {
            Set<String> active = session.activeNow().stream().map(MeteorShower::code)
                    .collect(Collectors.toSet());
            codes.clear();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            codes.add(null);
            model.addElement(said.say("meteor.selector.none"));
            for (MeteorShower shower : session.showers().showers()) {
                codes.add(shower.code());
                model.addElement(active.contains(shower.code())
                        ? said.say("meteor.selector.active", text.name(shower))
                        : text.name(shower));
            }
            selector.setModel(model);
            selector.setSelectedIndex(Math.max(0, codes.indexOf(session.selected())));
        } finally {
            refreshing = false;
        }
        Optional<MeteorShowerSession.Facts> now = session.facts();
        List<String> lines = session.selected() == null ? text.unselected(session)
                : now.map(f -> text.lines(f, session.showers().pack()))
                        .orElseGet(() -> text.outside(session));
        facts.setText(String.join("\n", lines));
        facts.setCaretPosition(0);
        setMaximum.setEnabled(session.canSetChartTime()
                && now.flatMap(MeteorShowerSession.Facts::maximum).isPresent());
        centre.setEnabled(session.canCentre());
    }

    /** Lets go of the session. */
    public void release() {
        unfollow.run();
        unfollow = () -> { };
    }
}
