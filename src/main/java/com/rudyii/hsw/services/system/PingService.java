package com.rudyii.hsw.services.system;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.IPEvent;
import com.rudyii.hsw.services.SystemModeAndStateService;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Map;

import static com.rudyii.hs.common.type.SystemStateType.ARMED;
import static com.rudyii.hs.common.type.SystemStateType.RESOLVING;
import static com.rudyii.hsw.enums.IPStateEnum.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.SystemUtils.IS_OS_LINUX;

@Slf4j
@Service
public class PingService {
    private final EventService eventService;
    private final Map<String, String> ipResolver;
    private final ThreadPoolTaskExecutor hswExecutor;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final SystemModeAndStateService systemModeAndStateService;

    @Autowired
    public PingService(EventService eventService, Map ipResolver,
                       ThreadPoolTaskExecutor hswExecutor, FirebaseGlobalSettingsService globalSettingsService,
                       SystemModeAndStateService systemModeAndStateService) {
        this.eventService = eventService;
        this.ipResolver = ipResolver;
        this.hswExecutor = hswExecutor;
        this.globalSettingsService = globalSettingsService;
        this.systemModeAndStateService = systemModeAndStateService;

        if (IS_OS_LINUX) {
            log.info("Linux OS detected");
        } else {
            log.info("NON Linux OS detected! Reachability will be used!");
        }
    }

    @PostConstruct
    public void scheduleNextPing() {
        hswExecutor.submit(() -> {
            while (true) {
                ipResolver.forEach((ip, name) -> hswExecutor.submit(new PingRunnable(ip)));
                long sleepFor;
                if (ARMED.equals(systemModeAndStateService.getSystemState()) || RESOLVING.equals(systemModeAndStateService.getSystemState())) {
                    sleepFor = globalSettingsService.getGlobalSettings().getMasterCheckPeriodIfArmedSec();
                } else {
                    sleepFor = globalSettingsService.getGlobalSettings().getMasterCheckPeriodIfDisarmedSec();
                }

                try {
                    log.info("Next ping action will run in {} seconds", sleepFor);
                    Thread.sleep(sleepFor * 1000L);
                } catch (Exception e) {
                    log.info("PingService failed:", e);
                }
            }
        });
    }

    @Scheduled(initialDelay = 5000L, fixedRate = 60000L)
    public void updateIpStates() {
        ipResolver.forEach((ip, name) -> hswExecutor.submit(new PingRunnable(ip)));
    }

    public IPStateEnum ping(String ip) {
        log.info("Trying to ping " + ip);

        if (IS_OS_LINUX) {
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
                log.error("Failed to ping address: " + ip, e);
                return ERROR;
            }

        } else {
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
                log.error("Error occurred: ", e);
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
    }


    private class PingRunnable implements Runnable {
        private final String ip;

        PingRunnable(String ip) {
            this.ip = ip;
        }

        @Override
        public void run() {
            if (IS_OS_LINUX) {
                try {
                    Process pingProcess = getPingProcessBuilderFor(ip).start();
                    pingProcess.waitFor();

                    if (pingProcess.exitValue() == 0) {
                        fireEventWithState(ip, ONLINE);
                    } else {
                        fireEventWithState(ip, OFFLINE);
                    }
                } catch (Exception e) {
                    log.error("Failed to ping address: " + ip, e);
                }

            } else {
                try {
                    if (InetAddress.getByName(ip).isReachable(5000)) {
                        fireEventWithState(ip, ONLINE);
                    } else {
                        fireEventWithState(ip, OFFLINE);
                    }

                } catch (Exception e) {
                    log.error("Error occurred: ", e);
                }
            }
        }
    }
}
