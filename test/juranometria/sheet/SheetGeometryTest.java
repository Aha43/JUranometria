package juranometria.sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sheet against the screen, through the sky (Sprint 29,
 * issue #285).
 *
 * <p>The two are not the same shape and never will be: a window is
 * whatever a reader dragged it to, and a chart rectangle is 271.6 mm
 * of paper. Comparing them pixel for pixel would be comparing the
 * wrong thing.
 *
 * <p>What must agree is the astronomy. So this asks the chart on
 * screen what it drew and where, converts each of those marks to the
 * <em>sky position</em> it stands for, projects that onto the paper
 * the way the sheet's own viewport does, and then goes looking in the
 * <strong>written file</strong> - parsed back out of the SVG text,
 * not read from the recorder - for a mark at that place.
 *
 * <p>Across the pages the atlas finds hardest: the widest field, the
 * right-ascension seam, a pole, a dense field, and a chart whose
 * screen is black with its modules showing.
 */
class SheetGeometryTest {

    private static final int SCREEN_WIDE = 900;
    private static final int SCREEN_HIGH = 700;

    /** The pages the acceptance asks for, by name. */
    private static List<Object[]> hardPages() {
        return List.of(
                new Object[] {"the widest field",
                        new ChartViewState(new SkyPosition(83.0, 0.0),
                                42.0, 6.0)},
                new Object[] {"the right-ascension seam",
                        new ChartViewState(new SkyPosition(359.9, 0.0),
                                42.0, 6.0)},
                new Object[] {"a pole",
                        new ChartViewState(new SkyPosition(0.0, 89.0),
                                24.0, 6.0)},
                new Object[] {"a dense field",
                        new ChartViewState(new SkyPosition(270.0, -25.0),
                                18.0, 8.0)},
                new Object[] {"a released page",
                        ChartViewState.DEFAULT});
    }

    @Test
    void whatTheScreenDrewIsWhereTheSkySaysItShouldBeOnPaper() {
        for (Object[] page : hardPages()) {
            String what = (String) page[0];
            ChartViewState state = (ChartViewState) page[1];
            check(what, state, ChartOptions.DEFAULTS,
                    ChartRenderer.ReferenceLayer.NONE);
        }
    }

    @Test
    void aBlackScreenCarryingItsModulesLandsOnPaperJustTheSame() {
        // Both of the things that legitimately differ, at once: the
        // screen is black and carrying module ink, and the sheet is
        // white paper carrying the same astronomy.
        ChartViewState equinox = new ChartViewState(
                new SkyPosition(0.0, 0.0), 42.0, 6.0);
        check("a black screen with modules showing", equinox,
                ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY),
                ChartSheetTest.modules());
    }

    private void check(String what, ChartViewState state,
                       ChartOptions options,
                       ChartRenderer.ReferenceLayer reference) {
        // What the screen drew, at a window's shape.
        ChartScene onScreen = Atlas.assembler().assemble(state,
                SCREEN_WIDE, SCREEN_HIGH);
        List<ChartRenderer.DrawnMark> screenMarks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(onScreen, options);

        // The same chart as a sheet, written out and read back.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, state, options, reference,
                PaperSize.A4);
        List<double[]> inFile = pathCentres(
                SvgSheetWriter.write(sheet, SvgSheetWriter.Text.EDITABLE));
        assertTrue(inFile.size() > 100,
                what + ": the file carries a chart's worth of ink: "
                        + inFile.size() + " paths");

        // Every screen mark, put where the sky says it goes on paper.
        GnomonicProjection projection = new GnomonicProjection(
                sheet.scene().viewport().centre());
        ViewportMapping onPaper = new ViewportMapping(
                sheet.scene().viewport());

        // Every mark the paper render decided on has to be in the
        // file, at the place the sky puts it.
        List<ChartRenderer.DrawnMark> paperMarks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(sheet.scene(), sheet.options());
        int owed = 0;
        List<String> missing = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : paperMarks) {
            var at = projection.project(skyOf(mark)).map(onPaper::toPixel);
            if (at.isEmpty() || offPaper(at.get(), sheet)) {
                continue;
            }
            owed++;
            double x = at.get().x();
            double y = at.get().y();
            if (inFile.stream().noneMatch(centre ->
                    Math.hypot(centre[0] - x, centre[1] - y) < 1.0)) {
                missing.add(String.format("%s at %.1f,%.1f",
                        idOf(mark), x, y));
            }
        }
        assertTrue(owed > 20,
                what + ": the sheet owes a real number of marks -"
                        + " a near-polar page is the sparsest of"
                        + " them, at 49: " + owed);
        assertEquals(List.of(), missing.stream().limit(5).toList(),
                what + ": " + missing.size() + " of " + owed
                        + " marks the paper render decided on are"
                        + " missing from the written sheet at the"
                        + " place the sky puts them");

        // And the screen and the paper agree about the sky. They do
        // not agree about everything, and should not: a page drawn
        // into 770 units carries slightly less detail than one drawn
        // into 900, exactly as a smaller window does, and the
        // renderer's own detail policy - not the sheet - decides
        // that. So what is required is that every mark the screen
        // drew either reached the paper too, or was declined by
        // production's own render of the paper.
        List<String> unexplained = new ArrayList<>();
        int declined = 0;
        for (ChartRenderer.DrawnMark mark : screenMarks) {
            var at = projection.project(skyOf(mark)).map(onPaper::toPixel);
            if (at.isEmpty() || offPaper(at.get(), sheet)) {
                continue;
            }
            String id = idOf(mark);
            boolean onPaperToo = paperMarks.stream()
                    .anyMatch(other -> idOf(other).equals(id));
            if (onPaperToo) {
                continue;
            }
            declined++;
            double x = at.get().x();
            double y = at.get().y();
            if (inFile.stream().anyMatch(centre ->
                    Math.hypot(centre[0] - x, centre[1] - y) < 1.0)) {
                unexplained.add(id + " is on the sheet but not in the"
                        + " paper render");
            }
        }
        assertEquals(List.of(), unexplained,
                what + ": the sheet draws nothing production did not");
        assertTrue(declined <= screenMarks.size() / 10,
                what + ": the two extents agree about nearly"
                        + " everything - " + declined + " of "
                        + screenMarks.size() + " screen marks fall"
                        + " below the paper's detail threshold, which"
                        + " is the renderer's decision and not the"
                        + " sheet's");
    }

    private static SkyPosition skyOf(ChartRenderer.DrawnMark mark) {
        return mark.star() != null ? mark.star().position()
                : mark.deepSky().position();
    }

    private static String idOf(ChartRenderer.DrawnMark mark) {
        return mark.star() != null ? mark.star().id()
                : mark.deepSky().id();
    }

    private static boolean offPaper(juranometria.project.PixelPoint at,
                                    SheetRecording sheet) {
        return at.x() < 2 || at.y() < 2
                || at.x() > sheet.paper().chartWideUnits() - 2
                || at.y() > sheet.paper().chartHighUnits() - 2;
    }

    /**
     * The centre of every drawn path in the file, in chart units -
     * parsed out of the SVG text, so nothing here can agree with the
     * recorder by sharing it.
     */
    private static List<double[]> pathCentres(String svg) {
        String ink = svg.substring(svg.indexOf("<g id=\"chart\""));
        List<double[]> centres = new ArrayList<>();
        Matcher each = Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(ink);
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            Matcher number = Pattern.compile("-?\\d+(?:\\.\\d+)?")
                    .matcher(each.group(1));
            while (number.find()) {
                numbers.add(Double.parseDouble(number.group()));
            }
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (int i = 0; i + 1 < numbers.size(); i += 2) {
                minX = Math.min(minX, numbers.get(i));
                maxX = Math.max(maxX, numbers.get(i));
                minY = Math.min(minY, numbers.get(i + 1));
                maxY = Math.max(maxY, numbers.get(i + 1));
            }
            centres.add(new double[] {(minX + maxX) / 2.0,
                    (minY + maxY) / 2.0});
        }
        return centres;
    }
}
