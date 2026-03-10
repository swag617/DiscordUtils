package com.swag.discordutils;

import com.swag.discordutils.commands.DiscordChatCommand;
import com.swag.discordutils.commands.DiscordLinkCommand;
import com.swag.discordutils.commands.DiscordUnlinkCommand;
import com.swag.discordutils.discord.DiscordBot;
import com.swag.discordutils.link.LinkDatabase;
import com.swag.discordutils.link.LinkHttpServer;
import com.swag.discordutils.link.LinkManager;
import com.swag.discordutils.listeners.DiscordMessageListener;
import com.swag.discordutils.listeners.AuctionHouseListener;
import com.swag.discordutils.listeners.CmiAfkListener;
import com.swag.discordutils.listeners.MinecraftChatListener;
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

        setupVault();
        setupLinkSystem();

        discordBot = new DiscordBot(this);

<<<<<<< HEAD
        DiscordMessageListener discordMessageListener = new DiscordMessageListener(this);
        discordBot.connect(discordMessageListener);

=======
        // Register the JDA listener before connecting so events aren't missed
        DiscordMessageListener discordMessageListener = new DiscordMessageListener(this);
        discordBot.connect(discordMessageListener);

        // Bukkit listeners
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        getServer().getPluginManager().registerEvents(new MinecraftChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        setupAfkListener();
        setupAuctionHouseListener();

        DiscordChatCommand cmd = new DiscordChatCommand(this);
        getCommand("discordchat").setExecutor(cmd);
        getCommand("discordchat").setTabCompleter(cmd);

        getCommand("discordlink").setExecutor(new DiscordLinkCommand(this));
        getCommand("discordunlink").setExecutor(new DiscordUnlinkCommand(this));

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
            getServer().getPluginManager().registerEvents(new AuctionHouseListener(this), this);
            getLogger().info("Auction House plugin detected - auction logging enabled.");
        } else {
            getLogger().info("No Auction House plugin found - auction logging disabled.");
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
