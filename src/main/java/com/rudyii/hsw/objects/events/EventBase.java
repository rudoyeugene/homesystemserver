package com.rudyii.hsw.objects.events;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jack on 07.10.16.
 */
public abstract class EventBase {
    private String timeStamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    private Long eventTimeMillis = System.currentTimeMillis();

    EventBase() {
        System.out.println("New event of " + this.getClass().getSimpleName() + " created at: " + getEventAppeared());
    }

    public String getEventAppeared() {
        return this.timeStamp;
    }

    public Long getEventTimeMillis() {
        return eventTimeMillis;
    }
}