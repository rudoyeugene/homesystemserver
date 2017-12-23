package com.rudyii.hsw.events;

import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;

/**
 * Created by jack on 08.10.16.
 */
public class ArmedEvent extends EventBase {
    private ArmedModeEnum armedMode;
    private ArmedStateEnum armedState;

    public ArmedEvent(ArmedModeEnum armedMode, ArmedStateEnum armedState) {
        this.armedMode = armedMode;
        this.armedState = armedState;
        System.out.println("With mode: " + armedMode + " and state: " + armedState);
    }

    public ArmedStateEnum getArmedState() {
        return armedState;
    }

    public ArmedModeEnum getArmedMode() {
        return armedMode;
    }
}
