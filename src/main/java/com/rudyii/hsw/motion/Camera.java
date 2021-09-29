package com.rudyii.hsw.motion;

import com.google.common.net.MediaType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.configuration.Logger;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.StorageProvider;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.system.EventService;
import com.rudyii.hsw.services.system.PingService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_CAMERA;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;
import static com.rudyii.hs.common.type.SystemStateType.ARMED;
import static com.rudyii.hs.common.type.SystemStateType.DISARMED;

@Slf4j
@Data
public class Camera {
    private final ApplicationContext context;
    private final PingService pingService;
    private final StorageProvider storageProvider;
    private final EventService eventService;
    private final ArmedStateService armedStateService;
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private Logger logger;
    private CameraMotionDetector currentCameraMotionDetector;
    private File lock;
    private boolean rebootInProgress, detectorEnabled;
    @Setter(AccessLevel.NONE)
    private CameraSettings cameraSettings;
    private String mjpegUrl, jpegUrl, rtspUrl, rebootUrl, cameraName;
    private int interval = 500;
    private int rebootTimeout = 60;
    private int noiseLevel = 5;
    private int motionArea = 20;
    private boolean healthCheckEnabled = true;
    private boolean autostartMonitoring;
    private boolean continuousMonitoring;
    private String ip;
    private Integer httpPort;
    private Integer rtspPort;
    private String login;
    private String password;
    private String mjpegUrlTemplate;
    private String jpegUrlTemplate;
    private String rtspUrlTemplate;
    private String rebootUrlTemplate;
    private String rtspTransport;

    @Autowired
    public Camera(ApplicationContext context, PingService pingService,
                  StorageProvider storageProvider, EventService eventService,
                  ArmedStateService armedStateService, FirebaseDatabaseProvider firebaseDatabaseProvider,
                  Logger logger) {
        this.context = context;
        this.pingService = pingService;
        this.storageProvider = storageProvider;
        this.eventService = eventService;
        this.armedStateService = armedStateService;
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
        this.logger = logger;
    }

