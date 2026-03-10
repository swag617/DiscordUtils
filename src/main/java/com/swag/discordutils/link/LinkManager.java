package com.swag.discordutils.link;

import com.swag.discordutils.DiscordUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LinkManager {

    /** Characters used for the link code - excludes O/0 and I/1 to prevent confusion. */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    public record LinkStart(String code, String oauthUrl) {}

    private final DiscordUtils plugin;
    private final LinkDatabase db;
    private final HttpClient httpClient;

    /** code → PendingLink */
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();

    public LinkManager(DiscordUtils plugin, LinkDatabase db) {
        this.plugin = plugin;
        this.db = db;
        this.httpClient = HttpClient.newHttpClient();
    }

<<<<<<< HEAD
    public LinkStart startLink(UUID playerUuid) {
=======
    /**
     * Starts the link process for a player. Returns the code and OAuth2 URL to send in chat.
     */
    public LinkStart startLink(UUID playerUuid) {
        // Remove any existing pending code for this player
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        pending.entrySet().removeIf(e -> e.getValue().playerUuid().equals(playerUuid));

        String code = generateCode();
        pending.put(code, new PendingLink(playerUuid));

        String clientId  = plugin.getConfig().getString("link.client-id", "");
        String serverIp  = plugin.getConfig().getString("link.server-ip", "");
        int    port      = plugin.getConfig().getInt("link.port", 4567);
        String redirectUri = "http://" + serverIp + ":" + port + "/link";

        String oauthUrl = "https://discord.com/oauth2/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=identify"
                + "&state=" + code;

        return new LinkStart(code, oauthUrl);
    }

<<<<<<< HEAD
=======
    /**
     * Called by the HTTP server when Discord redirects back with an auth code.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public boolean completeLinkOAuth2(String state, String authCode) {
        plugin.getLogger().info("[Link] OAuth2 callback received. state=" + state);
        PendingLink link = pending.remove(state);
        if (link == null) {
            plugin.getLogger().warning("[Link] No pending link found for state: " + state);
            return false;
        }
        if (link.isExpired()) {
            plugin.getLogger().warning("[Link] Pending link for state " + state + " has expired.");
            return false;
        }

        try {
            String accessToken = exchangeCodeForToken(authCode);
            if (accessToken == null) {
                plugin.getLogger().warning("[Link] Token exchange returned null - check client-id/client-secret in config.yml.");
                return false;
            }

            String discordUserId = getDiscordUserId(accessToken);
            if (discordUserId == null) {
                plugin.getLogger().warning("[Link] Failed to retrieve Discord user ID from access token.");
                return false;
            }

            db.link(link.playerUuid(), discordUserId);
            assignRoleAsync(link.playerUuid(), discordUserId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(link.playerUuid());
                if (player != null) {
                    player.sendMessage("§aYour Discord account has been successfully linked!");
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("OAuth2 link failed: " + e.getMessage());
            return false;
        }
    }

<<<<<<< HEAD
=======
    /**
     * Called when a Discord user DMs the bot a link code.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public boolean completeLinkDM(String discordUserId, String code) {
        String upper = code.toUpperCase().trim();
        plugin.getLogger().info("[Link] DM code attempt. code=" + upper + " pendingKeys=" + pending.keySet());
        PendingLink link = pending.remove(upper);
        if (link == null) {
            plugin.getLogger().warning("[Link] No pending link found for DM code: " + upper);
            return false;
        }
        if (link.isExpired()) {
            plugin.getLogger().warning("[Link] DM code " + upper + " has expired.");
            return false;
        }

        try {
            db.link(link.playerUuid(), discordUserId);
            assignRoleAsync(link.playerUuid(), discordUserId);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(link.playerUuid());
                if (player != null) {
                    player.sendMessage("§aYour Discord account has been successfully linked!");
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("DM link failed: " + e.getMessage());
            return false;
        }
    }

<<<<<<< HEAD
=======
    /**
     * Syncs the player's Discord role to match their current LuckPerms group.
     * Should be called asynchronously (e.g. from PlayerJoinEvent).
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public void syncRoleAsync(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String discordId = db.getDiscordId(player.getUniqueId());
                if (discordId == null) return;
                assignRoleAsync(player.getUniqueId(), discordId);
            } catch (SQLException e) {
                plugin.getLogger().warning("Role sync failed for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

<<<<<<< HEAD
=======
    /**
     * Unlinks a player, removing their rank roles from Discord and deleting the DB record.
     * Returns false if the player was not linked.
     */
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
    public boolean unlink(UUID uuid) {
        try {
            String discordId = db.getDiscordId(uuid);
            if (discordId == null) return false;

            db.unlink(uuid);
            removeRankRolesAsync(discordId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Unlink failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    private void removeRankRolesAsync(String discordUserId) {
        String guildId = plugin.getConfig().getString("link.guild-id", "");
        if (guildId.isEmpty() || !plugin.getDiscordBot().isReady()) return;

        Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
        if (guild == null) return;

        List<String> rankNames = plugin.getConfig().getStringList("link.rank-role-names");
        List<Role> allRankRoles = guild.getRoles().stream()
                .filter(r -> rankNames.stream().anyMatch(n -> n.equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        guild.retrieveMemberById(discordUserId).queue(member -> {
            List<Role> toRemove = allRankRoles.stream()
                    .filter(r -> member.getRoles().contains(r))
                    .collect(Collectors.toList());
            if (!toRemove.isEmpty()) {
                guild.modifyMemberRoles(member, List.of(), toRemove).queue(
                        null,
                        err -> plugin.getLogger().warning("Failed to remove rank roles on unlink: " + err.getMessage())
                );
            }
        }, err -> plugin.getLogger().warning("Could not retrieve Discord member for role removal: " + err.getMessage()));
    }

    public boolean isLinked(UUID uuid) {
        try {
            return db.getDiscordId(uuid) != null;
        } catch (SQLException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void assignRoleAsync(UUID playerUuid, String discordUserId) {
        String guildId = plugin.getConfig().getString("link.guild-id", "");
        if (guildId.isEmpty() || !plugin.getDiscordBot().isReady()) return;

        Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning("Guild not found for ID: " + guildId);
            return;
        }

        String group = getPlayerGroup(playerUuid);
        if (group == null) return;

<<<<<<< HEAD
        // Exact match first, fuzzy fallback — prevents "Flea" from matching "Fleabot".
=======
        // Exact match first, fuzzy fallback so "Mod" still finds "Moderator"
        // but "Flea" never accidentally matches "Fleabot".
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        Role targetRole = guild.getRoles().stream()
                .filter(r -> r.getName().equalsIgnoreCase(group))
                .findFirst()
                .orElseGet(() -> guild.getRoles().stream()
                        .filter(r -> rolesMatch(group, r.getName()))
                        .findFirst()
                        .orElse(null));

        if (targetRole == null) {
            plugin.getLogger().warning("No Discord role found matching group '" + group + "'");
            return;
        }

<<<<<<< HEAD
=======
        // All configured rank roles — used to remove old rank before adding new one.
        // Use exact matching here so only explicitly listed roles are ever touched.
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        List<String> rankNames = plugin.getConfig().getStringList("link.rank-role-names");
        List<Role> allRankRoles = guild.getRoles().stream()
                .filter(r -> rankNames.stream().anyMatch(n -> n.equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        guild.retrieveMemberById(discordUserId).queue(member -> {
            List<Role> toRemove = allRankRoles.stream()
                    .filter(r -> !r.equals(targetRole) && member.getRoles().contains(r))
                    .collect(Collectors.toList());

            guild.modifyMemberRoles(member, List.of(targetRole), toRemove).queue(
                    null,
                    err -> plugin.getLogger().warning("Failed to modify Discord roles: " + err.getMessage())
            );
        }, err -> plugin.getLogger().warning("Failed to retrieve Discord member " + discordUserId + ": " + err.getMessage()));
    }

    private String getPlayerGroup(UUID playerUuid) {
        Chat chat = plugin.getVaultChat();
        if (chat == null) return null;

        Player online = Bukkit.getPlayer(playerUuid);
        if (online != null) {
            return chat.getPrimaryGroup(online);
        }

<<<<<<< HEAD
=======
        // Offline player fallback
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerUuid);
        try {
            return chat.getPrimaryGroup(null, offline);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not get group for offline player " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    private String exchangeCodeForToken(String authCode) throws Exception {
        String clientId     = plugin.getConfig().getString("link.client-id", "");
        String clientSecret = plugin.getConfig().getString("link.client-secret", "");
        String serverIp     = plugin.getConfig().getString("link.server-ip", "");
        int    port         = plugin.getConfig().getInt("link.port", 4567);
        String redirectUri  = "http://" + serverIp + ":" + port + "/link";

        String body = "client_id="     + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code"
                + "&code="         + URLEncoder.encode(authCode, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            plugin.getLogger().warning("Token exchange failed (" + response.statusCode() + "): " + response.body());
            return null;
        }

        return extractJsonString(response.body(), "access_token");
    }

    private String getDiscordUserId(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/users/@me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            plugin.getLogger().warning("Discord user lookup failed (" + response.statusCode() + "): " + response.body());
            return null;
        }

        return extractJsonString(response.body(), "id");
    }

    /** "Mod" matches "Moderator", "Admin" matches "Administrator", etc. */
    private boolean rolesMatch(String configName, String discordName) {
        String a = configName.toLowerCase();
        String b = discordName.toLowerCase();
        return a.equals(b) || b.startsWith(a) || a.startsWith(b);
    }

    /** Extracts a string value from a flat JSON object without any JSON library.
     *  Handles both {"key":"value"} and {"key": "value"} formats. */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        // skip optional whitespace and the colon
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) start++;
<<<<<<< HEAD
        if (start >= json.length() || json.charAt(start) != '"') return null;
        start++;
=======
        // now we expect an opening quote
        if (start >= json.length() || json.charAt(start) != '"') return null;
        start++; // skip opening quote
>>>>>>> 31bb7b49538eff7be8066ff17ceb9a55cf18290c
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public LinkDatabase getDb() {
        return db;
    }
}
