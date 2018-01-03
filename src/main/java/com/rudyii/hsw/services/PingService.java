package com.rudyii.hsw.services;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.events.IPEvent;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.rudyii.hsw.enums.IPStateEnum.*;

/**
 * Created by jack on 01.02.17.
 */
@Service
public class PingService {
    private static Logger LOG = LogManager.getLogger(PingService.class);

    private List<String> masterIpList;
    private ArmedStateService armedStateService;
    private EventService eventService;
    private Map<String, String> ipResolver;

    @Autowired
    public PingService(List masterIpList, ArmedStateService armedStateService,
                       EventService eventService, Map ipResolver) {
        this.masterIpList = masterIpList;
        this.armedStateService = armedStateService;
        this.eventService = eventService;
        this.ipResolver = ipResolver;
    }

    @Scheduled(fixedRateString = "${cron.ip.check.millis}")
    public void run() {
        if (armedStateService.isSystemInAutoMode()) {
            for (String ip : masterIpList) {
                ping(ip);
            }
        }
    }

    public IPStateEnum ping(String ip) {
        LOG.info("Trying to ping " + ip);
        List<String> pingCommand = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            LOG.info("Linux OS detected");
            pingCommand.add("/bin/ping");
            pingCommand.add(ip);
            pingCommand.add("-c");
            pingCommand.add("1");

            ProcessBuilder pingBuilder = new ProcessBuilder(pingCommand);
            try {
                Process pingProcess = pingBuilder.start();
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
            }

        } else {
            LOG.info("NON Linux OS detected! Reachability will be used!");
            InetAddress host = null;
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
            }
        }

        return ERROR;
    }

    private void fireEventWithState(String ip, IPStateEnum state) {
        IPEvent ipEvent = new IPEvent(ip, state);
        eventService.publish(ipEvent);
        System.out.println(ipResolver.get(ip) == null ? ip : ipResolver.get(ip) + " is " + state);
        LOG.info(ip + " is " + state);
    }
}
