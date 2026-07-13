package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.util.FormattingUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {

    private final DiscordUtils plugin;

    public DeathListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("deaths.enabled", false)) return;
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) return;
        String clean = FormattingUtil.stripFormatting(deathMessage);
        plugin.getDiscordBot().sendDeathMessage(clean);
    }
}
