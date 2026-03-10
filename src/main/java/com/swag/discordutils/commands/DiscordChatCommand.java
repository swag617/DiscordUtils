package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordChatCommand implements CommandExecutor, TabCompleter {

    private final DiscordUtils plugin;

    public DiscordChatCommand(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("discordutils.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "allow" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /discordchat allow <all|admins>");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "all" -> {
                        plugin.getConfig().set("discord-chat.allow-everyone", true);
                        plugin.saveConfig();
                        sender.sendMessage("§aDiscord chat relay set to §fall§a - everyone on Discord can chat to Minecraft.");
                    }
                    case "admins" -> {
                        plugin.getConfig().set("discord-chat.allow-everyone", false);
                        plugin.saveConfig();
                        String role = plugin.getConfig().getString("discord-chat.admin-role-name", "Admin");
                        sender.sendMessage("§aDiscord chat relay set to §fadmins only§a - Discord users need the §f" + role + "§a role.");
                    }
                    default -> sender.sendMessage("§cUnknown option. Use §fall§c or §fadmins§c.");
                }
            }
            case "enable" -> {
                plugin.getConfig().set("discord-chat.enabled", true);
                plugin.saveConfig();
                sender.sendMessage("§aDiscord->Minecraft chat relay §fenabled§a.");
            }
            case "disable" -> {
                plugin.getConfig().set("discord-chat.enabled", false);
                plugin.saveConfig();
                sender.sendMessage("§cDiscord->Minecraft chat relay §fdisabled§c.");
            }
            case "status" -> {
                boolean enabled  = plugin.getConfig().getBoolean("discord-chat.enabled", true);
                boolean everyone = plugin.getConfig().getBoolean("discord-chat.allow-everyone", true);
                String role      = plugin.getConfig().getString("discord-chat.admin-role-name", "Admin");
                sender.sendMessage("§7--- DiscordUtils Status ---");
                sender.sendMessage("§7Bot connected: " + (plugin.getDiscordBot().isReady() ? "§ayes" : "§cno"));
                sender.sendMessage("§7Relay enabled: " + (enabled ? "§ayes" : "§cno"));
                sender.sendMessage("§7Allow everyone: " + (everyone ? "§ayes" : "§cno (admins only: §f" + role + "§7)"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7--- DiscordUtils Commands ---");
        sender.sendMessage("§f/discordchat allow all §7- let all Discord users chat to Minecraft");
        sender.sendMessage("§f/discordchat allow admins §7- restrict to Discord users with the admin role");
        sender.sendMessage("§f/discordchat enable §7- enable the Discord->Minecraft relay");
        sender.sendMessage("§f/discordchat disable §7- disable the Discord->Minecraft relay");
        sender.sendMessage("§f/discordchat status §7- show current settings");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("allow", "enable", "disable", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("allow")) {
            return Arrays.asList("all", "admins");
        }
        return Collections.emptyList();
    }
}
