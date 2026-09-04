package com.swag.discordutils.link;

import com.swag.discordutils.DiscordUtils;

import java.sql.*;
import java.util.UUID;

public class LinkDatabase {

    // MIGRATED: connection field removed — pool owned by SwagAPI IDatabaseService
    private final com.SwagDev.SwagAPI.api.IDatabaseService dbService;

    public LinkDatabase(DiscordUtils plugin, com.SwagDev.SwagAPI.api.IDatabaseService dbService) throws SQLException {
        this.dbService = dbService;
        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement()) {
            // VARCHAR(191), not TEXT: MySQL rejects a BLOB/TEXT column used in a key
            // (PRIMARY KEY or UNIQUE) without an explicit length - a no-op for SQLite,
            // which doesn't enforce VARCHAR length.
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS links (" +
                "  uuid VARCHAR(191) PRIMARY KEY, " +
                "  discord_id VARCHAR(191) NOT NULL UNIQUE" +
                ")"
            );
        }
    }

    private Connection getConnection() throws SQLException {
        return dbService.getConnection();
    }

    public synchronized void link(UUID uuid, String discordId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                dbService.isMySQL()
                        ? "INSERT INTO links (uuid, discord_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id)"
                        : "INSERT OR REPLACE INTO links (uuid, discord_id) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.executeUpdate();
        }
    }

    public synchronized void unlink(UUID uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM links WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public synchronized String getDiscordId(UUID uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT discord_id FROM links WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("discord_id") : null;
            }
        }
    }

    public synchronized UUID getMinecraftUUID(String discordId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid FROM links WHERE discord_id = ?")) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        }
    }

    /** No-op — the connection pool is owned and closed by SwagAPI, not this plugin. */
    public synchronized void close() {
        // MIGRATED: pool is owned by SwagAPI — do not close it here.
    }
}
