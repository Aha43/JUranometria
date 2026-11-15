package juranometria.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.SyntheticShowers;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.ui.ChartModuleHost;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sky.Observer;
import juranometria.tool.WholeSkyComposition;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A selected meteor radiant in the two outputs a reader takes away: the
 * view report and the exported sheet (#718, closing a gap the 6.6.0
 * handover found).
 *
 * <p>A radiant rides the bodies' route to both, and that route is held
 * for the Sun, the Moon and the planets elsewhere. But a radiant is
 * deliberately not a body - it has no disc, it exists only inside an
 * activity, and it is drawn for one selected shower - so it gets one
 * journey of its own through the application's own report and its own
 * export path: not selected, selected and active, and selected with the
 * chart time outside the activity.
 *
 * <p>The shower is invented - it only borrows a code the page has a name
 * for - so the route is proved without any listed value.
 */
class RadiantInReportAndSheetTest {

    private static final String ID = "meteor-shower.PER";
    private static final MeteorShowers SERVICE = SyntheticShowers.serviceNamed("PER");
    private static final SkyPosition RADIANT = MeteorShowers.radiantAt(
            SERVICE.shower("PER").orElseThrow(), 100.0).at();

    private record Live(ChartComponent chart, ChartViewController navigation,
                        ChartModuleHost host, MeridianModule meridian,
                        MeteorShowerModule module, SelectionModel selection,
                        SkyLanguageSession language) {

        String report() {
            return ViewReport.format(ViewReport.snapshot(chart, language, meridian, null,
                    selection, host.workingSelection(),
                    // The machine's facts are not this test's subject.
                    fact -> "unstated"));
        }
    }

    @Test
    void aSelectedRadiantIsReportedAndTravelsToTheSheet(@TempDir Path folder)
            throws Exception {
        SwingSession.scratchPreferences("radiant-report-sheet",
                node -> journey(folder, node));
    }

    private void journey(Path folder, java.util.prefs.Preferences node) throws Exception {
        Observer first = new Observer(59.91, 10.75, SyntheticShowers.at(100.0));
        InterfaceText english = InterfaceText.forLanguage("en");
        Live[] made = new Live[1];
        SwingUtilities.invokeAndWait(() -> {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            store.save(SkyLanguageChoice.read(Map.of(), Atlas.languages())
                    .withInterface("en"));
            SkyLanguageSession language = SkyLanguageSession.begin(store, Atlas.languages());
            ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(english));
            chart.setSize(770, 523);
            ChartViewController navigation = new ChartViewController();
            navigation.onChange(chart::setViewState);
            SelectionModel selection = new SelectionModel();
            ChartModuleHost host = new ChartModuleHost(chart, selection, request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(first));
            meridian.showing(false, false, false);
            chart.setContext(meridian.context());
            MeteorShowerModule module = host.attach(new MeteorShowerModule(
                    meridian::observer, () -> SERVICE, () -> false));
            navigation.recenter(RADIANT, 60.0);
            WholeSkyComposition.paintNow(chart, 770, 523);
            made[0] = new Live(chart, navigation, host, meridian, module, selection, language);
        });
        Live live = made[0];
        ChartOptionsController options = new ChartOptionsController(
                ChartOptionsStore.forNode(node));
        ExportSheet.Request request = new ExportSheet.Request(SheetFormat.SVG,
                PaperSize.A4, 300, false);

        // No shower selected: neither output knows of a radiant.
        String[] report = new String[1];
        SwingUtilities.invokeAndWait(() -> report[0] = live.report());
        assertTrue(report[0].contains("selection: none"), report[0]);
        assertFalse(report[0].contains("meteor-shower"), "nothing of a shower in the report");
        String unselected = sheet(folder, "unselected", request, live, options, english);
        assertFalse(unselected.contains(">Perseids<"), "and no radiant on the sheet");

        // Selected and active: the report names the occurrence whole, the sheet carries
        // the mark's name.
        Selection.Occurrence[] selected = new Selection.Occurrence[1];
        SwingUtilities.invokeAndWait(() -> {
            live.module().select("PER");
            WholeSkyComposition.paintNow(live.chart(), 770, 523);
            live.host().workingSelection().replaceWith(List.of(ID), ID);
            selected[0] = assertInstanceOf(Selection.Occurrence.class,
                    live.selection().selection());
            report[0] = live.report();
        });
        assertEquals(ID, selected[0].identity());
        assertEquals(first.instant(), selected[0].instant());
        assertTrue(selected[0].position().separationDegrees(RADIANT) < 0.2,
                "the radiant for that chart time");
        String line = "selection: occurrence " + ID + " at " + first.instant()
                + " for latitude 59.91; longitude 10.75; RA ";
        assertTrue(report[0].contains(line), report[0]);
        assertTrue(report[0].contains("selection: " + ViewReport.selection(selected[0])),
                "identity, instant, observer and place, exactly as selected");
        String active = sheet(folder, "active", request, live, options, english);
        assertTrue(active.contains(">Perseids<"), "the sheet names the radiant the screen shows");
        // And the mark itself: six rays and one name more than the sheet without it,
        // each ray a stroke of its own, as the screen draws them (R-13).
        assertEquals(6, count(active, "<path") - count(unselected, "<path"),
                "six rays reach the paper");
        assertEquals(1, count(active, "<text") - count(unselected, "<text"),
                "and one name");

        // The chart time leaves the activity: the shower stays selected, the sheet
        // draws no radiant, and the report keeps the moment it was last drawn for.
        SwingUtilities.invokeAndWait(() -> {
            live.meridian().observer(first.at(SyntheticShowers.at(200.0)));
            WholeSkyComposition.paintNow(live.chart(), 770, 523);
            report[0] = live.report();
        });
        assertEquals("PER", live.module().selected());
        assertTrue(report[0].contains(line), "never a new moment for a radiant not drawn: "
                + report[0]);
        String inactive = sheet(folder, "inactive", request, live, options, english);
        assertFalse(inactive.contains(">Perseids<"),
                "outside the activity the sheet draws no radiant");
        assertEquals(count(unselected, "<path"), count(inactive, "<path"), "and no rays");
    }

    private static int count(String text, String piece) {
        return text.split(java.util.regex.Pattern.quote(piece), -1).length - 1;
    }

    private static String sheet(Path folder, String name, ExportSheet.Request request,
                                Live live, ChartOptionsController options,
                                InterfaceText english) throws Exception {
        // On the event thread, where the application exports: the chart's time
        // and place are held for the length of one sheet (#612).
        SwingUtilities.invokeAndWait(() -> ExportSheetSession.exportTo(
                folder.resolve(name).toFile(), request, live.navigation(), live.chart(),
                options, live.host().workingSelection(), file -> true, english));
        return Files.readString(folder.resolve(name + ".svg"));
    }
}