    @PostConstruct
    public void init() throws Exception {
        this.lock = new File(getCameraName() + ".lock");

        try {
            if (lock.delete()) {
                log.warn("Deleted lock from previous run of {} Camera", cameraName);
            }
        } catch (Exception e) {
            log.info("Camera {} lock not found", cameraName);
        }

        buildUrls();

        this.cameraSettings = CameraSettings.builder()
                .continuousMonitoring(continuousMonitoring)
                .healthCheckEnabled(healthCheckEnabled)
                .showMotionObject(false)
                .interval(interval)
                .motionArea(motionArea)
                .noiseLevel(noiseLevel)
                .recordLength(5)
                .rebootTimeoutSec(rebootTimeout)
                .build();

        if (isAutostartMonitoring() || cameraSettings.isContinuousMonitoring()) {
            enableMotionDetection();
        }

        firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA).child(cameraName).addValueEventListener(new ValueEventListener() {
            @SneakyThrows
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    cameraSettings = dataSnapshot.getValue(CameraSettings.class);
                } else {
                    firebaseDatabaseProvider.getRootReference().child(SETTINGS_ROOT).child(SETTINGS_CAMERA).child(cameraName).setValueAsync(cameraSettings);
                }
                if (!detectorEnabled && cameraSettings.isContinuousMonitoring()) {
                    enableMotionDetection();
                } else if (detectorEnabled && !cameraSettings.isContinuousMonitoring() && !armedStateService.isArmed()) {
                    disableMotionDetection();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to read/update Camera {} settings", cameraName, databaseError);
            }
        });
    }

    private void buildUrls() {
        this.jpegUrl = buildUrlFromTemplate(jpegUrlTemplate);
        this.mjpegUrl = buildUrlFromTemplate(mjpegUrlTemplate);
        this.rtspUrl = buildUrlFromTemplate(rtspUrlTemplate);
        this.rebootUrl = buildUrlFromTemplate(rebootUrlTemplate);
    }

    @Async
    public void enableMotionDetection() throws Exception {
        if (isOnline()) {
            this.detectorEnabled = true;

            this.currentCameraMotionDetector = context.getBean(CameraMotionDetector.class);

            currentCameraMotionDetector.on(this).start();

            log.info("Motion detector enabled for camera: {}", getCameraName());
        } else {
            log.error("Failed to start motion detection on camera {} due to OFFLINE", getCameraName());
        }
    }

    public void disableMotionDetection() throws IOException {
        if (cameraSettings.isContinuousMonitoring()) return;

        this.detectorEnabled = false;

        if (currentCameraMotionDetector != null) {
            try {
                currentCameraMotionDetector.stop();
            } catch (Exception e) {
                log.warn("Some error occurred during disabling motion detector on camera {}", getCameraName(), e);
            }
            this.currentCameraMotionDetector = null;
        }

        try {
            lock.delete();
        } catch (Exception e) {
            log.info("No lock to delete for {}", cameraName);
        }

        log.info("Motion detector disabled for camera: {}", getCameraName());
    }

    public void performReboot() {
        this.rebootInProgress = true;

        String USER_AGENT = "Mozilla/5.0";

        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(getRebootUrl());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            urlConnection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = urlConnection.getResponseCode();
            logger.printAdditionalInfo("Sending 'GET' request to URL : " + url);
            logger.printAdditionalInfo("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            logger.printAdditionalInfo(response.toString());
            in.close();
        } catch (IOException e) {
            log.error("Failed to reboot camera: {}", getCameraName(), e);
        }
    }

    private String buildUrlFromTemplate(String template) {
        return template
                .replace("${ip}", getIp())
                .replace("${httpPort}", getHttpPort().toString())
                .replace("${rtspPort}", getRtspPort().toString())
                .replace("${login}", getLogin())
                .replace("${password}", getPassword());
    }

    @Async
    @EventListener({ArmedEvent.class, MotionDetectedEvent.class, SettingsUpdatedEvent.class})
    public void onEvent(EventBase event) throws Exception {
        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;
            if (ARMED.equals(armedEvent.getSystemState()) && !isDetectorEnabled()) {
                enableMotionDetection();
            } else if (DISARMED.equals(armedEvent.getSystemState())
                    && isDetectorEnabled()
                    && !cameraSettings.isContinuousMonitoring()) {
                disableMotionDetection();
            } else {
                log.warn("New ArmedEvent received but system state unchanged.");
            }
        } else if (event instanceof MotionDetectedEvent) {
            MotionDetectedEvent motionDetectedEvent = (MotionDetectedEvent) event;
            if (!motionDetectedEvent.getCameraName().equals(getCameraName())) {
                return;
            }

            if (lock.exists()) {
                log.info("New motion detected on camera: {} but previous capture is in progress, ignoring...", getCameraName());
            } else {
                log.info("New motion detected at: {} on Camera {}", new Date(), getCameraName());
                try {
                    BufferedImage bufferedImage;
                    if (cameraSettings.isShowMotionObject()) {
                        bufferedImage = motionDetectedEvent.getMotionObject();
                    } else {
                        bufferedImage = motionDetectedEvent.getCurrentImage();
                    }

                    eventService.publish(MotionToNotifyEvent.builder()
                            .cameraName(getCameraName())
                            .currentImage(motionDetectedEvent.getCurrentImage())
                            .motionObject(motionDetectedEvent.getMotionObject())
                            .motionArea(motionDetectedEvent.getMotionArea())
                            .eventId(motionDetectedEvent.getEventId())
                            .snapshotUrl(uploadMotionImageFrom(motionDetectedEvent.getEventId(), bufferedImage))
                            .build());

                    lock.createNewFile();
                    context.getBean(VideoCaptor.class).startCaptureFrom(this);
                } catch (Exception e) {
                    log.error("Failed to lock {}", lock.getAbsolutePath(), e);
                }
            }
        }
    }

    private URL uploadMotionImageFrom(long eventId, BufferedImage bufferedImage) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "JPG", bos);
            byte[] imageBytes = bos.toByteArray();
            return storageProvider.putData(eventId + ".jpg", MediaType.JPEG, imageBytes);
        } catch (Exception e) {
            log.error("Failed to upload Image", e);
            return null;
        }
    }

    public boolean isOnline() {
        return pingService.ping(getIp()).equals(IPStateEnum.ONLINE);
    }

    public boolean isDetectorEnabled() {
        return detectorEnabled;
    }

    public boolean isRecordingInProgress() {
        return lock.exists();
    }

    public boolean isRebootInProgress() {
        return rebootInProgress;
    }

    public void rebootComplete() {
        this.rebootInProgress = false;
    }
}
