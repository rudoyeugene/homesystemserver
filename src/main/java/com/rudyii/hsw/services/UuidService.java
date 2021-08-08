package com.rudyii.hsw.services;

import com.rudyii.hsw.objects.events.ServerKeyUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Slf4j
@Service
public class UuidService {
    private final EventService eventService;
    private final Connection connection;

    private String serverKey;

    @Value("#{hswProperties['server.alias']}")
    private String serverAlias;

    public UuidService(Connection connection, EventService eventService) {
        this.connection = connection;
        this.eventService = eventService;
        readServerKey();
    }

    @PostConstruct
    public void resolveSecret() {
        readServerKey();
        if (serverKey == null) {
            generateAndInsertNewServerKey();
        }
    }

    public void resetServerKey() {
        try {
            connection.createStatement().execute("DELETE FROM SETTINGS WHERE KEY = 'SERVER_KEY'");
            this.serverKey = null;
        } catch (SQLException e) {
            log.error("Failed to delete Server Key:", e);
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
            log.error("Failed to get Server Key: ", e);
        }
    }

    private void generateAndInsertNewServerKey() {
        log.info("Generating a new server key...");
        serverKey = UUID.randomUUID().toString();

        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("INSERT INTO SETTINGS (KEY, VALUE) VALUES (?, ?)");
            statement.setString(1, "SERVER_KEY");
            statement.setString(2, serverKey);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to generate a new server key", e);
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
