package com.rudyii.hsw.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BoardMonitor {
    private final List<String> monitorCommandsList;

    @Autowired
    public BoardMonitor(List monitorCommandsList) {
        this.monitorCommandsList = monitorCommandsList;
    }

    public ArrayList<String> getMonitoringResults() {
        if (monitorCommandsList == null) {
            return (ArrayList<String>) Collections.<String>emptyList();
        }

        AtomicInteger totalCommands = new AtomicInteger(monitorCommandsList.size());
        ArrayList<String> body = new ArrayList<>();

        monitorCommandsList.forEach(command -> {
            try {
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (StringUtils.isNotBlank((line = in.readLine()))) {
                    body.add(line);
                }
                process.waitFor();

                in.close();
            } catch (Exception e) {
                log.error("Failed on command: {}", command, e);
            } finally {
                totalCommands.getAndDecrement();
            }
        });

        return body;
    }
}
