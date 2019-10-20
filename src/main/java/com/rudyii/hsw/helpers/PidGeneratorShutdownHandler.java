package com.rudyii.hsw.helpers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;

@Slf4j
@Component
public class PidGeneratorShutdownHandler {
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
        PrintWriter out = new PrintWriter("homesystemserver.pid");
        out.println(pid);
        out.close();

        try {
            log.info("Home system application started at: {} with PID: {}", new Date(), pid);
        } catch (Exception e) {
            log.error("Error in pid handling", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            new File("homesystemserver.pid").delete();
            log.info("Home system application closed at: {}", new Date());
        }));
    }
}
