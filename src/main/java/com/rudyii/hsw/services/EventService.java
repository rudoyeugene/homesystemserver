package com.rudyii.hsw.services;

import com.rudyii.hsw.events.EventBase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

/**
 * Created by jack on 13.12.16.
 */
@Service
public class EventService implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    public void setApplicationEventPublisher
            (ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(EventBase event) {
        publisher.publishEvent(event);
    }
}
