package com.rudyii.hsw.events;

public class ServerKeyUpdatedEvent extends EventBase {
    private String serverKey;

    public ServerKeyUpdatedEvent(String serverKey) {
        this.serverKey = serverKey;
    }

    public String getServerKey() {
        return serverKey;
    }
}
