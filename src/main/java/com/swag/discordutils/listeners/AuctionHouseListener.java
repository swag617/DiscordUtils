package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import fr.maxlego08.zauctionhouse.api.AuctionItem;
import fr.maxlego08.zauctionhouse.api.enums.StorageType;
import fr.maxlego08.zauctionhouse.api.event.events.AuctionAdminRemoveEvent;
import fr.maxlego08.zauctionhouse.api.event.events.AuctionPostBuyEvent;
import fr.maxlego08.zauctionhouse.api.event.events.AuctionRemoveEvent;
import fr.maxlego08.zauctionhouse.api.event.events.AuctionSellEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuctionHouseListener implements Listener {

    private final DiscordUtils plugin;

    public AuctionHouseListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onList(AuctionSellEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        AuctionItem item = event.getAuctionItem();
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.LISTED,
                item.getItemName(),
                item.getAmount(),
                item.getMaterialName(),
                item.getPrice(),
                item.getSellerName(),
                null
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBuy(AuctionPostBuyEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        AuctionItem item = event.getAuctionItem();
        String buyerName = event.getPlayer() != null ? event.getPlayer().getName() : "Unknown";
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.SOLD,
                item.getItemName(),
                item.getAmount(),
                item.getMaterialName(),
                item.getPrice(),
                item.getSellerName(),
                buyerName
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(AuctionRemoveEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        if (event.getType() == StorageType.BUY) return;
        AuctionItem item = event.getAuctionItem();
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.REMOVED,
                item.getItemName(),
                item.getAmount(),
                item.getMaterialName(),
                item.getPrice(),
                item.getSellerName(),
                null
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdminRemove(AuctionAdminRemoveEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        AuctionItem item = event.getAuctionItem();
        String adminName = event.getPlayer() != null ? event.getPlayer().getName() : "Console";
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.ADMIN_REMOVED,
                item.getItemName(),
                item.getAmount(),
                item.getMaterialName(),
                item.getPrice(),
                item.getSellerName(),
                adminName
        );
    }

    public enum AuctionAction {
        LISTED, SOLD, REMOVED, ADMIN_REMOVED
    }
}
