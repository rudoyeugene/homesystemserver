package com.rudyii.hsw.objects.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleWatcherEvent extends EventBase {
    private String notificationText;
}
