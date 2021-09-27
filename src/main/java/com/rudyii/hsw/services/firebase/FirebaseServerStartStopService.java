package com.rudyii.hsw.services.firebase;

import com.rudyii.hs.common.objects.logs.StartStopLog;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.LOG_ROOT;
import static com.rudyii.hs.common.type.LogType.SERVER_START_STOP;
import static com.rudyii.hs.common.type.ServerStateType.STARTED;
import static com.rudyii.hs.common.type.ServerStateType.STOPPED;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;

@Service
@AllArgsConstructor
public class FirebaseServerStartStopService {
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;

    @PostConstruct
    public void logStart() {
        long startupTime = System.currentTimeMillis();
        StartStopLog startStopLog = StartStopLog.builder()
                .eventId(startupTime)
                .logType(SERVER_START_STOP)
                .pid(getPid())
                .serverState(STARTED)
                .build();
        firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(startupTime)).setValueAsync(startStopLog);
    }

    @PreDestroy
    public void logStop() {
        long stopTime = System.currentTimeMillis();
        StartStopLog startStopLog = StartStopLog.builder()
                .eventId(stopTime)
                .logType(SERVER_START_STOP)
                .serverState(STOPPED)
                .build();
        firebaseDatabaseProvider.getRootReference().child(LOG_ROOT).child(String.valueOf(stopTime)).setValueAsync(startStopLog);
    }
}
