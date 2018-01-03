package com.rudyii.hsw.motion;

import com.rudyii.hsw.events.ArmedEvent;
import com.rudyii.hsw.events.CameraRebootEvent;
import com.rudyii.hsw.events.MotionDetectedEvent;
import com.rudyii.hsw.objects.Camera;
import com.rudyii.hsw.services.ArmedStateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;

/**
 * Created by jack on 02.10.16.
 */
public class CameraMotionDetectionController {
    private static Logger LOG = LogManager.getLogger(CameraMotionDetectionController.class);

    private String mjpegUrl, jpegUrl, rtspUrl, rebootUrl, cameraName, monitoringMode;
    private Long interval, rebootTimeout;
    private Double motionArea;
    private int noiseLevel;
    private boolean detectorEnabled, healthCheckEnabled, rebootInProgress;
    private ArmedStateService armedStateService;
    private ApplicationContext context;
    private CameraMotionDetector currentCameraMotionDetector;
    private Camera camera;
    private File lock;
    private String ip;
    private Integer httpPort;
    private Integer outerHttpPort;
    private boolean useOuterHttpPort;
    private Integer rtspPort;
    private Integer outerRtspPort;
    private boolean useOuterRtspPort;
    private boolean openPortsOnStartup;
    private String login;
    private String password;
    private String mjpegUrlTemplate;
    private String jpegUrlTemplate;
    private String rtspUrlTemplate;
    private String rebootUrlTemplate;

    @Autowired
    public CameraMotionDetectionController(ArmedStateService armedStateService, ApplicationContext context) {
        this.armedStateService = armedStateService;
        this.context = context;
    }

    @PostConstruct
    public void init() throws Exception {
        this.lock = new File(cameraName + ".lock");

        buildUrls();
        this.camera = new Camera();
        camera.setName(cameraName);
        camera.setJpegUrl(jpegUrl);
        camera.setRtspUrl(rtspUrl);

        if (monitoringMode != null && monitoringMode.equals("CONTINUOUS")) {
            enableMotionDetection();
        }

        System.out.println("CameraMotionDetectionController for " + cameraName + " is initialized in " + monitoringMode + " mode");
    }

    private void buildUrls() {
        jpegUrl = jpegUrlTemplate.replace("${ip}", ip)
                .replace("${httpPort}", httpPort.toString())
                .replace("${login}", login)
                .replace("${password}", password);

        mjpegUrl = mjpegUrlTemplate.replace("${ip}", ip)
                .replace("${httpPort}", httpPort.toString())
                .replace("${login}", login)
                .replace("${password}", password);

        rtspUrl = rtspUrlTemplate.replace("${ip}", ip)
                .replace("${rtspPort}", rtspPort.toString())
                .replace("${login}", login)
                .replace("${password}", password);

        rebootUrl = rebootUrlTemplate.replace("${ip}", ip)
                .replace("${httpPort}", httpPort.toString())
                .replace("${login}", login)
                .replace("${password}", password);
    }

