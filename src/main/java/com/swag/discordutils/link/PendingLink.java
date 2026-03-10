package com.swag.discordutils.link;

import java.util.UUID;

public record PendingLink(UUID playerUuid, long createdAt) {

    private static final long EXPIRY_MS = 10 * 60 * 1000L; // 10 minutes

    public PendingLink(UUID playerUuid) {
        this(playerUuid, System.currentTimeMillis());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRY_MS;
    }
}
