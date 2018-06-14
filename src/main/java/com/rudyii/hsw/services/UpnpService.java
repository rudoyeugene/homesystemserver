package com.rudyii.hsw.services;

import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.events.ArmedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.ConnectException;

import static com.rudyii.hsw.enums.ArmedStateEnum.ARMED;
import static com.rudyii.hsw.enums.ArmedStateEnum.DISARMED;


@Service
public class UpnpService {
    private static Logger LOG = LogManager.getLogger(UpnpService.class);

    private boolean isPortsOpen;

    private GatewayDevice gateway;
    private IspService ispService;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;

    public UpnpService(IspService ispService, CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.ispService = ispService;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;

        reloadGateway();
    }

    @PostConstruct
    public void openPortsAutomatically() {
        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            if (cameraMotionDetectionController.openPortsOnStartup()) {
                if (cameraMotionDetectionController.openOuterHttpPort()) {
                    openPortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getIp(), cameraMotionDetectionController.getHttpInnerPort(), cameraMotionDetectionController.getHttpOuterPort());
                }
                if (cameraMotionDetectionController.openOuterRtspPort()) {
                    openPortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getIp(), cameraMotionDetectionController.getRtspInnerPort(), cameraMotionDetectionController.getRtspOuterPort());
                }
            }
        }
    }

    @Async
    @EventListener(ArmedEvent.class)
    public void onEvent(ArmedEvent event) {
        if (event.getArmedState().equals(ARMED)) {
            openPorts();
        } else if (event.getArmedState().equals(DISARMED)) {
            closePorts();
        } else {
            LOG.warn("New ArmedEvent received but ports state unchanged.");
        }

    }

    public void openPorts() {
        isPortsOpen = true;
        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            if (cameraMotionDetectionController.openOuterHttpPort()) {
                openPortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getIp(), cameraMotionDetectionController.getHttpInnerPort(), cameraMotionDetectionController.getHttpOuterPort());
            }
            if (cameraMotionDetectionController.openOuterRtspPort()) {
                openPortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getIp(), cameraMotionDetectionController.getRtspInnerPort(), cameraMotionDetectionController.getRtspOuterPort());
            }
        }
    }

    public void closePorts() {
        isPortsOpen = false;
        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            if (!cameraMotionDetectionController.openPortsOnStartup()) {
                closePortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getHttpOuterPort());
                closePortFor(cameraMotionDetectionController.getCameraName(), cameraMotionDetectionController.getRtspOuterPort());
            } else {
                LOG.warn("Port for " + buildDescription(cameraMotionDetectionController.getCameraName(), 0, 0) + " will stay open because it was opened on startup");
            }
        }
    }

    public boolean isPortsOpen() {
        return isPortsOpen;
    }

    private void openPortFor(String cameraName, String ip, Integer innerPort, Integer outerPort) {
        try {
            boolean successed;

            if (gateway == null) {
                reloadGateway();
            }

            successed = gateway.addPortMapping(outerPort, innerPort, ip, "TCP", buildDescription(cameraName, innerPort, outerPort));

            if (successed) {
                LOG.info("Opening port for " + buildDescription(cameraName, innerPort, outerPort) + " successed");
            } else {
                LOG.warn("UPNP service is running in Secure mode, trying the alternate mode: opening server:outerPort <> gateway:outerPort");
                LOG.warn("Additional Server configuration required!");
                LOG.warn("eg.: enable port forwarding from server to cameras");
                successed = gateway.addPortMapping(outerPort, outerPort, ispService.getLocalIpAddress(), "TCP", buildDescription(ispService.getLocalIpAddress(), outerPort, outerPort));
                LOG.info("Opening port for " + buildDescription(cameraName, outerPort, outerPort) + (successed ? " successed" : " failed"));
            }

        } catch (Exception e) {
            LOG.error("Failed to open port for: ", e);
            if (e instanceof ConnectException) {
                reloadGateway();
            }
        }
    }

    private void closePortFor(String cameraName, Integer outerPort) {
        try {
            if (gateway == null) {
                reloadGateway();
            }

            boolean successed = gateway.deletePortMapping(outerPort, "TCP");
            LOG.info("Closing port for " + cameraName + ">" + outerPort + (successed ? " successed" : " failed"));
        } catch (Exception e) {
            LOG.error("Failed to close port for: ", e);
            if (e instanceof ConnectException) {
                reloadGateway();
            }
        }
    }

    private void reloadGateway() {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            discover.discover();
            gateway = discover.getValidGateway();
            LOG.info("Discovered gateway device: " + gateway.getModelName() + ", " + gateway.getModelDescription());
        } catch (Exception e) {
            LOG.error("Failed to discovered gateway device, ", e);
        }
    }

    private String buildDescription(String cameraName, Integer innerPort, Integer outerPort) {
        return cameraName + ":" + innerPort + ">" + outerPort;
    }
}
