package juranometria.tool;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The bookmark document proposed by the #497 study, as executable
 * study code: the versioned schema, a strict reader that keeps what it
 * cannot use, and a canonical writer, so that every example in
 * {@code docs/studies/bookmarks/} is proved to round-trip byte for
 * byte. Study code only - it lives in the tool package and no
 * application class uses it; the production service is built after the
 * owner's ruling.
 *
 * <p>The document is UTF-8 JSON written canonically: two-space
 * indentation, keys in the order this class writes them, one value per
 * line, LF line ends, one trailing LF, non-ASCII text written as
 * itself and only {@code "}, {@code \} and control characters escaped.
 * Numbers keep the exact text they were read with, so a value read is
 * a value written.
 */
public final class BookmarkDocument {

    /** The document's own name for itself; anything else is not ours. */
    public static final String FORMAT = "juranometria-bookmarks";

    /** The one schema version this study defines. */
    public static final int VERSION = 1;

    private BookmarkDocument() {
    }

    // ------------------------------------------------------------------
    // The generic JSON layer: lossless, ordered, strict.
    // ------------------------------------------------------------------

    /** A JSON value, keeping key order and the exact text of numbers. */
    public sealed interface Json permits JObject, JArray, JString, JNumber, JBool, JNull {
    }

    /** An object; keys in the order read or written. */
    public record JObject(Map<String, Json> members) implements Json {
        public JObject {
            members = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(members));
        }
    }

    /** An array. */
    public record JArray(List<Json> items) implements Json {
        public JArray {
            items = List.copyOf(items);
        }
    }

    /** A string. */
    public record JString(String value) implements Json {
    }

    /** A number, held as the exact text it was read or written with. */
    public record JNumber(String lexeme) implements Json {
        static JNumber of(double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("not a JSON number: " + value);
            }
            // The shortest text that reads back as the same double.
            String text = Double.toString(value);
            if (text.contains("E")) {
                text = new BigDecimal(text).toPlainString();
            }
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return new JNumber(text);
        }

        double asDouble() {
            return Double.parseDouble(lexeme);
        }
    }

    /** true or false. */
    public record JBool(boolean value) implements Json {
    }

    /** null. */
    public record JNull() implements Json {
    }

    /** A document that is not well-formed JSON, with where it failed. */
    public static final class Malformed extends Exception {
        private final int offset;

        Malformed(String message, int offset) {
            super(message + " at character " + offset);
            this.offset = offset;
        }

        public int offset() {
            return offset;
        }
    }

    /** Strict RFC 8259 reading: no comments, no trailing commas, no byte-order mark. */
    public static Json parse(String text) throws Malformed {
        if (text.startsWith("\uFEFF")) {
            throw new Malformed("a byte-order mark before the document", 0);
        }
        Reader reader = new Reader(text);
        reader.space();
        Json value = reader.value(0);
        reader.space();
        if (reader.at != text.length()) {
            throw new Malformed("text after the document", reader.at);
        }
        return value;
    }

    private static final class Reader {
        private static final int DEEPEST = 64;
        private final String text;
        private int at;

        Reader(String text) {
            this.text = text;
        }

        void space() {
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    at++;
                } else {
                    return;
                }
            }
        }

        Json value(int depth) throws Malformed {
            if (depth > DEEPEST) {
                throw new Malformed("nested deeper than " + DEEPEST, at);
            }
            if (at >= text.length()) {
                throw new Malformed("the document ends early", at);
            }
            char c = text.charAt(at);
            switch (c) {
                case '{':
                    return object(depth);
                case '[':
                    return array(depth);
                case '"':
                    return new JString(string());
                case 't':
                    word("true");
                    return new JBool(true);
                case 'f':
                    word("false");
                    return new JBool(false);
                case 'n':
                    word("null");
                    return new JNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    throw new Malformed("unexpected '" + c + "'", at);
            }
        }

        private void word(String w) throws Malformed {
            if (!text.startsWith(w, at)) {
                throw new Malformed("expected " + w, at);
            }
            at += w.length();
        }

        private JObject object(int depth) throws Malformed {
            at++;
            Map<String, Json> members = new LinkedHashMap<>();
            space();
            if (at < text.length() && text.charAt(at) == '}') {
                at++;
                return new JObject(members);
            }
            while (true) {
                space();
                if (at >= text.length() || text.charAt(at) != '"') {
                    throw new Malformed("expected a key", at);
                }
                int keyAt = at;
                String key = string();
                if (members.containsKey(key)) {
                    throw new Malformed("the key \"" + key + "\" twice", keyAt);
                }
                space();
                expect(':');
                space();
                members.put(key, value(depth + 1));
                space();
                if (at < text.length() && text.charAt(at) == ',') {
                    at++;
                    continue;
                }
                expect('}');
                return new JObject(members);
            }
        }

        private JArray array(int depth) throws Malformed {
            at++;
            List<Json> items = new ArrayList<>();
            space();
            if (at < text.length() && text.charAt(at) == ']') {
                at++;
                return new JArray(items);
            }
            while (true) {
                space();
                items.add(value(depth + 1));
                space();
                if (at < text.length() && text.charAt(at) == ',') {
                    at++;
                    continue;
                }
                expect(']');
                return new JArray(items);
            }
        }

        private void expect(char c) throws Malformed {
            if (at >= text.length() || text.charAt(at) != c) {
                throw new Malformed("expected '" + c + "'", at);
            }
            at++;
        }

        private String string() throws Malformed {
            at++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (at >= text.length()) {
                    throw new Malformed("a string never ends", at);
                }
                char c = text.charAt(at++);
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    throw new Malformed("a control character inside a string", at - 1);
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (at >= text.length()) {
                    throw new Malformed("an escape never ends", at);
                }
                char e = text.charAt(at++);
                switch (e) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > text.length()) {
                            throw new Malformed("a short \\u escape", at);
                        }
                        try {
                            out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        } catch (NumberFormatException bad) {
                            throw new Malformed("a bad \\u escape", at);
                        }
                        at += 4;
                    }
                    default -> throw new Malformed("an unknown escape \\" + e, at - 1);
                }
            }
        }

        private JNumber number() throws Malformed {
            int start = at;
            if (text.charAt(at) == '-') {
                at++;
            }
            if (at < text.length() && text.charAt(at) == '0') {
                at++;
            } else {
                digits(true);
            }
            if (at < text.length() && text.charAt(at) == '.') {
                at++;
                digits(true);
            }
            if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
                at++;
                if (at < text.length() && (text.charAt(at) == '+' || text.charAt(at) == '-')) {
                    at++;
                }
                digits(true);
            }
            return new JNumber(text.substring(start, at));
        }

        private void digits(boolean required) throws Malformed {
            int start = at;
            while (at < text.length() && text.charAt(at) >= '0' && text.charAt(at) <= '9') {
                at++;
            }
            if (required && at == start) {
                throw new Malformed("expected a digit", at);
            }
        }
    }

    /** The canonical text of a value: what every library write produces. */
    public static String write(Json value) {
        StringBuilder out = new StringBuilder();
        write(value, 0, out);
        out.append('\n');
        return out.toString();
    }

    private static void write(Json value, int indent, StringBuilder out) {
        switch (value) {
            case JObject o -> {
                if (o.members().isEmpty()) {
                    out.append("{}");
                    return;
                }
                out.append("{\n");
                int i = 0;
                for (Map.Entry<String, Json> e : o.members().entrySet()) {
                    pad(indent + 1, out);
                    quote(e.getKey(), out);
                    out.append(": ");
                    write(e.getValue(), indent + 1, out);
                    out.append(++i < o.members().size() ? ",\n" : "\n");
                }
                pad(indent, out);
                out.append('}');
            }
            case JArray a -> {
                if (a.items().isEmpty()) {
                    out.append("[]");
                    return;
                }
                out.append("[\n");
                for (int i = 0; i < a.items().size(); i++) {
                    pad(indent + 1, out);
                    write(a.items().get(i), indent + 1, out);
                    out.append(i + 1 < a.items().size() ? ",\n" : "\n");
                }
                pad(indent, out);
                out.append(']');
            }
            case JString s -> quote(s.value(), out);
            case JNumber n -> out.append(n.lexeme());
            case JBool b -> out.append(b.value());
            case JNull n -> out.append("null");
        }
    }

    private static void pad(int indent, StringBuilder out) {
        out.append("  ".repeat(indent));
    }

    private static void quote(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------
    // The bookmark domain, schema version 1.
    // ------------------------------------------------------------------

    /** What kind of thing a subject identity names. */
    public enum SubjectKind {
        /** A Tycho-2 star, by its catalogue identity ("TYC 1266-1416-1"). */
        STAR("star"),
        /** An OpenNGC object, by its catalogue identity ("NGC 598"). */
        DEEP_SKY("deep-sky"),
        /** A Solar System body or moon, by the atlas's body identity ("moon", "jovian.io"). */
        BODY("body");

        private final String token;

        SubjectKind(String token) {
            this.token = token;
        }

        public String token() {
            return token;
        }

        static Optional<SubjectKind> of(String token) {
            for (SubjectKind k : values()) {
                if (k.token.equals(token)) {
                    return Optional.of(k);
                }
            }
            return Optional.empty();
        }
    }

    /** A subject: a kind and a language-independent identity, never display text. */
    public record Subject(SubjectKind kind, String id) {
        public Subject {
            if (kind == null || id == null || id.isBlank()) {
                throw new IllegalArgumentException("a subject is a kind and an identity");
            }
        }
    }

    /** Why a bookmark carries an instant. */
    public enum TimePurpose {
        /** The instant belongs to the astronomical configuration. */
        EVENT("event"),
        /** The instant is the user's confirmed time of an observation. */
        OBSERVATION("observation");

        private final String token;

        TimePurpose(String token) {
            this.token = token;
        }

        public String token() {
            return token;
        }

        static Optional<TimePurpose> of(String token) {
            for (TimePurpose p : values()) {
                if (p.token.equals(token)) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }
    }

    /** An instant and why it is kept. */
    public record ChartTime(Instant instant, TimePurpose purpose) {
    }

    /** A place on the Earth, as the chart context holds one. */
    public record Place(double latitudeDegrees, double eastLongitudeDegrees) {
    }

    /**
     * The geometric view: the centre (ICRS/J2000, degrees) and the field
     * width, one of the atlas's steps. Projection and orientation are
     * not stored: the atlas derives the projection from the field and
     * draws celestial north up, east left on every page.
     */
    public record View(double raDegrees, double decDegrees, double fieldWidthDegrees) {
    }

    /** A tree node. */
    public sealed interface Node permits Folder, Bookmark, Kept {
        String id();
    }

    /** A user folder; its children in user order. */
    public record Folder(String id, String title, List<Node> children) implements Node {
        public Folder {
            children = List.copyOf(children);
        }
    }

    /**
     * A bookmark: a saved view, optionally enriched. The primary subject
     * is held by identity, independent of its position in the ordered
     * subjects; empty means opening changes no selection.
     */
    public record Bookmark(String id, String title, View view, List<Subject> subjects,
                           Optional<Subject> primary, Optional<ChartTime> time,
                           Optional<Place> place, Optional<String> note) implements Node {
        public Bookmark {
            subjects = List.copyOf(subjects);
        }
    }

    /**
     * A record this reader could not use, kept exactly as read so that
     * nothing is lost: written back unchanged, reported, never opened.
     */
    public record Kept(String id, Json raw, List<String> reasons) implements Node {
        public Kept {
            reasons = List.copyOf(reasons);
        }
    }

    /** The whole library: the permanent root's children, and who last wrote it. */
    public record Library(String writtenBy, List<Node> root) {
        public Library {
            root = List.copyOf(root);
        }
    }

    /** What reading a document came to. */
    public sealed interface Read permits Usable, NotOurs, Newer {
    }

    /** A version-1 library, possibly with kept records. */
    public record Usable(Library library) implements Read {
    }

    /** Not a bookmark document at all, or one whose envelope is damaged. */
    public record NotOurs(String reason) implements Read {
    }

    /** A later schema: never reinterpreted and never written. */
    public record Newer(int version, String writtenBy) implements Read {
    }

    /** The field steps the atlas draws (ChartViewState's ladder and its closer fields). */
    private static final Set<Double> FIELD_STEPS = Set.of(180.0, 120.0, 90.0, 60.0, 42.0,
            36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0, 0.5, 0.25);

    /** Reads a parsed document into the domain; never throws on content. */
    public static Read read(Json document) {
        if (!(document instanceof JObject envelope)) {
            return new NotOurs("the document is not an object");
        }
        Json format = envelope.members().get("format");
        if (!(format instanceof JString f) || !FORMAT.equals(f.value())) {
            return new NotOurs("the document does not say it is \"" + FORMAT + "\"");
        }
        Json version = envelope.members().get("version");
        if (!(version instanceof JNumber v) || !v.lexeme().matches("[1-9][0-9]{0,8}")) {
            return new NotOurs("the document's version is not a whole number");
        }
        int n = Integer.parseInt(v.lexeme());
        String writtenBy = envelope.members().get("writtenBy") instanceof JString w
                ? w.value() : "";
        if (n > VERSION) {
            return new Newer(n, writtenBy);
        }
        if (!(envelope.members().get("root") instanceof JArray root)) {
            return new NotOurs("the document has no root list");
        }
        Set<String> seen = new java.util.HashSet<>();
        return new Usable(new Library(writtenBy, nodes(root, seen)));
    }

    private static List<Node> nodes(JArray array, Set<String> seen) {
        List<Node> out = new ArrayList<>();
        for (Json item : array.items()) {
            out.add(node(item, seen));
        }
        return out;
    }

    private static Node node(Json item, Set<String> seen) {
        List<String> why = new ArrayList<>();
        if (!(item instanceof JObject o)) {
            return new Kept("", item, List.of("a record that is not an object"));
        }
        Map<String, Json> m = o.members();
        String id = text(m, "id").orElse("");
        if (id.isBlank()) {
            why.add("no id");
        } else if (!seen.add(id)) {
            why.add("the id " + id + " twice");
        }
        String type = text(m, "type").orElse("");
        if (type.equals("folder")) {
            unknownKeys(m, Set.of("type", "id", "title", "children"), why);
            Optional<String> title = text(m, "title");
            if (title.isEmpty() || title.get().isBlank()) {
                why.add("a folder with no title");
            }
            if (!(m.get("children") instanceof JArray kids)) {
                why.add("a folder with no children list");
                return new Kept(id, item, why);
            }
            if (!why.isEmpty()) {
                return new Kept(id, item, why);
            }
            return new Folder(id, title.get(), nodes(kids, seen));
        }
        if (!type.equals("bookmark")) {
            why.add(type.isEmpty() ? "no type" : "an unknown type \"" + type + "\"");
            return new Kept(id, item, why);
        }
        unknownKeys(m, Set.of("type", "id", "title", "view", "subjects", "primary",
                "time", "place", "note"), why);
        Optional<String> title = text(m, "title");
        if (title.isEmpty() || title.get().isBlank()) {
            why.add("a bookmark with no title");
        }
        View view = view(m.get("view"), why);
        List<Subject> subjects = new ArrayList<>();
        if (m.containsKey("subjects")) {
            if (m.get("subjects") instanceof JArray list) {
                Set<Subject> once = new java.util.HashSet<>();
                for (Json s : list.items()) {
                    subject(s, why).ifPresent(subject -> {
                        if (!once.add(subject)) {
                            why.add("the subject " + subject.kind().token() + " "
                                    + subject.id() + " twice");
                        }
                        subjects.add(subject);
                    });
                }
            } else {
                why.add("subjects that are not a list");
            }
        }
        Optional<Subject> primary = Optional.empty();
        if (m.containsKey("primary")) {
            primary = subject(m.get("primary"), why);
            if (primary.isPresent() && !subjects.contains(primary.get())) {
                why.add("a primary that is not one of the subjects");
            }
        }
        Optional<ChartTime> time = Optional.empty();
        if (m.containsKey("time")) {
            time = time(m.get("time"), why);
        }
        Optional<Place> place = Optional.empty();
        if (m.containsKey("place")) {
            place = place(m.get("place"), why);
        }
        Optional<String> note = Optional.empty();
        if (m.containsKey("note")) {
            if (m.get("note") instanceof JString s) {
                note = Optional.of(s.value());
            } else {
                why.add("a note that is not text");
            }
        }
        if (!why.isEmpty() || view == null) {
            return new Kept(id, item, why);
        }
        return new Bookmark(id, title.get(), view, subjects, primary, time, place, note);
    }

    private static void unknownKeys(Map<String, Json> m, Set<String> known, List<String> why) {
        for (String key : m.keySet()) {
            if (!known.contains(key)) {
                why.add("an unknown field \"" + key + "\"");
            }
        }
    }

    private static Optional<String> text(Map<String, Json> m, String key) {
        return m.get(key) instanceof JString s ? Optional.of(s.value()) : Optional.empty();
    }

    private static Optional<Double> number(Map<String, Json> m, String key) {
        return m.get(key) instanceof JNumber n ? Optional.of(n.asDouble()) : Optional.empty();
    }

    private static View view(Json json, List<String> why) {
        if (!(json instanceof JObject o)) {
            why.add("no view");
            return null;
        }
        Map<String, Json> m = o.members();
        unknownKeys(m, Set.of("ra", "dec", "field"), why);
        Optional<Double> ra = number(m, "ra");
        Optional<Double> dec = number(m, "dec");
        Optional<Double> field = number(m, "field");
        if (ra.isEmpty() || ra.get() < 0 || ra.get() >= 360) {
            why.add("a view whose right ascension is not in [0°, 360°)");
        }
        if (dec.isEmpty() || dec.get() < -90 || dec.get() > 90) {
            why.add("a view whose declination is not in [-90°, 90°]");
        }
        if (field.isEmpty() || !FIELD_STEPS.contains(field.get())) {
            why.add("a view whose field is not one of the atlas's steps");
        }
        if (ra.isEmpty() || dec.isEmpty() || field.isEmpty()) {
            return null;
        }
        return new View(ra.get(), dec.get(), field.get());
    }

    private static Optional<Subject> subject(Json json, List<String> why) {
        if (!(json instanceof JObject o)) {
            why.add("a subject that is not an object");
            return Optional.empty();
        }
        Map<String, Json> m = o.members();
        unknownKeys(m, Set.of("kind", "id"), why);
        Optional<SubjectKind> kind = text(m, "kind").flatMap(SubjectKind::of);
        Optional<String> id = text(m, "id").filter(s -> !s.isBlank());
        if (kind.isEmpty() || id.isEmpty()) {
            why.add("a subject without a known kind and an identity");
            return Optional.empty();
        }
        return Optional.of(new Subject(kind.get(), id.get()));
    }

    private static Optional<ChartTime> time(Json json, List<String> why) {
        if (!(json instanceof JObject o)) {
            why.add("a time that is not an object");
            return Optional.empty();
        }
        Map<String, Json> m = o.members();
        unknownKeys(m, Set.of("instant", "purpose"), why);
        Optional<TimePurpose> purpose = text(m, "purpose").flatMap(TimePurpose::of);
        Optional<String> instant = text(m, "instant");
        if (purpose.isEmpty()) {
            why.add("a time without the purpose \"event\" or \"observation\"");
        }
        if (instant.isEmpty() || !instant.get().endsWith("Z")) {
            why.add("a time whose instant is not stated in UTC");
            return Optional.empty();
        }
        try {
            Instant at = Instant.parse(instant.get());
            return purpose.map(p -> new ChartTime(at, p));
        } catch (DateTimeParseException bad) {
            why.add("a time whose instant cannot be read");
            return Optional.empty();
        }
    }

    private static Optional<Place> place(Json json, List<String> why) {
        if (!(json instanceof JObject o)) {
            why.add("a place that is not an object");
            return Optional.empty();
        }
        Map<String, Json> m = o.members();
        unknownKeys(m, Set.of("latitude", "eastLongitude"), why);
        Optional<Double> lat = number(m, "latitude");
        Optional<Double> lon = number(m, "eastLongitude");
        if (lat.isEmpty() || lat.get() < -90 || lat.get() > 90) {
            why.add("a place whose latitude is not in [-90°, 90°]");
        }
        if (lon.isEmpty() || lon.get() < -180 || lon.get() > 180) {
            why.add("a place whose east longitude is not in [-180°, 180°]");
        }
        if (lat.isEmpty() || lon.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Place(lat.get(), lon.get()));
    }

    /** The domain as the canonical JSON value every write produces. */
    public static Json json(Library library) {
        Map<String, Json> envelope = new LinkedHashMap<>();
        envelope.put("format", new JString(FORMAT));
        envelope.put("version", new JNumber(Integer.toString(VERSION)));
        envelope.put("writtenBy", new JString(library.writtenBy()));
        envelope.put("root", nodesJson(library.root()));
        return new JObject(envelope);
    }

    private static JArray nodesJson(List<Node> nodes) {
        List<Json> out = new ArrayList<>();
        for (Node n : nodes) {
            out.add(nodeJson(n));
        }
        return new JArray(out);
    }

    private static Json nodeJson(Node node) {
        return switch (node) {
            case Kept k -> k.raw();
            case Folder f -> {
                Map<String, Json> m = new LinkedHashMap<>();
                m.put("type", new JString("folder"));
                m.put("id", new JString(f.id()));
                m.put("title", new JString(f.title()));
                m.put("children", nodesJson(f.children()));
                yield new JObject(m);
            }
            case Bookmark b -> {
                Map<String, Json> m = new LinkedHashMap<>();
                m.put("type", new JString("bookmark"));
                m.put("id", new JString(b.id()));
                m.put("title", new JString(b.title()));
                Map<String, Json> v = new LinkedHashMap<>();
                v.put("ra", JNumber.of(b.view().raDegrees()));
                v.put("dec", JNumber.of(b.view().decDegrees()));
                v.put("field", JNumber.of(b.view().fieldWidthDegrees()));
                m.put("view", new JObject(v));
                if (!b.subjects().isEmpty()) {
                    List<Json> s = new ArrayList<>();
                    for (Subject subject : b.subjects()) {
                        s.add(subjectJson(subject));
                    }
                    m.put("subjects", new JArray(s));
                }
                b.primary().ifPresent(p -> m.put("primary", subjectJson(p)));
                b.time().ifPresent(t -> {
                    Map<String, Json> tm = new LinkedHashMap<>();
                    tm.put("instant", new JString(t.instant().toString()));
                    tm.put("purpose", new JString(t.purpose().token()));
                    m.put("time", new JObject(tm));
                });
                b.place().ifPresent(p -> {
                    Map<String, Json> pm = new LinkedHashMap<>();
                    pm.put("latitude", JNumber.of(p.latitudeDegrees()));
                    pm.put("eastLongitude", JNumber.of(p.eastLongitudeDegrees()));
                    m.put("place", new JObject(pm));
                });
                b.note().ifPresent(n -> m.put("note", new JString(n)));
                yield new JObject(m);
            }
        };
    }

    private static Json subjectJson(Subject s) {
        Map<String, Json> m = new LinkedHashMap<>();
        m.put("kind", new JString(s.kind().token()));
        m.put("id", new JString(s.id()));
        return new JObject(m);
    }

    /** Every bookmark in the library, depth first in user order. */
    public static List<Bookmark> bookmarks(List<Node> nodes) {
        List<Bookmark> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n instanceof Bookmark b) {
                out.add(b);
            } else if (n instanceof Folder f) {
                out.addAll(bookmarks(f.children()));
            }
        }
        return out;
    }

    /** Every kept record in the library, depth first. */
    public static List<Kept> kept(List<Node> nodes) {
        List<Kept> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n instanceof Kept k) {
                out.add(k);
            } else if (n instanceof Folder f) {
                out.addAll(kept(f.children()));
            }
        }
        return out;
    }

    /** Every folder in the library, depth first. */
    public static List<Folder> folders(List<Node> nodes) {
        List<Folder> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n instanceof Folder f) {
                out.add(f);
                out.addAll(folders(f.children()));
            }
        }
        return out;
    }
}
