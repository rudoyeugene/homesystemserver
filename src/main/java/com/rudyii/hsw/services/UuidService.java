package com.rudyii.hsw.services;

import com.rudyii.hsw.events.ServerKeyUpdatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


@Service
public class UuidService {
    private static Logger LOG = LogManager.getLogger(UuidService.class);

    private EventService eventService;
    private Connection connection;

    private String serverKey;

    @Value("${server.alias}")
    private String serverAlias;

    public UuidService(Connection connection, EventService eventService) {
        this.connection = connection;
        this.eventService = eventService;
        readServerKey();
    }

    public void resetServerKey() {
        try {
            connection.createStatement().execute("DELETE FROM SETTINGS WHERE KEY = 'SERVER_KEY'");
            this.serverKey = null;
        } catch (SQLException e) {
            LOG.error("Failed to delete Server Key:", e);
        }
    }

    public String getQRCodeImageUrl() {
        resolveSecret();
        return "https://api.qrserver.com/v1/create-qr-code/?size=256x256&data=" + serverAlias + ":" + serverKey;
    }

    private void resolveSecret() {
        if (serverKey == null) {
            generateAndInsertNewServerKey();
        }
    }

    private void readServerKey() {
        PreparedStatement selectStatement;
        try {
            selectStatement = connection.prepareStatement("SELECT VALUE FROM SETTINGS WHERE KEY = 'SERVER_KEY'");
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                this.serverKey = rs.getString(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to get Server Key: ", e);
        }
    }

    private void generateAndInsertNewServerKey() {
        LOG.info("Generating a new server key...");
        serverKey = UUID.randomUUID().toString();

        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT INTO SETTINGS (KEY, VALUE) VALUES (?, ?)");
            statement.setString(1, "SERVER_KEY");
            statement.setString(2, serverKey);
            statement.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Failed to generate a new server key", e);
        }
        readServerKey();

        eventService.publish(new ServerKeyUpdatedEvent());
    }

    public String getServerKey() {
        resolveSecret();
        return serverKey;
    }

    public String getServerAlias() {
        return serverAlias;
    }
}
