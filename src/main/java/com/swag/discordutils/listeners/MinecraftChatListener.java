package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.util.FormattingUtil;
import com.swag.discordutils.util.ItemTooltipRenderer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class MinecraftChatListener implements Listener {

    private final DiscordUtils plugin;

    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)\\[item]");

    public MinecraftChatListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage();

        if (plugin.getConfig().getBoolean("formatting.parse-minimessage", true)
                && player.hasPermission("discordutils.formatting")) {
            String formatted = FormattingUtil.parseMiniMessage(raw);
            event.setMessage(formatted);
            raw = formatted;
        }

        if (!plugin.getConfig().getBoolean("discord-chat.enabled", true)) return;

        String clean = plugin.getConfig().getBoolean("formatting.strip-for-discord", true)
                ? FormattingUtil.stripFormatting(raw)
                : raw;

        String rank = getRank(player);
        String authorDisplay = "[" + rank + "] " + player.getName();

        if (ITEM_PATTERN.matcher(clean).find()) {
            ItemStack held = player.getInventory().getItemInMainHand();

            String itemName = getItemName(held);
            String messageWithItem = ITEM_PATTERN.matcher(clean).replaceAll("[" + itemName + "]");

            String format = plugin.getConfig().getString("formatting.discord-send-format", "**[{rank}] {player}**: {message}");
            String discordMsg = format
                    .replace("{rank}", rank)
                    .replace("{player}", player.getName())
                    .replace("{message}", messageWithItem);

            if (held.getType() == Material.AIR) {
                plugin.getDiscordBot().sendMessage(discordMsg);
            } else {
                try {
                    byte[] tooltipImage = ItemTooltipRenderer.render(held);
                    plugin.getDiscordBot().sendItemEmbed(authorDisplay, discordMsg, tooltipImage);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to render item tooltip: " + e.getMessage());
                    plugin.getDiscordBot().sendMessage(discordMsg);
                }
            }
        } else {
            String format = plugin.getConfig().getString("formatting.discord-send-format", "**[{rank}] {player}**: {message}");
            String discordMsg = format
                    .replace("{rank}", rank)
                    .replace("{player}", player.getName())
                    .replace("{message}", clean);
            plugin.getDiscordBot().sendMessage(discordMsg);
        }
    }

    /** Returns just the display name or formatted material name of the item. */
    private String getItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "Air";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return FormattingUtil.stripFormatting(meta.getDisplayName()).trim();
        }
        return formatMaterialName(item.getType().name());
    }

    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private String getRank(Player player) {
        Chat chat = plugin.getVaultChat();
        if (chat == null) return "Player";

        String prefix = chat.getPlayerPrefix(player);
        if (prefix != null && !prefix.isEmpty()) {
            String stripped = FormattingUtil.stripFormatting(prefix).trim();
            // Strip surrounding brackets that LuckPerms prefixes often include (e.g. "[Flea]" → "Flea")
            if (stripped.startsWith("[") && stripped.endsWith("]")) {
                stripped = stripped.substring(1, stripped.length() - 1).trim();
            }
            if (!stripped.isEmpty()) return stripped;
        }

        String group = chat.getPrimaryGroup(player);
        if (group != null && !group.isEmpty()) return group;

        return "Player";
    }
}
