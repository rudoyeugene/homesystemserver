package com.rudyii.hsw.objects.events;

public abstract class EventBase {
    private final Long eventTimeMillis = System.currentTimeMillis();

    public long getEventId() {
        return eventTimeMillis;
    }
}
