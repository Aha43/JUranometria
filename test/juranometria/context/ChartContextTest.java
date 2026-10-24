package juranometria.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import juranometria.sky.Observer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one chart time and place (#612): an immutable snapshot with a
 * revision; installed atomically on the event thread, observer and
 * instant together; readable from any thread; refused while a paint
 * holds it; every listener told of the whole change.
 */
class ChartContextTest {

    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-10-05T00:00:00Z"));

    @Test
    void aContextIsAnObserverAtAnInstantWithARevision() {
        ChartContext context = new ChartContext(OSLO, 3);
        assertEquals(OSLO.instant(), context.instant());
        assertEquals(3, context.revision());
        assertTrue(context.sameAs(new ChartContext(OSLO, 9)), "sameness ignores the revision");
        assertFalse(context.sameAs(new ChartContext(OSLO.at(Instant.EPOCH), 3)));
        assertThrows(IllegalArgumentException.class, () -> new ChartContext(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChartContext(OSLO, -1));
    }

    @Test
    void installationIsAtomicOnTheEventThreadAndTellsEveryListenerOnce() throws Exception {
        ChartContextHolder holder = new ChartContextHolder(OSLO);
        assertEquals(0, holder.current().revision());
        List<ChartContextHolder.Change> heard = new ArrayList<>();
        holder.onChange(heard::add);
        Observer later = OSLO.at(Instant.parse("2026-10-06T12:00:00Z")).from(60.0, 11.0);
        SwingUtilities.invokeAndWait(() -> holder.install(later));
        assertEquals(1, heard.size());
        assertEquals(0, heard.get(0).previous().revision());
        assertEquals(1, heard.get(0).current().revision());
        assertEquals(later, heard.get(0).current().observer(),
                "the observer and the instant arrive together");
        assertSame(heard.get(0).current(), holder.current());
    }

    @Test
    void installationOffTheEventThreadIsRefusedAndChangesNothing() {
        ChartContextHolder holder = new ChartContextHolder(OSLO);
        List<ChartContextHolder.Change> heard = new ArrayList<>();
        holder.onChange(heard::add);
        assertFalse(SwingUtilities.isEventDispatchThread(), "this test runs off the event thread");
        assertThrows(IllegalStateException.class, () -> holder.install(OSLO.at(Instant.EPOCH)));
        assertEquals(0, holder.current().revision());
        assertEquals(List.of(), heard);
    }

    @Test
    void aSnapshotIsReadableFromAnyThreadAndNeverTorn() throws Exception {
        ChartContextHolder holder = new ChartContextHolder(OSLO);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 20_000; i++) {
                    ChartContext seen = holder.current();
                    // Every snapshot pairs its own observer with its own revision:
                    // revision n > 0 carries latitude n (mod 90) by construction
                    // below; revision 0 is Oslo.
                    double expected = seen.revision() == 0 ? 59.91 : seen.revision() % 90;
                    if (seen.observer().latitudeDegrees() != expected) {
                        throw new AssertionError("torn: " + seen);
                    }
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        reader.start();
        for (int n = 1; n <= 500; n++) {
            double latitude = n % 90;
            SwingUtilities.invokeAndWait(() -> holder.install(
                    holder.observer().from(latitude, 10.75)));
        }
        reader.join();
        assertEquals(null, failure.get(), () -> String.valueOf(failure.get()));
        assertEquals(500, holder.current().revision());
    }

    @Test
    void whileAPaintHoldsTheContextAnInstallationIsRefused() throws Exception {
        ChartContextHolder holder = new ChartContextHolder(OSLO);
        AtomicReference<Exception> refused = new AtomicReference<>();
        AtomicReference<ChartContext> during = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.hold(() -> {
            assertTrue(holder.held());
            ChartContext first = holder.current();
            try {
                holder.install(OSLO.at(Instant.EPOCH));
            } catch (IllegalStateException e) {
                refused.set(e);
            }
            during.set(holder.current());
            assertSame(first, during.get(), "one snapshot throughout the paint");
            return null;
        }));
        assertNotNull(refused.get(), "the installation during the paint was refused");
        assertFalse(holder.held());
        assertEquals(0, holder.current().revision(), "and nothing changed");
        SwingUtilities.invokeAndWait(() -> holder.install(OSLO.at(Instant.EPOCH)));
        assertEquals(1, holder.current().revision(), "after the paint, installs resume");
    }

    @Test
    void aCancelledSubscriptionHearsNothingMore() throws Exception {
        ChartContextHolder holder = new ChartContextHolder(OSLO);
        List<ChartContextHolder.Change> heard = new ArrayList<>();
        ChartContextHolder.Subscription s = holder.onChange(heard::add);
        assertEquals(1, holder.subscribers());
        s.cancel();
        s.cancel();
        assertEquals(0, holder.subscribers());
        SwingUtilities.invokeAndWait(() -> holder.install(OSLO.at(Instant.EPOCH)));
        assertEquals(List.of(), heard);
    }
}
