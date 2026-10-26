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
 * section itself was introduced collapsed; since #507 it is the
 * Controller's Solar System tab, the groups standing directly on it
 * with no outer group of their own. Since #474 a third group, Jupiter, follows the Moon,
 * introduced collapsed, holding one {@link JovianTableControls} over
 * Jupiter's shared session - and no Show on chart box, because nothing
 * Jovian is drawn. Since #502 a Venus group stands between the Sun and
 * the Moon - the ruled order outward from the Sun, showing only the
 * bodies the atlas has - introduced collapsed, holding one
 * {@link SolarTableControls} over Venus's shared session - since #514
 * with Show on chart and Centre on chart, as the Sun's and the Moon's.
 * Since #530 a Mercury group stands between the Sun and Venus - the
 * same order, outward from the Sun - introduced collapsed, holding one
 * {@link SolarTableControls} over Mercury's shared session, with no
 * chart switch and no Centre on chart: no Mercury is drawn. Since #576 a
 * Saturn group follows Jupiter's - the same order - introduced
 * collapsed, holding one {@link SaturnTableControls} over Saturn's
 * shared session, with no chart switch and no Centre on chart: no Saturn
 * is drawn yet. Since #603 a Mars group stands between the Moon's and
 * Jupiter's - the same order, outward from the Sun - introduced collapsed,
 * holding one {@link MarsTableControls} over Mars's shared session, with
 * no chart switch and no Centre on chart: no Mars is drawn yet.
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
    /** Mercury's controls, or null for a host built without Mercury. */
    private final SolarTableControls mercury;
    private final SolarTableControls moon;
    /** Venus's controls, or null for a host built without Venus. */
    private final SolarTableControls venus;
    /** Jupiter's controls, or null for a host built without Jupiter. */
    private final JovianTableControls jupiter;
    /** Saturn's controls, or null until a host gives the section Saturn's session. */
    private SaturnTableControls saturn;
    /** Mars's controls, or null until a host gives the section Mars's session. */
    private MarsTableControls mars;

    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              InterfaceText said) {
        this(sunSession, moonSession, sunOnChart, moonOnChart, null, said);
    }

    /** With Jupiter's group after the Moon's (#474); a null session leaves it out. */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, InterfaceText said) {
        this(sunSession, moonSession, sunOnChart, moonOnChart, jupiterSession, null, said);
    }

    /**
     * With the application's Centre on chart (#483) on the Sun's and the
     * Moon's groups, each turning on its own Show on chart switch; a
     * null action leaves the buttons out. Jupiter has no switch here.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, CentreOnChart centre,
                              InterfaceText said) {
        this(sunSession, moonSession, sunOnChart, moonOnChart, jupiterSession, null, centre,
                said);
    }

    /**
     * With Jupiter drawn (#484): Jupiter's group carries the Jovian
     * module's Show on chart box where {@code jupiterOnChart} is given,
     * and Centre on chart, turning that switch on, where {@code centre}
     * is given too.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, BodyOnChart jupiterOnChart,
                              CentreOnChart centre, InterfaceText said) {
        this(sunSession, null, moonSession, sunOnChart, moonOnChart, jupiterSession,
                jupiterOnChart, centre, said);
    }

    /**
     * With Venus (#502): its group between the Sun's and the Moon's,
     * over Venus's shared session, with no chart switch and no Centre on
     * chart - the form before #514; a null session leaves it out.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession venusSession,
                              SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, BodyOnChart jupiterOnChart,
                              CentreOnChart centre, InterfaceText said) {
        this(sunSession, venusSession, moonSession, sunOnChart, null, moonOnChart,
                jupiterSession, jupiterOnChart, centre, said);
    }

    /**
     * With Venus drawn (#514, the rulings on #512): Venus's group carries
     * its own Show on chart and, with the shared action, Centre on chart,
     * as the Sun's and the Moon's do; a null switch leaves both out.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession venusSession,
                              SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart venusOnChart,
                              BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, BodyOnChart jupiterOnChart,
                              CentreOnChart centre, InterfaceText said) {
        this(sunSession, null, venusSession, moonSession, sunOnChart, null, venusOnChart,
                moonOnChart, jupiterSession, jupiterOnChart, centre, said);
    }

    /**
     * With Mercury (#530): its group between the Sun's and Venus's, with no
     * chart switch and no Centre on chart - the form before #542.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession mercurySession,
                              SolarTableSession venusSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart venusOnChart,
                              BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, BodyOnChart jupiterOnChart,
                              CentreOnChart centre, InterfaceText said) {
        this(sunSession, mercurySession, venusSession, moonSession, sunOnChart, null,
                venusOnChart, moonOnChart, jupiterSession, jupiterOnChart, centre, said);
    }

    /**
     * With Mercury drawn (#542, the rulings on #539): its group between the
     * Sun's and Venus's, carrying its own Show on chart and, with the
     * shared action, Centre on chart, as Venus's does; a null switch leaves
     * both out; a null session leaves the group out.
     */
    public SolarSystemSection(SolarTableSession sunSession, SolarTableSession mercurySession,
                              SolarTableSession venusSession, SolarTableSession moonSession,
                              BodyOnChart sunOnChart, BodyOnChart mercuryOnChart,
                              BodyOnChart venusOnChart, BodyOnChart moonOnChart,
                              JovianTableSession jupiterSession, BodyOnChart jupiterOnChart,
                              CentreOnChart centre, InterfaceText said) {
        if (mercurySession != null && mercurySession.table().body()
                != juranometria.solar.SolarSystemService.Body.MERCURY) {
            throw new IllegalArgumentException("Mercury's session, between the Sun's and Venus's");
        }
        if (sunSession.table().body() != juranometria.solar.SolarSystemService.Body.SUN
                || moonSession.table().body()
                        != juranometria.solar.SolarSystemService.Body.MOON
                || (venusSession != null && venusSession.table().body()
                        != juranometria.solar.SolarSystemService.Body.VENUS)) {
            throw new IllegalArgumentException("the Sun's session, Venus's, then the Moon's");
        }
        sun = new SolarTableControls(sunSession, said, sunOnChart, false, centre,
                centre == null ? null : sunOnChart);
        mercury = mercurySession == null ? null
                : new SolarTableControls(mercurySession, said, mercuryOnChart, false,
                        mercuryOnChart == null ? null : centre,
                        mercuryOnChart == null || centre == null ? null : mercuryOnChart);
        venus = venusSession == null ? null
                : new SolarTableControls(venusSession, said, venusOnChart, false,
                        venusOnChart == null ? null : centre,
                        venusOnChart == null || centre == null ? null : venusOnChart);
        moon = new SolarTableControls(moonSession, said, moonOnChart, false, centre,
                centre == null ? null : moonOnChart);
        jupiter = jupiterSession == null ? null
                : new JovianTableControls(jupiterSession, said, false, jupiterOnChart,
                        jupiterOnChart == null ? null : centre, jupiterOnChart);
    }

    /** Jupiter's controls, or null for a host built without Jupiter. */
    public JovianTableControls jupiter() {
        return jupiter;
    }

    /**
     * Gives the section Saturn's group after Jupiter's (#576): its
     * controls over Saturn's shared session, with no chart switch and no
     * Centre on chart (a host built without the module). Stated before
     * the tab is built; building them computes nothing and reads no pack.
     */
    public void withSaturn(SaturnTableSession session, InterfaceText said) {
        withSaturn(session, said, null, null);
    }

    /**
     * The same, with the Saturn module's switch (#579, ruling 5 on #578):
     * Show on chart over {@code onChart}, and Centre on chart through
     * {@code centre} where both are given, as Jupiter's group has them.
     */
    public void withSaturn(SaturnTableSession session, InterfaceText said,
                           BodyOnChart onChart, CentreOnChart centre) {
        if (session == null || said == null) {
            throw new IllegalArgumentException("Saturn's session, in a language");
        }
        if (saturn != null) {
            saturn.release();
        }
        saturn = new SaturnTableControls(session, said, false, onChart,
                onChart == null ? null : centre, onChart);
    }

    /** Saturn's controls, or null for a host built without Saturn. */
    public SaturnTableControls saturn() {
        return saturn;
    }

    /**
     * Gives the section Mars's group between the Moon's and Jupiter's
     * (#603): its controls over Mars's shared session, with no chart switch
     * and no Centre on chart - no Mars is drawn yet. Stated before the tab
     * is built; building them computes nothing and reads no pack.
     */
    public void withMars(MarsTableSession session, InterfaceText said) {
        withMars(session, said, null, null);
    }

    /**
     * The same, with the Solar System module's Mars switch (#606, R-5 on
     * #605): Show on chart over {@code onChart}, and Centre on chart through
     * {@code centre} where both are given.
     */
    public void withMars(MarsTableSession session, InterfaceText said,
                         BodyOnChart onChart, CentreOnChart centre) {
        if (session == null || said == null) {
            throw new IllegalArgumentException("Mars's session, in a language");
        }
        if (mars != null) {
            mars.release();
        }
        mars = new MarsTableControls(session, said, false, onChart,
                onChart == null ? null : centre, onChart);
    }

    /** Mars's controls, or null for a host built without Mars. */
    public MarsTableControls mars() {
        return mars;
    }

    /** Where the chart's instant is read, and how the tab follows it; null for none. */
    private java.util.function.Supplier<juranometria.sky.Observer> chartObserver;
    private java.util.function.Function<Runnable, Runnable> chartChanges;
    private Runnable updateToNow;
    private Runnable followingChart = () -> { };
    /** The tab's chart-instant line, once the tab is built with one. */
    private javax.swing.JLabel chartInstant;
    private javax.swing.JButton chartUpdate;

    /**
     * Opens the tab with the instant the chart is frozen at and Update to
     * now (#259, ruling 9 on #556): a reader at the laptop with the chart
     * on another screen knows what the chart shows without visiting the
     * Chart tab. Stated before the tab is built.
     *
     * @param chart Place and Time's observer now, or null while it holds none
     * @param follow the subscription that tells the line Place and Time changed
     * @param update the one Update to now the atlas runs
     */
    public void showChartInstant(java.util.function.Supplier<juranometria.sky.Observer> chart,
                                 java.util.function.Function<Runnable, Runnable> follow,
                                 Runnable update) {
        if (chart == null || follow == null || update == null) {
            throw new IllegalArgumentException(
                    "the chart's instant is read, followed and updated");
        }
        this.chartObserver = chart;
        this.chartChanges = follow;
        this.updateToNow = update;
    }

    /** What the tab's chart-instant line says now; empty without one. */
    public String chartInstantText() {
        return chartInstant == null || !chartInstant.isVisible() ? ""
                : chartInstant.getText();
    }

    /** The tab's Update to now, or null without a chart-instant line. */
    public javax.swing.JButton chartUpdateButton() {
        return chartUpdate;
    }

    private JComponent chartInstantLine(InterfaceText said) {
        chartInstant = new javax.swing.JLabel();
        chartInstant.setName("solarSystemChartInstant");
        chartUpdate = new javax.swing.JButton(said.say("placeandtime.now.label"));
        chartUpdate.setName("solarSystemUpdateToNow");
        chartUpdate.getAccessibleContext().setAccessibleName(
                said.say("placeandtime.now.a11y"));
        juranometria.ui.Explain.control(chartUpdate, said.say("placeandtime.now.hover"),
                said.say("placeandtime.now.explain"));
        chartUpdate.addActionListener(event -> updateToNow.run());
        JPanel line = new JPanel(new java.awt.BorderLayout(0, 4));
        line.setName("solarSystemChartInstantLine");
        // The groups' own alignment, so the line spans the tab as they do.
        line.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        line.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createEmptyBorder(6, 10, 4, 10),
                javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(
                                javax.swing.UIManager.getColor("Separator.foreground")),
                        javax.swing.BorderFactory.createEmptyBorder(5, 7, 5, 7))));
        line.add(chartInstant, java.awt.BorderLayout.CENTER);
        JPanel button = new JPanel(new java.awt.BorderLayout());
        button.setOpaque(false);
        button.add(chartUpdate, java.awt.BorderLayout.WEST);
        line.add(button, java.awt.BorderLayout.SOUTH);
        line.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                line.getPreferredSize().height + 40));
        Runnable say = () -> {
            juranometria.sky.Observer now = chartObserver.get();
            line.setVisible(now != null);
            if (now != null) {
                chartInstant.setText(said.say("solarsystem.chartinstant",
                        SolarTableSession.shown(now.instant())));
                chartInstant.getAccessibleContext().setAccessibleName(
                        chartInstant.getText());
            }
        };
        say.run();
        followingChart.run();
        followingChart = chartChanges.apply(say);
        return line;
    }

    /** The Sun's controls, for a host that checks what it shares. */
    public SolarTableControls sun() {
        return sun;
    }

    /** Mercury's controls, or null for a host built without Mercury. */
    public SolarTableControls mercury() {
        return mercury;
    }

    /** The Moon's controls. */
    public SolarTableControls moon() {
        return moon;
    }

    /** Venus's controls, or null for a host built without Venus. */
    public SolarTableControls venus() {
        return venus;
    }

    /** The groups, remembered apart, as the tab's content. */
    public JComponent inController(CompanionStore store, InterfaceText said) {
        JPanel groups = new JPanel();
        groups.setLayout(new BoxLayout(groups, BoxLayout.Y_AXIS));
        if (chartObserver != null) {
            groups.add(chartInstantLine(said));
        }
        groups.add(CompanionSection.remembered(ID + ".sun",
                said.say("solarsystem.sun"), sun.inController(), said, store, false));
        if (mercury != null) {
            groups.add(CompanionSection.remembered(ID + ".mercury",
                    said.say("solarsystem.mercury"), mercury.inController(), said, store, true));
        }
        if (venus != null) {
            groups.add(CompanionSection.remembered(ID + ".venus",
                    said.say("solarsystem.venus"), venus.inController(), said, store, true));
        }
        groups.add(CompanionSection.remembered(ID + ".moon",
                said.say("solarsystem.moon"), moon.inController(), said, store, true));
        if (mars != null) {
            groups.add(CompanionSection.remembered(ID + ".mars",
                    said.say("solarsystem.mars"), mars.inController(), said, store, true));
        }
        if (jupiter != null) {
            groups.add(CompanionSection.remembered(ID + ".jupiter",
                    said.say("solarsystem.jupiter"), jupiter.inController(), said, store,
                    true));
        }
        if (saturn != null) {
            groups.add(CompanionSection.remembered(ID + ".saturn",
                    said.say("solarsystem.saturn"), saturn.inController(), said, store,
                    true));
        }
        return groups;
    }

    /** Lets both sets of controls go of their sessions. */
    public void release() {
        followingChart.run();
        followingChart = () -> { };
        sun.release();
        if (mercury != null) {
            mercury.release();
        }
        if (venus != null) {
            venus.release();
        }
        moon.release();
        if (mars != null) {
            mars.release();
        }
        if (jupiter != null) {
            jupiter.release();
        }
        if (saturn != null) {
            saturn.release();
        }
    }
}
