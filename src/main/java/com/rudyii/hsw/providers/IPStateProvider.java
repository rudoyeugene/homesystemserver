package com.rudyii.hsw.providers;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.IPEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rudyii.hsw.enums.IPStateEnum.ERROR;
import static com.rudyii.hsw.enums.IPStateEnum.ONLINE;

@Slf4j
@Component
public class IPStateProvider {
    private final List<String> masterIpList;
    private final Map<String, String> ipResolver;
    private final HashMap<String, IPStateEnum> ipStates = new HashMap<>();

    @Autowired
    public IPStateProvider(List masterIpList, Map ipResolver) {
        this.masterIpList = masterIpList;
        this.ipResolver = ipResolver;
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
        if (!event.getState().equals(getIPState(event.getIp()))) {
            log.info("Device {} state changed to {}", ipResolver.get(event.getIp()), event.getState());
        }
    }
}
