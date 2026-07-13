package com.swag.discordutils.discord;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.listeners.AuctionHouseListener.AuctionAction;
import com.swag.discordutils.util.AuctionBadgeRenderer;
import com.swag.discordutils.util.CleanEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.util.UUID;
import java.util.logging.Level;

public class DiscordBot {

    private final DiscordUtils plugin;
    private JDA jda;
    private volatile boolean ready = false;

    public DiscordBot(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the JDA connection. Called from onEnable() on the main thread.
     * Does NOT call awaitReady() to avoid blocking the server.
     */
    public void connect(Object... extraListeners) {
        String token = plugin.getConfig().getString("bot-token", "");
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().severe("bot-token is not configured in config.yml! DiscordUtils will not connect.");
            return;
        }

        try {
            // Suppress JDA's fallback logger — Bukkit has its own logging system
            net.dv8tion.jda.internal.utils.JDALogger.setFallbackLoggerEnabled(false);

            JDABuilder builder = JDABuilder.createLight(token,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS); // GUILD_MEMBERS is a privileged intent - enable in Discord Dev Portal

            // Inner ReadyListener sets the ready flag and logs the bot name
            builder.addEventListeners(new ListenerAdapter() {
                @Override
                public void onReady(ReadyEvent event) {
                    ready = true;
                    plugin.getLogger().info("DiscordUtils bot connected as: " + event.getJDA().getSelfUser().getName());
                    if (plugin.getConfig().getBoolean("announce-status", true)) {
                        sendMessage(":white_check_mark: **DiscordUtils** is now online.");
                    }
                    updatePresence();
                    updateChatChannelTopic();
                }
            });

            for (Object listener : extraListeners) {
                builder.addEventListeners(listener);
            }

            jda = builder.build();
            // Do NOT call awaitReady() here - it would block the main server thread
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect DiscordUtils bot: " + e.getMessage(), e);
        }
    }

    /**
     * Gracefully shuts down the JDA connection. Called from onDisable().
     */
    public void shutdown() {
        if (jda == null) return;
        if (plugin.getConfig().getBoolean("announce-status", true) && ready) {
            sendMessage(":octagonal_sign: **DiscordUtils** is going offline.");
        }
        ready = false;
        jda.shutdown();
        try {
            if (!jda.awaitShutdown(java.time.Duration.ofSeconds(5))) {
                jda.shutdownNow();
            }
        } catch (InterruptedException e) {
            jda.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends a plain-text message to the configured chat channel. Reads chat.server
     * and chat.channel-id at send-time so hot-reloads take effect without a restart.
     */
    public void sendMessage(String text) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("chat.server", 1);
        String channelId = plugin.getConfig().getString("chat.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        channel.sendMessage(text).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send Discord message: " + err.getMessage())
        );
    }

