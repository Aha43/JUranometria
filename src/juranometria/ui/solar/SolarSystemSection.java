package juranometria.ui.solar;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import juranometria.ui.companion.CompanionSection;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.language.InterfaceText;

/**
 * The Solar System section of the JUranometria Controller (Sprint 42,
 * issue #458, ruled on #457): the Sun and the Moon as groups that each
 * remember their own collapse - Sun introduced open, Moon collapsed -
 * each holding one {@link SolarTableControls} in the Controller's
 * arrangement over its body's shared session and chart switch. The
 * section itself is introduced collapsed; the host adds it with that
 * default. Since #474 a third group, Jupiter, follows the Moon,
 * introduced collapsed, holding one {@link JovianTableControls} over
 * Jupiter's shared session - and no Show on chart box, because nothing
 * Jovian is drawn.
 *
 * <p>Building the section computes nothing: each set of controls
 * reads the observer for its note and seeds, and shows whatever its
 * session already holds. Collapse is presentation, kept in the
 * companion's store under {@code companion.collapsed.solarsystem.sun}
 * and {@code ….moon}; it tells no session anything.
 */
public final class SolarSystemSection {

    /** The section's id in the companion. */
    public static final String ID = "solarsystem";

    private final SolarTableControls sun;
    private final SolarTableControls moon;
    /** Jupiter's controls, or null for a host built without Jupiter. */
    private final JovianTableControls jupiter;

    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              InterfaceText said) {
        this(sunSession, moonSession, sunOnChart, moonOnChart, null, said);
    }

    /** With Jupiter's group after the Moon's (#474); a null session leaves it out. */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, InterfaceText said) {
        if (sunSession.table().body() != juranometria.solar.SolarSystemService.Body.SUN
                || moonSession.table().body()
                        != juranometria.solar.SolarSystemService.Body.MOON) {
            throw new IllegalArgumentException("the Sun's session, then the Moon's");
        }
        sun = new SolarTableControls(sunSession, said, sunOnChart, false);
        moon = new SolarTableControls(moonSession, said, moonOnChart, false);
        jupiter = jupiterSession == null ? null
                : new JovianTableControls(jupiterSession, said, false);
    }

    /** Jupiter's controls, or null for a host built without Jupiter. */
    public JovianTableControls jupiter() {
        return jupiter;
    }

    /** The Sun's controls, for a host that checks what it shares. */
    public SolarTableControls sun() {
        return sun;
    }

    /** The Moon's controls. */
    public SolarTableControls moon() {
        return moon;
    }

    /** The two groups, remembered apart, as the section's content. */
    public JComponent inController(CompanionStore store, InterfaceText said) {
        JPanel groups = new JPanel();
        groups.setLayout(new BoxLayout(groups, BoxLayout.Y_AXIS));
        groups.add(CompanionSection.remembered(ID + ".sun",
                said.say("solarsystem.sun"), sun.inController(), said, store, false));
        groups.add(CompanionSection.remembered(ID + ".moon",
                said.say("solarsystem.moon"), moon.inController(), said, store, true));
        if (jupiter != null) {
            groups.add(CompanionSection.remembered(ID + ".jupiter",
                    said.say("solarsystem.jupiter"), jupiter.inController(), said, store,
                    true));
        }
        return groups;
    }

    /** Lets both sets of controls go of their sessions. */
    public void release() {
        sun.release();
        moon.release();
        if (jupiter != null) {
            jupiter.release();
        }
    }
}
