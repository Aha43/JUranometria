package juranometria.meteor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import juranometria.catalog.Sha256;

/**
 * A made-up meteor-shower pack and a made-up Sun for tests (#716).
 *
 * <p>Nothing here is a real shower's value: the codes, dates, radiants
 * and rates are invented, so no test embeds the International Meteor
 * Organization's data. The invented Sun moves one degree of longitude a
 * day from longitude 0 at {@link #EPOCH}, which makes every expected
 * instant a matter of counting days.
 */
public final class SyntheticShowers {

    /** Solar longitude 0 of the invented Sun. */
    public static final Instant EPOCH = Instant.parse("2030-01-01T00:00:00Z");

    /** The invented Sun: one degree a day from 0 at the epoch. */
    public static final ToDoubleFunction<Instant> SUN = instant -> {
        double days = (instant.getEpochSecond() - EPOCH.getEpochSecond()) / 86400.0;
        return ((days % 360.0) + 360.0) % 360.0;
    };

    public static final String SHOWERS = MeteorShowerPack.SHOWERS_HEADER + "\n"
            // Active from longitude 90 to 110, maximum at 100.5; four rows.
            + String.join("\t", "AAA", "901", "Alphaids", "9P/Invented", "Apr 01", "Apr 21",
                    "90.0000", "110.0000", "Apr 11", "100.5", "40", "20", "30", "2.5", "12")
            + "\n"
            // Spans longitude 0: active from 350 to 10, maximum at 0.25; no rows.
            + String.join("\t", "BBB", "902", "Betaids", "", "Dec 22", "Jan 11",
                    "350.0000", "10.0000", "Jan 01", "0.25", "200", "-30", "50", "3.0", "20+")
            + "\n"
            // Maximum printed to a whole degree; one row only.
            + String.join("\t", "CCC", "903", "Gammaids", "", "Jul 01", "Jul 11",
                    "180.0000", "190.0000", "Jul 06", "185", "10", "80", "20", "2.0", "Var")
            + "\n";

    public static final String RADIANTS = MeteorShowerPack.RADIANTS_HEADER + "\n"
            + String.join("\t", "AAA", "4", "1", "95.0000", "30", "20") + "\n"
            + String.join("\t", "AAA", "4", "6", "100.0000", "40", "20") + "\n"
            + String.join("\t", "AAA", "4", "11", "105.0000", "50", "20") + "\n"
            + String.join("\t", "CCC", "7", "6", "185.0000", "12", "81") + "\n";

    private SyntheticShowers() {
    }

    /** The pack's files, with a manifest that matches them. */
    public static Map<String, String> files() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(MeteorShowerPack.SHOWERS, SHOWERS);
        files.put(MeteorShowerPack.RADIANTS, RADIANTS);
        files.put("manifest.properties", String.join("\n", "format.version=1",
                "pack.name=meteor-showers", "edition=Invented Calendar 2030",
                "edition.year=2030", "edition.correctAsOf=never", "credit=Source: nobody",
                "identity.source=an invented list", "identity.lastUpdate=never", "showers=3",
                "radiant.rows=4",
                "checksum." + MeteorShowerPack.SHOWERS + "=" + sha(SHOWERS),
                "checksum." + MeteorShowerPack.RADIANTS + "=" + sha(RADIANTS)) + "\n");
        return files;
    }

    /** An opener over a set of files. */
    public static Function<String, InputStream> opener(Map<String, String> files) {
        return name -> files.containsKey(name)
                ? new ByteArrayInputStream(files.get(name).getBytes(StandardCharsets.UTF_8))
                : null;
    }

    /**
     * The invented pack with its first shower renamed to a code the page
     * has words for, for a test that draws the radiant: the page names
     * a shower through its language files. The values stay invented.
     */
    public static MeteorShowers serviceNamed(String code) {
        Map<String, String> files = new LinkedHashMap<>();
        String showers = SHOWERS.replace("AAA", code);
        String radiants = RADIANTS.replace("AAA", code);
        files.put(MeteorShowerPack.SHOWERS, showers);
        files.put(MeteorShowerPack.RADIANTS, radiants);
        files.put("manifest.properties", files().get("manifest.properties")
                .replace(sha(SHOWERS), sha(showers)).replace(sha(RADIANTS), sha(radiants)));
        return new MeteorShowers(MeteorShowerPack.load(opener(files)), SUN);
    }

    /** The invented pack. */
    public static MeteorShowerPack pack() {
        return MeteorShowerPack.load(opener(files()));
    }

    /** The service over the invented pack and the invented Sun. */
    public static MeteorShowers service() {
        return new MeteorShowers(pack(), SUN);
    }

    /** The instant the invented Sun is at a longitude, in the epoch's first turn. */
    public static Instant at(double longitude) {
        return EPOCH.plusSeconds(Math.round(longitude * 86400.0));
    }

    static String sha(String text) {
        return Sha256.hex(text.getBytes(StandardCharsets.UTF_8));
    }
}
