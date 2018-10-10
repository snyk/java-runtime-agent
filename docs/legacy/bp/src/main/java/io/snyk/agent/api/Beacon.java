package io.snyk.agent.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Beacon {
    public final UUID projectId;
    public final Instant timestamp;
    public final BeaconEvent[] eventsToSend;

    public Beacon(UUID projectId, Instant timestamp, MethodEntry[] methodEntries) {
        this.projectId = projectId;
        this.timestamp = timestamp;
        final BeaconEvent[] eventsToSend = new BeaconEvent[methodEntries.length];
        for (int i = 0; i < methodEntries.length; ++i) {
            eventsToSend[i] = new BeaconEvent(methodEntries[i]);
        }
        this.eventsToSend = eventsToSend;
    }
}
