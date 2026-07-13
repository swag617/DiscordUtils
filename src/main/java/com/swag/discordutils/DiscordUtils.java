package com.swag.discordutils;

import com.swag.discordutils.commands.DiscordChatCommand;
import com.swag.discordutils.commands.DiscordCommand;
import com.swag.discordutils.commands.DiscordLinkCommand;
import com.swag.discordutils.commands.DiscordUnlinkCommand;
import com.swag.discordutils.discord.DiscordBot;
import com.swag.discordutils.link.LinkDatabase;
import com.swag.discordutils.link.LinkHttpServer;
import com.swag.discordutils.link.LinkManager;
import com.swag.discordutils.listeners.DeathListener;
import com.swag.discordutils.listeners.DiscordMessageListener;
import com.swag.discordutils.listeners.AuctionHouseListener;
import com.swag.discordutils.listeners.CmiAfkListener;
import com.swag.discordutils.listeners.MinecraftChatListener;
import com.swag.discordutils.listeners.PunishmentsListener;
import com.swag.discordutils.listeners.ServerEventListener;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class DiscordUtils extends JavaPlugin {

    private static DiscordUtils instance;
    private DiscordBot discordBot;
    private Chat vaultChat;
    private LinkManager linkManager;
    private LinkHttpServer linkHttpServer;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        migrateConfig();

        setupVault();
        setupLinkSystem();

        discordBot = new DiscordBot(this);

        // Register the JDA listener before connecting so events aren't missed
        DiscordMessageListener discordMessageListener = new DiscordMessageListener(this);
        discordBot.connect(discordMessageListener);

        // Bukkit listeners
        getServer().getPluginManager().registerEvents(new MinecraftChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishmentsListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        setupAfkListener();
        setupAuctionHouseListener();
        setupStaffChatBridgeListener();

        DiscordChatCommand cmd = new DiscordChatCommand(this);
        getCommand("discordchat").setExecutor(cmd);
        getCommand("discordchat").setTabCompleter(cmd);

        getCommand("discordlink").setExecutor(new DiscordLinkCommand(this));
        getCommand("discordunlink").setExecutor(new DiscordUnlinkCommand(this));

        if (getConfig().getBoolean("discord-invite.enabled", false)) {
            DiscordCommand discordCmd = new DiscordCommand(this);
            if (getCommand("discord") != null) {
                getCommand("discord").setExecutor(discordCmd);
            }
        }

        getLogger().info("DiscordUtils enabled.");
    }

    @Override
    public void onDisable() {
        if (linkHttpServer != null) {
            linkHttpServer.stop();
        }
        if (linkManager != null) {
            linkManager.getDb().close();
        }
        if (discordBot != null) {
            discordBot.shutdown();
        }
        getLogger().info("DiscordUtils disabled.");
    }

    private void setupAuctionHouseListener() {
        boolean found = java.util.Arrays.stream(getServer().getPluginManager().getPlugins())
                .anyMatch(p -> {
                    String name = p.getName().toLowerCase();
                    return name.contains("auctionhouse") || name.contains("auction") || name.equals("ah");
                });
        if (found) {
            try {
                getServer().getPluginManager().registerEvents(new AuctionHouseListener(this), this);
                getLogger().info("Auction House plugin detected - auction logging enabled.");
            } catch (Exception e) {
                getLogger().warning("Could not register AuctionHouseListener: " + e.getMessage());
            }
        } else {
            getLogger().info("No Auction House plugin found - auction logging disabled.");
        }
    }

    private void setupStaffChatBridgeListener() {
        try {
            Class.forName("dev.pace.staffchat.StaffChat");
            getServer().getPluginManager().registerEvents(new com.swag.discordutils.listeners.StaffChatBridgeListener(this), this);
            getLogger().info("StaffChat detected - staff chat Discord bridge enabled.");
        } catch (ClassNotFoundException ignored) {
            // StaffChat not present — staff chat relay uses broadcast detection as fallback.
        }
    }

    private void setupLinkSystem() {
        String clientId = getConfig().getString("link.client-id", "");
        if (clientId.isEmpty() || clientId.equals("YOUR_CLIENT_ID_HERE")) {
            getLogger().info("link.client-id not configured - /discordlink will be unavailable.");
            return;
        }

        try {
            LinkDatabase db = new LinkDatabase(this);
            linkManager = new LinkManager(this, db);
        } catch (SQLException e) {
            getLogger().severe("Failed to open links.db: " + e.getMessage());
            return;
        }

        linkHttpServer = new LinkHttpServer(this);
        try {
            linkHttpServer.start();
        } catch (Exception e) {
            getLogger().severe("Failed to start Discord link HTTP server: " + e.getMessage());
            linkHttpServer = null;
        }
    }

    private void setupAfkListener() {
        if (getServer().getPluginManager().getPlugin("CMI") != null) {
            getServer().getPluginManager().registerEvents(new CmiAfkListener(this), this);
            getLogger().info("CMI detected - AFK messages will be sent to Discord.");
        } else {
            getLogger().info("CMI not found - AFK embeds disabled.");
        }
    }

    private void migrateConfig() {
        int version = getConfig().getInt("config-version", 0);
        boolean dirty = false;

        if (version < 1) {
            // Version 1 — initial release. Ensures all base keys exist with sensible defaults.

            // Rescue old channel-id value BEFORE removing it, so it can seed the new per-section keys.
            String oldChannelId = getConfig().getString("channel-id", "");

            // Remove old top-level keys that were restructured.
            if (getConfig().isSet("channel-id"))    getConfig().set("channel-id",    null);
            if (getConfig().isSet("link.guild-id")) getConfig().set("link.guild-id", null);

            // Top-level keys
            if (!getConfig().isSet("debug"))           getConfig().set("debug",           false);
            if (!getConfig().isSet("bot-token"))       getConfig().set("bot-token",       "YOUR_BOT_TOKEN_HERE");
            if (!getConfig().isSet("announce-status")) getConfig().set("announce-status", true);

            // formatting section
            if (!getConfig().isSet("formatting.parse-minimessage"))   getConfig().set("formatting.parse-minimessage",   true);
            if (!getConfig().isSet("formatting.strip-for-discord"))   getConfig().set("formatting.strip-for-discord",   true);
            if (!getConfig().isSet("formatting.discord-send-format")) getConfig().set("formatting.discord-send-format", "**[{rank}] {player}**: {message}");

            // chat section
            if (!getConfig().isSet("chat.server"))     getConfig().set("chat.server",     1);
            if (!getConfig().isSet("chat.channel-id")) getConfig().set("chat.channel-id", oldChannelId.isEmpty() ? "YOUR_CHANNEL_ID_HERE" : oldChannelId);

            // discord-chat section
            if (!getConfig().isSet("discord-chat.enabled"))                  getConfig().set("discord-chat.enabled",                  true);
            if (!getConfig().isSet("discord-chat.allow-everyone"))           getConfig().set("discord-chat.allow-everyone",           true);
            if (!getConfig().isSet("discord-chat.admin-role-name"))          getConfig().set("discord-chat.admin-role-name",          "Admin");
            if (!getConfig().isSet("discord-chat.admin-role-id"))            getConfig().set("discord-chat.admin-role-id",            "");
            if (!getConfig().isSet("discord-chat.format"))                   getConfig().set("discord-chat.format",                   "&7[&bDiscord&7] &b[{role}] {username}&7: &f{message}");
            if (!getConfig().isSet("discord-chat.convert-discord-markdown")) getConfig().set("discord-chat.convert-discord-markdown", true);
            if (!getConfig().isSet("discord-chat.display-roles"))            getConfig().set("discord-chat.display-roles",            java.util.Arrays.asList("Owner", "Manager", "Admin", "Mod", "Helper", "Support", "Flea"));

            // auction-house section
            if (!getConfig().isSet("auction-house.enabled"))    getConfig().set("auction-house.enabled",    true);
            if (!getConfig().isSet("auction-house.server"))     getConfig().set("auction-house.server",     1);
            if (!getConfig().isSet("auction-house.channel-id")) getConfig().set("auction-house.channel-id", "YOUR_AUCTION_CHANNEL_ID_HERE");

            // link section
            if (!getConfig().isSet("link.client-id"))       getConfig().set("link.client-id",      "YOUR_CLIENT_ID_HERE");
            if (!getConfig().isSet("link.client-secret"))   getConfig().set("link.client-secret",  "YOUR_CLIENT_SECRET_HERE");
            if (!getConfig().isSet("link.server-ip"))       getConfig().set("link.server-ip",       "YOUR_SERVER_IP_HERE");
            if (!getConfig().isSet("link.port"))            getConfig().set("link.port",             4567);
            if (!getConfig().isSet("link.server"))          getConfig().set("link.server",           1);
            if (!getConfig().isSet("link.rank-role-names")) getConfig().set("link.rank-role-names", java.util.Arrays.asList("Axolotl", "Lizard", "Flea"));

            // servers section
            if (!getConfig().isSet("servers.1.guild-id")) getConfig().set("servers.1.guild-id", "YOUR_GUILD_ID_HERE");
            if (!getConfig().isSet("servers.2.guild-id")) getConfig().set("servers.2.guild-id", "YOUR_SECOND_GUILD_ID_HERE");

            // punishments section
            if (!getConfig().isSet("punishments.enabled"))    getConfig().set("punishments.enabled",    false);
            if (!getConfig().isSet("punishments.server"))     getConfig().set("punishments.server",     1);
            if (!getConfig().isSet("punishments.channel-id")) getConfig().set("punishments.channel-id", "CHANNEL_ID_HERE");

            // join-leave section
            if (!getConfig().isSet("join-leave.enabled"))      getConfig().set("join-leave.enabled",      true);
            if (!getConfig().isSet("join-leave.server"))       getConfig().set("join-leave.server",       1);
            if (!getConfig().isSet("join-leave.channel-id"))   getConfig().set("join-leave.channel-id",   oldChannelId.isEmpty() ? "YOUR_CHANNEL_ID_HERE" : oldChannelId);
            if (!getConfig().isSet("join-leave.join-format"))  getConfig().set("join-leave.join-format",  ":green_circle: **{player}** joined the server.");
            if (!getConfig().isSet("join-leave.leave-format")) getConfig().set("join-leave.leave-format", ":red_circle: **{player}** left the server.");

            // afk section
            if (!getConfig().isSet("afk.enabled"))     getConfig().set("afk.enabled",    true);
            if (!getConfig().isSet("afk.server"))      getConfig().set("afk.server",     1);
            if (!getConfig().isSet("afk.channel-id"))  getConfig().set("afk.channel-id", oldChannelId.isEmpty() ? "YOUR_CHANNEL_ID_HERE" : oldChannelId);
            if (!getConfig().isSet("afk.afk-format"))  getConfig().set("afk.afk-format",  ":zzz: **{player}** is now AFK.");
            if (!getConfig().isSet("afk.back-format")) getConfig().set("afk.back-format", ":wave: **{player}** is no longer AFK.");

            // server-messages section
            if (!getConfig().isSet("server-messages.enabled"))        getConfig().set("server-messages.enabled",        true);
            if (!getConfig().isSet("server-messages.server"))         getConfig().set("server-messages.server",         1);
            if (!getConfig().isSet("server-messages.channel-id"))     getConfig().set("server-messages.channel-id",     oldChannelId.isEmpty() ? "YOUR_CHANNEL_ID_HERE" : oldChannelId);
            if (!getConfig().isSet("server-messages.relay-prefixes")) getConfig().set("server-messages.relay-prefixes", java.util.Arrays.asList("[MiniGame]", "[Trivia]", "[Quiz]", "[Event]"));
            if (!getConfig().isSet("server-messages.discord-format")) getConfig().set("server-messages.discord-format", ":game_die: {message}");

            getConfig().set("config-version", 1);
            dirty = true;
            getLogger().info("Config migrated to version 1.");
        }

        if (version < 2) {
            // Version 2 — staff chat system.
            if (!getConfig().isSet("staff-chat.enabled"))                      getConfig().set("staff-chat.enabled",                      true);
            if (!getConfig().isSet("staff-chat.server"))                       getConfig().set("staff-chat.server",                       1);
            if (!getConfig().isSet("staff-chat.channel-id"))                   getConfig().set("staff-chat.channel-id",                   "YOUR_STAFF_CHANNEL_ID_HERE");
            if (!getConfig().isSet("staff-chat.permission"))                   getConfig().set("staff-chat.permission",                   "discordutils.staffchat");
            if (!getConfig().isSet("staff-chat.minecraft-to-discord-format"))  getConfig().set("staff-chat.minecraft-to-discord-format",  "**[Staff] {player}**: {message}");
            if (!getConfig().isSet("staff-chat.discord-to-minecraft-format"))  getConfig().set("staff-chat.discord-to-minecraft-format",  "&c[Staff Discord] &f[{role}] {username}&7: &f{message}");

            getConfig().set("config-version", 2);
            dirty = true;
            getLogger().info("Config migrated to version 2.");
        }

        if (version < 3) {
            // Version 3 — broadcast-based staff chat relay (replaces /sc command approach).
            if (!getConfig().isSet("staff-chat.relay-prefixes"))  getConfig().set("staff-chat.relay-prefixes",  java.util.Arrays.asList("[Staff]", "[SC]"));
            if (!getConfig().isSet("staff-chat.discord-format"))   getConfig().set("staff-chat.discord-format",   ":shield: **[Staff] {player}**: {message}");
            if (!getConfig().isSet("staff-chat.channel-type"))     getConfig().set("staff-chat.channel-type",     "staff");
            // Remove keys that were only used by the old /sc command
            getConfig().set("staff-chat.minecraft-to-discord-format", null);

            getConfig().set("config-version", 3);
            dirty = true;
            getLogger().info("Config migrated to version 3.");
        }

        if (version < 4) {
            // Version 4 — presence/topic updates, /discord invite command, death messages.
            if (!getConfig().isSet("presence.enabled"))            getConfig().set("presence.enabled",            true);
            if (!getConfig().isSet("presence.format"))              getConfig().set("presence.format",              "{online}/{max} players online");
            if (!getConfig().isSet("presence.update-chat-topic"))   getConfig().set("presence.update-chat-topic",   true);
            if (!getConfig().isSet("presence.topic-format"))        getConfig().set("presence.topic-format",        "{online}/{max} players online");

            if (!getConfig().isSet("discord-invite.enabled")) getConfig().set("discord-invite.enabled", false);
            if (!getConfig().isSet("discord-invite.url"))     getConfig().set("discord-invite.url",     "YOUR_INVITE_HERE");

            if (!getConfig().isSet("deaths.enabled"))    getConfig().set("deaths.enabled",    false);
            if (!getConfig().isSet("deaths.server"))     getConfig().set("deaths.server",     1);
            if (!getConfig().isSet("deaths.channel-id")) getConfig().set("deaths.channel-id", "YOUR_CHANNEL_ID_HERE");
            if (!getConfig().isSet("deaths.format"))     getConfig().set("deaths.format",     ":skull: {message}");

            getConfig().set("config-version", 4);
            dirty = true;
            getLogger().info("Config migrated to version 4.");
        }

        // Future versions go here as additional if blocks:
        // if (version < 5) { ... getConfig().set("config-version", 5); dirty = true; }

        if (dirty) saveConfig();
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault not found - rank prefixes will not be shown in Discord messages.");
            return;
        }
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            getLogger().warning("Vault found but no Chat provider registered (is LuckPerms/PermissionsEx installed?).");
            return;
        }
        vaultChat = rsp.getProvider();
        getLogger().info("Vault Chat hooked - rank prefixes enabled.");
    }

    public static DiscordUtils getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    /** Returns the Vault Chat provider, or null if Vault is not available. */
    public Chat getVaultChat() {
        return vaultChat;
    }

    /** Returns the LinkManager, or null if the link system is not configured. */
    public LinkManager getLinkManager() {
        return linkManager;
    }
}
