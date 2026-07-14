package com.swag.discordutils.web;

import java.util.List;

/**
 * Minimal hand-rolled JSON writer — same shape as SwagAC's web layer
 * (com.swag.chatsecurity.web.JsonUtil), kept dependency-free rather than pulling in
 * Gson/Jackson for these simple response shapes.
 */
public final class JsonUtil {

    private JsonUtil() {}

    /** Wraps an already-built JSON fragment so obj()/arr() embed it raw instead of re-escaping it as a string. */
    public record Raw(String json) {}

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static String str(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }

    private static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Raw r) return r.json();
        if (v instanceof String s) return str(s);
        if (v instanceof Boolean || v instanceof Number) return String.valueOf(v);
        return str(String.valueOf(v));
    }

    /** Builds a JSON object from alternating key/value pairs: {@code obj("a", 1, "b", "x")}. */
    public static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append(str(String.valueOf(kv[i]))).append(":").append(value(kv[i + 1]));
        }
        sb.append("}");
        return sb.toString();
    }

    /** Joins pre-built JSON fragments (objects/strings/etc.) into a JSON array. */
    public static String arr(List<String> rawJsonItems) {
        return "[" + String.join(",", rawJsonItems) + "]";
    }

    public static Raw raw(String json) {
        return new Raw(json);
    }
}
