package com.rudyii.hsw.services;

import com.google.gson.Gson;
import com.rudyii.hsw.objects.WanIp;
import com.rudyii.hsw.providers.NotificationsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import static com.rudyii.hsw.enums.IPStateEnum.ONLINE;

@Service
public class IspService {
    private static Logger LOG = LogManager.getLogger(IspService.class);
    private NotificationsService notificationsService;
    private PingService pingService;

    private WanIp previousWanIp;

    private WanIp currentWanIp;

    @Autowired
    public IspService(NotificationsService notificationsService, PingService pingService) {
        this.notificationsService = notificationsService;
        this.pingService = pingService;
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

        if (previousWanIp.getQuery().equals(currentWanIp.getQuery())) {
            previousWanIp = currentWanIp;
        } else {
            ArrayList<String> body = new ArrayList<>();

            body.add("Previous ISP and WAN IP address:");
            body.add("Previous Internet provider: " + previousWanIp.getIsp());
            body.add("Previous WAN IP: " + previousWanIp.getQuery());
            body.add("You are visited from: " + previousWanIp.getRegionName());
            body.add("<br>");
            body.add("Current ISP and WAN IP address:");
            body.add("Current Internet provider: " + currentWanIp.getIsp());
            body.add("Current WAN IP: " + currentWanIp.getQuery());
            body.add("You are visiting from: " + currentWanIp.getRegionName());

            notificationsService.sendMessage("ISP/WAN IP Changed!", body, true);

            previousWanIp = currentWanIp;
        }
    }

    private WanIp getWanIp() {
        String whatsMyIpJson = "http://ip-api.com/json";
        Gson gson = new Gson();
        WanIp wanIp = null;
        String response = null;

        try {
            response = IOUtil.toString(new URL(whatsMyIpJson).openStream());
            wanIp = gson.fromJson(response, WanIp.class);
        } catch (Exception e) {
            LOG.error("Failed to fetch WAN IP information", e);
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
            LOG.error("Failed to fetch local IP address: ", e);
        }

        return "0.0.0.0";
    }

    public WanIp getCurrentWanIp() {
        return currentWanIp;
    }
}
