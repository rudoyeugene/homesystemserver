package com.rudyii.hsw.services;

import com.rudyii.hsw.objects.events.UploadEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class UploadedFilesRegistrar {
    private static Logger LOG = LogManager.getLogger(UploadedFilesRegistrar.class);

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
            LOG.error("Failed to generate a new server key", e);
        }
    }
}
