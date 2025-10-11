package com.example.objectversion.model;

import java.time.Instant;

public record AuditLog(
        String id,
        String entityType,
        String entityId,
        String action,
        String actor,
        Instant occurredAt,
        String diffJson
) {
}
