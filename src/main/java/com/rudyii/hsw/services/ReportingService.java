package com.rudyii.hsw.services;

import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.helpers.BoardMonitor;
import com.rudyii.hsw.helpers.IpMonitor;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.events.CameraRebootEvent;
import com.rudyii.hsw.providers.NotificationsService;
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

import static com.rudyii.hsw.configuration.OptionsService.*;

@Slf4j
@Service
public class ReportingService {
    private Uptime uptime;
    private ArmedStateService armedStateService;
    private IpMonitor ipMonitor;
    private IspService ispService;
    private NotificationsService notificationsService;
    private BoardMonitor boardMonitor;
    private OptionsService optionsService;
    private UuidService uuidService;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;
    private EventService eventService;

    @Autowired
    public ReportingService(ArmedStateService armedStateService, IspService ispService,
                            NotificationsService notificationsService, IpMonitor ipMonitor,
                            Uptime uptime, BoardMonitor boardMonitor, EventService eventService,
                            OptionsService optionsService, UuidService uuidService,
                            CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.armedStateService = armedStateService;
        this.ispService = ispService;
        this.notificationsService = notificationsService;
        this.ipMonitor = ipMonitor;
        this.uptime = uptime;
        this.boardMonitor = boardMonitor;
        this.eventService = eventService;
        this.optionsService = optionsService;
        this.uuidService = uuidService;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendHourlyReportScheduled() {
        if (armedStateService.isArmed() && (boolean) optionsService.getOption(HOURLY_REPORT_ENABLED) || (boolean) optionsService.getOption(HOURLY_REPORT_FORCED))
            sendHourlyReport();
    }

    @Async
    public void sendHourlyReport() {
        ArrayList<Attachment> attachments = new ArrayList<>();

        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            try {
                if (cameraMotionDetectionController.getJpegUrl() != null) {
                    ByteArrayOutputStream currentImageBOS = new ByteArrayOutputStream();
                    BufferedImage currentImage = ImageIO.read(new URL(cameraMotionDetectionController.getJpegUrl()));
                    ImageIO.write(currentImage, "jpeg", currentImageBOS);
                    byte[] currentImageByteArray = currentImageBOS.toByteArray();

                    attachments.add(Attachment.builder()
                            .name(cameraMotionDetectionController.getCameraName())
                            .data(currentImageByteArray)
                            .mimeType("image/jpeg").build());
                }
            } catch (Exception e) {
                log.error("Camera " + cameraMotionDetectionController.getCameraName() + " snapshot extraction failed:", e);
                eventService.publish(new CameraRebootEvent(cameraMotionDetectionController.getCameraName()));
            }
        }

        ArrayList<String> body = new ArrayList<>();

        body.add("Home system uptime: <b>" + uptime.getUptime() + "</b>");
        body.add("Current external IP: <b>" + ispService.getCurrentOrLastWanIpAddress() + "</b>");
        body.add("Current internal IP: <b>" + ispService.getLocalIpAddress() + "</b>");
        body.add("Total monitored cameras: <b>" + cameraMotionDetectionControllers.length + "</b>");

        if ((boolean) optionsService.getOption(MONITORING_ENABLED)) {
            body.add("Monitored targets states:");
            body.add("<ul>");
            ipMonitor.getStates().forEach(line -> body.add("<li>" + line));
            body.add("</ul>");

            body.addAll(boardMonitor.getMonitoringResults());
        }

        notificationsService.sendEmail(uuidService.getServerAlias() + " hourly report", body, attachments);
    }
}
