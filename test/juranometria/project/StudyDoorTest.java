package juranometria.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #686 study door: shut by default, shut after the work however
 * it ends, opened only by the tool package, and removed by #687.
 */
class StudyDoorTest {

    private static final SkyPosition ORION = new SkyPosition(83.8, -5.4);

    @Test
    void theDoorIsShutByDefaultAndProductionProjectionsAreUnchanged() {
        assertFalse(Projections.studyDoorOpen());
        assertTrue(Projections.of(ChartProjection.STEREOGRAPHIC, ORION)
                instanceof StereographicProjection);
    }

    @Test
    void insideTheDoorEveryDerivationIsTheToldProjection() throws Exception {
        Projection told = new GnomonicProjection(ORION);
        Projection seen = Projections.drawnForStudy(told,
                () -> Projections.of(ChartProjection.ORTHOGRAPHIC, ORION));
        assertSame(told, seen);
        assertFalse(Projections.studyDoorOpen());
    }

    @Test
    void theDoorShutsWhenTheWorkFails() {
        assertThrows(IllegalStateException.class, () -> Projections.drawnForStudy(
                new GnomonicProjection(ORION), () -> {
                    throw new IllegalStateException("work failed");
                }));
        assertFalse(Projections.studyDoorOpen());
    }

    @Test
    void onlyTheToolPackageOpensTheDoor() throws IOException {
        List<String> openers;
        try (Stream<Path> tree = Files.walk(Path.of("src"))) {
            openers = tree.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().replace('\\', '/')
                            .startsWith("src/juranometria/tool/"))
                    .filter(p -> !p.toString().replace('\\', '/')
                            .equals("src/juranometria/project/Projections.java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("drawnForStudy(");
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    })
                    .map(Path::toString).sorted().toList();
        }
        assertEquals(List.of(), openers, "only study code may open the #686 door");
        assertTrue(Files.readString(Path.of("src/juranometria/project/Projections.java"))
                        .contains("removed by #687"),
                "the door names its remover");
    }
}
