package juranometria.sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.AppInfo;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SVG a reader is handed (Sprint 29, issue #285).
 *
 * <p>The file has to survive leaving this machine. It goes to a club
 * member with Inkscape and no idea what JUranometria is, on a network
 * that may not exist, to be printed at a size someone else's printer
 * decides. So what is asserted here is what the file says about
 * itself and what it refuses to depend on - read back out of the
 * document, never assumed from the code that wrote it.
 */
class SvgSheetWriterTest {

    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0);

    private static String svg(ChartViewState state, PaperSize paper,
                              SvgSheetWriter.Text text) {
        return SvgSheetWriter.write(ChartSheet.record(
                Atlas.assembler()::assemble, state, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, paper), text);
    }

    @Test
    void theFileStatesTheSizeOfTheActualPaper() {
        String svg = svg(ORION, PaperSize.A4, SvgSheetWriter.Text.EDITABLE);

        assertTrue(svg.contains("width=\"841.89pt\""),
                "the width is A4's own, in points a viewer can honour");
        assertTrue(svg.contains("height=\"595.28pt\""), "and the height");
        assertTrue(svg.contains("viewBox=\"0 0 841.89 595.28\""),
                "with a view box in the same units, so a viewer that"
                        + " reads only the box still gets the"
                        + " proportions");

        String letter = svg(ORION, PaperSize.LETTER,
                SvgSheetWriter.Text.EDITABLE);
        assertTrue(letter.contains("width=\"792.00pt\""),
                "and Letter states Letter's");
        assertTrue(letter.contains("height=\"612.00pt\""), "in both"
                + " directions");
        assertNotEquals(svg, letter,
                "the two papers are two documents, not one relabelled");
    }

    @Test
    void theFileDependsOnNothingItCannotSee() {
        String svg = svg(ORION, PaperSize.A4, SvgSheetWriter.Text.EDITABLE);

        for (String fetched : List.of("<image", "<script", "<use",
                "xlink:href", "@import", "<style", "url(http",
                "src=", "font-face")) {
            assertFalse(svg.contains(fetched),
                    "a sheet fetches nothing: found " + fetched);
        }

        // The only URL in the file is the one saying where the
        // program came from, and it sits in the metadata where it is
        // read by people rather than by a parser.
        List<String> urls = new ArrayList<>();
        Matcher each = Pattern.compile("https?://[^\\s\"<]+").matcher(svg);
        while (each.find()) {
            urls.add(each.group());
        }
        assertEquals(List.of("http://www.w3.org/2000/svg",
                        AppInfo.REPO_URL), urls,
                "the only two URLs are SVG's namespace - an XML name"
                        + " rather than a place anything is fetched"
                        + " from - and the project's own address");
        assertTrue(svg.substring(svg.indexOf("<metadata>"),
                        svg.indexOf("</metadata>")).contains(AppInfo.REPO_URL),
                "and it is in the metadata, not in a reference"
                        + " anything would follow");
    }

    @Test
    void theFileSaysWhatChartItIsAndNothingAboutThisMachine() {
        String svg = svg(ORION, PaperSize.A4, SvgSheetWriter.Text.EDITABLE);

        String title = between(svg, "<title>", "</title>");
        String desc = between(svg, "<desc>", "</desc>");
        String about = between(svg, "<metadata>", "</metadata>");
        assertFalse(title.isBlank(), "the sheet has a title");
        assertFalse(desc.isBlank(), "and a description");
        assertFalse(about.isBlank(), "and says what made it");

        for (String owed : List.of("83.0000", "+0.0000", "ICRS/J2000",
                "42 degrees", "gnomonic", "V 6.0", "297.0 x 210.0 mm",
                "271.6 x 184.6 mm")) {
            assertTrue(desc.contains(owed),
                    "a sheet that outlives its session says " + owed
                            + ": " + desc);
        }
        assertTrue(about.contains(AppInfo.NAME)
                        && about.contains(AppInfo.version()),
                "and which version drew it: " + about);

        // And nothing about where it was made. A reader sending a
        // chart to a mailing list is not offering their home
        // directory with it.
        for (String priv : List.of(System.getProperty("user.home"),
                System.getProperty("user.name"),
                System.getProperty("user.dir"))) {
            if (priv != null && !priv.isBlank()) {
                assertFalse(svg.contains(priv),
                        "the sheet carries no private path or name");
            }
        }
    }

    @Test
    void everyLayerIsThereUnderTheNameProductionGaveIt() {
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, ORION, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);

        for (String group : List.of("id=\"paper\"", "id=\"chart\"",
                "id=\"ink\"", "id=\"labels\"")) {
            assertTrue(svg.contains(group),
                    "ink and labels can be selected apart in an"
                            + " editor: " + group);
        }
        assertTrue(svg.contains("transform=\"translate(36.00,36.00)\""),
                "and the chart sits inside the half-inch margin");

        // Presence by production's own count, not by eye: every shape
        // the renderer drew is a path in the file, and every run of
        // text it drew is a text element with those exact
        // characters.
        assertEquals(sheet.shapeCount(),
                count(svg, "<path d=") - count(svg, "<clipPath"),
                "every shape the renderer drew reached the file");
        assertEquals(sheet.textCount(), count(svg, "<text "),
                "and every label");

        List<String> drawn = sheet.recorder().text().stream()
                .map(SheetRecorder.Text::text).sorted().toList();
        List<String> inFile = new ArrayList<>();
        Matcher each = Pattern.compile(">([^<]*)</text>").matcher(svg);
        while (each.find()) {
            inFile.add(unescape(each.group(1)));
        }
        assertEquals(drawn, inFile.stream().sorted().toList(),
                "with the words production chose, not a re-derivation"
                        + " of them");
    }

    @Test
    void aLabelIsStillAWordAReaderCanRetype() {
        // The gate chose text over outlines for one reason: the
        // reader who asked for this wanted a file he could work with.
        String svg = svg(new ChartViewState(new SkyPosition(83.0, -5.0),
                8.0, 8.0), PaperSize.A4, SvgSheetWriter.Text.EDITABLE);

        assertTrue(svg.contains("font-family=\"sans-serif\""),
                "in a family any machine has, rather than one this"
                        + " machine happens to");
        assertFalse(svg.contains("font-family=\"Helvetica")
                        || svg.contains(".ttf") || svg.contains(".otf"),
                "and no font this file would have to carry or fetch");

        // The characters survive as characters - the notation the
        // chart uses is Greek, and an export that could not hold it
        // would be silently wrong rather than loudly broken.
        assertTrue(svg.matches("(?s).*<text [^>]*>[^<]*[\\u03b1-\\u03c9].*"),
                "a Bayer letter reaches the file as a letter");
        assertTrue(svg.startsWith("<svg"), "and the document is SVG");

        // Substitution is the price of that choice, and it is a
        // bounded one: the file says the size and the position of
        // every run, so a viewer with a different sans-serif places
        // the same words in the same places at the same size.
        Matcher label = Pattern.compile(
                "<text x=\"(-?[\\d.]+)\" y=\"(-?[\\d.]+)\"[^>]*"
                        + "font-size=\"(\\d+)\"").matcher(svg);
        assertTrue(label.find(), "every run states where and how big");
        assertTrue(Integer.parseInt(label.group(3)) >= 8,
                "at a size a printer can resolve: "
                        + label.group(3) + " pt");
    }

    @Test
    void theOutlineVariantIsForAMachineWhoseFontsAreUnknown() {
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, ORION, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        String outlined = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.OUTLINES);

        assertFalse(outlined.contains("<text "),
                "no text element is left to substitute a font into");
        assertFalse(outlined.contains("font-family"),
                "and no family is named");
        assertEquals(sheet.shapeCount() + sheet.textCount(),
                count(outlined, "<path d=") - count(outlined, "<clipPath"),
                "every label became a path of its own, one for one");
        assertTrue(outlined.contains("id=\"labels\""),
                "still grouped as labels, so they can be found again");
    }

    @Test
    void thesameChartWrittenTwiceIsTheSameFile() {
        // Determinism where the stack allows it: the writer holds no
        // clock, no random and no map iteration order, so two runs
        // over the same chart are the same bytes. What is not
        // deterministic is named in the evidence contracts rather
        // than left for someone to discover.
        String once = svg(ORION, PaperSize.A4, SvgSheetWriter.Text.EDITABLE);
        String twice = svg(ORION, PaperSize.A4, SvgSheetWriter.Text.EDITABLE);
        assertEquals(once, twice, "the same chart writes the same file");
        assertEquals(once.length(), twice.length(), "to the byte");
    }

    @Test
    void theSheetCutsItsInkWhereProductionCutIt() throws Exception {
        // The defect the gate's own prototype shipped and its review
        // caught (PR #288): the recorder captures the clips
        // production had in force, and a writer that drops them lets
        // ink the chart had cut at its edge run out across the
        // margin. Nothing else here would notice.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, ORION, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);
        double chartWide = PaperSize.A4.chartWideUnits();
        double chartHigh = PaperSize.A4.chartHighUnits();

        // The clip is production's own: the chart rectangle inset by
        // the pixel the renderer insets it by. Not wider, which would
        // bleed; not narrower, which would crop.
        List<double[]> clips = new ArrayList<>();
        Matcher declared = Pattern.compile(
                        "<clipPath id=\"clip\\d+\"><path d=\"([^\"]+)\"")
                .matcher(svg);
        while (declared.find()) {
            clips.add(bounds(declared.group(1)));
        }
        assertFalse(clips.isEmpty(), "the sheet carries clips at all");
        for (double[] clip : clips) {
            assertEquals(1.0, clip[0], 0.6, "clipped from the chart's"
                    + " own left edge: " + java.util.Arrays.toString(clip));
            assertEquals(1.0, clip[1], 0.6, "and its top");
            assertEquals(chartWide - 1.0, clip[2], 0.6, "to its right");
            assertEquals(chartHigh - 1.0, clip[3], 0.6, "and its bottom");
        }

        // And it is doing work: ink whose own geometry leaves the
        // chart rectangle exists, and all of it carries a clip.
        int escaping = 0;
        int unclipped = 0;
        Matcher paths = Pattern.compile("<path d=\"([^\"]+)\"([^/]*)/>")
                .matcher(svg.substring(svg.indexOf("<g id=\"ink\"")));
        while (paths.find()) {
            double[] box = bounds(paths.group(1));
            if (box[0] >= -0.5 && box[1] >= -0.5
                    && box[2] <= chartWide + 0.5
                    && box[3] <= chartHigh + 0.5) {
                continue;
            }
            escaping++;
            if (!paths.group(2).contains("clip-path=\"url(#clip")) {
                unclipped++;
            }
        }
        assertTrue(escaping > 0,
                "some ink crosses the chart boundary, so the check"
                        + " below could have failed: " + escaping);
        assertEquals(0, unclipped,
                unclipped + " of " + escaping + " paths crossing the"
                        + " boundary are drawn unclipped, which is ink"
                        + " running into the half-inch margin");
    }

    /** The bounding box of an SVG path's own coordinates. */
    private static double[] bounds(String d) {
        List<Double> numbers = new ArrayList<>();
        Matcher each = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(d);
        while (each.find()) {
            numbers.add(Double.parseDouble(each.group()));
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
        return new double[] {minX, minY, maxX, maxY};
    }

    @Test
    void anotherReaderEntirelyCanParseIt() throws Exception {
        // Every other assertion in this class reads the file with the
        // same regard for it that wrote it. This one hands the bytes
        // to a parser that knows nothing about JUranometria - the
        // JDK's own XML reader - and asks it what is there.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, ORION, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);

        var factory = javax.xml.parsers.DocumentBuilderFactory
                .newInstance();
        factory.setNamespaceAware(true);
        // A parser that will not go looking for anything outside the
        // document, which is also the check that the document does
        // not ask it to.
        factory.setFeature("http://apache.org/xml/features/"
                + "nonvalidating/load-external-dtd", false);
        var document = factory.newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(
                        svg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        var root = document.getDocumentElement();
        assertEquals("svg", root.getLocalName(),
                "the document is an SVG to someone who has never seen"
                        + " this program");
        assertEquals("http://www.w3.org/2000/svg", root.getNamespaceURI(),
                "in SVG's own namespace");
        assertEquals("841.89pt", root.getAttribute("width"),
                "at A4's width");

        assertEquals(sheet.textCount(),
                document.getElementsByTagNameNS(
                        "http://www.w3.org/2000/svg", "text").getLength(),
                "with every label the renderer drew, as elements a"
                        + " parser can find");
        assertEquals(sheet.shapeCount() + countClipPaths(document),
                document.getElementsByTagNameNS(
                        "http://www.w3.org/2000/svg", "path").getLength(),
                "and every shape");

        // The groups an editor offers a reader, found by identity.
        for (String id : List.of("paper", "chart", "ink", "labels")) {
            assertTrue(document.getElementById(id) != null
                            || hasIdAttribute(document, id),
                    "an editor finds the " + id + " layer by name");
        }
    }

    private static int countClipPaths(org.w3c.dom.Document document) {
        return document.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg", "clipPath").getLength();
    }

    private static boolean hasIdAttribute(org.w3c.dom.Document document,
                                          String id) {
        var all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (id.equals(((org.w3c.dom.Element) all.item(i))
                    .getAttribute("id"))) {
                return true;
            }
        }
        return false;
    }

    private static String between(String text, String open, String close) {
        return text.substring(text.indexOf(open) + open.length(),
                text.indexOf(close));
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0;
                at = text.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    private static String unescape(String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
