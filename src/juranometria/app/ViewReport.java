package juranometria.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SkyFormat;
import juranometria.render.ChartOptions;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;

/**
 * The current view as plain text a reader can paste into an issue,
 * a chat or an email (#372).
 *
 * <p>Two halves, kept apart on purpose. {@link Snapshot} is an
 * immutable record of the live state, assembled from each value's
 * one production owner at the moment the reader asks; this class
 * only formats it. Formatting reads nothing else - no clock, no
 * system property, no preference - so the same state gives the same
 * bytes in any interface language and on any platform.
 *
 * <p>The field names are stable English diagnostic keys, so two
 * reports compare line by line whatever language the reader's
 * interface speaks; the values keep their ordinary notation. Every
 * field is named exactly once, and a field whose owner is absent -
 * a module that is not attached - is absent from the report rather
 * than remembered.
 */
public final class ViewReport {

    /** What the report says of a state; see {@link #format}. */
    public record Snapshot(
            String version,
            ChartViewState view,
            int chartWidthPx, int chartHeightPx,
            int pageWidthPx, int pageHeightPx, int pageOffsetYPx,
            Double displayScale,
            ChartOptions chosen, ChartOptions drawn,
            String interfaceLanguage,
            String skyLanguageChosen, String skyLanguageOnChart,
            Set<ChartStructure> emphasis,
            List<String> modules,
            PlaceAndTime placeAndTime,
            Boolean eclipticShowing,
            Selection selection,
            List<String> workingSet, String workingLead,
            String osName, String osVersion, String osArch,
            String javaVersion) {

        public Snapshot {
            emphasis = emphasis == null ? Set.of() : Set.copyOf(emphasis);
            modules = List.copyOf(modules);
            workingSet = workingSet == null ? List.of()
                    : List.copyOf(workingSet);
        }
    }

    /**
     * The Place and Time module's stated place and instant, and its
     * three switches - present only while the module is attached.
     */
    public record PlaceAndTime(Observer observer, boolean meridian,
                               boolean horizon, boolean zenith) {
    }

    private ViewReport() {
    }

    /** The report, ending with a blank {@code Comment:} section. */
    public static String format(Snapshot s) {
        List<String> lines = new ArrayList<>();
        lines.add("JUranometria view report");
        lines.add("version: " + s.version());
        ChartViewState view = s.view();
        lines.add("centre: RA " + SkyFormat.formatRa(view.centre().raDegrees())
                + "; Dec " + SkyFormat.formatDec(view.centre().decDegrees()));
        lines.add("centre-degrees: RA " + exact(view.centre().raDegrees())
                + "; Dec " + exact(view.centre().decDegrees()));
        lines.add("field-degrees: " + exact(view.fieldWidthDegrees()));
        lines.add("projection: " + token(view.projection().name()));
        lines.add("target: " + (view.targetIdentity() == null
                && view.targetLabel() == null ? "none"
                : nullToNone(view.targetLabel()) + " ("
                        + nullToNone(view.targetIdentity()) + ")"));
        lines.add("chart-pixels: " + s.chartWidthPx() + " x "
                + s.chartHeightPx());
        lines.add("page-pixels: " + s.pageWidthPx() + " x "
                + s.pageHeightPx() + " at offset " + s.pageOffsetYPx());
        lines.add("display-scale: " + (s.displayScale() == null
                ? "unavailable" : exact(s.displayScale())));
        lines.add("ground: " + s.chosen().palette().storedAs());
        lines.add("interface-language: " + s.interfaceLanguage());
        lines.add("sky-language: " + s.skyLanguageOnChart() + " (chosen: "
                + s.skyLanguageChosen() + ")");
        lines.add("limiting-magnitude: " + exact(view.limitingMagnitude()));
        lines.add("options: " + switches(s.chosen()));
        lines.add("page-overrides: " + overrides(s.chosen(), s.drawn()));
        lines.add("emphasis: " + ChartStructure.joinedTokens(s.emphasis())
                .orElse("none"));
        lines.add("modules: " + (s.modules().isEmpty() ? "none"
                : String.join(", ", s.modules())));
        if (s.placeAndTime() != null) {
            PlaceAndTime place = s.placeAndTime();
            lines.add("place-and-time: meridian " + onOff(place.meridian())
                    + ", horizon " + onOff(place.horizon()) + ", zenith "
                    + onOff(place.zenith()));
            lines.add("observer: latitude "
                    + exact(place.observer().latitudeDegrees())
                    + "; longitude-east "
                    + exact(place.observer().eastLongitudeDegrees()));
            lines.add("instant-utc: " + place.observer().instant());
        }
        if (s.eclipticShowing() != null) {
            lines.add("ecliptic: " + onOff(s.eclipticShowing()));
        }
        lines.add("selection: " + selection(s.selection()));
        lines.add("working-set: " + (s.workingSet().isEmpty() ? "none"
                : String.join(", ", s.workingSet()) + " (lead: "
                        + nullToNone(s.workingLead()) + ")"));
        lines.add("os: " + s.osName() + " " + s.osVersion() + " "
                + s.osArch());
        lines.add("java: " + s.javaVersion());
        lines.add("");
        lines.add("Comment:");
        lines.add("");
        return String.join("\n", lines);
    }

