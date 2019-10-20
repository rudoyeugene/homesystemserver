package com.rudyii.hsw.services;

import com.rudyii.hsw.objects.events.EventBase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

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
