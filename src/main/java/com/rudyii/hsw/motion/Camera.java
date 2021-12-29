package com.rudyii.hsw.motion;

import com.google.common.net.MediaType;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hs.common.type.MonitoringModeType;
import com.rudyii.hs.common.type.SystemStateType;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.StorageProvider;
import com.rudyii.hsw.services.SystemModeAndStateService;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_CAMERA;
import static com.rudyii.hs.common.names.FirebaseNameSpaces.SETTINGS_ROOT;

@Slf4j
@Data
public class Camera {
    private final ApplicationContext context;
    private final PingService pingService;
    private final StorageProvider storageProvider;
    private final EventService eventService;
    private final SystemModeAndStateService systemModeAndStateService;
    private FirebaseDatabaseProvider firebaseDatabaseProvider;
    private CameraMotionDetector currentCameraMotionDetector;
    private boolean rebootInProgress;
    @Setter(AccessLevel.NONE)
    private CameraSettings cameraSettings;
    private String mjpegUrl, jpegUrl, rtspUrl, rebootUrl, cameraName;
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
    private VideoCaptor videoCaptor;

    @Autowired
    public Camera(ApplicationContext context, PingService pingService,
                  StorageProvider storageProvider, EventService eventService,
                  SystemModeAndStateService systemModeAndStateService, FirebaseDatabaseProvider firebaseDatabaseProvider) {
        this.context = context;
        this.pingService = pingService;
        this.storageProvider = storageProvider;
        this.eventService = eventService;
        this.systemModeAndStateService = systemModeAndStateService;
        this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    }

    @PostConstruct
    public void init() throws Exception {
        buildUrls();

        this.cameraSettings = CameraSettings.builder().build();

        if (isEnabled()) {
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
                switch (cameraSettings.getMonitoringMode()) {
                    case ENABLED -> enableMotionDetection();
                    case DISABLED -> disableMotionDetection();
                    case AUTO -> {
                        if (currentCameraMotionDetector == null && getSystemModeAndStateService().isArmed()) {
                            enableMotionDetection();
                        } else if (currentCameraMotionDetector != null && !getSystemModeAndStateService().isArmed()) {
                            disableMotionDetection();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to read/update Camera {} settings", cameraName, databaseError.toException());
            }
        });
    }

    private void buildUrls() {
        this.jpegUrl = buildUrlFromTemplate(jpegUrlTemplate);
        this.mjpegUrl = buildUrlFromTemplate(mjpegUrlTemplate);
        this.rtspUrl = buildUrlFromTemplate(rtspUrlTemplate);
        this.rebootUrl = buildUrlFromTemplate(rebootUrlTemplate);
    }

    public void enableMotionDetection() throws Exception {
        if (currentCameraMotionDetector != null) {
            log.warn("Detector already enabled on {}:\nCamera: {}\nSystem armed: {}", getCameraName(), cameraSettings.getMonitoringMode(), systemModeAndStateService.isArmed());
            return;
        } else if (MonitoringModeType.DISABLED.equals(cameraSettings.getMonitoringMode())) {
            log.warn("Skip detector change on {}:\nCamera: {}\nSystem armed: {}", getCameraName(), cameraSettings.getMonitoringMode(), systemModeAndStateService.isArmed());
            return;
        }

        if (isOnline()) {
            this.currentCameraMotionDetector = context.getBean(CameraMotionDetector.class);

            currentCameraMotionDetector.on(this).start();

            log.info("Motion detector enabled for camera: {}", getCameraName());
        } else {
            log.error("Failed to start motion detection on camera {} due to OFFLINE", getCameraName());
        }
    }

    public void disableMotionDetection() throws IOException {
        if (currentCameraMotionDetector == null) {
            log.warn("Detector already disabled on {}:\nCamera: {}\nSystem armed: {}", getCameraName(), cameraSettings.getMonitoringMode(), systemModeAndStateService.isArmed());
            return;
        } else if (MonitoringModeType.ENABLED.equals(cameraSettings.getMonitoringMode())) {
            log.warn("Skip detector change on {}:\nCamera: {}\nSystem armed: {}", getCameraName(), cameraSettings.getMonitoringMode(), systemModeAndStateService.isArmed());
            return;
        }

        if (currentCameraMotionDetector != null) {
            try {
                currentCameraMotionDetector.stop();
            } catch (Exception e) {
                log.warn("Some error occurred during disabling motion detector on camera {}", getCameraName(), e);
            }
            this.currentCameraMotionDetector = null;
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
            log.info("Sending 'GET' request to URL : " + url);
            log.info("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            log.info(response.toString());
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
    @EventListener({ArmedEvent.class, MotionDetectedEvent.class, SystemStateChangedEvent.class})
    public void onEvent(EventBase event) throws Exception {
        if (event instanceof ArmedEvent armedEvent) {
            enableDisableDetector(armedEvent.getSystemState());
        } else if (event instanceof SystemStateChangedEvent systemStateChangedEvent) {
            enableDisableDetector(systemStateChangedEvent.getSystemState());
        } else if (event instanceof MotionDetectedEvent motionDetectedEvent) {
            if (!motionDetectedEvent.getCameraName().equals(getCameraName())) {
                return;
            }

            if (videoCaptor != null) {
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
                    assignCaptorAndStartCapture(context.getBean(VideoCaptor.class));

                    eventService.publish(MotionToNotifyEvent.builder()
                            .cameraName(getCameraName())
                            .currentImage(motionDetectedEvent.getCurrentImage())
                            .motionObject(motionDetectedEvent.getMotionObject())
                            .motionArea(motionDetectedEvent.getMotionArea())
                            .eventId(motionDetectedEvent.getEventId())
                            .snapshotUrl(uploadMotionImageFrom(motionDetectedEvent.getEventId(), bufferedImage))
                            .build());

                } catch (Exception e) {
                    log.error("Failed to capture video", e);
                }
            }
        }
    }

    @SneakyThrows
    private void assignCaptorAndStartCapture(VideoCaptor captor) {
        this.videoCaptor = captor;
        videoCaptor.startCaptureFrom(this);
    }

    private synchronized void enableDisableDetector(SystemStateType systemState) throws Exception {
        switch (systemState) {
            case ARMED -> enableMotionDetection();
            case DISARMED -> disableMotionDetection();
        }
    }

    private boolean isEnabled() {
        return MonitoringModeType.ENABLED.equals(cameraSettings.getMonitoringMode());
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
        return currentCameraMotionDetector != null;
    }

    public boolean isRecordingInProgress() {
        return videoCaptor != null;
    }

    public boolean isRebootInProgress() {
        return rebootInProgress;
    }

    public void rebootComplete() {
        this.rebootInProgress = false;
    }

    public void resetVideoCaptor() {
        this.videoCaptor = null;
    }

    public long getRebootTimeoutSec() {
        return cameraSettings.getRebootTimeoutSec();
    }
}
