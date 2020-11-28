package com.rudyii.hsw.actions.watchers;

import com.rudyii.hsw.objects.events.SimpleWatcherEvent;
import com.rudyii.hsw.services.EventService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Slf4j
@Data
@Builder
@AllArgsConstructor
public class SimpleWatcher {
    private String checkingCommand, notificationTextFailure, notificationTextSuccess;
    private long period;
    private int lastExitCode = 0;
    private EventService eventService;
    private ThreadPoolTaskScheduler hswScheduler;

    @Autowired
    public SimpleWatcher(EventService eventService, ThreadPoolTaskScheduler hswScheduler) {
        this.eventService = eventService;
        this.hswScheduler = hswScheduler;
    }

    @PostConstruct
    public void scheduleWatching() {
        hswScheduler.scheduleAtFixedRate(this::check, period * 1000);
    }

    private void check() {
        CommandLine commandline = CommandLine.parse(checkingCommand);
        DefaultExecutor exec = new DefaultExecutor();
        try {
            int exitCode = exec.execute(commandline);
            reactOn(exitCode);
        } catch (IOException e) {
            if (e instanceof ExecuteException) {
                reactOn(((ExecuteException) e).getExitValue());
            } else {
                log.error("Unexpected error for {}", checkingCommand, e);
            }
        }
    }

    private void reactOn(int exitCode) {
        if (lastExitCode != exitCode) {
            log.warn("Results of {} been changed from {} to {}", checkingCommand, lastExitCode, exitCode);
            lastExitCode = exitCode;
            notifyAboutChanges();
        }
    }

    private void notifyAboutChanges() {
        if (lastExitCode == 0) {
            eventService.publish(SimpleWatcherEvent.builder().notificationText(notificationTextSuccess).build());
        } else {
            eventService.publish(SimpleWatcherEvent.builder().notificationText(notificationTextFailure).build());
        }
    }
}
