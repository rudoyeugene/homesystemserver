package com.rudyii.hsw.web;

import com.rudyii.hsw.helpers.BoardMonitor;
import com.rudyii.hsw.helpers.IpMonitor;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.services.ActionsService;
import com.rudyii.hsw.services.ArmedStateService;
import com.rudyii.hsw.services.FirebaseService;
import com.rudyii.hsw.services.UuidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.rudyii.hsw.enums.ArmedModeEnum.AUTOMATIC;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;

/**
 * Created by jack on 13.04.17.
 */
@RequestMapping("/admin")
@RestController
public class AdminPortal {

    private Uptime uptime;
    private ArmedStateService armedStateService;
    private BoardMonitor boardMonitor;
    private UuidService uuidService;
    private FirebaseService firebaseService;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;
    private IpMonitor ipMonitor;
    private ActionsService actionsService;

    @Value("${arm.delay.seconds}")
    private Long armDelaySeconds;

    @Value("${application.version}")
    private String appVersion;

    @Value("${client.apk.path}")
    private String apkFileLocation;

    @Autowired
    public AdminPortal(Uptime uptime, ArmedStateService armedStateService,
                       BoardMonitor boardMonitor, UuidService uuidService,
                       IpMonitor ipMonitor, ActionsService actionsService,
                       FirebaseService firebaseService,
                       CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.uptime = uptime;
        this.armedStateService = armedStateService;
        this.boardMonitor = boardMonitor;
        this.uuidService = uuidService;
        this.ipMonitor = ipMonitor;
        this.actionsService = actionsService;
        this.firebaseService = firebaseService;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView buildIndexPage() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("title", "Home System " + appVersion);
        modelAndView.addObject("armDelaySeconds", armDelaySeconds);
        modelAndView.addObject("currentState", armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        modelAndView.addObject("currentMode", armedStateService.getArmedMode() == AUTOMATIC ? AUTOMATIC.toString() : "MANUAL");
        modelAndView.addObject("uptime", uptime.getUptime());

        return modelAndView;
    }

    @RequestMapping(value = "/pair", method = RequestMethod.GET)
    public ModelAndView pair() {
        ModelAndView modelAndView = new ModelAndView("pair");
        modelAndView.addObject("pair", uuidService.getQRCodeImageUrl());
        return modelAndView;
    }

    @RequestMapping(value = "/resetSecret", method = RequestMethod.GET)
    public ModelAndView resetSecret() {
        uuidService.resetServerKey();
        return new ModelAndView("redirect:/admin/pair");
    }

    @RequestMapping(value = "/cameras", method = RequestMethod.GET)
    public ModelAndView buildCamerasPage() {
        ModelAndView modelAndView = new ModelAndView("cameras");

        ArrayList<ArrayList> cameraList = new ArrayList<>();

        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            ArrayList<String> cameraAttributes = new ArrayList<>();

            cameraAttributes.add(cameraMotionDetectionController.getCameraName());

            if (cameraMotionDetectionController.getMjpegUrl() != null) {
                cameraAttributes.add(cameraMotionDetectionController.getMjpegUrl());
            } else if (cameraMotionDetectionController.getJpegUrl() != null) {
                cameraAttributes.add(cameraMotionDetectionController.getJpegUrl());
            } else {
                cameraAttributes.add("http://www.solidbackgrounds.com/images/640x480/640x480-black-solid-color-background.jpg");
            }

            cameraList.add(cameraAttributes);
        }

        modelAndView.addObject("cameraList", cameraList);
        return modelAndView;
    }

    @RequestMapping(value = "/stats", method = RequestMethod.GET)
    public ModelAndView buildStats() {
        ModelAndView modelAndView = new ModelAndView("stats");

        modelAndView.addObject("boardStats", boardMonitor.getMonitoringResults());

        return modelAndView;
    }

    @RequestMapping(value = "/ipStates", method = RequestMethod.GET)
    public ModelAndView buildIpStates() {
        ModelAndView modelAndView = new ModelAndView("ipStates");

        modelAndView.addObject("ipStates", ipMonitor.getStates());

        return modelAndView;
    }

    @RequestMapping(value = "/fireAction", method = RequestMethod.POST)
    public void fireAction(@RequestParam(value = "action") String action) {
        actionsService.performAction(action);
    }

    @RequestMapping(value = "/rebootCamera", method = RequestMethod.POST)
    public void rebootCamera(@RequestParam(value = "cameraName") String cameraName) {
        actionsService.rebootCamera(cameraName);
    }

    @RequestMapping(value = "/currentState", method = RequestMethod.GET)
    public String currentState() {
        String result = "System is " + armedStateService.getArmedMode().toString() + " and " + (armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        return result;
    }

    @RequestMapping(value = "/uptime", method = RequestMethod.GET)
    public String uptime() {
        return "System uptime: " + uptime.getUptime();
    }

    @RequestMapping(path = "/downloadClient", method = RequestMethod.GET)
    public ResponseEntity<Resource> download() throws IOException {
        File file = new File(apkFileLocation);
        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=HomeSystemClient.apk")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
