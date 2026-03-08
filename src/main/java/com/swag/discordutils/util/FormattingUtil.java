package com.swag.discordutils.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FormattingUtil {

    // Matches &#RRGGBB (common shorthand in Minecraft chat plugins)
    private static final Pattern HEX_HASH = Pattern.compile("&#([A-Fa-f0-9]{6})");
    // Matches <#RRGGBB> (MiniMessage hex)
    private static final Pattern HEX_TAG = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    // Matches <color:#RRGGBB> (MiniMessage long form)
    private static final Pattern HEX_COLOR_TAG = Pattern.compile("<color:#([A-Fa-f0-9]{6})>");

    // Strip §-based color/format codes (including hex §x§R§R§G§G§B§B)
    private static final Pattern STRIP_SECTION = Pattern.compile("§x(§[0-9a-fA-F]){6}|§[0-9a-fklmnorxA-FK-OR-X]");
    // Strip &-based color/format codes (common in Vault prefixes: &a, &c, &l, etc.)
    private static final Pattern STRIP_AMPERSAND = Pattern.compile("&[0-9a-fklmnorxA-FK-OR-X]");

    // Closing MiniMessage tags -> reset
    private static final Pattern CLOSING_TAG = Pattern.compile("</(bold|italic|underlined|underline|strikethrough|obfuscated|color|aqua|black|blue|dark_aqua|dark_blue|dark_gray|dark_green|dark_purple|dark_red|gold|gray|green|light_purple|red|white|yellow)>");

    // Named color tags: order matters (longer names first to avoid partial matches)
    private static final Map<String, String> COLORS = new LinkedHashMap<>();
    private static final Map<String, String> FORMATS = new LinkedHashMap<>();

    static {
        COLORS.put("dark_blue",    "§1");
        COLORS.put("dark_green",   "§2");
        COLORS.put("dark_aqua",    "§3");
        COLORS.put("dark_red",     "§4");
        COLORS.put("dark_purple",  "§5");
        COLORS.put("dark_gray",    "§8");
        COLORS.put("light_purple", "§d");
        COLORS.put("black",        "§0");
        COLORS.put("gold",         "§6");
        COLORS.put("gray",         "§7");
        COLORS.put("blue",         "§9");
        COLORS.put("green",        "§a");
        COLORS.put("aqua",         "§b");
        COLORS.put("red",          "§c");
        COLORS.put("yellow",       "§e");
        COLORS.put("white",        "§f");

        FORMATS.put("bold",          "§l");
        FORMATS.put("italic",        "§o");
        FORMATS.put("underlined",    "§n");
        FORMATS.put("underline",     "§n");
        FORMATS.put("strikethrough", "§m");
        FORMATS.put("obfuscated",    "§k");
        FORMATS.put("reset",         "§r");
    }

    /**
     * Parses MiniMessage-style tags, &#RRGGBB hex, and & color codes into
     * Minecraft §-based formatting. Used for displaying formatted text in-game.
     */
    public static String parseMiniMessage(String text) {
        if (text == null) return null;

        // Convert & codes (e.g. &6, &l) -> § codes
        text = translateAmpersand(text);

        // Convert &#RRGGBB
        text = HEX_HASH.matcher(text).replaceAll(m -> hexToSection(m.group(1)));

        // Convert <#RRGGBB>
        text = HEX_TAG.matcher(text).replaceAll(m -> hexToSection(m.group(1)));

        // Convert <color:#RRGGBB>
        text = HEX_COLOR_TAG.matcher(text).replaceAll(m -> hexToSection(m.group(1)));

        // Closing tags -> §r
        text = CLOSING_TAG.matcher(text).replaceAll("§r");

        // Named color and format opening tags
        for (Map.Entry<String, String> e : COLORS.entrySet()) {
            text = text.replace("<" + e.getKey() + ">", e.getValue());
        }
        for (Map.Entry<String, String> e : FORMATS.entrySet()) {
            text = text.replace("<" + e.getKey() + ">", e.getValue());
        }

        // Remove any leftover unknown MiniMessage tags
        text = text.replaceAll("</?[a-zA-Z_:#0-9]+>", "");

        return text;
    }

    /**
     * Strips all Minecraft formatting (§ codes, MiniMessage tags, &#hex, &codes)
     * from text. Used to produce clean plain text for Discord.
     */
    public static String stripFormatting(String text) {
        if (text == null) return null;
        // Strip §x hex codes first (longer pattern), then regular § codes
        text = STRIP_SECTION.matcher(text).replaceAll("");
        // Strip & color codes (e.g. from Vault prefixes: &a, &c, &l)
        text = STRIP_AMPERSAND.matcher(text).replaceAll("");
        // Strip any remaining MiniMessage/hex tags
        text = HEX_HASH.matcher(text).replaceAll("");
        text = HEX_TAG.matcher(text).replaceAll("");
        text = HEX_COLOR_TAG.matcher(text).replaceAll("");
        text = text.replaceAll("</?[a-zA-Z_:#0-9]+>", "");
        return text;
    }

    /**
     * Converts Discord markdown to Minecraft §-based formatting.
     * **bold** -> §lbold§r, *italic* -> §oitalic§r, etc.
     */
    public static String discordToMinecraft(String text) {
        if (text == null) return null;
        // Order matters: process ** before * to avoid mismatches
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "§l$1§r");
        text = text.replaceAll("~~(.+?)~~",          "§m$1§r");
        text = text.replaceAll("__(.+?)__",          "§n$1§r");
        text = text.replaceAll("\\*(.+?)\\*",        "§o$1§r");
        text = text.replaceAll("_(.+?)_",            "§o$1§r");
        text = text.replaceAll("`(.+?)`",            "§7$1§r");
        return text;
    }

    // Converts a 6-character hex string to §x§R§R§G§G§B§B Spigot format
    private static String hexToSection(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    // Translates & color codes to § but only for recognized codes (0-9, a-f, k-r)
    private static String translateAmpersand(String text) {
        if (!text.contains("&")) return text;
        char[] chars = text.toCharArray();
        StringBuilder out = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char next = Character.toLowerCase(chars[i + 1]);
                if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f') || (next >= 'k' && next <= 'r')) {
                    out.append('§').append(chars[i + 1]);
                    i++;
                    continue;
                }
            }
            out.append(chars[i]);
        }
        return out.toString();
    }
}
