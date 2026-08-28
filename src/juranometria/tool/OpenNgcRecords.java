package juranometria.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Row parsing for the OpenNGC semicolon-separated CSV files. The header
 * row names the columns; sexagesimal J2000 coordinates become decimal
 * degrees. Type mapping and filtering are the caller's concern.
 */
final class OpenNgcRecords {

    /** One raw OpenNGC object; optional values are NaN when absent. */
    record DsoRow(String id, List<String> aliases, String type,
                  double raDegrees, double decDegrees,
                  double majorAxisArcmin, double minorAxisArcmin,
                  double positionAngleDegrees, double vmag, double bmag) {
    }

    private OpenNgcRecords() {
    }

    static Map<String, Integer> headerIndex(String headerLine) {
        String[] names = headerLine.split(";", -1);
        Map<String, Integer> index = new java.util.HashMap<>();
        for (int i = 0; i < names.length; i++) {
            index.put(names[i], i);
        }
        for (String required : new String[] {
                "Name", "Type", "RA", "Dec", "MajAx", "MinAx", "PosAng",
                "B-Mag", "V-Mag", "M", "NGC", "IC", "Common names"}) {
            if (!index.containsKey(required)) {
                throw new IllegalArgumentException(
                        "OpenNGC header lacks required column " + required);
            }
        }
        return index;
    }

    static DsoRow fromLine(String line, Map<String, Integer> header) {
        String[] fields = line.split(";", -1);
        if (fields.length < header.size()) {
            throw new IllegalArgumentException("malformed OpenNGC line: " + line);
        }
        String name = fields[header.get("Name")].trim();
        String type = fields[header.get("Type")].trim();
        String ra = fields[header.get("RA")].trim();
        String dec = fields[header.get("Dec")].trim();
        if (name.isEmpty() || type.isEmpty()) {
            throw new IllegalArgumentException("OpenNGC line lacks name or type: " + line);
        }
        double raDegrees = ra.isEmpty() ? Double.NaN : raDegrees(ra, line);
        double decDegrees = dec.isEmpty() ? Double.NaN : decDegrees(dec, line);
        return new DsoRow(
                normalizeName(name),
                aliases(fields, header),
                type,
                raDegrees, decDegrees,
                optional(fields[header.get("MajAx")]),
                optional(fields[header.get("MinAx")]),
                optional(fields[header.get("PosAng")]),
                optional(fields[header.get("V-Mag")]),
                optional(fields[header.get("B-Mag")]));
    }

    /** NGC0224 becomes NGC 224; suffixes such as " NED01" or "A" survive. */
    static String normalizeName(String name) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("^(NGC|IC)0*(\\d+)(.*)$").matcher(name);
        if (matcher.matches()) {
            return matcher.group(1) + " " + matcher.group(2) + matcher.group(3);
        }
        return name;
    }

    private static List<String> aliases(String[] fields, Map<String, Integer> header) {
        List<String> aliases = new ArrayList<>();
        String messier = fields[header.get("M")].trim();
        if (!messier.isEmpty()) {
            aliases.add("M " + Integer.parseInt(messier));
        }
        for (String column : new String[] {"NGC", "IC"}) {
            String crossRef = fields[header.get(column)].trim();
            if (!crossRef.isEmpty()) {
                aliases.add(normalizeName(column + crossRef));
            }
        }
        for (String common : fields[header.get("Common names")].split(",")) {
            String trimmed = common.trim();
            if (!trimmed.isEmpty()) {
                aliases.add(trimmed);
            }
        }
        return aliases;
    }

    private static double optional(String field) {
        String trimmed = field.trim();
        return trimmed.isEmpty() ? Double.NaN : Double.parseDouble(trimmed);
    }

    private static double raDegrees(String sexagesimal, String line) {
        String[] parts = sexagesimal.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("malformed RA in OpenNGC line: " + line);
        }
        return (Integer.parseInt(parts[0])
                + Integer.parseInt(parts[1]) / 60.0
                + Double.parseDouble(parts[2]) / 3600.0) * 15.0;
    }

    private static double decDegrees(String sexagesimal, String line) {
        String[] parts = sexagesimal.substring(1).split(":");
        if (parts.length != 3 || (!sexagesimal.startsWith("+") && !sexagesimal.startsWith("-"))) {
            throw new IllegalArgumentException("malformed Dec in OpenNGC line: " + line);
        }
        double magnitude = Integer.parseInt(parts[0])
                + Integer.parseInt(parts[1]) / 60.0
                + Double.parseDouble(parts[2]) / 3600.0;
        return sexagesimal.startsWith("-") ? -magnitude : magnitude;
    }
}
