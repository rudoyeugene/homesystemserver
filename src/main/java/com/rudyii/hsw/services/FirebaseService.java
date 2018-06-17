package com.rudyii.hsw.services;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.objects.WanIp;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.NotificationsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.configuration.OptionsService.RECORD_INTERVAL;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;
import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.isHomeSystemInitComplete;

@Service
public class FirebaseService {
    public static final String REASON = "reason";
    public static final String IMAGE = "image";
    public static final String SERVER_NAME = "serverName";
    public static final String SERVER_STARTUP_OR_SHUTDOWN = "serverStartupOrShutdown";
    public static final String ACTION = "action";
    public static final String STOPPING = "stopping";
    public static final String SYSTEM_STATE_CHANGED = "systemStateChanged";
    public static final String ARMED_MODE = "armedMode";
    public static final String ARMED_STATE = "armedState";
    public static final String PORTS_OPEN = "portsOpen";
    public static final String MOTION_DETECTED = "motionDetected";
    public static final String MOTION_ID = "motionId";
    public static final String TIME_STAMP = "timeStamp";
    public static final String CAMERA_NAME = "cameraName";
    public static final String MOTION_AREA = "motionArea";
    public static final String VIDEO_RECORDED = "videoRecorded";
    public static final String FILE_NAME = "fileName";
    public static final String URL = "url";
    public static final String ISP_CHANGED = "ispChanged";
    public static final String ISP = "isp";
    public static final String IP = "ip";
    public static final String CAMERA_REBOOT = "cameraReboot";
    public static final String STATE = "state";
    public static final String RESEND_HOURLY = "resendHourly";
    public static final String WAN_IP = "wanIp";
    public static final String PID = "pid";
    public static final String STARTING = "starting";
    public static final String RECORD_ID = "recordId";
    public static final String ALL = "all";
    private static Logger LOG = LogManager.getLogger(FirebaseService.class);

    @Value("${application.version}")
    private String appVersion;

    private String serverAlias;
    private Random random = new Random();
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private ArmedStateService armedStateService;
    private Uptime uptime;
    private ReportingService reportingService;
    private UpnpService upnpService;
    private EventService eventService;
    private IspService ispService;
    private OptionsService optionsService;
    private NotificationsService notificationsService;
    private ThreadPoolTaskExecutor hswExecutor;
    private ArrayList<DatabaseReference> databaseReferences;
    private ArrayList<ValueEventListener> valueEventListeners;
    private ArrayList<Client> clients = new ArrayList<>();

    private ConcurrentHashMap<String, Object> statuses, motions, requests;
    private boolean alreadyFired;

    public FirebaseService(FirebaseDatabaseProvider firebaseDatabaseProvider, UuidService uuidService,
                           ArmedStateService armedStateService, Uptime uptime,
                           ReportingService reportingService, UpnpService upnpService,
                           EventService eventService, IspService ispService,
                           OptionsService optionsService,
                           NotificationsService notificationsService, ThreadPoolTaskExecutor hswExecutor) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.armedStateService = armedStateService;
        this.uptime = uptime;
        this.reportingService = reportingService;
        this.upnpService = upnpService;
        this.eventService = eventService;
        this.ispService = ispService;
        this.optionsService = optionsService;
        this.notificationsService = notificationsService;
        this.hswExecutor = hswExecutor;

        this.statuses = new ConcurrentHashMap<>();
        this.motions = new ConcurrentHashMap<>();
        this.requests = new ConcurrentHashMap<>();
        this.databaseReferences = new ArrayList();
        this.valueEventListeners = new ArrayList();

        this.serverAlias = uuidService.getServerAlias();
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    @PostConstruct
    public void init() {
        HashMap<String, String> state = new HashMap<>();

        state.put(ARMED_STATE, armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        state.put(ARMED_MODE, armedStateService.getArmedMode().toString());

        requests.put(STATE, state);
        requests.put(RESEND_HOURLY, random.nextInt(999));
        requests.put(PORTS_OPEN, upnpService.isPortsOpen());

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

        updateConnectedClients();
        unregisterListeners();
        registerListeners();
    }

    @PreDestroy
    private void destroy() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SERVER_NAME, serverAlias);
        jsonObject.addProperty(REASON, SERVER_STARTUP_OR_SHUTDOWN);
        jsonObject.addProperty(ACTION, STOPPING);

