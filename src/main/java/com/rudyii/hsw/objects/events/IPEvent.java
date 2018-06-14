package com.rudyii.hsw.objects.events;

import com.rudyii.hsw.enums.IPStateEnum;

/**
 * Created by jack on 07.10.16.
 */
public class IPEvent extends EventBase {

    private final String ip;
    private final IPStateEnum state;

    public IPEvent(String ip, IPStateEnum state) {
        this.ip = ip;
        this.state = state;
        System.out.println("With " + ip + " in " + state + " state");
    }

    public String getIp() {
        return ip;
    }

    public IPStateEnum getState() {
        return state;
    }


}
