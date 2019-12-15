package com.rudyii.hsw.helpers;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class Uptime {
    private Long startUpTimeStamp = System.currentTimeMillis();

    public Long getUptimeLong() {
        return System.currentTimeMillis() - startUpTimeStamp;
    }

    public String getUptime() {
        if (startUpTimeStamp <= 0L) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        } else {
            long uptime = System.currentTimeMillis() - startUpTimeStamp;
            long days = TimeUnit.MILLISECONDS.toDays(uptime);
            long hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24L;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60L;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60L;

            StringBuilder builder = new StringBuilder();

            if (days == 1) {
                builder.append((String.format("%s Day ", days)));
            } else if (days > 1) {
                builder.append((String.format("%s Days ", days)));
            }

            if (hours == 1) {
                builder.append((String.format("%s Hour ", hours)));
            } else if (hours > 1) {
                builder.append((String.format("%s Hours ", hours)));
            }

            if (minutes == 1) {
                builder.append((String.format("%s Minute ", minutes)));
            } else if (minutes > 1) {
                builder.append((String.format("%s Minutes ", minutes)));
            }

            if (seconds == 1) {
                builder.append((String.format("%s Second ", seconds)));
            } else if (seconds > 1) {
                builder.append(String.format("%s Seconds", seconds));
            }

            return builder.toString();
        }
    }
}
