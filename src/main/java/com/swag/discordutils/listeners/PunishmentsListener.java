package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class PunishmentsListener implements Listener {

    private final DiscordUtils plugin;

    public PunishmentsListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!plugin.getConfig().getBoolean("punishments.enabled", false)) return;

        Player player = event.getPlayer();
        if (!player.isBanned()) return;

        @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
        BanEntry<String> entry = ((BanList) Bukkit.getBanList(BanList.Type.NAME)).getBanEntry(player.getName());

        String reason   = entry != null ? entry.getReason() : null;
        String bannedBy = entry != null ? entry.getSource() : null;

        plugin.getDiscordBot().sendPunishmentEmbed(
                player.getName(),
                player.getUniqueId(),
                reason,
                bannedBy
        );
    }
}
