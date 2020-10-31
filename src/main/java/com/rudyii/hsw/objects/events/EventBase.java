package com.rudyii.hsw.objects.events;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class EventBase {
    private final String timeStamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    private final Long eventTimeMillis = System.currentTimeMillis();

    EventBase() {
        System.out.println("New event of " + this.getClass().getSimpleName() + " created at: " + getEventAppeared());
    }

    public String getEventAppeared() {
        return this.timeStamp;
    }

    public long getEventId() {
        return eventTimeMillis;
    }
}
