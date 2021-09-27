package com.rudyii.hsw.objects.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SimpleWatcherEvent extends EventBase {
    private String notificationText;
}
