package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {

    private final DiscordUtils plugin;

    public DiscordCommand(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String url = plugin.getConfig().getString("discord-invite.url", "");
        if (url.isEmpty() || url.equals("YOUR_INVITE_HERE")) {
            sender.sendMessage("§cDiscord invite URL is not configured.");
            return true;
        }
        if (sender instanceof Player player) {
            TextComponent msg = new TextComponent("§7Join our Discord: §b" + url);
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§eClick to open Discord invite").create()));
            player.spigot().sendMessage(msg);
        } else {
            sender.sendMessage("Discord invite: " + url);
        }
        return true;
    }
}
