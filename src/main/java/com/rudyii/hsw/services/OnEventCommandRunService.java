package com.rudyii.hsw.services;

import com.rudyii.hsw.objects.events.ArmedEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class OnEventCommandRunService {
    private static Logger LOG = LogManager.getLogger(OnEventCommandRunService.class);

    private List<String> onArmCommands;
    private List<String> onDisarmCommands;
    private List<String> onStartCommands;
    private List<String> onStopCommands;
    private List<String> onIspChangeCommands;

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
        switch (event.getArmedState()) {
            case ARMED:
                executeCommandList(onArmCommands);
                break;
            case DISARMED:
                executeCommandList(onDisarmCommands);
                break;
            case AUTO:
                LOG.info("Ignoring ArmedEvent: " + event.getArmedState());
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
                    LOG.info(command + ": " + line);
                }

                process.waitFor();

                in.close();

                LOG.info(command + " execution success");
            } catch (Exception e) {
                LOG.error("Failed on command: " + String.valueOf(command), e);
            }
        });
    }
}
