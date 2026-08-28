package juranometria.tool;

import java.util.Optional;

/**
 * Line parsing for the Tycho-2 main catalogue and supplement-1, following
 * the byte/field layout in the I/259 ReadMe. Records are pipe-separated.
 * Malformed lines throw; a record without a VT magnitude returns empty
 * (the one documented drop). Johnson V follows the ReadMe's own recipe:
 * V = VT - 0.090 * (BT - VT), with VT (or Hp in supplement-1) used
 * unchanged when BT is absent.
 */
final class Tycho2Records {

    /** One normalized star candidate; region and limit filtering is the caller's. */
    record StarRow(String id, double raDegrees, double decDegrees, double vmag,
                   boolean usedFallbackPosition, boolean usedVtWithoutBt,
                   boolean usedHpMagnitude) {
    }

    private Tycho2Records() {
    }

    /** Parses a main-catalogue line (32 pipe-separated fields). */
    static Optional<StarRow> fromMainLine(String line) {
        String[] fields = split(line, 26, "main");
        String vt = fields[19].trim();
        if (vt.isEmpty()) {
            return Optional.empty();
        }
        boolean fallback = fields[2].trim().isEmpty();
        double ra = number(fields[fallback ? 24 : 2], line);
        double dec = number(fields[fallback ? 25 : 3], line);
        String bt = fields[17].trim();
        double v = johnsonV(bt, vt);
        return Optional.of(new StarRow(tycId(fields[0], line), ra, dec, v,
                fallback, bt.isEmpty(), false));
    }

    /** Parses a supplement-1 line (positions are epoch J1991.25). */
    static Optional<StarRow> fromSupplementLine(String line) {
        String[] fields = split(line, 15, "supplement-1");
        String vt = fields[13].trim();
        if (vt.isEmpty()) {
            return Optional.empty();
        }
        double ra = number(fields[2], line);
        double dec = number(fields[3], line);
        String bt = fields[11].trim();
        boolean hp = "H".equals(fields[10].trim());
        double v = hp ? Double.parseDouble(vt) : johnsonV(bt, vt);
        return Optional.of(new StarRow(tycId(fields[0], line), ra, dec, v,
                false, !hp && bt.isEmpty(), hp));
    }

    private static double johnsonV(String bt, String vt) {
        double vtValue = Double.parseDouble(vt);
        if (bt.isEmpty()) {
            return vtValue;
        }
        return vtValue - 0.090 * (Double.parseDouble(bt) - vtValue);
    }

    private static String tycId(String field, String line) {
        String[] parts = field.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("malformed TYC identifier in line: " + line);
        }
        return "TYC " + Integer.parseInt(parts[0])
                + "-" + Integer.parseInt(parts[1])
                + "-" + Integer.parseInt(parts[2]);
    }

    private static String[] split(String line, int minimumFields, String file) {
        String[] fields = line.split("\\|", -1);
        if (fields.length < minimumFields) {
            throw new IllegalArgumentException(
                    "malformed Tycho-2 " + file + " line (" + fields.length
                            + " fields): " + line);
        }
        return fields;
    }

    private static double number(String field, String line) {
        String trimmed = field.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("missing required number in line: " + line);
        }
        return Double.parseDouble(trimmed);
    }
}
