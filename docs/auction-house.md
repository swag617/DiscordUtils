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
  channel-id: "YOUR_AUCTION_CHANNEL_ID_HERE"
```

`channel-id` is the Discord channel where auction embeds are sent. This can be the same as the main `channel-id` or a dedicated `#auction-log` channel. Right-click the channel in Discord (with Developer Mode on) and select **Copy Channel ID**.

---

## Event types

Four event types are logged, each with a distinct embed color and badge icon:

| Event | Embed color | Badge | Triggered by |
|---|---|---|---|
| New Listing | Yellow | Star | Player lists an item |
| Item Sold | Green | Checkmark | Player purchases a listing |
| Listing Removed | Red | X | Seller removes their own listing |
| Listing Removed by Admin | Red | Warning | Admin force-removes a listing |

`AuctionRemoveEvent` with `StorageType.BUY` is ignored — that case is already covered by `AuctionPostBuyEvent` (the Item Sold embed).

---

## Embed fields

| Field | Present for |
|---|---|
| Item Name | All events |
| Amount & Material | All events |
| Price | All events |
| Seller | All events |
| Buyer | Item Sold only |
| Removed By | Admin Remove only |

If the item has no custom display name, **Item Name** falls back to the material name.

---

## Limitations

- zAuctionHouse does not fire an event when a listing's price is changed, so **price changes are not logged**.
- If the bot is not connected when an event fires, the embed is silently dropped.

---

## Related

- [Setup guide](setup.md)
