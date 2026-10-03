package juranometria.app;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;

/** Application entry point. */
public final class JUranometriaMain {

    private JUranometriaMain() {
    }

    public static void main(String[] args) {
        // macOS integration properties must be set before any AWT class loads.
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", AppInfo.NAME);
        boolean darkOverride = java.util.Arrays.asList(args).contains("--dark");
        SwingUtilities.invokeLater(() -> {
            // Launch is the one place a failure has no reader-visible
            // consequence of its own: the packaged application has no
            // console, and an exception here would otherwise leave a
            // live process with no window (issue #145).
            try {
                start(darkOverride, StartupStores.user());
            } catch (Throwable failure) {
                StartupFailure.reportAndExit(failure);
            }
        });
    }

    /**
     * The inspector's one navigating action (issue #170): explicit,
     * pressed by the reader, and using the same recentre path search
     * uses - so coverage and titling behave exactly as they always
     * have. Selecting alone never does this.
     */
    private static void centreOn(ChartViewController navigation,
                                 juranometria.chart.Selection chosen) {
        if (chosen == null || chosen.position() == null) {
            return;
        }
        navigation.recenter(chosen.position());
    }

    /**
     * The application, composed.
     *
     * <p>Package-private and handed its five preference stores rather
     * than opening them, so a journey can run <em>these lines</em>
     * instead of a reconstruction of them (#350). A replica that
     * rebuilds the wiring in a fixture stays green when a call site
     * below is broken, which is how a toolbar shipped in English
     * behind eight passing contracts.
     *
     * <p>Returns the frame it built. {@code main} ignores it - the
     * reader closes their own window - and a test owns disposing of
     * the one it asked for.
     */
    static JFrame start(boolean darkOverride, StartupStores stores) {
        // The appearance session policy: the saved preference decides an
        // ordinary launch; an active --dark override keeps this whole
        // session dark and can never be converted into a stored choice
        // by merely confirming Settings (contract on AppearanceSession).
        AppearanceSession appearance = new AppearanceSession(
                stores.appearance(), darkOverride);
        UiTheme.apply(appearance.startupDark());
        ChartOptionsController chartOptions =
                new ChartOptionsController(stores.chartOptions());
        // The reader's language, read from the store exactly once and
        // owned by this session from here on (#348). The store says
        // what the NEXT session starts with; the session says what
        // THIS one is showing. Nothing below reads the preference
        // node again - a page that could change because something
        // wrote to preferences, with no application state having
        // passed through the call, is a page nothing can account for.
        juranometria.ui.language.InterfaceLanguages interfaces =
                juranometria.ui.language.InterfaceLanguages.discover();
        juranometria.ui.language.SkyLanguageSession language =
                juranometria.ui.language.SkyLanguageSession.begin(
                        stores.language(), Atlas.languages());
        // The toolkit's own words, before any component exists.
        //
        // Swing resolves its buttons, its chooser and every hover and
        // spoken name in them against Locale.getDefault() - the
        // operating system - so a reader was asked a Norwegian
        // question and answered it with "Yes". These are installed
        // over the toolkit's, in whichever language the reader chose,
        // and the language cannot move beneath them: it is read once
        // above and never consulted again.
        //
        // UiTheme knows nothing about language and must not: a look
        // and feel is a look and feel. The two are PAIRED here, and
        // everywhere else `apply` is called, because installing a
        // look and feel replaces the defaults table and takes these
        // with it (#350).
        juranometria.ui.language.SwingText toolkitWords =
                juranometria.ui.language.SwingText.in(
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()));
        toolkitWords.installInto(javax.swing.UIManager.getDefaults());

