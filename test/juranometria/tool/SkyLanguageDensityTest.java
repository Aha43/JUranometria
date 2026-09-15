package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The study measures the page a reader gets (issue #347).
 *
 * <p>Every fixture in the placement study was measured at V 8.0
 * until 2026-09-15, including all five 180-degree globes. That was
 * not the page a reader arrives at: production caps the hemisphere
 * rung at V 5.0, on the rule that zooming out never adds stars, and
 * the globe decision says of V 8.0 that the disc "is solid". The
 * study was internally consistent and asking production to draw
 * something else - which the numbers could not show and one look at
 * the rendered globe did.
 *
 * <p>So the density a study measures is now a stated premise rather
 * than a constant somebody typed. These hold it from both ends: the
 * report says which limit each fixture used, and the limit comes
 * from production rather than from a copy the study owns and could
 * drift from.
 */
class SkyLanguageDensityTest {

    private static final Path REPORT =
            Path.of("docs/studies/sky-language/placement.md");

    /** Every 180-degree fixture is measured at the globe's default. */
    @Test
    void everyGlobeFixtureIsMeasuredAtTheDensityAGlobeArrivesWith()
            throws Exception {
        List<String> globes = new ArrayList<>();
        for (Fixture fixture : fixtures()) {
            if (fixture.field() >= 180.0) {
                globes.add(fixture.page());
                assertEquals(5.0, fixture.magnitude(), 1.0e-9,
                        fixture.page() + " is a globe and is measured"
                                + " at the limit a globe arrives with,"
                                + " not at one that fills the disc");
            }
        }
        assertEquals(5, globes.size(),
                "all five 180-degree fixtures are checked - "
                        + "Sagittarius, Orion, the RA seam and both"
                        + " poles: " + globes);
    }

    /**
     * Not every rung has the same default.
     *
     * <p>Without this the test above would pass on a study that had
     * simply written 5.0 everywhere, which would be the same mistake
     * in the other direction.
     */
    @Test
    void aNonGlobeRungIsMeasuredAtADifferentDensity() {
        List<Double> elsewhere = new ArrayList<>();
        for (Fixture fixture : fixtures()) {
            if (fixture.field() < 180.0) {
                elsewhere.add(fixture.magnitude());
            }
        }
        assertTrue(!elsewhere.isEmpty(), "there are non-globe pages");
        assertTrue(elsewhere.stream().anyMatch(v -> v != 5.0),
                "and at least one of them is measured at a different"
                        + " density, so the globes agreeing on 5.0 is"
                        + " a property of the rung rather than of a"
                        + " study that wrote one number everywhere: "
                        + elsewhere);
    }

    /** The limit comes from production, not from a study's own copy. */
    @Test
    void everyFixtureUsesTheLimitProductionGivesItsRung() {
        for (Fixture fixture : fixtures()) {
            assertEquals(
                    ChartViewState.defaultMagnitudeFor(fixture.field()),
                    fixture.magnitude(), 1.0e-9,
                    fixture.page() + " uses the limit"
                            + " ChartViewState.defaultMagnitudeFor gives"
                            + " its rung; a study-owned copy would"
                            + " drift the moment production changed");
        }
    }

    /** One fixture as the committed report states it. */
    private record Fixture(String page, double field, double magnitude) {
    }

    /**
     * Read from the report, not from the generator.
     *
     * <p>Asking the generator what it measured would be asking the
     * same code twice. The committed report is what a reader of the
     * evidence sees, and it is what must be true.
     */
    private static List<Fixture> fixtures() {
        List<Fixture> fixtures = new ArrayList<>();
        try {
            String text = Files.readString(REPORT);
            Matcher section = Pattern.compile(
                    "## ([^\n]*?(\\d+) degrees)\n\nLimiting magnitude"
                            + " \\*\\*V (\\d+\\.\\d)\\*\\*")
                    .matcher(text);
            while (section.find()) {
                fixtures.add(new Fixture(section.group(1),
                        Double.parseDouble(section.group(2)),
                        Double.parseDouble(section.group(3))));
            }
        } catch (java.io.IOException cannotRead) {
            throw new AssertionError("the study report is missing",
                    cannotRead);
        }
        assertEquals(14, fixtures.size(),
                "the report states a limiting magnitude for every one"
                        + " of its fourteen pages; a page that stated"
                        + " none would pass every assertion here by"
                        + " not being looked at");
        return fixtures;
    }
}
