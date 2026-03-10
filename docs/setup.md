# Setup Guide

This guide walks through a complete installation from a fresh Discord application to a running bot.

---

## 1. Create a Discord Application and Bot

1. Go to <https://discord.com/developers/applications> and click **New Application**.
2. Give it a name (e.g. `DiscordUtils`) and confirm.
3. In the left sidebar, click **Bot**.
4. Click **Reset Token**, confirm, and copy the token. You will paste this into `config.yml` as `bot-token`. Keep it secret.

---

## 2. Enable Required Intents

On the same **Bot** page, scroll down to **Privileged Gateway Intents** and enable **both**:

- **Server Members Intent** — required for role management and member lookups.
- **Message Content Intent** — required for reading Discord message text. Without this, the bot receives empty strings for all non-bot messages.

Save your changes.

---

## 3. Invite the Bot to Your Server

1. In the left sidebar, click **OAuth2 > URL Generator**.
2. Under **Scopes**, select `bot`.
3. Under **Bot Permissions**, select:
   - Send Messages
   - Embed Links
   - Attach Files
   - Manage Roles
4. Copy the generated URL, open it in a browser, and authorize the bot into your server.

> **Role hierarchy:** If you use account linking, the bot's role in your Discord server must be positioned **above** any rank roles it will manage. Role assignment fails silently if the bot's role is lower than the target role.

---

## 4. Install the Plugin

1. Place `DiscordUtils.jar` in your server's `plugins/` directory.
2. Ensure **Vault** and **LuckPerms** (or another Vault-compatible Chat provider) are also installed.
3. Optional: install **CMI** for AFK detection, or **zAuctionHouse** for auction logging.

---

## 5. Configure config.yml

Start the server once to generate the default config, then stop it and edit `plugins/DiscordUtils/config.yml`.

### Minimum required settings

```yaml
bot-token: "YOUR_BOT_TOKEN_HERE"
channel-id: "YOUR_CHANNEL_ID_HERE"
```

To get your channel ID: enable Developer Mode in Discord (**User Settings > Advanced > Developer Mode**), then right-click the channel and select **Copy Channel ID**.

### Optional: status announcements

```yaml
announce-status: true
```

When enabled, the bot posts a message to the channel when it connects and disconnects.

### Full config reference

See the inline comments in `config.yml` for every available option. The key sections are:

| Section | Purpose |
|---|---|
| `formatting` | Minecraft→Discord message format, MiniMessage parsing |
| `discord-chat` | Discord→Minecraft relay settings |
| `join-leave` | Join/leave embed format |
| `afk` | AFK embed format |
| `server-messages` | Broadcast relay prefix filter |
| `auction-house` | Auction log channel and enable toggle |
| `link` | OAuth2 credentials for account linking |

---

## 6. Start the Server

Start the server and check the console. A successful connection looks like:

```
[DiscordUtils] Vault Chat hooked - rank prefixes enabled.
[DiscordUtils] DiscordUtils bot connected as: YourBotName#0000
```

If the bot token is wrong or the intents are not enabled, you will see an error in the console instead.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Bot connects but Discord messages show empty text | Message Content Intent not enabled |
| Role sync fails or roles not assigned | Server Members Intent not enabled, or bot role is below managed roles |
| `Could not find Discord channel with ID` | Wrong channel ID, or bot not in that server/channel |
| `link.client-id not configured` | Account linking not set up — `/discordlink` is unavailable until configured |
| No auction embeds | zAuctionHouse not detected, or `auction-house.enabled` is false |

---

Next: [Chat bridge configuration](chat-bridge.md)
