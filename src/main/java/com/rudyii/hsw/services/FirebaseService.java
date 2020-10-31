package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.objects.WanIp;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.NotificationsService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;
import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.isHomeSystemInitComplete;

@Slf4j
@Service
public class FirebaseService {
    public static final String REASON = "reason";
    public static final String IMAGE_URL = "imageUrl";
    public static final String VIDEO_URL = "videoUrl";
    public static final String SERVER_NAME = "serverName";
    public static final String SERVER_STARTUP_OR_SHUTDOWN = "serverStartupOrShutdown";
    public static final String ACTION = "action";
    public static final String STOPPING = "stopping";
    public static final String SYSTEM_STATE_CHANGED = "systemStateChanged";
    public static final String ARMED_MODE = "armedMode";
    public static final String ARMED_STATE = "armedState";
    public static final String MOTION_DETECTED = "motionDetected";
    public static final String MOTION_ID = "motionId";
    public static final String EVENT_ID = "eventId";
    public static final String TIME_STAMP = "timeStamp";
    public static final String CAMERA_NAME = "cameraName";
    public static final String MOTION_AREA = "motionArea";
    public static final String VIDEO_RECORDED = "videoRecorded";
    public static final String FILE_NAME = "fileName";
    public static final String ISP_CHANGED = "ispChanged";
    public static final String ISP = "isp";
    public static final String IP = "ip";
    public static final String CAMERA_REBOOT = "cameraReboot";
    public static final String STATE = "state";
    public static final String RESEND_HOURLY = "resendHourly";
    public static final String WAN_IP = "wanIp";
    public static final String PID = "pid";
    public static final String STARTING = "starting";
    public static final String ALL = "all";
    private final String serverAlias;
    private final Random random = new Random();
    private final FirebaseDatabaseProvider firebaseDatabaseProvider;
    private final ArmedStateService armedStateService;
    private final Uptime uptime;
    private final ReportingService reportingService;
    private final EventService eventService;
    private final IspService ispService;
    private final NotificationsService notificationsService;
    private final ThreadPoolTaskExecutor hswExecutor;
    private final ClientsService clientsService;
    private final ArrayList<DatabaseReference> databaseReferences;
    private final ArrayList<ValueEventListener> valueEventListeners;
    private final ConcurrentHashMap<String, Object> statuses;
    private final ConcurrentHashMap<String, Object> requests;
    private final String serverKey;
    @Value("${application.version}")
    private String appVersion;
    private boolean alreadyFired;

    public FirebaseService(FirebaseDatabaseProvider firebaseDatabaseProvider, UuidService uuidService,
                           ArmedStateService armedStateService, Uptime uptime,
                           ReportingService reportingService, EventService eventService,
                           IspService ispService, NotificationsService notificationsService,
                           ThreadPoolTaskExecutor hswExecutor, ClientsService clientsService) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.armedStateService = armedStateService;
        this.uptime = uptime;
        this.reportingService = reportingService;
        this.eventService = eventService;
        this.ispService = ispService;
        this.notificationsService = notificationsService;
        this.hswExecutor = hswExecutor;
        this.clientsService = clientsService;

        this.statuses = new ConcurrentHashMap<>();
        this.requests = new ConcurrentHashMap<>();
        this.databaseReferences = new ArrayList();
        this.valueEventListeners = new ArrayList();

