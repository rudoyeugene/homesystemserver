package com.rudyii.hsw.services;

import com.rudyii.hsw.objects.events.EventBase;
import com.rudyii.hsw.objects.events.MotionToNotifyEvent;
import com.rudyii.hsw.objects.events.UploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
public class UploadedFilesRegistrar {
    private final Connection connection;

    @Autowired
    public UploadedFilesRegistrar(Connection connection) {
        this.connection = connection;
    }

    @EventListener({UploadEvent.class, MotionToNotifyEvent.class})
    public void registerUploadedFile(EventBase eventBase) {
        if (eventBase instanceof UploadEvent) {
            UploadEvent uploadEvent = (UploadEvent) eventBase;
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO RECORD_FILES (FILE_ID, CREATED) VALUES (?, ?)")) {
                statement.setString(1, uploadEvent.getFileName());
                statement.setLong(2, uploadEvent.getEventId());
                statement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed put data", e);
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO RECORD_FILES (FILE_ID, CREATED) VALUES (?, ?)")) {
                statement.setString(1, uploadEvent.getEventId() + ".event");
                statement.setLong(2, uploadEvent.getEventId());
                statement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed put data", e);
            }
        } else if (eventBase instanceof MotionToNotifyEvent) {
            MotionToNotifyEvent motionToNotifyEvent = (MotionToNotifyEvent) eventBase;
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO RECORD_FILES (FILE_ID, CREATED) VALUES (?, ?)")) {
                statement.setString(1, motionToNotifyEvent.getEventId() + ".jpg");
                statement.setLong(2, motionToNotifyEvent.getEventId());
                statement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed put data", e);
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO RECORD_FILES (FILE_ID, CREATED) VALUES (?, ?)")) {
                statement.setString(1, motionToNotifyEvent.getEventId() + ".event");
                statement.setLong(2, motionToNotifyEvent.getEventId());
                statement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed put data", e);
            }
        }
    }
}
