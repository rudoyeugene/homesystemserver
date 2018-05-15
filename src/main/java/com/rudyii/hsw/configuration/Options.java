package com.rudyii.hsw.configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class Options {
    private ConcurrentHashMap<String, String> options = new ConcurrentHashMap<>();
}
