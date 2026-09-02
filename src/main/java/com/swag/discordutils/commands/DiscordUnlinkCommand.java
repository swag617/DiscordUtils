package com.swag.discordutils.commands;

import com.swag.discordutils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordUnlinkCommand implements CommandExecutor {

    private final DiscordUtils plugin;

    public DiscordUnlinkCommand(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players in-game.");
            return true;
        }

        var lm = plugin.getLinkManager();
        if (lm == null) {
            player.sendMessage("§cThe Discord link system is not configured.");
            return true;
        }

        // lm.isLinked()/unlink() hit the SwagAPI-backed database — never block the main
        // thread on DB I/O, so resolve the unlink off-thread and hop back to send messages.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!lm.isLinked(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§eYour account is not linked to any Discord account."));
                return;
            }

            boolean success = lm.unlink(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage("§aYour Discord account has been unlinked. Your rank role has been removed.");
                } else {
                    player.sendMessage("§cSomething went wrong while unlinking. Please contact an admin.");
                }
            });
        });
        return true;
    }
}
