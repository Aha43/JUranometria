package juranometria.ui.meteor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.ShowerNight;
import juranometria.ui.language.InterfaceText;

/**
 * The words the meteor-shower panel says (#716), built headless so a
 * test reads exactly what a reader does.
 *
 * <p>Three rules hold in every line (R-7, R-8, R-15, R-16): a radiant
 * is never stated without the chart time that defines it; the values
 * are called nominal and the edition is named; and no line judges the
 * night.
 */
public final class MeteorShowerText {

    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter HOUR =
            DateTimeFormatter.ofPattern("HH", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    private final InterfaceText said;

    public MeteorShowerText(InterfaceText said) {
        this.said = said;
    }

    /** A shower's name in the reader's language: the name the page writes. */
    public String name(MeteorShower shower) {
        // A shower the words do not know yet is named as the IAU names it.
        String key = "page.body." + shower.identity();
        return said.englishKeys().contains(key) ? said.say(key) : shower.iauName();
    }

    /** What the panel says with no shower selected. */
    public List<String> unselected(MeteorShowerSession session) {
        List<String> lines = new ArrayList<>();
        if (session.chart() == null) {
            lines.add(said.say("meteor.noChart"));
            return lines;
        }
        List<MeteorShower> active = session.activeNow();
        String when = MINUTE.format(session.chart().instant());
        lines.add(active.isEmpty() ? said.say("meteor.none.active", when)
                : said.say("meteor.some.active", when, active.stream().map(this::name)
                        .collect(Collectors.joining(", "))));
        lines.add(edition(session.showers().pack()));
        return lines;
    }

    /** What the panel says of a selected shower the facts could not be computed for. */
    public List<String> outside(MeteorShowerSession session) {
        return List.of(session.chart() == null ? said.say("meteor.noChart")
                : said.say("meteor.outside"));
    }

    /** What the panel says of the selected shower at the chart's place and time. */
    public List<String> lines(MeteorShowerSession.Facts facts, MeteorShowerPack pack) {
        MeteorShower s = facts.shower();
        String when = MINUTE.format(facts.chart().instant());
        List<String> lines = new ArrayList<>();
        lines.add(said.say("meteor.identity", name(s),
                String.format(Locale.ROOT, "%03d", s.iauNumber()), s.code(), s.iauName()));
        if (!s.parentBody().isEmpty()) {
            lines.add(said.say("meteor.parent", s.parentBody()));
        }
        lines.add(said.say(facts.active() ? "meteor.active" : "meteor.inactive", when,
                one(facts.solarLongitude())));
        lines.add(said.say("meteor.activity", s.activityStart(), s.activityEnd(),
                one(s.startLongitude()), one(s.endLongitude())));
        String which = !facts.active() ? "meteor.maximum"
                : facts.maximum().map(at -> at.isBefore(facts.chart().instant())).orElse(false)
                        ? "meteor.maximum.thisPassed" : "meteor.maximum.thisAhead";
        lines.add(facts.maximum().map(at -> said.say(which, s.maximumLongitudeText(),
                maximum(s, at))).orElseGet(() -> said.say("meteor.maximum.none",
                s.maximumLongitudeText())));
        lines.add(said.say("meteor.rate", s.zhr(), s.populationIndex(), whole(s.vInfinity())));
        if (facts.active()) {
            lines.add("");
            lines.add(said.say("meteor.at", when, latitude(facts.chart().latitudeDegrees()),
                    longitude(facts.chart().eastLongitudeDegrees())));
            lines.add(said.say("meteor.radiant", whole(facts.radiant().at().raDegrees()),
                    signed(facts.radiant().at().decDegrees()),
                    signed(facts.radiantAltitudeDegrees())));
            if (facts.radiant().footing() == MeteorShowers.Footing.BEYOND_ROWS) {
                lines.add(said.say("meteor.radiant.beyond"));
            }
            lines.add(said.say("meteor.sun", signed(facts.sunAltitudeDegrees())));
            lines.add(said.say("meteor.moon",
                    whole(facts.moon().illuminatedFraction() * 100.0),
                    said.say("moontable.phase." + phaseKey(facts.moon().phase().name())),
                    signed(facts.moon().horizontal().altitudeDegrees()),
                    whole(facts.moonToRadiantDegrees())));
            facts.night().ifPresent(night -> night(night, lines));
        }
        lines.add("");
        lines.add(edition(pack));
        return lines;
    }

    private void night(ShowerNight night, List<String> lines) {
        lines.add("");
        lines.add(said.say("meteor.night", MINUTE.format(night.from()),
                MINUTE.format(night.to())));
        lines.add(said.say("meteor.night.sun", hours(night.below6()), hours(night.below12()),
                hours(night.below18()), signed(night.sunLowestDegrees())));
        lines.add(night.radiantHighestDegrees().map(highest -> said.say("meteor.night.radiant",
                signed(highest), CLOCK.format(night.radiantHighestAt())))
                .orElseGet(() -> said.say("meteor.night.radiant.none")));
    }

    /** The edition line every view of a value ends with (R-4, R-8). */
    public String edition(MeteorShowerPack pack) {
        return said.say("meteor.edition", pack.edition(), pack.correctAsOf(), pack.credit());
    }

    /** A nominal maximum, shown no finer than its listing warrants (R-7). */
    public String maximum(MeteorShower shower, Instant at) {
        return switch (shower.maximumPrecision()) {
            case TEN_MINUTES -> said.say("meteor.maximum.tenMinutes", MINUTE.format(
                    Instant.ofEpochSecond(Math.round(at.getEpochSecond() / 600.0) * 600L)));
            case HOURS -> {
                Instant hour = Instant.ofEpochSecond(
                        Math.round(at.getEpochSecond() / 3600.0) * 3600L);
                yield said.say("meteor.maximum.hours", DAY.format(hour), HOUR.format(hour));
            }
            case DATE -> said.say("meteor.maximum.date", DAY.format(at));
        };
    }

    private String hours(Duration duration) {
        return duration.isZero() ? said.say("meteor.night.none")
                : said.say("meteor.night.hours", one(duration.getSeconds() / 3600.0));
    }

    private String latitude(double degrees) {
        return said.say(degrees < 0 ? "meteor.south" : "meteor.north", two(Math.abs(degrees)));
    }

    private String longitude(double degrees) {
        return said.say(degrees < 0 ? "meteor.west" : "meteor.east", two(Math.abs(degrees)));
    }

    private static String phaseKey(String name) {
        StringBuilder key = new StringBuilder();
        boolean upper = false;
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                key.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return key.toString();
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String whole(double value) {
        return Long.toString(Math.round(value));
    }

    private static String signed(double value) {
        long rounded = Math.round(value);
        return rounded == 0 ? "0" : (rounded < 0 ? "\u2212" : "+") + Math.abs(rounded);
    }
}
