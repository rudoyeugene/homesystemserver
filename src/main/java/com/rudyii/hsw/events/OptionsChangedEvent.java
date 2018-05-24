package com.rudyii.hsw.events;

import java.util.concurrent.ConcurrentHashMap;

public class OptionsChangedEvent extends EventBase {
    private ConcurrentHashMap<String, Object> options;

    public OptionsChangedEvent(ConcurrentHashMap<String, Object> options) {
        this.options = options;
    }

    public Object getOption(String option) {
        return options.get(option);
    }
}
