import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tiny JSON helpers so we need no libraries. Good enough for flat objects. */
public class Json {
    /** "MS-101" -> "\"MS-101\"" (adds quotes, escapes inner quotes). */
    public static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Reads {"name":"value"} -> value, or null when the field is missing. */
    public static String stringField(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
