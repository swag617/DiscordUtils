package com.swag.discordutils.discord;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.listeners.AuctionHouseListener.AuctionAction;
import com.swag.discordutils.util.AuctionBadgeRenderer;
import net.dv8tion.jda.api.EmbedBuilder;
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

<<<<<<< HEAD
=======
    /**
     * Starts the JDA connection. Called from onEnable() on the main thread.
     * Does NOT call awaitReady() to avoid blocking the server.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
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
<<<<<<< HEAD
                    GatewayIntent.GUILD_MEMBERS); // privileged intent - enable in Discord Dev Portal

=======
                    GatewayIntent.GUILD_MEMBERS); // GUILD_MEMBERS is a privileged intent - enable in Discord Dev Portal

            // Inner ReadyListener sets the ready flag and logs the bot name
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
            builder.addEventListeners(new ListenerAdapter() {
                @Override
                public void onReady(ReadyEvent event) {
                    ready = true;
                    plugin.getLogger().info("DiscordUtils bot connected as: " + event.getJDA().getSelfUser().getName());
                    if (plugin.getConfig().getBoolean("announce-status", true)) {
                        sendMessage(":white_check_mark: **DiscordUtils** is now online.");
                    }
                }
            });

            for (Object listener : extraListeners) {
                builder.addEventListeners(listener);
            }

            jda = builder.build();
<<<<<<< HEAD
=======
            // Do NOT call awaitReady() here - it would block the main server thread
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect DiscordUtils bot: " + e.getMessage(), e);
        }
    }

<<<<<<< HEAD
=======
    /**
     * Gracefully shuts down the JDA connection. Called from onDisable().
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
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

<<<<<<< HEAD
=======
    /**
     * Sends a message to the configured channel. Reads channel-id at send-time
     * so hot-reloads of config take effect without a restart.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void sendMessage(String text) {
        if (!ready || jda == null) return;

        String channelId = plugin.getConfig().getString("channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            return;
        }

        channel.sendMessage(text).queue(
                null,
                err -> plugin.getLogger().warning("Failed to send Discord message: " + err.getMessage())
        );
    }

<<<<<<< HEAD
=======
    /**
     * Sends a join or leave embed with the player's head as the thumbnail.
     * The head image is fetched from the Crafatar CDN using the player's UUID.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void sendJoinLeaveEmbed(String playerName, UUID playerUuid, boolean joined) {
        if (!ready || jda == null) return;

        String channelId = plugin.getConfig().getString("channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            return;
        }

        String configKey = joined ? "join-leave.join-format" : "join-leave.leave-format";
        String defaultFmt = joined
                ? ":green_circle: **{player}** joined the server."
                : ":red_circle: **{player}** left the server.";
        String description = plugin.getConfig().getString(configKey, defaultFmt)
                .replace("{player}", playerName);

<<<<<<< HEAD
        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";
=======
        // Crafatar provides player head renders by UUID. The overlay=true flag
        // includes the outer helmet/hat layer, matching the in-game appearance.
        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";

        // Green for join, red for leave - matching Discord's status colours
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        Color color = joined ? new Color(0x57, 0xF2, 0x87) : new Color(0xED, 0x42, 0x45);

        var embed = new EmbedBuilder()
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

<<<<<<< HEAD
=======
    /**
     * Sends an AFK status embed with the player's head as the thumbnail.
     *
     * @param goingAfk true if the player is going AFK, false if they are returning
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void sendAfkEmbed(String playerName, UUID playerUuid, boolean goingAfk) {
        if (!ready || jda == null) return;

        String channelId = plugin.getConfig().getString("channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            return;
        }

        String configKey  = goingAfk ? "afk.afk-format" : "afk.back-format";
        String defaultFmt = goingAfk ? ":zzz: **{player}** is now AFK." : ":wave: **{player}** is no longer AFK.";
        String description = plugin.getConfig().getString(configKey, defaultFmt)
                .replace("{player}", playerName);

        String headUrl = "https://mc-heads.net/avatar/" + playerUuid + "/64";
<<<<<<< HEAD
=======

        // Yellow for going AFK, light blue for returning
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        Color color = goingAfk ? new Color(0xFF, 0xD7, 0x00) : new Color(0x00, 0xB0, 0xFF);

        var embed = new EmbedBuilder()
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

<<<<<<< HEAD
=======
    /**
     * Sends a chat message as a Discord embed with a rendered item tooltip image attached.
     *
     * @param authorDisplay  e.g. "[Admin] Steve"
     * @param messageText    the chat message (with [item] already replaced by item name inline)
     * @param tooltipImage   PNG bytes from ItemTooltipRenderer
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void sendItemEmbed(String authorDisplay, String messageText, byte[] tooltipImage) {
        if (!ready || jda == null) return;

        String channelId = plugin.getConfig().getString("channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_CHANNEL_ID_HERE")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            return;
        }

        var embed = new EmbedBuilder()
                .setAuthor("Item Display")
                .setDescription(messageText)
                .setImage("attachment://tooltip.png")
                .setColor(new Color(0x55, 0xFF, 0x55)) // Minecraft green
                .build();

        channel.sendMessageEmbeds(embed)
                .addFiles(FileUpload.fromData(tooltipImage, "tooltip.png"))
                .queue(null, err -> plugin.getLogger().warning("Failed to send item embed: " + err.getMessage()));
    }

<<<<<<< HEAD
=======
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
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void sendAuctionEmbed(AuctionAction action, String itemName, int amount,
                                 String material, long price, String sellerName, String secondParty) {
        if (!ready || jda == null) return;

        String channelId = plugin.getConfig().getString("auction-house.channel-id", "");
        if (channelId.isEmpty() || channelId.equals("YOUR_AUCTION_CHANNEL_ID_HERE")) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find auction log channel with ID: " + channelId);
            return;
        }

        Color color;
        String title;
        switch (action) {
            case LISTED        -> { color = new Color(0xFF, 0xD7, 0x00); title = "Item Listed"; }
            case SOLD          -> { color = new Color(0x57, 0xF2, 0x87); title = "Item Sold"; }
            case REMOVED       -> { color = new Color(0xED, 0x42, 0x45); title = "Listing Removed"; }
            case ADMIN_REMOVED -> { color = new Color(0xED, 0x42, 0x45); title = "Listing Removed by Admin"; }
            default            -> { color = Color.GRAY;                  title = "Auction House"; }
        }

<<<<<<< HEAD
=======
        // Fetch the badge image (cached after first render)
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
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
        String amountAndMaterial = amount + "x " + material;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .addField("Item Name", itemName.isEmpty() ? material : itemName, true)
                .addField("Amount & Material", amountAndMaterial, true)
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

    public JDA getJda() {
        return jda;
    }

    public boolean isReady() {
        return ready;
    }
}
