package com.rudyii.hsw.services.internet;

import com.google.gson.Gson;
import com.rudyii.hsw.objects.WanIp;
import com.rudyii.hsw.objects.events.IspEvent;
import com.rudyii.hsw.services.system.EventService;
import com.rudyii.hsw.services.system.PingService;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.IOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;

import static com.rudyii.hsw.enums.IPStateEnum.ONLINE;

@Slf4j
@Service
public class IspService {
    private final PingService pingService;
    private final EventService eventService;
    private WanIp previousWanIp = WanIp.builder().build();
    private WanIp currentWanIp = WanIp.builder().build();
    ;

    @Autowired
    public IspService(PingService pingService, EventService eventService) {
        this.pingService = pingService;
        this.eventService = eventService;
    }

    @PostConstruct
    public void initPreviousWanIp() {
        previousWanIp = getWanIp();
    }

    public boolean internetIsAvailable() {
        return pingService.ping("8.8.8.8").equals(ONLINE);
    }

    public String getCurrentOrLastWanIpAddress() {
        if (currentWanIp != null) {
            return currentWanIp.getQuery();
        } else if (previousWanIp != null) {
            return previousWanIp.getQuery();
        } else {
            return "UNKNOWN";
        }
    }

    @Scheduled(fixedRate = 60000L)
    public void resolveWanIp() {

        currentWanIp = getWanIp();

        if (previousWanIp == null) {
            if (currentWanIp == null) {
                return;
            } else {
                previousWanIp = currentWanIp;
                return;
            }
        }

        if (!previousWanIp.getQuery().equals(currentWanIp.getQuery())) {
            eventService.publish(IspEvent.builder()
                    .externalIp(currentWanIp.getQuery())
                    .ispName(currentWanIp.getIsp())
                    .build());
        }

        previousWanIp = currentWanIp;
    }

    private WanIp getWanIp() {
        String whatsMyIpJson = "http://ip-api.com/json";
        Gson gson = new Gson();
        WanIp wanIp = null;
        String response;

        try {
            response = IOUtil.toString(new URL(whatsMyIpJson).openStream());
            wanIp = gson.fromJson(response, WanIp.class);
        } catch (Exception e) {
            log.error("Failed to fetch WAN IP information", e);
        }
        return wanIp;
    }

    public String getLocalIpAddress() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
                    .filter(ip -> ip instanceof Inet4Address && ip.isSiteLocalAddress())
                    .findFirst().orElseThrow(RuntimeException::new)
                    .getHostAddress();
        } catch (SocketException e) {
            log.error("Failed to fetch local IP address: ", e);
        }

        return "0.0.0.0";
    }

    public WanIp getCurrentWanIp() {
        return currentWanIp;
    }

    public boolean isWanUpdated() {
        return currentWanIp != null;
    }
}
