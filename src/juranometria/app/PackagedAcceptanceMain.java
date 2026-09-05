package juranometria.app;

import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

import juranometria.chart.ChartViewport;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartScene;
import juranometria.chart.Selection;
import juranometria.chart.SelectionDetails;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.render.ChartOptions;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.ui.ChartViewController;

/**
 * The packaged acceptance surface of issue #150, run INSIDE the
 * native application image through its inner launcher (no system
 * Java, no display): exercises the About dialog's real content
 * paths - the packaged licensing summary and the complete
 * concatenated notice texts, which throw on any missing resource -
 * and a genuine preference change-and-reload through the bundled
 * runtime's java.prefs backend (the registry on Windows, plists on
 * macOS, the file store on Linux), against the application's own
 * preference node with the prior value restored afterwards. Prints
 * one line per verified surface and exits nonzero on any failure.
 */
public final class PackagedAcceptanceMain {

    private PackagedAcceptanceMain() {
    }

    /**
     * Runs something with a temporary chart-options choice in the
     * reader's own store, and puts theirs back whatever happens.
     *
     * <p>An acceptance run that changes a real preference and
     * restores it on the way out restores it only when it passes -
     * so the run that finds a defect is also the run that leaves the
     * reader with galaxies switched off (sprint review). A check
     * that damages what it is checking is worse than no check.
     */
    static void withTemporaryOptions(ChartOptionsStore store,
                                     ChartOptions temporary,
                                     ThrowingRunnable body) throws Exception {
        // Read first, and mutate nothing until the guard is up: the
        // save and the flush that follows can each fail, and the
        // first version left them outside the try, so a flush that
        // threw left the reader wearing this run's choice with
        // nobody to put theirs back (sprint review).
        ChartOptions theirs = store.load();
        Throwable failure = null;
        try {
            store.save(temporary);
            // Asked of this store rather than of a node assumed to
            // be behind it - the helper used to flush the
            // application's own node even when handed another store.
            store.flush();
            body.run();
        } catch (Throwable thrown) {
            failure = thrown;
            throw thrown;
        } finally {
            try {
                store.save(theirs);
                store.flush();
            } catch (RuntimeException | Error restoring) {
                // A restoration that failed must not replace the
                // failure it was cleaning up after: that would hide
                // the very thing the run exists to report.
                if (failure == null) {
                    throw restoring;
                }
                failure.addSuppressed(restoring);
            }
        }
    }

    /** A body that may fail, which is the case that matters here. */
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        // About, through its real static content paths: the packaged
        // summary must state every licence family and the
        // non-commercial consequence; the full notice view must load
        // every bundled notice resource (it throws on a missing one).
        String summary = AboutDialog.summaryText();
        require(summary.contains("CC BY-NC 3.0 IGO")
                        && summary.contains("non-commercially")
                        && summary.contains("BSD-3-Clause")
                        && summary.contains("CC BY-SA 4.0"),
                "About summary states the licence families");
        String notices = AboutDialog.noticesText();
        require(notices.contains("may not be used commercially")
                        && notices.contains("Redistribution and use in"
                                + " source and binary forms"),
                "About notices load every packaged resource");
        require(!AppInfo.version().isBlank()
                        && !"unknown".equals(AppInfo.version()),
                "the packaged version is a real version, not the"
                        + " 'unknown' fallback: " + AppInfo.version());
        // The 1.0 acceptance binding (issue #146): the application
        // running inside this image must report the version the
        // image was built for and named after. An archive that
        // claims one version while its application reports another
        // is exactly the artifact a release must never publish, and
        // only the packaged runtime can answer this.
        if (args.length > 0 && !args[0].isBlank()) {
            require(args[0].equals(AppInfo.version()),
                    "the packaged application reports the version"
                            + " this image was built for: expected "
                            + args[0] + ", running "
                            + AppInfo.version());
            System.out.println("version binding OK (image and"
                    + " application both " + args[0] + ")");
        }
        System.out.println("about surface OK (summary, "
                + notices.length() + " chars of notices, version "
                + AppInfo.version() + ")");

        // Preferences, changed and reloaded through the bundled
        // runtime against the application's real node - snapshot the
        // reader's actual choice, flip it, prove a FRESH store reads
        // the flip, then restore and prove the restoration.
        ChartOptionsStore store = ChartOptionsStore.user();
        ChartOptions before = store.load();
        ChartOptions flipped = new ChartOptions(before.deepSkyObjects(),
                before.deepSkyLabels(), before.constellationFigures(),
                before.constellationBoundaries(),
                before.constellationNames(), before.starNames(),
                before.bayerLetters(), before.flamsteedNumbers(),
                !before.equatorialGrid());
        withTemporaryOptions(store, flipped, () ->
                require(ChartOptionsStore.user().load().equals(flipped),
                        "a fresh store reloads the changed preference"));
        require(ChartOptionsStore.user().load().equals(before),
                "the original preference is restored");
        System.out.println("preference change-and-reload OK"
                + " (equatorialGrid flipped and restored through the"
                + " bundled runtime's preference backend)");

        readerJourney();
        onThisPageJourney();
        meridianJourney();

