package com.rudyii.hsw.motion;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.objects.events.ArmedEvent;
import com.rudyii.hsw.objects.events.EventBase;
import com.rudyii.hsw.objects.events.MotionDetectedEvent;
import com.rudyii.hsw.objects.events.OptionsChangedEvent;
import com.rudyii.hsw.services.PingService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static com.rudyii.hsw.configuration.OptionsService.CONTINUOUS_MONITORING;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;

@Slf4j
public class Camera {
    private ApplicationContext context;
    private PingService pingService;
    private CameraMotionDetector currentCameraMotionDetector;
    private File lock;
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
    private boolean detectorEnabled;
    @Getter
    @Setter
    private boolean healthCheckEnabled;
    private boolean rebootInProgress;
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
    public Camera(ApplicationContext context, PingService pingService) {
        this.context = context;
        this.pingService = pingService;
    }

    @PostConstruct
    public void init() throws Exception {
        this.lock = new File(cameraName + ".lock");

        buildUrls();

        if (autostartMonitoring || continuousMonitoring) {
            enableMotionDetection();
        }
    }

    private void buildUrls() {
        jpegUrl = buildUrlFromTemplate(jpegUrlTemplate);
        mjpegUrl = buildUrlFromTemplate(mjpegUrlTemplate);
        rtspUrl = buildUrlFromTemplate(rtspUrlTemplate);
        rebootUrl = buildUrlFromTemplate(rebootUrlTemplate);
    }

    @Async
    public void enableMotionDetection() throws Exception {
        this.detectorEnabled = true;

        this.currentCameraMotionDetector = context.getBean(CameraMotionDetector.class);

        currentCameraMotionDetector.on(this).start();

        log.info("Motion detector enabled for camera: {}", cameraName);
    }

    public void disableMotionDetection() throws IOException {
        if (continuousMonitoring) return;

        this.detectorEnabled = false;

        if (currentCameraMotionDetector != null) {
            try {
                currentCameraMotionDetector.stop();
            } catch (Exception e) {
                log.warn("Some error occurred during disabling motion detector on camera {}", cameraName, e);
            }
            this.currentCameraMotionDetector = null;
        }

        Files.delete(lock.toPath());

        log.info("Motion detector disabled for camera: {}", cameraName);
    }

    public void performReboot() {
        this.rebootInProgress = true;

        String USER_AGENT = "Mozilla/5.0";

        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(rebootUrl);
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
                .replace("${ip}", ip)
                .replace("${httpPort}", httpPort.toString())
                .replace("${rtspPort}", rtspPort.toString())
                .replace("${login}", login)
                .replace("${password}", password);
    }

    @Async
    @EventListener(EventBase.class)
    public void onEvent(EventBase event) throws Exception {
        if (event instanceof ArmedEvent) {
            ArmedEvent armedEvent = (ArmedEvent) event;
            if (armedEvent.getArmedState().equals(ARMED) && !detectorEnabled) {
                enableMotionDetection();
            } else if (armedEvent.getArmedState().equals(DISARMED)
                    && detectorEnabled
                    && !continuousMonitoring) {
                disableMotionDetection();
            } else {
                log.warn("New ArmedEvent received but system state unchanged.");
            }
        } else if (event instanceof MotionDetectedEvent) {
            MotionDetectedEvent motionDetectedEvent = (MotionDetectedEvent) event;
            if (!motionDetectedEvent.getCameraName().equals(cameraName)) {
                return;
            }

            if (lock.exists()) {
                System.out.println("New motion detected on camera: " + cameraName + " but previous capture is in progress, waiting...");
            } else {
                System.out.println("New motion detected at: " + new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date()) + " on camera: " + cameraName);
                try {
                    lock.createNewFile();
                    context.getBean(VideoCaptor.class).startCaptureFrom(this);
                } catch (Exception e) {
                    log.error("Failed to lock {}", lock.getAbsolutePath(), e);
                }
            }
        } else if (event instanceof OptionsChangedEvent) {
            ConcurrentHashMap<String, Object> cameraOptions = ((OptionsChangedEvent) event).getCameraOptions(cameraName);
            if (!detectorEnabled && (Boolean) cameraOptions.get(CONTINUOUS_MONITORING)) {
                enableMotionDetection();
            }
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
