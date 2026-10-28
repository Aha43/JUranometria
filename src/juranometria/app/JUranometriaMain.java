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
     * Shows or hides the chart window's toolbar (#450, ruled on #449):
     * layout only. The window keeps its size and the chart gains or
     * gives back the bar's height; nothing the chart draws or
     * remembers is touched. A control on a bar about to hide hands the
     * keyboard to the chart, so focus never goes nowhere.
     */
    static void showToolbar(JFrame frame, AtlasToolbar toolbar,
                            ChartComponent chart, boolean shown) {
        if (toolbar.isVisible() == shown) {
            return;
        }
        java.awt.Component focused = frame.getFocusOwner();
        if (!shown && focused != null
                && SwingUtilities.isDescendingFrom(focused, toolbar)) {
            chart.requestFocusInWindow();
        }
        toolbar.setVisible(shown);
        frame.revalidate();
        frame.repaint();
    }

    /** The window's bounds, if they are ordinary ones worth remembering. */
    private static void rememberOrdinary(JFrame frame,
                                         juranometria.ui.ChartWindowStore store) {
        if (frame.isShowing()
                && juranometria.ui.ChartWindowPlacement.ordinary(frame)) {
            store.saveBounds(frame.getBounds());
        }
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
        return start(darkOverride, stores, java.time.Instant::now);
    }

    /**
     * The same, reading time from a stated clock (#259): the start-up
     * instant and every <em>Update to now</em> read it, once each, and
     * nothing else does - so a journey can count the reads on the
     * application's own routes rather than on a reconstruction of them.
     */
    static JFrame start(boolean darkOverride, StartupStores stores,
                        java.util.function.Supplier<java.time.Instant> clock) {
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
        // The closer fields (#544, ruled on #539, rulings 5 and 6; Jupiter
        // added by the owner at the #544 checkpoint; Saturn by ruling 6 on
        // #578; Mars by R-6 on #605; Uranus by R-7 on #637): offered where
        // Mercury, Venus, Mars, Jupiter, Saturn or Uranus, as their
        // modules draw them, lies on the page the step would display - not
        // the Sun and not the Moon.
        chart.setCloserFieldBodies(java.util.Set.of(
                juranometria.solarchart.SolarSystemModule.MERCURY,
                juranometria.solarchart.SolarSystemModule.VENUS,
                juranometria.solarchart.SolarSystemModule.MARS,
                juranometria.jovianchart.JovianModule.JUPITER,
                juranometria.saturnchart.SaturnModule.SATURN,
                juranometria.solarchart.SolarSystemModule.URANUS));
        controller.offerCloserFields(chart::offersCloserField);
        chart.onPageContentsChange(controller::closerOfferChanged);
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
        // And when a paint draws other bodies, or the same for another
        // moment (#513): what the Inspector says of a selected
        // occurrence - on this page, or for the instant it held - is
        // the page's to decide.
        chart.onBodiesChange(inspector::refresh);

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
                        modules, placeStore, clock.get());
        // The chart's one time and place (#612): the module's holder is
        // the application's; every paint holds it, so one paint reads
        // one snapshot and an installation during a paint is refused.
        chart.setContext(meridian.context());
        // Subdue below horizon (#615): the zenith of the chart's one
        // observer while the switch is on; the bodies above follow the
        // same switch, so a body is subdued once, by the common rule.
        chart.setSubdueBelowHorizon(() -> meridian.attached() && meridian.subduing()
                ? new juranometria.sky.LocalSky(meridian.observer()).zenith() : null);
        meridian.onChange(chart::repaint);
        // Update to now (#259, rulings 4 and 7 on #556): one action, one
        // clock read per activation, and every route below runs this
        // same instance - the chart's Place and Time box, the chart
        // keyboard's W and the Controller's Solar System tab.
        juranometria.ui.placeandtime.UpdateToNow updateToNow =
                new juranometria.ui.placeandtime.UpdateToNow(meridian, clock);
        // How a presentation hears that Place and Time changed: the
        // module's own subscription, cancelled by the handle it returns.
        java.util.function.Function<Runnable, Runnable> placeAndTimeChanges =
                listener -> meridian.onChange(listener)::cancel;
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
                meridian, updateToNow,
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
        // so its panel lets go of the module it follows.
        juranometria.ui.companion.CompanionStore companionStore =
                stores.companion();
        juranometria.ui.language.InterfaceText companionWords =
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage());
        juranometria.ui.companion.CompanionWindow companion =
                new juranometria.ui.companion.CompanionWindow(frame,
                        companionWords, companionStore);
        // Chart controls (Sprint 41, issue #450, ruled on #449): the
        // chart's own controls as a compact section, first, because it
        // is the one a reader reaches for while the chart is a clean
        // sheet on another display. A second set of the same class the
        // bar hosts, over the same authorities and the shared actions;
        // its search field is its own, wired to the same selection
        // services as the bar's, so what it finds joins the same
        // working selection under the same mode.
        juranometria.ui.ChartControls chartControls =
                controls.companionControls(chart);
        chartControls.search().setSelectionModel(selection);
        chartControls.search().setWorkingSelection(modules.workingSelection(),
                modules.selectionMode());
        companion.addSection("chartcontrols",
                companionWords.say("chartcontrols.title"),
                chartControls.inCompanion());
        // Emphasis (#525): one immediate box per choice of the toolbar's
        // Emphasis popup, over the chart's one raised set; collapsed
        // until a reader opens it, and nothing it shows is persisted.
        companion.addSection("emphasis", companionWords.say("emphasis.title"),
                new juranometria.ui.EmphasisControls(chart, companionWords).inController(),
                true);
        companion.addSection("placeandtime",
                companionWords.say("placeandtime.title"),
                new juranometria.ui.placeandtime.PlaceAndTimePanel(meridian,
                        placeStore, clock, companionWords));
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
        // The bar's visibility (Sprint 41, issue #450, ruled on #449):
        // the reader's choice, restored before the window is packed so
        // a hidden bar never flashes, and saved on every change. Hiding
        // it touches nothing else - not the companion, not the
        // Inspector, not the chart's state - and its controls keep
        // every subscription, so showing it again shows every change
        // made meanwhile.
        juranometria.ui.ChartChromeStore chromeStore = stores.chartChrome();
        toolbar.setVisible(chromeStore.toolbarShownOrDefault());

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
        juranometria.ui.solar.VenusChartStore venusChartStore = stores.venusChart();
        juranometria.ui.solar.MercuryChartStore mercuryChartStore = stores.mercuryChart();
        juranometria.solarchart.SolarSystemModule sunOnChart =
                juranometria.ui.solar.SunChartSession.begin(modules,
                        () -> meridian.attached() ? meridian.observer() : null,
                        solarSystem, meridian::subduing);
        // The Sun's and the Moon's applied query and result (#458,
        // ruled on #457): one session per body, shared by its dialog
        // and the Controller's group, over Place and Time's observer
        // and the same lazy pack. Nothing computes until a host asks.
        java.util.function.Supplier<juranometria.sky.Observer> observerNow =
                () -> meridian.attached() ? meridian.observer() : null;
        juranometria.ui.solar.SolarTableSession sunTable =
                new juranometria.ui.solar.SolarTableSession(observerNow,
                        solarSystem, juranometria.ui.solar.SolarTable.sun());
        juranometria.ui.solar.SolarTableSession moonTable =
                new juranometria.ui.solar.SolarTableSession(observerNow,
                        solarSystem, juranometria.ui.solar.SolarTable.moon());
        // Venus (#502): one session shared by its dialog and the
        // Controller's Venus group, over the same observer and the same
        // lazy service, which reads the planets pack the first time a
        // host computes Venus - never at startup, never on opening.
        juranometria.ui.solar.SolarTableSession venusTable =
                new juranometria.ui.solar.SolarTableSession(observerNow,
                        solarSystem, juranometria.ui.solar.SolarTable.venus());
        // Mercury (#530): the same, over the same lazy service, which reads
        // Mercury's file of the planets pack the first time a host
        // computes Mercury - never at startup, never on opening.
        juranometria.ui.solar.SolarTableSession mercuryTable =
                new juranometria.ui.solar.SolarTableSession(observerNow,
                        solarSystem, juranometria.ui.solar.SolarTable.mercury());
        // Each table follows the chart's one context (#612): a result is
        // stamped with the revision it was computed at, and when the
        // chart moves on the table says it is stale - never recomputed
        // until the reader computes (ruling P1).
        for (juranometria.ui.solar.SolarTableSession session
                : java.util.List.of(sunTable, moonTable, venusTable, mercuryTable)) {
            session.followContext(meridian.context());
        }
        // Jupiter and the four Galilean moons (#474): one session shared
        // by the Controller's Jupiter group and the Jupiter dialog, over
        // the same observer; the 53 MB Jovian pack is read once, when a
        // host first computes - never at startup, never on opening.
        java.util.function.Supplier<juranometria.solar.JovianSystemService>
                jovianSystem = new java.util.function.Supplier<>() {
                    private juranometria.solar.JovianSystemService loaded;

                    @Override
                    public synchronized juranometria.solar.JovianSystemService get() {
                        if (loaded == null) {
                            loaded = new juranometria.solar.JovianSystemService(
                                    juranometria.solar.SolarSystemPack.load(),
                                    juranometria.solar.JovianPack.load());
                        }
                        return loaded;
                    }
                };
        juranometria.ui.solar.JovianTableSession jupiterTable =
                new juranometria.ui.solar.JovianTableSession(observerNow, jovianSystem);
        jupiterTable.followContext(meridian.context());
        // Saturn, its rings and its seven ruled moons (#576): one session
        // shared by the Controller's Saturn group and the Saturn dialog,
        // over the same observer; the 79 MB Saturn pack is read once, when
        // a host first computes - never at startup, never on opening.
        java.util.function.Supplier<juranometria.solar.SaturnSystemService>
                saturnSystem = new java.util.function.Supplier<>() {
                    private juranometria.solar.SaturnSystemService loaded;

                    @Override
                    public synchronized juranometria.solar.SaturnSystemService get() {
                        if (loaded == null) {
                            loaded = new juranometria.solar.SaturnSystemService(
                                    juranometria.solar.SolarSystemPack.load(),
                                    juranometria.solar.SaturnPack.load());
                        }
                        return loaded;
                    }
                };
        juranometria.ui.solar.SaturnTableSession saturnTable =
                new juranometria.ui.solar.SaturnTableSession(observerNow, saturnSystem);
        saturnTable.followContext(meridian.context());
        // The Mars table (#603), over the same chart context: the 44 MB Mars
        // System pack is read once, when a host first computes - never at
        // startup, never on opening.
        java.util.function.Supplier<juranometria.solar.MarsService> marsSystem =
                new java.util.function.Supplier<>() {
                    private juranometria.solar.MarsService loaded;

                    @Override
                    public synchronized juranometria.solar.MarsService get() {
                        if (loaded == null) {
                            loaded = new juranometria.solar.MarsService(
                                    juranometria.solar.SolarSystemPack.load(),
                                    juranometria.solar.PlanetsPack.load(),
                                    juranometria.solar.MarsSystemPack.load());
                        }
                        return loaded;
                    }
                };
        juranometria.ui.solar.MarsTableSession marsTable =
                new juranometria.ui.solar.MarsTableSession(observerNow, marsSystem);
        marsTable.followContext(meridian.context());
        // The Uranus table (#635), over the same chart context: the 38 MB
        // Uranus System pack is read once, when a host first computes - never
        // at startup, never on opening.
        java.util.function.Supplier<juranometria.solar.UranusService> uranusSystem =
                new java.util.function.Supplier<>() {
                    private juranometria.solar.UranusService loaded;

                    @Override
                    public synchronized juranometria.solar.UranusService get() {
                        if (loaded == null) {
                            loaded = juranometria.solar.UranusService.load();
                        }
                        return loaded;
                    }
                };
        juranometria.ui.solar.UranusTableSession uranusTable =
                new juranometria.ui.solar.UranusTableSession(observerNow, uranusSystem);
        uranusTable.followContext(meridian.context());
        // Set chart time to its next meridian transit (#612 absorbing #569,
        // ruling P4): the Inspector's action over the selected object,
        // installing through the meridian module as Place and Time does.
        inspector.offerMeridianTransit(new MeridianTransitAction(meridian, solarSystem,
                jovianSystem, saturnSystem, juranometria.ui.language.InterfaceText
                        .forLanguage(language.interfaceLanguage())));
        // Jupiter on the chart (#484, ruled on #482): one Jovian module
        // over the same observer and the same lazy pack, with its one
        // remembered switch, off by default. The pack loads only when
        // the switch is on and a page asks.
        juranometria.ui.solar.JovianChartStore jovianChartStore = stores.jovianChart();
        juranometria.jovianchart.JovianModule jovianOnChart = modules.attach(
                new juranometria.jovianchart.JovianModule(observerNow, jovianSystem,
                        meridian::subduing));
        // Centre on chart (#483): one action for every body and host.
        // It centres through the chart's own controller - the centre and
        // the field in one notification - at the chart's normal minimum
        // field, and brings the chart window forward, closing nothing.
        juranometria.ui.solar.CentreOnChart centreOnChart =
                new juranometria.ui.solar.CentreOnChart(
                        new juranometria.ui.solar.CentreOnChart.Chart() {
                            @Override
                            public void centre(juranometria.chart.SkyPosition j2000,
                                               double fieldWidthDegrees) {
                                controller.recenter(j2000, fieldWidthDegrees);
                            }

                            @Override
                            public void bringForward() {
                                frame.toFront();
                                chart.requestFocusInWindow();
                            }

                            @Override
                            public double normalMinimumFieldDegrees() {
                                return juranometria.chart.ChartViewState
                                        .normalMinimumFieldDegrees();
                            }
                        });
        juranometria.ui.solar.BodyOnChart sunLayer =
                juranometria.ui.solar.SunChartSession.switchOf(sunOnChart, sunChartStore);
        juranometria.ui.solar.BodyOnChart moonLayer =
                juranometria.ui.solar.MoonChartSession.switchOf(sunOnChart, moonChartStore);
        // Venus on the chart (#514, the rulings on #512): the same module,
        // a third switch, remembered like the Sun's and the Moon's.
        juranometria.ui.solar.BodyOnChart venusLayer =
                juranometria.ui.solar.VenusChartSession.switchOf(sunOnChart, venusChartStore);
        // Mercury on the chart (#542, the rulings on #539): a fourth switch on
        // the same module, remembered like the others.
        juranometria.ui.solar.BodyOnChart mercuryLayer =
                juranometria.ui.solar.MercuryChartSession.switchOf(sunOnChart, mercuryChartStore);
        juranometria.ui.solar.BodyOnChart jupiterLayer =
                juranometria.ui.solar.JovianChartSession.switchOf(jovianOnChart,
                        jovianChartStore);
        // Saturn on the chart (#579, ruled on #578): one Saturn module over
        // the same observer and the same lazy pack, with its one remembered
        // switch, off by default; the pack loads only when the switch is on
        // and a page asks.
        juranometria.ui.solar.SaturnChartStore saturnChartStore = stores.saturnChart();
        juranometria.saturnchart.SaturnModule saturnOnChart = modules.attach(
                new juranometria.saturnchart.SaturnModule(observerNow, saturnSystem,
                        meridian::subduing));
        juranometria.ui.solar.BodyOnChart saturnLayer =
                juranometria.ui.solar.SaturnChartSession.switchOf(saturnOnChart,
                        saturnChartStore);
        // Mars on the chart (#606, the rulings on #605): a switch on the Solar
        // System module over Mars's own lazy service, remembered like the
        // others; the packs load only when the switch is on and a page asks.
        sunOnChart.withMars(marsSystem);
        juranometria.ui.solar.MarsChartStore marsChartStore = stores.marsChart();
        juranometria.ui.solar.BodyOnChart marsLayer =
                juranometria.ui.solar.MarsChartSession.switchOf(sunOnChart, marsChartStore);
        // Uranus on the chart (#638, the rulings on #637): a switch on the
        // Solar System module over Uranus's own lazy service, remembered like
        // the others; the pack loads only when the switch is on and a page asks.
        sunOnChart.withUranus(uranusSystem);
        juranometria.ui.solar.UranusChartStore uranusChartStore = stores.uranusChart();
        juranometria.ui.solar.BodyOnChart uranusLayer =
                juranometria.ui.solar.UranusChartSession.switchOf(sunOnChart, uranusChartStore);
        // Place and Time's box on the chart (#259, the rulings on #556):
        // drawn whenever a layer drawn for the instant is on - Place and
        // Time's own lines, or a Solar System body, whose switches are
        // composed here so the module learns nothing of the others - and
        // never otherwise. Its action is the one Update to now.
        juranometria.ui.placeandtime.PlaceAndTimeFurniture.offer(meridian,
                () -> sunLayer.showing() || mercuryLayer.showing()
                        || venusLayer.showing() || moonLayer.showing()
                        || jupiterLayer.showing() || saturnLayer.showing()
                        || marsLayer.showing() || uranusLayer.showing(),
                updateToNow,
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()));
        // Solar System in the Controller (ruled on #457), since #507 a
        // tab of its own after Chart: the Sun, Mercury (#530), Venus
        // (#502), Moon and Jupiter groups over those sessions and the
        // module's own switches, each remembering its own collapse, with
        // no outer group repeating the tab's name; building and choosing
        // the tab compute nothing; every group with its Show on chart and
        // Centre on chart, Mercury's since #542 and Jupiter's since #484.
        juranometria.ui.solar.SolarSystemSection solarSection =
                new juranometria.ui.solar.SolarSystemSection(sunTable, mercuryTable,
                        venusTable, moonTable, sunLayer, mercuryLayer, venusLayer, moonLayer,
                        jupiterTable, jupiterLayer, centreOnChart, companionWords);
        // Saturn's group after Jupiter's (#576), with its Show on chart and
        // Centre on chart since #579.
        solarSection.withSaturn(saturnTable, companionWords, saturnLayer, centreOnChart);
        // Mars's group between the Moon's and Jupiter's (#603), outward from
        // the Sun, with its Show on chart and Centre on chart since #606.
        solarSection.withMars(marsTable, companionWords, marsLayer, centreOnChart);
        // Uranus's group after Saturn's (#635), outward from the Sun, with its
        // Show on chart and Centre on chart since #638.
        solarSection.withUranus(uranusTable, companionWords, uranusLayer, centreOnChart);
        // The tab opens with the instant the chart is frozen at and Update
        // to now (#259, ruling 9 on #556).
        solarSection.showChartInstant(observerNow, placeAndTimeChanges, updateToNow);
        companion.addTab(juranometria.ui.solar.SolarSystemSection.ID,
                companionWords.say("companion.tab.solarsystem"),
                solarSection.inController(companionStore, companionWords));
        shutdown.onShutdown(solarSection::release);
        // Chart Options (#443, ruled on #442), third on the Chart tab
        // since #507 (fourth section since #458): the same controls
        // class the dialog holds, over the same controller, as
        // remembered groups; Restore Defaults asks over the companion.
        companion.addSection("chartoptions",
                companionWords.say("chartoptions.title"),
                new ChartOptionsControls(chartOptions,
                        () -> ChartOptionsDialog.restoreConfirmed(companion,
                                companionWords),
                        companionWords).inCompanion(companionStore,
                        companionWords));
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
                        clock,
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
                // Since #458 the dialog is a second host of the session
                // the Controller's group follows, so both show the same
                // applied result.
                () -> juranometria.ui.solar.SolarTableDialog.open(frame, sunTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, sunLayer),
                // View, Moon (#408): the same table shell over the same
                // observer and pack, for the Moon's own columns.
                () -> juranometria.ui.solar.SolarTableDialog.open(frame, moonTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, moonLayer),
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
                },
                // View, Chart Toolbar (#450): the bar, shown or hidden,
                // from the item or its keystroke.
                () -> showToolbar(frame, toolbar, chart, !toolbar.isVisible())));
        // View > Solar System > Venus... (#502), between Sun... and Moon...:
        // the dialog host of the same Venus session the Controller holds,
        // since #514 with Centre on chart over Venus's layer.
        AppMenuBar.addVenusTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.SolarTableDialog.open(frame, venusTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, venusLayer));
        // View > Solar System > Mercury... (#530), between Sun... and
        // Venus...: the dialog host of the same Mercury session the
        // Controller holds; no Centre on chart, since nothing of Mercury
        // is drawn.
        AppMenuBar.addMercuryTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.SolarTableDialog.open(frame, mercuryTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, mercuryLayer));
        // View > Solar System > Mercury on the chart (#542), between Sun on the
        // chart and Venus on the chart: the switch, remembered.
        javax.swing.JCheckBoxMenuItem mercuryOnChartItem = AppMenuBar.addMercuryOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.MercuryChartSession.toggle(sunOnChart, mercuryChartStore));
        juranometria.ui.solar.MercuryChartSession.restore(sunOnChart, mercuryChartStore,
                mercuryOnChartItem);
        // View > Solar System > Venus on the chart (#514), after Sun on the
        // chart: the switch, remembered, its item following the module.
        javax.swing.JCheckBoxMenuItem venusOnChartItem = AppMenuBar.addVenusOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.VenusChartSession.toggle(sunOnChart, venusChartStore));
        juranometria.ui.solar.VenusChartSession.restore(sunOnChart, venusChartStore,
                venusOnChartItem);
        // View > Solar System > Jupiter... (#474), after Moon...: the
        // dialog host of the same Jupiter session the Controller holds.
        AppMenuBar.addJupiterTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.JovianTableDialog.open(frame, jupiterTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, jupiterLayer));
        // View > Solar System > Mars... (#603), between Moon... and Jupiter...:
        // the dialog host of the same Mars session the Controller holds.
        AppMenuBar.addMarsTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.MarsTableDialog.open(frame, marsTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, marsLayer));
        // View > Solar System > Saturn... (#576), directly after Jupiter...:
        // the dialog host of the same Saturn session the Controller holds.
        AppMenuBar.addSaturnTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.SaturnTableDialog.open(frame, saturnTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, saturnLayer));
        // View > Solar System > Uranus... (#635), directly after Saturn...:
        // the dialog host of the same Uranus session the Controller holds.
        AppMenuBar.addUranusTable(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.solar.UranusTableDialog.open(frame, uranusTable,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()), centreOnChart, uranusLayer));
        // View > Solar System > Jupiter and moons on the chart (#484),
        // directly after Moon on the chart: the Jovian module's switch.
        javax.swing.JCheckBoxMenuItem jupiterOnChartItem = AppMenuBar.addJupiterOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.JovianChartSession.toggle(jovianOnChart,
                        jovianChartStore));
        // View > Centre on coordinates... (#623, rulings C-1 to C-3): J2000
        // coordinates centred through Search's coordinate contract, the
        // selection cleared as Search clears it, and the requested-position
        // cross left at the place until a search, a selection or Clear
        // selection ends it.
        AppMenuBar.addCentreOnCoordinates(frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                () -> juranometria.ui.coordinates.CoordinatesDialog.open(frame,
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage()),
                        position -> {
                            String shown = juranometria.chart.SkyFormat.formatRa(
                                    position.raDegrees()) + ", "
                                    + juranometria.chart.SkyFormat.formatDec(
                                            position.decDegrees());
                            juranometria.ui.SearchNavigation.Outcome moved =
                                    juranometria.ui.SearchNavigation.apply(
                                            new juranometria.search.SearchResult(shown,
                                                    shown,
                                                    juranometria.search.SearchResult.Kind
                                                            .COORDINATES,
                                                    position, shown),
                                            chart.assembler(), controller, selection);
                            chart.setRequestedPosition(moved
                                    == juranometria.ui.SearchNavigation.Outcome.NO_FIT
                                    ? chart.requestedPosition() : position);
                            return moved;
                        }));
        searchField.onSearch(() -> chart.setRequestedPosition(null));
        chartControls.search().onSearch(() -> chart.setRequestedPosition(null));
        selection.onChange(change -> {
            if (!(change.selection() instanceof juranometria.chart.Selection.None)) {
                chart.setRequestedPosition(null);
            }
        });
        modules.workingSelection().onChange(change -> chart.setRequestedPosition(null));
        // View > Solar System > Saturn on the chart (#579), directly after
        // Jupiter and moons on the chart: the Saturn module's switch.
        javax.swing.JCheckBoxMenuItem saturnOnChartItem = AppMenuBar.addSaturnOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.SaturnChartSession.toggle(saturnOnChart,
                        saturnChartStore));
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
        juranometria.ui.solar.JovianChartSession.restore(jovianOnChart,
                jovianChartStore, jupiterOnChartItem);
        juranometria.ui.solar.SaturnChartSession.restore(saturnOnChart,
                saturnChartStore, saturnOnChartItem);
        // View > Solar System > Mars on the chart (#606), directly after Moon
        // on the chart: the switch, remembered, its item following the module.
        javax.swing.JCheckBoxMenuItem marsOnChartItem = AppMenuBar.addMarsOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.MarsChartSession.toggle(sunOnChart, marsChartStore));
        juranometria.ui.solar.MarsChartSession.restore(sunOnChart, marsChartStore,
                marsOnChartItem);
        // View > Solar System > Uranus on the chart (#638), directly after
        // Saturn on the chart: the switch, remembered, its item following the module.
        javax.swing.JCheckBoxMenuItem uranusOnChartItem = AppMenuBar.addUranusOnChart(
                frame.getJMenuBar(),
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.interfaceLanguage()),
                juranometria.ui.solar.UranusChartSession.toggle(sunOnChart, uranusChartStore));
        juranometria.ui.solar.UranusChartSession.restore(sunOnChart, uranusChartStore,
                uranusOnChartItem);
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
        // The Chart Toolbar tick follows the bar, however it was
        // hidden, and the store follows it too (#450).
        javax.swing.JCheckBoxMenuItem toolbarItem =
                AppMenuBar.toolbarItem(frame.getJMenuBar());
        toolbarItem.setSelected(toolbar.isVisible());
        toolbar.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent event) {
                toolbarItem.setSelected(true);
                chromeStore.saveToolbarShown(true);
            }

            @Override
            public void componentHidden(java.awt.event.ComponentEvent event) {
                toolbarItem.setSelected(false);
                chromeStore.saveToolbarShown(false);
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
        // Where the window opens (#450, ruled on #449): where it was,
        // if that is on a screen that exists now, else packed and
        // centred as it always has been; then maximised if it was.
        // The ordinary bounds are saved on every move and resize in
        // the normal state - never a maximised window's, which the
        // flag remembers, and never native full screen's, which is
        // not remembered at all.
        juranometria.ui.ChartWindowStore windowStore = stores.chartWindow();
        java.util.Optional<java.awt.Rectangle> opening =
                juranometria.ui.ChartWindowPlacement.opening(windowStore.bounds(),
                        juranometria.ui.ChartWindowPlacement.screensNow(),
                        frame.getMinimumSize());
        if (opening.isPresent()) {
            frame.setBounds(opening.get());
        } else {
            frame.setLocationRelativeTo(null);
        }
        if (windowStore.maximized()) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent event) {
                rememberOrdinary(frame, windowStore);
            }

            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                rememberOrdinary(frame, windowStore);
            }
        });
        frame.addWindowStateListener(event -> windowStore.saveMaximized(
                (event.getNewState() & JFrame.MAXIMIZED_BOTH)
                        == JFrame.MAXIMIZED_BOTH));
        frame.setVisible(true);
        // The companion starts closed at every launch (ruled on #455):
        // open, it tends to cover the chart a reader came for, and View
        // is one gesture away. It opens where it was left.
        return frame;
    }
}
