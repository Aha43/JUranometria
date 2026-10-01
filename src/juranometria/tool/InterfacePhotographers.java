package juranometria.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The interface photographers: every generator that owns part of
 * {@code docs/studies/interface-language}, and what each writes
 * (issue #428).
 *
 * <p>Moved here from the interface evidence gate, which holds this
 * list and the committed directory to describing the same set, so
 * that the CI classifier reads the very same registry: a new
 * photograph enters the interaction route by being registered, and
 * there is no second list to forget.
 */
public final class InterfacePhotographers {

    /** Where every photographer writes. */
    public static final String DIRECTORY = "docs/studies/interface-language/";

    private InterfacePhotographers() {
    }

    /**
     * One photographer: what it writes, and what it holds still.
     *
     * <p>The kind is declared and never defaulted. Chart Options was
     * photographed at its packed 394 px and Place and Time at 326 -
     * both widths no reader meets - because the coordinator assumed
     * every window was a packed one, and an undeclared default is
     * how that assumption gets made again.
     */
    public record Photographer(String companion,
                               SheetCapture.Kind kind) {
    }

    /**
     * Every generator that owns part of this directory.
     *
     * <p>One registry, in one place. The inventory checks below read
     * this and the committed directory and require them to describe
     * the same set - in both directions, because a generator nobody
     * registered and an artifact nobody generates are the same
     * defect seen from two ends.
     */
    private static final Map<String, Photographer> GENERATORS =
            new LinkedHashMap<>();

    static {
        // Dialogs that production only packs.
        GENERATORS.put("AboutSheetMain",
                new Photographer("about-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("ExportSheetDialogSheetMain",
                new Photographer("export-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("SwingChromeSheetMain",
                new Photographer("swing-chrome-strings.md",
                        SheetCapture.Kind.PACKED));
        GENERATORS.put("SunTableSheetMain",
                new Photographer("suntable-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("MoonTableSheetMain",
                new Photographer("moontable-strings.md", SheetCapture.Kind.PACKED));
        // Components inside a packed study frame.
        GENERATORS.put("ChartKeyboardSheetMain",
                new Photographer("chartkeyboard-strings.md",
                        SheetCapture.Kind.PACKED));
        GENERATORS.put("InspectorSheetMain",
                new Photographer("inspector-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("MenuSheetMain",
                new Photographer("menu-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("OnThisPageSheetMain",
                new Photographer("onthispage-strings.md", SheetCapture.Kind.PACKED));
        GENERATORS.put("ToolbarSheetMain",
                new Photographer("toolbar-strings.md", SheetCapture.Kind.PACKED));
        // Dialogs whose size the application states.
        GENERATORS.put("ChartOptionsSheetMain",
                new Photographer("chartoptions-strings.md",
                        SheetCapture.Kind.APPLICATION_SIZED));
        GENERATORS.put("PlaceAndTimeSheetMain",
                new Photographer("placeandtime-strings.md",
                        SheetCapture.Kind.APPLICATION_SIZED));
        // No window to size.
        GENERATORS.put("SettingsSheetMain",
                new Photographer("settings-strings.md",
                        SheetCapture.Kind.FIXED_CANVAS));
        GENERATORS.put("PageLanguageSheetMain",
                new Photographer("page-language-strings.md",
                        SheetCapture.Kind.FIXED_CANVAS));
    }

    /** Every photographer, by simple class name, in registry order. */
    public static final Map<String, Photographer> ALL =
            Collections.unmodifiableMap(GENERATORS);
}
