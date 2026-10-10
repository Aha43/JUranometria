package juranometria.app;

import org.junit.jupiter.api.Test;

import juranometria.solar.JovianSystemService;
import juranometria.tool.JupiterOnTheChartStudyMain;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The packaged Jovian sheet journey restates its moment and field because
 * packaged journeys do not reach into the study tools (#486); this holds
 * the restatement to the study's own moment and measured field.
 */
class PackagedJovianSheetTest {

    @Test
    void thePackagedSheetIsTheStudysMeasuredSheet() {
        assertEquals(JupiterOnTheChartStudyMain.SHEET_MOMENT,
                PackagedAcceptanceMain.JOVIAN_SHEET_MOMENT);
        assertEquals(JupiterOnTheChartStudyMain.sheetField(JovianSystemService.load()),
                PackagedAcceptanceMain.JOVIAN_SHEET_FIELD,
                "the field the study measured, not one assumed");
    }
}
