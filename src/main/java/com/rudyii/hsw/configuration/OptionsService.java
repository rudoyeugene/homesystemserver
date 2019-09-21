package com.rudyii.hsw.configuration;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.objects.events.OptionsChangedEvent;
import com.rudyii.hsw.objects.events.ServerKeyUpdatedEvent;
import com.rudyii.hsw.services.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OptionsService {
    public static final String RECORD_INTERVAL = "recordInterval";
    public static final String SHOW_MOTION_AREA = "showMotionArea";
    public static final String HOURLY_REPORT_ENABLED = "hourlyReportEnabled";
    public static final String HOURLY_REPORT_FORCED = "hourlyReportForced";
    public static final String MONITORING_ENABLED = "monitoringEnabled";
    public static final String COLLECT_STATISTICS = "collectStatistics";
    public static final String KEEP_DAYS = "keepDays";
    public static final String VERBOSE_OUTPUT_ENABLED = "verboseOutputEnabled";
    public static final String DELAYED_ARM_INTERVAL = "delayedArmInterval";
    public static final String CONTINUOUS_MONITORING = "continuousMonitoring";
    public static final String HEALTH_CHECK_ENABLED = "healthCheckEnabled";
    public static final String NOISE_LEVEL = "noiseLevel";
    public static final String MOTION_AREA = "motionArea";
    public static final String REBOOT_TIMEOUT = "rebootTimeout";
    public static final String INTERVAL = "interval";
    public static final String CAMERAS = "cameras";

    private ConcurrentHashMap<String, Object> localOptions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Object> localCamerasOptions = new ConcurrentHashMap<>();
    private ArrayList<DatabaseReference> databaseReferences = new ArrayList<>();
    private ArrayList<ValueEventListener> valueEventListeners = new ArrayList<>();
    private EventService eventService;
    private FirebaseDatabaseProvider databaseProvider;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;

    @Autowired
    public OptionsService(EventService eventService, FirebaseDatabaseProvider databaseProvider,
                          CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.eventService = eventService;
        this.databaseProvider = databaseProvider;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @PostConstruct
    private void populateDefaultOptions() {
        //Motion detection settings
        localOptions.put(RECORD_INTERVAL, 10L);
        localOptions.put(SHOW_MOTION_AREA, true);

        //Reporting settings
        localOptions.put(HOURLY_REPORT_ENABLED, true);
        localOptions.put(HOURLY_REPORT_FORCED, false);
        localOptions.put(MONITORING_ENABLED, true);

        //Stats settings
        localOptions.put(COLLECT_STATISTICS, true);
        localOptions.put(KEEP_DAYS, 30L);

        //System settings
        localOptions.put(VERBOSE_OUTPUT_ENABLED, true);
        localOptions.put(DELAYED_ARM_INTERVAL, 60L);

        fillOptionsFromCameras();

        syncCameras();

        registerListeners();
    }

    private void registerListeners() {
        DatabaseReference optionsRef = databaseProvider.getReference("/options");
        databaseReferences.add(optionsRef);
        ValueEventListener optionsValueEventListener = getOptionsValueEventListener();
        valueEventListeners.add(optionsValueEventListener);
        optionsRef.addValueEventListener(optionsValueEventListener);
    }

    private ValueEventListener getOptionsValueEventListener() {
        return new ValueEventListener() {
            private boolean optionsUpdated, newOptionsAdded;

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    log.warn("Initializing options");

                    HashMap<String, Object> tempOptions = new HashMap<>(localOptions);
                    HashMap<String, Object> tempCamerasOptions = new HashMap<>(localCamerasOptions);
                    tempOptions.put(CAMERAS, tempCamerasOptions);
                    databaseProvider.pushData("/options", tempOptions);

                    return;
                }

                HashMap<String, Object> cloudOptions = new HashMap<>(((HashMap<String, Object>) dataSnapshot.getValue()));
                HashMap<String, Object> cloudOptionsWithoutCameras = new HashMap<>(cloudOptions);
                cloudOptionsWithoutCameras.remove(CAMERAS);
                HashMap<String, Object> cloudCamerasOptionsOnly = new HashMap<>((HashMap<String, Object>) cloudOptions.get(CAMERAS));

                synchronizeOptions(cloudOptions, cloudOptionsWithoutCameras, localOptions);
                synchronizeCameraOptions(cloudOptions, cloudCamerasOptionsOnly, localCamerasOptions);

                if (optionsUpdated) {
                    optionsUpdated = false;
                    log.warn("Options updated, firing event");
                    eventService.publish(new OptionsChangedEvent(localOptions));
                }

                if (newOptionsAdded) {
                    newOptionsAdded = false;
                    log.warn("New option added, pushing");
                    databaseProvider.pushData("/options", cloudOptions);
                }
            }

            private void synchronizeOptions(HashMap<String, Object> cloudOptions, HashMap<String, Object> cloudOptionsWithoutCameras, ConcurrentHashMap<String, Object> localOptions) {
                localOptions.forEach((localOption, localValue) -> {
                    Object cloudValue = cloudOptionsWithoutCameras.get(localOption);

                    if (cloudValue == null) {
                        newOptionsAdded = true;
                        log.warn("Pushing new option: " + localOption + "=" + localValue);
                        cloudOptions.put(localOption, localValue);
                    } else if (!localValue.equals(cloudValue)) {
                        optionsUpdated = true;
                        log.warn("Option updated: " + localOption + "=" + cloudValue);
                        localOptions.put(localOption, cloudValue);
                    }
                });
            }

            private void synchronizeCameraOptions(HashMap<String, Object> cloudOptions, HashMap<String, Object> cloudCamerasOptionsOnly, ConcurrentHashMap<String, Object> localCamerasOptions) {
                localCamerasOptions.forEach((cameraName, cameraOptions) -> {
                    HashMap<String, Object> cloudCameraOptions = (HashMap<String, Object>) cloudCamerasOptionsOnly.get(cameraName);
                    ConcurrentHashMap<String, Object> localCameraOptions = (ConcurrentHashMap<String, Object>) localCamerasOptions.get(cameraName);

                    localCameraOptions.forEach((localCameraOption, localValue) -> {
                        Object cloudValue = cloudCameraOptions.get(localCameraOption);

                        if (cloudValue == null) {
                            newOptionsAdded = true;
                            log.warn("Pushing new option for Camera " + cameraName + ": " + localCameraOption + "=" + localValue);
                            ((HashMap<String, Object>) ((HashMap<String, Object>) cloudOptions.get(CAMERAS)).get(cameraName)).put(localCameraOption, localValue);
                        } else if (!localValue.equals(cloudValue)) {
                            optionsUpdated = true;
                            log.warn("Option updated on Camera " + cameraName + ": " + localCameraOption + "=" + cloudValue);
                            localCameraOptions.put(localCameraOption, cloudValue);
                        }
                    });

                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void fillOptionsFromCameras() {
        for (CameraMotionDetectionController cameraMotionDetectionController : cameraMotionDetectionControllers) {
            ConcurrentHashMap<String, Object> cameraOptions = new ConcurrentHashMap<>();

            String cameraName = cameraMotionDetectionController.getCameraName();
            Long interval = cameraMotionDetectionController.getInterval();
            Long rebootTimeout = cameraMotionDetectionController.getRebootTimeout();
            Long motionArea = cameraMotionDetectionController.getMotionArea();
            Long noiseLevel = cameraMotionDetectionController.getNoiseLevel();

            cameraOptions.put(INTERVAL, interval);
            cameraOptions.put(REBOOT_TIMEOUT, rebootTimeout);
            cameraOptions.put(MOTION_AREA, motionArea);
            cameraOptions.put(NOISE_LEVEL, noiseLevel);
            cameraOptions.put(HEALTH_CHECK_ENABLED, cameraMotionDetectionController.isHealthCheckEnabled());
            cameraOptions.put(CONTINUOUS_MONITORING, cameraMotionDetectionController.isContinuousMonitoring());

            localCamerasOptions.put(cameraName, cameraOptions);
        }
    }

    private void syncCameras() {
        DatabaseReference cloudCamerasOptions = databaseProvider.getReference("/options/cameras");

        cloudCamerasOptions.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> cloudCamerasOptions = (Map<String, Object>) dataSnapshot.getValue();

                localCamerasOptions.forEach((cameraName, options) -> {
                    if (!cloudCamerasOptions.containsKey(cameraName)) {
                        databaseProvider.getReference("/options/cameras/" + cameraName).setValueAsync(localCamerasOptions.get(cameraName));
                    }
                });

                cloudCamerasOptions.forEach((cameraName, options) -> {
                    if (!localCamerasOptions.containsKey(cameraName)) {
                        databaseProvider.getReference("/options/cameras/" + cameraName).removeValueAsync();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.error("Failed to retrieve Options data", databaseError.toException());
            }
        });
    }

    @EventListener(ServerKeyUpdatedEvent.class)
    public void reRegister() {
        unregisterListeners();
        populateDefaultOptions();
    }

    @PreDestroy
    private void unregisterListeners() {
        for (DatabaseReference reference : databaseReferences) {
            for (ValueEventListener eventListener : valueEventListeners) {
                reference.removeEventListener(eventListener);
            }
        }
    }

    public Object getOption(String option) {
        return localOptions.get(option);
    }

    public ConcurrentHashMap<String, Object> getCameraOptions(String cameraName) {
        return (ConcurrentHashMap<String, Object>) localCamerasOptions.get(cameraName);
    }
}
