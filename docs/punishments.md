# Punishment Logging

The punishment logging feature posts a colored embed to a Discord channel whenever a player is punished. There are two independent sources:

1. **Vanilla Bukkit BanList** — no extra plugin required. DiscordUtils hooks into Bukkit's built-in `BanList` API, which is populated by the vanilla `/ban` command and any plugin that calls the Bukkit BanList internally. This path always reports type `BAN`.
2. **SwagCore's moderation system** (optional) — if SwagAPI/SwagCore is installed, DiscordUtils subscribes to SwagCore's `swagcore:player_punished` event-bus channel and also reports warns, mutes, kicks, and temp-bans issued through SwagCore's own DB-backed moderation, none of which touch the vanilla BanList and would otherwise be invisible to Discord.

---

## Requirements

The vanilla BanList path needs no extra plugin. The SwagCore path needs SwagAPI installed (soft dependency) with SwagCore publishing to its event bus; DiscordUtils no-ops this integration if SwagAPI's event bus service isn't present.

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

Each punishment embed includes the following fields:

| Field | Description |
|---|---|
| Player name | The in-game username of the punished player. |
| UUID | The player's UUID. |
| Reason | The punishment reason, or "No reason provided". |
| Staff / Banned By | The source of the punishment (operator name, `"Console"`, or plugin name). Labeled "Banned By" for BAN/TEMPBAN, "Staff" otherwise. |
| Duration | Shown only for timed punishments (e.g. TEMPBAN, TEMPMUTE); omitted for permanent ones. |
| Timestamp | The date and time the punishment was applied. |
| Thumbnail | The player's head rendered via the mc-heads.net API. |

The embed title and color vary by punishment type:

| Type | Title | Color |
|---|---|---|
| BAN / TEMPBAN | Player Banned / Player Temp-Banned | Red |
| MUTE / TEMPMUTE | Player Muted | Orange |
| KICK | Player Kicked | Orange |
| WARN | Player Warned | Yellow |

Vanilla BanList bans always report as type `BAN`; the other types (WARN/MUTE/KICK/TEMPBAN) only occur via the SwagCore event-bus integration described above.

---

## Limitations

- Only bans that go through Bukkit's `BanList` API are captured on the vanilla path. Third-party punishment plugins that maintain their own database (e.g. LiteBans, AdvancedBan) are **not supported** on that path unless they also call Bukkit's BanList alongside their own storage.
- Mutes, kicks, warns, and temporary bans are only logged if issued through SwagCore's moderation system (requires SwagAPI). Without SwagAPI installed, only vanilla BanList bans are logged.
- If the bot is not connected and no webhook is configured, the embed is silently dropped.

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
