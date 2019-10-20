package com.rudyii.hsw.helpers;

import com.rudyii.hsw.providers.IPStateProvider;
import com.rudyii.hsw.services.PingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

@Component
public class IpMonitor {

    private Map<String, String> ipResolver;
    private PingService pingService;
    private IPStateProvider ipStateProvider;

    @Autowired
    public IpMonitor(Map ipResolver, PingService pingService, IPStateProvider ipStateProvider) {
        this.ipResolver = ipResolver;
        this.pingService = pingService;
        this.ipStateProvider = ipStateProvider;
    }

    public ArrayList<String> getStates() {
        pingService.forceUpdateIpStates();

        try {
            Thread.sleep(6000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ArrayList<String> states = new ArrayList<>();

        ipResolver.forEach((ip, name) -> states.add(name + " is <b>" + ipStateProvider.getIPState(ip) + "</b>"));

        return states;
    }
}
