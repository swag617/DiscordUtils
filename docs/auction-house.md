# Auction House Logging

The auction house integration logs listing, sale, and removal events from zAuctionHouse to a Discord channel as rich embeds.

---

## Requirements

- **zAuctionHouse** must be installed on the server.
- The plugin auto-detects any installed plugin whose name contains `"auction"` or equals `"ah"` (case-insensitive). No manual configuration flag is needed for detection — only `auction-house.enabled` controls whether events are sent.

---

## Configuration

```yaml
auction-house:
  enabled: true
  server: 1
  channel-id: "YOUR_AUCTION_CHANNEL_ID_HERE"
```

`channel-id` is the Discord channel where auction embeds are sent. This can be the same as the main `channel-id` or a dedicated `#auction-log` channel. Right-click the channel in Discord (with Developer Mode on) and select **Copy Channel ID**.

---

## Event types

Four event types are logged, each with a distinct embed color and badge icon:

| Event | Embed color | Badge | API event | Triggered by |
|---|---|---|---|---|
| New Listing | Yellow | Star | `AuctionPreSellEvent` | Player lists an item |
| Item Sold | Green | Checkmark | `AuctionPrePurchaseItemEvent` | Fires at the point of purchase |
| Listing Removed | Red | X | `AuctionRemoveListedItemEvent` | Seller manually removes their listing |
| Listing Expired | Gray | Clock | `AuctionRemoveExpiredItemEvent` | Listing expires without a sale |

---

## Embed fields

| Field | Present for |
|---|---|
| Item Name | All events |
| Amount & Material | All events |
| Price | All events |
| Seller | All events |
| Buyer | Item Sold only |

If the item has no custom display name, **Item Name** falls back to the material name.

---

## Limitations

- zAuctionHouse does not fire an event when a listing's price is changed, so **price changes are not logged**.
- If zAuctionHouse's rules engine denies a listing (e.g. a minimum price rule), the listing event (`AuctionPreSellEvent`) may still fire and log to Discord before the denial is processed. The item will appear in the Discord log even though the listing was ultimately rejected.
- If the bot is not connected when an event fires, the embed is silently dropped.

---

## Related

- [Setup guide](setup.md)
