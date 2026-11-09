package juranometria.bookmark;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The version-1 bookmark document (#681; schema in
 * docs/decisions/bookmarks.md, section 3): reading a parsed document into
 * the library, and the library into the canonical document. Reading never
 * throws on content: a record it cannot use is kept with its reasons, a
 * later version is reported and never reinterpreted, and anything that is
 * not ours is said to be not ours.
 */
public final class BookmarkDocument {

    /** The document's own name for itself. */
    public static final String FORMAT = "juranometria-bookmarks";

    /** The schema version this application reads and writes. */
    public static final int VERSION = 1;

    private BookmarkDocument() {
    }

    /** What reading a document came to. */
    public sealed interface Read {
    }

    /** A version-1 library, possibly holding records needing attention. */
    public record Usable(Library library) implements Read {
    }

    /** Not a bookmark document, or one whose envelope is damaged. */
    public record NotOurs(String reason) implements Read {
    }

    /** A later schema: never reinterpreted, never written. */
    public record Newer(int version, String writtenBy) implements Read {
    }

    /** Parses and reads text; malformed JSON is reported as not ours. */
    public static Read read(String text) {
        try {
            return read(Json.parse(text));
        } catch (Json.Malformed malformed) {
            return new NotOurs(malformed.getMessage());
        }
    }

    /** Reads a parsed document. */
    public static Read read(Json.Value document) {
        if (!(document instanceof Json.Obj envelope)) {
            return new NotOurs("the document is not an object");
        }
        Map<String, Json.Value> m = envelope.members();
        if (!(m.get("format") instanceof Json.Str f) || !FORMAT.equals(f.value())) {
            return new NotOurs("the document does not say it is \"" + FORMAT + "\"");
        }
        if (!(m.get("version") instanceof Json.Num v) || !v.lexeme().matches("[1-9][0-9]{0,8}")) {
            return new NotOurs("the document's version is not a whole number");
        }
        int version = Integer.parseInt(v.lexeme());
        String writtenBy = m.get("writtenBy") instanceof Json.Str w ? w.value() : "";
        if (version > VERSION) {
            return new Newer(version, writtenBy);
        }
        if (!(m.get("root") instanceof Json.Arr root)) {
            return new NotOurs("the document has no root list");
        }
        return new Usable(new Library(writtenBy, nodes(root, new HashSet<>())));
    }

    /** The canonical text of a library. */
    public static String text(Library library) {
        return Json.write(json(library));
    }

    /** The library as the canonical document value. */
    public static Json.Value json(Library library) {
        Map<String, Json.Value> envelope = new LinkedHashMap<>();
        envelope.put("format", new Json.Str(FORMAT));
        envelope.put("version", new Json.Num(Integer.toString(VERSION)));
        envelope.put("writtenBy", new Json.Str(library.writtenBy()));
        envelope.put("root", nodesJson(library.root()));
        return new Json.Obj(envelope);
    }

    private static List<Node> nodes(Json.Arr array, Set<String> seen) {
        List<Node> out = new ArrayList<>();
        for (Json.Value item : array.items()) {
            out.add(node(item, seen));
        }
        return out;
    }

