package com.swag.discordutils.listeners;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.util.FormattingUtil;
import fr.maxlego08.zauctionhouse.api.event.events.purchase.AuctionPrePurchaseItemEvent;
import fr.maxlego08.zauctionhouse.api.event.events.remove.AuctionRemoveExpiredItemEvent;
import fr.maxlego08.zauctionhouse.api.event.events.remove.AuctionRemoveListedItemEvent;
import fr.maxlego08.zauctionhouse.api.event.events.sell.AuctionPreSellEvent;
import fr.maxlego08.zauctionhouse.api.item.Item;
import fr.maxlego08.zauctionhouse.items.ZAuctionItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AuctionHouseListener implements Listener {

    private final DiscordUtils plugin;

    public AuctionHouseListener(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onList(AuctionPreSellEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        ItemStack stack = event.getItemStack();
        String itemName = getItemName(stack);
        String material = formatMaterial(stack.getType().name());
        String seller = event.getPlayer() != null ? event.getPlayer().getName() : "Unknown";
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.LISTED,
                itemName,
                event.getAmount(),
                material,
                event.getPrice().longValue(),
                seller,
                null
        );
    }

    // Fires when a player actually purchases a listing — use this for SOLD, not AuctionRemovePurchasedItemEvent
    // (which fires when the buyer collects the item later, not at point of purchase).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSold(AuctionPrePurchaseItemEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        Item item = event.getItem();
        String buyer = event.getPlayer() != null ? event.getPlayer().getName() : "Unknown";
        String itemName = getItemNameFromItem(item);
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.SOLD,
                itemName,
                item.getAmount(),
                itemName,
                item.getPrice().longValue(),
                item.getSellerName(),
                buyer
        );
    }

    // Only fires REMOVED for manual de-listings. Skip if the item has a buyer (it was purchased —
    // already handled by onSold above).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(AuctionRemoveListedItemEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        Item item = event.getItem();
        String buyer = item.getBuyerName();
        if (buyer != null && !buyer.isEmpty()) return; // purchased — skip, already sent SOLD
        String itemName = getItemNameFromItem(item);
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.REMOVED,
                itemName,
                item.getAmount(),
                itemName,
                item.getPrice().longValue(),
                item.getSellerName(),
                null
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExpire(AuctionRemoveExpiredItemEvent event) {
        if (!plugin.getConfig().getBoolean("auction-house.enabled", true)) return;
        Item item = event.getItem();
        String itemName = getItemNameFromItem(item);
        plugin.getDiscordBot().sendAuctionEmbed(
                AuctionAction.REMOVED,
                itemName,
                item.getAmount(),
                itemName,
                item.getPrice().longValue(),
                item.getSellerName(),
                null
        );
    }

    /**
     * Gets a clean display name from an Item by reading the underlying ItemStack directly.
     * ZAuctionItem stores the raw ItemStacks in getItemStacks(), which we access via a
     * cast — this bypasses getItemDisplay()'s opaque color-coded format entirely.
     * Falls back to stripping getItemDisplay() if the cast fails.
     */
    private String getItemNameFromItem(Item item) {
        if (item instanceof ZAuctionItem zItem) {
            List<ItemStack> stacks = zItem.getItemStacks();
            if (stacks != null && !stacks.isEmpty()) {
                return getItemName(stacks.get(0));
            }
        }
        // Fallback: strip formatting codes from the display string
        return FormattingUtil.stripFormatting(item.getItemDisplay());
    }

    private String getItemName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return "Air";
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String name = FormattingUtil.stripFormatting(meta.getDisplayName()).trim();
            if (!name.isEmpty()) return name;
        }
        return formatMaterial(stack.getType().name());
    }

    private String formatMaterial(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    public enum AuctionAction {
        LISTED, SOLD, REMOVED, ADMIN_REMOVED
    }
}
