package com.rudyii.hsw.services;

import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.events.ArmedEvent;
import com.rudyii.hsw.providers.IPStateProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.rudyii.hsw.enums.ArmedModeEnum.AUTOMATIC;
import static com.rudyii.hsw.enums.ArmedModeEnum.MANUAL;
import static com.rudyii.hsw.enums.ArmedStateEnum.*;

@Service
public class ArmedStateService {
    private static Logger LOG = LogManager.getLogger(ArmedStateService.class);
    private EventService eventService;
    private long count = 1L;
    private Boolean systemArmed = false;
    private ArmedModeEnum armedMode = AUTOMATIC;
    private Connection connection;
    private IPStateProvider ipStateProvider;

    @Autowired
    public ArmedStateService(EventService eventService, Connection connection, IPStateProvider ipStateProvider) {
        this.eventService = eventService;
        this.connection = connection;
        this.ipStateProvider = ipStateProvider;
    }

    @Scheduled(initialDelayString = "${cron.armed.state.check.init.delay.millis}", fixedRateString = "${cron.armed.state.check.millis}")
    public void run() {
        this.count++;

        if (armedMode.equals(AUTOMATIC)) {
            if (!systemArmed && !ipStateProvider.mastersOnline()) {
                System.out.println(getMessage());
                arm(true);
            } else if (systemArmed && ipStateProvider.mastersOnline()) {
                System.out.println(getMessage());
                disarm(true);
            }
            System.out.println(getMessage());
        } else {
            System.out.println(count + ": system state is unchanged because of system is in MANUAL mode");
        }
    }

    @NotNull
    private String getMessage() {
        return count + ": system is " + (systemArmed ? ARMED : DISARMED) + "; " + (ipStateProvider.mastersOnline() ? "Master is present." : "Master is absent.");
    }

    private void disarm(boolean publishEvent) {
        this.systemArmed = false;
        if (publishEvent) {
            eventService.publish(new ArmedEvent(AUTOMATIC, DISARMED));
        }

        LOG.info("System " + DISARMED);
        updateArmedStateHistory(DISARMED);
    }

    private void arm(boolean publishEvent) {
        this.systemArmed = true;
        if (publishEvent) {
            eventService.publish(new ArmedEvent(AUTOMATIC, ARMED));
        }

        LOG.info("System " + ARMED);
        updateArmedStateHistory(ARMED);
    }

    private void updateArmedStateHistory(ArmedStateEnum state) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT OR REPLACE INTO ARMED_STATE_HIST (STATE) VALUES (?)");
            statement.setString(1, state.toString());
            statement.execute();
        } catch (SQLException e) {
            LOG.error("Error while updating DB to the " + state + " state!", e);
        }
    }

    @Async
    @EventListener(ArmedEvent.class)
    public void onEvent(ArmedEvent event) {
        if (event.getArmedMode().equals(MANUAL) && event.getArmedState().equals(ARMED)) {
            this.armedMode = MANUAL;
            arm(false);
        } else if (event.getArmedMode().equals(MANUAL) && event.getArmedState().equals(DISARMED)) {
            this.armedMode = MANUAL;
            disarm(false);
        } else if (event.getArmedMode().equals(AUTOMATIC) && event.getArmedState().equals(AUTO)) {
            this.armedMode = AUTOMATIC;
        } else {
            LOG.info("Unsupported case: mode " + event.getArmedMode() + " with state: " + event.getArmedState());
        }
    }

    public boolean isSystemInAutoMode() {
        return armedMode.equals(AUTOMATIC);
    }

    public boolean isArmed() {
        return systemArmed;
    }

    public ArmedModeEnum getArmedMode() {
        return armedMode;
    }
}
