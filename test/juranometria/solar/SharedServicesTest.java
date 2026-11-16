package juranometria.solar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The suite's shared services answer by the question alone (#708).
 * One instance of each planetary service serves every computational
 * test, so what one answers must not depend on what it was asked
 * before, how often, in which order or from which thread - and a
 * service read on its own, asked nothing else first, must answer the
 * same. The freshly read set here is this test's subject and is local
 * to it; nothing is kept when it returns.
 */
class SharedServicesTest {

    private static final Observer OSLO =
            new Observer(59.91, 10.75, Instant.parse("2026-08-15T06:00:00Z"));
    private static final Observer SOUTH =
            new Observer(-33.87, 151.21, Instant.parse("2031-01-03T21:30:00Z"));
    private static final Observer EARLY =
            new Observer(40.0, -75.0, Instant.parse("2004-06-08T08:20:00Z"));

    /** One of each service, shared or freshly read. */
    private record Services(SolarSystemService solar, JovianSystemService jovian,
                            SaturnSystemService saturn, MarsService mars,
                            UranusService uranus, NeptuneService neptune,
                            VenusService venus, MercuryService mercury) {

        static Services shared() {
            return new Services(SharedServices.solar(), SharedServices.jovian(),
                    SharedServices.saturn(), SharedServices.mars(),
                    SharedServices.uranus(), SharedServices.neptune(),
                    SharedServices.venus(), SharedServices.mercury());
        }

        static Services fresh() {
            return new Services(SolarSystemService.load(), JovianSystemService.load(),
                    SaturnSystemService.load(), MarsService.load(),
                    UranusService.load(), NeptuneService.load(),
                    VenusService.load(), MercuryService.load());
        }
    }

    /** Every kind of question the services answer, at three places and times. */
    private static List<Function<Services, Object>> questions() {
        List<Function<Services, Object>> q = new ArrayList<>();
        for (Observer o : List.of(OSLO, SOUTH, EARLY)) {
            Instant t = o.instant();
            for (Body body : Body.values()) {
                q.add(s -> s.solar().observe(body, o));
            }
            q.add(s -> s.jovian().observeJupiter(o));
            q.add(s -> s.jovian().observeMoons(o));
            q.add(s -> s.jovian().observeJupiterGeocentric(t));
            q.add(s -> s.saturn().observeSaturn(o));
            q.add(s -> s.saturn().observeMoons(o));
            q.add(s -> s.saturn().observeMoonsGeocentric(t));
            q.add(s -> s.mars().observe(o));
            q.add(s -> s.mars().observeMoons(o));
            q.add(s -> s.mars().observeGeocentric(t));
            q.add(s -> s.uranus().observe(o));
            q.add(s -> s.uranus().observeMoons(o));
            q.add(s -> s.uranus().observeGeocentric(t));
            q.add(s -> s.neptune().observe(o));
            q.add(s -> s.neptune().observeMoons(o));
            q.add(s -> s.neptune().observeGeocentric(t));
            q.add(s -> s.venus().observe(o));
            q.add(s -> s.venus().observeGeocentric(t));
            q.add(s -> s.mercury().observe(o));
            q.add(s -> s.mercury().observeGeocentric(t));
        }
        return q;
    }

    /** The answers, indexed as the questions are, asked in the order given. */
    private static Object[] ask(Services services, List<Function<Services, Object>> questions,
                                List<Integer> order) {
        Object[] answers = new Object[questions.size()];
        for (int i : order) {
            answers[i] = questions.get(i).apply(services);
        }
        return answers;
    }

    private static List<Integer> inOrder(int n) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        return order;
    }

    private static void assertSameAnswers(Object[] expected, Object[] actual, String how) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "question " + i + " answered differently " + how);
        }
    }

    @Test
    void theFixtureHandsOutOneInstanceOfEachService() {
        Services a = Services.shared();
        Services b = Services.shared();
        assertSame(a.solar(), b.solar());
        assertSame(a.jovian(), b.jovian());
        assertSame(a.saturn(), b.saturn());
        assertSame(a.mars(), b.mars());
        assertSame(a.uranus(), b.uranus());
        assertSame(a.neptune(), b.neptune());
        assertSame(a.venus(), b.venus());
        assertSame(a.mercury(), b.mercury());
    }

    @Test
    void aQuestionAskedAgainIsAnsweredTheSame() {
        Services shared = Services.shared();
        List<Function<Services, Object>> questions = questions();
        List<Integer> order = inOrder(questions.size());
        Object[] first = ask(shared, questions, order);
        assertSameAnswers(first, ask(shared, questions, order), "the second time");
        assertSameAnswers(first, ask(shared, questions, order), "the third time");
    }

    @Test
    void theOrderOfTheQuestionsDoesNotChangeTheAnswers() {
        Services shared = Services.shared();
        List<Function<Services, Object>> questions = questions();
        List<Integer> forward = inOrder(questions.size());
        Object[] first = ask(shared, questions, forward);
        List<Integer> backward = new ArrayList<>(forward);
        Collections.reverse(backward);
        assertSameAnswers(first, ask(shared, questions, backward), "asked backwards");
        for (long seed : new long[] {708L, 2026L, 61L}) {
            List<Integer> shuffled = new ArrayList<>(forward);
            Collections.shuffle(shuffled, new Random(seed));
            assertSameAnswers(first, ask(shared, questions, shuffled),
                    "in the order shuffled by seed " + seed);
        }
    }

    @Test
    void aServiceReadOnItsOwnAndAskedNothingElseFirstAnswersAsTheSharedOne() {
        List<Function<Services, Object>> questions = questions();
        List<Integer> forward = inOrder(questions.size());
        Object[] shared = ask(Services.shared(), questions, forward);
        // Each fresh service's first question is a different one from
        // the shared service's first: the fresh set is asked backwards.
        List<Integer> backward = new ArrayList<>(forward);
        Collections.reverse(backward);
        assertSameAnswers(shared, ask(Services.fresh(), questions, backward),
                "by a freshly read service");
    }

    @Test
    void questionsAskedFromSeveralThreadsAtOnceAreAnsweredTheSame() throws Exception {
        Services shared = Services.shared();
        List<Function<Services, Object>> questions = questions();
        List<Integer> forward = inOrder(questions.size());
        Object[] alone = ask(shared, questions, forward);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Object[]>> runs = new ArrayList<>();
            for (int thread = 0; thread < 4; thread++) {
                List<Integer> order = new ArrayList<>(forward);
                Collections.shuffle(order, new Random(thread));
                runs.add(pool.submit(() -> ask(shared, questions, order)));
            }
            for (int thread = 0; thread < runs.size(); thread++) {
                assertSameAnswers(alone, runs.get(thread).get(),
                        "on thread " + thread + " among four");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
