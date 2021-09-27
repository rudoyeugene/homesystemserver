package com.rudyii.hsw.services.system;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UptimeService {
    private static final String DAYS_FORMAT = "%02d days and %02d:%02d";
    private static final String NO_DAYS_FORMAT = "%02d:%02d";
    private final AtomicInteger uptime = new AtomicInteger();

    public int getUptimeMinutes() {
        return uptime.incrementAndGet();
    }

    public String getUptime() {
        int uptimeMinutes = uptime.get();
        int days = (int) TimeUnit.MINUTES.toDays(uptimeMinutes);
        if (days == 0) {
            return String.format(NO_DAYS_FORMAT,
                    TimeUnit.MINUTES.toHours(uptimeMinutes)
                            - TimeUnit.DAYS.toHours(TimeUnit.MINUTES.toDays(uptimeMinutes)),
                    TimeUnit.MINUTES.toMinutes(uptimeMinutes)
                            - TimeUnit.HOURS.toMinutes(TimeUnit.MINUTES.toHours(uptimeMinutes)));
        } else {
            return String.format(DAYS_FORMAT,
                    TimeUnit.MINUTES.toDays(uptimeMinutes),
                    TimeUnit.MINUTES.toHours(uptimeMinutes)
                            - TimeUnit.DAYS.toHours(TimeUnit.MINUTES.toDays(uptimeMinutes)),
                    TimeUnit.MINUTES.toMinutes(uptimeMinutes)
                            - TimeUnit.HOURS.toMinutes(TimeUnit.MINUTES.toHours(uptimeMinutes)));
        }
    }
}
