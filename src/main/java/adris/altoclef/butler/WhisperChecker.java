package adris.altoclef.butler;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.csharpisbetter.Timer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhisperChecker {

    private static final Timer _repeatTimer = new Timer(0.1);

    private static String _lastMessage = null;

    /*
    public WhisperChecker() {
        // CHECK
        temp("{from} whispers to you: {message}",
                "FROM whispers to you: MESSAGE IS HERE BOYS! Do the thing.",
                "Boby whispers to you: Please eat potato bimmy",
                "Bobby: Bimot plz playe dee em see too",
                "FROM whispers to you: {to} WORKS HAHAHA {message} This is fucked {oof}");
        temp("\\[{from} -> {to}\\] {message}",
                "[From -> FuzzyC00kie] Valid message!",
                "[FuzzyC00kie -> From] Response, Invalid!!!!",
                "[]hello!!! Oof.");
    }
    private static void temp(String format, String ...messages) {
        String ourUsername = "FuzzyC00kie";
        for (String message : messages) {
            MessageResult check = tryParse(ourUsername, format, message);
            Debug.logInternal("CHECK: {" + format + "} with {" + message + "} => " + check);
        }
    }
     */

    public MessageResult receiveMessage(AltoClef mod, String ourUsername, String msg) {
        String foundMiddlePart = "";
        int index = -1;

        boolean duplicate = (msg.equals(_lastMessage));
        if (duplicate && !_repeatTimer.elapsed()) {
            _repeatTimer.reset();
            // It's probably an actual duplicate. IDK why we get those but yeah.
            return null;
        }


        for (String format : mod.getModSettings().getWhisperFormats()) {
            MessageResult check = tryParse(ourUsername, format, msg);
            if (check != null) {
                String user = check.from;
                String message = check.message;
                if (user == null || message == null) break;
                return check;
            }
        }

        return null;
    }

    private static MessageResult tryParse(String ourUsername, String whisperFormat, String message) {
        List<String> parts = new ArrayList<>(Arrays.asList("{from}", "{to}", "{message}"));

        // Sort by the order of appearance in whisperFormat.
        parts.sort(Comparator.comparingInt(whisperFormat::indexOf));
        parts.removeIf(part -> !whisperFormat.contains(part));

        String regexFormat = Pattern.quote(whisperFormat);
        for (String part : parts) {
            regexFormat = regexFormat.replace(part, "(.+)");
        }
        if (regexFormat.startsWith("\\Q")) regexFormat = regexFormat.substring("\\Q".length());
        if (regexFormat.endsWith("\\E")) regexFormat = regexFormat.substring(0, regexFormat.length() - "\\E".length());
        //Debug.logInternal("FORMAT: " + regexFormat + " tested on " + message);
        Pattern p = Pattern.compile(regexFormat);
        Matcher m = p.matcher(message);
        Map<String, String> values = new HashMap<>();
        if (m.matches()) {
            for (int i = 0; i < m.groupCount(); ++i) {
                // parts is sorted, so the order should lign up.
                if (i >= parts.size()) {
                    Debug.logError("Invalid whisper format parsing: " + whisperFormat + " for message: " + message);
                    break;
                }
                //Debug.logInternal("     GOT: " + parts.get(i) + " -> " + m.group(i + 1));
                values.put(parts.get(i), m.group(i + 1));
            }
        }

        if (values.containsKey("{to}")) {
            // Make sure the "to" target is us.
            String toUser = values.get("{to}");
            if (!toUser.equals(ourUsername)) {
                Debug.logInternal("Rejected message since it is sent to " + toUser + " and not " + ourUsername);
                return null;
            }
        }
        if (values.containsKey("{from}") && values.containsKey("{message}")) {
            MessageResult result = new MessageResult();
            result.from = values.get("{from}");
            result.message = values.get("{message}");
            return result;
        }
        return null;
    }

    public static class MessageResult {
        public String from;
        public String message;

        @Override
        public String toString() {
            return "MessageResult{" +
                    "from='" + from + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
