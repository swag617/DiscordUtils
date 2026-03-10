# Account Linking

Account linking connects a player's Minecraft UUID to their Discord user ID. Once linked, the plugin assigns the player's LuckPerms group as a Discord role and keeps it in sync on every join.

---

## What linking does

- Stores a persistent UUID↔Discord ID mapping in a local SQLite database (`plugins/DiscordUtils/links.db`).
- Assigns the Discord role whose name matches the player's primary LuckPerms group.
- Removes any other managed rank roles from the player when updating (only one rank role is held at a time).
- Re-syncs the Discord role every time the player joins the server.
- Removing the link (`/discordunlink`) strips all managed rank roles from the player's Discord account.

The link system only initializes if `link.client-id` is set to a non-placeholder value. If it is not configured, `/discordlink` and `/discordunlink` are unavailable.

---

## Prerequisites

1. A Discord application with OAuth2 configured (same application as your bot, or a separate one).
2. Port `4567` (or your chosen port) open and reachable from the internet on your server machine.
3. Vault + LuckPerms installed (the plugin reads the player's primary group via Vault's Chat API).

---

## Discord OAuth2 Setup

1. Go to <https://discord.com/developers/applications> and open your application.
2. In the left sidebar, click **OAuth2 > General**.
3. Note your **Client ID** and generate (or copy) a **Client Secret**.
4. Under **Redirects**, add:
   ```
   http://YOUR_SERVER_IP:4567/link
   ```
   Replace `YOUR_SERVER_IP` with the public IP or domain of your Minecraft server and `4567` with your chosen port if different.
5. Save changes.

---

## config.yml — link section

```yaml
link:
  client-id: "YOUR_CLIENT_ID_HERE"
  client-secret: "YOUR_CLIENT_SECRET_HERE"
  server-ip: "123.456.789.0"   # public IP or domain, no http:// prefix
  port: 4567
  guild-id: "YOUR_GUILD_ID_HERE"
  rank-role-names:
    - Axolotl
    - Lizard
    - Flea
```

**`rank-role-names`** is the list of Discord role names that the plugin manages. These must match the role names in your Discord server exactly (case-insensitive). The plugin assigns the role whose name matches the player's LuckPerms group.

> **Role hierarchy:** The bot's own role in Discord must be positioned **above** all roles listed in `rank-role-names`. Discord prevents bots from managing roles that are equal to or higher than their own.

To get your Guild ID: enable Developer Mode in Discord, right-click your server icon, and select **Copy Server ID**.

---

## Linking a Minecraft account

Players run `/discordlink` in-game. The plugin responds with:

1. A clickable chat message that opens the Discord OAuth2 authorization page in the browser.
2. A backup 8-character code for players who cannot click the link.

```
[Discord Link] Click here to link your Discord account
Can't click? DM the bot this code: A3BX9K2M
This code expires in 10 minutes.
```

### OAuth2 web flow

1. Player clicks the link → browser opens the Discord authorization page.
2. Player authorizes the application.
3. Discord redirects to `http://YOUR_SERVER_IP:4567/link` with an authorization code.
4. The plugin exchanges the code for an access token, retrieves the player's Discord user ID, saves the link, and assigns the role.
5. The player sees a success page in the browser and a green message in Minecraft.

### DM code flow

1. Player copies the 8-character code shown in chat.
2. Player opens a DM with the bot in Discord and sends the code.
3. The bot replies with a success or failure message and assigns the role.

Codes expire after **10 minutes**. Expired or invalid codes produce an error; the player must run `/discordlink` again.

---

## Unlinking

```
/discordunlink
```

Removes the stored link and strips all rank roles listed in `rank-role-names` from the player's Discord account.

---

## Role sync on join

Every time a linked player joins the server, the plugin asynchronously re-checks their LuckPerms group and updates their Discord role. This keeps Discord roles accurate if a player's rank changes while they are offline.

---

## Related

- [Permissions reference](permissions.md)
- [Setup guide](setup.md)
