package com.rudyii.hsw.services;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.events.*;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.objects.WanIp;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;
import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.isHomeSystemInitComplete;

@Service
public class FirebaseService {
    private static Logger LOG = LogManager.getLogger(FirebaseService.class);

    @Value("${application.version}")
    private String appVersion;

    @Value("${motion.record.length.millis}")
    private Long recordInterval;

    @Value("${motion.snapshot.keep.minutes}")
    private Long snapshotKeepMinutes;

    private String serverAlias;
    private Random random = new Random();
    private FirebaseDatabase firebaseDatabase;
    private UuidService uuidService;
    private ArmedStateService armedStateService;
    private Uptime uptime;
    private ReportingService reportingService;
    private UpnpService upnpService;
    private EventService eventService;
    private IspService ispService;
    private NotificationsService notificationsService;
    private ThreadPoolTaskExecutor hswExecutor;
    private ArrayList<DatabaseReference> databaseReferences;
    private ArrayList<ValueEventListener> valueEventListeners;

    private Map<String, Object> statuses, motions, requests;
    private Map<String, String> localConnectedClients;
    private boolean alreadyFired;

    public FirebaseService(FirebaseDatabase firebaseDatabase, UuidService uuidService,
                           ArmedStateService armedStateService, Uptime uptime,
                           ReportingService reportingService, UpnpService upnpService,
                           EventService eventService, IspService ispService,
                           NotificationsService notificationsService, ThreadPoolTaskExecutor hswExecutor) {
        this.firebaseDatabase = firebaseDatabase;
        this.uuidService = uuidService;
        this.armedStateService = armedStateService;
        this.uptime = uptime;
        this.reportingService = reportingService;
        this.upnpService = upnpService;
        this.eventService = eventService;
        this.ispService = ispService;
        this.notificationsService = notificationsService;
        this.hswExecutor = hswExecutor;

        this.statuses = Collections.synchronizedMap(new HashMap<>());
        this.motions = Collections.synchronizedMap(new HashMap<>());
        this.requests = Collections.synchronizedMap(new HashMap<>());
        this.localConnectedClients = Collections.synchronizedMap(new HashMap<>());
        this.databaseReferences = new ArrayList();
        this.valueEventListeners = new ArrayList();

        this.serverAlias = uuidService.getServerAlias();
        createStructure(null);
    }

    @PostConstruct
    private void init() {
        DatabaseReference pidRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/pid");
        pidRef.setValueAsync(getPid());

        DatabaseReference pingRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference serverVersionRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/serverVersion");
        serverVersionRef.setValueAsync(appVersion);

        DatabaseReference uptimeRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());