    @Async
    @EventListener(MotionDetectedEvent.class)
    public void motionDetected(MotionDetectedEvent motionDetectedEvent) {
        if (!motionDetectedEvent.getCameraName().equals(cameraName)) {
            return;
        }

        if (lock.exists()) {
            System.out.println("New motion detected on camera: " + cameraName + " but previous capture is in progress, waiting...");
        } else {
            System.out.println("New motion detected at: " + new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date()) + " on camera: " + cameraName);
            try {
                lock.createNewFile();
                context.getBean(VideoCaptor.class).startCaptureFrom(camera);
            } catch (IOException e) {
                LOG.error("Failed to lock " + lock.getAbsolutePath(), e);
            }
        }
    }

    @Async
    void enableMotionDetection() throws Exception {
        this.detectorEnabled = true;

        this.currentCameraMotionDetector = context.getBean(CameraMotionDetector.class);

        currentCameraMotionDetector.setCamera(camera);
        currentCameraMotionDetector.setInterval(interval);
        currentCameraMotionDetector.setMotionArea(motionArea);
        currentCameraMotionDetector.setNoiseLevel(noiseLevel);

        currentCameraMotionDetector.start();
        LOG.info("Motion detector enabled for camera:" + cameraName);
    }

    private void disableMotionDetection() {
        this.detectorEnabled = false;

        try {
            currentCameraMotionDetector.stop();
            this.currentCameraMotionDetector = null;
        } catch (Exception e) {
            LOG.warn("Some error occurred during disabling motion detector on camera " + cameraName, e);
        }

        lock.delete();

        LOG.info("Motion detector disabled for camera: " + cameraName);
    }

    private void performRebootSequence() throws Exception {
        this.rebootInProgress = true;

        if (detectorEnabled) {
            disableMotionDetection();
        }

        performReboot();

        if (armedStateService.isArmed()) {
            enableMotionDetection();
        }

        this.rebootInProgress = false;
    }

    private void performReboot() {
        LOG.info("Rebooting camera " + cameraName + ", will wait for " + rebootTimeout + " milliseconds.");
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
            LOG.error("Failed to reboot camera: " + getCameraName(), e);
            return;
        }

        waitForRebootCompletion();
    }

    private void waitForRebootCompletion() {
        LOG.info(String.format("Camera: " + getCameraName() + " reboot in progress...\nWaiting for %s milliseconds...", rebootTimeout));
        try {
            Thread.sleep(rebootTimeout);
        } catch (InterruptedException e) {
            LOG.error("Error occurred: ", e);
        }
        LOG.info("Camera: " + getCameraName() + " reboot complete");
    }

    @Async
    @EventListener(ArmedEvent.class)
    public void onEvent(ArmedEvent event) throws Exception {
        if (monitoringMode.equals("AUTO")) {
            if (event.getArmedState().equals(ARMED) && !detectorEnabled) {
                enableMotionDetection();
            } else if (event.getArmedState().equals(DISARMED) && detectorEnabled) {
                disableMotionDetection();
            } else {
                LOG.warn("New ArmedEvent received but system state unchanged.");
            }
        }
    }

    @Async
    @EventListener(CameraRebootEvent.class)
    public void onEvent(CameraRebootEvent event) throws Exception {
        if (event.getCameraName().equals(cameraName) && !rebootInProgress) {
            performRebootSequence();
        }
    }

    public String getMjpegUrl() {
        return mjpegUrl;
    }

    public String getJpegUrl() {
        return jpegUrl;
    }

    public String getRtspUrl() {
        return rtspUrl;
    }

    public String getRebootUrl() {
        return rebootUrl;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getHttpInnerPort() {
        return httpPort;
    }

    public Integer getHttpOuterPort() {
        return outerHttpPort;
    }

    public Integer getRtspInnerPort() {
        return rtspPort;
    }

    public Integer getRtspOuterPort() {
        return outerRtspPort;
    }

    public boolean openPortsOnStartup() {
        return openPortsOnStartup;
    }

    public boolean openOuterHttpPort() {
        return useOuterHttpPort;
    }

    public boolean openOuterRtspPort() {
        return useOuterRtspPort;
    }

    public Double getMotionArea() {
        return motionArea;
    }

    public void setMotionArea(Double motionArea) {
        this.motionArea = motionArea;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public int getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(int noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
    }

    public Long getRebootTimeout() {
        return rebootTimeout;
    }

    public void setRebootTimeout(Long rebootTimeout) {
        this.rebootTimeout = rebootTimeout;
    }

    public boolean isDetectorEnabled() {
        return detectorEnabled;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public boolean isRecordingInProgress() {
        return lock.exists();
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setOuterHttpPort(int outerHttpPort) {
        this.outerHttpPort = outerHttpPort;
    }

    public void setUseOuterHttpPort(boolean useOuterHttpPort) {
        this.useOuterHttpPort = useOuterHttpPort;
    }

    public void setRtspPort(int rtspPort) {
        this.rtspPort = rtspPort;
    }

    public void setOuterRtspPort(int outerRtspPort) {
        this.outerRtspPort = outerRtspPort;
    }

    public void setUseOuterRtspPort(boolean useOuterRtspPort) {
        this.useOuterRtspPort = useOuterRtspPort;
    }

    public void setOpenPortsOnStartup(boolean openPortsOnStartup) {
        this.openPortsOnStartup = openPortsOnStartup;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMjpegUrlTemplate(String mjpegUrlTemplate) {
        this.mjpegUrlTemplate = mjpegUrlTemplate;
    }

    public void setJpegUrlTemplate(String jpegUrlTemplate) {
        this.jpegUrlTemplate = jpegUrlTemplate;
    }

    public void setRtspUrlTemplate(String rtspUrlTemplate) {
        this.rtspUrlTemplate = rtspUrlTemplate;
    }

    public void setRebootUrlTemplate(String rebootUrlTemplate) {
        this.rebootUrlTemplate = rebootUrlTemplate;
    }
}
