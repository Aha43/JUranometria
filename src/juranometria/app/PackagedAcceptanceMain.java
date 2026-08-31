package juranometria.app;

import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

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

        System.out.println("PACKAGED ACCEPTANCE OK");
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            System.err.println("PACKAGED ACCEPTANCE FAILED: " + what);
            System.exit(1);
        }
    }
}
