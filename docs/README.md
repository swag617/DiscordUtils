# DiscordUtils

A standalone Spigot/Paper plugin that bridges Minecraft and Discord using JDA. No DiscordSRV required.

## Features

- **Chat bridge** — Minecraft chat relayed to Discord; Discord messages broadcast in-game
- **Item display** — typing `[item]` in chat renders an item tooltip image and attaches it to the Discord message
- **Join/leave embeds** — player join and leave events posted to Discord with their Minecraft avatar
- **AFK detection** — works with CMI or any plugin that broadcasts AFK status messages
- **Server message relay** — forward plugin broadcasts (mini-game events, etc.) to Discord by prefix filter
- **Account linking** — players link their Minecraft account to Discord via OAuth2; LuckPerms rank synced as a Discord role
- **Auction House logging** — listings, sales, and removals logged to Discord as embeds (requires zAuctionHouse)
- **Role display** — Discord→Minecraft messages show the sender's highest configured role
- **MiniMessage support** — players with permission can use MiniMessage tags and hex colors in chat

## Requirements

| Dependency | Required | Notes |
|---|---|---|
| Java 21 | Yes | |
| Paper / Spigot 1.20+ | Yes | |
| Vault | Yes | Provides rank prefixes and group lookup |
| LuckPerms | Yes | Vault's Chat provider; other permission plugins may work |
| CMI | No | AFK detection uses native CMI events when present |
| zAuctionHouse | No | Auction House logging only activates if detected |

> **Bot intents required:** Enable **Message Content Intent** AND **Server Members Intent** in the [Discord Developer Portal](https://discord.com/developers/applications).

## Quick Links

- [Installation & Setup](setup.md)
- [Chat Bridge](chat-bridge.md)
- [Account Linking](account-linking.md)
- [Auction House Logging](auction-house.md)
- [Permissions](permissions.md)
