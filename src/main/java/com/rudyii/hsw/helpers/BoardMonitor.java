package com.rudyii.hsw.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BoardMonitor {
    private ThreadPoolTaskExecutor hswExecutor;
    private final Map monitorCommandList;

    @Autowired
    public BoardMonitor(ThreadPoolTaskExecutor hswExecutor, Map monitorCommandsList) {
        this.hswExecutor = hswExecutor;
        this.monitorCommandList = monitorCommandsList;
    }

    public ArrayList<String> getMonitoringResults() {
        if (monitorCommandList == null) {
            return (ArrayList<String>) Collections.<String>emptyList();
        }

        AtomicInteger totalCommands = new AtomicInteger(monitorCommandList.size());
        ArrayList<String> body = new ArrayList<>();

        monitorCommandList.forEach((key, value) -> hswExecutor.submit(() -> {
            try {
                Process process = Runtime.getRuntime().exec(String.valueOf(value));

                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (StringUtils.isNotBlank((line = in.readLine()))) {
                    body.add(key + line);
                }
                process.waitFor();

                in.close();
            } catch (Exception e) {
                log.error("Failed on command: {}", key, e);
            } finally {
                totalCommands.getAndDecrement();
            }
        }));

        while (totalCommands.get() != 0) {
            //STUB
        }

        return body;
    }
}
