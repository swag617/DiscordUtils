package com.swag.discordutils.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a Minecraft-style item tooltip as a PNG image.
 * Uses Java2D — no external dependencies required.
 * The output mimics the in-game tooltip: dark background, purple gradient border,
 * white item name, aqua enchantments, purple italic lore.
 */
public class ItemTooltipRenderer {

    // 2x scale for a sharper image
    private static final int S = 2;

    // Padding inside the border (pixels, post-scale)
    private static final int PAD = 5 * S;

    // Height of each text line
    private static final int LINE_H = 11 * S;

    // Extra gap between the item name and the rest of the tooltip
    private static final int NAME_GAP = 3 * S;

    // Minecraft tooltip colors
    private static final Color BG          = new Color(0x10, 0x00, 0x10, 245);
    private static final Color BORDER_DARK = new Color(0x14, 0x00, 0x26);
    private static final Color BORDER_TOP  = new Color(0x50, 0x00, 0xFF);
    private static final Color BORDER_BOT  = new Color(0x28, 0x00, 0x7F);

    private static final Color C_NAME    = new Color(0xFF, 0xFF, 0xFF);       // white  — unenchanted
    private static final Color C_NAMED   = new Color(0xFF, 0xFF, 0x55);       // yellow — custom display name
    private static final Color C_ENCHANT = new Color(0x7C, 0xFC, 0xFF);       // aqua
    private static final Color C_LORE    = new Color(0xBE, 0x55, 0xFF);       // purple
    private static final Color C_GRAY    = new Color(0x80, 0x80, 0x80);       // gray — material type sub-label

    public static byte[] render(ItemStack item) throws IOException {
        List<TooltipLine> lines = buildLines(item);

        Font plain  = new Font(Font.MONOSPACED, Font.PLAIN,  9 * S);
        Font italic = plain.deriveFont(Font.ITALIC);

<<<<<<< HEAD
=======
        // Measure text dimensions using a scratch image
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scratch.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fmPlain   = sg.getFont() == plain ? sg.getFontMetrics() : sg.getFontMetrics(plain);
        FontMetrics fmItalic  = sg.getFontMetrics(italic);
        sg.dispose();

        int maxText = 0;
        for (TooltipLine l : lines) {
            int w = (l.italic ? fmItalic : fmPlain).stringWidth(l.text);
            if (w > maxText) maxText = w;
        }

<<<<<<< HEAD
        int innerW = maxText + PAD * 2;
        int innerH = calcHeight(lines);

=======
        // Inner dimensions (inside the 2px border on each side)
        int innerW = maxText + PAD * 2;
        int innerH = calcHeight(lines);

        // Total image = inner + 2px outer border + 2px inner border on each side
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        int imgW = innerW + 4;
        int imgH = innerH + 4;

        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // --- Background ---
        g.setColor(BG);
        g.fillRect(2, 2, imgW - 4, imgH - 4);

        // --- Outer border (1px, very dark) ---
        g.setColor(BORDER_DARK);
        g.drawRect(0, 0, imgW - 1, imgH - 1);

        // --- Inner border (1px gradient, left + right sides) ---
        for (int y = 1; y < imgH - 1; y++) {
            float t = (float)(y - 1) / Math.max(1, imgH - 2);
            Color c = lerp(BORDER_TOP, BORDER_BOT, t);
            g.setColor(c);
            g.drawLine(1, y, 1, y);
            g.drawLine(imgW - 2, y, imgW - 2, y);
        }
<<<<<<< HEAD
=======
        // Inner border top/bottom
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        g.setColor(BORDER_TOP);
        g.drawLine(1, 1, imgW - 2, 1);
        g.setColor(BORDER_BOT);
        g.drawLine(1, imgH - 2, imgW - 2, imgH - 2);

        // --- Text ---
        int textX = 2 + PAD;
        int ascent = fmPlain.getAscent();
        int textY = 2 + PAD + ascent;

        for (int i = 0; i < lines.size(); i++) {
            TooltipLine l = lines.get(i);
            g.setColor(l.color);
            g.setFont(l.italic ? italic : plain);
            g.drawString(l.text, textX, textY);
            textY += LINE_H;
            // Add extra gap after the item name
            if (i == 0 && lines.size() > 1) textY += NAME_GAP;
        }

        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    // -------------------------------------------------------------------------

    private static List<TooltipLine> buildLines(ItemStack item) {
        List<TooltipLine> lines = new ArrayList<>();
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

<<<<<<< HEAD
=======
        // --- Item name ---
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        boolean hasCustomName = meta != null && meta.hasDisplayName();
        String name;
        Color nameColor;
        if (hasCustomName) {
            name = FormattingUtil.stripFormatting(meta.getDisplayName()).trim();
            nameColor = C_NAMED;
        } else {
            name = formatName(item.getType().name());
<<<<<<< HEAD
=======
            // White if unenchanted, yellow if enchanted (vanilla behaviour)
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
            nameColor = item.getEnchantments().isEmpty() ? C_NAME : C_NAMED;
        }
        if (item.getAmount() > 1) name = item.getAmount() + "x " + name;
        lines.add(new TooltipLine(name, nameColor, false));

<<<<<<< HEAD
=======
        // --- Enchantments ---
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            String eName = formatName(e.getKey().getKey().getKey());
            lines.add(new TooltipLine(eName + " " + roman(e.getValue()), C_ENCHANT, false));
        }

<<<<<<< HEAD
=======
        // --- Lore ---
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String loreLine : lore) {
                    String stripped = FormattingUtil.stripFormatting(loreLine).trim();
                    if (!stripped.isEmpty()) lines.add(new TooltipLine(stripped, C_LORE, true));
                }
            }
        }

<<<<<<< HEAD
=======
        // --- Sub-label: material type (shown when item has a custom name) ---
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        if (hasCustomName) {
            lines.add(new TooltipLine(formatName(item.getType().name()), C_GRAY, true));
        }

        return lines;
    }

    private static int calcHeight(List<TooltipLine> lines) {
        int h = PAD * 2 + lines.size() * LINE_H;
        if (lines.size() > 1) h += NAME_GAP;
        return h;
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }

    /** DIAMOND_SWORD → Diamond Sword */
    private static String formatName(String key) {
        String[] parts = key.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static String roman(int n) {
        return switch (n) {
            case 1  -> "I";
            case 2  -> "II";
            case 3  -> "III";
            case 4  -> "IV";
            case 5  -> "V";
            case 6  -> "VI";
            case 7  -> "VII";
            case 8  -> "VIII";
            case 9  -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private record TooltipLine(String text, Color color, boolean italic) {}
}
