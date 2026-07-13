package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.util.FormattingUtil;
import com.swag.discordutils.util.ItemTooltipRenderer;
import dev.pace.staffchat.StaffChat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.regex.Pattern;

public class StaffChatBridgeListener implements Listener {

    private final DiscordUtils plugin;
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)\\[item]");

    public StaffChatBridgeListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires after StaffChat has processed the chat event (MONITOR, ignoreCancelled=false).
     * If the event was cancelled by StaffChat and the player is locked into the staff channel,
     * we relay the message to Discord — including [item] tooltip embeds.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.isCancelled()) return;
        if (!plugin.getConfig().getBoolean("staff-chat.enabled", true)) return;

        Player player = event.getPlayer();
        StaffChat sc = StaffChat.getInstance();
        if (sc == null) return;

        String channelType = sc.lockMap.get(player.getUniqueId());
        String configuredType = plugin.getConfig().getString("staff-chat.channel-type", "staff");
        if (channelType == null || !channelType.equalsIgnoreCase(configuredType)) return;

        String raw = event.getMessage();
        String clean = FormattingUtil.stripFormatting(raw);

        String format = plugin.getConfig().getString(
                "staff-chat.discord-format", ":shield: **[Staff] {player}**: {message}");

        if (ITEM_PATTERN.matcher(clean).find()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            String itemName = getItemName(held);
            String messageWithItem = ITEM_PATTERN.matcher(clean).replaceAll("[" + itemName + "]");
            String discordMessage = format
                    .replace("{player}", player.getName())
                    .replace("{message}", messageWithItem);

            if (held.getType() == Material.AIR) {
                plugin.getDiscordBot().sendStaffChatText(discordMessage);
            } else {
                try {
                    byte[] tooltipImage = ItemTooltipRenderer.render(held);
                    plugin.getDiscordBot().sendStaffChatItemEmbed(discordMessage, tooltipImage);
                } catch (IOException e) {
                    plugin.getLogger().warning("[StaffChat] Failed to render item tooltip: " + e.getMessage());
                    plugin.getDiscordBot().sendStaffChatText(discordMessage);
                }
            }
        } else {
            String discordMessage = format
                    .replace("{player}", player.getName())
                    .replace("{message}", clean);
            plugin.getDiscordBot().sendStaffChatText(discordMessage);
        }
    }

    private String getItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "Air";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return FormattingUtil.stripFormatting(meta.getDisplayName()).trim();
        }
        return formatMaterialName(item.getType().name());
    }

    private String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
