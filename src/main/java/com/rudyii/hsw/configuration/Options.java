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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Options {
    private ConcurrentHashMap<String, Object> options = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Object> camerasOptions = new ConcurrentHashMap<>();
    private ArrayList<DatabaseReference> databaseReferences = new ArrayList<>();
    private ArrayList<ValueEventListener> valueEventListeners = new ArrayList<>();
    private EventService eventService;
    private FirebaseDatabaseProvider databaseProvider;
    private CameraMotionDetectionController[] cameraMotionDetectionControllers;

    @Autowired
    public Options(EventService eventService, FirebaseDatabaseProvider databaseProvider,
                   CameraMotionDetectionController... cameraMotionDetectionControllers) {
        this.eventService = eventService;
        this.databaseProvider = databaseProvider;
        this.cameraMotionDetectionControllers = cameraMotionDetectionControllers;
    }

    @PostConstruct
    private void populateDefaultOptions() {
        //Motion detection settings
        options.put("recordInterval", 10000L);
        options.put("showMotionArea", true);

        //Reporting settings
        options.put("hourlyReportEnabled", true);
        options.put("hourlyReportForced", false);
        options.put("monitoringEnabled", true);

        //Stats settings
        options.put("collectStatistics", true);
        options.put("keepDays", 30L);

        //System settings
        options.put("redirectSystemOutToLogFile", true);
        options.put("delayedArmInterval", 60L);

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
            private boolean optionsUpdated;

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    HashMap<String, Object> tempOptions = new HashMap<>(options);
                    HashMap<String, Object> tempCamerasOptions = new HashMap<>(camerasOptions);
                    tempOptions.put("cameras", tempCamerasOptions);
                    databaseProvider.pushData("/options", tempOptions);
                    return;
                }

                HashMap<String, Object> cloudOptions = new HashMap<>(((HashMap<String, Object>) dataSnapshot.getValue()));
                HashMap<String, Object> cloudCamerasOptions = new HashMap<>((HashMap<String, Object>) cloudOptions.get("cameras"));
                cloudOptions.remove("cameras");

                synchronizeOptions(cloudOptions, options);
                synchronizeCameraOptions(cloudCamerasOptions, camerasOptions);

                if (optionsUpdated) {
                    eventService.publish(new OptionsChangedEvent(options));
                }
            }

            private void synchronizeCameraOptions(HashMap<String, Object> cloudOptions, ConcurrentHashMap<String, Object> localOptions) {
                localOptions.forEach((k, v) -> {
                    HashMap<String, Object> cloudCameraOptions = (HashMap<String, Object>) cloudOptions.get(k);
                    ConcurrentHashMap<String, Object> localCameraOptions = (ConcurrentHashMap<String, Object>) localOptions.get(k);

                    localCameraOptions.forEach((k1, v1) -> {
                        Object cloudOption = cloudCameraOptions.get(k1);
                        Object localOption = localCameraOptions.get(k1);
                        if (cloudOption != null && !cloudOption.equals(localOption)) {
                            localCameraOptions.put(k1, cloudCameraOptions.get(k1));
                            optionsUpdated = true;
                        }
                    });

                });
            }

            private void synchronizeOptions(HashMap<String, Object> cloudOptions, ConcurrentHashMap<String, Object> localOptions) {
                localOptions.forEach((k, v) -> {
                    Object cloudOption = cloudOptions.get(k);
                    Object localOption = localOptions.get(k);

                    if (cloudOption != null && !cloudOption.equals(localOption)) {
                        localOptions.put(k, cloudOptions.get(k));
                        optionsUpdated = true;
                    }
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

            camerasOptions.put(cameraName, cameraOptions);
        }

        options.put("cameras", camerasOptions);
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
        return options.get(option);
    }

    public ConcurrentHashMap<String, Object> getCameraOptions(String cameraName) {
        return (ConcurrentHashMap<String, Object>) camerasOptions.get(cameraName);
    }
}