        this.serverAlias = uuidService.getServerAlias();
        this.serverKey = uuidService.getServerKey();
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    @PostConstruct
    public void init() {
        HashMap<String, String> state = new HashMap<>();

        state.put(ARMED_STATE, armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        state.put(ARMED_MODE, armedStateService.getArmedMode().toString());

        requests.put(STATE, state);
        requests.put(RESEND_HOURLY, random.nextInt(999));

        updateStatuses(armedStateService.isArmed() ? ARMED : DISARMED, armedStateService.getArmedMode());

        firebaseDatabaseProvider.pushData("/statuses", statuses);
        firebaseDatabaseProvider.pushData("/requests", requests);

        DatabaseReference serverNameRef = firebaseDatabaseProvider.getReference("/info/serverName");
        serverNameRef.setValueAsync(serverAlias);

        DatabaseReference pidRef = firebaseDatabaseProvider.getReference("/info/pid");
        pidRef.setValueAsync(getPid());

        DatabaseReference pingRef = firebaseDatabaseProvider.getReference("/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference serverVersionRef = firebaseDatabaseProvider.getReference("/info/serverVersion");
        serverVersionRef.setValueAsync(appVersion);

        DatabaseReference uptimeRef = firebaseDatabaseProvider.getReference("/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());

        unregisterListeners();
        registerListeners();
        notifyServerStarted();
    }

    @PreDestroy
    private void destroy() {
        JsonObject jsonObject = getStartStopJsonObject(STOPPING);

        sendFcmMessage(jsonObject, ALL);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        unregisterListeners();
    }

    @EventListener({ArmedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionToNotifyEvent.class, UploadEvent.class})
    public void onEvent(EventBase event) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SERVER_NAME, serverAlias);

        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;

            updateStatuses(armedEvent.getArmedState(), armedEvent.getArmedMode());

            jsonObject.addProperty(REASON, SYSTEM_STATE_CHANGED);
            jsonObject.addProperty(ARMED_MODE, armedEvent.getArmedMode().toString());
            jsonObject.addProperty(ARMED_STATE, armedEvent.getArmedState().toString());

            sendFcmMessage(jsonObject, ALL);

        } else if (event instanceof MotionToNotifyEvent) {
            MotionToNotifyEvent motionToNotifyEvent = (MotionToNotifyEvent) event;

            jsonObject.addProperty(REASON, MOTION_DETECTED);
            jsonObject.addProperty(IMAGE_URL, motionToNotifyEvent.getSnapshotUrl().toString());
            jsonObject.addProperty(EVENT_ID, motionToNotifyEvent.getEventId());
            jsonObject.addProperty(TIME_STAMP, motionToNotifyEvent.getEventId());
            jsonObject.addProperty(CAMERA_NAME, motionToNotifyEvent.getCameraName());
            jsonObject.addProperty(MOTION_AREA, motionToNotifyEvent.getMotionArea());

            sendFcmMessage(jsonObject, MOTION_DETECTED);

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;

            jsonObject.addProperty(REASON, VIDEO_RECORDED);
            jsonObject.addProperty(VIDEO_URL, uploadEvent.getVideoUrl().toString());
            jsonObject.addProperty(EVENT_ID, uploadEvent.getEventId());
            jsonObject.addProperty(FILE_NAME, uploadEvent.getFileName());

            sendFcmMessage(jsonObject, VIDEO_RECORDED);

        } else if (event instanceof IspEvent) {
            refreshWanInfo();

            jsonObject.addProperty(REASON, ISP_CHANGED);
            jsonObject.addProperty(ISP, ispService.getCurrentWanIp().getIsp());
            jsonObject.addProperty(IP, ispService.getCurrentWanIp().getQuery());

            sendFcmMessage(jsonObject, ALL);

        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;

            jsonObject.addProperty(REASON, CAMERA_REBOOT);
            jsonObject.addProperty(CAMERA_NAME, cameraRebootEvent.getCameraName());

            sendFcmMessage(jsonObject, ALL);
        }

        firebaseDatabaseProvider.pushData("/log/" + event.getEventId(), new Gson().fromJson(jsonObject, HashMap.class));
    }

    private void updateStatuses(ArmedStateEnum armedState, ArmedModeEnum armedMode) {
        statuses.put(ARMED_STATE, armedState);
        statuses.put(ARMED_MODE, armedMode);

        statuses.put(TIME_STAMP, System.currentTimeMillis());

        firebaseDatabaseProvider.pushData("/statuses", statuses);
    }

