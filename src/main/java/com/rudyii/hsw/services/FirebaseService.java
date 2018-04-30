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

    private String serverAlias;
    private Random random = new Random();
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
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

    public FirebaseService(FirebaseDatabaseProvider firebaseDatabaseProvider, UuidService uuidService,
                           ArmedStateService armedStateService, Uptime uptime,
                           ReportingService reportingService, UpnpService upnpService,
                           EventService eventService, IspService ispService,
                           NotificationsService notificationsService, ThreadPoolTaskExecutor hswExecutor) {
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
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
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    @PostConstruct
    public void init() {
        Map<String, String> state = new HashMap<>();

        state.put("armedState", armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        state.put("armedMode", armedStateService.getArmedMode().toString());

        requests.put("state", state);
        requests.put("resendHourly", random.nextInt(999));
        requests.put("resendWeekly", random.nextInt(999));
        requests.put("portsOpen", upnpService.isPortsOpen());

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
        registerListeners();
    }

    @PreDestroy
    private void destroy() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("serverName", serverAlias);
        jsonObject.addProperty("reason", "serverStartupOrShutdown");
        jsonObject.addProperty("action", "stopping");

        sendFcmMessage(jsonObject);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

        unregisterListeners();
    }

    @EventListener({ArmedEvent.class, CameraRebootEvent.class, IspEvent.class, MotionDetectedEvent.class, UploadEvent.class})
    public void onEvent(EventBase event) {
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

            jsonObject.addProperty("reason", "motionDetected");
            jsonObject.addProperty("motionId", currentMotionTimestamp);
            jsonObject.addProperty("timeStamp", currentMotionTimestamp);
            jsonObject.addProperty("cameraName", motionDetectedEvent.getCameraName());
            jsonObject.addProperty("motionArea", motionDetectedEvent.getMotionArea().intValue());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                ImageIO.write(motionDetectedEvent.getCurrentImage(), "PNG", bos);
                byte[] imageBytes = bos.toByteArray();

                BASE64Encoder encoder = new BASE64Encoder();
                jsonObject.addProperty("image", encoder.encode(imageBytes));

                bos.close();
            } catch (IOException e) {
                LOG.error("Error occurred: ", e);
            }

            firebaseDatabaseProvider.pushData("/log/" + currentMotionTimestamp, new Gson().fromJson(jsonObject, HashMap.class)).addListener(new Runnable() {
                private JsonObject thisJsonObject;

                @Override
                public void run() {
                    this.thisJsonObject = jsonObject;
                    thisJsonObject.remove("cameraName");
                    thisJsonObject.remove("motionArea");
                    thisJsonObject.remove("timeStamp");
                    thisJsonObject.remove("image");

                    sendFcmMessage(thisJsonObject);
                }
            }, hswExecutor);

        } else if (event instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) event;

            jsonObject.addProperty("reason", "videoRecorded");
            jsonObject.addProperty("fileName", uploadEvent.getFileName());
            jsonObject.addProperty("url", uploadEvent.getUrl());

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

        firebaseDatabaseProvider.pushData("/log/" + event.getEventTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));
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

        DatabaseReference resendWeeklyRef = firebaseDatabaseProvider.getReference("/requests/resendWeekly");
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
                    firebaseDatabaseProvider.pushData("/statuses", statuses);
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

    @Scheduled(cron = "0 */1 * * * *")
    public void ping() {
        DatabaseReference pingRef = firebaseDatabaseProvider.getReference("/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference uptimeRef = firebaseDatabaseProvider.getReference("/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());

        updateConnectedClients();
        refreshWanInfo();
        notifyServerStarted();
    }

    private void updateConnectedClients() {
        DatabaseReference connectedClientsRef = firebaseDatabaseProvider.getReference("/connectedClients");
        connectedClientsRef.addListenerForSingleValueEvent(getConnectedClientsValueEventListener());
    }

    private void refreshWanInfo() {
        Map<String, Object> wanInfo = new HashMap<>();
        WanIp wanIp = ispService.getCurrentWanIp();

        wanInfo.put("wanIp", wanIp.getQuery());
        wanInfo.put("isp", wanIp.getIsp());

        firebaseDatabaseProvider.pushData("/info/wanInfo", wanInfo);
    }

    private void notifyServerStarted() {
        if (alreadyFired) {
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("reason", "serverStartupOrShutdown");
        jsonObject.addProperty("action", "starting");
        jsonObject.addProperty("pid", getPid());
        jsonObject.addProperty("serverName", serverAlias);

        sendFcmMessage(jsonObject);

        firebaseDatabaseProvider.pushData("/log/" + System.currentTimeMillis(), new Gson().fromJson(jsonObject, HashMap.class));

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
