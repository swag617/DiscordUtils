package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Admin command to configure named webhooks (the webhooks.* config section) without
 * hand-editing config.yml — set/test immediately send a confirmation message so you know
 * right away whether the URL actually works.
 */
public class DiscordWebhookCommand implements CommandExecutor, TabCompleter {

    private final DiscordUtils plugin;

    public DiscordWebhookCommand(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    // /discordwebhook set <name> <url>   - configure a named webhook and test it
    // /discordwebhook remove <name>      - clear a named webhook
    // /discordwebhook list               - list configured names (URLs masked)
    // /discordwebhook test <name>        - resend a test message on demand
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
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /discordwebhook set <name> <url>");
                    return true;
                }
                String name = args[1].toLowerCase();
                String url = args[2];
                plugin.getConfig().set("webhooks." + name, url);
                plugin.saveConfig();
                sender.sendMessage("§aWebhook §f" + name + " §asaved. Sending a test message...");
                sendTest(sender, name, url);
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /discordwebhook remove <name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                if (plugin.getConfig().getString("webhooks." + name, "").isEmpty()) {
                    sender.sendMessage("§cNo webhook named §f" + name + "§c is configured.");
                    return true;
                }
                plugin.getConfig().set("webhooks." + name, "");
                plugin.saveConfig();
                sender.sendMessage("§aWebhook §f" + name + " §aremoved.");
            }
            case "list" -> {
                List<String> names = configuredWebhookNames();
                if (names.isEmpty()) {
                    sender.sendMessage("§7No webhooks configured. Use §f/discordwebhook set <name> <url>§7.");
                    return true;
                }
                sender.sendMessage("§7--- Configured Webhooks ---");
                for (String name : names) {
                    String url = plugin.getConfig().getString("webhooks." + name, "");
                    sender.sendMessage("§f" + name + " §7— " + mask(url));
                }
            }
            case "test" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /discordwebhook test <name>");
                    return true;
                }
                String name = args[1].toLowerCase();
                String url = plugin.getConfig().getString("webhooks." + name, "");
                if (url.isEmpty()) {
                    sender.sendMessage("§cNo webhook named §f" + name + "§c is configured.");
                    return true;
                }
                sender.sendMessage("§7Sending a test message to §f" + name + "§7...");
                sendTest(sender, name, url);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendTest(CommandSender sender, String name, String url) {
        plugin.getDiscordBot().getWebhookSender().sendTestWithCallback(url, success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage("§aTest message to §f" + name + "§a delivered successfully.");
                    } else {
                        sender.sendMessage("§cTest message to §f" + name + "§c failed — check the URL and console for details.");
                    }
                }));
    }

    /** Names with a non-empty URL under the webhooks.* config section. */
    private List<String> configuredWebhookNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("webhooks");
        if (section == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (!section.getString(key, "").isEmpty()) names.add(key);
        }
        return names;
    }

    private String mask(String url) {
        if (url.length() <= 10) return "***";
        return "..." + url.substring(url.length() - 6);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7--- DiscordUtils Webhook Commands ---");
        sender.sendMessage("§f/discordwebhook set <name> <url> §7- configure a webhook and test it");
        sender.sendMessage("§f/discordwebhook remove <name> §7- clear a webhook");
        sender.sendMessage("§f/discordwebhook list §7- list configured webhooks");
        sender.sendMessage("§f/discordwebhook test <name> §7- resend a test message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "remove", "list", "test");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("test") || args[0].equalsIgnoreCase("set"))) {
            return configuredWebhookNames();
        }
        return Collections.emptyList();
    }
}
