package io.snyk.agent.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Beacon {
    public final UUID projectId;
    public final Instant timestamp;
    public final MethodEntry[] eventsToSend;

    public Beacon(UUID projectId, Instant timestamp, MethodEntry[] eventsToSend) {
        this.projectId = projectId;
        this.timestamp = timestamp;
        this.eventsToSend = eventsToSend;
    }
}
