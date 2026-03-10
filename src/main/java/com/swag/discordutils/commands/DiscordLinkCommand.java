package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.link.LinkManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordLinkCommand implements CommandExecutor {

    private final DiscordUtils plugin;

    public DiscordLinkCommand(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players in-game.");
            return true;
        }

        LinkManager lm = plugin.getLinkManager();
        if (lm == null) {
            player.sendMessage(ChatColor.RED + "The Discord link system is not configured. "
                    + "Ask an admin to set link.client-id and link.client-secret in config.yml.");
            return true;
        }

        if (lm.isLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Your Minecraft account is already linked to a Discord account.");
            return true;
        }

        LinkManager.LinkStart start = lm.startLink(player.getUniqueId());

        TextComponent header = new TextComponent(ChatColor.GRAY + "[" + ChatColor.AQUA + "Discord Link" + ChatColor.GRAY + "] ");
        TextComponent clickLink = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "Click here to link your Discord account");
        clickLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, start.oauthUrl()));
        clickLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.GRAY + "Opens the Discord authorization page in your browser").create()));
        player.spigot().sendMessage(header, clickLink);

        player.sendMessage(ChatColor.GRAY + "Can't click? DM the bot this code: " + ChatColor.YELLOW + "" + ChatColor.BOLD + start.code());
        player.sendMessage(ChatColor.DARK_GRAY + "This code expires in 10 minutes.");

        return true;
    }
}
