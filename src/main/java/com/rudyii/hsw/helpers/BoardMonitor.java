package com.rudyii.hsw.helpers;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jack on 09.07.17.
 */
@Component
public class BoardMonitor {
    private static Logger LOG = LogManager.getLogger(BoardMonitor.class);
    private final Map monitorCommandList;

    @Autowired
    public BoardMonitor(Map monitorCommandsList) {
        this.monitorCommandList = monitorCommandsList;
    }

    public ArrayList<String> getMonitoringResults() {
        if (monitorCommandList == null) {
            return (ArrayList<String>) Collections.<String>emptyList();
        }

        ArrayList<String> body = new ArrayList<>();

        monitorCommandList.forEach((key, value) -> {
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
                LOG.error("Failed on command:\" " + String.valueOf(key), e);
            }
        });

        return body;
    }
}
