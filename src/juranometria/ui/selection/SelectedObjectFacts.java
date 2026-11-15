package juranometria.ui.selection;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.module.Furniture;
import juranometria.page.PageEntry;
import juranometria.render.SymbolFamily;
import juranometria.ui.language.InterfaceText;

/**
 * What the chart may say about the last selected object (#692, ruled C-9
 * and C-14 on #690, docs/decisions/guide-and-explain.md §5): one sealed,
 * typed value per family the selection model admits - a star, a deep-sky
 * object, a Solar System body - built only from what the page already
 * holds, so the box performs no second calculation and accepts no free
 * text.
 *
 * <p>It is also the one place the Inspector and the box share their
 * formatting of a position, a magnitude and a Messier name, so the two
 * cannot come to write the same fact differently.
 */
public sealed interface SelectedObjectFacts {

    /** The identity the working selection names. */
    String identity();

    /** A star: its designations, its V magnitude and its J2000 place. */
    record StarFacts(String identity, String name, String bayer, String flamsteed,
                     String constellation, double magnitude, SkyPosition position)
            implements SelectedObjectFacts {
    }

    /** A deep-sky object: its Messier name if any, its family, its recorded magnitude. */
    record DeepSkyFacts(String identity, String messier, SymbolFamily family,
                        DeepSkyObject.Recorded.Band band, double magnitude,
                        SkyPosition position) implements SelectedObjectFacts {
    }

    /** A body: its page name, the chart time it was drawn for, its place, dimmed or not. */
    record BodyFacts(String identity, String name, Instant instant, SkyPosition position,
                     boolean belowHorizon) implements SelectedObjectFacts {
    }

    /**
     * A meteor shower's radiant (#716, ruling C-5): the shower's page
     * name, the chart time that defines the radiant, its place to a whole
     * degree as its source prints it, dimmed or not. Never without the
     * chart time.
     */
    record RadiantFacts(String identity, String name, Instant instant, SkyPosition position,
                        boolean belowHorizon) implements SelectedObjectFacts {
    }

    /**
     * The facts for one page-inventory entry, or empty for an entry the
     * box does not describe. {@code belowHorizon} is the page's own
     * answer for a body, from the placement it painted.
     */
    static Optional<SelectedObjectFacts> of(PageEntry entry, boolean belowHorizon) {
        return switch (entry) {
            case PageEntry.StarEntry s -> {
                Star star = s.star();
                StarIdentity id = star.identity();
                yield Optional.of(new StarFacts(star.id(),
                        id == null ? null : id.name(), id == null ? null : id.bayer(),
                        id == null ? null : id.flamsteed(),
                        id == null ? null : id.constellation(),
                        star.magnitude(), star.position()));
            }
            case PageEntry.DeepSky d -> {
                DeepSkyObject dso = d.object();
                yield Optional.of(new DeepSkyFacts(dso.id(), messierName(dso).orElse(null),
                        SymbolFamily.of(dso.type()), dso.recorded().band(),
                        dso.magnitude(), dso.position()));
            }
            case PageEntry.Body b -> b.moment() == null ? Optional.empty()
                    : juranometria.meteor.MeteorShower.codeOf(b.identity()) != null
                    ? Optional.of(new RadiantFacts(b.identity(), b.name(),
                            b.moment().instant(), b.position(), belowHorizon))
                    : Optional.of(new BodyFacts(b.identity(), b.name(), b.moment().instant(),
                            b.position(), belowHorizon));
            default -> Optional.empty();
        };
    }

