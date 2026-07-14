package com.swag.discordutils;

import com.swag.discordutils.commands.DiscordChatCommand;
import com.swag.discordutils.commands.DiscordCommand;
import com.swag.discordutils.commands.DiscordLinkCommand;
import com.swag.discordutils.commands.DiscordUnlinkCommand;
import com.swag.discordutils.commands.DiscordWebhookCommand;
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
    private final com.swag.discordutils.webhook.DigestBuffer digestBuffer = new com.swag.discordutils.webhook.DigestBuffer();
    private boolean dashboardRegistered = false;

    // ── SwagAPI service references ─────────────────────────────────────────────
    private com.SwagDev.SwagAPI.api.IDatabaseService dbService;
    private com.SwagDev.SwagAPI.api.IEventBusService busService;
    private com.SwagDev.SwagAPI.api.IWebService webService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        migrateConfig();

        // Step 1 — Hook SwagAPI FIRST, before any manager initialization
        if (!hookSwagAPI()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupVault();
        setupLinkSystem();

        discordBot = new DiscordBot(this);
        subscribeNotifyChannel();
        startDigestFlushTask();
        registerDashboard();

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

        DiscordWebhookCommand webhookCmd = new DiscordWebhookCommand(this);
        getCommand("discordwebhook").setExecutor(webhookCmd);
        getCommand("discordwebhook").setTabCompleter(webhookCmd);

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
        if (dashboardRegistered && webService != null) {
            webService.unregisterModule(this);
        }
        if (busService != null) {
            busService.unsubscribeAll(this);
        }
        if (linkHttpServer != null) {
            linkHttpServer.stop();
        }
        // MIGRATED: linkManager.getDb().close() removed — connection pool is owned by SwagAPI
        if (discordBot != null) {
            discordBot.shutdown();
            // Independent of the bot connection (webhook-only setups never connect a bot at
            // all), so this must not be nested inside DiscordBot#shutdown()'s early return.
            discordBot.getWebhookSender().shutdown();
        }
        getLogger().info("DiscordUtils disabled.");
    }

    /**
     * Hooks SwagAPI's shared services. IDatabaseService is a hard requirement — the
     * link database has no functionality without it. IEventBusService is used for
     * the discordutils:notify inbound channel and the account_linked/account_unlinked
     * outbound events; DiscordUtils still runs without it, just without those.
     *
     * <p>IWebService is hooked here too, but ONLY for the admin dashboard
     * ({@code registerDashboard()}) — every module registered with it is gated behind
     * SwagAPI's own panel-login session cookie (see WebService#registerModule), which is
     * exactly right for an admin-only page. {@link LinkHttpServer}'s {@code /link} route
     * is a different story: it's a Discord OAuth2 callback that a player's browser hits
     * anonymously straight off Discord's redirect, so gating it behind a SwagAPI panel
     * login would break linking for any player not also logged into the web panel.
     * LinkHttpServer stays its own standalone HttpServer for that reason and does not use
     * this IWebService hook.</p>
     */
    private boolean hookSwagAPI() {
        org.bukkit.plugin.ServicesManager sm = getServer().getServicesManager();

        org.bukkit.plugin.RegisteredServiceProvider<com.SwagDev.SwagAPI.api.IDatabaseService> dbProv =
                sm.getRegistration(com.SwagDev.SwagAPI.api.IDatabaseService.class);
        if (dbProv == null) {
            getLogger().severe("SwagAPI IDatabaseService not found! Is SwagAPI loaded? Disabling.");
            return false;
        }
        dbService = dbProv.getProvider();
        getLogger().info("Hooked SwagAPI IDatabaseService.");

        org.bukkit.plugin.RegisteredServiceProvider<com.SwagDev.SwagAPI.api.IEventBusService> busProv =
                sm.getRegistration(com.SwagDev.SwagAPI.api.IEventBusService.class);
        if (busProv != null) {
            busService = busProv.getProvider();
            getLogger().info("Hooked SwagAPI IEventBusService.");
        }

        org.bukkit.plugin.RegisteredServiceProvider<com.SwagDev.SwagAPI.api.IWebService> webProv =
                sm.getRegistration(com.SwagDev.SwagAPI.api.IWebService.class);
        if (webProv != null) {
            webService = webProv.getProvider();
            getLogger().info("Hooked SwagAPI IWebService.");
        }

        return true;
    }

    /**
     * Registers the webhook dashboard with SwagAPI's shared IWebService, if available.
     * DiscordUtils does not own an HttpServer for this — SwagAPI's shared server does,
     * already gated by SwagAPI's own login system, so the dashboard has no password/auth
     * of its own.
     */
    private void registerDashboard() {
        if (!getConfig().getBoolean("dashboard.enabled", true)) {
            getLogger().info("Dashboard disabled in config, skipping.");
            return;
        }
        if (webService == null) {
            getLogger().warning("SwagAPI IWebService not present — dashboard unavailable.");
            return;
        }
        webService.registerModule(this, new com.swag.discordutils.web.WebHttpHandler(this));
        dashboardRegistered = true;
        getLogger().info("Dashboard registered at " + webService.getPluginUrl(getName().toLowerCase()));
    }

    /**
     * Subscribes to the discordutils:notify event-bus channel so any plugin can send
     * a webhook-routed Discord message without a compile-time or reflection-based
     * dependency on DiscordUtils. See README for the payload contract.
     */
    private void subscribeNotifyChannel() {
        if (busService == null) return;

        busService.subscribe("discordutils:notify", event -> {
            java.util.Map<String, Object> data = event.getData();

            // A raw URL always wins if present — lets a plugin send a one-off Discord
            // message without needing a name pre-configured in webhooks.* first.
            // statsName stays null for these since there's no stable name to attribute to.
            String webhookName = data.get("webhook") instanceof String s && !s.isEmpty() ? s : null;
            String webhookUrl;
            if (data.get("webhookUrl") instanceof String rawUrl && !rawUrl.isEmpty()) {
                webhookUrl = rawUrl;
            } else if (webhookName != null) {
                webhookUrl = getConfig().getString("webhooks." + webhookName, "");
                if (webhookUrl.isEmpty()) {
                    getLogger().warning("discordutils:notify from " + event.getSourcePlugin()
                            + " requested webhook '" + webhookName + "', but it is not configured "
                            + "(webhooks." + webhookName + " in config.yml) — dropped.");
                    return;
                }
            } else {
                getLogger().warning("discordutils:notify from " + event.getSourcePlugin()
                        + " has neither a 'webhook' name nor a 'webhookUrl' — dropped.");
                return;
            }

            String username = data.get("username") instanceof String s && !s.isEmpty() ? s : null;
            String avatarUrl = data.get("avatarUrl") instanceof String s && !s.isEmpty() ? s : null;

            if (isDigestEnabled(webhookName)) {
                digestBuffer.append(webhookName, webhookUrl, buildDigestLine(data), username, avatarUrl);
                return;
            }

            Object content = data.get("content");
            if (content instanceof String s && !s.isEmpty()) {
                discordBot.getWebhookSender().sendContent(webhookUrl, s, username, avatarUrl, webhookName);
                return;
            }

            com.swag.discordutils.webhook.WebhookSender.Embed embed =
                    new com.swag.discordutils.webhook.WebhookSender.Embed();

            if (data.get("title") instanceof String s && !s.isEmpty()) embed.title(s);
            if (data.get("description") instanceof String s && !s.isEmpty()) embed.description(s);
            if (data.get("color") instanceof Number n) embed.color(n.intValue());
            if (username != null) embed.username(username);
            if (avatarUrl != null) embed.avatarUrl(avatarUrl);

            Object fieldsObj = data.get("fields");
            if (fieldsObj instanceof java.util.List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof java.util.Map<?, ?> fieldMap)) continue;
                    String name = String.valueOf(fieldMap.get("name"));
                    String value = String.valueOf(fieldMap.get("value"));
                    boolean inline = fieldMap.get("inline") instanceof Boolean b && b;
                    embed.field(name, value, inline);
                }
            }

            discordBot.getWebhookSender().send(webhookUrl, embed, webhookName);
        }, this);

        getLogger().info("Subscribed to discordutils:notify event-bus channel.");
    }

    private boolean isDigestEnabled(String webhookName) {
        if (webhookName == null) return false;
        return getConfig().getStringList("digest.enabled-webhooks").contains(webhookName);
    }

    /** Collapses a notify event's payload into one line for the digest's bullet list. */
    private String buildDigestLine(java.util.Map<String, Object> data) {
        if (data.get("content") instanceof String s && !s.isEmpty()) return s;
        String title = data.get("title") instanceof String s ? s : null;
        String description = data.get("description") instanceof String s ? s : null;
        if (title != null && description != null) return "**" + title + "** — " + description;
        if (title != null) return title;
        if (description != null) return description;
        return "(no content)";
    }

    /**
     * Starts the periodic task that flushes every non-empty digest buffer into a single
     * embed per webhook name. No-ops if digest.window-seconds is unset/non-positive.
     */
    private void startDigestFlushTask() {
        long windowSeconds = getConfig().getLong("digest.window-seconds", 30L);
        if (windowSeconds <= 0) return;
        long ticks = windowSeconds * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::flushDigests, ticks, ticks);
    }

    private void flushDigests() {
        for (String name : digestBuffer.bufferedWebhookNames()) {
            var result = digestBuffer.flush(name);
            if (result == null) continue;

            StringBuilder description = new StringBuilder();
            for (String line : result.lines()) {
                description.append("• ").append(line).append("\n");
            }

            var embed = new com.swag.discordutils.webhook.WebhookSender.Embed()
                    .description(description.toString().trim())
                    .color(0x5865F2)
                    .timestamp(java.time.Instant.now());
            if (result.username() != null) embed.username(result.username());
            if (result.avatarUrl() != null) embed.avatarUrl(result.avatarUrl());

            discordBot.getWebhookSender().send(result.webhookUrl(), embed, name);
        }
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
            LinkDatabase db = new LinkDatabase(this, dbService);
            linkManager = new LinkManager(this, db);
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize the links table: " + e.getMessage());
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

        if (version < 5) {
            // Version 5 — named webhooks, now the recommended way to send Discord
            // messages (bot connection becomes optional, only needed for two-way
            // features). Seed empty so existing bot-based setups keep working as-is.
            if (!getConfig().isSet("webhooks.chat"))            getConfig().set("webhooks.chat",            "");
            if (!getConfig().isSet("webhooks.staff-chat"))      getConfig().set("webhooks.staff-chat",      "");
            if (!getConfig().isSet("webhooks.join-leave"))      getConfig().set("webhooks.join-leave",      "");
            if (!getConfig().isSet("webhooks.afk"))             getConfig().set("webhooks.afk",             "");
            if (!getConfig().isSet("webhooks.auction-house"))   getConfig().set("webhooks.auction-house",   "");
            if (!getConfig().isSet("webhooks.punishments"))     getConfig().set("webhooks.punishments",     "");
            if (!getConfig().isSet("webhooks.deaths"))          getConfig().set("webhooks.deaths",          "");
            if (!getConfig().isSet("webhooks.server-messages")) getConfig().set("webhooks.server-messages", "");

            getConfig().set("config-version", 5);
            dirty = true;
            getLogger().info("Config migrated to version 5.");
        }

        if (version < 6) {
            // Version 6 — digest mode for discordutils:notify traffic.
            if (!getConfig().isSet("digest.enabled-webhooks")) getConfig().set("digest.enabled-webhooks", java.util.List.of());
            if (!getConfig().isSet("digest.window-seconds"))   getConfig().set("digest.window-seconds",   30);

            getConfig().set("config-version", 6);
            dirty = true;
            getLogger().info("Config migrated to version 6.");
        }

        if (version < 7) {
            // Version 7 — web dashboard.
            if (!getConfig().isSet("dashboard.enabled")) getConfig().set("dashboard.enabled", true);

            getConfig().set("config-version", 7);
            dirty = true;
            getLogger().info("Config migrated to version 7.");
        }

        // Future versions go here as additional if blocks:
        // if (version < 8) { ... getConfig().set("config-version", 8); dirty = true; }

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

    public com.SwagDev.SwagAPI.api.IDatabaseService getDbService() {
        return dbService;
    }

    /** Returns the SwagAPI event bus, or null if SwagAPI didn't register one. */
    public com.SwagDev.SwagAPI.api.IEventBusService getBusService() {
        return busService;
    }
}
