package com.rudyii.hsw.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;

@Component
public class PidGeneratorShutdownHandler {
    private static Logger LOG = LogManager.getLogger(PidGeneratorShutdownHandler.class);
    private static int pid;

    public static int getPid() {
        return pid;
    }

    private int getProcessPid() {
        String procId = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.valueOf(procId.split("@")[0]);
    }

    @PostConstruct
    public void hookSigTerm() throws FileNotFoundException {
        pid = getProcessPid();
        PrintWriter out = new PrintWriter("pid");
        out.println(pid);
        out.close();

        try {
            LOG.info("Home system application started at: " + (new Date()).toString() + " with PID: " + pid);
        } catch (Exception e) {
            LOG.error("Error in pid handling", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            new File("pid").delete();
            LOG.info("Home system application closed at: " + (new Date()).toString());
        }));
    }
}
