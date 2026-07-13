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
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class ServerEventListener implements Listener {

    private final DiscordUtils plugin;
    /** True when CMI is present — CmiAfkListener already handles AFK via dedicated events. */
    private final boolean cmiPresent;

    public ServerEventListener(DiscordUtils plugin) {
        this.plugin = plugin;
        this.cmiPresent = plugin.getServer().getPluginManager().getPlugin("CMI") != null;
    }

    // MONITOR priority so we see the event after other plugins have processed it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Skip vanished players — SuperVanish, PremiumVanish, CMI vanish, and most other
        // vanish plugins set a "vanished" metadata key on the player when they are hidden.
        if (isVanished(player)) return;

        if (plugin.getConfig().getBoolean("join-leave.enabled", true)) {
            plugin.getDiscordBot().sendJoinLeaveEmbed(
                    player.getName(), player.getUniqueId(), true);
        }

        // Sync Discord role to match current LuckPerms group (runs async)
        if (plugin.getLinkManager() != null) {
            plugin.getLinkManager().syncRoleAsync(player);
        }

        plugin.getDiscordBot().updatePresence();
        plugin.getDiscordBot().updateChatChannelTopic();
    }

    /**
     * Returns true if the player is currently vanished.
     *
     * Detection strategy (in order):
     *  1. "vanished" metadata key (boolean true) — used by SuperVanish, PremiumVanish,
     *     CMI vanish, EssentialsX vanish, and most other vanish plugins.
     *  2. CMI API check via CMIUser.isVanished() — catches edge cases where CMI has not
     *     yet set metadata at the moment MONITOR fires (rare race condition on first join).
     *
     * This intentionally avoids permission checks because permission nodes vary widely
     * between plugins and checking them would produce false positives.
     */
    private boolean isVanished(Player player) {
        // Check metadata key — the universal convention used by most vanish plugins
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }

        // CMI fallback — use its API directly if CMI is loaded
        if (cmiPresent) {
            try {
                com.Zrips.CMI.CMI cmi = com.Zrips.CMI.CMI.getInstance();
                if (cmi != null) {
                    com.Zrips.CMI.Containers.CMIUser user = cmi.getPlayerManager().getUser(player);
                    if (user != null && user.isVanished()) return true;
                }
            } catch (Exception e) {
                // CMI API unavailable or changed — silently ignore, metadata check was already done
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isVanished(event.getPlayer())) return;
        if (plugin.getConfig().getBoolean("join-leave.enabled", true)) {
            plugin.getDiscordBot().sendJoinLeaveEmbed(
                    event.getPlayer().getName(), event.getPlayer().getUniqueId(), false);
        }
        plugin.getDiscordBot().updatePresence();
        plugin.getDiscordBot().updateChatChannelTopic();
    }

    /**
     * BroadcastMessageEvent fires whenever Bukkit.broadcastMessage() is called — which is
     * how most mini-game, event, and AFK plugins send announcements.
     *
     * We handle two things here:
     *   1. Mini-game messages: relay if the text starts with a configured prefix.
     *   2. AFK messages: relay as a player-head embed if the text matches the AFK regex.
     *
     * Note: this does NOT fire for normal player chat (that goes through AsyncPlayerChatEvent)
     * so there is no risk of double-relaying player messages.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        // Strip §-codes once; both AFK and mini-game checks use the plain text
        String plain = FormattingUtil.stripFormatting(event.getMessage());
        if (plain == null || plain.isBlank()) return;

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] Broadcast caught: " + plain);
        }

        // When CMI is installed, CmiAfkListener handles AFK via its own events — skip here
        // to avoid sending duplicate embeds for every CMI AFK broadcast.
        if (!cmiPresent) checkAfk(plain);
        checkStaffChat(plain);
        checkServerMessage(plain);
    }

    private void checkAfk(String plain) {
        if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;

        String lower = plain.toLowerCase();

        // Keywords that indicate going AFK (covers CMI, EssentialsX, and most other plugins)
        boolean goingAfk = lower.contains("is now afk")
                || lower.contains("has gone afk")
                || lower.contains("went afk");

        // Keywords that indicate returning from AFK
        boolean returning = lower.contains("is no longer afk")
                || lower.contains("is back from afk")
                || lower.contains("returned from afk")
                || lower.contains("no longer away");

        if (!goingAfk && !returning) return;

        // Find which online player this message is about by scanning for their name.
        // Use a word-boundary regex to prevent "Dan" from matching "Danger" etc.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plain.matches("(?i).*\\b" + java.util.regex.Pattern.quote(player.getName()) + "\\b.*")) {
                plugin.getDiscordBot().sendAfkEmbed(
                        player.getName(), player.getUniqueId(), goingAfk);
                return;
            }
        }
    }

    private void checkStaffChat(String plain) {
        if (!plugin.getConfig().getBoolean("staff-chat.enabled", true)) return;

        List<String> prefixes = plugin.getConfig().getStringList("staff-chat.relay-prefixes");
        if (prefixes.isEmpty()) return;

        boolean relay = prefixes.contains("*") ||
                prefixes.stream().anyMatch(plain::startsWith);
        if (!relay) return;

        String format = plugin.getConfig().getString("staff-chat.discord-format", ":shield: {message}");
        plugin.getDiscordBot().sendStaffChatText(format.replace("{message}", plain));
    }

    private void checkServerMessage(String plain) {
        if (!plugin.getConfig().getBoolean("server-messages.enabled", true)) return;

        List<String> prefixes = plugin.getConfig().getStringList("server-messages.relay-prefixes");
        if (prefixes.isEmpty()) return;

        boolean relay = prefixes.contains("*") ||
                prefixes.stream().anyMatch(plain::startsWith);
        if (!relay) return;

        String format = plugin.getConfig().getString("server-messages.discord-format", ":game_die: {message}");
        plugin.getDiscordBot().sendServerMessageText(format.replace("{message}", plain));
    }
}
