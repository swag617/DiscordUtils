package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StaffChatCommand {

    private StaffChatCommand() {}

    /**
     * Sends a formatted staff chat message to every online player who holds the
     * configured staff chat permission, and to the console.
     *
     * Called from DiscordMessageListener when a message arrives from the staff Discord channel.
     */
    public static void broadcastToStaff(DiscordUtils plugin, String formattedMessage) {
        String permission = plugin.getConfig().getString("staff-chat.permission", "discordutils.staffchat");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.sendMessage(formattedMessage);
            }
        }
        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }
}
