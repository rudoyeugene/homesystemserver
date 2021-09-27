package com.rudyii.hsw.services.actions;

import com.rudyii.hsw.objects.events.ArmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@Service
public class OnEventCommandRunService {
    private final List<String> onArmCommands;
    private final List<String> onDisarmCommands;
    private final List<String> onStartCommands;
    private final List<String> onStopCommands;
    private final List<String> onIspChangeCommands;

    @Autowired
    public OnEventCommandRunService(List onArmCommands, List onDisarmCommands, List onStartCommands, List onStopCommands, List onIspChangeCommands) {

        this.onArmCommands = onArmCommands;
        this.onDisarmCommands = onDisarmCommands;
        this.onStartCommands = onStartCommands;
        this.onStopCommands = onStopCommands;
        this.onIspChangeCommands = onIspChangeCommands;
    }

    @PostConstruct
    private void executeOnStartCommands() {
        executeCommandList(onStartCommands);
    }

    @PreDestroy
    private void executeOnStopCommands() {
        executeCommandList(onStopCommands);
    }

    @PreDestroy
    private void executeOnIspChangeCommands() {
        executeCommandList(onIspChangeCommands);
    }

    @EventListener(ArmedEvent.class)
    public void executeOnArmDisarm(ArmedEvent event) {
        switch (event.getSystemState()) {
            case ARMED:
                executeCommandList(onArmCommands);
                break;
            case DISARMED:
                executeCommandList(onDisarmCommands);
                break;
            case RESOLVING:
                log.info("Ignoring ArmedEvent: " + event.getSystemState());
                break;
        }
    }

    private void executeCommandList(List<String> commandsList) {
        if (commandsList == null || commandsList.size() == 0) {
            return;
        }

        commandsList.forEach(command -> {
            try {
                Process process = Runtime.getRuntime().exec(String.valueOf(command));

                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while (StringUtils.isNotBlank((line = in.readLine()))) {
                    log.info(command + ": " + line);
                }

                process.waitFor();

                in.close();

                log.info(command + " execution success");
            } catch (Exception e) {
                log.error("Failed on command: " + command, e);
            }
        });
    }
}