    private void registerListeners() {
        DatabaseReference armRef = firebaseDatabaseProvider.getReference("/requests/state");
        databaseReferences.add(armRef);
        ValueEventListener armRefValueEventListener = getArmedEventValueEventListener();
        valueEventListeners.add(armRefValueEventListener);
        armRef.addValueEventListener(armRefValueEventListener);

        DatabaseReference resendHourlyRef = firebaseDatabaseProvider.getReference("/requests/resendHourly");
        databaseReferences.add(resendHourlyRef);
        ValueEventListener resendHourlyRefValueEventListener = getResendHourlyValueEventListener();
        valueEventListeners.add(resendHourlyRefValueEventListener);
        resendHourlyRef.addValueEventListener(resendHourlyRefValueEventListener);
    }

    private void unregisterListeners() {
        for (DatabaseReference reference : databaseReferences) {
            for (ValueEventListener eventListener : valueEventListeners) {
                reference.removeEventListener(eventListener);
            }
        }
    }

    private ValueEventListener getResendHourlyValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!requests.get(RESEND_HOURLY).toString().equals(dataSnapshot.getValue().toString()) && isHomeSystemInitComplete()) {
                    reportingService.sendHourlyReport();
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Hourly Resend Firebase data!");
            }
        };
    }

    private ValueEventListener getArmedEventValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isHomeSystemInitComplete()) {
                    HashMap<String, Object> state = (HashMap<String, Object>) dataSnapshot.getValue();

                    ArmedEvent armedEvent = new ArmedEvent(ArmedModeEnum.valueOf(state.get(ARMED_MODE).toString()), ArmedStateEnum.valueOf(state.get(ARMED_STATE).toString()));

                    eventService.publish(armedEvent);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to fetch Armed Mode & Armed State Firebase data!");
            }
        };
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void ping() {
        DatabaseReference pingRef = firebaseDatabaseProvider.getReference("/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference uptimeRef = firebaseDatabaseProvider.getReference("/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());

        refreshWanInfo();
    }

    private void refreshWanInfo() {
        HashMap<String, Object> wanInfo = new HashMap<>();
        WanIp wanIp = ispService.getCurrentWanIp();

        wanInfo.put(WAN_IP, wanIp.getQuery());
        wanInfo.put(ISP, wanIp.getIsp());

        firebaseDatabaseProvider.pushData("/info/wanInfo", wanInfo);
    }

    private void notifyServerStarted() {
        JsonObject jsonObject = getStartStopJsonObject(STARTING);
        jsonObject.addProperty(PID, getPid());

        sendFcmMessage(jsonObject, ALL);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        alreadyFired = true;
    }

    @NotNull
    private JsonObject getStartStopJsonObject(String starting) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SERVER_NAME, serverAlias);
        jsonObject.addProperty(REASON, SERVER_STARTUP_OR_SHUTDOWN);
        jsonObject.addProperty(ACTION, starting);
        return jsonObject;
    }

    private void sendFcmMessage(JsonObject messageData, String notificationType) {
        clientsService.getClients().forEach(client -> {
            String clientNotificationType = client.getNotificationType();
            String email = client.getEmail();
            String device = client.getDevice();
            String appVersion = client.getAppVersion();

            boolean notify = false;

            if (notificationType.equals(clientNotificationType)
                    || ALL.equals(clientNotificationType)
                    || notificationType.equals(ALL)) {
                notify = true;
            }

            if (client.getNotificationsMuted()) {
                notify = false;
            }

            if (notify && tokenLooksGood(client.getToken())) {
                String token = client.getToken();

                log.info("Ready to send message to the Client:" + email + " on device " + device + " with client version " + appVersion);

                notificationsService.sendFcmMessage(email, token, messageData);
            } else {
                log.warn("Client:" + email + " on device " + device + " with client version " + appVersion + " is not interested in such type of notification: server - " + notificationType + ", client - " + clientNotificationType);
            }
        });
    }

    private boolean tokenLooksGood(String token) {
        return token != null && !"".equals(token) && !"null".equalsIgnoreCase(token);
    }
}
