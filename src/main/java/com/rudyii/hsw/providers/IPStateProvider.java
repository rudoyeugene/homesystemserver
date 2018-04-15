package com.rudyii.hsw.providers;

import com.rudyii.hsw.enums.IPStateEnum;
import com.rudyii.hsw.events.IPEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static com.rudyii.hsw.enums.IPStateEnum.*;

@Component
public class IPStateProvider {
    private static Logger LOG = LogManager.getLogger(IPStateProvider.class);

    private Connection connection;
    private List<String> masterIpList;
    private Map<String, String> ipResolver;

    @Autowired
    public IPStateProvider(List masterIpList, Map ipResolver, Connection connection) {
        this.masterIpList = masterIpList;
        this.ipResolver = ipResolver;
        this.connection = connection;
    }

    @PostConstruct
    private void resetIpStatesToOffline() throws Exception {
        Statement statement = connection.createStatement();
        int rowsUpdated = statement.executeUpdate("UPDATE IP_STATE SET STATE = 'OFFLINE' WHERE STATE = 'ONLINE'");
        LOG.info("Updated to OFFLINE " + rowsUpdated + " addresses");
    }

    public IPStateEnum getIPState(String ip) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT STATE FROM IP_STATE WHERE IP = ?");
            statement.setString(1, ip);
            ResultSet rs = statement.executeQuery();
            if (rs.getString(1).equals("ONLINE")) {
                return ONLINE;
            } else if (rs.getString(1).equals("OFFLINE")) {
                return OFFLINE;
            }
        } catch (SQLException e) {
            LOG.error("Failed to get state for address: " + ip, e);
        }
        return ERROR;
    }

    private void setIPState(String ip, IPStateEnum state) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO IP_STATE (IP, HOSTNAME, MASTER, STATE) VALUES (?,?,?,?)");
            statement.setString(1, ip);
            statement.setString(2, ipResolver.get(ip) == null ? ip : ipResolver.get(ip));

            if (masterIpList.contains(ip)) {
                statement.setInt(3, 1);
            } else {
                statement.setInt(3, 0);
            }

            if (state.equals(ONLINE)) {
                if (getIPState(ip) == OFFLINE) {
                    LOG.warn((ipResolver.get(ip) == null ? ip : ipResolver.get(ip)) + " is back ONLINE");
                }

                statement.setString(4, String.valueOf(ONLINE));
                statement.execute();

            } else {
                if (getIPState(ip) == ONLINE) {
                    LOG.warn((ipResolver.get(ip) == null ? ip : ipResolver.get(ip)) + " has fall OFFLINE");
                }

                statement.setString(4, String.valueOf(OFFLINE));
                statement.execute();
            }

        } catch (SQLException e) {
            LOG.error("Failed to set state for address: " + ip, e);
        }
    }

    public Boolean mastersOnline() {
        ResultSet rs;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM IP_STATE WHERE STATE = 'ONLINE' AND MASTER = 1");
            rs = statement.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOG.error("Error getting data from DB", e);
        }
        return false;
    }

    @Async
    @EventListener(IPEvent.class)
    public void onEvent(IPEvent event) {
        setIPState(event.getIp(), event.getState());
    }
}
