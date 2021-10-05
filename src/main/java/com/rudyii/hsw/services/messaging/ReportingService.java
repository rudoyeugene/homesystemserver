package com.rudyii.hsw.services.messaging;

import com.rudyii.hsw.helpers.BoardMonitor;
import com.rudyii.hsw.helpers.IpMonitor;
import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.providers.NotificationsService;
import com.rudyii.hsw.services.SystemModeAndStateService;
import com.rudyii.hsw.services.firebase.FirebaseGlobalSettingsService;
import com.rudyii.hsw.services.internet.IspService;
import com.rudyii.hsw.services.system.EventService;
import com.rudyii.hsw.services.system.ServerKeyService;
import com.rudyii.hsw.services.system.UptimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReportingService {
    private final UptimeService uptimeService;
    private final SystemModeAndStateService systemModeAndStateService;
    private final IpMonitor ipMonitor;
    private final IspService ispService;
    private final NotificationsService notificationsService;
    private final BoardMonitor boardMonitor;
    private final ServerKeyService serverKeyService;
    private final FirebaseGlobalSettingsService globalSettingsService;
    private final List<Camera> cameras;
    private final EventService eventService;

    @Autowired
    public ReportingService(SystemModeAndStateService systemModeAndStateService, IspService ispService,
                            NotificationsService notificationsService, IpMonitor ipMonitor,
                            UptimeService uptimeService, BoardMonitor boardMonitor,
                            ServerKeyService serverKeyService, EventService eventService,
                            FirebaseGlobalSettingsService globalSettingsService, List<Camera> cameras) {
        this.systemModeAndStateService = systemModeAndStateService;
        this.ispService = ispService;
        this.notificationsService = notificationsService;
        this.ipMonitor = ipMonitor;
        this.uptimeService = uptimeService;
        this.boardMonitor = boardMonitor;
        this.eventService = eventService;
        this.serverKeyService = serverKeyService;
        this.globalSettingsService = globalSettingsService;
        this.cameras = cameras;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendHourlyReportScheduled() {
        if (systemModeAndStateService.isArmed() && (globalSettingsService.getGlobalSettings().isHourlyReportEnabled() || globalSettingsService.getGlobalSettings().isHourlyReportForced()))
            sendHourlyReport();
    }

    @Async
    public void sendHourlyReport() {
        ArrayList<Attachment> attachments = new ArrayList<>();

        cameras.forEach(camera -> {
            try {
                if (camera.getJpegUrl() != null) {
                    ByteArrayOutputStream currentImageBOS = new ByteArrayOutputStream();
                    BufferedImage currentImage = ImageIO.read(new URL(camera.getJpegUrl()));
                    ImageIO.write(currentImage, "jpeg", currentImageBOS);
                    byte[] currentImageByteArray = currentImageBOS.toByteArray();

                    attachments.add(Attachment.builder()
                            .name(camera.getCameraName())
                            .data(currentImageByteArray)
                            .mimeType("image/jpeg").build());
                }
            } catch (Exception e) {
                log.error("Camera " + camera.getCameraName() + " snapshot extraction failed:", e);
                eventService.publish(new CameraRebootEvent(camera.getCameraName()));
            }
        });

        ArrayList<String> body = new ArrayList<>();

        body.add("Home system uptime: <b>" + uptimeService.getUptime() + "</b>");
        body.add("Current external IP: <b>" + ispService.getCurrentOrLastWanIpAddress() + "</b>");
        body.add("Current internal IP: <b>" + ispService.getLocalIpAddress() + "</b>");
        body.add("Total monitored cameras: <b>" + cameras.size() + "</b>");

        if (globalSettingsService.getGlobalSettings().isMonitoringEnabled()) {
            body.add("Monitored targets states:");
            body.add("<ul>");
            ipMonitor.getStates().forEach(line -> body.add("<li>" + line));
            body.add("</ul>");

            body.addAll(boardMonitor.getMonitoringResults());
        }

        notificationsService.sendEmail(serverKeyService.getServerAlias() + " hourly report", body, attachments);
    }
}
