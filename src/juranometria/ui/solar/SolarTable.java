package juranometria.ui.solar;

import java.util.List;

import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.CompassPoint;
import juranometria.solar.SolarSystemService.LimbConditioning;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Observation;
import juranometria.solar.SolarSystemService.Phase;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.SolarSystemService.SunObservation;

/**
 * What one body's table shows: its columns and how each cell is spelled
 * (Sprint 36, issue #408).
 *
 * <p>The table's shell - rows over instants, the marks, the range and
 * its refusals - is body-agnostic and lives in {@link SolarTableDialog}
 * and {@link SolarTableModel}. What a body contributes is exactly this:
 * which body the service is asked for, which key family its own words
 * are in, which columns it has, and how an observation becomes a row of
 * strings. Numbers are notation, formatted in {@code Locale.ROOT} and
 * respelled with the language's decimal separator; the words in a cell
 * - a phase, a compass point, a status - are the language's.
 */
public interface SolarTable {

    /** The body the service is asked for. */
    Body body();

    /** The key family of this body's own words: {@code suntable}, {@code moontable}. */
    String stem();

    /** The prefix of this table's component names, for tests and photographers. */
    String prefix();

    /** The column key suffixes, in order, under {@code column.}. */
    List<String> columns();

    /** Preferred column widths, in pixels, one per column. */
    int[] widths();

    /** The preferred width of the table's viewport, in pixels. */
    int preferredWidth();

    /** One cell, spelled for the reader. */
    String cell(Observation observation, int column, SolarTableWords words);

    /** The Sun's table (issue #400): the eight columns the contract froze. */
    static SolarTable sun() {
        return Sun.INSTANCE;
    }

    /** The Moon's table (issue #406, M5 and M6): the eleven columns as ruled. */
    static SolarTable moon() {
        return Moon.INSTANCE;
    }

    /** The Sun's columns. */
    enum Sun implements SolarTable {
        INSTANCE;

        private static final List<String> COLUMNS = List.of("instant", "ra",
                "dec", "longitude", "altitude", "azimuth", "distance", "diameter");
        private static final int[] WIDTHS = {150, 130, 120, 130, 170, 150, 190, 130};

        @Override
        public Body body() {
            return Body.SUN;
        }

        @Override
        public int preferredWidth() {
            return 760;
        }

        @Override
        public String stem() {
            return "suntable";
        }

        @Override
        public String prefix() {
            return "sun";
        }

        @Override
        public List<String> columns() {
            return COLUMNS;
        }

        @Override
        public int[] widths() {
            return WIDTHS.clone();
        }

        @Override
        public String cell(Observation observation, int column, SolarTableWords w) {
            SunObservation o = (SunObservation) observation;
            return switch (column) {
                case 1 -> w.n(SunTableFormat.hms(o.astrometricJ2000()));
                case 2 -> SunTableFormat.dms(o.astrometricJ2000());
                case 3 -> w.n(SunTableFormat.degrees(o.eclipticLongitudeJ2000Degrees()));
                case 4 -> SunTableFormat.altitude(
                        w.n(SunTableFormat.degrees(o.horizontal().altitudeDegrees())),
                        o.horizontal().altitudeDegrees(), w.say("below"));
                case 5 -> w.n(SunTableFormat.degrees(o.horizontal().azimuthDegrees()));
                case 6 -> w.say("distance.pattern",
                        w.n(SunTableFormat.astronomicalUnits(o.distanceAu())),
                        w.n(SunTableFormat.millionKilometres(o.distanceKm())));
                case 7 -> w.n(SunTableFormat.minutesSeconds(o.angularDiameterArcseconds()));
                default -> throw new IndexOutOfBoundsException(column);
            };
        }
    }

    /** The Moon's columns. */
    enum Moon implements SolarTable {
        INSTANCE;

        private static final List<String> COLUMNS = List.of("instant", "ra",
                "dec", "altitude", "azimuth", "distance", "diameter",
                "illuminated", "phase", "elongation", "litside");
        private static final int[] WIDTHS = {140, 125, 115, 160, 150, 105, 115,
                85, 130, 140, 180};

        /** The key of each phase's word, in the enum's order. */
        static final List<String> PHASE_KEYS = List.of("phase.nearNew",
                "phase.waxingCrescent", "phase.nearFirstQuarter",
                "phase.waxingGibbous", "phase.nearFull", "phase.waningGibbous",
                "phase.nearLastQuarter", "phase.waningCrescent");

        @Override
        public Body body() {
            return Body.MOON;
        }

        /** Wide enough for every column, so the phase and lit side are seen. */
        @Override
        public int preferredWidth() {
            int total = 0;
            for (int w : WIDTHS) {
                total += w;
            }
            return total + 20;
        }

        @Override
        public String stem() {
            return "moontable";
        }

        @Override
        public String prefix() {
            return "moon";
        }

        @Override
        public List<String> columns() {
            return COLUMNS;
        }

        @Override
        public int[] widths() {
            return WIDTHS.clone();
        }

        /** The phase's word in the language. */
        public static String phaseWord(Phase phase, SolarTableWords w) {
            return w.say(PHASE_KEYS.get(phase.ordinal()));
        }

        /** The compass point's word in the language. */
        public static String compassWord(CompassPoint point, SolarTableWords w) {
            return w.say("compass." + point.name());
        }

        /** The lit side: the angle and its compass word, or why not. */
        public static String litSide(MoonObservation o, SolarTableWords w) {
            if (o.brightLimbConditioning() == LimbConditioning.NEAR_NEW_OR_FULL) {
                return w.say(o.phase() == Phase.NEAR_NEW
                        ? "litside.undefined.new" : "litside.undefined.full");
            }
            return w.say("litside.pattern",
                    MoonTableFormat.positionAngle(o.brightLimbAngleDegrees()),
                    compassWord(o.brightLimbCompassPoint(), w));
        }

        /** The elongation with the side's letter in the language. */
        public static String elongation(MoonObservation o, SolarTableWords w) {
            return w.n(MoonTableFormat.elongation(o.elongationDegrees(),
                    w.say(o.side() == Side.EAST_OF_SUN ? "side.east" : "side.west")));
        }

        @Override
        public String cell(Observation observation, int column, SolarTableWords w) {
            MoonObservation o = (MoonObservation) observation;
            return switch (column) {
                case 1 -> w.n(SunTableFormat.hms(o.astrometricJ2000()));
                case 2 -> SunTableFormat.dms(o.astrometricJ2000());
                case 3 -> SunTableFormat.altitude(
                        w.n(SunTableFormat.degrees(o.horizontal().altitudeDegrees())),
                        o.horizontal().altitudeDegrees(), w.say("below"));
                case 4 -> w.n(SunTableFormat.degrees(o.horizontal().azimuthDegrees()));
                case 5 -> MoonTableFormat.kilometres(o.distanceKm());
                case 6 -> w.n(SunTableFormat.minutesSeconds(o.angularDiameterArcseconds()));
                case 7 -> w.n(MoonTableFormat.percent(o.illuminatedFraction()));
                case 8 -> phaseWord(o.phase(), w);
                case 9 -> elongation(o, w);
                case 10 -> litSide(o, w);
                default -> throw new IndexOutOfBoundsException(column);
            };
        }
    }
}
