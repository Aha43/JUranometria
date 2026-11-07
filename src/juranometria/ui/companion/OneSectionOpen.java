package juranometria.ui.companion;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import juranometria.ui.language.InterfaceText;

/**
 * The Controller's one global choice to keep one section open (#699,
 * road #698): while it is on, opening a section collapses its siblings in
 * the group that governs it - the Chart tab's sections, the Solar System
 * tab's body groups - each group on its own, so each tab may keep its one
 * open section. Chart Options' nested groups are not governed.
 *
 * <p>A collapse it makes is an ordinary stored collapse; turning the
 * choice off reopens nothing. The choice itself is remembered in the
 * Controller's store, off until a reader turns it on.
 */
public final class OneSectionOpen {

    /** The choice's component name. */
    public static final String BOX = "oneSectionOpen";

    private final CompanionStore store;
    private boolean on;
    private final List<Group> groups = new ArrayList<>();

    /** One governed set of siblings, and the one last opened among them. */
    private static final class Group {
        final List<CompanionSection> members;
        CompanionSection lastOpened;

        Group(List<CompanionSection> members) {
            this.members = List.copyOf(members);
        }
    }

    public OneSectionOpen(CompanionStore store) {
        if (store == null) {
            throw new IllegalArgumentException("the choice is remembered somewhere");
        }
        this.store = store;
        this.on = store.oneOpen();
    }

    /** Whether one section is kept open. */
    public boolean on() {
        return on;
    }

    /**
     * Governs a group of sibling sections, in visual order. While the
     * choice is on, a group found with several open is brought to one at
     * once, by the rule {@link #choose} states.
     */
    public void govern(List<CompanionSection> members) {
        Group group = new Group(members);
        groups.add(group);
        for (CompanionSection section : group.members) {
            section.onOpened(opened -> {
                group.lastOpened = opened;
                if (on) {
                    collapseAllBut(group, opened);
                }
            });
        }
        if (on) {
            enforce(group);
        }
    }

    /**
     * Turns the choice on or off and remembers it. Turning it on brings
     * each group to one open section; turning it off reopens nothing.
     */
    public void setOn(boolean turnedOn) {
        if (turnedOn == on) {
            return;
        }
        on = turnedOn;
        store.saveOneOpen(turnedOn);
        if (turnedOn) {
            for (Group group : groups) {
                enforce(group);
            }
        }
    }

    /** The choice as the Controller's slim row offers it, following the state. */
    public JCheckBox checkBox(InterfaceText said) {
        JCheckBox box = new JCheckBox(said.say("companion.oneOpen.label"), on);
        box.setName(BOX);
        box.getAccessibleContext().setAccessibleName(said.say("companion.oneOpen.a11y"));
        juranometria.ui.Explain.control(box, said.say("companion.oneOpen.hover"),
                said.say("companion.oneOpen.explain"));
        box.addActionListener(event -> setOn(box.isSelected()));
        return box;
    }

    private void enforce(Group group) {
        List<CompanionSection> open = new ArrayList<>();
        for (CompanionSection section : group.members) {
            if (section.expanded()) {
                open.add(section);
            }
        }
        if (open.size() > 1) {
            collapseAllBut(group, choose(group, open));
        }
    }

    /**
     * The one kept when several are open (ruled on #699): the section
     * holding the keyboard focus; otherwise the one most recently opened;
     * otherwise the first in visual order.
     */
    private static CompanionSection choose(Group group, List<CompanionSection> open) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getFocusOwner();
        for (CompanionSection section : open) {
            if (focus != null && SwingUtilities.isDescendingFrom(focus, section)) {
                return section;
            }
        }
        if (group.lastOpened != null && open.contains(group.lastOpened)) {
            return group.lastOpened;
        }
        return open.get(0);
    }

    private static void collapseAllBut(Group group, CompanionSection kept) {
        for (CompanionSection section : group.members) {
            if (section != kept) {
                section.setExpanded(false);
            }
        }
    }
}
