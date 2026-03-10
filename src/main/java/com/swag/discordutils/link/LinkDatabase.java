package com.swag.discordutils.link;

import com.swag.discordutils.DiscordUtils;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class LinkDatabase {

    private final Connection conn;

    public LinkDatabase(DiscordUtils plugin) throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "links.db");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS links (" +
                "  uuid TEXT PRIMARY KEY, " +
                "  discord_id TEXT NOT NULL UNIQUE" +
                ")"
            );
        }
    }

    public void link(UUID uuid, String discordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO links (uuid, discord_id) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.executeUpdate();
        }
    }

    public void unlink(UUID uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM links WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public String getDiscordId(UUID uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT discord_id FROM links WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("discord_id") : null;
            }
        }
    }

    public UUID getMinecraftUUID(String discordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid FROM links WHERE discord_id = ?")) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        }
    }

    public void close() {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}