    private static Node node(Json.Value item, Set<String> seen) {
        List<String> why = new ArrayList<>();
        if (!(item instanceof Json.Obj o)) {
            return new Node.Kept("", item, List.of("a record that is not an object"));
        }
        Map<String, Json.Value> m = o.members();
        String id = text(m, "id").orElse("");
        if (id.isBlank()) {
            why.add("no id");
        } else if (!seen.add(id)) {
            why.add("the id " + id + " twice");
        }
        String type = text(m, "type").orElse("");
        if (type.equals("folder")) {
            unknownKeys(m, Set.of("type", "id", "title", "children"), why);
            Optional<String> title = text(m, "title").filter(t -> !t.isBlank());
            if (title.isEmpty()) {
                why.add("a folder with no title");
            }
            if (!(m.get("children") instanceof Json.Arr kids)) {
                why.add("a folder with no children list");
                return new Node.Kept(id, item, why);
            }
            if (!why.isEmpty()) {
                return new Node.Kept(id, item, why);
            }
            return new Node.Folder(id, title.get(), nodes(kids, seen));
        }
        if (!type.equals("bookmark")) {
            why.add(type.isEmpty() ? "no type" : "an unknown type \"" + type + "\"");
            return new Node.Kept(id, item, why);
        }
        unknownKeys(m, Set.of("type", "id", "title", "view", "subjects", "primary",
                "time", "place", "note"), why);
        Optional<String> title = text(m, "title").filter(t -> !t.isBlank());
        if (title.isEmpty()) {
            why.add("a bookmark with no title");
        }
        Optional<SavedView> view = view(m.get("view"), why);
        List<Subject> subjects = new ArrayList<>();
        if (m.containsKey("subjects")) {
            if (m.get("subjects") instanceof Json.Arr list) {
                for (Json.Value s : list.items()) {
                    subject(s, why).ifPresent(subject -> {
                        if (subjects.contains(subject)) {
                            why.add("the subject " + subject.kind().token() + " "
                                    + subject.id() + " twice");
                        } else {
                            subjects.add(subject);
                        }
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
        Optional<ChartTime> time = m.containsKey("time") ? time(m.get("time"), why) : Optional.empty();
        Optional<Place> place = m.containsKey("place") ? place(m.get("place"), why) : Optional.empty();
        Optional<String> note = Optional.empty();
        if (m.containsKey("note")) {
            if (m.get("note") instanceof Json.Str s) {
                note = Optional.of(s.value());
            } else {
                why.add("a note that is not text");
            }
        }
        // A whole-sky bookmark keeps its chart time and place (#687,
        // R-11): both derive the page, and a record without them could
        // not be opened as it was saved.
        if (view.isPresent() && view.get().wholeSky()
                && (time.isEmpty() || place.isEmpty())) {
            why.add("a whole-sky bookmark without its chart time and place");
        }
        if (!why.isEmpty() || view.isEmpty()) {
            return new Node.Kept(id, item, why);
        }
        return new Node.Bookmark(id, title.get(), view.get(), subjects, primary, time, place, note);
    }

    private static void unknownKeys(Map<String, Json.Value> m, Set<String> known, List<String> why) {
        for (String key : m.keySet()) {
            if (!known.contains(key)) {
                why.add("an unknown field \"" + key + "\"");
            }
        }
    }

    private static Optional<String> text(Map<String, Json.Value> m, String key) {
        return m.get(key) instanceof Json.Str s ? Optional.of(s.value()) : Optional.empty();
    }

    private static Optional<Double> number(Map<String, Json.Value> m, String key) {
        return m.get(key) instanceof Json.Num n ? Optional.of(n.asDouble()) : Optional.empty();
    }

    private static Optional<SavedView> view(Json.Value json, List<String> why) {
        if (!(json instanceof Json.Obj o)) {
            why.add("no view");
            return Optional.empty();
        }
        Map<String, Json.Value> m = o.members();
        unknownKeys(m, Set.of("ra", "dec", "field", "mode"), why);
        if (m.containsKey("mode")) {
            // The whole sky (#687, B-2): the one optional member, and
            // the only one such a view carries.
            Optional<String> mode = text(m, "mode");
            boolean good = true;
            if (mode.isEmpty() || !mode.get().equals(SavedView.WHOLE_SKY_MODE)) {
                why.add("a view whose mode is not \"" + SavedView.WHOLE_SKY_MODE + "\"");
                good = false;
            }
            if (m.containsKey("ra") || m.containsKey("dec") || m.containsKey("field")) {
                why.add("a whole-sky view with a centre or a field");
                good = false;
            }
            return good ? Optional.of(SavedView.wholeSkyView()) : Optional.empty();
        }
        Optional<Double> ra = number(m, "ra");
        Optional<Double> dec = number(m, "dec");
        Optional<Double> field = number(m, "field");
        boolean good = true;
        if (ra.isEmpty() || !(ra.get() >= 0 && ra.get() < 360)) {
            why.add("a view whose right ascension is not in [0°, 360°)");
            good = false;
        }
        if (dec.isEmpty() || !(dec.get() >= -90 && dec.get() <= 90)) {
            why.add("a view whose declination is not in [-90°, 90°]");
            good = false;
        }
        if (field.isEmpty() || !SavedView.FIELD_STEPS.contains(field.get())) {
            why.add("a view whose field is not one of the atlas's steps");
            good = false;
        }
        return good ? Optional.of(new SavedView(ra.get(), dec.get(), field.get())) : Optional.empty();
    }

    private static Optional<Subject> subject(Json.Value json, List<String> why) {
        if (!(json instanceof Json.Obj o)) {
            why.add("a subject that is not an object");
            return Optional.empty();
        }
        Map<String, Json.Value> m = o.members();
        unknownKeys(m, Set.of("kind", "id"), why);
        Optional<Subject.Kind> kind = text(m, "kind").flatMap(Subject.Kind::of);
        Optional<String> id = text(m, "id").filter(s -> !s.isBlank());
        if (kind.isEmpty() || id.isEmpty()) {
            why.add("a subject without a known kind and an identity");
            return Optional.empty();
        }
        return Optional.of(new Subject(kind.get(), id.get()));
    }

    private static Optional<ChartTime> time(Json.Value json, List<String> why) {
        if (!(json instanceof Json.Obj o)) {
            why.add("a time that is not an object");
            return Optional.empty();
        }
        Map<String, Json.Value> m = o.members();
        unknownKeys(m, Set.of("instant", "purpose"), why);
        Optional<ChartTime.Purpose> purpose = text(m, "purpose").flatMap(ChartTime.Purpose::of);
        if (purpose.isEmpty()) {
            why.add("a time without the purpose \"event\" or \"observation\"");
        }
        Optional<String> instant = text(m, "instant");
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

    private static Optional<Place> place(Json.Value json, List<String> why) {
        if (!(json instanceof Json.Obj o)) {
            why.add("a place that is not an object");
            return Optional.empty();
        }
        Map<String, Json.Value> m = o.members();
        unknownKeys(m, Set.of("latitude", "eastLongitude"), why);
        Optional<Double> lat = number(m, "latitude");
        Optional<Double> lon = number(m, "eastLongitude");
        boolean good = true;
        if (lat.isEmpty() || !(lat.get() >= -90 && lat.get() <= 90)) {
            why.add("a place whose latitude is not in [-90°, 90°]");
            good = false;
        }
        if (lon.isEmpty() || !(lon.get() >= -180 && lon.get() <= 180)) {
            why.add("a place whose east longitude is not in [-180°, 180°]");
            good = false;
        }
        return good ? Optional.of(new Place(lat.get(), lon.get())) : Optional.empty();
    }

    private static Json.Arr nodesJson(List<Node> nodes) {
        List<Json.Value> out = new ArrayList<>();
        for (Node n : nodes) {
            out.add(nodeJson(n));
        }
        return new Json.Arr(out);
    }

    private static Json.Value nodeJson(Node node) {
        return switch (node) {
            case Node.Kept k -> k.raw();
            case Node.Folder f -> {
                Map<String, Json.Value> m = new LinkedHashMap<>();
                m.put("type", new Json.Str("folder"));
                m.put("id", new Json.Str(f.id()));
                m.put("title", new Json.Str(f.title()));
                m.put("children", nodesJson(f.children()));
                yield new Json.Obj(m);
            }
            case Node.Bookmark b -> {
                Map<String, Json.Value> m = new LinkedHashMap<>();
                m.put("type", new Json.Str("bookmark"));
                m.put("id", new Json.Str(b.id()));
                m.put("title", new Json.Str(b.title()));
                Map<String, Json.Value> v = new LinkedHashMap<>();
                if (b.view().wholeSky()) {
                    // The one optional member (#687, B-2); no centre
                    // and no field are written, because none is stored.
                    v.put("mode", new Json.Str(SavedView.WHOLE_SKY_MODE));
                } else {
                    v.put("ra", Json.Num.of(b.view().raDegrees()));
                    v.put("dec", Json.Num.of(b.view().decDegrees()));
                    v.put("field", Json.Num.of(b.view().fieldWidthDegrees()));
                }
                m.put("view", new Json.Obj(v));
                if (!b.subjects().isEmpty()) {
                    List<Json.Value> s = new ArrayList<>();
                    for (Subject subject : b.subjects()) {
                        s.add(subjectJson(subject));
                    }
                    m.put("subjects", new Json.Arr(s));
                }
                b.primary().ifPresent(p -> m.put("primary", subjectJson(p)));
                b.time().ifPresent(t -> {
                    Map<String, Json.Value> tm = new LinkedHashMap<>();
                    tm.put("instant", new Json.Str(t.instant().toString()));
                    tm.put("purpose", new Json.Str(t.purpose().token()));
                    m.put("time", new Json.Obj(tm));
                });
                b.place().ifPresent(p -> {
                    Map<String, Json.Value> pm = new LinkedHashMap<>();
                    pm.put("latitude", Json.Num.of(p.latitudeDegrees()));
                    pm.put("eastLongitude", Json.Num.of(p.eastLongitudeDegrees()));
                    m.put("place", new Json.Obj(pm));
                });
                b.note().ifPresent(n -> m.put("note", new Json.Str(n)));
                yield new Json.Obj(m);
            }
        };
    }

    private static Json.Value subjectJson(Subject s) {
        Map<String, Json.Value> m = new LinkedHashMap<>();
        m.put("kind", new Json.Str(s.kind().token()));
        m.put("id", new Json.Str(s.id()));
        return new Json.Obj(m);
    }
}
