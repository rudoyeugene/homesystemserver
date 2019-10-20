package com.rudyii.hsw.services;

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
    private Connection connection;

    @Autowired
    public UploadedFilesRegistrar(Connection connection) {
        this.connection = connection;
    }

    @EventListener({UploadEvent.class})
    public void registerUploadedFile(UploadEvent uploadEvent) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT OR REPLACE INTO DROPBOX_FILES (FILE_NAME) VALUES (?)");
            statement.setString(1, uploadEvent.getFileName());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to generate a new server key", e);
        }
    }
}
