package juranometria.solar;

/**
 * The planetary services the suite shares: one instance of each, read
 * when first asked for and kept for the run, over one copy of each
 * pack - as the application reads each pack once and hands the same
 * service to every reader (#708).
 *
 * <p>The whole suite runs in one process. A test class that read its
 * own service and kept it in a static field kept that copy of the
 * kernel bytes, 15 to 95 MB a service, until the process ended;
 * seventy-five such classes held about 3 GB between them. The services
 * hold no observational state - what one answers depends on the
 * question alone, never on what was asked before - so one instance
 * serves every computational test, and
 * {@link SharedServicesTest} holds that to repeated questions and to
 * questions asked in different orders.
 *
 * <p>A test whose subject is the reading itself - a pack's contract,
 * a corrupt or missing resource, an excerpt, a kernel's segments, a
 * service's independence of another - reads its own and does not come
 * here. {@link juranometria.tool.StaticServiceHolderTest} refuses a
 * new static holder anywhere but here and on its reviewed list.
 */
public final class SharedServices {

    private static SolarSystemPack solarPack;
    private static PlanetsPack planetsPack;
    private static SolarSystemService solar;
    private static JovianSystemService jovian;
    private static SaturnSystemService saturn;
    private static MarsService mars;
    private static UranusService uranus;
    private static NeptuneService neptune;
    private static VenusService venus;
    private static MercuryService mercury;

    private SharedServices() {
    }

    private static synchronized SolarSystemPack solarPack() {
        if (solarPack == null) {
            solarPack = SolarSystemPack.load();
        }
        return solarPack;
    }

    private static synchronized PlanetsPack planetsPack() {
        if (planetsPack == null) {
            planetsPack = PlanetsPack.load();
        }
        return planetsPack;
    }

    /** The Sun's and the Moon's service, as {@code SolarSystemService.load()} builds it. */
    public static synchronized SolarSystemService solar() {
        if (solar == null) {
            solar = new SolarSystemService(solarPack());
        }
        return solar;
    }

    /** Jupiter's, as {@code JovianSystemService.load()} builds it. */
    public static synchronized JovianSystemService jovian() {
        if (jovian == null) {
            jovian = new JovianSystemService(solarPack(), JovianPack.load());
        }
        return jovian;
    }

    /** Saturn's, as {@code SaturnSystemService.load()} builds it. */
    public static synchronized SaturnSystemService saturn() {
        if (saturn == null) {
            saturn = new SaturnSystemService(solarPack(), SaturnPack.load());
        }
        return saturn;
    }

    /** Mars's, as {@code MarsService.load()} builds it. */
    public static synchronized MarsService mars() {
        if (mars == null) {
            mars = new MarsService(solarPack(), planetsPack(), MarsSystemPack.load());
        }
        return mars;
    }

    /** Uranus's, as {@code UranusService.load()} builds it. */
    public static synchronized UranusService uranus() {
        if (uranus == null) {
            uranus = new UranusService(solarPack(), UranusSystemPack.load());
        }
        return uranus;
    }

    /** Neptune's, as {@code NeptuneService.load()} builds it. */
    public static synchronized NeptuneService neptune() {
        if (neptune == null) {
            neptune = new NeptuneService(solarPack(), NeptuneSystemPack.load());
        }
        return neptune;
    }

    /** Venus's own service, as {@code VenusService.load()} builds it. */
    public static synchronized VenusService venus() {
        if (venus == null) {
            venus = new VenusService(solarPack(), planetsPack());
        }
        return venus;
    }

    /** Mercury's own service, as {@code MercuryService.load()} builds it. */
    public static synchronized MercuryService mercury() {
        if (mercury == null) {
            mercury = new MercuryService(solarPack(), planetsPack());
        }
        return mercury;
    }
}
