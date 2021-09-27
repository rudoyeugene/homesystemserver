package com.rudyii.hsw.actions.watchers;

import com.rudyii.hsw.objects.events.SimpleWatcherEvent;
import com.rudyii.hsw.services.system.EventService;
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
public class SimpleWatcher {
    private final EventService eventService;
    private final ThreadPoolTaskScheduler hswScheduler;
    private String checkingCommand, notificationTextFailure, notificationTextSuccess;
    private long period;
    private boolean lastRun = true;

    @Autowired
    public SimpleWatcher(EventService eventService, ThreadPoolTaskScheduler hswScheduler) {
        this.eventService = eventService;
        this.hswScheduler = hswScheduler;
    }

    @PostConstruct
    public void scheduleWatching() {
        hswScheduler.scheduleAtFixedRate(this::check, period * 1000);
        log.info("Scheduled SimpleWatcher task with action {} with fire rate every {} seconds", checkingCommand, period);
    }

    private void check() {
        CommandLine commandline = CommandLine.parse(checkingCommand);
        DefaultExecutor exec = new DefaultExecutor();
        try {
            int exitCode = exec.execute(commandline);
            reactOn(exitCode == 0);
        } catch (IOException e) {
            if (e instanceof ExecuteException) {
                reactOn(((ExecuteException) e).getExitValue() == 0);
            } else {
                log.error("Unexpected error for {}", checkingCommand, e);
            }
        }
    }

    private void reactOn(boolean thisRun) {
        if (lastRun != thisRun) {
            log.warn("Results of {} been changed from {} to {}", checkingCommand, lastRun, thisRun);
            this.lastRun = thisRun;
            notifyAboutChangesOf(thisRun);
        }
    }

    private void notifyAboutChangesOf(boolean thisRun) {
        if (thisRun) {
            eventService.publish(SimpleWatcherEvent.builder().notificationText(notificationTextSuccess).build());
        } else {
            eventService.publish(SimpleWatcherEvent.builder().notificationText(notificationTextFailure).build());
        }
    }
}
