# Punishment Logging

The punishment logging feature posts a red embed to a Discord channel whenever a player is banned from the server.

---

## Requirements

No extra plugin is required. DiscordUtils hooks into Bukkit's built-in `BanList` API, which is populated by the vanilla `/ban` command and any plugin that calls the Bukkit BanList internally.

---

## Configuration

```yaml
punishments:
  enabled: true
  server: 1
  channel-id: "YOUR_PUNISHMENTS_CHANNEL_ID_HERE"
```

| Key | Description |
|---|---|
| `enabled` | Set to `false` to disable ban embeds. |
| `server` | Which entry in the `servers` map to post to. |
| `channel-id` | Discord channel ID where ban embeds are sent. Typically a private staff or moderation channel. |

---

## Embed contents

Each ban embed includes the following fields:

| Field | Description |
|---|---|
| Player name | The in-game username of the banned player. |
| UUID | The player's UUID. |
| Reason | The ban reason as recorded in Bukkit's BanList. |
| Banned by | The source of the ban (operator name, plugin name, or `"Server"`). |
| Timestamp | The date and time the ban was applied. |
| Thumbnail | The player's head rendered via the Crafatar API. |

The embed is colored red to make it visually distinct from informational embeds.

---

## Limitations

- Only bans that go through Bukkit's `BanList` API are captured. Third-party punishment plugins that maintain their own database (e.g. LiteBans, AdvancedBan) are **not supported** unless they also call Bukkit's BanList alongside their own storage.
- Mutes, kicks, and temporary bans that bypass the Bukkit BanList are not logged.
- If the bot is not connected when a ban occurs, the embed is silently dropped.

---

## Multi-server routing

To post ban embeds to a dedicated moderation guild rather than the main guild, set `punishments.server` to the appropriate entry number in the `servers` map:

```yaml
punishments:
  server: 2
  channel-id: "444444444444444444"
```

See the [multi-server setup section](setup.md#multi-server--multi-guild-setup) in the setup guide for details.

---

## Related

- [Setup guide](setup.md)
- [Permissions](permissions.md)