        sendFcmMessage(jsonObject, ALL);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        unregisterListeners();
    }

    @EventListener({ArmedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionDetectedEvent.class, UploadEvent.class})
    public void onEvent(EventBase event) {
        final boolean[] logRecordPushed = {false};
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(SERVER_NAME, serverAlias);

        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;

            updateStatuses(armedEvent.getArmedState(), armedEvent.getArmedMode());

            jsonObject.addProperty(REASON, SYSTEM_STATE_CHANGED);
            jsonObject.addProperty(ARMED_MODE, armedEvent.getArmedMode().toString());
            jsonObject.addProperty(ARMED_STATE, armedEvent.getArmedState().toString());
            jsonObject.addProperty(PORTS_OPEN, upnpService.isPortsOpen());

            sendFcmMessage(jsonObject, ALL);

        } else if (event instanceof MotionDetectedEvent) {
            MotionDetectedEvent motionDetectedEvent = (MotionDetectedEvent) event;

            Long lastMotionTimestamp = (Long) motions.get(motionDetectedEvent.getCameraName());
            Long currentMotionTimestamp = motionDetectedEvent.getEventTimeMillis();

            if (lastMotionTimestamp == null) {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            } else if ((currentMotionTimestamp - lastMotionTimestamp) < (long) optionsService.getOption(RECORD_INTERVAL) * 1000) {
                System.out.println("Motion event for camera " + (motionDetectedEvent.getCameraName() + " was ignored"));
                return;
            } else {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            }

            jsonObject.addProperty(REASON, MOTION_DETECTED);
            jsonObject.addProperty(MOTION_ID, currentMotionTimestamp);
            jsonObject.addProperty(TIME_STAMP, currentMotionTimestamp);
            jsonObject.addProperty(CAMERA_NAME, motionDetectedEvent.getCameraName());
            jsonObject.addProperty(MOTION_AREA, motionDetectedEvent.getMotionArea().intValue());

            if (motionDetectedEvent.getCurrentImage() != null) {
                tryToFillJsonObjectWithImage(jsonObject, motionDetectedEvent.getCurrentImage());
            }

            firebaseDatabaseProvider.pushData("/log/" + currentMotionTimestamp, new Gson().fromJson(jsonObject, HashMap.class)).addListener(new Runnable() {
                private JsonObject thisJsonObject;

                @Override
                public void run() {
                    this.thisJsonObject = jsonObject;
                    thisJsonObject.remove(CAMERA_NAME);
                    thisJsonObject.remove(MOTION_AREA);
                    thisJsonObject.remove(TIME_STAMP);
                    thisJsonObject.remove(IMAGE);

                    sendFcmMessage(thisJsonObject, MOTION_DETECTED);
                    logRecordPushed[0] = true;
                }
            }, hswExecutor);

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;

            jsonObject.addProperty(RECORD_ID, event.getEventTimeMillis());
            jsonObject.addProperty(REASON, VIDEO_RECORDED);
            jsonObject.addProperty(FILE_NAME, uploadEvent.getFileName());
            jsonObject.addProperty(URL, uploadEvent.getUrl());

            if (uploadEvent.getImage() != null) {
                tryToFillJsonObjectWithImage(jsonObject, uploadEvent.getImage());
            }

            firebaseDatabaseProvider.pushData("/log/" + event.getEventTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class)).addListener(new Runnable() {
                private JsonObject thisJsonObject;

                @Override
                public void run() {
                    this.thisJsonObject = jsonObject;
                    thisJsonObject.remove(IMAGE);

                    sendFcmMessage(thisJsonObject, VIDEO_RECORDED);

                    logRecordPushed[0] = true;
                }
            }, hswExecutor);


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

        if (logRecordPushed[0]) {
            logRecordPushed[0] = false;
        } else {
            firebaseDatabaseProvider.pushData("/log/" + event.getEventTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));
        }
    }

    private void updateStatuses(ArmedStateEnum armedState, ArmedModeEnum armedMode) {
        statuses.put(ARMED_STATE, armedState);
        statuses.put(ARMED_MODE, armedMode);

        if (armedState.equals(ARMED)) {
            statuses.put(PORTS_OPEN, true);
        } else {
            statuses.put(PORTS_OPEN, false);
        }
        statuses.put(TIME_STAMP, System.currentTimeMillis());

        firebaseDatabaseProvider.pushData("/statuses", statuses);
    }

    private void registerListeners() {
        DatabaseReference openPortsRef = firebaseDatabaseProvider.getReference("/requests/portsOpen");
        databaseReferences.add(openPortsRef);
        ValueEventListener openPortsRefValueEventListener = getOpenPortsValueEventListener();
        valueEventListeners.add(openPortsRefValueEventListener);
        openPortsRef.addValueEventListener(openPortsRefValueEventListener);

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

        DatabaseReference connectedClientsRef = firebaseDatabaseProvider.getReference("/connectedClients");
        databaseReferences.add(connectedClientsRef);
        ValueEventListener connectedClientsValueEventListener = getConnectedClientsValueEventListener();
        valueEventListeners.add(connectedClientsValueEventListener);
        connectedClientsRef.addValueEventListener(connectedClientsValueEventListener);
    }

    private void unregisterListeners() {
        for (DatabaseReference reference : databaseReferences) {
            for (ValueEventListener eventListener : valueEventListeners) {
                reference.removeEventListener(eventListener);
            }
        }
    }

    private ValueEventListener getOpenPortsValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isHomeSystemInitComplete()) {
                    if ((boolean) dataSnapshot.getValue()) {
                        upnpService.openPorts();
                        statuses.put(PORTS_OPEN, true);
                    } else {
                        upnpService.closePorts();
                        statuses.put(PORTS_OPEN, false);
                    }

                    statuses.put(TIME_STAMP, System.currentTimeMillis());
                    firebaseDatabaseProvider.pushData("/statuses", statuses);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Ports State Firebase data!");
            }
        };
    }

    private ValueEventListener getResendHourlyValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!requests.get(RESEND_HOURLY).toString().equals(dataSnapshot.getValue().toString()) && isHomeSystemInitComplete()) {
                    reportingService.sendHourlyReport();
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Hourly Resend Firebase data!");
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
                LOG.error("Failed to fetch Armed Mode & Armed State Firebase data!");
            }
        };
    }

    private ValueEventListener getConnectedClientsValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> connectedClients = (HashMap<String, Object>) dataSnapshot.getValue();

                if (connectedClients == null) {
                    LOG.warn("No connected clients found!");
                } else {
                    clients.clear();

                    connectedClients.forEach((userId, value) -> {
                        HashMap<String, String> userProperties = (HashMap<String, String>) value;
                        clients.add(new Client(userId, userProperties));
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Connected Clients Firebase data!");
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
        notifyServerStarted();
    }

    private void updateConnectedClients() {
        DatabaseReference connectedClientsRef = firebaseDatabaseProvider.getReference("/connectedClients");
        connectedClientsRef.addListenerForSingleValueEvent(getConnectedClientsValueEventListener());
    }

    private void refreshWanInfo() {
        HashMap<String, Object> wanInfo = new HashMap<>();
        WanIp wanIp = ispService.getCurrentWanIp();

        wanInfo.put(WAN_IP, wanIp.getQuery());
        wanInfo.put(ISP, wanIp.getIsp());

        firebaseDatabaseProvider.pushData("/info/wanInfo", wanInfo);
    }

    private void notifyServerStarted() {
        if (alreadyFired) {
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(REASON, SERVER_STARTUP_OR_SHUTDOWN);
        jsonObject.addProperty(ACTION, STARTING);
        jsonObject.addProperty(PID, getPid());
        jsonObject.addProperty(SERVER_NAME, serverAlias);

        sendFcmMessage(jsonObject, ALL);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        alreadyFired = true;
    }

    private void tryToFillJsonObjectWithImage(JsonObject jsonObject, BufferedImage image) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", bos);
            byte[] imageBytes = bos.toByteArray();

            BASE64Encoder encoder = new BASE64Encoder();
            jsonObject.addProperty(IMAGE, encoder.encode(imageBytes));

            bos.close();
        } catch (IOException e) {
            LOG.error("Error occurred: ", e);
        }
    }

    private void sendFcmMessage(JsonObject messageData, String notificationType) {
        clients.forEach(client -> {
            if (client.getNotificationType().equals(notificationType) || notificationType.equals(ALL)) {
                String userId = client.getUserId();
                String device = client.getDevice();
                String appVersion = client.getAppVersion();
                String token = client.getToken();

                LOG.info("Ready to send message to the Client:" + userId + " on device " + device + " with client version " + appVersion);

                notificationsService.sendFcmMessage(userId, token, messageData);
            }

        });
    }
}
