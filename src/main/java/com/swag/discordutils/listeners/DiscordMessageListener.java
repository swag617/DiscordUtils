package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.link.LinkManager;
import com.swag.discordutils.util.FormattingUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.util.List;

// NOTE: requires "Message Content Intent" enabled in the Discord Developer Portal,
// otherwise getContentDisplay() returns empty for all non-bot messages.
public class DiscordMessageListener extends ListenerAdapter {

    private final DiscordUtils plugin;

    public DiscordMessageListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.isFromType(ChannelType.PRIVATE)) {
            String content = event.getMessage().getContentDisplay().trim().toUpperCase();
            LinkManager lm = plugin.getLinkManager();
            // 8-char alphanumeric = a link code
            if (lm != null && content.matches("[A-Z0-9]{8}")) {
                boolean success = lm.completeLinkDM(event.getAuthor().getId(), content);
                String reply = success
                        ? "\u2705 Your Minecraft account has been successfully linked! You should now have your rank role in the server."
                        : "\u274C Invalid or expired code. Please run `/discordlink` in Minecraft to get a new code.";
                event.getChannel().sendMessage(reply).queue();
            }
            return; // never relay DMs to Minecraft chat
        }

        if (!event.isFromGuild()) return;

        String channelId = plugin.getConfig().getString("channel-id", "");
        if (!event.getChannel().getId().equals(channelId)) return;

        if (!plugin.getConfig().getBoolean("discord-chat.enabled", true)) return;

        boolean allowEveryone = plugin.getConfig().getBoolean("discord-chat.allow-everyone", true);
        if (!allowEveryone) {
            Member member = event.getMember();
            if (member == null || !hasAdminRole(member)) return;
        }

        String rawContent = event.getMessage().getContentDisplay();
        if (rawContent.isEmpty()) return;

        if (plugin.getConfig().getBoolean("discord-chat.convert-discord-markdown", true)) {
            rawContent = FormattingUtil.discordToMinecraft(rawContent);
        }

        String username = getDisplayName(event.getMember(), event.getAuthor().getName());
        String role = getHighestRole(event.getMember());
        String format = plugin.getConfig().getString("discord-chat.format", "&7[&bDiscord&7] &b[{role}] {username}&7: &f{message}");
        String message = format
                .replace("{username}", username)
                .replace("{role}", role)
                .replace("{message}", rawContent);

        message = FormattingUtil.parseMiniMessage(message);

        final String finalMessage = message;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(finalMessage));
    }

    private boolean hasAdminRole(Member member) {
        String adminRoleId   = plugin.getConfig().getString("discord-chat.admin-role-id", "");
        String adminRoleName = plugin.getConfig().getString("discord-chat.admin-role-name", "Admin");

        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (!adminRoleId.isEmpty() && role.getId().equals(adminRoleId)) return true;
            if (role.getName().equalsIgnoreCase(adminRoleName)) return true;
        }
        return false;
    }

    private String getDisplayName(Member member, String fallback) {
        if (member != null) return member.getEffectiveName();
        return fallback;
    }

    // Two passes: exact match wins over fuzzy so "Flea" never accidentally matches "Fleabot".
    private String getHighestRole(Member member) {
        if (member == null) return "Member";
        List<String> displayRoles = plugin.getConfig().getStringList("discord-chat.display-roles");
        for (Role role : member.getRoles()) {
            for (String name : displayRoles) {
                if (role.getName().equalsIgnoreCase(name)) return role.getName();
            }
        }
        for (Role role : member.getRoles()) {
            for (String name : displayRoles) {
                if (rolesMatch(name, role.getName())) return role.getName();
            }
        }
        return "Member";
    }

    private boolean rolesMatch(String configName, String discordName) {
        String a = configName.toLowerCase();
        String b = discordName.toLowerCase();
        return a.equals(b) || b.startsWith(a) || a.startsWith(b);
    }
}
