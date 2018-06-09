package com.rudyii.hsw.configuration;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.rudyii.hsw.database.FirebaseDatabaseProvider;
import com.rudyii.hsw.events.OptionsChangedEvent;
import com.rudyii.hsw.events.ServerKeyUpdatedEvent;
import com.rudyii.hsw.motion.CameraMotionDetectionController;
import com.rudyii.hsw.services.EventService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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
    private static Logger LOG = LogManager.getLogger(OptionsService.class);

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
                    LOG.warn("Initializing options");

                    HashMap<String, Object> tempOptions = new HashMap<>(localOptions);
                    HashMap<String, Object> tempCamerasOptions = new HashMap<>(localCamerasOptions);
                    tempOptions.put("cameras", tempCamerasOptions);
                    databaseProvider.pushData("/options", tempOptions);

                    return;
                }

                HashMap<String, Object> cloudOptions = new HashMap<>(((HashMap<String, Object>) dataSnapshot.getValue()));
                HashMap<String, Object> cloudOptionsWithoutCameras = new HashMap<>(cloudOptions);
                cloudOptionsWithoutCameras.remove("cameras");
                HashMap<String, Object> cloudCamerasOptionsOnly = new HashMap<>((HashMap<String, Object>) cloudOptions.get("cameras"));

                synchronizeOptions(cloudOptions, cloudOptionsWithoutCameras, localOptions);
                synchronizeCameraOptions(cloudOptions, cloudCamerasOptionsOnly, localCamerasOptions);

                if (optionsUpdated) {
                    optionsUpdated = false;
                    LOG.warn("Options updated, firing event");
                    eventService.publish(new OptionsChangedEvent(localOptions));
                }

                if (newOptionsAdded) {
                    newOptionsAdded = false;
                    LOG.warn("New option added, pushing");
                    databaseProvider.pushData("/options", cloudOptions);
                }
            }

            private void synchronizeOptions(HashMap<String, Object> cloudOptions, HashMap<String, Object> cloudOptionsWithoutCameras, ConcurrentHashMap<String, Object> localOptions) {
                localOptions.forEach((localOption, localValue) -> {
                    Object cloudValue = cloudOptionsWithoutCameras.get(localOption);

                    if (cloudValue == null) {
                        newOptionsAdded = true;
                        LOG.warn("Pushing new option: " + localOption + "=" + localValue);
                        cloudOptions.put(localOption, localValue);
                    } else if (!localValue.equals(cloudValue)) {
                        optionsUpdated = true;
                        LOG.warn("Option updated: " + localOption + "=" + cloudValue);
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
                            LOG.warn("Pushing new option for Camera " + cameraName + ": " + localCameraOption + "=" + localValue);
                            ((HashMap<String, Object>) ((HashMap<String, Object>) cloudOptions.get("cameras")).get(cameraName)).put(localCameraOption, localValue);
                        } else if (!localValue.equals(cloudValue)) {
                            optionsUpdated = true;
                            LOG.warn("Option updated on Camera " + cameraName + ": " + localCameraOption + "=" + cloudValue);
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

            cameraOptions.put("interval", interval);
            cameraOptions.put("rebootTimeout", rebootTimeout);
            cameraOptions.put("motionArea", motionArea);
            cameraOptions.put("noiseLevel", noiseLevel);

            localCamerasOptions.put(cameraName, cameraOptions);
        }
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
