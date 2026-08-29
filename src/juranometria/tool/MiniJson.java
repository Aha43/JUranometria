package juranometria.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately minimal JSON reader for the constellation study tool
 * (issue #63). Parses the subset the d3-celestial GeoJSON files use -
 * objects, arrays, strings, numbers, booleans, null - into plain Java
 * maps and lists. Tool-only: the application itself bundles no JSON,
 * and no general-purpose JSON dependency is wanted (architecture
 * decision since Sprint 1).
 */
final class MiniJson {

    private final String text;
    private int at;

    private MiniJson(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        MiniJson parser = new MiniJson(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (parser.at != text.length()) {
            throw new IllegalArgumentException(
                    "trailing content at offset " + parser.at);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object value) {
        return (List<Object>) value;
    }

    static double number(Object value) {
        return ((Number) value).doubleValue();
    }

    private Object value() {
        skipWhitespace();
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> object = new LinkedHashMap<>();
        at++; // {
        skipWhitespace();
        if (text.charAt(at) == '}') {
            at++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            object.put(key, value());
            skipWhitespace();
            char c = text.charAt(at);
            at++;
            if (c == '}') {
                return object;
            }
            if (c != ',') {
                throw new IllegalArgumentException(
                        "expected , or } at offset " + (at - 1));
            }
        }
    }

    private List<Object> readArray() {
        List<Object> array = new ArrayList<>();
        at++; // [
        skipWhitespace();
        if (text.charAt(at) == ']') {
            at++;
            return array;
        }
        while (true) {
            array.add(value());
            skipWhitespace();
            char c = text.charAt(at);
            at++;
            if (c == ']') {
                return array;
            }
            if (c != ',') {
                throw new IllegalArgumentException(
                        "expected , or ] at offset " + (at - 1));
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = text.charAt(at);
            at++;
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                char escape = text.charAt(at);
                at++;
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(
                                text.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw new IllegalArgumentException(
                            "unsupported escape \\" + escape);
                }
            } else {
                out.append(c);
            }
        }
    }

    private Object readNumber() {
        int start = at;
        while (at < text.length()
                && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
        return Double.parseDouble(text.substring(start, at));
    }

    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, at)) {
            throw new IllegalArgumentException(
                    "unexpected token at offset " + at);
        }
        at += literal.length();
        return value;
    }

    private void expect(char c) {
        if (text.charAt(at) != c) {
            throw new IllegalArgumentException(
                    "expected " + c + " at offset " + at);
        }
        at++;
    }

    private void skipWhitespace() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
            at++;
        }
    }
}
