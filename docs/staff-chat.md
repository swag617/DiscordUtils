# Staff Chat Bridge

The staff chat bridge relays messages between in-game staff chat and a private Discord channel in both directions. Only players with the configured permission see messages coming from Discord.

---

## Requirements

- **StaffChat** by dev.pace is the recommended plugin. When detected, DiscordUtils hooks into its native events and toggle system (`/sctoggle`).
- If StaffChat is not installed, DiscordUtils falls back to **broadcast prefix matching**: any `BroadcastMessageEvent` whose message starts with one of the prefixes listed in `staff-chat.relay-prefixes` is treated as a staff chat message and forwarded to Discord.

---

## Configuration

```yaml
staff-chat:
  enabled: true
  server: 1
  channel-id: "YOUR_STAFF_CHANNEL_ID_HERE"
  channel-type: "staff"
  discord-format: "[Staff] {player}: {message}"
  discord-to-minecraft-format: "&9[Discord Staff] &f[{role}] {username}&7: {message}"
  permission: "discordutils.staffchat"
  relay-prefixes:
    - "[Staff]"
    - "[SC]"
```

| Key | Description |
|---|---|
| `enabled` | Set to `false` to disable the bridge entirely. |
| `server` | Which entry in the `servers` map to post to. |
| `channel-id` | Discord channel ID for the private staff channel. |
| `channel-type` | Set to `"staff"` to use the StaffChat plugin's channel. Only relevant when StaffChat is installed. |
| `discord-format` | Format of the message posted to Discord. Supports `{player}` and `{message}` placeholders. |
| `discord-to-minecraft-format` | Format of the in-game message shown when a Discord user sends a message in the staff channel. Supports `{role}`, `{username}`, and `{message}` placeholders. Standard `&` color codes are supported. |
| `permission` | Permission required to receive Discord→Minecraft staff messages in-game. Defaults to `discordutils.staffchat`. |
| `relay-prefixes` | List of message prefixes used by the broadcast fallback. Only used when StaffChat is not installed. |

---

## How it works

### With StaffChat (dev.pace)

When the StaffChat plugin is present, DiscordUtils registers listeners against its event API. Players toggle into staff chat using `/sctoggle` as normal. Any message they send in staff chat is forwarded to the configured Discord channel using `discord-format`. Incoming Discord messages are broadcast in-game to all online players who have the `permission` node.

### Broadcast fallback

When StaffChat is not present, DiscordUtils listens to `BroadcastMessageEvent`. If the broadcast message starts with any of the strings in `relay-prefixes`, it is treated as a staff message and forwarded to Discord. This provides basic compatibility with other staff chat plugins without requiring native integration.

---

## [item] tooltips

Staff chat supports the same `[item]` embed syntax as the regular chat bridge. A player holding an item and typing `[item]` in staff chat will have the placeholder replaced with a hover-able tooltip embed in Discord, showing the item name and material.

---

## Discord → Minecraft

Messages sent in the configured Discord staff channel are relayed in-game using `discord-to-minecraft-format`. Only players who have the `permission` permission node will see these messages. Players without the permission do not receive any indication that a message was sent.

---

## Multi-server routing

To post staff messages to a separate moderation guild rather than the main guild, set `staff-chat.server` to the appropriate entry number in the `servers` map:

```yaml
staff-chat:
  server: 2
  channel-id: "555555555555555555"
```

See the [multi-server setup section](setup.md#multi-server--multi-guild-setup) in the setup guide for details.

---

## Related

- [Setup guide](setup.md)
- [Permissions](permissions.md)
- [Chat bridge](chat-bridge.md)
