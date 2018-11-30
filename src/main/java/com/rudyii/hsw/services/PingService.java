package com.rudyii.hsw.services;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.IPEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Map;

import static com.rudyii.hsw.enums.IPStateEnum.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.SystemUtils.IS_OS_LINUX;

/**
 * Created by jack on 01.02.17.
 */
@Service
public class PingService {
    private static Logger LOG = LogManager.getLogger(PingService.class);

    private ArmedStateService armedStateService;
    private EventService eventService;
    private Map<String, String> ipResolver;

    @Autowired
    public PingService(ArmedStateService armedStateService,
                       EventService eventService, Map ipResolver) {
        this.armedStateService = armedStateService;
        this.eventService = eventService;
        this.ipResolver = ipResolver;
    }

    @Scheduled(initialDelay = 5000L, fixedRate = 60000L)
    public void updateIpStates() {
        if (armedStateService.isSystemInAutoMode()) {
            firePing();
        }
    }

    public void forceUpdateIpStates() {
        firePing();
    }

    private void firePing() {
        ipResolver.forEach((ip, name) -> new PingRunnable(ip));
    }

    public IPStateEnum ping(String ip) {
        LOG.info("Trying to ping " + ip);

        if (IS_OS_LINUX) {
            LOG.info("Linux OS detected");
            try {
                Process pingProcess = getPingProcessBuilderFor(ip).start();
                pingProcess.waitFor();

                if (pingProcess.exitValue() == 0) {
                    fireEventWithState(ip, ONLINE);
                    return ONLINE;
                } else {
                    fireEventWithState(ip, OFFLINE);
                    return OFFLINE;
                }
            } catch (Exception e) {
                LOG.error("Failed to ping address: " + ip, e);
                return ERROR;
            }

        } else {
            LOG.info("NON Linux OS detected! Reachability will be used!");
            InetAddress host;
            try {
                host = InetAddress.getByName(ip);

                if (host.isReachable(5000)) {
                    fireEventWithState(ip, ONLINE);
                    return ONLINE;
                } else {
                    fireEventWithState(ip, OFFLINE);
                    return OFFLINE;
                }

            } catch (Exception e) {
                LOG.error("Error occurred: ", e);
                return ERROR;
            }
        }
    }

    @NotNull
    private ProcessBuilder getPingProcessBuilderFor(String ip) {
        return new ProcessBuilder(asList("/bin/ping", ip, "-c", "1"));
    }

    private void fireEventWithState(String ip, IPStateEnum state) {
        IPEvent ipEvent = new IPEvent(ip, state);
        eventService.publish(ipEvent);
        System.out.println(ipResolver.get(ip) == null ? ip : ipResolver.get(ip) + " is " + state);
        LOG.info(ip + " is " + state);
    }


    private class PingRunnable implements Runnable {
        private String ip;
        private Thread thread;

        PingRunnable(String ip) {
            this.ip = ip;
            this.thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run() {
            if (IS_OS_LINUX) {
                LOG.info("Linux OS detected");
                try {
                    Process pingProcess = getPingProcessBuilderFor(ip).start();
                    pingProcess.waitFor();

                    if (pingProcess.exitValue() == 0) {
                        fireEventWithState(ip, ONLINE);
                    } else {
                        fireEventWithState(ip, OFFLINE);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to ping address: " + ip, e);
                }

            } else {
                LOG.info("NON Linux OS detected! Reachability will be used!");
                try {
                    if (InetAddress.getByName(ip).isReachable(5000)) {
                        fireEventWithState(ip, ONLINE);
                    } else {
                        fireEventWithState(ip, OFFLINE);
                    }

                } catch (Exception e) {
                    LOG.error("Error occurred: ", e);
                }
            }
        }
    }
}
