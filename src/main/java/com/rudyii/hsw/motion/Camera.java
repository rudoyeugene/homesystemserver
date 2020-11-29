package com.rudyii.hsw.motion;

import com.google.common.net.MediaType;
import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.*;
import com.rudyii.hsw.providers.StorageProvider;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.EventService;
import com.rudyii.hsw.services.PingService;
import lombok.Getter;
import lombok.Setter;
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
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.configuration.OptionsService.CONTINUOUS_MONITORING;
import static com.rudyii.hsw.configuration.OptionsService.USE_MOTION_OBJECT;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;

@Slf4j
public class Camera {
    private final ApplicationContext context;
    private final PingService pingService;
    private final StorageProvider storageProvider;
    private final EventService eventService;
    private ArmedStateService armedStateService;
    private CameraMotionDetector currentCameraMotionDetector;
    private File lock;
    private boolean rebootInProgress, detectorEnabled, useMotionObject;
    @Getter
    @Setter
    private String mjpegUrl, jpegUrl, rtspUrl, rebootUrl, cameraName;
    @Getter
    @Setter
    private long interval = 500L;
    @Getter
    @Setter
    private long rebootTimeout = 60L;
    @Getter
    @Setter
    private long noiseLevel = 5L;
    @Getter
    @Setter
    private long motionArea = 20L;
    @Getter
    @Setter
    private boolean healthCheckEnabled;
    @Getter
    @Setter
    private boolean autostartMonitoring;
    @Getter
    @Setter
    private boolean continuousMonitoring;
    @Getter
    @Setter
    private String ip;
    @Getter
    @Setter
    private Integer httpPort;
    @Getter
    @Setter
    private Integer rtspPort;
    @Getter
    @Setter
    private String login;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private String mjpegUrlTemplate;
    @Getter
    @Setter
    private String jpegUrlTemplate;
    @Getter
    @Setter
    private String rtspUrlTemplate;
    @Getter
    @Setter
    private String rebootUrlTemplate;

    @Autowired
    public Camera(ApplicationContext context, PingService pingService,
                  StorageProvider storageProvider, EventService eventService,
                  ArmedStateService armedStateService) {
        this.context = context;
        this.pingService = pingService;
        this.storageProvider = storageProvider;
        this.eventService = eventService;
        this.armedStateService = armedStateService;
    }

    @PostConstruct
    public void init() throws Exception {
        this.lock = new File(getCameraName() + ".lock");

        buildUrls();

        if (isAutostartMonitoring() || isContinuousMonitoring()) {
            enableMotionDetection();
        }
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
        if (isContinuousMonitoring()) return;

        this.detectorEnabled = false;

        if (currentCameraMotionDetector != null) {
            try {
                currentCameraMotionDetector.stop();
            } catch (Exception e) {
                log.warn("Some error occurred during disabling motion detector on camera {}", getCameraName(), e);
            }
            this.currentCameraMotionDetector = null;
        }

        Files.delete(lock.toPath());

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
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            System.out.println(response);
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
    @EventListener(EventBase.class)
    public void onEvent(EventBase event) throws Exception {
        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;
            if (armedEvent.getArmedState().equals(ARMED) && !isDetectorEnabled()) {
                enableMotionDetection();
            } else if (armedEvent.getArmedState().equals(DISARMED)
                    && isDetectorEnabled()
                    && !isContinuousMonitoring()) {
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
                    if (useMotionObject) {
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
        } else if (event instanceof OptionsChangedEvent) {
            ConcurrentHashMap<String, Object> cameraOptions = ((OptionsChangedEvent) event).getCameraOptions(getCameraName());
            this.useMotionObject = (Boolean) cameraOptions.get(USE_MOTION_OBJECT);

            this.continuousMonitoring = (Boolean) cameraOptions.get(CONTINUOUS_MONITORING);
            if (!detectorEnabled && continuousMonitoring) {
                enableMotionDetection();
            } else if (detectorEnabled && !continuousMonitoring && !armedStateService.isArmed()) {
                disableMotionDetection();
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
