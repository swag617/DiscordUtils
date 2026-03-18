# Permissions Reference

| Permission | Description | Default |
|---|---|---|
| `discordutils.admin` | Grants access to the `/discordchat` command (allow, enable, disable, status subcommands). | op |
| `discordutils.link` | Grants access to `/discordlink` and `/discordunlink`. | true (all players) |
| `discordutils.formatting` | Allows the player to use MiniMessage tags (`<red>`, `<bold>`, `<#FF5733>`) and `&` color codes in Minecraft chat. Messages are formatted in-game; formatting is stripped before relaying to Discord if `formatting.strip-for-discord` is true. | true (all players) |
| `discordutils.staffchat` | Allows receiving Discord staff chat messages in-game. Players without this permission will not see messages relayed from the staff Discord channel. | op |

---

## Notes

- `discordutils.link` does nothing if `link.client-id` is not configured in `config.yml`. The link system is only initialized when a valid client ID is present.
- `discordutils.formatting` gates MiniMessage parsing in `MinecraftChatListener`. Players without this permission have their chat sent as-is without any tag substitution.
- `discordutils.staffchat` also controls which players can send messages into staff chat via the StaffChat plugin. It does not affect what is forwarded from Minecraft to Discord — only what is relayed back from Discord to Minecraft.
- All permissions can be managed through LuckPerms or any other Vault-compatible permissions plugin.

---

## Related

- [Setup guide](setup.md)
- [Chat bridge](chat-bridge.md)
- [Account linking](account-linking.md)
- [Staff chat bridge](staff-chat.md)
