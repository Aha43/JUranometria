package juranometria.tool;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.Selection;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.SyntheticShowers;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.module.Furniture;
import juranometria.page.LeadSelection;
import juranometria.page.PageEntry;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;
import juranometria.ui.selection.SelectedObjectFacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A radiant as an occurrence on the application's own chart (#716,
 * R-15): drawn through the production module on a composed page, in the
 * page's inventory as a selectable entry at the chart time, selected as
 * identity, instant, observer and position, following the chart time
 * while the shower is active and gone from the page when it is not.
 *
 * <p>The shower is invented - it only borrows a code the page has a
 * name for - so the route is proved without any listed value.
 */
class RadiantOccurrenceJourneyTest {

    private static final String ID = "meteor-shower.PER";
    private static final MeteorShowers SERVICE = SyntheticShowers.serviceNamed("PER");

    private static <T> T onEventThread(Callable<T> work) throws Exception {
        Object[] out = new Object[1];
        Throwable[] failed = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                out[0] = work.call();
            } catch (Throwable e) {
                failed[0] = e;
            }
        });
        if (failed[0] instanceof Error error) {
            throw error;
        }
        if (failed[0] != null) {
            throw new RuntimeException(failed[0]);
        }
        @SuppressWarnings("unchecked")
        T value = (T) out[0];
        return value;
    }

    @Test
    void aSelectedRadiantIsAnOccurrenceThatFollowsTheChartTime() throws Exception {
        Observer first = new Observer(59.91, 10.75, SyntheticShowers.at(100.0));
        WholeSkyComposition.Page page = WholeSkyComposition.paint(new WholeSkyComposition.Ask(
                first, ChartPalette.WHITE_PAPER, "en", 6.0, true, true, 60.0, 1000, 760),
                new WholeSkyComposition.Services());
        onEventThread(() -> {
            MeteorShowerModule module = page.host().attach(new MeteorShowerModule(
                    () -> page.meridian().observer(), () -> SERVICE,
                    page.meridian()::subduing));
            page.session().leave();
            page.controller().recenter(MeteorShowers.radiantAt(
                    SERVICE.shower("PER").orElseThrow(), 100.0).at(), 60.0);
            WholeSkyComposition.paintNow(page.chart(), 1000, 760);
            assertTrue(page.chart().renderedBodies().stream()
                    .noneMatch(b -> b.identity().equals(ID)), "not ink until it is selected");

            module.select("PER");
            WholeSkyComposition.paintNow(page.chart(), 1000, 760);
            ReferenceInk.BodyPlacement drawn = page.chart().renderedBodies().stream()
                    .filter(b -> b.identity().equals(ID)).findFirst().orElseThrow();
            assertEquals(first.instant(), drawn.moment().instant());
            assertTrue(page.chart().selectableBodyAt(drawn.centre().x() + 6.0,
                    drawn.centre().y()).map(b -> b.identity().equals(ID)).orElse(false),
                    "a reader pointing at the rays points at the radiant");
            assertTrue(page.chart().bodyAt(drawn.centre().x(), drawn.centre().y()).isEmpty(),
                    "and it covers nothing");

            // The page's inventory carries it, and selecting it is an occurrence.
            PageEntry.Body entry = page.host().inventory().bodies().stream()
                    .filter(b -> b.identity().equals(ID)).findFirst().orElseThrow();
            assertEquals("Perseids", entry.name());
            Selection.Occurrence selected = (Selection.Occurrence) LeadSelection.resolve(
                    page.host().inventory(), ID).orElseThrow();
            assertEquals(first.instant(), selected.instant());
            assertEquals(59.91, selected.latitudeDegrees());
            assertEquals(10.75, selected.eastLongitudeDegrees());
            assertTrue(selected.position().separationDegrees(MeteorShowers.radiantAt(
                    SERVICE.shower("PER").orElseThrow(), 100.0).at()) < 0.2,
                    "the radiant for that chart time, to the pixel it was drawn at");
            page.host().workingSelection().replaceWith(List.of(ID), ID);
            assertTrue(page.host().selection().selection() instanceof Selection.Occurrence);

            // The box a selected radiant gets says what it is and when (C-5).
            Furniture box = SelectedObjectFacts.box(SelectedObjectFacts.of(entry, false)
                    .orElseThrow(), page.said());
            assertEquals("Perseids", box.heading());
            assertEquals("meteor shower radiant", box.lines().get(0));
            assertEquals("for 2030-04-11 00:00 UTC", box.lines().get(1));

            // Another chart time inside the activity: the same identity, the new moment.
            Observer later = first.at(SyntheticShowers.at(104.0));
            page.meridian().observer(later);
            WholeSkyComposition.paintNow(page.chart(), 1000, 760);
            Selection.Occurrence followed =
                    (Selection.Occurrence) page.host().selection().selection();
            assertEquals(ID, followed.identity());
            assertEquals(later.instant(), followed.instant(),
                    "the selected occurrence follows the page (#513)");
            assertTrue(followed.position().separationDegrees(selected.position()) > 1.0,
                    "and the radiant has moved with the Sun's longitude");

            // Outside the activity the shower stays selected and the page draws no radiant.
            page.meridian().observer(first.at(SyntheticShowers.at(200.0)));
            WholeSkyComposition.paintNow(page.chart(), 1000, 760);
            assertEquals("PER", module.selected());
            assertTrue(page.chart().renderedBodies().stream()
                    .noneMatch(b -> b.identity().equals(ID)));
            assertTrue(page.host().inventory().bodies().stream()
                    .noneMatch(b -> b.identity().equals(ID)));
            Selection.Occurrence kept =
                    (Selection.Occurrence) page.host().selection().selection();
            assertEquals(later.instant(), kept.instant(),
                    "the selection keeps the moment it was last drawn for, never a new one");
            String described = page.chart().getAccessibleContext().getAccessibleDescription();
            assertFalse(described.contains("Perseids"), "and the page no longer speaks of it");
            return null;
        });
    }
}