    /** The chart options by stable name, every one, in a fixed order. */
    private static String switches(ChartOptions o) {
        List<String> said = new ArrayList<>();
        for (Switch one : switchesOf(o)) {
            said.add(one.name() + " " + onOff(one.on()));
        }
        return String.join(", ", said);
    }

    /**
     * Where the page draws differently from the reader's switches -
     * a globe turns constellation boundaries off - named, so the
     * report never passes the chosen options off as the drawn ones or
     * the other way round.
     */
    private static String overrides(ChartOptions chosen,
                                     ChartOptions drawn) {
        List<Switch> asked = switchesOf(chosen);
        List<Switch> shown = switchesOf(drawn);
        List<String> said = new ArrayList<>();
        for (int i = 0; i < asked.size(); i++) {
            if (asked.get(i).on() != shown.get(i).on()) {
                said.add(shown.get(i).name() + " "
                        + onOff(shown.get(i).on()));
            }
        }
        return said.isEmpty() ? "none" : String.join(", ", said);
    }

    private record Switch(String name, boolean on) {
    }

    private static List<Switch> switchesOf(ChartOptions o) {
        return List.of(
                new Switch("deep-sky-objects", o.deepSkyObjects()),
                new Switch("deep-sky-labels", o.deepSkyLabels()),
                new Switch("galaxies", o.galaxies()),
                new Switch("open-clusters", o.openClusters()),
                new Switch("globular-clusters", o.globularClusters()),
                new Switch("nebulae", o.nebulae()),
                new Switch("planetary-nebulae", o.planetaryNebulae()),
                new Switch("constellation-figures",
                        o.constellationFigures()),
                new Switch("constellation-boundaries",
                        o.constellationBoundaries()),
                new Switch("constellation-names", o.constellationNames()),
                new Switch("star-names", o.starNames()),
                new Switch("bayer-letters", o.bayerLetters()),
                new Switch("flamsteed-numbers", o.flamsteedNumbers()),
                new Switch("equatorial-grid", o.equatorialGrid()),
                new Switch("title-block", o.titleBlock()),
                new Switch("magnitude-key", o.magnitudeKey()));
    }

    private static String selection(Selection selection) {
        if (selection instanceof Selection.Object object) {
            return token(object.kind().name()) + " "
                    + object.catalogueId();
        }
        if (selection instanceof Selection.EmptySky sky) {
            return "empty sky at RA " + exact(sky.position().raDegrees())
                    + "; Dec " + exact(sky.position().decDegrees());
        }
        return "none";
    }

    /** A double exactly as Java round-trips it, independent of locale. */
    private static String exact(double value) {
        return Double.toString(value);
    }

    private static String token(String enumName) {
        return enumName.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String onOff(boolean on) {
        return on ? "on" : "off";
    }

    private static String nullToNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
