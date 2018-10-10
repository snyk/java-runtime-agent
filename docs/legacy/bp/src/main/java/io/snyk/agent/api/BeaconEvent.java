package io.snyk.agent.api;

public class BeaconEvent {
    public final MethodEntry methodEntry;

    public BeaconEvent(MethodEntry methodEntry) {
        this.methodEntry = methodEntry;
    }
}