        System.out.println("PACKAGED ACCEPTANCE OK");
    }


    /**
     * The reader's journey, run INSIDE the packaged application on
     * the bundled runtime (issue #146, sprint review P1). The other
     * checks above prove the image can answer questions about
     * itself; this one proves the atlas works in it.
     *
     * Every step goes through the production classes the window
     * drives - the same search, the same navigation controller, the
     * same renderer, the same preference store - so what is proved
     * here is the packaged artifact, not a rehearsal of it in the
     * test suite. It renders headlessly because a release cell has
     * no screen; what a screen adds is the on-screen journey the
     * maintainer runs on real machines.
     */
    /**
     * What a reader does with <strong>On this page</strong>, inside
     * the packaged runtime (Sprint 24, issue #217).
     *
     * <p>Every native image runs this. The feature is built out of
     * the bundled pack, the projection and the renderer, and a
     * package that shipped a broken one of those would answer this
     * wrongly here rather than in front of a reader.
     *
     * <p>It marks an object the page does <em>not</em> draw, on
     * purpose: that is the case the whole sprint exists for, and the
     * one a smoke test of the visible chart would never reach.
     */
    private static void meridianJourney() throws Exception {
        // The removable meridian module, on a real chart component,
        // through the bundled runtime (issue #227). Counted rather
        // than predicted, and for the same reason as the journey
        // below it: an image missing the module, the reference
        // painter or the sky model must fail here, and only a page
        // that was actually drawn can show that.
        //
        // The observer is a stated case, not a place anyone lives:
        // 42.5 N, 130.994 W at the 2026 equinox, chosen because it
        // lays the meridian across the released reference page and
        // puts the zenith on clear paper above M31. The horizon is
        // ninety degrees from that zenith and crosses no part of
        // this page - the silence the decision asks for, exercised
        // on purpose.
        juranometria.ui.ChartComponent chart =
                new juranometria.ui.ChartComponent(Atlas.assembler());
        chart.setSize(900, 700);
        chart.setViewState(ChartViewState.DEFAULT);
        java.util.List<juranometria.module.NavigationRequest> asked =
                new java.util.ArrayList<>();
        juranometria.ui.ChartModuleHost host =
                new juranometria.ui.ChartModuleHost(chart,
                        new juranometria.chart.SelectionModel(), asked::add);

        java.awt.image.BufferedImage without = paint(chart);

        juranometria.meridian.MeridianModule module =
                host.attach(new juranometria.meridian.MeridianModule(
                        new juranometria.sky.Observer(42.5, -130.994,
                                java.time.Instant.parse(
                                        "2026-03-20T21:33:00Z"))));

        // Attached but showing nothing: removable has to mean
        // removable, and a module that is merely quiet must leave the
        // page exactly as it found it.
        module.showing(false, false, false);
        require(differingPixels(without, paint(chart)) == 0,
                "a module showing nothing leaves no mark on the page");
        require(module.timesTheSkyWasComputed() == 0,
                "and works out no sidereal time to say so");

        // Each geometry alone, before all three together: a single
        // combined count would accept an incorrectly drawn horizon,
        // because any pixels it wrongly put down would hide inside
        // the meridian's and the zenith's (review). On this page the
        // horizon is ninety degrees from a zenith that is ON the
        // page, so the right amount of horizon ink is exactly none -
        // proved on its own, where nothing else's ink can cover for
        // it.
        module.showing(false, true, false);
        require(differingPixels(without, paint(chart)) == 0,
                "the horizon alone leaves this page untouched: it"
                        + " crosses no part of the paper, and off the"
                        + " page is silence");
        module.showing(true, false, false);
        int meridianInk = differingPixels(without, paint(chart));
        require(meridianInk > 100, "the meridian alone is drawn: "
                + meridianInk + " pixels");
        module.showing(false, false, true);
        int zenithInk = differingPixels(without, paint(chart));
        require(zenithInk > 8 && zenithInk < meridianInk,
                "the zenith alone is a small ring, not a line: "
                        + zenithInk + " pixels");

        module.showing(true, true, true);
        java.awt.image.BufferedImage with = paint(chart);
        int ink = differingPixels(without, with);
        require(ink > 100, "the reference ink reaches the page: " + ink
                + " pixels changed");
        require(ink <= meridianInk + zenithInk,
                "and all three together add nothing a horizon could"
                        + " be hiding in: " + ink + " pixels against "
                        + meridianInk + " + " + zenithInk);

        java.util.List<String> offered = new java.util.ArrayList<>();
        for (var owned : chart.overlays().collect()) {
            offered.add(owned.geometry().identity());
        }
        require(offered.equals(java.util.List.of("meridian", "horizon",
                        "zenith")),
                "the module offers its three geometries: " + offered);

        // And the zenith ring is drawn where the model puts it.
        double[] at = host.projection()
                .toPage(new juranometria.sky.LocalSky(module.observer())
                        .zenith())
                .orElseThrow();
        require(inkNear(with, without, (int) Math.round(at[0]),
                        (int) Math.round(at[1]), 10),
                "and the zenith is inked where the model puts it, at "
                        + Math.round(at[0]) + "," + Math.round(at[1]));

        // Setting a place or an instant redraws and does not move the
        // page; only a reader asking does that.
        juranometria.chart.ChartViewState where = chart.viewState();
        module.observer(module.observer().from(-33.87, 151.21));
        require(chart.viewState().equals(where)
                        && asked.isEmpty(),
                "changing the place redraws the lines and leaves the"
                        + " page where the reader put it");
        module.centreOnZenith();
        require(asked.size() == 1,
                "and only a reader asking moves it, once: " + asked);

        module.detach();
        require(chart.overlays().collect().isEmpty(),
                "detaching withdraws every line");
        require(differingPixels(without, paint(chart)) == 0,
                "and the page is the page the atlas draws without it");

        // The stored-state restart, as a second application session
        // (issue #229): the place is saved through the bundled
        // preference backend, and then the whole wiring is built
        // again the way JUranometriaMain builds it - a fresh chart,
        // a fresh host, a fresh module fed from a fresh store. A
        // first version only reloaded the store and constructed an
        // observer by hand, which proves the backend and nothing
        // about what a reader's next evening looks like (sprint
        // review). Run against a dedicated node, never the reader's
        // real preferences.
        java.util.prefs.Preferences restartNode =
                java.util.prefs.Preferences.userRoot().node(
                        "juranometria-packaged-restart");
        try {
            juranometria.ui.placeandtime.PlaceStore first =
                    juranometria.ui.placeandtime.PlaceStore
                            .forNode(restartNode);
            first.save(42.5, -130.994);
            first.flush();

            // A September moment at the same sidereal time as the
            // equinox case, so the remembered place puts the same
            // lines back on this same page - the sky repeats every
            // sidereal day, and an arbitrary hour would honestly
            // draw nothing here.
            java.time.Instant secondSession =
                    java.time.Instant.parse("2026-09-05T10:28:31Z");
            juranometria.ui.ChartComponent nextEvening =
                    new juranometria.ui.ChartComponent(Atlas.assembler());
            nextEvening.setSize(900, 700);
            nextEvening.setViewState(ChartViewState.DEFAULT);
            juranometria.ui.ChartModuleHost secondHost =
                    new juranometria.ui.ChartModuleHost(nextEvening,
                            new juranometria.chart.SelectionModel(),
                            request -> { });
            juranometria.meridian.MeridianModule restarted =
                    juranometria.ui.placeandtime.PlaceAndTimeSession
                            .begin(secondHost,
                                    juranometria.ui.placeandtime.PlaceStore
                                            .forNode(restartNode),
                                    secondSession);
            try {
                require(restarted.observer().latitudeDegrees() == 42.5
                                && restarted.observer()
                                        .eastLongitudeDegrees()
                                        == -130.994,
                        "the second session's module wears the place the"
                                + " first one saved, read through the"
                                + " bundled preference backend");
                require(restarted.observer().instant()
                                .equals(secondSession),
                        "and the new session's instant: no stored"
                                + " moment exists to masquerade as Now");
                require(differingPixels(without, paint(nextEvening)) == 0,
                        "the next evening begins on the ordinary chart:"
                                + " the switches were not remembered");
                restarted.showing(true, true, true);
                require(differingPixels(without, paint(nextEvening)) > 100,
                        "and the remembered place draws real lines when"
                                + " asked");
                for (String key : restartNode.keys()) {
                    require(key.startsWith("place."),
                            "the store holds the place and nothing"
                                    + " else: " + key);
                }
            } finally {
                secondHost.detachAll();
            }
        } finally {
            restartNode.removeNode();
        }

        System.out.println("meridian module OK (" + meridianInk
                + " px meridian + " + zenithInk + " px zenith, the"
                + " horizon proved silent alone, withdrawn cleanly,"
                + " page never moved except when asked, and the place"
                + " survives a restart while the instant does not)");
    }

    private static void onThisPageJourney() throws Exception {
        // Through the real module on a real chart component, and the
        // ink is counted rather than predicted.
        //
        // The first version of this built its own marks model and
        // recomputed which objects "would" be crossed with a copy of
        // the module's own rule (sprint review). It never attached
        // the module and never drew a pixel, so it could have passed
        // in an image where the module was missing or the ink was
        // broken - which is the one thing a packaged acceptance
        // exists to catch.
        juranometria.ui.ChartComponent chart =
                new juranometria.ui.ChartComponent(Atlas.assembler());
        chart.setSize(900, 700);
        chart.setViewState(ChartViewState.DEFAULT);
        juranometria.ui.ChartModuleHost host =
                new juranometria.ui.ChartModuleHost(chart,
                        new juranometria.chart.SelectionModel(),
                        request -> { });
        juranometria.ui.onthispage.OnThisPageModule module =
                host.attach(new juranometria.ui.onthispage.OnThisPageModule());

        juranometria.page.PageContents inventory = host.inventory();
        require(inventory.entries().size() > 50,
                "the released page has an inventory: "
                        + inventory.entries().size() + " entries");
        int listed = module.panel().rows().size();
        require(listed > 10,
                "and the module's own table lists it: " + listed
                        + " rows");

        // Present and invisible: the state the table exists to
        // explain, and the only kind of object that gets a cross.
        juranometria.page.PageEntry invisible = null;
        juranometria.page.PageEntry drawn = null;
        for (juranometria.page.PageEntry entry : inventory.entries()) {
            if (invisible == null && entry.visibility()
                    != juranometria.page.PageVisibility.DRAWN) {
                invisible = entry;
            }
            if (drawn == null && entry.visibility()
                    == juranometria.page.PageVisibility.DRAWN) {
                drawn = entry;
            }
        }
        require(invisible != null && drawn != null,
                "the released page holds both a drawn object and one"
                        + " that is present but not drawn");

        java.awt.image.BufferedImage unmarked = paint(chart);
        // Declared out here so the summary below can still say what
        // was measured, and assigned inside the guarded region.
        int added;
        try {

        host.workingSelection().replaceWith(java.util.List.of(drawn.identity(),
                invisible.identity()), invisible.identity());

        // What the chart was actually given to ink, by the module.
        java.util.List<String> offered = new java.util.ArrayList<>();
        for (var owned : chart.overlays().collect()) {
            offered.add(owned.geometry().identity());
        }
        require(offered.equals(java.util.List.of(invisible.identity())),
                "the module offers one cross, for the object the page"
                        + " does not draw: " + offered);

        // And what it looks like on the page: ink that was not there
        // before, at the pixel the projection puts the object at.
        java.awt.image.BufferedImage marked = paint(chart);
        added = differingPixels(unmarked, marked);
        require(added > 8, "the cross reaches the page: " + added
                + " pixels changed");
        double[] at = host.projection().toPage(invisible.position())
                .orElseThrow();
        require(inkNear(marked, unmarked, (int) Math.round(at[0]),
                        (int) Math.round(at[1]), 9),
                "and it is drawn where the object is, at "
                        + Math.round(at[0]) + "," + Math.round(at[1]));

        host.workingSelection().clear();
        require(chart.overlays().collect().isEmpty(),
                "clearing withdraws the ink");
        require(differingPixels(unmarked, paint(chart)) == 0,
                "and the page returns to the one the atlas draws with"
                        + " nothing marked, pixel for pixel");

        // A restart begins empty, because there is nowhere for a mark
        // to have been kept: the reader's stored options survive and
        // the working set does not.
        Preferences node = Preferences.userRoot().node("juranometria");
        node.flush();
        for (String key : node.keys()) {
            require(!key.toLowerCase(java.util.Locale.ROOT)
                            .contains("mark"),
                    "no working mark was written to preferences: " + key);
        }
        } finally {
            host.detachAll();
        }
        require(chart.overlays().collect().isEmpty(),
                "and leaving releases the module");

        // A second session, built the way the first was. Asserting
        // that a fresh WorkingMarksModel is empty proves only that a
        // new object is new (sprint review): what a restart has to
        // show is that the application, assembled again from
        // scratch, comes up with nothing marked and nothing inked -
        // while the reader's stored options are still there, which
        // is the difference between ephemeral and forgotten.
        // The reader leaves with a choice made: galaxies off, and
        // it goes back whatever happens below - including when a
        // require fails and this run ends here.
        ChartOptions theirs = ChartOptionsStore.user().load();
        ChartOptions galaxiesOff = theirs.withFamily(
                juranometria.render.SymbolFamily.GALAXIES, false);
        int[] hiddenByChoice = {0};
        withTemporaryOptions(ChartOptionsStore.user(), galaxiesOff, () -> {
            juranometria.ui.ChartComponent restarted =
                    new juranometria.ui.ChartComponent(Atlas.assembler());
            restarted.setSize(900, 700);
            restarted.setViewState(ChartViewState.DEFAULT);
            ChartOptions reloaded = ChartOptionsStore.user().load();
            restarted.setChartOptions(reloaded);
            juranometria.ui.ChartModuleHost second =
                    new juranometria.ui.ChartModuleHost(restarted,
                            new juranometria.chart.SelectionModel(),
                            request -> { });
            juranometria.ui.onthispage.OnThisPageModule again =
                    second.attach(
                            new juranometria.ui.onthispage.OnThisPageModule());
            try {
                require(second.workingSelection().members().isEmpty()
                                && second.workingSelection().lead() == null,
                        "the new session begins with nothing marked");
                require(restarted.overlays().collect().isEmpty(),
                        "and nothing inked");
                require(again.panel().rows().size() == listed,
                        "it lists the same page as before: "
                                + again.panel().rows().size() + " rows");

                // The reader's choice is not merely readable - it is
                // in force.
                require(reloaded.equals(galaxiesOff),
                        "the restart read the choice the reader left");
                require(restarted.chartOptions().equals(galaxiesOff),
                        "and applied it, rather than starting on the"
                                + " defaults");
                hiddenByChoice[0] = second.inventory().tally()
                        .get(juranometria.page.PageVisibility.FAMILY_HIDDEN);
                require(hiddenByChoice[0] > 0,
                        "so the restarted session reports objects"
                                + " hidden by a chart option: "
                                + second.inventory().tally());
                require(second.inventory().entries().size()
                                == inventory.entries().size(),
                        "while the page holds exactly what it held"
                                + " before - a choice about drawing is"
                                + " not a choice about what is there");
            } finally {
                second.detachAll();
            }
        });
        require(ChartOptionsStore.user().load().equals(theirs),
                "and the reader's own options are back");
        int hidden = hiddenByChoice[0];

        System.out.println("on this page OK (" + inventory.entries().size()
                + " entries, " + listed
                + " rows, marked " + invisible.identity()
                + " which the page does not draw, " + added
                + " pixels of cross drawn at its own position,"
                + " cleared to the byte, and a second session begins"
                + " empty while wearing the reader's stored choice,"
                + " with " + hidden + " objects hidden by it)");
    }

    /** The component's own painting, into an image. */
    private static java.awt.image.BufferedImage paint(
            juranometria.ui.ChartComponent chart) {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(chart.getWidth(),
                        chart.getHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int differingPixels(java.awt.image.BufferedImage a,
                                       java.awt.image.BufferedImage b) {
        int differing = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        return differing;
    }

    /** Whether new ink appeared within this many pixels of a point. */
    private static boolean inkNear(java.awt.image.BufferedImage marked,
                                   java.awt.image.BufferedImage unmarked,
                                   int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= marked.getWidth()
                        || py >= marked.getHeight()) {
                    continue;
                }
                if (marked.getRGB(px, py) != unmarked.getRGB(px, py)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void readerJourney() throws Exception {
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);

        // The default page: M31, 8 degrees, stars to V 8.0.
        ChartViewState home = navigation.state();
        require(home.fieldWidthDegrees() == 8.0
                        && home.limitingMagnitude() == 8.0
                        && home.targetLabel().contains("M31"),
                "the default page is the reviewed one: "
                        + home.targetLabel() + " at "
                        + home.fieldWidthDegrees() + " degrees");
        BufferedImage homePage = page(renderer, navigation,
                ChartOptions.DEFAULTS);
        int homeInk = ink(homePage);
        require(homeInk > 1000, "the default page draws a sky: "
                + homeInk + " inked pixels");

        // Search, across forms and both hemispheres: a traditional
        // name, a Bayer designation, a Flamsteed number, deep-sky
        // objects north and south, and coordinates in both notations.
        String[][] queries = {
                {"betelgeuse", "Betelgeuse"},
                {"alpha crucis", "Acrux"},
                {"61 Cyg", "61 Cyg"},
                {"M42", "M 42"},
                {"NGC 253", "Sculptor"},
                {"0:42:44 +41:16:09", "0h"},
                {"83.82 -5.39", "5h"},
        };
        for (String[] query : queries) {
            var results = Atlas.search().search(query[0]);
            require(!results.isEmpty(),
                    "search finds '" + query[0] + "'");
            var found = results.get(0);
            require(found.regionTitle().contains(query[1]),
                    "'" + query[0] + "' finds " + query[1] + ": "
                            + found.regionTitle());
            navigation.recenter(found.position(), found.regionTitle(),
                    found.identity());
            require(ink(page(renderer, navigation, ChartOptions.DEFAULTS))
                            > 200,
                    "and its page draws: " + found.regionTitle());
        }
        System.out.println("search journey OK (" + queries.length
                + " forms, both hemispheres, each page rendered)");

        // Zoom: the promised sequence, walked by the same calls the
        // toolbar and keyboard make, with the target surviving.
        while (navigation.state().fieldWidthDegrees() > 1.0) {
            navigation.zoomIn();
        }
        require(navigation.state().fieldWidthDegrees() == 1.0,
                "zoom reaches the 1-degree page");
        while (navigation.state().fieldWidthDegrees() < 36.0) {
            navigation.zoomOut();
        }
        require(navigation.state().fieldWidthDegrees() == 36.0
                        && navigation.state().targetIdentity() != null,
                "and the 36-degree page, target intact");

        // Pointer zoom, where the reader points. Chosen in open
        // sky at a field where the step is ACCEPTED, and required
        // to be accepted (sprint review: a refusal used to satisfy
        // this leg by skipping it).
        navigation.recenter(new SkyPosition(83.8, -5.4), 18.0);
        var viewport = new ChartViewport(navigation.state().centre(),
                navigation.state().fieldWidthDegrees(), 900, 700);
        var pointer = PanSolver.planeFromPixel(viewport,
                new PixelPoint(700, 250));
        var under = PanSolver.skyFromPlane(viewport.centre(), pointer);
        var outcome = navigation.zoomAt(pointer, true);
        require(outcome == ChartViewController.PointerZoomOutcome.ACCEPTED,
                "a pointer zoom in open sky is accepted: " + outcome);
        require(navigation.state().fieldWidthDegrees() == 12.0,
                "and steps 18 to 12 degrees: "
                        + navigation.state().fieldWidthDegrees());
        var after = new ChartViewport(navigation.state().centre(),
                navigation.state().fieldWidthDegrees(), 900, 700);
        var stillUnder = PanSolver.skyFromPlane(after.centre(),
                PanSolver.planeFromPixel(after, new PixelPoint(700, 250)));
        double adrift = under.separationDegrees(stillUnder);
        require(adrift < 1.0e-9,
                "and the sky stays under the pointer exactly: "
                        + adrift + " degrees adrift");
        System.out.println("navigation journey OK (field sequence"
                + " walked to both bounds, pointer zoom accepted and"
                + " anchored to " + String.format("%.1e", adrift)
                + " degrees)");

        // Grab-to-pan across the RA wrap, WITH a live target, so the
        // atomic clearing is a real transition rather than a
        // restatement of null (sprint review).
        var eastOfZero = Atlas.search().search("alpheratz").get(0);
        navigation.recenter(eastOfZero.position(),
                eastOfZero.regionTitle(), eastOfZero.identity());
        while (navigation.state().fieldWidthDegrees() > 18.0) {
            navigation.zoomIn();
        }
        while (navigation.state().fieldWidthDegrees() < 18.0) {
            navigation.zoomOut();
        }
        require(navigation.state().targetIdentity() != null,
                "the wrap begins on a searched target: "
                        + navigation.state().targetLabel());
        double raBefore = navigation.state().centre().raDegrees();
        require(raBefore < 20.0,
                "just east of RA 0, so a westward drag must cross it: "
                        + raBefore);
        var wrap = new ChartViewport(navigation.state().centre(),
                navigation.state().fieldWidthDegrees(), 900, 700);
        require(navigation.pan(
                        PanSolver.skyFromPlane(wrap.centre(),
                                PanSolver.planeFromPixel(wrap,
                                        new PixelPoint(700, 350))),
                        PanSolver.planeFromPixel(wrap,
                                new PixelPoint(200, 350))),
                "the chart pans");
        double raAfter = navigation.state().centre().raDegrees();
        require(raAfter > 340.0 && raAfter < 360.0,
                "and crosses RA 0 rather than drifting: " + raBefore
                        + " to " + raAfter);
        require(navigation.state().targetIdentity() == null
                        && navigation.state().targetLabel() == null,
                "the first real pan clears the target and its label"
                        + " together, atomically: identity "
                        + navigation.state().targetIdentity()
                        + ", label " + navigation.state().targetLabel());
        require(Atlas.assembler()
                        .assemble(navigation.state(), 900, 700).title()
                        .matches("\\d+h.*"),
                "so the page titles itself by coordinates instead");

        // Near the pole, both halves of the honest contract: a
        // modest grab follows the hand, and one that would carry
        // past the pole is refused without moving anything.
        navigation.recenter(new SkyPosition(0.0, 85.0), 18.0);
        SkyPosition atPole = navigation.state().centre();
        var polar = new ChartViewport(atPole, 18.0, 900, 700);
        require(navigation.pan(
                        PanSolver.skyFromPlane(polar.centre(),
                                PanSolver.planeFromPixel(polar,
                                        new PixelPoint(450, 300))),
                        PanSolver.planeFromPixel(polar,
                                new PixelPoint(450, 380))),
                "a modest grab near the pole follows the hand");
        double poleward = navigation.state().centre().decDegrees();
        require(poleward > 85.5 && poleward < 90.0,
                "moving poleward and staying on the sky: " + poleward);

        navigation.recenter(new SkyPosition(0.0, 85.0), 18.0);
        SkyPosition unmoved = navigation.state().centre();
        var overPole = new ChartViewport(unmoved, 18.0, 900, 700);
        require(!navigation.pan(
                        PanSolver.skyFromPlane(overPole.centre(),
                                PanSolver.planeFromPixel(overPole,
                                        new PixelPoint(450, 250))),
                        PanSolver.planeFromPixel(overPole,
                                new PixelPoint(450, 500))),
                "a grab that would carry past the pole is refused");
        require(navigation.state().centre().equals(unmoved),
                "and refusing moves nothing at all: "
                        + navigation.state().centre());
        System.out.println("pan journey OK (crossed RA 0 from "
                + String.format("%.1f", raBefore) + " to "
                + String.format("%.1f", raAfter)
                + ", target cleared; near the pole followed to dec "
                + String.format("%.1f", poleward)
                + " and refused an over-pole grab without drift)");

        // Magnitude, between its promised bounds.
        for (int step = 0; step < 8; step++) {
            navigation.decreaseMagnitudeLimit();
        }
        require(navigation.state().limitingMagnitude() == 4.0,
                "the magnitude limit stops at V 4.0");
        for (int step = 0; step < 8; step++) {
            navigation.increaseMagnitudeLimit();
        }
        require(navigation.state().limitingMagnitude() == 8.0,
                "and at V 8.0");

        // Layers: turning ink off must remove ink, and the
        // identifier layers must be separable.
        navigation.reset();
        ChartOptions bare = new ChartOptions(false, false, false, false,
                false, false, false, false, false);
        int bareInk = ink(page(renderer, navigation, bare));
        require(bareInk < homeInk,
                "every layer off draws less than every layer on: "
                        + bareInk + " < " + homeInk);
        ChartOptions namesOnly = new ChartOptions(true, true, true, true,
                true, true, false, false, true);
        require(ink(page(renderer, navigation, namesOnly)) != homeInk,
                "and the identifier layers are separable");
        System.out.println("chart options journey OK (layers off "
                + bareInk + " ink, all on " + homeInk + ")");

        // The black sky (issue #246): the same page on the other
        // ground must differ for the intended palette pixels while
        // its geometry - the set of non-ground pixels - and the
        // reader's state stay put. The 99.9% bound is the study's
        // measured worst case (155 edge-rounding pixels of 630,000)
        // with room, and a broken palette misses it by miles.
        ChartViewState placeBefore = navigation.state();
        BufferedImage paperPage = page(renderer, navigation,
                ChartOptions.DEFAULTS);
        BufferedImage darkPage = page(renderer, navigation,
                ChartOptions.DEFAULTS.withPalette(
                        juranometria.render.ChartPalette.BLACK_SKY));
        require(!java.util.Arrays.equals(bytes(paperPage),
                        bytes(darkPage)),
                "the black sky draws different ink");
        int paperGround = juranometria.render.ChartPalette.WHITE_PAPER
                .ground().getRGB();
        int darkGround = juranometria.render.ChartPalette.BLACK_SKY
                .ground().getRGB();
        int maskAgree = 0;
        int total = paperPage.getWidth() * paperPage.getHeight();
        for (int y = 0; y < paperPage.getHeight(); y++) {
            for (int x = 0; x < paperPage.getWidth(); x++) {
                if ((paperPage.getRGB(x, y) != paperGround)
                        == (darkPage.getRGB(x, y) != darkGround)) {
                    maskAgree++;
                }
            }
        }
        require(maskAgree >= total * 0.999,
                "the black sky changes ink, never geometry: "
                        + (total - maskAgree) + " mask pixels differ");
        // The commonest pixel, not the centre one: the default page
        // centres on M31, whose pale wash is exactly what sits at
        // the middle - the sky is what dominates the page.
        java.util.Map<Integer, Integer> darkCensus =
                new java.util.HashMap<>();
        for (int y = 0; y < darkPage.getHeight(); y++) {
            for (int x = 0; x < darkPage.getWidth(); x++) {
                darkCensus.merge(darkPage.getRGB(x, y), 1,
                        Integer::sum);
            }
        }
        require(darkCensus.entrySet().stream()
                        .max(java.util.Map.Entry.comparingByValue())
                        .orElseThrow().getKey() == darkGround,
                "the dark page's sky is the black ground");
        require(navigation.state() == placeBefore,
                "and rendering the other ground moved nothing");
        System.out.println("black sky journey OK (mask agreement "
                + (total - maskAgree) + " px short of exact on "
                + total + ", ground black, place untouched)");

        // Persistence and the documented upgrade, through the
        // bundled runtime's own preference backend, on a scratch
        // node so the reader's settings are never involved.
        Preferences scratch = Preferences.userRoot()
                .node("juranometria-packaged-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(scratch);
            store.save(namesOnly);
            scratch.flush();
            require(ChartOptionsStore.forNode(scratch).load()
                            .equals(namesOnly),
                    "a restart reads back the reader's choices");

            Preferences legacy = Preferences.userRoot()
                    .node("juranometria-legacy-" + System.nanoTime());
            try {
                legacy.put("chart.starLabels", "false");
                legacy.flush();
                ChartOptions upgraded =
                        ChartOptionsStore.forNode(legacy).load();
                require(!upgraded.starNames() && !upgraded.bayerLetters()
                                && !upgraded.flamsteedNumbers()
                                && upgraded.equatorialGrid(),
                        "an older store upgrades as documented: one"
                                + " star-text choice governs all three"
                                + " identifier layers, and a key that"
                                + " release never wrote takes its"
                                + " default");
            } finally {
                legacy.removeNode();
            }
        } finally {
            scratch.removeNode();
        }
        System.out.println("persistence and upgrade journey OK"
                + " (through the bundled preference backend)");

        // Point and identify, inside the packaged application
        // (Sprint 19): the reader asks the page what a mark is, and
        // is answered from the catalogue the image carries - no
        // network, no query beyond the page already assembled.
        navigation.reset();
        ChartScene homeScene = Atlas.assembler()
                .assemble(navigation.state(), 900, 700);
        ChartHitTest hitTest = new ChartHitTest(renderer);
        ChartRenderer.DrawnMark aMark = renderer
                .drawnMarks(homeScene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800
                        && mark.centre().y() > 100
                        && mark.centre().y() < 600)
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "the default page draws no reachable star"));
        ChartHitTest.Hit hit = hitTest.at(homeScene, ChartOptions.DEFAULTS,
                aMark.centre().x(), aMark.centre().y());
        require(hit != null && !hit.isEmptySky(),
                "pointing at a drawn star finds it");
        SelectionModel selectionModel = new SelectionModel();
        java.util.List<String> heard = new java.util.ArrayList<>();
        selectionModel.onChange(change -> heard.add(
                change.selection() instanceof Selection.Object object
                        ? object.catalogueId() : "none"));
        selectionModel.select(hit.candidates().get(0));
        require(SelectionDetails.star(homeScene, selectionModel.selection())
                        .isPresent(),
                "and the page can say what it is without a catalogue"
                        + " query");
        require(heard.size() == 2 && heard.get(1).equals(
                        aMark.star().id()),
                "with the shared selection telling its consumers"
                        + " exactly once: " + heard);
        ChartHitTest.Hit chrome = hitTest.at(homeScene,
                ChartOptions.DEFAULTS, -5, 300);
        require(chrome == null, "and the surround is not sky");
        System.out.println("point-and-identify OK (" + aMark.star().id()
                + " identified from the packaged catalogue)");

        // Chart furniture, inside the packaged application (Sprint
        // 20): both options draw through the packaged renderer, and
        // the key's circles come from the same size policy the
        // packaged star pass uses.
        navigation.reset();
        ChartScene furnished = Atlas.assembler()
                .assemble(navigation.state(), 900, 700);
        ChartOptions released = ChartOptions.DEFAULTS;
        require(released.titleBlock() && !released.magnitudeKey(),
                "the packaged release ships the title block on and the"
                        + " magnitude key off");
        int withTitle = ink(page(renderer, navigation, released));
        ChartOptions bareChart = new ChartOptions(released.deepSkyObjects(),
                released.deepSkyLabels(), released.constellationFigures(),
                released.constellationBoundaries(),
                released.constellationNames(), released.starNames(),
                released.bayerLetters(), released.flamsteedNumbers(),
                released.equatorialGrid(), false, false);
        require(ink(page(renderer, navigation, bareChart)) < withTitle,
                "switching the title block off removes ink");
        ChartOptions keyed = new ChartOptions(released.deepSkyObjects(),
                released.deepSkyLabels(), released.constellationFigures(),
                released.constellationBoundaries(),
                released.constellationNames(), released.starNames(),
                released.bayerLetters(), released.flamsteedNumbers(),
                released.equatorialGrid(), true, true);
        require(ink(page(renderer, navigation, keyed)) > withTitle,
                "and switching the magnitude key on adds it");
        double[] keySamples = ChartRenderer.magnitudeKeySamples(
                furnished.limitingMagnitude());
        require(keySamples.length == 3
                        && keySamples[2] == furnished.limitingMagnitude(),
                "the packaged key names this page's own limit: "
                        + java.util.Arrays.toString(keySamples));
        System.out.println("chart furniture OK (title block and"
                + " magnitude key drawn by the packaged renderer,"
                + " key samples " + java.util.Arrays.toString(keySamples)
                + ")");

        // The five deep-sky families, inside the packaged image: each
        // one hides its own marks and nobody else's, the master
        // governs all five, and the family flags round-trip through
        // the packaged preference store (Sprint 21, issue #185).
        // No one page draws all five, so two do: Sagittarius carries
        // the clusters, nebulae and planetaries, Orion the galaxies.
        // Each family is then hidden on the page that actually draws
        // it, because hiding what was not there proves nothing.
        java.util.List<ChartScene> pages = java.util.List.of(
                Atlas.assembler().assemble(new ChartViewState(
                        new SkyPosition(271.0, -24.0), 18.0, 8.0, null,
                        null), 900, 700),
                Atlas.assembler().assemble(new ChartViewState(
                        new SkyPosition(83.8, 0.0), 18.0, 8.0, null, null),
                        900, 700));
        java.util.List<String> hidden = new java.util.ArrayList<>();
        for (juranometria.render.SymbolFamily family
                : juranometria.render.SymbolFamily.values()) {
            ChartScene crowded = pages.get(0);
            for (ChartScene page : pages) {
                if (marksByFamily(renderer, page, released)
                        .getOrDefault(family.name(), 0)
                        > marksByFamily(renderer, crowded, released)
                                .getOrDefault(family.name(), 0)) {
                    crowded = page;
                }
            }
            java.util.Map<String, Integer> withAll =
                    marksByFamily(renderer, crowded, released);
            java.util.Map<String, Integer> without = marksByFamily(renderer,
                    crowded, released.withFamily(family, false));
            // The searched target is exempt and stays drawn, so what
            // must go is every OTHER mark of that family. The home
            // page names M 31, which is exactly the exemption at work
            // rather than a leak.
            int survivors = without.getOrDefault(family.name(), 0);
            int exempt = targetOf(renderer, crowded,
                    released.withFamily(family, false), family);
            require(survivors == exempt,
                    family + " switched off left " + survivors
                            + " marks where only " + exempt
                            + " target was exempt");
            for (juranometria.render.SymbolFamily other
                    : juranometria.render.SymbolFamily.values()) {
                if (other != family) {
                    require(withAll.getOrDefault(other.name(), 0)
                                    == without.getOrDefault(other.name(), 0),
                            "switching off " + family + " changed "
                                    + other);
                }
            }
            require(withAll.getOrDefault(family.name(), 0) > 0,
                    "the page must draw " + family + " before hiding"
                            + " it can prove anything");
            hidden.add(family.label() + " "
                    + withAll.getOrDefault(family.name(), 0) + "\u2192"
                    + survivors);
        }
        Preferences familyNode = Preferences.userRoot()
                .node("juranometria-acceptance-families");
        try {
            ChartOptionsStore familyStore =
                    ChartOptionsStore.forNode(familyNode);
            ChartOptions chosen = released.withFamily(
                    juranometria.render.SymbolFamily.NEBULAE, false);
            familyStore.save(chosen);
            require(ChartOptionsStore.forNode(familyNode).load()
                            .equals(chosen),
                    "a family choice round-trips through the packaged"
                            + " preference store");
            // A store from before the families existed upgrades into
            // the chart it already had.
            familyNode.remove("chart.galaxies");
            familyNode.remove("chart.openClusters");
            familyNode.remove("chart.globularClusters");
            familyNode.remove("chart.nebulae");
            familyNode.remove("chart.planetaryNebulae");
            ChartOptions upgraded =
                    ChartOptionsStore.forNode(familyNode).load();
            for (juranometria.render.SymbolFamily family
                    : juranometria.render.SymbolFamily.values()) {
                require(upgraded.family(family),
                        "a 1.2.0 store upgrades with " + family + " on");
            }
        } finally {
            familyNode.removeNode();
        }
        // A hidden family really changes the page the packaged
        // renderer draws, not merely a list it publishes.
        juranometria.render.SymbolFamily hiddenFamily =
                juranometria.render.SymbolFamily.NEBULAE;
        ChartScene nebulous = pages.get(0);
        byte[] withNebulae = bytes(renderer.renderToImage(nebulous,
                released));
        byte[] withoutNebulae = bytes(renderer.renderToImage(nebulous,
                released.withFamily(hiddenFamily, false)));
        require(!java.util.Arrays.equals(withNebulae, withoutNebulae),
                "hiding a family changes the packaged page's pixels");
        require(ink(renderer.renderToImage(nebulous,
                        released.withFamily(hiddenFamily, false)))
                        < ink(renderer.renderToImage(nebulous, released)),
                "and takes ink away rather than adding it");

        // The searched symbol-less type, through the packaged search:
        // found, recentred, titled, and given no invented mark.
        juranometria.chart.DeepSkyObject symbolless =
                nebulous.deepSkyObjects().stream()
                .filter(dso -> !ChartRenderer.hasSymbol(dso))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "no symbol-less object on the study page"));
        java.util.List<juranometria.search.SearchResult> found =
                Atlas.search().search(symbolless.id());
        require(found.size() == 1,
                "the packaged search finds a symbol-less object"
                        + " unambiguously: " + symbolless.id() + " gave "
                        + found.size() + " results");
        // The result is driven into navigation through the production
        // policy the search field itself uses, rather than the
        // expected page being rebuilt here from the object already in
        // hand - which would have passed even if choosing a result
        // recentred on the wrong place or lost the identity between
        // search and navigation (sprint review, P1).
        juranometria.ui.ChartViewController searched =
                new juranometria.ui.ChartViewController(
                        Atlas.assembler()::fits);
        require(juranometria.ui.SearchNavigation.apply(found.get(0),
                        Atlas.assembler(), searched, null)
                        != juranometria.ui.SearchNavigation.Outcome.NO_FIT,
                "and the packaged coverage reaches it");
        ChartViewState reached = searched.state();
        require(Math.abs(reached.centre().raDegrees()
                        - symbolless.position().raDegrees()) < 1e-6
                        && Math.abs(reached.centre().decDegrees()
                                - symbolless.position().decDegrees()) < 1e-6,
                "the search recentred on it: " + reached.centre());
        require(symbolless.id().equals(reached.targetIdentity()),
                "carrying its identity from search into navigation: "
                        + reached.targetIdentity());
        require(found.get(0).regionTitle().equals(reached.targetLabel()),
                "and titling the page by the name the search gave: "
                        + reached.targetLabel());
        ChartScene centred = Atlas.assembler().assemble(reached, 900, 700);
        require(renderer.drawnMarks(centred, released).stream()
                        .noneMatch(mark -> mark.deepSky() != null
                                && mark.deepSky().id()
                                        .equals(symbolless.id())),
                "and the packaged renderer invents no mark for it");

        // And the exemption, on the page that names a target: the
        // home page's own galaxy stays drawn with galaxies hidden.
        int exemptAtHome = targetOf(renderer, furnished,
                released.withFamily(
                        juranometria.render.SymbolFamily.GALAXIES, false),
                juranometria.render.SymbolFamily.GALAXIES);
        require(exemptAtHome == 1,
                "the searched target survives its family being hidden");
        System.out.println("deep-sky families OK (" + String.join(", ",
                hidden) + ", each leaving the others untouched; a"
                + " hidden family changes the drawn page; the named"
                + " target still drawn with its own family hidden;"
                + " symbol-less " + symbolless.id() + " found,"
                + " centred and titled with no invented mark)");

        // Home: the journey ends on the page it started from,
        // rendered identically.
        navigation.reset();
        require(java.util.Arrays.equals(
                        bytes(page(renderer, navigation,
                                ChartOptions.DEFAULTS)),
                        bytes(homePage)),
                "Home returns to the reviewed default page, pixel for"
                        + " pixel");
        System.out.println("reader journey OK (ends on the reviewed"
                + " default page)");
    }

    /** Whether the page's named target is a mark of this family. */
    private static int targetOf(ChartRenderer renderer, ChartScene scene,
                                ChartOptions options,
                                juranometria.render.SymbolFamily family) {
        if (scene.targetIdentity() == null) {
            return 0;
        }
        for (ChartRenderer.DrawnMark mark
                : renderer.drawnMarks(scene, options)) {
            if (mark.deepSky() != null
                    && scene.targetIdentity().equals(mark.deepSky().id())
                    && juranometria.render.SymbolFamily.of(mark.deepSky())
                            == family) {
                return 1;
            }
        }
        return 0;
    }

    /** The packaged renderer's own marks, counted by family. */
    private static java.util.Map<String, Integer> marksByFamily(
            ChartRenderer renderer, ChartScene scene,
            ChartOptions options) {
        java.util.Map<String, Integer> counts =
                new java.util.HashMap<>();
        for (ChartRenderer.DrawnMark mark
                : renderer.drawnMarks(scene, options)) {
            if (mark.deepSky() == null) {
                continue;
            }
            juranometria.render.SymbolFamily family =
                    juranometria.render.SymbolFamily.of(mark.deepSky());
            require(family != null,
                    "a drawn mark belongs to a family: "
                            + mark.deepSky().id());
            counts.merge(family.name(), 1, Integer::sum);
        }
        return counts;
    }

    /** A page rendered exactly as the window would draw it. */
    private static BufferedImage page(ChartRenderer renderer,
                                      ChartViewController navigation,
                                      ChartOptions options) {
        return renderer.renderToImage(
                Atlas.assembler().assemble(navigation.state(), 900, 700),
                options);
    }

    /** Inked pixels: how much of the page carries chart ink. */
    private static int ink(BufferedImage image) {
        int inked = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xff) < 140) {
                    inked++;
                }
            }
        }
        return inked;
    }

    private static byte[] bytes(BufferedImage image) throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            System.err.println("PACKAGED ACCEPTANCE FAILED: " + what);
            System.exit(1);
        }
    }
}
