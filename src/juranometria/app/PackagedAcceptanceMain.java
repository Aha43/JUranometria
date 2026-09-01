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
        store.save(flipped);
        Preferences.userRoot().node("juranometria").flush();
        require(ChartOptionsStore.user().load().equals(flipped),
                "a fresh store reloads the changed preference");
        store.save(before);
        Preferences.userRoot().node("juranometria").flush();
        require(ChartOptionsStore.user().load().equals(before),
                "the original preference is restored");
        System.out.println("preference change-and-reload OK"
                + " (equatorialGrid flipped and restored through the"
                + " bundled runtime's preference backend)");

        readerJourney();

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
        require(!found.isEmpty()
                        && found.get(0).identity().equals(symbolless.id()),
                "the packaged search finds a symbol-less object: "
                        + symbolless.id());
        ChartScene centred = Atlas.assembler().assemble(
                new ChartViewState(symbolless.position(), 8.0, 8.0,
                        symbolless.id(), symbolless.id()), 900, 700);
        require(renderer.drawnMarks(centred, released).stream()
                        .noneMatch(mark -> mark.deepSky() != null
                                && mark.deepSky().id()
                                        .equals(symbolless.id())),
                "and the packaged renderer invents no mark for it");
        require(symbolless.id().equals(centred.targetIdentity()),
                "though the page is titled by it honestly");

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