        // The catalogues verify themselves as they load, so they are
        // loaded before any window exists: a damaged download should
        // be explained, not half-drawn behind a frame that will never
        // be usable.
        //
        // One assembler for the running application, told the
        // language once. Two would not differ today - navigation and
        // search ask it geometric questions that no language changes
        // - but a second one built without the language is a trap
        // laid for whoever later adds something to those paths that
        // does need a name.
        juranometria.ui.SceneAssembler assembler =
                Atlas.assemblerNamedIn(language.namesOnTheChart());
        ChartViewController controller =
                new ChartViewController(assembler::fits);
        JFrame frame = new JFrame(AppInfo.NAME + " " + AppInfo.version());
        // The mark the gate chose, drawn at every size a window
        // manager might want (issue #202). Without this the title
        // bar, the task switcher and a portable launch all fall back
        // to Java's default cup.
        frame.setIconImages(ApplicationIcon.windowIcons());
        // The page's own language, resolved ONCE from the session
        // and handed to the chart. The export path resolves it again
        // from the same session state in ExportSheet.write; nothing
        // downstream of either remembers a language, so the screen
        // and the file cannot disagree (#349, #350).
        juranometria.project.PageWords pageWords =
                juranometria.ui.language.PageText.in(
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()));
        ChartComponent chart = new ChartComponent(assembler, pageWords);
        controller.onChange(chart::setViewState);
        // Choosing a language rebuilds the page in it. Reconstruction
        // rather than repaint: names are resolved into a scene when
        // it is assembled, so a page already drawn cannot be made to
        // show a language it was not built in. The view state is not
        // touched - choosing a language is not navigation, and the
        // reader stays where they were while the sky is relabelled
        // around them (#348).
        language.onChange(chosen -> chart.setAssembler(
                Atlas.assemblerNamedIn(chosen.namesOnTheChart())));
        // Hiding the family a searched target belongs to retires the
        // target (issue #196): the explicit hide is the later and
        // equally explicit request, so it wins. Ordinary family
        // hiding stays exactly what it was - a repaint - and only
        // this conflict becomes a navigation transition, which is
        // why the decision is asked here rather than folded into
        // options state that owns no navigation.
        TargetRetirement.connect(chartOptions, chart, controller);
        juranometria.ui.PanInteraction.install(chart, controller);
        // The zoom lock (Sprint 38, issue #428): one state for the
        // wheel, the toolbar's toggle and the remembered choice,
        // restored before the reader can turn a wheel, and saved on
        // every change; the clean shutdown flushes it with the rest.
        juranometria.ui.ZoomLockStore zoomLockStore = stores.zoomLock();
        juranometria.ui.ZoomLock zoomLock = new juranometria.ui.ZoomLock();
        zoomLock.lock(zoomLockStore.lockedOrDefault());
        zoomLock.onChange(zoomLockStore::save);
        juranometria.ui.ZoomInteraction.install(chart, controller, zoomLock);

        // Point and identify (issue #170). The selection is shared
        // state; the chart produces it, the inspector consumes it,
        // and neither knows about the other.
        juranometria.chart.SelectionModel selection =
                new juranometria.chart.SelectionModel();
        // The host holds the chart-level selection services - the
        // working selection and its accumulate mode live here, in
        // the core's own seam, whether or not any module attaches -
        // and wires the membership to the chart's ink: one ring per
        // selected drawn member, the crosses' lead treatment, and
        // the lead fed to the answering model (issue #261).
        juranometria.ui.ChartModuleHost modules =
                new juranometria.ui.ChartModuleHost(chart, selection,
                        request -> controller.recenter(request.centre()));
        juranometria.ui.SelectInteraction.install(chart, selection,
                modules.workingSelection(), modules.selectionMode());
        InspectorPanel inspector = new InspectorPanel(selection,
                chart::currentScene, chartOptions::options,
                chosen -> centreOn(controller, chosen));
        inspector.showWorkingSet(modules.workingSelection(),
                modules::inventory);
        // A page the reader navigated to may no longer draw what they
        // selected; the panel re-reads it rather than going on
        // describing something that has gone.
        chart.onSceneChange(inspector::refresh);

        // The first module (issue #216). The chart offers services;
        // this asks for them and gives back a table and some crosses.
        // The atlas draws its ordinary page, with its selection
        // working, if these lines are deleted.
        juranometria.ui.onthispage.OnThisPageModule onThisPage =
                modules.attach(
                        new juranometria.ui.onthispage.OnThisPageModule(
                                juranometria.ui.language.InterfaceText
                                        .forLanguage(language
                                                .interfaceLanguage())));
        // The second module (issue #228), begun through the one
        // seam that owns the session-start policy - the clock is
        // read exactly once, here, stated rather than hidden in a
        // constructor.
        juranometria.ui.placeandtime.PlaceStore placeStore =
                stores.place();
        juranometria.meridian.MeridianModule meridian =
                juranometria.ui.placeandtime.PlaceAndTimeSession.begin(
                        modules, placeStore, java.time.Instant.now());
        // The third module (issue #274), begun through its own seam:
        // attached, then told what the reader last chose. A reader
        // who never chose gets the released default, which is
        // hidden.
        juranometria.ui.ecliptic.EclipticStore eclipticStore =
                stores.ecliptic();
        juranometria.ecliptic.EclipticModule ecliptic =
                juranometria.ui.ecliptic.EclipticSession.begin(modules);
        inspector.showPageView(onThisPage.panel());
        inspector.onClose(chart::requestFocusInWindow);
        // One switch, three ways to reach it: the toolbar button, the
        // View menu, and the window's own width (issue #180).
        juranometria.ui.InspectorToggle inspectorToggle =
                new juranometria.ui.InspectorToggle();
        inspectorToggle.bind(
                () -> inspector.setRequestedVisible(
                        !inspector.isRequestedVisible()),
                inspector::canShow);
        inspector.onVisibilityChange(inspectorToggle::report);

        // The chart's own keyboard (issue #312): one key opens a
        // palette of every layer with its letter and its state, and
        // every letter reaches the same transition the reader's own
        // control reaches. The switches are handed the controller and
        // the modules' own seams, never a second path to the store.
        ChartKeyboardSession.install(frame.getRootPane(), chartOptions,
                ecliptic,
                juranometria.ui.ecliptic.EclipticSession.toggle(
                        ecliptic, eclipticStore),
                meridian,
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()));
        // The reviewed layout rule: below 640 px of window the
        // inspector yields, and a window that widens again restores
        // what the reader asked for.
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                inspector.setAvailableWidth(frame.getWidth());
                frame.revalidate();
            }
        });
        AppMenuBar.installZoomShortcuts(frame.getRootPane(), controller);
        // One way out, whichever surface asks (issue #198). The
        // toolbar button, the window's close box and the platform's
        // Quit all reach the same path, so leaving means one thing.
        // Named here rather than below because the toolbar is built
        // through the controls seam and needs it.
        AppShutdown shutdown = AppShutdown.real();
        // Both reader-facing controls are built through one seam, so
        // that "does the application hand them the session's
        // language?" is a question with an address (#350).
        AtlasChrome controls = AtlasChrome.of(language, controller,
                Atlas.search(), assembler, inspectorToggle,
                AppInfo.version(), shutdown::request,
                modules.selectionMode(), zoomLock);
        juranometria.ui.SearchField searchField = controls.searchField();
        // Finding an object by name selects it, so a reader with no
        // pointer can reach the inspector at all - and it joins the
        // working selection under the decided search semantics.
        searchField.setSelectionModel(selection);
        searchField.setWorkingSelection(modules.workingSelection(),
                modules.selectionMode());

        shutdown.onShutdown(inspector::dispose);
        // And the modules, on the same path. Disposing the panel a
        // module put its table in is not releasing the module: it
        // would still hold its subscriptions and still be
        // contributing geometry to a chart on its way out (review).
        // Detached newest first, which is what the detach step is
        // for.
        shutdown.onShutdown(modules::detachAll);

        // The companion (Sprint 39, issue #434, ruled on #433):
        // Controls kept beside the chart - one owned window, built
        // once, holding the same Place and Time panel the dialog
        // holds, over the same module and place store. Released on
        // the shutdown path before the modules detach (newest first),
        // so its panel lets go of the module it follows; disposing it
        // does not forget that it was open.
        juranometria.ui.companion.CompanionStore companionStore =
                stores.companion();
        juranometria.ui.language.InterfaceText companionWords =
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage());
        juranometria.ui.companion.CompanionWindow companion =
                new juranometria.ui.companion.CompanionWindow(frame,
                        companionWords, companionStore);
        companion.addSection("placeandtime",
                companionWords.say("placeandtime.title"),
                new juranometria.ui.placeandtime.PlaceAndTimePanel(meridian,
                        placeStore, java.time.Instant::now, companionWords));
        // Chart Options (#443, ruled on #442): the same controls class the
        // dialog holds, over the same controller, as remembered groups;
        // Restore Defaults asks over the companion.
        companion.addSection("chartoptions",
                companionWords.say("chartoptions.title"),
                new ChartOptionsControls(chartOptions,
                        () -> ChartOptionsDialog.restoreConfirmed(companion,
                                companionWords),
                        companionWords).inCompanion(companionStore,
                        companionWords));
        shutdown.onShutdown(companion::dispose);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                shutdown.request();
            }
        });
        // The application menu's Quit on macOS, and the equivalent
        // where a desktop provides one: without this it would exit by
        // its own route and skip the flush the other surfaces make.
        // A desktop that will not take a handler keeps its own Quit.
        shutdown.installQuitHandler();

        // The same AppInfo.version() About prints, handed over
        // rather than looked up twice.
        AtlasToolbar toolbar = controls.toolbar();
        // The reader's transient emphasis control (#361): the chart
        // is the truth it reads, and nothing about it is persisted.
        toolbar.attachEmphasis(chart);

        // The Solar System service reads an 8.9 MB pack; once, and
        // only when a reader first opens the Sun table (#400).
        java.util.function.Supplier<juranometria.solar.SolarSystemService>
                solarSystem = new java.util.function.Supplier<>() {
                    private juranometria.solar.SolarSystemService loaded;

                    @Override
                    public synchronized juranometria.solar.SolarSystemService get() {
                        if (loaded == null) {
                            loaded = juranometria.solar.SolarSystemService.load();
                        }
                        return loaded;
                    }
                };
        // The Sun on the chart (Sprint 37, issue #415): one Solar
        // System module over Place and Time's observer, read through
        // the meridian module when the page paints and never copied;
        // the pack through the same lazy supplier the tables use;
        // dimmed below the horizon only while the meridian module
        // draws it. Hidden until the reader switches it on, and the
        // choice is remembered like the ecliptic's.
        juranometria.ui.solar.SunChartStore sunChartStore = stores.sunChart();
        // The Moon (#416) on the same module, with a switch of its own.
        juranometria.ui.solar.MoonChartStore moonChartStore = stores.moonChart();
        juranometria.solarchart.SolarSystemModule sunOnChart =
                juranometria.ui.solar.SunChartSession.begin(modules,
                        () -> meridian.attached() ? meridian.observer() : null,
                        solarSystem, meridian::horizonShowing);
        // Built after the controls seam, because the menu
        // says its words in the same language the seam
        // derived from the session (#350).
        frame.setJMenuBar(controls.menuBar(controller,
                () -> SettingsDialog.open(frame, appearance,
                        effectiveDark -> {
                            // The ordered pair, again. A new look and
                            // feel replaces the defaults table, so
                            // the toolkit's words have to be put back
                            // before anything is refreshed with them
                            // (#350).
                            UiTheme.apply(effectiveDark);
                            toolkitWords.installInto(
                                    javax.swing.UIManager.getDefaults());
                            com.formdev.flatlaf.FlatLaf.updateUI();
                        }, language, Atlas.names(), interfaces),
                () -> ChartOptionsDialog.open(frame, chartOptions,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage())),
                () -> AboutDialog.open(frame,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage())),
                () -> {
                    inspectorToggle.toggle();
                    frame.revalidate();
                    frame.repaint();
                },
                () -> juranometria.ui.placeandtime.PlaceAndTimeDialog.open(
                        frame, meridian, placeStore,
                        java.time.Instant::now,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage())),
                // One switch, and its whole behaviour lives in the
                // module's own seam so a test can drive exactly what
                // a reader sets off.
                juranometria.ui.ecliptic.EclipticSession.toggle(
                        ecliptic, eclipticStore),
                // File, Export Chart Sheet: the chart the reader is
                // looking at, on paper (Sprint 29, issue #286).
                () -> ExportSheetSession.open(frame, controller, chart,
                        chartOptions, modules.workingSelection(),
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage())),
                // Help, Copy View Report (#372): the view as plain text,
                // read from its live owners when the reader asks, and
                // copied - never sent, saved or remembered. A clipboard
                // that refuses is said, as an export refusal is.
                CopyViewReport.action(
                        () -> ViewReport.snapshot(chart, language, meridian,
                                ecliptic, selection,
                                modules.workingSelection(),
                                System::getProperty),
                        CopyViewReport.SYSTEM,
                        reason -> {
                            juranometria.ui.language.InterfaceText said =
                                    juranometria.ui.language.InterfaceText
                                            .forLanguage(language
                                                    .interfaceLanguage());
                            javax.swing.JOptionPane.showMessageDialog(frame,
                                    said.say("viewReport.refused.message",
                                            reason),
                                    said.say("viewReport.refused.title"),
                                    javax.swing.JOptionPane.WARNING_MESSAGE);
                        }),
                // View, Sun (#400): where the Sun is for the place and
                // instant the meridian module owns, as a table. The
                // observer is read from the module when the table asks,
                // never copied; the pack is read on first opening.
                () -> juranometria.ui.solar.SolarTableDialog.open(frame,
                        () -> meridian.attached() ? meridian.observer() : null,
                        solarSystem.get(),
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()),
                        juranometria.ui.solar.SolarTable.sun()),
                // View, Moon (#408): the same table shell over the same
                // observer and pack, for the Moon's own columns.
                () -> juranometria.ui.solar.SolarTableDialog.open(frame,
                        () -> meridian.attached() ? meridian.observer() : null,
                        solarSystem.get(),
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()),
                        juranometria.ui.solar.SolarTable.moon()),
                // View, Sun on the chart (#415): the switch, remembered.
                juranometria.ui.solar.SunChartSession.toggle(sunOnChart,
                        sunChartStore),
                // View, Moon on the chart (#416): its own switch on the
                // same module, remembered the same way.
                juranometria.ui.solar.MoonChartSession.toggle(sunOnChart,
                        moonChartStore),
                // View, Controls (#434): the companion, shown or hidden.
                // Opened by the reader, it takes the keyboard.
                () -> {
                    if (companion.isVisible()) {
                        companion.hideCompanion();
                    } else {
                        companion.showCompanion(true);
                    }
                }));
        // Both of these read the bar, so both come AFTER it is set.
        // They sat above the menu until the bar moved down to be
        // built in the session's language (#350), and reading a bar
        // that is not on the frame yet is a startup that ends in the
        // failure reporter rather than a window. No test ran the real
        // start, so 1445 of them passed over it.

        // One call, so the chart and the tick cannot disagree about
        // what the reader last chose.
        juranometria.ui.ecliptic.EclipticSession.restore(ecliptic,
                eclipticStore,
                AppMenuBar.eclipticItem(frame.getJMenuBar()));
        juranometria.ui.solar.SunChartSession.restore(sunOnChart,
                sunChartStore, AppMenuBar.sunChartItem(frame.getJMenuBar()));
        juranometria.ui.solar.MoonChartSession.restore(sunOnChart,
                moonChartStore, AppMenuBar.moonChartItem(frame.getJMenuBar()));
        // The tick follows the window, however it was closed.
        javax.swing.JCheckBoxMenuItem companionItem =
                AppMenuBar.companionItem(frame.getJMenuBar());
        companion.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent event) {
                companionItem.setSelected(true);
            }

            @Override
            public void componentHidden(java.awt.event.ComponentEvent event) {
                companionItem.setSelected(false);
            }
        });
        javax.swing.JCheckBoxMenuItem inspectorItem =
                AppMenuBar.inspectorItem(frame.getJMenuBar());
        if (inspectorItem != null) {
            // The item shows what is actually on screen, including
            // when a narrow window has closed the panel for the
            // reader rather than at their asking - the same state the
            // toolbar button shows, from the same switch.
            inspectorToggle.onChange(state -> {
                inspectorItem.setSelected(state.showing());
                inspectorItem.setEnabled(state.available());
            });
        }

        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(chart, BorderLayout.CENTER);
        frame.add(inspector, BorderLayout.EAST);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // Open at the last clean quit, open again (ruled on #433) -
        // after the chart window has its place on a screen, so the
        // companion is placed against real geometry, and without
        // taking the keyboard the chart window is meant to have.
        if (companionStore.visible()) {
            javax.swing.SwingUtilities.invokeLater(
                    () -> companion.showCompanion(false));
        }
        return frame;
    }
}