    /**
     * Sends a plain-text staff chat message to the staff chat Discord channel.
     * Reads staff-chat.server and staff-chat.channel-id at send-time.
     */
    public void sendStaffChatText(String text) {
        if (!ready || jda == null) {
            plugin.getLogger().warning("[StaffChat] Bot is not ready yet — message not sent.");
            return;
        }

        int serverNumber = plugin.getConfig().getInt("staff-chat.server", 1);
        String channelId = plugin.getConfig().getString("staff-chat.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_STAFF_CHANNEL_ID_HERE")) {
            plugin.getLogger().warning("[StaffChat] staff-chat.channel-id is not configured in config.yml.");
            return;
        }

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) {
            plugin.getLogger().warning("[StaffChat] Could not resolve staff chat channel (server=" + serverNumber + ", channel-id=" + channelId + "). Check servers." + serverNumber + ".guild-id and that the channel ID is correct.");
            return;
        }

        channel.sendMessage(text).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send staff chat message to Discord: " + err.getMessage())
        );
    }

    /**
     * Sends a staff chat message as a Discord embed with a rendered item tooltip image attached.
     */
    public void sendStaffChatItemEmbed(String messageText, byte[] tooltipImage) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("staff-chat.server", 1);
        String channelId = plugin.getConfig().getString("staff-chat.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_STAFF_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        var embed = new CleanEmbedBuilder()
                .setDescription(messageText)
                .setImage("attachment://tooltip.png")
                .setColor(new Color(0xED, 0x42, 0x45)) // red tint for staff
                .build();

        channel.sendMessageEmbeds(embed)
                .addFiles(FileUpload.fromData(tooltipImage, "tooltip.png"))
                .queue(null, err -> plugin.getLogger().warning("Failed to send staff chat item embed: " + err.getMessage()));
    }

    /**
     * Sends a plain-text message to the server-messages channel.
     * Reads server-messages.server and server-messages.channel-id at send-time.
     */
    public void sendServerMessageText(String text) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("server-messages.server", 1);
        String channelId = plugin.getConfig().getString("server-messages.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        channel.sendMessage(text).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send server message to Discord: " + err.getMessage())
        );
    }

    /**
     * Sends a join or leave embed with the player's head as the thumbnail.
     * The head image is fetched from the mc-heads.net CDN using the player's UUID.
     */
    public void sendJoinLeaveEmbed(String playerName, UUID playerUuid, boolean joined) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("join-leave.server", 1);
        String channelId = plugin.getConfig().getString("join-leave.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        String configKey = joined ? "join-leave.join-format" : "join-leave.leave-format";
        String defaultFmt = joined
                ? ":green_circle: **{player}** joined the server."
                : ":red_circle: **{player}** left the server.";
        String description = plugin.getConfig().getString(configKey, defaultFmt)
                .replace("{player}", playerName);

        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";

        // Green for join, red for leave - matching Discord's status colours
        Color color = joined ? new Color(0x57, 0xF2, 0x87) : new Color(0xED, 0x42, 0x45);

        var embed = new CleanEmbedBuilder()
                .setDescription(description)
                .setThumbnail(headUrl)
                .setColor(color)
                .setTimestamp(java.time.Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send join/leave embed: " + err.getMessage())
        );
    }

    /**
     * Sends an AFK status embed with the player's head as the thumbnail.
     *
     * @param goingAfk true if the player is going AFK, false if they are returning
     */
    public void sendAfkEmbed(String playerName, UUID playerUuid, boolean goingAfk) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("afk.server", 1);
        String channelId = plugin.getConfig().getString("afk.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        String configKey  = goingAfk ? "afk.afk-format" : "afk.back-format";
        String defaultFmt = goingAfk ? ":zzz: **{player}** is now AFK." : ":wave: **{player}** is no longer AFK.";
        String description = plugin.getConfig().getString(configKey, defaultFmt)
                .replace("{player}", playerName);

        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";

        // Yellow for going AFK, light blue for returning
        Color color = goingAfk ? new Color(0xFF, 0xD7, 0x00) : new Color(0x00, 0xB0, 0xFF);

        var embed = new CleanEmbedBuilder()
                .setDescription(description)
                .setThumbnail(headUrl)
                .setColor(color)
                .setTimestamp(java.time.Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send AFK embed: " + err.getMessage())
        );
    }

    /**
     * Sends a chat message as a Discord embed with a rendered item tooltip image attached.
     *
     * @param authorDisplay  e.g. "[Admin] Steve"
     * @param messageText    the chat message (with [item] already replaced by item name inline)
     * @param tooltipImage   PNG bytes from ItemTooltipRenderer
     */
    public void sendItemEmbed(String authorDisplay, String messageText, byte[] tooltipImage) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("chat.server", 1);
        String channelId = plugin.getConfig().getString("chat.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        var embed = new CleanEmbedBuilder()
                .setAuthor("Item Display")
                .setDescription(messageText)
                .setImage("attachment://tooltip.png")
                .setColor(new Color(0x55, 0xFF, 0x55)) // Minecraft green
                .build();

        channel.sendMessageEmbeds(embed)
                .addFiles(FileUpload.fromData(tooltipImage, "tooltip.png"))
                .queue(null, err -> plugin.getLogger().warning("Failed to send item embed: " + err.getMessage()));
    }

    /**
     * Sends an auction house action embed to the configured auction log channel.
     *
     * @param action      the type of action (listed, sold, removed, admin removed)
     * @param itemName    display name of the item
     * @param amount      stack size
     * @param material    material/type name
     * @param price       raw price value
     * @param sellerName  username of the seller
     * @param secondParty buyer name (for SOLD) or admin name (for ADMIN_REMOVED), null otherwise
     */
    public void sendAuctionEmbed(AuctionAction action, String itemName, int amount,
                                 String material, long price, String sellerName, String secondParty) {
        if (!ready || jda == null) return;

        int serverNumber = plugin.getConfig().getInt("auction-house.server", 1);
        String channelId = plugin.getConfig().getString("auction-house.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_AUCTION_CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        Color color;
        String title;
        switch (action) {
            case LISTED        -> { color = new Color(0xFF, 0xD7, 0x00); title = "Item Listed"; }
            case SOLD          -> { color = new Color(0x57, 0xF2, 0x87); title = "Item Sold"; }
            case REMOVED       -> { color = new Color(0xED, 0x42, 0x45); title = "Listing Removed"; }
            case ADMIN_REMOVED -> { color = new Color(0xED, 0x42, 0x45); title = "Listing Removed by Admin"; }
            default            -> { color = Color.GRAY;                  title = "Auction House"; }
        }

        // Fetch the badge image (cached after first render)
        byte[] badge = null;
        try {
            badge = switch (action) {
                case LISTED        -> AuctionBadgeRenderer.getListed();
                case SOLD          -> AuctionBadgeRenderer.getSold();
                case REMOVED       -> AuctionBadgeRenderer.getRemoved();
                case ADMIN_REMOVED -> AuctionBadgeRenderer.getAdminRemoved();
            };
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to render auction badge: " + e.getMessage());
        }

        String priceFormatted = String.format("%,d", price);
        String itemDisplay = amount + "x " + ((itemName == null || itemName.isEmpty()) ? material : itemName);

        var embed = new CleanEmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .addField("Item", itemDisplay, true)
                .addField("Price", priceFormatted, true)
                .addField("Seller", sellerName, true)
                .setTimestamp(java.time.Instant.now());

        if (action == AuctionAction.SOLD && secondParty != null) {
            embed.addField("Buyer", secondParty, true);
        } else if (action == AuctionAction.ADMIN_REMOVED && secondParty != null) {
            embed.addField("Removed By", secondParty, true);
        }

        if (badge != null) {
            embed.setThumbnail("attachment://badge.png");
        }

        var msg = channel.sendMessageEmbeds(embed.build());
        if (badge != null) {
            msg = msg.addFiles(FileUpload.fromData(badge, "badge.png"));
        }
        msg.queue(
                null,
                err -> plugin.getLogger().warning("Failed to send auction embed: " + err.getMessage())
        );
    }

    /**
     * Updates the bot's Discord presence activity with the current online player count.
     */
    public void updatePresence() {
        if (!ready || jda == null) return;
        if (!plugin.getConfig().getBoolean("presence.enabled", true)) return;
        int online = plugin.getServer().getOnlinePlayers().size();
        int max    = plugin.getServer().getMaxPlayers();
        String format = plugin.getConfig().getString("presence.format", "{online}/{max} players online")
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.watching(format));
    }

    /**
     * Updates the chat channel's topic with the current online player count.
     * Discord rate-limits topic edits (~2 per 10 minutes); rapid bursts queue and apply the latest value.
     */
    public void updateChatChannelTopic() {
        if (!ready || jda == null) return;
        if (!plugin.getConfig().getBoolean("presence.update-chat-topic", true)) return;
        int online = plugin.getServer().getOnlinePlayers().size();
        int max    = plugin.getServer().getMaxPlayers();
        String topic = plugin.getConfig().getString("presence.topic-format", "{online}/{max} players online")
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
        int serverNumber = plugin.getConfig().getInt("chat.server", 1);
        String channelId = plugin.getConfig().getString("chat.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;
        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;
        channel.getManager().setTopic(topic).queue(
            null,
            err -> plugin.getLogger().warning("Failed to update chat channel topic: " + err.getMessage())
        );
    }

    /**
     * Sends a death message to the configured deaths channel.
     *
     * @param message the plain-text vanilla death message (formatting already stripped)
     */
    public void sendDeathMessage(String message) {
        if (!ready || jda == null) return;
        if (!plugin.getConfig().getBoolean("deaths.enabled", false)) return;
        int serverNumber = plugin.getConfig().getInt("deaths.server", 1);
        String channelId = plugin.getConfig().getString("deaths.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;
        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;
        String format = plugin.getConfig().getString("deaths.format", ":skull: {message}")
                .replace("{message}", message);
        channel.sendMessage(format).queue(
            null,
            err -> plugin.getLogger().warning("Failed to send death message: " + err.getMessage())
        );
    }

    /**
     * Resolves a TextChannel from the multi-guild servers map.
     *
     * Config shape:
     * <pre>
     * servers:
     *   1:
     *     guild-id: "111111111111111111"
     *   2:
     *     guild-id: "222222222222222222"
     * </pre>
     *
     * @param serverNumber the integer key in config.servers (e.g. 1 or 2)
     * @param channelId    the Discord channel ID string
     * @return the TextChannel, or null if not found (with a warning logged)
     */
    public TextChannel getTextChannel(int serverNumber, String channelId) {
        if (!ready || jda == null) return null;
        if (channelId == null || channelId.isEmpty()) return null;

        String guildId = plugin.getConfig().getString("servers." + serverNumber + ".guild-id", "");
        if (guildId.isEmpty()) {
            plugin.getLogger().warning("No guild-id configured for servers." + serverNumber + " in config.yml");
            return null;
        }

        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning("Bot is not a member of guild " + guildId + " (servers." + serverNumber + ")");
            return null;
        }

        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find channel " + channelId + " in guild " + guild.getName());
        }
        return channel;
    }

    /**
     * Sends a punishment embed when a player is banned.
     *
     * @param playerName   the banned player's name
     * @param playerUuid   the banned player's UUID
     * @param reason       the ban reason (may be null)
     * @param bannedBy     operator or "Console" who issued the ban (may be null)
     */
    public void sendPunishmentEmbed(String playerName, java.util.UUID playerUuid,
                                    String reason, String bannedBy) {
        if (!ready || jda == null) return;
        if (!plugin.getConfig().getBoolean("punishments.enabled", false)) return;

        int serverNumber = plugin.getConfig().getInt("punishments.server", 1);
        String channelId = plugin.getConfig().getString("punishments.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("CHANNEL_ID_HERE")) return;

        TextChannel channel = getTextChannel(serverNumber, channelId);
        if (channel == null) return;

        String safeReason  = (reason   != null && !reason.isEmpty())   ? reason   : "No reason provided";
        String safeBannedBy = (bannedBy != null && !bannedBy.isEmpty()) ? bannedBy : "Unknown";

        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";

        var embed = new CleanEmbedBuilder()
                .setTitle(":hammer: Player Banned")
                .setColor(new Color(0xED, 0x42, 0x45))
                .setThumbnail(headUrl)
                .addField("Player", playerName, true)
                .addField("UUID", playerUuid.toString(), false)
                .addField("Reason", safeReason, false)
                .addField("Banned By", safeBannedBy, true)
                .setTimestamp(java.time.Instant.now())
                .build();

        channel.sendMessageEmbeds(embed).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send punishment embed: " + err.getMessage())
        );
    }

    public JDA getJda() {
        return jda;
    }

    public boolean isReady() {
        return ready;
    }
}
