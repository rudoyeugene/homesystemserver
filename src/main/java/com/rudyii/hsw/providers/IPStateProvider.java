package com.rudyii.hsw.providers;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.events.IPEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static com.rudyii.hsw.enums.IPStateEnum.ERROR;
import static com.rudyii.hsw.enums.IPStateEnum.ONLINE;

@Component
public class IPStateProvider {
    private HashMap<String, IPStateEnum> ipStates = new HashMap<>();
    ;

    private List<String> masterIpList;

    @Autowired
    public IPStateProvider(List masterIpList) {
        this.masterIpList = masterIpList;
    }

    public IPStateEnum getIPState(String ip) {
        return ipStates.get(ip) == null ? ERROR : ipStates.get(ip);
    }

    private void setIPState(String ip, IPStateEnum state) {
        ipStates.put(ip, state);
    }

    public Boolean mastersOnline() {
        return masterIpList.stream().anyMatch(ip -> ONLINE.equals(ipStates.get(ip)));
    }

    @Async
    @EventListener(IPEvent.class)
    public void onEvent(IPEvent event) {
        setIPState(event.getIp(), event.getState());
    }
}
