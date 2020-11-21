package com.rudyii.hsw.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        ArrayList<String> body = new ArrayList<>();

        monitorCommandsList.forEach(command -> {

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                CommandLine commandline = CommandLine.parse(command);
                DefaultExecutor exec = new DefaultExecutor();
                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
                exec.setStreamHandler(streamHandler);
                exec.execute(commandline);

                body.addAll(List.of(outputStream.toString().split(System.getProperty("line.separator"))));
            } catch (Exception e) {
                log.error("Failed on command: {}", command, e);
            }
        });

        return body;
    }
}
