package com.rudyii.hsw.services;

import com.rudyii.hsw.events.CameraRebootEvent;
import com.rudyii.hsw.helpers.BoardMonitor;
import com.rudyii.hsw.helpers.IpMonitor;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.providers.NotificationsService;
import com.rudyii.hsw.providers.StatsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by jack on 13.04.17.
 */
@Service
public class ReportingService {
    private static Logger LOG = LogManager.getLogger(ReportingService.class);
    private final String SUBJECT_HOURLY = "Home System hourly report";
    private final String SUBJECT_WEEKLY = "Home System weekly report";
    private Uptime uptime;
    private ArmedStateService armedStateService;
    private IpMonitor ipMonitor;
    private IspService ispService;
    private NotificationsService notificationsService;
    private BoardMonitor boardMonitor;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;
    private StatsProvider statsProvider;
    private EventService eventService;

    @Value("${weekly.report.enabled}")
    private Boolean weeklyReportEnabled;

    @Value("${hourly.report.forced}")
    private Boolean hourlyReportForced;

    @Value("${hourly.report.enabled}")
    private Boolean hourlyReportEnabled;

    @Value("${monitoring.enabled}")
    private Boolean monitoringEnabled;

    @Autowired
    public ReportingService(ArmedStateService armedStateService, IspService ispService,
                            NotificationsService notificationsService, IpMonitor ipMonitor,
                            Uptime uptime, StatsProvider statsProvider,
                            BoardMonitor boardMonitor, EventService eventService,
                            CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.armedStateService = armedStateService;
        this.ispService = ispService;
        this.notificationsService = notificationsService;
        this.ipMonitor = ipMonitor;
        this.uptime = uptime;
        this.statsProvider = statsProvider;
        this.boardMonitor = boardMonitor;
        this.eventService = eventService;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @Scheduled(cron = "${cron.hourly.report}")
    public void sendHourlyReportScheduled() {
        LOG.info("Generating hourly report...");
        if (armedStateService.isArmed() && hourlyReportEnabled) {
            sendHourlyReport();
        } else if (hourlyReportForced) {
            sendHourlyReport();
        } else {
            LOG.info("System neither ARMED nor hourly report forced, skipping hourly report sending.");
        }
    }

    @Scheduled(cron = "${cron.weekly.report}")
    public void sendWeeklyReportScheduled() {
        LOG.info("Generating weekly report.");
        if (weeklyReportEnabled) {
            sendWeeklyReport();
        }
    }

    @Async
    public void sendWeeklyReport() {
        ArrayList<String> body = new ArrayList<>();

        try {
            body = statsProvider.generateReportBody();
        } catch (Exception e) {
            LOG.error("ERROR getting stats!", e);
        }

        notificationsService.sendMessage(SUBJECT_WEEKLY, body, false);
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

                    Attachment attachment = new Attachment(cameraMotionDetectionController.getCameraName(), currentImageByteArray, "image/jpeg");
                    attachments.add(attachment);
                }
            } catch (Exception e) {
                LOG.error("Camera " + cameraMotionDetectionController.getCameraName() + " snapshot extraction failed:", e);
                eventService.publish(new CameraRebootEvent(cameraMotionDetectionController.getCameraName()));
            }
        }

        ArrayList<String> body = new ArrayList<>();

        body.add("Home system uptime: <b>" + uptime.getUptime() + "</b>");
        body.add("Current external IP: <b>" + ispService.getCurrentOrLastWanIpAddress() + "</b>");
        body.add("Current internal IP: <b>" + ispService.getLocalIpAddress() + "</b>");
        body.add("Total monitored cameras: <b>" + cameraMotionDetectionControllers.length + "</b>");

        if (monitoringEnabled) {
            body.add("Monitored targets states:");
            body.add("<ul>");
            ipMonitor.getStates().forEach(line -> body.add("<li>" + line));
            body.add("</ul>");

            body.addAll(boardMonitor.getMonitoringResults());
        }

        notificationsService.sendMessage(SUBJECT_HOURLY, body, attachments, false);
    }
}
