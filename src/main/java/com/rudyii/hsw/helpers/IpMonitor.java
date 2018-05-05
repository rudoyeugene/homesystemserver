package com.rudyii.hsw.helpers;

import com.rudyii.hsw.services.PingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jack on 25.09.16.
 */
@Component
public class IpMonitor {

    private HashMap<String, String> ipResolver;
    private List<String> monitoringIpList;
    private PingService pingService;

    @Autowired
    public IpMonitor(Map ipResolver, List monitoringIpList, PingService pingService) {
        this.ipResolver = (HashMap<String, String>) ipResolver;
        this.monitoringIpList = monitoringIpList;
        this.pingService = pingService;
    }

    public ArrayList<String> getStates() {
        ArrayList<String> states = new ArrayList<>();

        for (String ip : monitoringIpList) {
            states.add((ipResolver.get(ip) == null ? ip : ipResolver.get(ip))
                    + " is <b>" + pingService.ping(ip) + "</b>");
        }
        return states;
    }
}
