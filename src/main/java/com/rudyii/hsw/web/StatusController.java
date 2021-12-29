package com.rudyii.hsw.web;

import com.rudyii.hsw.motion.Camera;
import com.rudyii.hsw.objects.SystemStatusReport;
import com.rudyii.hsw.services.SystemModeAndStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
public class StatusController {
    private final List<Camera> cameras;
    private final SystemModeAndStateService systemModeAndStateService;


    @GetMapping
    public SystemStatusReport getReport() {
        return SystemStatusReport.builder()
                .systemMode(systemModeAndStateService.getSystemMode())
                .systemState(systemModeAndStateService.getSystemState())
                .runningDetectors(cameras.stream().collect(Collectors.toMap(Camera::getCameraName, Camera::isDetectorEnabled)))
                .monitoringModes(cameras.stream().collect(Collectors.toMap(Camera::getCameraName, cam -> cam.getCameraSettings().getMonitoringMode())))
                .build();
    }
}
