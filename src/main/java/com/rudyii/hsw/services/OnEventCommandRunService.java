package com.rudyii.hsw.services;

import com.rudyii.hsw.events.ArmedEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class OnEventCommandRunService {
    private static Logger LOG = LogManager.getLogger(OnEventCommandRunService.class);

    @Resource(name = "onArmCommands")
    private List<String> onArmCommands;

    @Resource(name = "onDisarmCommands")
    private List<String> onDisarmCommands;

    @Resource(name = "onStartCommands")
    private List<String> onStartCommands;

    @Resource(name = "onStopCommands")
    private List<String> onStopCommands;

    @Resource(name = "onIspChangeCommands")
    private List<String> onIspChangeCommands;

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
