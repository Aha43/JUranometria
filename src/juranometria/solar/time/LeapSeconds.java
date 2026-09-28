package juranometria.solar.time;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The IERS leap-second record, read from the bundled, unmodified
 * {@code Leap_Second.dat} the IERS Earth Orientation Centre publishes
 * with Bulletin C.
 *
 * <p>Two things are read and nothing is assumed. The entries: from
 * each date the file names, the whole-second value of TAI − UTC that
 * holds until the next. And the file's own expiry, which is the
 * boundary of what the record vouches for: Bulletin C is issued twice
 * a year and states until when no further leap second will be
 * introduced. Beyond that date the file says nothing, and neither
 * does this class; the caller decides what an estimate looks like.
 * An expiry is a data-validity boundary, not a claim about the future
 * of leap seconds (issue #398, R-b).
 */
public final class LeapSeconds {

    /** One entry: TAI − UTC is {@code offset} whole seconds from {@code from}. */
    public record Entry(LocalDate from, int offset) {
    }

    private static final Pattern ROW = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?)\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4})\\s+(\\d+)\\s*$");

    private static final Pattern EXPIRY = Pattern.compile(
            "File expires on\\s+(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})");

    private static final DateTimeFormatter EXPIRY_DATE =
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);

    private final List<Entry> entries;
    private final LocalDate expires;

    private LeapSeconds(List<Entry> entries, LocalDate expires) {
        this.entries = entries;
        this.expires = expires;
    }

    /** Parses the IERS file as published. */
    public static LeapSeconds parse(InputStream in) throws IOException {
        return parse(new String(in.readAllBytes(), StandardCharsets.US_ASCII));
    }

    static LeapSeconds parse(String text) throws IOException {
        List<Entry> entries = new ArrayList<>();
        LocalDate expires = null;
        for (String line : text.split("\n")) {
            Matcher row = ROW.matcher(line);
            if (row.matches()) {
                entries.add(new Entry(LocalDate.of(
                        Integer.parseInt(row.group(4)),
                        Integer.parseInt(row.group(3)),
                        Integer.parseInt(row.group(2))),
                        Integer.parseInt(row.group(5))));
                continue;
            }
            Matcher expiry = EXPIRY.matcher(line);
            if (expiry.find()) {
                expires = LocalDate.parse(expiry.group(1), EXPIRY_DATE);
            }
        }
        if (entries.isEmpty()) {
            throw new IOException("the leap-second file holds no entries");
        }
        if (expires == null) {
            throw new IOException("the leap-second file states no expiry;"
                    + " without one the record's validity is unknown");
        }
        for (int i = 1; i < entries.size(); i++) {
            Entry before = entries.get(i - 1);
            Entry after = entries.get(i);
            if (!after.from.isAfter(before.from)
                    || after.offset != before.offset + 1) {
                throw new IOException("leap-second entries must step by one"
                        + " second in date order; " + before + " then " + after);
            }
        }
        return new LeapSeconds(Collections.unmodifiableList(entries), expires);
    }

    /** The entries, in date order. */
    public List<Entry> entries() {
        return entries;
    }

    /** The first date the record covers (1972-01-01 in the IERS file). */
    public LocalDate first() {
        return entries.get(0).from;
    }

    /** The date the file states it expires: exact knowledge ends here. */
    public LocalDate expires() {
        return expires;
    }

    /**
     * TAI − UTC in whole seconds on a date the record covers.
     *
     * @throws IllegalArgumentException before the first entry or on or
     *         after the expiry, where the record vouches for nothing
     */
    public int taiMinusUtc(LocalDate date) {
        if (date.isBefore(first())) {
            throw new IllegalArgumentException(date + " is before the"
                    + " leap-second record, which begins " + first());
        }
        if (!date.isBefore(expires)) {
            throw new IllegalArgumentException(date + " is on or after the"
                    + " record's stated expiry " + expires
                    + "; beyond it TAI - UTC is not known");
        }
        int offset = entries.get(0).offset;
        for (Entry entry : entries) {
            if (!date.isBefore(entry.from)) {
                offset = entry.offset;
            }
        }
        return offset;
    }
}
