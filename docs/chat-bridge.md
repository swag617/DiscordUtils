# Chat Bridge

The chat bridge has two directions: Minecraft→Discord and Discord→Minecraft. Each is configured independently.

---

## Minecraft → Discord

### Format string

Configured under `formatting.discord-send-format`. Default:

```yaml
formatting:
  discord-send-format: "**[{rank}] {player}**: {message}"
```

**Placeholders:**

| Placeholder | Value |
|---|---|
| `{rank}` | The player's Vault prefix (brackets stripped). Falls back to their primary LuckPerms group, then `Player` if Vault is unavailable. |
| `{player}` | The player's Minecraft username. |
| `{message}` | The chat message, with formatting stripped if `strip-for-discord: true`. |

### MiniMessage and formatting codes

```yaml
formatting:
  parse-minimessage: true   # parse <red>, <bold>, <#FF5733>, &a, etc.
  strip-for-discord: true   # strip color codes before sending to Discord
```

Players need the `discordutils.formatting` permission to use MiniMessage tags. If `strip-for-discord` is true, formatting is removed from the Discord copy while remaining visible in-game.

### Item display

If a player types `[item]` anywhere in their message, the plugin checks the item in their main hand:

- If their hand is empty, `[item]` is replaced with `[Air]` and sent as a plain message.
- If they are holding an item, `[item]` is replaced with the item's display name (or formatted material name), and a rendered tooltip image is attached to the Discord message as an embed.

The item name in the embed uses the custom display name if set, otherwise a human-readable version of the material name (e.g. `DIAMOND_SWORD` → `Diamond Sword`).

---

## Discord → Minecraft

### Enable/disable

```yaml
discord-chat:
  enabled: true
```

Can also be toggled at runtime with `/discordchat enable` and `/discordchat disable`.

### Access control

```yaml
discord-chat:
  allow-everyone: true        # if false, only users with the admin role can chat
  admin-role-name: "Admin"    # role name used when allow-everyone is false
  admin-role-id: ""           # role ID takes priority over name if set
```

Toggle at runtime:
- `/discordchat allow all` — sets `allow-everyone: true`
- `/discordchat allow admins` — sets `allow-everyone: false`

### Format string

```yaml
discord-chat:
  format: "&7[&bDiscord&7] &b[{role}] {username}&7: &f{message}"
```

**Placeholders:**

| Placeholder | Value |
|---|---|
| `{role}` | The sender's highest Discord role from the `display-roles` list. Falls back to `Member` if none match. |
| `{username}` | The sender's effective server nickname, or their username if no nickname is set. |
| `{message}` | The Discord message content. |

The format string supports `&` color codes and MiniMessage tags.

### Role display

Only roles listed in `discord-chat.display-roles` are eligible for the `{role}` placeholder. Discord's own role position hierarchy determines which is "highest" — the order in the config list does not matter.

```yaml
discord-chat:
  display-roles:
    - Owner
    - Manager
    - Admin
    - Mod
    - Helper
    - Support
```

If a member has none of the listed roles, `{role}` renders as `Member`.

### Fuzzy role matching

Role matching uses two passes:

1. **Exact match** (case-insensitive) — `"Admin"` matches the Discord role `"admin"` or `"ADMIN"`.
2. **Fuzzy match** — one name is a prefix of the other. `"Mod"` matches `"Moderator"`; `"Helper"` matches `"Helper-Trial"`.

Exact match always wins over fuzzy, so `"Flea"` will not accidentally match `"Fleabot"`.

### Discord markdown conversion

```yaml
discord-chat:
  convert-discord-markdown: true
```

When enabled, Discord formatting is converted to Minecraft equivalents before broadcast:

| Discord | Minecraft |
|---|---|
| `**bold**` | `§l` bold |
| `*italic*` or `_italic_` | `§o` italic |
| `~~strikethrough~~` | `§m` strikethrough |
| `__underline__` | `§n` underline |

---

## Related

- [Permissions reference](permissions.md)
- [Setup guide](setup.md)
