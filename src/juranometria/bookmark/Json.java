package juranometria.bookmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The bookmark document's own JSON (#681, ruled C-1 on #497): a strict
 * reader and a canonical writer, owned by the application, with no
 * dependency. Strict means RFC 8259 and nothing more - no comments, no
 * trailing commas, no byte-order mark, no duplicate keys. Lossless means
 * keys keep their order and numbers keep the exact text they were read
 * with, so a record this version cannot use is written back unchanged.
 *
 * <p>The canonical text: two-space indentation, one value per line,
 * {@code ": "} after a key, LF line ends and one trailing LF, non-ASCII
 * written as itself, and only {@code "}, {@code \} and control
 * characters escaped. Every write of the library is this text.
 */
public final class Json {

    private Json() {
    }

    /** A JSON value. */
    public sealed interface Value permits Obj, Arr, Str, Num, Bool, Null {
    }

    /** An object, its keys in order. */
    public record Obj(Map<String, Value> members) implements Value {
        public Obj {
            members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
        }
    }

    /** An array. */
    public record Arr(List<Value> items) implements Value {
        public Arr {
            items = List.copyOf(items);
        }
    }

    /** A string. */
    public record Str(String value) implements Value {
        public Str {
            if (value == null) {
                throw new IllegalArgumentException("a string has a value");
            }
        }
    }

    /** A number, held as its exact text. */
    public record Num(String lexeme) implements Value {

        /** The shortest text that reads back as the same double. */
        public static Num of(double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("not a JSON number: " + value);
            }
            String text = Double.toString(value);
            if (text.contains("E")) {
                text = new BigDecimal(text).toPlainString();
            }
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return new Num(text);
        }

        public double asDouble() {
            return Double.parseDouble(lexeme);
        }
    }

    /** true or false. */
    public record Bool(boolean value) implements Value {
    }

    /** null. */
    public record Null() implements Value {
    }

    /** Text that is not one well-formed JSON value, and where it failed. */
    public static final class Malformed extends Exception {
        private static final long serialVersionUID = 1L;
        private final int offset;

        Malformed(String reason, int offset) {
            super(reason + " at character " + offset);
            this.offset = offset;
        }

        /** The character offset at which reading stopped. */
        public int offset() {
            return offset;
        }
    }

    /** Reads one value, strictly. */
    public static Value parse(String text) throws Malformed {
        if (text.startsWith("﻿")) {
            throw new Malformed("a byte-order mark before the document", 0);
        }
        Reader reader = new Reader(text);
        reader.space();
        Value value = reader.value(0);
        reader.space();
        if (reader.at != text.length()) {
            throw new Malformed("text after the document", reader.at);
        }
        return value;
    }

    /** The canonical text of a value. */
    public static String write(Value value) {
        StringBuilder out = new StringBuilder();
        write(value, 0, out);
        out.append('\n');
        return out.toString();
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

        Value value(int depth) throws Malformed {
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
                    return new Str(string());
                case 't':
                    word("true");
                    return new Bool(true);
                case 'f':
                    word("false");
                    return new Bool(false);
                case 'n':
                    word("null");
                    return new Null();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return number();
                    }
                    throw new Malformed("an unexpected character", at);
            }
        }

        private void word(String w) throws Malformed {
            if (!text.startsWith(w, at)) {
                throw new Malformed("expected " + w, at);
            }
            at += w.length();
        }

        private Obj object(int depth) throws Malformed {
            at++;
            Map<String, Value> members = new LinkedHashMap<>();
            space();
            if (at < text.length() && text.charAt(at) == '}') {
                at++;
                return new Obj(members);
            }
            while (true) {
                space();
                if (at >= text.length() || text.charAt(at) != '"') {
                    throw new Malformed("expected a key", at);
                }
                int keyAt = at;
                String key = string();
                if (members.containsKey(key)) {
                    throw new Malformed("a key repeated", keyAt);
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
                return new Obj(members);
            }
        }

        private Arr array(int depth) throws Malformed {
            at++;
            List<Value> items = new ArrayList<>();
            space();
            if (at < text.length() && text.charAt(at) == ']') {
                at++;
                return new Arr(items);
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
                return new Arr(items);
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
                    default -> throw new Malformed("an unknown escape", at - 1);
                }
            }
        }

        private Num number() throws Malformed {
            int start = at;
            if (text.charAt(at) == '-') {
                at++;
            }
            if (at < text.length() && text.charAt(at) == '0') {
                at++;
            } else {
                digits();
            }
            if (at < text.length() && text.charAt(at) == '.') {
                at++;
                digits();
            }
            if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
                at++;
                if (at < text.length() && (text.charAt(at) == '+' || text.charAt(at) == '-')) {
                    at++;
                }
                digits();
            }
            return new Num(text.substring(start, at));
        }

        private void digits() throws Malformed {
            int start = at;
            while (at < text.length() && text.charAt(at) >= '0' && text.charAt(at) <= '9') {
                at++;
            }
            if (at == start) {
                throw new Malformed("expected a digit", at);
            }
        }
    }

    private static void write(Value value, int indent, StringBuilder out) {
        switch (value) {
            case Obj o -> {
                if (o.members().isEmpty()) {
                    out.append("{}");
                    return;
                }
                out.append("{\n");
                int i = 0;
                for (Map.Entry<String, Value> e : o.members().entrySet()) {
                    pad(indent + 1, out);
                    quote(e.getKey(), out);
                    out.append(": ");
                    write(e.getValue(), indent + 1, out);
                    out.append(++i < o.members().size() ? ",\n" : "\n");
                }
                pad(indent, out);
                out.append('}');
            }
            case Arr a -> {
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
            case Str s -> quote(s.value(), out);
            case Num n -> out.append(n.lexeme());
            case Bool b -> out.append(b.value());
            case Null n -> out.append("null");
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
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
