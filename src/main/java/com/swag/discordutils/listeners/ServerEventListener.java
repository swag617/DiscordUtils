package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.util.FormattingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;

import java.util.List;

public class ServerEventListener implements Listener {

    private final DiscordUtils plugin;

    public ServerEventListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("join-leave.enabled", true)) {
            plugin.getDiscordBot().sendJoinLeaveEmbed(
                    event.getPlayer().getName(), event.getPlayer().getUniqueId(), true);
        }

        if (plugin.getLinkManager() != null) {
            plugin.getLinkManager().syncRoleAsync(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("join-leave.enabled", true)) return;
        plugin.getDiscordBot().sendJoinLeaveEmbed(
                event.getPlayer().getName(), event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        String plain = FormattingUtil.stripFormatting(event.getMessage());
        if (plain == null || plain.isBlank()) return;

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] Broadcast caught: " + plain);
        }

        checkAfk(plain);
        checkServerMessage(plain);
    }

    private void checkAfk(String plain) {
        if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;

        String lower = plain.toLowerCase();

        boolean goingAfk = lower.contains("is now afk")
                || lower.contains("has gone afk")
                || lower.contains("went afk");

        boolean returning = lower.contains("is no longer afk")
                || lower.contains("is back from afk")
                || lower.contains("returned from afk")
                || lower.contains("no longer away");

        if (!goingAfk && !returning) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plain.contains(player.getName())) {
                plugin.getDiscordBot().sendAfkEmbed(
                        player.getName(), player.getUniqueId(), goingAfk);
                return;
            }
        }
    }

    private void checkServerMessage(String plain) {
        if (!plugin.getConfig().getBoolean("server-messages.enabled", true)) return;

        List<String> prefixes = plugin.getConfig().getStringList("server-messages.relay-prefixes");
        if (prefixes.isEmpty()) return;

        boolean relay = prefixes.contains("*") ||
                prefixes.stream().anyMatch(plain::startsWith);
        if (!relay) return;

        String format = plugin.getConfig().getString("server-messages.discord-format", ":game_die: {message}");
        plugin.getDiscordBot().sendMessage(format.replace("{message}", plain));
    }
}
