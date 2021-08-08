package com.rudyii.hsw.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rudyii.hsw.configuration.OptionsService;
import com.rudyii.hsw.helpers.BoardMonitor;
import com.rudyii.hsw.helpers.IpMonitor;
import com.rudyii.hsw.helpers.Uptime;
import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.providers.PairingDataProvider;
import com.rudyii.hsw.services.ActionsService;
import com.rudyii.hsw.services.ArmedStateService;
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
import java.util.List;

import static com.rudyii.hsw.configuration.OptionsService.DELAYED_ARM_INTERVAL;
import static com.rudyii.hsw.enums.ArmedModeEnum.AUTOMATIC;
import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;

@RestController
public class AdminPortal {
    private final Uptime uptime;
    private final ArmedStateService armedStateService;
    private final BoardMonitor boardMonitor;
    private final PairingDataProvider pairingDataProvider;
    private final OptionsService optionsService;
    private final List<Camera> cameras;
    private final IpMonitor ipMonitor;
    private final ActionsService actionsService;

    @Value("${application.version}")
    private String appVersion;

    @Value("#{hswProperties['client.apk.path']}")
    private String apkFileLocation;

    @Autowired
    public AdminPortal(Uptime uptime, ArmedStateService armedStateService,
                       BoardMonitor boardMonitor, PairingDataProvider pairingDataProvider,
                       IpMonitor ipMonitor, ActionsService actionsService,
                       OptionsService optionsService,
                       List<Camera> cameras) {
        this.uptime = uptime;
        this.armedStateService = armedStateService;
        this.boardMonitor = boardMonitor;
        this.pairingDataProvider = pairingDataProvider;
        this.ipMonitor = ipMonitor;
        this.actionsService = actionsService;
        this.optionsService = optionsService;
        this.cameras = cameras;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView buildIndexPage() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("title", "Home System " + appVersion);
        modelAndView.addObject("armDelaySeconds", optionsService.getOption(DELAYED_ARM_INTERVAL));
        modelAndView.addObject("currentState", armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
        modelAndView.addObject("currentMode", armedStateService.getArmedMode() == AUTOMATIC ? AUTOMATIC.toString() : "MANUAL");
        modelAndView.addObject("uptime", uptime.getUptime());

        return modelAndView;
    }

    @RequestMapping(value = "/pair", method = RequestMethod.GET)
    public ModelAndView pair() throws JsonProcessingException {
        ModelAndView modelAndView = new ModelAndView("pair");
        modelAndView.addObject("pair", pairingDataProvider.getQRCodeImageUrl());
        return modelAndView;
    }

    @RequestMapping(value = "/cameras", method = RequestMethod.GET)
    public ModelAndView buildCamerasPage() {
        ModelAndView modelAndView = new ModelAndView("cameras");

        ArrayList<ArrayList> cameraList = new ArrayList<>();

        cameras.forEach(camera -> {
            ArrayList<String> cameraAttributes = new ArrayList<>();

            cameraAttributes.add(camera.getCameraName());

            if (camera.getMjpegUrl() != null) {
                cameraAttributes.add(camera.getMjpegUrl());
            } else if (camera.getJpegUrl() != null) {
                cameraAttributes.add(camera.getJpegUrl());
            } else {
                cameraAttributes.add("http://www.solidbackgrounds.com/images/640x480/640x480-black-solid-color-background.jpg");
            }

            cameraList.add(cameraAttributes);
        });

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

    @RequestMapping(value = "/currentState", method = RequestMethod.GET)
    public String currentState() {
        return "System is " + armedStateService.getArmedMode().toString() + " and " + (armedStateService.isArmed() ? ARMED.toString() : DISARMED.toString());
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
