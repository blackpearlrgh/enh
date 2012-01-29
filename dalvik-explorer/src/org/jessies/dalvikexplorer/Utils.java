package org.jessies.dalvikexplorer;

import java.util.*;

public class Utils {
    // Original in salma-hayek "DebugMenu.java".
    public static String sortedStringOfMap(Map<String, String> hash) {
        StringBuilder builder = new StringBuilder();
        String[] keys = hash.keySet().toArray(new String[hash.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            builder.append(key + "=" + hash.get(key) + "\n");
        }
        return builder.toString();
    }
    
    // Original in salma-hayek "StringUtilities.java".
    public static String escapeForJava(CharSequence s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                result.append(String.format("\\u%04x", c)); // android-changed.
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    public static String offsetString(int ms, boolean showHours, boolean showMinutes) {
        int minutes = ms/1000/60;
        String result = "";
        if (showHours) {
            result += String.format(Locale.US, "%+03d:%02d", minutes / 60, Math.abs(minutes % 60));
        }
        if (showMinutes) {
            result += String.format(Locale.US, "%s%+d minutes%s", showHours ? " (" : "", minutes, showHours ? ")" : "");
        }
        return result;
    }
}