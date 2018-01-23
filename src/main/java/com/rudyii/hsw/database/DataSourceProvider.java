package com.rudyii.hsw.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jack on 04.11.16.
 */
@Component
public class DataSourceProvider {
    private static Logger LOG = LogManager.getLogger(DataSourceProvider.class);

    @Value("${jdbc.driverClassName}")
    private String driverClassName;

    @Value("${jdbc.url}")
    private String databaseUrl;

    @Bean
    public DataSource jdbcDataSource() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(databaseUrl);
        return ds;
    }

    @Bean
    public Connection getConnection() throws SQLException {
        new File("database").mkdirs();

        Connection connection = jdbcDataSource().getConnection();

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS DROPBOX_FILES (FILE_NAME STRING UNIQUE, UPLOAD_DATE_TIME DATETIME DEFAULT CURRENT_TIMESTAMP)");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS DROPBOX_FILES_INDEX ON DROPBOX_FILES (FILE_NAME)");

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS ARMED_STATE_HIST (STATE STRING CHECK (STATE = \"ARMED\" OR STATE = \"DISARMED\"), MODIFIED_DATE DATETIME DEFAULT (CURRENT_TIMESTAMP))");

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS ARMED_STATE_STATS (STATS_DATE INTEGER UNIQUE, MINUTES_COUNT INTEGER DEFAULT (0), MODIFIED_DATE DATETIME DEFAULT (CURRENT_TIMESTAMP))");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS ARMED_STATE_STATS_INDEX ON ARMED_STATE_STATS (STATS_DATE)");
        
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS IP_STATE (IP STRING (15) NOT NULL UNIQUE, HOSTNAME STRING DEFAULT HOSTNAME, STATE STRING CHECK (STATE = \"ONLINE\" OR STATE = \"OFFLINE\" OR STATE = \"ERROR\"), MASTER INT (1) DEFAULT (0), MODIFiED_DATE DATETIME DEFAULT (CURRENT_TIMESTAMP))");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS IP_STATE_INDEX ON IP_STATE (IP)");
        
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS SETTINGS (KEY STRING NOT NULL UNIQUE, VALUE)");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS SETTINGS_INDEX ON SETTINGS (KEY)");
        
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS WAN_IP_HIST (IP STRING (15) UNIQUE, MODIFIED_DATE DATETIME DEFAULT (CURRENT_TIMESTAMP))");

        return connection;
    }

    public synchronized void closeConnection() {
        try {
            jdbcDataSource().getConnection().close();
        } catch (SQLException e) {
            LOG.error("Failed to close the connection!", e);
        }
    }

    @PreDestroy
    public synchronized void optimizeDB() throws Exception {
        getConnection().createStatement().execute("VACUUM");
        closeConnection();
    }
}
