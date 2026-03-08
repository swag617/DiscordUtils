package com.swag.discordutils.listeners;

import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import com.swag.discordutils.DiscordUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles CMI AFK events.
 * Only registered when the "CMI" plugin is detected on enable.
 * Requires CMI.jar in the 'libs/' folder - see pom.xml.
 */
public class CmiAfkListener implements Listener {

    private final DiscordUtils plugin;

    public CmiAfkListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAfkEnter(CMIAfkEnterEvent event) {
        if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;
        plugin.getDiscordBot().sendAfkEmbed(
                event.getPlayer().getName(),
                event.getPlayer().getUniqueId(),
                true
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAfkLeave(CMIAfkLeaveEvent event) {
        if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;
        plugin.getDiscordBot().sendAfkEmbed(
                event.getPlayer().getName(),
                event.getPlayer().getUniqueId(),
                false
        );
    }
}
