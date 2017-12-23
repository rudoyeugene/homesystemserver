package com.rudyii.hsw.services;

import com.google.firebase.database.*;
import com.rudyii.hsw.enums.ArmedModeEnum;
import com.rudyii.hsw.enums.ArmedStateEnum;
import com.rudyii.hsw.events.*;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.WanIp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;
import static com.rudyii.hsw.helpers.PidGeneratorShutdownHandler.getPid;
import static com.rudyii.hsw.helpers.SimplePropertiesKeeper.isHomeSystemInitComplete;

@Service
public class FirebaseService {

    @Value("${application.version}")
    private String appVersion;

    @Value("${motion.record.length.millis}")
    private Long recordInterval;

    private Random random = new Random();
    private FirebaseDatabase firebaseDatabase;
    private UuidService uuidService;
    private ArmedStateService armedStateService;
    private Uptime uptime;
    private ReportingService reportingService;
    private UpnpService upnpService;
    private EventService eventService;
    private IspService ispService;
    private CameraMotionDetectionController[] motionDetectionControllers;
    private ArrayList<DatabaseReference> databaseReferences;
    private ArrayList<ValueEventListener> valueEventListeners;

    private Map<String, Object> statuses, motions, requests, settings;

    public FirebaseService(FirebaseDatabase firebaseDatabase, UuidService uuidService,
                           ArmedStateService armedStateService, Uptime uptime,
                           ReportingService reportingService, UpnpService upnpService,
                           EventService eventService, IspService ispService,
                           CameraMotionDetectionController... motionDetectionControllers) {
        this.firebaseDatabase = firebaseDatabase;
        this.uuidService = uuidService;
        this.armedStateService = armedStateService;
        this.uptime = uptime;
        this.reportingService = reportingService;
        this.upnpService = upnpService;
        this.eventService = eventService;
        this.ispService = ispService;
        this.motionDetectionControllers = motionDetectionControllers;

        this.statuses = new HashMap<>();
        this.motions = new HashMap<>();
        this.requests = new HashMap<>();
        this.settings = new HashMap<>();
        this.databaseReferences = new ArrayList();
        this.valueEventListeners = new ArrayList();

        createStructure(null);
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

        StringBuilder stringBuilder = new StringBuilder();
        for (CameraMotionDetectionController controller : motionDetectionControllers) {
            stringBuilder.append(controller.getCameraName() + ",");
        }
        settings.put("cameraList", stringBuilder.substring(0, stringBuilder.length() - 1));

        updateStatuses(armedStateService.isArmed() ? ARMED : DISARMED, armedStateService.getArmedMode());

        if (event == null) {
            pushData(uuidService.getServerKey() + "/statuses", statuses);
            pushData(uuidService.getServerKey() + "/requests", requests);
            pushData(uuidService.getServerKey() + "/settings", settings);
        } else {
            pushData(event.getServerKey() + "/statuses", statuses);
            pushData(event.getServerKey() + "/requests", requests);
            pushData(event.getServerKey() + "/settings", settings);
        }

        registerListeners();
    }

    private void pushData(String path, Map<String, Object> value) {
        DatabaseReference reference = firebaseDatabase.getReference(path);
        reference.setValueAsync(value);
    }

    private void pushData(String path, String value) {
        DatabaseReference reference = firebaseDatabase.getReference(path);
        reference.setValueAsync(value);
    }

    @EventListener({ArmedEvent.class, MotionDetectedEvent.class, CaptureEvent.class, CameraRebootEvent.class})
    private void onEvent(EventBase event) {
        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;

            updateStatuses(armedEvent.getArmedState(), armedEvent.getArmedMode());

        } else if (event instanceof MotionDetectedEvent) {
            MotionDetectedEvent motionDetectedEvent = (MotionDetectedEvent) event;

            Long lastMotionTimestamp = (Long) motions.get(motionDetectedEvent.getCameraName());
            Long currentMotionTimestamp = System.currentTimeMillis();

            if (lastMotionTimestamp == null) {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            } else if ((currentMotionTimestamp - lastMotionTimestamp) < (recordInterval)) {
                System.out.println("Motion event for camera " + (motionDetectedEvent.getCameraName() + " was ignored"));
                return;
            } else {
                motions.put(motionDetectedEvent.getCameraName(), currentMotionTimestamp);
            }

            Map<String, Object> camera = new HashMap<>();

            camera.put("motionArea", motionDetectedEvent.getMotionArea().intValue());
            camera.put("timeStamp", System.currentTimeMillis());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                ImageIO.write(motionDetectedEvent.getCurrentImage(), "PNG", bos);
                byte[] imageBytes = bos.toByteArray();

                BASE64Encoder encoder = new BASE64Encoder();
                camera.put("image", encoder.encode(imageBytes));

                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            pushData(uuidService.getServerKey() + "/motions/" + motionDetectedEvent.getCameraName(), camera);

        } else if (event instanceof CaptureEvent) {
            CaptureEvent captureEvent = (CaptureEvent) event;

            pushData(uuidService.getServerKey() + "/motions/lastRecord", captureEvent.getUploadCandidate().getName());
        } else if (event instanceof CameraRebootEvent) {
            CameraRebootEvent cameraRebootEvent = (CameraRebootEvent) event;

            Map<String, Object> offlineDevice = new HashMap<>();
            offlineDevice.put(cameraRebootEvent.getCameraName(), System.currentTimeMillis());

            pushData(uuidService.getServerKey() + "/offlineDevices", offlineDevice);
        }
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

    @PostConstruct
    private void updateDb() {
        DatabaseReference serverVersion = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/serverVersion");
        serverVersion.setValueAsync(appVersion);

        DatabaseReference pidRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/pid");
        pidRef.setValueAsync(getPid());

        registerListeners();
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
            }
        };
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void ping() {
        DatabaseReference pingRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/ping");
        pingRef.setValueAsync(System.currentTimeMillis());

        DatabaseReference uptimeRef = firebaseDatabase.getReference(uuidService.getServerKey() + "/info/uptime");
        uptimeRef.setValueAsync(uptime.getUptimeLong());


        Map<String, Object> wanInfo = new HashMap<>();
        WanIp wanIp = ispService.getCurrentWanIp();

        wanInfo.put("wanIp", wanIp.getQuery());
        wanInfo.put("isp", wanIp.getIsp());

        pushData(uuidService.getServerKey() + "/info/wanInfo", wanInfo);
    }
}
