package com.rudyii.hsw.services.firebase;

import com.rudyii.hs.common.objects.info.ServerInfo;
import com.rudyii.hs.common.objects.info.Uptime;
import com.rudyii.hs.common.objects.info.WanInfo;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.objects.events.IspEvent;
import com.rudyii.hsw.services.SystemModeAndStateService;
import com.rudyii.hsw.services.internet.IspService;
import com.rudyii.hsw.services.system.ServerKeyService;
import com.rudyii.hsw.services.system.UptimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.*;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseInfoService {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final ServerKeyService serverKeyService;
    private final UptimeService uptimeService;
    private final SystemModeAndStateService systemModeAndStateService;
    private final IspService ispService;
    private final AtomicInteger currentSession = new AtomicInteger();
    @Value("${application.version}")
    private String appVersion;

    @PostConstruct
    public void postServerInfo() {
        firebaseDatabaseProvider.getRootReference().child(INFO_ROOT).child(INFO_SERVER).setValueAsync(ServerInfo.builder()
                .pid(getPid())
                .serverAlias(serverKeyService.getServerAlias())
                .serverVersion(appVersion)
                .build());
        updateWanInfo();
        doPing();
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void doPing() {
        firebaseDatabaseProvider.getRootReference().child(INFO_ROOT).child(INFO_PING).setValueAsync(Uptime.builder()
                .ping(System.currentTimeMillis())
                .uptime(uptimeService.getUptimeMinutes())
                .currentSession(systemModeAndStateService.isArmed() ? currentSession.getAndIncrement() : currentSession.get())
                .build());

    }

    @EventListener(IspEvent.class)
    public void updateWanInfo() {
        if (ispService.isWanUpdated()) {
            firebaseDatabaseProvider.getRootReference().child(INFO_ROOT).child(INFO_WAN).setValueAsync(WanInfo.builder()
                    .isp(ispService.getCurrentWanIp().getIsp())
                    .wanIp(ispService.getCurrentWanIp().getQuery())
                    .build());
        }
    }

    @EventListener(ArmedEvent.class)
    public void resetCurrentSession(ArmedEvent armedEvent) {
        if (SystemStateType.ARMED.equals(armedEvent.getSystemState())) {
            currentSession.getAndSet(0);
        }
    }
}
