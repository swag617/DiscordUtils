<div align="center">

<br>

# ✦ DiscordUtils

<p align="center">
  <img src="https://img.shields.io/badge/Paper-1.20+-667eea?style=for-the-badge" alt="Paper 1.20+">
  <img src="https://img.shields.io/badge/Java-21-764ba2?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/JDA-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="JDA">
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-f0c060?style=for-the-badge" alt="License">
</p>

**A standalone Spigot/Paper plugin that bridges Minecraft and Discord using JDA.**
Chat bridging, account linking, item previews, and auction logging out of the box.

<br>

</div>

---

## ✦ Features

- **Chat bridge** — Minecraft chat relayed to Discord; Discord messages broadcast in-game
- **Item display** — typing `[item]` in chat renders an item tooltip image and attaches it to the Discord message
- **Join/leave embeds** — player join and leave events posted to Discord with their Minecraft avatar
- **AFK detection** — works with CMI or any plugin that broadcasts AFK status messages
- **Server message relay** — forward plugin broadcasts (mini-game events, etc.) to Discord by prefix filter
- **Account linking** — players link their Minecraft account to Discord via OAuth2; their LuckPerms rank is synced as a Discord role
- **Auction House logging** — new listings, sales, and removals logged to a Discord channel as embeds (requires zAuctionHouse)
- **Role display** — Discord→Minecraft messages show the sender's highest configured role
- **MiniMessage support** — players with permission can use MiniMessage tags and hex colors in chat

---

## ✦ Installation

1. Download `DiscordUtils.jar` from [Releases](https://github.com/swag617/DiscordUtils/releases)
2. Drop it into your server's `plugins/` folder alongside Vault and LuckPerms
3. Start the server once to generate `plugins/DiscordUtils/config.yml`, then stop it
4. Create a Discord bot at <https://discord.com/developers/applications>, enable both privileged intents, and paste the token into `bot-token`
5. Enable Developer Mode in Discord, right-click your bridge channel, copy its ID, and paste it into `channel-id`
6. Invite the bot with permissions: **Send Messages, Embed Links, Attach Files, Manage Roles**
7. Start the server — the bot will connect and post a status message to the channel

> **Requirements:** Paper / Spigot 1.20+ — Java 21 — Vault — LuckPerms
> **Bot intents required:** Enable **Message Content Intent** AND **Server Members Intent** (Privileged Gateway Intents) in the Discord Developer Portal.

See [docs/setup.md](docs/setup.md) for a full walkthrough.

---

## ✦ Dependencies

| Dependency | Required | Notes |
|---|---|---|
| Java 21 | Yes | |
| Paper / Spigot 1.20+ | Yes | |
| Vault | Yes | Provides rank prefixes and group lookup |
| LuckPerms | Yes | Vault's Chat provider; other permission plugins may work |
| CMI | No | AFK detection uses native CMI events when present |
| zAuctionHouse | No | Auction House logging only activates if detected |

---

## ✦ Commands

| Command | Description | Permission |
|---|---|---|
| `/discordchat allow <all\|admins>` | Set who can chat from Discord to Minecraft | `discordutils.admin` |
| `/discordchat <enable\|disable>` | Toggle the Discord→Minecraft relay | `discordutils.admin` |
| `/discordchat status` | Show bot connection and relay status | `discordutils.admin` |
| `/discordlink` | Link your Minecraft account to Discord | `discordutils.link` |
| `/discordunlink` | Unlink your Minecraft account from Discord | `discordutils.link` |

---

## ✦ Permissions

| Permission | Description | Default |
|---|---|---|
| `discordutils.admin` | Access to `/discordchat` | op |
| `discordutils.link` | Access to `/discordlink` and `/discordunlink` | true |
| `discordutils.formatting` | Use MiniMessage/hex color tags in chat | true |

---

## ✦ Documentation

- [Setup guide](docs/setup.md)
- [Chat bridge](docs/chat-bridge.md)
- [Account linking](docs/account-linking.md)
- [Auction House logging](docs/auction-house.md)
- [Permissions reference](docs/permissions.md)

---

<div align="center">

**Built by [Swag617](https://swag617.github.io/) · [Portfolio](https://swag617.github.io/) · [SwagMenus](https://github.com/swag617/SwagMenus)**

</div>