    /**
     * The box the facts make: a heading and at most three lines, a fourth
     * only for a body the page dims below the horizon (C-9); placed upper
     * left first (C-11); spoken and described as one sentence.
     */
    static Furniture box(SelectedObjectFacts facts, InterfaceText said) {
        String heading;
        List<String> lines = new ArrayList<>();
        switch (facts) {
            case StarFacts s -> {
                heading = s.name() != null ? s.name()
                        : s.bayer() != null ? designation(s.bayer(), s.constellation())
                        : s.flamsteed() != null ? designation(s.flamsteed(), s.constellation())
                        : s.identity();
                List<String> names = new ArrayList<>();
                if (s.name() != null && s.bayer() != null) {
                    names.add(designation(s.bayer(), s.constellation()));
                } else if (s.name() != null && s.flamsteed() != null) {
                    names.add(designation(s.flamsteed(), s.constellation()));
                }
                if (!heading.equals(s.identity())) {
                    names.add(s.identity());
                }
                if (!names.isEmpty()) {
                    lines.add(String.join(" · ", names));
                }
                lines.add(said.say("selected.magnitude.visual", magnitudeValue(s.magnitude())));
                lines.add(coordinates(s.position()));
            }
            case DeepSkyFacts d -> {
                heading = d.messier() != null ? d.messier() : d.identity();
                List<String> first = new ArrayList<>();
                if (!heading.equals(d.identity())) {
                    first.add(d.identity());
                }
                first.add(said.say("selected.family." + d.family().name()));
                lines.add(String.join(" · ", first));
                lines.add(switch (d.band()) {
                    case VISUAL -> said.say("selected.magnitude.visual",
                            magnitudeValue(d.magnitude()));
                    case BLUE -> said.say("selected.magnitude.blue",
                            magnitudeValue(d.magnitude()));
                    case NONE -> said.say("inspector.magnitude.none");
                });
                lines.add(coordinates(d.position()));
            }
            case BodyFacts b -> {
                heading = b.name();
                lines.add(said.say("selected.time",
                        MINUTE.format(b.instant().atOffset(ZoneOffset.UTC))));
                lines.add(coordinates(b.position()));
                if (b.belowHorizon()) {
                    lines.add(said.say("page.body.belowHorizon"));
                }
            }
            case RadiantFacts r -> {
                heading = r.name();
                lines.add(said.say("selected.radiant"));
                lines.add(said.say("selected.time",
                        MINUTE.format(r.instant().atOffset(ZoneOffset.UTC))));
                lines.add(wholeDegrees(r.position(), said));
                if (r.belowHorizon()) {
                    lines.add(said.say("page.body.belowHorizon"));
                }
            }
        }
        String sentence = said.say("selected.spoken",
                heading + ", " + String.join(", ", lines));
        return new Furniture(IDENTITY, heading, lines, null, sentence, sentence, true);
    }

    /** The box's identity on the page and in the view report. */
    String IDENTITY = "selected-object";

    /** The chart time to the minute, as Place and Time's box writes it. */
    DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    /** A Bayer letter or Flamsteed number with its constellation: "α Ori", "58 Ori". */
    private static String designation(String designation, String constellation) {
        return constellation == null ? designation : designation + " " + constellation;
    }

    /**
     * A J2000 place as the Inspector has always written it.
     *
     * <p>Moved here from {@code InspectorPanel} unchanged (#692), so the
     * Inspector and the box write one place one way.
     */
    static String coordinates(SkyPosition position) {
        double hours = position.raDegrees() / 15.0;
        int h = (int) hours;
        double minutes = (hours - h) * 60.0;
        char sign = position.decDegrees() < 0 ? '−' : '+';
        double absolute = Math.abs(position.decDegrees());
        int d = (int) absolute;
        int m = (int) Math.round((absolute - d) * 60.0);
        return String.format(Locale.ROOT, "%dh %04.1fm   %c%d° %02d′",
                h, minutes, sign, d, m);
    }

    /**
     * A magnitude, formatted the same way everywhere: Locale.ROOT, because
     * a magnitude is a measurement the atlas states, not a number the
     * reader's desktop may punctuate. Moved here from the Inspector (#692).
     */
    static String magnitudeValue(double magnitude) {
        return String.format(Locale.ROOT, "%.2f", magnitude);
    }

    /** An object's Messier name, if it has one. Moved here from the Inspector (#692). */
    static Optional<String> messierName(DeepSkyObject dso) {
        return dso.aliases().stream().filter(a -> a.startsWith("M ")).findFirst();
    }

    /** A radiant's J2000 place to a whole degree: its source prints no finer. */
    static String wholeDegrees(SkyPosition position, InterfaceText said) {
        long dec = Math.round(position.decDegrees());
        return said.say("selected.radiant.position",
                Long.toString(Math.round(position.raDegrees()) % 360),
                dec == 0 ? "0" : (dec < 0 ? "\u2212" : "+") + Math.abs(dec));
    }
}
