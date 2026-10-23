package juranometria.ui.placeandtime;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import juranometria.meridian.MeridianModule;
import juranometria.module.Furniture;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;

/**
 * Place and Time's box on the chart (Sprint 51, issue #259, the rulings
 * on #556): what the page says about the place and the frozen instant
 * it is drawn for, and the one action it offers.
 *
 * <p>The meaning and the words are Place and Time's; the chart owns the
 * corner, the layout, the ink, the hit test, the speech and what paper
 * gets. What the box says (ruling 2): the heading; the observer to two
 * decimals with hemisphere letters; the instant to the second, written
 * as Place and Time writes it, with UTC; the local sidereal time to the
 * minute; and <em>Update to now</em>. No civil time, no zone, and no
 * offset inferred from longitude - longitude decides the relation of
 * Greenwich to local sidereal time and nothing about the clocks on a
 * wall.
 *
 * <p>When it is offered (ruling 1): whenever a layer drawn for the
 * instant is switched on - one of Place and Time's own lines, or a
 * Solar System body - whether or not its ink falls on the page in view,
 * so panning never makes the box come and go; and never otherwise, so a
 * page with no such layer is the released page. There is no switch to
 * hide it: a page that depends on its instant says so.
 */
public final class PlaceAndTimeFurniture {

    /** The box's identity, which the view report names. */
    public static final String IDENTITY = "place-and-time";
    /** The box's one action. */
    public static final String UPDATE_TO_NOW = "update-to-now";

    /** The instant as Place and Time writes it, to the second. */
    private static final DateTimeFormatter SHOWN = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private PlaceAndTimeFurniture() {
    }

    /**
     * Offers the box through the module's own seam.
     *
     * @param dependents whether something other than the module's own
     *     lines is drawn for its instant - the Solar System bodies'
     *     switches, composed where the modules are wired, so this module
     *     learns nothing of the others
     * @param update the one Update to now the atlas runs
     */
    public static void offer(MeridianModule module, BooleanSupplier dependents,
                             UpdateToNow update, InterfaceText said) {
        if (module == null || dependents == null || update == null || said == null) {
            throw new IllegalArgumentException(
                    "the box is offered for a module, its dependents, its action,"
                            + " in a language");
        }
        module.offerFurniture(() -> boxFor(module, dependents, said), action -> {
            if (UPDATE_TO_NOW.equals(action)) {
                update.run();
            }
        });
    }

    /** The box, while a layer drawn for the instant is on; none otherwise. */
    static List<Furniture> boxFor(MeridianModule module, BooleanSupplier dependents,
                                  InterfaceText said) {
        boolean timeDependent = module.meridianShowing() || module.horizonShowing()
                || module.zenithShowing() || dependents.getAsBoolean();
        return timeDependent ? List.of(box(module.observer(), said)) : List.of();
    }

    /** The latitude as the box writes it, to this many decimals. */
    static String latitude(Observer at, InterfaceText said, int decimals) {
        return said.say("furniture.placeandtime.latitude",
                String.format(Locale.ROOT, "%." + decimals + "f",
                        Math.abs(at.latitudeDegrees())),
                said.say(at.latitudeDegrees() >= 0.0 ? "furniture.placeandtime.north"
                        : "furniture.placeandtime.south"));
    }

    /** The longitude as the box writes it, folded, to this many decimals. */
    static String longitude(Observer at, InterfaceText said, int decimals) {
        double east = at.eastLongitudeFolded();
        return said.say("furniture.placeandtime.longitude",
                String.format(Locale.ROOT, "%." + decimals + "f", Math.abs(east)),
                said.say(east >= 0.0 ? "furniture.placeandtime.east"
                        : "furniture.placeandtime.west"));
    }

    /**
     * The place as the box writes it - latitude and longitude with
     * hemisphere letters - to this many decimals: the box's two, or more
     * where a panel must tell two nearby places apart.
     */
    public static String place(Observer at, InterfaceText said, int decimals) {
        return said.say("furniture.placeandtime.place", latitude(at, said, decimals),
                longitude(at, said, decimals));
    }

    /**
     * The local apparent sidereal time as the box states it: minutes
     * from 0 to 1439, truncated - a box that rounded up would state a
     * sidereal minute the chart has not reached. The Place and Time
     * panel's sidereal field reads and writes the same minute (#612).
     */
    public static int siderealMinutes(Observer at) {
        return (int) Math.floor(
                new LocalSky(at).localSiderealTimeDegrees() / 15.0 * 60.0) % (24 * 60);
    }

    /** What the box says for this observer, in this language. */
    public static Furniture box(Observer at, InterfaceText said) {
        String latitude = latitude(at, said, 2);
        String longitude = longitude(at, said, 2);
        String instant = SHOWN.format(at.instant());
        int minutes = siderealMinutes(at);
        String hours = String.format(Locale.ROOT, "%02d", minutes / 60);
        String mins = String.format(Locale.ROOT, "%02d", minutes % 60);
        return new Furniture(IDENTITY,
                said.say("furniture.placeandtime.heading"),
                List.of(said.say("furniture.placeandtime.place", latitude, longitude),
                        said.say("furniture.placeandtime.instant", instant, "UTC"),
                        said.say("furniture.placeandtime.lst", hours, mins)),
                new Furniture.Action(UPDATE_TO_NOW,
                        said.say("furniture.placeandtime.action"),
                        said.say("furniture.placeandtime.paper"),
                        said.say("furniture.placeandtime.explain")),
                said.say("furniture.placeandtime.spoken", latitude, longitude, instant,
                        "UTC", hours, mins),
                said.say("furniture.placeandtime.sheet", latitude, longitude, instant,
                        "UTC"));
    }

    /** The instant as the box writes it - for the panels that compare against it. */
    public static String shown(Instant instant) {
        return SHOWN.format(instant);
    }
}