        updateConnectedClients();
        registerListeners();
    }

    @PreDestroy
    private void destroy() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("serverName", serverAlias);
        jsonObject.addProperty("reason", "serverStartupOrShutdown");
        jsonObject.addProperty("action", "stopping");

        sendFcmMessage(jsonObject);

        pushData(uuidService.getServerKey() + "/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        unregisterListeners();
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    public void createStructure(ServerKeyUpdatedEvent event) {
        Map<String, String> state = new HashMap<>();

        state.put("armedState", armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        state.put("armedMode", armedStateService.getArmedMode().toString());

        requests.put("state", state);
        requests.put("resendHourly", random.nextInt(999));
        requests.put("resendWeekly", random.nextInt(999));
        requests.put("portsOpen", upnpService.isPortsOpen());

        updateStatuses(armedStateService.isArmed() ? ARMED : DISARMED, armedStateService.getArmedMode());

        if (event == null) {
            pushData(uuidService.getServerKey() + "/statuses", statuses);
            pushData(uuidService.getServerKey() + "/requests", requests);
        } else {
            pushData(event.getServerKey() + "/statuses", statuses);
            pushData(event.getServerKey() + "/requests", requests);
            unregisterListeners();
            registerListeners();
        }
    }

    @EventListener({ArmedEvent.class, CameraRebootEvent.class, CaptureEvent.class, IspEvent.class, MotionDetectedEvent.class})
    private void onEvent(EventBase event) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("serverName", serverAlias);

        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;

            updateStatuses(armedEvent.getArmedState(), armedEvent.getArmedMode());

            jsonObject.addProperty("reason", "systemStateChanged");
            jsonObject.addProperty("armedMode", armedEvent.getArmedMode().toString());
            jsonObject.addProperty("armedState", armedEvent.getArmedState().toString());
            jsonObject.addProperty("portsOpen", upnpService.isPortsOpen());

            sendFcmMessage(jsonObject);

        } else if (event instanceof MotionDetectedEvent) {
            MotionDetectedEvent motionDetectedEvent = (MotionDetectedEvent) event;

            Long lastMotionTimestamp = (Long) motions.get(motionDetectedEvent.getCameraName());
            Long currentMotionTimestamp = motionDetectedEvent.getEventTimeMillis();

            if (lastMotionTimestamp == null) {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            } else if ((currentMotionTimestamp - lastMotionTimestamp) < recordInterval) {
                System.out.println("Motion event for camera " + (motionDetectedEvent.getCameraName() + " was ignored"));
                return;
            } else {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            }

            Map<String, Object> camera = new HashMap<>();
            camera.put("cameraName", motionDetectedEvent.getCameraName());
            camera.put("timeStamp", currentMotionTimestamp);
            camera.put("motionArea", motionDetectedEvent.getMotionArea().intValue());

            jsonObject.addProperty("reason", "motionDetected");
            jsonObject.addProperty("motionId", currentMotionTimestamp);
            jsonObject.addProperty("cameraName", motionDetectedEvent.getCameraName());
            jsonObject.addProperty("motionArea", motionDetectedEvent.getMotionArea().intValue());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                ImageIO.write(motionDetectedEvent.getCurrentImage(), "PNG", bos);
                byte[] imageBytes = bos.toByteArray();

                BASE64Encoder encoder = new BASE64Encoder();
                camera.put("image", encoder.encode(imageBytes));

                bos.close();
            } catch (IOException e) {
                LOG.error("Error occurred: ", e);
            }

            pushData(uuidService.getServerKey() + "/motions/" + currentMotionTimestamp, camera).addListener(new Runnable() {
                private JsonObject thisJsonObject;

                @Override
                public void run() {
                    this.thisJsonObject = jsonObject;
                    sendFcmMessage(thisJsonObject);
                }
            }, hswExecutor);

        } else if (event instanceof CaptureEvent) {
            CaptureEvent captureEvent = (CaptureEvent) event;

            jsonObject.addProperty("reason", "videoRecorded");
            jsonObject.addProperty("fileName", captureEvent.getUploadCandidate().getName());

        } else if (event instanceof IspEvent) {
            refreshWanInfo();

            jsonObject.addProperty("reason", "ispChanged");
            jsonObject.addProperty("isp", ispService.getCurrentWanIp().getIsp());
            jsonObject.addProperty("ip", ispService.getCurrentWanIp().getQuery());

            sendFcmMessage(jsonObject);

        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;

            jsonObject.addProperty("reason", "cameraReboot");
            jsonObject.addProperty("cameraName", cameraRebootEvent.getCameraName());

            sendFcmMessage(jsonObject);
        }

        pushData(uuidService.getServerKey() + "/log/" + event.getEventTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));
    }

    private void updateStatuses(ArmedStateEnum armedState, ArmedModeEnum armedMode) {
        statuses.put("armedState", armedState);
        statuses.put("armedMode", armedMode);

        if (armedState.equals(ARMED)) {
            statuses.put("portsOpen", true);
        } else {
            statuses.put("portsOpen", false);
        }
        statuses.put("timeStamp", System.currentTimeMillis());

        pushData(uuidService.getServerKey() + "/statuses", statuses);
    }

    private void registerListeners() {
        DatabaseReference openPortsRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/requests/portsOpen");
        databaseReferences.add(openPortsRef);
        ValueEventListener openPortsRefValueEventListener = getOpenPortsValueEventListener();
        valueEventListeners.add(openPortsRefValueEventListener);
        openPortsRef.addValueEventListener(openPortsRefValueEventListener);

        DatabaseReference armRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/requests/state");
        databaseReferences.add(armRef);
        ValueEventListener armRefValueEventListener = getArmedEventValueEventListener();
        valueEventListeners.add(armRefValueEventListener);
        armRef.addValueEventListener(armRefValueEventListener);

        DatabaseReference resendHourlyRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/requests/resendHourly");
        databaseReferences.add(resendHourlyRef);
        ValueEventListener resendHourlyRefValueEventListener = getResendHourlyValueEventListener();
        valueEventListeners.add(resendHourlyRefValueEventListener);
        resendHourlyRef.addValueEventListener(resendHourlyRefValueEventListener);

        DatabaseReference resendWeeklyRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/requests/resendWeekly");
        databaseReferences.add(resendWeeklyRef);
        ValueEventListener resendWeeklyRefValueEventListener = getResendWeeklyValueEventListener();
        valueEventListeners.add(resendWeeklyRefValueEventListener);
        resendWeeklyRef.addValueEventListener(resendWeeklyRefValueEventListener);
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
                        statuses.put("portsOpen", true);
                    } else {
                        upnpService.closePorts();
                        statuses.put("portsOpen", false);
                    }

                    statuses.put("timeStamp", System.currentTimeMillis());
                    pushData(uuidService.getServerKey() + "/statuses", statuses);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Ports State Firebase data!");
            }
        };
    }

    private ValueEventListener getResendWeeklyValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!requests.get("resendWeekly").toString().equals(dataSnapshot.getValue().toString()) && isHomeSystemInitComplete()) {
                    reportingService.sendWeeklyReport();
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Weekly Resend Firebase data!");
            }
        };
    }

    private ValueEventListener getResendHourlyValueEventListener() {
        return new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!requests.get("resendHourly").toString().equals(dataSnapshot.getValue().toString()) && isHomeSystemInitComplete()) {
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
                    Map<String, Object> state = (Map<String, Object>) dataSnapshot.getValue();

                    ArmedEvent armedEvent = new ArmedEvent(ArmedModeEnum.valueOf(state.get("armedMode").toString()), ArmedStateEnum.valueOf(state.get("armedState").toString()));

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
                Map<String, String> connectedClients = (Map<String, String>) dataSnapshot.getValue();

                if (connectedClients != null) {
                    localConnectedClients.clear();
                    localConnectedClients.putAll(connectedClients);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Connected Clients Firebase data!");
            }
        };
    }

    private ValueEventListener getMotionsCleanupValueEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> motions = (Map<String, Object>) dataSnapshot.getValue();

                motions.forEach((timeStampString, data) -> {
                    Long timeStamp = Long.valueOf(timeStampString);

                    if (timeStamp < (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(snapshotKeepMinutes))) {
                        DatabaseReference obsoleteRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/motions/" + timeStampString);
                        obsoleteRef.removeValueAsync();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LOG.error("Failed to fetch Motions Obsolete Records Firebase data!");
            }
        };
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void ping() {
        DatabaseReference pingRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference uptimeRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());

        updateConnectedClients();
        refreshWanInfo();
        notifyServerStarted();
        cleanupObsoleteMotions();
    }

    private void cleanupObsoleteMotions() {
        DatabaseReference motionsRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/motions");
        motionsRef.addListenerForSingleValueEvent(getMotionsCleanupValueEventListener());
    }

    private void updateConnectedClients() {
        DatabaseReference connectedClientsRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/connectedClients");
        connectedClientsRef.addListenerForSingleValueEvent(getConnectedClientsValueEventListener());
    }

    private void refreshWanInfo() {
        Map<String, Object> wanInfo = new HashMap<>();
        WanIp wanIp = ispService.getCurrentWanIp();

        wanInfo.put("wanIp", wanIp.getQuery());
        wanInfo.put("isp", wanIp.getIsp());

        pushData(uuidService.getServerKey() + "/info/wanInfo", wanInfo);
    }

    private ApiFuture pushData(String path, Map<?, ?> value) {
        DatabaseReference reference = firebaseDatabase.getReference(path);
        return reference.setValueAsync(value);
    }

    private ApiFuture pushData(String path, String value) {
        DatabaseReference reference = firebaseDatabase.getReference(path);
        return reference.setValueAsync(value);
    }


    public void notifyServerStarted() {
        if (alreadyFired) {
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("reason", "serverStartupOrShutdown");
        jsonObject.addProperty("action", "starting");
        jsonObject.addProperty("pid", getPid());
        jsonObject.addProperty("serverName", serverAlias);

        sendFcmMessage(jsonObject);

        pushData(uuidService.getServerKey() + "/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        alreadyFired = true;
    }

    private void sendFcmMessage(JsonObject messageData) {
        localConnectedClients.forEach((name, token) -> {
            try {
                notificationsService.sendFcmMessage(name, token, messageData);
            } catch (Exception e) {
                LOG.error("Failed to send FCM Message to " + name, e);
            }
        });

    }
}
